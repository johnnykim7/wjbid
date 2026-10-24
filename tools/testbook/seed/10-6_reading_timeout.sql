-- @marker SEED-10-6
-- 대상 IT-10-047
-- 분석 단계에서 하루 전부터 읽는 중으로 멈춘 서류가 있는 입찰 건 두 개. 1번은 화면 시험, 2번은 서버 판정 몫이다
-- 초과 기준은 워크플로우 대기 한도(설정 간격과 횟수)라 서버마다, 설정을 바꿀 때마다 달라진다. 어느 한도보다도 오래된 하루 전으로 둔다
-- @key 10.b047_virtual_bid SELECT '[[uuid:03:1]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:1]] AND state = 'ANALYZING'
-- @key 10.b047_server_bid SELECT '[[uuid:03:2]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:2]] AND state = 'ANALYZING'
INSERT INTO members (id, email, company_name, role, created_at, updated_at)
VALUES ([[id:01:1]], '[[email:a]]', '시딩 회사 10-6', 'CUSTOMER', [[now]], [[now]]);
INSERT INTO opportunities (id, notice_id, title, active, created_at, updated_at)
VALUES ([[id:02:1]], '[[notice:1]]', 'Seed opportunity 10-6 A', 1, [[now]], [[now]]),
       ([[id:02:2]], '[[notice:2]]', 'Seed opportunity 10-6 B', 1, [[now]], [[now]]);
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
VALUES ([[id:03:1]], [[id:01:1]], [[id:02:1]], 'ANALYZING', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:2]], [[id:01:1]], [[id:02:2]], 'ANALYZING', '[]', 'FULL_PACKAGE', [[now]], [[now]]);
INSERT INTO client_documents (id, member_id, bid_request_id, file_name, storage_url, file_size, content_type, uploaded_at, content_sha256, created_at, updated_at)
VALUES ([[id:04:1]], [[id:01:1]], [[id:03:1]], '실적증명서.pdf', 'seed/10-6/1.pdf', 1024, 'application/pdf', [[now]], '[[sha:1]]', [[now]], [[now]]),
       ([[id:04:2]], [[id:01:1]], [[id:03:2]], '실적증명서.pdf', 'seed/10-6/2.pdf', 1024, 'application/pdf', [[now]], '[[sha:2]]', [[now]], [[now]]);
INSERT INTO document_readings (id, content_sha256, file_name, content_type, read_as, status, status_at, facts_json, fact_count, vision_used, failure_reason, read_at, created_at, updated_at)
VALUES ([[id:06:1]], '[[sha:1]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'READING', [[now]] - INTERVAL 1 DAY, NULL, 0, 0, NULL, NULL, [[now]], [[now]]),
       ([[id:06:2]], '[[sha:2]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'READING', [[now]] - INTERVAL 1 DAY, NULL, 0, 0, NULL, NULL, [[now]], [[now]]);
