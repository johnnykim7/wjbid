-- @marker SEED-10-5
-- 대상 IT-10-046
-- 분석 단계에서 서류 하나를 읽는 중인 입찰 건 세 개. 1번은 화면 시험, 2번과 3번은 서버 판정 기대결과 2, 3 몫이다
-- 읽는 중 기한은 워크플로우 대기 한도(10/25 상용 60분) 안이어야 하므로 키는 50분 안에 넣은 것만 준다. 서버 판정은 IT-10-001 이
-- 제안서 작성을 30분까지 기다린 뒤에 이 항목에 오므로 그보다 넓어야 한다. 대기 한도를 50분 아래로 줄이면 이 창도 줄인다
-- @key 10.b046_virtual_bid SELECT '[[uuid:03:1]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:1]] AND state = 'ANALYZING' AND EXISTS (SELECT 1 FROM document_readings WHERE id = [[id:06:1]] AND status = 'READING' AND status_at >= [[now]] - INTERVAL 50 MINUTE)
-- @key 10.b046_server2_bid SELECT '[[uuid:03:2]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:2]] AND state = 'ANALYZING' AND EXISTS (SELECT 1 FROM document_readings WHERE id = [[id:06:2]] AND status = 'READING' AND status_at >= [[now]] - INTERVAL 50 MINUTE)
-- @key 10.b046_server3_bid SELECT '[[uuid:03:3]]' AS id, HEX(id) AS hex, JSON_LENGTH(state_history) AS hist FROM bid_requests WHERE id = [[id:03:3]] AND state = 'ANALYZING' AND EXISTS (SELECT 1 FROM document_readings WHERE id = [[id:06:3]] AND status = 'READING' AND status_at >= [[now]] - INTERVAL 50 MINUTE)
INSERT INTO members (id, email, company_name, role, created_at, updated_at)
VALUES ([[id:01:1]], '[[email:a]]', '시딩 회사 10-5', 'CUSTOMER', [[now]], [[now]]);
INSERT INTO opportunities (id, notice_id, title, active, created_at, updated_at)
VALUES ([[id:02:1]], '[[notice:1]]', 'Seed opportunity 10-5 A', 1, [[now]], [[now]]),
       ([[id:02:2]], '[[notice:2]]', 'Seed opportunity 10-5 B', 1, [[now]], [[now]]),
       ([[id:02:3]], '[[notice:3]]', 'Seed opportunity 10-5 C', 1, [[now]], [[now]]);
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
VALUES ([[id:03:1]], [[id:01:1]], [[id:02:1]], 'ANALYZING', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:2]], [[id:01:1]], [[id:02:2]], 'ANALYZING', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:3]], [[id:01:1]], [[id:02:3]], 'ANALYZING', '[]', 'FULL_PACKAGE', [[now]], [[now]]);
INSERT INTO client_documents (id, member_id, bid_request_id, file_name, storage_url, file_size, content_type, uploaded_at, content_sha256, created_at, updated_at)
VALUES ([[id:04:1]], [[id:01:1]], [[id:03:1]], '실적증명서.pdf', 'seed/10-5/1.pdf', 1024, 'application/pdf', [[now]], '[[sha:1]]', [[now]], [[now]]),
       ([[id:04:2]], [[id:01:1]], [[id:03:2]], '실적증명서.pdf', 'seed/10-5/2.pdf', 1024, 'application/pdf', [[now]], '[[sha:2]]', [[now]], [[now]]),
       ([[id:04:3]], [[id:01:1]], [[id:03:3]], '실적증명서.pdf', 'seed/10-5/3.pdf', 1024, 'application/pdf', [[now]], '[[sha:3]]', [[now]], [[now]]);
INSERT INTO document_readings (id, content_sha256, file_name, content_type, read_as, status, status_at, facts_json, fact_count, vision_used, failure_reason, read_at, created_at, updated_at)
VALUES ([[id:06:1]], '[[sha:1]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'READING', [[now]], NULL, 0, 0, NULL, NULL, [[now]], [[now]]),
       ([[id:06:2]], '[[sha:2]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'READING', [[now]], NULL, 0, 0, NULL, NULL, [[now]], [[now]]),
       ([[id:06:3]], '[[sha:3]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'READING', [[now]], NULL, 0, 0, NULL, NULL, [[now]], [[now]]);
