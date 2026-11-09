/**
 * CR-196: 자료실 (T3-3 「자료실」, T3-4 「CR-196: 자료실」).
 * 파일 비밀번호는 메일로만 오므로 이 화면은 비밀번호를 보여 주지도 기억하지도 않는다.
 */
import { useCallback, useEffect, useRef, useState } from 'react'
import { Download, Info, Trash2, Upload } from 'lucide-react'
import {
  Alert,
  AlertDescription,
  Button,
  Card,
  CardFooter,
  CardTable,
  ConfirmDialog,
  Dialog,
  DialogBody,
  DialogContent,
  DialogFooter,
  DialogHeader,
  DialogTitle,
  Input,
  Label,
  Spinner,
  Table,
  TableBody,
  TableCell,
  TableHead,
  TableHeader,
  TableRow,
  Tabs,
  TabsContent,
  TabsList,
  TabsTrigger,
} from '@therecommerce-develop/ds-ui'
import {
  deleteSharedFile,
  downloadSharedFile,
  getSharedFileLogs,
  getSharedFiles,
  uploadSharedFile,
  type SharedFileItem,
  type SharedFileList,
  type SharedFileLogPage,
  type SharedFileRules,
} from '../api/client'
import ListPagination from '../components/ListPagination'
import { TableMessageRow } from '../components/LoadState'
import { ToneBadge } from '../components/StateBadge'

const T = {
  intro: '파일을 올리면 비밀번호가 내 메일로 옵니다. 함께 볼 관리자에게는 그 메일을 전달해 주세요',
  upload: '파일 올리기',
  uploading: '보내는 중',
  rules: (r: SharedFileRules) =>
    `한 파일 ${r.maxUploadMb}MB 까지. 받지 않는 파일 종류: ${r.blockedExtensions.join(', ')}`,
  tabFiles: '파일',
  tabLogs: '기록',
  fileCols: ['파일 이름', '크기', '올린 사람', '등록일', ''],
  logCols: ['때', '관리자', '파일 이름', '구분', 'IP'],
  empty: '올라온 파일이 없습니다',
  logsEmpty: '기록이 없습니다',
  locked: '잠김',
  receive: '받기',
  remove: '지우기',
  close: '닫기',
  loadFail: '불러오지 못했습니다.',
  retry: '다시 시도',
  uploadFail: '올리지 못했습니다.',
  uploaded: (mailedTo: string) =>
    `비밀번호를 ${mailedTo}로 보냈습니다. 메일이 오지 않으면 이 파일은 열 수 없으니 지우고 다시 올려 주세요`,
  blocked: (name: string) => `${name}: 이 종류의 파일은 올릴 수 없습니다`,
  tooLarge: (name: string, mb: number) => `${name}: 한 파일 ${mb}MB 를 넘어 올릴 수 없습니다`,
  receiveTitle: '내려받기',
  password: '파일 비밀번호',
  receiving: '받는 중',
  wrong: (n: number) => `비밀번호가 맞지 않습니다. ${n}번 남았습니다`,
  lockedFor: (n: number) => `많이 틀려 잠겼습니다. ${n}분 뒤 다시 시도해 주세요`,
  receiveFail: '받지 못했습니다.',
  confirmTitle: '파일을 지울까요?',
  confirm: (name: string) => `${name} 을 지웁니다. 되돌릴 수 없습니다.`,
  deleteFail: '지우지 못했습니다.',
}

const ACTION_LABEL: Record<string, string> = {
  UPLOAD: '올리기',
  DOWNLOAD: '받기',
  WRONG_PASSWORD: '비밀번호가 맞지 않습니다',
  LOCKED: '잠김',
  DELETE: '지우기',
}

interface DownloadErrorBody {
  remainingAttempts?: number
  retryAfterMinutes?: number
  message?: string
}

const serverMessage = (e: unknown): string | undefined =>
  (e as { response?: { data?: { message?: string } } })?.response?.data?.message

