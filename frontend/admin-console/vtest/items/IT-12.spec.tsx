import { beforeAll, test } from 'vitest'
import { judge, noMaterial, reportTo } from '../report'
import { apiGet, buttonNamed, click, loginAs, openScreen, screen, text, transitionTo } from '../harness'
import { seedId } from '../seed'

reportTo('IT-12')

// 전이 시험은 회차마다 넣는 시딩 입찰 건에만 건다(tools/testbook/seed/12-*.sql). 실제 고객 건이 움직이지 않는다
const ADMIN = process.env.VTEST_ADMIN ?? 'smoke-admin@example.com'

beforeAll(async () => {
  await loginAs(ADMIN)
})

/** 입찰 상세 관리 머리의 현재 상태 표시 */
function detailState(): string {
  const head = screen.queryByText('입찰 요청 상세')?.closest('.justify-between')
  return text(head?.querySelector('span.rounded-full'))
}

/** 입찰 요청 관리 목록에서 그 공고 제목의 줄 */
function listRow(title: string): HTMLTableRowElement | undefined {
  return Array.from(document.querySelectorAll('tbody tr')).find((tr) => text((tr as HTMLTableRowElement).cells[0]) === title) as
    | HTMLTableRowElement
    | undefined
}

async function filterTab(label: string) {
  const tab = buttonNamed(label)
  if (tab) await click(tab)
}

test('IT-12-031 입찰 요청 관리 목록에서 DOCS_RECEIVED 를 ANALYZING 으로 전이한다', async () => {
  const ID = 'IT-12-031'
  const id = seedId('12.b031_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 12.b031_virtual_bid(DOCS_RECEIVED 입찰 건)가 없다')
    noMaterial(ID, 2, '시딩 12.b031_virtual_bid(DOCS_RECEIVED 입찰 건)가 없다')
    return
  }
  // 목록 줄은 공고 제목으로 찾는다. 제목은 시험 코드에 쓰지 않고 그 입찰 건을 서버에서 읽어 온다
  const title = (await apiGet<{ opportunityTitle?: string }>(`/admin/bid-requests/${id}`))?.opportunityTitle
  await openScreen('/bid-requests')
  await filterTab('문서 접수 완료')
  const row = title ? listRow(title) : undefined
  const opener = row && Array.from(row.querySelectorAll('button')).find((b) => text(b) === '상태 전환')
  if (!row || !opener) {
    noMaterial(ID, 1, `문서 접수 완료 목록 첫 쪽에 그 입찰 건(${title ?? '제목 없음'}) 줄이 없다`)
    noMaterial(ID, 2, `문서 접수 완료 목록 첫 쪽에 그 입찰 건(${title ?? '제목 없음'}) 줄이 없다`)
    return
  }
  const blocked = await transitionTo('분석 중', opener)
  // 문서 접수 완료로 거른 목록에서는 전이한 줄이 빠진다. 분석 중으로 걸러 그 줄의 상태를 본다
  await filterTab('분석 중')
  const after = listRow(title!)
  const state = after ? text(after.cells[3]) : '(줄 없음)'
  judge(ID, 1, !blocked && state === '분석 중' ? 'pass' : 'fail',
    '목록에서 전이한 뒤 그 입찰 건 줄의 상태가 분석 중인지', blocked || `${title} / ${state}`)

  await openScreen(`/bid-requests/${id}`)
  const current = detailState()
  judge(ID, 2, current === '분석 중' ? 'pass' : 'fail', '입찰 상세 관리 머리의 현재 상태가 분석 중인지', current || '(상태 표시 없음)')
})

test('IT-12-034 ANALYZING 을 GENERATING 으로 전이하면 상태가 문서 작성 중이 된다', async () => {
  const ID = 'IT-12-034'
  const id = seedId('12.b034_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 12.b034_virtual_bid(ANALYZING 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const before = detailState()
  const blocked = await transitionTo('문서 작성 중')
  const after = detailState()
  judge(ID, 1, !blocked && after === '문서 작성 중' ? 'pass' : 'fail',
    '다음 상태 문서 작성 중으로 전이한 뒤 상태 표시가 문서 작성 중인지', blocked || `${before} 에서 ${after}`)
})
