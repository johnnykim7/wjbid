-- @marker SEED-10-7
-- 대상 IT-10-048
-- 분석 단계에서 서류 하나는 읽지 못함, 하나는 읽음인 입찰 건 두 개. 1번은 화면 시험, 2번은 서버 판정 몫이다
-- @key 10.b048_virtual_bid SELECT '[[uuid:03:1]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:1]] AND state = 'ANALYZING'
-- @key 10.b048_server_bid SELECT '[[uuid:03:2]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:2]] AND state = 'ANALYZING'
INSERT INTO members (id, email, company_name, role, created_at, updated_at)
VALUES ([[id:01:1]], '[[email:a]]', '시딩 회사 10-7', 'CUSTOMER', [[now]], [[now]]);
INSERT INTO opportunities (id, notice_id, title, active, created_at, updated_at)
VALUES ([[id:02:1]], '[[notice:1]]', 'Seed opportunity 10-7 A', 1, [[now]], [[now]]),
       ([[id:02:2]], '[[notice:2]]', 'Seed opportunity 10-7 B', 1, [[now]], [[now]]);
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
VALUES ([[id:03:1]], [[id:01:1]], [[id:02:1]], 'ANALYZING', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:2]], [[id:01:1]], [[id:02:2]], 'ANALYZING', '[]', 'FULL_PACKAGE', [[now]], [[now]]);
INSERT INTO client_documents (id, member_id, bid_request_id, file_name, storage_url, file_size, content_type, uploaded_at, content_sha256, created_at, updated_at)
VALUES ([[id:04:1]], [[id:01:1]], [[id:03:1]], '실적증명서.pdf', 'seed/10-7/1.pdf', 1024, 'application/pdf', [[now]], '[[sha:1]]', [[now]], [[now]]),
       ([[id:04:2]], [[id:01:1]], [[id:03:1]], '사업자등록증.pdf', 'seed/10-7/2.pdf', 1024, 'application/pdf', [[now]], '[[sha:2]]', [[now]], [[now]]),
       ([[id:04:3]], [[id:01:1]], [[id:03:2]], '실적증명서.pdf', 'seed/10-7/3.pdf', 1024, 'application/pdf', [[now]], '[[sha:3]]', [[now]], [[now]]),
       ([[id:04:4]], [[id:01:1]], [[id:03:2]], '사업자등록증.pdf', 'seed/10-7/4.pdf', 1024, 'application/pdf', [[now]], '[[sha:4]]', [[now]], [[now]]);
INSERT INTO document_readings (id, content_sha256, file_name, content_type, read_as, status, status_at, facts_json, fact_count, vision_used, failure_reason, read_at, created_at, updated_at)
VALUES ([[id:06:1]], '[[sha:1]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'FAILED', [[now]] - INTERVAL 1 HOUR, NULL, 0, 0, '워크플로우가 결과를 남기지 않음', NULL, [[now]], [[now]]),
       ([[id:06:2]], '[[sha:2]]', '사업자등록증.pdf', 'application/pdf', 'PDF', 'DONE', [[now]] - INTERVAL 1 HOUR, '[{"category":"COMPANY","key":"상호","value":"시딩 회사","page":"1","anchor":"상호: 시딩 회사","confidence":"HIGH","readMode":"TEXT"},{"category":"COMPANY","key":"사업자등록번호","value":"000-00-00000","page":"1","anchor":"등록번호: 000-00-00000","confidence":"HIGH","readMode":"TEXT"}]', 2, 0, NULL, [[now]] - INTERVAL 1 HOUR, [[now]], [[now]]),
       ([[id:06:3]], '[[sha:3]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'FAILED', [[now]] - INTERVAL 1 HOUR, NULL, 0, 0, '워크플로우가 결과를 남기지 않음', NULL, [[now]], [[now]]),
       ([[id:06:4]], '[[sha:4]]', '사업자등록증.pdf', 'application/pdf', 'PDF', 'DONE', [[now]] - INTERVAL 1 HOUR, '[{"category":"COMPANY","key":"상호","value":"시딩 회사","page":"1","anchor":"상호: 시딩 회사","confidence":"HIGH","readMode":"TEXT"},{"category":"COMPANY","key":"사업자등록번호","value":"000-00-00000","page":"1","anchor":"등록번호: 000-00-00000","confidence":"HIGH","readMode":"TEXT"}]', 2, 0, NULL, [[now]] - INTERVAL 1 HOUR, [[now]], [[now]]);
