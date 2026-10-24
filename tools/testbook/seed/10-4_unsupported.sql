-- @marker SEED-10-4
-- 대상 IT-10-045
-- 옛 워드 DOC 와 이미 읽은 PDF 를 낸 입찰 건 세 개. 전환하면 서버가 DOC 를 읽을 수 없는 형식으로 적는다. 1번은 화면 시험, 2번과 3번은 서버 판정 기대결과 2, 3 몫이다
-- @key 10.b045_virtual_bid SELECT '[[uuid:03:1]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:1]] AND state = 'DOCS_RECEIVED'
-- @key 10.b045_server2_bid SELECT '[[uuid:03:2]]' AS id, HEX(id) AS hex, '[[sha:3]]' AS doc_sha FROM bid_requests WHERE id = [[id:03:2]] AND state = 'DOCS_RECEIVED'
-- @key 10.b045_server3_bid SELECT '[[uuid:03:3]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:3]] AND state = 'DOCS_RECEIVED'
INSERT INTO members (id, email, company_name, role, created_at, updated_at)
VALUES ([[id:01:1]], '[[email:a]]', '시딩 회사 10-4', 'CUSTOMER', [[now]], [[now]]);
INSERT INTO opportunities (id, notice_id, title, active, created_at, updated_at)
VALUES ([[id:02:1]], '[[notice:1]]', 'Seed opportunity 10-4 A', 1, [[now]], [[now]]),
       ([[id:02:2]], '[[notice:2]]', 'Seed opportunity 10-4 B', 1, [[now]], [[now]]),
       ([[id:02:3]], '[[notice:3]]', 'Seed opportunity 10-4 C', 1, [[now]], [[now]]);
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
VALUES ([[id:03:1]], [[id:01:1]], [[id:02:1]], 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:2]], [[id:01:1]], [[id:02:2]], 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:3]], [[id:01:1]], [[id:02:3]], 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]]);
INSERT INTO client_documents (id, member_id, bid_request_id, file_name, storage_url, file_size, content_type, uploaded_at, content_sha256, created_at, updated_at)
VALUES ([[id:04:1]], [[id:01:1]], [[id:03:1]], '회사소개.doc', 'seed/10-4/1.doc', 1024, 'application/msword', [[now]], '[[sha:1]]', [[now]], [[now]]),
       ([[id:04:2]], [[id:01:1]], [[id:03:1]], '실적증명서.pdf', 'seed/10-4/2.pdf', 1024, 'application/pdf', [[now]], '[[sha:2]]', [[now]], [[now]]),
       ([[id:04:3]], [[id:01:1]], [[id:03:2]], '회사소개.doc', 'seed/10-4/3.doc', 1024, 'application/msword', [[now]], '[[sha:3]]', [[now]], [[now]]),
       ([[id:04:4]], [[id:01:1]], [[id:03:2]], '실적증명서.pdf', 'seed/10-4/4.pdf', 1024, 'application/pdf', [[now]], '[[sha:4]]', [[now]], [[now]]),
       ([[id:04:5]], [[id:01:1]], [[id:03:3]], '회사소개.doc', 'seed/10-4/5.doc', 1024, 'application/msword', [[now]], '[[sha:5]]', [[now]], [[now]]),
       ([[id:04:6]], [[id:01:1]], [[id:03:3]], '실적증명서.pdf', 'seed/10-4/6.pdf', 1024, 'application/pdf', [[now]], '[[sha:6]]', [[now]], [[now]]);
INSERT INTO document_readings (id, content_sha256, file_name, content_type, read_as, status, status_at, facts_json, fact_count, vision_used, failure_reason, read_at, created_at, updated_at)
VALUES ([[id:06:2]], '[[sha:2]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'DONE', [[now]], '[{"category":"PAST_PERFORMANCE","key":"사업명","value":"Camp Humphreys Plumbing Repair","page":"1","anchor":"Project: Camp Humphreys Plumbing Repair","confidence":"HIGH","readMode":"TEXT"},{"category":"PAST_PERFORMANCE","key":"발주처","value":"411th CSB","page":"1","anchor":"Client: 411th CSB","confidence":"HIGH","readMode":"TEXT"},{"category":"PAST_PERFORMANCE","key":"계약 금액","value":"USD 1,250,000","page":"1","anchor":"Amount: USD 1,250,000","confidence":"HIGH","readMode":"TEXT"}]', 3, 0, NULL, [[now]], [[now]], [[now]]),
       ([[id:06:4]], '[[sha:4]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'DONE', [[now]], '[{"category":"PAST_PERFORMANCE","key":"사업명","value":"Camp Humphreys Plumbing Repair","page":"1","anchor":"Project: Camp Humphreys Plumbing Repair","confidence":"HIGH","readMode":"TEXT"},{"category":"PAST_PERFORMANCE","key":"발주처","value":"411th CSB","page":"1","anchor":"Client: 411th CSB","confidence":"HIGH","readMode":"TEXT"},{"category":"PAST_PERFORMANCE","key":"계약 금액","value":"USD 1,250,000","page":"1","anchor":"Amount: USD 1,250,000","confidence":"HIGH","readMode":"TEXT"}]', 3, 0, NULL, [[now]], [[now]], [[now]]),
       ([[id:06:6]], '[[sha:6]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'DONE', [[now]], '[{"category":"PAST_PERFORMANCE","key":"사업명","value":"Camp Humphreys Plumbing Repair","page":"1","anchor":"Project: Camp Humphreys Plumbing Repair","confidence":"HIGH","readMode":"TEXT"},{"category":"PAST_PERFORMANCE","key":"발주처","value":"411th CSB","page":"1","anchor":"Client: 411th CSB","confidence":"HIGH","readMode":"TEXT"},{"category":"PAST_PERFORMANCE","key":"계약 금액","value":"USD 1,250,000","page":"1","anchor":"Amount: USD 1,250,000","confidence":"HIGH","readMode":"TEXT"}]', 3, 0, NULL, [[now]], [[now]], [[now]]);
