import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import {
  Dialog,
  DialogContent,
  DialogHeader,
  DialogTitle,
  DialogBody,
  DialogFooter,
} from '../ui/dialog'
import { Button } from '../ui/button'
import { createBidRequest } from '../../api/client'

interface AiProposalModalProps {
  isOpen: boolean
  onClose: () => void
  opportunityId: string  // UUID for API call
  noticeId: string       // solicitationNumber for display
  noticeTitle: string
}

export function AiProposalModal({
  isOpen,
  onClose,
  opportunityId,
  noticeId,
  noticeTitle,
}: AiProposalModalProps) {
  const navigate = useNavigate()
  const [submitting, setSubmitting] = useState(false)
  const [error, setError] = useState('')

  // 모달 열릴 때마다 상태 초기화
  useEffect(() => {
    if (isOpen) {
      setSubmitting(false)
      setError('')
    }
  }, [isOpen])

  const handleClose = () => {
    setSubmitting(false)
    setError('')
    onClose()
  }

  const submitRequest = async () => {
    if (!opportunityId) {
      setError('공고 정보가 올바르지 않습니다.')
      return
    }
    setError('')
    setSubmitting(true)
    try {
      // 입찰 요청 생성 → 생성된 제안서(bidRequest) 상세로 바로 이동
      const res = await createBidRequest(opportunityId)
      const bidId = res.data?.id
      handleClose()
      // 첨부(필요서류) 업로드 화면(제출 서류 탭)으로 자동 이동
      navigate(bidId ? `/proposals/${bidId}?tab=uploads` : '/proposals')
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string }; status?: number } }
      if (err.response?.status === 409) {
        setError('이미 해당 공고에 대한 입찰 요청이 존재합니다.')
      } else {
        setError('입찰 요청 생성에 실패했습니다. 다시 시도해주세요.')
      }
      setSubmitting(false)
    }
  }

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent showCloseButton={!submitting}>
        <DialogHeader>
          <div className="flex items-center gap-3">
            <div className="bg-secondary/10 text-secondary p-2 rounded-lg">
              <i className="fa-solid fa-wand-magic-sparkles" />
            </div>
            <div>
              <DialogTitle>제안서 작성 신청</DialogTitle>
              <p className="text-xs text-gray-500 mt-0.5 font-mono">{noticeId}</p>
            </div>
          </div>
        </DialogHeader>

        <DialogBody>
          <div>
            <p className="text-sm text-gray-600 mb-3">
              아래 공고에 대한 제안서 작성을 신청합니다.
            </p>
            <div className="bg-gray-50 border border-gray-200 rounded-lg px-4 py-3 text-sm font-medium text-gray-800 line-clamp-2">
              {noticeTitle}
            </div>
            <div className="mt-4 p-3 bg-blue-50 rounded-lg border border-blue-100">
              <p className="text-xs text-blue-700">
                <i className="fa-solid fa-circle-info mr-1" />
                신청 후 <strong>내 제안서</strong> 화면으로 이동합니다. 필요서류를 업로드하면 요구사항 분석 및 문서 작성이 진행됩니다.
              </p>
            </div>
            {error && (
              <div className="mt-3 p-3 bg-red-50 rounded-lg border border-red-100">
                <p className="text-xs text-red-600">
                  <i className="fa-solid fa-triangle-exclamation mr-1" />
                  {error}
                </p>
              </div>
            )}
          </div>
        </DialogBody>

        <DialogFooter>
          <Button variant="outline" onClick={handleClose} disabled={submitting}>취소</Button>
          <Button variant="accent" onClick={submitRequest} disabled={submitting}>
            {submitting ? (
              <><i className="fa-solid fa-spinner fa-spin mr-1" /> 신청 중...</>
            ) : (
              <><i className="fa-solid fa-wand-magic-sparkles mr-1" /> 신청하기</>
            )}
          </Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
