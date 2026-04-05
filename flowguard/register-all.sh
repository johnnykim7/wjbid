#!/bin/bash
# FlowGuard Step 일괄 등록 스크립트
# 사용법: ./flowguard/register-all.sh

set -e

FG_BASE="http://localhost:8180/api"
FG_EMAIL="admin@flowguard.dev"
FG_PASSWORD="admin1234"

echo "=== FlowGuard 로그인 ==="
TOKEN=$(curl -s -X POST "$FG_BASE/auth/login" \
  -H "Content-Type: application/json" \
  -d "{\"email\":\"$FG_EMAIL\",\"password\":\"$FG_PASSWORD\"}" \
  | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['accessToken'])")
AUTH="Authorization: Bearer $TOKEN"
echo "Token acquired (${#TOKEN} chars)"

echo ""
echo "=== 1. 솔루션 등록 ==="
SOLUTION_ID=$(curl -s -X POST "$FG_BASE/solutions" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d '{
    "name": "입찰대행플랫폼",
    "description": "SAM.gov 미군(USFK) 정부 조달 입찰 AI 대행 서비스",
    "env": "DEV",
    "type": "INTERNAL",
    "baseUrl": "http://host.docker.internal:8080/api",
    "healthCheckUrl": "/health"
  }' | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
echo "Solution ID: $SOLUTION_ID"

