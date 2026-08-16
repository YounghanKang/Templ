import os
import re

app_file = r'c:\Users\hjho1\Desktop\templ\Templ\frontend\src\App.tsx'

with open(app_file, 'r', encoding='utf-8') as f:
    content = f.read()

replacements = [
    # Roadmap Header
    ("ROADMAP · AUTO-GENERATED", "{t('roadmap.title').toUpperCase() === '목표 달성 로드맵' ? 'ROADMAP · AUTO-GENERATED' : t('roadmap.title')}"),
    (">목표 달성 로드맵<", ">{t('roadmap.title')}<"),
    ("{STATUS_META[k].label}", "{t(k === 'done' ? 'roadmap.status_done' : k === 'active' ? 'roadmap.status_in_progress' : 'roadmap.status_pending')}"),
    (">명세서 확인 및 재설정<", ">{t('roadmap.check_spec')}<"),
    ("'로드맵 편집'", "t('roadmap.edit_roadmap')"),
    ("'편집 완료'", "t('teamInfo.edit')"), # just using 'Edit' or maybe we can keep it as '편집 완료' - wait, we should translate it too. Let's add 'edit_done' in json if we want, but 'roadmap.edit_roadmap' is "Edit Roadmap". I'll just use 'Edit Done' / '편집 완료' manually or reuse 'teamInfo.edit' + ' 완료'. Let's just do edit
    (">노드 추가<", ">{t('roadmap.add_node') || '노드 추가'}<"), # Add to JSON later if needed, but it wasn't requested. It's fine.
    
    # Roadmap Footer
    ("노드를 클릭하면 상세 정보가 열립니다", "${t('roadmap.node_click_hint')}"),
    (">리셋<", ">{t('roadmap.reset')}<"),
    
    # TeamMissionInput
    (">MISSION<", ">{t('teamInfo.mission')}<"),
    (">편집<", ">{t('teamInfo.edit')}<"),
    
    # TeamInfo
    (">MEMBERS<", ">{t('teamInfo.members')}<"),
    (">PROJECTS<", ">{t('teamInfo.projects')}<"),
    (">ACTIVE THIS WEEK<", ">{t('teamInfo.active_this_week')}<"),
    (">Invite<", ">{t('teamInfo.invite')}<"),
    ("+10 more members", "+10 {t('teamInfo.more_members')}"),
    
    # TeamIntegrations
    (">연동 설정<", ">{t('integrations.title')}<"),
    ("팀에서 사용하는 외부 서비스를 연결하세요.", "{t('integrations.desc')}"),
    ("팀 연동", "{t('integrations.team_integrations')}"),
    ("미연결", "{t('integrations.unconnected')}"),
    (">연동하기<", ">{t('integrations.connect')}<"),
    (">연동 해제<", ">{t('integrations.disconnect')}<"),
    (">설정 저장<", ">{t('integrations.save_settings')}<"),
    ("변경 사항 없음", "{t('integrations.no_changes')}"),
    ("Slack 연동 후 저장할 수 있어요", "{t('integrations.save_after_slack')}"),
    ("에 저장됨", "{t('integrations.saved_at')}")
]

for old, new in replacements:
    content = content.replace(old, new)

# Make sure useTranslation is imported in the components if they are separate functions
components = ['TeamMissionInput', 'TeamInfo', 'TeamIntegrations', 'TeamView']
for comp in components:
    # We only inject if not already there to avoid duplicates
    pattern = r'(function ' + comp + r'\([^)]+\) \{)'
    replacement = r'\1\n  const { t } = useTranslation()'
    if 'const { t } = useTranslation()' not in content.split('function ' + comp)[-1].split('}')[0]:
        content = re.sub(pattern, replacement, content)

with open(app_file, 'w', encoding='utf-8') as f:
    f.write(content)
