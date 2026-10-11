import type { ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Icon } from '../layout/Icon'
import { serviceHref } from '../layout/menu'
import { FREE_BENEFITS } from '../../data/homeContent'
import { usePricing, formatWon, MEMBER_PRICE_PENDING_LABEL } from '../../hooks/usePricing'
import type { SitePhotoSlot } from '../../hooks/useSitePhoto'
import { SitePhoto } from '../SitePhoto'

/**
 * 로그인·회원가입 오른쪽 첫걸음 상자 (CR-131).
 * 홈 첫걸음 상자와 같은 혜택, 회원 금액, SAM 안내를 보여 주고, 화면마다 다른 단추만 children 으로 받는다.
 */
export function JoinIntro({ image, tag, title, children }: { image: SitePhotoSlot; tag: string; title: ReactNode; children: ReactNode }) {
  const { memberMonthlyPrice: price, loading } = usePricing()
  // 홈과 같이 불러오는 동안은 비워 두고, 금액이 없으면 요금 출처의 안내 문구를 쓴다(CR-129 4.2.1)
  const monthly = loading ? ' ' : price != null ? `월 ${formatWon(price)}` : MEMBER_PRICE_PENDING_LABEL

  return (
    <aside className="intro">
      <div className="im"><SitePhoto slot={image} /><span className="tag">{tag}</span></div>
      <div className="bx">
        <h3>{title}</h3>
        <div className="fr"><b>무료 회원</b><ul>{FREE_BENEFITS.map((t) => <li key={t}><Icon name="ok" />{t}</li>)}</ul></div>
        <div className="up"><span>상세 번역·적격성 판정은 <b>회원</b></span><Link to="/pricing">{monthly}<Icon name="cr" /></Link></div>
        {children}
        <div className="sam"><Icon name="check" /><span>제출 때 SAM 등록이 필요할 수 있습니다</span><Link to={serviceHref('sam-registration')}>등록 대행<Icon name="cr" /></Link></div>
      </div>
    </aside>
  )
}
