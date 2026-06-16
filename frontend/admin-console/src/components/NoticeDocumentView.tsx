/**
 * NoticeDocumentView — CR-021 / CR-114 재설계
 *
 * 공고문(Notice) 본문을 PDF 양식 계열로 렌더링.
 *
 * CR-114: contentJson(LLM이 골격을 채운 TipTap JSON) 의존 제거.
 *  - contentJson 에 들어갈 값은 전부 summary/requiredDocuments 의 재배열일 뿐(새 정보 0).
 *  - LLM 에게 골격 치환을 시키면 무거워서 null 로 회피(실측) → 화면이 데이터로 직접 그린다.
 *  - 노드 컴포넌트(KvTable/DataTable/CalloutList/GroupedList/Heading...)는 그대로 재사용,
 *    메인이 summary/requiredDocuments 로 섹션을 결정론적으로 조립.
 *
 * 디자인: 네이비 정부문서 계열.
 */

type Node = {
  type: string
  attrs?: Record<string, unknown>
  content?: Node[]
  text?: string
}

// CR-033: 자격요건 정밀추출 항목
export interface EligibilityItem {
  title: string
  description?: string
  mandatory?: boolean
  evidenceBy?: string
  isGate?: boolean
  sourceRef?: string
}

// CR-114: 화면이 직접 받는 분석 데이터 (contentJson 대체)
export interface NoticeSummary {
  overview?: string
  scope?: string
  eligibility?: string
  evaluationCriteria?: string
  budgetInfo?: string
  keyDates?: { label?: string; date?: string; note?: string }[]
  specialNotes?: string[]
  contactInfo?: { name?: string; role?: string; email?: string; phone?: string; organization?: string }[]
}

export interface RequiredDocumentItem {
  name: string
  description?: string
  mandatory?: boolean
  format?: string
  pageLimit?: string
  notes?: string
}

interface Props {
  // CR-114: 데이터 직접 렌더 (주 경로)
  koreanTitle?: string | null
  summary?: NoticeSummary | null
  documents?: RequiredDocumentItem[]
  solicitationNumber?: string | null
  organizationName?: string | null
  // CR-033: 정밀추출 자격요건 — "참여 자격요건" 섹션
  eligibility?: EligibilityItem[]
  // 공고 게시일(SAM 수집 확정값)
  postedDate?: string | null
}

const NAVY = 'bg-slate-800 text-white'
const NAVY_DARK = 'bg-slate-900 text-white'
const SECTION_BORDER = 'border-slate-200'

