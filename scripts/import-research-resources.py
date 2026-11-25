#!/usr/bin/env python3
"""将原库资源CSV/JSONL/JSON数组分批导入本地接口；不请求原库，不执行DDL，不推断PMID。"""
import argparse, csv, hashlib, json, re, sys, urllib.request, urllib.parse
from pathlib import Path

def value(row, path):
    current = row
    for part in path.split('.'):
        if not isinstance(current, dict) or part not in current:
            return None
        current = current[part]
    return current

def parse_json(value):
    if isinstance(value, str):
        try:
            return json.loads(value)
        except ValueError:
            return value
    return value

def boolean(value):
    if isinstance(value, bool):
        return value
    if value in (1, '1', 'true', 'TRUE'):
        return True
    if value in (0, '0', 'false', 'FALSE'):
        return False
    raise ValueError('hasPublication必须明确为true/false，不能因为没有PMID就推断没有文章')

def rows(path, format):
    with path.open(encoding='utf-8-sig', newline='') as stream:
        if format == 'csv':
            yield from csv.DictReader(stream)
        elif format == 'jsonl':
            for number, line in enumerate(stream, 1):
                if line.strip():
                    record = json.loads(line)
                    if not isinstance(record, dict):
                        raise ValueError(f'第{number}行不是资源对象')
                    yield record
        else:
            # 原生JSON数组逐条解码，缓冲区最多16MB，不将全文件载入内存。
            decoder, buffer, started, ended = json.JSONDecoder(), '', False, False
            while not ended:
                chunk = stream.read(65536)
                buffer += chunk
                if len(buffer) > 16 * 1024 * 1024:
                    raise ValueError('单条资源超过16MB，或输入不是JSON数组；请检查导出结构')
                while True:
                    buffer = buffer.lstrip()
                    if not started:
                        if not buffer:
                            break
                        if not buffer.startswith('['):
                            raise ValueError('JSON模式要求顶层数组；嵌套接口请先导出data数组或JSONL')
                        buffer, started = buffer[1:], True
                        continue
                    if buffer.startswith(','):
                        buffer = buffer[1:]
                        continue
                    if buffer.startswith(']'):
                        ended = True
                        break
                    try:
                        record, end = decoder.raw_decode(buffer)
                    except json.JSONDecodeError:
                        break
                    if not isinstance(record, dict):
                        raise ValueError('数组元素不是资源对象')
                    yield record
                    buffer = buffer[end:]
                if not chunk and not ended:
                    raise ValueError('JSON数组不完整')

def normalized(row, args):
    accession = value(row, args.accession_field)
    if accession is None or not str(accession).strip():
        raise ValueError('缺少原库资源编号，禁止使用标题或PMID代替资源编号')
    accession = str(accession).strip()
    if args.has_publication_field:
        present = boolean(value(row, args.has_publication_field))
    else:
        publications = parse_json(value(row, args.publications_field))
        if not isinstance(publications, list):
            raise ValueError('出版信息字段必须是明确的数组，不能将字段缺失当成无文章')
        present = bool(publications)
    url = value(row, args.url_field)
    if not url and args.url_template:
        url = args.url_template.format(accession=urllib.parse.quote(accession, safe=''))
    if not url:
        raise ValueError('缺少原库详情地址；请指定--url-field或--url-template')
    raw_year = str(value(row, args.year_field) or '')
    year = int(raw_year[:4]) if re.match(r'^\d{4}(?:$|[-/])', raw_year) else None
    authors = parse_json(value(row, args.authors_field))
    grants = parse_json(value(row, args.grants_field))
    if isinstance(grants, dict):
        grants = [grants]
    if not isinstance(grants, list):
        grants = []
    return {
        'databaseCode': args.database.upper(), 'accession': accession,
        'hasPublication': present, 'primaryRecord': not args.related_only,
        'title': str(value(row, args.title_field) or accession), 'sourceUrl': str(url),
        'releaseYear': year, 'affiliationInfo': value(row, args.organization_field),
        'authorsJson': json.dumps(authors if isinstance(authors, list) else [], ensure_ascii=False),
        'grantsJson': json.dumps(grants, ensure_ascii=False),
        'rawPayload': json.dumps(row, ensure_ascii=False)
    }

def post(base, batch):
    payload = json.dumps(batch, ensure_ascii=False).encode('utf-8')
    request = urllib.request.Request(base.rstrip('/') + '/api/collection/resources/batch', payload,
                                     {'Content-Type': 'application/json'}, method='POST')
    with urllib.request.urlopen(request, timeout=120) as response:
        result = json.load(response)
    if str(result.get('code')) not in ('OK', '200'):
        raise RuntimeError(result.get('message', '导入失败'))
    return result

def main(argv=None):
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument('--input', type=Path, required=True)
    parser.add_argument('--database', required=True)
    parser.add_argument('--format', choices=['jsonl', 'json', 'csv'], default='jsonl')
    parser.add_argument('--base-url', default='http://127.0.0.1:8080')
    parser.add_argument('--accession-field', default='accession')
    parser.add_argument('--title-field', default='title')
    parser.add_argument('--url-field', default='sourceUrl')
    parser.add_argument('--url-template')
    group = parser.add_mutually_exclusive_group(required=True)
    group.add_argument('--has-publication-field')
    group.add_argument('--publications-field')
    parser.add_argument('--year-field', default='releaseDate')
    parser.add_argument('--organization-field', default='organization')
    parser.add_argument('--authors-field', default='authors')
    parser.add_argument('--grants-field', default='grants')
    parser.add_argument('--batch-size', type=int, default=100)
    parser.add_argument('--related-only', action='store_true')
    parser.add_argument('--resume', action='store_true')
    parser.add_argument('--checkpoint', type=Path)
    parser.add_argument('--dry-run', action='store_true')
    args = parser.parse_args(argv)
    if not 1 <= args.batch_size <= 200:
        parser.error('batch-size应为1..200')
    args.input = args.input.resolve()
    stat = args.input.stat()
    config = vars(args).copy()
    for field in ['resume', 'dry_run', 'checkpoint']:
        config.pop(field)
    config = json.dumps(config, sort_keys=True, default=str)
    signature = hashlib.sha256(f'{stat.st_size}:{stat.st_mtime_ns}:{config}'.encode()).hexdigest()
    checkpoint = args.checkpoint or args.input.with_suffix(args.input.suffix + '.resources-progress.json')
    after = 0
    if args.resume and checkpoint.exists():
        saved = json.loads(checkpoint.read_text(encoding='utf-8'))
        if saved['signature'] != signature:
            raise ValueError('源文件或字段映射已改变，不能套用旧断点')
        after = int(saved['processed'])
    batch, processed = [], after
    for index, row in enumerate(rows(args.input, args.format), 1):
        if index <= after:
            continue
        batch.append(normalized(row, args))
        processed = index
        if len(batch) == args.batch_size:
            if not args.dry_run:
                post(args.base_url, batch)
                checkpoint.write_text(json.dumps({'signature': signature, 'processed': processed}), encoding='utf-8')
            batch.clear()
    if batch and not args.dry_run:
        post(args.base_url, batch)
        checkpoint.write_text(json.dumps({'signature': signature, 'processed': processed}), encoding='utf-8')
    print(f'{"检查" if args.dry_run else "已提交"} {processed - after} 条资源观察；累计断点 {processed}。有原生文献的资源不新增资源项，原文献请走该库主采集。')
    return processed

if __name__ == '__main__':
    try:
        main()
    except Exception as error:
        print(f'停止：{error}；已提交批次的断点保留，可使用--resume续做。', file=sys.stderr)
        sys.exit(1)
