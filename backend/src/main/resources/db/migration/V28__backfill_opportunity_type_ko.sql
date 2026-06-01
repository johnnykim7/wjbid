-- V28: 기존 opportunities.type_ko 백필 (CR-022)
-- type은 SAM 표준값(유한 집합) → 코드 매핑(NoticeTypeTranslator)과 동일한 매핑으로 일괄 채움.
-- 매핑에 없는 신규 type은 NULL 유지 → 영문 fallback.

UPDATE opportunities SET type_ko = '입찰요청'         WHERE type = 'Solicitation'                    AND (type_ko IS NULL OR type_ko = '');
UPDATE opportunities SET type_ko = '공고+입찰요청 통합' WHERE type = 'Combined Synopsis/Solicitation'    AND (type_ko IS NULL OR type_ko = '');
UPDATE opportunities SET type_ko = '낙찰공지'         WHERE type = 'Award Notice'                    AND (type_ko IS NULL OR type_ko = '');
UPDATE opportunities SET type_ko = '사전수요조사'      WHERE type = 'Sources Sought'                  AND (type_ko IS NULL OR type_ko = '');
UPDATE opportunities SET type_ko = '사전공고'         WHERE type = 'Presolicitation'                 AND (type_ko IS NULL OR type_ko = '');
