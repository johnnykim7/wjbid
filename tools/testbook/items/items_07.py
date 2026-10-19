# 영역 07 공고 자료 화면 시험항목. 근거는 T1-1 기능요구사항 명세서 v1.19(BID-MKT-007~014, 025~027, 034), T1-3 BIZ-004·012·028~031·052~054·057, T1-4 POL-002·016·031~034·036, T3-1, T3-2, T3-3, T3-4 화면 설계다.

ITEMS = [

dict(id='IT-07-001', flow='낙찰 업체 저장', kind='정상', title='새 낙찰 공지를 수집하면 업체 이름, UEI, 금액을 숫자로 저장한다',
     pre='관리자, SAM 검색 Mock 에 award(awardee.name "ACME KOREA CO., LTD.", awardee.ueiSAM "ABCDEF123456", amount 1234567.89)가 든 새 낙찰 공지 1건',
     steps='1. 관리자 화면 [SAM.gov 수집] 에서 [수집 시작] 을 누른다\n2. 수집이 끝난 뒤 그 낙찰 공지의 opportunities 행을 본다\n3. 고객 화면 /awards/recent 에서 그 공지 줄을 본다',
     expect='1. awardee_name 이 "ACME KOREA CO., LTD." 다\n'
            '2. awardee_uei 가 "ABCDEF123456" 이다\n'
            '3. award_amount_usd 가 숫자 1234567.89 다\n'
            '4. 원본 보관 raw_json 에 award 정보가 SAM 응답 그대로 들어 있다\n'
            '5. 최근 낙찰 그 공지 줄 제목 아래에 "낙찰 업체 ACME KOREA CO., LTD." 꼬리표가 있다',
     judge=['서버', '서버', '서버', '서버', '가상'],
     req=['BID-MKT-007', 'BID-MKT-004'], fc=['BID-MKT-007', 'BID-MKT-004'], screen='관리자 /collection', pri='Medium',
     issue='관리자 왼쪽 메뉴 이름이 설계는 수집 관리, 화면 소스는 "SAM.gov 수집" 이다'),

dict(id='IT-07-002', flow='낙찰 업체 저장', kind='정상', title='이미 있는 낙찰 공지가 다시 오면 업체 칸을 채운다',
     pre='관리자, 업체 칸이 빈 기존 낙찰 공지 1건, SAM 검색 Mock 에 같은 공지가 award 정보와 함께 다시 들어 있음',
     steps='1. 관리자 화면 [SAM.gov 수집] 에서 [수집 시작] 을 누른다\n2. 그 공지의 opportunities 행을 본다',
     expect='1. 같은 id 행에 awardee_name, awardee_uei, award_amount_usd 가 채워져 있다\n'
            '2. 그 공고번호의 낙찰 공지 행이 새로 생기지 않았다',
     judge=['서버', '서버'],
     req=['BID-MKT-007'], fc=['BID-MKT-007'], screen='관리자 /collection', pri='Medium'),

dict(id='IT-07-003', flow='낙찰 업체 저장', kind='정상', title='응답에 없는 업체 값은 비워 둔다',
     pre='관리자, SAM 검색 Mock 에 awardee.ueiSAM 과 amount 가 없는 새 낙찰 공지 1건',
     steps='1. 관리자 화면 [SAM.gov 수집] 에서 [수집 시작] 을 누른다\n2. 그 공지의 opportunities 행을 본다',
     expect='1. awardee_name 은 응답 이름 그대로 저장되어 있다\n'
            '2. awardee_uei 와 award_amount_usd 가 NULL 이다',
     judge=['서버', '서버'],
     req=['BID-MKT-007'], fc=['BID-MKT-007'], screen='관리자 /collection', pri='Medium'),

dict(id='IT-07-004', flow='낙찰 업체 저장', kind='예외', title='낙찰 정보 저장이 실패해도 수집은 이어진다',
     pre='낙찰 정보 저장에서 오류가 나도록 장애를 넣은 시험 서버, SAM 검색 Mock 에 낙찰 공지 1건과 일반 공고 2건',
     steps='1. 수집을 실행한다\n2. 수집 결과와 opportunities 행을 본다',
     expect='1. 수집 회차가 실패로 끝나지 않는다\n'
            '2. 일반 공고 2건이 opportunities 에 저장되어 있다',
     judge=['단위', '단위'],
     req=['BID-MKT-007'], fc=['BID-MKT-007'], screen='', pri='Medium'),

dict(id='IT-07-005', flow='지난 낙찰 공지 다시 받기', kind='정상', title='관리자가 공지일 기간을 주어 지난 낙찰 공지의 업체와 금액을 채운다',
     pre='관리자, 업체 칸이 빈 기존 낙찰 공지 2건, SAM 검색 Mock 에 그 기간 낙찰 공지 4건(기존 2건, 새 공지 1건, 업체 이름 없는 공지 1건)',
     steps='1. 관리자 화면 [SAM.gov 수집] 의 지난 낙찰 공지 다시 받기에서 공지일 시작과 공지일 끝을 고른다\n2. [실행] 을 누른다\n3. 확인 창에서 확인을 누른다\n4. 끝난 뒤 결과와 opportunities 행, SAM 호출 기록, Mock 요청 기록을 본다',
     expect='1. 확인 창에 "SAM 검색 1회를 씁니다" 가 있다\n'
            '2. 도는 동안 [실행] 단추가 잠겨 있다\n'
            '3. 결과에 SAM 호출 수, 받은 낙찰 공지, 업체 채운 행, 새로 넣은 행, 업체 이름 없는 행 다섯 숫자가 있다\n'
            '4. 다섯 숫자가 응답 samCalls 1, fetched 4, filled 2, created 1, withoutAwardee 1 과 같다\n'
            '5. 기존 낙찰 공지 2건에 awardee_name, awardee_uei, award_amount_usd 가 채워져 있다\n'
            '6. 새 낙찰 공지 1건이 opportunities 에 새 행으로 들어 있다\n'
            '7. SAM 호출 기록에 이번 검색 호출 1건이 남아 있다\n'
            '8. Mock 이 받은 검색 조건이 POL-002 발주처 조건과 낙찰 공지 유형이다',
     judge=['가상', '가상', '가상', '서버', '서버', '서버', '서버', '서버'],
     req=['BID-MKT-008'], fc=['BID-MKT-008'], screen='관리자 /collection', pri='Medium'),

dict(id='IT-07-006', flow='지난 낙찰 공지 다시 받기', kind='정상', title='검색 결과가 1000건을 넘으면 다음 쪽을 부른다',
     pre='관리자, SAM 검색 Mock 에 그 기간 낙찰 공지 1500건',
     steps='1. 지난 낙찰 공지 다시 받기에서 기간을 고르고 [실행] 을 눌러 확인한다',
     expect='1. 응답 samCalls 가 2 다\n'
            '2. 응답 fetched 가 1500 이다',
     judge=['서버', '서버'],
     req=['BID-MKT-008'], fc=['BID-MKT-008'], screen='관리자 /collection', pri='Medium',
     issue='T3-3·T3-4 확인 창은 "SAM 검색 1회" 로 적었고 T1-1 은 1000건이 넘으면 다음 쪽을 부른다고 적는다'),

dict(id='IT-07-007', flow='지난 낙찰 공지 다시 받기', kind='예외', title='공지일 시작이 끝보다 뒤면 400 이고 실행하지 않는다',
     pre='관리자',
     steps='1. 지난 낙찰 공지 다시 받기에서 공지일 시작을 끝보다 뒤 날짜로 고른다\n2. [실행] 을 눌러 확인한다',
     expect='1. 응답이 400 이다\n'
            '2. 결과 자리에 사유 한 줄이 있다\n'
            '3. SAM 호출 기록과 낙찰 공지 행이 그대로다',
     judge=['서버', '가상', '서버'],
     req=['BID-MKT-008'], fc=['BID-MKT-008'], screen='관리자 /collection', pri='Medium'),

dict(id='IT-07-008', flow='지난 낙찰 공지 다시 받기', kind='예외', title='기간이 한 해를 넘으면 400 이다',
     pre='관리자',
     steps='1. 공지일 시작 2025-01-01, 공지일 끝 2026-03-01 로 고르고 [실행] 을 눌러 확인한다',
     expect='1. 응답이 400 이다\n'
            '2. SAM 호출 기록이 늘지 않는다',
     judge=['서버', '서버'],
     req=['BID-MKT-008'], fc=['BID-MKT-008'], screen='관리자 /collection', pri='Medium'),

dict(id='IT-07-009', flow='지난 낙찰 공지 다시 받기', kind='예외', title='SAM 이 한도 초과나 인증 실패를 주면 502 와 사유를 보이고 채우지 않는다',
     pre='관리자, SAM 검색 Mock 이 한도 초과(429)를 주게 둔 상태',
     steps='1. 지난 낙찰 공지 다시 받기에서 기간을 고르고 [실행] 을 눌러 확인한다',
     expect='1. 응답이 502 다\n'
            '2. 결과 자리에 SAM 이 준 사유 한 줄이 있다\n'
            '3. 업체 칸을 채운 낙찰 공지 행이 없다',
     judge=['서버', '가상', '서버'],
     req=['BID-MKT-008'], fc=['BID-MKT-008'], screen='관리자 /collection', pri='Medium'),

dict(id='IT-07-010', flow='지난 낙찰 공지 다시 받기', kind='예외', title='관리자가 아닌 계정은 다시 받기를 실행하지 못한다',
     pre='회원 토큰',
     steps='1. 회원 토큰으로 POST /admin/opportunities/award-backfill 에 postedFrom, postedTo 를 보낸다',
     expect='1. 요청이 거부된다\n'
            '2. SAM 호출 기록이 늘지 않는다',
     judge=['서버', '서버'],
     req=['BID-MKT-008'], fc=['BID-MKT-008'], screen='', pri='Medium'),

dict(id='IT-07-011', flow='낙찰 업체', kind='정상', title='비로그인으로 낙찰 업체를 열면 업체 이름 있는 공지만 업체별로 묶어 보인다',
     pre='비로그인, 업체 이름 있는 낙찰 공지 여럿과 업체 이름 없는 낙찰 공지 1건 이상, 금액 없는 공지 1건 이상',
     steps='1. 낙찰정보 묶음 메뉴에서 [낙찰 업체] 를 누른다\n2. 같은 때 GET /opportunities/awardees 응답을 받아 둔다',
     expect='1. 로그인 화면을 거치지 않고 /awards/awardees 가 열린다\n'
            '2. 제목 줄에 "낙찰 업체" 와 "미군 낙찰 공지에 이름이 오른 업체입니다" 가 있다\n'
            '3. 제목 글자가 32px 이다\n'
            '4. 자료 기준이 옅은 회색 칸에 정보 아이콘과 함께 있다\n'
            '5. 자료 기준에 공지일 기간, 낙찰 공지 n건 중 업체 이름이 있는 n건, "업체 이름이 오지 않은 낙찰 공지 n건" 이 있다\n'
            '6. 자료 기준 건수가 서버 응답 값과 같다\n'
            '7. 업체 표 칸이 순위, 업체 이름, 낙찰 건수, 금액 합계, 최근 공지일 순서다\n'
            '8. 업체 이름이 SAM 영문 그대로다\n'
            '9. 표가 낙찰 건수 많은 순이다\n'
            '10. 업체 이름 없는 낙찰 공지는 어느 업체 줄에도 들어 있지 않다\n'
            '11. 업체마다 금액 합계가 그 업체 공지 중 금액이 있는 것만 더한 값이다\n'
            '12. 아래 띠의 채운 단추는 [무료로 시작] 이다',
     judge=['가상', '가상', '가상', '가상', '가상', '서버', '가상', '가상', '가상', '서버', '서버', '가상'],
     req=['BID-MKT-009'], fc=['BID-MKT-009'], screen='/awards/awardees', pri='Medium',
     issue='T3-3 공통은 아래 띠를 무료로 시작 또는 마감 전 공고로 두고 화면 소스는 "낙찰 업체가 낸 공고를 미리 받아 보세요" 띠를 쓴다'),

dict(id='IT-07-012', flow='낙찰 업체', kind='정상', title='UEI 가 같은 낙찰 공지는 이름이 달라도 한 업체로 묶는다',
     pre='비로그인, UEI 가 같고 업체 이름이 "ACME KOREA" 와 "ACME KOREA CO" 로 다른 낙찰 공지 2건(뒤의 것이 더 최근)',
     steps='1. /awards/awardees 를 연다\n2. 그 업체 줄을 찾는다',
     expect='1. 두 공지가 한 줄로 묶여 낙찰 건수가 2 다\n'
            '2. 그 줄 업체 이름이 더 최근 공지의 이름 "ACME KOREA CO" 다\n'
            '3. 응답 그 업체 key 가 UEI 로 시작하는 묶음 키다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-009'], fc=['BID-MKT-009'], screen='/awards/awardees', pri='Medium'),

