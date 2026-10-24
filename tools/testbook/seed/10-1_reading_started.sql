-- @marker SEED-10-1
-- 대상 IT-10-041
-- 분석 단계로 넘기기 전 입찰 건. 두 서류는 이미 읽는 중이라 전환 직후 읽는 중으로 보이고, 만료일이 어제인 보험증권은 영역에 없어야 한다
-- @key 10.b041_before_bid SELECT '[[uuid:03:1]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:1]] AND state = 'DOCS_RECEIVED'
INSERT INTO members (id, email, company_name, role, created_at, updated_at)
VALUES ([[id:01:1]], '[[email:a]]', '시딩 회사 10-1', 'CUSTOMER', [[now]], [[now]]);
INSERT INTO opportunities (id, notice_id, title, active, created_at, updated_at)
VALUES ([[id:02:1]], '[[notice:1]]', 'Seed opportunity 10-1', 1, [[now]], [[now]]);
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
VALUES ([[id:03:1]], [[id:01:1]], [[id:02:1]], 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]]);
INSERT INTO client_documents (id, member_id, bid_request_id, file_name, storage_url, file_size, content_type, uploaded_at, content_sha256, created_at, updated_at)
VALUES ([[id:04:1]], [[id:01:1]], [[id:03:1]], '실적증명서.pdf', 'seed/10-1/1.pdf', 1024, 'application/pdf', [[now]], '[[sha:1]]', [[now]], [[now]]);
INSERT INTO member_documents (id, member_id, category, file_name, file_size, content_type, storage_url, created_at, updated_at, expires_on, content_sha256)
VALUES ([[id:05:1]], [[id:01:1]], 'BUSINESS_REGISTRATION', '사업자등록증.pdf', 1024, 'application/pdf', 'seed/10-1/2.pdf', [[now]], [[now]], NULL, '[[sha:2]]'),
       ([[id:05:2]], [[id:01:1]], 'OTHER', '보험증권.pdf', 1024, 'application/pdf', 'seed/10-1/3.pdf', [[now]], [[now]], DATE_SUB(CURDATE(), INTERVAL 1 DAY), '[[sha:3]]');
INSERT INTO document_readings (id, content_sha256, file_name, content_type, read_as, status, status_at, fact_count, vision_used, created_at, updated_at)
VALUES ([[id:06:1]], '[[sha:1]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'READING', [[now]], 0, 0, [[now]], [[now]]),
       ([[id:06:2]], '[[sha:2]]', '사업자등록증.pdf', 'application/pdf', 'PDF', 'READING', [[now]], 0, 0, [[now]], [[now]]);
