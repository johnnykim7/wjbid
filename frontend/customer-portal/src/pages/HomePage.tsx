import { useState, useEffect } from 'react'
import { useNavigate } from 'react-router-dom'
import { getOpportunities } from '../api/client'

const HERO_STATS = [
  { value: '2,800+', label: '활성 입찰 공고' },
  { value: '43%',    label: '평균 낙찰 성공률' },
  { value: '1,200+', label: '작성 제안서' },
  { value: '150+',   label: '기업 파트너' },
]

const PROCESS_STEPS = [
  {
    num: '01',
    icon: 'fa-solid fa-magnifying-glass',
    title: 'SAM.gov 공고 검색',
    desc: 'DoD, VA, GSA 등 미군·연방 기관 입찰 공고를 키워드·NAICS 코드로 실시간 검색합니다.',
  },
  {
    num: '02',
    icon: 'fa-solid fa-robot',
    title: '적격성 분석',
    desc: '공고 요건과 귀사 역량을 비교 분석하여 입찰 적합도와 전략을 제시합니다.',
  },
  {
    num: '03',
    icon: 'fa-solid fa-file-lines',
    title: '제안서 작성',
    desc: 'Cover Letter, Technical Proposal, Past Performance 등 입찰 필수 서류를 작성합니다.',
  },
  {
    num: '04',
    icon: 'fa-solid fa-paper-plane',
    title: '검토 및 제출 준비',
    desc: '전문 편집기에서 초안을 수정·완성하고 SAM.gov 제출 규격에 맞춰 최종 검토합니다.',
  },
]

const FEATURES = [
  {
    icon: 'fa-solid fa-bolt',
    title: '실시간 SAM.gov 연동',
    desc: 'SAM.gov API를 통해 신규 공고를 자동 수집합니다. 마감 임박 공고는 우선 표시되어 기회를 놓치지 않습니다.',
  },
  {
    icon: 'fa-solid fa-brain',
    title: '맞춤형 제안서 작성',
    desc: '입찰 요건을 깊이 분석하여 경쟁력 있는 맞춤형 제안서를 작성합니다.',
  },
  {
    icon: 'fa-solid fa-pen-to-square',
    title: '전문가 수준 편집기',
    desc: '생성된 초안을 리치 텍스트 편집기에서 즉시 수정합니다. 서식·표·섹션 구조를 자유롭게 편집합니다.',
  },
  {
    icon: 'fa-solid fa-shield-halved',
    title: '제출 규격 자동 검증',
    desc: 'SAM.gov 필수 항목과 서식 요건을 자동으로 체크합니다. 오류 없는 완성도 높은 제안서를 보장합니다.',
  },
]

interface Bid {
  id: string
  solicitationNumber: string
  title: string
  agencyName: string
  responseDeadline: string
}

