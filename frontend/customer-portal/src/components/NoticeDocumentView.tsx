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
 * (admin-console의 동명 컴포넌트와 동일. 향후 공통 패키지로 추출 후보.)
 */

import type { ReactNode } from 'react'

type Node = {
  type: string
  attrs?: Record<string, unknown>
  content?: Node[]
  text?: string
}

interface Props {
  contentJson?: Record<string, unknown> | null
}

const NAVY = 'bg-slate-800 text-white'
const NAVY_DARK = 'bg-slate-900 text-white'
const NAVY_BORDER = 'border-slate-700'
const SECTION_BORDER = 'border-slate-200'

export default function NoticeDocumentView({ contentJson }: Props) {
  if (!contentJson) {
    return (
      <div className="rounded-lg bg-yellow-50 border border-yellow-200 px-4 py-3 text-sm text-yellow-800">
        공고문 본문이 아직 준비되지 않았습니다.
      </div>
    )
  }

  const doc = contentJson as unknown as Node
  if (doc.type !== 'doc' || !Array.isArray(doc.content)) {
    return <div className="text-sm text-red-600">올바르지 않은 본문 형식입니다.</div>
  }

  return (
    <article className="bg-white">
      {doc.content.map((node, i) => (
        <NodeRenderer key={i} node={node} />
      ))}
    </article>
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
