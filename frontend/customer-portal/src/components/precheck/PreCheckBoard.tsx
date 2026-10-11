import { useRef, useState, type ChangeEvent, type ReactNode } from 'react'
import { Link } from 'react-router-dom'
import { isAxiosError } from 'axios'
import { Button } from '@therecommerce-develop/ds-ui'
import { autoFillSlots, slotFromVault, transitionBidRequestState, unmapSlot, uploadToSlot } from '../../api/client'
import { Icon } from '../layout/Icon'
import { FORMAT_LABEL, PRE_CHECK_DETAIL_TEXT, PRE_CHECK_TEXT, extensionLabel, reviewHref } from '../../data/preCheckContent'
import type { PreCheckClientItem, PreCheckDetail, PreCheckDocument, PreCheckSummary, PreCheckVerdict } from '../../types/preCheck'
import { dotDate } from '../../utils/eligibilityFormat'
import { PreCheckVaultPicker } from './PreCheckVaultPicker'
import '../../wjbid-eligibility.css'
import '../../wjbid-precheck.css'

const T = PRE_CHECK_DETAIL_TEXT
const SUBMIT_FROM = 'DOCS_PENDING'
const SUBMIT_TO = 'DOCS_RECEIVED'
/** 남은 서류 이름은 세 개까지 보이고 나머지는 개수로 줄인다 */
const NEED_CHIPS = 3
const BYTES_PER_MB = 1024 * 1024

const CHIP: Record<PreCheckVerdict, string> = {
  MATCH: 'ok',
  MISMATCH: 'warn',
  OVER: 'warn',
  NEEDS_HUMAN: 'hum',
  NO_CONDITION: 'nc',
  NOT_UPLOADED: 'off',
}

const isProblem = (v: PreCheckVerdict) => v === 'MISMATCH' || v === 'OVER'
const isUploaded = (i: PreCheckClientItem) => i.presence === 'UPLOADED'

/** 서버가 준 거절 사유(409 서류 접수 뒤, 400 고객 서류 아님 등)를 그대로 보인다 */
const errorText = (e: unknown, fallback: string = T.actionError) => {
  const message = isAxiosError(e) ? (e.response?.data as { message?: unknown } | undefined)?.message : undefined
  return typeof message === 'string' && message.trim() ? message : fallback
}

/**
 * 입찰 건 하나의 제출 전 검사 (CR-141, T3-3 「입찰 건별 제출 전 검사」, 화면 정본 detail, states, vault, proposal).
 * 판정과 숫자는 서버가 낸 값만 보이고, 잇기, 바꾸기, 빼기 뒤에는 검사 결과를 다시 읽는다.
 * 입찰 건별 화면과 입찰 건 상세의 제출 서류 탭이 같이 쓴다.
 */
