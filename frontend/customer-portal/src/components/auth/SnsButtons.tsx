import { useEffect, useState } from 'react'
import { Button } from '@therecommerce-develop/ds-ui'
import { getSnsProviders, snsStartUrl, type SnsProviderItem } from '../../api/client'
import { SNS_NAME, SNS_RETURN_KEY } from './authRules'

/** 네이버, 카카오 로그인 단추 안내서의 표시를 단순하게 옮긴 것 */
function SnsMark({ provider }: { provider: string }) {
  if (provider === 'NAVER') {
    return <svg viewBox="0 0 20 20" aria-hidden="true"><path d="M4 3h4.2l3.6 5.6V3H16v14h-4.2L8.2 11.4V17H4z" fill="currentColor" /></svg>
  }
  return <svg viewBox="0 0 20 20" aria-hidden="true"><path d="M10 2.5c-4.7 0-8.5 3-8.5 6.6 0 2.3 1.5 4.4 3.9 5.5l-1 3.6c-.1.3.3.6.6.4l4.2-2.8c.3 0 .5.1.8.1 4.7 0 8.5-3 8.5-6.8S14.7 2.5 10 2.5z" fill="currentColor" /></svg>
}

/**
 * 키가 설정된 SNS 단추 (CR-146). 하나도 없거나 목록을 못 받으면 구분선까지 통째로 숨긴다.
 * 로그인은 로그인 단추 아래 "또는" 뒤에, 회원가입은 가입 상자 맨 위에 둔다.
 */
export function SnsButtons({ place, returnTo }: { place: 'login' | 'register'; returnTo?: string }) {
  const [providers, setProviders] = useState<SnsProviderItem[]>([])

  useEffect(() => {
    let alive = true
    getSnsProviders()
      .then(({ data }) => { if (alive) setProviders(data.providers.filter((p) => SNS_NAME[p.provider])) })
      .catch(() => { if (alive) setProviders([]) })
    return () => { alive = false }
  }, [])

  if (providers.length === 0) return null

  const remember = () => {
    try {
      if (returnTo?.startsWith('/')) sessionStorage.setItem(SNS_RETURN_KEY, returnTo)
      else sessionStorage.removeItem(SNS_RETURN_KEY)
    } catch {
      // 저장소를 못 쓰면 로그인 뒤 공고 검색으로 간다
    }
  }

  const buttons = (
    <div className="sns">
      {providers.map((p) => (
        <Button key={p.provider} asChild className={`wj-reset snsb ${p.provider.toLowerCase()}`}>
          <a href={snsStartUrl(p.startPath)} onClick={remember}>
            <SnsMark provider={p.provider} />
            {SNS_NAME[p.provider]}로 시작하기
          </a>
        </Button>
      ))}
    </div>
  )

  if (place === 'login') {
    return (
      <>
        <div className="or"><span>또는</span></div>
        {buttons}
      </>
    )
  }
  return (
    <div className="snsq">
      <p className="snst">SNS 계정으로 가입</p>
      {buttons}
      <div className="or"><span>또는 이메일로 가입</span></div>
    </div>
  )
}
