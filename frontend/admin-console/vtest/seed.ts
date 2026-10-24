import { existsSync, readFileSync } from 'node:fs'
import { dirname, resolve } from 'node:path'
import { fileURLToPath } from 'node:url'

/**
 * 시딩 데이터의 번호를 키 이름으로 찾는다.
 * 번호는 대상 서버마다 다르고 시험 코드에 쓰지 않는다. 키와 번호는 tools/testbook/seed.py 가
 * seed/ids/{대상}.json 에 쓴다. 대상은 VTEST_SEED_TARGET 이고 기본은 로컬이다.
 */

type Ids = Record<string, string | string[]>

let cache: Ids | undefined

function load(): Ids {
  if (!cache) {
    const target = process.env.VTEST_SEED_TARGET ?? '로컬'
    // jsdom 의 URL 은 node:url 이 받지 않아 문자열로 넘긴다
    const here = dirname(fileURLToPath(String(import.meta.url)))
    const path = resolve(here, `../../../tools/testbook/seed/ids/${target}.json`)
    // 파일이 없으면 키가 전부 없는 것으로 본다. 시험은 재료 없음으로 남는다
    cache = existsSync(path) ? (JSON.parse(readFileSync(path, 'utf-8')) as Ids) : {}
  }
  return cache
}

/** @key 로 적은 키의 번호. 시딩이 비었으면 undefined */
export function seedId(key: string): string | undefined {
  const v = load()[key]
  return typeof v === 'string' ? v : undefined
}
