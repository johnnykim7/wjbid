import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { Button, Input } from '@therecommerce-develop/ds-ui'
import { getOpportunities } from '../api/client'
import { DdayBadge } from '../utils/dday'
import type { Opportunity } from '../types'

/* CR-127 — 메인 전면 개편: 마케팅 랜딩 → 업무용 포털(나라장터 홈 격자형)
 *  격자: [좌] 시작하기 + 알림판 / [중] 공고문 / [우] 공지사항 + 관련사이트·고객센터
 *  나라장터 홈은 공고 목록이 아니라 공지·안내 허브 구조 — 그 골격을 그대로 따르되
 *  중앙 자리(나라장터=공지사항)에 우리 핵심 콘텐츠인 공고문을 배치.
 *  전체 검색·필터·페이징은 /search(SearchPage)가 담당하고 여기선 진입만 시킨다.
 */

// 좌상단 "시작하기" — 나라장터 5버튼 구성 대응. 로그인 여부와 무관하게 고정.
// 아직 없는 페이지(동영상 매뉴얼·SAM 등록 가이드)는 임시로 /guide 로 연결.
const START_MENU = [
  { label: '이용자 등록', icon: 'fa-user-plus', to: '/register', tone: 'solid' },
  { label: '이용 안내', icon: 'fa-circle-info', to: '/guide', tone: 'soft' },
  { label: '동영상 매뉴얼', icon: 'fa-circle-play', to: '/guide', tone: 'soft' },
  { label: 'SAM.gov 등록 가이드', icon: 'fa-clipboard-check', to: '/guide', tone: 'dark' },
  { label: '요금 안내', icon: 'fa-won-sign', to: '/pricing', tone: 'dark' },
]

// 알림판 배너 — 이미지 자산 확보 전까지 텍스트 기반 컬러 배너로 자리를 잡는다.
const BANNERS = [
  { tag: '필독', title: 'SAM.gov 등록 갱신', desc: '매년 갱신하지 않으면 입찰 참여가 제한됩니다', bg: 'linear-gradient(135deg,#13294f,#2b50b4)' },
  { tag: 'AI', title: '제안서 자동 작성', desc: '공고 요건 분석부터 초안 작성까지', bg: 'linear-gradient(135deg,#134e4a,#0d9488)' },
  { tag: '안내', title: 'USFK 입찰 첫걸음', desc: 'UEI·CAGE 발급부터 순서대로', bg: 'linear-gradient(135deg,#5b21b6,#7c3aed)' },
  { tag: '알림', title: '마감 임박 공고 알림', desc: '관심 공고를 저장하면 메일로 알려드립니다', bg: 'linear-gradient(135deg,#9a3412,#ea580c)' },
]

// 공지사항 — BE 미연동. NoticesPage 더미와 동일 소스.
const NOTICES = [
  { id: 6, title: 'SAM.gov 등록 갱신 안내 — 매년 갱신 필수', date: '2026-06-25', important: true },
  { id: 5, title: 'WJbid 제안서 작성 AI 엔진 v3 업데이트', date: '2026-06-18', important: false },
  { id: 4, title: '서버 정기 점검 안내 (06/30 02:00~04:00 KST)', date: '2026-06-15', important: true },
  { id: 3, title: 'NAICS 코드 기반 공고 추천 기능 추가', date: '2026-06-08', important: false },
  { id: 2, title: '요금제 개편 안내 (Basic / Professional / Enterprise)', date: '2026-05-30', important: false },
]

const RELATED_SITES = [
  { label: 'SAM.gov', desc: '미 연방 조달 포털', href: 'https://sam.gov' },
  { label: 'PIEE', desc: '국방부 전자조달', href: 'https://piee.eb.mil' },
  { label: 'DLA', desc: '국방군수국', href: 'https://www.dla.mil' },
]

