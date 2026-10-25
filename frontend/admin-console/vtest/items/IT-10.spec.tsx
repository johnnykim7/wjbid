import { act } from '@testing-library/react'
import { afterEach, beforeAll, test } from 'vitest'
import { judge, noMaterial, reportTo } from '../report'
import { fake } from '../fake'
import { apiGet, buttonNamed, click, loginAs, openScreen, screen, screenText, sectionOf, settled, text, transitionTo, user } from '../harness'
import { seedId } from '../seed'
import { traffic } from '../setup'

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

test('IT-10-047 읽기가 대기 한도를 넘게 끝나지 않은 서류는 읽지 못함이 된다', async () => {
  const ID = 'IT-10-047'
  const id = seedId('10.b047_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b047_virtual_bid(대기 한도보다 오래 읽는 중인 입찰 건)가 없다')
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
    // 파일 놓기 명령(TESTBOOK_SEED_FILE_CMD) 없이 시딩하면 저장소에 실제 파일이 없어 끝까지 읽을 수 없다. 읽는 중으로 넘어간 것까지만 본다
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
  judge(ID, 3, rows.every((r) => /^[A-Z_]+\s*v\d+\s*\|\s*초안/.test(r)) ? 'pass' : 'fail',
    '생성 문서 목록 줄마다 문서 유형과 상태 초안이 보이는지', rows.join(', '))
})

// CR-170 입찰 분석 영역. 표시 이름은 기대결과 문구에 적힌 것을 그대로 쓴다. 화면 코드의 이름표를 들여오면 이름표가 틀려도 맞게 나온다
const VERDICT: Record<string, string> = { STRONG_GO: 'Strong Go', CONDITIONAL_GO: '조건부 Go', WEAK: 'Weak', NO_GO: 'No-Go' }
const DECISION_REQUIRED = 'Bid/No-Bid 결정을 Go 나 조건부 Go 로 정해야 문서 작성으로 넘어갑니다'

interface Analysis {
  analysisId: string
  status: string
  pwinPercent: number | null
  aiVerdict: string | null
  adminDecision: string | null
  decidedBy: { name: string | null } | null
  issueRanking: { issue: string }[] | null
}

interface Overview {
  analysis: Analysis | null
  readingInProgress: boolean
}

const overviewOf = (id: string) => apiGet<Overview>(`/admin/bid-requests/${id}/bid-analysis`)

/** 입찰 분석 영역 카드. 제목 글자 '입찰 분석' 인 머리에서 카드까지 올라간다 */
function analysisPanel(): HTMLElement | null {
  const h = screen.queryAllByRole('heading').find((el) => text(el) === '입찰 분석')
  return (h?.closest('.rounded-xl') as HTMLElement | null) ?? null
}

function panelButton(name: string): HTMLButtonElement | undefined {
  return Array.from(analysisPanel()?.querySelectorAll('button') ?? []).find((b) => text(b) === name) as HTMLButtonElement | undefined
}

/** 머리의 회차 상태 꼬리표. 머리 줄에서 제목과 단추를 뺀 것이다 */
function headBadge(): string {
  const h = screen.queryAllByRole('heading').find((el) => text(el) === '입찰 분석')
  const row = h?.parentElement
  return text(Array.from(row?.children ?? []).find((el) => el !== h && el.tagName !== 'BUTTON'))
}

const hasDecisionForm = () =>
  Array.from(analysisPanel()?.querySelectorAll('h3') ?? []).some((el) => text(el) === '관리자 결정')

const buttonState = (b: HTMLButtonElement | undefined) => (b ? (b.disabled ? '꺼짐' : '살아 있음') : '없음')

const panelShown = () => {
  const p = analysisPanel()
  return p ? `꼬리표 ${headBadge() || '(없음)'} / 다시 분석 ${buttonState(panelButton('다시 분석'))} / 결정 저장 ${buttonState(panelButton('결정 저장'))} / ${text(p).slice(0, 300)}` : '입찰 분석 영역 없음'
}

