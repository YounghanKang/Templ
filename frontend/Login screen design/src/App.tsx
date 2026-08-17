import { useState, useRef, useEffect } from 'react'

const BG_PEOPLE = 'https://images.unsplash.com/photo-1758272133771-b149318883c5?w=1800&h=1200&fit=crop&auto=format'
const BG_GLOBE  = 'https://images.unsplash.com/photo-1684610529682-553625a1ffed?w=1800&h=1200&fit=crop&auto=format'

type Lang = 'ko' | 'en' | 'zh'
type Page = 'login' | 'signup'
type Status = 'idle' | 'sent' | 'verified' | 'error'

const LANGS: { code: Lang; label: string; flag: string; font: string }[] = [
  { code: 'ko', label: '한국어', flag: '🇰🇷', font: "'Noto Sans KR', sans-serif" },
  { code: 'en', label: 'English', flag: '🇺🇸', font: "'Noto Sans', sans-serif" },
  { code: 'zh', label: '中文',    flag: '🇨🇳', font: "'Noto Sans SC', sans-serif" },
]

const JOBS: Record<Lang, string[]> = {
  ko: ['선택하세요', '개발자 / IT', '디자이너', '마케터', '학생', '교육자', '의료 / 헬스케어', '금융 / 회계', '영업', '창업가', '기타'],
  en: ['Select...', 'Developer / IT', 'Designer', 'Marketer', 'Student', 'Educator', 'Healthcare', 'Finance / Accounting', 'Sales', 'Entrepreneur', 'Other'],
  zh: ['请选择', '开发者 / IT', '设计师', '市场营销', '学生', '教育者', '医疗 / 健康', '金融 / 会计', '销售', '创业者', '其他'],
}

const T: Record<Lang, Record<string, string>> = {
  ko: {
    welcome: 'Welcome back', subtitle: '서비스를 계속 이용하려면\n계정에 로그인하세요.',
    pageTitle: '계정 로그인', greeting: '다시 만나서 반가워요', mobileTitle: '로그인',
    idLabel: '아이디', idPlaceholder: '아이디를 입력하세요',
    pwLabel: '비밀번호', pwPlaceholder: '비밀번호를 입력하세요',
    login: '로그인', or: '또는', google: 'Google로 로그인',
    signup: '회원가입', forgot: '아이디 · 비밀번호 찾기',
    signupWelcome: 'Join us', signupSubtitle: '지금 가입하고\n서비스를 시작해보세요.',
    signupTitle: '새 계정 만들기', signupGreeting: '함께해서 반가워요',
    emailLabel: '이메일', emailPlaceholder: '이메일을 입력하세요',
    nicknameLabel: '닉네임', nicknamePlaceholder: '닉네임을 입력하세요',
    jobLabel: '직업군', signupBtn: '가입하기',
    backToLogin: '이미 계정이 있으신가요?', backLogin: '로그인',
    checkDup: '중복확인', sendCode: '인증 발송',
    codeLabel: '인증 코드', codePlaceholder: '코드를 입력하세요', confirmCode: '확인',
    dupOk: '사용 가능한 아이디입니다', dupFail: '이미 사용 중인 아이디입니다',
    codeSent: '인증 코드가 발송되었습니다', codeOk: '인증 완료', codeFail: '코드가 올바르지 않습니다',
  },
  en: {
    welcome: 'Welcome back', subtitle: 'Sign in to continue\nusing our service.',
    pageTitle: 'Account Login', greeting: 'Good to see you again', mobileTitle: 'Sign In',
    idLabel: 'Username', idPlaceholder: 'Enter your username',
    pwLabel: 'Password', pwPlaceholder: 'Enter your password',
    login: 'Sign In', or: 'or', google: 'Continue with Google',
    signup: 'Sign Up', forgot: 'Forgot ID · Password',
    signupWelcome: 'Join us', signupSubtitle: 'Create an account\nand get started today.',
    signupTitle: 'Create Account', signupGreeting: 'Great to have you here',
    emailLabel: 'Email', emailPlaceholder: 'Enter your email',
    nicknameLabel: 'Nickname', nicknamePlaceholder: 'Enter your nickname',
    jobLabel: 'Occupation', signupBtn: 'Create Account',
    backToLogin: 'Already have an account?', backLogin: 'Sign In',
    checkDup: 'Check', sendCode: 'Send Code',
    codeLabel: 'Verification Code', codePlaceholder: 'Enter code', confirmCode: 'Verify',
    dupOk: 'Username is available', dupFail: 'Username already taken',
    codeSent: 'Verification code sent', codeOk: 'Verified', codeFail: 'Incorrect code',
  },
  zh: {
    welcome: '欢迎回来', subtitle: '请登录您的账户\n继续使用我们的服务。',
    pageTitle: '账户登录', greeting: '很高兴再次见到您', mobileTitle: '登录',
    idLabel: '用户名', idPlaceholder: '请输入用户名',
    pwLabel: '密码', pwPlaceholder: '请输入密码',
    login: '登录', or: '或者', google: '使用 Google 登录',
    signup: '注册账户', forgot: '忘记用户名 · 密码',
    signupWelcome: '加入我们', signupSubtitle: '立即注册\n开始使用我们的服务。',
    signupTitle: '创建新账户', signupGreeting: '很高兴认识您',
    emailLabel: '电子邮件', emailPlaceholder: '请输入电子邮件',
    nicknameLabel: '昵称', nicknamePlaceholder: '请输入昵称',
    jobLabel: '职业', signupBtn: '立即注册',
    backToLogin: '已有账户？', backLogin: '登录',
    checkDup: '检查', sendCode: '发送验证码',
    codeLabel: '验证码', codePlaceholder: '请输入验证码', confirmCode: '确认',
    dupOk: '用户名可用', dupFail: '用户名已被使用',
    codeSent: '验证码已发送', codeOk: '验证成功', codeFail: '验证码不正确',
  },
}