// 이용 절차 3단계 — 중앙 하단 채움 겸 신규 이용자 동선 안내
const STEPS = [
  { icon: 'fa-magnifying-glass', title: '공고 검색', desc: 'SAM.gov 공고를 한글로 확인', to: '/search' },
  { icon: 'fa-file-pen', title: '제안서 작성', desc: 'AI가 요건 분석 후 초안 작성', to: '/proposals' },
  { icon: 'fa-paper-plane', title: '서류 준비·제출', desc: '필수 서류 확인 후 최종 제출', to: '/documents' },
]

// 신고·문의 배너 2칸
const QUICK_BANNERS = [
  { icon: 'fa-headset', title: '문의하기', desc: '입찰·서비스 관련 상담', to: '/support' },
  { icon: 'fa-circle-question', title: '자주 묻는 질문', desc: 'FAQ에서 먼저 찾아보세요', to: '/faq' },
]

const LIST_SIZE = 7

/** 섹션 헤더 — 나라장터식 좌측 컬러바 + 굵은 제목 + 우측 더보기 */
function PanelHead({ title, onMore }: { title: string; onMore?: () => void }) {
  return (
    <div className="flex items-center justify-between px-4 h-[52px] border-b-2 border-[#13294f]">
      <h2 className="flex items-center gap-2 text-[18px] font-black text-[#13294f] tracking-tight">
        <span className="w-[4px] h-[17px] bg-[#2b50b4]" />
        {title}
      </h2>
      {onMore && (
        <button
          onClick={onMore}
          className="w-6 h-6 grid place-items-center rounded-full border border-gray-300 text-gray-500 hover:bg-[#13294f] hover:border-[#13294f] hover:text-white transition-colors"
          title="더보기"
        >
          <i className="fa-solid fa-plus text-[10px]" />
        </button>
      )}
    </div>
  )
}