dict(id='IT-07-013', flow='낙찰 업체', kind='정상', title='UEI 가 없으면 대문자와 공백, 마침표·쉼표를 맞춘 이름으로 묶는다',
     pre='비로그인, UEI 없이 업체 이름이 "Acme Korea Co." 와 " ACME KOREA, CO " 인 낙찰 공지 2건',
     steps='1. /awards/awardees 를 연다\n2. 그 업체 줄을 찾는다',
     expect='1. 두 공지가 한 줄로 묶여 낙찰 건수가 2 다\n'
            '2. 응답 그 업체 key 가 NAME 으로 시작하는 묶음 키다',
     judge=['가상', '서버'],
     req=['BID-MKT-009'], fc=['BID-MKT-009'], screen='/awards/awardees', pri='Medium'),

dict(id='IT-07-014', flow='낙찰 업체', kind='정상', title='업체 이름을 소문자로 찾아도 그 글자가 든 업체만 남고 순위는 그대로다',
     pre='비로그인, 이름에 ACME 가 든 업체가 있음',
     steps='1. /awards/awardees 에서 찾기 전 ACME 업체의 순위를 적어 둔다\n2. 업체 이름으로 찾기 칸에 acme 를 넣고 엔터를 누른다\n3. 새로고침한다',
     expect='1. 이름에 ACME 가 든 업체 줄만 남는다\n'
            '2. ACME 업체 순위가 찾기 전과 같다\n'
            '3. 주소에 q=acme 가 남는다\n'
            '4. 새로고침 뒤에도 찾는 말과 결과가 그대로다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-MKT-009'], fc=['BID-MKT-009'], screen='/awards/awardees', pri='Medium'),

dict(id='IT-07-015', flow='낙찰 업체', kind='정상', title='찾은 업체가 0건이면 빈 상태와 찾기 풀기가 있다',
     pre='비로그인',
     steps='1. /awards/awardees 찾기 칸에 zzzz 를 넣고 찾는다\n2. [찾기 풀기] 를 누른다',
     expect='1. 표 자리에 빈 상태와 [찾기 풀기] 가 있다\n'
            '2. 찾기 풀기를 누르면 찾기 칸이 비고 업체 줄이 전부 돌아온다',
     judge=['가상', '가상'],
     req=['BID-MKT-009'], fc=['BID-MKT-009'], screen='/awards/awardees', pri='Medium'),

dict(id='IT-07-016', flow='낙찰 업체', kind='정상', title='금액순은 금액 합계 큰 순이고 금액 공개 0건 업체가 맨 뒤다',
     pre='비로그인, 낙찰 공지 금액이 모두 없는 업체 1개 이상',
     steps='1. /awards/awardees 에서 정렬을 금액순으로 바꾼다\n2. 표 끝까지 내린다',
     expect='1. 표가 금액 합계 큰 순이다\n'
            '2. 금액 공개 0건 업체가 표 맨 뒤에 모여 있다\n'
            '3. 금액 공개 0건 업체의 금액 합계 칸이 회색 "공개 안 됨" 이다\n'
            '4. 주소에 sort 값이 남는다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-MKT-009'], fc=['BID-MKT-009'], screen='/awards/awardees', pri='Medium'),

dict(id='IT-07-017', flow='낙찰 업체', kind='정상', title='업체 줄을 누르면 그 업체의 낙찰 공지를 공지일 최신순으로 펼친다',
     pre='비로그인, 낙찰 공지 3건 이상이고 그중 금액 없는 공지가 있는 업체',
     steps='1. /awards/awardees 에서 그 업체 줄을 누른다\n2. 같은 업체 줄을 다시 누른다',
     expect='1. 그 업체 아래에 낙찰 공지가 공지일 최신순으로 펼쳐진다\n'
            '2. 펼친 줄마다 공지일, 제목, 공고번호, 금액이 있다\n'
            '3. 한글 제목이 있는 줄은 한글 제목이 굵게, 원문 제목이 작게 있다\n'
            '4. 한글 제목이 없는 줄은 원문 제목이 굵게 있다\n'
            '5. 공고번호가 IBM Plex Mono 글꼴이다\n'
            '6. 금액 없는 공지의 금액 칸은 회색 "공개 안 됨" 이다\n'
            '7. 펼친 공지 줄이 응답 그 업체 awards 와 같다\n'
            '8. 다시 누르면 그 업체의 낙찰 공지가 접힌다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '서버', '가상'],
     req=['BID-MKT-009'], fc=['BID-MKT-009'], screen='/awards/awardees', pri='Medium'),

dict(id='IT-07-018', flow='낙찰 업체', kind='정상', title='공개 공고문이 있는 펼친 공지 줄은 로그인을 거쳐 공고 상세로 간다',
     pre='비로그인에서 시작, 회원 계정 1개, 공개 한글 공고문이 있는 낙찰 공지를 가진 업체',
     steps='1. /awards/awardees 에서 acme 로 찾고 금액순으로 바꾼다\n2. 그 업체 줄을 펼치고 그 공지 줄을 누른다\n3. 회원 계정으로 로그인한다\n4. 브라우저 뒤로 가기로 낙찰 업체로 돌아온다',
     expect='1. 공지 줄을 누르면 로그인 화면으로 간다\n'
            '2. 로그인 뒤 그 공지의 공고 상세 /search/{공고문 id} 가 열린다\n'
            '3. 뒤로 오면 찾는 말 acme 와 금액순이 그대로다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-009'], fc=['BID-MKT-009'], screen='/awards/awardees', pri='Medium'),

dict(id='IT-07-019', flow='낙찰 업체', kind='정상', title='공개 공고문이 없는 펼친 공지 줄은 원본 공고 상세로 간다',
     pre='회원, 공개 공고문이 없는 낙찰 공지를 가진 업체',
     steps='1. 회원 계정으로 로그인한다\n2. /awards/awardees 에서 그 업체 줄을 펼치고 그 공지 줄을 누른다',
     expect='1. 로그인한 상태의 낙찰 업체 아래 띠 채운 단추는 [마감 전 공고] 다\n'
            '2. 그 공지의 원본 공고 상세 /search/origin/{원본 공고 id} 가 열린다\n'
            '3. 머리에 [SAM.gov 원문 보기] 단추가 있다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-009'], fc=['BID-MKT-009'], screen='/awards/awardees', pri='Medium'),

dict(id='IT-07-020', flow='낙찰 업체', kind='정상', title='업체 이름 있는 낙찰 공지가 0건이면 모으고 있다는 빈 상태다',
     pre='비로그인, 업체 이름 있는 낙찰 공지 0건(응답 awardsWithAwardee 0)',
     steps='1. /awards/awardees 를 연다\n2. [최근 낙찰] 을 누른다',
     expect='1. 표 대신 "낙찰 업체 자료를 모으고 있습니다" 가 있다\n'
            '2. 최근 낙찰을 누르면 주소가 /awards/recent 로 바뀐다',
     judge=['가상', '가상'],
     req=['BID-MKT-009'], fc=['BID-MKT-009'], screen='/awards/awardees', pri='Medium'),

dict(id='IT-07-021', flow='낙찰 업체', kind='예외', title='업체 목록을 못 받으면 표 자리에 다시 시도가 있다',
     pre='비로그인, GET /opportunities/awardees 가 실패하게 막은 상태',
     steps='1. /awards/awardees 를 연다',
     expect='1. 표 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 있다',
     judge=['가상'],
     req=['BID-MKT-009'], fc=['BID-MKT-009'], screen='/awards/awardees', pri='Medium'),

dict(id='IT-07-022', flow='기지 관리', kind='정상', title='처음 배포한 기지 관리는 기지 0개와 기지를 못 찾은 공고 목록을 보인다',
     pre='관리자, V44 직후 military_bases 0행, 모은 공고 여럿',
     steps='1. 관리자 화면 왼쪽 메뉴 [기지 관리] 를 누른다\n2. GET /admin/bases/unmatched 응답을 받아 둔다',
     expect='1. military_bases 가 0행이다\n'
            '2. 기지 표 칸이 순서, 한글 이름, 영문 이름, 찾을 낱말, 지역, 위치, 맞는 공고 순서다\n'
            '3. 기지를 못 찾은 공고에 원문 제목과 건수가 게시일 최신순으로 있다\n'
            '4. 못 찾은 공고 건수와 줄이 서버 응답과 같고 낙찰 공지가 없다',
     judge=['서버', '가상', '가상', '서버'],
     req=['BID-MKT-010'], fc=['BID-MKT-010'], screen='관리자 /bases', pri='Medium'),

dict(id='IT-07-023', flow='기지 관리', kind='정상', title='기지를 추가하면 표와 못 찾은 공고가 다시 불러와지고 맞는 공고 수가 생긴다',
     pre='관리자, 제목에 Humphreys 가 든 공고 3개 공고번호',
     steps='1. 관리자 /bases 에서 [기지 추가] 를 누른다\n2. 한글 이름 캠프 험프리스, 영문 이름 Camp Humphreys, 찾을 낱말 Humphreys, Pyeongtaek, 지역 평택, 위도 36.9634, 경도 127.0317 을 넣는다\n3. [저장] 을 누른다',
     expect='1. 추가 창이 빈 칸으로 열리고 한글 이름, 영문 이름, 찾을 낱말에 필수 표시가 있다\n'
            '2. 저장하면 창이 닫히고 표에 Camp Humphreys 줄이 있다\n'
            '3. Camp Humphreys 줄 맞는 공고 수가 3 이다\n'
            '4. 기지를 못 찾은 공고에서 그 공고 3개가 빠져 있다\n'
            '5. military_bases 에 name_en Camp Humphreys, match_terms Humphreys,Pyeongtaek, 위도·경도가 든 1행이 있다',
     judge=['가상', '가상', '가상', '가상', '서버'],
     req=['BID-MKT-010'], fc=['BID-MKT-010'], screen='관리자 /bases', pri='Medium'),

dict(id='IT-07-024', flow='기지 관리', kind='정상', title='위도와 경도를 둘 다 비운 기지는 지도에 안 나옴으로 표시된다',
     pre='관리자',
     steps='1. 관리자 /bases 에서 [기지 추가] 로 위도와 경도를 비운 기지 Camp Mobile 을 저장한다',
     expect='1. 그 줄 위치 칸에 "지도에 안 나옴" 이 있다\n'
            '2. military_bases 그 행 latitude, longitude 가 NULL 이다',
     judge=['가상', '서버'],
     req=['BID-MKT-010'], fc=['BID-MKT-010'], screen='관리자 /bases', pri='Medium'),

dict(id='IT-07-025', flow='기지 관리', kind='정상', title='찾을 낱말을 고치면 모든 공고에 바로 다시 적용된다',
     pre='관리자, 기지 Camp Humphreys(찾을 낱말 Humphreys), 제목에 Pyeongtaek 만 든 공고 2개 공고번호',
     steps='1. 관리자 /bases 에서 Camp Humphreys 줄 [고치기] 를 누른다\n2. 찾을 낱말에 Pyeongtaek 를 더하고 [저장] 을 누른다\n3. 고객 화면 /analysis/base-map 을 연다',
     expect='1. Camp Humphreys 맞는 공고 수가 2 늘어난다\n'
            '2. 기지를 못 찾은 공고에서 그 공고 2개가 빠진다\n'
            '3. 고객 기지별 지도 표 Camp Humphreys 공고 수가 관리자 맞는 공고 수와 같다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-010', 'BID-MKT-011'], fc=['BID-MKT-010', 'BID-MKT-011'], screen='관리자 /bases', pri='Medium'),