const GoogleIcon = () => (
  <svg width="18" height="18" viewBox="0 0 18 18" fill="none">
    <path d="M17.64 9.205c0-.639-.057-1.252-.164-1.841H9v3.481h4.844a4.14 4.14 0 0 1-1.796 2.716v2.259h2.908c1.702-1.567 2.684-3.875 2.684-6.615Z" fill="#4285F4"/>
    <path d="M9 18c2.43 0 4.467-.806 5.956-2.18l-2.908-2.259c-.806.54-1.837.86-3.048.86-2.344 0-4.328-1.584-5.036-3.711H.957v2.332A8.997 8.997 0 0 0 9 18Z" fill="#34A853"/>
    <path d="M3.964 10.71A5.41 5.41 0 0 1 3.682 9c0-.593.102-1.17.282-1.71V4.958H.957A8.996 8.996 0 0 0 0 9c0 1.452.348 2.827.957 4.042l3.007-2.332Z" fill="#FBBC05"/>
    <path d="M9 3.58c1.321 0 2.508.454 3.44 1.345l2.582-2.58C13.463.891 11.426 0 9 0A8.997 8.997 0 0 0 .957 4.958L3.964 7.29C4.672 5.163 6.656 3.58 9 3.58Z" fill="#EA4335"/>
  </svg>
)

