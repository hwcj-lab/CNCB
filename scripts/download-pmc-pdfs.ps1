[CmdletBinding()]
param(
    [Parameter()]
    [string]$ExcelPath = 'F:\工作任务\papers\261116\ICG_2026_All_39_New_Papers.xlsx',

    [Parameter()]
    [string]$OutputDirectory = 'F:\工作任务\papers\261116',

    [Parameter()]
    [int]$MaxRetries = 3,

    # Optional test/partial-run limit. Zero processes every data row.
    [Parameter()]
    [ValidateRange(0, 1000000)]
    [int]$MaxRows = 0
)

Set-StrictMode -Version Latest
$ErrorActionPreference = 'Stop'
$ProgressPreference = 'SilentlyContinue'
[Net.ServicePointManager]::SecurityProtocol = [Net.SecurityProtocolType]::Tls12

function Invoke-WithRetry {
    param(
        [Parameter(Mandatory)] [scriptblock]$Action,
        [Parameter(Mandatory)] [string]$Description
    )

    $lastError = $null
    for ($attempt = 1; $attempt -le $MaxRetries; $attempt++) {
        try {
            return & $Action
        }
        catch {
            $lastError = $_
            if ($attempt -lt $MaxRetries) {
                $delaySeconds = [Math]::Min(10, [Math]::Pow(2, $attempt))
                Write-Warning "$Description 失败，第 $attempt 次重试将在 $delaySeconds 秒后进行。"
                Start-Sleep -Seconds $delaySeconds
            }
        }
    }

    throw $lastError
}

function Get-PmcId {
    param([Parameter(Mandatory)] [string]$PMID)

    $url = "https://pmc.ncbi.nlm.nih.gov/tools/idconv/api/v1/articles/?ids=$Pmid&format=json"
    $response = Invoke-WithRetry -Description "查询 PMID $Pmid 对应的 PMCID" -Action {
        Invoke-RestMethod -Uri $url -Method Get -TimeoutSec 45
    }

    $record = @($response.records) | Select-Object -First 1
    $pmcidProperty = if ($null -ne $record) { $record.PSObject.Properties['pmcid'] } else { $null }
    $pmcid = if ($null -ne $pmcidProperty) { [string]$pmcidProperty.Value } else { $null }
    if ([string]::IsNullOrWhiteSpace($pmcid)) {
        return $null
    }
    return $pmcid.Trim().ToUpperInvariant()
}

function Convert-S3UrlToHttps {
    param([Parameter(Mandatory)] [string]$Url)

    if ($Url.StartsWith('s3://pmc-oa-opendata/')) {
        return 'https://pmc-oa-opendata.s3.amazonaws.com/' +
            $Url.Substring('s3://pmc-oa-opendata/'.Length)
    }
    return $Url
}

