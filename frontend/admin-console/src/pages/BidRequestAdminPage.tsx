import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Alert,
  AlertDescription,
  Button,
  Card,
  CardFooter,
  CardHeader,
  CardTable,
  Dialog,
  DialogBody,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Label,
  RadioGroup,
  RadioGroupItem,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Textarea,
} from '@therecommerce-develop/ds-ui'
import { getAdminBidRequests, transitionBidRequest } from '../api/client'
import { STATE_LABELS } from '../utils/bidState'
import StateBadge from '../components/StateBadge'
import ListPagination from '../components/ListPagination'
import { TableMessageRow } from '../components/LoadState'

const ALL_STATES = [
  'CREATED', 'DOCS_PENDING', 'DOCS_RECEIVED',
  'ANALYZING', 'GENERATING', 'REVIEW',
  'CONFIRMED', 'SUBMITTED', 'CLOSED',
]

// 상태별 유효한 다음 상태 (FSM)
const NEXT_STATES: Record<string, string[]> = {
  CREATED: ['DOCS_PENDING', 'CLOSED'],
  DOCS_PENDING: ['DOCS_RECEIVED', 'CLOSED'],
  DOCS_RECEIVED: ['ANALYZING', 'CLOSED'],
  ANALYZING: ['GENERATING'],
  GENERATING: ['REVIEW'],
  REVIEW: ['CONFIRMED', 'GENERATING'],
  CONFIRMED: ['SUBMITTED', 'REVIEW'],
}

interface BidRequest {
  id: string
  state: string
  opportunityTitle?: string
  agencyName?: string
  memberEmail?: string
  assignedTo?: string
  createdAt?: string
}

interface TransitionModal {
  bidRequestId: string
  currentState: string
  opportunityTitle: string
}

export default function BidRequestAdminPage() {
  const [requests, setRequests] = useState<BidRequest[]>([])
  const [filterState, setFilterState] = useState<string>('')
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [modal, setModal] = useState<TransitionModal | null>(null)
  const [selectedNextState, setSelectedNextState] = useState('')
  const [notes, setNotes] = useState('')
  const [transitioning, setTransitioning] = useState(false)
  const [error, setError] = useState('')
  const navigate = useNavigate()

  const load = async (state: string, p: number) => {
    setLoading(true)
    try {
      const { data } = await getAdminBidRequests(state || undefined, p)
      setRequests(data.content ?? [])
      setTotalPages(data.totalPages ?? 0)
    } finally {
      setLoading(false)
    }
  }

  useEffect(() => {
    setPage(0)
    load(filterState, 0)
  }, [filterState])

  useEffect(() => {
    load(filterState, page)
  }, [page])

  const openTransition = (br: BidRequest) => {
    setModal({ bidRequestId: br.id, currentState: br.state, opportunityTitle: br.opportunityTitle ?? '-' })
    setSelectedNextState(NEXT_STATES[br.state]?.[0] ?? '')
    setNotes('')
    setError('')
  }

  const handleTransition = async () => {
    if (!modal || !selectedNextState) return
    setTransitioning(true)
    setError('')
    try {
      await transitionBidRequest(modal.bidRequestId, selectedNextState, notes)
      setModal(null)
      load(filterState, page)
    } catch {
      setError('상태 전환에 실패했습니다.')
    } finally {
      setTransitioning(false)
    }
  }

  const nextStates = modal ? (NEXT_STATES[modal.currentState] ?? []) : []

  return (
    <div className="space-y-4">
      <Card>
        <CardHeader className="py-2.5">
          <div className="flex flex-wrap gap-1.5">
            {['', ...ALL_STATES].map(s => (
              <Button
                key={s || 'ALL'}
                size="sm"
                variant={filterState === s ? 'primary' : 'outline'}
                onClick={() => setFilterState(s)}
              >
                {s ? STATE_LABELS[s] : '전체'}
              </Button>
            ))}
          </div>
        </CardHeader>
        <CardTable>
          <Table>
            <TableHeader className="bg-muted">
              <TableRow>
                <TableHead>공고 제목</TableHead>
                <TableHead>기관</TableHead>
                <TableHead>회원</TableHead>
                <TableHead>상태</TableHead>
                <TableHead />
              </TableRow>
            </TableHeader>
            <TableBody>
              {loading ? (
                <TableMessageRow colSpan={5} loading />
              ) : requests.length === 0 ? (
                <TableMessageRow colSpan={5}>해당 조건의 입찰 요청이 없습니다.</TableMessageRow>
              ) : (
                requests.map((br) => {
                  const canTransition = (NEXT_STATES[br.state]?.length ?? 0) > 0
                  return (
                    <TableRow key={br.id}>
                      <TableCell className="max-w-xs font-medium">
                        <Button
                          mode="link"
                          variant="primary"
                          className="block max-w-full truncate text-left text-secondary"
                          onClick={() => navigate(`/bid-requests/${br.id}`)}
                        >
                          {br.opportunityTitle ?? '-'}
                        </Button>
                      </TableCell>
                      <TableCell className="text-xs text-muted-foreground">{br.agencyName ?? '-'}</TableCell>
                      <TableCell className="text-xs text-muted-foreground">{br.memberEmail ?? '-'}</TableCell>
                      <TableCell>
                        <StateBadge state={br.state} />
                      </TableCell>
                      <TableCell className="text-right">
                        {canTransition && (
                          <Button size="sm" variant="outline" onClick={() => openTransition(br)}>
                            상태 전환
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  )
                })
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

      <Dialog open={!!modal} onOpenChange={(open) => { if (!open) setModal(null) }}>
        <DialogContent className="max-w-md">
          <DialogHeader>
            <DialogTitle>상태 전환</DialogTitle>
          </DialogHeader>
          {modal && (
            <DialogBody className="space-y-4">
              <div className="rounded-lg bg-muted px-4 py-3 text-sm">
                <div className="mb-1 truncate font-medium text-foreground">{modal.opportunityTitle}</div>
                <div className="flex items-center gap-2">
                  <StateBadge state={modal.currentState} />
                  <span className="text-xs text-muted-foreground">다음 상태 선택</span>
                </div>
              </div>

              <div className="space-y-2">
                <Label>전환할 상태</Label>
                <RadioGroup value={selectedNextState} onValueChange={setSelectedNextState} className="space-y-2">
                  {nextStates.map(s => (
                    <div key={s} className="flex items-center gap-3">
                      <RadioGroupItem value={s} id={`next-${s}`} />
                      <Label htmlFor={`next-${s}`} className="cursor-pointer">
                        <StateBadge state={s} />
                      </Label>
                    </div>
                  ))}
                </RadioGroup>
              </div>

              <div className="space-y-1">
                <Label htmlFor="transition-notes">메모 (선택)</Label>
                <Textarea
                  id="transition-notes"
                  value={notes}
                  onChange={e => setNotes(e.target.value)}
                  rows={3}
                  placeholder="전환 사유 또는 메모를 입력하세요."
                  className="resize-none"
                />
              </div>

              {error && (
                <Alert variant="destructive" appearance="light">
                  <AlertDescription>{error}</AlertDescription>
                </Alert>
              )}
            </DialogBody>
          )}
          <DialogFooter>
            <Button variant="outline" onClick={() => setModal(null)}>취소</Button>
            <Button variant="primary" onClick={handleTransition} disabled={!selectedNextState || transitioning}>
              {transitioning ? '처리 중' : '전환'}
            </Button>
          </DialogFooter>
        </DialogContent>
      </Dialog>
    </div>
  )
}
