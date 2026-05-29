-- CR-014: 공고 사업유형 자동 분류 (BIZ-018)
-- 성공 제안서 자산(RfpSample/PatternGuide, industry_type 단위)과 신규 공고를 매칭하기 위한 분류 컬럼.
-- naicsCode/classificationCode(PSC)/title 기반 결정적 분류. 미매칭 시 NULL(미분류).
-- length 30 = pattern_guide.industry_type(V11)과 통일. enum 집합 공유.

ALTER TABLE opportunities ADD COLUMN industry_type VARCHAR(30) NULL;

CREATE INDEX idx_opportunities_industry_type ON opportunities (industry_type);