function Get-PmcPdfUrl {
    param([Parameter(Mandatory)] [string]$PmcId)

    # PMC 2026年起通过公开AWS数据集分发可自动获取的全文文件。
    $listUrl = "https://pmc-oa-opendata.s3.amazonaws.com/?list-type=2&prefix=$PmcId.&delimiter=/"
    $listing = Invoke-WithRetry -Description "查询 $PmcId 的开放数据版本" -Action {
        Invoke-WebRequest -UseBasicParsing -Uri $listUrl -TimeoutSec 45
    }

    [xml]$xml = $listing.Content
    # Keep the pipeline result as an array even when S3 returns one version.
    $prefixes = @(
        @($xml.GetElementsByTagName('Prefix')) |
            ForEach-Object { ([string]$_.InnerText).TrimEnd('/') } |
            Where-Object { $_ -match "^$([Regex]::Escape($PmcId))\.\d+$" } |
            Sort-Object -Unique
    )

    if ($prefixes.Count -eq 0) {
        return $null
    }

    $candidates = @(foreach ($prefix in $prefixes) {
        $metadataUrl = "https://pmc-oa-opendata.s3.amazonaws.com/metadata/$prefix.json"
        try {
            $metadata = Invoke-WithRetry -Description "读取 $prefix 元数据" -Action {
                Invoke-RestMethod -Uri $metadataUrl -Method Get -TimeoutSec 45
            }
            $pdfProperty = $metadata.PSObject.Properties['pdf_url']
            $pdfUrl = if ($null -ne $pdfProperty) { [string]$pdfProperty.Value } else { $null }
            if (-not [string]::IsNullOrWhiteSpace($pdfUrl)) {
                $manuscriptProperty = $metadata.PSObject.Properties['is_manuscript']
                $versionProperty = $metadata.PSObject.Properties['version']
                [pscustomobject]@{
                    PdfUrl       = Convert-S3UrlToHttps $pdfUrl
                    IsManuscript = ($null -ne $manuscriptProperty -and [string]$manuscriptProperty.Value -eq 'yes')
                    Version      = if ($null -ne $versionProperty) { [int]$versionProperty.Value } else { 0 }
                }
            }
        }
        catch {
            Write-Warning "无法读取 $prefix 元数据：$($_.Exception.Message)"
        }
    })

    # 优先正式发表版本；同类版本中优先较新的PMC处理版本。
    $selected = @($candidates) |
        Sort-Object IsManuscript, @{ Expression = 'Version'; Descending = $true } |
        Select-Object -First 1
    if ($null -eq $selected) {
        return $null
    }
    return $selected.PdfUrl
}

function Test-PdfFile {
    param([Parameter(Mandatory)] [string]$Path)

    if (-not (Test-Path -LiteralPath $Path -PathType Leaf)) {
        return $false
    }
    $stream = [IO.File]::OpenRead($Path)
    try {
        if ($stream.Length -lt 5) { return $false }
        $bytes = New-Object byte[] 5
        [void]$stream.Read($bytes, 0, 5)
        return ([Text.Encoding]::ASCII.GetString($bytes) -eq '%PDF-')
    }
    finally {
        $stream.Dispose()
    }
}

function Save-PmcPdf {
    param(
        [Parameter(Mandatory)] [string]$Pmid,
        [Parameter(Mandatory)] [string]$Destination
    )

    if (Test-PdfFile -Path $Destination) {
        return $true
    }

    $pmcId = Get-PmcId -Pmid $Pmid
    if ([string]::IsNullOrWhiteSpace($pmcId)) {
        Write-Host "[$Pmid] No：未找到 PMCID。" -ForegroundColor DarkYellow
        return $false
    }

    $pdfUrl = Get-PmcPdfUrl -PmcId $pmcId
    if ([string]::IsNullOrWhiteSpace($pdfUrl)) {
        Write-Host "[$Pmid] No：$pmcId 不在可自动下载的数据集中，或没有PDF。" -ForegroundColor DarkYellow
        return $false
    }

    $temporaryPath = "$Destination.part"
    Remove-Item -LiteralPath $temporaryPath -Force -ErrorAction SilentlyContinue
    try {
        Invoke-WithRetry -Description "下载 PMID $Pmid PDF" -Action {
            Invoke-WebRequest -UseBasicParsing -Uri $pdfUrl -OutFile $temporaryPath -TimeoutSec 180
        } | Out-Null

        if (-not (Test-PdfFile -Path $temporaryPath)) {
            throw '服务器返回的文件不是有效PDF。'
        }
        Move-Item -LiteralPath $temporaryPath -Destination $Destination -Force
        Write-Host "[$Pmid] Yes：已保存 $Destination" -ForegroundColor Green
        return $true
    }
    catch {
        Write-Warning "[$Pmid] 下载失败：$($_.Exception.Message)"
        return $false
    }
    finally {
        Remove-Item -LiteralPath $temporaryPath -Force -ErrorAction SilentlyContinue
    }
}

