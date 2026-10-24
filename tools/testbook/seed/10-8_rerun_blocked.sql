-- @marker SEED-10-8
-- 대상 IT-10-049
-- 문서 작성 중 단계인데 읽지 못한 서류가 남은 입찰 건. 다시 읽기는 막혀야 하므로 화면 시험과 서버 판정이 같이 쓴다
-- @key 10.b049_bid SELECT '[[uuid:03:1]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:1]] AND state = 'GENERATING'
INSERT INTO members (id, email, company_name, role, created_at, updated_at)
VALUES ([[id:01:1]], '[[email:a]]', '시딩 회사 10-8', 'CUSTOMER', [[now]], [[now]]);
INSERT INTO opportunities (id, notice_id, title, active, created_at, updated_at)
VALUES ([[id:02:1]], '[[notice:1]]', 'Seed opportunity 10-8', 1, [[now]], [[now]]);
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
VALUES ([[id:03:1]], [[id:01:1]], [[id:02:1]], 'GENERATING', '[]', 'FULL_PACKAGE', [[now]], [[now]]);
INSERT INTO client_documents (id, member_id, bid_request_id, file_name, storage_url, file_size, content_type, uploaded_at, content_sha256, created_at, updated_at)
VALUES ([[id:04:1]], [[id:01:1]], [[id:03:1]], '실적증명서.pdf', 'seed/10-8/1.pdf', 1024, 'application/pdf', [[now]], '[[sha:1]]', [[now]], [[now]]);
INSERT INTO document_readings (id, content_sha256, file_name, content_type, read_as, status, status_at, facts_json, fact_count, vision_used, failure_reason, read_at, created_at, updated_at)
VALUES ([[id:06:1]], '[[sha:1]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'FAILED', [[now]] - INTERVAL 1 HOUR, NULL, 0, 0, '워크플로우가 결과를 남기지 않음', NULL, [[now]], [[now]]);
