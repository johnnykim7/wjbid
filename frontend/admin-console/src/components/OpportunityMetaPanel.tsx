/**
 * OpportunityMetaPanel — CR-021 2차
 *
 * SAM.gov 원본 페이지 수준의 메타 정보를 패널로 표시.
 * 공고문 본문(NoticeDocumentView) 위에 위치 — 사용자가 "어디서 온 공고인지" 먼저 파악.
 *
 * 정보 분류 (없는 항목은 그 행 자체를 스킵):
 *  - Header: 영문 제목 + 한글 제목 + Active 배지 + SAM.gov 원본 링크
 *  - General Information: Type / Posted / Deadline / Set Aside
 *  - Classification: NAICS / PSC (Classification Code)
 *  - Place of Performance
 *  - Office (Organization)
 *  - Contact (POC)
 *  - Description (외부 링크 또는 본문 일부)
 *  - Resource Links
 */

import type { ReactNode } from 'react'

interface POC {
  type?: string
  fullName?: string
  email?: string
  phone?: string
  fax?: string
}

interface Props {
  data: {
    title?: string
    koreanTitle?: string
    solicitationNumber?: string
    type?: string
    organizationName?: string
    postedDate?: string
    responseDeadline?: string
    active?: boolean
    uiLink?: string
    setAside?: string | null
    naicsCode?: string | null
    classificationCode?: string | null
    placeOfPerformance?: unknown
    description?: string | null
    pointOfContact?: POC[] | null
    resourceLinks?: string[] | null
    /** CR-034: 입찰서류 정본이 PIEE에 있는 공고 */
    pieeAvailable?: boolean
    pieeUrl?: string | null
  }
  /** 페이지가 자체 헤더(제목·배지)를 갖는 경우 hideHeader=true로 메타 그룹만 렌더 */
  hideHeader?: boolean
}

