import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, RadioGroup, RadioGroupItem } from '@therecommerce-develop/ds-ui'
import { getMemberDocumentVault } from '../../api/client'
import { Dialog, DialogBody, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '../ui/dialog'
import { LoadingSpinner } from '../ui/loading-spinner'
import { PRE_CHECK_DETAIL_TEXT, PRE_CHECK_TEXT, VAULT_PATH } from '../../data/preCheckContent'
import type { PreCheckClientItem, PreCheckVaultDocument, PreCheckVaultResponse } from '../../types/preCheck'
import { dotDate } from '../../utils/eligibilityFormat'
import { daysLeftKst } from '../../utils/kstDate'

const T = PRE_CHECK_DETAIL_TEXT.picker

type Load = { status: 'loading' } | { status: 'error' } | { status: 'ok'; docs: PreCheckVaultDocument[] }

const nameOf = (d: PreCheckVaultDocument) => (d.displayName && d.displayName.trim() ? d.displayName : d.fileName)

/**
 * 서류함에서 고르기 창 (CR-141, T3-4 「입찰 건별 제출 전 검사」, 화면 정본 vault).
 * 열 때마다 서류함을 새로 읽는다. 고른 서류는 입찰 건 서류로 복사되고 형식과 쪽수 판정은 서버가 낸다.
 */
export function PreCheckVaultPicker({ item, onClose, onPick }: {
  item: PreCheckClientItem | null
  onClose: () => void
  onPick: (item: PreCheckClientItem, memberDocumentId: string) => void
}) {
  // 어느 항목을 위해 읽은 서류함인지 함께 둔다. 다른 항목으로 열면 읽는 동안 불러오는 중으로 보인다
  const [state, setState] = useState<{ target: PreCheckClientItem | null; load: Load }>({ target: null, load: { status: 'loading' } })
  const [picked, setPicked] = useState('')

  const fetchDocs = useCallback((target: PreCheckClientItem) => {
    getMemberDocumentVault()
      .then(({ data }) => {
        const vault = data as PreCheckVaultResponse
        const docs = [...vault.fixedSlots.flatMap((s) => s.files.flatMap((f) => (f.document ? [f.document] : []))), ...vault.others]
        setPicked('')
        setState({ target, load: { status: 'ok', docs } })
      })
      .catch(() => setState({ target, load: { status: 'error' } }))
  }, [])

  useEffect(() => {
    if (item) fetchDocs(item)
  }, [item, fetchDocs])

  const load: Load = item && state.target === item ? state.load : { status: 'loading' }
  const retry = () => {
    if (!item) return
    setState({ target: null, load: { status: 'loading' } })
    fetchDocs(item)
  }
  const submit = () => {
    if (!item || !picked) return
    onPick(item, picked)
    onClose()
  }

  return (
    <Dialog open={!!item} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="pc-dlg">
        <DialogHeader>
          <DialogTitle>{T.title}</DialogTitle>
          {item && <DialogDescription className="pc-dsub">{T.desc(item.title)}</DialogDescription>}
        </DialogHeader>
        <DialogBody>
          {load.status === 'loading' ? (
            <div className="pc-dempty"><LoadingSpinner /></div>
          ) : load.status === 'error' ? (
            <div className="pc-dempty">
              {PRE_CHECK_TEXT.loadError}
              <div style={{ marginTop: 12 }}><Button variant="outline" onClick={retry}>{PRE_CHECK_TEXT.retry}</Button></div>
            </div>
          ) : load.docs.length === 0 ? (
            <div className="pc-dempty">{T.empty}<br /><Link to={VAULT_PATH}>{T.toVault}</Link></div>
          ) : (
            <RadioGroup value={picked} onValueChange={setPicked} aria-label={T.title}>
              {load.docs.map((d) => {
                const expired = !!d.expiresOn && daysLeftKst(d.expiresOn) < 0
                return (
                  <label key={d.id} className={`pc-dr${picked === d.id ? ' pc-on' : ''}`}>
                    <RadioGroupItem value={d.id} />
                    <div>
                      <b>{nameOf(d)}</b>
                      <small>{d.categoryLabel}</small>
                    </div>
                    <em className={expired ? 'r' : undefined}>
                      {!d.expiresOn ? T.noExpiry : expired ? T.expired(dotDate(d.expiresOn)) : T.expiresOn(dotDate(d.expiresOn))}
                    </em>
                  </label>
                )
              })}
              <p className="pc-dnote">{T.note}</p>
            </RadioGroup>
          )}
        </DialogBody>
        <DialogFooter>
          <Button variant="outline" onClick={onClose}>{T.cancel}</Button>
          <Button variant="primary" disabled={!picked} onClick={submit}>{T.ok}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}