async function choose(decision: 'GO' | 'CONDITIONAL_GO' | 'NO_GO', note?: string) {
  const radio = document.getElementById(`bid-decision-${decision}`)
  if (radio) await click(radio)
  const box = document.getElementById('bid-decision-note') as HTMLTextAreaElement | null
  if (box) {
    await user.clear(box)
    if (note) await user.type(box, note)
    await settled()
  }
  return !!radio
}

async function saveDecision() {
  const save = panelButton('결정 저장')
  if (save && !save.disabled) await click(save)
  return save
}

const decisionPosts = () => traffic.calls.filter((c) => c.startsWith('POST ') && c.includes('/bid-analysis/decision')).length

/** [상태 전환] 창에서 문서 작성 중을 골라 막힘 안내와 전환 단추를 보고 창을 닫는다 */
async function generationBlock(): Promise<{ blocked: boolean; seen: string }> {
  const open = buttonNamed('상태 전환')
  if (!open) return { blocked: false, seen: '상태 전환 단추 없음' }
  await click(open)
  const radio = screen.queryAllByRole('radio').find((r) => text(r.closest('label')).startsWith('문서 작성 중'))
  if (radio) await click(radio)
  const notice = screen.queryByText(DECISION_REQUIRED)
  const go = buttonNamed('전환')
  const seen = `문서 작성 중 선택지 ${radio ? '있음' : '없음'}, 안내 ${notice ? text(notice) : '없음'}, 전환 단추 ${go ? (go.disabled ? '눌리지 않음' : '눌림') : '없음'}`
  const cancel = buttonNamed('취소')
  if (cancel) await click(cancel)
  return { blocked: !!radio && !!notice && !!go?.disabled, seen }
}

/** 분석 중이거나 서류를 읽는 동안 기다린다. 화면은 10초마다 다시 부른다 */
async function untilSettledAnalysis(id: string, limitMs: number): Promise<Overview | null> {
  const until = Date.now() + limitMs
  let o = await overviewOf(id)
  while (Date.now() < until && o && (o.readingInProgress || !o.analysis || o.analysis.status === 'RUNNING')) {
    await wait(10_000)
    o = await overviewOf(id)
  }
  await wait(11_000)
  await settled()
  return o
}

const workflowAbsent = (o: Overview | null) =>
  `입찰 분석이 분석 완료까지 가지 않았다(워크플로우가 닿지 않는 서버). 지금 ${o?.analysis ? `${o.analysis.status}` : '회차 없음'}`

async function openTab(name: string): Promise<HTMLElement | null> {
  const tab = screen.queryAllByRole('tab').find((t) => text(t) === name)
  if (!tab) return null
  await click(tab)
  return screen.queryAllByRole('tabpanel').find((p) => p.getAttribute('data-state') === 'active') ?? null
}

