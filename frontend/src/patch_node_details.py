import os
import json
import re

app_file = r'c:\Users\hjho1\Desktop\templ\Templ\frontend\src\App.tsx'
locales = ['ko', 'en', 'zh']

translations = {
    'ko': {
        'node': {
            'add': '추가',
            'justNow': '방금 전',
            'info': '정보',
            'comments': '댓글',
            'commentPlaceholder': '댓글 작성… (Enter로 등록)',
            'issueOccurred': '문제 발생',
            'resolved': '해결됨',
            'issuePlaceholder': '어떤 문제가 발생했는지 적어주세요',
            'noFiles': '제출된 파일이 없습니다',
            'submitFile': '+ 파일 제출',
            'done_checked': '✓ 완료',
            'markAsIssue': '! 문제 발생으로 표시',
            'objectivePlaceholder': '이 작업이 달성해야 하는 것',
            'memberPlaceholder': '멤버 이름 입력 후 Enter',
            'noPrerequisites': '선행 작업 없음 · 바로 시작 가능',
            'prerequisitesPlaceholder': '선행 작업 입력 후 Enter',
            'aiSummaryPlaceholder': 'AI 요약이 여기에 생성됩니다',
            'deleteFile': '파일 삭제',
            'files': '제출된 파일',
            'status': '진행 상태',
            'objective': '목표',
            'deadline': '시간 제한',
            'members': '진행 멤버',
            'prerequisites': '전 단계 할 일',
            'aiSummary': 'AI 요약',
            'todo': '대기',
            'active': '진행중',
            'done': '완료'
        },
        'roadmap': {
            'canvas_help_edit': '노드를 드래그해 이동 · 아래 화살표 버튼으로 연결 시작 · × 로 노드 삭제 · 화살표를 클릭하면 삭제',
            'delete_node': '노드 삭제',
            'link_node': '링크 연결'
        }
    },
    'en': {
        'node': {
            'add': 'Add',
            'justNow': 'Just now',
            'info': 'Info',
            'comments': 'Comments',
            'commentPlaceholder': 'Write a comment... (Enter to post)',
            'issueOccurred': 'Issue Occurred',
            'resolved': 'Resolved',
            'issuePlaceholder': 'Describe the issue here',
            'noFiles': 'No files submitted',
            'submitFile': '+ Submit File',
            'done_checked': '✓ Done',
            'markAsIssue': '! Mark as Issue',
            'objectivePlaceholder': 'What this task aims to achieve',
            'memberPlaceholder': 'Enter member name and press Enter',
            'noPrerequisites': 'No prerequisites · Can start immediately',
            'prerequisitesPlaceholder': 'Enter prerequisite and press Enter',
            'aiSummaryPlaceholder': 'AI summary will be generated here',
            'deleteFile': 'Delete file',
            'files': 'FILES',
            'status': 'STATUS',
            'objective': 'OBJECTIVE',
            'deadline': 'DEADLINE',
            'members': 'MEMBERS',
            'prerequisites': 'PREREQUISITES',
            'aiSummary': 'AI SUMMARY',
            'todo': 'To Do',
            'active': 'Active',
            'done': 'Done'
        },
        'roadmap': {
            'canvas_help_edit': 'Drag to move · Click arrow to link · Click × to delete node · Click arrow to delete link',
            'delete_node': 'Delete Node',
            'link_node': 'Link Node'
        }
    },
    'zh': {
        'node': {
            'add': '添加',
            'justNow': '刚刚',
            'info': '信息',
            'comments': '评论',
            'commentPlaceholder': '写评论... (按Enter发表)',
            'issueOccurred': '发生问题',
            'resolved': '已解决',
            'issuePlaceholder': '请在此描述问题',
            'noFiles': '未提交文件',
            'submitFile': '+ 提交文件',
            'done_checked': '✓ 完成',
            'markAsIssue': '! 标记为问题',
            'objectivePlaceholder': '此任务旨在实现什么',
            'memberPlaceholder': '输入成员名称并按Enter',
            'noPrerequisites': '无先决条件 · 可立即开始',
            'prerequisitesPlaceholder': '输入先决条件并按Enter',
            'aiSummaryPlaceholder': 'AI摘要将在此生成',
            'deleteFile': '删除文件',
            'files': '相关文件',
            'status': '进度状况',
            'objective': '目标',
            'deadline': '截止时间',
            'members': '成员',
            'prerequisites': '前置任务',
            'aiSummary': 'AI摘要',
            'todo': '待办',
            'active': '进行中',
            'done': '已完成'
        },
        'roadmap': {
            'canvas_help_edit': '拖动以移动 · 点击箭头以链接 · 点击×删除节点 · 点击箭头删除链接',
            'delete_node': '删除节点',
            'link_node': '链接节点'
        }
    }
}

