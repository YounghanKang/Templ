import re

app_file = r'c:\Users\hjho1\Desktop\templ\Templ\frontend\src\App.tsx'

with open(app_file, 'r', encoding='utf-8') as f:
    content = f.read()

components = [
    "RoadmapNode",
    "RoadmapCanvas",
    "TeamMissionInput",
    "TeamInfoTab",
    "TeamIntegrations"
]

for comp in components:
    # Look for function ComponentName(...
    # and insert const { t } = useTranslation() if not already there
    pattern = r'(function ' + comp + r'\([^)]+\)\s*\{)'
    
    match = re.search(pattern, content)
    if match:
        block_start = match.group(1)
        # Check if already has it
        after_block = content[match.end():match.end()+200]
        if 'useTranslation()' not in after_block:
            content = content[:match.start()] + block_start + '\n  const { t } = useTranslation()' + content[match.end():]
    else:
        # try without arguments if no args
        pattern2 = r'(function ' + comp + r'\(\)\s*\{)'
        match2 = re.search(pattern2, content)
        if match2:
            block_start = match2.group(1)
            after_block = content[match2.end():match2.end()+200]
            if 'useTranslation()' not in after_block:
                content = content[:match2.start()] + block_start + '\n  const { t } = useTranslation()' + content[match2.end():]

# Wait, TeamMissionInput might have `onChange: (mission: string) => void` where `)` is inside.
# My regex `[^)]+` fails on that. Let's use a simpler string replace.

replacements = {
    "function RoadmapNode({ node, selected, alert, onSelect, onStartLink }: {": "function RoadmapNode({ node, selected, alert, onSelect, onStartLink }: {\n  const { t } = useTranslation()",
    "function RoadmapCanvas({ nodes, edges, selectedId, editing, linkFrom, onSelect, onMove, onDeleteNode, onStartLink, onDeleteEdge }: {": "function RoadmapCanvas({ nodes, edges, selectedId, editing, linkFrom, onSelect, onMove, onDeleteNode, onStartLink, onDeleteEdge }: {\n  const { t } = useTranslation()",
    "function TeamMissionInput({ team, onSave }: { team: TeamType; onSave: (mission: string) => void }) {": "function TeamMissionInput({ team, onSave }: { team: TeamType; onSave: (mission: string) => void }) {\n  const { t } = useTranslation()",
    "function TeamInfoTab({ team, onSave }: { team: TeamType; onSave: (patch: Partial<TeamType>) => void }) {": "function TeamInfoTab({ team, onSave }: { team: TeamType; onSave: (patch: Partial<TeamType>) => void }) {\n  const { t } = useTranslation()",
    "function TeamIntegrations({ team }: { team: TeamType }) {": "function TeamIntegrations({ team }: { team: TeamType }) {\n  const { t } = useTranslation()"
}

for k, v in replacements.items():
    if k in content:
        # Check if t is already added
        idx = content.find(k)
        if 'const { t } = useTranslation()' not in content[idx:idx+200]:
            content = content.replace(k, v)

with open(app_file, 'w', encoding='utf-8') as f:
    f.write(content)
