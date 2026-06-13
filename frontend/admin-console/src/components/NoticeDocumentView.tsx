/**
 * NoticeDocumentView — CR-021
 *
 * 공고문(Notice)의 TipTap JSON 본문을 PDF 양식 그대로 렌더링.
 * Aimbase 한글화 워크플로우가 NOTICE_VIEW 템플릿 골격을 채워 반환한 contentJson을 받음.
 *
 * 지원 노드:
 *  - 표준: doc / heading(level) / paragraph(tone?) / bulletList(items)
 *  - 커스텀: noticeHeader / metaGrid / kvTable / dataTable / groupedList / calloutList
 *
 * 디자인: 네이비 마스키 정부문서 계열. 첨부 PDF(W90VN926QA034) 양식 재현.
 *
 * contentJson 미존재 시(양식 미등록 / LLM 미생성) summaryJson fallback 렌더 — 옛 화면.
 */

import { Fragment } from 'react'
import type { ReactNode } from 'react'

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

interface Props {
  contentJson?: Record<string, unknown> | null
  // CR-033: 정밀추출 자격요건 — 본문 "자격 요건" 섹션 자리에 인라인 렌더(중복 방지)
  eligibility?: EligibilityItem[]
  // 공고 게시일(SAM 수집 확정값). LLM이 채운 issuedDate("2026" 등)보다 우선하여 "공고일"로 표시.
  postedDate?: string | null
}

const NAVY = 'bg-slate-800 text-white'
const NAVY_DARK = 'bg-slate-900 text-white'
const NAVY_BORDER = 'border-slate-700'
const SECTION_BORDER = 'border-slate-200'