dict(id='IT-07-026', flow='기지 관리', kind='정상', title='기지를 지우면 확인 창을 거쳐 고객 화면에서도 빠진다',
     pre='관리자, 기지 2개 이상',
     steps='1. 관리자 /bases 에서 한 기지 줄 [지우기] 를 누른다\n2. 확인 창에서 지운다\n3. 고객 화면 /analysis/base-map 을 연다',
     expect='1. 지우기를 누르면 확인 창이 있다\n'
            '2. 확인 뒤 표에서 그 기지 줄이 빠진다\n'
            '3. military_bases 그 행이 없다\n'
            '4. 고객 기지별 지도 표와 지도에 그 기지가 없다',
     judge=['가상', '가상', '서버', '가상'],
     req=['BID-MKT-010'], fc=['BID-MKT-010'], screen='관리자 /bases', pri='Medium'),

dict(id='IT-07-027', flow='기지 관리', kind='예외', title='위도만 넣고 저장하면 400 과 함께 넣으라는 사유가 나온다',
     pre='관리자',
     steps='1. 관리자 /bases 에서 [기지 추가] 로 필수 칸과 위도 37.0 만 넣고 경도는 비운 채 [저장] 을 누른다',
     expect='1. 응답이 400 이다\n'
            '2. 창 안에 "위도와 경도를 함께 넣으세요" 가 있다\n'
            '3. 창이 닫히지 않고 military_bases 행 수가 그대로다',
     judge=['서버', '가상', '서버'],
     req=['BID-MKT-010'], fc=['BID-MKT-010'], screen='관리자 /bases', pri='Medium'),

dict(id='IT-07-028', flow='기지 관리', kind='예외', title='이미 있는 영문 이름으로 저장하면 409 와 영문 이름 칸 아래 사유가 나온다',
     pre='관리자, 기지 Camp Humphreys 가 있음',
     steps='1. 관리자 /bases 에서 [기지 추가] 로 영문 이름 Camp Humphreys 를 넣고 [저장] 을 누른다',
     expect='1. 응답이 409 다\n'
            '2. 영문 이름 칸 아래에 서버가 준 사유가 있다\n'
            '3. military_bases 행 수가 그대로다',
     judge=['서버', '가상', '서버'],
     req=['BID-MKT-010'], fc=['BID-MKT-010'], screen='관리자 /bases', pri='Medium'),

dict(id='IT-07-029', flow='기지 관리', kind='예외', title='찾을 낱말이 0개면 400 이다',
     pre='관리자',
     steps='1. 관리자 /bases 에서 [기지 추가] 로 한글 이름과 영문 이름만 넣고 찾을 낱말을 비운 채 저장한다',
     expect='1. 저장되지 않고 찾을 낱말 칸 아래에 사유가 있다\n'
            '2. POST /admin/bases 에 matchTerms 빈 배열을 보내면 응답이 400 이다',
     judge=['가상', '서버'],
     req=['BID-MKT-010'], fc=['BID-MKT-010'], screen='관리자 /bases', pri='Medium'),

dict(id='IT-07-030', flow='기지 관리', kind='예외', title='이름이 비면 400 이다',
     pre='관리자 토큰',
     steps='1. POST /admin/bases 에 nameKo 를 빈 값으로, nameEn 과 matchTerms 를 넣어 보낸다',
     expect='1. 응답이 400 이다\n'
            '2. military_bases 행 수가 그대로다',
     judge=['서버', '서버'],
     req=['BID-MKT-010'], fc=['BID-MKT-010'], screen='관리자 /bases', pri='Medium'),

dict(id='IT-07-031', flow='기지 관리', kind='예외', title='위도나 경도가 범위 밖이면 400 이다',
     pre='관리자',
     steps='1. 관리자 /bases 에서 [기지 추가] 로 필수 칸과 위도 91, 경도 127 을 넣고 [저장] 을 누른다\n2. 위도 37, 경도 181 로 바꿔 [저장] 을 누른다',
     expect='1. 위도 91 은 응답이 400 이다\n'
            '2. 경도 181 은 응답이 400 이다\n'
            '3. military_bases 행 수가 그대로다',
     judge=['서버', '서버', '서버'],
     req=['BID-MKT-010'], fc=['BID-MKT-010'], screen='관리자 /bases', pri='Medium'),

dict(id='IT-07-032', flow='기지 관리', kind='예외', title='관리자가 아닌 계정은 기지를 넣고 고치고 지우지 못한다',
     pre='회원 토큰, 기지 1개의 id',
     steps='1. 회원 토큰으로 POST /admin/bases 를 보낸다\n2. 회원 토큰으로 PUT /admin/bases/{id} 를 보낸다\n3. 회원 토큰으로 DELETE /admin/bases/{id} 를 보낸다',
     expect='1. 세 요청 모두 거부된다\n'
            '2. military_bases 행이 그대로다',
     judge=['서버', '서버'],
     req=['BID-MKT-010'], fc=['BID-MKT-010'], screen='', pri='Medium'),

dict(id='IT-07-033', flow='기지 관리', kind='예외', title='기지 목록을 못 받으면 표 자리에 다시 시도가 있다',
     pre='관리자, GET /admin/bases 가 실패하게 막은 상태',
     steps='1. 관리자 /bases 를 연다',
     expect='1. 표 자리에 "불러오지 못했습니다." 와 [다시 시도] 가 있다',
     judge=['가상'],
     req=['BID-MKT-010'], fc=['BID-MKT-010'], screen='관리자 /bases', pri='Medium'),

dict(id='IT-07-034', flow='기지별 지도', kind='정상', title='비로그인으로 기지별 지도를 열면 공고 수 많은 기지가 골라진 채 열린다',
     pre='비로그인, 위치를 넣은 기지 3개, 기지를 알 수 없는 공고 1개 이상',
     steps='1. 발주분석 묶음 메뉴에서 [기지별 지도] 를 누른다\n2. 같은 때 GET /opportunities/stats/bases 응답을 받아 둔다',
     expect='1. 로그인 화면을 거치지 않고 /analysis/base-map 이 열린다\n'
            '2. 제목 줄에 "기지별 지도" 와 "주한미군 기지마다 공고와 낙찰이 얼마나 나왔는지 보여 드립니다" 가 있다\n'
            '3. 제목 글자가 32px 이다\n'
            '4. 자료 기준이 옅은 회색 칸에 정보 아이콘과 함께 있다\n'
            '5. 자료 기준에 게시일 기간, 공고번호 n개, "공고 제목에 기지 이름이 있는 공고만 셉니다. 기지를 알 수 없는 공고 n개" 가 있다\n'
            '6. 자료 기준 공고번호 수와 기지를 알 수 없는 공고 수가 응답 totalNumbers, unmatchedNumbers 와 같다\n'
            '7. 기지 표 칸이 기지 이름(한글, 영문 작게), 지역, 공고 수, 낙찰 수, 마감 전 공고 수 순서다\n'
            '8. 기지 표가 공고 수 많은 순이고 숫자가 응답 bases 값과 같다\n'
            '9. 공고 수가 가장 많은 기지가 골라져 아래에 그 기지 최근 공고가 있다\n'
            '10. 아래 띠의 채운 단추는 [무료로 시작] 이다',
     judge=['가상', '가상', '가상', '가상', '가상', '서버', '가상', '서버', '가상', '가상'],
     req=['BID-MKT-011'], fc=['BID-MKT-011'], screen='/analysis/base-map', pri='Medium'),

dict(id='IT-07-035', flow='기지별 지도', kind='정상', title='지도는 바깥 지도 서비스 없이 한국 남쪽 윤곽 위에 위치를 넣은 기지만 점으로 찍는다',
     pre='비로그인, 공고 수가 다른 위치 있는 기지 2개와 위치 없는 기지 1개, 브라우저 개발자 도구 네트워크 탭을 연 상태',
     steps='1. /analysis/base-map 을 연다\n2. 지도와 네트워크 탭을 본다',
     expect='1. 한국 남쪽 윤곽 그림 위에 위치 있는 기지 2개 점이 있다\n'
            '2. 공고 수가 많은 기지의 점이 더 크다\n'
            '3. 위치 없는 기지는 지도에 점이 없고 표에만 있다\n'
            '4. 화면을 여는 동안 바깥 지도 서비스로 가는 요청이 없다',
     judge=['합동', '합동', '가상', '가상'],
     req=['BID-MKT-011'], fc=['BID-MKT-011'], screen='/analysis/base-map', pri='Medium'),

dict(id='IT-07-036', flow='기지별 지도', kind='정상', title='표 줄을 누르면 점과 줄이 함께 강조되고 그 기지 최근 공고 10건이 나온다',
     pre='비로그인, 위치 있고 공고가 11건 이상인 기지',
     steps='1. /analysis/base-map 표에서 그 기지 줄을 누른다\n2. 같은 기지로 GET /opportunities/stats/bases/{baseId}/opportunities 응답을 받아 둔다\n3. 새로고침한다',
     expect='1. 그 기지 표 줄과 지도 점이 함께 강조된다\n'
            '2. 아래에 그 기지 최근 공고 10건이 유형 꼬리표, 제목, 공고번호, 게시일, 마감일과 함께 있다\n'
            '3. 한글 제목이 있는 공고 줄은 한글 제목이 굵게, 원문 제목이 작게 있다\n'
            '4. 공고번호가 IBM Plex Mono 글꼴이다\n'
            '5. 최근 공고 10건이 서버 응답과 같고 낙찰 공지가 없다\n'
            '6. 주소에 base 값이 남는다\n'
            '7. 새로고침 뒤에도 같은 기지가 골라져 있다',
     judge=['가상', '가상', '가상', '가상', '서버', '가상', '가상'],
     req=['BID-MKT-011'], fc=['BID-MKT-011'], screen='/analysis/base-map', pri='Medium'),

dict(id='IT-07-037', flow='기지별 지도', kind='정상', title='지도 점을 누르면 그 기지가 골라진다',
     pre='비로그인, 위치 있는 기지 2개 이상',
     steps='1. /analysis/base-map 에서 처음 골라진 기지가 아닌 다른 기지 점을 누른다',
     expect='1. 누른 점과 그 기지 표 줄이 함께 강조된다\n'
            '2. 아래 공고가 그 기지 최근 공고로 바뀐다',
     judge=['가상', '가상'],
     req=['BID-MKT-011'], fc=['BID-MKT-011'], screen='/analysis/base-map', pri='Medium'),

dict(id='IT-07-038', flow='기지별 지도', kind='정상', title='위치 없는 기지 줄을 누르면 지도 강조 없이 공고만 바뀐다',
     pre='비로그인, 위치 없는 기지 1개',
     steps='1. /analysis/base-map 표에서 위치 없는 기지 줄을 누른다',
     expect='1. 지도에 강조된 점이 없다\n'
            '2. 아래 공고가 그 기지 최근 공고로 바뀐다',
     judge=['가상', '가상'],
     req=['BID-MKT-011'], fc=['BID-MKT-011'], screen='/analysis/base-map', pri='Medium'),

dict(id='IT-07-039', flow='기지별 지도', kind='정상', title='기지는 제목의 낱말 단위로 대소문자 없이 찾는다',
     pre='비로그인, 찾을 낱말 Osan 인 기지, 제목이 "OSAN AB ROOF REPAIR" 인 공고와 "OSANVILLE SUPPLY" 인 공고',
     steps='1. /analysis/base-map 에서 그 기지를 고른다\n2. 같은 때 기지별 집계 응답을 받아 둔다',
     expect='1. "OSAN AB ROOF REPAIR" 공고가 그 기지 공고에 있다\n'
            '2. "OSANVILLE SUPPLY" 공고는 그 기지 공고에 없다\n'
            '3. "OSANVILLE SUPPLY" 공고는 기지를 알 수 없는 공고 수에 들어 있다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-011'], fc=['BID-MKT-011'], screen='/analysis/base-map', pri='Medium'),

