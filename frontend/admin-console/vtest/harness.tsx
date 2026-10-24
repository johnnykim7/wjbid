import { act, cleanup, render, screen, waitFor, within } from '@testing-library/react'
import userEvent from '@testing-library/user-event'
import { Toaster } from '@therecommerce-develop/ds-ui'
import { traffic } from './setup'

/**
 * 가상 화면 시험 틀.
 * 사람이 브라우저에서 하는 것(주소 입력, 로그인, 클릭, 화면 읽기)을 Node 안에서 한다.
 * 화면 코드는 고치지 않는다. src 를 그대로 불러 쓴다.
 */

export const API = import.meta.env.VITE_API_BASE_URL as string
const BASE = import.meta.env.BASE_URL.replace(/\/$/, '')

/** 로그인 화면을 거치지 않고 관리자 세션만 만든다. 화면이 쓰는 저장 자리에 토큰을 둔다 */
export async function loginAs(email: string) {
  const password = process.env.TESTBOOK_QA_PASSWORD
  if (!password) throw new Error('TESTBOOK_QA_PASSWORD 가 없다. 시험 계정 비밀번호는 환경변수로만 준다')
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
  if (!res.ok || !token) throw new Error(`로그인 실패 ${email} ${res.status} ${text.slice(0, 200)}`)
  localStorage.setItem('adminToken', token)
  return token
}

/** 화면 밖에서 서버를 부른다. 재료를 고를 때만 쓰고 판정은 화면에서 읽은 것으로 한다 */
export async function apiGet<T>(path: string): Promise<T | null> {
  const res = await fetch(`${API}${path}`, {
    headers: { Authorization: `Bearer ${localStorage.getItem('adminToken') ?? ''}` },
  })
  if (!res.ok) return null
  return (await res.json()) as T
}

/** 주소창에 주소를 넣고 화면을 여는 것에 해당한다. path 는 /admin 뒤의 경로다 */
export async function openScreen(path: string) {
  // 앞서 연 화면이 남아 있으면 글자가 섞여 읽힌다. 주소창에 새로 넣는 것처럼 비우고 연다
  cleanup()
  window.history.replaceState({}, '', `${BASE}${path}`)
  const { default: App } = await import('../src/App')
  const view = render(
    <>
      <App />
      <Toaster />
    </>,
  )
  await settled()
  return view
}

/**
 * 화면이 다 그려질 때까지 기다린다.
 * 로딩 표시만 보면 늘 시한까지 기다리게 된다. 실제로 서버 응답을 기다리는 중인지로 보고, 조회 결과로 다음 조회가
 * 이어지는 화면이 있어 한동안 잠잠해야 끝으로 본다
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

/** 글자가 그대로인 단추. 같은 글자가 여럿이면 마지막 것(나중에 열린 창의 것)을 준다 */
export function buttonNamed(name: string): HTMLButtonElement | undefined {
  const all = screen.queryAllByRole('button', { name })
  return all[all.length - 1] as HTMLButtonElement | undefined
}

export async function click(el: Element) {
  await user.click(el)
  await settled()
}

/** 제목 글자에서 위로 올라가 표를 품은 영역을 찾는다 */
export function sectionOf(title: string): HTMLElement | null {
  let el: HTMLElement | null = screen.queryByText(title)
  while (el && !el.querySelector('table')) el = el.parentElement
  return el
}

/** 요소의 글자. 줄바꿈과 겹친 공백을 하나로 접는다 */
export const text = (el: Element | null | undefined) => (el?.textContent ?? '').replace(/\s+/g, ' ').trim()

/** [상태 전환] 창에서 다음 상태를 골라 전환한다. 막히면 그 사유를 돌려준다 */
export async function transitionTo(label: string, opener?: Element): Promise<string> {
  const open = opener ?? buttonNamed('상태 전환')
  if (!open) return '상태 전환 단추가 없다'
  await click(open)
  const radio = screen.queryAllByRole('radio').find((r) => text(r.closest('label')).startsWith(label))
  if (!radio) return `다음 상태에 ${label} 이 없다`
  await click(radio)
  const go = buttonNamed('전환')
  if (!go) return '전환 단추가 없다'
  if (go.disabled) return '전환 단추가 눌리지 않는다'
  await click(go)
  return ''
}

export const user = userEvent.setup({ delay: null })

export { screen, within, waitFor }
