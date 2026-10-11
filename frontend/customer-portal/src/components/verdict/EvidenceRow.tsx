import { Link } from 'react-router-dom'
import { Button } from '@therecommerce-develop/ds-ui'
import { Icon } from '../layout/Icon'
import { EVIDENCE_TEXT as T } from '../../data/verdictContent'
import type { EligibilityItem } from '../../types'
import { dotDate } from '../../utils/eligibilityFormat'

const VAULT_PATH = '/documents'

/**
 * 있음으로 답한 자격 아래 증빙 서류 줄 (CR-140, T3-3 「공고별 진단」 증빙 서류, 화면 정본 detail, detail2, states).
 * 붙인 서류가 없으면 맞은 분류의 가장 최근 서류를 제안한다(서버가 최근 순으로 준다).
 */
export function EvidenceRow({ item, busy, failed, onAttach, onPick }: {
  item: EligibilityItem
  busy: boolean
  failed: boolean
  onAttach: (documentId: string | null) => void
  onPick: () => void
}) {
  const d = item.attachedDocument
  const error = failed && <span className="vd-err" role="alert">{T.attachError}</span>
  if (d) {
    return (
      <div className={`vd-ev ${d.expired ? 'vd-exp' : 'vd-att'}`}>
        <Icon name={d.expired ? 'alert' : 'folder'} />
        <div className="vd-et">
          <span className="vd-lb2">{T.label}</span><b>{d.name}</b>
          <small>
            {d.expired ? T.expired(dotDate(d.expiresOn)) : d.expiresOn ? T.expiresOn(dotDate(d.expiresOn)) : T.noExpiry}
          </small>
          {error}
        </div>
        <div className="vd-acts">
          <Button variant="ghost" className="vd-lk" disabled={busy} onClick={onPick}>{T.change}</Button>
          <Button variant="ghost" className="vd-lk vd-m" disabled={busy} onClick={() => onAttach(null)}>{T.detach}</Button>
        </div>
      </div>
    )
  }
  // 붙이자마자 서류 부족이 되지 않도록 만료된 서류는 제안하지 않는다
  const suggested = item.vaultDocuments.find((d) => !d.expired)
  const hint = suggested ? T.suggestion(suggested.name) : item.vaultDocuments.length > 0 ? T.allExpired : T.noMatch
  return (
    <div className="vd-ev vd-need">
      <Icon name="alert" />
      <div className="vd-et">
        <b>{T.need}</b>
        <small>{hint}</small>
        {error}
      </div>
      <div className="vd-acts">
        {suggested && (
          <Button variant="primary" className="vd-b vd-p" disabled={busy} onClick={() => onAttach(suggested.id)}>
            {T.attachSuggested}
          </Button>
        )}
        <Button variant="outline" className="vd-b" disabled={busy} onClick={onPick}>{T.pick}</Button>
        {!suggested && (
          <Button asChild variant="outline" className="vd-b"><Link to={VAULT_PATH}>{T.upload}</Link></Button>
        )}
      </div>
    </div>
  )
}
