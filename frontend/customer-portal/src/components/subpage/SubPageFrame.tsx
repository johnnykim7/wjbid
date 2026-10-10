import { Fragment, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { Icon } from '../layout/Icon'
import { isReady, menuHref, type MenuGroup, type MenuItem } from '../layout/menu'
import { SubIcon } from './SubIcon'
import { SUB_FRAME } from '../../data/subpageContent'
import '../../wjbid-sub.css'

/**
 * 속페이지 틀 (CR-133, T3-3 「속페이지 틀」). 왼쪽 묶음 메뉴와 위치 줄을 그리고 오른쪽에 본문을 둔다.
 * 묶음 메뉴는 메뉴 정의를 그대로 읽어 메뉴 띠·전체메뉴와 같은 목적지로 보낸다.
 */
export function SubPageFrame({ group, item, children }: { group: MenuGroup; item: MenuItem; children: ReactNode }) {
  return (
    <div className="wj sbp">
      <div className="spg">
        <aside className="lnb">
          <h2>{group.label}</h2>
          <ul>
            {group.items.map((i) => (
              <li key={i.key}>
                <Link className={i.key === item.key ? 'on' : undefined} to={menuHref(i)} aria-current={i.key === item.key ? 'page' : undefined}>
                  {i.label}
                  {i.key === item.key ? <Icon name="cr" /> : !isReady(i) && <small>{SUB_FRAME.pendingTag}</small>}
                </Link>
              </li>
            ))}
          </ul>
          <div className="call">
            <b>{SUB_FRAME.call.title}</b>
            <span>{SUB_FRAME.call.desc}</span>
            <Link to="/support">{SUB_FRAME.call.link}<Icon name="cr" /></Link>
          </div>
        </aside>
        <div>
          <nav className="crumb" aria-label="현재 위치">
            <Link to="/"><SubIcon name="home" className="h" />홈</Link>
            <Icon name="cr" />
            <Link to={menuHref(group)}>{group.label}</Link>
            <Icon name="cr" />
            <b>{item.label}</b>
          </nav>
          {children}
        </div>
      </div>
    </div>
  )
}

/** 본문 위 제목 줄 (교육 자료, 용어 사전, 서식 모음, 공지사항, 자주 묻는 질문, 요금 안내) */
export function PageHead({ title, desc }: { title: string; desc: string }) {
  return (
    <div className="ph2">
      <div>
        <h1>{title}</h1>
        <p>{desc}</p>
      </div>
    </div>
  )
}

/** 아래 신청 띠. 채운 단추는 화면마다 하나라 이 띠의 단추가 머리 단추와 같은 곳으로 간다 */
export function ApplyBand({ icon, title, desc, action }: { icon: 'headset' | 'cal'; title: string; desc: ReactNode; action: { label: string; to: string } }) {
  return (
    <div className="apply">
      <div className="icn"><Icon name={icon} /></div>
      <div className="xtx">
        <b>{title}</b>
        <span>{desc}</span>
      </div>
      <Link className="bigb" to={action.to}>{action.label}</Link>
    </div>
  )
}

/** 정본에서 줄을 나눠 쓴 글. 줄마다 끊어 보인다 */
export function Lines({ lines }: { lines: string[] }) {
  return <>{lines.map((l, i) => <Fragment key={i}>{i > 0 && <br />}{l}</Fragment>)}</>
}
