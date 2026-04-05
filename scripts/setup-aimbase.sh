#!/usr/bin/env bash
#
# Aimbase 셋업 스크립트 (CR-002)
# 입찰제안시스템을 Aimbase에 연동하기 위한 초기 설정을 수행합니다.
#
# 사전 조건:
#   - Aimbase가 실행 중이어야 합니다 (14.63.25.49:8280)
#   - API Key가 유효해야 합니다
#
# 사용법:
#   ./scripts/setup-aimbase.sh
#   PLATFORM_HOST=192.168.1.100 ./scripts/setup-aimbase.sh

set -euo pipefail

# ─── 설정 ────────────────────────────────────────────────────────────────────
AIMBASE_URL="${AIMBASE_URL:-http://14.63.25.49:8280}"
AIMBASE_API="${AIMBASE_URL}/api/v1"
API_KEY="${AIMBASE_API_KEY:-plat-20cf57fbc623424584eeda2e355cbb43}"
PLATFORM_HOST="${PLATFORM_HOST:-localhost}"
PLATFORM_PORT="${PLATFORM_PORT:-8088}"

HEADER=(-H "X-API-Key: ${API_KEY}" -H "Content-Type: application/json")

echo "=== Aimbase 셋업 시작 ==="
echo "Aimbase: ${AIMBASE_URL}"
echo "Platform: ${PLATFORM_HOST}:${PLATFORM_PORT}"
echo ""

# ─── 1. 연결 확인 ────────────────────────────────────────────────────────────
echo "[1/5] Aimbase 연결 확인..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" --max-time 5 "${HEADER[@]}" "${AIMBASE_API}/connections")
if [ "$HTTP_CODE" != "200" ]; then
    echo "ERROR: Aimbase 연결 실패 (HTTP ${HTTP_CODE})"
    exit 1
fi
echo "  OK (HTTP 200)"

# ─── 2. LLM Connection 확인/생성 ─────────────────────────────────────────────
echo ""
echo "[2/5] LLM Connection 확인/생성..."

EXISTING_CONN=$(curl -s "${HEADER[@]}" "${AIMBASE_API}/connections" | \
    python3 -c "
import sys,json
data = json.load(sys.stdin).get('data',[])
# content가 list가 아니면 pagination 처리
if isinstance(data, dict): data = data.get('content', [])
result = next((c['id'] for c in data if 'bidding' in c.get('name','').lower()), '')
print(result)
" 2>/dev/null || echo "")

if [ -n "$EXISTING_CONN" ]; then
    echo "  기존 Connection 발견: ${EXISTING_CONN}"
    CONNECTION_ID="${EXISTING_CONN}"
else
    ANTHROPIC_KEY="${ANTHROPIC_API_KEY:-}"
    if [ -z "$ANTHROPIC_KEY" ]; then
        echo "  Connection이 없고 ANTHROPIC_API_KEY 환경변수도 없습니다."
        echo "  다음 중 하나를 실행하세요:"
        echo "    export ANTHROPIC_API_KEY=sk-ant-... && ./scripts/setup-aimbase.sh"
        echo "    export LLM_CONNECTION_ID=<기존ID> && ./scripts/setup-aimbase.sh"
        CONNECTION_ID="${LLM_CONNECTION_ID:-}"
    else
        echo "  Claude Sonnet Connection 생성 중..."
        # Aimbase Connection 필드: name, adapter(not provider), type(소문자), config
        CONN_RESPONSE=$(curl -s "${HEADER[@]}" -X POST "${AIMBASE_API}/connections" \
            -d "{
                \"name\": \"bidding-claude-sonnet\",
                \"adapter\": \"anthropic\",
                \"type\": \"llm\",
                \"config\": {
                    \"apiKey\": \"${ANTHROPIC_KEY}\",
                    \"model\": \"claude-sonnet-4-20250514\",
                    \"maxTokens\": 4096
                }
            }")
        CONNECTION_ID=$(echo "$CONN_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('id',''))" 2>/dev/null || echo "")
        if [ -n "$CONNECTION_ID" ]; then
            echo "  생성 완료: ${CONNECTION_ID}"
            # 연결 테스트
            TEST_RESULT=$(curl -s "${HEADER[@]}" -X POST "${AIMBASE_API}/connections/${CONNECTION_ID}/test" | \
                python3 -c "import sys,json; d=json.load(sys.stdin).get('data',{}); print(f'ok={d.get(\"ok\")}, latency={d.get(\"latencyMs\")}ms')" 2>/dev/null || echo "test failed")
            echo "  연결 테스트: ${TEST_RESULT}"
        else
            echo "  ERROR: Connection 생성 실패. 응답: ${CONN_RESPONSE}"
            CONNECTION_ID="${LLM_CONNECTION_ID:-}"
        fi
    fi
