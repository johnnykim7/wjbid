import axios from 'axios'
import { afterEach, beforeAll } from 'vitest'
import { cleanup } from '@testing-library/react'

/**
 * 가상 화면 시험 준비.
 * 브라우저 없이 화면을 열기 때문에 jsdom 이 갖고 있지 않은 것들을 여기서 채운다.
 */

// jsdom 의 XMLHttpRequest 는 다른 주소의 서버를 부를 때 브라우저처럼 출처 검사를 해 막힌다.
// 화면 코드의 axios 가 Node 의 fetch 로 부르게 바꾼다. src/api/client.ts 가 만들어질 때 이 값을 물려받는다
axios.defaults.adapter = 'fetch'

/** 화면이 서버 응답을 기다리는 중인지. 판정 전에 조회가 끝났는지 보는 데 쓴다 */
export const traffic = { pending: 0, calls: [] as string[], errors: [] as string[] }

const nativeFetch = globalThis.fetch

beforeAll(() => {
  globalThis.fetch = async (input: RequestInfo | URL, init?: RequestInit) => {
    const method = init?.method ?? (input instanceof Request ? input.method : 'GET')
    const url = input instanceof Request ? input.url : String(input)
    traffic.pending += 1
    traffic.calls.push(`${method} ${url}`)
    try {
      return await nativeFetch(input, init)
    } finally {
      traffic.pending -= 1
    }
  }
})

afterEach(() => {
  try {
    cleanup()
  } catch {
    // 화면을 걷어낼 때 나는 오류는 판정과 무관하다
  }
})

class NoopObserver {
  observe() {}
  unobserve() {}
  disconnect() {}
  takeRecords() {
    return []
  }
}

globalThis.IntersectionObserver ??= NoopObserver as never
globalThis.ResizeObserver ??= NoopObserver as never

window.matchMedia ??= ((query: string) => ({
  matches: false,
  media: query,
  onchange: null,
  addListener: () => {},
  removeListener: () => {},
  addEventListener: () => {},
  removeEventListener: () => {},
  dispatchEvent: () => false,
})) as never

window.scrollTo = () => {}
HTMLElement.prototype.scrollIntoView = () => {}
