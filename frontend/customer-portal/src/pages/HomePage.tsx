import { Fragment, useEffect, useState } from 'react'
import { Link, useOutletContext } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'
import { getNearDeadline, getOpportunities } from '../api/client'
import { useAuth } from '../context/AuthContext'
import { MEMBER_PRICE_PENDING_LABEL, formatWon, usePricing } from '../hooks/usePricing'
import { Icon } from '../components/layout/Icon'
import { comingSoonHref, findMenuItem, menuHref, serviceHref } from '../components/layout/menu'
import type { MemberSummary } from '../components/layout/useMemberSummary'
import { daysUntil, ddayLabel } from '../utils/dday'
import type { Opportunity } from '../types'
import {
  BENEFIT_FIT,
  BENEFIT_TRANSLATE,
  FLOW,
  FREE_BENEFITS,
  HELP_LINKS,
  IMAGES,
  LIST_ROWS,
  PLAN_CONSULT,
  PLAN_FREE,
  PLAN_MEMBER,
  PROMOS,
  RELATED_SITES,
  SAMPLE_NOTICE,
  SERVICE_MAP,
  SOON_DAYS,
  TIER_LABEL,
  TIER_NOTE,
  type Tier,
} from '../data/homeContent'
import { FAQS, HOME_FAQ_COUNT, NOTICES, NOTICE_TEXT } from '../data/supportContent'

/** 도움 구역은 자주 묻는 질문·공지사항 화면과 같은 목록을 읽는다 (CR-133) */
const FAQ_TITLES = FAQS.slice(0, HOME_FAQ_COUNT).map((f) => f.q)
const NOTICE_TITLES = NOTICES.map((n) => n.title)

/** 첫 화면의 대표 문장. 큰 제목이 두 곳에 나뉘면 눈이 멈출 곳이 없어 한 번에 한 자리에만 둔다 (CR-134) */
const HEADLINE = <>주한미군 공사·용역 입찰,<br />한글로 찾고 순서대로 준비하세요</>

/*
 * 홈 (CR-127). 화면 정본 홈시안_파랑.html 의 #s0(공고 0건), #s1(공고 있음), #s2(회원)를 한 화면에서 상태로 가른다.
 * 클래스 이름은 정본과 같게 두어 wjbid-aida.css 규칙이 그대로 걸린다.
 * 맞춤 판정은 기능이 없어 같은 자리에 준비 중 안내를 둔다(사용자 2026-10-10 18:50 승인).
 */

const WEEKDAYS = ['일', '월', '화', '수', '목', '금', '토']
const pad = (n: number) => String(n).padStart(2, '0')

/** 홈 링크도 메뉴 정의를 따른다. 화면이 생기면 준비 안내 대신 그 화면으로 간다 */
const itemHref = (groupKey: string, itemKey: string) => {
  const found = findMenuItem(groupKey, itemKey)
  return found ? menuHref(found.item) : comingSoonHref(itemKey)
}

/** "2026-10-13" 을 "10.13" 과 요일로. 날짜 문자열을 그대로 읽어 브라우저 시간대에 흔들리지 않게 한다 */
function deadlineParts(s?: string | null) {
  const [y, m, d] = (s ?? '').slice(0, 10).split('-').map(Number)
  if (!y || !m || !d) return null
  return { md: `${pad(m)}.${pad(d)}`, dow: WEEKDAYS[new Date(Date.UTC(y, m - 1, d)).getUTCDay()] }
}

function todayMd() {
  const t = new Date()
  return `${pad(t.getMonth() + 1)}.${pad(t.getDate())}`
}

