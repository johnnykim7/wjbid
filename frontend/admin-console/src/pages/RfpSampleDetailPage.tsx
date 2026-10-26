import { useEffect, useRef, useState, useCallback } from 'react'
import { useParams, useNavigate } from 'react-router-dom'
import { FileIcon, FileText, ScrollText, Trash2, Upload, X } from 'lucide-react'
import {
  Button,
  Card,
  CardContent,
  CardDescription,
  CardHeader,
  CardTitle,
  CardToolbar,
  ConfirmDialog,
  InfoGrid,
  Input,
  PageHeader,
} from '@therecommerce-develop/ds-ui'
import {
  getRfpSampleDetail, uploadRfpSampleFile, deleteRfpSampleFile, deleteRfpSample,
} from '../api/client'
import { ToneBadge } from '../components/StateBadge'
import { Loading } from '../components/LoadState'
import type { StateTone } from '../utils/bidState'

interface RfpFile {
  id: string
  fileName: string
  fileSize?: number
  contentType?: string
  isPws: boolean
}

interface RfpSampleDetail {
  id: string
  opportunityNo: string
  industryType: string
  outcome: string
  company?: string
  agency?: string
  awardAmount?: number
  fiscalYear?: number
  note?: string
  files: RfpFile[]
}

const OUTCOME_TONE: Record<string, StateTone> = {
  WON: 'success',
  SUBMITTED: 'info',
  OTHER: 'gray',
}

export default function RfpSampleDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [detail, setDetail] = useState<RfpSampleDetail | null>(null)
  const [loading, setLoading] = useState(true)
  const [uploading, setUploading] = useState(false)
  const [actionBusy, setActionBusy] = useState(false)
  const [confirmDelete, setConfirmDelete] = useState(false)
  const proposalInput = useRef<HTMLInputElement>(null)
  const pwsInput = useRef<HTMLInputElement>(null)

  const fetchData = useCallback(async () => {
    if (!id) return
    setLoading(true)
    try {
      const { data } = await getRfpSampleDetail(id)
      setDetail(data)
    } catch (err) {
      console.error('상세 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }, [id])

  useEffect(() => { fetchData() }, [fetchData])

  const handleUpload = async (e: React.ChangeEvent<HTMLInputElement>, isPws: boolean) => {
    if (!id || !e.target.files?.[0]) return
    setUploading(true)
    try {
      await uploadRfpSampleFile(id, e.target.files[0], isPws)
      fetchData()
    } catch (err) {
      console.error('파일 업로드 실패:', err)
    } finally {
      setUploading(false)
      e.target.value = ''
    }
  }

  const handleDeleteFile = async (fileId: string) => {
    if (!id) return
    setActionBusy(true)
    try {
      await deleteRfpSampleFile(id, fileId)
      fetchData()
    } finally {
      setActionBusy(false)
    }
  }

  const handleDeleteSample = async () => {
    if (!id) return
    await deleteRfpSample(id)
    setConfirmDelete(false)
    navigate('/rfp-samples')
  }

  if (loading) return <Loading className="h-64" />

  if (!detail) {
    return (
      <div className="space-y-4">
        <PageHeader variant="detail" back={{ onClick: () => navigate('/rfp-samples') }} />
        <p className="text-sm text-muted-foreground">제안서를 찾을 수 없습니다.</p>
      </div>
    )
  }

  return (
    <div className="space-y-4">
      <PageHeader
        variant="detail"
        back={{ onClick: () => navigate('/rfp-samples') }}
        title={detail.opportunityNo}
        badge={<ToneBadge tone={OUTCOME_TONE[detail.outcome] ?? 'gray'}>{detail.outcome}</ToneBadge>}
        actions={
          <Button variant="outline" className="text-destructive" onClick={() => setConfirmDelete(true)}>
            <Trash2 />
            삭제
          </Button>
        }
      />

      <Card>
        <CardContent>
          <InfoGrid
            cols={2}
            items={[
              { label: '사업유형', value: detail.industryType },
              { label: '업체', value: detail.company ?? '-' },
              { label: '발주처', value: detail.agency ?? '-' },
              { label: '낙찰액', value: detail.awardAmount != null ? `${detail.awardAmount.toLocaleString()}원` : '-' },
            ]}
          />
        </CardContent>
      </Card>

      {/* 원본 파일 (FACTOR별로 나뉜 그대로 보관하고 슬롯으로 나누지 않는다) */}
      <Card>
        <CardHeader className="py-3">
          <div className="space-y-1">
            <CardTitle>원본 파일</CardTitle>
            <CardDescription>제출 시 나뉜 파일(FACTOR/섹션)을 그대로 업로드하세요. 파일명이 곧 섹션 태그입니다.</CardDescription>
          </div>
          <CardToolbar>
            <Button size="sm" variant="outline" disabled={uploading} onClick={() => proposalInput.current?.click()}>
              <Upload />
              {uploading ? '업로드 중...' : '제안서 파일'}
            </Button>
            <Input ref={proposalInput} type="file" className="hidden" onChange={(e) => handleUpload(e, false)} />
            <Button size="sm" variant="outline" disabled={uploading} onClick={() => pwsInput.current?.click()}>
              <FileText />
              PWS 공고문
            </Button>
            <Input ref={pwsInput} type="file" className="hidden" onChange={(e) => handleUpload(e, true)} />
          </CardToolbar>
        </CardHeader>
        <CardContent>
          {detail.files.length === 0 ? (
            <p className="text-sm text-muted-foreground">업로드된 파일이 없습니다.</p>
          ) : (
            <div className="space-y-1.5">
              {detail.files.map((f) => (
                <div key={f.id} className="flex items-center justify-between rounded-lg bg-muted px-3 py-2">
                  <div className="flex min-w-0 items-center gap-2">
                    {f.isPws
                      ? <ScrollText className="size-4 shrink-0 text-muted-foreground" />
                      : <FileIcon className="size-4 shrink-0 text-secondary" />}
                    <span className="truncate text-sm text-foreground">{f.fileName}</span>
                    {f.isPws && <ToneBadge tone="gray">PWS</ToneBadge>}
                  </div>
                  <Button
                    variant="ghost"
                    mode="icon"
                    size="icon-xs"
                    aria-label="삭제"
                    className="text-muted-foreground hover:text-destructive"
                    onClick={() => handleDeleteFile(f.id)}
                    disabled={actionBusy}
                  >
                    <X />
                  </Button>
                </div>
              ))}
            </div>
          )}
        </CardContent>
      </Card>

      <ConfirmDialog
        open={confirmDelete}
        onOpenChange={setConfirmDelete}
        title="성공 제안서 삭제"
        description="올린 파일도 모두 삭제됩니다."
        confirmLabel="삭제"
        confirmVariant="destructive"
        onConfirm={handleDeleteSample}
        errorText={() => '삭제에 실패했습니다.'}
      />
    </div>
  )
}
