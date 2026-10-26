-- @marker SEED-10-11
-- 대상 IT-10-055 IT-10-056 IT-10-057 IT-10-059
-- 입찰 분석이 시작되는 장면. 1, 2번은 사전 분석이 끝난 실제 공고(그 업종의 미 연방 계약이 있는 것)에 건 서류 접수 완료 건으로
-- 분석 단계로 넘기면 서류를 읽은 뒤 분석이 돈다. 맞는 공고가 없으면 입찰 건이 생기지 않아 키가 비고 재료 없음이 된다. 공고는 지우지 않는다
-- 1번 회차가 끝나면 IT-10-059 가 그 산출물(워크플로우가 쓴 것)을 본다. 시딩 본문을 보면 시딩 값을 확인하는 것이라 쓰지 않는다
-- 1번 회원은 그 공고의 참가 자격 첫 두 항목에 답해 둔다. IT-10-059 6번이 참가 자격 답에서 온 값의 출처를 보려면 답이 있어야 한다
-- 3, 4번은 읽을 서류가 없는 건, 5, 6번은 같은 내용 서류를 올린 같은 회원의 두 건, 7, 8번은 두 건 모두 분석 중인 화면 시험 몫이다
-- 분석 중 회차는 워크플로우 대기 한도(10/25 상용 60분) 안이어야 하므로 키는 50분 안에 시작한 것만 준다. 대기 한도를 50분 아래로 줄이면 이 창도 줄인다
-- @file seed/10-11/1.pdf past-performance.pdf
-- @file seed/10-11/2.pdf past-performance.pdf
-- @file seed/10-11/5.pdf past-performance.pdf
-- @file seed/10-11/6.pdf past-performance.pdf
-- @key 10.b055_server_bid SELECT '[[uuid:03:1]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:1]]
-- @key 10.b055_virtual_bid SELECT '[[uuid:03:2]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:2]] AND state = 'DOCS_RECEIVED'
-- @key 10.b056_server_bid SELECT '[[uuid:03:3]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:3]]
-- @key 10.b056_virtual_bid SELECT '[[uuid:03:4]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:4]] AND state = 'DOCS_RECEIVED'
-- @key 10.b057_server SELECT '[[uuid:03:5]]' AS id, HEX(id) AS hex, '[[uuid:03:6]]' AS b, HEX([[id:03:6]]) AS b_hex, '[[sha:5]]' AS sha FROM bid_requests WHERE id = [[id:03:5]]
-- @key 10.b057_virtual_a SELECT '[[uuid:03:7]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:7]] AND EXISTS (SELECT 1 FROM bid_analyses ba WHERE ba.id = [[id:0c:1]] AND ba.status = 'RUNNING' AND ba.started_at >= [[now]] - INTERVAL 50 MINUTE)
-- @key 10.b057_virtual_b SELECT '[[uuid:03:8]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:8]] AND EXISTS (SELECT 1 FROM bid_analyses ba WHERE ba.id = [[id:0c:2]] AND ba.status = 'RUNNING' AND ba.started_at >= [[now]] - INTERVAL 50 MINUTE)
INSERT INTO members (id, email, company_name, role, created_at, updated_at)
VALUES ([[id:01:1]], '[[email:a]]', '시딩 회사 10-11', 'CUSTOMER', [[now]], [[now]]),
       ([[id:01:2]], '[[email:b]]', '시딩 회사 10-11', 'CUSTOMER', [[now]], [[now]]),
       ([[id:01:3]], '[[email:c]]', '시딩 회사 10-11', 'CUSTOMER', [[now]], [[now]]),
       ([[id:01:4]], '[[email:d]]', '시딩 회사 10-11', 'CUSTOMER', [[now]], [[now]]),
       ([[id:01:5]], '[[email:e]]', '시딩 회사 10-11', 'CUSTOMER', [[now]], [[now]]);
