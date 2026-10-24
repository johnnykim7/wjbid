import { writeFileSync, mkdirSync } from 'node:fs';
import { afterAll } from 'vitest';

/**
 * 가상 화면 판정 결과를 기대결과 단위로 모은다. 화면 레포의 vtest/report.ts 로 복사해 쓴다.
 * 한 줄의 모양은 collect_virtual.py 가 읽는 약속이라 바꾸지 않는다.
 */

export type Verdict = 'pass' | 'fail' | 'none';

interface Line {
  item: string;   // 시험항목 ID. IT-05-006
  line: number;   // 기대결과 번호. 1부터
  verdict: Verdict;
  asked: string;  // 무엇을 봤는지
  got: string;    // 화면에서 읽은 것
}

const lines: Line[] = [];
let target = 'unknown';

/** 시험 파일마다 결과 파일 이름을 준다. 파일이 각자 다른 실행기에서 돌기 때문이다 */
export function reportTo(name: string) {
  target = name;
}

/** 기대결과 한 줄을 판정한다. asked·got 은 합동시험 콘솔의 근거로 남는다 */
export function judge(item: string, line: number, verdict: Verdict, asked: string, got: string) {
  lines.push({ item, line, verdict, asked, got: got.slice(0, 2000) });
}

/** 판정할 재료가 화면에 없을 때. 미충족으로 적지 않는다 */
export function noMaterial(item: string, line: number, why: string) {
  judge(item, line, 'none', why, '');
}

afterAll(() => {
  if (lines.length === 0) return;
  // -t 로 항목 하나만 돌린 것은 회차 결과를 덮으면 안 된다. collect_virtual.py 는 vtest-out 바로
  // 아래만 읽으므로 하위 폴더로 빼면 수집에서 빠진다. 회귀·스모크는 VTEST_OUT 으로 자기 폴더를 준다
  const dir = process.env.VTEST_OUT ?? (process.env.VTEST_ONE ? 'vtest-out/one' : 'vtest-out');
  mkdirSync(dir, { recursive: true });
  writeFileSync(`${dir}/${target}.json`, JSON.stringify(lines, null, 1), 'utf-8');
});
