-- @marker SEED-12-1
-- 대상 IT-12-031
-- 서류 접수가 끝난 입찰 건 세 개. 1번은 화면 시험, 2번과 3번은 서버 판정 기대결과 3, 4 몫이다
-- @key 12.b031_virtual_bid SELECT '[[uuid:03:1]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:1]] AND state = 'DOCS_RECEIVED'
-- @key 12.b031_server3_bid SELECT '[[uuid:03:2]]' AS id, HEX(id) AS hex, JSON_LENGTH(state_history) AS hist FROM bid_requests WHERE id = [[id:03:2]] AND state = 'DOCS_RECEIVED'
-- @key 12.b031_server4_bid SELECT '[[uuid:03:3]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:3]] AND state = 'DOCS_RECEIVED'
INSERT INTO members (id, email, company_name, role, created_at, updated_at)
VALUES ([[id:01:1]], '[[email:a]]', '시딩 회사 12-1', 'CUSTOMER', [[now]], [[now]]);
INSERT INTO opportunities (id, notice_id, title, active, created_at, updated_at)
VALUES ([[id:02:1]], '[[notice:1]]', 'Seed opportunity 12-1 A', 1, [[now]], [[now]]),
       ([[id:02:2]], '[[notice:2]]', 'Seed opportunity 12-1 B', 1, [[now]], [[now]]),
       ([[id:02:3]], '[[notice:3]]', 'Seed opportunity 12-1 C', 1, [[now]], [[now]]);
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
VALUES ([[id:03:1]], [[id:01:1]], [[id:02:1]], 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:2]], [[id:01:1]], [[id:02:2]], 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:3]], [[id:01:1]], [[id:02:3]], 'DOCS_RECEIVED', '[]', 'FULL_PACKAGE', [[now]], [[now]]);