export default function NoticeDocumentView({
  koreanTitle,
  summary,
  documents,
  solicitationNumber,
  organizationName,
  eligibility,
  postedDate,
}: Props) {
  // CR-114: 분석 데이터가 전혀 없으면 안내 (COMPLETED 인데 summary 빈 경우)
  if (!summary && (!documents || documents.length === 0) && (!eligibility || eligibility.length === 0)) {
    return (
      <div className="rounded-lg bg-yellow-50 border border-yellow-200 px-4 py-3 text-sm text-yellow-800">
        공고문 분석 결과가 아직 없습니다. 한글화/분석을 다시 실행해 주세요.
      </div>
    )
  }

  const hasElig = !!eligibility && eligibility.length > 0
  const issuedDate = fmtDateKo(postedDate) || str(postedDate)

  // CR-114: 기본 정보 kvTable 행 — summary/메타에서 직접 구성 (발행일 끼움)
  const basicRows = withIssuedDate(
    [
      solicitationNumber ? { label: '공고번호', value: str(solicitationNumber) } : null,
      summary?.budgetInfo ? { label: '예상 금액', value: str(summary.budgetInfo) } : null,
      summary?.evaluationCriteria ? { label: '평가 방식', value: str(summary.evaluationCriteria) } : null,
    ].filter(Boolean) as { label: string; value: string }[],
    issuedDate,
  )

  // 주요 일정 dataTable (label/date/note → 행)
  const timelineRows = (summary?.keyDates ?? [])
    .map((d) => [str(d.label), [str(d.date), str(d.note)].filter(Boolean).join(' / ')])
    .filter((r) => r[0] || r[1])

  // 연락처 kvTable
  const contactRows = (summary?.contactInfo ?? [])
    .map((c) => ({
      label: [str(c.name), str(c.role)].filter(Boolean).join(' · ') || '담당자',
      value: [str(c.email), str(c.phone), str(c.organization)].filter(Boolean).join(' / '),
    }))
    .filter((r) => r.value)

  // 제출 서류 dataTable
  const docHeaders = ['서류', '필수', '형식', '비고']
  const docRows = (documents ?? []).map((d) => [
    str(d.name),
    d.mandatory ? '필수' : '선택',
    str(d.format),
    [str(d.description), str(d.notes)].filter(Boolean).join(' / '),
  ])

  return (
    <article className="bg-white">
      {/* 헤더 */}
      <NoticeHeader
        attrs={{
          title: str(koreanTitle),
          organization: str(organizationName),
          solicitationNumber: str(solicitationNumber),
          issuedDate,
        }}
      />

      {/* 1. 공고 기본 정보 */}
      {basicRows.length > 0 && (
        <>
          <Heading level={2} content={[{ type: 'text', text: '공고 기본 정보' }]} />
          <KvTable rows={basicRows} />
        </>
      )}

      {/* 2. 개요 */}
      {summary?.overview && (
        <>
          <Heading level={2} content={[{ type: 'text', text: '개요' }]} />
          <Paragraph content={[{ type: 'text', text: str(summary.overview) }]} />
        </>
      )}

      {/* 3. 작업 범위 */}
      {summary?.scope && (
        <>
          <Heading level={2} content={[{ type: 'text', text: '작업 범위' }]} />
          <Paragraph content={[{ type: 'text', text: str(summary.scope) }]} />
        </>
      )}

      {/* 4. 주요 일정 */}
      {timelineRows.length > 0 && (
        <>
          <Heading level={2} content={[{ type: 'text', text: '주요 일정' }]} />
          <DataTable headers={['구분', '일정']} rows={timelineRows} />
        </>
      )}

      {/* 5. 참여 자격요건 (CR-033 정밀추출) */}
      {hasElig && (
        <>
          <Heading level={2} content={[{ type: 'text', text: '참여 자격요건' }]} />
          <EligibilityBlock items={eligibility!} />
        </>
      )}

      {/* 6. 제출 서류 */}
      {docRows.length > 0 && (
        <>
          <Heading level={2} content={[{ type: 'text', text: '제출 서류' }]} />
          <DataTable headers={docHeaders} rows={docRows} />
        </>
      )}

      {/* 7. 담당자 */}
      {contactRows.length > 0 && (
        <>
          <Heading level={2} content={[{ type: 'text', text: '담당자 (POC)' }]} />
          <KvTable rows={contactRows} />
        </>
      )}

      {/* 8. 특이사항 */}
      {summary?.specialNotes && summary.specialNotes.length > 0 && (
        <>
          <Heading level={2} content={[{ type: 'text', text: '특이사항' }]} />
          <CalloutList items={summary.specialNotes} tone="warning" />
        </>
      )}
    </article>
  )
}

// CR-033: 자격요건 정밀추출 렌더 (본문 §자격요건 자리에 인라인)
function EligibilityBlock({ items }: { items: EligibilityItem[] }) {
  return (
    <div className="space-y-2 mb-4">
      {items.map((e, i) => (
        <div key={i} className="flex items-start gap-3 p-3 bg-white border border-slate-200 rounded-md">
          <i className={`fa-solid ${e.isGate ? 'fa-shield-halved text-amber-500' : 'fa-circle-check text-slate-300'} text-lg mt-0.5`} />
          <div className="flex-1 text-sm">
            <div className="flex items-center gap-2 mb-0.5 flex-wrap">
              <span className="font-semibold text-slate-900">{e.title}</span>
              {e.mandatory ? (
                <span className="px-1.5 py-0.5 bg-red-50 text-red-600 rounded text-[10px] font-semibold uppercase">필수</span>
              ) : (
                <span className="px-1.5 py-0.5 bg-slate-100 text-slate-500 rounded text-[10px] font-semibold uppercase">선택</span>
              )}
              {e.isGate && (
                <span className="px-1.5 py-0.5 bg-amber-50 text-amber-600 rounded text-[10px] font-semibold">미충족 시 부적격</span>
              )}
            </div>
            {e.description && <p className="text-slate-600 mb-0.5">{e.description}</p>}
            {e.evidenceBy && (
              <p className="text-xs text-emerald-600 mt-1"><i className="fa-solid fa-link mr-1" />증빙 서류: {e.evidenceBy}</p>
            )}
            {e.sourceRef && <p className="text-[11px] text-slate-400 mt-1"><i className="fa-solid fa-quote-left mr-1" />{e.sourceRef}</p>}
          </div>
        </div>
      ))}
    </div>
  )
}

