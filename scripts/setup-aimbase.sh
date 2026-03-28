#!/usr/bin/env bash
#
# Aimbase 셋업 스크립트 (CR-002)
# 입찰제안시스템을 Aimbase에 연동하기 위한 초기 설정을 수행합니다.
#
# 사전 조건:
#   - Aimbase가 실행 중이어야 합니다
#   - API Key가 유효해야 합니다
#   - 입찰 플랫폼이 PLATFORM_HOST:8088에서 실행 중이어야 합니다 (MCP 서버 등록용)
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
echo "[1/6] Aimbase 연결 확인..."
HTTP_CODE=$(curl -s -o /dev/null -w "%{http_code}" "${HEADER[@]}" "${AIMBASE_API}/connections")
if [ "$HTTP_CODE" != "200" ]; then
    echo "ERROR: Aimbase 연결 실패 (HTTP ${HTTP_CODE})"
    exit 1
fi
echo "  OK (HTTP 200)"

# ─── 2. LLM Connection 생성 ──────────────────────────────────────────────────
echo ""
echo "[2/6] LLM Connection 확인/생성..."

# 기존 connection 확인
EXISTING_CONN=$(curl -s "${HEADER[@]}" "${AIMBASE_API}/connections" | \
    python3 -c "import sys,json; data=json.load(sys.stdin).get('data',[]); print(next((c['id'] for c in data if 'bidding' in c.get('name','').lower()), ''))" 2>/dev/null || echo "")

if [ -n "$EXISTING_CONN" ]; then
    echo "  기존 Connection 발견: ${EXISTING_CONN}"
    CONNECTION_ID="${EXISTING_CONN}"
else
    echo "  Connection이 없습니다. 수동으로 생성해주세요:"
    echo "  POST ${AIMBASE_API}/connections"
    echo '  { "name": "bidding-claude-sonnet", "type": "LLM", "provider": "ANTHROPIC", "config": { "apiKey": "sk-ant-...", "model": "claude-sonnet-4-20250514", "maxTokens": 4096 } }'
    echo ""
    echo "  또는 환경변수 LLM_CONNECTION_ID를 설정하세요."
    CONNECTION_ID="${LLM_CONNECTION_ID:-}"
fi

# ─── 3. MCP 서버 등록 ────────────────────────────────────────────────────────
echo ""
echo "[3/6] MCP 서버 등록..."

MCP_SSE_URL="http://${PLATFORM_HOST}:${PLATFORM_PORT}/api/mcp/sse"

# 기존 MCP 서버 확인
EXISTING_MCP=$(curl -s "${HEADER[@]}" "${AIMBASE_API}/mcp-servers" | \
    python3 -c "import sys,json; data=json.load(sys.stdin).get('data',[]); print(next((s['id'] for s in data if 'bidding' in s.get('name','').lower()), ''))" 2>/dev/null || echo "")

if [ -n "$EXISTING_MCP" ]; then
    echo "  기존 MCP 서버 발견: ${EXISTING_MCP}"
    MCP_SERVER_ID="${EXISTING_MCP}"
else
    echo "  MCP 서버 등록 중... (URL: ${MCP_SSE_URL})"
    MCP_RESPONSE=$(curl -s "${HEADER[@]}" -X POST "${AIMBASE_API}/mcp-servers" \
        -d "{
            \"name\": \"bidding-agency-mcp\",
            \"transport\": \"sse\",
            \"config\": {
                \"url\": \"${MCP_SSE_URL}\"
            },
            \"autoStart\": true
        }")
    MCP_SERVER_ID=$(echo "$MCP_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('id',''))" 2>/dev/null || echo "")

    if [ -n "$MCP_SERVER_ID" ]; then
        echo "  등록 완료: ${MCP_SERVER_ID}"
    else
        echo "  WARNING: MCP 서버 등록 실패. 응답: ${MCP_RESPONSE}"
    fi
fi

# ─── 4. Tool Discovery ───────────────────────────────────────────────────────
echo ""
echo "[4/6] Tool Discovery..."

if [ -n "$MCP_SERVER_ID" ]; then
    DISCOVER_RESPONSE=$(curl -s "${HEADER[@]}" -X POST "${AIMBASE_API}/mcp-servers/${MCP_SERVER_ID}/discover")
    TOOL_COUNT=$(echo "$DISCOVER_RESPONSE" | python3 -c "import sys,json; print(json.load(sys.stdin).get('data',{}).get('toolCount',0))" 2>/dev/null || echo "0")
    echo "  발견된 도구: ${TOOL_COUNT}개"

    if [ "$TOOL_COUNT" = "0" ]; then
        echo "  WARNING: 도구가 발견되지 않았습니다. 플랫폼이 실행 중인지 확인하세요."
        echo "  플랫폼 MCP SSE URL: ${MCP_SSE_URL}"
    fi
else
    echo "  SKIP: MCP 서버 ID가 없습니다."
fi

# ─── 5. 워크플로우 생성 ──────────────────────────────────────────────────────
echo ""
echo "[5/6] 워크플로우 확인/생성..."

