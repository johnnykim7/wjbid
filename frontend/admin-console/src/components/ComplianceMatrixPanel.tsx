import { Fragment, useCallback, useEffect, useState } from 'react'
import {
  Badge,
  Button,
  Card,
  CardContent,
  CardHeader,
  CardTable,
  CardTitle,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
} from '@therecommerce-develop/ds-ui'
import { getNoticeComplianceMatrix } from '../api/client'
import { ToneBadge } from './StateBadge'
import { Loading } from './LoadState'

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
  const showTable = !analyzing && !loading && !failed && lines.length > 0

  return (
    <Card>
      <CardHeader>
        <div className="flex flex-wrap items-center gap-2">
          <CardTitle className="mr-2">컴플라이언스 매트릭스</CardTitle>
          {lines.length > 0 && (
            <>
              <span className="text-sm text-muted-foreground">{lines.length}줄</span>
              {(['SHALL', 'SHOULD', 'MAY'] as Level[]).map((level) => (
                <ToneBadge key={level} tone="gray">{LEVEL_LABEL[level]} {countOf(level)}</ToneBadge>
              ))}
              <ToneBadge tone={questions.length > 0 ? 'warning' : 'gray'}>질의 후보 {questions.length}</ToneBadge>
            </>
          )}
        </div>
      </CardHeader>

      {!showTable && (
        <CardContent>
          {analyzing ? (
            <p className="text-sm text-muted-foreground">공고를 분석하고 있습니다. 분석이 끝나면 매트릭스가 보입니다.</p>
          ) : loading ? (
            <Loading className="h-24" />
          ) : failed ? (
            <div className="flex items-center gap-3">
              <p className="text-sm text-destructive">불러오지 못했습니다.</p>
              <Button variant="outline" size="sm" onClick={load}>다시 시도</Button>
            </div>
          ) : (
            <p className="text-sm text-muted-foreground">컴플라이언스 매트릭스가 없습니다. 공고 분석이 매트릭스를 낸 뒤에 보입니다.</p>
          )}
        </CardContent>
      )}

      {showTable && (
        <>
          <CardTable>
            <Table className="text-sm">
              <TableHeader className="bg-muted">
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
                        {line.queryCandidate && (
                          <Badge variant="warning" appearance="light" size="xs" className="ml-1 whitespace-nowrap">질의</Badge>
                        )}
                      </TableCell>
                      <TableCell className="whitespace-pre-wrap">{line.text || '-'}</TableCell>
                      <TableCell className="whitespace-pre-wrap text-muted-foreground">{line.translation || '-'}</TableCell>
                      <TableCell className="text-muted-foreground">{source(line)}</TableCell>
                      <TableCell>{line.level ? LEVEL_LABEL[line.level] : '-'}</TableCell>
                      <TableCell>{line.verb || '-'}</TableCell>
                      <TableCell className="text-muted-foreground">
                        {line.factorId || line.factorTitle ? [line.factorId, line.factorTitle].filter(Boolean).join('. ') : '-'}
                      </TableCell>
                      <TableCell className="whitespace-nowrap">{line.fulfillmentParty ? PARTY_LABEL[line.fulfillmentParty] : '-'}</TableCell>
                    </TableRow>
                    {openReqId === line.reqId && (
                      <TableRow className="bg-muted">
                        <TableCell />
                        <TableCell colSpan={7} className="text-xs text-foreground space-y-1">
                          <div><span className="text-muted-foreground">출처 </span>{source(line)}</div>
                          <div className="whitespace-pre-wrap"><span className="text-muted-foreground">원문 인용 </span>{line.quote || '-'}</div>
                        </TableCell>
                      </TableRow>
                    )}
                  </Fragment>
                ))}
              </TableBody>
            </Table>
          </CardTable>

          {questions.length > 0 && (
            <>
              <CardHeader className="border-t">
                <CardTitle>질의 후보</CardTitle>
              </CardHeader>
              <CardTable>
                <Table className="text-sm">
                  <TableHeader className="bg-muted">
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
              </CardTable>
            </>
          )}
        </>
      )}
    </Card>
  )
}
