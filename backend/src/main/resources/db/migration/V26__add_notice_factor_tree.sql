-- CR-028: 제안서 파이프라인 3단계 분리 — design 단계 입력 의존 컬럼.
-- CR-027 V25 주석에서 "Notice 컬럼은 후속 CR"로 명시 → 본 CR에서 추가.
--
-- factor_tree_json : 공고 정제 시 Section L/M 에서 도출한 FACTOR/Subfactor 트리.
--                    proposal-design 워크플로우가 이 트리를 그대로 chapter/section 으로 복사·검증.
-- price_items_json : Price 항목·수량·단위 추천 (단가는 ❌ — AI 안 만짐).
--
-- 둘 다 NULL 허용. 정제(NoticeService)가 도출하지 못했으면 design WF 가 공고 본문에서 직접 발췌.

ALTER TABLE notices
    ADD COLUMN factor_tree_json LONGTEXT NULL
        COMMENT 'Section L/M 에서 도출한 FACTOR/Subfactor 트리 (proposal-design 입력)' AFTER content_json,
    ADD COLUMN price_items_json LONGTEXT NULL
        COMMENT 'Price 항목·수량·단위 추천 (단가 제외)' AFTER factor_tree_json;
