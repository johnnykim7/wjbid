import { Link } from 'react-router-dom'
import { SubIcon } from '../subpage/SubIcon'
import { CONSULT_PATH, MEMBERSHIP_TEXT, PRICING_PATH } from '../../data/membershipContent'
import '../../wjbid-eligibility.css'

/** 무료회원에게 보이는 회원 안내 (CR-157, T3-3 「회원 전용 화면」). 로그인 안내 상자와 같은 모양이다 */
export function MemberOnlyBox({ title, desc }: { title: string; desc: string }) {
  return (
    <section className="cbx">
      <div className="eq-gbox">
        <div className="empty2"><div className="icn"><SubIcon name="user" /></div></div>
        <b>{title}</b>
        <span>{desc}</span>
        <div className="eq-bb">
          <Link className="eq-obtn eq-p" to={PRICING_PATH}>{MEMBERSHIP_TEXT.pricing}</Link>
          <Link className="eq-obtn" to={CONSULT_PATH}>{MEMBERSHIP_TEXT.consult}</Link>
        </div>
      </div>
    </section>
  )
}
