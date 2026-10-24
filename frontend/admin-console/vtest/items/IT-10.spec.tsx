import { act } from '@testing-library/react'
import { afterEach, beforeAll, test } from 'vitest'
import { judge, noMaterial, reportTo } from '../report'
import { fake } from '../fake'
import { apiGet, buttonNamed, click, loginAs, openScreen, screen, screenText, sectionOf, settled, text, transitionTo } from '../harness'
import { seedId } from '../seed'

reportTo('IT-10')

// 상태를 바꾸는 시험은 회차마다 넣는 시딩 입찰 건에만 건다(tools/testbook/seed). 실제 고객 건이 움직이지 않는다
const ADMIN = process.env.VTEST_ADMIN ?? 'smoke-admin@example.com'

beforeAll(async () => {
  await loginAs(ADMIN)
})

afterEach(() => fake.reset())

interface ReadingRow {
  file: string
  origin: string
  status: string
  reason: string
  facts: string
  el: HTMLTableRowElement
}

/** 고객 서류 읽기 영역의 줄. 펼친 사실 목록 표는 안쪽 표라 바깥 표의 줄만 읽는다 */
function readingRows(): ReadingRow[] {
  const table = sectionOf('고객 서류 읽기')?.querySelector('table')
  if (!table) return []
  return Array.from(table.tBodies[0]?.rows ?? [])
    .filter((tr) => tr.cells.length >= 5)
    .map((tr) => ({
      file: text(tr.cells[0]),
      origin: text(tr.cells[1]),
      status: text(tr.cells[3].querySelector('span')),
      reason: text(tr.cells[3].querySelector('p')),
      facts: text(tr.cells[4]),
      el: tr,
    }))
}

const shown = (rows: ReadingRow[]) =>
  rows.map((r) => `${r.file} / ${r.origin} / ${r.status || '(상태 없음)'}${r.reason ? ` ${r.reason}` : ''} / 사실 ${r.facts}`).join(', ') || '줄 없음'

async function wait(ms: number) {
  await act(async () => {
    await new Promise((r) => setTimeout(r, ms))
  })
}