// ── 커스텀 노드 렌더러 (CR-114: 메인이 데이터로 직접 조립해 호출) ──────────

function NoticeHeader({ attrs }: { attrs: Record<string, unknown> }) {
  const title = str(attrs.title)
  const subtitle = str(attrs.subtitle)
  const organization = str(attrs.organization)
  const solNo = str(attrs.solicitationNumber)
  const issuedDate = str(attrs.issuedDate)

  return (
    <div className={`${NAVY_DARK} rounded-t-lg px-6 py-5 mb-0`}>
      <h1 className="text-xl font-bold leading-snug">{title}</h1>
      {subtitle && <div className="text-sm text-slate-300 mt-1">{subtitle}</div>}
      {organization && <div className="text-sm text-slate-300 mt-0.5">{organization}</div>}
      <div className="flex gap-4 mt-3 text-xs text-slate-200">
        {solNo && <span>Solicitation: <span className="font-mono">{solNo}</span></span>}
        {issuedDate && <span>발행일: {issuedDate}</span>}
      </div>
    </div>
  )
}

function Heading({ level, content }: { level: number; content: Node[] }) {
  const text = collectText(content)
  if (level === 2) {
    return (
      <h2 className="text-base font-bold text-slate-900 mt-6 mb-3 pb-1.5 border-b-2 border-slate-800">
        {text}
      </h2>
    )
  }
  if (level === 3) {
    return <h3 className="text-sm font-semibold text-slate-700 mt-4 mb-2">{text}</h3>
  }
  return <h4 className="text-sm font-medium text-slate-700 mt-3 mb-1.5">{text}</h4>
}

function Paragraph({ tone, content }: { tone?: string; content: Node[] }) {
  const text = collectText(content)
  const cls = tone === 'muted' ? 'text-xs text-slate-500 mb-2' : 'text-sm text-slate-700 leading-relaxed mb-3'
  return <p className={cls}>{text}</p>
}

function KvTable({ rows }: { rows: { label: string; value: string }[] }) {
  if (rows.length === 0) return null
  return (
    <table className={`w-full text-sm border ${SECTION_BORDER} mb-4`}>
      <tbody>
        {rows.map((r, i) => (
          <tr key={i} className={`border-b ${SECTION_BORDER} last:border-0`}>
            <th className="w-1/3 bg-slate-50 px-3 py-2 text-left font-semibold text-slate-700 align-top">
              {r.label}
            </th>
            <td className="px-3 py-2 text-slate-700">{r.value}</td>
          </tr>
        ))}
      </tbody>
    </table>
  )
}

function DataTable({
  headers,
  rows,
  footer,
}: {
  headers: string[]
  rows: string[][]
  footer?: string
}) {
  if (rows.length === 0) return null
  return (
    <div className="mb-4">
      <table className={`w-full text-sm border ${SECTION_BORDER}`}>
        <thead>
          <tr className={`${NAVY}`}>
            {headers.map((h, i) => (
              <th key={i} className="px-3 py-2 text-left font-semibold border-r border-slate-700 last:border-0">
                {h}
              </th>
            ))}
          </tr>
        </thead>
        <tbody>
          {rows.map((row, i) => (
            <tr key={i} className={`border-b ${SECTION_BORDER} last:border-0 ${i % 2 === 1 ? 'bg-slate-50' : ''}`}>
              {row.map((cell, j) => (
                <td key={j} className="px-3 py-2 text-slate-700 align-top">
                  {cell}
                </td>
              ))}
            </tr>
          ))}
        </tbody>
      </table>
      {footer && <div className="text-xs text-slate-500 mt-1.5">{footer}</div>}
    </div>
  )
}