# requirement-extraction 워크플로우
EXISTING_WF_REQ=$(curl -s "${HEADER[@]}" "${AIMBASE_API}/workflows" | \
    python3 -c "import sys,json; data=json.load(sys.stdin).get('data',[]); print(next((w['id'] for w in data if 'requirement' in w.get('name','').lower()), ''))" 2>/dev/null || echo "")

if [ -n "$EXISTING_WF_REQ" ]; then
    echo "  requirement-extraction 워크플로우 존재: ${EXISTING_WF_REQ}"
else
    echo "  requirement-extraction 워크플로우 생성 중..."
    WF_REQ_RESPONSE=$(curl -s "${HEADER[@]}" -X POST "${AIMBASE_API}/workflows" \
        -d "{
            \"name\": \"requirement-extraction\",
            \"domain\": \"bidding\",
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
                        \"connection_id\": \"${CONNECTION_ID}\",
                        \"system\": \"You are an expert government procurement analyst. Extract all requirements from the given RFP/opportunity text. Categorize each requirement as: DOCUMENT, FORMAT, SUBMISSION, DEADLINE, ELIGIBILITY, TECHNICAL, or OTHER. Identify blockers (mandatory requirements). Also extract submission format requirements (Word/PDF/page limits).\",
                        \"prompt\": \"Analyze the following opportunity and extract structured requirements:\\n\\nOpportunity Details:\\n{{fetch_opportunity.output}}\\n\\nFull Text:\\n{{input.opportunityText}}\\n\\nReturn a JSON array of requirements with fields: category, title, description, isBlocker (boolean).\",
                        \"response_schema\": {
                            \"type\": \"object\",
                            \"properties\": {
                                \"requirements\": {
                                    \"type\": \"array\",
                                    \"items\": {
                                        \"type\": \"object\",
                                        \"properties\": {
                                            \"category\": { \"type\": \"string\" },
                                            \"title\": { \"type\": \"string\" },
                                            \"description\": { \"type\": \"string\" },
                                            \"isBlocker\": { \"type\": \"boolean\" }
                                        }
                                    }
                                }
                            }
                        }
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
    echo "  생성 완료: ${WF_REQ_ID:-FAILED}"
fi

# bid-document-generation 워크플로우
EXISTING_WF_DOC=$(curl -s "${HEADER[@]}" "${AIMBASE_API}/workflows" | \
    python3 -c "import sys,json; data=json.load(sys.stdin).get('data',[]); print(next((w['id'] for w in data if 'document' in w.get('name','').lower()), ''))" 2>/dev/null || echo "")

if [ -n "$EXISTING_WF_DOC" ]; then
    echo "  bid-document-generation 워크플로우 존재: ${EXISTING_WF_DOC}"
else
    echo "  bid-document-generation 워크플로우 생성 중..."
    WF_DOC_RESPONSE=$(curl -s "${HEADER[@]}" -X POST "${AIMBASE_API}/workflows" \
        -d "{
            \"name\": \"bid-document-generation\",
            \"domain\": \"bidding\",
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
                        \"connection_id\": \"${CONNECTION_ID}\",
                        \"system\": \"You are an expert proposal writer for US government contracts (USFK). Generate professional proposal content based on the RFP requirements, bid request details, and document template. Output in TipTap JSON format.\",
                        \"prompt\": \"Generate a {{input.documentType}} document for this bid request.\\n\\nBid Request:\\n{{fetch_bid.output}}\\n\\nTemplate Structure:\\n{{fetch_template.output}}\\n\\nOpportunity Text:\\n{{input.opportunityText}}\\n\\nRequirements:\\n{{input.requirements}}\\n\\nGenerate comprehensive, professional content in TipTap JSON format.\",
                        \"response_schema\": {
                            \"type\": \"object\",
                            \"properties\": {
                                \"contentJson\": { \"type\": \"object\" },
                                \"changeSummary\": { \"type\": \"string\" }
                            }
                        }
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
    echo "  생성 완료: ${WF_DOC_ID:-FAILED}"
fi

# ─── 6. 요약 ─────────────────────────────────────────────────────────────────
echo ""
echo "=== 셋업 완료 ==="
echo ""
echo "다음 환경변수를 application.yml 또는 환경에 설정하세요:"
echo "  AIMBASE_URL=${AIMBASE_URL}"
echo "  AIMBASE_API_KEY=${API_KEY}"
[ -n "${CONNECTION_ID:-}" ] && echo "  LLM_CONNECTION_ID=${CONNECTION_ID}"
[ -n "${MCP_SERVER_ID:-}" ] && echo "  AIMBASE_MCP_SERVER_ID=${MCP_SERVER_ID}"
echo ""
echo "주의사항:"
echo "  - LLM Connection에 실제 API Key가 설정되어야 합니다"
echo "  - 플랫폼이 ${PLATFORM_HOST}:${PLATFORM_PORT}에서 실행 중이어야 MCP 연동이 작동합니다"
echo "  - Tool Discovery는 플랫폼 실행 후 다시 실행할 수 있습니다:"
echo "    curl -X POST ${AIMBASE_API}/mcp-servers/${MCP_SERVER_ID:-<ID>}/discover -H 'X-API-Key: ${API_KEY}'"
