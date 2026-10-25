-- @marker SEED-12-2
-- 대상 IT-12-034
-- 읽는 중인 서류가 없는 분석 단계 입찰 건 두 개. 1번은 화면 시험, 2번은 서버 판정 몫이다
-- 문서 작성 중으로 넘기려면 지금 분석의 관리자 결정이 Go 여야 해서(CR-170, BIZ-092) 결정만 있는 회차를 둔다
-- @key 12.b034_virtual_bid SELECT '[[uuid:03:1]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:1]] AND state = 'ANALYZING'
-- @key 12.b034_server_bid SELECT '[[uuid:03:2]]' AS id, HEX(id) AS hex, JSON_LENGTH(state_history) AS hist FROM bid_requests WHERE id = [[id:03:2]] AND state = 'ANALYZING'
INSERT INTO members (id, email, company_name, role, created_at, updated_at)
VALUES ([[id:01:1]], '[[email:a]]', '시딩 회사 12-2', 'CUSTOMER', [[now]], [[now]]);
INSERT INTO opportunities (id, notice_id, title, active, created_at, updated_at)
VALUES ([[id:02:1]], '[[notice:1]]', 'Seed opportunity 12-2 A', 1, [[now]], [[now]]),
       ([[id:02:2]], '[[notice:2]]', 'Seed opportunity 12-2 B', 1, [[now]], [[now]]);
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
VALUES ([[id:03:1]], [[id:01:1]], [[id:02:1]], 'ANALYZING', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:2]], [[id:01:1]], [[id:02:2]], 'ANALYZING', '[]', 'FULL_PACKAGE', [[now]], [[now]]);
INSERT INTO bid_analyses (id, bid_request_id, status, started_at, admin_decision, decided_by, decided_at, created_at, updated_at)
SELECT [[id:0c:1]], br.id, 'DECISION_ONLY', [[now]], 'GO', (SELECT a.id FROM members a WHERE a.email = 'smoke-admin@example.com'), [[now]], [[now]], [[now]] FROM bid_requests br WHERE br.id = [[id:03:1]];
INSERT INTO bid_analyses (id, bid_request_id, status, started_at, admin_decision, decided_by, decided_at, created_at, updated_at)
SELECT [[id:0c:2]], br.id, 'DECISION_ONLY', [[now]], 'GO', (SELECT a.id FROM members a WHERE a.email = 'smoke-admin@example.com'), [[now]], [[now]], [[now]] FROM bid_requests br WHERE br.id = [[id:03:2]];
