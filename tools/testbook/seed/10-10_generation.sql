-- @marker SEED-10-10
-- 대상 IT-10-001
-- 실제 공고 가운데 사전 분석이 끝나 노출 중이고 그 유형의 성공 제안서가 있는 것을 골라 시딩 회원의 분석 단계 입찰 건을 만든다.
-- 서류 하나는 이미 읽음이다. 맞는 공고가 없으면 입찰 건이 생기지 않아 키가 비고 재료 없음이 된다. 공고는 지우지 않는다
-- 읽음 서류는 저장소에도 실제 PDF 를 놓는다. 제안서 작성이 서류 본문을 내려받아도 비지 않게 한다
-- 문서 작성 중으로 넘기려면 지금 분석의 관리자 결정이 Go 여야 해서(CR-170, BIZ-092) 결정만 있는 회차를 둔다
-- @file seed/10-10/1.pdf past-performance.pdf
-- @key 10.b001_bid SELECT '[[uuid:03:1]]' AS id, HEX(id) AS hex, state, JSON_LENGTH(state_history) AS hist FROM bid_requests WHERE id = [[id:03:1]]
INSERT INTO members (id, email, company_name, role, created_at, updated_at)
VALUES ([[id:01:1]], '[[email:a]]', '시딩 회사 10-10', 'CUSTOMER', [[now]], [[now]]);
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
SELECT [[id:03:1]], [[id:01:1]], o.id, 'ANALYZING', '[]', 'FULL_PACKAGE', [[now]], [[now]] FROM opportunities o
WHERE o.industry_type IS NOT NULL AND o.deleted_at IS NULL
  AND EXISTS (SELECT 1 FROM notices n WHERE n.opportunity_id = o.id AND n.generation_status = 'COMPLETED' AND n.visibility = 'VISIBLE')
  AND EXISTS (SELECT 1 FROM rfp_sample s WHERE s.industry_type = o.industry_type AND s.use_for_pattern = 1)
ORDER BY o.updated_at DESC LIMIT 1;
INSERT INTO client_documents (id, member_id, bid_request_id, file_name, storage_url, file_size, content_type, uploaded_at, content_sha256, created_at, updated_at)
SELECT [[id:04:1]], [[id:01:1]], id, '실적증명서.pdf', 'seed/10-10/1.pdf', 1338, 'application/pdf', [[now]], '[[sha:1]]', [[now]], [[now]] FROM bid_requests WHERE id = [[id:03:1]];
INSERT INTO document_readings (id, content_sha256, file_name, content_type, read_as, status, status_at, facts_json, fact_count, vision_used, failure_reason, read_at, created_at, updated_at)
VALUES ([[id:06:1]], '[[sha:1]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'DONE', [[now]], '[{"category":"PAST_PERFORMANCE","key":"사업명","value":"Camp Humphreys Plumbing Repair","page":"1","anchor":"Project: Camp Humphreys Plumbing Repair","confidence":"HIGH","readMode":"TEXT"},{"category":"PAST_PERFORMANCE","key":"발주처","value":"411th CSB","page":"1","anchor":"Client: 411th CSB","confidence":"HIGH","readMode":"TEXT"},{"category":"PAST_PERFORMANCE","key":"계약 금액","value":"USD 1,250,000","page":"1","anchor":"Amount: USD 1,250,000","confidence":"HIGH","readMode":"TEXT"}]', 3, 0, NULL, [[now]], [[now]], [[now]]);
INSERT INTO bid_analyses (id, bid_request_id, status, started_at, admin_decision, decided_by, decided_at, created_at, updated_at)
SELECT [[id:0c:1]], br.id, 'DECISION_ONLY', [[now]], 'GO', (SELECT a.id FROM members a WHERE a.email = 'smoke-admin@example.com'), [[now]], [[now]], [[now]] FROM bid_requests br WHERE br.id = [[id:03:1]];
