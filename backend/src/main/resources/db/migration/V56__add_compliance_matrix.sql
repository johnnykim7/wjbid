-- CR-159 컴플라이언스 매트릭스(BIZ-073~078). 매트릭스 줄은 요구 항목 표에 REQ ID 를 달고 들어간다.
-- 지운 REQ 번호를 다시 쓰지 않도록 공고마다 마지막으로 준 번호를 둔다. 옮길 데이터는 없다
ALTER TABLE notices ADD COLUMN compliance_matrix_json LONGTEXT NULL;

ALTER TABLE opportunity_requirement_items ADD COLUMN req_id VARCHAR(20) NULL;
ALTER TABLE opportunity_requirement_items ADD COLUMN req_level VARCHAR(10) NULL;
ALTER TABLE opportunity_requirement_items
    ADD CONSTRAINT uq_requirement_items_req_id UNIQUE (opportunity_id, req_id);

ALTER TABLE opportunities ADD COLUMN last_req_no INT NOT NULL DEFAULT 0;
