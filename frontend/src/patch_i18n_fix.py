import os

app_file = r'c:\Users\hjho1\Desktop\templ\Templ\frontend\src\App.tsx'

with open(app_file, 'r', encoding='utf-8') as f:
    content = f.read()

replacements = [
    # Fix the quote issue for label()
    ("label('{t('integrations.team_integrations')}'", "label(t('integrations.team_integrations')"),
    # Fix anywhere else where I might have put '{t(...)}' inside JS code instead of JSX
    ("'{t('roadmap.edit_roadmap')}'", "t('roadmap.edit_roadmap')"),
    ("'{t('teamInfo.edit')}'", "t('teamInfo.edit')")
]

for old, new in replacements:
    content = content.replace(old, new)

with open(app_file, 'w', encoding='utf-8') as f:
    f.write(content)