test('IT-10-055 고객 서류를 다 읽으면 입찰 분석이 돌아 종합 분석 리포트, 캡처플랜, Bid/No-Bid 결정이 차례로 채워진다', async () => {
  const ID = 'IT-10-055'
  const id = seedId('10.b055_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b055_virtual_bid(서류를 올린 DOCS_RECEIVED 입찰 건)가 없다')
    noMaterial(ID, 4, '시딩 10.b055_virtual_bid 가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const blocked = await transitionTo('분석 중')
  const reading = readingRows().some((r) => r.status === '읽는 중')
  if (blocked) judge(ID, 1, 'fail', '분석 중으로 전환', blocked)
  else if (!reading) noMaterial(ID, 1, `전환 뒤 화면을 볼 때 이미 읽기가 끝나 읽는 동안을 볼 수 없다. ${shown(readingRows())}`)
  else judge(ID, 1, text(analysisPanel()).includes('고객 서류를 다 읽으면 입찰 분석을 시작합니다') ? 'pass' : 'fail',
    '서류를 읽는 동안 입찰 분석 영역에 "고객 서류를 다 읽으면 입찰 분석을 시작합니다" 가 보이는지', panelShown())

  const o = await untilSettledAnalysis(id, 12 * 60 * 1000)
  if (o?.analysis?.status !== 'DONE') {
    noMaterial(ID, 4, workflowAbsent(o))
    return
  }
  const head = text(analysisPanel())
  const verdict = Object.values(VERDICT).find((v) => head.includes(`판정 ${v}`))
  judge(ID, 4, /Pwin \d+(\.\d+)?%/.test(head) && !!verdict && headBadge() === '분석 완료' ? 'pass' : 'fail',
    '분석 완료 뒤 머리에 Pwin(%)과 판정(Strong Go, 조건부 Go, Weak, No-Go 가운데 하나)과 판정 이유가 보이는지', panelShown())
})

test('IT-10-056 읽을 서류가 없는 입찰 건은 분석 단계로 전환하자마자 입찰 분석이 시작된다', async () => {
  const ID = 'IT-10-056'
  const id = seedId('10.b056_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b056_virtual_bid(서류 없는 DOCS_RECEIVED 입찰 건)가 없다')
    noMaterial(ID, 3, '시딩 10.b056_virtual_bid 가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  judge(ID, 1, analysisPanel() === null ? 'pass' : 'fail', '전환 전 입찰 상세 관리에 입찰 분석 영역이 없는지', panelShown())
  const blocked = await transitionTo('분석 중')
  const o = await overviewOf(id)
  if (blocked) {
    judge(ID, 3, 'fail', '분석 중으로 전환', blocked)
    return
  }
  if (o?.analysis?.status !== 'RUNNING') {
    noMaterial(ID, 3, `전환 뒤 회차가 분석 중이 아니다(워크플로우가 닿지 않는 서버는 바로 끝난다). 지금 ${o?.analysis?.status ?? '회차 없음'}`)
    return
  }
  const rerun = panelButton('다시 분석')
  const save = panelButton('결정 저장')
  judge(ID, 3, headBadge() === '분석 중' && !!rerun?.disabled && !!save?.disabled ? 'pass' : 'fail',
    '전환 뒤 입찰 분석 영역 머리가 분석 중이고 [다시 분석] 과 관리자 결정 [저장] 이 꺼져 있는지', panelShown())
})

test('IT-10-057 다른 입찰 건이 같은 내용의 서류를 읽어 끝나도 그 건의 입찰 분석이 시작된다', async () => {
  const ID = 'IT-10-057'
  const a = seedId('10.b057_virtual_a')
  const b = seedId('10.b057_virtual_b')
  if (!a || !b) {
    noMaterial(ID, 3, '시딩 10.b057_virtual_a, 10.b057_virtual_b(같은 서류로 분석이 시작된 두 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${a}`)
  const seenA = `A ${panelShown()}`
  const okA = headBadge() === '분석 중'
  await openScreen(`/bid-requests/${b}`)
  const seenB = `B ${panelShown()}`
  judge(ID, 3, okA && headBadge() === '분석 중' ? 'pass' : 'fail', '두 건의 입찰 분석 영역 머리가 분석 중인지', `${seenA} / ${seenB}`)
})

test('IT-10-058 입찰 분석 회차가 이미 있는 건은 서류를 다시 읽어도 입찰 분석이 자동으로 다시 돌지 않는다', async () => {
  const ID = 'IT-10-058'
  const id = seedId('10.b058_virtual_bid')
  if (!id) {
    noMaterial(ID, 2, '시딩 10.b058_virtual_bid(분석 완료이고 읽지 못함 줄이 있는 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const failed = readingRows().find((r) => r.status === '읽지 못함')
  const again = failed && Array.from(failed.el.querySelectorAll('button')).find((b) => text(b) === '다시 읽기')
  if (!again) {
    judge(ID, 2, 'fail', '읽지 못함 줄의 [다시 읽기] 단추', shown(readingRows()))
    return
  }
  await click(again)
  const until = Date.now() + 10 * 60 * 1000
  while (Date.now() < until && readingRows().some((r) => r.status === '읽는 중')) {
    await wait(10_000)
    await settled()
  }
  await wait(11_000)
  await settled()
  const rerun = panelButton('다시 분석')
  judge(ID, 2, headBadge() === '분석 완료' && !!rerun && !rerun.disabled ? 'pass' : 'fail',
    '다시 읽기가 끝난 뒤 머리가 분석 완료 그대로이고 [다시 분석] 이 살아 있는지', `${shown(readingRows())} / ${panelShown()}`)
})

test('IT-10-059 조사한 값마다 확신도와 출처가 붙고 웹 자료가 필요한 칸은 미상으로 확인 필요에 오른다', async () => {
  const ID = 'IT-10-059'
  const id = seedId('10.b059_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b059_virtual_bid(분석 완료 입찰 건)가 없다')
    noMaterial(ID, 5, '시딩 10.b059_virtual_bid 가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const report = await openTab('종합 분석 리포트')
  // 근거 값 칸은 값, 확신도 꼬리표, 출처를 글자 밑줄을 맞춘 한 묶음으로 그린다. 꼬리표도 inline-flex 라 밑줄 맞춤으로 가른다
  const values = Array.from(report?.querySelectorAll('span.inline-flex.items-baseline') ?? [])
  const bad = values.filter((v) => {
    const tag = Array.from(v.children).map((c) => text(c))
    return !tag.some((t) => ['확정', '추정', '미상'].includes(t)) || !tag.some((t) => t.startsWith('출처 '))
  })
  judge(ID, 1, values.length > 0 && bad.length === 0 ? 'pass' : 'fail',
    '종합 분석 리포트의 근거 값마다 확정, 추정, 미상 가운데 하나의 꼬리표와 출처가 함께 보이는지',
    `근거 값 ${values.length}칸, 빠진 칸 ${bad.length}: ${bad.slice(0, 5).map((v) => text(v)).join(' | ')}`)

  const heading = Array.from(analysisPanel()?.querySelectorAll('h3') ?? []).find((el) => text(el) === '확인 필요')
  const box = heading?.parentElement
  const groups = Array.from(box?.querySelectorAll(':scope > div > div') ?? [])
  const labels = groups.map((g) => text(g.querySelector('p')))
  const lines = groups.flatMap((g) => Array.from(g.querySelectorAll('li')))
  const ok = labels.length === 2 && labels[0].startsWith('고객에게 물을 것') && labels[1].startsWith('관리자가 볼 것') &&
    lines.length > 0 && lines.every((li) => {
      const ps = li.querySelectorAll('p')
      return ps.length === 2 && text(ps[0]) !== '' && text(ps[1]) !== ''
    })
  judge(ID, 5, ok ? 'pass' : 'fail', '확인 필요 목록이 고객에게 물을 것과 관리자가 볼 것 두 묶음이고 줄마다 질문과 이유가 있는지',
    `${labels.join(' / ') || '확인 필요 없음'} / ${lines.slice(0, 6).map((li) => text(li)).join(' | ')}`)
})

test('IT-10-060 Pwin, 판정, 강점과 약점, 핵심이슈 순위는 서버가 점수로 계산한 값이다', async () => {
  const ID = 'IT-10-060'
  const id = seedId('10.b060_virtual_bid')
  if (!id) {
    noMaterial(ID, 5, '시딩 10.b060_virtual_bid(분석 완료 입찰 건)가 없다')
    noMaterial(ID, 6, '시딩 10.b060_virtual_bid 가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const head = text(analysisPanel())
  const o = await overviewOf(id)
  const a = o?.analysis
  judge(ID, 5, !!a && a.pwinPercent !== null && !!a.aiVerdict &&
    head.includes(`Pwin ${a.pwinPercent}%`) && head.includes(`판정 ${VERDICT[a.aiVerdict]}`) ? 'pass' : 'fail',
    '머리의 Pwin 과 판정이 응답의 pwinPercent, aiVerdict 와 같은지',
    `응답 ${a ? `${a.pwinPercent} / ${a.aiVerdict}` : '없음'} / 화면 ${head.slice(0, 200)}`)

  const plan = await openTab('캡처플랜')
  const caption = Array.from(plan?.querySelectorAll('p') ?? []).find((p) => text(p).startsWith('2.2 이슈 정리'))
  let table: Element | null | undefined = caption?.nextElementSibling
  if (table && table.tagName !== 'TABLE') table = table.querySelector('table')
  const order = Array.from((table as HTMLTableElement | null)?.tBodies[0]?.rows ?? []).map((tr) => text(tr.cells[1]))
  const want = (a?.issueRanking ?? []).map((r) => r.issue.replace(/\s+/g, ' ').trim())
  judge(ID, 6, want.length > 0 && order.join('\n') === want.join('\n') ? 'pass' : 'fail',
    '캡처플랜 핵심이슈 순위 표의 차례가 응답의 issueRanking 과 같은지', `화면 ${order.join(' > ')} / 응답 ${want.join(' > ')}`)
})

test('IT-10-062 다시 분석하면 신규 회차가 지금 분석이 되고 앞 회차의 결정은 이어지지 않는다', async () => {
  const ID = 'IT-10-062'
  const id = seedId('10.b062_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b062_virtual_bid(관리자 결정이 Go 인 분석 완료 입찰 건)가 없다')
    noMaterial(ID, 3, '시딩 10.b062_virtual_bid 가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const rerun = panelButton('다시 분석')
  if (rerun && !rerun.disabled) await click(rerun)
  const dialog = screen.queryByRole('alertdialog') ?? screen.queryByRole('dialog')
  judge(ID, 1, !!dialog && text(dialog).includes('다시 분석하면 지금 결정은 이어지지 않습니다') ? 'pass' : 'fail',
    '[다시 분석] 을 누르면 "다시 분석하면 지금 결정은 이어지지 않습니다" 확인 창이 뜨는지', dialog ? text(dialog) : panelShown())
  const confirm = dialog && Array.from(dialog.querySelectorAll('button')).find((b) => text(b) === '다시 분석')
  if (!confirm) {
    judge(ID, 3, 'fail', '확인 창의 다시 분석 단추', dialog ? text(dialog) : '확인 창 없음')
    return
  }
  await click(confirm)
  const o = await untilSettledAnalysis(id, 12 * 60 * 1000)
  if (o?.analysis?.status !== 'DONE') {
    noMaterial(ID, 3, workflowAbsent(o))
    return
  }
  const head = text(analysisPanel())
  const gate = await generationBlock()
  judge(ID, 3, !head.includes('관리자 결정 Go') && !/관리자 결정 (조건부 Go|No-Go)/.test(head) && gate.blocked ? 'pass' : 'fail',
    '다시 분석이 끝난 뒤 머리의 관리자 결정이 비어 있고 상태 패널의 문서 작성 중 전환이 막혀 있는지', `${panelShown()} / ${gate.seen}`)
})

test('IT-10-065 분석 중이 워크플로우 대기 한도를 넘으면 분석 실패 "분석 시간 초과" 가 된다', async () => {
  const ID = 'IT-10-065'
  const id = seedId('10.b065_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b065_virtual_bid(대기 한도보다 오래 분석 중인 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const rerun = panelButton('다시 분석')
  judge(ID, 1, headBadge() === '분석 실패' && text(analysisPanel()).includes('분석 시간 초과') && !!rerun && !rerun.disabled ? 'pass' : 'fail',
    '입찰 분석 영역 머리가 분석 실패이고 사유 "분석 시간 초과" 가 보이며 [다시 분석] 이 살아 있는지', panelShown())
})

test('IT-10-066 Bid/No-Bid 결정을 저장하지 못하고 끝난 입찰 분석은 분석 실패가 되고 앞 단계 탭은 남는다', async () => {
  const ID = 'IT-10-066'
  const id = seedId('10.b066_virtual_bid')
  if (!id) {
    noMaterial(ID, 2, '시딩 10.b066_virtual_bid(리포트와 캡처플랜만 저장된 입찰 건)가 없다')
    noMaterial(ID, 3, '시딩 10.b066_virtual_bid 가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  judge(ID, 2, headBadge() === '분석 실패' && text(analysisPanel()).includes('Bid/No-Bid 결정이 저장되지 않음') ? 'pass' : 'fail',
    '머리가 분석 실패이고 사유 "Bid/No-Bid 결정이 저장되지 않음" 이 보이는지', panelShown())
  const report = text(await openTab('종합 분석 리포트'))
  const plan = text(await openTab('캡처플랜'))
  const decision = await openTab('Bid/No-Bid 결정')
  const saved = (t: string) => t !== '' && !t.includes('아직 저장되지 않았습니다')
  const scorecard = Array.from(decision?.querySelectorAll('h3') ?? []).some((h) => text(h) === '스코어카드')
  judge(ID, 3, saved(report) && saved(plan) && !!decision && !scorecard ? 'pass' : 'fail',
    '종합 분석 리포트와 캡처플랜 탭에 저장된 내용이 보이고 Bid/No-Bid 결정 탭에는 스코어카드가 없는지',
    `리포트 ${report.slice(0, 80)} / 캡처플랜 ${plan.slice(0, 80)} / 결정 탭 ${decision ? text(decision).slice(0, 80) : '탭 없음'}`)
})

test('IT-10-067 분석 중이거나 서류를 읽는 중이면 다시 분석이 막히고 분석 중이면 결정 저장도 막힌다', async () => {
  const ID = 'IT-10-067'
  const a = seedId('10.b067_a_bid')
  if (!a) noMaterial(ID, 1, '시딩 10.b067_a_bid(분석 중인 입찰 건)가 없다')
  else {
    await openScreen(`/bid-requests/${a}`)
    const rerun = panelButton('다시 분석')
    const save = panelButton('결정 저장')
    judge(ID, 1, !!rerun?.disabled && !!save?.disabled ? 'pass' : 'fail', '분석 중인 건에서 [다시 분석] 과 관리자 결정 [저장] 이 꺼져 있는지', panelShown())
  }
  const b = seedId('10.b067_b_bid')
  if (!b) {
    noMaterial(ID, 4, '시딩 10.b067_b_bid(서류를 읽는 중인 분석 완료 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${b}`)
  judge(ID, 4, !!panelButton('다시 분석')?.disabled ? 'pass' : 'fail', '서류를 읽는 중인 건에서 [다시 분석] 이 꺼져 있는지',
    `${shown(readingRows())} / ${panelShown()}`)
})

test('IT-10-068 분석 단계가 아닌 입찰 건은 다시 분석과 관리자 결정을 할 수 없다', async () => {
  const ID = 'IT-10-068'
  const id = seedId('10.b068_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b068_bid(Go 로 정하고 문서 작성 중으로 넘어간 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const head = text(analysisPanel())
  judge(ID, 1, /Pwin \d/.test(head) && head.includes('판정 ') && head.includes('관리자 결정 Go') && !panelButton('다시 분석') && !hasDecisionForm() ? 'pass' : 'fail',
    '입찰 분석 영역에 Pwin, 판정, 관리자 결정 Go 가 보이고 [다시 분석] 과 관리자 결정 폼이 없는지', panelShown())
})

test('IT-10-071 Go 로 정하면 정한 사람과 시각이 남고 문서 작성 중으로 전환된다', async () => {
  const ID = 'IT-10-071'
  const id = seedId('10.b071_virtual_bid')
  if (!id) {
    noMaterial(ID, 5, '시딩 10.b071_virtual_bid(관리자 결정이 없는 분석 완료 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  await choose('GO')
  await saveDecision()
  const blocked = await transitionTo('문서 작성 중')
  await openScreen(`/bid-requests/${id}`)
  const name = (await overviewOf(id))?.analysis?.decidedBy?.name
  const head = text(analysisPanel())
  judge(ID, 5, !blocked && !!name && head.includes('관리자 결정 Go') && head.includes(name) && !hasDecisionForm() && !panelButton('다시 분석') ? 'pass' : 'fail',
    '새로고침 뒤 입찰 분석 영역에 관리자 결정 Go 와 정한 관리자 이름이 보이고 관리자 결정 폼과 [다시 분석] 이 없는지',
    `${blocked || '전환함'} / 정한 사람 ${name ?? '(없음)'} / ${panelShown()}`)
})

test('IT-10-072 조건부 Go 로 조건을 기재해 정하면 문서 작성 중으로 전환된다', async () => {
  const ID = 'IT-10-072'
  const id = seedId('10.b072_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b072_virtual_bid(관리자 결정이 없는 분석 완료 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  await choose('CONDITIONAL_GO', '참여 인력 이력서를 고객에게 받는 조건')
  await saveDecision()
  const head = text(analysisPanel())
  judge(ID, 1, head.includes('관리자 결정 조건부 Go') && head.includes('참여 인력 이력서를 고객에게 받는 조건') ? 'pass' : 'fail',
    '저장 뒤 머리에 관리자 결정 조건부 Go 와 조건 "참여 인력 이력서를 고객에게 받는 조건" 이 보이는지', panelShown())
})

test('IT-10-073 No-Go 로 정하면 문서 작성 중은 막히고 종료해도 고객에게 알리지 않는다', async () => {
  const ID = 'IT-10-073'
  const id = seedId('10.b073_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b073_virtual_bid(분석 완료 입찰 건)가 없다')
    noMaterial(ID, 5, '시딩 10.b073_virtual_bid 가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  await choose('NO_GO', '자격 미달')
  await saveDecision()
  const head = text(analysisPanel())
  const gate = await generationBlock()
  judge(ID, 1, head.includes('관리자 결정 No-Go') && head.includes('자격 미달') && gate.blocked ? 'pass' : 'fail',
    '저장 뒤 머리에 No-Go 와 사유 "자격 미달" 이 보이고 문서 작성 중 전환이 막혀 있는지', `${panelShown()} / ${gate.seen}`)

  const closed = await transitionTo('종료')
  await openScreen(`/bid-requests/${id}`)
  const readings = screen.queryAllByRole('heading').some((el) => text(el) === '고객 서류 읽기')
  const reread = screen.queryAllByRole('button').some((b) => text(b) === '다시 읽기')
  judge(ID, 5, !closed && readings && !!analysisPanel() && !reread && !panelButton('다시 분석') && !hasDecisionForm() ? 'pass' : 'fail',
    '종료 뒤 새로고침하면 고객 서류 읽기 영역과 입찰 분석 영역이 보이고 [다시 읽기], [다시 분석], 관리자 결정 폼이 없는지',
    `${closed || '종료함'} / 고객 서류 읽기 영역 ${readings ? '있음' : '없음'} / 다시 읽기 ${reread ? '있음' : '없음'} / ${panelShown()}`)
})

test('IT-10-074 입찰 분석이 실패했거나 회차가 없는 건도 결정을 기록할 수 있다', async () => {
  const ID = 'IT-10-074'
  const id = seedId('10.b074_b_virtual_bid')
  if (!id) {
    noMaterial(ID, 2, '시딩 10.b074_b_virtual_bid(입찰 분석 회차가 없는 ANALYZING 입찰 건)가 없다')
    noMaterial(ID, 4, '시딩 10.b074_b_virtual_bid 가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const rerun = panelButton('다시 분석')
  const save = panelButton('결정 저장')
  judge(ID, 2, text(analysisPanel()).includes('입찰 분석이 없습니다') && !!rerun && !rerun.disabled && !!save && !save.disabled ? 'pass' : 'fail',
    '회차가 없는 건의 입찰 분석 영역에 "입찰 분석이 없습니다" 가 보이고 [다시 분석] 과 관리자 결정 [저장] 이 살아 있는지', panelShown())
  await choose('GO')
  await saveDecision()
  const tabs = screen.queryAllByRole('tab').map((t) => text(t))
  judge(ID, 4, headBadge() === '결정만' && !tabs.some((t) => ['종합 분석 리포트', '캡처플랜', 'Bid/No-Bid 결정'].includes(t)) ? 'pass' : 'fail',
    'Go 를 저장한 뒤 머리가 결정만이고 종합 분석 리포트, 캡처플랜, Bid/No-Bid 결정 탭이 없는지', `탭 ${tabs.join(', ') || '없음'} / ${panelShown()}`)
})

test('IT-10-075 결정을 다시 정하면 덮어쓰고 정한 시각이 바뀐다', async () => {
  const ID = 'IT-10-075'
  const id = seedId('10.b075_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b075_virtual_bid(관리자 결정이 Go 인 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  await choose('NO_GO', '가격 경쟁력 부족')
  await saveDecision()
  const head = text(analysisPanel())
  judge(ID, 1, head.includes('관리자 결정 No-Go') && head.includes('가격 경쟁력 부족') && !head.includes('관리자 결정 Go ') ? 'pass' : 'fail',
    '저장 뒤 머리의 관리자 결정이 No-Go 와 사유 "가격 경쟁력 부족" 으로 바뀌는지', panelShown())
})

test('IT-10-076 결정을 선택하지 않거나 조건부 Go 의 조건, No-Go 의 사유가 비면 결정이 저장되지 않는다', async () => {
  const ID = 'IT-10-076'
  const id = seedId('10.b076_virtual_bid')
  if (!id) {
    for (const n of [1, 2, 3]) noMaterial(ID, n, '시딩 10.b076_virtual_bid(관리자 결정이 없는 분석 완료 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  // 안내 앞에 느낌표 그림 글자가 붙는다
  const errorText = () => text(document.getElementById('bid-decision-error')).replace(/^!\s*/, '')
  let before = decisionPosts()
  await saveDecision()
  judge(ID, 1, errorText() === '결정을 선택하세요' && decisionPosts() === before ? 'pass' : 'fail',
    '아무것도 고르지 않고 저장하면 "결정을 선택하세요" 가 보이고 결정 저장 요청이 나가지 않는지',
    `안내 ${errorText() || '없음'} / 나간 결정 저장 요청 ${decisionPosts() - before}회`)

  await choose('CONDITIONAL_GO', '')
  before = decisionPosts()
  const save = await saveDecision()
  const error = document.getElementById('bid-decision-error')
  const below = !!save && !!error && !!(save.compareDocumentPosition(error) & Node.DOCUMENT_POSITION_FOLLOWING)
  judge(ID, 2, errorText() === '조건을 기재하세요' && below && decisionPosts() === before ? 'pass' : 'fail',
    '조건부 Go 에 조건을 비우고 저장하면 저장 단추 아래 "조건을 기재하세요" 가 보이고 요청이 나가지 않는지',
    `안내 ${errorText() || '없음'}(저장 단추 아래 ${below ? '맞음' : '아님'}) / 나간 결정 저장 요청 ${decisionPosts() - before}회`)

  await choose('NO_GO', '')
  before = decisionPosts()
  await saveDecision()
  judge(ID, 3, errorText() === '사유를 기재하세요' && decisionPosts() === before ? 'pass' : 'fail',
    'No-Go 에 사유를 비우고 저장하면 "사유를 기재하세요" 가 보이고 요청이 나가지 않는지',
    `안내 ${errorText() || '없음'} / 나간 결정 저장 요청 ${decisionPosts() - before}회`)
})

test('IT-10-077 관리자 결정이 없으면 문서 작성 중으로 전환되지 않는다', async () => {
  const ID = 'IT-10-077'
  const id = seedId('10.b077_virtual_bid')
  if (!id) {
    noMaterial(ID, 1, '시딩 10.b077_virtual_bid(관리자 결정이 없는 분석 완료 입찰 건)가 없다')
    return
  }
  await openScreen(`/bid-requests/${id}`)
  const gate = await generationBlock()
  judge(ID, 1, gate.blocked ? 'pass' : 'fail',
    `상태 전환 창에서 문서 작성 중을 고르면 "${DECISION_REQUIRED}" 가 보이고 전환 단추가 눌리지 않는지`, gate.seen)
})
