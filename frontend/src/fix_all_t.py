import re

app_file = r'c:\Users\hjho1\Desktop\templ\Templ\frontend\src\App.tsx'

with open(app_file, 'r', encoding='utf-8') as f:
    content = f.read()

# We need to inject `const { t } = useTranslation()` exactly at the start of the function body for:
components = [
    "RoadmapNode",
    "ChipEditor",
    "NodeDetailPanel",
    "RoadmapCanvas",
    "TeamMissionInput",
    "TeamInfoTab",
    "TeamIntegrations",
    "PersonalSettings"
]

for comp in components:
    # Match the function declaration, accounting for multi-line props/type definitions
    # example: function NodeDetailPanel({ ... }: { \n ... \n }) {
    # We find `function CompName(` and then find the matching closing `) {` or `}) {`
    # A robust regex for this specific file format:
    pattern = r'(function ' + comp + r'\b[^\{]*\{[\s\S]*?\n\}\) \{)'
    match = re.search(pattern, content)
    
    if match:
        block_start = match.group(1)
        after_block = content[match.end():match.end()+200]
        if 'const { t } = useTranslation()' not in after_block:
            content = content[:match.start()] + block_start + '\n  const { t } = useTranslation()' + content[match.end():]
    else:
        # Maybe it's a simple function Comp(...) {
        pattern2 = r'(function ' + comp + r'\b[^{]+ \{)'
        match2 = re.search(pattern2, content)
        if match2:
            block_start = match2.group(1)
            after_block = content[match2.end():match2.end()+200]
            if 'const { t } = useTranslation()' not in after_block:
                content = content[:match2.start()] + block_start + '\n  const { t } = useTranslation()' + content[match2.end():]

# Let's fix the PersonalSettings issue. Did I inject t into PersonalSettings?
# I will do it just in case.

with open(app_file, 'w', encoding='utf-8') as f:
    f.write(content)
