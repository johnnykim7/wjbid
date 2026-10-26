import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { ArrowRight, Languages, Paperclip, Plus, Search, Trash2, TriangleAlert } from 'lucide-react'
import {
  Alert,
  AlertDescription,
  Badge,
  Button,
  Card,
  CardFooter,
  CardHeader,
  CardTable,
  CardToolbar,
  ConfirmDialog,
  Dialog,
  DialogBody,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  FieldError,
  InfoGrid,
  Input,
  InputWrapper,
  Label,
  Select,
  SelectContent,
  SelectItem,
  SelectTrigger,
  SelectValue,
  Spinner,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  toast,
} from '@therecommerce-develop/ds-ui'
import { getAdminOpportunities, getAdminOpportunityTypes, deleteOpportunity, translateOpportunityTitle, createManualOpportunity } from '../api/client'
import { DdayBadge } from '../utils/dday'
import { ToneBadge } from '../components/StateBadge'
import ListPagination from '../components/ListPagination'
import { TableMessageRow } from '../components/LoadState'

interface OpportunityAdmin {
  id: string
  noticeId: string
  solicitationNumber?: string
  title: string
  titleKo?: string          // CR-022: 한글 제목 (없으면 영문 fallback)
  typeKo?: string           // CR-022: SAM type 한글 라벨
  translatedAt?: string     // CR-022: 마지막 번역 시각
  organizationName?: string
  postedDate?: string
  responseDeadline?: string
  attachmentCount: number
  manualFetchRequiredCount: number
  noticeCount: number
  pieeLinkBroken?: boolean   // CR-043: PIEE 링크 오류 표식
}

// Select 는 빈 문자열 값을 받지 않아 "전체" 를 이 값으로 두고 조회할 때 뺀다
const ALL = 'ALL'
type BoolFilter = typeof ALL | 'true' | 'false'

const toBool = (v: BoolFilter) => (v === ALL ? undefined : v === 'true')

const deleteErrorText = (err: any) =>
  err?.response?.data?.reason ||
  (err?.response?.status === 409
    ? '노출 중이거나 분석 중인 공고문이 연결돼 있어 삭제할 수 없습니다.'
    : '삭제에 실패했습니다.')

