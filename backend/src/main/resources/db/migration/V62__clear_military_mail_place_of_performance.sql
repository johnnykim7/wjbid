-- CR-191 수행 장소 바로잡기 (BIZ-103)
-- 수집은 SAM 수행 장소가 비면 계약 사무소 주소로 짧은 수행 장소를 채웠다. 그 값은 군사우편 주소(APO, AP 등)다
-- 수정 시각은 고친 공고로 보이지 않게 제 값을 다시 넣는다
UPDATE opportunities
   SET place_of_performance_short = NULL,
       last_modified_at = last_modified_at,
       updated_at = updated_at
 WHERE UPPER(TRIM(SUBSTRING_INDEX(place_of_performance_short, ',', 1))) IN ('APO', 'FPO', 'DPO')
    OR UPPER(TRIM(SUBSTRING_INDEX(place_of_performance_short, ',', -1))) IN ('AA', 'AE', 'AP');

-- 원본 JSON 의 placeOfPerformance 글은 계약 사무소 주소였다. officeAddress 로 옮긴다
UPDATE opportunities
   SET raw_json = JSON_REMOVE(
           JSON_SET(raw_json, '$.officeAddress', JSON_VALUE(raw_json, '$.placeOfPerformance')),
           '$.placeOfPerformance'),
       last_modified_at = last_modified_at,
       updated_at = updated_at
 WHERE JSON_TYPE(JSON_EXTRACT(raw_json, '$.placeOfPerformance')) = 'STRING';
