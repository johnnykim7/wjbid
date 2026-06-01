-- CR-026: 산출물 포맷 + 가이드 분리, DEFAULT 패턴 + 상속 구조
-- 운영 DB에는 수동 적용 완료 (ALTER + INSERT). 본 마이그레이션은 새 환경용 idempotent baseline.
-- 변경 후 절대 본 파일을 다시 수정하지 말 것 (checksum 고정).

-- pattern_guide.format_json 컬럼 (이미 있으면 ALTER 실패해도 무시되도록 stored procedure 사용)
DROP PROCEDURE IF EXISTS add_format_json_if_missing;
DELIMITER //
CREATE PROCEDURE add_format_json_if_missing()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pattern_guide' AND COLUMN_NAME = 'format_json') THEN
        ALTER TABLE pattern_guide ADD COLUMN format_json LONGTEXT NULL COMMENT '산출물 포맷(Volume/Section 골격)';
    END IF;
END//
DELIMITER ;
CALL add_format_json_if_missing();
DROP PROCEDURE IF EXISTS add_format_json_if_missing;

-- pattern_guide.parent_id 컬럼 (BINARY(16), FK to pattern_guide.id)
DROP PROCEDURE IF EXISTS add_parent_id_if_missing;
DELIMITER //
CREATE PROCEDURE add_parent_id_if_missing()
BEGIN
    IF NOT EXISTS (SELECT 1 FROM INFORMATION_SCHEMA.COLUMNS
                   WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'pattern_guide' AND COLUMN_NAME = 'parent_id') THEN
        ALTER TABLE pattern_guide ADD COLUMN parent_id BINARY(16) NULL COMMENT '상속 부모 패턴 ID (DEFAULT면 null)';
        ALTER TABLE pattern_guide ADD CONSTRAINT fk_pattern_guide_parent FOREIGN KEY (parent_id) REFERENCES pattern_guide(id);
    END IF;
END//
DELIMITER ;
CALL add_parent_id_if_missing();
DROP PROCEDURE IF EXISTS add_parent_id_if_missing;

-- DEFAULT 패턴 INSERT (없을 때만)
INSERT INTO pattern_guide (
    id, industry_type, status, source, parent_id,
    guide_json, format_json, guide_markdown,
    sample_count, extracted_at, created_at, updated_at
)
SELECT
    UNHEX(REPLACE(UUID(), '-', '')),
    'DEFAULT', 'COMPLETED', 'HUMAN_EDITED', NULL,
    '{"principles":[{"id":"no-pws-mirroring","title":"PWS 미러링 금지","detail":"공고 요구문 그대로 베껴쓰지 말 것"},{"id":"quantify-everything","title":"정량 우선","detail":"형용사 대신 숫자"},{"id":"evidence-first","title":"주장-근거 1:1","detail":"근거 없는 주장은 무시됨"},{"id":"acceptable-binary","title":"Acceptable/Unacceptable 대비","detail":"한 항목이라도 탈락하면 전체 탈락"},{"id":"evaluator-perspective","title":"평가관 시점","detail":"첫 단락 결론, 표·불릿 시각화"}],"doNot":["PWS 미러링","정성 형용사 남용","Volume 혼재","FACTOR 라벨 변경","페이지 제한 초과","성공 샘플 복제"],"tone":{"voice":"능동태 우선","register":"공식·간결","terminology":"PWS/CLIN/IGCE/COR/KO 약어 정확히"},"evidence":{"preferredTypes":["계약번호+발주처+기간+금액","자격증","장비 사양서","PPQ"],"freshness":"최근 3~5년 이내"}}',
    '{"volumes":[{"id":"vol1","title":"Volume I - Technical Proposal","sections":[{"id":"technical-approach","title":"Technical Approach","required":true},{"id":"management-approach","title":"Management Approach","required":true},{"id":"key-personnel","title":"Key Personnel","required":false},{"id":"staffing-plan","title":"Staffing Plan","required":false},{"id":"equipment-plan","title":"Equipment Plan","required":false}]},{"id":"vol2","title":"Volume II - Past Performance","sections":[{"id":"experience-list","title":"Prior Experience List","required":true},{"id":"ppq","title":"Past Performance Questionnaire","required":false}]},{"id":"vol3","title":"Volume III - Price","sections":[{"id":"price-schedule","title":"CLIN 단가표 (xlsx)","required":true,"generatedBy":"HUMAN"}]},{"id":"vol4","title":"Volume IV - Contract Documents","sections":[{"id":"sf1449","title":"SF1449","required":true},{"id":"reps-certs","title":"FAR 52.212-3 Reps & Certs","required":true},{"id":"business-reg","title":"사업자등록증","required":true},{"id":"tax-exemption","title":"Tax Exemption","required":false}]}],"labeling":{"coverPage":"각 Volume 표지에 RFP 번호+회사명+Volume 번호","factorLabeling":"공고가 지정한 FACTOR 라벨 그대로"},"constraints":{"fontMinSize":12,"submitMedium":"PIEE","volumeSeparation":"Technical과 Price 혼재 금지"}}',
    'USFK 입찰 DEFAULT 패턴. FAR Part 15 + FAR 52.212-1 기반.',
    0, NOW(), NOW(), NOW()
FROM dual
WHERE NOT EXISTS (SELECT 1 FROM pattern_guide WHERE industry_type='DEFAULT');
