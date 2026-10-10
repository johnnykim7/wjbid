/**
 * CR-124 — 공지사항(Notices) 페이지.
 * WJbid(미군·연방 조달 입찰 대행) 공지 목록. 나라장터식 공공 톤 + 네이비 헤더.
 * BE 미연동 — 아래 NOTICES는 더미 데이터(플레이스홀더). 추후 API 교체.
 */
import { useState } from 'react'
import { Link } from 'react-router-dom'
import { Badge, Button } from '@therecommerce-develop/ds-ui'

interface Notice {
  id: number
  title: string
  date: string
  important: boolean
  isNew: boolean
  body: string
}

// 더미 데이터 (BE 미연동)
const NOTICES: Notice[] = [
  {
    id: 6,
    title: 'SAM.gov 등록 갱신 안내 — 매년 갱신 필수',
    date: '2026-06-25',
    important: true,
    isNew: true,
    body: 'SAM.gov 사업자 등록(Entity Registration)은 매년 갱신해야 유효합니다. 갱신 누락 시 입찰 참여가 제한되니, 만료 60일 전 알림을 확인하시고 사전에 갱신을 진행해 주세요.',
  },
  {
    id: 5,
    title: 'WJbid 제안서 작성 AI 엔진 v3 업데이트',
    date: '2026-06-18',
    important: false,
    isNew: true,
    body: '제안서 초안 작성 엔진이 v3로 업데이트되었습니다. 공고 요구사항(PWS/Instructions) 분석 정밀도가 향상되어 필수 서류·자격요건 추출 정확도가 개선되었습니다.',
  },
  {
    id: 4,
    title: '서버 정기 점검 안내 (06/30 02:00~04:00 KST)',
    date: '2026-06-15',
    important: true,
    isNew: false,
    body: '안정적인 서비스 제공을 위해 정기 점검을 진행합니다. 점검 시간 동안 공고 검색·제안서 작성 신청이 일시 중단됩니다. 이용에 참고 부탁드립니다.',
  },
  {
    id: 3,
    title: 'NAICS 코드 기반 공고 추천 기능 추가',
    date: '2026-06-08',
    important: false,
    isNew: false,
    body: '회사 프로필에 등록한 NAICS 코드를 기준으로 관련 USFK 조달 공고를 자동 추천하는 기능이 추가되었습니다. 프로필에서 NAICS 코드를 등록해 보세요.',
  },
  {
    id: 1,
    title: 'WJbid 정식 서비스 오픈',
    date: '2026-05-16',
    important: true,
    isNew: false,
    body: 'WJbid 미군(USFK)·연방 조달 입찰 대행 서비스가 정식 오픈했습니다. SAM.gov 공고 수집부터 제안서 작성·전문가 검토·제출까지 한 번에 대행해 드립니다.',
  },
]

export default function NoticesPage() {
  const [openId, setOpenId] = useState<number | null>(null)

  return (
    <div className="flex-1 overflow-y-auto">
      {/* 페이지 헤더 (제목 + 부제 — 전 페이지 통일) */}
      <div className="text-center mb-10 pt-2">
        <h1 className="text-2xl font-bold text-ink">공지사항</h1>
        <p className="mt-2 text-ink-mute">
          WJbid 서비스 업데이트와 운영 소식을 안내해 드립니다.
        </p>
      </div>

      {/* 목록 */}
      <div className="max-w-3xl mx-auto px-6 py-12">
        <div className="bg-white rounded-xl border border-line overflow-hidden">
          {NOTICES.map((notice) => {
            const open = openId === notice.id
            return (
              <div key={notice.id} className="border-b border-line-soft last:border-b-0">
                <Button
                  type="button"
                  variant="ghost"
                  onClick={() => setOpenId(open ? null : notice.id)}
                  className="w-full h-auto flex items-center justify-start gap-3 px-5 py-4 text-left rounded-none"
                >
                  <div className="flex gap-1.5 shrink-0">
                    {notice.important && (
                      <Badge className="bg-brand text-white text-[11px]">중요</Badge>
                    )}
                    {notice.isNew && (
                      <Badge className="bg-new text-white text-[11px]">NEW</Badge>
                    )}
                  </div>
                  <span className="flex-1 text-sm font-semibold text-ink truncate">
                    {notice.title}
                  </span>
                  <span className="text-xs text-ink-mute shrink-0">{notice.date}</span>
                  <i
                    className={`fa-solid fa-chevron-down text-ink-faint text-xs transition-transform ${
                      open ? 'rotate-180' : ''
                    }`}
                  />
                </Button>
                {open && (
                  <div className="px-5 pb-5 pt-1 text-sm text-ink-sub leading-relaxed bg-surface">
                    {notice.body}
                  </div>
                )}
              </div>
            )
          })}
        </div>

        <p className="text-center text-xs text-ink-mute mt-6">
          문의 사항은{' '}
          <Link to="/support" className="text-brand font-medium hover:underline">
            고객센터
          </Link>
          를 이용해 주세요.
        </p>
      </div>
    </div>
  )
}