export default function HomePage() {
  const navigate = useNavigate()
  const [recentBids, setRecentBids] = useState<Bid[]>([])

  useEffect(() => {
    getOpportunities(0, 6)
      .then((res) => setRecentBids(res.data.content?.slice(0, 6) ?? []))
      .catch(() => {})
  }, [])

  const scrollTo = (id: string) =>
    document.getElementById(id)?.scrollIntoView({ behavior: 'smooth' })

  return (
    <div>
      {/* ── HERO ── */}
      <section
        style={{ background: 'linear-gradient(160deg, #0c1e3d 0%, #1a3a6c 100%)' }}
        className="text-white relative overflow-hidden"
      >
        {/* 독수리 이미지 - mask-image로 왼쪽 가장자리 자체를 투명하게 */}
        <div className="absolute inset-0">
          <img
            src="https://images.pexels.com/photos/15014096/pexels-photo-15014096.jpeg?auto=compress&cs=tinysrgb&w=1200&dpr=1"
            alt=""
            aria-hidden="true"
            className="absolute top-0 right-0 h-full object-cover object-center"
            style={{
              width: '65%',
              WebkitMaskImage: 'linear-gradient(to right, transparent 0%, black 30%)',
              maskImage: 'linear-gradient(to right, transparent 0%, black 30%)',
            }}
            onError={(e) => {
              const t = e.currentTarget
              if (!t.dataset.fallback) {
                t.dataset.fallback = '1'
                t.src = 'https://images.pexels.com/photos/209084/pexels-photo-209084.jpeg?auto=compress&cs=tinysrgb&w=1200&dpr=1'
              }
            }}
          />
          {/* 전체 약한 네이비 톤 통일 */}
          <div className="absolute inset-0" style={{ background: 'rgba(8,18,38,0.25)' }} />
          {/* 상하 페이드 */}
          <div
            className="absolute inset-0"
            style={{
              background: 'linear-gradient(to bottom, rgba(8,18,38,0.5) 0%, transparent 25%, transparent 75%, rgba(8,18,38,0.6) 100%)',
            }}
          />
          {/* 우측 끝 마감 */}
          <div
            className="absolute inset-0"
            style={{ background: 'linear-gradient(to left, rgba(12,30,61,0.5) 0%, transparent 12%)' }}
          />
        </div>

        <div className="relative max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-24 md:py-36">
          {/* 좌측 텍스트만 (이미지는 배경으로) */}
          <div className="max-w-xl">
            <div
              className="inline-flex items-center gap-2 px-3 py-1.5 rounded-full text-xs font-medium mb-6"
              style={{ background: 'rgba(255,255,255,0.1)', color: 'rgba(255,255,255,0.75)' }}
            >
              <span className="w-1.5 h-1.5 rounded-full bg-emerald-400 inline-block" />
              SAM.gov 실시간 연동 · 전문가 제안서 작성
            </div>

            <h1 className="text-4xl md:text-[56px] font-bold leading-tight tracking-tight mb-5">
              미군 입찰,<br />더 빠르고<br />정확하게
            </h1>

            <p className="text-base md:text-lg leading-relaxed mb-8" style={{ color: 'rgba(255,255,255,0.65)' }}>
              SAM.gov 공고 실시간 검색부터 전문가 제안서 작성까지.<br />
              복잡한 미군 입찰 절차를 WJbid가 간소화합니다.
            </p>

            <div className="flex flex-wrap gap-3">
              <button
                onClick={() => navigate('/login')}
                className="px-6 py-3 text-sm font-semibold bg-white text-[#0c1e3d] rounded-lg hover:bg-gray-100 transition-colors"
              >
                무료로 시작하기
              </button>
              <button
                onClick={() => scrollTo('process')}
                className="px-6 py-3 text-sm font-medium rounded-lg transition-colors border"
                style={{ borderColor: 'rgba(255,255,255,0.25)', color: 'rgba(255,255,255,0.85)' }}
              >
                서비스 알아보기 →
              </button>
            </div>
          </div>

          {/* Stats bar */}
          <div
            className="grid grid-cols-2 md:grid-cols-4 gap-x-8 gap-y-6 mt-16 pt-12 border-t"
            style={{ borderColor: 'rgba(255,255,255,0.1)' }}
          >
            {HERO_STATS.map((s) => (
              <div key={s.label}>
                <div className="text-2xl font-bold text-white">{s.value}</div>
                <div className="text-sm mt-0.5" style={{ color: 'rgba(255,255,255,0.55)' }}>
                  {s.label}
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── FEATURES (서비스 소개) ── */}
      <section id="about" style={{ background: '#f5f7fa' }}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 md:py-20">
          <div className="text-center mb-12">
            <p className="text-xs font-semibold tracking-widest uppercase text-[#1a56db] mb-2">
              Why WJbid
            </p>
            <h2 className="text-2xl md:text-3xl font-bold text-[#0c1e3d]">
              미군 입찰의 모든 것을 한 곳에서
            </h2>
            <p className="mt-3 text-sm text-gray-500 max-w-lg mx-auto">
              입찰 준비에 드는 시간과 비용을 대폭 절감합니다
            </p>
          </div>
          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 items-stretch">
            {FEATURES.map((f) => (
              <div
                key={f.title}
                className="flex flex-col p-6 rounded-xl border border-gray-200 bg-white hover:border-[#1a56db]/30 hover:shadow-md transition-all"
              >
                <div
                  className="w-10 h-10 rounded-lg flex items-center justify-center mb-4 flex-shrink-0"
                  style={{ background: '#eef3ff' }}
                >
                  <i className={`${f.icon} text-[#1a56db] text-sm`} />
                </div>
                <h3 className="text-sm font-semibold text-[#0c1e3d] mb-2">{f.title}</h3>
                <p className="text-xs text-gray-500 leading-relaxed flex-1">{f.desc}</p>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── PROCESS ── */}
      <section id="process" className="bg-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 md:py-20">
          <div className="text-center mb-12">
            <p className="text-xs font-semibold tracking-widest uppercase text-[#1a56db] mb-2">
              How It Works
            </p>
            <h2 className="text-2xl md:text-3xl font-bold text-[#0c1e3d]">
              4단계 입찰 대행 프로세스
            </h2>
            <p className="mt-3 text-sm text-gray-500 max-w-lg mx-auto">
              복잡한 미군 입찰 절차를 WJbid가 단계별로 대행합니다
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-2 lg:grid-cols-4 gap-5 items-stretch">
            {PROCESS_STEPS.map((step, idx) => (
              <div key={step.num} className="relative flex flex-col">
                {idx < PROCESS_STEPS.length - 1 && (
                  <div
                    className="hidden lg:block absolute top-9 left-[calc(100%+0px)] w-5 h-px z-10"
                    style={{ background: '#e2e8f0' }}
                  />
                )}
                <div className="flex flex-col flex-1 p-6 rounded-xl border border-gray-100 bg-white hover:border-[#1a56db]/30 hover:shadow-md transition-all">
                  <div className="text-[11px] font-bold tracking-widest text-[#1a56db] mb-3 uppercase">
                    Step {step.num}
                  </div>
                  <div
                    className="w-10 h-10 rounded-lg flex items-center justify-center mb-4 flex-shrink-0"
                    style={{ background: '#eef3ff' }}
                  >
                    <i className={`${step.icon} text-[#1a56db] text-sm`} />
                  </div>
                  <h3 className="text-sm font-semibold text-[#0c1e3d] mb-2">{step.title}</h3>
                  <p className="text-xs text-gray-500 leading-relaxed flex-1">{step.desc}</p>
                </div>
              </div>
            ))}
          </div>
        </div>
      </section>

      {/* ── RECENT BIDS ── */}
      <section id="bids" style={{ background: '#f5f7fa' }}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16">
          <div className="flex items-end justify-between mb-8">
            <div>
              <p className="text-xs font-semibold tracking-widest uppercase text-[#1a56db] mb-1.5">
                Live Feed
              </p>
              <h2 className="text-2xl font-bold text-[#0c1e3d]">최근 입찰 공고</h2>
            </div>
            <button
              onClick={() => navigate('/login')}
              className="text-sm font-medium text-[#1a56db] hover:underline hidden sm:flex items-center gap-1"
            >
              전체 보기 <i className="fa-solid fa-arrow-right text-xs" />
            </button>
          </div>

          <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
            {recentBids.length === 0 ? (
              <div className="py-16 text-center">
                <i className="fa-solid fa-spinner fa-spin text-xl text-gray-300 mb-3" />
                <p className="text-sm text-gray-400">공고를 불러오는 중입니다...</p>
              </div>
            ) : (
              <div className="overflow-x-auto">
                <table className="w-full text-sm">
                  <thead>
                    <tr style={{ background: '#f8fafc', borderBottom: '1px solid #e2e8f0' }}>
                      <th className="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wide whitespace-nowrap">
                        공고번호
                      </th>
                      <th className="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wide">
                        공고명
                      </th>
                      <th className="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wide hidden md:table-cell whitespace-nowrap">
                        발주 기관
                      </th>
                      <th className="text-left px-5 py-3 text-xs font-semibold text-gray-400 uppercase tracking-wide whitespace-nowrap">
                        마감일
                      </th>
                      <th className="px-5 py-3 w-16" />
                    </tr>
                  </thead>
                  <tbody>
                    {recentBids.map((bid) => {
                      const daysLeft = Math.ceil(
                        (new Date(bid.responseDeadline).getTime() - Date.now()) / 86400000
                      )
                      const urgent = daysLeft <= 14
                      return (
                        <tr
                          key={bid.id}
                          className="border-t border-gray-100 hover:bg-gray-50 transition-colors"
                        >
                          <td className="px-5 py-4 font-mono text-xs text-gray-400 whitespace-nowrap align-top">
                            {bid.solicitationNumber}
                          </td>
                          <td className="px-5 py-4 font-medium text-[#0c1e3d] align-top max-w-sm">
                            <div className="line-clamp-2 text-sm">{bid.title}</div>
                          </td>
                          <td className="px-5 py-4 text-xs text-gray-500 hidden md:table-cell align-top whitespace-nowrap">
                            <div className="max-w-[200px] truncate">{bid.agencyName}</div>
                          </td>
                          <td className="px-5 py-4 whitespace-nowrap align-top">
                            <span
                              className={`inline-flex items-center gap-1 px-2.5 py-1 rounded text-xs font-medium ${
                                urgent
                                  ? 'bg-red-50 text-red-700'
                                  : 'bg-emerald-50 text-emerald-700'
                              }`}
                            >
                              {urgent && <i className="fa-regular fa-clock text-[10px]" />}
                              {bid.responseDeadline}
                            </span>
                          </td>
                          <td className="px-5 py-4 align-middle text-right">
                            <button
                              onClick={() => navigate('/login')}
                              className="text-xs font-medium text-[#1a56db] hover:underline whitespace-nowrap"
                            >
                              상세보기
                            </button>
                          </td>
                        </tr>
                      )
                    })}
                  </tbody>
                </table>
              </div>
            )}
          </div>

          <p className="text-center text-sm text-gray-500 mt-5">
            로그인하면 전체 2,800+ 건의 공고를 검색하고 제안서를 작성할 수 있습니다.
            <button
              onClick={() => navigate('/login')}
              className="ml-1.5 font-semibold text-[#1a56db] hover:underline"
            >
              지금 무료 시작하기 →
            </button>
          </p>
        </div>
      </section>

      {/* ── CTA ── */}
      <section style={{ background: '#0c1e3d' }} className="text-white">
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 md:py-20 text-center">
          <h2 className="text-2xl md:text-3xl font-bold mb-4">지금 바로 시작하세요</h2>
          <p className="mb-8 max-w-xl mx-auto text-sm leading-relaxed" style={{ color: 'rgba(255,255,255,0.6)' }}>
            별도 설치 없이 웹 브라우저에서 즉시 사용. 무료 플랜으로 핵심 기능을 먼저 체험해보세요.
          </p>
          <div className="flex flex-wrap justify-center gap-3 mb-6">
            <button
              onClick={() => navigate('/login')}
              className="px-6 py-3 text-sm font-semibold bg-white text-[#0c1e3d] rounded-lg hover:bg-gray-100 transition-colors"
            >
              무료로 시작하기
            </button>
            <button
              onClick={() => navigate('/pricing')}
              className="px-6 py-3 text-sm font-medium rounded-lg border transition-colors"
              style={{ borderColor: 'rgba(255,255,255,0.25)', color: 'rgba(255,255,255,0.85)' }}
            >
              요금제 보기
            </button>
          </div>
          <p className="text-xs" style={{ color: 'rgba(255,255,255,0.35)' }}>
            신용카드 불필요 &nbsp;·&nbsp; 언제든 해지 가능 &nbsp;·&nbsp; 14일 무료 체험
          </p>
        </div>
      </section>

      {/* ── 고객센터 ── */}
      <section id="contact" style={{ background: '#f5f7fa' }}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-16 md:py-20">
          <div className="text-center mb-12">
            <p className="text-xs font-semibold tracking-widest uppercase text-[#1a56db] mb-2">Support</p>
            <h2 className="text-2xl md:text-3xl font-bold text-[#0c1e3d]">고객센터</h2>
            <p className="mt-3 text-sm text-gray-500 max-w-lg mx-auto">
              궁금한 점이 있으시면 아래 채널로 문의해 주세요. 영업일 기준 24시간 내 답변 드립니다.
            </p>
          </div>

          <div className="grid grid-cols-1 md:grid-cols-3 gap-6 mb-12">
            {/* 이메일 문의 */}
            <div className="bg-white rounded-xl border border-gray-200 p-7 flex flex-col items-center text-center hover:shadow-md transition-all">
              <div className="w-12 h-12 rounded-xl flex items-center justify-center mb-4" style={{ background: '#eef3ff' }}>
                <i className="fa-regular fa-envelope text-[#1a56db] text-lg" />
              </div>
              <h3 className="text-sm font-semibold text-[#0c1e3d] mb-1.5">이메일 문의</h3>
              <p className="text-xs text-gray-500 mb-4 leading-relaxed">서비스 이용, 계정, 결제 관련<br />모든 문의를 접수합니다.</p>
              <a
                href="mailto:support@wjbid.com"
                className="text-sm font-semibold text-[#1a56db] hover:underline"
              >
                support@wjbid.com
              </a>
            </div>

            {/* 전화 문의 */}
            <div className="bg-white rounded-xl border border-gray-200 p-7 flex flex-col items-center text-center hover:shadow-md transition-all">
              <div className="w-12 h-12 rounded-xl flex items-center justify-center mb-4" style={{ background: '#eef3ff' }}>
                <i className="fa-solid fa-phone text-[#1a56db] text-lg" />
              </div>
              <h3 className="text-sm font-semibold text-[#0c1e3d] mb-1.5">전화 문의</h3>
              <p className="text-xs text-gray-500 mb-4 leading-relaxed">평일 09:00 – 18:00 (KST)<br />점심시간 12:00 – 13:00 제외</p>
              <a
                href="tel:+8215880000"
                className="text-sm font-semibold text-[#1a56db] hover:underline"
              >
                1588-0000
              </a>
            </div>

            {/* FAQ */}
            <div className="bg-white rounded-xl border border-gray-200 p-7 flex flex-col items-center text-center hover:shadow-md transition-all">
              <div className="w-12 h-12 rounded-xl flex items-center justify-center mb-4" style={{ background: '#eef3ff' }}>
                <i className="fa-regular fa-circle-question text-[#1a56db] text-lg" />
              </div>
              <h3 className="text-sm font-semibold text-[#0c1e3d] mb-1.5">자주 묻는 질문</h3>
              <p className="text-xs text-gray-500 mb-4 leading-relaxed">서비스 이용 방법, 요금제,<br />SAM.gov 연동 방법을 안내합니다.</p>
              <button
                onClick={() => navigate('/login')}
                className="text-sm font-semibold text-[#1a56db] hover:underline"
              >
                FAQ 바로가기 →
              </button>
            </div>
          </div>

          {/* 공지 배너 */}
          <div
            className="rounded-xl px-6 py-4 flex flex-col sm:flex-row items-start sm:items-center justify-between gap-3"
            style={{ background: '#eef3ff', border: '1px solid #c7d9ff' }}
          >
            <div className="flex items-start gap-3">
              <i className="fa-solid fa-circle-info text-[#1a56db] mt-0.5 flex-shrink-0" />
              <div>
                <p className="text-sm font-semibold text-[#0c1e3d] mb-0.5">온보딩 지원 안내</p>
                <p className="text-xs text-gray-600">처음 가입하신 기업 고객께는 담당 매니저가 SAM.gov 설정부터 첫 입찰 제출까지 1:1로 지원합니다.</p>
              </div>
            </div>
            <button
              onClick={() => navigate('/login')}
              className="flex-shrink-0 px-4 py-2 text-xs font-semibold text-white rounded-lg transition-colors"
              style={{ background: '#1a56db' }}
            >
              무료 상담 신청
            </button>
          </div>
        </div>
      </section>

      {/* ── FOOTER ── */}
      <footer style={{ background: '#070f1e' }}>
        <div className="max-w-7xl mx-auto px-4 sm:px-6 lg:px-8 py-10">
          <div className="flex flex-col md:flex-row justify-between items-start gap-8">
            {/* Brand */}
            <div>
              <div className="flex items-center gap-2.5 mb-3">
                <div
                  className="w-7 h-7 rounded flex items-center justify-center"
                  style={{ background: '#0c1e3d' }}
                >
                  <i className="fa-solid fa-file-signature text-white text-xs" />
                </div>
                <span className="font-bold text-sm text-white">WJbid.com</span>
              </div>
              <p className="text-xs leading-relaxed max-w-xs" style={{ color: 'rgba(255,255,255,0.4)' }}>
                미군 입찰 대행 플랫폼.<br />
                SAM.gov 연동 및 제안서 작성 서비스를 제공합니다.
              </p>
            </div>

            {/* Links */}
            <div className="flex gap-12 text-xs" style={{ color: 'rgba(255,255,255,0.45)' }}>
              <div>
                <p className="font-semibold mb-3" style={{ color: 'rgba(255,255,255,0.7)' }}>
                  서비스
                </p>
                <div className="space-y-2">
                  {['입찰 검색', '제안서 작성', '요금제'].map((t) => (
                    <button
                      key={t}
                      onClick={() => navigate('/login')}
                      className="block hover:text-white transition-colors text-left"
                    >
                      {t}
                    </button>
                  ))}
                </div>
              </div>
              <div>
                <p className="font-semibold mb-3" style={{ color: 'rgba(255,255,255,0.7)' }}>
                  고객 지원
                </p>
                <div className="space-y-2">
                  {['이용 약관', '개인정보처리방침', '문의하기'].map((t) => (
                    <button key={t} className="block hover:text-white transition-colors text-left">
                      {t}
                    </button>
                  ))}
                </div>
              </div>
            </div>
          </div>

          <div
            className="mt-8 pt-6 text-center text-xs"
            style={{ borderTop: '1px solid rgba(255,255,255,0.06)', color: 'rgba(255,255,255,0.25)' }}
          >
            © 2025 WJbid.com. All rights reserved.
          </div>
        </div>
      </footer>
    </div>
  )
}
