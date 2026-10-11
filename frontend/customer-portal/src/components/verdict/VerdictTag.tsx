import { VERDICT_TAG, verdictWhy } from '../../data/verdictContent'
import type { EligibilityBlocker, EligibilityVerdict } from '../../types'

/** 판정 꼬리표 (CR-140). 색은 CR-127 이름표의 성공, 주의, 위험, 회색 */
export function VerdictTag({ verdict, className }: { verdict: EligibilityVerdict; className?: string }) {
  const t = VERDICT_TAG[verdict]
  return (
    <span className={`vd-tag vd-${t.tone}${className ? ` ${className}` : ''}`}>
      <i />{t.label}
    </span>
  )
}

/** 걸린 곳 한 줄. 앞 자격 이름만 굵게 */
export function VerdictWhy({ verdict, blockers, gateCount }: {
  verdict: EligibilityVerdict
  blockers: EligibilityBlocker[]
  gateCount: number
}) {
  const w = verdictWhy(verdict, blockers, gateCount)
  return <>{w.name && <b>{w.name}</b>}{w.text}</>
}
