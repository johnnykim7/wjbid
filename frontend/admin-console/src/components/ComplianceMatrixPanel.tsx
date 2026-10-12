import { Fragment, useCallback, useEffect, useState } from 'react'
import {
  Badge,
  Button,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@therecommerce-develop/ds-ui'
import { getNoticeComplianceMatrix } from '../api/client'

type Level = 'SHALL' | 'SHOULD' | 'MAY'
type Party = 'CLIENT_UPLOAD' | 'PLATFORM_GENERATED' | 'SYSTEM_FORM'

interface MatrixLine {
  reqId: string
  level: Level | null
  text: string | null
  translation: string | null
  sourceFile: string | null
  page: string | number | null
  clause: string | null
  quote: string | null
  verb: string | null
  category: string | null
  fulfillmentParty: Party | null
  factorId: string | null
  factorTitle: string | null
  queryCandidate: boolean
}

interface MatrixQuestion {
  reqId: string
  ambiguity: string | null
  draftQuery: string | null
}

interface ComplianceMatrix {
  lines: MatrixLine[]
  questions: MatrixQuestion[]
}

// ds-ui outline 배지는 관리자 색 변수에서 글자가 흰색이 되어 회색 알약을 직접 칠한다(InquiryPills 와 같은 방식)
const NEUTRAL_PILL = 'bg-gray-100 text-gray-700'

const LEVEL_LABEL: Record<Level, string> = { SHALL: 'Shall', SHOULD: 'Should', MAY: 'May' }

const PARTY_LABEL: Record<Party, string> = {
  CLIENT_UPLOAD: '고객 업로드',
  PLATFORM_GENERATED: '플랫폼 생성',
  SYSTEM_FORM: '시스템 양식',
}

function source(line: MatrixLine): string {
  const parts = [line.sourceFile, line.page != null && line.page !== '' ? `${line.page}쪽` : null, line.clause]
  return parts.filter(Boolean).join(' · ') || '-'
}

/** CR-159 공고문 관리 상세의 컴플라이언스 매트릭스. 읽기만 한다 */
export default function ComplianceMatrixPanel({ noticeId, analyzing, reloadKey }: {
  noticeId: string
  analyzing: boolean
  reloadKey?: string
}) {
  const [matrix, setMatrix] = useState<ComplianceMatrix | null>(null)
  const [failed, setFailed] = useState(false)
  const [loading, setLoading] = useState(true)
  const [openReqId, setOpenReqId] = useState<string | null>(null)

  const load = useCallback(async () => {
    setLoading(true)
    setFailed(false)
    try {
      const { data } = await getNoticeComplianceMatrix(noticeId)
      setMatrix(data)
    } catch {
      setFailed(true)
    } finally {
      setLoading(false)
    }
  }, [noticeId])

  // reloadKey 는 분석이 끝난 시각이라 재생성이 끝나 바뀌면 매트릭스를 다시 부른다
  useEffect(() => { load() }, [load, reloadKey])

  const jumpTo = (reqId: string) => {
    setOpenReqId(reqId)
    document.getElementById(`matrix-${reqId}`)?.scrollIntoView({ behavior: 'smooth', block: 'center' })
  }

  const lines = matrix?.lines ?? []
  const questions = matrix?.questions ?? []
  const countOf = (level: Level) => lines.filter((l) => l.level === level).length

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5 space-y-4">
      <div className="flex flex-wrap items-center gap-2">
        <h3 className="text-sm font-semibold text-gray-700 mr-2">컴플라이언스 매트릭스</h3>
        {lines.length > 0 && (
          <>
            <span className="text-sm text-gray-600">{lines.length}줄</span>
            {(['SHALL', 'SHOULD', 'MAY'] as Level[]).map((level) => (
              <Badge key={level} size="sm" className={NEUTRAL_PILL}>{LEVEL_LABEL[level]} {countOf(level)}</Badge>
            ))}
            <Badge variant="warning" size="sm" className={questions.length > 0 ? '' : NEUTRAL_PILL}>질의 후보 {questions.length}</Badge>
          </>
        )}
      </div>

      {analyzing ? (
        <p className="text-sm text-gray-500">공고를 분석하고 있습니다. 분석이 끝나면 매트릭스가 보입니다.</p>
      ) : loading ? (
        <p className="text-sm text-gray-400">불러오는 중</p>
      ) : failed ? (
        <div className="flex items-center gap-3">
          <p className="text-sm text-red-600">불러오지 못했습니다.</p>
          <Button variant="outline" size="sm" onClick={load}>다시 시도</Button>
        </div>
      ) : lines.length === 0 ? (
        <p className="text-sm text-gray-500">컴플라이언스 매트릭스가 없습니다. 공고 분석이 매트릭스를 낸 뒤에 보입니다.</p>
      ) : (
        <>
          <Table className="text-sm">
            <TableHeader>
              <TableRow>
                <TableHead className="w-24">REQ ID</TableHead>
                <TableHead>요구사항</TableHead>
                <TableHead>번역</TableHead>
                <TableHead className="w-40">출처</TableHead>
                <TableHead className="w-20">구분</TableHead>
                <TableHead className="w-24">요구 동사</TableHead>
                <TableHead className="w-32">평가항목</TableHead>
                <TableHead className="w-28 whitespace-nowrap">충족 주체</TableHead>
              </TableRow>
            </TableHeader>
            <TableBody>
              {lines.map((line) => (
                <Fragment key={line.reqId}>
                  <TableRow
                    id={`matrix-${line.reqId}`}
                    className="cursor-pointer align-top"
                    onClick={() => setOpenReqId(openReqId === line.reqId ? null : line.reqId)}
                  >
                    <TableCell className="font-mono whitespace-nowrap">
                      {line.reqId}
                      {line.queryCandidate && <Badge variant="warning" size="xs" className="ml-1">질의</Badge>}
                    </TableCell>
                    <TableCell className="whitespace-pre-wrap">{line.text || '-'}</TableCell>
                    <TableCell className="whitespace-pre-wrap text-gray-600">{line.translation || '-'}</TableCell>
                    <TableCell className="text-gray-600">{source(line)}</TableCell>
                    <TableCell>{line.level ? LEVEL_LABEL[line.level] : '-'}</TableCell>
                    <TableCell>{line.verb || '-'}</TableCell>
                    <TableCell className="text-gray-600">
                      {line.factorId || line.factorTitle ? [line.factorId, line.factorTitle].filter(Boolean).join('. ') : '-'}
                    </TableCell>
                    <TableCell className="whitespace-nowrap">{line.fulfillmentParty ? PARTY_LABEL[line.fulfillmentParty] : '-'}</TableCell>
                  </TableRow>
                  {openReqId === line.reqId && (
                    <TableRow className="bg-gray-50">
                      <TableCell />
                      <TableCell colSpan={7} className="text-xs text-gray-700 space-y-1">
                        <div><span className="text-gray-500">출처 </span>{source(line)}</div>
                        <div className="whitespace-pre-wrap"><span className="text-gray-500">원문 인용 </span>{line.quote || '-'}</div>
                      </TableCell>
                    </TableRow>
                  )}
                </Fragment>
              ))}
            </TableBody>
          </Table>

          {questions.length > 0 && (
            <div className="space-y-2">
              <h4 className="text-sm font-semibold text-gray-700">질의 후보</h4>
              <Table className="text-sm">
                <TableHeader>
                  <TableRow>
                    <TableHead className="w-24">REQ ID</TableHead>
                    <TableHead>모호한 점</TableHead>
                    <TableHead>질의(안)</TableHead>
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {questions.map((q) => (
                    <TableRow key={q.reqId} className="align-top">
                      <TableCell>
                        <Button variant="dim" size="sm" className="font-mono px-0" onClick={() => jumpTo(q.reqId)}>
                          {q.reqId}
                        </Button>
                      </TableCell>
                      <TableCell className="whitespace-pre-wrap">{q.ambiguity || '-'}</TableCell>
                      <TableCell className="whitespace-pre-wrap">{q.draftQuery || '-'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </div>
          )}
        </>
      )}
    </div>
  )
}
