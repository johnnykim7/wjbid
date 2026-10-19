# 영역 04 요금/자격 진단 시험항목. 근거는 T1-1 기능요구사항 명세서(BID-PRICE, BID-QUAL, BID-MKT-015~019), T1-3 BIZ-032~044·071, T1-4 POL-010, T3-2, T3-3, T3-4 화면 설계다.

ITEMS = [

dict(id='IT-04-001', flow='요금 안내', kind='정상', title='비로그인 방문자가 메뉴로 요금 안내를 열어 두 단계 견줌 표를 본다',
     pre='비로그인, 설정 PRICING_MEMBER_MONTHLY_KRW 값 50000',
     steps='1. 비로그인으로 고객 화면 메뉴 고객센터 > 요금 안내 를 누른다\n2. 화면을 끝까지 내린다',
     expect='1. 주소가 /pricing 이고 제목 "요금 안내" 와 설명 "무료로 시작하고, 필요할 때 회원으로 넓히세요. 회원은 월 단위, 대행과 컨설팅은 건마다 상담합니다." 가 보인다\n'
            '2. 왼쪽 묶음 메뉴가 고객센터 묶음이고 요금 안내 항목이 옅은 파랑 바탕과 왼쪽 파란 줄로 켜져 있다\n'
            '3. 위치 줄이 홈, 고객센터, 요금 안내 순서로 보인다\n'
            '4. 무료 칸에 "0원" 과 대상 "처음 둘러보는 회사" 가 있다\n'
            '5. 회원 칸에 "추천" 꼬리표, "50,000원 / 월", 대상 "입찰을 꾸준히 하는 회사" 가 있다\n'
            '6. 무료 칸과 회원 칸 어디에도 "지금 단계" 꼬리표가 없다\n'
            '7. 무료 칸에 [무료로 시작], 회원 칸에 [회원으로 시작] 단추가 있다\n'
            '8. 끝에 "금액은 부가세 별도입니다. 회원은 월 단위로 결제하며 약정 없이 해지할 수 있습니다. 대행·컨설팅은 상담 후 견적으로 안내합니다." 가 보인다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-PRICE-001'], fc=['BID-PRICE-001'], screen='/pricing', pri='Medium'),

dict(id='IT-04-002', flow='요금 안내', kind='정상', title='견줌 표 기능 행이 열람 조건(CR-157, CR-147)대로 나뉜다',
     pre='비로그인',
     steps='1. /pricing 을 연다\n2. 월 구독 비교 표의 기능 행을 위에서부터 읽는다',
     expect='1. 무료 칸과 회원 칸이 모두 포함 표시인 행이 SAM.gov 공고 검색, 공고 한글로 보기, 관심 공고 저장, 입찰 교육 자료, 새 공고 알림, 서류함과 제출 전 검사 여섯이다\n'
            '2. 회원 칸에만 포함 표시인 행이 입찰 신청, 공고문 상세 번역, 적격성 진단 셋이다\n'
            '3. 표 안에 "재발주 예측 달력" 행이 없다\n'
            '4. 표 안에 "무료 입찰 교실" 이라는 글이 없다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-PRICE-001'], fc=['BID-PRICE-001'], screen='/pricing', pri='Medium'),

dict(id='IT-04-003', flow='요금 안내', kind='정상', title='무료회원이 로그인해 요금 안내를 열면 무료 단계에 "지금 단계" 가 달린다',
     pre='무료회원 계정(회원 기간 없음)',
     steps='1. 고객 화면에 무료회원 계정으로 로그인한다\n2. 메뉴 고객센터 > 요금 안내 를 누른다',
     expect='1. 무료 칸에 "지금 단계" 꼬리표가 있다\n'
            '2. 회원 칸에는 "지금 단계" 꼬리표가 없다\n'
            '3. 무료 칸에 [무료로 시작] 단추가 없다',
     judge=['가상', '가상', '가상'],
     req=['BID-PRICE-001'], fc=['BID-PRICE-001'], screen='/pricing', pri='Medium'),

dict(id='IT-04-004', flow='요금 안내', kind='정상', title='회원이 로그인해 요금 안내를 열면 회원 단계에 "지금 단계" 가 달린다',
     pre='회원 기간 안인 회원 계정',
     steps='1. 고객 화면에 회원 계정으로 로그인한다\n2. /pricing 을 연다',
     expect='1. 회원 칸에 "지금 단계" 꼬리표가 있다\n'
            '2. 무료 칸에는 "지금 단계" 꼬리표가 없다\n'
            '3. 무료 칸에 [무료로 시작] 단추가 없다',
     judge=['가상', '가상', '가상'],
     req=['BID-PRICE-001'], fc=['BID-PRICE-001'], screen='/pricing', pri='Medium',
     issue='화면 소스는 꼬리표 글을 "현재 플랜" 으로 쓴다. 설계(CR-157)는 "지금 단계" 다'),

dict(id='IT-04-005', flow='요금 안내', kind='정상', title='회원 월 금액 설정이 비어 있으면 금액 대신 "요금 안내 준비 중" 이 보인다',
     pre='비로그인, 설정 PRICING_MEMBER_MONTHLY_KRW 값 비움',
     steps='1. 설정값을 비운 상태로 /pricing 을 연다',
     expect='1. 회원 칸 금액 자리에 숫자 금액 없이 "요금 안내 준비 중" 이 보인다\n'
            '2. 요금 조회(GET /pricing) 응답의 memberMonthlyPrice 가 null 이다',
     judge=['가상', '서버'],
     req=['BID-PRICE-001'], fc=['BID-PRICE-001'], screen='/pricing', pri='Medium'),

dict(id='IT-04-006', flow='요금 안내', kind='정상', title='설정값을 바꾸면 화면 금액이 그 값으로 바뀐다',
     pre='비로그인, 설정 PRICING_MEMBER_MONTHLY_KRW 값 50000',
     steps='1. /pricing 에서 회원 칸 금액을 본다\n2. 설정 PRICING_MEMBER_MONTHLY_KRW 값을 70000 으로 바꾼다\n3. /pricing 을 새로고침한다',
     expect='1. 1번에서 회원 칸 금액이 "50,000원 / 월" 이다\n'
            '2. 3번에서 요금 조회 응답의 memberMonthlyPrice 가 70000 이다\n'
            '3. 3번에서 회원 칸 금액이 "70,000원 / 월" 로 바뀌어 있다',
     judge=['가상', '서버', '가상'],
     req=['BID-PRICE-001'], fc=['BID-PRICE-001'], screen='/pricing', pri='Medium'),

dict(id='IT-04-007', flow='요금 안내', kind='정상', title='요금 조회 응답 전에는 금액 자리만 옅은 자리표시다',
     pre='비로그인, 설정값 50000, 요금 조회 응답이 늦게 오는 상태',
     steps='1. /pricing 을 연다\n2. 요금 조회 응답이 오기 전 회원 칸을 본다\n3. 응답이 온 뒤 다시 본다',
     expect='1. 2번에서 회원 칸 금액 자리에 숫자 없이 옅은 회색 자리표시 칸이 있다\n'
            '2. 2번에서 회원 칸 대상 "입찰을 꾸준히 하는 회사" 는 이미 보인다\n'
            '3. 3번에서 자리표시가 "50,000원 / 월" 로 바뀐다',
     judge=['가상', '가상', '가상'],
     req=['BID-PRICE-001'], fc=['BID-PRICE-001'], screen='/pricing', pri='Medium'),

dict(id='IT-04-008', flow='요금 안내', kind='정상', title='결제 연동 없이 단계 단추가 회원가입과 1:1 문의로 간다',
     pre='비로그인',
     steps='1. /pricing 무료 칸의 [무료로 시작] 을 누른다\n2. 뒤로 가서 회원 칸의 [회원으로 시작] 을 누른다',
     expect='1. 1번에서 회원가입 화면(/register)이 열린다\n'
            '2. 2번에서 1:1 문의 화면(/support)이 열려 이름, 이메일, 문의 유형, 제목, 내용 칸이 있다\n'
            '3. 요금 안내 화면 어디에도 결제 단추나 카드 번호 입력 칸이 없다',
     judge=['가상', '가상', '가상'],
     req=['BID-PRICE-001'], fc=['BID-PRICE-001'], screen='/pricing', pri='Medium'),

dict(id='IT-04-009', flow='요금 안내', kind='정상', title='대행·컨설팅 구역의 항목 상자가 서비스 안내, 준비 안내, 1:1 문의로 이어진다',
     pre='비로그인',
     steps='1. /pricing 대행·컨설팅 구역을 본다\n2. "SAM.gov·PIEE 등록 대행" 상자를 누른다\n3. 뒤로 가서 "제안서 자문·검토" 상자를 누른다\n4. 뒤로 가서 상담 상자의 [상담 신청] 을 누른다',
     expect='1. 제목 "대행·컨설팅" 과 설명 "맡기고 싶은 일이 있는 회사. 일마다 범위와 비용이 달라 상담 후 견적으로 안내합니다." 가 보인다\n'
            '2. 항목 상자 5개가 SAM.gov·PIEE 등록 대행, 제안서 자문·검토, 현장 설명회 동행과 기지 출입 지원, 대금 청구 대행, 서류 영문 번역 이름으로 있다\n'
            '3. 다섯 상자 중 제안서 자문·검토 상자에만 "준비 중" 꼬리표가 있다\n'
            '4. 2번에서 /services/ 로 시작하는 서비스 안내 화면이 열려 머리에 [상담 신청] 단추가 있다\n'
            '5. 3번에서 /coming-soon/ 로 시작하는 항목별 준비 안내 화면이 열려 "준비하고 있습니다" 가 보인다\n'
            '6. 4번에서 1:1 문의 화면(/support)이 열린다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-PRICE-001'], fc=['BID-PRICE-001'], screen='/pricing', pri='Medium'),

dict(id='IT-04-010', flow='요금 안내', kind='정상', title='홈 요금 구역과 요금 안내의 단계 이름, 대상, 포함 항목이 같다',
     pre='비로그인',
     steps='1. 홈(/)의 요금 구역을 본다\n2. /pricing 을 연다',
     expect='1. 두 화면의 무료 칸 대상이 같은 글 "처음 둘러보는 회사" 다\n'
            '2. 두 화면의 회원 칸 대상이 같은 글 "입찰을 꾸준히 하는 회사" 다\n'
            '3. 홈 요금 구역 무료 칸 포함 항목에 "재발주 예측 달력" 과 "무료 입찰 교실" 이 없다',
     judge=['가상', '가상', '가상'],
     req=['BID-PRICE-001'], fc=['BID-PRICE-001'], screen='/', pri='Medium'),