for lang in locales:
    path = f'c:\\Users\\hjho1\\Desktop\\templ\\Templ\\frontend\\src\\locales\\{lang}\\translation.json'
    with open(path, 'r', encoding='utf-8') as f:
        data = json.load(f)
    for section, kv in translations[lang].items():
        if section not in data:
            data[section] = {}
        data[section].update(kv)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(data, f, ensure_ascii=False, indent=2)

with open(app_file, 'r', encoding='utf-8') as f:
    content = f.read()

# Replace ChipEditor
if 'function ChipEditor(' in content:
    content = re.sub(r'(function ChipEditor\([^)]+\) \{)', r'\1\n  const { t } = useTranslation()', content)
    content = content.replace('>추가<', '>{t("node.add")}<')

# Replace NodeDetailPanel
if 'function NodeDetailPanel(' in content:
    content = re.sub(r'(function NodeDetailPanel\([^)]+\) \{)', r'\1\n  const { t } = useTranslation()', content)
    
    # Simple strings
    reps = {
        "'방금 전'": "t('node.justNow')",
        "('정보')": "(t('node.info'))",
        "('댓글')": "(t('node.comments'))",
        "placeholder=\"댓글 작성… (Enter로 등록)\"": "placeholder={t('node.commentPlaceholder')}",
        ">문제 발생<": ">{t('node.issueOccurred')}<",
        ">해결됨<": ">{t('node.resolved')}<",
        "placeholder=\"어떤 문제가 발생했는지 적어주세요\"": "placeholder={t('node.issuePlaceholder')}",
        ">제출된 파일이 없습니다<": ">{t('node.noFiles')}<",
        "title=\"파일 삭제\"": "title={t('node.deleteFile')}",
        ">+ 파일 제출<": ">{t('node.submitFile')}<",
        ">✓ 완료<": ">{t('node.done_checked')}<",
        ">! 문제 발생으로 표시<": ">{t('node.markAsIssue')}<",
        "placeholder=\"이 작업이 달성해야 하는 것\"": "placeholder={t('node.objectivePlaceholder')}",
        "placeholder=\"멤버 이름 입력 후 Enter\"": "placeholder={t('node.memberPlaceholder')}",
        ">선행 작업 없음 · 바로 시작 가능<": ">{t('node.noPrerequisites')}<",
        "placeholder=\"선행 작업 입력 후 Enter\"": "placeholder={t('node.prerequisitesPlaceholder')}",
        "placeholder=\"AI 요약이 여기에 생성됩니다\"": "placeholder={t('node.aiSummaryPlaceholder')}",
        
        # Sections
        "section('제출된 파일', 'Files')": "section(t('node.files'), 'FILES')",
        "section('진행 상태', 'Status')": "section(t('node.status'), 'STATUS')",
        "section('목표', 'Objective')": "section(t('node.objective'), 'OBJECTIVE')",
        "section('시간 제한', 'Deadline')": "section(t('node.deadline'), 'DEADLINE')",
        "section('진행 멤버', 'Members')": "section(t('node.members'), 'MEMBERS')",
        "section('전 단계 할 일', 'Prerequisites')": "section(t('node.prerequisites'), 'PREREQUISITES')",
        "section('AI 요약', 'AI Summary')": "section(t('node.aiSummary'), 'AI SUMMARY')",
        
        # "댓글" with length
        "댓글{comments.length > 0 ? ` ${comments.length}` : ''}": "{t('node.comments')}{comments.length > 0 ? ` ${comments.length}` : ''}"
    }
    for old, new in reps.items():
        content = content.replace(old, new)
        
    # For status label: {k === 'done' ? '✓ 완료' : m.label}
    content = content.replace("{k === 'done' ? t('node.done_checked') : m.label}", "{k === 'done' ? t('node.done_checked') : t(`node.${k}`)}")
    # Wait, the original was {k === 'done' ? '✓ 완료' : m.label}. Let me replace the intermediate state.
    content = content.replace("{k === 'done' ? '✓ 완료' : m.label}", "{k === 'done' ? t('node.done_checked') : t(`node.${k}`)}")

# RoadmapCanvas 
content = content.replace(
    "'노드를 드래그해 이동 · 아래 화살표 버튼으로 연결 시작 · × 로 노드 삭제 · 화살표를 클릭하면 삭제'",
    "t('roadmap.canvas_help_edit')"
)
content = content.replace('title="노드 삭제"', 'title={t("roadmap.delete_node")}')
content = content.replace('title="링크 연결"', 'title={t("roadmap.link_node")}')

with open(app_file, 'w', encoding='utf-8') as f:
    f.write(content)
