import { useState } from 'react'
import { useNavigate, Link } from 'react-router-dom'
import { register } from '../api/client'

export default function RegisterPage() {
  const navigate = useNavigate()
  const [form, setForm] = useState({
    email: '',
    password: '',
    passwordConfirm: '',
    companyName: '',
    contactPerson: '',
    phone: '',
    address: '',
  })
  const [error, setError] = useState('')
  const [loading, setLoading] = useState(false)

  const update = (field: string, value: string) =>
    setForm((f) => ({ ...f, [field]: value }))

  const handleSubmit = async (e: React.FormEvent) => {
    e.preventDefault()
    setError('')

    if (form.password !== form.passwordConfirm) {
      setError('비밀번호가 일치하지 않습니다.')
      return
    }
    if (form.password.length < 8) {
      setError('비밀번호는 8자 이상이어야 합니다.')
      return
    }

    setLoading(true)
    try {
      await register({
        email: form.email,
        password: form.password,
        companyName: form.companyName,
        contactPerson: form.contactPerson,
        phone: form.phone,
        address: form.address,
      })
      navigate('/login', { state: { registered: true } })
    } catch (err: unknown) {
      const axiosErr = err as { response?: { data?: { message?: string } } }
      setError(axiosErr.response?.data?.message ?? '회원가입에 실패했습니다. 다시 시도해 주세요.')
    } finally {
      setLoading(false)
    }
  }

  const inputClass =
    'w-full px-4 py-2.5 text-sm border border-gray-300 rounded-lg bg-white focus:outline-none focus:ring-2 focus:border-[#1a56db] transition-colors'

  return (
    <div className="min-h-screen flex" style={{ background: '#f5f7fa' }}>
      {/* 좌측 브랜드 패널 */}
      <div
        className="hidden lg:flex flex-col justify-between w-[460px] flex-shrink-0 p-12 text-white"
        style={{ background: 'linear-gradient(160deg, #0c1e3d 0%, #1a3a6c 100%)' }}
      >
        <div className="flex items-center gap-2.5">
          <div
            className="w-8 h-8 rounded-md flex items-center justify-center"
            style={{ background: 'rgba(255,255,255,0.12)' }}
          >
            <i className="fa-solid fa-file-signature text-white text-sm" />
          </div>
          <span className="font-bold text-lg text-white">WJbid.com</span>
        </div>

        <div>
          <h2 className="text-3xl font-bold leading-snug mb-4">
            지금 가입하고<br />입찰을 시작하세요
          </h2>
          <p className="text-sm leading-relaxed mb-10" style={{ color: 'rgba(255,255,255,0.6)' }}>
            회원가입 후 바로 SAM.gov 공고를 검색하고<br />
            전문가 제안서 작성 서비스를 이용할 수 있습니다.
          </p>
          <div className="space-y-3">
            {[
              { icon: 'fa-solid fa-user-plus', text: '간편한 회원가입' },
              { icon: 'fa-solid fa-bolt', text: '즉시 공고 검색 시작' },
              { icon: 'fa-solid fa-brain', text: '전문가 제안서 작성' },
            ].map((f) => (
              <div key={f.text} className="flex items-center gap-3">
                <div
                  className="w-8 h-8 rounded-lg flex items-center justify-center flex-shrink-0"
                  style={{ background: 'rgba(255,255,255,0.1)' }}
                >
                  <i className={`${f.icon} text-xs`} style={{ color: 'rgba(255,255,255,0.75)' }} />
                </div>
                <span className="text-sm" style={{ color: 'rgba(255,255,255,0.65)' }}>{f.text}</span>
              </div>
            ))}
          </div>
        </div>

        <p className="text-xs" style={{ color: 'rgba(255,255,255,0.3)' }}>© 2025 WJbid.com</p>
      </div>

      {/* 우측 가입 폼 */}
      <div className="flex-1 flex items-center justify-center px-6 py-12">
        <div className="w-full max-w-[440px]">
          {/* 모바일 로고 */}
          <div className="lg:hidden flex items-center gap-2.5 mb-8">
            <div className="w-8 h-8 rounded-md flex items-center justify-center" style={{ background: '#0c1e3d' }}>
              <i className="fa-solid fa-file-signature text-white text-sm" />
            </div>
            <span className="font-bold text-[17px]" style={{ color: '#0c1e3d' }}>
              WJbid<span className="font-normal text-sm" style={{ color: '#1a56db' }}>.com</span>
            </span>
          </div>

          <div className="mb-8">
            <h1 className="text-2xl font-bold text-[#0c1e3d] mb-1">회원가입</h1>
            <p className="text-sm text-gray-500">기업 정보를 입력하여 서비스에 가입하세요</p>
          </div>

          <form onSubmit={handleSubmit} className="space-y-4">
            {/* 계정 정보 */}
            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">
                이메일 <span className="text-red-500">*</span>
              </label>
              <input
                type="email"
                value={form.email}
                onChange={(e) => update('email', e.target.value)}
                required
                placeholder="name@company.com"
                className={inputClass}
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">
                  비밀번호 <span className="text-red-500">*</span>
                </label>
                <input
                  type="password"
                  value={form.password}
                  onChange={(e) => update('password', e.target.value)}
                  required
                  placeholder="8자 이상"
                  className={inputClass}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">
                  비밀번호 확인 <span className="text-red-500">*</span>
                </label>
                <input
                  type="password"
                  value={form.passwordConfirm}
                  onChange={(e) => update('passwordConfirm', e.target.value)}
                  required
                  placeholder="비밀번호 재입력"
                  className={inputClass}
                />
              </div>
            </div>

            {/* 구분선 */}
            <div className="border-t border-gray-200 pt-4">
              <p className="text-xs font-semibold text-gray-400 uppercase tracking-wider mb-3">기업 정보</p>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">
                회사명 <span className="text-red-500">*</span>
              </label>
              <input
                type="text"
                value={form.companyName}
                onChange={(e) => update('companyName', e.target.value)}
                required
                placeholder="예: VOOMERANG Co., Ltd."
                className={inputClass}
              />
            </div>

            <div className="grid grid-cols-2 gap-3">
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">담당자명</label>
                <input
                  type="text"
                  value={form.contactPerson}
                  onChange={(e) => update('contactPerson', e.target.value)}
                  placeholder="예: 김철수"
                  className={inputClass}
                />
              </div>
              <div>
                <label className="block text-sm font-medium text-gray-700 mb-1.5">전화번호</label>
                <input
                  type="tel"
                  value={form.phone}
                  onChange={(e) => update('phone', e.target.value)}
                  placeholder="010-1234-5678"
                  className={inputClass}
                />
              </div>
            </div>

            <div>
              <label className="block text-sm font-medium text-gray-700 mb-1.5">주소</label>
              <input
                type="text"
                value={form.address}
                onChange={(e) => update('address', e.target.value)}
                placeholder="예: 서울특별시 강남구 ..."
                className={inputClass}
              />
            </div>

            {error && (
              <div className="flex items-center gap-2 p-3 rounded-lg bg-red-50 border border-red-200">
                <i className="fa-solid fa-circle-exclamation text-red-500 text-sm flex-shrink-0" />
                <p className="text-sm text-red-700">{error}</p>
              </div>
            )}

            <button
              type="submit"
              disabled={loading}
              className="w-full py-2.5 text-sm font-semibold text-white rounded-lg transition-opacity disabled:opacity-60 mt-2"
              style={{ background: '#0c1e3d' }}
              onMouseEnter={(e) => !loading && (e.currentTarget.style.background = '#162d57')}
              onMouseLeave={(e) => !loading && (e.currentTarget.style.background = '#0c1e3d')}
            >
              {loading ? (
                <span className="flex items-center justify-center gap-2">
                  <i className="fa-solid fa-spinner fa-spin text-xs" /> 가입 처리 중...
                </span>
              ) : '회원가입'}
            </button>
          </form>

          <div className="mt-6 text-center">
            <p className="text-sm text-gray-500">
              이미 계정이 있으신가요?{' '}
              <Link to="/login" className="text-[#1a56db] font-medium hover:underline">
                로그인
              </Link>
            </p>
          </div>

          <div className="mt-4 pt-4 border-t border-gray-200">
            <Link
              to="/"
              className="flex items-center gap-1.5 text-xs text-gray-400 hover:text-gray-600 transition-colors"
            >
              <i className="fa-solid fa-arrow-left text-[10px]" />
              홈으로 돌아가기
            </Link>
          </div>
        </div>
      </div>
    </div>
  )
}
