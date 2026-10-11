import { useEffect, useRef } from 'react'
import { useNavigate, useSearchParams } from 'react-router-dom'
import { snsLogin } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { SNS_RETURN_KEY } from '../components/auth/authRules'
import '../wjbid-auth.css'

const FAILED = '/login?sns=FAILED'

function takeReturnTo() {
  try {
    const to = sessionStorage.getItem(SNS_RETURN_KEY)
    sessionStorage.removeItem(SNS_RETURN_KEY)
    return to?.startsWith('/') ? to : null
  } catch {
    return null
  }
}

/** SNS 에서 돌아와 표를 토큰으로 바꾸는 동안 잠깐 보이는 화면 (CR-146 T3-4 시나리오 7) */
export default function SnsDonePage() {
  const navigate = useNavigate()
  const { signIn } = useAuth()
  const [params] = useSearchParams()
  // 표는 한 번만 쓸 수 있어 개발 모드에서 두 번 도는 effect 가 두 번째로 바꾸지 않게 막는다
  const started = useRef(false)

  useEffect(() => {
    if (started.current) return
    started.current = true
    const ticket = params.get('ticket')
    if (!ticket) {
      navigate(FAILED, { replace: true })
      return
    }
    snsLogin(ticket)
      .then(({ data }) => {
        signIn(data.email, data.accessToken, data.refreshToken)
        navigate(takeReturnTo() ?? '/search', { replace: true })
      })
      .catch(() => navigate(FAILED, { replace: true }))
  }, [params, navigate, signIn])

  return (
    <div className="wj au-pg">
      <p className="sns-wait" role="status">로그인하는 중</p>
    </div>
  )
}