export function PreCheckBoard({ detail, reload, onSubmitted }: {
  detail: PreCheckDetail
  reload: () => Promise<void>
  onSubmitted: () => void
}) {
  const { bidRequestId, clientItems, platformItems, summary, editable, maxUploadMb } = detail
  const fileInput = useRef<HTMLInputElement>(null)
  const uploadTarget = useRef<PreCheckClientItem | null>(null)
  const [busy, setBusy] = useState<string | null>(null)
  const [errors, setErrors] = useState<Record<string, string>>({})
  const [picking, setPicking] = useState<PreCheckClientItem | null>(null)
  const [autoFill, setAutoFill] = useState<{ running: boolean; message: string | null }>({ running: false, message: null })

  const setError = (itemId: string, message: string | null) =>
    setErrors((prev) => {
      const next = { ...prev }
      if (message) next[itemId] = message
      else delete next[itemId]
      return next
    })

  const act = async (item: PreCheckClientItem, run: () => Promise<unknown>) => {
    setBusy(item.requirementItemId)
    setError(item.requirementItemId, null)
    try {
      await run()
      await reload()
    } catch (e) {
      setError(item.requirementItemId, errorText(e))
    } finally {
      setBusy(null)
    }
  }

  const chooseFile = (item: PreCheckClientItem) => {
    uploadTarget.current = item
    fileInput.current?.click()
  }
  const onFile = (e: ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    const item = uploadTarget.current
    e.target.value = ''
    if (!file || !item) return
    if (file.size > maxUploadMb * BYTES_PER_MB) {
      setError(item.requirementItemId, T.tooBig(maxUploadMb))
      return
    }
    act(item, () => uploadToSlot(bidRequestId, item.requirementItemId, file))
  }
  const remove = (item: PreCheckClientItem) => act(item, () => unmapSlot(bidRequestId, item.requirementItemId))
  const pickFromVault = (item: PreCheckClientItem, memberDocumentId: string) =>
    act(item, () => slotFromVault(bidRequestId, item.requirementItemId, memberDocumentId))

  const runAutoFill = async () => {
    setAutoFill({ running: true, message: null })
    try {
      const { data } = await autoFillSlots(bidRequestId)
      const count = Number((data as { filledCount?: number } | undefined)?.filledCount ?? 0)
      await reload()
      setAutoFill({ running: false, message: count > 0 ? T.autoFill.done(count) : T.autoFill.none })
    } catch (e) {
      setAutoFill({ running: false, message: errorText(e) })
    }
  }

  if (!detail.analysisReady) {
    return (
      <>
        <section className="cbx pc-sec">
          <div className="empty2" style={{ borderBottom: 0, padding: '44px 20px' }}>
            <div className="icn"><Icon name="file" /></div>
            <b>{T.notReady.title}</b>
            <span>{T.notReady.desc}</span>
            <div style={{ marginTop: 18 }}>
              <Link className="eq-obtn eq-p" to={reviewHref(detail.solicitationNumber)}>{PRE_CHECK_TEXT.review.action}</Link>
            </div>
          </div>
        </section>
        <SubmitBand detail={detail} onSubmitted={onSubmitted} />
      </>
    )
  }

  const hasMissing = clientItems.some((i) => !isUploaded(i))

  return (
    <>
      {summary && <Nums summary={summary} />}

      <section className="cbx pad pc-sec">
        {clientItems.length > 0 ? (
          <>
            <h2 className="h2">{T.listTitle(clientItems.length)}<small>{T.listDesc}</small></h2>
            {editable && hasMissing && (
              <div className="pc-tools">
                <Button variant="outline" size="sm" disabled={autoFill.running || busy !== null} onClick={runAutoFill}>
                  <Icon name="folder" />{T.autoFill.action}
                </Button>
                {autoFill.message && <span>{autoFill.message}</span>}
              </div>
            )}
            <ul className="pc-list">
              {clientItems.map((item, idx) => (
                <ItemRow
                  key={item.requirementItemId}
                  no={idx + 1}
                  item={item}
                  editable={editable}
                  maxUploadMb={maxUploadMb}
                  busy={busy === item.requirementItemId}
                  locked={busy !== null || autoFill.running}
                  error={errors[item.requirementItemId]}
                  onUpload={chooseFile}
                  onPick={setPicking}
                  onRemove={remove}
                />
              ))}
            </ul>
            <div className="eq-notj">
              <Icon name="alert" />
              <div>{T.notJudged}</div>
            </div>
          </>
        ) : (
          <p className="pc-note" style={{ margin: 0 }}>{T.noClientItems}</p>
        )}
      </section>

      <section className="cbx pad pc-sec">
        <h2 className="h2">{T.platform.title}<small>{T.platform.desc}</small></h2>
        {platformItems.length > 0 ? (
          <ul className="pc-plat">
            {platformItems.map((p) => (
              <li key={p.requirementItemId}>
                <Icon name="file" />
                <div>
                  <b>{p.title}</b>
                  {p.sourceRef && <small className="mono"> {p.sourceRef}</small>}
                </div>
                {T.platform.party[p.fulfillmentParty] && <span className="eq-tag">{T.platform.party[p.fulfillmentParty]}</span>}
              </li>
            ))}
          </ul>
        ) : (
          <p className="pc-note">{T.platform.empty}</p>
        )}
      </section>

      <SubmitBand detail={detail} onSubmitted={onSubmitted} />
      <NeedBand detail={detail} />

      <input ref={fileInput} type="file" hidden onChange={onFile} />
      <PreCheckVaultPicker item={picking} onClose={() => setPicking(null)} onPick={pickFromVault} />
    </>
  )
}

function Nums({ summary }: { summary: PreCheckSummary }) {
  const cells: [string, string, number][] = [
    ['k0', T.nums.required, summary.required],
    ['k1', T.nums.uploaded, summary.uploaded],
    ['k2', T.nums.missing, summary.missing],
    ['k3', T.nums.mismatch, summary.mismatch],
    ['k4', T.nums.needsHuman, summary.needsHuman],
  ]
  return (
    <div className="pc-nums">
      {cells.map(([k, label, n]) => (
        <div key={k} className={k}><span><i />{label}</span><b>{n}<small>{T.unit}</small></b></div>
      ))}
    </div>
  )
}