dict(id='IT-07-040', flow='기지별 지도', kind='정상', title='한 공고 제목에 기지 둘이 나오면 두 기지에 모두 센다',
     pre='비로그인, 제목에 Humphreys 와 Osan 이 함께 든 공고 1건, 기지 Camp Humphreys 와 Osan AB',
     steps='1. /analysis/base-map 에서 Camp Humphreys 와 Osan AB 를 차례로 고른다\n2. 기지별 집계 응답을 본다',
     expect='1. 그 공고가 두 기지 최근 공고에 모두 있다\n'
            '2. 두 기지 opportunityCount 에 그 공고번호가 각각 들어 있다',
     judge=['가상', '서버'],
     req=['BID-MKT-011'], fc=['BID-MKT-011'], screen='/analysis/base-map', pri='Medium'),

dict(id='IT-07-041', flow='기지별 지도', kind='정상', title='낙찰 공지 제목에 기지가 없으면 같은 공고번호의 다른 공고 제목으로 찾아 낙찰 수에 센다',
     pre='비로그인, 제목에 Humphreys 가 든 입찰 공고와 같은 공고번호로 제목에 기지가 없는 낙찰 공지',
     steps='1. 기지별 집계 응답에서 Camp Humphreys 의 awardCount 를 본다\n2. /analysis/base-map 표에서 Camp Humphreys 낙찰 수를 본다',
     expect='1. awardCount 에 그 낙찰 공지가 들어 있다\n'
            '2. 표 낙찰 수가 awardCount 와 같다',
     judge=['서버', '가상'],
     req=['BID-MKT-011'], fc=['BID-MKT-011'], screen='/analysis/base-map', pri='Medium'),

dict(id='IT-07-042', flow='기지별 지도', kind='정상', title='공개 공고문이 있는 기지 공고 줄은 로그인을 거쳐 공고 상세로 간다',
     pre='비로그인에서 시작, 회원 계정 1개, 공개 한글 공고문이 있는 기지 공고 1건',
     steps='1. /analysis/base-map 에서 그 기지를 고르고 그 공고 줄을 누른다\n2. 회원 계정으로 로그인한다\n3. 브라우저 뒤로 가기로 기지별 지도로 돌아온다',
     expect='1. 줄을 누르면 로그인 화면으로 간다\n'
            '2. 로그인 뒤 그 공고의 /search/{공고문 id} 가 열린다\n'
            '3. 뒤로 오면 고른 기지가 그대로다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-011'], fc=['BID-MKT-011'], screen='/analysis/base-map', pri='Medium'),

dict(id='IT-07-043', flow='기지별 지도', kind='정상', title='공개 공고문이 없는 기지 공고 줄은 원본 공고 상세로 간다',
     pre='회원, 공개 공고문이 없는 기지 공고 1건',
     steps='1. 회원 계정으로 로그인한다\n2. /analysis/base-map 에서 그 기지를 고르고 그 공고 줄을 누른다',
     expect='1. 그 공고의 /search/origin/{원본 공고 id} 가 열린다\n'
            '2. 머리에 [SAM.gov 원문 보기] 단추가 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-011'], fc=['BID-MKT-011'], screen='/analysis/base-map', pri='Medium'),

dict(id='IT-07-044', flow='기지별 지도', kind='정상', title='기지 목록이 비면 숫자 대신 빈 상태를 보인다',
     pre='비로그인, military_bases 0행',
     steps='1. /analysis/base-map 을 연다\n2. [마감 전 공고] 를 누른다',
     expect='1. 응답 bases 가 빈 배열이다\n'
            '2. 지도와 표 대신 "기지 목록을 준비하고 있습니다" 가 있다\n'
            '3. 공고 수 숫자가 화면에 없다\n'
            '4. 마감 전 공고를 누르면 주소가 /search 로 바뀐다',
     judge=['서버', '가상', '가상', '가상'],
     req=['BID-MKT-011'], fc=['BID-MKT-011'], screen='/analysis/base-map', pri='Medium'),

dict(id='IT-07-045', flow='기지별 지도', kind='예외', title='기지별 집계를 못 받으면 지도와 표 자리에 다시 시도가 있다',
     pre='비로그인, GET /opportunities/stats/bases 가 실패하게 막은 상태',
     steps='1. /analysis/base-map 을 연다',
     expect='1. 지도와 표 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 있다',
     judge=['가상'],
     req=['BID-MKT-011'], fc=['BID-MKT-011'], screen='/analysis/base-map', pri='Medium'),

dict(id='IT-07-046', flow='기지별 지도', kind='예외', title='고른 기지 공고를 못 받으면 그 자리만 다시 시도다',
     pre='비로그인, GET /opportunities/stats/bases/{baseId}/opportunities 만 실패하게 막은 상태',
     steps='1. /analysis/base-map 에서 기지 줄 하나를 누른다',
     expect='1. 고른 기지 공고 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 있다\n'
            '2. 지도와 기지 표는 그대로 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-011'], fc=['BID-MKT-011'], screen='/analysis/base-map', pri='Medium'),

dict(id='IT-07-047', flow='기지별 지도', kind='예외', title='없는 기지 id 로 공고를 부르면 404 다',
     pre='비로그인',
     steps='1. GET /opportunities/stats/bases/{없는 UUID}/opportunities 를 부른다',
     expect='1. 응답이 404 다',
     judge=['서버'],
     req=['BID-MKT-011'], fc=['BID-MKT-011'], screen='', pri='Medium'),

dict(id='IT-07-048', flow='서류 만료일', kind='정상', title='기타 서류를 올릴 때 만료일을 넣으면 서류함 줄에 만료일과 남은 날이 보인다',
     pre='회원, 오늘보다 40일 뒤 날짜를 만료일로 씀',
     steps='1. 회원 계정으로 /documents 를 연다\n2. [기타 서류 추가] 창에서 파일과 이름을 넣고 만료일 칸에서 40일 뒤 날짜를 고른다\n3. 저장한다',
     expect='1. 기타 서류 추가 창에 선택 칸인 만료일 날짜 고르기 칸이 있다\n'
            '2. 기타 서류 줄에 그 만료일과 "D-40" 이 한 줄로 있다\n'
            '3. member_documents 그 행 expires_on 이 고른 날짜다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-012'], fc=['BID-MKT-012'], screen='/documents', pri='Medium'),

dict(id='IT-07-049', flow='서류 만료일', kind='정상', title='기타 서류 고치기 창에서 만료일을 비우면 만료일 없음이 된다',
     pre='회원, 만료일이 있는 기타 서류 1건',
     steps='1. /documents 에서 그 기타 서류 고치기 창을 연다\n2. 만료일 칸을 비우고 저장한다',
     expect='1. 기타 서류 줄에 만료일과 남은 날이 없다\n'
            '2. member_documents 그 행 expires_on 이 NULL 이다',
     judge=['가상', '서버'],
     req=['BID-MKT-012'], fc=['BID-MKT-012'], screen='/documents', pri='Medium'),

dict(id='IT-07-050', flow='서류 만료일', kind='정상', title='고정 카드의 만료일과 서류함 머리 바로가기는 유효기간 알림으로 간다',
     pre='회원, 고정 카드에 서류가 올라 있음',
     steps='1. /documents 에서 고정 카드의 [만료일] 을 누른다\n2. /documents 로 돌아와 머리의 유효기간 알림 바로가기를 누른다',
     expect='1. 고정 카드 만료일을 누르면 주소가 /documents/expiry 로 바뀐다\n'
            '2. 머리 바로가기를 누르면 주소가 /documents/expiry 로 바뀐다\n'
            '3. 고정 카드에는 만료일을 넣는 창이 열리지 않는다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-012'], fc=['BID-MKT-012'], screen='/documents', pri='Medium',
     issue='T1-1 은 올리기·바꾸기 창에 만료일 칸을 둔다고 적고 T3-3 은 기타 서류 창에만 두고 고정 카드는 유효기간 알림에서 넣는다고 적는다'),

dict(id='IT-07-051', flow='서류 만료일', kind='정상', title='유효기간 알림에서 넣은 고정 카드 만료일이 서류함 카드에 보인다',
     pre='회원, 사업자등록증 고정 카드에 서류가 올라 있고 만료일 없음',
     steps='1. /documents/expiry 에서 사업자등록증 줄에 오늘보다 10일 뒤 만료일을 넣고 저장한다\n2. /documents 로 돌아와 사업자등록증 카드를 본다',
     expect='1. 사업자등록증 카드에 그 만료일과 "D-10" 이 한 줄로 있다',
     judge=['가상'],
     req=['BID-MKT-012', 'BID-MKT-013'], fc=['BID-MKT-012', 'BID-MKT-013'], screen='/documents', pri='Medium'),