INSERT INTO opportunities (id, notice_id, title, active, created_at, updated_at)
VALUES ([[id:02:1]], '[[notice:1]]', 'Seed opportunity 10-11 1', 1, [[now]], [[now]]),
       ([[id:02:2]], '[[notice:2]]', 'Seed opportunity 10-11 2', 1, [[now]], [[now]]),
       ([[id:02:3]], '[[notice:3]]', 'Seed opportunity 10-11 3', 1, [[now]], [[now]]),
       ([[id:02:4]], '[[notice:4]]', 'Seed opportunity 10-11 4', 1, [[now]], [[now]]),
       ([[id:02:5]], '[[notice:5]]', 'Seed opportunity 10-11 5', 1, [[now]], [[now]]),
       ([[id:02:6]], '[[notice:6]]', 'Seed opportunity 10-11 6', 1, [[now]], [[now]]);
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
SELECT [[id:03:1]], [[id:01:1]], x.id, 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]] FROM (SELECT o.id FROM opportunities o WHERE o.deleted_at IS NULL AND EXISTS (SELECT 1 FROM notices n WHERE n.opportunity_id = o.id AND n.generation_status = 'COMPLETED' AND n.visibility = 'VISIBLE') AND EXISTS (SELECT 1 FROM federal_contracts fc WHERE fc.naics_code = JSON_UNQUOTE(JSON_EXTRACT(o.raw_json, '$.naicsCode'))) ORDER BY o.updated_at DESC LIMIT 1) x;
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
SELECT [[id:03:2]], [[id:01:2]], x.id, 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]] FROM (SELECT o.id FROM opportunities o WHERE o.deleted_at IS NULL AND EXISTS (SELECT 1 FROM notices n WHERE n.opportunity_id = o.id AND n.generation_status = 'COMPLETED' AND n.visibility = 'VISIBLE') AND EXISTS (SELECT 1 FROM federal_contracts fc WHERE fc.naics_code = JSON_UNQUOTE(JSON_EXTRACT(o.raw_json, '$.naicsCode'))) ORDER BY o.updated_at DESC LIMIT 1) x;
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
VALUES ([[id:03:3]], [[id:01:3]], [[id:02:1]], 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:4]], [[id:01:3]], [[id:02:2]], 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:5]], [[id:01:4]], [[id:02:3]], 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:6]], [[id:01:4]], [[id:02:4]], 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:7]], [[id:01:5]], [[id:02:5]], 'ANALYZING', JSON_ARRAY(JSON_OBJECT('fromState', 'DOCS_RECEIVED', 'toState', 'ANALYZING', 'userId', NULL, 'username', 'seed', 'timestamp', JSON_ARRAY(YEAR([[now]] - INTERVAL 3 HOUR), MONTH([[now]] - INTERVAL 3 HOUR), DAY([[now]] - INTERVAL 3 HOUR), HOUR([[now]] - INTERVAL 3 HOUR), MINUTE([[now]] - INTERVAL 3 HOUR), SECOND([[now]] - INTERVAL 3 HOUR), 0), 'notes', '시딩', 'automatic', FALSE)), 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:8]], [[id:01:5]], [[id:02:6]], 'ANALYZING', JSON_ARRAY(JSON_OBJECT('fromState', 'DOCS_RECEIVED', 'toState', 'ANALYZING', 'userId', NULL, 'username', 'seed', 'timestamp', JSON_ARRAY(YEAR([[now]] - INTERVAL 3 HOUR), MONTH([[now]] - INTERVAL 3 HOUR), DAY([[now]] - INTERVAL 3 HOUR), HOUR([[now]] - INTERVAL 3 HOUR), MINUTE([[now]] - INTERVAL 3 HOUR), SECOND([[now]] - INTERVAL 3 HOUR), 0), 'notes', '시딩', 'automatic', FALSE)), 'FULL_PACKAGE', [[now]], [[now]]);
INSERT INTO client_documents (id, member_id, bid_request_id, file_name, storage_url, file_size, content_type, uploaded_at, content_sha256, created_at, updated_at)
SELECT [[id:04:1]], member_id, id, '실적증명서.pdf', 'seed/10-11/1.pdf', 1338, 'application/pdf', [[now]], '[[sha:1]]', [[now]], [[now]] FROM bid_requests WHERE id = [[id:03:1]]
UNION ALL SELECT [[id:04:2]], member_id, id, '실적증명서.pdf', 'seed/10-11/2.pdf', 1338, 'application/pdf', [[now]], '[[sha:2]]', [[now]], [[now]] FROM bid_requests WHERE id = [[id:03:2]];
INSERT INTO client_documents (id, member_id, bid_request_id, file_name, storage_url, file_size, content_type, uploaded_at, content_sha256, created_at, updated_at)
VALUES ([[id:04:5]], [[id:01:4]], [[id:03:5]], '실적증명서.pdf', 'seed/10-11/5.pdf', 1338, 'application/pdf', [[now]], '[[sha:5]]', [[now]], [[now]]),
       ([[id:04:6]], [[id:01:4]], [[id:03:6]], '실적증명서.pdf', 'seed/10-11/6.pdf', 1338, 'application/pdf', [[now]], '[[sha:5]]', [[now]], [[now]]);
INSERT INTO bid_analyses (id, bid_request_id, status, started_at, started_by, created_at, updated_at)
SELECT [[id:0c:1]], br.id, 'RUNNING', [[now]], (SELECT a.id FROM members a WHERE a.email = 'smoke-admin@example.com'), [[now]], [[now]] FROM bid_requests br WHERE br.id = [[id:03:7]];
INSERT INTO bid_analyses (id, bid_request_id, status, started_at, started_by, created_at, updated_at)
SELECT [[id:0c:2]], br.id, 'RUNNING', [[now]], (SELECT a.id FROM members a WHERE a.email = 'smoke-admin@example.com'), [[now]], [[now]] FROM bid_requests br WHERE br.id = [[id:03:8]];
INSERT INTO eligibility_answers (id, member_id, notice_id, item_no, item_title, answer, created_at, updated_at)
SELECT [[id:0e:1]], br.member_id, n.id, 0, JSON_UNQUOTE(JSON_EXTRACT(n.required_documents_json, '$.eligibility[0].title')), 'HAVE', [[now]], [[now]]
FROM bid_requests br JOIN notices n ON n.opportunity_id = br.opportunity_id AND n.visibility = 'VISIBLE' AND n.generation_status = 'COMPLETED'
WHERE br.id = [[id:03:1]] AND JSON_EXTRACT(n.required_documents_json, '$.eligibility[0].title') IS NOT NULL
ORDER BY n.analyzed_at DESC LIMIT 1;
INSERT INTO eligibility_answers (id, member_id, notice_id, item_no, item_title, answer, created_at, updated_at)
SELECT [[id:0e:2]], br.member_id, n.id, 1, JSON_UNQUOTE(JSON_EXTRACT(n.required_documents_json, '$.eligibility[1].title')), 'LACK', [[now]], [[now]]
FROM bid_requests br JOIN notices n ON n.opportunity_id = br.opportunity_id AND n.visibility = 'VISIBLE' AND n.generation_status = 'COMPLETED'
WHERE br.id = [[id:03:1]] AND JSON_EXTRACT(n.required_documents_json, '$.eligibility[1].title') IS NOT NULL
ORDER BY n.analyzed_at DESC LIMIT 1;