dict(id='IT-04-011', flow='요금 안내', kind='예외', title='요금 조회가 실패하면 금액 자리가 "요금 안내 준비 중" 이다',
     pre='비로그인, 요금 조회가 오류로 답하는 상태',
     steps='1. /pricing 을 연다',
     expect='1. 회원 칸 금액 자리에 "요금 안내 준비 중" 이 보인다\n'
            '2. 무료, 회원 단계 이름과 기능 행은 조회 성공 때와 같은 글로 있다',
     judge=['가상', '가상'],
     req=['BID-PRICE-001'], fc=['BID-PRICE-001'], screen='/pricing', pri='Medium'),

dict(id='IT-04-012', flow='요금 안내', kind='예외', title='회원 월 금액 설정값이 정수가 아니면 금액을 내지 않는다',
     pre='비로그인, 설정 PRICING_MEMBER_MONTHLY_KRW 값 "오만원"',
     steps='1. 요금 조회(GET /pricing)를 부른다\n2. /pricing 을 연다',
     expect='1. 요금 조회 응답의 memberMonthlyPrice 가 null 이다\n'
            '2. 회원 칸 금액 자리에 "요금 안내 준비 중" 이 보인다',
     judge=['서버', '가상'],
     req=['BID-PRICE-001'], fc=['BID-PRICE-001'], screen='/pricing', pri='Medium'),

dict(id='IT-04-013', flow='서비스 레벨 선택', kind='정상', title='회원이 입찰 신청에서 서비스 레벨 하나를 골라 신청한다',
     pre='회원 계정, 아직 신청하지 않은 마감 전 공개 공고문 1건',
     steps='1. 회원으로 로그인해 그 공고 상세(/search/{공고문 id})를 연다\n2. [입찰 참여] 를 눌러 서비스 레벨 4개를 본다\n3. "분석+정리" 를 고른다\n4. [신청하기] 를 누르고 확인 창 "이 공고에 입찰하시겠습니까?" 에서 확인한다',
     expect='1. 서비스 레벨 4개가 모니터링만, 분석+정리, AI 초안 생성, 관리자 검토 포함 풀패키지 이름으로 있다\n'
            '2. 고른 "분석+정리" 가 강조되고 그 레벨의 포함 서비스가 보인다\n'
            '3. 고른 레벨의 예상 비용이 보인다\n'
            '4. 새 입찰 요청 기록의 서비스 레벨이 ANALYSIS 로 저장된다',
     judge=['가상', '가상', '가상', '서버'],
     req=['BID-PRICE-002'], fc=['BID-PRICE-002'], screen='/search/{id}', pri='Medium',
     issue='예상 비용을 낸다고 하나 POL-010(CR-129)은 레벨별 금액을 두지 않아 값의 근거가 없다. 단추 이름이 T3-4 는 "입찰 참여", T3-3(CR-157)은 "제안서 작성" 이다. 화면 소스에 서비스 레벨 고르기가 보이지 않는다'),

dict(id='IT-04-014', flow='서비스 레벨 선택', kind='예외', title='서비스 레벨을 고르지 않으면 입찰 신청이 접수되지 않는다',
     pre='회원 계정, 아직 신청하지 않은 마감 전 공개 공고문 1건',
     steps='1. 그 공고 상세에서 [입찰 참여] 를 누른다\n2. 서비스 레벨을 고르지 않고 [신청하기] 를 누른다\n3. 서비스 레벨 없이 입찰 신청(POST /bid-requests)을 보낸다',
     expect='1. 2번 뒤 주소가 그 공고 상세(/search/{공고문 id}) 그대로다\n'
            '2. 3번 요청이 거부되고 그 공고의 입찰 요청 기록이 생기지 않는다',
     judge=['가상', '서버'],
     req=['BID-PRICE-002'], fc=['BID-PRICE-002'], screen='/search/{id}', pri='Medium'),

dict(id='IT-04-015', flow='적격성 진단 목록', kind='정상', title='회원이 메뉴로 적격성 진단을 열어 진단할 공고 목록을 본다',
     pre='회원 계정, 진단 대상 공고문(공개, 분석 끝, 참가 자격 4개) 마감 전 2건과 마감 지난 2건, 마감 전 1건에는 4개 중 2개를 답함',
     steps='1. 고객 화면에 회원 계정으로 로그인한다\n2. 메뉴 맞춤 판정 > 적격성 진단 을 누른다',
     expect='1. 머리에 "적격성 진단" 과 "공고가 요구하는 참가 자격을 하나씩 짚어 보고, 우리 회사에 있는지 직접 답해 두는 곳입니다" 가 보인다\n'
            '2. 진단 순서 세 칸이 공고 고르기, 자격마다 답하기, 모자란 자격은 전문가에게 순서로 있다\n'
            '3. 진단할 공고(마감 전) 칸에 마감 전 2건이 마감일 가까운 순으로 있다\n'
            '4. 줄마다 한글 제목, 공고번호, 시각 없이 날짜만 있는 마감일, "참가 자격 4개" 가 있다\n'
            '5. 2개 답한 공고 줄에 진행 막대와 "2 / 4" 가 있다\n'
            '6. 답이 없는 공고 줄에 "아직 안 함" 이 있다\n'
            '7. "지난 공고 2건" 칸에 마감 지난 2건이 마감일 최근 순으로 같은 줄 모양으로 있다\n'
            '8. 상담 띠에 "어느 자격이 걸리는지 모르겠다면" 과 [전문가 진단 신청] 이 있다\n'
            '9. 진단 목록 응답(GET /eligibility/notices)에 공개이고 분석이 끝나 참가 자격이 1개 이상인 공고문만 있다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility', pri='Medium'),