fi

if [ -z "${CONNECTION_ID:-}" ]; then
    echo "  WARNING: CONNECTION_ID가 없습니다. 워크플로우의 LLM_CALL 스텝이 동작하지 않습니다."
fi

# ─── 3. MCP 서버 등록 ────────────────────────────────────────────────────────
echo ""
echo "[3/5] MCP 서버 등록..."

MCP_SSE_URL="http://${PLATFORM_HOST}:${PLATFORM_PORT}/api/mcp/sse"

EXISTING_MCP=$(curl -s "${HEADER[@]}" "${AIMBASE_API}/mcp-servers" | \
    python3 -c "
import sys,json
data = json.load(sys.stdin).get('data',[])
if isinstance(data, dict): data = data.get('content', [])
result = next((s['id'] for s in data if 'bidding' in s.get('name','').lower()), '')
print(result)
" 2>/dev/null || echo "")

if [ -n "$EXISTING_MCP" ]; then
    echo "  기존 MCP 서버 발견: ${EXISTING_MCP}"
    # URL 업데이트
    curl -s "${HEADER[@]}" -X PUT "${AIMBASE_API}/mcp-servers/${EXISTING_MCP}" \
        -d "{\"name\":\"bidding-agency-mcp\",\"transport\":\"sse\",\"config\":{\"url\":\"${MCP_SSE_URL}\"}}" > /dev/null
    echo "  URL 업데이트: ${MCP_SSE_URL}"
    MCP_SERVER_ID="${EXISTING_MCP}"
else
    echo "  MCP 서버 등록 중... (URL: ${MCP_SSE_URL})"
    MCP_RESPONSE=$(curl -s "${HEADER[@]}" -X POST "${AIMBASE_API}/mcp-servers" \
        -d "{
            \"name\": \"bidding-agency-mcp\",
            \"transport\": \"sse\",
            \"config\": { \"url\": \"${MCP_SSE_URL}\" },
            \"autoStart\": true
        }")
    MCP_SERVER_ID=$(echo "$MCP_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('id',''))" 2>/dev/null || echo "")
    if [ -n "$MCP_SERVER_ID" ]; then
        echo "  등록 완료: ${MCP_SERVER_ID}"
    else
        echo "  WARNING: 등록 실패. 응답: ${MCP_RESPONSE}"
    fi
fi

# Tool Discovery (플랫폼이 실행 중일 때만 성공)
if [ -n "${MCP_SERVER_ID:-}" ]; then
    echo "  Tool Discovery 시도..."
    DISCOVER_RESPONSE=$(curl -s --max-time 10 "${HEADER[@]}" -X POST "${AIMBASE_API}/mcp-servers/${MCP_SERVER_ID}/discover" 2>/dev/null || echo "{}")
    TOOL_COUNT=$(echo "$DISCOVER_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('toolCount',0))" 2>/dev/null || echo "0")
    if [ "$TOOL_COUNT" != "0" ]; then
        echo "  발견된 도구: ${TOOL_COUNT}개"
    else
        echo "  도구 미발견 (플랫폼 미실행 또는 네트워크 접근 불가). 나중에 재시도 가능."
    fi
fi

# ─── 4. 워크플로우 생성 ──────────────────────────────────────────────────────
echo ""
echo "[4/5] 워크플로우 확인/생성..."

# Aimbase WorkflowRequest 필수 필드: name(@NotBlank), triggerConfig(@NotNull), steps(@NotNull)

# --- requirement-extraction ---
EXISTING_WF_REQ=$(curl -s "${HEADER[@]}" "${AIMBASE_API}/workflows" | \
    python3 -c "
import sys,json
data = json.load(sys.stdin).get('data',[])
if isinstance(data, dict): data = data.get('content', [])
result = next((w['id'] for w in data if 'requirement' in w.get('name','').lower()), '')
print(result)
" 2>/dev/null || echo "")

if [ -n "$EXISTING_WF_REQ" ]; then
    echo "  requirement-extraction 존재: ${EXISTING_WF_REQ}"
else
    echo "  requirement-extraction 생성 중..."
    WF_REQ_RESPONSE=$(curl -s "${HEADER[@]}" -X POST "${AIMBASE_API}/workflows" \
        -d "{
            \"name\": \"requirement-extraction\",
            \"domain\": \"bidding\",
            \"triggerConfig\": { \"type\": \"api\" },
            \"inputSchema\": {
                \"type\": \"object\",
                \"properties\": {
                    \"opportunityId\": { \"type\": \"string\" },
                    \"opportunityText\": { \"type\": \"string\" },
                    \"metadata\": { \"type\": \"object\" }
                },
                \"required\": [\"opportunityId\", \"opportunityText\"]
            },
            \"steps\": [
                {
                    \"id\": \"fetch_opportunity\",
                    \"type\": \"TOOL_USE\",
                    \"config\": {
                        \"tool_name\": \"get_opportunity\",
                        \"arguments\": { \"opportunityId\": \"{{input.opportunityId}}\" }
                    }
                },
                {
                    \"id\": \"extract_requirements\",
                    \"type\": \"LLM_CALL\",
                    \"config\": {
                        \"connection_id\": \"${CONNECTION_ID:-}\",
                        \"system\": \"You are an expert US government procurement analyst specializing in USFK contracts. Extract ALL requirements from the given RFP/opportunity. Categorize each as: DOCUMENT, FORMAT, SUBMISSION, DEADLINE, ELIGIBILITY, TECHNICAL, or OTHER. Mark mandatory requirements as blockers. Also extract submission format requirements (Word/PDF/page limits).\",
                        \"prompt\": \"Analyze this opportunity and extract structured requirements.\\n\\nOpportunity Details:\\n{{fetch_opportunity.output}}\\n\\nFull Text:\\n{{input.opportunityText}}\\n\\nReturn JSON: { \\\"requirements\\\": [{ \\\"category\\\": \\\"...\\\", \\\"title\\\": \\\"...\\\", \\\"description\\\": \\\"...\\\", \\\"isBlocker\\\": true/false }] }\"
                    },
                    \"depends_on\": [\"fetch_opportunity\"]
                },
                {
                    \"id\": \"save_results\",
                    \"type\": \"TOOL_USE\",
                    \"config\": {
                        \"tool_name\": \"save_requirements\",
                        \"arguments\": {
                            \"opportunityId\": \"{{input.opportunityId}}\",
                            \"requirements\": \"{{extract_requirements.output.requirements}}\"
                        }
                    },
                    \"depends_on\": [\"extract_requirements\"]
                }
            ]
        }")
    WF_REQ_ID=$(echo "$WF_REQ_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('id',''))" 2>/dev/null || echo "")
    if [ -n "$WF_REQ_ID" ]; then
        echo "  생성 완료: ${WF_REQ_ID}"
    else
        echo "  FAILED. 응답: $(echo "$WF_REQ_RESPONSE" | head -c 200)"
    fi
