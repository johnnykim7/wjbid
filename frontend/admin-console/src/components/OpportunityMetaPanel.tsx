/**
 * OpportunityMetaPanel, CR-021 2차
 *
 * SAM.gov 원본 페이지 수준의 메타 정보를 패널로 표시.
 * 공고문 본문(NoticeDocumentView) 위에 위치해 사용자가 "어디서 온 공고인지" 먼저 파악하게 한다.
 *
 * 정보 분류 (없는 항목은 그 행 자체를 스킵):
 *  - Header: 영문 제목 + 한글 제목 + Active 배지 + SAM.gov 원본 링크
 *  - General Information: Solicitation / Type / Posted / Deadline / Set Aside
 *  - Classification: NAICS / PSC (Classification Code)
 *  - Place of Performance
 *  - Office (Organization)
 *  - Contact (POC)
 *  - Description (외부 링크 또는 본문 일부)
 *  - Resource Links
 */

import type { ReactNode } from 'react'
import { Link as LinkIcon, Mail, Phone, Printer, SquareArrowOutUpRight } from 'lucide-react'
import { Badge, Card, CardContent, CardHeader, InfoGrid, type InfoGridItem } from '@therecommerce-develop/ds-ui'
import { ToneBadge } from './StateBadge'

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
  }
  /** 페이지가 자체 헤더(제목·배지)를 갖는 경우 hideHeader=true로 메타 그룹만 렌더 */
  hideHeader?: boolean
}

const LINK_CLASS = 'inline-flex items-center gap-1 text-secondary hover:underline'

