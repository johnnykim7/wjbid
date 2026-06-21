import { useEffect, useState } from 'react'
import { useNavigate } from 'react-router-dom'
import { getAdminOpportunities, getAdminOpportunityTypes, deleteOpportunity, translateOpportunityTitle, createManualOpportunity } from '../api/client'

interface OpportunityAdmin {
  id: string
  noticeId: string
  solicitationNumber?: string
  title: string
  titleKo?: string          // CR-022: 한글 제목 (없으면 영문 fallback)
  typeKo?: string           // CR-022: SAM type 한글 라벨
  translatedAt?: string     // CR-022: 마지막 번역 시각
  organizationName?: string
  postedDate?: string
  responseDeadline?: string
  attachmentCount: number
  manualFetchRequiredCount: number
  noticeCount: number
  pieeLinkBroken?: boolean   // CR-043: PIEE 링크 오류 표식
}

export default function OpportunityAdminPage() {
  const navigate = useNavigate()
  const [opportunities, setOpportunities] = useState<OpportunityAdmin[]>([])
  const [page, setPage] = useState(0)
  const [totalPages, setTotalPages] = useState(0)
  const [loading, setLoading] = useState(true)
  const [batchTranslating, setBatchTranslating] = useState(false)
  const [batchProgress, setBatchProgress] = useState({ done: 0, total: 0 })

  // CR-120: 테스트용 수동 공고 등록 모달
  const [manualOpen, setManualOpen] = useState(false)
  const [manualNoticeId, setManualNoticeId] = useState('')
  const [manualTitle, setManualTitle] = useState('')
  const [manualSaving, setManualSaving] = useState(false)

  // CR-118: 검색·필터
  const [keywordInput, setKeywordInput] = useState('')   // 입력 중(엔터/버튼으로 확정)
  const [keyword, setKeyword] = useState('')             // 실제 조회에 쓰는 확정 값
  const [type, setType] = useState('')                   // '' = 전체
  const [hasAttachment, setHasAttachment] = useState<'' | 'true' | 'false'>('') // '' = 전체
  const [hasNotice, setHasNotice] = useState<'' | 'true' | 'false'>('')         // '' = 전체(생성여부)
  const [types, setTypes] = useState<Array<{ type: string; typeKo: string }>>([])

  const fetchData = async () => {
    setLoading(true)
    try {
      const { data } = await getAdminOpportunities(page, {
        keyword: keyword || undefined,
        type: type || undefined,
        hasAttachment: hasAttachment === '' ? undefined : hasAttachment === 'true',
        hasNotice: hasNotice === '' ? undefined : hasNotice === 'true',
      })
      setOpportunities(data.content || [])
      setTotalPages(data.totalPages || 0)
    } catch (err) {
      console.error('원본 공고 목록 조회 실패:', err)
    } finally {
      setLoading(false)
    }
  }

  // 공고유형 셀렉트 옵션 1회 로드
  useEffect(() => {
    getAdminOpportunityTypes()
      .then(({ data }) => setTypes(data || []))
      .catch((err) => console.error('공고유형 목록 조회 실패:', err))
  }, [])

  // 필터/페이지 변경 시 재조회 (keyword는 확정 값 기준)
  useEffect(() => { fetchData() }, [page, keyword, type, hasAttachment, hasNotice])

  // 필터(유형/첨부/생성여부) 또는 키워드 확정 시 첫 페이지로
  const applyKeyword = () => { setPage(0); setKeyword(keywordInput.trim()) }
  const onTypeChange = (v: string) => { setPage(0); setType(v) }
  const onAttachmentChange = (v: '' | 'true' | 'false') => { setPage(0); setHasAttachment(v) }
  const onNoticeChange = (v: '' | 'true' | 'false') => { setPage(0); setHasNotice(v) }

  // 현재 페이지 미번역분(제목 한글화 미완료) 일괄 번역.
  // 제목만 번역하므로 SAM 쿼터를 소진하지 않는다(본문 fetch 없음).
  //
  // ⚠️ 직렬(1건씩) 처리한다. 백엔드 제목 번역은 Aimbase CLI 러너(anthropic-cli)를 타는데,
  //    동시 호출을 던지면 러너 세션이 꼬여 모든 호출이 멈추는 사고가 있었다.
  //    한 건 끝나야 다음 건을 보낸다.
  const handleBatchTranslate = async () => {
    const targets = opportunities.filter((o) => !o.translatedAt)
    if (targets.length === 0) {
      window.alert('현재 페이지에 미번역 공고가 없습니다.')
      return
    }
    if (!window.confirm(`현재 페이지의 미번역 ${targets.length}건 제목을 1건씩 순차 번역합니다.\n(SAM 쿼터는 소진하지 않으며, 건당 수십 초가 걸립니다)`)) {
      return
    }
    setBatchTranslating(true)
    setBatchProgress({ done: 0, total: targets.length })

    let done = 0
    let failed = 0
    for (const opp of targets) {
      try {
        const { data } = await translateOpportunityTitle(opp.id)
        // 성공(status=TRANSLATED)일 때만 해당 행을 즉시 갱신 — 실시간으로 미번역 배지가 사라진다.
        if (data?.status === 'TRANSLATED') {
          setOpportunities((prev) =>
            prev.map((o) =>
              o.id === opp.id
                ? { ...o, titleKo: data.titleKo || o.titleKo, translatedAt: data.translatedAt || new Date().toISOString() }
                : o,
            ),
          )
        } else {
          failed += 1
        }
      } catch (err) {
        // 타임아웃·에러는 실패로 집계하고 다음 건으로 진행(best-effort).
        console.error('제목 번역 실패:', opp.id, err)
        failed += 1
      } finally {
        done += 1
        setBatchProgress({ done, total: targets.length })
      }
    }
    setBatchTranslating(false)
    const ok = targets.length - failed
    if (failed > 0) {
      window.alert(`일괄 번역 완료 — 성공 ${ok}건 / 실패 ${failed}건.\n실패 건은 잠시 후 다시 시도해 주세요.`)
    }
    // 최종 동기화(혹시 모를 누락 보정)
    fetchData()
  }

  // CR-120: 테스트용 수동 공고 등록 모달 열기 — 공고번호 기본값(TEST-{timestamp}) 미리 채움.
  const openManual = () => {
    setManualNoticeId(`TEST-${Date.now()}`)
    setManualTitle('')
    setManualOpen(true)
  }

  // CR-120: 수동 공고 생성 후 상세로 이동(바로 첨부 업로드 가능).
  const handleCreateManual = async () => {
    if (!manualTitle.trim()) {
      window.alert('제목을 입력해 주세요.')
      return
    }
    setManualSaving(true)
    try {
      const { data } = await createManualOpportunity(manualNoticeId.trim(), manualTitle.trim())
      setManualOpen(false)
      navigate(`/opportunities/${data.opportunityId}`)
    } catch (err: any) {
      if (err?.response?.status === 409) {
        window.alert(err.response.data?.reason || '이미 존재하는 공고번호입니다.')
      } else {
        window.alert('테스트 공고 생성에 실패했습니다.')
        console.error('수동 공고 생성 실패:', err)
      }
    } finally {
      setManualSaving(false)
    }
  }

  // CR-042: 원본 공고 소프트 삭제. 행 클릭(상세 이동)과 분리하기 위해 stopPropagation.
  const handleDelete = async (e: React.MouseEvent, opp: OpportunityAdmin) => {
    e.stopPropagation()
    const label = opp.titleKo || opp.title
    if (!window.confirm(`이 공고를 목록에서 삭제하시겠습니까?\n\n${label}\n\n(소프트 삭제 — 원본 데이터는 보존되며 목록·검색·고객 노출에서만 제외됩니다)`)) {
      return
    }
    try {
      await deleteOpportunity(opp.id)
      // 현재 페이지가 마지막 1건이었으면 이전 페이지로, 아니면 재조회
      if (opportunities.length === 1 && page > 0) {
        setPage((p) => p - 1)
      } else {
        fetchData()
      }
    } catch (err: any) {
      if (err?.response?.status === 409) {
        window.alert(err.response.data?.reason || '노출 중·분석 중인 공고문이 연결돼 있어 삭제할 수 없습니다.')
      } else {
        window.alert('삭제에 실패했습니다.')
        console.error('원본 공고 삭제 실패:', err)
      }
    }
  }

  return (
    <div className="p-6 space-y-5">
      <div className="flex items-center justify-between">
        <div>
          <h1 className="text-xl font-bold text-gray-900">원본 공고 (선별 풀)</h1>
          <p className="text-sm text-gray-500 mt-1">SAM.gov 수집 원본. "공고문 만들기"로 선별 → 한글화</p>
        </div>
        <div className="flex items-center gap-2">
          {/* CR-120: SAM 수집 없이 테스트용 공고 1건 수동 등록 (PWS 기반 제안서 시뮬레이션 진입점) */}
          <button
            onClick={openManual}
            title="SAM 수집 없이 테스트용 공고를 만든다 (이후 PWS 첨부 업로드 → 분석 → 제안서 시뮬레이션)"
            className="px-4 py-2 text-sm rounded-lg border border-gray-300 text-gray-700 hover:bg-gray-50"
          >
            <i className="fa-solid fa-plus mr-1.5" />테스트 공고 추가
          </button>
          {(() => {
            const untranslated = opportunities.filter((o) => !o.translatedAt).length
            return (
              <button
                onClick={handleBatchTranslate}
                disabled={batchTranslating || untranslated === 0}
                title="현재 페이지의 미번역 공고 제목을 일괄 번역 (SAM 쿼터 미소진)"
                className="px-4 py-2 text-sm rounded-lg border border-secondary text-secondary hover:bg-blue-50 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                {batchTranslating ? (
                  <><i className="fa-solid fa-spinner fa-spin mr-1.5" />번역 중 {batchProgress.done}/{batchProgress.total}</>
                ) : (
                  <><i className="fa-solid fa-language mr-1.5" />이 페이지 일괄 번역{untranslated > 0 ? ` (${untranslated})` : ''}</>
                )}
              </button>
            )
          })()}
          <button
            onClick={() => navigate('/notices')}
            className="px-4 py-2 text-sm rounded-lg bg-secondary text-white hover:bg-blue-600"
          >
            공고문 리스트 →
          </button>
        </div>
      </div>

      {/* CR-118: 검색·필터 — 키워드(제목·본문·공고번호) / 공고유형 / 첨부유무 */}
      <div className="bg-white rounded-xl border border-gray-200 p-3 flex flex-wrap items-center gap-2">
        <div className="relative flex-1 min-w-[220px]">
          <i className="fa-solid fa-magnifying-glass absolute left-3 top-1/2 -translate-y-1/2 text-gray-400 text-sm" />
          <input
            value={keywordInput}
            onChange={(e) => setKeywordInput(e.target.value)}
            onKeyDown={(e) => { if (e.key === 'Enter') applyKeyword() }}
            placeholder="제목·본문·공고번호 검색"
            className="w-full pl-9 pr-3 py-2 text-sm rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-secondary/40"
          />
        </div>
        <select
          value={type}
          onChange={(e) => onTypeChange(e.target.value)}
          className="px-3 py-2 text-sm rounded-lg border border-gray-200 bg-white focus:outline-none focus:ring-2 focus:ring-secondary/40"
        >
          <option value="">공고유형 전체</option>
          {types.map((t) => (
            <option key={t.type} value={t.type}>{t.typeKo}</option>
          ))}
        </select>
        <select
          value={hasAttachment}
          onChange={(e) => onAttachmentChange(e.target.value as '' | 'true' | 'false')}
          className="px-3 py-2 text-sm rounded-lg border border-gray-200 bg-white focus:outline-none focus:ring-2 focus:ring-secondary/40"
        >
          <option value="">첨부 전체</option>
          <option value="true">첨부 있음</option>
          <option value="false">첨부 없음</option>
        </select>
        <select
          value={hasNotice}
          onChange={(e) => onNoticeChange(e.target.value as '' | 'true' | 'false')}
          className="px-3 py-2 text-sm rounded-lg border border-gray-200 bg-white focus:outline-none focus:ring-2 focus:ring-secondary/40"
        >
          <option value="">생성여부 전체</option>
          <option value="true">생성됨</option>
          <option value="false">미생성</option>
        </select>
        <button
          onClick={applyKeyword}
          className="px-4 py-2 text-sm rounded-lg bg-secondary text-white hover:bg-blue-600"
        >
          검색
        </button>
        {(keyword || type || hasAttachment || hasNotice) && (
          <button
            onClick={() => {
              setKeywordInput(''); setKeyword(''); setType(''); setHasAttachment(''); setHasNotice(''); setPage(0)
            }}
            className="px-3 py-2 text-sm rounded-lg border border-gray-200 text-gray-500 hover:bg-gray-50"
          >
            초기화
          </button>
        )}
      </div>

      <div className="bg-white rounded-xl border border-gray-200 overflow-hidden">
        {loading ? (
          <div className="flex items-center justify-center py-20">
            <div className="w-8 h-8 border-2 border-gray-200 border-t-secondary rounded-full animate-spin" />
          </div>
        ) : opportunities.length === 0 ? (
          <div className="text-center py-20 text-gray-400 text-sm">원본 공고가 없습니다.</div>
        ) : (
          <table className="w-full text-sm table-fixed">
            <colgroup>
              <col className="w-[40%]" />
              <col className="w-28" />
              <col className="w-28" />
              <col className="w-28" />
              <col className="w-24" />
              <col className="w-24" />
              <col className="w-16" />
            </colgroup>
            <thead>
              <tr className="bg-gray-50 border-b border-gray-200">
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase">공고 (원문)</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">공고유형</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">공고일</th>
                <th className="text-left px-4 py-3 text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">마감일</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">첨부</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase whitespace-nowrap">생성여부</th>
                <th className="text-center px-4 py-3 text-xs font-semibold text-gray-500 uppercase whitespace-nowrap"></th>
              </tr>
            </thead>
            <tbody className="divide-y divide-gray-100">
              {opportunities.map((opp) => (
                <tr
                  key={opp.id}
                  className="hover:bg-gray-50 cursor-pointer"
                  onClick={() => navigate(`/opportunities/${opp.id}`)}
                >
                  <td className="px-4 py-3">
                    <div className="font-medium text-gray-900 truncate">{opp.titleKo || opp.title}</div>
                    {opp.titleKo && (
                      <div className="text-xs text-gray-500 truncate">{opp.title}</div>
                    )}
                    <div className="text-xs text-gray-400 font-mono flex items-center gap-2 mt-0.5">
                      <span>{opp.solicitationNumber || opp.noticeId}</span>
                      {!opp.translatedAt && (
                        <span className="inline-flex px-1.5 py-0.5 rounded bg-amber-50 text-amber-600 text-[10px]" title="제목 한글화 미완료 — 영문 표시">미번역</span>
                      )}
                      {opp.pieeLinkBroken && (
                        <span className="inline-flex items-center gap-1 px-1.5 py-0.5 rounded bg-red-50 text-red-600 text-[10px] font-semibold" title="PIEE 링크 오류로 표시됨 — 직링크가 안 열릴 수 있음">
                          <i className="fa-solid fa-triangle-exclamation" />PIEE 오류
                        </span>
                      )}
                    </div>
                  </td>
                  <td className="px-4 py-3 text-gray-600">
                    <div className="truncate" title={opp.typeKo || ''}>{opp.typeKo || '-'}</div>
                  </td>
                  <td className="px-4 py-3 text-gray-600 whitespace-nowrap">
                    {opp.postedDate ? new Date(opp.postedDate).toLocaleDateString('ko') : '-'}
                  </td>
                  <td className="px-4 py-3 text-gray-600 whitespace-nowrap">
                    {opp.responseDeadline ? new Date(opp.responseDeadline).toLocaleDateString('ko') : '-'}
                  </td>
                  <td className="px-4 py-3 text-center">
                    {opp.attachmentCount > 0 ? (
                      <span className="inline-flex items-center gap-1 text-gray-600 text-xs" title={`첨부 ${opp.attachmentCount}건`}>
                        <i className="fa-solid fa-paperclip" />{opp.attachmentCount}
                      </span>
                    ) : (
                      <span className="inline-flex px-2 py-0.5 rounded-full text-xs font-medium bg-amber-50 text-amber-600" title="첨부파일 없음">
                        없음
                      </span>
                    )}
                  </td>
                  <td className="px-4 py-3 text-center">
                    <span className={`inline-flex px-2 py-0.5 rounded-full text-xs font-medium ${
                      opp.noticeCount > 0 ? 'bg-green-100 text-green-700' : 'bg-gray-100 text-gray-500'
                    }`}>
                      {opp.noticeCount > 0 ? '생성' : '미생성'}
                    </span>
                  </td>
                  <td className="px-4 py-3 text-center">
                    <button
                      onClick={(e) => handleDelete(e, opp)}
                      title="공고 삭제(소프트)"
                      className="text-gray-300 hover:text-red-500 transition-colors"
                    >
                      <i className="fa-solid fa-trash-can text-sm" />
                    </button>
                  </td>
                </tr>
              ))}
            </tbody>
          </table>
        )}

        {totalPages > 1 && (
          <div className="flex items-center justify-center gap-2 px-5 py-4 border-t border-gray-100">
            <button onClick={() => setPage((p) => Math.max(0, p - 1))} disabled={page === 0}
              className="px-3 py-1.5 text-xs rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50">
              이전
            </button>
            <span className="text-xs text-gray-500">{page + 1} / {totalPages}</span>
            <button onClick={() => setPage((p) => Math.min(totalPages - 1, p + 1))} disabled={page >= totalPages - 1}
              className="px-3 py-1.5 text-xs rounded-lg border border-gray-200 disabled:opacity-40 hover:bg-gray-50">
              다음
            </button>
          </div>
        )}
      </div>

      {/* CR-120: 테스트용 수동 공고 등록 모달 */}
      {manualOpen && (
        <div
          className="fixed inset-0 z-50 flex items-center justify-center bg-black/40"
          onClick={() => !manualSaving && setManualOpen(false)}
        >
          <div
            className="bg-white rounded-xl w-full max-w-md p-6 space-y-4 shadow-xl"
            onClick={(e) => e.stopPropagation()}
          >
            <div>
              <h2 className="text-lg font-bold text-gray-900">테스트 공고 추가</h2>
              <p className="text-xs text-gray-500 mt-1">
                SAM 수집 없이 빈 공고를 만듭니다. 생성 후 상세 화면에서 PWS 첨부를 올려 분석·제안서 생성을 시뮬레이션하세요.
              </p>
            </div>
            <div className="space-y-3">
              <div>
                <label className="block text-xs font-semibold text-gray-600 mb-1">공고번호</label>
                <input
                  value={manualNoticeId}
                  onChange={(e) => setManualNoticeId(e.target.value)}
                  placeholder="비워두면 자동 생성"
                  className="w-full px-3 py-2 text-sm font-mono rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-secondary/40"
                />
                <p className="text-[11px] text-gray-400 mt-1">기본값(TEST-…)을 그대로 써도 됩니다. 중복이면 거부됩니다.</p>
              </div>
              <div>
                <label className="block text-xs font-semibold text-gray-600 mb-1">제목 <span className="text-red-500">*</span></label>
                <input
                  value={manualTitle}
                  onChange={(e) => setManualTitle(e.target.value)}
                  onKeyDown={(e) => { if (e.key === 'Enter') handleCreateManual() }}
                  placeholder="예: 411th CSB — NTV 구매 (테스트)"
                  className="w-full px-3 py-2 text-sm rounded-lg border border-gray-200 focus:outline-none focus:ring-2 focus:ring-secondary/40"
                  autoFocus
                />
              </div>
            </div>
            <div className="flex justify-end gap-2 pt-2">
              <button
                onClick={() => setManualOpen(false)}
                disabled={manualSaving}
                className="px-4 py-2 text-sm rounded-lg border border-gray-200 text-gray-600 hover:bg-gray-50 disabled:opacity-40"
              >
                취소
              </button>
              <button
                onClick={handleCreateManual}
                disabled={manualSaving || !manualTitle.trim()}
                className="px-4 py-2 text-sm rounded-lg bg-secondary text-white hover:bg-blue-600 disabled:opacity-40 disabled:cursor-not-allowed"
              >
                {manualSaving ? (
                  <><i className="fa-solid fa-spinner fa-spin mr-1.5" />생성 중…</>
                ) : '생성하고 상세로 이동'}
              </button>
            </div>
          </div>
        </div>
      )}
    </div>
  )
}
