import { Link } from 'react-router-dom'
import { COMPANY, COPYRIGHT, FOOTER_LINKS } from './siteContent'

const filled = (v?: string) => (v && !/^\(.*\)$/.test(v.trim()) ? v.trim() : undefined)

/** 공통 틀 아래쪽 꼬리말 (CR-127). 정책 링크, 사업자 정보, 저작권 줄 */
export function AppFooter() {
  const bizItems = [
    filled(COMPANY.name) && `상호 ${COMPANY.name}`,
    filled(COMPANY.ceo) && `대표 ${COMPANY.ceo}`,
    filled(COMPANY.businessNumber) && `사업자등록번호 ${COMPANY.businessNumber}`,
    filled(COMPANY.address) && `주소 ${COMPANY.address}`,
    filled(COMPANY.email) && `이메일 ${COMPANY.email}`,
  ].filter(Boolean) as string[]

  return (
    <footer className="wj">
      <div className="wrap">
        <div className="lk">
          {FOOTER_LINKS.map((l) => (
            <Link key={l.label} to={l.to}>{l.label}</Link>
          ))}
        </div>
        <div className="info">
          <div>
            {bizItems.map((t) => (
              <span key={t}>{t}</span>
            ))}
          </div>
          <div>{COPYRIGHT}</div>
        </div>
      </div>
    </footer>
  )
}