if (-not (Test-Path -LiteralPath $ExcelPath -PathType Leaf)) {
    throw "Excel文件不存在：$ExcelPath"
}
New-Item -ItemType Directory -Path $OutputDirectory -Force | Out-Null

$backupPath = Join-Path ([IO.Path]::GetDirectoryName($ExcelPath)) (
    [IO.Path]::GetFileNameWithoutExtension($ExcelPath) +
    '.backup-' + (Get-Date -Format 'yyyyMMdd-HHmmss') + '.xlsx'
)
Copy-Item -LiteralPath $ExcelPath -Destination $backupPath
Write-Host "已创建Excel备份：$backupPath"

$excel = $null
$workbook = $null
$worksheet = $null
try {
    $excel = New-Object -ComObject Excel.Application
    $excel.Visible = $false
    $excel.DisplayAlerts = $false
    $workbook = $excel.Workbooks.Open($ExcelPath, 0, $false)

    $pmidColumn = 0
    $downloadColumn = 0
    foreach ($sheet in @($workbook.Worksheets)) {
        $lastColumn = $sheet.UsedRange.Columns.Count
        $headers = @{}
        for ($column = 1; $column -le $lastColumn; $column++) {
            $header = ([string]$sheet.Cells.Item(1, $column).Text).Trim().ToLowerInvariant()
            if ($header) { $headers[$header] = $column }
        }
        if ($headers.ContainsKey('pmid') -and $headers.ContainsKey('download')) {
            $worksheet = $sheet
            $pmidColumn = $headers['pmid']
            $downloadColumn = $headers['download']
            break
        }
        [void][Runtime.InteropServices.Marshal]::ReleaseComObject($sheet)
    }

    if ($null -eq $worksheet) {
        throw '未找到同时包含 PMID 和 Download 表头的工作表。'
    }

    $lastRow = $worksheet.UsedRange.Row + $worksheet.UsedRange.Rows.Count - 1
    $yesCount = 0
    $noCount = 0
    $processedCount = 0
    for ($row = 2; $row -le $lastRow; $row++) {
        if ($MaxRows -gt 0 -and $processedCount -ge $MaxRows) { break }
        $rawPmid = $worksheet.Cells.Item($row, $pmidColumn).Value2
        if ($null -eq $rawPmid) { continue }
        $processedCount++

        $pmid = if ($rawPmid -is [double] -or $rawPmid -is [decimal]) {
            ([decimal]$rawPmid).ToString('0')
        }
        else {
            ([string]$rawPmid).Trim()
        }
        if ($pmid -notmatch '^\d+$') {
            Write-Warning "第 $row 行 PMID 无效：$rawPmid"
            $worksheet.Cells.Item($row, $downloadColumn).Value2 = 'No'
            $noCount++
            $workbook.Save()
            continue
        }

        $destination = Join-Path $OutputDirectory "$pmid.pdf"
        $downloaded = Save-PmcPdf -Pmid $pmid -Destination $destination
        $worksheet.Cells.Item($row, $downloadColumn).Value2 = if ($downloaded) { 'Yes' } else { 'No' }
        if ($downloaded) { $yesCount++ } else { $noCount++ }

        # 每行保存，网络或人工中断后已完成的状态不会丢失。
        $workbook.Save()
        Start-Sleep -Milliseconds 350
    }

    Write-Host "处理完成：Yes=$yesCount，No=$noCount，输出目录=$OutputDirectory" -ForegroundColor Cyan
}
finally {
    if ($null -ne $workbook) {
        try { $workbook.Close($true) } catch {}
        [void][Runtime.InteropServices.Marshal]::ReleaseComObject($workbook)
    }
    if ($null -ne $worksheet) {
        [void][Runtime.InteropServices.Marshal]::ReleaseComObject($worksheet)
    }
    if ($null -ne $excel) {
        try { $excel.Quit() } catch {}
        [void][Runtime.InteropServices.Marshal]::ReleaseComObject($excel)
    }
    [GC]::Collect()
    [GC]::WaitForPendingFinalizers()
}
