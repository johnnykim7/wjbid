import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { Plus } from 'lucide-react'
import {
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTable,
  CardTitle,
  CardToolbar,
  Dialog,
  DialogBody,
  DialogContent,
  DialogDescription,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Input,
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
  Textarea,
  toast,
} from '@therecommerce-develop/ds-ui'
import {
  getRfpSamples, createRfpSample, getPatternGuides, extractPatternGuide, updatePatternGuide,
} from '../api/client'
import { ToneBadge } from '../components/StateBadge'
import { TableMessageRow } from '../components/LoadState'
import type { StateTone } from '../utils/bidState'

interface RfpSample {
  id: string
  opportunityNo: string
  industryType: string
  outcome: string
  company?: string
  agency?: string
  awardAmount?: number
  fileCount: number
  createdAt?: string
}

interface PatternGuide {
  id: string
  industryType: string
  status: string
  source: string
  guideMarkdown?: string
  sampleCount?: number
}

const INDUSTRY_TYPES = [
  'GROUND_MAINTENANCE', 'CUSTODIAL', 'LAUNDRY', 'HVAC',
  'WASTE', 'PIPELINE', 'SECURITY', 'FACILITY_LEASE',
]
const OUTCOMES = ['WON', 'SUBMITTED', 'OTHER']

const OUTCOME_TONE: Record<string, StateTone> = {
  WON: 'success',
  SUBMITTED: 'info',
  OTHER: 'gray',
}

const STATUS_TONE: Record<string, StateTone> = {
  PENDING: 'gray',
  EXTRACTING: 'info',
  COMPLETED: 'success',
  FAILED: 'destructive',
}

const SOURCE_TONE: Record<string, StateTone> = {
  AI_EXTRACTED: 'info',
  HUMAN_EDITED: 'success',
  HUMAN_ADDED: 'success',
}

const EMPTY_FORM = {
  opportunityNo: '', industryType: 'GROUND_MAINTENANCE', outcome: 'WON',
  company: '', agency: '', awardAmount: '', fiscalYear: '',
}

