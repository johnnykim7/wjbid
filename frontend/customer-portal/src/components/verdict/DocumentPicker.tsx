import { useCallback, useEffect, useState } from 'react'
import { Link } from 'react-router-dom'
import { Button, RadioGroup, RadioGroupItem } from '@therecommerce-develop/ds-ui'
import { getVaultDocuments } from '../../api/client'
import { Dialog, DialogBody, DialogContent, DialogDescription, DialogFooter, DialogHeader, DialogTitle } from '../ui/dialog'
import { Icon } from '../layout/Icon'
import { LoadingSpinner } from '../ui/loading-spinner'
import { EVIDENCE_TEXT } from '../../data/verdictContent'
import type { EligibilityItem, VaultDocumentRow } from '../../types'
import { dotDate } from '../../utils/eligibilityFormat'
import { daysLeftKst } from '../../utils/kstDate'

const T = EVIDENCE_TEXT.picker
const VAULT_PATH = '/documents'

type Load = { status: 'loading' } | { status: 'error' } | { status: 'ok'; docs: VaultDocumentRow[] }

const nameOf = (d: VaultDocumentRow) => (d.displayName && d.displayName.trim() ? d.displayName : d.fileName)

/**
 * 서류 고르기 창 (CR-140, T3-4 「공고별 판정」 시나리오 3-3, 화면 정본 picker).
 * 열 때마다 서류함을 새로 읽고, 자격과 맞은 분류의 서류를 앞에 둔다. 만료된 서류도 고를 수 있고 판정은 서버가 낸다.
 */
export function DocumentPicker({ item, onClose, onAttach }: {
  item: EligibilityItem | null
  onClose: () => void
  onAttach: (item: EligibilityItem, documentId: string) => void
}) {
  // 어느 자격을 위해 읽은 서류함인지 함께 둔다. 다른 자격으로 열면 읽는 동안 불러오는 중으로 보인다
  const [state, setState] = useState<{ target: EligibilityItem | null; load: Load }>({ target: null, load: { status: 'loading' } })
  const [picked, setPicked] = useState('')

  const fetchDocs = useCallback((target: EligibilityItem) => {
    getVaultDocuments()
      .then(({ data }) => {
        const docs = [...data.fixedSlots.flatMap((s) => s.files.flatMap((f) => (f.document ? [f.document] : []))), ...data.others]
        // 붙인 서류가 있으면 그것을, 없으면 맞은 분류의 첫 서류를 골라 둔다
        const current = target.attachedDocument?.id
        const first = docs.find((d) => d.category === target.vaultCategory)
        setPicked(current && docs.some((d) => d.id === current) ? current : first?.id ?? '')
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
  const docs = load.status === 'ok' ? load.docs : []
  const matched = item?.vaultCategory ? docs.filter((d) => d.category === item.vaultCategory) : []
  const others = docs.filter((d) => !matched.includes(d))

  const unchanged = !picked || picked === item?.attachedDocument?.id
  const submit = () => {
    if (!item || unchanged) return
    onAttach(item, picked)
    onClose()
  }

  return (
    <Dialog open={!!item} onOpenChange={(o) => !o && onClose()}>
      <DialogContent className="vd-dlg">
        <DialogHeader>
          <DialogTitle>{T.title}</DialogTitle>
          {item && <DialogDescription className="vd-dsub">{T.desc(item.title, item.evidenceBy)}</DialogDescription>}
        </DialogHeader>
        <DialogBody>
          {load.status === 'loading' ? (
            <div className="vd-dempty"><LoadingSpinner /></div>
          ) : load.status === 'error' ? (
            <div className="vd-dempty">
              {T.loadError}
              <div style={{ marginTop: 12 }}><Button variant="outline" onClick={retry}>{T.retry}</Button></div>
            </div>
          ) : docs.length === 0 ? (
            <div className="vd-dempty">{T.empty}<br /><Link to={VAULT_PATH}>{T.toVault}</Link></div>
          ) : (
            <RadioGroup value={picked} onValueChange={setPicked} aria-label={T.title}>
              {matched.length > 0 && <DocGroup title={T.matched} docs={matched} picked={picked} />}
              {others.length > 0 && <DocGroup title={T.others} docs={others} picked={picked} />}
              <p className="vd-dnote">{T.note}</p>
            </RadioGroup>
          )}
        </DialogBody>
        <DialogFooter>
          <Link className="vd-dfl" to={VAULT_PATH}><Icon name="folder" />{T.upload}</Link>
          <Button variant="outline" onClick={onClose}>{T.cancel}</Button>
          <Button variant="primary" disabled={unchanged} onClick={submit}>{T.ok}</Button>
        </DialogFooter>
      </DialogContent>
    </Dialog>
  )
}

function DocGroup({ title, docs, picked }: { title: string; docs: VaultDocumentRow[]; picked: string }) {
  return (
    <div className="vd-dg">
      <h4>{title}</h4>
      {docs.map((d) => {
        const expired = !!d.expiresOn && daysLeftKst(d.expiresOn) < 0
        return (
          <label key={d.id} className={`vd-dr${picked === d.id ? ' vd-on' : ''}`}>
            <RadioGroupItem value={d.id} />
            <div>
              <b>{nameOf(d)}</b>
              <small>{T.uploaded(d.categoryLabel, dotDate(d.createdAt))}</small>
            </div>
            <em className={expired ? 'vd-r' : undefined}>
              {!d.expiresOn ? T.noExpiry : expired ? T.expired(dotDate(d.expiresOn)) : T.expiresOn(dotDate(d.expiresOn))}
            </em>
          </label>
        )
      })}
    </div>
  )
}
