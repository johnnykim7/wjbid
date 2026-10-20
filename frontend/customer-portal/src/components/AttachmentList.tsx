import { useEffect, useState } from 'react'
import { useLocation, useNavigate } from 'react-router-dom'
import { Button as DsButton } from '@therecommerce-develop/ds-ui'
import { isAxiosError } from 'axios'
import { downloadOpportunityAttachment, getOpportunityAttachments } from '../api/client'
import type { OpportunityFile } from '../types'
import { useAuth } from '../context/AuthContext'
import { Badge } from './ui/badge'
import { ATTACHMENT_TEXT as T } from '../data/attachmentContent'

interface Props {
  /** 모은 공고 id. 공고문 상세는 그 공고문이 만들어진 공고다 */
  opportunityId?: string
  resourceLinks?: string[]
  samUrl?: string
}

function fileIcon(name: string) {
  const ext = name.split('.').pop()?.toLowerCase() ?? ''
  if (ext === 'pdf') return 'fa-solid fa-file-pdf'
  if (['doc', 'docx'].includes(ext)) return 'fa-solid fa-file-word'
  if (['xls', 'xlsx'].includes(ext)) return 'fa-solid fa-file-excel'
  return 'fa-solid fa-file'
}

function formatSize(bytes: number): string {
  if (bytes < 1024) return `${bytes} B`
  if (bytes < 1024 * 1024) return `${(bytes / 1024).toFixed(0)} KB`
  return `${(bytes / 1024 / 1024).toFixed(1)} MB`
}

/** 받아 둔 파일을 먼저 두고, 받아 두지 못한 SAM.gov 첨부 주소를 그 아래에 둔다 (CR-168, BIZ-084) */
export function AttachmentList({ opportunityId, resourceLinks, samUrl }: Props) {
  const navigate = useNavigate()
  const location = useLocation()
  const { isAuthenticated } = useAuth()
  const [files, setFiles] = useState<OpportunityFile[]>([])
  const [busyId, setBusyId] = useState<string | null>(null)
  const [failed, setFailed] = useState(false)

  useEffect(() => {
    setFiles([])
    if (!opportunityId) return
    let alive = true
    getOpportunityAttachments(opportunityId)
      .then((res) => { if (alive) setFiles(res.data) })
      .catch(() => { /* 받아 둔 파일 줄 없이 SAM.gov 주소만 보인다 */ })
    return () => { alive = false }
  }, [opportunityId])

  const toLogin = () => navigate('/login', { state: { from: location.pathname } })

  const download = async (file: OpportunityFile) => {
    if (!opportunityId || busyId) return
    if (!isAuthenticated) { toLogin(); return }
    setBusyId(file.id)
    setFailed(false)
    try {
      const res = await downloadOpportunityAttachment(opportunityId, file.id)
      const url = URL.createObjectURL(res.data)
      const a = document.createElement('a')
      a.href = url
      a.download = file.fileName ?? 'attachment'
      document.body.appendChild(a)
      a.click()
      a.remove()
      setTimeout(() => URL.revokeObjectURL(url), 1000)
    } catch (e) {
      if (isAxiosError(e) && e.response?.status === 401) toLogin()
      else setFailed(true)
    } finally {
      setBusyId(null)
    }
  }

  // 받아 둔 SAM.gov 파일은 원문 주소 줄로 한 번 더 보이지 않게 한다
  const storedUrls = new Set(files.map((f) => f.sourceUrl).filter(Boolean))
  const links = (resourceLinks ?? []).filter((url) => !storedUrls.has(url))

  if (files.length === 0 && links.length === 0) {
    return (
      <div className="text-sm bg-surface p-5 rounded-lg border border-line flex items-start gap-3">
        <i className="fa-solid fa-circle-info text-brand mt-0.5" />
        <div>
          <p className="text-ink-sub mb-2">{T.empty}</p>
          {samUrl && (
            <a href={samUrl} target="_blank" rel="noopener noreferrer"
              className="inline-flex items-center gap-1.5 text-brand hover:text-brand-strong font-medium">
              <i className="fa-solid fa-arrow-up-right-from-square text-xs" />
              {T.samCheck}
            </a>
          )}
        </div>
      </div>
    )
  }

  return (
    <div className="space-y-3">
      {failed && <p className="text-sm text-danger">{T.failed}</p>}
      <ul className="grid grid-cols-1 md:grid-cols-2 gap-3">
        {files.map((f) => {
          const name = f.fileName ?? 'attachment'
          const tag = T.source[f.source]
          return (
            <li key={f.id} className="flex items-center p-4 border border-line rounded-lg">
              <div className="w-10 h-10 flex items-center justify-center rounded mr-3 bg-surface text-ink-mute shrink-0">
                <i className={`${fileIcon(name)} text-xl`} />
              </div>
              <div className="flex-1 min-w-0">
                <div className="text-sm font-bold text-ink truncate" title={name}>{name}</div>
                <div className="text-xs text-ink-mute mt-1 flex items-center gap-2">
                  {tag && <Badge variant="secondary" size="sm">{tag}</Badge>}
                  {f.fileSize != null && <span>{formatSize(f.fileSize)}</span>}
                </div>
              </div>
              <DsButton variant="outline" size="sm" className="ml-2 shrink-0" disabled={busyId === f.id} onClick={() => download(f)}>
                {busyId === f.id ? T.downloading : T.download}
              </DsButton>
            </li>
          )
        })}
        {links.map((url) => {
          const filename = url.split('/').pop()?.split('?')[0] ?? url
          return (
            <li key={url}>
              <a href={url} target="_blank" rel="noopener noreferrer"
                className="flex items-center p-4 border border-line rounded-lg hover:bg-surface transition">
                <div className="w-10 h-10 flex items-center justify-center rounded mr-3 bg-surface text-ink-mute shrink-0">
                  <i className={`${fileIcon(filename)} text-xl`} />
                </div>
                <div className="flex-1 min-w-0">
                  <div className="text-sm font-bold text-ink truncate">{filename}</div>
                  <div className="text-xs text-ink-mute mt-0.5 truncate">{url}</div>
                </div>
                <i className="fa-solid fa-arrow-up-right-from-square text-ink-mute ml-2" />
              </a>
            </li>
          )
        })}
      </ul>
    </div>
  )
}
