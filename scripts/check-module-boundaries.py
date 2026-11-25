"""Check task module ownership without connecting to a database or network."""
from pathlib import Path
import re

ROOT = Path(__file__).resolve().parents[1]
JAVA = ROOT / 'portal-backend/src/main/java/com/example/bioplatform'
FRONTEND = ROOT / 'portal-frontend/src/modules'
errors = []
for task, other in [('catalog', 'research'), ('research', 'catalog')]:
    for path in (JAVA / task).rglob('*.java'):
        source = path.read_text(encoding='utf-8')
        if re.search(r'\bimport\s+(?:static\s+)?com\.example\.bioplatform\.' + other + r'\.', source):
            errors.append(f'{path.relative_to(ROOT)} imports {other}')
        expected = 'com.example.bioplatform.' + task + '.' + '.'.join(path.relative_to(JAVA / task).parent.parts)
        if not re.search(r'\bpackage\s+' + re.escape(expected) + r'\s*;', source):
            errors.append(f'{path.relative_to(ROOT)} has an inconsistent package')
    for path in (FRONTEND / task).rglob('*'):
        if path.suffix not in {'.vue', '.js', '.mjs'}:
            continue
        for spec in re.findall(r'(?:from\s*|import\s*\(\s*)[\"\']([^\"\']+)', path.read_text(encoding='utf-8')):
            if spec.startswith('.') and (FRONTEND / other).resolve() in (path.parent / spec).resolve().parents:
                errors.append(f'{path.relative_to(ROOT)} imports {other}')
for path in (ROOT / 'portal-backend/src/main/resources/mapper').rglob('*.xml'):
    task = path.parent.name
    if task not in {'catalog', 'research'}:
        errors.append(f'{path.relative_to(ROOT)} is outside a task mapper directory')
    elif f'namespace="com.example.bioplatform.{task}.' not in path.read_text(encoding='utf-8'):
        errors.append(f'{path.relative_to(ROOT)} has an inconsistent mapper namespace')
for path in JAVA.rglob('*Mapper.java'):
    if re.search(r'@(Select|Insert|Update|Delete)\s*\(', path.read_text(encoding='utf-8')):
        errors.append(f'{path.relative_to(ROOT)} contains SQL annotations')
if errors:
    raise SystemExit('\n'.join(errors))
print('Module boundaries OK: catalog and research do not import each other; Java packages and mapper namespaces agree.')
