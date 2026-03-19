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
import { cn } from '../../lib/utils'
import { createBidRequest } from '../../api/client'
import type { AiStatus } from '../../types'

const AI_STEPS = [
  '공고 분석 중...',
  '요구사항 추출 중...',
  '제안서 초안 작성 중...',
  '검토 및 최적화 중...',
  '최종본 생성 중...',
]

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
  const [status, setStatus] = useState<AiStatus>('idle')
  const [currentStep, setCurrentStep] = useState(0)
  const [error, setError] = useState('')

  // 모달 열릴 때마다 상태 초기화
  useEffect(() => {
    if (isOpen) {
      setStatus('idle')
      setCurrentStep(0)
      setError('')
    }
  }, [isOpen])

  // 단계 자동 진행 (API 응답 후에만)
  useEffect(() => {
    if (status !== 'generating') return

    if (currentStep < AI_STEPS.length) {
      const timer = setTimeout(() => {
        setCurrentStep((s) => s + 1)
      }, 1200)
      return () => clearTimeout(timer)
    } else {
      setStatus('completed')
    }
  }, [status, currentStep])

  const startGeneration = async () => {
    if (!opportunityId) {
      setError('공고 정보가 올바르지 않습니다.')
      return
    }
    setError('')
    setCurrentStep(0)
    try {
      // 실제 백엔드 API 호출: 입찰 요청 생성
      await createBidRequest(opportunityId)
      // 성공 시 애니메이션 시작
      setStatus('generating')
    } catch (e: unknown) {
      const err = e as { response?: { data?: { message?: string }; status?: number } }
      if (err.response?.status === 409) {
        setError('이미 해당 공고에 대한 입찰 요청이 존재합니다.')
      } else {
        setError('입찰 요청 생성에 실패했습니다. 다시 시도해주세요.')
      }
    }
  }

  const handleClose = () => {
    setStatus('idle')
    setCurrentStep(0)
    setError('')
    onClose()
  }

  const goToProposals = () => {
    handleClose()
    navigate('/proposals')
  }

  return (
    <Dialog open={isOpen} onOpenChange={handleClose}>
      <DialogContent showCloseButton={status !== 'generating'}>
        <DialogHeader>
          <div className="flex items-center gap-3">
            <div className="bg-secondary/10 text-secondary p-2 rounded-lg">
              <i className="fa-solid fa-wand-magic-sparkles" />
            </div>
            <div>
              <DialogTitle>AI 제안서 자동 작성</DialogTitle>
              <p className="text-xs text-gray-500 mt-0.5 font-mono">{noticeId}</p>
            </div>
          </div>
        </DialogHeader>

        <DialogBody>
          {status === 'idle' && (
            <div>
              <p className="text-sm text-gray-600 mb-3">
                아래 공고에 대한 AI 제안서를 자동으로 생성합니다.
              </p>
              <div className="bg-gray-50 border border-gray-200 rounded-lg px-4 py-3 text-sm font-medium text-gray-800 line-clamp-2">
                {noticeTitle}
              </div>
              <div className="mt-4 p-3 bg-blue-50 rounded-lg border border-blue-100">
                <p className="text-xs text-blue-700">
                  <i className="fa-solid fa-circle-info mr-1" />
                  입찰 요청이 생성되고 AI가 백그라운드에서 요구사항 분석 및 문서를 자동 작성합니다.
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
          )}

          {status === 'generating' && (
            <div>
              <p className="text-sm text-gray-600 mb-4">제안서를 생성하고 있습니다. 잠시만 기다려주세요...</p>
              <div className="space-y-3">
                {AI_STEPS.map((step, idx) => {
                  const done = idx < currentStep
                  const active = idx === currentStep
                  return (
                    <div key={idx} className="flex items-center gap-3">
                      <div className={cn(
                        'w-6 h-6 rounded-full flex items-center justify-center flex-shrink-0 text-xs',
                        done && 'bg-accent text-white',
                        active && 'bg-secondary/10',
                        !done && !active && 'bg-gray-100'
                      )}>
                        {done && <i className="fa-solid fa-check text-xs" />}
                        {active && <div className="loader" style={{ width: 14, height: 14, border: '2px solid #e2e8f0', borderTopColor: '#3b82f6' }} />}
                        {!done && !active && <span className="text-gray-400">{idx + 1}</span>}
                      </div>
                      <span className={cn(
                        'text-sm',
                        done && 'text-gray-400 line-through',
                        active && 'text-gray-800 font-medium',
                        !done && !active && 'text-gray-400'
                      )}>
                        {step}
                      </span>
                    </div>
                  )
                })}
              </div>
            </div>
          )}

          {status === 'completed' && (
            <div className="text-center py-4">
              <div className="w-16 h-16 bg-green-100 rounded-full flex items-center justify-center mx-auto mb-4">
                <i className="fa-solid fa-check text-2xl text-accent" />
              </div>
              <h3 className="text-lg font-bold text-gray-800 mb-2">입찰 요청 접수 완료!</h3>
              <p className="text-sm text-gray-500">
                입찰 요청이 생성되었습니다.<br />
                AI가 백그라운드에서 요구사항 분석과 문서 작성을 진행합니다.<br />
                <span className="text-xs text-gray-400 mt-1 block">완료까지 몇 분이 소요될 수 있습니다.</span>
              </p>
            </div>
          )}
        </DialogBody>

        <DialogFooter>
          {status === 'idle' && (
            <>
              <Button variant="outline" onClick={handleClose}>취소</Button>
              <Button variant="accent" onClick={startGeneration}>
                <i className="fa-solid fa-wand-magic-sparkles mr-1" /> 생성 시작
              </Button>
            </>
          )}
          {status === 'generating' && (
            <Button variant="outline" disabled>생성 중...</Button>
          )}
          {status === 'completed' && (
            <>
              <Button variant="outline" onClick={handleClose}>닫기</Button>
              <Button variant="primary" onClick={goToProposals}>
                <i className="fa-solid fa-folder-open mr-1" /> 제안서 확인하기
              </Button>
            </>
          )}
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