export default function OpportunityMetaPanel({ data, hideHeader = false }: Props) {
  const daysLeft = data.responseDeadline
    ? Math.ceil((new Date(data.responseDeadline).getTime() - Date.now()) / 86400000)
    : null

  const place = formatPlace(data.placeOfPerformance)
  const description = data.description?.trim()
  const isDescUrl = !!description && /^https?:\/\//.test(description)

  const general = items([
    // 헤더가 숨겨지면 공고번호가 보일 곳이 없어 일반 정보에 둔다
    hideHeader && data.solicitationNumber
      ? { label: '공고번호', value: <span className="font-mono">{data.solicitationNumber}</span> }
      : null,
    data.type ? { label: '공고 유형', value: data.type } : null,
    data.postedDate ? { label: '게시일', value: formatDate(data.postedDate) } : null,
    data.responseDeadline
      ? {
          label: '마감일',
          value: (
            <span className="inline-flex flex-wrap items-center gap-2">
              {formatDateTime(data.responseDeadline)}
              {daysLeft != null && daysLeft >= 0 && <DaysLeftBadge days={daysLeft} />}
            </span>
          ),
        }
      : null,
    data.setAside ? { label: '우선조달', value: data.setAside } : null,
  ])

  const classification = items([
    data.naicsCode ? { label: 'NAICS 코드', value: <span className="font-mono">{data.naicsCode}</span> } : null,
    data.classificationCode
      ? { label: 'PSC 코드', value: <span className="font-mono">{data.classificationCode}</span> }
      : null,
  ])

  return (
    <Card>
      {/* 헤더 (페이지가 자체 헤더 있으면 hideHeader=true) */}
      {!hideHeader && (
        <CardHeader className="items-start py-3">
          <div className="min-w-0 flex-1 space-y-1">
            {data.koreanTitle && (
              <h2 className="text-base font-semibold leading-snug text-foreground">{data.koreanTitle}</h2>
            )}
            {data.title && <p className="text-sm leading-snug text-muted-foreground">{data.title}</p>}
            <div className="flex flex-wrap gap-4 text-xs text-muted-foreground">
              {data.solicitationNumber && (
                <span>
                  <span className="mr-1">Solicitation</span>
                  <span className="font-mono text-foreground">{data.solicitationNumber}</span>
                </span>
              )}
              {data.uiLink && (
                <a href={data.uiLink} target="_blank" rel="noreferrer" className={LINK_CLASS}>
                  <SquareArrowOutUpRight className="size-4" />
                  SAM.gov 원본
                </a>
              )}
            </div>
          </div>
          {data.active ? (
            <Badge variant="success" appearance="light" className="whitespace-nowrap">
              ACTIVE
            </Badge>
          ) : (
            <ToneBadge tone="gray">INACTIVE</ToneBadge>
          )}
        </CardHeader>
      )}

      <CardContent className="space-y-5 py-4">
        {general.length > 0 && (
          <MetaGroup title="일반 정보">
            <InfoGrid items={general} />
          </MetaGroup>
        )}

        {classification.length > 0 && (
          <MetaGroup title="분류 코드">
            <InfoGrid items={classification} />
          </MetaGroup>
        )}

        {place && (
          <MetaGroup title="수행 장소">
            <InfoGrid cols={1} items={[{ label: '장소', value: place }]} />
          </MetaGroup>
        )}

        {data.organizationName && (
          <MetaGroup title="발주 기관">
            <InfoGrid cols={1} items={[{ label: '기관명', value: <span className="break-all">{data.organizationName}</span> }]} />
          </MetaGroup>
        )}

        {data.pointOfContact && data.pointOfContact.length > 0 && (
          <MetaGroup title="담당자">
            <div className="space-y-3">
              {data.pointOfContact.map((p, i) => (
                <div key={i} className="border-l-2 border-border pl-3">
                  <div className="mb-0.5 flex items-center gap-2">
                    {p.fullName && <span className="text-sm font-semibold text-foreground">{p.fullName}</span>}
                    {p.type && <ToneBadge tone="gray">{p.type.toUpperCase()}</ToneBadge>}
                  </div>
                  <div className="flex flex-wrap gap-x-4 gap-y-0.5 text-xs text-muted-foreground">
                    {p.email && (
                      <a href={`mailto:${p.email}`} className={LINK_CLASS}>
                        <Mail className="size-4" />
                        {p.email}
                      </a>
                    )}
                    {p.phone && (
                      <span className="inline-flex items-center gap-1">
                        <Phone className="size-4" />
                        {p.phone}
                      </span>
                    )}
                    {p.fax && (
                      <span className="inline-flex items-center gap-1">
                        <Printer className="size-4" />
                        Fax: {p.fax}
                      </span>
                    )}
                  </div>
                </div>
              ))}
            </div>
          </MetaGroup>
        )}

        {/* Description, CR-035: 본문 텍스트(있으면) + SAM 원문 링크(번역·본문 유무와 무관하게 항상) */}
        <MetaGroup title="본문">
          {description && !isDescUrl ? (
            <p className="mb-2 whitespace-pre-wrap text-sm leading-relaxed text-foreground">{description}</p>
          ) : (
            // description이 noticedesc API URL(api_key 없어 404)이거나 비어있는 본문 미수집 상태.
            // 깨진 API 링크는 노출하지 않고 안내만. 본문은 "한글 번역하기"로 받는다.
            <p className="mb-2 text-sm text-muted-foreground">
              본문 미수집, "한글 번역하기"를 누르면 원문 본문을 가져옵니다.
            </p>
          )}
          {/* 원문(SAM 공개 공고 페이지)으로 가는 링크, 항상 노출 */}
          {data.uiLink && (
            <a href={data.uiLink} target="_blank" rel="noreferrer" className={`${LINK_CLASS} break-all text-sm`}>
              <SquareArrowOutUpRight className="size-4" />
              SAM.gov에서 원문 보기
            </a>
          )}
        </MetaGroup>

        {data.resourceLinks && data.resourceLinks.length > 0 && (
          <MetaGroup title="첨부 링크">
            <ul className="space-y-1">
              {data.resourceLinks.map((url, i) => (
                <li key={i}>
                  <a href={url} target="_blank" rel="noreferrer" className={`${LINK_CLASS} break-all text-sm`}>
                    <LinkIcon className="size-4 shrink-0" />
                    {url}
                  </a>
                </li>
              ))}
            </ul>
          </MetaGroup>
        )}
      </CardContent>
    </Card>
  )
}

function items(list: (InfoGridItem | null)[]): InfoGridItem[] {
  return list.filter((i): i is InfoGridItem => i !== null)
}

function MetaGroup({ title, children }: { title: string; children: ReactNode }) {
  return (
    <div className="space-y-2">
      <h3 className="text-xs font-semibold text-muted-foreground">{title}</h3>
      {children}
    </div>
  )
}

function DaysLeftBadge({ days }: { days: number }) {
  return <ToneBadge tone={days <= 7 ? 'destructive' : days <= 14 ? 'warning' : 'gray'}>D-{days}</ToneBadge>
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
