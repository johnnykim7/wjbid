import { Fragment, useEffect, useRef } from 'react'
import { Link, Navigate, useSearchParams } from 'react-router-dom'
import { Icon } from '../components/layout/Icon'
import { findMenuItem } from '../components/layout/menu'
import { SubIcon } from '../components/subpage/SubIcon'
import { PageHead, SubPageFrame } from '../components/subpage/SubPageFrame'
import { ARTICLES, MATERIALS_EDGE, MATERIALS_HEAD, MATERIALS_STEP_PARAM, prevStepLabel } from '../data/academyContent'

const PLACE = findMenuItem('academy', 'materials')

const stepHref = (n: number) => `?${MATERIALS_STEP_PARAM}=${n}`

/** 주소의 단계 값. 없거나 범위 밖이면 1단계 */
function readStep(raw: string | null) {
  const n = Number(raw)
  return Number.isInteger(n) && n >= 1 && n <= ARTICLES.length ? n : 1
}

/** 교육 자료 (CR-133, T3-3 「교육 자료」, 화면 정본 guide). 다섯 단계를 주소 ?step=n 으로 옮겨 다닌다 */
export default function AcademyMaterialsPage() {
  const [params] = useSearchParams()
  const step = readStep(params.get(MATERIALS_STEP_PARAM))
  const first = useRef(true)

  // 단계를 바꾸면 단계 탭이 보이도록 맨 위로. 첫 진입은 공통 틀이 이미 올려 준다
  useEffect(() => {
    if (first.current) {
      first.current = false
      return
    }
    window.scrollTo(0, 0)
  }, [step])

  if (!PLACE) return <Navigate to="/" replace />
  const { group, item } = PLACE
  const a = ARTICLES[step - 1]
  const prev = step > 1
    ? { label: prevStepLabel(step - 1, ARTICLES[step - 2].name), to: stepHref(step - 1) }
    : MATERIALS_EDGE.first
  const next = step < ARTICLES.length
    ? { label: ARTICLES[step].goLabel, to: stepHref(step + 1) }
    : MATERIALS_EDGE.last

  return (
    <SubPageFrame group={group} item={item}>
      <PageHead title={MATERIALS_HEAD.title} desc={MATERIALS_HEAD.desc} />
      <nav className="steptab">
        {ARTICLES.map((s, i) => (
          <Link key={s.name} className={i + 1 === step ? 'on' : undefined} to={stepHref(i + 1)} aria-current={i + 1 === step ? 'step' : undefined}>
            <i>{i + 1}</i>{s.name}
          </Link>
        ))}
      </nav>
      <section className="cbx pad art">
        <h3>{a.title}</h3>
        <p className="ld">{a.lead}</p>
        {a.sections.map((s) => (
          <Fragment key={s.title}>
            <h4>{s.title}</h4>
            {s.body && <p className="b">{s.body}</p>}
            {s.table && (
              <table className="tbl">
                <thead><tr><th>{s.table.head[0]}</th><th>{s.table.head[1]}</th></tr></thead>
                <tbody>
                  {s.table.rows.map((r) => (
                    <tr key={r.name}>
                      <td><b>{r.name}</b>{r.en && <span className="eng">{r.en}</span>}</td>
                      <td>{r.desc}</td>
                    </tr>
                  ))}
                </tbody>
              </table>
            )}
          </Fragment>
        ))}
        <div className="xnote">
          <SubIcon name={a.note.icon} />
          <span>{a.note.text} <Link to={a.note.link.to}>{a.note.link.label}</Link></span>
        </div>
        <div className="nxt">
          <Link to={prev.to}>{prev.label}</Link>
          <Link className="gogo" to={next.to}>{next.label}<Icon name="cr" /></Link>
        </div>
      </section>
    </SubPageFrame>
  )
}