function ItemRow({ no, item, editable, maxUploadMb, busy, locked, error, onUpload, onPick, onRemove }: {
  no: number
  item: PreCheckClientItem
  editable: boolean
  maxUploadMb: number
  busy: boolean
  locked: boolean
  error?: string
  onUpload: (item: PreCheckClientItem) => void
  onPick: (item: PreCheckClientItem) => void
  onRemove: (item: PreCheckClientItem) => void
}) {
  const doc = item.document
  return (
    <li aria-busy={busy}>
      <span className="eq-no">{no}</span>
      <div>
        <div className="pc-tt">
          <b>{item.title}</b>
          <span className={item.mandatory ? 'eq-tag pc-tag-m' : 'eq-tag'}>{item.mandatory ? T.mandatory : T.optional}</span>
        </div>
        <dl className="pc-kv">
          {item.sourceRef && <><dt>{T.sourceRef}</dt><dd className="mono">{item.sourceRef}</dd></>}
          <dt>{T.condition}</dt>
          <dd>{T.formatWord} <Condition text={item.formatText} /> · {T.pagesWord} <Condition text={item.pageLimitText} /></dd>
        </dl>
        {(item.description || item.notes) && (
          <details className="pc-more">
            <summary>{T.more}<Icon name="cr" /></summary>
            {item.description && <p>{item.description}</p>}
            {item.notes && <p>{item.notes}</p>}
          </details>
        )}
        <div className={doc ? 'pc-file' : 'pc-file pc-empty'}>
          <Icon name="file" />
          <div>
            <span className="fn">{doc ? doc.fileName : T.file.empty}</span>
            <span className="fm">{doc ? fileMeta(doc) : T.file.limit(maxUploadMb)}</span>
          </div>
          {editable && (
            <div className="act">
              {doc ? (
                <>
                  <Button variant="outline" size="sm" disabled={locked} onClick={() => onUpload(item)}>{T.actions.replace}</Button>
                  <Button variant="outline" size="sm" className="pc-d" disabled={locked} onClick={() => onRemove(item)}>{T.actions.remove}</Button>
                </>
              ) : (
                <>
                  <Button variant="primary" size="sm" disabled={locked} onClick={() => onUpload(item)}>{T.actions.upload}</Button>
                  <Button variant="outline" size="sm" disabled={locked} onClick={() => onPick(item)}>{T.actions.vault}</Button>
                </>
              )}
            </div>
          )}
        </div>
        {error && <div className="pc-err" role="alert">{error}</div>}
      </div>
      <Verdicts item={item} />
    </li>
  )
}

function Condition({ text }: { text: string | null }) {
  return text && text.trim() ? <q>{text}</q> : <span className="pc-nc">{T.noCondition}</span>
}

function fileMeta(doc: PreCheckDocument) {
  const uploadedOn = dotDate(doc.uploadedAt).slice(5)
  return [
    extensionLabel(doc.extension),
    doc.pageCount != null ? T.file.pages(doc.pageCount) : T.file.noPages,
    uploadedOn && T.file.uploadedOn(uploadedOn),
  ].filter(Boolean).join(' · ')
}

function Verdicts({ item }: { item: PreCheckClientItem }) {
  const uploaded = isUploaded(item)
  const presenceChip = uploaded ? 'ok' : item.mandatory ? 'miss' : 'off'
  return (
    <div className="pc-vd">
      <div>
        <div className="lb">{T.label.presence}<span className={`pc-c ${presenceChip}`}>{T.presence[item.presence]}</span></div>
        {!uploaded && <p>{item.mandatory ? T.why.missingMandatory : T.why.missingOptional}</p>}
      </div>
      {uploaded && (
        <>
          <div>
            <div className="lb">{T.label.format}<span className={`pc-c ${CHIP[item.format.verdict]}`}>{T.verdict[item.format.verdict]}</span></div>
            <p>{formatWhy(item)}</p>
          </div>
          <div>
            <div className="lb">{T.label.pages}<span className={`pc-c ${CHIP[item.pages.verdict]}`}>{T.verdict[item.pages.verdict]}</span></div>
            <p>{pagesWhy(item)}</p>
          </div>
        </>
      )}
    </div>
  )
}

function formatWhy(item: PreCheckClientItem): ReactNode {
  const { verdict, basis, allowed } = item.format
  const allowedText = allowed.map((f) => FORMAT_LABEL[f]).join(', ')
  switch (verdict) {
    case 'MATCH':
      return T.why.formatMatch(allowedText)
    case 'MISMATCH':
      return T.why.formatMismatch(allowedText, extensionLabel(item.document?.extension) || item.document?.fileName || '')
    case 'NEEDS_HUMAN':
      return <>{T.condition} <q>{basis ?? item.formatText}</q> {T.why.formatHuman}</>
    case 'NO_CONDITION':
      return T.why.formatNone
    default:
      return null
  }
}

