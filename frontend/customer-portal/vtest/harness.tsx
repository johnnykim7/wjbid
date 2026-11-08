import { execSync } from 'node:child_process'
import { act, cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { BrowserRouter } from 'react-router-dom'
import { traffic } from './setup'

/**
 * 고객 화면 가상 시험 틀.
 * 사람이 브라우저에서 하는 것(주소 입력, 클릭, 글자 넣기, 화면 읽기)을 Node 안에서 한다.
 * 화면 코드는 고치지 않는다. src 를 그대로 불러 쓰고, 브라우저 진입점(main.tsx)이 하던 라우터만 여기서 씌운다.
 */

export const API = import.meta.env.VITE_API_BASE_URL as string

/** 로그인 화면을 거치지 않고 고객 세션만 만든다. 화면이 쓰는 저장 자리에 토큰을 둔다 */
export async function loginAs(email: string, password = process.env.TESTBOOK_QA_PASSWORD) {
  if (!password) throw new Error('시험 계정 비밀번호가 없다. 비밀번호는 환경변수로만 준다')
  const res = await fetch(`${API}/auth/login`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify({ email, password }),
  })
  const text = await res.text()
  let token: string | undefined
  try {
    token = (JSON.parse(text) as { accessToken?: string }).accessToken
  } catch {
    token = undefined
  }
  if (!res.ok || !token) throw new Error(`로그인 실패 ${email} ${res.status}`)
  localStorage.setItem('accessToken', token)
  return token
}

/** 화면 밖에서 서버를 부른다. 재료를 만들고 되돌릴 때만 쓰고 판정은 화면에서 읽은 것으로 한다 */
export async function apiPost(path: string, body: unknown): Promise<{ status: number; body: Record<string, unknown> }> {
  const res = await fetch(`${API}${path}`, {
    method: 'POST',
    headers: { 'Content-Type': 'application/json' },
    body: JSON.stringify(body),
  })
  const text = await res.text()
  let parsed: Record<string, unknown> = {}
  try {
    parsed = JSON.parse(text) as Record<string, unknown>
  } catch {
    parsed = {}
  }
  return { status: res.status, body: parsed }
}

/**
 * 대상 서버 DB 를 읽는다. 명령은 서버 판정과 같은 TESTBOOK_DB_CMD 다(SQL 을 표준입력으로 받아 머리행 있는 탭 구분 결과).
 * 명령이 없으면 null 이고 그 기대결과는 재료 없음이다
 */
export function dbRows(sql: string): Record<string, string>[] | null {
  const cmd = process.env.TESTBOOK_DB_CMD
  if (!cmd) return null
  const out = execSync(cmd, { input: sql, timeout: 60000 }).toString('utf-8').replace(/\n$/, '')
  if (!out) return []
  const [head, ...rows] = out.split('\n')
  const cols = head.split('\t')
  return rows.map((row) => Object.fromEntries(row.split('\t').map((v, i) => [cols[i], v])))
}

/** 주소창에 주소를 넣고 화면을 여는 것에 해당한다 */
export async function openScreen(path: string) {
  // 앞서 연 화면이 남아 있으면 글자가 섞여 읽힌다. 주소창에 새로 넣는 것처럼 비우고 연다
  cleanup()
  window.history.replaceState({}, '', path)
  const { default: App } = await import('../src/App')
  const view = render(
    <BrowserRouter>
      <App />
    </BrowserRouter>,
  )
  await settled()
  return view
}

/** 지금 열린 화면의 주소 경로 */
export const here = () => window.location.pathname

/**
 * 화면이 다 그려질 때까지 기다린다.
 * 실제로 서버 응답을 기다리는 중인지로 보고, 조회 결과로 다음 조회가 이어지는 화면이 있어 한동안 잠잠해야 끝으로 본다
 */
export async function settled(timeout = 20000) {
  const until = Date.now() + timeout
  const QUIET_MS = 400
  let quietFrom = 0
  while (Date.now() < until) {
    const before = traffic.calls.length
    try {
      await act(async () => {
        await new Promise((r) => setTimeout(r, 50))
      })
    } catch (e) {
      traffic.errors.push(String(e).slice(0, 300))
    }
    if (traffic.pending !== 0 || traffic.calls.length !== before) quietFrom = 0
    else if (quietFrom === 0) quietFrom = Date.now()
    else if (Date.now() - quietFrom >= QUIET_MS) return
  }
}

/** 화면에 보이는 글자 전체. 판정 근거로 결과 파일에 남긴다 */
export function screenText(root: ParentNode = document.body) {
  return (root.textContent ?? '').replace(/\s+/g, ' ').trim()
}

/** 글자가 그대로인 단추. 같은 글자가 여럿이면 마지막 것을 준다 */
export function buttonNamed(name: string | RegExp): HTMLButtonElement | undefined {
  const all = screen.queryAllByRole('button', { name })
  return all[all.length - 1] as HTMLButtonElement | undefined
}

export async function click(el: Element) {
  await user.click(el)
  await settled()
}

/** 칸을 비우고 글자를 넣는다 */
export async function typeInto(el: Element, value: string) {
  await user.clear(el)
  if (value) await user.type(el, value)
}

/** 요소의 글자. 줄바꿈과 겹친 공백을 하나로 접는다 */
export const text = (el: Element | null | undefined) => (el?.textContent ?? '').replace(/\s+/g, ' ').trim()

export const user = userEvent.setup({ delay: null })

export { screen, within, waitFor, traffic }
