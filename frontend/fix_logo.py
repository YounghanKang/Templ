import re
content = open('src/App.tsx', encoding='utf-8').read()
content = re.sub(
    r'<div style=\{\{ display: \'flex\', alignItems: \'center\', gap: 10 \}\}>\s*<img src=\"/favicon\.png\" alt=\"Orchestree\"[^>]*>\s*<span[^>]*>\s*Orchestree\s*</span>\s*</div>',
    r'<div style={{ display: \'flex\', alignItems: \'center\' }}>\n              <img src="/logo.png" alt="Orchestree" style={{ width: 140, objectFit: \'contain\', flexShrink: 0 }} />\n            </div>',
    content
)
open('src/App.tsx', 'w', encoding='utf-8').write(content)