function pagesWhy(item: PreCheckClientItem): ReactNode {
  const { verdict, basis, limit } = item.pages
  const count = item.document?.pageCount
  switch (verdict) {
    case 'MATCH':
      return count != null && limit != null ? T.why.pagesMatch(count, limit) : null
    case 'OVER':
      return count != null && limit != null ? T.why.pagesOver(count, limit) : null
    case 'NEEDS_HUMAN':
      return limit == null
        ? <>{T.condition} <q>{basis ?? item.pageLimitText}</q> {T.why.pagesNotSingle}</>
        : T.why.pagesNotCounted
    case 'NO_CONDITION':
      return basis ? T.why.pagesNoNumber : T.why.pagesNone
    default:
      return null
  }
}

/** 서류 대기일 때만 보인다. 서류 접수로 전환할 수 있는지는 서버의 canTransition 만 따른다 */
function SubmitBand({ detail, onSubmitted }: { detail: PreCheckDetail; onSubmitted: () => void }) {
  const [submitting, setSubmitting] = useState(false)
  const [failed, setFailed] = useState<string | null>(null)
  if (detail.state !== SUBMIT_FROM) return null

  const missing = detail.clientItems.filter((i) => i.mandatory && !isUploaded(i))
  const attention = detail.summary ? detail.summary.mismatch + detail.summary.needsHuman : 0
  const line = !detail.analysisReady
    ? <span>{T.submit.notReady}</span>
    : !detail.canTransition
      ? <span className="r">{T.submit.blocked(missing.length, missing.map((i) => i.title).join(', '))}</span>
      : attention > 0
        ? <span className="a">{T.submit.warn(attention)}</span>
        : <span>{T.submit.ready}</span>

  const submit = async () => {
    setSubmitting(true)
    setFailed(null)
    try {
      await transitionBidRequestState(detail.bidRequestId, SUBMIT_TO, T.submit.notes)
      onSubmitted()
    } catch (e) {
      setFailed(errorText(e, T.submit.error))
    } finally {
      setSubmitting(false)
    }
  }

  return (
    <div className="pc-sub">
      <div className="xtx">
        <b>{T.submit.title}</b>
        {line}
        {failed && <span className="r" role="alert">{failed}</span>}
      </div>
      <Button className="wj-reset bigb" disabled={!detail.canTransition || submitting} onClick={submit}>{T.submit.action}</Button>
    </div>
  )
}

/** 빠짐, 어긋남, 사람 확인이 남았으면 그 서류 이름을 검토 신청에 채워 둔다. 없으면 일반 검토 신청 띠 */
function NeedBand({ detail }: { detail: PreCheckDetail }) {
  const uploaded = detail.clientItems.filter(isUploaded)
  const human = uploaded.filter((i) => i.format.verdict === 'NEEDS_HUMAN' || i.pages.verdict === 'NEEDS_HUMAN')
  const missing = detail.clientItems.filter((i) => i.mandatory && !isUploaded(i))
  const mismatch = uploaded.filter((i) => isProblem(i.format.verdict) || isProblem(i.pages.verdict))
  const groups = [
    { label: T.nums.needsHuman, items: human },
    { label: T.nums.missing, items: missing },
    { label: T.nums.mismatch, items: mismatch },
  ].filter((g) => g.items.length > 0)

  const titles = [...new Set(groups.flatMap((g) => g.items.map((i) => i.title)))]
  const chips = titles.length > NEED_CHIPS
    ? [...titles.slice(0, NEED_CHIPS - 1), T.need.more(titles[NEED_CHIPS - 1], titles.length - NEED_CHIPS)]
    : titles
  const href = reviewHref(detail.solicitationNumber, groups.map((g) => ({ label: g.label, titles: g.items.map((i) => i.title) })))

  return (
    <div className="apply">
      <div className="icn"><Icon name="headset" /></div>
      <div className="xtx">
        <b>{groups.length > 0 ? T.need.title(groups.map((g) => T.need.part(g.label, g.items.length))) : PRE_CHECK_TEXT.review.title}</b>
        <span>{groups.length > 0 ? T.need.desc : PRE_CHECK_TEXT.review.desc}</span>
        {chips.length > 0 && <div className="eq-need">{chips.map((c) => <em key={c}>{c}</em>)}</div>}
      </div>
      <Link className="bigb" to={href}>{PRE_CHECK_TEXT.review.action}</Link>
    </div>
  )
}