export default function OpportunityMetaPanel({ data, hideHeader = false }: Props) {
  const daysLeft = data.responseDeadline
    ? Math.ceil((new Date(data.responseDeadline).getTime() - Date.now()) / 86400000)
    : null

  const place = formatPlace(data.placeOfPerformance)
  const description = data.description?.trim()
  const isDescUrl = !!description && /^https?:\/\//.test(description)

  return (
    <section className="bg-white border border-slate-200 rounded-lg overflow-hidden mb-4">
      {/* 헤더 (페이지가 자체 헤더 있으면 hideHeader=true) */}
      {!hideHeader && (
        <div className="bg-slate-900 text-white px-5 py-4">
          <div className="flex items-start justify-between gap-3 mb-2">
            <div className="flex-1 min-w-0">
              {data.koreanTitle && (
                <h2 className="text-base font-semibold leading-snug mb-1">{data.koreanTitle}</h2>
              )}
              {data.title && (
                <p className="text-sm text-slate-300 leading-snug">{data.title}</p>
              )}
            </div>
            <div className="flex items-center gap-2 shrink-0">
              {data.active ? (
                <span className="px-2 py-0.5 bg-emerald-500/20 text-emerald-300 border border-emerald-400/40 rounded text-xs font-semibold uppercase">
                  Active
                </span>
              ) : (
                <span className="px-2 py-0.5 bg-slate-500/20 text-slate-300 border border-slate-400/40 rounded text-xs font-semibold uppercase">
                  Inactive
                </span>
              )}
            </div>
          </div>
          <div className="flex flex-wrap gap-4 text-xs text-slate-300 mt-2">
            {data.solicitationNumber && (
              <span>
                <span className="text-slate-400 mr-1">Solicitation</span>
                <span className="font-mono text-white">{data.solicitationNumber}</span>
              </span>
            )}
            {data.uiLink && (
              <a
                href={data.uiLink}
                target="_blank"
                rel="noreferrer"
                className="text-sky-300 hover:text-sky-200 underline"
              >
                <i className="fa-solid fa-up-right-from-square mr-1" />
                SAM.gov 원본
              </a>
            )}
          </div>
        </div>
      )}

      {/* 메타 그룹들 */}
      <div className="divide-y divide-slate-200">
        {/* General Information */}
        <MetaGroup title="일반 정보">
          {data.type && <Field label="공고 유형" value={data.type} />}
          {data.postedDate && <Field label="게시일" value={formatDate(data.postedDate)} />}
          {data.responseDeadline && (
            <Field
              label="마감일"
              value={
                <span>
                  {formatDateTime(data.responseDeadline)}
                  {daysLeft != null && daysLeft >= 0 && (
                    <span
                      className={`ml-2 font-bold ${daysLeft <= 7 ? 'text-red-500' : daysLeft <= 14 ? 'text-amber-500' : 'text-slate-500'}`}
                    >
                      D-{daysLeft}
                    </span>
                  )}
                </span>
              }
            />
          )}
          {data.setAside && <Field label="우선조달" value={data.setAside} />}
        </MetaGroup>

        {/* Classification */}
        {(data.naicsCode || data.classificationCode) && (
          <MetaGroup title="분류 코드">
            {data.naicsCode && <Field label="NAICS 코드" value={<span className="font-mono">{data.naicsCode}</span>} />}
            {data.classificationCode && (
              <Field label="PSC 코드" value={<span className="font-mono">{data.classificationCode}</span>} />
            )}
          </MetaGroup>
        )}

        {/* Place of Performance */}
        {place && (
          <MetaGroup title="수행 장소">
            <Field label="장소" value={place} />
          </MetaGroup>
        )}

        {/* Office */}
        {data.organizationName && (
          <MetaGroup title="발주 기관">
            <Field label="기관명" value={<span className="break-all">{data.organizationName}</span>} />
          </MetaGroup>
        )}

        {/* Contact (POC) */}
        {data.pointOfContact && data.pointOfContact.length > 0 && (
          <MetaGroup title="담당자">
            <div className="space-y-3">
              {data.pointOfContact.map((p, i) => (
                <div key={i} className="border-l-2 border-slate-300 pl-3">
                  <div className="flex items-center gap-2 mb-0.5">
                    {p.fullName && <span className="font-semibold text-slate-900 text-sm">{p.fullName}</span>}
                    {p.type && (
                      <span className="text-[10px] px-1.5 py-0.5 bg-slate-100 text-slate-600 rounded uppercase">
                        {p.type}
                      </span>
                    )}
                  </div>
                  <div className="flex flex-wrap gap-x-4 gap-y-0.5 text-xs text-slate-600">
                    {p.email && (
                      <a href={`mailto:${p.email}`} className="text-sky-600 hover:underline">
                        <i className="fa-solid fa-envelope mr-1" />
                        {p.email}
                      </a>
                    )}
                    {p.phone && (
                      <span>
                        <i className="fa-solid fa-phone mr-1" />
                        {p.phone}
                      </span>
                    )}
                    {p.fax && (
                      <span>
                        <i className="fa-solid fa-fax mr-1" />
                        Fax: {p.fax}
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </MetaGroup>
        )}

        {/* Description */}
        {description && (
          <MetaGroup title="본문">
            {isDescUrl ? (
              <a
                href={description}
                target="_blank"
                rel="noreferrer"
                className="text-sky-600 hover:underline text-sm break-all"
              >
                <i className="fa-solid fa-up-right-from-square mr-1" />
                원본 설명 보기 (SAM.gov API)
              </a>
            ) : (
              <p className="text-sm text-slate-700 leading-relaxed whitespace-pre-wrap">{description}</p>
            )}
          </MetaGroup>
        )}

        {/* CR-034: PIEE 입찰서류 (정본). 본문에 piee.eb.mil 제출 지정이 있는 공고만 노출 */}
        {data.pieeAvailable && data.pieeUrl && (
          <MetaGroup title="입찰서류 (PIEE)">
            <div className="rounded-md border border-amber-300 bg-amber-50 px-3 py-2.5">
              <p className="text-xs text-amber-800 mb-2 leading-relaxed">
                이 공고의 입찰서류 정본(본 공고서·수정본)은 PIEE에 있습니다. SAM 자동 수집분만으로는
                불완전할 수 있으니, 아래에서 직접 받아 첨부에 업로드하세요.
              </p>
              <a
                href={data.pieeUrl}
                target="_blank"
                rel="noreferrer"
                className="inline-flex items-center gap-1.5 rounded-md bg-amber-600 px-3 py-1.5 text-sm font-medium text-white hover:bg-amber-700"
              >
                <i className="fa-solid fa-up-right-from-square" />
                PIEE 입찰서류 보기
              </a>
            </div>
          </MetaGroup>
        )}

        {/* Resource Links */}
        {data.resourceLinks && data.resourceLinks.length > 0 && (
          <MetaGroup title="첨부 링크">
            <ul className="space-y-1">
              {data.resourceLinks.map((url, i) => (
                <li key={i}>
                  <a
                    href={url}
                    target="_blank"
                    rel="noreferrer"
                    className="text-sky-600 hover:underline text-sm break-all"
                  >
                    <i className="fa-solid fa-link mr-1" />
                    {url}
                  </a>
                </li>
              ))}
            </ul>
          </MetaGroup>
        )}
      </div>
    </section>
  )
}

function MetaGroup({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="px-5 py-3">
      <h3 className="text-[11px] font-semibold text-slate-500 uppercase tracking-wide mb-2">{title}</h3>
      <dl className="space-y-1.5">{children}</dl>
    </div>
  )
}

function Field({ label, value }: { label: string; value: ReactNode }) {
  return (
    <div className="grid grid-cols-12 gap-2 text-sm">
      <dt className="col-span-4 md:col-span-3 text-slate-500">{label}</dt>
      <dd className="col-span-8 md:col-span-9 text-slate-800">{value}</dd>
    </div>
  )
}

function formatDate(s: string): string {
  try {
    return new Date(s).toLocaleDateString('ko-KR', { year: 'numeric', month: 'long', day: 'numeric' })
  } catch {
    return s
  }
}

function formatDateTime(s: string): string {
  try {
    const d = new Date(s)
    return d.toLocaleString('ko-KR', {
      year: 'numeric',
      month: 'long',
      day: 'numeric',
      hour: '2-digit',
      minute: '2-digit',
    })
  } catch {
    return s
  }
}

function formatPlace(p: unknown): string | null {
  if (p == null) return null
  if (typeof p === 'string') return p.trim() || null
  if (typeof p === 'object') {
    const o = p as Record<string, unknown>
    const parts: string[] = []
    if (o.streetAddress) parts.push(String(o.streetAddress))
    if (o.city) parts.push(getCityCountry(o.city))
    if (o.state) parts.push(getCityCountry(o.state))
    if (o.zip) parts.push(String(o.zip))
    if (o.country) parts.push(getCityCountry(o.country))
    return parts.filter(Boolean).join(', ') || null
  }
  return null
}

function getCityCountry(v: unknown): string {
  if (typeof v === 'string') return v
  if (v && typeof v === 'object') {
    const o = v as Record<string, unknown>
    return String(o.name ?? o.code ?? '')
  }
  return ''
}
