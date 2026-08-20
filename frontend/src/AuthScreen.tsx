import { useState, useRef, useEffect } from 'react'
import { useGoogleLogin } from '@react-oauth/google'
import { useTranslation } from 'react-i18next'

const BG_PEOPLE = 'https://images.unsplash.com/photo-1758272133771-b149318883c5?w=1800&h=1200&fit=crop&auto=format'
const BG_GLOBE  = 'https://images.unsplash.com/photo-1684610529682-553625a1ffed?w=1800&h=1200&fit=crop&auto=format'

type Page = 'login' | 'signup'
type Status = 'idle' | 'sent' | 'verified' | 'error'

const LANG_OPTIONS = [
  { code: 'ko', label: '한국어', flag: '🇰🇷' },
  { code: 'en', label: 'English', flag: '🇺🇸' },
  { code: 'zh', label: '中文', flag: '🇨🇳' },
]

const LANG_FONTS: Record<string, string> = {
  ko: "'Noto Sans KR', sans-serif",
  en: "'Noto Sans', sans-serif",
  zh: "'Noto Sans SC', sans-serif",
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

export default function AuthScreen({ onSuccess }: { onSuccess: () => void }) {
  const { t, i18n } = useTranslation()
  const font = LANG_FONTS[i18n.language] || LANG_FONTS.en
  const currentLang = LANG_OPTIONS.find(l => l.code === i18n.language) || LANG_OPTIONS[1]

  const [page, setPage] = useState<Page>('login')
  const [dropOpen, setDropOpen] = useState(false)
  const dropRef = useRef<HTMLDivElement>(null)

  // login
  const [loginId, setLoginId] = useState('')
  const [loginPw, setLoginPw] = useState('')
  const [lf, setLf] = useState({ id: false, pw: false })

  // signup fields
  const [su, setSu] = useState({ id: '', pw: '', email: '', nickname: '' })
  const [sf, setSf] = useState({ id: false, pw: false, email: false, nickname: false })

  // ID duplicate check
  const [idStatus, setIdStatus] = useState<Status>('idle')


  const loginWithGoogle = useGoogleLogin({
    onSuccess: async (codeResponse) => {
      console.log('Google login success:', codeResponse)
      try {
        const langLabel = LANG_OPTIONS.find(l => l.code === i18n.language)?.label || '한국어'
        const res = await fetch('/api/auth/google', {
          method: 'POST',
          headers: { 'Content-Type': 'application/json' },
          body: JSON.stringify({ 
            accessToken: codeResponse.access_token,
            language: langLabel 
          })
        })
        if (res.ok) {
          const data = await res.json()
          localStorage.setItem('templ_token', data.accessToken)
          if (data.nickname) localStorage.setItem('templ_user_nickname', data.nickname)
          if (data.username) localStorage.setItem('templ_user_email', data.username)
          onSuccess()
        } else {
          console.error('Failed to authenticate with backend')
        }
      } catch (e) {
        console.error('Google login error:', e)
      }
    },
    onError: (error) => console.log('Google login error:', error),
  })

  // reset verification states on lang change
  useEffect(() => {
    setIdStatus('idle')
  }, [i18n.language])

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


  // Login handler
  const handleLogin = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const res = await fetch('/api/auth/login', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ id: loginId, pw: loginPw })
      })
      if (res.ok) {
        const data = await res.json()
        localStorage.setItem('templ_token', data.accessToken)
        if (data.nickname) localStorage.setItem('templ_user_nickname', data.nickname)
        if (data.username) localStorage.setItem('templ_user_email', data.username)
        onSuccess()
      } else {
        console.error('Login failed')
      }
    } catch (err) {
      console.error(err)
    }
  }

  // Signup handler
  const handleSignup = async (e: React.FormEvent) => {
    e.preventDefault()
    try {
      const langLabel = LANG_OPTIONS.find(l => l.code === i18n.language)?.label || '한국어'
      const res = await fetch('/api/auth/signup', {
        method: 'POST',
        headers: { 'Content-Type': 'application/json' },
        body: JSON.stringify({ 
          id: su.id, 
          pw: su.pw, 
          email: su.email, 
          nickname: su.nickname,
          language: langLabel
        })
      })
      if (res.ok) {
        setPage('login')
      } else {
        console.error('Signup failed')
      }
    } catch (err) {
      console.error(err)
    }
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
          {LANG_OPTIONS.map(l => {
            const lFont = LANG_FONTS[l.code] || LANG_FONTS.en
            return (
              <button key={l.code} onClick={() => { i18n.changeLanguage(l.code); setDropOpen(false) }}
                className="w-full flex items-center gap-2.5 px-4 py-2 text-xs text-left transition-colors hover:bg-blue-50"
                style={{ fontFamily: lFont, color: l.code === i18n.language ? '#2d5be3' : '#1a1916', fontWeight: l.code === i18n.language ? 600 : 400 }}>
                <span>{l.flag}</span><span>{l.label}</span>
                {l.code === i18n.language && <svg className="ml-auto" width="12" height="12" viewBox="0 0 12 12" fill="none"><path d="M2 6L5 9L10 3" stroke="#2d5be3" strokeWidth="1.5" strokeLinecap="round" strokeLinejoin="round"/></svg>}
              </button>
            )
          })}
        </div>
      )}
    </div>
  )

  const LeftPanel = ({
  welcome,
  title,
  subtitle,
}: {
  welcome: string
  title: string
  subtitle: string
}) => (
  <div
    className="hidden md:flex flex-col justify-between p-8 shrink-0"
    style={{
      width: 250,
      background:
        "linear-gradient(160deg, #111321 0%, #191b31 55%, #332b76 100%)",
    }}
  >
    <div>
      {/* Orchestree 브랜드 영역 */}
      <div
        style={{
          display: "flex",
          alignItems: "center",
          gap: 12,
          marginBottom: 30,
        }}
      >
        <img
          src="/favicon.png"
          alt=""
          aria-hidden="true"
          style={{
            width: 48,
            height: 48,
            borderRadius: 12,
            objectFit: "contain",
            flexShrink: 0,
          }}
        />

        <div style={{ minWidth: 0 }}>
          <div
            style={{
              color: "#f5f4ff",
              fontSize: 23,
              fontWeight: 700,
              lineHeight: 1.05,
              letterSpacing: "-0.03em",
              whiteSpace: "nowrap",
            }}
          >
            Orchestree
          </div>

          <div
            style={{
              color: "rgba(199, 196, 232, 0.7)",
              fontSize: 8,
              letterSpacing: "0.16em",
              marginTop: 7,
              whiteSpace: "nowrap",
            }}
          >
            ONE TREE · EVERY TOOL
          </div>
        </div>
      </div>

      {/* 기존 화면 안내 */}
      <div
        className="text-[10px] tracking-[0.2em] uppercase mb-1"
        style={{
          color: "rgba(255,255,255,0.5)",
          fontFamily: font,
        }}
      >
        {welcome}
      </div>

      <div
        className="text-xl leading-tight font-medium"
        style={{
          fontFamily: font,
          color: "#ffffff",
        }}
      >
        {title}
      </div>
    </div>

    <div>
      <div
        className="w-6 h-px mb-4"
        style={{
          backgroundColor: "rgba(255,255,255,0.35)",
        }}
      />

      <p
        className="text-xs leading-relaxed whitespace-pre-line"
        style={{
          color: "rgba(255,255,255,0.58)",
          fontFamily: font,
          fontWeight: 300,
        }}
      >
        {subtitle}
      </p>
    </div>
  </div>
)

  // ── LOGIN ───────────────────────────────────────────────────
  if (page === 'login') {
    return (
      <div className="min-h-screen flex items-center justify-center relative overflow-hidden" style={{ backgroundColor: '#0d1b3e' }}>
        <Background /><LangSwitcher />
        <div className="relative z-10 flex rounded-2xl overflow-hidden shadow-2xl w-full mx-4" style={{ maxWidth: 780 }}>
          <LeftPanel welcome={t('auth.welcome')} title={t('auth.mobileTitle')} subtitle={t('auth.subtitle')} />
          <div className="flex-1 px-8 py-10" style={{ backgroundColor: '#fff' }}>
            <div className="hidden md:block mb-7">
              <div className="text-[10px] tracking-[0.2em] uppercase mb-1" style={{ color: '#6b6760', fontFamily: font }}>{t('auth.pageTitle')}</div>
              <div className="text-xl font-medium" style={{ fontFamily: font, color: '#1a1916' }}>{t('auth.greeting')}</div>
            </div>
            <form onSubmit={handleLogin} className="space-y-4">
              <div>
                <label className="block text-[10px] tracking-[0.12em] uppercase mb-1.5" style={{ color: '#6b6760', fontFamily: font }}>{t('auth.idLabel')}</label>
                <input type="text" value={loginId} onChange={e => setLoginId(e.target.value)}
                  onFocus={() => setLf(p => ({ ...p, id: true }))} onBlur={() => setLf(p => ({ ...p, id: false }))}
                  placeholder={t('auth.idPlaceholder')} autoComplete="username" style={inputBase(lf.id, font)} />
              </div>
              <div>
                <label className="block text-[10px] tracking-[0.12em] uppercase mb-1.5" style={{ color: '#6b6760', fontFamily: font }}>{t('auth.pwLabel')}</label>
                <input type="password" value={loginPw} onChange={e => setLoginPw(e.target.value)}
                  onFocus={() => setLf(p => ({ ...p, pw: true }))} onBlur={() => setLf(p => ({ ...p, pw: false }))}
                  placeholder={t('auth.pwPlaceholder')} autoComplete="current-password" style={inputBase(lf.pw, font)} />
              </div>
              <button type="submit" className="w-full py-3 text-sm font-medium transition-all hover:opacity-90 active:scale-[0.98]"
                style={{ background: 'linear-gradient(90deg, #1e3a6e 0%, #2d5be3 100%)', color: '#fff', borderRadius: '4px', fontFamily: font }}>
                {t('auth.login')}
              </button>
            </form>
            <div className="flex items-center gap-3 my-5">
              <div className="flex-1 h-px" style={{ backgroundColor: '#e8e4dc' }} />
              <span className="text-[11px]" style={{ color: '#6b6760', fontFamily: font }}>{t('auth.or')}</span>
              <div className="flex-1 h-px" style={{ backgroundColor: '#e8e4dc' }} />
            </div>
            <button type="button" onClick={() => loginWithGoogle()} className="w-full py-2.5 text-sm font-medium flex items-center justify-center gap-3 bg-white transition-all hover:bg-gray-50 active:scale-[0.98]"
              style={{ border: '1.5px solid #d4cfc6', borderRadius: '4px', color: '#1a1916', fontFamily: font }}>
              <GoogleIcon />{t('auth.google')}
            </button>
            <div className="flex items-center justify-between mt-6">
              <button onClick={() => setPage('signup')}
                className="text-sm font-medium transition-opacity hover:opacity-70 underline underline-offset-4"
                style={{ color: '#1a1916', fontFamily: font, textDecorationColor: '#2d5be3' }}>
                {t('auth.signup')}
              </button>
              <button className="text-xs transition-opacity hover:opacity-70" style={{ color: '#6b6760', fontFamily: font }}>
                {t('auth.forgot')}
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
        <LeftPanel welcome={t('auth.signupWelcome')} title={t('auth.signupTitle')} subtitle={t('auth.signupSubtitle')} />

        <div className="flex-1 px-8 py-8 overflow-y-auto" style={{ backgroundColor: '#fff' }}>
          <div className="mb-5">
            <div className="text-[10px] tracking-[0.2em] uppercase mb-1" style={{ color: '#6b6760', fontFamily: font }}>{t('auth.signupWelcome')}</div>
            <div className="text-xl font-medium" style={{ fontFamily: font, color: '#1a1916' }}>{t('auth.signupGreeting')}</div>
          </div>

          <form onSubmit={handleSignup} className="space-y-3.5">

            {/* ── ID with duplicate check ── */}
            <div>
              <label className="block text-[10px] tracking-[0.12em] uppercase mb-1.5" style={{ color: '#6b6760', fontFamily: font }}>{t('auth.idLabel')}</label>
              <div className="flex gap-2">
                <input type="text" value={su.id}
                  onChange={e => { setSu(p => ({ ...p, id: e.target.value })); setIdStatus('idle') }}
                  onFocus={() => setSf(p => ({ ...p, id: true }))} onBlur={() => setSf(p => ({ ...p, id: false }))}
                  placeholder={t('auth.idPlaceholder')} autoComplete="username"
                  style={{ ...inputBase(sf.id, font), width: undefined, flex: 1 }} />
                <button type="button" onClick={handleCheckId}
                  disabled={!su.id.trim() || idStatus === 'verified'}
                  style={actionBtn(font, !su.id.trim() || idStatus === 'verified')}
                  onMouseOver={e => { if (su.id.trim() && idStatus !== 'verified') (e.currentTarget as HTMLButtonElement).style.background = '#eff4ff' }}
                  onMouseOut={e => { (e.currentTarget as HTMLButtonElement).style.background = 'transparent' }}>
                  {idStatus === 'verified' ? '✓' : t('auth.checkDup')}
                </button>
              </div>
              <StatusMsg status={idStatus} okMsg={t('auth.dupOk')} failMsg={t('auth.dupFail')} font={font} />
            </div>

            {/* ── Password ── */}
            <div>
              <label className="block text-[10px] tracking-[0.12em] uppercase mb-1.5" style={{ color: '#6b6760', fontFamily: font }}>{t('auth.pwLabel')}</label>
              <input type="password" value={su.pw} onChange={e => setSu(p => ({ ...p, pw: e.target.value }))}
                onFocus={() => setSf(p => ({ ...p, pw: true }))} onBlur={() => setSf(p => ({ ...p, pw: false }))}
                placeholder={t('auth.pwPlaceholder')} autoComplete="new-password"
                style={inputBase(sf.pw, font)} />
            </div>

            {/* ── Email ── */}
            <div>
              <label className="block text-[10px] tracking-[0.12em] uppercase mb-1.5" style={{ color: '#6b6760', fontFamily: font }}>{t('auth.emailLabel')}</label>
              <input type="email" value={su.email}
                onChange={e => setSu(p => ({ ...p, email: e.target.value }))}
                onFocus={() => setSf(p => ({ ...p, email: true }))} onBlur={() => setSf(p => ({ ...p, email: false }))}
                placeholder={t('auth.emailPlaceholder')} autoComplete="email"
                style={inputBase(sf.email, font)} />
            </div>

            {/* ── Nickname (Job field removed) ── */}
            <div>
              <label className="block text-[10px] tracking-[0.12em] uppercase mb-1.5" style={{ color: '#6b6760', fontFamily: font }}>{t('auth.nicknameLabel')}</label>
              <input type="text" value={su.nickname} onChange={e => setSu(p => ({ ...p, nickname: e.target.value }))}
                onFocus={() => setSf(p => ({ ...p, nickname: true }))} onBlur={() => setSf(p => ({ ...p, nickname: false }))}
                placeholder={t('auth.nicknamePlaceholder')}
                style={inputBase(sf.nickname, font)} />
            </div>

            {/* ── Preferred Language ── */}
            <div>
              <label className="block text-[10px] tracking-[0.12em] uppercase mb-1.5" style={{ color: '#6b6760', fontFamily: font }}>Language</label>
              <select 
                value={i18n.language} 
                onChange={e => i18n.changeLanguage(e.target.value)}
                style={{ ...inputBase(false, font), cursor: 'pointer' }}>
                {LANG_OPTIONS.map(l => (
                  <option key={l.code} value={l.code}>{l.flag} {l.label}</option>
                ))}
              </select>
            </div>

            <button type="submit" className="w-full py-3 text-sm font-medium transition-all hover:opacity-90 active:scale-[0.98] mt-1"
              style={{ background: 'linear-gradient(90deg, #1e3a6e 0%, #2d5be3 100%)', color: '#fff', borderRadius: '4px', fontFamily: font }}>
              {t('auth.signupBtn')}
            </button>
          </form>

          <div className="flex items-center justify-center gap-1.5 mt-5">
            <span className="text-xs" style={{ color: '#6b6760', fontFamily: font }}>{t('auth.backToLogin')}</span>
            <button onClick={() => setPage('login')}
              className="text-xs font-semibold transition-opacity hover:opacity-70 underline underline-offset-4"
              style={{ color: '#2d5be3', fontFamily: font, textDecorationColor: '#2d5be3' }}>
              {t('auth.backLogin')}
            </button>
          </div>
        </div>
      </div>
    </div>
  )
}
