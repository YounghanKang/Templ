import re

app_file = r'c:\Users\hjho1\Desktop\templ\Templ\frontend\src\App.tsx'

with open(app_file, 'r', encoding='utf-8') as f:
    content = f.read()

# I need to find `const { t } = useTranslation()` inside type definitions and remove it.
# Then insert it right after `}) {\n` or `) {\n`.

# Remove all bad insertions:
content = content.replace("}: {\n  const { t } = useTranslation()", "}: {")
content = content.replace("}) {\n  const { t } = useTranslation()", "}) {")

# Clean up any leftover inside `RoadmapNode` etc.
# Actually let's just make sure we remove the EXACT bad injections I did earlier.
bad_injections = [
    "function RoadmapNode({ node, selected, alert, onSelect, onStartLink }: {\n  const { t } = useTranslation()",
    "function RoadmapCanvas({ nodes, edges, selectedId, editing, linkFrom, onSelect, onMove, onDeleteNode, onStartLink, onDeleteEdge }: {\n  const { t } = useTranslation()",
    "function TeamMissionInput({ team, onSave }: { team: TeamType; onSave: (mission: string) => void }) {\n  const { t } = useTranslation()",
    "function TeamInfoTab({ team, onSave }: { team: TeamType; onSave: (patch: Partial<TeamType>) => void }) {\n  const { t } = useTranslation()",
    "function TeamIntegrations({ team }: { team: TeamType }) {\n  const { t } = useTranslation()"
]

for bad in bad_injections:
    good = bad.replace("\n  const { t } = useTranslation()", "")
    content = content.replace(bad, good)


# Now properly insert `const { t } = useTranslation()` at the top of the function BODY.

# 1. RoadmapNode
match = re.search(r'(function RoadmapNode\([^)]+\) \{\n)', content)
# wait, RoadmapNode has a multi-line type definition:
# function RoadmapNode({ node, selected, alert, onSelect, onStartLink }: {
#   node: RNode; selected: boolean; alert: 'warn' | 'issue' | null
#   onSelect: (id: string) => void; onStartLink: (id: string) => void
# }) {
content = re.sub(
    r'(function RoadmapNode\([^)]+\) \{[\s\S]*?\n\}\) \{)',
    r'\1\n  const { t } = useTranslation()',
    content
)

# 2. RoadmapCanvas
# function RoadmapCanvas({ nodes, edges, selectedId, editing, linkFrom, onSelect, onMove, onDeleteNode, onStartLink, onDeleteEdge }: {
#   nodes: RNode[]; edges: REdge[]; selectedId: string | null; editing: boolean; linkFrom: string | null
#   onSelect: (id: string) => void; onMove: (id: string, x: number, y: number) => void
#   onDeleteNode: (id: string) => void; onStartLink: (id: string) => void; onDeleteEdge: (id: string) => void
# }) {
content = re.sub(
    r'(function RoadmapCanvas\([^)]+\) \{[\s\S]*?\n\}\) \{)',
    r'\1\n  const { t } = useTranslation()',
    content
)

# 3. TeamMissionInput
content = re.sub(
    r'(function TeamMissionInput\([^)]+\) \{)',
    r'\1\n  const { t } = useTranslation()',
    content
)

# 4. TeamInfoTab
content = re.sub(
    r'(function TeamInfoTab\([^)]+\) \{)',
    r'\1\n  const { t } = useTranslation()',
    content
)

# 5. TeamIntegrations
content = re.sub(
    r'(function TeamIntegrations\([^)]+\) \{)',
    r'\1\n  const { t } = useTranslation()',
    content
)

with open(app_file, 'w', encoding='utf-8') as f:
    f.write(content)
