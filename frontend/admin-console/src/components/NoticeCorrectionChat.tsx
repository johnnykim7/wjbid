import { useEffect, useRef, useState } from 'react'
import { issueWidgetToken } from '../api/client'

/**
 * CR-033: 공고문 교정 채팅 패널.
 *
 * 관리자가 COMPLETED 공고 분석 결과를 자연어로 부분 교정하는 채팅. Aimbase 위젯(UMD)을
 * <script> 동적 로드하여 마운트한다. 위젯이 chat/completions 를 호출하면 Aimbase LLM 이
 * 우리 MCP 도구(get_opportunity_analysis → 수정 → save_opportunity_analysis)를 직접 부른다.
 *
 * ⚠️ 전제 (실측): 현재 배포된 Aimbase 위젯 번들은 connectionId/actionsEnabled 를 chat body 로
 *    전송하지 않는다(widget.ts sendMessage 호출이 6필드만 전달). 따라서 이 컴포넌트는
 *    Aimbase 위젯 패키지가 connectionId/actionsEnabled 옵션을 지원하도록 수정·재배포된 뒤에야
 *    실제 교정이 동작한다. 그 전까지는 위젯이 connection_id 없이 호출 → placeholder 키 401 → 400.
 *    (지시서: /tmp/aimbase-widget-correction-task.md)
 */

const AIMBASE_BASE_URL =
  import.meta.env.VITE_AIMBASE_BASE_URL || 'http://59.8.160.12:8280'

// bidding_system 테넌트의 Anthropic connection (bidding-claude-sonnet).
// connection_id 없으면 BE 가 전역 placeholder 키로 폴백 → 401.
const AIMBASE_CONNECTION_ID =
  import.meta.env.VITE_AIMBASE_CONNECTION_ID ||
  'bb3bc8bc-d17d-4e8e-a15b-d9f077e83d18'

const WIDGET_SCRIPT_URL = `${AIMBASE_BASE_URL}/widget/v1/aimbase-chat.umd.global.js`
const SCRIPT_ID = 'aimbase-chat-umd'

/** UMD 번들이 노출하는 전역 (tsup globalName: "AimbaseChat"). */
interface AimbaseChatGlobal {
  createWidget: (options: AimbaseWidgetOptions) => AimbaseWidgetHandle
}
interface AimbaseWidgetOptions {
  baseUrl: string
  authResolver: () => Promise<{ token: string; expires_at?: string; refresh_after?: string }>
  display?: 'bubble' | 'inline' | 'panel'
  target?: string | HTMLElement
  sessionId?: string
  contextProvider?: () => Record<string, unknown>
  // ⚠️ 아래 3개는 Aimbase 위젯 패키지 수정 후 지원되는 옵션 (현재 번들엔 미존재).
  connectionId?: string
  actionsEnabled?: boolean
  model?: string
  on?: {
    onMessage?: (delta: unknown) => void
    onError?: (err: Error) => void
  }
}
interface AimbaseWidgetHandle {
  destroy: () => void
}

declare global {
  interface Window {
    AimbaseChat?: AimbaseChatGlobal
  }
}

/** UMD 스크립트를 1회만 주입하고 window.AimbaseChat 을 resolve. */
function loadWidgetScript(): Promise<AimbaseChatGlobal> {
  return new Promise((resolve, reject) => {
    if (window.AimbaseChat) {
      resolve(window.AimbaseChat)
      return
    }
    const existing = document.getElementById(SCRIPT_ID) as HTMLScriptElement | null
    if (existing) {
      existing.addEventListener('load', () => {
        window.AimbaseChat
          ? resolve(window.AimbaseChat)
          : reject(new Error('위젯 로드됐으나 AimbaseChat 전역이 없습니다.'))
      })
      existing.addEventListener('error', () => reject(new Error('위젯 스크립트 로드 실패')))
      return
    }
    const script = document.createElement('script')
    script.id = SCRIPT_ID
    script.src = WIDGET_SCRIPT_URL
    script.async = true
    script.onload = () => {
      window.AimbaseChat
        ? resolve(window.AimbaseChat)
        : reject(new Error('위젯 로드됐으나 AimbaseChat 전역이 없습니다.'))
    }
    script.onerror = () => reject(new Error('위젯 스크립트 로드 실패'))
    document.head.appendChild(script)
  })
}

