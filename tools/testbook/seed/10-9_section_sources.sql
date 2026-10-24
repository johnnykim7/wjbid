-- @marker SEED-10-9
-- 대상 IT-10-050 IT-10-051 IT-10-052
-- 제안서 설계와 절 작성에 쓰는 고객 서류. 1번 입찰 건은 읽음 A(요구 항목 충족), 읽음 B, 읽지 못함 C 와 장절 없는 문서 X, 절 1 근거가 [A] 인 문서 Y 를 가진다.
-- 2번 입찰 건의 읽음 D 는 다른 건 서류다. 3번 입찰 건은 읽는 중 E 와 절 1 근거가 [E] 인 문서 Z 를 가져, 읽기 결과 저장부터 절 입력까지 한 번에 본다
-- @key 10.b050_doc SELECT '[[uuid:07:1]]' AS id, '[[uuid:06:1]]' AS a, '[[uuid:06:2]]' AS b, '[[uuid:06:3]]' AS c, HEX([[id:07:1]]) AS hex, (SELECT title FROM opportunity_requirement_items WHERE id = [[id:0a:1]]) AS req FROM bid_documents WHERE id = [[id:07:1]]
-- @key 10.b051_doc SELECT '[[uuid:07:2]]' AS id, '[[uuid:06:1]]' AS a, '[[uuid:06:3]]' AS c, '[[uuid:06:4]]' AS d, HEX([[id:07:2]]) AS hex FROM bid_documents WHERE id = [[id:07:2]] AND EXISTS (SELECT 1 FROM proposal_section WHERE id = [[id:09:2]])
-- @key 10.b052_section SELECT '[[uuid:09:3]]' AS id, '[[uuid:03:3]]' AS bid, '[[uuid:06:5]]' AS e, HEX([[id:06:5]]) AS e_hex FROM proposal_section WHERE id = [[id:09:3]]
-- @key 10.b050_virtual_doc SELECT '[[uuid:07:2]]' AS id, '[[uuid:09:2]]' AS section, '[[uuid:06:1]]' AS a FROM proposal_section WHERE id = [[id:09:2]]
INSERT INTO members (id, email, company_name, role, created_at, updated_at)
VALUES ([[id:01:1]], '[[email:a]]', '시딩 회사 10-9 A', 'CUSTOMER', [[now]], [[now]]),
       ([[id:01:2]], '[[email:b]]', '시딩 회사 10-9 B', 'CUSTOMER', [[now]], [[now]]),
       ([[id:01:3]], '[[email:c]]', '시딩 회사 10-9 C', 'CUSTOMER', [[now]], [[now]]);
INSERT INTO opportunities (id, notice_id, title, active, created_at, updated_at)
VALUES ([[id:02:1]], '[[notice:1]]', 'Seed opportunity 10-9', 1, [[now]], [[now]]);
INSERT INTO opportunity_requirement_items (id, opportunity_id, category, title, requirement_json, req_id, req_level, sort_order, created_at, updated_at)
VALUES ([[id:0a:1]], [[id:02:1]], 'DOCUMENT', '수행 실적 증명서', '{}', 'REQ-SEED-1', 'SHALL', 1, [[now]], [[now]]);
INSERT INTO bid_requests (id, member_id, opportunity_id, state, state_history, service_level, created_at, updated_at)
VALUES ([[id:03:1]], [[id:01:1]], [[id:02:1]], 'ANALYZING', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:2]], [[id:01:2]], [[id:02:1]], 'ANALYZING', '[]', 'FULL_PACKAGE', [[now]], [[now]]),
       ([[id:03:3]], [[id:01:3]], [[id:02:1]], 'ANALYZING', '[]', 'FULL_PACKAGE', [[now]], [[now]]);
INSERT INTO client_documents (id, member_id, bid_request_id, file_name, storage_url, file_size, content_type, uploaded_at, content_sha256, created_at, updated_at)
VALUES ([[id:04:1]], [[id:01:1]], [[id:03:1]], '실적증명서.pdf', 'seed/10-9/1.pdf', 1024, 'application/pdf', [[now]], '[[sha:1]]', [[now]], [[now]]),
       ([[id:04:2]], [[id:01:1]], [[id:03:1]], '사업자등록증.pdf', 'seed/10-9/2.pdf', 1024, 'application/pdf', [[now]], '[[sha:2]]', [[now]], [[now]]),
       ([[id:04:3]], [[id:01:1]], [[id:03:1]], '면허증.pdf', 'seed/10-9/3.pdf', 1024, 'application/pdf', [[now]], '[[sha:3]]', [[now]], [[now]]),
       ([[id:04:4]], [[id:01:2]], [[id:03:2]], '실적증명서_다른회사.pdf', 'seed/10-9/4.pdf', 1024, 'application/pdf', [[now]], '[[sha:4]]', [[now]], [[now]]),
       ([[id:04:5]], [[id:01:3]], [[id:03:3]], '장비목록.pdf', 'seed/10-9/5.pdf', 1024, 'application/pdf', [[now]], '[[sha:5]]', [[now]], [[now]]);