export default function NoticeDocumentView({ contentJson, eligibility, postedDate }: Props) {
  if (!contentJson) {
    return (
      <div className="rounded-lg bg-yellow-50 border border-yellow-200 px-4 py-3 text-sm text-yellow-800">
        공고문 본문(contentJson)이 아직 생성되지 않았습니다. NOTICE_VIEW 양식 등록 후 한글화 재생성이 필요합니다.
      </div>
    )
  }

  const doc = contentJson as unknown as Node
  if (doc.type !== 'doc' || !Array.isArray(doc.content)) {
    return <div className="text-sm text-red-600">올바르지 않은 본문 형식입니다.</div>
  }

  // CR-033: 본문에 "자격 요건"/"Qualifications" heading 이 있는지 — 있으면 그 직후에 정밀추출 블록을 끼운다.
  const hasElig = !!eligibility && eligibility.length > 0
  const isEligHeading = (node: Node) =>
    node.type === 'heading' &&
    (node.content || []).some((c) => {
      const t = String(c.text || '')
      return t.includes('자격') || /qualif/i.test(t)
    })
  const eligHeadingRendered = hasElig && doc.content.some(isEligHeading)

  // CR-033: 중복/발행일은 데이터(contentJson) 재생성 없이 "그릴 때"만 처리한다.
  //  - 중복: 파란 박스 아래 metaGrid(6칸)와 "1. 공고 기본 정보" kvTable 이 같은 6항목 → metaGrid 를 렌더 스킵(주제표=기본정보 표를 남긴다).
  //  - 발행일: noticeHeader.issuedDate 를 "공고 기본 정보" kvTable 의 한 행으로 끌어올려 표시(회색 줄에 묻히던 것).
  const headerNode = doc.content.find((n) => n.type === 'noticeHeader')
  // 공고일: 실제 게시일(postedDate) 우선 — LLM이 채운 issuedDate("2026" 등)는 부정확하므로 폴백으로만.
  const issuedDate = fmtDateKo(postedDate) || fmtDateKo(str(headerNode?.attrs?.issuedDate)) || str(headerNode?.attrs?.issuedDate)

  const isBasicInfoHeading = (node: Node) =>
    node.type === 'heading' &&
    (node.content || []).some((c) => String(c.text || '').includes('공고 기본 정보') || /general information/i.test(String(c.text || '')))

  // "공고 기본 정보" heading 바로 다음 kvTable 인덱스(발행일 행을 끼울 대상)
  const basicInfoIdx = doc.content.findIndex(isBasicInfoHeading)
  const basicKvIdx = basicInfoIdx >= 0 && doc.content[basicInfoIdx + 1]?.type === 'kvTable'
    ? basicInfoIdx + 1 : -1

  return (
    <article className="bg-white">
      {doc.content.map((node, i) => {
        // 중복 스킵: 파란 박스 아래 metaGrid(6칸) — "1. 공고 기본 정보" 표와 동일 내용
        if (node.type === 'metaGrid') return null
        // "공고 기본 정보" kvTable 에는 발행일 행을 끼워 렌더
        if (i === basicKvIdx) {
          return (
            <Fragment key={i}>
              <KvTable rows={withIssuedDate(asKvRows(node.attrs?.rows), issuedDate)} />
            </Fragment>
          )
        }
        return (
          <Fragment key={i}>
            <NodeRenderer node={node} />
            {/* 본문 "자격 요건" heading 직후에 정밀추출 자격요건 인라인 */}
            {hasElig && isEligHeading(node) && <EligibilityBlock items={eligibility!} />}
          </Fragment>
        )
      })}
      {/* 본문에 자격요건 heading 이 없으면(양식 차이) 맨 끝에라도 노출 */}
      {hasElig && !eligHeadingRendered && (
        <section className="mt-6">
          <h2 className="text-base font-bold text-slate-900 mb-3 pb-1.5 border-b-2 border-slate-800">참여 자격요건</h2>
          <EligibilityBlock items={eligibility!} />
        </section>
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

function NodeRenderer({ node }: { node: Node }): ReactNode {
  switch (node.type) {
    case 'noticeHeader':
      return <NoticeHeader attrs={node.attrs || {}} />
    case 'metaGrid':
      return <MetaGrid content={node.content || []} />
    case 'heading':
      return <Heading level={Number(node.attrs?.level) || 2} content={node.content || []} />
    case 'paragraph':
      return <Paragraph tone={(node.attrs?.tone as string) || undefined} content={node.content || []} />
    case 'bulletList':
      return <BulletList items={asStringArray(node.attrs?.items)} />
    case 'kvTable':
      return <KvTable rows={asKvRows(node.attrs?.rows)} />
    case 'dataTable':
      return (
        <DataTable
          headers={asStringArray(node.attrs?.headers)}
          rows={asMatrix(node.attrs?.rows)}
          footer={node.attrs?.footer as string | undefined}
        />
      )
    case 'groupedList':
      return <GroupedList groups={asGroups(node.attrs?.groups ?? node.attrs?.items)} />
    case 'calloutList':
      return <CalloutList items={asCalloutItems(node.attrs?.items)} tone={(node.attrs?.tone as string) || 'info'} />
    case 'text':
      return <>{String(node.text || '')}</>
    default:
      return null
  }
}

// ── 커스텀 노드 렌더러 ─────────────────────────────────────

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

function MetaGrid({ content }: { content: Node[] }) {
  const cells = content.filter((n) => n.type === 'metaCell')
  if (cells.length === 0) return null
  return (
    <div className={`grid grid-cols-3 md:grid-cols-6 ${NAVY} rounded-b-lg mb-6`}>
      {cells.map((cell, i) => {
        const label = str(cell.attrs?.label)
        const value = str(cell.attrs?.value)
        return (
          <div
            key={i}
            className={`px-3 py-2 ${i < cells.length - 1 ? 'border-r ' + NAVY_BORDER : ''} text-center`}
          >
            <div className="text-[10px] text-slate-300 uppercase tracking-wide">{label}</div>
            <div className="text-sm font-semibold mt-0.5 break-keep">{value || '-'}</div>
          </div>
        )
      })}
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

function BulletList({ items }: { items: string[] }) {
  if (items.length === 0) return null
  return (
    <ul className="list-disc pl-5 space-y-1 text-sm text-slate-700 mb-3">
      {items.map((item, i) => (
        <li key={i}>{item}</li>
      ))}
    </ul>
  )
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

function GroupedList({ groups }: { groups: { label: string; items: string[]; required?: boolean }[] }) {
  if (groups.length === 0) return null
  return (
    <div className="space-y-3 mb-3">
      {groups.map((g, i) => (
        <div key={i} className={`border ${SECTION_BORDER} rounded-md overflow-hidden`}>
          <div className="bg-slate-100 px-3 py-1.5 text-sm font-semibold text-slate-800 flex items-center justify-between">
            <span>{g.label}</span>
            {g.required && (
              <span className="text-[10px] px-1.5 py-0.5 bg-red-100 text-red-700 rounded">필수</span>
            )}
          </div>
          <ul className="list-disc pl-8 py-2 space-y-1 text-sm text-slate-700">
            {g.items.map((item, j) => (
              <li key={j}>{item}</li>
            ))}
          </ul>
        </div>
      ))}
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

function asStringArray(v: unknown): string[] {
  if (Array.isArray(v)) return v.map(String)
  return []
}

function asKvRows(v: unknown): { label: string; value: string }[] {
  if (!Array.isArray(v)) return []
  return v
    .filter((r) => r && typeof r === 'object')
    .map((r) => ({
      label: str((r as Record<string, unknown>).label),
      value: str((r as Record<string, unknown>).value),
    }))
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

function asMatrix(v: unknown): string[][] {
  if (!Array.isArray(v)) return []
  return v.filter(Array.isArray).map((row) => (row as unknown[]).map(String))
}

function asGroups(v: unknown): { label: string; items: string[]; required?: boolean }[] {
  if (!Array.isArray(v)) return []
  return v
    .filter((g) => g && typeof g === 'object')
    .map((g) => {
      const o = g as Record<string, unknown>
      return {
        // LLM이 label/title/name 중 무엇으로 채워도 받기
        label: str(o.label ?? o.title ?? o.name),
        items: asStringArray(o.items),
        required: o.required === true,
      }
    })
}

function asCalloutItems(v: unknown): CalloutItem[] {
  if (!Array.isArray(v)) return []
  return v
    .filter((it) => it != null)
    .map((it) => {
      if (typeof it === 'string') return it
      if (typeof it === 'object') {
        const o = it as Record<string, unknown>
        return {
          title: o.title != null ? str(o.title) : undefined,
          content: o.content != null ? str(o.content) : (o.text != null ? str(o.text) : undefined),
        }
      }
      return String(it)
    })
}

