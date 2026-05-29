-- CR-010: 공고 요구서류 ↔ 고객 업로드 서류 슬롯 매칭
-- RequirementFulfillmentMap을 고객 업로드 서류(ClientDocument)로도 충족할 수 있도록 FK 추가.
-- 기존엔 BidDocument(AI 생성 문서)로만 충족 가능했음.

-- 1. client_document_id FK 컬럼 추가 (고객 업로드 서류로 충족 시)
ALTER TABLE requirement_fulfillment_maps
    ADD COLUMN client_document_id BINARY(16) NULL AFTER document_id;

ALTER TABLE requirement_fulfillment_maps
    ADD CONSTRAINT fk_fulfillment_client_document
        FOREIGN KEY (client_document_id) REFERENCES client_documents(id) ON DELETE SET NULL;

ALTER TABLE requirement_fulfillment_maps
    ADD INDEX idx_fulfillment_client_document (client_document_id);

-- 2. 한 의뢰의 한 요구사항은 한 슬롯 (UNIQUE 제약)
ALTER TABLE requirement_fulfillment_maps
    ADD CONSTRAINT uq_fulfillment_bid_requirement
        UNIQUE (bid_request_id, requirement_item_id);