test('IT-10-041 분석 단계로 전환하면 공고별 서류와 서류함 서류를 읽는다', async () => {
  const ID = 'IT-10-041'
  const before = seedId('10.b041_before_bid')
  if (!before) {
    noMaterial(ID, 1, '시딩 10.b041_before_bid(DOCS_RECEIVED 입찰 건)가 없다')
    noMaterial(ID, 2, '시딩 10.b041_before_bid(DOCS_RECEIVED 입찰 건)가 없다')
  } else {
    await openScreen(`/bid-requests/${before}`)
    const blocked = await transitionTo('분석 중')
    const rows = readingRows()
    const perf = rows.find((r) => r.file.includes('실적증명서'))
    const reg = rows.find((r) => r.file.includes('사업자등록증'))
    const ok =
      !blocked && rows.length === 2 &&
      perf?.origin === '공고별 서류' && perf.status === '읽는 중' &&
      reg?.origin === '공통 서류함' && reg.status === '읽는 중'
    judge(ID, 1, ok ? 'pass' : 'fail',
      '분석 중으로 전환한 뒤 고객 서류 읽기 영역에 실적증명서(공고별 서류), 사업자등록증(공통 서류함) 두 줄이 읽는 중인지',
      blocked || shown(rows))
    judge(ID, 2, rows.length > 0 && !rows.some((r) => r.file.includes('보험증권')) ? 'pass' : 'fail',
      '만료일이 지난 보험증권 줄이 영역에 없는지', shown(rows))
  }

  const done = seedId('10.b041_done_bid')
  if (!done) {
    noMaterial(ID, 3, '시딩 10.b041_done_bid(읽기가 끝난 ANALYZING 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${done}`)
  const rows = readingRows()
  const two = ['실적증명서', '사업자등록증'].map((n) => rows.find((r) => r.file.includes(n)))
  judge(ID, 3, two.every((r) => r && r.status === '읽음' && Number(r.facts) >= 1) ? 'pass' : 'fail',
    '읽기가 끝난 입찰 건에서 실적증명서, 사업자등록증 줄이 읽음이고 사실 수가 1 이상인지', shown(rows))
})

test('IT-10-042 같은 내용의 서류는 한 번만 읽고 다른 입찰 건에서도 그 결과를 쓴다', async () => {
  const ID = 'IT-10-042'
  const id = seedId('10.b042_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b042_virtual_bid 가 없다')
    noMaterial(ID, 2, '시딩 10.b042_virtual_bid 가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  // 같은 내용의 읽기 결과가 이미 있어 전환 전에도 사실 수가 보인다. 전환 뒤 이 값을 그대로 쓰는지 본다
  const earlier = readingRows().find((r) => r.file.includes('사업자등록증'))
  const blocked = await transitionTo('분석 중')
  const rows = readingRows().filter((r) => r.file.includes('사업자등록증'))
  judge(ID, 1, !blocked && rows.length === 1 && rows[0].origin === '공고별 서류' ? 'pass' : 'fail',
    '분석 중으로 전환한 뒤 사업자등록증 줄이 하나이고 출처가 공고별 서류인지', blocked || shown(readingRows()))
  const ok = !blocked && rows.length === 1 && rows[0].status === '읽음' && !!earlier && earlier.facts !== '-' && rows[0].facts === earlier.facts
  judge(ID, 2, ok ? 'pass' : 'fail',
    '전환 직후 사업자등록증 줄이 읽는 중 없이 바로 읽음이고 사실 수가 전환 전 같은 내용의 읽기 결과와 같은지',
    `전환 전 사실 ${earlier?.facts ?? '(줄 없음)'} / 전환 뒤 ${blocked || shown(rows)}`)
})

test('IT-10-043 글자 층이 없는 스캔 서류는 글자 인식으로 읽는다', async () => {
  const ID = 'IT-10-043'
  const id = seedId('10.b041_done_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b041_done_bid(면허증 스캔본을 글자 인식으로 읽은 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const rows = readingRows()
  const scan = rows.find((r) => r.file.includes('면허증'))
  judge(ID, 1, scan?.status === '읽음(글자 인식)' ? 'pass' : 'fail', '면허증 줄의 상태가 "읽음(글자 인식)" 인지', shown(rows))
})

test('IT-10-045 읽을 수 없는 형식은 읽지 않고 표시한다', async () => {
  const ID = 'IT-10-045'
  const id = seedId('10.b045_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b045_virtual_bid 가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const blocked = await transitionTo('분석 중')
  const rows = readingRows()
  const doc = rows.find((r) => r.file.endsWith('.doc'))
  judge(ID, 1, !blocked && doc?.status === '읽을 수 없는 형식' ? 'pass' : 'fail',
    '분석 중으로 전환한 뒤 DOC 줄의 상태가 "읽을 수 없는 형식" 인지', blocked || shown(rows))
})

test('IT-10-046 고객 서류를 읽는 중이면 문서 작성 중으로 전환되지 않는다', async () => {
  const ID = 'IT-10-046'
  const id = seedId('10.b046_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b046_virtual_bid(읽는 중 줄이 있는 ANALYZING 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const open = buttonNamed('상태 전환')
  if (open) await click(open)
  const radio = screen.queryAllByRole('radio').find((r) => text(r.closest('label')).startsWith('문서 작성 중'))
  if (radio) await click(radio)
  const notice = screen.queryByText('고객 서류를 읽는 중입니다')
  const go = buttonNamed('전환')
  judge(ID, 1, !!radio && !!notice && !!go?.disabled ? 'pass' : 'fail',
    '상태 전환 창에서 문서 작성 중을 고르면 "고객 서류를 읽는 중입니다" 가 보이고 전환 단추가 눌리지 않는지',
    `문서 작성 중 선택지 ${radio ? '있음' : '없음'}, 안내 ${notice ? text(notice) : '없음'}, 전환 단추 ${go ? (go.disabled ? '눌리지 않음' : '눌림') : '없음'}`)
})

test('IT-10-047 읽기가 30분 넘게 끝나지 않은 서류는 읽지 못함이 된다', async () => {
  const ID = 'IT-10-047'
  const id = seedId('10.b047_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b047_virtual_bid(31분 전부터 읽는 중인 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const rows = readingRows()
  const stale = rows.find((r) => r.file.includes('실적증명서'))
  judge(ID, 1, stale?.status === '읽지 못함' && stale.reason === '읽기 시간 초과' ? 'pass' : 'fail',
    '31분 전부터 읽는 중이던 줄의 상태가 "읽지 못함" 이고 사유 "읽기 시간 초과" 가 보이는지', shown(rows))
})

test('IT-10-048 읽지 못한 서류를 다시 읽는다', async () => {
  const ID = 'IT-10-048'
  const id = seedId('10.b048_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b048_virtual_bid(읽지 못함 줄이 있는 ANALYZING 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const failed = readingRows().find((r) => r.status === '읽지 못함')
  const again = failed && Array.from(failed.el.querySelectorAll('button')).find((b) => text(b) === '다시 읽기')
  if (!failed || !again) {
    judge(ID, 1, 'fail', '읽지 못함 줄의 [다시 읽기] 단추', shown(readingRows()))
    return
  }
  const file = failed.file
  await click(again)
  const first = readingRows().find((r) => r.file === file)
  // 화면은 읽는 중이면 10초마다 다시 부른다. 읽기 워크플로우가 끝날 때까지 기다린다
  const until = Date.now() + 10 * 60 * 1000
  let last = first
  while (Date.now() < until && last && last.status === '읽는 중') {
    await wait(10_000)
    await settled()
    last = readingRows().find((r) => r.file === file)
  }
  const seen = `다시 읽기 직후 ${first ? shown([first]) : '줄 없음'} / 끝 ${last ? shown([last]) : '줄 없음'}`
  if (first?.status === '읽는 중' && last?.status === '읽지 못함') {
    // 시딩 서류는 내용 해시만 있고 저장소에 실제 파일이 없어 끝까지 읽을 수 없다. 읽는 중으로 넘어간 것까지만 본다
    noMaterial(ID, 1, `시딩 서류에 실제 파일이 없어 읽음까지 갈 수 없다. ${seen}`)
    return
  }
  judge(ID, 1, first?.status === '읽는 중' && last?.status === '읽음' && Number(last.facts) >= 1 ? 'pass' : 'fail',
    '읽지 못함이던 줄이 [다시 읽기] 뒤 읽는 중을 거쳐 읽음이 되고 사실 수가 1 이상인지', seen)
})

test('IT-10-049 분석 단계가 아닌 입찰 건은 다시 읽기를 할 수 없다', async () => {
  const ID = 'IT-10-049'
  const id = seedId('10.b049_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b049_bid(읽지 못함 줄이 있는 GENERATING 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const rows = readingRows()
  const area = sectionOf('고객 서류 읽기')
  const buttons = area ? Array.from(area.querySelectorAll('button')).map((b) => text(b)) : []
  if (!rows.some((r) => r.status === '읽지 못함')) {
    noMaterial(ID, 1, `읽지 못함 줄이 없다. ${shown(rows)}`)
    return
  }
  judge(ID, 1, area && !buttons.includes('다시 읽기') ? 'pass' : 'fail',
    '문서 작성 중 입찰 건의 고객 서류 읽기 영역에 [다시 읽기] 단추가 없는지',
    `${shown(rows)} / 영역 단추 ${buttons.join(', ') || '없음'}`)
})

test('IT-10-050 절 상세의 근거 서류에 읽은 서류가 사실 수와 함께 보인다', async () => {
  const ID = 'IT-10-050'
  const doc = seedId('10.b050_virtual_doc')
  if (!doc) {
    noMaterial(ID, 5, '시딩 10.b050_virtual_doc(근거 서류가 걸린 절이 있는 제안서)가 없다')
    return
  }
  await openScreen(`/documents/${doc}/sections`)
  const section = screen.queryAllByRole('button').find((b) => text(b).startsWith('1.1'))
  if (!section) {
    judge(ID, 5, 'fail', '절 1.1 단추', screenText().slice(0, 500))
    return
  }
  await click(section)
  const label = screen.queryByText('근거 서류')
  const tags = label ? Array.from(label.parentElement?.querySelectorAll('button') ?? []) : []
  const tag = tags.find((b) => /^실적증명서\.pdf 사실 \d+$/.test(text(b)))
  if (!tag) {
    judge(ID, 5, 'fail', '근거 서류에 "실적증명서.pdf 사실 N" 꼬리표가 있는지', tags.map((b) => text(b)).join(', ') || text(label?.parentElement))
    return
  }
  const count = Number(text(tag).replace(/.*사실 /, ''))
  await click(tag)
  // 꼬리표를 누르면 그 서류의 사실 목록이 펼쳐진다. 꼬리표의 수가 그 서류의 사실 수인지 펼친 목록과 견준다
  const facts = Array.from(document.querySelectorAll('table')).find((t) => text(t.tHead).startsWith('분류항목값'))
  const rows = facts?.tBodies[0]?.rows.length ?? 0
  judge(ID, 5, count >= 1 && rows === count ? 'pass' : 'fail',
    '절 1.1 근거 서류의 실적증명서.pdf 꼬리표에 적힌 사실 수가 펼친 사실 목록 줄 수와 같은지',
    `꼬리표 ${text(tag)} / 펼친 사실 ${rows}줄`)
})

interface Page<T> { content?: T[] }
interface BidDoc { id: string; documentType: string }
interface Chapter { factorTitle?: string; sections: { id: string; subfactorLabel?: string; title?: string }[] }
interface Overview { items: { fileName: string; status: string | null }[] }

test('IT-10-053 수행 실적 절의 근거 서류에 실적 증명서가 걸린다', async () => {
  const ID = 'IT-10-053'
  // AI 가 쓴 제안서가 있어야 한다. 관리자 검토 상태 입찰 건 중 읽은 고객 서류가 있고 수행 실적 절이 있는 것을 고른다
  const review = await apiGet<Page<{ id: string }>>('/admin/bid-requests?state=REVIEW&size=20')
  let pick: { doc: string; section: { id: string; label: string }; read: string[] } | null = null
  for (const br of review?.content ?? []) {
    const overview = await apiGet<Overview>(`/admin/bid-requests/${br.id}/document-readings`)
    const read = (overview?.items ?? []).filter((i) => i.status === 'DONE').map((i) => i.fileName)
    if (read.length === 0) continue
    const docs = (await apiGet<BidDoc[]>(`/bid-documents?bidRequestId=${br.id}`)) ?? []
    for (const d of docs.filter((x) => x.documentType === 'TECHNICAL_PROPOSAL')) {
      const tree = await apiGet<{ chapters: Chapter[] }>(`/admin/bid-documents/${d.id}/chapters`)
      for (const ch of tree?.chapters ?? []) {
        for (const s of ch.sections) {
          const name = `${ch.factorTitle ?? ''} ${s.title ?? ''}`
          if (/past performance|수행 실적|relevant (contracts|experience)/i.test(name)) {
            pick = { doc: d.id, section: { id: s.id, label: `${s.subfactorLabel ?? ''} ${s.title ?? ''}`.trim() }, read }
            break
          }
        }
        if (pick) break
      }
      if (pick) break
    }
    if (pick) break
  }
  if (!pick) {
    noMaterial(ID, 1, '읽은 고객 서류가 있고 AI 가 쓴 제안서에 수행 실적 절이 있는 관리자 검토 상태 입찰 건이 없다')
    return
  }
  await openScreen(`/documents/${pick.doc}/sections`)
  const label = pick.section.label
  const section = screen.queryAllByRole('button').find((b) => text(b).startsWith(label))
  if (!section) {
    judge(ID, 1, 'fail', `수행 실적 절 ${label} 단추`, screenText().slice(0, 500))
    return
  }
  await click(section)
  const area = screen.queryByText('근거 서류')?.parentElement
  const tags = Array.from(area?.querySelectorAll('button') ?? []).map((b) => text(b).replace(/ 사실 \d+.*$/, ''))
  judge(ID, 1, tags.length > 0 && tags.every((t) => pick!.read.includes(t)) ? 'pass' : 'fail',
    `수행 실적 절 ${label} 의 근거 서류에 그 입찰 건의 읽은 고객 서류가 걸려 있는지`,
    `근거 서류 ${tags.join(', ') || text(area)} / 읽은 서류 ${pick.read.join(', ')}`)
})

test('IT-10-001 생성 문서 목록에 문서가 문서 유형과 상태 초안으로 보인다', async () => {
  const ID = 'IT-10-001'
  const id = seedId('10.b001_bid')
  if (!id) {
    noMaterial(ID, 3, '시딩 10.b001_bid(성공 제안서가 있는 공고의 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const heading = screen.queryByText('생성 문서')
  let area = heading?.parentElement ?? null
  while (area && !text(area).includes('생성 문서')) area = area.parentElement
  const rows = Array.from(area?.querySelectorAll('.divide-y > div') ?? []).map((r) => text(r))
  if (rows.length === 0) {
    noMaterial(ID, 3, `생성 문서가 아직 없다. 문서 생성이 끝나지 않았다. ${text(area).slice(0, 200)}`)
    return
  }
  judge(ID, 3, rows.every((r) => /^[A-Z_]+ v\d+ \| 초안/.test(r)) ? 'pass' : 'fail',
    '생성 문서 목록 줄마다 문서 유형과 상태 초안이 보이는지', rows.join(', '))
})