/** 교정 채팅에 주입할 시스템 지시. contextProvider 가 반환하면 위젯이 system 메시지로 선행 주입. */
function buildCorrectionInstruction(noticeId: string): Record<string, unknown> {
  return {
    instruction:
      `당신은 미군 공고 분석 결과를 교정하는 도우미입니다. noticeId=${noticeId}. ` +
      `사용자가 수정을 요청하면 (1) get_opportunity_analysis 로 현재 분석 결과를 읽고 ` +
      `(2) 사용자가 지시한 부분만 수정하고 나머지는 그대로 보존한 뒤 ` +
      `(3) save_opportunity_analysis 로 저장하세요. ` +
      `⚠️ save 는 전체 치환이므로, 읽어들인 facts 배열도 반드시 함께 다시 실어야 합니다(빠지면 근거가 소멸). ` +
      `트럭·인력·과거실적처럼 고객이 보유/제출하는 항목의 fulfillmentParty 는 CLIENT_UPLOAD 입니다.`,
    noticeId,
  }
}

interface Props {
  noticeId: string
}

export default function NoticeCorrectionChat({ noticeId }: Props) {
  const [open, setOpen] = useState(false)
  const [error, setError] = useState<string | null>(null)
  const [loading, setLoading] = useState(false)
  const containerRef = useRef<HTMLDivElement | null>(null)
  const handleRef = useRef<AimbaseWidgetHandle | null>(null)

  useEffect(() => {
    if (!open || !containerRef.current) return
    let cancelled = false
    setLoading(true)
    setError(null)

    loadWidgetScript()
      .then((AimbaseChat) => {
        if (cancelled || !containerRef.current) return
        handleRef.current = AimbaseChat.createWidget({
          baseUrl: AIMBASE_BASE_URL,
          authResolver: async () => {
            const { data } = await issueWidgetToken()
            return data as { token: string; expires_at?: string; refresh_after?: string }
          },
          display: 'inline',
          target: containerRef.current,
          sessionId: `notice-correction-${noticeId}`,
          connectionId: AIMBASE_CONNECTION_ID,
          actionsEnabled: true,
          contextProvider: () => buildCorrectionInstruction(noticeId),
          on: {
            onError: (err) => setError(err.message),
          },
        })
      })
      .catch((e) => !cancelled && setError(e instanceof Error ? e.message : '위젯 초기화 실패'))
      .finally(() => !cancelled && setLoading(false))

    return () => {
      cancelled = true
      handleRef.current?.destroy()
      handleRef.current = null
    }
  }, [open, noticeId])

  return (
    <div className="bg-white rounded-xl border border-gray-200 p-5">
      <div className="flex items-center justify-between">
        <div>
          <h3 className="text-sm font-semibold text-gray-700">
            <i className="fa-solid fa-comments mr-1.5 text-purple-500" />교정 채팅
          </h3>
          <p className="text-xs text-gray-400 mt-0.5">
            분석 결과를 자연어로 수정합니다. 예: "트럭 보유 요건은 고객 업로드로 바꿔줘"
          </p>
        </div>
        <button
          onClick={() => setOpen((v) => !v)}
          className="px-4 py-2 text-sm rounded-lg bg-purple-50 text-purple-700 hover:bg-purple-100"
        >
          {open ? '닫기' : '교정 채팅 열기'}
        </button>
      </div>

      {open && (
        <div className="mt-4">
          {loading && (
            <div className="flex items-center gap-2 text-sm text-gray-400 py-4">
              <div className="w-4 h-4 border-2 border-gray-200 border-t-purple-500 rounded-full animate-spin" />
              채팅을 불러오는 중...
            </div>
          )}
          {error && (
            <div className="bg-red-50 rounded-lg p-3 text-sm text-red-700 mb-3">
              채팅 로드 실패: {error}
            </div>
          )}
          <div ref={containerRef} className="min-h-[420px] border border-gray-100 rounded-lg" />
        </div>
      )}
    </div>
  )
}