echo ""
echo "=== 2. 커넥터 등록 ==="
# API 커넥터
API_CONN_ID=$(curl -s -X POST "$FG_BASE/connectors" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{
    \"solutionId\": \"$SOLUTION_ID\",
    \"name\": \"bidding-api\",
    \"type\": \"API_ACTION\",
    \"defaultMode\": \"REAL\",
    \"config\": {\"baseUrl\": \"http://host.docker.internal:8080/api\", \"timeout\": 30}
  }" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
echo "API Connector: $API_CONN_ID"

# Customer Portal UI 커넥터
PORTAL_CONN_ID=$(curl -s -X POST "$FG_BASE/connectors" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{
    \"solutionId\": \"$SOLUTION_ID\",
    \"name\": \"customer-portal\",
    \"type\": \"UI_SMOKE\",
    \"defaultMode\": \"REAL\",
    \"config\": {\"baseUrl\": \"http://host.docker.internal:5173\"}
  }" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
echo "Portal UI Connector: $PORTAL_CONN_ID"

# Admin Console UI 커넥터
ADMIN_CONN_ID=$(curl -s -X POST "$FG_BASE/connectors" \
  -H "$AUTH" -H "Content-Type: application/json" \
  -d "{
    \"solutionId\": \"$SOLUTION_ID\",
    \"name\": \"admin-console\",
    \"type\": \"UI_SMOKE\",
    \"defaultMode\": \"REAL\",
    \"config\": {\"baseUrl\": \"http://host.docker.internal:5174\"}
  }" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])")
echo "Admin UI Connector: $ADMIN_CONN_ID"

echo ""
echo "=== 3. Step 일괄 등록 ==="
STEPS_DIR="$(dirname "$0")/steps"
STEP_IDS=""

register_step() {
  local key=$1
  local name=$2
  local type=$3
  local file=$4
  local conn_id=$5
  local tags=$6

  local dsl=$(cat "$STEPS_DIR/$file" | python3 -c "import sys,json; print(json.dumps(json.load(sys.stdin)))")

  local result=$(curl -s -X POST "$FG_BASE/steps" \
    -H "$AUTH" -H "Content-Type: application/json" \
    -d "{
      \"solutionId\": \"$SOLUTION_ID\",
      \"connectorId\": \"$conn_id\",
      \"key\": \"$key\",
      \"name\": \"$name\",
      \"type\": \"$type\",
      \"tags\": \"$tags\",
      \"dslJson\": $dsl
    }")

  local step_id=$(echo "$result" | python3 -c "import sys,json; print(json.load(sys.stdin)['data']['id'])" 2>/dev/null || echo "FAIL")
  echo "  [$step_id] $key — $name"
  STEP_IDS="$STEP_IDS $step_id"
}

echo "--- 인증 ---"
register_step "auth.register" "회원가입" "ACTION" "auth.register.json" "$API_CONN_ID" "auth,biz"
register_step "auth.login" "로그인" "ACTION" "auth.login.json" "$API_CONN_ID" "auth,biz"
register_step "auth.refresh-token" "토큰 갱신" "ACTION" "auth.refresh-token.json" "$API_CONN_ID" "auth,biz"
register_step "auth.me" "내 정보 조회" "ACTION" "auth.me.json" "$API_CONN_ID" "auth,biz"
register_step "auth.duplicate-register" "BIZ-008 중복이메일거부" "ACTION" "auth.duplicate-register.json" "$API_CONN_ID" "auth,biz,negative"

echo "--- 공고 ---"
register_step "opportunity.list" "공고 목록 조회" "ACTION" "opportunity.list.json" "$API_CONN_ID" "opportunity,biz"
register_step "opportunity.detail" "공고 상세 조회" "ACTION" "opportunity.detail.json" "$API_CONN_ID" "opportunity,biz"
register_step "opportunity.search" "공고 키워드 검색" "ACTION" "opportunity.search.json" "$API_CONN_ID" "opportunity,biz"
register_step "opportunity.near-deadline" "마감 임박 공고" "ACTION" "opportunity.near-deadline.json" "$API_CONN_ID" "opportunity,biz"
register_step "bookmark.add" "즐겨찾기 추가" "ACTION" "bookmark.add.json" "$API_CONN_ID" "bookmark,biz"
register_step "bookmark.list" "즐겨찾기 목록" "ACTION" "bookmark.list.json" "$API_CONN_ID" "bookmark,biz"

echo "--- 입찰 FSM ---"
register_step "bid.create-request" "입찰 신청" "ACTION" "bid.create-request.json" "$API_CONN_ID" "bid,fsm,biz"
register_step "bid.transition-to-docs-pending" "FSM: CREATED→DOCS_PENDING" "ACTION" "bid.transition-to-docs-pending.json" "$API_CONN_ID" "bid,fsm"
register_step "bid.transition-to-docs-received" "FSM: DOCS_PENDING→DOCS_RECEIVED" "ACTION" "bid.transition-to-docs-received.json" "$API_CONN_ID" "bid,fsm"
register_step "bid.transition-to-analyzing" "FSM: DOCS_RECEIVED→ANALYZING" "ACTION" "bid.transition-to-analyzing.json" "$API_CONN_ID" "bid,fsm"
register_step "bid.transition-to-generating" "FSM: ANALYZING→GENERATING" "ACTION" "bid.transition-to-generating.json" "$API_CONN_ID" "bid,fsm"
register_step "bid.transition-to-review" "FSM: GENERATING→REVIEW" "ACTION" "bid.transition-to-review.json" "$API_CONN_ID" "bid,fsm"
register_step "bid.transition-to-confirmed" "FSM: REVIEW→CONFIRMED" "ACTION" "bid.transition-to-confirmed.json" "$API_CONN_ID" "bid,fsm"
register_step "bid.transition-to-submitted" "FSM: CONFIRMED→SUBMITTED" "ACTION" "bid.transition-to-submitted.json" "$API_CONN_ID" "bid,fsm"
register_step "bid.forbidden-transition" "BIZ-001 금지전이거부" "ACTION" "bid.forbidden-transition.json" "$API_CONN_ID" "bid,fsm,biz,negative"
register_step "bid.duplicate-request" "BIZ-006 중복신청거부" "ACTION" "bid.duplicate-request.json" "$API_CONN_ID" "bid,biz,negative"
register_step "bid.verify-state-history" "상태 전이 이력 확인" "ACTION" "bid.verify-state-history.json" "$API_CONN_ID" "bid,fsm"
register_step "bid.verify-validation-api" "Validation API 상태 검증" "ACTION" "bid.verify-validation-api.json" "$API_CONN_ID" "bid,validation"

echo "--- 문서 ---"
register_step "doc.template-list" "문서 템플릿 목록" "ACTION" "doc.template-list.json" "$API_CONN_ID" "doc,biz"
register_step "doc.bid-documents-list" "입찰 문서 목록" "ACTION" "doc.bid-documents-list.json" "$API_CONN_ID" "doc,biz"
register_step "doc.version-list" "문서 버전 목록 (BIZ-002)" "ACTION" "doc.version-list.json" "$API_CONN_ID" "doc,biz"
register_step "doc.lock" "문서 잠금 (BIZ-003)" "ACTION" "doc.lock.json" "$API_CONN_ID" "doc,biz"
register_step "doc.locked-edit-rejected" "LOCKED 편집 거부 (BIZ-003)" "ACTION" "doc.locked-edit-rejected.json" "$API_CONN_ID" "doc,biz,negative"
register_step "doc.unlock" "문서 잠금 해제" "ACTION" "doc.unlock.json" "$API_CONN_ID" "doc,biz"

echo "--- MCP ---"
register_step "mcp.initialize" "MCP initialize" "ACTION" "mcp.initialize.json" "$API_CONN_ID" "mcp"
register_step "mcp.tools-list" "MCP tools/list (8개)" "ACTION" "mcp.tools-list.json" "$API_CONN_ID" "mcp"
register_step "mcp.search-opportunities" "MCP search_opportunities" "ACTION" "mcp.search-opportunities.json" "$API_CONN_ID" "mcp"
register_step "mcp.get-opportunity" "MCP get_opportunity" "ACTION" "mcp.get-opportunity.json" "$API_CONN_ID" "mcp"
register_step "mcp.get-bid-request" "MCP get_bid_request" "ACTION" "mcp.get-bid-request.json" "$API_CONN_ID" "mcp"
register_step "mcp.get-document-template" "MCP get_document_template" "ACTION" "mcp.get-document-template.json" "$API_CONN_ID" "mcp"
register_step "mcp.invalid-tool" "MCP 무효Tool 에러 (BIZ-013)" "ACTION" "mcp.invalid-tool.json" "$API_CONN_ID" "mcp,negative"
register_step "mcp.unsupported-method" "MCP 미지원메서드 에러" "ACTION" "mcp.unsupported-method.json" "$API_CONN_ID" "mcp,negative"

echo "--- 헬스체크/Evidence ---"
register_step "bidding.health-check" "헬스체크" "ACTION" "bidding.health-check.json" "$API_CONN_ID" "smoke,infra"
register_step "bidding.verify-sam-gov-evidence" "SAM.gov Evidence 검증" "PROBE_LOG" "bidding.verify-sam-gov-evidence.json" "$API_CONN_ID" "evidence"
register_step "bidding.verify-documents" "문서 Validation API" "ACTION" "bidding.verify-documents.json" "$API_CONN_ID" "validation"

echo "--- Customer Portal UI ---"
register_step "ui.portal.home" "[Portal] 메인" "UI_SMOKE" "ui.portal.home.json" "$PORTAL_CONN_ID" "ui,portal,smoke"
register_step "ui.portal.login" "[Portal] 로그인" "UI_SMOKE" "ui.portal.login.json" "$PORTAL_CONN_ID" "ui,portal,smoke"
register_step "ui.portal.register" "[Portal] 회원가입" "UI_SMOKE" "ui.portal.register.json" "$PORTAL_CONN_ID" "ui,portal,smoke"
register_step "ui.portal.search" "[Portal] 공고 검색" "UI_SMOKE" "ui.portal.search.json" "$PORTAL_CONN_ID" "ui,portal,smoke"
register_step "ui.portal.pricing" "[Portal] 요금 안내" "UI_SMOKE" "ui.portal.pricing.json" "$PORTAL_CONN_ID" "ui,portal,smoke"
register_step "ui.portal.guide" "[Portal] 이용 가이드" "UI_SMOKE" "ui.portal.guide.json" "$PORTAL_CONN_ID" "ui,portal,smoke"
register_step "ui.portal.proposals" "[Portal] 내 입찰" "UI_SMOKE" "ui.portal.proposals.json" "$PORTAL_CONN_ID" "ui,portal,smoke"
register_step "ui.portal.bookmarks" "[Portal] 즐겨찾기" "UI_SMOKE" "ui.portal.bookmarks.json" "$PORTAL_CONN_ID" "ui,portal,smoke"
register_step "ui.portal.profile" "[Portal] 프로필" "UI_SMOKE" "ui.portal.profile.json" "$PORTAL_CONN_ID" "ui,portal,smoke"

echo "--- Admin Console UI ---"
register_step "ui.admin.login" "[Admin] 로그인" "UI_SMOKE" "ui.admin.login.json" "$ADMIN_CONN_ID" "ui,admin,smoke"
register_step "ui.admin.dashboard" "[Admin] 대시보드" "UI_SMOKE" "ui.admin.dashboard.json" "$ADMIN_CONN_ID" "ui,admin,smoke"
register_step "ui.admin.bid-requests" "[Admin] 입찰 관리" "UI_SMOKE" "ui.admin.bid-requests.json" "$ADMIN_CONN_ID" "ui,admin,smoke"
register_step "ui.admin.collection" "[Admin] 공고 수집" "UI_SMOKE" "ui.admin.collection.json" "$ADMIN_CONN_ID" "ui,admin,smoke"
register_step "ui.admin.templates" "[Admin] 문서 템플릿" "UI_SMOKE" "ui.admin.templates.json" "$ADMIN_CONN_ID" "ui,admin,smoke"
register_step "ui.admin.members" "[Admin] 회원 관리" "UI_SMOKE" "ui.admin.members.json" "$ADMIN_CONN_ID" "ui,admin,smoke"

echo ""
echo "=== 등록 완료 ==="
echo "총 Step 수: $(echo $STEP_IDS | wc -w | tr -d ' ')"
echo "Solution ID: $SOLUTION_ID"
echo "API Connector: $API_CONN_ID"
echo "Portal Connector: $PORTAL_CONN_ID"
echo "Admin Connector: $ADMIN_CONN_ID"