dict(id='IT-07-052', flow='서류 만료일', kind='정상', title='지난 날짜도 만료일로 넣을 수 있고 만료됨이 된다',
     pre='회원, 만료일 없는 서류 1건',
     steps='1. /documents/expiry 에서 그 서류 줄 만료일 칸을 누른다\n2. 어제 날짜를 고르고 저장한다\n3. /documents 로 가 그 서류를 본다',
     expect='1. 날짜 고르기 칸에서 어제 날짜를 고를 수 있다\n'
            '2. 저장 뒤 그 줄 남은 날 자리가 빨강 "만료됨" 이다\n'
            '3. member_documents 그 행 expires_on 이 어제 날짜다\n'
            '4. 서류함 그 서류 줄에 "만료됨" 이 있다',
     judge=['가상', '가상', '서버', '가상'],
     req=['BID-MKT-012', 'BID-MKT-013'], fc=['BID-MKT-012', 'BID-MKT-013'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-053', flow='서류 만료일', kind='정상', title='남은 날은 한국 날짜로 센다',
     pre='회원, 만료일이 한국 날짜 오늘인 서류 1건, 한국 시각 00시 30분(UTC 로는 전날)에 시험',
     steps='1. 한국 시각 00시 30분에 GET /member-documents/expiry 를 부른다\n2. /documents/expiry 에서 그 서류 줄을 본다',
     expect='1. 응답 today 가 한국 날짜 오늘이고 그 서류 daysLeft 가 0 이다\n'
            '2. 그 줄 남은 날이 "D-day" 다',
     judge=['서버', '가상'],
     req=['BID-MKT-012', 'BID-MKT-013'], fc=['BID-MKT-012', 'BID-MKT-013'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-054', flow='서류 만료일', kind='예외', title='남의 서류 만료일은 바꾸지 못한다',
     pre='회원 A 토큰, 회원 B 의 서류 id',
     steps='1. 회원 A 토큰으로 PATCH /member-documents/{B 의 서류 id}/expiry 에 expiresOn 2027-01-01 을 보낸다',
     expect='1. 응답이 403 또는 404 다\n'
            '2. B 서류의 expires_on 이 그대로다',
     judge=['서버', '서버'],
     req=['BID-MKT-012'], fc=['BID-MKT-012'], screen='', pri='Medium'),

dict(id='IT-07-055', flow='서류 만료일', kind='예외', title='만료일 날짜 형식이 틀리면 400 이다',
     pre='회원 토큰, 본인 서류 id',
     steps='1. PATCH /member-documents/{id}/expiry 에 expiresOn 2027/01/01 을 보낸다\n2. POST /member-documents 에 expiresOn 20270101 을 넣어 기타 서류를 올린다',
     expect='1. PATCH 응답이 400 이다\n'
            '2. POST 응답이 400 이다\n'
            '3. 그 서류 expires_on 이 그대로다',
     judge=['서버', '서버', '서버'],
     req=['BID-MKT-012'], fc=['BID-MKT-012'], screen='', pri='Medium'),

dict(id='IT-07-056', flow='유효기간 알림', kind='정상', title='회원이 유효기간 알림을 열면 알림 시점과 서류별 만료 상태를 본다',
     pre='회원, DOC_EXPIRY_ALERT_DAYS "30,7", 만료됨 1건, 곧 만료 1건, 만료일 없음 1건인 서류, 다른 회원 서류 여럿',
     steps='1. 서류함 묶음 메뉴에서 [유효기간 알림] 을 누른다\n2. 같은 때 GET /member-documents/expiry 응답을 받아 둔다',
     expect='1. 주소가 /documents/expiry 다\n'
            '2. 제목 줄에 "만료일을 넣어 두면 만료 30일 전과 7일 전 오전 9시(한국 시각)에 이메일로 알려 드립니다" 가 있다\n'
            '3. 받는 이메일이 로그인한 회원 이메일이다\n'
            '4. 요약 세 칸이 빨강 만료됨 1건, 곧 만료 1건, 만료일 없음 1건이다\n'
            '5. 요약 세 칸 숫자가 응답 summary 와 같다\n'
            '6. 서류 표 칸이 서류 이름, 종류, 만료일, 남은 날, 마지막 알림 날 순서다\n'
            '7. 종류 칸에 고정 카드 이름이나 기타 서류가 있다\n'
            '8. 응답 documents 에 다른 회원 서류가 없다\n'
            '9. 제목 글자가 32px 이다\n'
            '10. 아래 띠의 채운 단추는 [마감 전 공고] 다',
     judge=['가상', '가상', '가상', '가상', '서버', '가상', '가상', '서버', '가상', '가상'],
     req=['BID-MKT-013'], fc=['BID-MKT-013'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-057', flow='유효기간 알림', kind='정상', title='비로그인은 로그인 화면을 거쳐 유효기간 알림으로 돌아온다',
     pre='비로그인, 회원 계정 1개',
     steps='1. 주소창에 /documents/expiry 를 넣어 연다\n2. 회원 계정으로 로그인한다',
     expect='1. 로그인 화면으로 간다\n'
            '2. 로그인 뒤 주소가 /documents/expiry 로 돌아온다',
     judge=['가상', '가상'],
     req=['BID-MKT-013'], fc=['BID-MKT-013'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-058', flow='유효기간 알림', kind='정상', title='곧 만료는 알림 시점 중 가장 긴 날 안이고 경계 날짜가 상태를 가른다',
     pre='회원, DOC_EXPIRY_ALERT_DAYS "30,7", 만료일이 어제, 오늘, 30일 뒤, 31일 뒤인 서류 각 1건',
     steps='1. /documents/expiry 를 연다\n2. 같은 때 GET /member-documents/expiry 응답을 받아 둔다',
     expect='1. 응답 soonDays 가 30 이다\n'
            '2. 어제 서류는 status EXPIRED 이고 남은 날 자리가 빨강 "만료됨" 이다\n'
            '3. 오늘 서류는 status SOON 이고 남은 날이 "D-day" 다\n'
            '4. 30일 뒤 서류는 status SOON 이고 남은 날 "D-30" 이 빨강 아닌 진한 먹색이다\n'
            '5. 31일 뒤 서류는 status OK 이고 남은 날이 "D-31" 이다\n'
            '6. 요약 곧 만료 칸이 2건이다',
     judge=['서버', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-MKT-013'], fc=['BID-MKT-013'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-059', flow='유효기간 알림', kind='정상', title='표에서 만료일을 넣고 고치면 남은 날과 요약이 바뀐다',
     pre='회원, 만료일 없는 서류 1건',
     steps='1. /documents/expiry 에서 그 서류 줄 [만료일 넣기] 를 누른다\n2. 오늘보다 5일 뒤 날짜를 고르고 [저장] 을 누른다\n3. 그 줄 만료일 칸을 다시 눌러 60일 뒤로 바꾸고 [저장] 을 누른다',
     expect='1. 날짜 고르기 칸이 열린다\n'
            '2. 저장하면 그 줄 남은 날이 "D-5" 로 바뀐다\n'
            '3. 요약 곧 만료가 1 늘고 만료일 없음이 1 준다\n'
            '4. 60일 뒤로 고치면 그 줄 남은 날이 "D-60" 이고 요약 곧 만료가 1 준다\n'
            '5. member_documents 그 행 expires_on 이 60일 뒤 날짜다',
     judge=['가상', '가상', '가상', '가상', '서버'],
     req=['BID-MKT-013', 'BID-MKT-012'], fc=['BID-MKT-013', 'BID-MKT-012'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-060', flow='유효기간 알림', kind='정상', title='표에서 만료일을 지우면 만료일 없음이 된다',
     pre='회원, 만료일이 있는 서류 1건',
     steps='1. /documents/expiry 에서 그 서류 줄 [지우기] 를 누른다',
     expect='1. PATCH 본문이 expiresOn null 이다\n'
            '2. 그 줄 만료일과 남은 날이 비고 상태가 만료일 없음이다\n'
            '3. 요약 만료일 없음이 1 는다\n'
            '4. member_documents 그 행 expires_on 이 NULL 이다',
     judge=['서버', '가상', '가상', '서버'],
     req=['BID-MKT-013', 'BID-MKT-012'], fc=['BID-MKT-013', 'BID-MKT-012'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-061', flow='유효기간 알림', kind='정상', title='요약 칸을 누르면 그 상태로 거르고 다시 누르면 푼다',
     pre='회원, 만료됨 서류 1건과 다른 상태 서류 2건',
     steps='1. /documents/expiry 요약의 만료됨 칸을 누른다\n2. 만료됨 칸을 다시 누른다',
     expect='1. 서류 표에 만료됨 서류 줄만 남는다\n'
            '2. 다시 누르면 서류 표에 세 줄이 모두 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-013'], fc=['BID-MKT-013'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-062', flow='유효기간 알림', kind='정상', title='알림 시점 설정을 바꾸면 안내 문구와 곧 만료 기준이 따라 바뀐다',
     pre='회원, DOC_EXPIRY_ALERT_DAYS 를 "14" 로 바꾼 상태, 만료일이 20일 뒤인 서류 1건',
     steps='1. /documents/expiry 를 연다',
     expect='1. 제목 줄 설명에 "만료 14일 전 오전 9시(한국 시각)에 이메일로 알려 드립니다" 가 있다\n'
            '2. 20일 뒤 서류가 곧 만료가 아니고 요약 곧 만료 칸에 들지 않는다',
     judge=['가상', '가상'],
     req=['BID-MKT-013'], fc=['BID-MKT-013'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-063', flow='유효기간 알림', kind='정상', title='알림 시점 설정이 비었거나 읽을 수 없으면 알림 꺼짐을 보인다',
     pre='회원, 만료일 있는 서류 1건',
     steps='1. DOC_EXPIRY_ALERT_DAYS 를 빈 값으로 두고 /documents/expiry 를 연다\n2. DOC_EXPIRY_ALERT_DAYS 를 "abc" 로 두고 다시 연다',
     expect='1. 빈 값일 때 제목 줄 설명 자리에 "지금은 만료 메일을 보내지 않습니다" 가 있다\n'
            '2. 빈 값일 때 응답 alertDays 가 빈 배열이고 soonDays 가 null 이다\n'
            '3. "abc" 일 때도 제목 줄 설명 자리에 "지금은 만료 메일을 보내지 않습니다" 가 있다\n'
            '4. 알림 꺼짐에서도 서류 줄의 만료일과 남은 날이 있다',
     judge=['가상', '서버', '가상', '가상'],
     req=['BID-MKT-013'], fc=['BID-MKT-013'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-064', flow='유효기간 알림', kind='정상', title='서류가 0건이면 서류함으로 보내는 빈 상태다',
     pre='회원, 서류함 서류 0건',
     steps='1. /documents/expiry 를 연다\n2. [공통 서류함] 을 누른다',
     expect='1. 표 대신 "서류함에 올린 서류가 없습니다" 가 있다\n'
            '2. 공통 서류함을 누르면 주소가 /documents 로 바뀐다',
     judge=['가상', '가상'],
     req=['BID-MKT-013'], fc=['BID-MKT-013'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-065', flow='유효기간 알림', kind='정상', title='서류는 있고 만료일이 0건이면 표 위에 한 줄 안내가 있다',
     pre='회원, 만료일 없는 서류 2건',
     steps='1. /documents/expiry 를 연다',
     expect='1. 표 위에 "만료일을 넣은 서류가 없습니다" 가 있다\n'
            '2. 서류 표에 두 서류 줄이 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-013'], fc=['BID-MKT-013'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-066', flow='유효기간 알림', kind='정상', title='서류 이름을 누르면 서류함으로 간다',
     pre='회원, 서류 1건 이상',
     steps='1. /documents/expiry 표에서 서류 이름을 누른다',
     expect='1. 주소가 /documents 로 바뀐다',
     judge=['가상'],
     req=['BID-MKT-013'], fc=['BID-MKT-013'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-067', flow='유효기간 알림', kind='정상', title='마지막 알림 날은 보내기에 성공한 마지막 알림 이력의 날이다',
     pre='회원, 만료 메일 보내기 성공 이력 1건과 그 뒤 실패 이력 1건이 있는 서류',
     steps='1. /documents/expiry 에서 그 서류 줄 마지막 알림 날을 본다\n2. 같은 때 응답 lastAlertedAt 과 알림 이력을 본다',
     expect='1. 응답 lastAlertedAt 이 성공 이력의 보낸 시각이다\n'
            '2. 그 줄 마지막 알림 날이 성공 이력의 날짜다',
     judge=['서버', '가상'],
     req=['BID-MKT-013', 'BID-MKT-014'], fc=['BID-MKT-013', 'BID-MKT-014'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-068', flow='유효기간 알림', kind='예외', title='만료일 저장이 실패하면 그 줄 아래 사유가 나오고 입력값이 남는다',
     pre='회원, PATCH /member-documents/{id}/expiry 가 실패하게 막은 상태',
     steps='1. /documents/expiry 에서 한 서류 줄 만료일 칸에 날짜를 고르고 [저장] 을 누른다',
     expect='1. 그 줄 아래에 사유 한 줄이 있다\n'
            '2. 날짜 고르기 칸에 고른 날짜가 그대로 있다\n'
            '3. member_documents 그 행 expires_on 이 그대로다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-013'], fc=['BID-MKT-013'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-069', flow='유효기간 알림', kind='예외', title='만료 상태를 못 받으면 다시 시도가 있다',
     pre='회원, GET /member-documents/expiry 가 실패하게 막은 상태',
     steps='1. /documents/expiry 를 연다',
     expect='1. "불러오지 못했습니다" 와 [다시 시도] 가 있다',
     judge=['가상'],
     req=['BID-MKT-013'], fc=['BID-MKT-013'], screen='/documents/expiry', pri='Medium'),

dict(id='IT-07-070', flow='유효기간 알림', kind='예외', title='로그인하지 않은 요청은 만료 상태를 받지 못한다',
     pre='비로그인',
     steps='1. 토큰 없이 GET /member-documents/expiry 를 부른다\n2. 토큰 없이 PATCH /member-documents/{id}/expiry 를 부른다',
     expect='1. 두 요청 모두 거부된다\n'
            '2. 서류 정보가 응답에 없다',
     judge=['서버', '서버'],
     req=['BID-MKT-013'], fc=['BID-MKT-013'], screen='', pri='Medium'),

dict(id='IT-07-071', flow='서류 만료 메일', kind='정상', title='매일 09:00 에 만료 30일 전과 7일 전 서류만 메일을 보낸다',
     pre='회원, DOC_EXPIRY_ALERT_DAYS "30,7", 만료일이 오늘부터 30일 뒤인 서류 A, 7일 뒤인 서류 B, 29일 뒤인 서류 C',
     steps='1. 한국 시각 09:00 이 지나기를 기다린다\n2. 알림 이력(notification_log)과 회원 메일함을 본다',
     expect='1. A 와 B 의 DOCUMENT_EXPIRY 알림 이력이 success 참으로 1행씩 있다\n'
            '2. A 이력의 idempotency_key 가 DOC_EXPIRY_{A id}_{A 만료일}_30 이다\n'
            '3. C 의 알림 이력이 없다\n'
            '4. 회원 이메일로 온 메일에 서류 이름, 만료일, 남은 날, 서류함 주소가 있다\n'
            '5. /documents/expiry 의 A 와 B 줄 마지막 알림 날이 오늘이다',
     judge=['서버', '서버', '서버', '합동', '가상'],
     req=['BID-MKT-014'], fc=['BID-MKT-014'], screen='', pri='Medium'),

dict(id='IT-07-072', flow='서류 만료 메일', kind='정상', title='만료일을 바꾸면 새 일정으로 다시 센다',
     pre='회원, 오늘 30일 전 메일을 받은 서류 A',
     steps='1. A 의 만료일을 내일부터 30일 뒤 날짜로 바꾼다\n2. 다음 날 한국 시각 09:00 이 지나기를 기다린다',
     expect='1. A 의 DOCUMENT_EXPIRY 이력이 새 만료일 키 DOC_EXPIRY_{A id}_{새 만료일}_30 으로 1행 더 있다\n'
            '2. 회원 메일함에 A 의 새 만료일이 적힌 메일이 한 통 더 와 있다',
     judge=['서버', '합동'],
     req=['BID-MKT-014'], fc=['BID-MKT-014'], screen='', pri='Medium'),

dict(id='IT-07-073', flow='서류 만료 메일', kind='예외', title='같은 서류, 같은 만료일, 같은 시점은 한 번만 보낸다',
     pre='회원, 오늘 09:00 에 30일 전 메일을 이미 보낸 서류 A',
     steps='1. 같은 날 만료 메일 배치를 한 번 더 실행한다\n2. 알림 이력과 회원 메일함을 본다',
     expect='1. A 의 DOC_EXPIRY_{A id}_{A 만료일}_30 이력이 1행 그대로다\n'
            '2. 회원 메일함에 A 메일이 한 통뿐이다',
     judge=['서버', '합동'],
     req=['BID-MKT-014'], fc=['BID-MKT-014'], screen='', pri='Medium'),

dict(id='IT-07-074', flow='서류 만료 메일', kind='예외', title='지운 서류에는 만료 메일을 보내지 않는다',
     pre='회원, 만료일이 30일 뒤인 서류 D',
     steps='1. 09:00 전에 서류함에서 D 를 지운다\n2. 한국 시각 09:00 이 지나기를 기다린다',
     expect='1. D 의 DOCUMENT_EXPIRY 이력이 없다',
     judge=['서버'],
     req=['BID-MKT-014'], fc=['BID-MKT-014'], screen='', pri='Medium'),

dict(id='IT-07-075', flow='서류 만료 메일', kind='예외', title='만료일이 빈 서류에는 만료 메일을 보내지 않는다',
     pre='회원, 만료일 없는 서류 E',
     steps='1. 한국 시각 09:00 이 지나기를 기다린다',
     expect='1. E 의 DOCUMENT_EXPIRY 이력이 없다',
     judge=['서버'],
     req=['BID-MKT-014'], fc=['BID-MKT-014'], screen='', pri='Medium'),

dict(id='IT-07-076', flow='서류 만료 메일', kind='예외', title='보내기에 실패한 시점은 실패로 남기고 다시 보내지 않는다',
     pre='회원, 만료일이 30일 뒤인 서류 F, 알림 서버 보내기가 실패하게 장애를 넣은 상태',
     steps='1. 한국 시각 09:00 이 지나기를 기다린다\n2. 장애를 풀고 같은 날 배치를 한 번 더 실행한다',
     expect='1. F 의 DOCUMENT_EXPIRY 이력이 success 거짓과 error_message 로 1행 있다\n'
            '2. 장애를 푼 뒤에도 F 의 같은 시점 메일을 다시 보내지 않는다',
     judge=['단위', '단위'],
     req=['BID-MKT-014'], fc=['BID-MKT-014'], screen='', pri='Medium'),

dict(id='IT-07-077', flow='서류 만료 메일', kind='예외', title='알림 시점 설정이 비면 만료 메일을 보내지 않는다',
     pre='회원, DOC_EXPIRY_ALERT_DAYS 를 빈 값으로 둔 상태, 만료일이 30일 뒤인 서류 G',
     steps='1. 한국 시각 09:00 이 지나기를 기다린다',
     expect='1. G 의 DOCUMENT_EXPIRY 이력이 없다',
     judge=['서버'],
     req=['BID-MKT-014'], fc=['BID-MKT-014'], screen='', pri='Medium'),

dict(id='IT-07-078', flow='미 연방 계약 받기', kind='정상', title='성공한 받기가 없으면 거래일 시작일부터 받는 범위 전체를 받는다',
     pre='관리자 토큰, federal_contract_sync_runs 에 SUCCEEDED 회차 없음, USAspending Mock 에 계약 150건',
     steps='1. POST /admin/federal-contracts/sync 에 mode DAILY 를 보낸다\n2. 회차가 끝난 뒤 Mock 요청 기록과 federal_contracts 를 본다',
     expect='1. Mock 이 받은 검색 조건이 계약 종류 A,B,C,D, 발주 기관 Department of Defense, 수행 장소 KOR, 업체 주소 KOR, 거래일 시작 2023-10-01 이다\n'
            '2. Mock 이 받은 검색에 바뀐 날 조건이 없다\n'
            '3. federal_contracts 에 150행이 있다\n'
            '4. 행마다 piid, parent_piid, award_type, recipient_name, recipient_uei, start_date, end_date, amount_usd, naics_code, naics_name, description, awarding_sub_agency, source_modified_date 가 Mock 값과 같다\n'
            '5. 행마다 raw_json 에 검색 응답 한 줄 원본이 있다',
     judge=['서버', '서버', '서버', '서버', '서버'],
     req=['BID-MKT-025'], fc=['BID-MKT-025'], screen='', pri='Medium'),

dict(id='IT-07-079', flow='미 연방 계약 받기', kind='정상', title='매일 받기는 마지막 성공 받기의 하루 전부터 바뀐 계약만 받는다',
     pre='관리자 토큰, 2026-10-10 에 끝난 SUCCEEDED 회차 1건',
     steps='1. POST /admin/federal-contracts/sync 에 mode DAILY 를 보낸다\n2. 회차가 끝난 뒤 Mock 요청 기록과 그 회차 행을 본다',
     expect='1. Mock 이 받은 검색의 바뀐 날 조건이 2026-10-09 부터다\n'
            '2. 그 회차 modified_since 가 2026-10-09 다\n'
            '3. 그 회차 triggered_by 가 ADMIN 이다',
     judge=['서버', '서버', '서버'],
     req=['BID-MKT-025', 'BID-MKT-027'], fc=['BID-MKT-025', 'BID-MKT-027'], screen='', pri='Medium'),

dict(id='IT-07-080', flow='미 연방 계약 받기', kind='정상', title='계약 고유 키가 같으면 덮어쓰고 받지 않은 계약은 지우지 않는다',
     pre='관리자 토큰, federal_contracts 에 계약 X(금액 100)와 Y, Mock 매일 받기 결과에 X 만 금액 200 으로 들어 있음',
     steps='1. POST /admin/federal-contracts/sync 에 mode DAILY 를 보낸다\n2. federal_contracts 와 회차 행을 본다',
     expect='1. X 행이 award_key 그대로 1행이고 amount_usd 가 200 이다\n'
            '2. X 행 raw_json 이 새 응답 한 줄이다\n'
            '3. Y 행이 그대로 남아 있다\n'
            '4. 회차 updated_count 가 1 이고 inserted_count 가 0 이다',
     judge=['서버', '서버', '서버', '서버'],
     req=['BID-MKT-025', 'BID-MKT-027'], fc=['BID-MKT-025', 'BID-MKT-027'], screen='', pri='Medium'),

dict(id='IT-07-081', flow='미 연방 계약 받기', kind='정상', title='받은 쪽이 100건보다 적으면 끝으로 보고 다음 쪽 표시는 믿지 않는다',
     pre='관리자 토큰, Mock 첫 쪽 100건, 둘째 쪽 37건, 둘째 쪽 응답에도 다음 쪽 있음 표시',
     steps='1. POST /admin/federal-contracts/sync 에 mode FULL 을 보낸다\n2. Mock 요청 기록과 회차 행을 본다',
     expect='1. Mock 검색 요청이 2번이다\n'
            '2. 회차 search_calls 가 2 다',
     judge=['서버', '서버'],
     req=['BID-MKT-025'], fc=['BID-MKT-025'], screen='', pri='Medium'),

dict(id='IT-07-082', flow='미 연방 계약 받기', kind='정상', title='호출 사이에 1000ms 간격을 둔다',
     pre='관리자 토큰, Mock 에 검색 3쪽 분량 계약',
     steps='1. POST /admin/federal-contracts/sync 에 mode FULL 을 보낸다\n2. Mock 요청 시각을 본다',
     expect='1. 이어진 요청 사이 간격이 모두 1000ms 이상이다',
     judge=['서버'],
     req=['BID-MKT-025'], fc=['BID-MKT-025'], screen='', pri='Medium'),

dict(id='IT-07-083', flow='미 연방 계약 받기', kind='정상', title='매일 05:00 한국 시각에 스케줄러가 받기를 돈다',
     pre='시험 서버, Mock 에 바뀐 계약 1건',
     steps='1. 한국 시각 05:00 이 지나기를 기다린다\n2. federal_contract_sync_runs 를 본다',
     expect='1. 05:00 에 시작한 회차가 mode DAILY, triggered_by SCHEDULER 로 1행 있다\n'
            '2. 그 회차 status 가 SUCCEEDED 이고 finished_at 이 있다',
     judge=['서버', '서버'],
     req=['BID-MKT-025', 'BID-MKT-027'], fc=['BID-MKT-025', 'BID-MKT-027'], screen='', pri='Medium'),

dict(id='IT-07-084', flow='미 연방 계약 받기', kind='정상', title='한 번 실패한 호출은 5000ms 기다려 다시 불러 이어간다',
     pre='관리자 토큰, Mock 첫 검색 요청만 500 을 주게 둔 상태',
     steps='1. POST /admin/federal-contracts/sync 에 mode FULL 을 보낸다\n2. Mock 요청 기록과 회차 행을 본다',
     expect='1. 실패한 요청과 다시 부른 요청 사이가 5000ms 이상이다\n'
            '2. 회차 status 가 SUCCEEDED 다',
     judge=['단위', '단위'],
     req=['BID-MKT-025'], fc=['BID-MKT-025'], screen='', pri='Medium'),

dict(id='IT-07-085', flow='미 연방 계약 받기', kind='예외', title='같은 호출이 3번 실패하면 회차를 실패로 멈추고 받은 행은 남긴다',
     pre='관리자 토큰, Mock 첫 쪽은 성공하고 둘째 쪽 요청이 계속 500 을 주게 둔 상태',
     steps='1. POST /admin/federal-contracts/sync 에 mode FULL 을 보낸다\n2. 회차 행과 federal_contracts 를 본다',
     expect='1. 둘째 쪽 요청이 3번 간다\n'
            '2. 회차 status 가 FAILED 이고 error_message 가 있다\n'
            '3. 첫 쪽에서 받은 100행이 federal_contracts 에 남아 있다',
     judge=['단위', '단위', '단위'],
     req=['BID-MKT-025', 'BID-MKT-027'], fc=['BID-MKT-025', 'BID-MKT-027'], screen='', pri='Medium'),

dict(id='IT-07-086', flow='미 연방 계약 받기', kind='예외', title='429 가 아닌 4xx 는 다시 부르지 않는다',
     pre='관리자 토큰, Mock 검색 요청이 400 을 주게 둔 상태',
     steps='1. POST /admin/federal-contracts/sync 에 mode FULL 을 보낸다\n2. Mock 요청 기록을 본다',
     expect='1. 400 을 받은 검색 요청이 1번만 간다',
     judge=['단위'],
     req=['BID-MKT-025'], fc=['BID-MKT-025'], screen='', pri='Medium'),

dict(id='IT-07-087', flow='미 연방 계약 받기', kind='정상', title='실패 회차 뒤 매일 받기는 마지막 성공 회차를 기준으로 다시 받는다',
     pre='관리자 토큰, 2026-10-10 에 끝난 SUCCEEDED 회차와 그 뒤 FAILED 회차',
     steps='1. POST /admin/federal-contracts/sync 에 mode DAILY 를 보낸다\n2. 새 회차 행을 본다',
     expect='1. 새 회차 modified_since 가 2026-10-09 다',
     judge=['서버'],
     req=['BID-MKT-025'], fc=['BID-MKT-025'], screen='', pri='Medium'),

dict(id='IT-07-088', flow='미 연방 계약 받기', kind='예외', title='국방부가 아니거나 한국 밖인 계약은 받지 않는다',
     pre='관리자 토큰, Mock 에 국방부 한국 계약과 국무부 대사관 계약이 있음',
     steps='1. POST /admin/federal-contracts/sync 에 mode FULL 을 보낸다\n2. Mock 요청 기록과 federal_contracts 를 본다',
     expect='1. Mock 이 받은 검색 조건에 발주 기관 Department of Defense, 수행 장소 KOR, 업체 주소 KOR 가 있다\n'
            '2. federal_contracts 에 국무부 대사관 계약이 없다',
     judge=['서버', '서버'],
     req=['BID-MKT-025'], fc=['BID-MKT-025'], screen='', pri='Medium'),

dict(id='IT-07-089', flow='미 연방 계약 받기', kind='정상', title='상위 계약 없는 계약 중 끝날짜가 상세 받기 범위 안인 것만 상세를 받아 옵션 끝날짜와 원래 공고번호를 저장한다',
     pre='관리자 토큰, detail_fetched_at 이 빈 단독 계약 S1(지금 끝날짜 30일 뒤), Mock 상세에 potential end date 와 solicitation identifier 가 있음',
     steps='1. POST /admin/federal-contracts/sync 에 mode DAILY 를 보낸다\n2. S1 행과 회차 행을 본다',
     expect='1. S1 potential_end_date 가 Mock 상세 값이다\n'
            '2. S1 solicitation_number 가 Mock 상세 값이다\n'
            '3. S1 detail_fetched_at 이 채워져 있다\n'
            '4. S1 raw_json 에 상세 원본이 함께 들어 있다\n'
            '5. 회차 detail_calls 가 1 이다',
     judge=['서버', '서버', '서버', '서버', '서버'],
     req=['BID-MKT-026'], fc=['BID-MKT-026'], screen='', pri='Medium'),

dict(id='IT-07-090', flow='미 연방 계약 받기', kind='정상', title='상위 계약 안 주문과 끝날짜가 범위 밖인 계약은 상세를 부르지 않는다',
     pre='관리자 토큰, detail_fetched_at 이 빈 주문 O1(상위 계약 있음, 끝날짜 30일 뒤), 단독 계약 S2(지금 끝날짜 200일 전)',
     steps='1. POST /admin/federal-contracts/sync 에 mode DAILY 를 보낸다\n2. Mock 상세 요청 기록을 본다',
     expect='1. O1 상세 요청이 없다\n'
            '2. S2 상세 요청이 없다',
     judge=['서버', '서버'],
     req=['BID-MKT-026'], fc=['BID-MKT-026'], screen='', pri='Medium'),

dict(id='IT-07-091', flow='미 연방 계약 받기', kind='정상', title='이미 상세를 받은 계약은 다시 부르지 않는다',
     pre='관리자 토큰, detail_fetched_at 이 있고 그 뒤 바뀌지 않은 대상 단독 계약 S3',
     steps='1. POST /admin/federal-contracts/sync 에 mode DAILY 를 보낸다\n2. Mock 상세 요청 기록을 본다',
     expect='1. S3 상세 요청이 없다',
     judge=['서버'],
     req=['BID-MKT-026'], fc=['BID-MKT-026'], screen='', pri='Medium'),

dict(id='IT-07-092', flow='미 연방 계약 받기', kind='정상', title='상세를 받은 뒤 마지막 수정일이 뒤로 바뀐 계약은 상세를 다시 부른다',
     pre='관리자 토큰, 상세를 받은 대상 단독 계약 S4, Mock 매일 받기 결과에 S4 가 저장된 것보다 뒤 마지막 수정일로 들어 있음',
     steps='1. POST /admin/federal-contracts/sync 에 mode DAILY 를 보낸다\n2. Mock 상세 요청 기록과 S4 행을 본다',
     expect='1. S4 상세 요청이 1번 간다\n'
            '2. S4 detail_fetched_at 이 이번 회차 시각으로 바뀐다',
     judge=['서버', '서버'],
     req=['BID-MKT-026', 'BID-MKT-025'], fc=['BID-MKT-026', 'BID-MKT-025'], screen='', pri='Medium'),

dict(id='IT-07-093', flow='미 연방 계약 받기', kind='예외', title='상세 받기가 실패한 계약은 확인 전으로 남고 다음 회차에 다시 부른다',
     pre='관리자 토큰, 대상 단독 계약 S5, Mock S5 상세 요청이 계속 500 을 주게 둔 상태',
     steps='1. POST /admin/federal-contracts/sync 에 mode DAILY 를 보낸다\n2. Mock 장애를 풀고 다음 회차를 실행한다',
     expect='1. 첫 회차 뒤 S5 potential_end_date 와 detail_fetched_at 이 비어 있다\n'
            '2. 다음 회차에서 S5 상세 요청이 가고 potential_end_date 가 채워진다',
     judge=['단위', '단위'],
     req=['BID-MKT-026'], fc=['BID-MKT-026'], screen='', pri='Medium'),

dict(id='IT-07-094', flow='미 연방 계약 받기', kind='예외', title='상세 호출이 429 가 아닌 4xx 면 그 계약만 건너뛰고 회차를 이어간다',
     pre='관리자 토큰, 대상 단독 계약 S6 와 S7, Mock S6 상세 요청은 404, S7 은 성공',
     steps='1. POST /admin/federal-contracts/sync 에 mode DAILY 를 보낸다\n2. S6, S7 행과 회차 행을 본다',
     expect='1. S6 상세 요청이 1번만 간다\n'
            '2. S7 potential_end_date 가 채워져 있다\n'
            '3. 회차 status 가 SUCCEEDED 다',
     judge=['단위', '단위', '단위'],
     req=['BID-MKT-026'], fc=['BID-MKT-026'], screen='', pri='Medium'),

dict(id='IT-07-095', flow='미 연방 계약 받기', kind='정상', title='관리자가 전체 받기를 바로 실행하면 202 와 회차 id 를 받고 회차 기록이 남는다',
     pre='관리자 토큰, 돌고 있는 회차 없음',
     steps='1. POST /admin/federal-contracts/sync 에 mode FULL 을 보낸다\n2. 곧바로 그 회차 행을 본다\n3. 회차가 끝난 뒤 그 회차 행을 다시 본다',
     expect='1. 응답이 202 이고 회차 id 가 있다\n'
            '2. 곧바로 본 회차 status 가 RUNNING 이다\n'
            '3. 끝난 회차 status 가 SUCCEEDED 다\n'
            '4. 끝난 회차에 mode FULL, triggered_by ADMIN, started_at, finished_at, search_calls, detail_calls, inserted_count, updated_count 가 채워져 있다\n'
            '5. 끝난 회차 modified_since 가 비어 있다',
     judge=['서버', '서버', '서버', '서버', '서버'],
     req=['BID-MKT-027'], fc=['BID-MKT-027'], screen='', pri='Medium'),

dict(id='IT-07-096', flow='미 연방 계약 받기', kind='정상', title='관리자는 최근 받기 회차를 최근 시작순으로 본다',
     pre='관리자 토큰, 회차 25건',
     steps='1. GET /admin/federal-contracts/sync-runs 를 부른다\n2. size=100 으로 부른다',
     expect='1. size 없이 부르면 20건이 최근 시작순으로 온다\n'
            '2. size=100 이면 25건이 모두 온다\n'
            '3. 줄마다 federal_contract_sync_runs 칸이 그대로 있다',
     judge=['서버', '서버', '서버'],
     req=['BID-MKT-027'], fc=['BID-MKT-027'], screen='', pri='Medium'),

dict(id='IT-07-097', flow='미 연방 계약 받기', kind='정상', title='앱이 받는 중에 내려가 RUNNING 으로 남은 회차는 다음 실행 때 실패로 닫힌다',
     pre='관리자 토큰, 받는 중에 앱을 다시 띄워 RUNNING 으로 남은 회차 1건',
     steps='1. POST /admin/federal-contracts/sync 에 mode DAILY 를 보낸다\n2. 남아 있던 회차와 새 회차 행을 본다',
     expect='1. 남아 있던 회차 status 가 FAILED 이고 error_message 가 "앱 재기동으로 멈춤" 이다\n'
            '2. 새 회차가 시작되어 있다',
     judge=['서버', '서버'],
     req=['BID-MKT-027'], fc=['BID-MKT-027'], screen='', pri='Medium'),

dict(id='IT-07-098', flow='미 연방 계약 받기', kind='예외', title='돌고 있는 회차가 있으면 새 실행은 409 다',
     pre='관리자 토큰, status RUNNING 인 회차 1건',
     steps='1. POST /admin/federal-contracts/sync 에 mode FULL 을 보낸다',
     expect='1. 응답이 409 다\n'
            '2. 새 회차 행이 생기지 않는다',
     judge=['서버', '서버'],
     req=['BID-MKT-027'], fc=['BID-MKT-027'], screen='', pri='Medium'),

dict(id='IT-07-099', flow='미 연방 계약 받기', kind='예외', title='mode 가 없거나 다른 값이면 400 이다',
     pre='관리자 토큰',
     steps='1. POST /admin/federal-contracts/sync 에 본문 없이 보낸다\n2. mode WEEKLY 로 보낸다',
     expect='1. mode 가 없으면 응답이 400 이다\n'
            '2. mode WEEKLY 면 응답이 400 이다\n'
            '3. 새 회차 행이 생기지 않는다',
     judge=['서버', '서버', '서버'],
     req=['BID-MKT-027'], fc=['BID-MKT-027'], screen='', pri='Medium'),

dict(id='IT-07-100', flow='미 연방 계약 받기', kind='예외', title='관리자가 아닌 계정은 받기를 실행하거나 기록을 보지 못한다',
     pre='회원 토큰',
     steps='1. 회원 토큰으로 POST /admin/federal-contracts/sync 에 mode DAILY 를 보낸다\n2. 회원 토큰으로 GET /admin/federal-contracts/sync-runs 를 부른다',
     expect='1. 두 요청 모두 거부된다\n'
            '2. 새 회차 행이 생기지 않는다',
     judge=['서버', '서버'],
     req=['BID-MKT-027'], fc=['BID-MKT-027'], screen='', pri='Medium'),

dict(id='IT-07-101', flow='낙찰가 범위', kind='정상', title='회원이 낙찰가 범위를 열면 자료 기준과 넣으라는 안내가 있다',
     pre='회원, SUCCEEDED 회차가 있고 federal_contracts 에 계약이 있음, FEDERAL_DATA_LAG_DAYS 90',
     steps='1. 낙찰정보 묶음 메뉴에서 [낙찰가 범위] 를 누른다\n2. GET /federal-contracts/naics 응답의 source 를 본다',
     expect='1. 주소가 /awards/range 다\n'
            '2. 제목 줄에 "낙찰가 범위" 와 "같은 업종의 지난 한국 내 미군 계약 금액을 범위로 보여 드립니다. 가격을 정할 때 참고하는 자료입니다" 가 있다\n'
            '3. 자료 기준에 "미 연방 계약 공개 자료(USAspending.gov). 수행 장소와 업체가 모두 한국인 국방부 계약 n건(2023-10 부터)" 이 있다\n'
            '4. 자료 기준의 계약 수 n 이 source.totalCount 와 같다\n'
            '5. 자료 기준의 마지막으로 받은 때가 source.lastSyncedAt 의 한국 시각이다\n'
            '6. 자료 기준에 "국방부 계약 자료는 약 90일 늦게 공개되어 최근 3개월 계약과 옵션 연장은 아직 보이지 않을 수 있습니다" 가 있다\n'
            '7. 찾기 칸과 "공고번호나 업종을 넣어 주세요" 가 있다\n'
            '8. 아래 띠에 [마감 전 공고] 만 있다\n'
            '9. 제목 글자가 32px 이다\n'
            '10. 본문 글자가 Pretendard 17px 이상이다',
     judge=['가상', '가상', '가상', '서버', '서버', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-102', flow='낙찰가 범위', kind='정상', title='비로그인은 로그인 화면을 거쳐 낙찰가 범위로 돌아온다',
     pre='비로그인, 회원 계정 1개',
     steps='1. 주소창에 /awards/range 를 넣어 연다\n2. 회원 계정으로 로그인한다',
     expect='1. 로그인 화면으로 간다\n'
            '2. 로그인 뒤 주소가 /awards/range 로 돌아온다',
     judge=['가상', '가상'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-103', flow='낙찰가 범위', kind='정상', title='업종 칸에 글자를 넣고 멈추면 0.3초 뒤 업종 목록을 받는다',
     pre='회원, 브라우저 개발자 도구 네트워크 탭을 연 상태',
     steps='1. /awards/range 찾기 칸에 roof 를 빠르게 넣고 멈춘다\n2. 네트워크 탭과 고르기 목록을 본다',
     expect='1. 글자를 넣는 동안이 아니라 멈춘 뒤 0.3초에 GET /federal-contracts/naics 요청이 1번 간다\n'
            '2. 고르기 목록이 코드, 이름, 계약 수와 함께 계약 수 많은 순이다\n'
            '3. 응답 업종이 50건 이하다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-104', flow='낙찰가 범위', kind='정상', title='업종으로 찾으면 범위 두 줄과 근거 계약 20줄이 나온다',
     pre='회원, 업종 236220 계약이 상위 계약 없는 계약 10건, 상위 계약 안 주문 25건, 금액 0 이하 2건',
     steps='1. /awards/range 에서 업종 236220 을 고르고 [찾기] 를 누른다\n2. 같은 때 GET /federal-contracts/award-range?naics=236220 응답을 받아 둔다\n3. 새로고침한다',
     expect='1. 주소에 naics=236220 이 남는다\n'
            '2. 업종 머리에 업종 코드 236220 과 업종 이름이 있다\n'
            '3. 범위 줄이 상위 계약 없는 계약(구매 주문, 단독 계약)과 상위 계약 안 주문(주문, BPA 주문) 두 줄이다\n'
            '4. 줄마다 건수, 아래 4분의 1, 가운데, 위 4분의 1, 가장 작은 금액, 가장 큰 금액과 가로 범위 막대가 있다\n'
            '5. 두 줄 값이 응답 rows 의 count, p25, p50, p75, min, max 와 같다\n'
            '6. 자료 기준에 "금액은 계약에 지금까지 확정된 금액(달러)입니다. 0 이하인 계약 2건은 뺐습니다" 가 있다\n'
            '7. 근거 계약이 최근 시작순 20줄이고 줄마다 시작일, 작업 설명, 계약번호, 종류, 업체, 금액이 있다\n'
            '8. 금액과 계약번호가 IBM Plex Mono 글꼴이다\n'
            '9. 새로고침 뒤에도 같은 업종 결과가 있다',
     judge=['가상', '가상', '가상', '가상', '서버', '가상', '가상', '가상', '가상'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-105', flow='낙찰가 범위', kind='정상', title='범위는 6자리 업종의 0 보다 큰 금액만으로 선형 보간 백분위수를 낸다',
     pre='회원 토큰, 업종 236220 상위 계약 없는 계약 금액 100, 200, 300, 400, 500, 0, -50 달러, 업종 236210 계약 금액 9999 달러 1건',
     steps='1. GET /federal-contracts/award-range?naics=236220 을 부른다',
     expect='1. STANDALONE 줄 count 가 5 이고 excludedCount 가 2 다\n'
            '2. STANDALONE 줄 min 100, p25 200, p50 300, p75 400, max 500 이다\n'
            '3. 236210 계약 금액 9999 가 어느 값에도 들지 않는다\n'
            '4. 값이 원화로 바뀌지 않고 달러 그대로다',
     judge=['서버', '서버', '서버', '서버'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-106', flow='낙찰가 범위', kind='정상', title='한 줄 건수가 최소 건수보다 적으면 범위 대신 건수만 보인다',
     pre='회원, AWARD_RANGE_MIN_COUNT 5, 업종 236220 상위 계약 안 주문 3건',
     steps='1. /awards/range?naics=236220 을 연다\n2. 같은 때 award-range 응답을 본다',
     expect='1. 상위 계약 안 주문 줄에 "3건이라 범위를 내지 않습니다" 가 있다\n'
            '2. 응답 ORDER 줄 minCount 가 5 이고 min, p25, p50, p75, max 가 null 이다',
     judge=['가상', '서버'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-107', flow='낙찰가 범위', kind='정상', title='최소 건수 설정을 바꾸면 그 값으로 범위를 낸다',
     pre='회원, AWARD_RANGE_MIN_COUNT 를 3 으로 바꾼 상태, 업종 236220 상위 계약 안 주문 3건',
     steps='1. /awards/range?naics=236220 을 연다',
     expect='1. 상위 계약 안 주문 줄에 건수 3 과 다섯 값, 범위 막대가 있다\n'
            '2. 응답 ORDER 줄 minCount 가 3 이다',
     judge=['가상', '서버'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-108', flow='낙찰가 범위', kind='정상', title='최소 건수 설정이 비었거나 1 보다 작으면 처음 값 5 를 쓴다',
     pre='회원 토큰, 업종 236220 상위 계약 안 주문 3건',
     steps='1. AWARD_RANGE_MIN_COUNT 를 빈 값으로 두고 GET /federal-contracts/award-range?naics=236220 을 부른다\n2. AWARD_RANGE_MIN_COUNT 를 0 으로 두고 다시 부른다',
     expect='1. 빈 값일 때 ORDER 줄 minCount 가 5 다\n'
            '2. 0 일 때 ORDER 줄 minCount 가 5 다',
     judge=['서버', '서버'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-109', flow='낙찰가 범위', kind='정상', title='공고번호로 찾으면 우리 공고의 업종 코드로 범위를 낸다',
     pre='회원, 업종 코드 236220 인 우리 공고 1건',
     steps='1. /awards/range 찾기 칸에 그 공고번호를 넣고 [찾기] 를 누른다',
     expect='1. 주소에 notice 값으로 그 공고번호가 남는다\n'
            '2. 업종 머리에 업종 코드 236220 과 그 공고의 원문 제목이 있다\n'
            '3. 응답 naicsCode 가 236220 이고 noticeNumber 가 그 공고번호다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-110', flow='낙찰가 범위', kind='정상', title='주소에 공고번호를 담아 들어오면 바로 찾기 결과가 나온다',
     pre='회원, 업종 코드 있는 우리 공고 1건',
     steps='1. 주소창에 /awards/range?notice={그 공고번호} 를 넣어 연다',
     expect='1. 찾기를 누르지 않아도 업종 머리와 범위 두 줄, 근거 계약이 있다\n'
            '2. 진입하자마자 GET /federal-contracts/award-range 요청이 1번 간다',
     judge=['가상', '가상'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-111', flow='낙찰가 범위', kind='정상', title='작업 설명 낱말을 넣으면 그 낱말이 든 계약만으로 다시 낸다',
     pre='회원, 업종 236220 계약 중 작업 설명에 ROOF 가 든 것과 안 든 것이 섞여 있음',
     steps='1. /awards/range?naics=236220 에서 작업 설명 낱말 칸에 roof 를 넣고 [찾기] 를 누른다',
     expect='1. 주소에 q=roof 가 남는다\n'
            '2. 근거 계약 줄의 작업 설명에 모두 roof 가 대소문자 없이 들어 있다\n'
            '3. 범위 두 줄 건수가 응답 rows count 와 같고 낱말 없이 찾았을 때보다 적거나 같다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-112', flow='낙찰가 범위', kind='정상', title='근거 계약 줄을 누르면 USAspending 계약 쪽을 새 창으로 연다',
     pre='회원, 업종 236220 근거 계약 1건 이상',
     steps='1. /awards/range?naics=236220 에서 근거 계약 첫 줄을 누른다',
     expect='1. 새 창 주소가 응답 그 줄 usaspendingUrl 과 같다\n'
            '2. /awards/range 창은 그대로 남아 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-113', flow='낙찰가 범위', kind='정상', title='그 업종 계약이 0건이면 계약이 없다는 빈 상태다',
     pre='회원, 한국 내 계약이 0건인 업종 코드',
     steps='1. /awards/range 에서 그 업종으로 찾는다',
     expect='1. "이 업종의 한국 내 계약이 없습니다" 가 있다',
     judge=['가상'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-114', flow='낙찰가 범위', kind='정상', title='낙찰가 범위에 우리 낙찰 공지 금액이 섞이지 않는다',
     pre='회원 토큰, 업종 236220 미 연방 계약 금액 100~500 달러 5건, 같은 업종 우리 낙찰 공지 금액 777777 달러 1건',
     steps='1. GET /federal-contracts/award-range?naics=236220 을 부른다',
     expect='1. rows 와 evidence 어디에도 777777 금액이 없다\n'
            '2. evidence 줄이 모두 federal_contracts 행이다',
     judge=['서버', '서버'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-115', flow='낙찰가 범위', kind='정상', title='늦음 일수 설정에 따라 자료 기준 늦음 문장이 바뀐다',
     pre='회원',
     steps='1. FEDERAL_DATA_LAG_DAYS 를 60 으로 두고 /awards/range 를 연다\n2. FEDERAL_DATA_LAG_DAYS 를 빈 값으로 두고 다시 연다',
     expect='1. 60 일 때 자료 기준에 "약 60일 늦게 공개되어" 가 있다\n'
            '2. 빈 값일 때 자료 기준 늦음 문장에 일수 숫자가 없다',
     judge=['가상', '가상'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-116', flow='낙찰가 범위', kind='정상', title='미 연방 계약을 한 번도 받지 않았으면 본문 대신 받고 있다는 안내다',
     pre='회원, SUCCEEDED 회차 없음(source.lastSyncedAt null)',
     steps='1. /awards/range 를 연다',
     expect='1. 본문 대신 "미 연방 계약 자료를 받고 있습니다" 가 있다\n'
            '2. 찾기 칸과 범위 줄이 없다',
     judge=['가상', '가상'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-117', flow='낙찰가 범위', kind='예외', title='업종 코드가 없는 공고번호로 찾으면 업종을 고르라는 안내다',
     pre='회원, 업종 코드가 없는 우리 공고 1건',
     steps='1. /awards/range 에서 그 공고번호로 찾는다',
     expect='1. award-range 응답이 422 다\n'
            '2. "이 공고에는 업종 코드가 없습니다. 업종을 골라 주세요" 와 업종 고르기 칸이 있다',
     judge=['서버', '가상'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-118', flow='낙찰가 범위', kind='예외', title='없는 공고번호로 찾으면 찾지 못했다는 안내다',
     pre='회원',
     steps='1. /awards/range 에서 우리 공고에 없는 공고번호 NOSUCH-0000 으로 찾는다',
     expect='1. award-range 응답이 404 다\n'
            '2. "그 공고번호를 찾지 못했습니다" 가 있다',
     judge=['서버', '가상'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-119', flow='낙찰가 범위', kind='예외', title='공고번호와 업종이 모두 없으면 400 이다',
     pre='회원 토큰',
     steps='1. GET /federal-contracts/award-range 를 조건 없이 부른다',
     expect='1. 응답이 400 이다',
     judge=['서버'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='', pri='Medium'),

dict(id='IT-07-120', flow='낙찰가 범위', kind='예외', title='범위를 그 밖의 오류로 못 받으면 다시 시도가 있다',
     pre='회원, GET /federal-contracts/award-range 가 500 을 주게 막은 상태',
     steps='1. /awards/range 에서 업종으로 찾는다',
     expect='1. "불러오지 못했습니다" 와 [다시 시도] 가 있다',
     judge=['가상'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-121', flow='낙찰가 범위', kind='예외', title='업종 목록을 못 받으면 고르기 목록에 안내가 있다',
     pre='회원, GET /federal-contracts/naics 가 실패하게 막은 상태',
     steps='1. /awards/range 찾기 칸에 roof 를 넣고 멈춘다',
     expect='1. 고르기 목록에 "업종 목록을 불러오지 못했습니다" 가 있다',
     judge=['가상'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='/awards/range', pri='Medium'),

dict(id='IT-07-122', flow='낙찰가 범위', kind='예외', title='로그인하지 않은 요청은 낙찰가 범위와 업종 목록을 받지 못한다',
     pre='비로그인',
     steps='1. 토큰 없이 GET /federal-contracts/award-range?naics=236220 을 부른다\n2. 토큰 없이 GET /federal-contracts/naics 를 부른다',
     expect='1. 두 요청 모두 거부된다\n'
            '2. 계약 금액이 응답에 없다',
     judge=['서버', '서버'],
     req=['BID-MKT-034'], fc=['BID-MKT-034'], screen='', pri='Medium'),

]
