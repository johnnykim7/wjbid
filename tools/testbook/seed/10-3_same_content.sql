-- @marker SEED-10-3
-- 대상 IT-10-042
-- 서류함의 사업자등록증을 공고별 서류로 복사한 두 입찰 건. 그 내용은 앞 건에서 이미 읽음이라 전환해도 다시 읽지 않는다. 1번은 화면 시험, 2번은 서버 판정 몫이다
-- @key 10.b042_virtual_bid SELECT '[[uuid:03:1]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:1]] AND state = 'DOCS_RECEIVED'
-- @key 10.b042_server_bid SELECT '[[uuid:03:2]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:2]] AND state = 'DOCS_RECEIVED'
INSERT INTO members (id, email, company_name, role, created_at, updated_at)
VALUES ([[id:01:1]], '[[email:a]]', '시딩 회사 10-3', 'CUSTOMER', [[now]], [[now]]);
INSERT INTO opportunities (id, notice_id, title, active, created_at, updated_at)
VALUES ([[id:02:1]], '[[notice:1]]', 'Seed opportunity 10-3 A', 1, [[now]], [[now]]),
       ([[id:02:2]], '[[notice:2]]', 'Seed opportunity 10-3 B', 1, [[now]], [[now]]);
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
VALUES ([[id:03:1]], [[id:01:1]], [[id:02:1]], 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:2]], [[id:01:1]], [[id:02:2]], 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]]);
INSERT INTO member_documents (id, member_id, category, file_name, file_size, content_type, storage_url, created_at, updated_at, expires_on, content_sha256)
VALUES ([[id:05:1]], [[id:01:1]], 'BUSINESS_REGISTRATION', '사업자등록증.pdf', 1024, 'application/pdf', 'seed/10-3/1.pdf', [[now]], [[now]], NULL, '[[sha:1]]');
INSERT INTO client_documents (id, member_id, bid_request_id, file_name, storage_url, file_size, content_type, uploaded_at, content_sha256, created_at, updated_at)
VALUES ([[id:04:1]], [[id:01:1]], [[id:03:1]], '사업자등록증.pdf', 'seed/10-3/1.pdf', 1024, 'application/pdf', [[now]], '[[sha:1]]', [[now]], [[now]]),
       ([[id:04:2]], [[id:01:1]], [[id:03:2]], '사업자등록증.pdf', 'seed/10-3/1.pdf', 1024, 'application/pdf', [[now]], '[[sha:1]]', [[now]], [[now]]);
INSERT INTO document_readings (id, content_sha256, file_name, content_type, read_as, status, status_at, facts_json, fact_count, vision_used, failure_reason, read_at, created_at, updated_at)
VALUES ([[id:06:1]], '[[sha:1]]', '사업자등록증.pdf', 'application/pdf', 'PDF', 'DONE', [[now]], '[{"category":"COMPANY","key":"상호","value":"시딩 회사","page":"1","anchor":"상호: 시딩 회사","confidence":"HIGH","readMode":"TEXT"},{"category":"COMPANY","key":"사업자등록번호","value":"000-00-00000","page":"1","anchor":"등록번호: 000-00-00000","confidence":"HIGH","readMode":"TEXT"}]', 2, 0, NULL, [[now]], [[now]], [[now]]);