export default function HomePage() {
  const navigate = useNavigate()

  const [items, setItems] = useState<Opportunity[]>([])
  const [total, setTotal] = useState(0)
  const [loading, setLoading] = useState(true)
  const [keyword, setKeyword] = useState('')
  const [banner, setBanner] = useState(0)

  useEffect(() => {
    getOpportunities(0, LIST_SIZE)
      .then((res) => {
        setItems(res.data.content ?? [])
        setTotal(res.data.totalElements ?? 0)
      })
      .catch(() => setItems([]))
      .finally(() => setLoading(false))
  }, [])

  // 알림판 자동 롤링
  useEffect(() => {
    const t = setInterval(() => setBanner((i) => (i + 1) % BANNERS.length), 4500)
    return () => clearInterval(t)
  }, [])

  const handleSearch = (e: React.FormEvent) => {
    e.preventDefault()
    const kw = keyword.trim()
    navigate(kw ? `/search?keyword=${encodeURIComponent(kw)}` : '/search')
  }

  const b = BANNERS[banner]

  return (
    <div className="space-y-4">
      {/* ───────── 통합 검색 ───────── */}
      <div className="bg-white border border-gray-300 p-4">
        <form onSubmit={handleSearch} className="flex gap-2">
          <div className="flex-1 relative">
            <i className="fa-solid fa-magnifying-glass absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm z-10" />
            <Input
              type="text"
              placeholder="공고번호, 공고명, 발주기관으로 검색하세요"
              value={keyword}
              onChange={(e) => setKeyword(e.target.value)}
              className="w-full pl-11 h-12 rounded-none text-[15px]"
            />
          </div>
          <Button type="submit" className="h-12 px-8 rounded-none font-bold text-[16px]">
            검색
          </Button>
        </form>
      </div>

      {/* ───────── 3열 격자 ───────── */}
      <div className="grid grid-cols-1 lg:grid-cols-[268px_1fr_320px] gap-4 items-start">
        {/* ═══ 좌: 시작하기 + 알림판 ═══ */}
        <div className="space-y-4">
          <section className="bg-white border border-gray-300">
            <PanelHead title="WJbid 시작하기" />
            <div className="p-4">
              <p className="text-[13.5px] text-gray-600 leading-relaxed mb-3.5">
                미군·연방 조달 입찰이 처음이신 이용자를 위한 안내입니다.
              </p>
              <div className="grid grid-cols-2 gap-2">
                {START_MENU.map((m, i) => {
                  const cls =
                    m.tone === 'solid'
                      ? 'bg-[#2b50b4] text-white hover:bg-[#1b3a72] border-[#2b50b4]'
                      : m.tone === 'dark'
                        ? 'bg-[#4b5563] text-white hover:bg-[#374151] border-[#4b5563]'
                        : 'bg-[#eef3fb] text-[#1b3a72] hover:bg-[#dce7f7] border-[#dce7f7]'
                  return (
                    <button
                      key={m.label}
                      onClick={() => navigate(m.to)}
                      className={`border h-[70px] px-2 flex flex-col items-center justify-center gap-1.5 text-[13px] font-bold leading-tight text-center transition-colors ${cls} ${
                        i === START_MENU.length - 1 && START_MENU.length % 2 === 1 ? 'col-span-2' : ''
                      }`}
                    >
                      <i className={`fa-solid ${m.icon} text-[17px]`} />
                      {m.label}
                    </button>
                  )
                })}
              </div>
            </div>
          </section>

          {/* 알림판 */}
          <section className="bg-white border border-gray-300">
            <div className="flex items-center justify-between px-4 h-[46px] border-b border-gray-200">
              <h3 className="text-[16px] font-black text-[#13294f]">알림판</h3>
              <div className="flex items-center gap-1.5">
                <span className="text-[12px] text-gray-500 tabular-nums font-medium">
                  {banner + 1} / {BANNERS.length}
                </span>
                <button
                  onClick={() => setBanner((i) => (i - 1 + BANNERS.length) % BANNERS.length)}
                  className="w-5 h-5 grid place-items-center text-gray-400 hover:text-[#13294f]"
                >
                  <i className="fa-solid fa-chevron-left text-[10px]" />
                </button>
                <button
                  onClick={() => setBanner((i) => (i + 1) % BANNERS.length)}
                  className="w-5 h-5 grid place-items-center text-gray-400 hover:text-[#13294f]"
                >
                  <i className="fa-solid fa-chevron-right text-[10px]" />
                </button>
              </div>
            </div>
            <button
              onClick={() => navigate('/notices')}
              className="block w-full text-left p-5 h-[150px] text-white transition-all"
              style={{ background: b.bg }}
            >
              <span className="inline-block px-2 py-0.5 mb-2.5 text-[10px] font-bold bg-white/20 rounded-sm">
                {b.tag}
              </span>
              <p className="text-[21px] font-black leading-snug mb-1.5">{b.title}</p>
              <p className="text-[13px] text-white/85 leading-relaxed">{b.desc}</p>
            </button>
            <div className="flex justify-center gap-1.5 py-2.5 border-t border-gray-200">
              {BANNERS.map((_, i) => (
                <button
                  key={i}
                  onClick={() => setBanner(i)}
                  className={`h-1.5 rounded-full transition-all ${
                    i === banner ? 'w-4 bg-[#13294f]' : 'w-1.5 bg-gray-300'
                  }`}
                />
              ))}
            </div>
          </section>
        </div>

        {/* ═══ 중앙: 공고문 + 이용 절차 + 바로가기 ═══ */}
        <div className="space-y-4">
        <section className="bg-white border border-gray-300">
          <PanelHead title="입찰 공고문" onMore={() => navigate('/search')} />

          {loading ? (
            <div className="py-20 text-center text-sm text-gray-400">
              <i className="fa-solid fa-spinner fa-spin mr-2" />
              공고를 불러오는 중입니다
            </div>
          ) : items.length === 0 ? (
            <div className="py-20 text-center">
              <i className="fa-solid fa-inbox text-2xl text-gray-300 mb-2" />
              <p className="text-sm text-gray-500">노출 중인 공고가 없습니다</p>
            </div>
          ) : (
            <>
              <ul>
                {items.map((bid) => (
                  <li key={bid.id} className="border-b border-gray-100 last:border-0">
                    <button
                      onClick={() => navigate(`/search/${bid.id}`)}
                      className="w-full text-left px-4 py-3.5 hover:bg-[#f7f9fc] transition-colors group"
                    >
                      <div className="flex items-start gap-3">
                        <div className="min-w-0 flex-1">
                          <p className="text-[15.5px] font-bold text-gray-900 group-hover:text-[#2b50b4] line-clamp-1 mb-1.5">
                            {bid.title}
                          </p>
                          <p className="text-[12.5px] text-gray-600 flex flex-wrap items-center gap-x-2.5 gap-y-0.5">
                            <span className="font-bold text-[#2b50b4]">{bid.solicitationNumber}</span>
                            {bid.agencyName && (
                              <span className="truncate max-w-[300px]" title={bid.agencyName}>
                                {bid.agencyName}
                              </span>
                            )}
                          </p>
                        </div>
                        <div className="shrink-0 text-right leading-tight">
                          <div className="flex items-center justify-end gap-1.5 mb-1">
                            <span className="text-[13px] text-gray-700 tabular-nums font-medium">
                              ~{bid.responseDeadline || '-'}
                            </span>
                            <DdayBadge deadline={bid.responseDeadline} />
                          </div>
                          {bid.postedDate && (
                            <span className="text-[12px] text-gray-500 tabular-nums">
                              게시 {bid.postedDate}
                            </span>
                          )}
                        </div>
                      </div>
                    </button>
                  </li>
                ))}
              </ul>
              <div className="flex items-center justify-between px-4 py-2.5 bg-[#f7f9fc] border-t border-gray-200">
                <span className="text-[13px] text-gray-600">
                  전체 <b className="text-[#13294f]">{total.toLocaleString()}</b>건
                </span>
                <button
                  onClick={() => navigate('/search')}
                  className="text-[13px] font-bold text-[#2b50b4] hover:underline"
                >
                  공고 전체보기 <i className="fa-solid fa-angle-right text-[10px] ml-0.5" />
                </button>
              </div>
            </>
          )}
        </section>

        {/* 이용 절차 — 나라장터 중앙 하단 배너 자리 대응 */}
        <section className="bg-white border border-gray-300">
          <div className="px-4 h-[46px] flex items-center border-b border-gray-200">
            <h3 className="text-[16px] font-black text-[#13294f]">이용 절차</h3>
            <span className="ml-2 text-[12.5px] text-gray-500">공고 검색부터 입찰 제출까지</span>
          </div>
          <div className="grid grid-cols-1 sm:grid-cols-3 divide-y sm:divide-y-0 sm:divide-x divide-gray-200">
            {STEPS.map((st, i) => (
              <button
                key={st.title}
                onClick={() => navigate(st.to)}
                className="flex items-start gap-3 p-4 text-left hover:bg-[#f7f9fc] transition-colors group"
              >
                <span className="shrink-0 w-9 h-9 grid place-items-center bg-[#eef3fb] text-[#1b3a72] group-hover:bg-[#2b50b4] group-hover:text-white transition-colors">
                  <i className={`fa-solid ${st.icon} text-[15px]`} />
                </span>
                <span className="min-w-0">
                  <span className="flex items-center gap-1.5 mb-1">
                    <span className="text-[11px] font-black text-[#2b50b4] tabular-nums">
                      STEP {i + 1}
                    </span>
                  </span>
                  <span className="block text-[14px] font-bold text-gray-900 group-hover:text-[#2b50b4] mb-0.5">
                    {st.title}
                  </span>
                  <span className="block text-[12px] text-gray-500 leading-relaxed">{st.desc}</span>
                </span>
              </button>
            ))}
          </div>
        </section>

        {/* 신고·문의 배너 2칸 — 나라장터 신고센터 자리 대응 */}
        <div className="grid grid-cols-1 sm:grid-cols-2 gap-4">
          {QUICK_BANNERS.map((q) => (
            <button
              key={q.title}
              onClick={() => navigate(q.to)}
              className="flex items-center gap-3 bg-white border border-gray-300 px-4 py-4 text-left hover:border-[#2b50b4] hover:bg-[#f7f9fc] transition-colors group"
            >
              <span className="shrink-0 w-10 h-10 grid place-items-center bg-[#13294f] text-white">
                <i className={`fa-solid ${q.icon} text-[16px]`} />
              </span>
              <span className="min-w-0">
                <span className="block text-[14px] font-bold text-gray-900 group-hover:text-[#2b50b4]">
                  {q.title}
                </span>
                <span className="block text-[12px] text-gray-500 mt-0.5">{q.desc}</span>
              </span>
              <i className="fa-solid fa-angle-right text-gray-300 ml-auto group-hover:text-[#2b50b4]" />
            </button>
          ))}
        </div>
        </div>

        {/* ═══ 우: 공지사항 + 관련사이트·고객센터 ═══ */}
        <div className="space-y-4">
          <section className="bg-white border border-gray-300">
            <PanelHead title="공지사항" onMore={() => navigate('/notices')} />
            <ul className="px-4 py-1.5">
              {NOTICES.map((n) => (
                <li key={n.id} className="border-b border-gray-100 last:border-0">
                  <button
                    onClick={() => navigate('/notices')}
                    className="w-full text-left py-3 group flex items-start gap-2"
                  >
                    {n.important ? (
                      <span className="shrink-0 mt-[3px] px-1.5 py-0.5 text-[11px] font-bold text-white bg-[#c0392b] rounded-sm">
                        중요
                      </span>
                    ) : (
                      <span className="shrink-0 mt-[7px] w-1 h-1 rounded-full bg-gray-400" />
                    )}
                    <span className="min-w-0 flex-1">
                      <span className="block text-[14px] text-gray-800 group-hover:text-[#2b50b4] line-clamp-1 font-medium">
                        {n.title}
                      </span>
                      <span className="block text-[12px] text-gray-500 mt-1 tabular-nums">{n.date}</span>
                    </span>
                  </button>
                </li>
              ))}
            </ul>
          </section>

          {/* 관련 사이트 */}
          <section className="bg-white border border-gray-300">
            <div className="px-4 h-[46px] flex items-center border-b border-gray-200">
              <h3 className="text-[16px] font-black text-[#13294f]">관련 사이트</h3>
            </div>
            <ul className="p-2">
              {RELATED_SITES.map((s) => (
                <li key={s.label}>
                  <a
                    href={s.href}
                    target="_blank"
                    rel="noreferrer"
                    className="flex items-center gap-2.5 px-2 py-2 hover:bg-[#f7f9fc] group"
                  >
                    <span className="w-7 h-7 shrink-0 grid place-items-center bg-[#eef3fb] text-[#1b3a72] rounded-sm">
                      <i className="fa-solid fa-arrow-up-right-from-square text-[12px]" />
                    </span>
                    <span className="min-w-0">
                      <span className="block text-[14px] font-bold text-gray-800 group-hover:text-[#2b50b4]">
                        {s.label}
                      </span>
                      <span className="block text-[12px] text-gray-500">{s.desc}</span>
                    </span>
                  </a>
                </li>
              ))}
            </ul>
          </section>

          {/* 고객센터 */}
          <section className="bg-[#13294f] text-white p-5">
            <p className="text-[14px] text-white/70 mb-1">WJbid 이용이</p>
            <p className="text-[19px] font-black mb-4">궁금하신가요?</p>
            <div className="flex items-baseline gap-2 mb-1">
              <span className="text-[12.5px] text-white/60">이메일</span>
              <a href="mailto:support@wjbid.com" className="text-[14.5px] font-bold hover:underline">
                support@wjbid.com
              </a>
            </div>
            <div className="flex items-baseline gap-2 mb-4">
              <span className="text-[12.5px] text-white/60">상담시간</span>
              <span className="text-[14px] font-medium">평일 09:00 ~ 18:00</span>
            </div>
            <button
              onClick={() => navigate('/support')}
              className="w-full h-10 bg-white/15 hover:bg-white/25 text-[14px] font-bold transition-colors"
            >
              고객센터 바로가기
            </button>
          </section>
        </div>
      </div>
    </div>
  )
}
