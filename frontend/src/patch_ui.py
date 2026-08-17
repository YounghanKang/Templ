import sys

def main():
    with open('App.tsx', 'r', encoding='utf-8') as f:
        content = f.read()

    # 1. TeamInfo Members section
    content = content.replace(
        "const allMockMembers = [\n    { name: 'Alex Rivera', role: 'Lead', email: 'alex@acmecorp.io' },",
        "const allMockMembers = [\n    { name: localStorage.getItem('templ_user_nickname') || 'Jordan Kim', role: 'Lead', email: localStorage.getItem('templ_user_email') || 'jordan@acmecorp.io' },"
    )
    
    content = content.replace(
        "<button style={{ padding: '6px 14px', borderRadius: 8, border: '1.5px solid var(--color-border)', background: 'transparent', fontSize: 12, fontWeight: 600, color: 'var(--color-foreground)', cursor: 'pointer', fontFamily: 'var(--font-display)' }}>\n              Invite\n            </button>",
        "<button onClick={() => alert(t('node.issueOccurred') ? '초대 기능은 준비 중입니다.' : 'Invite feature is coming soon.')} style={{ padding: '6px 14px', borderRadius: 8, border: '1.5px solid var(--color-border)', background: 'transparent', fontSize: 12, fontWeight: 600, color: 'var(--color-foreground)', cursor: 'pointer', fontFamily: 'var(--font-display)' }}>\n              Invite\n            </button>"
    )

    # 2. NodeDetailOverlay Translations
    content = content.replace(
        "해결됨\n              </button>",
        "{t('node.resolved')}\n              </button>"
    )
    
    content = content.replace(
        "! 문제 발생으로 표시\n            </button>",
        "{t('node.markAsIssue')}\n            </button>"
    )

    content = content.replace(
        "<span style={{ fontFamily: 'var(--font-display)', fontSize: 12.5, fontWeight: 700, color: '#b91c1c' }}>\n              문제 발생\n            </span>",
        "<span style={{ fontFamily: 'var(--font-display)', fontSize: 12.5, fontWeight: 700, color: '#b91c1c' }}>\n              {t('node.issueOccurred')}\n            </span>"
    )

    content = content.replace(
        "선행 작업 없음 · 바로 시작 가능\n            </div>",
        "{t('node.noPrerequisites')}\n            </div>"
    )

    # Status badges
    content = content.replace(
        "<span style={{ width: 5, height: 5, borderRadius: '50%', background: st.color }} />\n              {st.label}\n            </span>",
        "<span style={{ width: 5, height: 5, borderRadius: '50%', background: st.color }} />\n              {t('node.' + node.status)}\n            </span>"
    )

    with open('App.tsx', 'w', encoding='utf-8') as f:
        f.write(content)

if __name__ == '__main__':
    main()
