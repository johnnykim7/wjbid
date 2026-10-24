-- @marker SEED-10-2
-- 대상 IT-10-041 IT-10-043 IT-10-054
-- 읽기가 끝난 분석 단계 입찰 건. 공고별 서류 실적증명서와 스캔 면허증, 서류함 사업자등록증은 읽음이고 만료된 보험증권은 읽음이어도 후보가 아니다
-- @key 10.b041_done_bid SELECT '[[uuid:03:1]]' AS id, HEX(id) AS hex FROM bid_requests WHERE id = [[id:03:1]] AND state = 'ANALYZING'
-- @key 10.b054_reading SELECT '[[uuid:06:1]]' AS id FROM document_readings WHERE id = [[id:06:1]] AND status = 'DONE'
INSERT INTO members (id, email, company_name, role, created_at, updated_at)
VALUES ([[id:01:1]], '[[email:a]]', '시딩 회사 10-2', 'CUSTOMER', [[now]], [[now]]);
INSERT INTO opportunities (id, notice_id, title, active, created_at, updated_at)
VALUES ([[id:02:1]], '[[notice:1]]', 'Seed opportunity 10-2', 1, [[now]], [[now]]);
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
VALUES ([[id:03:1]], [[id:01:1]], [[id:02:1]], 'ANALYZING', '[]', 'FULL_PACKAGE', [[now]], [[now]]);
INSERT INTO client_documents (id, member_id, bid_request_id, file_name, storage_url, file_size, content_type, uploaded_at, content_sha256, created_at, updated_at)
VALUES ([[id:04:1]], [[id:01:1]], [[id:03:1]], '실적증명서.pdf', 'seed/10-2/1.pdf', 1024, 'application/pdf', [[now]], '[[sha:1]]', [[now]], [[now]]),
       ([[id:04:2]], [[id:01:1]], [[id:03:1]], '면허증_스캔.pdf', 'seed/10-2/4.pdf', 1024, 'application/pdf', [[now]], '[[sha:4]]', [[now]], [[now]]);
INSERT INTO member_documents (id, member_id, category, file_name, file_size, content_type, storage_url, created_at, updated_at, expires_on, content_sha256)
VALUES ([[id:05:1]], [[id:01:1]], 'BUSINESS_REGISTRATION', '사업자등록증.pdf', 1024, 'application/pdf', 'seed/10-2/2.pdf', [[now]], [[now]], NULL, '[[sha:2]]'),
       ([[id:05:2]], [[id:01:1]], 'OTHER', '보험증권.pdf', 1024, 'application/pdf', 'seed/10-2/3.pdf', [[now]], [[now]], DATE_SUB(CURDATE(), INTERVAL 1 DAY), '[[sha:3]]');
INSERT INTO document_readings (id, content_sha256, file_name, content_type, read_as, status, status_at, facts_json, fact_count, vision_used, read_at, created_at, updated_at)
VALUES ([[id:06:1]], '[[sha:1]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'DONE', [[now]],
        '[{"category":"PAST_PERFORMANCE","key":"사업명","value":"Camp Humphreys Plumbing Repair","page":"1","anchor":"Project: Camp Humphreys Plumbing Repair","confidence":"HIGH","readMode":"TEXT"},{"category":"PAST_PERFORMANCE","key":"발주처","value":"411th CSB","page":"1","anchor":"Client: 411th CSB","confidence":"HIGH","readMode":"TEXT"},{"category":"PAST_PERFORMANCE","key":"계약 금액","value":"USD 1,250,000","page":"1","anchor":"Amount: USD 1,250,000","confidence":"HIGH","readMode":"TEXT"}]',
        3, 0, [[now]], [[now]], [[now]]),
       ([[id:06:2]], '[[sha:2]]', '사업자등록증.pdf', 'application/pdf', 'PDF', 'DONE', [[now]],
        '[{"category":"COMPANY","key":"상호","value":"시딩 회사 10-2","page":"1","anchor":"상호: 시딩 회사 10-2","confidence":"HIGH","readMode":"TEXT"},{"category":"COMPANY","key":"사업자등록번호","value":"000-00-00000","page":"1","anchor":"등록번호: 000-00-00000","confidence":"HIGH","readMode":"TEXT"}]',
        2, 0, [[now]], [[now]], [[now]]),
       ([[id:06:3]], '[[sha:3]]', '보험증권.pdf', 'application/pdf', 'PDF', 'DONE', [[now]],
        '[{"category":"INSURANCE","key":"보험 종류","value":"영업배상책임","page":"1","anchor":"영업배상책임","confidence":"HIGH","readMode":"TEXT"}]',
        1, 0, [[now]], [[now]], [[now]]),
       ([[id:06:4]], '[[sha:4]]', '면허증_스캔.pdf', 'application/pdf', 'PDF', 'DONE', [[now]],
        '[{"category":"LICENSE","key":"면허 종류","value":"기계설비공사업","page":"1","anchor":"기계설비공사업","confidence":"MEDIUM","readMode":"VISION"},{"category":"LICENSE","key":"면허 번호","value":"제2026-0001호","page":"1","anchor":"제2026-0001호","confidence":"MEDIUM","readMode":"VISION"}]',
        2, 1, [[now]], [[now]], [[now]]);