dict(id='IT-04-016', flow='적격성 진단 목록', kind='정상', title='마감 지난 진단 대상이 0건이면 지난 공고 칸을 두지 않는다',
     pre='회원 계정, 진단 대상 공고문이 마감 전 1건뿐',
     steps='1. 회원으로 /fit/eligibility 를 연다',
     expect='1. 마감 전 공고 1건 줄이 있다\n'
            '2. "지난 공고" 로 시작하는 칸 제목이 화면에 없다',
     judge=['가상', '가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility', pri='Medium'),

dict(id='IT-04-017', flow='적격성 진단 목록', kind='정상', title='마감 전 진단 대상이 0건이면 빈 상태 문구와 지난 공고가 보인다',
     pre='회원 계정, 진단 대상 공고문이 마감 지난 1건뿐',
     steps='1. 회원으로 /fit/eligibility 를 연다',
     expect='1. 마감 전 자리에 "진단할 수 있는 마감 전 공고가 아직 없습니다" 가 보인다\n'
            '2. 그 아래 "마감 전 공고는 분석이 끝나는 대로 여기에 올라옵니다. 그동안 아래 지난 공고로 미리 해 보세요" 가 보인다\n'
            '3. "지난 공고 1건" 칸에 마감 지난 그 공고 줄이 있다',
     judge=['가상', '가상', '가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility', pri='Medium'),

dict(id='IT-04-018', flow='적격성 진단 목록', kind='정상', title='마감 전과 지난 공고가 모두 0건이면 빈 상태 뒷문장을 뺀다',
     pre='회원 계정, 진단 대상 공고문 0건',
     steps='1. 회원으로 /fit/eligibility 를 연다',
     expect='1. "진단할 수 있는 마감 전 공고가 아직 없습니다" 와 "마감 전 공고는 분석이 끝나는 대로 여기에 올라옵니다." 가 보인다\n'
            '2. "그동안 아래 지난 공고로 미리 해 보세요" 라는 문장이 없다\n'
            '3. "지난 공고" 로 시작하는 칸 제목이 없다',
     judge=['가상', '가상', '가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility', pri='Medium'),

dict(id='IT-04-019', flow='적격성 진단 목록', kind='정상', title='공고 줄을 누르면 공고별 진단이 열리고 뒤로 오면 보던 위치다',
     pre='회원 계정, 진단 대상 공고문 10건 이상',
     steps='1. 회원으로 /fit/eligibility 를 연다\n2. 목록을 아래로 내려 8번째 줄을 누른다\n3. 브라우저 뒤로 가기를 누른다',
     expect='1. 2번에서 주소가 /fit/eligibility/{그 공고문 id} 로 바뀌고 공고 머리에 그 줄의 한글 제목이 보인다\n'
            '2. 3번에서 누르기 전 스크롤 위치로 돌아와 8번째 줄이 화면 안에 있다',
     judge=['가상', '가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility', pri='Medium'),

dict(id='IT-04-020', flow='적격성 진단 목록', kind='정상', title='공고 상세의 적격성 진단 단추로 공고별 진단에 들어간다',
     pre='회원 계정, 공고 분석에 참가 자격이 있는 공개 공고문 1건',
     steps='1. 회원으로 그 공고 상세(/search/{공고문 id})를 연다\n2. 참가 자격 칸 옆 [적격성 진단] 을 누른다',
     expect='1. 1번에서 참가 자격 칸 옆에 [적격성 진단] 단추가 하나 있다\n'
            '2. 2번에서 /fit/eligibility/{같은 공고문 id} 가 열려 공고 머리에 그 공고의 한글 제목이 보인다',
     judge=['가상', '가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/search/{id}', pri='Medium'),

dict(id='IT-04-021', flow='적격성 진단 목록', kind='정상', title='공고 분석에 참가 자격이 없는 공고 상세에는 적격성 진단 단추가 없다',
     pre='회원 계정, 공고 분석에 참가 자격이 0개인 공개 공고문 1건',
     steps='1. 회원으로 그 공고 상세(/search/{공고문 id})를 연다',
     expect='1. 공고 상세 어디에도 [적격성 진단] 단추가 없다',
     judge=['가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/search/{id}', pri='Medium'),

dict(id='IT-04-022', flow='적격성 진단 목록', kind='예외', title='비로그인은 진단 목록 대신 로그인 안내를 보고 로그인하면 돌아온다',
     pre='비로그인, 진단 대상 공고문 1건 이상',
     steps='1. 비로그인으로 /fit/eligibility 를 연다\n2. [로그인] 을 눌러 회원 계정으로 로그인한다',
     expect='1. 진단 순서 아래에 "로그인하면 진단할 공고가 보입니다" 와 "답은 회원마다 저장되어 다음에 와도 이어서 할 수 있습니다" 가 보인다\n'
            '2. 진단할 공고 줄이 하나도 없다\n'
            '3. [로그인] 과 [회원가입] 단추가 있다\n'
            '4. 로그인을 마치면 /fit/eligibility 로 돌아와 진단할 공고 줄이 있다\n'
            '5. 토큰 없이 진단 목록 조회(GET /eligibility/notices)를 부르면 401 LOGIN_REQUIRED 로 답한다',
     judge=['가상', '가상', '가상', '가상', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility', pri='Medium'),

dict(id='IT-04-023', flow='적격성 진단 목록', kind='예외', title='무료회원은 진단 목록 대신 회원 안내와 요금 안내 연결을 본다',
     pre='무료회원 계정, 진단 대상 공고문 1건 이상',
     steps='1. 무료회원으로 로그인해 메뉴 맞춤 판정 > 적격성 진단 을 누른다\n2. 무료회원 토큰으로 진단 목록 조회(GET /eligibility/notices)를 부른다\n3. 화면의 [요금 안내] 를 누른다',
     expect='1. 1번에서 진단할 공고 줄 대신 적격성 진단은 회원 전용이라는 회원 안내가 보인다\n'
            '2. 2번 응답이 403 MEMBERSHIP_REQUIRED 다\n'
            '3. 3번에서 요금 안내(/pricing)가 열린다',
     judge=['가상', '서버', '가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility', pri='Medium'),

dict(id='IT-04-024', flow='적격성 진단 목록', kind='예외', title='진단 목록을 불러오지 못하면 다시 시도를 준다',
     pre='회원 계정, 진단 목록 조회가 오류로 답하는 상태',
     steps='1. 회원으로 /fit/eligibility 를 연다\n2. 조회가 되살아난 뒤 [다시 시도] 를 누른다',
     expect='1. 1번에서 목록 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 보인다\n'
            '2. 2번 뒤 목록 자리에 진단할 공고 줄이 마감일 가까운 순으로 채워진다',
     judge=['가상', '가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility', pri='Medium'),

dict(id='IT-04-025', flow='자격 답하기', kind='정상', title='공고별 진단에 공고 머리, 개수 요약, 자격 목록, 끝 문구가 보인다',
     pre='회원 계정, 마감 전 진단 대상 공고문 1건(자격 4개 중 통과 조건 3개, 답 없음)',
     steps='1. 회원으로 /fit/eligibility 에서 그 공고 줄을 누른다\n2. 화면을 끝까지 내린다\n3. 공고 머리의 [공고 상세 보기] 를 누른다',
     expect='1. 공고 머리에 "마감 전" 꼬리표, 한글 제목, 공고번호, 날짜만 있는 마감일, 발주 기관 경로 끝 두 단계로 된 발주처가 있다\n'
            '2. 왼쪽 메뉴에서 적격성 진단이 켜져 있다\n'
            '3. 개수 요약이 통과 조건 자격 3개 중 있음 0, 없음 0, 확인 필요 0, 답 안 함 3 이다\n'
            '4. 있음 숫자는 성공 색, 없음은 위험 색, 확인 필요는 주의 색이다\n'
            '5. 자격 목록이 공고 분석 결과 순서대로 4줄이고 줄마다 순번, 이름, 통과 조건·필수 꼬리표, 설명, 원문 위치(예 "PWS 1.6.7.2"), 증빙 서류 글이 있다\n'
            '6. 공고별 진단 응답의 summary.gateCount 가 3, allCount 가 4 다\n'
            '7. 목록 끝에 "판정은 회원님이 남긴 답과 붙인 서류로만 냅니다. 서류 내용이 공고 조건에 맞는지는 보지 않습니다" 가 보인다\n'
            '8. 그 아래 서류함 서류는 이름으로 맞춘 것이라는 한 줄이 있다\n'
            '9. 화면 어디에도 AI 가 낸 "적합", "부적합", "조건부" 판정 글이 없다\n'
            '10. 3번에서 그 공고 상세(/search/{같은 공고문 id})가 열린다',
     judge=['가상', '가상', '가상', '가상', '가상', '서버', '가상', '가상', '가상', '가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-026', flow='자격 답하기', kind='정상', title='자격에 있음으로 답하면 저장되고 개수 요약이 바뀐다',
     pre='회원 계정, 진단 대상 공고문 1건(통과 조건 3개, 답 없음)',
     steps='1. 회원으로 그 공고별 진단을 연다\n2. 통과 조건 자격 1번의 [있음] 을 누른다\n3. 새로고침한다',
     expect='1. 2번에서 [있음] 만 채워지고 [없음], [확인 필요] 는 비어 있다\n'
            '2. 2번에서 개수 요약이 있음 1, 답 안 함 2 로 바뀐다\n'
            '3. 2번에서 답 단추 아래 "MM/DD HH:mm 저장" 꼴의 저장 시각이 있다\n'
            '4. 답 기록(eligibility_answers)에 그 회원, 그 공고문, 그 자격 순번으로 1행이 생기고 답 HAVE 와 그때의 자격 이름이 들어 있다\n'
            '5. 3번 뒤 공고별 진단 응답에서 그 자격의 answer 가 HAVE 다',
     judge=['가상', '가상', '가상', '서버', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-027', flow='자격 답하기', kind='정상', title='같은 자격에서 다른 답을 누르면 같은 기록이 바뀐다',
     pre='회원 계정, 진단 대상 공고문의 통과 조건 자격 1번에 있음으로 답함',
     steps='1. 그 공고별 진단에서 자격 1번의 [없음] 을 누른다',
     expect='1. 자격 1번에서 [없음] 만 채워져 있다\n'
            '2. 개수 요약의 있음이 1 줄고 없음이 1 는다\n'
            '3. 답 기록이 새로 생기지 않고 같은 행의 답이 LACK 으로 바뀐다',
     judge=['가상', '가상', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-028', flow='자격 답하기', kind='정상', title='이미 채운 답 단추를 다시 눌러도 답이 지워지지 않는다',
     pre='회원 계정, 진단 대상 공고문의 자격 1번에 있음으로 답함',
     steps='1. 그 공고별 진단에서 채워진 [있음] 을 다시 누른다',
     expect='1. [있음] 이 채워진 채로 남는다\n'
            '2. 개수 요약 숫자가 누르기 전과 같다\n'
            '3. 답 기록의 답이 HAVE 그대로다',
     judge=['가상', '가상', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-029', flow='자격 답하기', kind='정상', title='답을 저장하는 동안 그 자격 줄의 답 단추를 누를 수 없다',
     pre='회원 계정, 진단 대상 공고문 1건, 답 저장 응답이 늦게 오는 상태',
     steps='1. 자격 1번의 [있음] 을 누른다\n2. 저장 응답이 오기 전 [없음] 을 누른다\n3. 응답이 온 뒤 다시 본다',
     expect='1. 2번에서 자격 1번의 답 세 단추가 비활성이라 [없음] 이 눌리지 않는다\n'
            '2. 3번에서 [있음] 이 채워진 채로 답 세 단추가 다시 활성이다',
     judge=['가상', '가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-030', flow='자격 답하기', kind='정상', title='자격의 증빙 서류 글에 맞는 서류함 서류가 세 갈래로 보인다',
     pre='회원 계정, 진단 대상 공고문의 자격 A 는 증빙 서류 글이 지명원·회사소개 PPT 분류와 맞고 내 서류함에 그 분류 서류 2건(국문, 영문), 자격 B 는 재무제표 분류와 맞고 내 서류함에 재무제표 없음, 자격 C 는 맞는 분류 없음',
     steps='1. 회원으로 그 공고별 진단을 연다\n2. 자격 A, B, C 줄을 본다\n3. [서류함으로] 를 누른다',
     expect='1. 자격 A 줄에 그 분류에서 가장 최근에 올린 서류 이름, 분류 이름, 올린 날과 "외 1건" 이 있다\n'
            '2. 자격 B 줄에 "서류함에 재무제표 서류가 아직 없습니다" 가 보인다\n'
            '3. 자격 C 줄에는 서류함 줄이 없다\n'
            '4. 공고별 진단 응답에서 자격 C 의 vaultCategory 가 null 이다\n'
            '5. 3번에서 공통 서류함(/documents)이 열린다',
     judge=['가상', '가상', '가상', '서버', '가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-031', flow='자격 답하기', kind='정상', title='다른 회원의 답이 내 진단에 섞이지 않는다',
     pre='회원 계정 A 와 B, 진단 대상 공고문 1건, A 만 자격 1번에 있음으로 답함',
     steps='1. B 로 로그인해 그 공고별 진단을 연다\n2. B 로 /fit/eligibility 를 연다',
     expect='1. B 의 공고별 진단 응답에서 자격 1번 answer 가 null 이다\n'
            '2. B 의 개수 요약에서 있음이 0 이다\n'
            '3. B 의 진단 목록 응답에서 그 공고 answeredCount 가 0 이다',
     judge=['서버', '가상', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-032', flow='자격 답하기', kind='정상', title='마감 지난 공고의 공고별 진단은 마감 지남 꼬리표와 연습 안내를 보인다',
     pre='회원 계정, 마감 지난 진단 대상 공고문 1건',
     steps='1. /fit/eligibility 지난 공고 칸에서 그 줄을 누른다\n2. 자격 하나에 [있음] 을 누른다',
     expect='1. 공고 머리에 "마감 지남" 꼬리표가 있다\n'
            '2. "마감이 지난 공고입니다. 진단 방법을 미리 익혀 두는 용도로 쓰세요." 가 보인다\n'
            '3. 2번 답이 답 기록에 HAVE 로 저장된다',
     judge=['가상', '가상', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-033', flow='자격 답하기', kind='정상', title='공고 분석이 바뀐 자격에 다시 답하면 안내가 사라지고 새 이름으로 저장된다',
     pre='회원 계정, 자격 2번에 있음으로 답한 뒤 공고 분석을 다시 돌려(관리자 [재생성]) 자격 2번 이름이 바뀐 공고문',
     steps='1. 그 공고별 진단을 연다\n2. 자격 2번의 [확인 필요] 를 누른다',
     expect='1. 2번 뒤 자격 2번 위의 "공고 분석이 바뀌었습니다. 다시 답해 주세요" 가 사라진다\n'
            '2. 2번 뒤 개수 요약의 답 안 함이 1 줄고 확인 필요가 1 는다\n'
            '3. 답 기록 같은 행의 자격 이름이 지금 이름으로, 답이 UNSURE 로 바뀐다',
     judge=['가상', '가상', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-034', flow='자격 답하기', kind='정상', title='관리자가 공고문을 지우면 그 공고문의 답도 지워진다',
     pre='관리자 계정, 회원이 자격 2개에 답한 공고문 X',
     steps='1. 관리자 화면 공고문 관리 상세에서 X 를 노출 해제한다\n2. [삭제] 를 누르고 확인한다\n3. 회원으로 /fit/eligibility 를 연다',
     expect='1. 답 기록(eligibility_answers)에서 X 의 행이 0행이 된다\n'
            '2. 회원의 진단 목록 응답에 X 가 없다',
     judge=['서버', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='관리자 /notices/{id}', pri='Medium'),

dict(id='IT-04-035', flow='자격 답하기', kind='예외', title='비로그인으로 공고별 진단 주소를 열면 로그인을 거쳐 같은 주소로 돌아온다',
     pre='비로그인, 진단 대상 공고문 1건',
     steps='1. 비로그인으로 /fit/eligibility/{공고문 id} 를 직접 연다\n2. 회원 계정으로 로그인한다\n3. 토큰 없이 공고별 진단 조회(GET /eligibility/notices/{공고문 id})를 부른다',
     expect='1. 1번에서 로그인 화면(/login)으로 간다\n'
            '2. 2번 뒤 /fit/eligibility/{같은 공고문 id} 로 돌아와 자격 목록이 있다\n'
            '3. 3번 응답이 401 LOGIN_REQUIRED 다',
     judge=['가상', '가상', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-036', flow='자격 답하기', kind='예외', title='무료회원은 공고별 진단과 답 저장을 서버에서 막힌다',
     pre='무료회원 계정, 진단 대상 공고문 1건',
     steps='1. 무료회원으로 /fit/eligibility/{공고문 id} 를 직접 연다\n2. 무료회원 토큰으로 공고별 진단 조회를 부른다\n3. 무료회원 토큰으로 답 저장(PUT /eligibility/notices/{공고문 id}/answers/0, 답 HAVE)을 보낸다',
     expect='1. 1번에서 자격 목록 대신 적격성 진단은 회원 전용이라는 회원 안내와 [요금 안내] 가 보인다\n'
            '2. 2번 응답이 403 MEMBERSHIP_REQUIRED 다\n'
            '3. 3번 응답이 403 MEMBERSHIP_REQUIRED 이고 답 기록이 생기지 않는다',
     judge=['가상', '서버', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-037', flow='자격 답하기', kind='예외', title='숨김 공고문은 진단할 수 없는 공고로 막힌다',
     pre='회원 계정, 분석이 끝나고 참가 자격이 있으나 관리자가 노출 해제(HIDDEN)한 공고문 1건',
     steps='1. 회원으로 /fit/eligibility/{그 공고문 id} 를 연다\n2. [적격성 진단 목록으로] 를 누른다',
     expect='1. "진단할 수 없는 공고입니다" 가 보이고 자격 목록이 없다\n'
            '2. 공고별 진단 조회 응답이 404 ELIGIBILITY_NOTICE_NOT_FOUND 다\n'
            '3. 2번에서 /fit/eligibility 가 열리고 목록에 그 공고가 없다',
     judge=['가상', '서버', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-038', flow='자격 답하기', kind='예외', title='분석이 끝나지 않은 공고문은 진단과 답 저장을 받지 않는다',
     pre='회원 계정, 공개(VISIBLE)이나 분석 상태가 COMPLETED 가 아닌 공고문 1건',
     steps='1. 회원으로 /fit/eligibility/{그 공고문 id} 를 연다\n2. 답 저장(PUT .../answers/0, 답 HAVE)을 보낸다',
     expect='1. "진단할 수 없는 공고입니다" 가 보인다\n'
            '2. 공고별 진단 조회 응답이 404 ELIGIBILITY_NOTICE_NOT_FOUND 다\n'
            '3. 2번 요청이 거부되고 답 기록이 생기지 않는다',
     judge=['가상', '서버', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-039', flow='자격 답하기', kind='예외', title='참가 자격이 0개인 공고문은 진단 대상이 아니다',
     pre='회원 계정, 공개이고 분석이 끝났으나 참가 자격이 0개인 공고문 1건',
     steps='1. 회원으로 /fit/eligibility 를 연다\n2. /fit/eligibility/{그 공고문 id} 를 직접 연다',
     expect='1. 진단 목록 응답에 그 공고문이 없다\n'
            '2. 2번에서 "진단할 수 없는 공고입니다" 가 보인다\n'
            '3. 공고별 진단 조회 응답이 404 ELIGIBILITY_NOTICE_NOT_FOUND 다',
     judge=['서버', '가상', '서버'],
     req=['BID-QUAL-001', 'BID-MKT-019'], fc=['BID-QUAL-001', 'BID-MKT-019'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-040', flow='자격 답하기', kind='예외', title='공고 분석이 바뀐 자격의 옛 답은 답 안 함으로 센다',
     pre='회원 계정, 통과 조건 자격 4개 중 2개를 있음으로 답한 뒤 공고 분석을 다시 돌려 그중 자격 2번 이름이 바뀐 공고문',
     steps='1. 그 공고별 진단을 연다\n2. /fit/eligibility 에서 그 공고 줄을 본다',
     expect='1. 자격 2번 답 단추 위에 "공고 분석이 바뀌었습니다. 다시 답해 주세요" 가 보인다\n'
            '2. 자격 2번의 답 세 단추가 모두 비어 있다\n'
            '3. 개수 요약이 있음 1, 답 안 함 3 이다\n'
            '4. 공고별 진단 응답에서 자격 2번 stale 이 true, answer 가 null 이다\n'
            '5. 진단 목록 응답에서 그 공고 answeredCount 가 1 이다',
     judge=['가상', '가상', '가상', '서버', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-041', flow='자격 답하기', kind='예외', title='셋 중 하나가 아닌 답 값은 거부한다',
     pre='회원 계정, 진단 대상 공고문 1건, 자격 0번에 HAVE 로 답함',
     steps='1. 답 저장(PUT /eligibility/notices/{공고문 id}/answers/0)을 본문 answer "MAYBE" 로 보낸다',
     expect='1. 응답이 400 ELIGIBILITY_ANSWER_INVALID 다\n'
            '2. 답 기록의 답이 HAVE 그대로다',
     judge=['서버', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='', pri='Medium'),

dict(id='IT-04-042', flow='자격 답하기', kind='예외', title='자격 목록 범위 밖 순번의 답은 거부한다',
     pre='회원 계정, 참가 자격 4개인 진단 대상 공고문 1건',
     steps='1. 답 저장(PUT /eligibility/notices/{공고문 id}/answers/9, 답 HAVE)을 보낸다',
     expect='1. 응답이 400 ELIGIBILITY_ITEM_INVALID 다\n'
            '2. 그 공고문의 답 기록에 순번 9 행이 생기지 않는다',
     judge=['서버', '서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='', pri='Medium'),

dict(id='IT-04-043', flow='자격 답하기', kind='예외', title='공고문 id 가 UUID 모양이 아니면 공고별 진단 조회를 거부한다',
     pre='회원 계정',
     steps='1. 공고별 진단 조회(GET /eligibility/notices/abc)를 부른다',
     expect='1. 응답이 400 이다',
     judge=['서버'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='', pri='Medium'),

dict(id='IT-04-044', flow='자격 답하기', kind='예외', title='답 저장에 실패하면 단추가 누르기 전 답으로 돌아간다',
     pre='회원 계정, 자격 1번에 있음으로 답함, 답 저장이 오류로 답하는 상태',
     steps='1. 자격 1번의 [없음] 을 누른다',
     expect='1. 자격 1번 줄 아래 "저장하지 못했습니다" 가 보인다\n'
            '2. 자격 1번 단추가 [있음] 만 채워진 상태로 돌아간다\n'
            '3. 개수 요약 숫자가 누르기 전과 같다',
     judge=['가상', '가상', '가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-045', flow='전문가 진단 신청', kind='정상', title='없음과 확인 필요가 남으면 상담 띠가 보이고 신청하면 1:1 문의가 채워져 열린다',
     pre='회원 계정, 진단 대상 공고문의 자격 A 는 없음, 자격 B 는 확인 필요로 답함',
     steps='1. 그 공고별 진단을 연다\n2. 상담 띠의 [전문가 진단 신청] 을 누른다',
     expect='1. 상담 띠에 "없음 1개, 확인 필요 1개가 남았습니다" 와 "전문가가 이 공고의 자격을 함께 봐 드립니다" 가 보인다\n'
            '2. 상담 띠에 자격 A 와 자격 B 의 이름이 있다\n'
            '3. 2번에서 1:1 문의(/support)가 열려 문의 유형이 서비스 상담으로 골라져 있다\n'
            '4. 제목 칸이 "적격성 진단 상담 {그 공고번호}" 다\n'
            '5. 내용 칸에 "없음: {자격 A 이름}" 과 "확인 필요: {자격 B 이름}" 이 있다\n'
            '6. 폼 위에 초록 줄 "문의 유형과 제목을 채워 두었습니다" 가 보인다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-046', flow='전문가 진단 신청', kind='정상', title='없음과 확인 필요 답이 하나도 없으면 공고별 진단의 상담 띠를 두지 않는다',
     pre='회원 계정, 진단 대상 공고문의 자격을 모두 있음이거나 답 안 함으로 둠',
     steps='1. 그 공고별 진단을 연다\n2. /fit/eligibility 를 연다',
     expect='1. 1번 화면에 "남았습니다" 로 끝나는 상담 띠와 [전문가 진단 신청] 이 없다\n'
            '2. 2번 화면에는 상담 띠 "어느 자격이 걸리는지 모르겠다면" 이 있다',
     judge=['가상', '가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-047', flow='전문가 진단 신청', kind='정상', title='통과 조건이 아닌 자격만 없음이어도 상담 띠가 보인다',
     pre='회원 계정, 진단 대상 공고문의 통과 조건 자격은 모두 있음, 통과 조건이 아닌 자격 C 만 없음으로 답함',
     steps='1. 그 공고별 진단을 연다',
     expect='1. 상담 띠에 "없음 1개" 로 시작하는 줄과 자격 C 이름이 있다',
     judge=['가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-048', flow='전문가 진단 신청', kind='정상', title='적격성 진단 첫 화면 상담 띠의 신청은 제목 "적격성 진단 상담" 으로 1:1 문의를 연다',
     pre='비로그인',
     steps='1. /fit/eligibility 를 연다\n2. 상담 띠의 [전문가 진단 신청] 을 누른다',
     expect='1. 상담 띠에 "공고번호를 남겨 주시면 전문가가 참가 자격을 함께 봐 드립니다. 상담은 1:1 문의로 접수됩니다" 가 보인다\n'
            '2. 2번에서 1:1 문의(/support)가 열려 문의 유형이 서비스 상담으로 골라져 있다\n'
            '3. 제목 칸이 "적격성 진단 상담" 이다',
     judge=['가상', '가상', '가상'],
     req=['BID-QUAL-001'], fc=['BID-QUAL-001'], screen='/fit/eligibility', pri='Medium'),

dict(id='IT-04-049', flow='증빙 서류 붙이기', kind='정상', title='있음으로 답한 자격에 제안 서류를 붙이면 이름과 만료일이 보인다',
     pre='회원 계정, 진단 대상 공고문의 통과 조건 자격 A 는 증빙 서류 글이 사업자등록증 분류와 맞음, 내 서류함에 사업자등록증 1건(만료일 2027-12-31)',
     steps='1. 그 공고별 진단에서 자격 A 의 [있음] 을 누른다\n2. 자격 A 줄 제안 서류의 [붙이기] 를 누른다',
     expect='1. 1번 뒤 자격 A 줄에 "증빙 서류를 붙여 주세요" 와 제안 서류로 그 사업자등록증 이름이 있다\n'
            '2. 2번 뒤 자격 A 줄에 붙인 서류 이름과 만료일 2027-12-31 이 있다\n'
            '3. 답 기록 그 행의 document_id 가 그 서류의 id 다\n'
            '4. 요약 옆 판정 꼬리표가 붙이기 저장 응답의 verdict 와 같은 판정 글로 바뀐다',
     judge=['가상', '가상', '서버', '가상'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-050', flow='증빙 서류 붙이기', kind='정상', title='서류 고르기 창에서 내 서류를 골라 붙인다',
     pre='회원 계정 A 와 B, A 의 서류함에 서류 3건, B 의 서류함에 서류 1건, A 가 자격 A 에 있음으로 답함',
     steps='1. A 로 그 공고별 진단에서 자격 A 줄의 [서류 고르기] 를 누른다\n2. 창에서 둘째 서류를 고른다',
     expect='1. 창에 A 의 서류 3건이 줄마다 이름, 분류, 만료일로 있다\n'
            '2. 창에 B 의 서류가 없다\n'
            '3. 2번 뒤 자격 A 줄에 고른 서류 이름과 만료일이 있다\n'
            '4. 답 기록 그 행의 document_id 가 고른 서류의 id 다',
     judge=['가상', '서버', '가상', '서버'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-051', flow='증빙 서류 붙이기', kind='정상', title='붙인 서류를 떼면 풀리고 판정이 다시 난다',
     pre='회원 계정, 진단 대상 공고문의 통과 조건 자격을 모두 있음으로 답하고 만료되지 않은 서류를 붙여 판정이 참여 가능',
     steps='1. 통과 조건 자격 A 줄의 [떼기] 를 누른다',
     expect='1. 자격 A 줄에서 붙인 서류 이름이 사라지고 "증빙 서류를 붙여 주세요" 가 보인다\n'
            '2. 답 기록 그 행의 document_id 가 비워진다\n'
            '3. 요약 옆 판정 꼬리표가 "참여 가능" 에서 "서류 부족" 으로 바뀐다',
     judge=['가상', '서버', '가상'],
     req=['BID-MKT-018', 'BID-MKT-019'], fc=['BID-MKT-018', 'BID-MKT-019'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-052', flow='증빙 서류 붙이기', kind='정상', title='있음을 없음이나 확인 필요로 바꾸면 붙인 서류가 풀린다',
     pre='회원 계정, 진단 대상 공고문의 자격 A 와 B 를 있음으로 답하고 각각 서류를 붙임',
     steps='1. 자격 A 의 [없음] 을 누른다\n2. 자격 B 의 [확인 필요] 를 누른다',
     expect='1. 1번 뒤 자격 A 줄에 붙인 서류 이름이 없다\n'
            '2. 1번 뒤 자격 A 답 기록의 document_id 가 비어 있다\n'
            '3. 2번 뒤 자격 B 줄에 붙인 서류 이름이 없다\n'
            '4. 2번 뒤 자격 B 답 기록의 document_id 가 비어 있다',
     judge=['가상', '서버', '가상', '서버'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-053', flow='증빙 서류 붙이기', kind='정상', title='옛 답에 다시 답하면 붙였던 서류가 풀린다',
     pre='회원 계정, 자격 A 에 있음으로 답하고 서류를 붙인 뒤 공고 분석을 다시 돌려 자격 A 이름이 바뀐 공고문',
     steps='1. 그 공고별 진단에서 자격 A 의 [있음] 을 누른다',
     expect='1. 자격 A 줄에 "증빙 서류를 붙여 주세요" 가 보이고 붙인 서류 이름이 없다\n'
            '2. 답 기록 그 행의 document_id 가 비어 있다',
     judge=['가상', '서버'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-054', flow='증빙 서류 붙이기', kind='정상', title='서류함에서 붙인 서류를 지우면 붙임이 풀리고 서류 부족이 된다',
     pre='회원 계정, 통과 조건 자격을 모두 있음으로 답하고 자격 A 에 서류 D 를 붙여 판정이 참여 가능',
     steps='1. 공통 서류함(/documents)에서 서류 D 를 지운다\n2. 그 공고별 진단을 다시 연다',
     expect='1. 자격 A 줄에 붙인 서류 이름이 없고 "증빙 서류를 붙여 주세요" 가 보인다\n'
            '2. 답 기록 그 행의 document_id 가 비어 있다\n'
            '3. 요약 옆 판정 꼬리표가 "서류 부족" 이고 걸린 곳이 "{자격 A 이름}: 증빙 서류 없음" 이다',
     judge=['가상', '서버', '가상'],
     req=['BID-MKT-018', 'BID-MKT-019'], fc=['BID-MKT-018', 'BID-MKT-019'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-055', flow='증빙 서류 붙이기', kind='정상', title='한 서류를 여러 자격과 공고에 붙일 수 있다',
     pre='회원 계정, 진단 대상 공고문 X 와 Y, X 의 자격 A 와 Y 의 자격 B 를 있음으로 답함, 서류함에 서류 D',
     steps='1. X 공고별 진단에서 자격 A 에 서류 D 를 붙인다\n2. Y 공고별 진단에서 자격 B 에 서류 D 를 붙인다',
     expect='1. X 자격 A 줄과 Y 자격 B 줄에 모두 서류 D 이름이 있다\n'
            '2. 답 기록 두 행의 document_id 가 모두 서류 D 의 id 다',
     judge=['가상', '서버'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-056', flow='증빙 서류 붙이기', kind='정상', title='증빙 서류 글이 분류와 맞지 않으면 자격 이름이 맞는 분류의 서류를 제안한다',
     pre='회원 계정, 진단 대상 공고문 자격 A 의 증빙 서류 글은 어느 분류 낱말과도 맞지 않고 자격 이름에 "재무제표" 가 들어 있음, 서류함에 재무제표 1건',
     steps='1. 그 공고별 진단에서 자격 A 의 [있음] 을 누른다',
     expect='1. 자격 A 줄에 "증빙 서류를 붙여 주세요" 와 제안 서류로 그 재무제표 서류 이름이 있다\n'
            '2. 공고별 진단 응답에서 자격 A 의 vaultCategory 가 FINANCIAL_STATEMENT 다',
     judge=['가상', '서버'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-057', flow='증빙 서류 붙이기', kind='정상', title='제안 서류는 맞은 분류에서 만료되지 않은 서류다',
     pre='회원 계정, 자격 A 의 증빙 서류 글이 지명원·회사소개 PPT 분류와 맞음, 그 분류에 국문 서류(만료일 내년)와 영문 서류(만료일 어제)',
     steps='1. 그 공고별 진단에서 자격 A 의 [있음] 을 누른다',
     expect='1. 제안 서류로 만료되지 않은 국문 서류 이름이 있다\n'
            '2. 만료된 영문 서류 이름은 제안 서류 자리에 없다',
     judge=['가상', '가상'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-058', flow='증빙 서류 붙이기', kind='정상', title='맞은 분류 서류가 모두 만료됐으면 새 서류를 올리라는 줄과 서류함에 올리기가 있다',
     pre='회원 계정, 자격 A 의 증빙 서류 글이 사업자등록증 분류와 맞음, 그 분류 서류 1건 만료일 어제',
     steps='1. 그 공고별 진단에서 자격 A 의 [있음] 을 누른다\n2. [서류함에 올리기] 를 누른다',
     expect='1. 자격 A 줄에 제안 서류 이름 없이 새 서류를 올리라는 한 줄이 있다\n'
            '2. 2번에서 공통 서류함(/documents)이 열린다',
     judge=['가상', '가상'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-059', flow='증빙 서류 붙이기', kind='정상', title='국문과 영문 서류가 모두 유효하면 영문 서류를 먼저 제안한다',
     pre='회원 계정, 자격 A 의 증빙 서류 글이 사업자등록증명원 분류와 맞음, 그 분류에 영문 서류(먼저 올림)와 국문 서류(나중에 올림), 둘 다 만료일 내년',
     steps='1. 그 공고별 진단에서 자격 A 의 [있음] 을 누른다',
     expect='1. 제안 서류로 영문 서류 이름이 있다',
     judge=['가상'],
     req=['BID-MKT-018', 'BID-MKT-035'], fc=['BID-MKT-018', 'BID-MKT-035'], screen='/fit/eligibility/{공고문}', pri='Medium',
     issue='T3-3 공고별 진단은 "만료되지 않은 가장 최근 서류" 를 제안한다고 하고 BIZ-065 는 국문·영문 칸에서 영문을 먼저 고른다고 한다. 기대결과는 BIZ-065 를 따랐다'),

dict(id='IT-04-060', flow='증빙 서류 붙이기', kind='정상', title='붙인 서류의 만료일이 지났으면 위험 색 "만료됨" 이 보인다',
     pre='회원 계정, 자격 A 에 있음으로 답하고 만료일이 어제인 서류를 붙임',
     steps='1. 그 공고별 진단을 연다',
     expect='1. 자격 A 줄 서류 이름 옆에 위험 색 "만료됨 {어제 날짜}" 가 보인다\n'
            '2. 공고별 진단 응답에서 자격 A 의 attachedDocument.expired 가 true 다',
     judge=['가상', '서버'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-061', flow='증빙 서류 붙이기', kind='정상', title='서류함이 비어 있으면 서류 고르기 창이 서류함으로 안내한다',
     pre='회원 계정, 서류함 0건, 자격 A 에 있음으로 답함',
     steps='1. 자격 A 줄의 [서류 고르기] 를 누른다\n2. 창의 [서류함으로] 를 누른다',
     expect='1. 창 안에 "서류함에 올린 서류가 없습니다" 가 보인다\n'
            '2. 2번에서 공통 서류함(/documents)이 열린다',
     judge=['가상', '가상'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-062', flow='증빙 서류 붙이기', kind='정상', title='서류를 붙이는 동안 그 줄의 답 단추와 서류 단추를 누를 수 없다',
     pre='회원 계정, 자격 A 에 있음으로 답함, 서류 붙이기 응답이 늦게 오는 상태',
     steps='1. 자격 A 제안 서류의 [붙이기] 를 누른다\n2. 응답이 오기 전 자격 A 의 [없음] 과 [서류 고르기] 를 누른다',
     expect='1. 2번에서 자격 A 의 답 세 단추가 비활성이라 눌리지 않는다\n'
            '2. 2번에서 자격 A 의 [서류 고르기] 가 비활성이라 창이 열리지 않는다',
     judge=['가상', '가상'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-063', flow='증빙 서류 붙이기', kind='예외', title='서류함을 불러오지 못하면 고르기 창 안에 알린다',
     pre='회원 계정, 자격 A 에 있음으로 답함, 서류함 조회가 오류로 답하는 상태',
     steps='1. 자격 A 줄의 [서류 고르기] 를 누른다',
     expect='1. 창 안에 "서류함을 불러오지 못했습니다" 가 보인다',
     judge=['가상'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-064', flow='증빙 서류 붙이기', kind='예외', title='서류 붙이기 저장에 실패하면 붙인 서류가 누르기 전으로 돌아간다',
     pre='회원 계정, 자격 A 에 있음으로 답하고 서류 D 를 붙임, 서류 붙이기 저장이 오류로 답하는 상태',
     steps='1. 자격 A 줄의 [떼기] 를 누른다',
     expect='1. 자격 A 줄 아래 "저장하지 못했습니다" 가 보인다\n'
            '2. 자격 A 줄에 서류 D 이름이 그대로 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-065', flow='증빙 서류 붙이기', kind='예외', title='남의 서류함 서류는 붙이지 못한다',
     pre='회원 계정 A 와 B, A 가 자격 0번에 있음으로 답함, B 의 서류 E',
     steps='1. A 토큰으로 서류 붙이기(PUT /eligibility/notices/{공고문 id}/answers/0/document, documentId 는 서류 E id)를 보낸다',
     expect='1. 응답이 404 ELIGIBILITY_DOCUMENT_NOT_FOUND 다\n'
            '2. A 의 답 기록 그 행 document_id 가 바뀌지 않는다',
     judge=['서버', '서버'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='', pri='Medium'),

dict(id='IT-04-066', flow='증빙 서류 붙이기', kind='예외', title='있음이 아닌 답의 자격에는 서류를 붙이지 못한다',
     pre='회원 계정, 자격 0번은 없음, 자격 1번은 답 없음, 내 서류 D',
     steps='1. 공고별 진단에서 자격 0번 줄을 본다\n2. 서류 붙이기(PUT .../answers/0/document, 서류 D)를 보낸다\n3. 서류 붙이기(PUT .../answers/1/document, 서류 D)를 보낸다',
     expect='1. 자격 0번 줄에 "증빙 서류를 붙여 주세요" 와 [서류 고르기] 가 없다\n'
            '2. 2번 응답이 400 ELIGIBILITY_DOCUMENT_NOT_ALLOWED 이고 그 행 document_id 가 비어 있다\n'
            '3. 3번 응답이 400 ELIGIBILITY_DOCUMENT_NOT_ALLOWED 다',
     judge=['가상', '서버', '서버'],
     req=['BID-MKT-018'], fc=['BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-067', flow='판정 계산', kind='정상', title='통과 조건을 모두 있음으로 답하고 서류를 다 붙이면 참여 가능이다',
     pre='회원 계정, 진단 대상 공고문(통과 조건 3개), 셋 다 있음으로 답하고 만료되지 않은 서류를 붙임',
     steps='1. 그 공고별 진단을 연다\n2. /fit/verdicts 에서 그 공고 줄을 본다',
     expect='1. 1번 요약 옆에 성공 색 "참여 가능" 꼬리표가 있다\n'
            '2. 2번 줄의 걸린 곳이 "통과 조건 3개 모두 있음, 증빙 서류 붙임" 이다\n'
            '3. 공고별 진단 응답의 verdict 가 ELIGIBLE 이고 blockers 가 빈 배열이다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-019'], fc=['BID-MKT-019'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-068', flow='판정 계산', kind='정상', title='통과 조건에 없음이 하나라도 있으면 다른 답과 상관없이 어려움이다',
     pre='회원 계정, 진단 대상 공고문(통과 조건 4개) A 없음, B 없음, C 답 안 함, D 있음',
     steps='1. /fit/verdicts 에서 그 공고 줄을 본다',
     expect='1. 위험 색 "어려움" 꼬리표가 있다\n'
            '2. 걸린 곳이 "{A 이름}: 없음으로 답함, {A 원문 위치} 외 1개" 다\n'
            '3. 목록 응답에서 그 공고 verdict 가 DIFFICULT 이고 blockers 가 problem LACK 2개뿐이다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-019'], fc=['BID-MKT-019'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-069', flow='판정 계산', kind='정상', title='답 안 함과 확인 필요가 남으면 판정 전이고 개수를 걸린 곳에 적는다',
     pre='회원 계정, 진단 대상 공고문(통과 조건 4개) 있음 1, 답 안 함 2, 확인 필요 1',
     steps='1. /fit/verdicts 에서 그 공고 줄을 본다',
     expect='1. 회색 "판정 전" 꼬리표가 있다\n'
            '2. 걸린 곳이 "통과 조건 4개 중 답 안 함 2개, 확인 필요 1개" 다\n'
            '3. 목록 응답에서 verdict 가 UNDECIDED 이고 blockers 가 UNANSWERED 2개와 UNSURE 1개다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-019'], fc=['BID-MKT-019'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-070', flow='판정 계산', kind='정상', title='옛 답이 있으면 판정 전이고 다시 답해야 함으로 센다',
     pre='회원 계정, 통과 조건 3개를 모두 있음으로 답하고 서류를 붙인 뒤 공고 분석이 바뀌어 1개가 옛 답이 된 공고문',
     steps='1. /fit/verdicts 에서 그 공고 줄을 본다',
     expect='1. 회색 "판정 전" 꼬리표가 있다\n'
            '2. 걸린 곳이 "통과 조건 3개 중 다시 답해야 함 1개" 다\n'
            '3. 목록 응답의 blockers 가 problem STALE 1개다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-019'], fc=['BID-MKT-019'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-071', flow='판정 계산', kind='정상', title='모두 있음이나 서류를 안 붙인 자격이 있으면 서류 부족이다',
     pre='회원 계정, 통과 조건 3개 모두 있음, 그중 A 와 B 에 서류를 붙이지 않음',
     steps='1. /fit/verdicts 에서 그 공고 줄을 본다',
     expect='1. 주의 색 "서류 부족" 꼬리표가 있다\n'
            '2. 걸린 곳이 "{A 이름}: 증빙 서류 없음 외 1개" 다\n'
            '3. 목록 응답에서 verdict 가 DOCS_MISSING 이고 blockers 가 NO_DOCUMENT 2개다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-019'], fc=['BID-MKT-019'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-072', flow='판정 계산', kind='정상', title='붙인 서류의 만료일이 오늘보다 앞이면 서류 부족이고 만료일을 적는다',
     pre='회원 계정, 통과 조건 2개 모두 있음, A 에 만료일이 어제(한국 날짜)인 서류, B 에 만료일 내년 서류',
     steps='1. /fit/verdicts 에서 그 공고 줄을 본다',
     expect='1. 주의 색 "서류 부족" 꼬리표가 있다\n'
            '2. 걸린 곳이 "{A 이름}: 증빙 서류 만료 {어제 날짜}" 다\n'
            '3. 목록 응답의 blockers 가 problem EXPIRED 1개이고 documentName, expiresOn 이 그 서류의 값이다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-019'], fc=['BID-MKT-019'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-073', flow='판정 계산', kind='정상', title='만료일이 오늘인 서류는 있는 것으로 본다',
     pre='회원 계정, 통과 조건 2개 모두 있음, 둘 다 서류를 붙였고 하나는 만료일이 오늘(Asia/Seoul 날짜)',
     steps='1. 그 공고별 진단을 연다',
     expect='1. 요약 옆 판정 꼬리표가 "참여 가능" 이다\n'
            '2. 공고별 진단 응답의 verdict 가 ELIGIBLE 이다',
     judge=['가상', '서버'],
     req=['BID-MKT-019'], fc=['BID-MKT-019'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-074', flow='판정 계산', kind='정상', title='만료일이 빈 서류는 있는 것으로 본다',
     pre='회원 계정, 통과 조건 2개 모두 있음, 둘 다 서류를 붙였고 하나는 만료일이 비어 있음',
     steps='1. 그 공고별 진단을 연다',
     expect='1. 요약 옆 판정 꼬리표가 "참여 가능" 이다\n'
            '2. 공고별 진단 응답의 verdict 가 ELIGIBLE 이다',
     judge=['가상', '서버'],
     req=['BID-MKT-019'], fc=['BID-MKT-019'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-075', flow='판정 계산', kind='정상', title='통과 조건이 아닌 자격의 답은 판정에 넣지 않는다',
     pre='회원 계정, 통과 조건 2개는 있음과 서류 붙임, 통과 조건이 아닌 자격 C 는 없음',
     steps='1. 그 공고별 진단을 연다',
     expect='1. 요약 옆 판정 꼬리표가 "참여 가능" 이다\n'
            '2. 자격 목록의 자격 C 줄에 [없음] 이 채워진 채로 있다\n'
            '3. 공고별 진단 응답의 verdict 가 ELIGIBLE 이고 blockers 에 자격 C 가 없다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-019'], fc=['BID-MKT-019'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-076', flow='판정 계산', kind='정상', title='통과 조건이 0개인 공고는 판정 전이고 통과 조건이 없다고 밝힌다',
     pre='회원 계정, 참가 자격 2개가 모두 통과 조건이 아닌 진단 대상 공고문',
     steps='1. /fit/verdicts 에서 그 공고 줄을 본다\n2. 그 공고별 진단을 연다',
     expect='1. 1번 줄에 회색 "판정 전" 과 걸린 곳 "통과 조건으로 표시된 자격이 없습니다" 가 있다\n'
            '2. 2번 요약 자리에 "통과 조건으로 표시된 자격이 없습니다. 아래 자격을 확인해 주세요" 가 보인다\n'
            '3. 2번 자격 목록에 자격 2줄이 답 단추와 함께 있다\n'
            '4. 응답의 verdict 가 UNDECIDED, gateCount 가 0, blockers 가 빈 배열이다',
     judge=['가상', '가상', '가상', '서버'],
     req=['BID-MKT-019', 'BID-QUAL-001'], fc=['BID-MKT-019', 'BID-QUAL-001'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-077', flow='판정 계산', kind='정상', title='답을 바꾸면 공고별 판정, 공고 검색, 공고 상세의 판정이 함께 바뀐다',
     pre='회원 계정, 판정이 참여 가능인 마감 전 진단 대상 공고문 1건',
     steps='1. 그 공고별 진단에서 통과 조건 자격 하나를 [없음] 으로 바꾼다\n2. /fit/verdicts 를 연다\n3. 메뉴 입찰공고 > 마감 전 공고 를 연다\n4. 그 공고 상세(/search/{공고문 id})를 연다',
     expect='1. 2번에서 그 공고 줄 꼬리표가 "어려움" 이다\n'
            '2. 3번에서 그 공고 줄 우리 회사 칸이 "어려움" 이다\n'
            '3. 4번 판정 상자 꼬리표가 "어려움" 이다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-019', 'BID-MKT-015', 'BID-MKT-016', 'BID-MKT-017'], fc=['BID-MKT-019', 'BID-MKT-015', 'BID-MKT-016', 'BID-MKT-017'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-078', flow='판정 계산', kind='예외', title='판정할 수 없는 공고는 판정 자리를 비우고 판정 전에 섞지 않는다',
     pre='회원 계정, 판정할 수 있는 공고 2건(판정 전 1건), 공개이고 분석이 끝났으나 참가 자격이 0개인 공고문 1건',
     steps='1. /fit/verdicts 를 연다',
     expect='1. 공고 줄에 참가 자격 0개 공고가 없다\n'
            '2. 판정별 개수의 판정 전이 1 이다\n'
            '3. 목록 응답에 참가 자격 0개 공고문이 없다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-019', 'BID-MKT-015'], fc=['BID-MKT-019', 'BID-MKT-015'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-079', flow='판정 계산', kind='예외', title='한 공고의 답과 서류를 다른 공고에 쓰지 않는다',
     pre='회원 계정, 같은 이름의 통과 조건 자격이 있는 진단 대상 공고문 X 와 Y, X 에서만 그 자격을 있음으로 답하고 서류를 붙임',
     steps='1. Y 의 공고별 진단을 연다',
     expect='1. Y 공고별 진단 응답에서 그 자격 answer 가 null 이고 attachedDocument 가 null 이다\n'
            '2. Y 요약 옆 판정 꼬리표가 "판정 전" 이다\n'
            '3. 답 기록에 Y 공고문의 행이 없다',
     judge=['서버', '가상', '서버'],
     req=['BID-MKT-019', 'BID-MKT-018'], fc=['BID-MKT-019', 'BID-MKT-018'], screen='/fit/eligibility/{공고문}', pri='Medium'),

dict(id='IT-04-080', flow='공고별 판정', kind='정상', title='회원이 메뉴로 공고별 판정을 열어 공고마다 판정과 걸린 곳을 본다',
     pre='회원 계정, 판정할 수 있는 마감 전 공고 3건(참여 가능 1, 서류 부족 1, 판정 전 1)과 지난 공고 2건(어려움 1, 판정 전 1)',
     steps='1. 회원으로 로그인해 메뉴 맞춤 판정 > 공고별 판정 을 누른다\n2. 화면을 끝까지 내린다',
     expect='1. 머리에 "공고별 판정" 과 "적격성 진단에 남긴 답과 서류함 서류로 공고마다 우리 회사가 참여할 수 있는지 보여 드립니다" 가 보인다\n'
            '2. 판정 네 가지 꼬리표 참여 가능, 서류 부족, 어려움, 판정 전과 뜻이 한 줄씩 있다\n'
            '3. 왼쪽 메뉴에서 공고별 판정이 켜져 있다\n'
            '4. 판정별 개수가 참여 가능 1, 서류 부족 1, 어려움 1, 판정 전 2 다\n'
            '5. 마감 전 공고 3줄이 마감일 가까운 순이고 줄마다 판정 꼬리표, 한글 제목, 공고번호, 날짜만 있는 마감일, 걸린 곳 한 줄이 있다\n'
            '6. "지난 공고 2건" 칸에 2줄이 마감일 최근 순으로 있다\n'
            '7. 꼬리표 색이 참여 가능 성공 색, 서류 부족 주의 색, 어려움 위험 색, 판정 전 회색이다\n'
            '8. 끝에 "판정은 회원님이 남긴 답과 붙인 서류로만 냅니다. 서류 내용이 공고 조건에 맞는지는 보지 않습니다" 가 보인다\n'
            '9. 목록 응답의 공고가 같은 회원의 적격성 진단 목록 응답 공고와 같은 5건이다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상', '서버'],
     req=['BID-MKT-015'], fc=['BID-MKT-015'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-081', flow='공고별 판정', kind='정상', title='판정별 개수를 누르면 그 판정의 공고만 남고 다시 누르면 풀린다',
     pre='회원 계정, 판정할 수 있는 공고 5건(서류 부족 1건)',
     steps='1. /fit/verdicts 에서 판정별 개수의 "서류 부족" 을 누른다\n2. "서류 부족" 을 다시 누른다',
     expect='1. 1번 뒤 공고 줄이 서류 부족 꼬리표 1줄만 남는다\n'
            '2. 2번 뒤 공고 줄 5줄이 다시 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-015'], fc=['BID-MKT-015'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-082', flow='공고별 판정', kind='정상', title='거른 판정의 공고가 0건이면 "이 판정인 공고가 없습니다" 가 보인다',
     pre='회원 계정, 판정할 수 있는 공고가 있으나 어려움 0건',
     steps='1. /fit/verdicts 에서 판정별 개수의 "어려움" 을 누른다',
     expect='1. 공고 줄 자리에 "이 판정인 공고가 없습니다" 가 보인다',
     judge=['가상'],
     req=['BID-MKT-015'], fc=['BID-MKT-015'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-083', flow='공고별 판정', kind='정상', title='공고 줄을 누르면 공고별 진단이 열리고 뒤로 오면 위치와 거른 판정이 남는다',
     pre='회원 계정, 판정할 수 있는 공고 10건 이상(판정 전 6건 이상)',
     steps='1. /fit/verdicts 에서 "판정 전" 을 눌러 거른다\n2. 아래로 내려 5번째 줄을 누른다\n3. 브라우저 뒤로 가기를 누른다',
     expect='1. 2번에서 /fit/eligibility/{그 공고문 id} 가 열리고 왼쪽 메뉴에서 적격성 진단이 켜져 있다\n'
            '2. 3번 뒤 "판정 전" 거르기가 그대로 걸려 판정 전 줄만 있다\n'
            '3. 3번 뒤 누르기 전 스크롤 위치로 돌아와 5번째 줄이 화면 안에 있다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-015'], fc=['BID-MKT-015'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-084', flow='공고별 판정', kind='정상', title='마감 전 판정 대상이 0건이면 빈 상태 문구와 지난 공고가 보인다',
     pre='회원 계정, 판정할 수 있는 공고가 마감 지난 1건뿐',
     steps='1. /fit/verdicts 를 연다',
     expect='1. 마감 전 자리에 "판정할 수 있는 마감 전 공고가 아직 없습니다" 가 보인다\n'
            '2. 그 아래 "마감 전 공고는 분석이 끝나는 대로 여기에 올라옵니다. 그동안 아래 지난 공고로 미리 보세요" 가 보인다\n'
            '3. "지난 공고 1건" 칸에 그 공고 줄이 있다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-015'], fc=['BID-MKT-015'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-085', flow='공고별 판정', kind='정상', title='판정 대상이 모두 0건이면 뒷문장과 지난 공고 칸을 뺀다',
     pre='회원 계정, 판정할 수 있는 공고 0건',
     steps='1. /fit/verdicts 를 연다',
     expect='1. "판정할 수 있는 마감 전 공고가 아직 없습니다" 와 "마감 전 공고는 분석이 끝나는 대로 여기에 올라옵니다." 가 보인다\n'
            '2. "그동안 아래 지난 공고로 미리 보세요" 라는 문장이 없다\n'
            '3. "지난 공고" 로 시작하는 칸 제목이 없다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-015'], fc=['BID-MKT-015'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-086', flow='공고별 판정', kind='정상', title='공고별 판정 상담 띠의 신청은 제목 "공고별 판정 상담" 으로 1:1 문의를 연다',
     pre='회원 계정',
     steps='1. /fit/verdicts 끝의 상담 띠에서 [전문가 진단 신청] 을 누른다',
     expect='1. 1:1 문의(/support)가 열려 문의 유형이 서비스 상담으로 골라져 있다\n'
            '2. 제목 칸이 "공고별 판정 상담" 이다',
     judge=['가상', '가상'],
     req=['BID-MKT-015'], fc=['BID-MKT-015'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-087', flow='공고별 판정', kind='예외', title='비로그인은 판정 대신 판정 뜻과 로그인 안내를 보고 로그인하면 돌아온다',
     pre='비로그인, 판정할 수 있는 공고 1건 이상',
     steps='1. 비로그인으로 /fit/verdicts 를 연다\n2. [로그인] 을 눌러 회원 계정으로 로그인한다',
     expect='1. 판정 네 가지 꼬리표와 뜻이 한 줄씩 있다\n'
            '2. 머리 아래에 "로그인하면 공고마다 판정이 보입니다" 와 "판정은 적격성 진단에 남긴 답으로 냅니다" 가 보인다\n'
            '3. 판정별 개수와 공고 줄이 없다\n'
            '4. [로그인] 과 [회원가입] 단추가 있다\n'
            '5. 로그인을 마치면 /fit/verdicts 로 돌아와 판정별 개수와 공고 줄이 있다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-MKT-015'], fc=['BID-MKT-015'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-088', flow='공고별 판정', kind='예외', title='무료회원은 판정 대신 판정 뜻과 회원 안내를 본다',
     pre='무료회원 계정, 판정할 수 있는 공고 1건 이상',
     steps='1. 무료회원으로 로그인해 /fit/verdicts 를 연다',
     expect='1. 판정 네 가지 꼬리표와 뜻이 한 줄씩 있다\n'
            '2. 판정별 개수와 공고 줄 대신 판정은 회원 전용이라는 회원 안내가 보인다\n'
            '3. 무료회원 토큰으로 목록 조회(GET /eligibility/notices)를 부르면 403 MEMBERSHIP_REQUIRED 다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-015'], fc=['BID-MKT-015'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-089', flow='공고별 판정', kind='예외', title='판정 목록을 불러오지 못하면 다시 시도를 준다',
     pre='회원 계정, 목록 조회가 오류로 답하는 상태',
     steps='1. /fit/verdicts 를 연다',
     expect='1. 목록 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 보인다',
     judge=['가상'],
     req=['BID-MKT-015'], fc=['BID-MKT-015'], screen='/fit/verdicts', pri='Medium'),

dict(id='IT-04-090', flow='공고 검색 판정 칸', kind='정상', title='회원의 마감 전 공고 목록 우리 회사 칸에 판정 꼬리표가 보인다',
     pre='회원 계정, 마감 전 모은 공고 중 판정할 수 있는 공개 공고문이 있는 것 2건(어려움, 참여 가능), 공개 공고문은 있으나 참가 자격 0개 1건, 공개 공고문이 없는 것 1건',
     steps='1. 회원으로 메뉴 입찰공고 > 마감 전 공고 를 누른다\n2. 참여 가능 꼬리표를 누른다',
     expect='1. 공고 목록에 "우리 회사" 칸이 있다\n'
            '2. 판정할 수 있는 두 줄의 우리 회사 칸이 "어려움" 과 "참여 가능" 이다\n'
            '3. 참가 자격 0개 공고 줄의 우리 회사 칸이 "-" 다\n'
            '4. 공개 공고문이 없는 줄의 우리 회사 칸이 "-" 다\n'
            '5. 2번에서 그 공고 상세(/search/{공고문 id})가 열린다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-MKT-016'], fc=['BID-MKT-016'], screen='/search', pri='Medium'),

dict(id='IT-04-091', flow='공고 검색 판정 칸', kind='예외', title='무료회원의 우리 회사 칸에는 판정 대신 회원 꼬리표가 보인다',
     pre='무료회원 계정, 마감 전 모은 공고 중 공개 공고문이 있는 것 1건, 없는 것 1건',
     steps='1. 무료회원으로 /search 를 연다',
     expect='1. 공개 공고문이 있는 줄의 우리 회사 칸이 "회원" 이다\n'
            '2. 공개 공고문이 없는 줄의 우리 회사 칸이 "-" 다\n'
            '3. 우리 회사 칸 어디에도 "참여 가능", "서류 부족", "어려움", "판정 전" 꼬리표가 없다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-016'], fc=['BID-MKT-016'], screen='/search', pri='Medium'),

dict(id='IT-04-092', flow='공고 검색 판정 칸', kind='예외', title='판정 자료를 못 불러와도 공고 목록은 보인다',
     pre='회원 계정, 마감 전 공고 3건 이상, 판정 목록 조회(GET /eligibility/notices)가 오류로 답하는 상태',
     steps='1. 회원으로 /search 를 연다',
     expect='1. 마감 전 공고 줄이 3줄 이상 한글 제목과 공고번호를 갖고 있다\n'
            '2. 우리 회사 칸이 줄마다 비어 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-016'], fc=['BID-MKT-016'], screen='/search', pri='Medium'),

dict(id='IT-04-093', flow='공고 상세 판정 상자', kind='정상', title='공고 상세의 공고 요약 위에 판정과 걸린 자격이 보인다',
     pre='회원 계정, 판정이 서류 부족인 공개 공고문 1건(통과 조건 자격 A 는 있음, 서류 없음)',
     steps='1. 회원으로 그 공고 상세(/search/{공고문 id})를 연다\n2. 판정 상자의 [적격성 진단에서 답하기] 를 누른다',
     expect='1. 공고 요약 위에 판정 상자가 있고 주의 색 "서류 부족" 꼬리표가 있다\n'
            '2. 걸린 곳 한 줄이 "{A 이름}: 증빙 서류 없음" 이다\n'
            '3. 걸린 자격 목록 줄마다 자격 이름, 원문 위치, 내 답, 서류 상태가 있다\n'
            '4. 상자 끝에 "적격성 진단에 남긴 답과 붙인 서류로만 낸 판정입니다" 가 보인다\n'
            '5. 2번에서 /fit/eligibility/{같은 공고문 id} 가 열린다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-MKT-017'], fc=['BID-MKT-017'], screen='/search/{id}', pri='Medium'),

dict(id='IT-04-094', flow='공고 상세 판정 상자', kind='정상', title='참여 가능 공고의 판정 상자는 자격마다 붙인 서류를 보인다',
     pre='회원 계정, 판정이 참여 가능인 공개 공고문 1건(통과 조건 2개에 서류 D1, D2 를 붙임)',
     steps='1. 회원으로 그 공고 상세를 연다',
     expect='1. 판정 상자에 성공 색 "참여 가능" 꼬리표가 있다\n'
            '2. 상자의 자격 줄에 서류 D1, D2 이름이 각각 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-017'], fc=['BID-MKT-017'], screen='/search/{id}', pri='Medium'),

dict(id='IT-04-095', flow='공고 상세 판정 상자', kind='예외', title='판정할 수 없는 공고 상세에는 판정 상자를 두지 않는다',
     pre='회원 계정, 공고 분석에 참가 자격이 0개인 공개 공고문 1건',
     steps='1. 회원으로 그 공고 상세를 연다',
     expect='1. 공고 요약 위에 판정 꼬리표와 "적격성 진단에 남긴 답과 붙인 서류로만 낸 판정입니다" 가 없다',
     judge=['가상'],
     req=['BID-MKT-017'], fc=['BID-MKT-017'], screen='/search/{id}', pri='Medium'),

dict(id='IT-04-096', flow='공고 상세 판정 상자', kind='예외', title='판정을 불러오지 못하면 상자 자리에 알린다',
     pre='회원 계정, 판정할 수 있는 공개 공고문 1건, 공고별 진단 조회가 404 아닌 오류로 답하는 상태',
     steps='1. 회원으로 그 공고 상세를 연다',
     expect='1. 공고 요약 위 상자 자리에 "판정을 불러오지 못했습니다" 가 보인다\n'
            '2. 공고 요약은 그 아래에 그대로 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-017'], fc=['BID-MKT-017'], screen='/search/{id}', pri='Medium'),

dict(id='IT-04-097', flow='공고 상세 판정 상자', kind='예외', title='무료회원은 공고 상세 대신 회원 안내를 보고 판정 상자도 보지 못한다',
     pre='무료회원 계정, 판정할 수 있는 공개 공고문 1건',
     steps='1. 무료회원으로 /search/{그 공고문 id} 를 직접 연다',
     expect='1. 공고문은 회원 전용이라는 회원 안내와 [요금 안내] 단추, 마감 전 공고로 가는 단추가 있다\n'
            '2. 판정 꼬리표와 걸린 자격 목록이 없다\n'
            '3. 무료회원 토큰으로 공고 상세 조회(GET /opportunities/{공고문 id})를 부르면 403 MEMBERSHIP_REQUIRED 다',
     judge=['가상', '가상', '서버'],
     req=['BID-MKT-017'], fc=['BID-MKT-017'], screen='/search/{id}', pri='Medium'),

dict(id='IT-04-098', flow='인증 가이드', kind='정상', title='인증/서류 가이드에서 인증별 취득 방법, 소요 기간, 체크리스트를 본다',
     pre='비로그인',
     steps='1. 주소 /guide 를 연다\n2. 인증마다 가이드를 읽는다',
     expect='1. SAM 등록, CAGE Code, DUNS 번호 가이드가 각 인증 이름으로 있다\n'
            '2. 인증마다 취득 방법이 순서대로 적혀 있다\n'
            '3. 인증마다 소요 기간이 적혀 있다\n'
            '4. 인증마다 체크리스트 항목이 있다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-QUAL-002'], fc=['BID-QUAL-002'], screen='/guide', pri='Low',
     issue='T3-3 은 페이지 목록 한 줄뿐이고 영역 설계와 메뉴 진입이 없다. 화면 소스의 /guide 는 입찰 대행 서비스 가이드라 인증 가이드 내용이 아니다'),

]
