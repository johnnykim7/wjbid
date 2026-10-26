import { useEffect, useRef, useState } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import {
  Download,
  ExternalLink,
  Flag,
  Languages,
  RotateCcw,
  SquareArrowOutUpRight,
  Trash2,
  TriangleAlert,
  Upload,
  WandSparkles,
} from 'lucide-react'
import {
  Alert,
  AlertContent,
  AlertDescription,
  AlertIcon,
  AlertTitle,
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  CardToolbar,
  ConfirmDialog,
  Input,
  PageHeader,
  Spinner,
  toast,
} from '@therecommerce-develop/ds-ui'
import {
  getAdminOpportunityDetail,
  createNotice,
  uploadOpportunityAttachment,
  getOpportunityAttachments,
  downloadOpportunityAttachment,
  deleteOpportunityAttachment,
  retranslateOpportunityDescription,
  setOpportunityPieeLinkBroken,
} from '../api/client'
import OpportunityMetaPanel from '../components/OpportunityMetaPanel'
import { ToneBadge } from '../components/StateBadge'
import { Loading } from '../components/LoadState'
import type { StateTone } from '../utils/bidState'

interface OpportunityDetail {
  id: string
  noticeId: string
  solicitationNumber?: string
  title: string
  titleKo?: string                 // CR-022
  type?: string
  typeKo?: string                  // CR-022
  organizationName?: string
  postedDate?: string
  responseDeadline?: string
  active?: boolean
  uiLink?: string
  translatedAt?: string            // CR-022
  descriptionSummaryKo?: string    // CR-022 2차
  awardAmount?: string             // CR-022 2차
  placeOfPerformanceShort?: string // CR-022 2차
  setAsideKo?: string              // CR-022 2차
  naicsLabelKo?: string            // CR-022 2차
  // CR-021 2차, SAM 원본 메타 (raw_json 기반)
  setAside?: string | null
  naicsCode?: string | null
  classificationCode?: string | null
  placeOfPerformance?: unknown
  description?: string | null
  pointOfContact?: Array<{
    type?: string
    fullName?: string
    email?: string
    phone?: string
    fax?: string
  }> | null
  resourceLinks?: string[] | null
  attachmentCount: number
  manualFetchRequiredCount: number
  noticeCount: number
  pieeLinkBroken?: boolean          // CR-043: PIEE 링크 오류 표식
  // CR-160: PIEE 입찰서류 받기 결과. 받지 않는 공고는 null
  pieeFetchStatus?: 'WAITING' | 'FETCHING' | 'DONE' | 'NOT_FOUND' | 'FAILED' | null
  pieeFetchStatusAt?: string | null
  pieeFileCount?: number | null
  pieeFetchMessage?: string | null
  pieeUrl?: string | null
}

// CR-160: 서버가 서울 시각을 시간대 없이 보내므로 브라우저 시간대로 바꾸지 않고 글자 그대로 자른다
function formatSeoulShort(value?: string | null): string {
  if (!value || value.length < 16) return ''
  return `${value.slice(5, 7)}/${value.slice(8, 10)} ${value.slice(11, 16)}`
}

function pieeFetchLine(opp: OpportunityDetail): { text: string; tone: StateTone; detail?: string } | null {
  const at = formatSeoulShort(opp.pieeFetchStatusAt)
  switch (opp.pieeFetchStatus) {
    case 'DONE':
      return { text: `PIEE 입찰서류 ${opp.pieeFileCount ?? 0}개 받음 (${at})`, tone: 'success' }
    case 'WAITING':
    case 'FETCHING':
      return { text: 'PIEE 입찰서류 받는 중', tone: 'info' }
    case 'NOT_FOUND':
      return { text: `PIEE 에 이 공고가 없습니다 (${at})`, tone: 'gray' }
    case 'FAILED':
      return { text: `PIEE 입찰서류 받기 실패 (${at})`, tone: 'destructive', detail: opp.pieeFetchMessage ?? undefined }
    default:
      return null
  }
}

interface Attachment {
  id: string
  fileName?: string
  fileSize?: number
  contentType?: string
  sourceUrl?: string
  downloadStatus: 'SUCCESS' | 'FAILED' | 'LINK_ONLY' | 'MANUAL_FETCH_REQUIRED'
  manualFetchRequired: boolean
  downloadedAt?: string
}

interface MismatchedFile {
  name: string
  solNo: string
}

