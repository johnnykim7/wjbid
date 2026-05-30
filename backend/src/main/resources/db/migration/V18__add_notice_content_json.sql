-- CR-021: 공고문 표시 풍부화
-- notices.content_json: PDF 양식 풍부도 재현용 TipTap JSON 본문
-- (액션용 정형 JSON—summary_json/required_documents_json—은 그대로 유지)

ALTER TABLE notices
    ADD COLUMN content_json LONGTEXT NULL COMMENT 'TipTap JSON 본문 (사람이 읽는 풍부 표현)' AFTER llm_prompt_preset_json;