type CalloutItem = string | { title?: string; content?: string }

function CalloutList({ items, tone }: { items: CalloutItem[]; tone: string }) {
  if (items.length === 0) return null
  const toneCls =
    tone === 'danger'
      ? 'border-red-300 bg-red-50 text-red-900'
      : tone === 'warning'
        ? 'border-yellow-300 bg-yellow-50 text-yellow-900'
        : 'border-blue-300 bg-blue-50 text-blue-900'
  return (
    <div className="space-y-2 mb-3">
      {items.map((item, i) => {
        if (typeof item === 'string') {
          return (
            <div key={i} className={`border-l-4 ${toneCls} px-3 py-2 text-sm rounded-r`}>
              {item}
            </div>
          )
        }
        return (
          <div key={i} className={`border-l-4 ${toneCls} px-3 py-2 text-sm rounded-r`}>
            {item.title && <div className="font-semibold mb-0.5">{item.title}</div>}
            {item.content && <div>{item.content}</div>}
          </div>
        )
      })}
    </div>
  )
}

// ── 유틸 ──────────────────────────────────────────────────

function str(v: unknown): string {
  return v == null ? '' : String(v)
}

function collectText(content: Node[]): string {
  return content
    .map((n) => (n.type === 'text' ? String(n.text || '') : collectText(n.content || [])))
    .join('')
}

// 날짜 문자열/ISO 를 "YYYY년 M월 D일" 로 변환. 시·분·KST 제거. 파싱 실패 시 빈 문자열.
// "2026" 처럼 연도만 있는 값은 정확한 날짜로 못 보므로 변환 실패 처리(폴백은 호출부에서).
function fmtDateKo(v?: string | null): string {
  if (!v) return ''
  const s = String(v).trim()
  // ISO(2026-05-29...) 또는 "2026년 6월 10일 23:59 KST" 형태 모두에서 Y/M/D 추출
  let m = s.match(/(\d{4})[-/.년\s]+(\d{1,2})[-/.월\s]+(\d{1,2})/)
  if (!m) {
    // "2026-05-29" 같은 ISO 를 Date 로 한번 더 시도
    const d = new Date(s)
    if (isNaN(d.getTime())) return ''
    return `${d.getFullYear()}년 ${d.getMonth() + 1}월 ${d.getDate()}일`
  }
  return `${Number(m[1])}년 ${Number(m[2])}월 ${Number(m[3])}일`
}

// CR-033: "공고 기본 정보" 표에 공고일 행을 끼운다(데이터 재생성 없이 렌더에서). 마감일 행 다음에, 이미 있으면 중복 추가 안 함.
// + 마감일 행 value 도 "YYYY년 M월 D일" 로 포맷(시·분·KST 제거).
function withIssuedDate(
  rows: { label: string; value: string }[],
  issuedDate?: string,
): { label: string; value: string }[] {
  // 마감일 행 value 포맷팅 (issuedDate 유무와 무관하게 항상)
  const formatted = rows.map((r) => {
    if (r.label.includes('마감일') || /deadline/i.test(r.label)) {
      const f = fmtDateKo(r.value)
      return f ? { ...r, value: f } : r
    }
    return r
  })
  if (!issuedDate) return formatted
  if (formatted.some((r) => r.label.includes('발행일') || r.label.includes('공고일'))) return formatted
  const out = [...formatted]
  const deadlineIdx = out.findIndex((r) => r.label.includes('마감일') || /deadline/i.test(r.label))
  const issuedRow = { label: '공고일', value: issuedDate }
  if (deadlineIdx >= 0) out.splice(deadlineIdx + 1, 0, issuedRow)
  else out.push(issuedRow)
  return out
}