function BidRow({ o, member }: { o: Opportunity; member: boolean }) {
  const days = daysUntil(o.responseDeadline)
  const dl = deadlineParts(o.responseDeadline)
  return (
    <li>
      <span className={`dd ${days !== null && days <= SOON_DAYS ? 'u' : 'n'}`}>{ddayLabel(days)}</span>
      <Link className="t" to={`/search/${o.id}`}>
        <b>{o.title}</b>
        <span>
          <i className="mono">{o.solicitationNumber}</i>
          {member && <em className="fit no">판정 준비 중</em>}
        </span>
      </Link>
      <span>{o.type && <span className="bd r">{o.type}</span>}</span>
      <span className="loc">{o.placeOfPerformance && <b><Icon name="pin" />{o.placeOfPerformance}</b>}</span>
      <span className="dl">{dl && <>{dl.md}<small>({dl.dow})</small></>}</span>
    </li>
  )
}

export default function HomePage() {
  const { isAuthenticated } = useAuth()
  const member = useOutletContext<MemberSummary>()
  const { memberMonthlyPrice: price, loading: priceLoading } = usePricing()

  const [loaded, setLoaded] = useState(false)
  const [failed, setFailed] = useState(false)
  const [total, setTotal] = useState(0)
  const [latest, setLatest] = useState<Opportunity[]>([])
  const [soon, setSoon] = useState<Opportunity[]>([])
  const [tab, setTab] = useState<'soon' | 'latest'>('soon')
  const [helpTab, setHelpTab] = useState<'faq' | 'notice'>('faq')

  useEffect(() => {
    let alive = true
    Promise.allSettled([getOpportunities(0, LIST_ROWS), getNearDeadline(SOON_DAYS)]).then(([all, near]) => {
      if (!alive) return
      if (all.status === 'fulfilled') {
        setLatest(all.value.data.content ?? [])
        setTotal(all.value.data.totalElements ?? 0)
      } else {
        setFailed(true)
      }
      if (near.status === 'fulfilled') setSoon(near.value.data ?? [])
      setLoaded(true)
    })
    return () => {
      alive = false
    }
  }, [])

  // 금액 자리. 불러오는 동안은 비워 두고, 금액이 없으면 요금 출처의 안내 문구를 쓴다(CR-129 4.2.1)
  const monthly = priceLoading ? ' ' : price != null ? `월 ${formatWon(price)}` : MEMBER_PRICE_PENDING_LABEL
  const rows = tab === 'soon' ? soon.slice(0, LIST_ROWS) : latest
  const showEmpty = loaded && !failed && total === 0
  const countText = (n: number | null) => (n == null ? '-' : n)

  const benefits = (
    <ul>
      {FREE_BENEFITS.map((t) => <li key={t}><Icon name="ok" />{t}</li>)}
    </ul>
  )
  const start = (
    <>
      <Link className="bigb" to="/register">무료로 시작</Link>
      <div className="lg">이미 회원이신가요?<Link to="/login">로그인</Link></div>
    </>
  )

  const left = showEmpty ? (
    <div className="st">
      <div className="ph">
        <h3>마감 전 공고</h3>
        <span className="cnt">공개된 한글 공고문 0건 · 준비 중 · {todayMd()} 기준</span>
      </div>
      <div className="empty">
        <div className="tx">
          <h3>{HEADLINE}</h3>
          <Link className="smp" to={itemHref('bids', 'notice-sample')}>
            <small>{SAMPLE_NOTICE.caption}</small>
            <b>{SAMPLE_NOTICE.title}</b>
            <i className="mono">{SAMPLE_NOTICE.number}</i>
            <dl>
              {SAMPLE_NOTICE.facts.map((f) => (
                <div key={f.label}><dt>{f.label}</dt><dd>{f.value}</dd></div>
              ))}
            </dl>
          </Link>
        </div>
        <img src={IMAGES.empty} alt="" />
      </div>
    </div>
  ) : (
    <div className="st">
      <section className="lstw">
        <div className="ph">
          <h3>마감 전 공고</h3>
          <div className="tabs">
            <Button className={`wj-reset${tab === 'soon' ? ' on' : ''}`} onClick={() => setTab('soon')}>마감 임박</Button>
            <Button className={`wj-reset${tab === 'latest' ? ' on' : ''}`} onClick={() => setTab('latest')}>최신 공고</Button>
          </div>
          {loaded && !failed && (
            <span className="cnt">{total}건 · {SOON_DAYS}일 안 마감 <b>{soon.length}건</b></span>
          )}
        </div>
        <ul className="bids">
          {rows.map((o) => <BidRow key={o.id} o={o} member={isAuthenticated} />)}
          {loaded && rows.length === 0 && (
            <li className="none">
              {failed ? '공고를 불러오지 못했습니다. 잠시 뒤 다시 열어 주세요.' : `${SOON_DAYS}일 안에 마감하는 공고가 없습니다. 최신 공고를 보세요.`}
            </li>
          )}
        </ul>
        {loaded && !failed && (
          <div className="lft">
            <span>한글 공고문 · SAM.gov 원문 함께 보기</span>
            <Link to="/search">마감 전 공고 {total}건 모두 보기<Icon name="cr" /></Link>
          </div>
        )}
      </section>
    </div>
  )

  const right = isAuthenticated ? (
    <div className="st side">
      <div className="today me">
        <h4>{member.companyName ?? ''} 님<span>회원 · 오늘 확인할 것</span></h4>
        <div className="g">
          <Link to="/bookmarks"><b>{countText(member.bookmarkCount)}</b><span>관심 공고</span></Link>
          <Link to="/proposals"><b>{countText(member.bidRequestCount)}</b><span>입찰 신청</span></Link>
          <Link to="/notifications"><b className="r">{member.unreadCount}</b><span>새 알림</span></Link>
        </div>
      </div>
      <div className="mine">
        <h4>우리 회사 맞춤 판정<Link to="/profile">회사 정보 고치기</Link></h4>
        <div className="fitwait">
          <span className="tag">준비 중</span>
          <p>공고마다 우리 회사가 참여할 수 있는지, 무엇이 모자란지 판정하는 기능을 준비하고 있습니다.</p>
          <p>회사 정보를 미리 넣어 두시면 기능이 열릴 때 바로 판정해 드립니다.</p>
          <Link to="/profile">회사 정보 넣기<Icon name="cr" /></Link>
        </div>
      </div>
      <div className="qk">
        <Link to="/documents"><span className="fi"><Icon name="folder" /></span>서류함</Link>
      </div>
    </div>
  ) : showEmpty ? (
    // 공고 0건이면 대표 문장은 왼쪽이 가져가고, 여기는 왼쪽과 같은 머리 아래 사진과 가입·로그인만 남긴다.
    // 사진이 남는 높이를 채워 글을 늘리지 않고도 상자가 비어 보이지 않게 한다 (CR-134)
    <div className="st">
      <div className="ph"><h3>무료 회원</h3></div>
      <div className="intro join">
        <div className="im"><img src={IMAGES.intro} alt="" /><span className="tag">USFK 입찰 첫걸음</span></div>
        <div className="bx">
          <div className="fr">{benefits}</div>
          {start}
        </div>
      </div>
    </div>
  ) : (
    <div className="st side">
      <div className="intro">
        <div className="im"><img src={IMAGES.intro} alt="" /><span className="tag">USFK 입찰 첫걸음</span></div>
        <div className="bx">
          <h3>{HEADLINE}</h3>
          <div className="fr"><b>무료 회원</b>{benefits}</div>
          <div className="up">
            <span>상세 번역·적격성 판정은 <b>회원</b></span>
            <Link to="/pricing">{monthly}<Icon name="cr" /></Link>
          </div>
          {start}
          <div className="sam">
            <Icon name="check" />
            <span>제출 때 SAM 등록이 필요할 수 있습니다</span>
            <Link to={serviceHref('sam-registration')}>등록 대행<Icon name="cr" /></Link>
          </div>
        </div>
      </div>
    </div>
  )

  return (
    <div className="wj home">
      <div className="wrap">
        <div className="fs">
          <div>{left}</div>
          <div>{right}</div>
        </div>
        <div className="flow">
          {FLOW.map((s) => (
            <a key={s.no} href="#svc">
              <span><b><em>{s.no}</em>{s.name}</b><span>{s.desc}</span></span>
            </a>
          ))}
        </div>
      </div>

      <section className="band g" id="svc">
        <div className="wrap">
          <div className="bh"><small>서비스</small><h2>입찰하는 순서대로 도와 드립니다</h2><p>찾기부터 낙찰 뒤까지, 단계마다 필요한 것만 고르세요</p></div>
          <div className="smap">
            {SERVICE_MAP.map((c) => (
              <div className="sc" key={c.no}>
                <div className="hd2"><i>{c.no}</i><b>{c.name}</b></div>
                <p>{c.desc}</p>
                <ul>
                  {c.items.map((it) => (
                    <li key={it.name}>
                      {it.name}<br />
                      {it.tiers.map((t) => <Fragment key={t}><span className={`tier ${t}`}>{TIER_LABEL[t]}</span> </Fragment>)}
                    </li>
                  ))}
                </ul>
              </div>
            ))}
          </div>
          <div className="legend2">
            {(['f', 'm', 'c', 'k'] as Tier[]).map((t) => (
              <span key={t}><span className={`tier ${t}`}>{TIER_LABEL[t]}</span>{t === 'm' ? monthly : TIER_NOTE[t]}</span>
            ))}
          </div>
        </div>
      </section>

      <section className="band">
        <div className="wrap">
          <div className="bh">
            <small>회원 혜택</small>
            <h2>회원은 한 걸음 더 봅니다</h2>
            <p>{price != null ? `월 ${formatWon(price)}으로 ` : ''}공고를 끝까지 읽고, 할 수 있는 공고만 고릅니다</p>
          </div>
          <div className="b2">
            <div className="fc">
              <h3>{BENEFIT_TRANSLATE.title}</h3>
              <p>{BENEFIT_TRANSLATE.desc}</p>
              <div className="pv">
                <div className="cmp">
                  <div>
                    <small>{BENEFIT_TRANSLATE.free.caption}</small>
                    <p>{BENEFIT_TRANSLATE.free.title}</p>
                    <p className="sub">{BENEFIT_TRANSLATE.free.sub}</p>
                  </div>
                  <div className="m">
                    <small>{BENEFIT_TRANSLATE.member.caption}</small>
                    <ul>{BENEFIT_TRANSLATE.member.items.map((t) => <li key={t}>{t}</li>)}</ul>
                  </div>
                </div>
              </div>
              <Link className="go" to={itemHref('bids', 'notice-sample')}>{BENEFIT_TRANSLATE.link.label}<Icon name="cr" /></Link>
            </div>
            <div className="fc">
              <h3>{BENEFIT_FIT.title}</h3>
              <p>{BENEFIT_FIT.desc}</p>
              <div className="pv">
                <div className="co">{BENEFIT_FIT.company.map((t) => <span key={t}>{t}</span>)}</div>
                <div className="rs2">
                  {BENEFIT_FIT.rows.map((r) => (
                    <div key={r.title}><b>{r.title}</b><span className={`fit ${r.fit}`}>{r.label}</span></div>
                  ))}
                </div>
                <p className="ex">{BENEFIT_FIT.note}</p>
              </div>
              <Link className="go" to={BENEFIT_FIT.link.to}>{BENEFIT_FIT.link.label}<Icon name="cr" /></Link>
            </div>
          </div>
        </div>
      </section>

      <section className="band g">
        <div className="wrap">
          <div className="bh"><small>요금</small><h2>무료로 시작하고, 필요할 때 넓히세요</h2><p>회원은 월 단위, 대행과 컨설팅은 건마다 상담합니다</p></div>
          <div className="plans">
            <div className="pc">
              <h4>{PLAN_FREE.name}</h4>
              <div className="pr">0<small>원</small></div>
              <div className="who">{PLAN_FREE.who}</div>
              <ul>{PLAN_FREE.items.map((t) => <li key={t}><Icon name="ok" />{t}</li>)}</ul>
              <Link className="bigb o" to={PLAN_FREE.cta.to}>{PLAN_FREE.cta.label}</Link>
            </div>
            <div className="pc on">
              <span className="rec">추천</span>
              <h4>{PLAN_MEMBER.name}</h4>
              {price != null ? (
                <div className="pr">{price.toLocaleString('ko-KR')}<small>원/월</small></div>
              ) : (
                <div className="pr s">{priceLoading ? ' ' : MEMBER_PRICE_PENDING_LABEL}</div>
              )}
              <div className="who">{PLAN_MEMBER.who}</div>
              <ul>{PLAN_MEMBER.items.map((t) => <li key={t}><Icon name="ok" />{t}</li>)}</ul>
              <Link className="bigb" to={PLAN_MEMBER.cta.to}>{PLAN_MEMBER.cta.label}</Link>
            </div>
            <div className="pc">
              <h4>{PLAN_CONSULT.name}</h4>
              <div className="pr s">{PLAN_CONSULT.price}</div>
              <div className="who">{PLAN_CONSULT.who}</div>
              <ul>{PLAN_CONSULT.items.map((t) => <li key={t}><Icon name="ok" />{t}</li>)}</ul>
              <Link className="bigb o" to={PLAN_CONSULT.cta.to}>{PLAN_CONSULT.cta.label}</Link>
            </div>
          </div>
          <div className="promos2">
            {PROMOS.map((p) => (
              <Link key={p.small} className={`promo ${p.tone}`} to={p.to}>
                <img src={p.img} alt="" />
                <div className="tx"><small>{p.small}</small><b>{p.title}</b><span>{p.desc}</span></div>
              </Link>
            ))}
          </div>
        </div>
      </section>

      <section className="band last">
        <div className="wrap">
          <div className="b2">
            <div className="tbox">
              <div className="tt">
                <Button className={`wj-reset${helpTab === 'faq' ? ' on' : ''}`} onClick={() => setHelpTab('faq')}>자주 묻는 질문</Button>
                <Button className={`wj-reset${helpTab === 'notice' ? ' on' : ''}`} onClick={() => setHelpTab('notice')}>공지사항</Button>
                <Link className="more" to={helpTab === 'faq' ? '/faq' : '/notices'}>더 보기<Icon name="cr" /></Link>
              </div>
              <ul className="lst">
                {helpTab === 'notice' && NOTICE_TITLES.length === 0 && (
                  <li><span className="t">{NOTICE_TEXT.emptyTitle}</span></li>
                )}
                {(helpTab === 'faq' ? FAQ_TITLES : NOTICE_TITLES).map((t) => (
                  <li key={t}>
                    {helpTab === 'faq' && <span className="q">Q</span>}
                    <Link className="t" to={helpTab === 'faq' ? '/faq' : '/notices'}>{t}</Link>
                  </li>
                ))}
              </ul>
            </div>
            <div className="help">
              {HELP_LINKS.map((h) => (
                <Link key={h.title} to={h.to}>
                  <span className="fi"><Icon name={h.icon} /></span>
                  <b>{h.title}<span>{h.desc}</span></b>
                  <Icon name="cr" />
                </Link>
              ))}
            </div>
          </div>
          <div className="rel">
            <h4>관련 사이트</h4>
            {RELATED_SITES.map((s) => (
              <a key={s.label} href={s.href} target="_blank" rel="noopener noreferrer">{s.label}<Icon name="ext" /></a>
            ))}
          </div>
        </div>
      </section>
    </div>
  )
}