fi

# --- bid-document-generation ---
EXISTING_WF_DOC=$(curl -s "${HEADER[@]}" "${AIMBASE_API}/workflows" | \
    python3 -c "
import sys,json
data = json.load(sys.stdin).get('data',[])
if isinstance(data, dict): data = data.get('content', [])
result = next((w['id'] for w in data if 'document' in w.get('name','').lower()), '')
print(result)
" 2>/dev/null || echo "")

if [ -n "$EXISTING_WF_DOC" ]; then
    echo "  bid-document-generation 존재: ${EXISTING_WF_DOC}"
else
    echo "  bid-document-generation 생성 중..."
    WF_DOC_RESPONSE=$(curl -s "${HEADER[@]}" -X POST "${AIMBASE_API}/workflows" \
        -d "{
            \"name\": \"bid-document-generation\",
            \"domain\": \"bidding\",
            \"triggerConfig\": { \"type\": \"api\" },
            \"inputSchema\": {
                \"type\": \"object\",
                \"properties\": {
                    \"bidRequestId\": { \"type\": \"string\" },
                    \"documentType\": { \"type\": \"string\" },
                    \"opportunityText\": { \"type\": \"string\" },
                    \"requirements\": { \"type\": \"array\" },
                    \"context\": { \"type\": \"object\" }
                },
                \"required\": [\"bidRequestId\", \"documentType\"]
            },
            \"steps\": [
                {
                    \"id\": \"fetch_bid\",
                    \"type\": \"TOOL_USE\",
                    \"config\": {
                        \"tool_name\": \"get_bid_request\",
                        \"arguments\": { \"bidRequestId\": \"{{input.bidRequestId}}\" }
                    }
                },
                {
                    \"id\": \"fetch_template\",
                    \"type\": \"TOOL_USE\",
                    \"config\": {
                        \"tool_name\": \"get_document_template\",
                        \"arguments\": { \"documentType\": \"{{input.documentType}}\" }
                    }
                },
                {
                    \"id\": \"generate_content\",
                    \"type\": \"LLM_CALL\",
                    \"config\": {
                        \"connection_id\": \"${CONNECTION_ID:-}\",
                        \"system\": \"You are an expert proposal writer for US government contracts (USFK). Generate professional proposal content in TipTap JSON format based on the RFP requirements, bid details, and template structure.\",
                        \"prompt\": \"Generate a {{input.documentType}} document.\\n\\nBid Request:\\n{{fetch_bid.output}}\\n\\nTemplate:\\n{{fetch_template.output}}\\n\\nOpportunity:\\n{{input.opportunityText}}\\n\\nRequirements:\\n{{input.requirements}}\\n\\nReturn JSON: { \\\"contentJson\\\": { \\\"type\\\": \\\"doc\\\", \\\"content\\\": [...] }, \\\"changeSummary\\\": \\\"...\\\" }\"
                    },
                    \"depends_on\": [\"fetch_bid\", \"fetch_template\"]
                },
                {
                    \"id\": \"save_document\",
                    \"type\": \"TOOL_USE\",
                    \"config\": {
                        \"tool_name\": \"save_document_version\",
                        \"arguments\": {
                            \"bidRequestId\": \"{{input.bidRequestId}}\",
                            \"documentType\": \"{{input.documentType}}\",
                            \"contentJson\": \"{{generate_content.output.contentJson}}\",
                            \"changeSummary\": \"{{generate_content.output.changeSummary}}\"
                        }
                    },
                    \"depends_on\": [\"generate_content\"]
                },
                {
                    \"id\": \"transition_state\",
                    \"type\": \"TOOL_USE\",
                    \"config\": {
                        \"tool_name\": \"transition_bid_state\",
                        \"arguments\": {
                            \"bidRequestId\": \"{{input.bidRequestId}}\",
                            \"targetState\": \"INTERNAL_REVIEW\",
                            \"notes\": \"AI document generation completed for {{input.documentType}}\"
                        }
                    },
                    \"depends_on\": [\"save_document\"]
                }
            ]
        }")
    WF_DOC_ID=$(echo "$WF_DOC_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('id',''))" 2>/dev/null || echo "")
    if [ -n "$WF_DOC_ID" ]; then
        echo "  생성 완료: ${WF_DOC_ID}"
    else
        echo "  FAILED. 응답: $(echo "$WF_DOC_RESPONSE" | head -c 200)"
    fi
