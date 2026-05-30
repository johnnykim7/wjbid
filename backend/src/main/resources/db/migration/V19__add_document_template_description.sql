-- 기존 갭 보정: DocumentTemplate 엔티티의 description 필드가
-- document_templates 테이블에 없어 목록 조회·신규 등록 모두 SQL 오류.
-- CR-021 작업 중 처음 화면 사용으로 노출됨.

ALTER TABLE document_templates
    ADD COLUMN description TEXT NULL COMMENT '템플릿 설명' AFTER content_json;