/** 바이트를 사람이 읽기 좋은 단위로. null/0/미상은 빈 문자열 반환(표시 생략). */
function formatFileSize(bytes?: number): string {
  if (bytes == null || bytes <= 0) return ''
  const units = ['B', 'KB', 'MB', 'GB']
  let v = bytes
  let i = 0
  while (v >= 1024 && i < units.length - 1) {
    v /= 1024
    i++
  }
  return `${v < 10 && i > 0 ? v.toFixed(1) : Math.round(v)} ${units[i]}`
}

export default function OpportunityAdminDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [opp, setOpp] = useState<OpportunityDetail | null>(null)
  const [attachments, setAttachments] = useState<Attachment[]>([])
  const [loading, setLoading] = useState(true)
  const [actionLoading, setActionLoading] = useState(false)
  const [uploadLoading, setUploadLoading] = useState(false)
  const [translateLoading, setTranslateLoading] = useState(false)
  const [pendingUpload, setPendingUpload] = useState<{ files: File[]; mismatched: MismatchedFile[] } | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<Attachment | null>(null)
  const fileInputRef = useRef<HTMLInputElement>(null)

  const fetchData = async () => {
    if (!id) return
    setLoading(true)
    try {
      const [detailRes, attachRes] = await Promise.all([
        getAdminOpportunityDetail(id),
        getOpportunityAttachments(id),
      ])
      setOpp(detailRes.data)
      setAttachments(attachRes.data || [])
    } catch (err) {
      console.error('원본 공고 상세 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [id])

  const handleCreateNotice = async () => {
    if (!id) return
    setActionLoading(true)
    try {
      // 기존 공고문이 있으면 BE가 그 noticeId를 반환하므로(중복 생성 안 함) 그 상세로 이동
      const res = await createNotice(id)
      const noticeId = res?.data?.noticeId
      navigate(noticeId ? `/notices/${noticeId}` : '/notices')
    } catch (err: any) {
      console.error('공고문 생성 실패:', err)
      const reason = err?.response?.data?.reason
      toast.error(reason || '공고문 생성에 실패했습니다.')
    } finally {
      setActionLoading(false)
    }
  }

  const handleRetranslate = async () => {
    if (!id) return
    setTranslateLoading(true)
    try {
      const { data } = await retranslateOpportunityDescription(id)
      if (data?.status === 'FAILED_OR_EMPTY') {
        toast.error('번역에 실패했거나 원본 본문이 비어 있습니다. 잠시 후 다시 시도해 주세요.')
      }
      fetchData()
    } catch (err) {
      // 번역 LLM이 길면 프록시(nginx proxy_read_timeout)가 응답을 끊지만,
      // BE는 동기 트랜잭션으로 끝까지 돌아 DB에 저장된다. 에러여도 한 번 더 조회해
      // 저장된 번역을 반영한다(저장이 아직이면 빈손이라 안내).
      console.error('본문 번역 응답 수신 실패(번역은 백그라운드 진행 중일 수 있음):', err)
      toast.info('번역에 시간이 걸려 응답이 늦어졌습니다. 화면을 다시 불러옵니다. 아직 반영되지 않았으면 잠시 후 다시 눌러 주세요.')
      fetchData()
    } finally {
      setTranslateLoading(false)
    }
  }

  // PIEE에서 받은 zip은 "공고번호.zip"(예: W91QVN26QA030.zip, W91QVN26QA022 (1).zip) 형태로 떨어진다.
  // 다운로드 폴더에 섞인 '다른 공고'의 zip을 무의식적으로 잘못 올리는 사고를 막기 위해,
  // 파일명이 공고번호 패턴인데 현재 공고번호와 다르면 경고(업로드 자체는 막지 않음, 확인 시 진행).
  // 일반 파일명(report.zip 등)은 PIEE 산출물이 아니므로 경고하지 않는다.
  const SOLNO_PATTERN = /^[A-Z0-9]{10,}$/
  const fileBaseAsSolNo = (name: string): string | null => {
    const base = name
      .replace(/\.zip$/i, '')      // 확장자 제거
      .replace(/\s*\(\d+\)\s*$/, '') // 브라우저 중복 다운로드 표식 "(1)" 제거
      .trim()
      .toUpperCase()
    return SOLNO_PATTERN.test(base) ? base : null
  }

  // CR-043: PIEE 링크 오류 표식 토글. 낙관적 업데이트 후 서버 반영.
  const handleTogglePieeLinkBroken = async () => {
    if (!id || !opp) return
    const next = !opp.pieeLinkBroken
    setActionLoading(true)
    try {
      await setOpportunityPieeLinkBroken(id, next)
      setOpp({ ...opp, pieeLinkBroken: next })
    } catch (err) {
      console.error('PIEE 링크 오류 표식 변경 실패:', err)
      toast.error('표식 변경에 실패했습니다. 잠시 후 다시 시도해 주세요.')
    } finally {
      setActionLoading(false)
    }
  }

  const uploadFiles = async (files: File[]) => {
    if (!id) return
    setUploadLoading(true)
    try {
      await uploadOpportunityAttachment(id, files)
      fetchData()
    } catch (err) {
      console.error('파일 업로드 실패:', err)
    } finally {
      setUploadLoading(false)
    }
  }

  const handleFileUpload = (e: React.ChangeEvent<HTMLInputElement>) => {
    if (!id || !e.target.files?.length) return
    const files = Array.from(e.target.files)
    e.target.value = '' // 같은 파일 재선택 허용

    // 공고번호 패턴 zip인데 현재 공고번호와 불일치하는 파일 검출
    const currentSolNo = opp?.solicitationNumber?.trim().toUpperCase()
    if (currentSolNo) {
      const mismatched = files
        .map((f) => ({ name: f.name, solNo: fileBaseAsSolNo(f.name) }))
        .filter((f): f is MismatchedFile => !!f.solNo && f.solNo !== currentSolNo)
      if (mismatched.length > 0) {
        setPendingUpload({ files, mismatched })
        return
      }
    }
    uploadFiles(files)
  }

  // CR-034: 저장된 첨부 다운로드 (blob 응답을 받아 브라우저 저장)
  const handleDownload = async (a: Attachment) => {
    if (!id) return
    try {
      const res = await downloadOpportunityAttachment(id, a.id)
      const url = window.URL.createObjectURL(new Blob([res.data]))
      const link = document.createElement('a')
      link.href = url
      link.download = a.fileName || 'attachment'
      document.body.appendChild(link)
      link.click()
      link.remove()
      window.URL.revokeObjectURL(url)
    } catch (err) {
      console.error('첨부 다운로드 실패:', err)
      toast.error('다운로드에 실패했습니다.')
    }
  }

  // CR-034: 수동 업로드 첨부 삭제 (SAM 수집 첨부는 버튼 자체가 없음)
  const confirmDelete = async () => {
    if (!id || !deleteTarget) return
    await deleteOpportunityAttachment(id, deleteTarget.id)
    setDeleteTarget(null)
    fetchData()
  }

  if (loading) return <Loading className="h-64" />

  if (!opp) {
    return (
      <div className="space-y-4">
        <PageHeader variant="detail" back={{ onClick: () => navigate('/opportunities') }} />
        <p className="py-12 text-center text-sm text-muted-foreground">원본 공고를 찾을 수 없습니다.</p>
      </div>
    )
  }

  const pieeLine = pieeFetchLine(opp)

  return (
    <div className="space-y-4">
      <div className="space-y-1">
        <PageHeader
          variant="detail"
          back={{ onClick: () => navigate('/opportunities') }}
          title={opp.titleKo || opp.title}
          badge={
            <>
              {opp.noticeCount > 0 ? (
                <Badge variant="success" appearance="light" className="whitespace-nowrap">
                  공고문 {opp.noticeCount}개
                </Badge>
              ) : (
                <ToneBadge tone="gray">공고문 미생성</ToneBadge>
              )}
              {opp.translatedAt ? (
                <Badge
                  variant="info"
                  appearance="light"
                  className="whitespace-nowrap"
                  title={`번역 완료: ${new Date(opp.translatedAt).toLocaleString('ko')}`}
                >
                  <Languages />
                  번역됨
                </Badge>
              ) : (
                <Badge variant="warning" appearance="light" className="whitespace-nowrap" title="제목 한글화 미완료, 영문 fallback">
                  미번역 (영문 표시)
                </Badge>
              )}
            </>
          }
          actions={
            <>
              <Button
                variant="outline"
                onClick={handleRetranslate}
                disabled={translateLoading}
                title="원본 본문을 LLM으로 한글 번역 (자동 실패 시 수동)"
              >
                {translateLoading ? <Spinner /> : <Languages />}
                한글 번역하기
              </Button>
              <Button
                variant="primary"
                onClick={handleCreateNotice}
                disabled={actionLoading || attachments.length === 0}
                title={attachments.length === 0 ? '첨부파일이 있어야 공고문을 만들 수 있습니다' : undefined}
              >
                <WandSparkles />
                공고문 만들기
              </Button>
            </>
          }
        />
        {opp.titleKo && <p className="text-sm text-muted-foreground">{opp.title}</p>}
      </div>

      {/* CR-021 2차: SAM.gov 원본 메타 패널 (페이지 헤더가 별도로 있으므로 hideHeader) */}
      <OpportunityMetaPanel
        hideHeader
        data={{
          title: opp.title,
          koreanTitle: opp.titleKo,
          solicitationNumber: opp.solicitationNumber,
          type: opp.typeKo || opp.type,
          organizationName: opp.organizationName,
          postedDate: opp.postedDate,
          responseDeadline: opp.responseDeadline,
          active: opp.active,
          uiLink: opp.uiLink,
          // CR-022 한글화 라벨이 있으면 우선
          setAside: opp.setAsideKo || opp.setAside,
          naicsCode: opp.naicsLabelKo
            ? [opp.naicsCode, opp.naicsLabelKo].filter(Boolean).join(', ')
            : opp.naicsCode,
          classificationCode: opp.classificationCode,
          placeOfPerformance: opp.placeOfPerformanceShort || opp.placeOfPerformance,
          description: opp.descriptionSummaryKo || opp.description,
          pointOfContact: opp.pointOfContact,
          resourceLinks: opp.resourceLinks,
        }}
      />

      <Card>
        <CardHeader>
          <CardTitle>첨부파일</CardTitle>
          <CardToolbar>
            <Input
              ref={fileInputRef}
              type="file"
              multiple
              className="hidden"
              onChange={handleFileUpload}
              disabled={uploadLoading}
            />
            <Button size="sm" variant="outline" onClick={() => fileInputRef.current?.click()} disabled={uploadLoading}>
              {uploadLoading ? <Spinner /> : <Upload />}
              {uploadLoading ? '업로드 중...' : '수동 업로드'}
            </Button>
          </CardToolbar>
        </CardHeader>
        <CardContent className="space-y-3">
          {/* CR-034: PIEE 입찰서류 안내. 첨부 유무와 무관하게 항상 노출. 입찰서류 정본/추가본이 PIEE에 있을 수 있음 */}
          {/* CR-043: pieeLinkBroken=true면 빨강 경고로 전환. 일부 공고는 PIEE에 직링크로 안 열려(메인 리다이렉트) 헛클릭 방지 */}
          {opp.solicitationNumber && (
            <Alert variant={opp.pieeLinkBroken ? 'destructive' : 'warning'} appearance="light">
              <AlertContent className="grow">
                {opp.pieeLinkBroken && (
                  <AlertTitle className="flex items-center gap-1.5 text-destructive">
                    <TriangleAlert className="size-4" />
                    이 공고의 PIEE 링크는 오류로 표시됨, 직링크가 PIEE 메인으로 튕길 수 있습니다. 클릭 전 확인.
                  </AlertTitle>
                )}
                <AlertDescription>
                  <p>
                    입찰서류 정본(본 공고서·수정본·추가서류)은 PIEE에도 게시될 수 있습니다.
                    아래에서 공고번호 <span className="font-semibold">{opp.solicitationNumber}</span>로 확인 후,
                    필요한 파일을 받아 "수동 업로드" 하세요.
                  </p>
                  {pieeLine && (
                    <div className="mb-2 flex flex-wrap items-center gap-2">
                      <ToneBadge tone={pieeLine.tone}>{pieeLine.text}</ToneBadge>
                      {pieeLine.detail && <span className="text-destructive">{pieeLine.detail}</span>}
                    </div>
                  )}
                  <div className="flex flex-wrap items-center gap-2">
                    {opp.pieeUrl && (
                      <Button asChild variant={opp.pieeLinkBroken ? 'destructive' : 'primary'}>
                        <a href={opp.pieeUrl} target="_blank" rel="noreferrer">
                          <SquareArrowOutUpRight />
                          PIEE 입찰서류 보기
                        </a>
                      </Button>
                    )}
                    <Button
                      type="button"
                      variant="outline"
                      className={opp.pieeLinkBroken ? undefined : 'text-destructive'}
                      onClick={handleTogglePieeLinkBroken}
                      disabled={actionLoading}
                    >
                      {opp.pieeLinkBroken ? <RotateCcw /> : <Flag />}
                      {opp.pieeLinkBroken ? '오류 표식 해제' : 'PIEE 링크 오류로 표시'}
                    </Button>
                  </div>
                </AlertDescription>
              </AlertContent>
            </Alert>
          )}
          {opp.manualFetchRequiredCount > 0 && (
            <Alert variant="warning" appearance="light">
              <AlertIcon>
                <TriangleAlert />
              </AlertIcon>
              <AlertDescription>
                외부 사이트 첨부 {opp.manualFetchRequiredCount}건, 아래 링크에서 직접 받아 "수동 업로드" 하세요.
              </AlertDescription>
            </Alert>
          )}
          {attachments.length === 0 ? (
            <p className="text-sm text-muted-foreground">첨부파일 없음</p>
          ) : (
            <ul className="space-y-2">
              {attachments.map((a) => (
                <li key={a.id} className="flex items-center justify-between gap-2 rounded-lg border border-border px-3 py-2 text-sm">
                  <div className="min-w-0">
                    <div className="truncate font-medium text-foreground">
                      {a.fileName || '(이름 없음)'}
                      {formatFileSize(a.fileSize) && (
                        <span className="ml-2 text-xs font-normal text-muted-foreground">{formatFileSize(a.fileSize)}</span>
                      )}
                    </div>
                    {/* CR-160: PIEE 에서 받은 파일의 출처는 주소가 아니라 "piee:공고번호/압축 안 경로" 라 링크를 걸지 않는다 */}
                    {a.sourceUrl && a.sourceUrl !== 'admin-upload' && !a.sourceUrl.startsWith('piee:') && (
                      <a
                        href={a.sourceUrl}
                        target="_blank"
                        rel="noreferrer"
                        className="inline-flex max-w-full items-center gap-1 truncate text-xs text-secondary hover:underline"
                      >
                        <ExternalLink className="size-4 shrink-0" />
                        외부 원본 링크
                      </a>
                    )}
                  </div>
                  <div className="flex shrink-0 items-center gap-1.5">
                    {a.manualFetchRequired ? (
                      <Badge variant="warning" appearance="light" className="whitespace-nowrap">가져와야 함</Badge>
                    ) : a.downloadStatus === 'SUCCESS' ? (
                      <Badge variant="success" appearance="light" className="whitespace-nowrap">수집됨</Badge>
                    ) : (
                      <ToneBadge tone="gray">{a.downloadStatus}</ToneBadge>
                    )}
                    {/* CR-034: 저장된 파일(수집됨)은 다운로드 가능 */}
                    {a.downloadStatus === 'SUCCESS' && (
                      <Button
                        variant="outline"
                        mode="icon"
                        size="icon-xs"
                        onClick={() => handleDownload(a)}
                        title="다운로드"
                        aria-label="다운로드"
                      >
                        <Download />
                      </Button>
                    )}
                    {/* CR-034: 관리자 수동 업로드분만 삭제 가능. SAM 수집 첨부는 삭제 버튼 없음 */}
                    {a.sourceUrl === 'admin-upload' && (
                      <Button
                        variant="outline"
                        mode="icon"
                        size="icon-xs"
                        className="text-destructive"
                        onClick={() => setDeleteTarget(a)}
                        disabled={actionLoading}
                        title="삭제 (수동 업로드분)"
                        aria-label="삭제"
                      >
                        <Trash2 />
                      </Button>
                    )}
                  </div>
                </li>
              ))}
            </ul>
          )}
        </CardContent>
      </Card>

      <ConfirmDialog
        open={pendingUpload !== null}
        onOpenChange={(open) => { if (!open) setPendingUpload(null) }}
        title="다른 공고의 서류일 수 있습니다"
        description={`다음 파일 이름이 지금 공고번호(${opp.solicitationNumber ?? ''})와 다릅니다.\n다른 공고의 입찰서류를 잘못 올린 것은 아닌지 확인하세요.`}
        confirmLabel="그대로 올리기"
        onConfirm={() => {
          const files = pendingUpload?.files ?? []
          setPendingUpload(null)
          uploadFiles(files)
        }}
      >
        {pendingUpload && (
          <ul className="space-y-1 text-sm">
            {pendingUpload.mismatched.map((f, i) => (
              <li key={i} className="break-all">
                <span className="font-medium text-foreground">{f.name}</span>
                <span className="ml-2 text-muted-foreground">파일의 공고번호 {f.solNo}</span>
              </li>
            ))}
          </ul>
        )}
      </ConfirmDialog>

      <ConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => { if (!open) setDeleteTarget(null) }}
        title="첨부 삭제"
        description={`"${deleteTarget?.fileName || '이 첨부'}" 을 삭제합니다.`}
        confirmLabel="삭제"
        confirmVariant="destructive"
        onConfirm={confirmDelete}
        errorText={() => '삭제에 실패했습니다. SAM 수집 첨부는 삭제할 수 없습니다.'}
      />
    </div>
  )
}
