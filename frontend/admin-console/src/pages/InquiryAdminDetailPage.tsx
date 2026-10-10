/**
 * CR-132 — 1:1 문의 상세와 답변.
 * 답변은 고객 메일이 나간 뒤에만 저장되므로, 메일이 실패하면 쓴 답변을 칸에 그대로 두고 다시 보내게 한다.
 */
import { useCallback, useEffect, useState } from 'react'
import { useNavigate, useParams } from 'react-router-dom'
import axios from 'axios'
import { Button, Textarea } from '@therecommerce-develop/ds-ui'
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

  if (notFound) {
    return (
      <div className="p-6 space-y-4">
        <p className="text-sm text-gray-600">없는 문의입니다.</p>
        <Button variant="outline" size="sm" onClick={() => navigate('/inquiries')}>
          목록으로
        </Button>
      </div>
    )
  }

  if (loadFailed) {
    return (
      <div className="p-6 space-y-4">
        <p className="text-sm text-gray-600">문의를 불러오지 못했습니다.</p>
        <Button variant="outline" size="sm" onClick={load}>
          다시 시도
        </Button>
      </div>
    )
  }

  if (!detail) return null

  const answered = detail.status === 'ANSWERED'
  const notice = detail.adminNotice

  return (
    <div className="p-6 flex flex-col gap-5">
      <Button variant="ghost" size="sm" onClick={backToList} className="self-start px-0 text-[13px] text-gray-500 hover:bg-transparent">
        <i className="fa-solid fa-arrow-left" />
        목록으로
      </Button>

      <div>
        <div className="flex items-center gap-1.5">
          <InquiryStatusPill status={detail.status} />
          <InquiryTypePill label={detail.typeLabel} />
        </div>
        <h1 className="text-lg font-bold text-gray-900 mt-1.5">{detail.title}</h1>
      </div>

      <div className="grid grid-cols-1 lg:grid-cols-[minmax(0,1fr)_320px] gap-5 items-start">
        <div className="flex flex-col gap-5">
          <section className="bg-white border border-gray-200 rounded-xl px-6 py-5">
            <h2 className="text-[15px] font-bold mb-3.5 flex items-center gap-2">
              <i className="fa-regular fa-envelope text-secondary" />
              문의 내용
            </h2>
            <div className="text-sm leading-7 text-gray-700 whitespace-pre-line bg-gray-50 border border-gray-200 rounded-[10px] px-[18px] py-4">
              {detail.content}
            </div>
          </section>

          <section className="bg-white border border-gray-200 rounded-xl px-6 py-5">
            <h2 className="text-[15px] font-bold mb-3.5 flex items-center gap-2">
              <i className="fa-solid fa-reply text-secondary" />
              답변
            </h2>

            {answered ? (
              <div className="border border-emerald-200 bg-emerald-50 rounded-[10px] px-[18px] py-4">
                <div className="text-xs text-emerald-700 mb-2.5 flex flex-wrap gap-3.5">
                  <span>
                    <i className="fa-solid fa-paper-plane mr-1" />
                    보냄 {detail.answeredAt}
                  </span>
                  {detail.answeredByEmail && (
                    <span>
                      <i className="fa-regular fa-user mr-1" />
                      {detail.answeredByEmail}
                    </span>
                  )}
                  <span>
                    <i className="fa-solid fa-check mr-1" />
                    고객 메일 발송 성공
                  </span>
                </div>
                <div className="text-sm leading-7 text-gray-900 whitespace-pre-line">{detail.answer}</div>
              </div>
            ) : (
              <>
                <Textarea
                  value={answer}
                  onChange={(e) => setAnswer(e.target.value)}
                  maxLength={ANSWER_MAX}
                  disabled={sending}
                  placeholder={`${detail.name} 님께 보낼 답변을 적어 주세요.`}
                  className="min-h-[168px] text-sm leading-7 rounded-[10px] px-4 py-3.5"
                />
                {sendError && <p className="mt-3 text-sm text-danger">{sendError}</p>}
                <div className="flex items-center justify-between gap-3 mt-3">
                  <span className="text-xs text-gray-500 leading-relaxed">
                    보내면 {detail.email} 으로 메일이 가고 상태가 답변완료로 바뀝니다. 보낸 답변은 고칠 수 없습니다.
                  </span>
                  <Button variant="secondary" onClick={send} disabled={sending || !answer.trim()} className="shrink-0">
                    <i className="fa-solid fa-paper-plane" />
                    {sending ? '보내는 중' : '답변 보내기'}
                  </Button>
                </div>
              </>
            )}
          </section>
        </div>

        <section className="bg-white border border-gray-200 rounded-xl px-6 py-5">
          <h2 className="text-[15px] font-bold mb-3.5 flex items-center gap-2">
            <i className="fa-regular fa-user text-secondary" />
            문의자
          </h2>
          <dl className="grid grid-cols-[96px_1fr] gap-y-3 text-[13px]">
            <dt className="text-gray-500">이름</dt>
            <dd className="text-gray-900 font-medium break-all">{detail.name}</dd>
            <dt className="text-gray-500">이메일</dt>
            <dd className="text-gray-900 font-medium break-all">{detail.email}</dd>
            <dt className="text-gray-500">구분</dt>
            <dd className="text-gray-900 font-medium flex items-center gap-1.5 flex-wrap">
              <InquiryMemberPill member={!!detail.memberId} />
              {detail.memberCompanyName}
            </dd>
            <dt className="text-gray-500">접수 일시</dt>
            <dd className="text-gray-900 font-medium">{detail.createdAt}</dd>
            <dt className="text-gray-500">관리자 알림</dt>
            {notice.total === 0 ? (
              <dd className="text-gray-500 font-medium">기록 없음</dd>
            ) : notice.success > 0 ? (
              <dd className="text-emerald-700 font-medium">
                <i className="fa-solid fa-check mr-1" />
                보냄 {notice.lastSentAt} <span className="whitespace-nowrap">({notice.success}명)</span>
              </dd>
            ) : (
              <dd className="text-danger font-medium">
                <i className="fa-solid fa-xmark mr-1" />
                보내지 못함 ({notice.total}명)
              </dd>
            )}
          </dl>
        </section>
      </div>
    </div>
  )
}