fi

# ─── 5. 요약 ─────────────────────────────────────────────────────────────────
echo ""
echo "=== 셋업 완료 ==="
echo ""
echo "등록된 리소스:"
[ -n "${CONNECTION_ID:-}" ]  && echo "  Connection:  ${CONNECTION_ID}"
[ -n "${MCP_SERVER_ID:-}" ]  && echo "  MCP Server:  ${MCP_SERVER_ID}"
[ -n "${EXISTING_WF_REQ:-}${WF_REQ_ID:-}" ] && echo "  Workflow 1:  ${EXISTING_WF_REQ:-$WF_REQ_ID} (requirement-extraction)"
[ -n "${EXISTING_WF_DOC:-}${WF_DOC_ID:-}" ] && echo "  Workflow 2:  ${EXISTING_WF_DOC:-$WF_DOC_ID} (bid-document-generation)"
echo ""
echo "다음 단계:"
echo "  1. 플랫폼 기동: cd backend && SERVER_PORT=8088 ./gradlew bootRun"
echo "  2. Tool Discovery: curl -X POST ${AIMBASE_API}/mcp-servers/\${MCP_SERVER_ID}/discover -H 'X-API-Key: ${API_KEY}'"
echo "  3. E2E 테스트: 입찰 요청 → REQUIREMENT_ANALYSIS 전이 → 자동 추출 확인"
