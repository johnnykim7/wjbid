/**
 * CR-132: 1:1 문의 상세와 답변.
 * 답변은 고객 메일이 나간 뒤에만 저장되므로, 메일이 실패하면 쓴 답변을 칸에 그대로 두고 다시 보내게 한다.
 */
import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import axios from 'axios'
import { Check, Send, User, X } from 'lucide-react'
import {
  Alert,
  AlertDescription,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTitle,
  InfoGrid,
  PageHeader,
  Spinner,
  Textarea,
} from '@therecommerce-develop/ds-ui'
import {
  answerAdminInquiry,
  getAdminInquiry,
  INQUIRY_ALREADY_ANSWERED,
  type InquiryDetail,
} from '../api/client'
import { InquiryMemberPill, InquiryStatusPill, InquiryTypePill } from '../components/InquiryPills'
import { notifyInquiryCountsChanged } from '../components/inquiryCounts'

const ANSWER_MAX = 5000

export default function InquiryAdminDetailPage() {
  const { id } = useParams<{ id: string }>()
  const navigate = useNavigate()
  const [detail, setDetail] = useState<InquiryDetail | null>(null)
  const [notFound, setNotFound] = useState(false)
  const [loadFailed, setLoadFailed] = useState(false)
  const [answer, setAnswer] = useState('')
  const [sending, setSending] = useState(false)
  const [sendError, setSendError] = useState<string | null>(null)

  const load = useCallback(() => {
    if (!id) return
    getAdminInquiry(id)
      .then((res) => {
        setLoadFailed(false)
        setDetail(res.data)
      })
      .catch((err) => {
        if (axios.isAxiosError(err) && err.response?.status === 404) setNotFound(true)
        else setLoadFailed(true)
      })
  }, [id])

  useEffect(() => {
    load()
  }, [load])

  // 목록으로는 뒤로 가기로 돌아가야 목록 주소의 거르기와 쪽이 그대로 남는다
  const backToList = () => {
    if (window.history.state?.idx > 0) navigate(-1)
    else navigate('/inquiries')
  }

  const send = () => {
    if (!id || !answer.trim()) return
    setSending(true)
    setSendError(null)
    answerAdminInquiry(id, answer)
      .then((res) => {
        setDetail(res.data)
        setAnswer('')
        notifyInquiryCountsChanged()
      })
      .catch((err) => {
        if (axios.isAxiosError(err) && err.response?.data?.error === INQUIRY_ALREADY_ANSWERED) {
          setSendError('이미 답변된 문의입니다.')
          load()
          notifyInquiryCountsChanged()
        } else {
          setSendError('고객에게 메일을 보내지 못했습니다. 답변은 저장되지 않았습니다. 잠시 뒤 다시 보내 주세요.')
        }
      })
      .finally(() => setSending(false))
  }

  if (notFound || loadFailed) {
    return (
      <div className="space-y-4">
        <PageHeader variant="detail" back={{ onClick: () => navigate('/inquiries') }} />
        <Alert variant="destructive" appearance="light">
          <AlertDescription className="flex items-center justify-between gap-3">
            {notFound ? '없는 문의입니다.' : '문의를 불러오지 못했습니다.'}
            {loadFailed && (
              <Button variant="outline" size="sm" onClick={load}>
                다시 시도
              </Button>
            )}
          </AlertDescription>
        </Alert>
      </div>
    )
  }

  if (!detail) return null

  const answered = detail.status === 'ANSWERED'
  const notice = detail.adminNotice

  const noticeValue =
    notice.total === 0 ? (
      <span className="text-muted-foreground">기록 없음</span>
    ) : notice.success > 0 ? (
      <span className="inline-flex flex-wrap items-center gap-1 text-emerald-700">
        <Check className="size-4" />
        보냄 {notice.lastSentAt} <span className="whitespace-nowrap">({notice.success}명)</span>
      </span>
    ) : (
      <span className="inline-flex items-center gap-1 text-destructive">
        <X className="size-4" />
        보내지 못함 ({notice.total}명)
      </span>
    )

  return (
    <div className="space-y-4">
      <PageHeader
        variant="detail"
        back={{ onClick: backToList }}
        title={detail.title}
        badge={
          <>
            <InquiryStatusPill status={detail.status} />
            <InquiryTypePill label={detail.typeLabel} />
          </>
        }
      />

      <div className="grid grid-cols-1 items-start gap-4 lg:grid-cols-[minmax(0,1fr)_360px]">
        <div className="space-y-4">
          <Card>
            <CardHeader>
              <CardTitle>문의 내용</CardTitle>
            </CardHeader>
            <CardContent>
              <div className="whitespace-pre-line rounded-lg border border-border bg-muted px-4 py-4 text-sm leading-7 text-foreground">
                {detail.content}
              </div>
            </CardContent>
          </Card>

          <Card>
            <CardHeader>
              <CardTitle>답변</CardTitle>
            </CardHeader>
            <CardContent>
              {answered ? (
                <div className="rounded-lg border border-emerald-200 bg-emerald-50 px-4 py-4">
                  <div className="mb-2.5 flex flex-wrap gap-3.5 text-xs text-emerald-700">
                    <span className="inline-flex items-center gap-1">
                      <Send className="size-3.5" />
                      보냄 {detail.answeredAt}
                    </span>
                    {detail.answeredByEmail && (
                      <span className="inline-flex items-center gap-1">
                        <User className="size-3.5" />
                        {detail.answeredByEmail}
                      </span>
                    )}
                    <span className="inline-flex items-center gap-1">
                      <Check className="size-3.5" />
                      고객 메일 발송 성공
                    </span>
                  </div>
                  <div className="whitespace-pre-line text-sm leading-7 text-foreground">{detail.answer}</div>
                </div>
              ) : (
                <div className="space-y-3">
                  {sendError && (
                    <Alert variant="destructive" appearance="light">
                      <AlertDescription>{sendError}</AlertDescription>
                    </Alert>
                  )}
                  <Textarea
                    value={answer}
                    onChange={(e) => setAnswer(e.target.value)}
                    maxLength={ANSWER_MAX}
                    disabled={sending}
                    placeholder={`${detail.name} 님께 보낼 답변을 적어 주세요.`}
                    className="min-h-[168px] text-sm leading-7"
                  />
                  <div className="flex items-center justify-between gap-3">
                    <span className="text-xs leading-relaxed text-muted-foreground">
                      보내면 {detail.email} 으로 메일이 가고 상태가 답변완료로 바뀝니다. 보낸 답변은 고칠 수 없습니다.
                    </span>
                    <Button variant="primary" onClick={send} disabled={sending || !answer.trim()} className="shrink-0">
                      {sending ? <Spinner /> : <Send />}
                      답변 보내기
                    </Button>
                  </div>
                </div>
              )}
            </CardContent>
          </Card>
        </div>

        <Card>
          <CardHeader>
            <CardTitle>문의자</CardTitle>
          </CardHeader>
          <CardContent>
            <InfoGrid
              cols={1}
              items={[
                { label: '이름', value: <span className="break-all">{detail.name}</span> },
                { label: '이메일', value: <span className="break-all">{detail.email}</span> },
                {
                  label: '구분',
                  value: (
                    <span className="flex flex-wrap items-center gap-1.5">
                      <InquiryMemberPill member={!!detail.memberId} />
                      {detail.memberCompanyName}
                    </span>
                  ),
                },
                { label: '접수 일시', value: detail.createdAt },
                { label: '관리자 알림', value: noticeValue },
              ]}
            />
          </CardContent>
        </Card>
      </div>
    </div>
  )
}