INSERT INTO requirement_fulfillment_maps (id, bid_request_id, requirement_item_id, fulfillment_type, client_document_id, status, created_at, updated_at)
VALUES ([[id:0b:1]], [[id:03:1]], [[id:0a:1]], 'CLIENT_DOCUMENT', [[id:04:1]], 'PENDING', [[now]], [[now]]);
INSERT INTO document_readings (id, content_sha256, file_name, content_type, read_as, status, status_at, facts_json, fact_count, vision_used, failure_reason, read_at, created_at, updated_at)
VALUES ([[id:06:1]], '[[sha:1]]', '실적증명서.pdf', 'application/pdf', 'PDF', 'DONE', [[now]], '[{"category":"PAST_PERFORMANCE","key":"사업명","value":"Camp Humphreys Plumbing Repair","page":"1","anchor":"Project: Camp Humphreys Plumbing Repair","confidence":"HIGH","readMode":"TEXT"},{"category":"PAST_PERFORMANCE","key":"발주처","value":"411th CSB","page":"1","anchor":"Client: 411th CSB","confidence":"HIGH","readMode":"TEXT"},{"category":"PAST_PERFORMANCE","key":"계약 금액","value":"USD 1,250,000","page":"1","anchor":"Amount: USD 1,250,000","confidence":"HIGH","readMode":"TEXT"}]', 3, 0, NULL, [[now]], [[now]], [[now]]),
       ([[id:06:2]], '[[sha:2]]', '사업자등록증.pdf', 'application/pdf', 'PDF', 'DONE', [[now]], '[{"category":"COMPANY","key":"상호","value":"시딩 회사","page":"1","anchor":"상호: 시딩 회사","confidence":"HIGH","readMode":"TEXT"},{"category":"COMPANY","key":"사업자등록번호","value":"000-00-00000","page":"1","anchor":"등록번호: 000-00-00000","confidence":"HIGH","readMode":"TEXT"}]', 2, 0, NULL, [[now]], [[now]], [[now]]),
       ([[id:06:3]], '[[sha:3]]', '면허증.pdf', 'application/pdf', 'PDF', 'FAILED', [[now]], NULL, 0, 0, '워크플로우가 결과를 남기지 않음', NULL, [[now]], [[now]]),
       ([[id:06:4]], '[[sha:4]]', '실적증명서_다른회사.pdf', 'application/pdf', 'PDF', 'DONE', [[now]], '[{"category":"PAST_PERFORMANCE","key":"사업명","value":"Camp Humphreys Plumbing Repair","page":"1","anchor":"Project: Camp Humphreys Plumbing Repair","confidence":"HIGH","readMode":"TEXT"},{"category":"PAST_PERFORMANCE","key":"발주처","value":"411th CSB","page":"1","anchor":"Client: 411th CSB","confidence":"HIGH","readMode":"TEXT"},{"category":"PAST_PERFORMANCE","key":"계약 금액","value":"USD 1,250,000","page":"1","anchor":"Amount: USD 1,250,000","confidence":"HIGH","readMode":"TEXT"}]', 3, 0, NULL, [[now]], [[now]], [[now]]),
       ([[id:06:5]], '[[sha:5]]', '장비목록.pdf', 'application/pdf', 'PDF', 'READING', [[now]], NULL, 0, 0, NULL, NULL, [[now]], [[now]]);
INSERT INTO bid_documents (id, bid_request_id, document_type, title, status, current_version_no, created_at, updated_at)
VALUES ([[id:07:1]], [[id:03:1]], 'TECHNICAL_PROPOSAL', 'Seed proposal X', 'DRAFT', 0, [[now]], [[now]]),
       ([[id:07:2]], [[id:03:1]], 'TECHNICAL_PROPOSAL', 'Seed proposal Y', 'DRAFT', 0, [[now]], [[now]]),
       ([[id:07:3]], [[id:03:3]], 'TECHNICAL_PROPOSAL', 'Seed proposal Z', 'DRAFT', 0, [[now]], [[now]]);
INSERT INTO proposal_chapter (id, document_id, factor_label, factor_title, order_no, created_at, updated_at)
VALUES ([[id:08:2]], [[id:07:2]], '1', 'Past Performance', 1, [[now]], [[now]]),
       ([[id:08:3]], [[id:07:3]], '1', 'Equipment', 1, [[now]], [[now]]);
INSERT INTO proposal_section (id, chapter_id, subfactor_label, title, scope, requirement_refs, min_words, status, order_no, source_reading_ids, created_at, updated_at)
VALUES ([[id:09:2]], [[id:08:2]], '1.1', 'Relevant Contracts', 'Recent relevant contracts', '[]', 200, 'PENDING', 1, '["[[uuid:06:1]]"]', [[now]], [[now]]),
       ([[id:09:3]], [[id:08:3]], '1.1', 'Equipment List', 'Equipment on hand', '[]', 200, 'PENDING', 1, '["[[uuid:06:5]]"]', [[now]], [[now]]);