function StatusMsg({ status, okMsg, failMsg, sentMsg, font }: { status: Status; okMsg: string; failMsg: string; sentMsg?: string; font: string }) {
  if (status === 'idle') return null
  const map: Record<Status, { color: string; msg: string }> = {
    idle:     { color: '', msg: '' },
    sent:     { color: '#2d5be3', msg: sentMsg ?? okMsg },
    verified: { color: '#16a34a', msg: okMsg },
    error:    { color: '#dc2626', msg: failMsg },
  }
  const { color, msg } = map[status]
  return (
    <p className="text-[11px] mt-1 flex items-center gap-1" style={{ color, fontFamily: font }}>
      {status === 'verified' && <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M2 6l3 3 5-5" stroke="#16a34a" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>}
      {status === 'error' && <svg width="12" height="12" viewBox="0 0 12 12" fill="none"><circle cx="6" cy="6" r="5" stroke="#dc2626" strokeWidth="1.5"/><path d="M6 4v3M6 8.5v.5" stroke="#dc2626" strokeWidth="1.5" strokeLinecap="round"/></svg>}
      {msg}
    </p>
  )
}

const inputBase = (focused: boolean, font: string): React.CSSProperties => ({
  border: `1.5px solid ${focused ? '#2d5be3' : '#d4cfc6'}`,
  borderRadius: '4px',
  color: '#1a1916',
  fontFamily: font,
  width: '100%',
  padding: '10px 14px',
  fontSize: '14px',
  outline: 'none',
  transition: 'border-color 0.15s',
  backgroundColor: '#fff',
})

const actionBtn = (font: string, disabled = false): React.CSSProperties => ({
  flexShrink: 0,
  padding: '0 12px',
  height: '40px',
  fontSize: '12px',
  fontFamily: font,
  fontWeight: 500,
  borderRadius: '4px',
  border: '1.5px solid #2d5be3',
  background: disabled ? '#e8e4dc' : 'transparent',
  color: disabled ? '#6b6760' : '#2d5be3',
  cursor: disabled ? 'not-allowed' : 'pointer',
  whiteSpace: 'nowrap' as const,
  transition: 'all 0.15s',
})

export default function App() {
  const [lang, setLang] = useState<Lang>('ko')
  const [page, setPage] = useState<Page>('login')
  const [dropOpen, setDropOpen] = useState(false)
  const dropRef = useRef<HTMLDivElement>(null)

  // login
  const [loginId, setLoginId] = useState('')
  const [loginPw, setLoginPw] = useState('')
  const [lf, setLf] = useState({ id: false, pw: false })

  // signup fields
  const [su, setSu] = useState({ id: '', pw: '', email: '', nickname: '', job: '' })
  const [sf, setSf] = useState({ id: false, pw: false, email: false, nickname: false })

  // ID duplicate check
  const [idStatus, setIdStatus] = useState<Status>('idle')

  // Email verification
  const [emailStatus, setEmailStatus] = useState<Status>('idle')
  const [emailCode, setEmailCode] = useState('')
  const [emailCodeFocused, setEmailCodeFocused] = useState(false)

  const t = T[lang]
  const currentLang = LANGS.find(l => l.code === lang)!
  const font = currentLang.font

  // reset verification states on lang change
  useEffect(() => {
    setIdStatus('idle')
    setEmailStatus('idle')
    setEmailCode('')
  }, [lang])

  useEffect(() => {
    const handler = (e: MouseEvent) => {
      if (dropRef.current && !dropRef.current.contains(e.target as Node)) setDropOpen(false)
    }
    document.addEventListener('mousedown', handler)
    return () => document.removeEventListener('mousedown', handler)
  }, [])

  // Simulate ID duplicate check
  const handleCheckId = () => {
    if (!su.id.trim()) return
    // simulate: ids containing "admin" or "test" are taken
    const taken = ['admin', 'test', 'user'].includes(su.id.toLowerCase())
    setIdStatus(taken ? 'error' : 'verified')
  }

  // Simulate sending email code
  const handleSendCode = () => {
    if (!su.email.trim()) return
    setEmailStatus('sent')
    setEmailCode('')
  }

  // Simulate confirming code — "1234" is the magic code
  const handleConfirmCode = () => {
    setEmailStatus(emailCode === '1234' ? 'verified' : 'error')
  }

  const Background = () => (
    <>
      <div className="absolute inset-0" style={{ backgroundImage: `url(${BG_PEOPLE})`, backgroundSize: 'cover', backgroundPosition: 'center top', opacity: 0.35 }} />
      <div className="absolute inset-0" style={{ backgroundImage: `url(${BG_GLOBE})`, backgroundSize: 'cover', backgroundPosition: 'center', opacity: 0.45, mixBlendMode: 'screen' }} />
      <div className="absolute inset-0" style={{ background: 'rgba(255,255,255,0.45)' }} />
    </>
  )

  const LangSwitcher = () => (
    <div className="absolute top-4 right-4 z-20" ref={dropRef}>
      <button onClick={() => setDropOpen(v => !v)}
        className="flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium transition-all hover:bg-white/20"
        style={{ background: 'rgba(255,255,255,0.12)', border: '1px solid rgba(255,255,255,0.25)', color: '#fff', backdropFilter: 'blur(8px)', fontFamily: font }}>
        <span>{currentLang.flag}</span><span>{currentLang.label}</span>
        <svg width="10" height="10" viewBox="0 0 10 10" fill="none" style={{ transform: dropOpen ? 'rotate(180deg)' : 'rotate(0)', transition: 'transform 0.15s' }}>
          <path d="M2 3.5L5 6.5L8 3.5" stroke="white" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/>
        </svg>
      </button>
      {dropOpen && (
        <div className="absolute right-0 mt-1.5 py-1 rounded-lg overflow-hidden shadow-xl"
          style={{ background: 'rgba(255,255,255,0.95)', backdropFilter: 'blur(12px)', border: '1px solid rgba(0,0,0,0.08)', minWidth: 130 }}>
          {LANGS.map(l => (
            <button key={l.code} onClick={() => { setLang(l.code); setDropOpen(false) }}
              className="w-full flex items-center gap-2.5 px-4 py-2 text-xs text-left transition-colors hover:bg-blue-50"
              style={{ fontFamily: l.font, color: l.code === lang ? '#2d5be3' : '#1a1916', fontWeight: l.code === lang ? 600 : 400 }}>
              <span>{l.flag}</span><span>{l.label}</span>
              {l.code === lang && <svg className="ml-auto" width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M2 6L5 9L10 3" stroke="#2d5be3" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>}
            </button>
          ))}
        </div>
      )}
    </div>
  )

  const LeftPanel = ({ welcome, title, subtitle }: { welcome: string; title: string; subtitle: string }) => (
    <div className="hidden md:flex flex-col justify-between p-8 shrink-0"
      style={{ width: 220, background: 'linear-gradient(160deg, #1e3a6e 0%, #2d5be3 60%, #4f7fff 100%)' }}>
      <div>
        <div className="text-[10px] tracking-[0.2em] uppercase mb-1" style={{ color: 'rgba(255,255,255,0.5)', fontFamily: font }}>{welcome}</div>
        <div className="text-xl leading-tight font-medium" style={{ fontFamily: font, color: '#fff' }}>{title}</div>
      </div>
      <div>
        <div className="w-6 h-px mb-4" style={{ backgroundColor: 'rgba(255,255,255,0.35)' }} />
        <p className="text-xs leading-relaxed whitespace-pre-line" style={{ color: 'rgba(255,255,255,0.55)', fontFamily: font, fontWeight: 300 }}>{subtitle}</p>
      </div>
    </div>
  )

  // ── LOGIN ───────────────────────────────────────────────────
  if (page === 'login') {
    return (
      <div className="min-h-screen flex items-center justify-center relative overflow-hidden" style={{ backgroundColor: '#0d1b3e' }}>
        <Background /><LangSwitcher />
        <div className="relative z-10 flex rounded-2xl overflow-hidden shadow-2xl w-full mx-4" style={{ maxWidth: 780 }}>
          <LeftPanel welcome={t.welcome} title={t.mobileTitle} subtitle={t.subtitle} />
          <div className="flex-1 px-8 py-10" style={{ backgroundColor: '#fff' }}>
            <div className="hidden md:block mb-7">
              <div className="text-[10px] tracking-[0.2em] uppercase mb-1" style={{ color: '#6b6760', fontFamily: font }}>{t.pageTitle}</div>
              <div className="text-xl font-medium" style={{ fontFamily: font, color: '#1a1916' }}>{t.greeting}</div>
            </div>
            <form onSubmit={e => e.preventDefault()} className="space-y-4">
              <div>
                <label className="block text-[10px] tracking-[0.12em] uppercase mb-1.5" style={{ color: '#6b6760', fontFamily: font }}>{t.idLabel}</label>
                <input type="text" value={loginId} onChange={e => setLoginId(e.target.value)}
                  onFocus={() => setLf(p => ({ ...p, id: true }))} onBlur={() => setLf(p => ({ ...p, id: false }))}
                  placeholder={t.idPlaceholder} autoComplete="username" style={inputBase(lf.id, font)} />
              </div>
              <div>
                <label className="block text-[10px] tracking-[0.12em] uppercase mb-1.5" style={{ color: '#6b6760', fontFamily: font }}>{t.pwLabel}</label>
                <input type="password" value={loginPw} onChange={e => setLoginPw(e.target.value)}
                  onFocus={() => setLf(p => ({ ...p, pw: true }))} onBlur={() => setLf(p => ({ ...p, pw: false }))}
                  placeholder={t.pwPlaceholder} autoComplete="current-password" style={inputBase(lf.pw, font)} />
              </div>
              <button type="submit" className="w-full py-3 text-sm font-medium transition-all hover:opacity-90 active:scale-[0.98]"
                style={{ background: 'linear-gradient(90deg, #1e3a6e 0%, #2d5be3 100%)', color: '#fff', borderRadius: '4px', fontFamily: font }}>
                {t.login}
              </button>
            </form>
            <div className="flex items-center gap-3 my-5">
              <div className="flex-1 h-px" style={{ backgroundColor: '#e8e4dc' }} />
              <span className="text-[11px]" style={{ color: '#6b6760', fontFamily: font }}>{t.or}</span>
              <div className="flex-1 h-px" style={{ backgroundColor: '#e8e4dc' }} />
            </div>
            <button type="button" className="w-full py-2.5 text-sm font-medium flex items-center justify-center gap-3 bg-white transition-all hover:bg-gray-50 active:scale-[0.98]"
              style={{ border: '1.5px solid #d4cfc6', borderRadius: '4px', color: '#1a1916', fontFamily: font }}>
              <GoogleIcon />{t.google}
            </button>
            <div className="flex items-center justify-between mt-6">
              <button onClick={() => setPage('signup')}
                className="text-sm font-medium transition-opacity hover:opacity-70 underline underline-offset-4"
                style={{ color: '#1a1916', fontFamily: font, textDecorationColor: '#2d5be3' }}>
                {t.signup}
              </button>
              <button className="text-xs transition-opacity hover:opacity-70" style={{ color: '#6b6760', fontFamily: font }}>
                {t.forgot}
              </button>
            </div>
          </div>
        </div>
      </div>
    )
  }

  // ── SIGNUP ──────────────────────────────────────────────────
  return (
    <div className="min-h-screen flex items-center justify-center relative overflow-hidden py-8" style={{ backgroundColor: '#0d1b3e' }}>
      <Background /><LangSwitcher />
      <div className="relative z-10 flex rounded-2xl overflow-hidden shadow-2xl w-full mx-4" style={{ maxWidth: 820 }}>
        <LeftPanel welcome={t.signupWelcome} title={t.signupTitle} subtitle={t.signupSubtitle} />

        <div className="flex-1 px-8 py-8 overflow-y-auto" style={{ backgroundColor: '#fff' }}>
          <div className="mb-5">
            <div className="text-[10px] tracking-[0.2em] uppercase mb-1" style={{ color: '#6b6760', fontFamily: font }}>{t.signupWelcome}</div>
            <div className="text-xl font-medium" style={{ fontFamily: font, color: '#1a1916' }}>{t.signupGreeting}</div>
          </div>

          <form onSubmit={e => e.preventDefault()} className="space-y-3.5">

            {/* ── ID with duplicate check ── */}
            <div>
              <label className="block text-[10px] tracking-[0.12em] uppercase mb-1.5" style={{ color: '#6b6760', fontFamily: font }}>{t.idLabel}</label>
              <div className="flex gap-2">
                <input type="text" value={su.id}
                  onChange={e => { setSu(p => ({ ...p, id: e.target.value })); setIdStatus('idle') }}
                  onFocus={() => setSf(p => ({ ...p, id: true }))} onBlur={() => setSf(p => ({ ...p, id: false }))}
                  placeholder={t.idPlaceholder} autoComplete="username"
                  style={{ ...inputBase(sf.id, font), width: undefined, flex: 1 }} />
                <button type="button" onClick={handleCheckId}
                  disabled={!su.id.trim() || idStatus === 'verified'}
                  style={actionBtn(font, !su.id.trim() || idStatus === 'verified')}
                  onMouseOver={e => { if (su.id.trim() && idStatus !== 'verified') (e.currentTarget as HTMLButtonElement).style.background = '#eff4ff' }}
                  onMouseOut={e => { (e.currentTarget as HTMLButtonElement).style.background = 'transparent' }}>
                  {idStatus === 'verified' ? '✓' : t.checkDup}
                </button>
              </div>
              <StatusMsg status={idStatus} okMsg={t.dupOk} failMsg={t.dupFail} font={font} />
            </div>

            {/* ── Password ── */}
            <div>
              <label className="block text-[10px] tracking-[0.12em] uppercase mb-1.5" style={{ color: '#6b6760', fontFamily: font }}>{t.pwLabel}</label>
              <input type="password" value={su.pw} onChange={e => setSu(p => ({ ...p, pw: e.target.value }))}
                onFocus={() => setSf(p => ({ ...p, pw: true }))} onBlur={() => setSf(p => ({ ...p, pw: false }))}
                placeholder={t.pwPlaceholder} autoComplete="new-password"
                style={inputBase(sf.pw, font)} />
            </div>

            {/* ── Email with verification ── */}
            <div>
              <label className="block text-[10px] tracking-[0.12em] uppercase mb-1.5" style={{ color: '#6b6760', fontFamily: font }}>{t.emailLabel}</label>
              <div className="flex gap-2">
                <input type="email" value={su.email}
                  onChange={e => { setSu(p => ({ ...p, email: e.target.value })); setEmailStatus('idle'); setEmailCode('') }}
                  onFocus={() => setSf(p => ({ ...p, email: true }))} onBlur={() => setSf(p => ({ ...p, email: false }))}
                  placeholder={t.emailPlaceholder} autoComplete="email"
                  style={{ ...inputBase(sf.email, font), width: undefined, flex: 1 }} />
                <button type="button" onClick={handleSendCode}
                  disabled={!su.email.trim() || emailStatus === 'verified'}
                  style={actionBtn(font, !su.email.trim() || emailStatus === 'verified')}
                  onMouseOver={e => { if (su.email.trim() && emailStatus !== 'verified') (e.currentTarget as HTMLButtonElement).style.background = '#eff4ff' }}
                  onMouseOut={e => { (e.currentTarget as HTMLButtonElement).style.background = 'transparent' }}>
                  {emailStatus === 'verified' ? '✓' : t.sendCode}
                </button>
              </div>
              <StatusMsg status={emailStatus === 'sent' ? 'sent' : emailStatus === 'verified' ? 'verified' : 'idle'}
                okMsg={t.codeOk} failMsg={t.codeFail} sentMsg={t.codeSent} font={font} />

              {/* Code input — appears after code sent */}
              {(emailStatus === 'sent' || emailStatus === 'error') && (
                <div className="flex gap-2 mt-2">
                  <input type="text" value={emailCode} onChange={e => setEmailCode(e.target.value)}
                    onFocus={() => setEmailCodeFocused(true)} onBlur={() => setEmailCodeFocused(false)}
                    placeholder={t.codePlaceholder} maxLength={6}
                    style={{ ...inputBase(emailCodeFocused, font), width: undefined, flex: 1, padding: '8px 14px' }} />
                  <button type="button" onClick={handleConfirmCode} disabled={!emailCode.trim()}
                    style={actionBtn(font, !emailCode.trim())}
                    onMouseOver={e => { if (emailCode.trim()) (e.currentTarget as HTMLButtonElement).style.background = '#eff4ff' }}
                    onMouseOut={e => { (e.currentTarget as HTMLButtonElement).style.background = 'transparent' }}>
                    {t.confirmCode}
                  </button>
                </div>
              )}
              {emailStatus === 'error' && (
                <StatusMsg status="error" okMsg="" failMsg={t.codeFail} font={font} />
              )}
            </div>

            {/* ── Nickname + Job ── */}
            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-[10px] tracking-[0.12em] uppercase mb-1.5" style={{ color: '#6b6760', fontFamily: font }}>{t.nicknameLabel}</label>
                <input type="text" value={su.nickname} onChange={e => setSu(p => ({ ...p, nickname: e.target.value }))}
                  onFocus={() => setSf(p => ({ ...p, nickname: true }))} onBlur={() => setSf(p => ({ ...p, nickname: false }))}
                  placeholder={t.nicknamePlaceholder}
                  style={inputBase(sf.nickname, font)} />
              </div>
              <div>
                <label className="block text-[10px] tracking-[0.12em] uppercase mb-1.5" style={{ color: '#6b6760', fontFamily: font }}>{t.jobLabel}</label>
                <select value={su.job} onChange={e => setSu(p => ({ ...p, job: e.target.value }))}
                  style={{ ...inputBase(false, font), cursor: 'pointer', appearance: 'none' as const,
                    backgroundImage: `url("data:image/svg+xml,%3Csvg xmlns='http://www.w3.org/2000/svg' width='12' height='12' viewBox='0 0 12 12'%3E%3Cpath d='M2 4l4 4 4-4' stroke='%236b6760' stroke-width='1.5' fill='none' stroke-linecap='round'/%3E%3C/svg%3E")`,
                    backgroundRepeat: 'no-repeat', backgroundPosition: 'right 12px center', paddingRight: '32px' }}>
                  {JOBS[lang].map(j => <option key={j} value={j === JOBS[lang][0] ? '' : j}>{j}</option>)}
                </select>
              </div>
            </div>

            <button type="submit" className="w-full py-3 text-sm font-medium transition-all hover:opacity-90 active:scale-[0.98] mt-1"
              style={{ background: 'linear-gradient(90deg, #1e3a6e 0%, #2d5be3 100%)', color: '#fff', borderRadius: '4px', fontFamily: font }}>
              {t.signupBtn}
            </button>
          </form>

          <div className="flex items-center justify-center gap-1.5 mt-5">
            <span className="text-xs" style={{ color: '#6b6760', fontFamily: font }}>{t.backToLogin}</span>
            <button onClick={() => setPage('login')}
              className="text-xs font-semibold transition-opacity hover:opacity-70 underline underline-offset-4"
              style={{ color: '#2d5be3', fontFamily: font, textDecorationColor: '#2d5be3' }}>
              {t.backLogin}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
