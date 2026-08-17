import os
import json
import re

app_file = r'c:\Users\hjho1\Desktop\templ\Templ\frontend\src\App.tsx'

ko_path = r'c:\Users\hjho1\Desktop\templ\Templ\frontend\src\locales\ko\translation.json'
en_path = r'c:\Users\hjho1\Desktop\templ\Templ\frontend\src\locales\en\translation.json'
zh_path = r'c:\Users\hjho1\Desktop\templ\Templ\frontend\src\locales\zh\translation.json'

def update_json(path, new_data):
    with open(path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    for key, val in new_data.items():
        if key not in data:
            data[key] = val
        else:
            data[key].update(val)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

translations = {
    'roadmap': {
        'error_occurred': '문제가 발생했습니다',
        'close': '닫기',
        'view_node': '해당 노드 보기'
    }
}
en_translations = {
    'roadmap': {
        'error_occurred': 'An error occurred',
        'close': 'Close',
        'view_node': 'View Node'
    }
}
zh_translations = {
    'roadmap': {
        'error_occurred': '发生了问题',
        'close': '关闭',
        'view_node': '查看节点'
    }
}

update_json(ko_path, translations)
update_json(en_path, en_translations)
update_json(zh_path, zh_translations)

with open(app_file, 'r', encoding='utf-8') as f:
    content = f.read()

replacements = [
    (">문제가 발생했습니다<", ">{t('roadmap.error_occurred')}<"),
    (">닫기<", ">{t('roadmap.close')}<"),
    (">해당 노드 보기<", ">{t('roadmap.view_node')}<")
]
for old, new in replacements:
    content = content.replace(old, new)

# inject useTranslation into RoadmapCanvas
if 'const { t } = useTranslation()' not in content.split('function RoadmapCanvas')[1].split('}')[0]:
    content = re.sub(r'(function RoadmapCanvas\([^)]+\) \{)', r'\1\n  const { t } = useTranslation()', content)

with open(app_file, 'w', encoding='utf-8') as f:
    f.write(content)