// 받기는 응답을 파일로 받도록 요청해서 오류 본문도 파일 형태로 온다
const downloadFailure = async (e: unknown): Promise<{ status?: number; body: DownloadErrorBody }> => {
  const response = (e as { response?: { status?: number; data?: Blob } })?.response
  try {
    return { status: response?.status, body: JSON.parse((await response?.data?.text()) ?? '{}') }
  } catch {
    return { status: response?.status, body: {} }
  }
}

const sizeText = (bytes: number) =>
  bytes >= 1024 * 1024 ? `${(bytes / 1024 / 1024).toFixed(1)}MB` : `${Math.max(1, Math.round(bytes / 1024))}KB`
const timeText = (value: string) => value.slice(0, 16).replace('T', ' ')

const extensionOf = (name: string) => {
  const trimmed = name.replace(/[. ]+$/, '')
  const dot = trimmed.lastIndexOf('.')
  return dot < 0 ? '' : trimmed.slice(dot + 1).toLowerCase()
}

const saveAs = (blob: Blob, fileName: string) => {
  const url = URL.createObjectURL(blob)
  const link = document.createElement('a')
  link.href = url
  link.download = fileName
  link.click()
  URL.revokeObjectURL(url)
}

export default function SharedFilePage() {
  const [tab, setTab] = useState('files')
  const [list, setList] = useState<SharedFileList | null>(null)
  const [page, setPage] = useState(0)
  const [failed, setFailed] = useState(false)
  const [uploading, setUploading] = useState(false)
  const [uploadError, setUploadError] = useState<string | null>(null)
  const [uploadedTo, setUploadedTo] = useState<string | null>(null)
  const [receivingFile, setReceivingFile] = useState<SharedFileItem | null>(null)
  const [password, setPassword] = useState('')
  const [receiving, setReceiving] = useState(false)
  const [receiveError, setReceiveError] = useState<string | null>(null)
  const [locked, setLocked] = useState(false)
  const [removing, setRemoving] = useState<SharedFileItem | null>(null)
  const [logs, setLogs] = useState<SharedFileLogPage | null>(null)
  const [logPage, setLogPage] = useState(0)
  const fileInput = useRef<HTMLInputElement>(null)

  const loadFiles = useCallback(() => {
    getSharedFiles(page)
      .then(({ data }) => {
        setList(data)
        setFailed(false)
      })
      .catch(() => setFailed(true))
  }, [page])

  useEffect(() => {
    loadFiles()
  }, [loadFiles])

  useEffect(() => {
    if (tab !== 'logs') return
    getSharedFileLogs(logPage)
      .then(({ data }) => {
        setLogs(data)
        setFailed(false)
      })
      .catch(() => setFailed(true))
  }, [tab, logPage])

  const onPick = async (e: React.ChangeEvent<HTMLInputElement>) => {
    const file = e.target.files?.[0]
    e.target.value = ''
    if (!file || !list) return
    setUploadError(null)
    setUploadedTo(null)
    if (list.rules.blockedExtensions.includes(extensionOf(file.name))) {
      setUploadError(T.blocked(file.name))
      return
    }
    if (file.size > list.rules.maxUploadMb * 1024 * 1024) {
      setUploadError(T.tooLarge(file.name, list.rules.maxUploadMb))
      return
    }
    setUploading(true)
    try {
      const { data } = await uploadSharedFile(file)
      setUploadedTo(data.mailedTo)
      if (page === 0) loadFiles()
      else setPage(0)
    } catch (err) {
      setUploadError(serverMessage(err) ?? T.uploadFail)
    } finally {
      setUploading(false)
    }
  }

  const openReceive = (file: SharedFileItem) => {
    setPassword('')
    setReceiveError(null)
    setLocked(false)
    setReceivingFile(file)
  }

  const closeReceive = () => {
    setPassword('')
    setReceiveError(null)
    setLocked(false)
    setReceivingFile(null)
  }

  const receive = async (e: React.FormEvent) => {
    e.preventDefault()
    if (!receivingFile || !password || locked) return
    setReceiving(true)
    setReceiveError(null)
    try {
      const { data } = await downloadSharedFile(receivingFile.id, password)
      saveAs(data, receivingFile.fileName)
      closeReceive()
    } catch (err) {
      const { status, body } = await downloadFailure(err)
      setPassword('')
      if (status === 429) {
        setLocked(true)
        setReceiveError(T.lockedFor(body.retryAfterMinutes ?? 1))
        loadFiles()
      } else if (status === 400 && body.remainingAttempts !== undefined) {
        setReceiveError(T.wrong(body.remainingAttempts))
      } else {
        setReceiveError(body.message ?? T.receiveFail)
      }
    } finally {
      setReceiving(false)
    }
  }

  const remove = async () => {
    if (!removing) return
    await deleteSharedFile(removing.id)
    const lastRowOfPage = list?.files.length === 1 && page > 0
    setRemoving(null)
    if (lastRowOfPage) setPage(page - 1)
    else loadFiles()
  }

  return (
    <div className="space-y-4">
      <div className="flex items-center justify-between gap-4">
        <p className="text-sm text-muted-foreground">{T.intro}</p>
        <Button variant="primary" disabled={uploading || !list} onClick={() => fileInput.current?.click()}>
          {uploading ? <Spinner /> : <Upload />}
          {uploading ? T.uploading : T.upload}
        </Button>
        <Input ref={fileInput} type="file" className="hidden" onChange={onPick} />
      </div>

      {list && (
        <div className="flex items-start gap-2 rounded-lg border border-border bg-muted px-3 py-2 text-xs text-muted-foreground">
          <Info className="mt-0.5 size-4 shrink-0" /> {T.rules(list.rules)}
        </div>
      )}
      {failed && (
        <Alert variant="destructive" appearance="light" className="items-center">
          <AlertDescription>{T.loadFail}</AlertDescription>
          <Button variant="outline" size="sm" className="ms-auto" onClick={loadFiles}>{T.retry}</Button>
        </Alert>
      )}
      {uploadError && (
        <Alert variant="destructive" appearance="light" className="items-center">
          <AlertDescription>{uploadError}</AlertDescription>
          <Button variant="outline" size="sm" className="ms-auto" onClick={() => setUploadError(null)}>{T.close}</Button>
        </Alert>
      )}
      {uploadedTo && (
        <Alert variant="info" appearance="light" className="items-center">
          <AlertDescription>{T.uploaded(uploadedTo)}</AlertDescription>
          <Button variant="outline" size="sm" className="ms-auto" onClick={() => setUploadedTo(null)}>{T.close}</Button>
        </Alert>
      )}

      <Tabs value={tab} onValueChange={setTab}>
        <TabsList>
          <TabsTrigger value="files">{T.tabFiles}</TabsTrigger>
          <TabsTrigger value="logs">{T.tabLogs}</TabsTrigger>
        </TabsList>

        <TabsContent value="files">
          <Card className="overflow-hidden">
            <CardTable>
              <Table>
                <TableHeader className="bg-muted">
                  <TableRow>
                    {T.fileCols.map((c, i) => <TableHead key={i} className="whitespace-nowrap">{c}</TableHead>)}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {list === null && !failed && <TableMessageRow colSpan={T.fileCols.length} loading />}
                  {list?.files.length === 0 && <TableMessageRow colSpan={T.fileCols.length}>{T.empty}</TableMessageRow>}
                  {list?.files.map((file) => (
                    <TableRow key={file.id}>
                      <TableCell className="font-semibold">
                        <span className="me-2 break-all">{file.fileName}</span>
                        {file.lockedUntil && <ToneBadge tone="warning">{T.locked}</ToneBadge>}
                      </TableCell>
                      <TableCell className="whitespace-nowrap">{sizeText(file.sizeBytes)}</TableCell>
                      <TableCell>{file.uploadedByEmail}</TableCell>
                      <TableCell className="whitespace-nowrap">{timeText(file.createdAt)}</TableCell>
                      <TableCell className="whitespace-nowrap text-right">
                        <Button variant="outline" size="sm" onClick={() => openReceive(file)}>
                          <Download />
                          {T.receive}
                        </Button>
                        {file.mine && (
                          <Button variant="ghost" size="sm" className="ms-1" onClick={() => setRemoving(file)}>
                            <Trash2 />
                            {T.remove}
                          </Button>
                        )}
                      </TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardTable>
            {list && list.totalPages > 1 && (
              <CardFooter className="justify-center py-2">
                <ListPagination page={list.page} totalPages={list.totalPages} onChange={setPage} />
              </CardFooter>
            )}
          </Card>
        </TabsContent>

        <TabsContent value="logs">
          <Card className="overflow-hidden">
            <CardTable>
              <Table>
                <TableHeader className="bg-muted">
                  <TableRow>
                    {T.logCols.map((c) => <TableHead key={c} className="whitespace-nowrap">{c}</TableHead>)}
                  </TableRow>
                </TableHeader>
                <TableBody>
                  {logs === null && !failed && <TableMessageRow colSpan={T.logCols.length} loading />}
                  {logs?.logs.length === 0 && <TableMessageRow colSpan={T.logCols.length}>{T.logsEmpty}</TableMessageRow>}
                  {logs?.logs.map((log) => (
                    <TableRow key={log.id}>
                      <TableCell className="whitespace-nowrap">{timeText(log.createdAt)}</TableCell>
                      <TableCell>{log.memberEmail}</TableCell>
                      <TableCell className="break-all">{log.fileName}</TableCell>
                      <TableCell className="whitespace-nowrap">{ACTION_LABEL[log.action] ?? log.action}</TableCell>
                      <TableCell className="whitespace-nowrap font-mono text-xs">{log.clientIp ?? '-'}</TableCell>
                    </TableRow>
                  ))}
                </TableBody>
              </Table>
            </CardTable>
            {logs && logs.totalPages > 1 && (
              <CardFooter className="justify-center py-2">
                <ListPagination page={logs.page} totalPages={logs.totalPages} onChange={setLogPage} />
              </CardFooter>
            )}
          </Card>
        </TabsContent>
      </Tabs>

      <Dialog open={receivingFile !== null} onOpenChange={(o) => !receiving && !o && closeReceive()}>
        <DialogContent showCloseButton={!receiving}>
          <form onSubmit={receive}>
            <DialogHeader>
              <DialogTitle>{T.receiveTitle}</DialogTitle>
            </DialogHeader>
            <DialogBody className="space-y-3">
              <p className="break-all text-sm font-semibold">{receivingFile?.fileName}</p>
              <div className="space-y-1.5">
                <Label htmlFor="shared-file-password">{T.password}</Label>
                <Input
                  id="shared-file-password"
                  type="password"
                  autoComplete="off"
                  value={password}
                  disabled={locked || receiving}
                  onChange={(e) => setPassword(e.target.value)}
                />
              </div>
              {receiveError && (
                <Alert variant="destructive" appearance="light">
                  <AlertDescription>{receiveError}</AlertDescription>
                </Alert>
              )}
            </DialogBody>
            <DialogFooter>
              <Button type="button" variant="outline" disabled={receiving} onClick={closeReceive}>{T.close}</Button>
              <Button type="submit" variant="primary" disabled={!password || locked || receiving}>
                {receiving ? <Spinner /> : <Download />}
                {receiving ? T.receiving : T.receive}
              </Button>
            </DialogFooter>
          </form>
        </DialogContent>
      </Dialog>

      <ConfirmDialog
        open={removing !== null}
        onOpenChange={(o) => { if (!o) setRemoving(null) }}
        title={T.confirmTitle}
        description={removing ? T.confirm(removing.fileName) : undefined}
        confirmLabel={T.remove}
        confirmVariant="destructive"
        cancelLabel={T.close}
        onConfirm={remove}
        errorText={(e: unknown) => serverMessage(e) ?? T.deleteFail}
      />
    </div>
  )
}
