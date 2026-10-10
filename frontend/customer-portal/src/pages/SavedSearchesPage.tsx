import { useState } from 'react'
import { Button, Switch } from '@therecommerce-develop/ds-ui'
import { Card } from '../components/ui/card'
import { Badge } from '../components/ui/badge'
import { EmptyState } from '../components/ui/empty-state'

// ── 더미 데이터 (BE 미연동) ─────────────────────────────────────
// 저장한 검색 조건 + 알림 구독 토글. 실제 연동 시 별도 API 필요.
interface SavedSearch {
  id: string
  name: string
  naics?: string
  agency?: string
  keyword?: string
  alertOn: boolean
  lastMatched: number
}

const DUMMY_SEARCHES: SavedSearch[] = [
  {
    id: 's1',
    name: 'IT 용역 (USFK)',
    naics: '541512',
    agency: 'Defense Logistics Agency',
    keyword: 'information technology',
    alertOn: true,
    lastMatched: 12,
  },
  {
    id: 's2',
    name: '시설 유지보수',
    naics: '561210',
    agency: '411th CSB',
    keyword: 'maintenance',
    alertOn: true,
    lastMatched: 5,
  },
  {
    id: 's3',
    name: '식자재 납품',
    naics: '424490',
    agency: 'DLA Troop Support',
    keyword: 'food supply',
    alertOn: false,
    lastMatched: 0,
  },
  {
    id: 's4',
    name: '통번역 서비스',
    keyword: 'translation interpretation korea',
    alertOn: true,
    lastMatched: 3,
  },
]

export default function SavedSearchesPage() {
  // BE 미연동 — 로컬 상태로 토글만 동작
  const [searches, setSearches] = useState<SavedSearch[]>(DUMMY_SEARCHES)

  const toggleAlert = (id: string) => {
    setSearches((prev) =>
      prev.map((s) => (s.id === id ? { ...s, alertOn: !s.alertOn } : s))
    )
  }

  return (
    <div className="space-y-6">
      <div className="flex justify-between items-center mb-6">
        <div>
          <h2 className="text-xl font-bold text-ink">저장한 검색 / 알림 구독</h2>
          <p className="text-sm text-ink-mute mt-1">
            자주 쓰는 검색 조건을 저장하고, 새 공고가 올라오면 알림을 받으세요.
          </p>
        </div>
        {/* BE 미연동: 새 검색 저장 흐름은 추후 연결 */}
        <Button variant="primary" size="sm">
          <i className="fa-solid fa-plus mr-1.5" /> 새 검색 저장
        </Button>
      </div>

      {searches.length === 0 ? (
        <EmptyState
          icon="fa-solid fa-magnifying-glass"
          title="저장한 검색이 없습니다"
          description="입찰 검색 조건을 저장하면 여기에 표시됩니다."
        />
      ) : (
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          {searches.map((s) => (
            <Card key={s.id} className="p-5">
              <div className="flex items-start justify-between gap-3 mb-3">
                <div className="min-w-0">
                  <h3 className="text-base font-bold text-ink truncate">{s.name}</h3>
                  <p className="text-xs text-ink-mute mt-0.5">
                    {s.alertOn ? (
                      <span className="text-brand">
                        <i className="fa-solid fa-bell mr-1" /> 알림 켜짐
                      </span>
                    ) : (
                      <span>
                        <i className="fa-regular fa-bell-slash mr-1" /> 알림 꺼짐
                      </span>
                    )}
                  </p>
                </div>
                <Switch checked={s.alertOn} onCheckedChange={() => toggleAlert(s.id)} />
              </div>

              <div className="flex flex-wrap gap-1.5 mb-4">
                {s.naics && (
                  <Badge variant="info" size="sm">
                    NAICS {s.naics}
                  </Badge>
                )}
                {s.agency && (
                  <Badge variant="secondary" size="sm">
                    {s.agency}
                  </Badge>
                )}
                {s.keyword && (
                  <Badge variant="pending" size="sm">
                    “{s.keyword}”
                  </Badge>
                )}
              </div>

              <div className="flex items-center justify-between border-t border-line-soft pt-3">
                <span className="text-xs text-ink-mute">
                  최근 매칭{' '}
                  <span className="font-bold text-ink">{s.lastMatched}</span>건
                </span>
                <div className="flex gap-2">
                  <Button variant="ghost" size="sm" className="text-brand">
                    <i className="fa-solid fa-arrow-right mr-1" /> 결과 보기
                  </Button>
                  <Button variant="ghost" size="icon" className="text-ink-mute hover:text-danger" title="삭제">
                    <i className="fa-regular fa-trash-can" />
                  </Button>
                </div>
              </div>
            </Card>
          ))}
        </div>
      )}

      <p className="mt-6 text-xs text-ink-mute">
        <i className="fa-solid fa-circle-info mr-1" />
        저장한 검색·알림 구독은 현재 화면 시연용 더미 데이터입니다. (BE 미연동)
      </p>
    </div>
  )
}
