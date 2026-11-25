-- 仅供已经创建过openlb_route_pending的数据库执行；新部署无需执行。
-- 将每个PMID最新的暂存版本迁移到OpenLB。已有OpenLB记录仅在暂存版本更新时覆盖。
-- 建议在停止OpenLB采集后执行；不会访问上游。
START TRANSACTION;
INSERT INTO openlb_publication(
pmid,doi,title,abstract_text,pub_year,journal_title,source_url,authors_json,affiliation_info,keyword_list,mesh_headings_json,grants_json,cncb_banks_json,raw_payload,source_hash,fetched_at)
SELECT
pmid,
NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.doi')),'null'),
COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.title')),'null'),CONCAT('PMID ',pmid)),
NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.abstractText')),'null'),
CAST(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.pubYear')),'null') AS UNSIGNED),
NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.journalTitle')),'null'),
COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.sourceUrl')),'null'),CONCAT('https://ngdc.cncb.ac.cn/openlb/api/pmid?pmid=',pmid)),
NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.authorsJson')),'null'),
NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.affiliationInfo')),'null'),
NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.keywordList')),'null'),
NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.meshHeadingsJson')),'null'),
NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.grantsJson')),'null'),
NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.cncbBanksJson')),'null'),
COALESCE(NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.rawPayload')),'null'),payload),
NULLIF(JSON_UNQUOTE(JSON_EXTRACT(payload,'$.sourceHash')),'null'),update_time
FROM (
 SELECT p.*,ROW_NUMBER() OVER(PARTITION BY pmid ORDER BY update_time DESC,id DESC) rn
 FROM openlb_route_pending p
) latest WHERE rn=1
ON DUPLICATE KEY UPDATE
relation_status=IF((fetched_at IS NULL OR fetched_at<VALUES(fetched_at)) AND NOT(source_hash<=>VALUES(source_hash)),'PENDING',relation_status),
doi=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(doi),doi),
title=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(title),title),
abstract_text=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(abstract_text),abstract_text),
pub_year=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(pub_year),pub_year),
journal_title=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(journal_title),journal_title),
source_url=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(source_url),source_url),
authors_json=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(authors_json),authors_json),
affiliation_info=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(affiliation_info),affiliation_info),
keyword_list=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(keyword_list),keyword_list),
mesh_headings_json=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(mesh_headings_json),mesh_headings_json),
grants_json=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(grants_json),grants_json),
cncb_banks_json=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(cncb_banks_json),cncb_banks_json),
raw_payload=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(raw_payload),raw_payload),
source_hash=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(source_hash),source_hash),
fetched_at=IF(fetched_at IS NULL OR fetched_at<VALUES(fetched_at),VALUES(fetched_at),fetched_at);
COMMIT;

-- 验证：remaining必须为0，确认后再执行最后的DROP。
SELECT COUNT(*) remaining FROM openlb_route_pending p
LEFT JOIN openlb_publication o ON o.pmid=p.pmid WHERE o.pmid IS NULL;

-- remaining为0后执行：
-- DROP TABLE openlb_route_pending;