export default function RfpSamplePage() {
  const navigate = useNavigate()
  const [samples, setSamples] = useState<RfpSample[]>([])
  const [guides, setGuides] = useState<PatternGuide[]>([])
  const [loading, setLoading] = useState(true)
  const [showModal, setShowModal] = useState(false)
  const [form, setForm] = useState({ ...EMPTY_FORM })
  const [saving, setSaving] = useState(false)
  const [extracting, setExtracting] = useState<string | null>(null)
  const [editGuide, setEditGuide] = useState<{ industryType: string; markdown: string } | null>(null)
  const [savingGuide, setSavingGuide] = useState(false)

  const fetchData = async () => {
    setLoading(true)
    try {
      const [sRes, gRes] = await Promise.all([getRfpSamples(0), getPatternGuides()])
      setSamples(sRes.data.content || [])
      setGuides(gRes.data || [])
    } catch (err) {
      console.error('성공 제안서 목록 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => { fetchData() }, [])

  const handleSave = async () => {
    if (!form.opportunityNo.trim()) return
    setSaving(true)
    try {
      await createRfpSample({
        opportunityNo: form.opportunityNo.trim(),
        industryType: form.industryType,
        outcome: form.outcome,
        company: form.company || undefined,
        agency: form.agency || undefined,
        awardAmount: form.awardAmount ? Number(form.awardAmount) : undefined,
        fiscalYear: form.fiscalYear ? Number(form.fiscalYear) : undefined,
      })
      setShowModal(false)
      setForm({ ...EMPTY_FORM })
      fetchData()
    } catch (err) {
      console.error('등록 실패:', err)
    } finally {
      setSaving(false)
    }
  }

  const handleExtract = async (industryType: string) => {
    setExtracting(industryType)
    try {
      await extractPatternGuide(industryType)
      fetchData()
    } catch (err: unknown) {
      const msg = (err as { response?: { data?: { message?: string } } })?.response?.data?.message
      toast.error(msg || '패턴 추출에 실패했습니다. 그 유형의 성공 제안서가 1건 이상 있어야 합니다.')
    } finally {
      setExtracting(null)
    }
  }

  const handleSaveGuide = async () => {
    if (!editGuide) return
    setSavingGuide(true)
    try {
      await updatePatternGuide(editGuide.industryType, { guideMarkdown: editGuide.markdown })
      setEditGuide(null)
      fetchData()
    } catch (err) {
      console.error('가이드 저장 실패:', err)
    } finally {
      setSavingGuide(false)
    }
  }

  // 유형별 가이드 맵 (없으면 미생성)
  const guideByType = new Map(guides.map((g) => [g.industryType, g]))

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader>
          <CardTitle>공고유형별 패턴 가이드</CardTitle>
        </CardHeader>
        <CardContent>
          <div className="grid grid-cols-2 gap-3 md:grid-cols-4">
            {INDUSTRY_TYPES.map((type) => {
              const g = guideByType.get(type)
              return (
                <div key={type} className="rounded-lg border border-border p-3">
                  <div className="mb-2 flex items-center justify-between gap-2">
                    <span className="truncate text-sm font-medium text-foreground">{type}</span>
                    {g && <ToneBadge tone={STATUS_TONE[g.status] ?? 'gray'}>{g.status}</ToneBadge>}
                  </div>
                  <div className="flex items-center justify-between gap-1">
                    {g ? (
                      <ToneBadge tone={SOURCE_TONE[g.source] ?? 'gray'}>
                        {g.source === 'AI_EXTRACTED' ? 'AI' : '사람편집'}
                      </ToneBadge>
                    ) : (
                      <span className="text-xs text-muted-foreground">미생성</span>
                    )}
                    <div className="flex items-center gap-1">
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => setEditGuide({ industryType: type, markdown: g?.guideMarkdown || '' })}
                        title="사람이 직접 편집"
                      >
                        편집
                      </Button>
                      <Button
                        size="sm"
                        variant="outline"
                        onClick={() => handleExtract(type)}
                        disabled={extracting === type}
                      >
                        {extracting === type ? '추출중...' : (g ? '재추출' : '추출')}
                      </Button>
                    </div>
                  </div>
                  {g?.sampleCount != null && (
                    <div className="mt-1 text-xs text-muted-foreground">{g.sampleCount}건 기반</div>
                  )}
                </div>
              )
            })}
          </div>
        </CardContent>
      </Card>

      <Card>
        <CardHeader className="py-2.5">
          <CardToolbar className="ms-auto">
            <Button variant="primary" onClick={() => setShowModal(true)}>
              <Plus />
              신규 등록
            </Button>
          </CardToolbar>
        </CardHeader>
        <CardTable>
          <Table>
            <TableHeader className="bg-muted">
              <TableRow>
                <TableHead>공고번호</TableHead>
                <TableHead>사업유형</TableHead>
                <TableHead className="text-center">결과</TableHead>
                <TableHead>업체</TableHead>
                <TableHead className="text-center">파일</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableMessageRow colSpan={5} loading />
              ) : samples.length === 0 ? (
                <TableMessageRow colSpan={5}>등록된 성공 제안서가 없습니다.</TableMessageRow>
              ) : (
                samples.map((s) => (
                  <TableRow key={s.id} className="cursor-pointer" onClick={() => navigate(`/rfp-samples/${s.id}`)}>
                    <TableCell className="font-mono text-foreground">{s.opportunityNo}</TableCell>
                    <TableCell className="text-muted-foreground">{s.industryType}</TableCell>
                    <TableCell className="text-center">
                      <ToneBadge tone={OUTCOME_TONE[s.outcome] ?? 'gray'}>{s.outcome}</ToneBadge>
                    </TableCell>
                    <TableCell className="text-muted-foreground">{s.company || '-'}</TableCell>
                    <TableCell className="text-center text-muted-foreground">{s.fileCount}</TableCell>
                  </TableRow>
                ))
              )}
            </TableBody>
          </Table>
        </CardTable>
      </Card>

      <Dialog open={showModal} onOpenChange={setShowModal}>
        <DialogContent className="max-w-lg">
          <DialogHeader>
            <DialogTitle>성공 제안서 등록</DialogTitle>
          </DialogHeader>
          <DialogBody className="space-y-4">
            <div className="space-y-1">
              <Label htmlFor="rfp-opportunity-no">공고번호 <span className="text-destructive">*</span></Label>
              <Input
                id="rfp-opportunity-no"
                value={form.opportunityNo}
                onChange={(e) => setForm({ ...form, opportunityNo: e.target.value })}
                placeholder="예: W90VN725RA012"
              />
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label htmlFor="rfp-industry-type">사업유형 <span className="text-destructive">*</span></Label>
                <Select value={form.industryType} onValueChange={(v) => setForm({ ...form, industryType: v })}>
                  <SelectTrigger id="rfp-industry-type">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {INDUSTRY_TYPES.map((t) => <SelectItem key={t} value={t}>{t}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
              <div className="space-y-1">
                <Label htmlFor="rfp-outcome">결과 <span className="text-destructive">*</span></Label>
                <Select value={form.outcome} onValueChange={(v) => setForm({ ...form, outcome: v })}>
                  <SelectTrigger id="rfp-outcome">
                    <SelectValue />
                  </SelectTrigger>
                  <SelectContent>
                    {OUTCOMES.map((o) => <SelectItem key={o} value={o}>{o}</SelectItem>)}
                  </SelectContent>
                </Select>
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label htmlFor="rfp-company">업체</Label>
                <Input
                  id="rfp-company"
                  value={form.company}
                  onChange={(e) => setForm({ ...form, company: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor="rfp-agency">발주처</Label>
                <Input
                  id="rfp-agency"
                  value={form.agency}
                  onChange={(e) => setForm({ ...form, agency: e.target.value })}
                />
              </div>
            </div>
            <div className="grid grid-cols-2 gap-3">
              <div className="space-y-1">
                <Label htmlFor="rfp-award-amount">낙찰액 (원)</Label>
                <Input
                  id="rfp-award-amount"
                  type="number"
                  value={form.awardAmount}
                  onChange={(e) => setForm({ ...form, awardAmount: e.target.value })}
                />
              </div>
              <div className="space-y-1">
                <Label htmlFor="rfp-fiscal-year">회계연도</Label>
                <Input
                  id="rfp-fiscal-year"
                  type="number"
                  value={form.fiscalYear}
                  onChange={(e) => setForm({ ...form, fiscalYear: e.target.value })}
                />
              </div>
            </div>
          </DialogBody>
          <DialogFooter>
            <Button variant="outline" onClick={() => setShowModal(false)}>취소</Button>
            <Button variant="primary" onClick={handleSave} disabled={saving || !form.opportunityNo.trim()}>
              {saving ? <><Spinner />저장 중...</> : '등록'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>

      {/* 가이드 편집 창 (사람이 직접 인사이트 반영) */}
      <Dialog open={editGuide !== null} onOpenChange={(open) => { if (!open) setEditGuide(null) }}>
        <DialogContent className="max-w-2xl">
          <DialogHeader>
            <DialogTitle>{`가이드 편집, ${editGuide?.industryType ?? ''}`}</DialogTitle>
            <DialogDescription>저장 시 출처가 '사람편집'으로 바뀌어 이후 AI 자동추출이 덮어쓰지 않습니다.</DialogDescription>
          </DialogHeader>
          {editGuide && (
            <DialogBody>
              <Textarea
                value={editGuide.markdown}
                onChange={(e) => setEditGuide({ ...editGuide, markdown: e.target.value })}
                placeholder="이 공고유형 제안서를 잘 쓰는 법(전략·구조·금기 등)을 자유롭게 작성하세요."
                className="h-80 resize-none font-mono"
              />
            </DialogBody>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setEditGuide(null)}>취소</Button>
            <Button variant="primary" onClick={handleSaveGuide} disabled={savingGuide}>
              {savingGuide ? <><Spinner />저장 중...</> : '저장'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
