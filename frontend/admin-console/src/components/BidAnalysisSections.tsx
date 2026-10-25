import type { ReactNode } from 'react'
import {
  Badge,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@therecommerce-develop/ds-ui'
import {
  AWARD_SOURCE_LABEL,
  COMPETITIVENESS_AXES,
  COMPETITIVENESS_LABEL,
  CONFIDENCE_LABEL,
  ELIGIBILITY_LABEL,
  FACTORS,
  FAIRNESS_INDICATORS,
  FAIRNESS_LABEL,
  INDICATOR_VALUE_LABEL,
  LIKELIHOOD_LABEL,
  ORDER_VERDICT_LABEL,
  RISK_AREA_LABEL,
  arr,
  companyLabel,
  obj,
  str,
} from './bidAnalysis'
import type { CompanyScore, Json, RankedIssue, ThemeGap } from './bidAnalysis'

const CONFIDENCE_VARIANT: Record<string, 'success' | 'warning' | 'secondary'> = {
  CONFIRMED: 'success',
  ESTIMATED: 'warning',
  UNKNOWN: 'secondary',
}

function label(map: Record<string, string>, v: unknown): string {
  const s = str(v)
  return map[s] ?? s
}

function Section({ title, children }: { title: string; children: ReactNode }) {
  return (
    <section className="space-y-2">
      <h3 className="text-sm font-semibold text-gray-900">{title}</h3>
      {children}
    </section>
  )
}

function Empty() {
  return <p className="text-sm text-gray-400">없음</p>
}

function Lines({ items }: { items: unknown[] }) {
  const lines = items.map(str).filter(Boolean)
  if (lines.length === 0) return <Empty />
  return (
    <ul className="list-disc pl-5 space-y-1 text-sm text-gray-700">
      {lines.map((l, i) => <li key={i}>{l}</li>)}
    </ul>
  )
}

/** 근거 값 {value, confidence, source}. 값만 온 칸은 꼬리표 없이 보인다 */
export function Evidence({ value }: { value: unknown }) {
  const e = obj(value)
  if (!('confidence' in e) && !('value' in e)) {
    const s = str(value)
    return <span className="text-gray-700">{s || '-'}</span>
  }
  const confidence = str(e.confidence)
  const shown = str(e.value)
  return (
    <span className="inline-flex flex-wrap items-baseline gap-x-2 gap-y-0.5">
      <span className="text-gray-800">{shown || '-'}</span>
      {confidence && (
        <Badge variant={CONFIDENCE_VARIANT[confidence] ?? 'secondary'} size="sm">
          {CONFIDENCE_LABEL[confidence] ?? confidence}
        </Badge>
      )}
      {str(e.source) && <span className="text-xs text-gray-500">출처 {str(e.source)}</span>}
    </span>
  )
}

function Fields({ rows }: { rows: [string, unknown][] }) {
  return (
    <dl className="grid grid-cols-[9rem_1fr] gap-x-4 gap-y-1.5 text-sm">
      {rows.map(([k, v]) => (
        <div key={k} className="contents">
          <dt className="text-gray-500">{k}</dt>
          <dd><Evidence value={v} /></dd>
        </div>
      ))}
    </dl>
  )
}

function Caption({ text }: { text?: string }) {
  return text ? <p className="text-xs font-medium text-gray-500">{text}</p> : null
}

function SimpleTable({ title, head, rows }: { title?: string; head: string[]; rows: ReactNode[][] }) {
  if (rows.length === 0) {
    return title
      ? <p className="text-sm text-gray-400"><span className="text-xs font-medium text-gray-500 mr-2">{title}</span>없음</p>
      : <Empty />
  }
  return (
    <div className="space-y-1">
    <Caption text={title} />
    <Table className="text-sm">
      <TableHeader>
        <TableRow>
          {head.map((h) => <TableHead key={h}>{h}</TableHead>)}
        </TableRow>
      </TableHeader>
      <TableBody>
        {rows.map((cells, i) => (
          <TableRow key={i} className="align-top">
            {cells.map((c, j) => <TableCell key={j}>{c}</TableCell>)}
          </TableRow>
        ))}
      </TableBody>
    </Table>
    </div>
  )
}

function joined(v: unknown): string {
  return arr(v).map(str).filter(Boolean).join(', ') || '-'
}

// ─── 종합 분석 리포트 ────────────────────────────────────────────────

export function ReportView({ report }: { report: Json }) {
  const overview = obj(report.overview)
  const submission = obj(overview.submission)
  const agency = obj(report.agency)
  const decisionInfo = obj(agency.decisionInfo)
  const history = obj(report.orderHistory)
  const requirements = obj(report.requirements)
  const competitors = obj(report.competitors)
  const bcc = obj(competitors.bcc)
  const bccThemes = arr(bcc.themes).map(str)
  const bccScores = obj(bcc.scores)
  const fairness = obj(report.fairness)
  const indicators = new Map(arr(fairness.indicators).map((raw) => {
    const ind = obj(raw)
    return [Number(ind.no), ind] as const
  }))

  return (
    <div className="space-y-6">
      <Section title="과업 개요">
        <Fields rows={[
          ['사업명', overview.projectName],
          ['발주처', overview.agency],
          ['예산', overview.budget],
          ['기간', overview.period],
          ['계약 방식', overview.contractType],
        ]} />
        {str(overview.background) && <p className="text-sm text-gray-700 whitespace-pre-line">{str(overview.background)}</p>}
        <SimpleTable
          title="과업 분해"
          head={['과업', '세부 과업', '산출물']}
          rows={arr(overview.wbs).map((raw) => {
            const w = obj(raw)
            return [str(w.task), joined(w.subtasks), joined(w.deliverables)]
          })}
        />
        <Caption text="제출 지시" />
        <Fields rows={[
          ['분량', submission.pageLimit],
          ['서식', submission.format],
          ['제출 형식', submission.method],
          ['마감', submission.deadline],
          ['발표', submission.presentation],
        ]} />
        <SimpleTable
          title="일정"
          head={['단계', '날짜', '출처']}
          rows={arr(overview.schedule).map((raw) => {
            const s = obj(raw)
            return [str(s.step), str(s.date), str(s.source)]
          })}
        />
      </Section>

      <Section title="발주처">
        <Fields rows={[
          ['기관 개요', agency.overview],
          ['동향', agency.trends],
          ['사업 동기', agency.motive],
          ['담당 부서', decisionInfo.office],
          ['평가 방식', decisionInfo.evaluationMethod],
        ]} />
      </Section>

      <Section title="발주 이력">
        <p className="text-sm text-gray-700">판정 <b>{label(ORDER_VERDICT_LABEL, history.verdict) || '-'}</b></p>
        <SimpleTable
          head={['기간', '제목', '낙찰사', '금액(USD)', '계약 구분', '계약번호', '공고번호', '출처', '비고']}
          rows={arr(history.rows).map((raw) => {
            const r = obj(raw)
            return [str(r.period), str(r.title), str(r.awardee), str(r.amountUsd), str(r.contractType),
              str(r.piid), str(r.solicitationNumber), label(AWARD_SOURCE_LABEL, r.source), str(r.note)]
          })}
        />
        <Fields rows={[
          ['기존 수행사', history.incumbent],
          ['기존 수행사 이점', history.incumbentAdvantage],
        ]} />
      </Section>

      <Section title="요구 테마와 평가 항목">
        <SimpleTable
          title="요구 테마"
          head={['테마', '요구 항목']}
          rows={arr(requirements.themes).map((raw) => {
            const t = obj(raw)
            return [str(t.theme), joined(t.reqIds)]
          })}
        />
        <SimpleTable
          title="평가 항목"
          head={['평가 항목', '비중', '평가 방식', '요구 항목']}
          rows={arr(requirements.evaluation).map((raw) => {
            const e = obj(raw)
            return [str(e.factor), str(e.importance), str(e.method), joined(e.reqIds)]
          })}
        />
      </Section>

      <Section title="질의 후보">
        <SimpleTable
          head={['요구 항목', '질의', '이유']}
          rows={arr(requirements.questionCandidates).map((raw) => {
            const q = obj(raw)
            return [str(q.reqId), str(q.question), str(q.reason)]
          })}
        />
      </Section>

      <Section title="자격">
        <SimpleTable
          head={['자격 항목', '공고 요구', '상태', '근거', '해결 방안']}
          rows={arr(report.eligibility).map((raw) => {
            const e = obj(raw)
            const status = str(e.status)
            return [
              str(e.item),
              str(e.requirement),
              <span className={status === 'NOT_MET' ? 'text-red-700 font-medium' : status === 'CONDITIONAL' ? 'text-amber-700' : ''}>
                {label(ELIGIBILITY_LABEL, status)}
              </span>,
              <Evidence value={e.evidence} />,
              str(e.remedy),
            ]
          })}
        />
      </Section>

      <Section title="경쟁사">
        <SimpleTable
          head={['업체', 'UEI', '기존 수행사', '관련 실적', '발주처와의 관계', '예상 전략', '참여 가능성', '근거']}
          rows={arr(competitors.companies).map((raw) => {
            const c = obj(raw)
            const awards = arr(c.relevantAwards).map((a) => {
              const w = obj(a)
              return [str(w.piid), str(w.amountUsd) && `${str(w.amountUsd)} USD`, str(w.period)].filter(Boolean).join(' ')
            })
            return [str(c.name), str(c.uei), c.isIncumbent === true ? '예' : '아니오', awards.join(', ') || '-',
              str(c.relationship), str(c.expectedStrategy), label(LIKELIHOOD_LABEL, c.likelihood), str(c.basis)]
          })}
        />
        {bccThemes.length > 0 && (
          <SimpleTable
            title="BCC 테마별 점수(1~5)"
            head={['업체', ...bccThemes]}
            rows={Object.keys(bccScores).map((company) => {
              const scores = obj(bccScores[company])
              return [companyLabel(company), ...bccThemes.map((t) => str(scores[t]) || '-')]
            })}
          />
        )}
      </Section>

      <Section title="진입 공정성">
        <p className="text-sm text-gray-700">판단 <b>{label(FAIRNESS_LABEL, fairness.judgement) || '-'}</b></p>
        <SimpleTable
          head={['순번', '징후', '해당', '근거']}
          rows={Object.entries(FAIRNESS_INDICATORS).map(([no, name]) => {
            const ind = indicators.get(Number(no)) ?? {}
            return [no, name, label(INDICATOR_VALUE_LABEL, ind.value) || '-', str(ind.basis)]
          })}
        />
      </Section>

      <Section title="수면 아래 이슈">
        <SimpleTable
          head={['이슈', '근거']}
          rows={arr(report.hiddenIssues).map((raw) => {
            const h = obj(raw)
            return [str(h.issue), str(h.basis)]
          })}
        />
      </Section>

      <Section title="핵심 시사점">
        <Lines items={arr(report.insights)} />
      </Section>
    </div>
  )
}

// ─── 캡처플랜 ────────────────────────────────────────────────────────

function Competitiveness({ value }: { value: unknown }) {
  const c = obj(value)
  return (
    <SimpleTable
      head={['구분', '수준', '근거']}
      rows={COMPETITIVENESS_AXES.map(({ key, label: axis }) => {
        const a = obj(c[key])
        return [axis, label(COMPETITIVENESS_LABEL, a.level) || '-', str(a.basis)]
      })}
    />
  )
}

function Solutions({ value, title = '해결 방안' }: { value: unknown; title?: string }) {
  return (
    <SimpleTable
      title={title}
      head={['이슈', '해결 방안', '근거', '차별점']}
      rows={arr(value).map((raw) => {
        const s = obj(raw)
        return [str(s.issue), str(s.solution), str(s.basis), str(s.differentiation)]
      })}
    />
  )
}

export function CapturePlanView({ plan, issueRanking }: { plan: Json; issueRanking: RankedIssue[] | null }) {
  const business = obj(plan.business)
  const basic = obj(business.basic)
  const team = obj(business.team)
  const agencyInfo = obj(business.agencyInfo)
  const project = obj(business.project)
  const issues = obj(plan.issues)
  const company = obj(plan.company)
  const other = obj(plan.other)

  return (
    <div className="space-y-6">
      <Section title="1. 사업">
        <p className="text-xs font-medium text-gray-500">1.1 기본 정보</p>
        <Fields rows={[
          ['발주처', basic.agency],
          ['사업명', basic.projectName],
          ['사업 규모', basic.scale],
          ['참여 경쟁사', basic.competitors],
          ['일정', basic.schedule],
          ['평가', basic.evaluation],
        ]} />
        <p className="text-xs font-medium text-gray-500">1.2 제안팀</p>
        <Fields rows={[['책임자', team.lead], ['구성원', team.members]]} />
        <p className="text-xs font-medium text-gray-500">1.3 발주처 정보</p>
        <Fields rows={[['담당 부서', agencyInfo.office], ['핵심 관련자', agencyInfo.keyPeople]]} />
        <p className="text-xs font-medium text-gray-500">1.4 사업 정보</p>
        <Fields rows={[
          ['배경', project.background],
          ['목표', project.goal],
          ['범위', project.scope],
          ['공식 요구', project.formalRequirements],
          ['비공식 요구', project.informalRequirements],
        ]} />
      </Section>

      <Section title="2. 이슈">
        <p className="text-xs font-medium text-gray-500">2.1 관련자 이슈</p>
        <SimpleTable
          head={['번호', '무리', '관련자', '영향력', '이슈', '중요도', '근거']}
          rows={arr(issues.stakeholderIssues).map((raw) => {
            const s = obj(raw)
            return [str(s.id), str(s.group), str(s.stakeholder), str(s.influence), str(s.issue), str(s.importance), str(s.basis)]
          })}
        />
        <p className="text-xs font-medium text-gray-500">2.2 이슈 정리 (영향력 x 중요도 합, 높은 순)</p>
        <SimpleTable
          head={['순위', '묶은 이슈', '점수', '관련자 이슈']}
          rows={(issueRanking ?? []).map((r, i) => [
            String(i + 1),
            r.issue,
            String(r.score),
            r.memberIssues.map((m) => `${m.id} ${m.issue} (${m.influence} x ${m.importance} = ${m.score})`).join(', ') || '-',
          ])}
        />
      </Section>

      <Section title="3. 경쟁사">
        {arr(plan.competitors).length === 0 ? <Empty /> : arr(plan.competitors).map((raw, i) => {
          const c = obj(raw)
          return (
            <div key={i} className="space-y-2 border-l-2 border-gray-200 pl-3">
              <p className="text-sm font-medium text-gray-800">{str(c.name)} <span className="text-xs text-gray-500">(자사 대비 경쟁사)</span></p>
              <Competitiveness value={c.competitiveness} />
              <Fields rows={[['컨소시엄', c.consortium], ['경험', c.experience]]} />
              <Solutions value={c.solutions} />
            </div>
          )
        })}
      </Section>

      <Section title="4. 자사">
        <p className="text-xs font-medium text-gray-500">4.1 경쟁력</p>
        <Competitiveness value={company.competitiveness} />
        <Fields rows={[['컨소시엄', company.consortium], ['경험', company.experience]]} />
        <Solutions value={company.solutions} title="4.2 해결 방안" />
      </Section>

      <Section title="5. 기타">
        <Fields rows={[['리스크', other.risks], ['정치 변수', other.politics], ['시기', other.timing]]} />
      </Section>
    </div>
  )
}

// ─── Bid/No-Bid 결정 ─────────────────────────────────────────────────

function Gaps({ title, rows }: { title: string; rows: ThemeGap[] | null }) {
  return (
    <div className="space-y-1">
      <p className="text-xs font-medium text-gray-500">{title}</p>
      {(rows ?? []).length === 0 ? <Empty /> : (
        <ul className="text-sm text-gray-700 space-y-0.5">
          {(rows ?? []).map((g) => (
            <li key={g.theme}>{g.theme}: 자사 {g.our}, 경쟁사 최고 {g.bestCompetitor} ({g.diff > 0 ? `+${g.diff}` : g.diff})</li>
          ))}
        </ul>
      )}
    </div>
  )
}

export function DecisionView({ decision, scores, strengths, weaknesses }: {
  decision: Json
  scores: CompanyScore[] | null
  strengths: ThemeGap[] | null
  weaknesses: ThemeGap[] | null
}) {
  const scorecard = arr(decision.scorecard).map(obj)
  const percentOf = new Map((scores ?? []).map((s) => [s.company, s.percent]))
  const sensitivity = obj(decision.sensitivity)
  const profitability = obj(decision.profitability)
  const priceBasis = obj(profitability.priceBasis)

  return (
    <div className="space-y-6">
      <Section title="스코어카드">
        {scorecard.length === 0 ? <Empty /> : (
          <Table className="text-sm">
            <TableHeader>
              <TableRow>
                <TableHead>팩터</TableHead>
                {scorecard.map((row, i) => <TableHead key={i}>{companyLabel(str(row.company))}</TableHead>)}
              </TableRow>
            </TableHeader>
            <TableBody>
              {FACTORS.map(({ key, label: factor }) => (
                <TableRow key={key} className="align-top">
                  <TableCell className="whitespace-nowrap">{factor}</TableCell>
                  {scorecard.map((row, i) => {
                    const f = obj(obj(row.scores)[key])
                    return (
                      <TableCell key={i}>
                        <b>{str(f.score) || '-'}</b>
                        {str(f.basis) && <p className="text-xs text-gray-500">{str(f.basis)}</p>}
                      </TableCell>
                    )
                  })}
                </TableRow>
              ))}
              <TableRow>
                <TableCell className="font-medium">가중 점수(%)</TableCell>
                {scorecard.map((row, i) => {
                  const percent = percentOf.get(str(row.company))
                  return <TableCell key={i} className="font-medium">{percent ?? '-'}</TableCell>
                })}
              </TableRow>
            </TableBody>
          </Table>
        )}
      </Section>

      <Section title="민감도">
        <p className="text-sm text-gray-700">
          {FACTORS.find((f) => f.key === str(sensitivity.factor))?.label ?? (str(sensitivity.factor) || '-')}
          {str(sensitivity.reason) && <span className="text-gray-500">: {str(sensitivity.reason)}</span>}
        </p>
      </Section>

      <Section title="강점과 약점">
        <div className="grid grid-cols-1 md:grid-cols-2 gap-4">
          <Gaps title="강점(경쟁사 최고점보다 1 이상 높음)" rows={strengths} />
          <Gaps title="약점(경쟁사 최고점보다 1 이상 낮음)" rows={weaknesses} />
        </div>
      </Section>

      <Section title="자격 해결">
        <SimpleTable
          head={['자격 항목', '해결 방안', '해결 가능']}
          rows={arr(decision.qualification).map((raw) => {
            const q = obj(raw)
            return [
              str(q.item),
              str(q.remedy),
              q.resolvable === false ? <span className="text-red-700 font-medium">불가</span> : '가능',
            ]
          })}
        />
      </Section>

      <Section title="수익성">
        <Fields rows={[
          ['가격 기준', priceBasis.rangeUsd],
          ['비교 계약번호', joined(priceBasis.piids)],
          ['원가', profitability.costNote],
        ]} />
        <SimpleTable
          title="가격 시나리오"
          head={['기준 금액 대비', '가격(USD)', '이익률']}
          rows={arr(profitability.scenarios).map((raw) => {
            const s = obj(raw)
            return [`${str(s.percent)}%`, str(s.priceUsd), str(s.margin) || '미상']
          })}
        />
      </Section>

      <Section title="리스크">
        <SimpleTable
          head={['영역', '리스크', '확률', '영향']}
          rows={arr(decision.risks).map((raw) => {
            const r = obj(raw)
            const severe = str(r.probability) === 'H' && str(r.impact) === 'H'
            return [
              label(RISK_AREA_LABEL, r.area),
              severe ? <span className="text-red-700 font-medium">{str(r.risk)} (H x H)</span> : str(r.risk),
              str(r.probability),
              str(r.impact),
            ]
          })}
        />
      </Section>

      <Section title="조건">
        <Lines items={arr(decision.conditions)} />
      </Section>

      <Section title="결정 조언">
        <p className="text-sm text-gray-700 whitespace-pre-line">{str(decision.advice) || '-'}</p>
      </Section>
    </div>
  )
}
