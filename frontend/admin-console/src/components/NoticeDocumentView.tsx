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
  // CR-117: 옛 NOTICE_VIEW 10섹션 골격 복원 — 계약 기간 / 현장 설명회 전용 섹션
  contractPeriod?: string
  siteVisit?: string
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
  // CR-117: 옛 NOTICE_VIEW "1.공고 기본정보" 행 복원 — opportunity 메타 직접 공급(LLM 무관)
  noticeTypeKo?: string | null
  naicsLabelKo?: string | null
  responseDeadline?: string | null
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
  noticeTypeKo,
  naicsLabelKo,
  responseDeadline,
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

  // CR-117: 기본 정보 kvTable — 옛 NOTICE_VIEW 골격 복원(공고번호/공고유형/발주기관/공고일/마감일/NAICS).
  //         값 있는 행만 동적 노출(조달방식은 데이터 없어 제거, 평가방식은 8.낙찰기준 전용 섹션으로 이관).
  const deadlineKo = fmtDateKo(responseDeadline) || str(responseDeadline)
  const basicRows = (
    [
      solicitationNumber ? { label: '공고번호', value: str(solicitationNumber) } : null,
      noticeTypeKo ? { label: '공고유형', value: str(noticeTypeKo) } : null,
      organizationName ? { label: '발주기관', value: str(organizationName) } : null,
      issuedDate ? { label: '공고일', value: issuedDate } : null,
      deadlineKo ? { label: '마감일', value: deadlineKo } : null,
      naicsLabelKo ? { label: 'NAICS 코드', value: str(naicsLabelKo) } : null,
    ].filter(Boolean) as { label: string; value: string }[]
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

  const hasSpecialNotes = !!summary?.specialNotes && summary.specialNotes.length > 0

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

      {/* CR-119: 최초 요구사항 10항목 순서로 섹션 재배치 + 제출서류는 11번 별도. 데이터 없어도 골격 유지("해당 없음") */}

      {/* 1. 입찰 번호 (공고 기본 정보) */}
      <Heading level={2} content={[{ type: 'text', text: '1. 입찰 번호' }]} />
      {basicRows.length > 0 ? <KvTable rows={basicRows} /> : <EmptySection />}

      {/* 2. 내용 (개요 + 작업 범위 통합) */}
      <Heading level={2} content={[{ type: 'text', text: '2. 내용' }]} />
      {summary?.overview || summary?.scope ? (
        <>
          {summary?.overview && (
            <Paragraph content={[{ type: 'text', text: str(summary.overview) }]} />
          )}
          {summary?.scope && (
            <Paragraph content={[{ type: 'text', text: str(summary.scope) }]} />
          )}
        </>
      ) : (
        <EmptySection />
      )}

      {/* 3. 계약 기간 */}
      <Heading level={2} content={[{ type: 'text', text: '3. 계약 기간' }]} />
      {summary?.contractPeriod ? (
        <Paragraph content={[{ type: 'text', text: str(summary.contractPeriod) }]} />
      ) : (
        <EmptySection />
      )}

      {/* 4. 현장 설명회 */}
      <Heading level={2} content={[{ type: 'text', text: '4. 현장 설명회' }]} />
      {summary?.siteVisit ? (
        <Paragraph content={[{ type: 'text', text: str(summary.siteVisit) }]} />
      ) : (
        <EmptySection />
      )}

      {/* 5. 담당자 (POC) */}
      <Heading level={2} content={[{ type: 'text', text: '5. 담당자 (POC)' }]} />
      {contactRows.length > 0 ? <KvTable rows={contactRows} /> : <EmptySection />}

      {/* 6. 자격 요건 (CR-033 정밀추출) */}
      <Heading level={2} content={[{ type: 'text', text: '6. 자격 요건' }]} />
      {hasElig ? <EligibilityBlock items={eligibility!} /> : <EmptySection />}

      {/* 7. 낙찰 기준 */}
      <Heading level={2} content={[{ type: 'text', text: '7. 낙찰 기준' }]} />
      {summary?.evaluationCriteria ? (
        <Paragraph content={[{ type: 'text', text: str(summary.evaluationCriteria) }]} />
      ) : (
        <EmptySection />
      )}

      {/* 8. 참고 사항 (현재 전용 데이터 없음 — 골격 유지) */}
      <Heading level={2} content={[{ type: 'text', text: '8. 참고 사항' }]} />
      <EmptySection />

      {/* 9. 특별 유의 사항 (specialNotes) */}
      <Heading level={2} content={[{ type: 'text', text: '9. 특별 유의 사항' }]} />
      {hasSpecialNotes ? (
        <CalloutList items={summary!.specialNotes!} tone="warning" />
      ) : (
        <EmptySection />
      )}

      {/* 10. 타임라인 */}
      <Heading level={2} content={[{ type: 'text', text: '10. 타임라인' }]} />
      {timelineRows.length > 0 ? (
        <DataTable headers={['구분', '일정']} rows={timelineRows} />
      ) : (
        <EmptySection />
      )}

      {/* 11. 제출 서류 (요구사항 10항목 밖 부가 정보 — 별도 섹션) */}
      <Heading level={2} content={[{ type: 'text', text: '11. 제출 서류' }]} />
      {docRows.length > 0 ? (
        <DataTable headers={docHeaders} rows={docRows} />
      ) : (
        <EmptySection />
      )}
    </article>
  )
}

// CR-117: 데이터 없는 섹션의 골격 유지용 — "해당 없음" 플레이스홀더
function EmptySection() {
  return <p className="text-sm text-slate-400 italic mb-4">해당 없음</p>
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
  const text = splitEnumerations(collectText(content))
  const cls = tone === 'muted' ? 'text-xs text-slate-500 mb-2' : 'text-sm text-slate-700 leading-relaxed mb-3'
  // CR-119: 열거 항목이 한 줄로 붙어오는 평문을 항목 단위로 줄바꿈 표시
  return <p className={`${cls} whitespace-pre-line`}>{text}</p>
}

// CR-119: LLM 요약이 줄바꿈 없이 (1)/(2)·Phase One:/Phase Two:·일시:/장소: 등을
// 한 줄로 이어 출력하는 경우, 열거·라벨 패턴 앞에 줄바꿈을 삽입해 가독성 복원.
// 보수적 패턴만(정상 문장 오분할 방지): 문장 중간의 "(숫자)", "Phase/단계 + 서수:",
// 한글 라벨("일시:/장소:/POC:/필수 여부:") 앞.
function splitEnumerations(s: string): string {
  if (!s) return s
  return s
    // " (1) " / " (2) " 처럼 앞에 공백이 있는 괄호 숫자 열거 → 줄바꿈
    .replace(/\s+(\(\d+\))/g, '\n$1')
    // "Phase One:" "Phase Two:" 등 (앞에 공백) → 줄바꿈
    .replace(/\s+(Phase\s+(?:One|Two|Three|Four|1|2|3|4)\b)/gi, '\n$1')
    // 한글 라벨 열거(현장설명회 등): "장소:" "POC:" "필수 여부:" 앞 공백 → 줄바꿈
    .replace(/\s+(장소:|POC:|일시:|필수\s*여부:|연락처:)/g, '\n$1')
    .replace(/^\n+/, '')
    .trim()
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

