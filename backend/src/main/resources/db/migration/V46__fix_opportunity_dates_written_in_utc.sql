-- 운영 백엔드가 UTC 로 돌던 동안 수집이 쓴 게시일과 마감은 9시간 늦게 저장됐다.
-- 날짜만 오는 게시일이 00:00 이 아니라 09:00 으로 들어간 행이 그 표시다(게시일과 마감은 수집이 한 번에 함께 쓴다).
UPDATE opportunities
   SET posted_date       = posted_date - INTERVAL 9 HOUR,
       response_deadline = response_deadline - INTERVAL 9 HOUR
 WHERE TIME(posted_date) = '09:00:00';