export default function OpportunityAdminPage() {
  const navigate = useNavigate()
  const [opportunities, setOpportunities] = useState<OpportunityAdmin[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [batchTranslating, setBatchTranslating] = useState(false)
  const [batchProgress, setBatchProgress] = useState({ done: 0, total: 0 })
  const [batchConfirmCount, setBatchConfirmCount] = useState<number | null>(null)
  const [deleteTarget, setDeleteTarget] = useState<OpportunityAdmin | null>(null)

  // CR-120: 테스트용 수동 공고 등록 모달
  const [manualOpen, setManualOpen] = useState(false)
  const [manualNoticeId, setManualNoticeId] = useState('')
  const [manualTitle, setManualTitle] = useState('')
  const [manualSaving, setManualSaving] = useState(false)
  const [manualTitleError, setManualTitleError] = useState('')
  const [manualError, setManualError] = useState('')

  // CR-118: 검색·필터
  const [keywordInput, setKeywordInput] = useState('')   // 입력 중(엔터/버튼으로 확정)
  const [keyword, setKeyword] = useState('')             // 실제 조회에 쓰는 확정 값
  const [type, setType] = useState<string>(ALL)
  const [hasAttachment, setHasAttachment] = useState<BoolFilter>(ALL)
  const [hasNotice, setHasNotice] = useState<BoolFilter>(ALL)   // 생성여부
  const [types, setTypes] = useState<Array<{ type: string; typeKo: string }>>([])

  const fetchData = async () => {
    setLoading(true)
    try {
      const { data } = await getAdminOpportunities(page, {
        keyword: keyword || undefined,
        type: type === ALL ? undefined : type,
        hasAttachment: toBool(hasAttachment),
        hasNotice: toBool(hasNotice),
      })
      setOpportunities(data.content || [])
      setTotalPages(data.totalPages || 0)
    } catch (err) {
      console.error('원본 공고 목록 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }

  // 공고유형 셀렉트 옵션 1회 로드
  useEffect(() => {
    getAdminOpportunityTypes()
      .then(({ data }) => setTypes(data || []))
      .catch((err) => console.error('공고유형 목록 조회 실패:', err))
  }, [])

  // 필터/페이지 변경 시 재조회 (keyword는 확정 값 기준)
  useEffect(() => { fetchData() }, [page, keyword, type, hasAttachment, hasNotice])

  // 필터(유형/첨부/생성여부) 또는 키워드 확정 시 첫 페이지로
  const applyKeyword = () => { setPage(0); setKeyword(keywordInput.trim()) }
  const onTypeChange = (v: string) => { setPage(0); setType(v) }
  const onAttachmentChange = (v: string) => { setPage(0); setHasAttachment(v as BoolFilter) }
  const onNoticeChange = (v: string) => { setPage(0); setHasNotice(v as BoolFilter) }
  const filtered = !!keyword || type !== ALL || hasAttachment !== ALL || hasNotice !== ALL
  const resetFilters = () => {
    setKeywordInput(''); setKeyword(''); setType(ALL); setHasAttachment(ALL); setHasNotice(ALL); setPage(0)
  }

  const untranslatedCount = opportunities.filter((o) => !o.translatedAt).length

  const openBatchTranslate = () => {
    if (untranslatedCount === 0) {
      toast.info('이 페이지에 미번역 공고가 없습니다.')
      return
    }
    setBatchConfirmCount(untranslatedCount)
  }

  // 현재 페이지 미번역분(제목 한글화 미완료) 일괄 번역.
  // 제목만 번역하므로 SAM 쿼터를 소진하지 않는다(본문 fetch 없음).
  //
  // 직렬(1건씩) 처리한다. 백엔드 제목 번역은 Aimbase CLI 러너(anthropic-cli)를 타는데,
  // 동시 호출을 던지면 러너 세션이 꼬여 모든 호출이 멈추는 사고가 있었다.
  // 한 건 끝나야 다음 건을 보낸다.
  const runBatchTranslate = async () => {
    const targets = opportunities.filter((o) => !o.translatedAt)
    setBatchTranslating(true)
    setBatchProgress({ done: 0, total: targets.length })

    let done = 0
    let failed = 0
    for (const opp of targets) {
      try {
        const { data } = await translateOpportunityTitle(opp.id)
        // 성공(status=TRANSLATED)일 때만 해당 행을 즉시 갱신해 미번역 배지가 바로 사라진다.
        if (data?.status === 'TRANSLATED') {
          setOpportunities((prev) =>
            prev.map((o) =>
              o.id === opp.id
                ? { ...o, titleKo: data.titleKo || o.titleKo, translatedAt: data.translatedAt || new Date().toISOString() }
                : o,
            ),
          )
        } else {
          failed += 1
        }
      } catch (err) {
        // 타임아웃·에러는 실패로 집계하고 다음 건으로 진행(best-effort).
        console.error('제목 번역 실패:', opp.id, err)
        failed += 1
      } finally {
        done += 1
        setBatchProgress({ done, total: targets.length })
      }
    }
    setBatchTranslating(false)
    const ok = targets.length - failed
    if (failed > 0) {
      toast.info(`일괄 번역을 마쳤습니다. 성공 ${ok}건, 실패 ${failed}건. 실패 건은 잠시 후 다시 시도해 주세요.`)
    } else {
      toast.success(`일괄 번역을 마쳤습니다. 성공 ${ok}건.`)
    }
    // 최종 동기화(혹시 모를 누락 보정)
    fetchData()
  }

  // CR-120: 테스트용 수동 공고 등록 모달 열기. 공고번호 기본값(TEST-{timestamp})을 미리 채운다.
  const openManual = () => {
    setManualNoticeId(`TEST-${Date.now()}`)
    setManualTitle('')
    setManualTitleError('')
    setManualError('')
    setManualOpen(true)
  }

  // CR-120: 수동 공고 생성 후 상세로 이동(바로 첨부 업로드 가능).
  const handleCreateManual = async () => {
    if (!manualTitle.trim()) {
      setManualTitleError('제목을 입력해 주세요.')
      return
    }
    setManualSaving(true)
    setManualError('')
    try {
      const { data } = await createManualOpportunity(manualNoticeId.trim(), manualTitle.trim())
      setManualOpen(false)
      navigate(`/opportunities/${data.opportunityId}`)
    } catch (err: any) {
      if (err?.response?.status === 409) {
        setManualError(err.response.data?.reason || '이미 존재하는 공고번호입니다.')
      } else {
        setManualError('테스트 공고 생성에 실패했습니다.')
        console.error('수동 공고 생성 실패:', err)
      }
    } finally {
      setManualSaving(false)
    }
  }

  // CR-042: 원본 공고 소프트 삭제.
  const confirmDelete = async () => {
    if (!deleteTarget) return
    await deleteOpportunity(deleteTarget.id)
    setDeleteTarget(null)
    // 현재 페이지가 마지막 1건이었으면 이전 페이지로, 아니면 재조회
    if (opportunities.length === 1 && page > 0) {
      setPage((p) => p - 1)
    } else {
      fetchData()
    }
  }

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="py-2.5">
          {/* CR-118: 검색·필터. 키워드(제목·본문·공고번호), 공고유형, 첨부유무, 생성여부 */}
          <div className="flex flex-wrap items-center gap-2">
            <InputWrapper className="w-64">
              <Search />
              <Input
                value={keywordInput}
                onChange={(e) => setKeywordInput(e.target.value)}
                onKeyDown={(e) => { if (e.key === 'Enter') applyKeyword() }}
                placeholder="제목·본문·공고번호 검색"
              />
            </InputWrapper>
            <Select value={type} onValueChange={onTypeChange}>
              <SelectTrigger className="w-40">
                <SelectValue placeholder="공고유형 전체" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>공고유형 전체</SelectItem>
                {types.map((t) => (
                  <SelectItem key={t.type} value={t.type}>{t.typeKo}</SelectItem>
                ))}
              </SelectContent>
            </Select>
            <Select value={hasAttachment} onValueChange={onAttachmentChange}>
              <SelectTrigger className="w-32">
                <SelectValue placeholder="첨부 전체" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>첨부 전체</SelectItem>
                <SelectItem value="true">첨부 있음</SelectItem>
                <SelectItem value="false">첨부 없음</SelectItem>
              </SelectContent>
            </Select>
            <Select value={hasNotice} onValueChange={onNoticeChange}>
              <SelectTrigger className="w-36">
                <SelectValue placeholder="생성여부 전체" />
              </SelectTrigger>
              <SelectContent>
                <SelectItem value={ALL}>생성여부 전체</SelectItem>
                <SelectItem value="true">생성됨</SelectItem>
                <SelectItem value="false">미생성</SelectItem>
              </SelectContent>
            </Select>
            <Button variant="outline" onClick={applyKeyword}>검색</Button>
            {filtered && (
              <Button variant="dim" onClick={resetFilters}>초기화</Button>
            )}
          </div>
          <CardToolbar>
            {/* CR-120: SAM 수집 없이 테스트용 공고 1건 수동 등록 (PWS 기반 제안서 시뮬레이션 진입점) */}
            <Button
              variant="outline"
              onClick={openManual}
              title="SAM 수집 없이 테스트용 공고를 만든다. 이후 PWS 첨부를 올려 분석하고 제안서를 시뮬레이션한다"
            >
              <Plus />
              테스트 공고 추가
            </Button>
            <Button
              variant="outline"
              onClick={openBatchTranslate}
              disabled={batchTranslating}
              title="현재 페이지의 미번역 공고 제목을 일괄 번역 (SAM 쿼터 미소진)"
            >
              {batchTranslating ? (
                <><Spinner />번역 중 {batchProgress.done}/{batchProgress.total}</>
              ) : (
                <><Languages />이 페이지 일괄 번역{untranslatedCount > 0 ? ` (${untranslatedCount})` : ''}</>
              )}
            </Button>
            <Button variant="primary" onClick={() => navigate('/notices')}>
              공고문 리스트
              <ArrowRight />
            </Button>
          </CardToolbar>
        </CardHeader>
        <CardTable>
          <Table className="table-fixed">
            <colgroup>
              <col />
              <col className="w-32" />
              <col className="w-32" />
              <col className="w-44" />
              <col className="w-24" />
              <col className="w-28" />
              <col className="w-16" />
            </colgroup>
            <TableHeader className="bg-muted">
              <TableRow>
                <TableHead>공고 (원문)</TableHead>
                <TableHead className="whitespace-nowrap">공고유형</TableHead>
                <TableHead className="whitespace-nowrap">공고일</TableHead>
                <TableHead className="whitespace-nowrap">마감일</TableHead>
                <TableHead className="whitespace-nowrap text-center">첨부</TableHead>
                <TableHead className="whitespace-nowrap text-center">생성여부</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableMessageRow colSpan={7} loading />
              ) : opportunities.length === 0 ? (
                <TableMessageRow colSpan={7}>원본 공고가 없습니다.</TableMessageRow>
              ) : (
                opportunities.map((opp) => (
                  <TableRow
                    key={opp.id}
                    className="cursor-pointer"
                    onClick={() => navigate(`/opportunities/${opp.id}`)}
                  >
                    <TableCell>
                      <div className="truncate font-medium text-foreground">{opp.titleKo || opp.title}</div>
                      {opp.titleKo && (
                        <div className="truncate text-xs text-muted-foreground">{opp.title}</div>
                      )}
                      <div className="mt-0.5 flex items-center gap-2 text-xs text-muted-foreground">
                        <span className="font-mono">{opp.solicitationNumber || opp.noticeId}</span>
                        {!opp.translatedAt && (
                          <Badge
                            variant="warning"
                            appearance="light"
                            size="sm"
                            className="whitespace-nowrap"
                            title="제목 한글화 미완료, 영문 표시"
                          >
                            미번역
                          </Badge>
                        )}
                        {opp.pieeLinkBroken && (
                          <Badge
                            variant="destructive"
                            appearance="light"
                            size="sm"
                            className="whitespace-nowrap"
                            title="PIEE 링크 오류로 표시됨, 직링크가 안 열릴 수 있음"
                          >
                            <TriangleAlert />
                            PIEE 오류
                          </Badge>
                        )}
                      </div>
                    </TableCell>
                    <TableCell className="text-muted-foreground">
                      <div className="truncate" title={opp.typeKo || ''}>{opp.typeKo || '-'}</div>
                    </TableCell>
                    <TableCell className="whitespace-nowrap text-muted-foreground">
                      {opp.postedDate ? new Date(opp.postedDate).toLocaleDateString('ko') : '-'}
                    </TableCell>
                    <TableCell className="whitespace-nowrap text-muted-foreground">
                      {opp.responseDeadline ? (
                        <div className="flex items-center gap-1.5">
                          <span>{new Date(opp.responseDeadline).toLocaleDateString('ko')}</span>
                          <DdayBadge deadline={opp.responseDeadline} />
                        </div>
                      ) : '-'}
                    </TableCell>
                    <TableCell className="text-center">
                      {opp.attachmentCount > 0 ? (
                        <span
                          className="inline-flex items-center gap-1 text-xs text-muted-foreground"
                          title={`첨부 ${opp.attachmentCount}건`}
                        >
                          <Paperclip className="size-4" />
                          {opp.attachmentCount}
                        </span>
                      ) : (
                        <Badge variant="warning" appearance="light" className="whitespace-nowrap" title="첨부파일 없음">
                          없음
                        </Badge>
                      )}
                    </TableCell>
                    <TableCell className="text-center">
                      {opp.noticeCount > 0 ? (
                        <Badge variant="success" appearance="light" className="whitespace-nowrap">생성</Badge>
                      ) : (
                        <ToneBadge tone="gray">미생성</ToneBadge>
                      )}
                    </TableCell>
                    <TableCell className="text-center">
                      {/* 행 클릭(상세 이동)과 분리하기 위해 stopPropagation */}
                      <Button
                        variant="dim"
                        mode="icon"
                        size="icon-sm"
                        className="hover:text-destructive"
                        title="공고 삭제(소프트)"
                        aria-label="공고 삭제"
                        onClick={(e) => { e.stopPropagation(); setDeleteTarget(opp) }}
                      >
                        <Trash2 />
                      </Button>
                    </TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardTable>
        {totalPages > 1 && (
          <CardFooter className="justify-center py-2">
            <ListPagination page={page} totalPages={totalPages} onChange={setPage} />
          </CardFooter>
        )}
      </Card>

      <ConfirmDialog
        open={batchConfirmCount !== null}
        onOpenChange={(open) => { if (!open) setBatchConfirmCount(null) }}
        title="이 페이지 일괄 번역"
        description={`이 페이지의 미번역 ${batchConfirmCount ?? 0}건 제목을 1건씩 차례로 번역합니다.\nSAM 쿼터는 쓰지 않고 건당 수십 초가 걸립니다.`}
        confirmLabel="번역 시작"
        onConfirm={() => { setBatchConfirmCount(null); runBatchTranslate() }}
      />

      <ConfirmDialog
        open={deleteTarget !== null}
        onOpenChange={(open) => { if (!open) setDeleteTarget(null) }}
        title="공고 삭제"
        description="원본 데이터는 보존되고 목록, 검색, 고객 노출에서만 빠집니다."
        confirmLabel="삭제"
        confirmVariant="destructive"
        onConfirm={confirmDelete}
        errorText={deleteErrorText}
      >
        {deleteTarget && (
          <InfoGrid size="dialog" cols={1} items={[{ label: '공고', value: deleteTarget.titleKo || deleteTarget.title }]} />
        )}
      </ConfirmDialog>

      {/* CR-120: 테스트용 수동 공고 등록 모달 */}
      <Dialog open={manualOpen} onOpenChange={(open) => { if (!open && !manualSaving) setManualOpen(false) }}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>테스트 공고 추가</DialogTitle>
          </DialogHeader>
          <DialogBody className="space-y-4">
            {manualError && (
              <Alert variant="destructive" appearance="light">
                <AlertDescription>{manualError}</AlertDescription>
              </Alert>
            )}
            <p className="text-sm text-muted-foreground">
              SAM 수집 없이 빈 공고를 만듭니다. 생성 후 상세 화면에서 PWS 첨부를 올려 분석·제안서 생성을 시뮬레이션하세요.
            </p>
            <div className="space-y-1">
              <Label htmlFor="manual-notice-id">공고번호</Label>
              <Input
                id="manual-notice-id"
                value={manualNoticeId}
                onChange={(e) => setManualNoticeId(e.target.value)}
                placeholder="비워두면 자동 생성"
                className="font-mono"
              />
              <p className="text-xs text-muted-foreground">기본값(TEST-…)을 그대로 써도 됩니다. 중복이면 거부됩니다.</p>
            </div>
            <div className="space-y-1">
              <Label htmlFor="manual-title">
                제목 <span className="text-destructive">*</span>
              </Label>
              <Input
                id="manual-title"
                value={manualTitle}
                onChange={(e) => { setManualTitle(e.target.value); setManualTitleError('') }}
                onKeyDown={(e) => { if (e.key === 'Enter') handleCreateManual() }}
                placeholder="예: 411th CSB, NTV 구매 (테스트)"
                aria-invalid={!!manualTitleError}
                aria-describedby="manual-title-error"
                autoFocus
              />
              <FieldError id="manual-title-error">{manualTitleError}</FieldError>
            </div>
          </DialogBody>
          <DialogFooter>
            <Button variant="outline" onClick={() => setManualOpen(false)} disabled={manualSaving}>
              취소
            </Button>
            <Button variant="primary" onClick={handleCreateManual} disabled={manualSaving}>
              {manualSaving && <Spinner />}
              {manualSaving ? '생성 중…' : '생성하고 상세로 이동'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
