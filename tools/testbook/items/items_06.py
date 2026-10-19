# 영역 06 메뉴 속 화면 시험항목. 근거는 T1-1 기능요구사항 명세서 v1.19(BID-PAGE-001·002, BID-SVC-001, BID-EDU-002~004, BID-MKT-001~006, BID-SITE-001·002), T1-3 BIZ-067·068, T1-4 POL-015·039, T3-1, T3-2, T3-3, T3-4 화면 설계다.

ITEMS = [

dict(id='IT-06-001', flow='메뉴와 속페이지 틀', kind='정상', title='메뉴 띠는 7묶음이고 전체메뉴는 34항목이다',
     pre='비로그인',
     steps='1. 고객 화면 홈(/)을 연다\n2. 머리의 메뉴 띠를 본다\n3. [전체메뉴] 를 눌러 펼친다',
     expect='1. 메뉴 띠에 입찰공고, 낙찰정보, 발주분석, 맞춤 판정, 서류함, 등록·컨설팅, 고객센터 7개가 이 순서로 있다\n'
            '2. 전체메뉴에 7묶음 34항목이 있다\n'
            '3. 메뉴 띠와 전체메뉴 어디에도 재발주 예측 묶음과 입찰 교실 묶음이 없다\n'
            '4. 전체메뉴 고객센터 묶음의 항목은 공지사항, 자주 묻는 질문, 1:1 문의, 교육 자료, 용어 사전, 서식 모음, 요금 안내, 카카오톡 알림 순서다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-PAGE-001'], fc=['BID-PAGE-001'], screen='/', pri='High'),

dict(id='IT-06-002', flow='메뉴와 속페이지 틀', kind='정상', title='메뉴 띠의 묶음을 누르면 묶음 첫 항목 화면이 묶음 메뉴와 위치 줄을 갖춰 열린다',
     pre='비로그인',
     steps='1. 홈에서 메뉴 띠의 [등록·컨설팅] 을 누른다\n2. 왼쪽 묶음 메뉴와 위치 줄을 본다',
     expect='1. 주소가 묶음 첫 항목 /services/sam-registration 으로 바뀐다\n'
            '2. 왼쪽 묶음 메뉴 위에 파란 선과 묶음 이름 "등록·컨설팅" 이 있다\n'
            '3. 묶음 메뉴에 SAM 등록 대행, PIEE 등록 대행, SAM 갱신, 현장 설명회, 대금 청구, 서류 영문 번역, 팀 구성 찾기, 제안서 자문 8줄이 모두 있다\n'
            '4. SAM 등록 대행 줄이 옅은 파랑 바탕, 왼쪽 파란 줄, 오른쪽 꺾쇠로 강조되어 있다\n'
            '5. 화면이 아직 없는 제안서 자문 줄 오른쪽에 "준비 중" 작은 글자가 있다\n'
            '6. 묶음 메뉴 아래에 "상담이 필요하세요?" 상자가 있고 1:1 문의로 가는 링크가 있다\n'
            '7. 위치 줄에 집 아이콘과 홈, 등록·컨설팅, SAM 등록 대행(굵게)이 차례로 있다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-PAGE-001'], fc=['BID-PAGE-001'], screen='/services/{서비스키}', pri='High'),

dict(id='IT-06-003', flow='메뉴와 속페이지 틀', kind='정상', title='묶음 메뉴에서 다른 항목을 누르면 그 화면이 맨 위부터 열린다',
     pre='비로그인',
     steps='1. /services/sam-registration 을 열고 화면 아래 끝까지 내린다\n2. 왼쪽 묶음 메뉴에서 [현장 설명회] 를 누른다',
     expect='1. 주소가 /services/site-visit 로 바뀐다\n'
            '2. 화면 스크롤 위치가 맨 위에 있다\n'
            '3. 묶음 메뉴에서 현장 설명회 줄이 강조되고 SAM 등록 대행 줄에는 강조가 없다',
     judge=['가상', '가상', '가상'],
     req=['BID-PAGE-001'], fc=['BID-PAGE-001'], screen='/services/{서비스키}', pri='High'),

dict(id='IT-06-004', flow='메뉴와 속페이지 틀', kind='정상', title='위치 줄의 묶음 이름은 묶음 첫 항목으로, 홈은 홈으로 간다',
     pre='비로그인',
     steps='1. /services/teaming 을 연다\n2. 위치 줄의 [등록·컨설팅] 을 누른다\n3. 위치 줄의 [홈] 을 누른다',
     expect='1. 묶음 이름을 누르면 주소가 묶음 첫 항목 /services/sam-registration 으로 바뀐다\n'
            '2. 홈을 누르면 주소가 / 로 바뀐다',
     judge=['가상', '가상'],
     req=['BID-PAGE-001'], fc=['BID-PAGE-001'], screen='/services/{서비스키}', pri='High'),

dict(id='IT-06-005', flow='메뉴와 속페이지 틀', kind='정상', title='전체메뉴에서 항목을 눌러 속페이지로 들어간다',
     pre='비로그인',
     steps='1. 홈에서 [전체메뉴] 를 연다\n2. 고객센터 묶음의 [용어 사전] 을 누른다',
     expect='1. 주소가 /academy/glossary 로 바뀐다\n'
            '2. 왼쪽 묶음 메뉴 묶음 이름이 "고객센터" 이고 용어 사전 줄이 강조되어 있다\n'
            '3. 위치 줄에 홈, 고객센터, 용어 사전이 차례로 있다',
     judge=['가상', '가상', '가상'],
     req=['BID-PAGE-001'], fc=['BID-PAGE-001'], screen='/academy/glossary', pri='High'),

dict(id='IT-06-006', flow='메뉴와 속페이지 틀', kind='정상', title='교육 세 화면은 주소를 그대로 두고 고객센터 묶음에서 열린다',
     pre='비로그인',
     steps='1. 주소창에 /academy/materials 를 넣어 연다\n2. 왼쪽 묶음 메뉴와 위치 줄을 본다\n3. 위치 줄의 [고객센터] 를 누른다\n4. /academy/forms 를 열고 왼쪽 묶음 메뉴를 본다',
     expect='1. /academy/materials 에서 교육 자료 본문이 보인다\n'
            '2. 묶음 메뉴 묶음 이름이 "고객센터" 이고 교육 자료 줄이 강조되어 있다\n'
            '3. 위치 줄 묶음 이름을 누르면 주소가 고객센터 첫 항목 /notices 로 바뀐다\n'
            '4. /academy/forms 의 묶음 메뉴 묶음 이름이 "고객센터" 이고 서식 모음 줄이 강조되어 있다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-PAGE-001', 'BID-EDU-002', 'BID-EDU-004'], fc=['BID-PAGE-001', 'BID-EDU-002', 'BID-EDU-004'], screen='/academy/materials', pri='High'),

dict(id='IT-06-007', flow='메뉴와 속페이지 틀', kind='예외', title='지운 무료 입찰 교실과 재발주 예측 주소는 그 화면을 열지 않는다',
     pre='비로그인',
     steps='1. 주소창에 /academy/class 를 넣어 연다\n2. 주소창에 /recompete/calendar 를 넣어 연다',
     expect='1. /academy/class 에 무료 입찰 교실 안내와 [참가 신청] 단추가 없다\n'
            '2. /recompete/calendar 에 재발주 달력 달 칸과 목록이 없다',
     judge=['가상', '가상'],
     req=['BID-PAGE-001'], fc=['BID-PAGE-001'], screen='', pri='High',
     issue='T3-3 은 지운 주소가 없는 화면으로 간다고 적고 화면 소스는 모르는 주소를 홈(/)으로 돌린다'),

dict(id='IT-06-008', flow='메뉴와 속페이지 틀', kind='정상', title='비로그인 홈과 요금 안내에서 재발주 예측과 입찰 교실 칸이 빠졌다',
     pre='비로그인, 공개 마감 전 공고 1건 이상',
     steps='1. 홈(/)을 열고 서비스 구역, 회원 혜택 구역, 요금 구역을 본다\n2. /pricing 을 열고 요금 비교를 본다',
     expect='1. 서비스 구역 단계별 서비스 이름에 재발주 예측과 옵션 연장 알림이 없다\n'
            '2. 회원 혜택 카드는 공고문 상세 번역, 우리 회사 맞춤 판정 2장이고 재발주 예측 달력 카드가 없다\n'
            '3. 요금 구역 사진 안내는 등록 대행, 현장 지원 2칸이고 무료 입찰 교실 칸이 없다\n'
            '4. /pricing 요금 비교에 재발주 예측과 옵션 연장 알림 줄이 없다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-PAGE-001'], fc=['BID-PAGE-001'], screen='/', pri='High'),

dict(id='IT-06-009', flow='메뉴와 속페이지 틀', kind='정상', title='회원 홈 바로가기와 낙찰가 범위 아래 띠에서 재발주 화면이 빠졌다',
     pre='회원',
     steps='1. 회원 계정으로 로그인해 홈(/)을 연다\n2. 오른쪽 바로가기를 본다\n3. /awards/range 를 열고 아래 띠를 본다',
     expect='1. 바로가기에 재발주 달력이 없다\n'
            '2. 바로가기 서류함 칸이 바로가기 한 줄을 다 쓴다\n'
            '3. 낙찰가 범위 아래 띠에 [마감 전 공고] 단추만 있고 재발주 화면 링크가 없다',
     judge=['가상', '가상', '가상'],
     req=['BID-PAGE-001'], fc=['BID-PAGE-001'], screen='/', pri='High'),

dict(id='IT-06-010', flow='메뉴와 속페이지 틀', kind='정상', title='마감 전 공고도 속페이지 틀 안에서 열린다',
     pre='비로그인',
     steps='1. 메뉴 띠의 [입찰공고] 를 누른다',
     expect='1. 주소가 묶음 첫 항목 /search 로 바뀐다\n'
            '2. 왼쪽 묶음 메뉴 묶음 이름이 "입찰공고" 이고 마감 전 공고 줄이 강조되어 있다\n'
            '3. 묶음 메뉴에 마감 전 공고, 사전 공고, 변경 공고, 관심 공고, 공고문 예시 5줄이 있다\n'
            '4. 위치 줄에 홈, 입찰공고, 마감 전 공고가 차례로 있다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-PAGE-001'], fc=['BID-PAGE-001'], screen='/search', pri='High'),

dict(id='IT-06-011', flow='공지와 자주 묻는 질문', kind='정상', title='공지가 0건이면 공지사항 표와 홈 공지 탭에 같은 빈 안내가 있다',
     pre='비로그인, 화면 정의 파일의 공지 목록 0건',
     steps='1. 고객센터 묶음 메뉴에서 [공지사항] 을 누른다\n2. 홈(/) 도움 구역의 공지사항 탭을 누른다',
     expect='1. 머리에 "WJbid 서비스 소식과 점검 안내를 알려 드립니다" 가 있다\n'
            '2. 표 머리에 번호, 제목, 날짜 칸이 있다\n'
            '3. 표 안에 종 아이콘과 "아직 공지가 없습니다", "새 소식이 생기면 이곳에 올립니다" 가 있다\n'
            '4. 견본 공지 줄이 없다\n'
            '5. 홈 공지사항 탭에 "아직 공지가 없습니다" 가 있다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-PAGE-001'], fc=['BID-PAGE-001'], screen='/notices', pri='High'),

dict(id='IT-06-012', flow='공지와 자주 묻는 질문', kind='정상', title='자주 묻는 질문은 전체 탭에 첫 질문만 펼쳐 열고 분류 탭으로 거른다',
     pre='비로그인',
     steps='1. 고객센터 묶음 메뉴에서 [자주 묻는 질문] 을 누른다\n2. [가입·요금] 탭을 누른다',
     expect='1. 머리에 "찾는 답이 없으면 1:1 문의로 남겨 주세요" 가 있다\n'
            '2. 분류 탭이 전체, 가입·요금, 공고 보기, 등록·서류 순서이고 전체가 켜져 있다\n'
            '3. 첫 질문만 펼쳐져 있고 그 질문 글자가 파랑이다\n'
            '4. 펼친 질문 아래에 A 동그라미와 답이 있다\n'
            '5. 가입·요금 탭을 누르면 줄 오른쪽 분류가 가입·요금인 질문만 남는다\n'
            '6. 탭을 바꾼 뒤 펼친 질문 없이 모두 접혀 있다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-PAGE-001'], fc=['BID-PAGE-001'], screen='/faq', pri='High'),

dict(id='IT-06-013', flow='공지와 자주 묻는 질문', kind='정상', title='질문 줄을 누르면 그 질문만 펼치고 접는다',
     pre='비로그인',
     steps='1. /faq 를 연다\n2. 접혀 있는 둘째 질문 줄을 누른다\n3. 둘째 질문 줄을 다시 누른다',
     expect='1. 둘째 질문 아래에 A 동그라미와 답이 펼쳐진다\n'
            '2. 첫 질문은 펼친 채로 있다\n'
            '3. 다시 누르면 둘째 질문의 답이 접힌다',
     judge=['가상', '가상', '가상'],
     req=['BID-PAGE-001'], fc=['BID-PAGE-001'], screen='/faq', pri='High'),

dict(id='IT-06-014', flow='항목별 준비 안내', kind='정상', title='제안서 자문은 묶음 메뉴에서 항목별 준비 안내를 연다',
     pre='비로그인',
     steps='1. 등록·컨설팅 묶음 메뉴에서 [제안서 자문] 을 누른다',
     expect='1. 주소가 /coming-soon/proposal-review 로 바뀐다\n'
            '2. 머리에 등급 꼬리표, 묶음 꼬리표 "등록·컨설팅", 항목 이름 "제안서 자문" 이 있다\n'
            '3. 머리의 옅은 회색 칸에 모래시계 아이콘과 "준비하고 있습니다" 가 있다\n'
            '4. 지금 쓸 수 있는 것에 아이콘, 이름, 한 줄이 있는 화면 카드가 2~3개 있다\n'
            '5. 문의 띠에 "궁금한 점은 1:1 문의로 남겨 주세요" 와 "남겨 주시면 이메일로 답변 드립니다" 가 있다\n'
            '6. 화면 어디에도 여는 날짜가 없다\n'
            '7. 왼쪽 묶음 메뉴 묶음 이름이 "등록·컨설팅" 이고 제안서 자문 줄이 강조되어 있다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-PAGE-002'], fc=['BID-PAGE-002'], screen='/coming-soon/{항목키}', pri='High'),

dict(id='IT-06-015', flow='항목별 준비 안내', kind='정상', title='카카오톡 알림 준비 안내에서 대신 쓸 화면과 1:1 문의로 옮긴다',
     pre='비로그인',
     steps='1. 고객센터 묶음 메뉴에서 [카카오톡 알림] 을 누른다\n2. 지금 쓸 수 있는 것의 첫 카드를 누른다\n3. 뒤로 와서 문의 띠의 [1:1 문의] 를 누른다',
     expect='1. 주소가 /coming-soon/kakao 로 바뀐다\n'
            '2. 머리에 묶음 꼬리표 "고객센터" 와 항목 이름 "카카오톡 알림" 이 있다\n'
            '3. 카드를 누르면 그 카드에 적힌 이름의 화면으로 주소가 바뀐다\n'
            '4. 문의 띠의 1:1 문의를 누르면 주소가 /support 로 바뀐다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-PAGE-002'], fc=['BID-PAGE-002'], screen='/coming-soon/{항목키}', pri='High'),

dict(id='IT-06-016', flow='항목별 준비 안내', kind='예외', title='모르는 항목 키는 속페이지 틀 없는 일반 준비 중 안내를 연다',
     pre='비로그인',
     steps='1. 주소창에 /coming-soon/unknown-key 를 넣어 연다',
     expect='1. 왼쪽 묶음 메뉴와 위치 줄이 없다\n'
            '2. "준비 중입니다" 가 있다\n'
            '3. [홈], [마감 전 공고], [1:1 문의] 단추가 있다',
     judge=['가상', '가상', '가상'],
     req=['BID-PAGE-002'], fc=['BID-PAGE-002'], screen='/coming-soon/{항목키}', pri='High'),

dict(id='IT-06-017', flow='항목별 준비 안내', kind='예외', title='키 없는 /coming-soon 은 일반 준비 중 안내를 연다',
     pre='비로그인',
     steps='1. 주소창에 /coming-soon 을 넣어 연다',
     expect='1. 왼쪽 묶음 메뉴와 위치 줄이 없다\n'
            '2. "준비 중입니다" 가 있다\n'
            '3. [홈], [마감 전 공고], [1:1 문의] 단추가 있다',
     judge=['가상', '가상', '가상'],
     req=['BID-PAGE-002'], fc=['BID-PAGE-002'], screen='/coming-soon', pri='High'),

dict(id='IT-06-018', flow='서비스 안내와 상담 신청', kind='정상', title='SAM 등록 대행 안내가 정해진 영역 순서로 열린다',
     pre='비로그인',
     steps='1. /services/sam-registration 을 연다\n2. 화면을 끝까지 내려 본다',
     expect='1. 머리에 꼬리표 "건별", 묶음 꼬리표 "등록·컨설팅", 서비스 이름 "SAM 등록 대행" 과 설명 두 줄이 있다\n'
            '2. 머리에 채운 단추 [상담 신청] 과 테두리 단추 고객센터 이메일이 있다\n'
            '3. 이런 회사에 필요합니다에 아이콘 카드 3개가 있다\n'
            '4. 진행 순서에 번호 동그라미를 선으로 이은 단계가 4~5개 있고 단계마다 이름과 두 줄이 있다\n'
            '5. 진행 순서 아래 안내 상자에 회사가 직접 할 일과 WJbid 가 하는 일이 나뉘어 있다\n'
            '6. 준비할 것에 체크 목록이 있다\n'
            '7. 비용 칸에 "건별 견적" 과 "회사 상황과 필요한 일을 듣고 상담 뒤에 정합니다." 가 있다\n'
            '8. 신청 띠에 "SAM 등록 대행 상담 신청" 과 "남겨 주시면 이메일로 답변 드립니다. 메일로도 받습니다" 뒤 고객센터 이메일이 있다\n'
            '9. 함께 보는 서비스에 같은 묶음의 다른 서비스 카드 3개가 있다\n'
            '10. 화면의 채운 단추는 [상담 신청] 뿐이다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-SVC-001', 'BID-PAGE-001'], fc=['BID-SVC-001', 'BID-PAGE-001'], screen='/services/{서비스키}', pri='High'),

dict(id='IT-06-019', flow='서비스 안내와 상담 신청', kind='정상', title='SAM 등록 대행과 SAM 갱신 비용 칸에서 무료 문구가 바뀌었다',
     pre='비로그인',
     steps='1. /services/sam-registration 의 비용 칸을 본다\n2. /services/sam-renewal 의 비용 칸을 본다',
     expect='1. SAM 등록 대행 비용 칸에 "영문 입력, 본인 인증, 공증 서한까지 혼자 하기 어려운 등록을 대신 처리합니다. 비용은 상담 뒤 건별로 정합니다." 가 있다\n'
            '2. SAM 등록 대행 비용 칸에 "SAM.gov 등록 자체는 무료입니다" 가 없다\n'
            '3. SAM 갱신 비용 칸에 "영문 입력과 확인까지 혼자 하기 어려운 갱신을 대신 처리합니다. 비용은 상담 뒤 건별로 정합니다." 가 있다\n'
            '4. SAM 갱신 비용 칸에 "SAM.gov 갱신 자체도 무료입니다" 가 없다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-SVC-001'], fc=['BID-SVC-001'], screen='/services/{서비스키}', pri='High'),

dict(id='IT-06-020', flow='서비스 안내와 상담 신청', kind='정상', title='자주 묻는 질문, 교육 자료, 용어 사전에서 SAM 등록이 무료라는 문장이 빠졌다',
     pre='비로그인',
     steps='1. /faq 에서 "SAM.gov 등록에 비용이 드나요" 질문을 펼친다\n2. /academy/materials 에서 SAM.gov 등록 설명이 있는 단계를 연다\n3. /academy/glossary 에서 SAM 을 찾는다',
     expect='1. 그 질문의 답에 등록이 무료라는 문장이 없다\n'
            '2. 교육 자료 SAM.gov 등록 설명에 등록이 무료라는 문장이 없다\n'
            '3. 용어 사전 SAM 풀이에 등록이 무료라는 문장이 없다',
     judge=['가상', '가상', '가상'],
     req=['BID-SVC-001', 'BID-EDU-002', 'BID-EDU-003'], fc=['BID-SVC-001', 'BID-EDU-002', 'BID-EDU-003'], screen='/faq', pri='High'),

dict(id='IT-06-021', flow='서비스 안내와 상담 신청', kind='정상', title='SAM 등록 대행 상담 신청은 SAM_REGISTRATION 유형으로 1:1 문의를 채워 연다',
     pre='회원',
     steps='1. 회원 계정으로 로그인한다\n2. /services/sam-registration 머리의 [상담 신청] 을 누른다',
     expect='1. 주소가 /support?type=SAM_REGISTRATION&title=SAM 등록 대행 상담 신청 으로 바뀐다\n'
            '2. 문의 유형 칸에 SAM_REGISTRATION 유형이 골라져 있다\n'
            '3. 제목 칸에 "SAM 등록 대행 상담 신청" 이 들어 있다\n'
            '4. 폼 위에 초록 줄 "문의 유형과 제목을 채워 두었습니다" 가 있다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-SVC-001'], fc=['BID-SVC-001'], screen='/services/{서비스키}', pri='High'),

dict(id='IT-06-022', flow='서비스 안내와 상담 신청', kind='정상', title='SAM 갱신 신청 띠의 상담 신청도 SAM_REGISTRATION 유형이다',
     pre='회원',
     steps='1. 회원 계정으로 로그인한다\n2. /services/sam-renewal 아래 신청 띠의 [상담 신청] 을 누른다',
     expect='1. 주소가 /support?type=SAM_REGISTRATION&title=SAM 갱신 상담 신청 으로 바뀐다\n'
            '2. 제목 칸에 "SAM 갱신 상담 신청" 이 들어 있다',
     judge=['가상', '가상'],
     req=['BID-SVC-001'], fc=['BID-SVC-001'], screen='/services/{서비스키}', pri='High'),

dict(id='IT-06-023', flow='서비스 안내와 상담 신청', kind='정상', title='SAM 외 서비스의 상담 신청은 CONSULTING 유형이다',
     pre='회원',
     steps='1. 회원 계정으로 로그인한다\n2. /services/site-visit 머리의 [상담 신청] 을 누른다\n3. /services/piee-registration 머리의 [상담 신청] 을 누른다',
     expect='1. 현장 설명회는 주소가 /support?type=CONSULTING&title=현장 설명회 상담 신청 으로 바뀐다\n'
            '2. 문의 유형 칸에 CONSULTING 유형이 골라져 있다\n'
            '3. 초록 줄 "문의 유형과 제목을 채워 두었습니다" 가 있다\n'
            '4. PIEE 등록 대행은 주소가 /support?type=CONSULTING&title=PIEE 등록 대행 상담 신청 으로 바뀐다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-SVC-001'], fc=['BID-SVC-001'], screen='/services/{서비스키}', pri='High'),

dict(id='IT-06-024', flow='서비스 안내와 상담 신청', kind='정상', title='서비스 7개가 같은 틀에 내용만 바꿔 열린다',
     pre='비로그인',
     steps='1. 등록·컨설팅 묶음 메뉴에서 SAM 등록 대행부터 팀 구성 찾기까지 7개를 차례로 누른다',
     expect='1. /services/sam-registration, /services/piee-registration, /services/sam-renewal, /services/site-visit, /services/invoicing, /services/translation, /services/teaming 이 차례로 열린다\n'
            '2. 일곱 화면 모두 머리, 이런 회사에 필요합니다, 진행 순서, 준비할 것과 비용, 신청 띠, 함께 보는 서비스 순서로 놓인다\n'
            '3. 일곱 화면 모두 머리의 서비스 이름이 누른 묶음 메뉴 항목 이름과 같다\n'
            '4. 일곱 화면 모두 비용 칸에 금액 숫자와 기간 숫자가 없다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-SVC-001'], fc=['BID-SVC-001'], screen='/services/{서비스키}', pri='High'),

dict(id='IT-06-025', flow='서비스 안내와 상담 신청', kind='정상', title='함께 보는 서비스의 자세히는 그 서비스 안내로 간다',
     pre='비로그인',
     steps='1. /services/sam-registration 의 함께 보는 서비스 첫 카드 [자세히] 를 누른다',
     expect='1. 주소가 그 카드 서비스의 /services/{서비스키} 로 바뀐다\n'
            '2. 바뀐 화면 머리의 서비스 이름이 누른 카드 이름과 같다',
     judge=['가상', '가상'],
     req=['BID-SVC-001'], fc=['BID-SVC-001'], screen='/services/{서비스키}', pri='High'),

dict(id='IT-06-026', flow='서비스 안내와 상담 신청', kind='정상', title='고객센터 이메일 단추는 고객센터로 가는 메일 쓰기를 연다',
     pre='비로그인, 메일 프로그램이 있는 PC',
     steps='1. /services/sam-registration 머리의 고객센터 이메일 단추를 누른다',
     expect='1. 받는 사람 칸에 고객센터 이메일이 채워진 메일 쓰기 창이 있다',
     judge=['합동'],
     req=['BID-SVC-001'], fc=['BID-SVC-001'], screen='/services/{서비스키}', pri='High'),

dict(id='IT-06-027', flow='서비스 안내와 상담 신청', kind='정상', title='홈의 등록 대행과 현장 지원 링크는 서비스 안내로 간다',
     pre='비로그인, 공개 마감 전 공고 1건 이상',
     steps='1. 홈 첫걸음 상자의 등록 대행 링크를 누른다\n2. 홈으로 돌아와 요금 구역 사진 안내 [등록 대행] 을 누른다\n3. 홈으로 돌아와 요금 구역 사진 안내 [현장 지원] 을 누른다',
     expect='1. 첫걸음 상자 등록 대행은 /services/sam-registration 으로 간다\n'
            '2. 사진 안내 등록 대행은 /services/sam-registration 으로 간다\n'
            '3. 사진 안내 현장 지원은 /services/site-visit 로 간다',
     judge=['가상', '가상', '가상'],
     req=['BID-SVC-001'], fc=['BID-SVC-001'], screen='/', pri='High',
     issue='T3-3 로그인·회원가입 첫걸음 상자 줄은 아직 "등록 대행: 준비 중" 으로 적혀 홈 CR-133 줄과 다르다'),

dict(id='IT-06-028', flow='서비스 안내와 상담 신청', kind='예외', title='모르는 서비스 키는 일반 준비 중 안내를 연다',
     pre='비로그인',
     steps='1. 주소창에 /services/unknown 을 넣어 연다',
     expect='1. 서비스 안내 머리와 [상담 신청] 단추가 없다\n'
            '2. "준비 중입니다" 가 있다\n'
            '3. [홈], [마감 전 공고], [1:1 문의] 단추가 있다',
     judge=['가상', '가상', '가상'],
     req=['BID-SVC-001'], fc=['BID-SVC-001'], screen='/services/{서비스키}', pri='High'),

dict(id='IT-06-029', flow='교육 자료·용어·서식', kind='정상', title='교육 자료는 단계 없이 열면 1단계를 보인다',
     pre='비로그인',
     steps='1. 고객센터 묶음 메뉴에서 [교육 자료] 를 누른다',
     expect='1. 머리에 "미군 입찰을 다섯 단계로 나눠 차례대로 설명합니다" 가 있다\n'
            '2. 단계 탭 다섯 칸이 번호 동그라미와 함께 찾기, 판단, 준비, 제출, 낙찰 뒤 순서로 있다\n'
            '3. 1단계 탭이 파랑으로 채워져 있다\n'
            '4. 본문 제목이 1단계 찾기의 제목이다\n'
            '5. 오른쪽 채운 단추에 다음 단계 이름 판단이 적혀 있다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-EDU-002'], fc=['BID-EDU-002'], screen='/academy/materials', pri='Medium'),

dict(id='IT-06-030', flow='교육 자료·용어·서식', kind='정상', title='단계 탭과 앞뒤 단추로 단계를 옮기면 주소가 따라 바뀐다',
     pre='비로그인',
     steps='1. /academy/materials 를 연다\n2. 오른쪽 다음 단계 단추를 누른다\n3. 단계 탭의 4 제출 칸을 누른다\n4. 왼쪽 앞 단계 단추를 누른다',
     expect='1. 다음 단계 단추를 누르면 주소가 /academy/materials?step=2 로 바뀐다\n'
            '2. 본문 제목이 2단계 판단의 제목으로 바뀐다\n'
            '3. 단계를 바꾼 뒤 단계 탭이 화면 맨 위에 있다\n'
            '4. 탭 4 를 누르면 주소가 /academy/materials?step=4 로 바뀐다\n'
            '5. 4단계 탭이 파랑으로 채워져 있다\n'
            '6. 앞 단계 단추를 누르면 주소가 /academy/materials?step=3 으로 바뀐다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-EDU-002'], fc=['BID-EDU-002'], screen='/academy/materials', pri='Medium'),

dict(id='IT-06-031', flow='교육 자료·용어·서식', kind='정상', title='1단계 왼쪽 단추는 용어 사전, 5단계 끝 링크는 1:1 문의다',
     pre='비로그인',
     steps='1. /academy/materials?step=1 을 열고 왼쪽 테두리 단추를 누른다\n2. /academy/materials?step=5 를 열고 끝까지 내린다\n3. 끝 링크를 누른다',
     expect='1. 1단계 왼쪽 테두리 단추를 누르면 주소가 /academy/glossary 로 바뀐다\n'
            '2. 5단계에 "무료 입찰 교실" 링크가 없다\n'
            '3. 5단계 끝 링크를 누르면 주소가 /support 로 바뀐다',
     judge=['가상', '가상', '가상'],
     req=['BID-EDU-002', 'BID-PAGE-001'], fc=['BID-EDU-002', 'BID-PAGE-001'], screen='/academy/materials', pri='Medium'),

dict(id='IT-06-032', flow='교육 자료·용어·서식', kind='정상', title='본문 안내 상자의 관련 화면 링크로 옮긴다',
     pre='비로그인',
     steps='1. /academy/materials?step=1 본문 안내 상자의 관련 화면 링크 하나를 누른다',
     expect='1. 주소가 누른 링크 이름의 화면 주소로 바뀐다',
     judge=['가상'],
     req=['BID-EDU-002'], fc=['BID-EDU-002'], screen='/academy/materials', pri='Medium'),

dict(id='IT-06-033', flow='교육 자료·용어·서식', kind='예외', title='단계 값이 1~5 밖이면 1단계를 연다',
     pre='비로그인',
     steps='1. 주소창에 /academy/materials?step=9 를 넣어 연다\n2. 주소창에 /academy/materials?step=0 을 넣어 연다',
     expect='1. step=9 로 열면 1단계 탭이 파랑으로 채워져 있고 본문 제목이 1단계 찾기의 제목이다\n'
            '2. step=0 으로 열면 1단계 탭이 파랑으로 채워져 있고 본문 제목이 1단계 찾기의 제목이다',
     judge=['가상', '가상'],
     req=['BID-EDU-002'], fc=['BID-EDU-002'], screen='/academy/materials', pri='Medium'),

dict(id='IT-06-034', flow='교육 자료·용어·서식', kind='정상', title='용어 사전은 전체 용어와 건수, 색인으로 열린다',
     pre='비로그인',
     steps='1. 고객센터 묶음 메뉴에서 [용어 사전] 을 누른다\n2. 색인에서 옅게 보이는 글자 하나를 누른다',
     expect='1. 머리에 "미군 공고와 SAM.gov 에 자주 나오는 말을 한글로 풉니다" 가 있다\n'
            '2. 찾기 입력 칸과 [찾기] 단추가 있다\n'
            '3. 목록 위에 용어 건수와 "공식 정의를 근거로 풀었습니다" 가 있다\n'
            '4. 줄마다 왼쪽에 고정폭 글꼴 약어, 영문 풀이, 파랑 한글 이름이 있고 오른쪽에 풀이가 있다\n'
            '5. 색인에 전체와 A~Z 가 있고 전체가 켜져 있다\n'
            '6. 용어가 없는 색인 글자는 옅은 색이다\n'
            '7. 옅은 글자를 눌러도 목록과 건수가 바뀌지 않는다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-EDU-003'], fc=['BID-EDU-003'], screen='/academy/glossary', pri='Medium'),

dict(id='IT-06-035', flow='교육 자료·용어·서식', kind='정상', title='소문자로 찾아도 약어를 찾고 서버를 부르지 않는다',
     pre='비로그인, 브라우저 개발자 도구 네트워크 탭을 연 상태',
     steps='1. /academy/glossary 찾기 칸에 uei 를 넣고 엔터를 누른다\n2. 네트워크 탭을 본다',
     expect='1. 약어 UEI 줄이 목록에 있다\n'
            '2. 건수가 남은 줄 수와 같다\n'
            '3. 색인은 전체가 켜져 있다\n'
            '4. 찾기를 누른 뒤 서버로 간 요청이 없다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-EDU-003'], fc=['BID-EDU-003'], screen='/academy/glossary', pri='Medium'),

dict(id='IT-06-036', flow='교육 자료·용어·서식', kind='정상', title='한글 이름과 풀이 일부로도 용어를 찾는다',
     pre='비로그인',
     steps='1. /academy/glossary 찾기 칸에 견적 요청 을 넣고 [찾기] 를 누른다',
     expect='1. 약어, 영문 풀이, 한글 이름, 풀이 중 하나에 "견적 요청" 이 들어간 용어만 남는다\n'
            '2. 건수가 남은 줄 수와 같다',
     judge=['가상', '가상'],
     req=['BID-EDU-003'], fc=['BID-EDU-003'], screen='/academy/glossary', pri='Medium'),

dict(id='IT-06-037', flow='교육 자료·용어·서식', kind='정상', title='색인 글자를 누르면 그 글자로 시작하는 약어만 남고 찾는 말이 비워진다',
     pre='비로그인',
     steps='1. /academy/glossary 찾기 칸에 uei 를 넣고 찾는다\n2. 색인 [S] 를 누른다',
     expect='1. 약어가 S 로 시작하는 줄만 남는다\n'
            '2. 찾기 칸이 비어 있다',
     judge=['가상', '가상'],
     req=['BID-EDU-003'], fc=['BID-EDU-003'], screen='/academy/glossary', pri='Medium'),

dict(id='IT-06-038', flow='교육 자료·용어·서식', kind='정상', title='찾는 용어가 없으면 빈 안내와 1:1 문의 링크가 있다',
     pre='비로그인',
     steps='1. /academy/glossary 찾기 칸에 zzzz 를 넣고 찾는다\n2. 1:1 문의 링크를 누른다',
     expect='1. 목록 자리에 "찾는 용어가 없습니다" 가 있다\n'
            '2. 1:1 문의 링크를 누르면 주소가 /support 로 바뀐다',
     judge=['가상', '가상'],
     req=['BID-EDU-003'], fc=['BID-EDU-003'], screen='/academy/glossary', pri='Medium'),

dict(id='IT-06-039', flow='교육 자료·용어·서식', kind='정상', title='용어 줄의 관련 화면 링크로 옮긴다',
     pre='비로그인',
     steps='1. /academy/glossary 에서 관련 화면 링크가 있는 용어 줄의 링크를 누른다',
     expect='1. 주소가 누른 링크 이름의 화면 주소로 바뀐다',
     judge=['가상'],
     req=['BID-EDU-003'], fc=['BID-EDU-003'], screen='/academy/glossary', pri='Medium'),

dict(id='IT-06-040', flow='교육 자료·용어·서식', kind='정상', title='서식 모음은 서식을 공식 사이트 새 창으로 보내고 준비물을 보인다',
     pre='비로그인',
     steps='1. 고객센터 묶음 메뉴에서 [서식 모음] 을 누른다\n2. 표준 서식 표 첫 줄의 받는 곳 링크를 누른다\n3. 견적을 낼 때 흔히 함께 내는 것 아래 안내 상자의 링크를 누른다',
     expect='1. 머리에 "미군 공고에 답할 때 자주 쓰는 미 연방 표준 서식과 준비물입니다" 가 있다\n'
            '2. 표준 서식 제목 옆에 "원본은 미 연방 조달청(GSA) 서식 자료실에서 받습니다" 가 있다\n'
            '3. 표 칸이 서식 번호(고정폭), 이름(한글과 영문), 언제 쓰나요, 받는 곳 순서다\n'
            '4. 받는 곳 링크는 공식 사이트를 새 창으로 연다\n'
            '5. 화면 어디에도 이 서비스 주소에서 서식 파일을 내려받는 링크가 없다\n'
            '6. 견적을 낼 때 흔히 함께 내는 것에 체크 목록과 공고마다 다르니 제출 안내를 먼저 보라는 안내 상자가 있다\n'
            '7. 안내 상자 링크를 누르면 주소가 /academy/materials?step=4 로 바뀐다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-EDU-004'], fc=['BID-EDU-004'], screen='/academy/forms', pri='Medium'),

dict(id='IT-06-041', flow='사전 공고와 변경 공고', kind='정상', title='비로그인으로 사전 공고를 열면 자료 기준과 목록이 조회 결과대로 나온다',
     pre='비로그인, 모은 사전 공고와 사전 수요 조사 각 1건 이상, 한글 제목 있는 공고와 없는 공고가 섞여 있음',
     steps='1. 입찰공고 묶음 메뉴에서 [사전 공고] 를 누른다\n2. 같은 때 GET /opportunities/collected?group=PRE 응답을 받아 둔다',
     expect='1. 로그인 화면을 거치지 않고 /bids/pre-solicitation 이 열린다\n'
            '2. 제목 줄에 "사전 공고" 와 "본 공고보다 먼저 올라온 공고입니다. 미리 준비할 것을 챙기세요" 가 있다\n'
            '3. 옅은 회색 자료 기준 칸에 정보 아이콘과 게시일 기간, 사전 공고 n건, 사전 수요 조사 n건이 있다\n'
            '4. 자료 기준의 기간과 건수가 서버 응답 값과 같다\n'
            '5. 탭이 전체, 사전 공고, 사전 수요 조사 순서로 건수와 함께 있고 전체가 켜져 있다\n'
            '6. 답변 기한 남은 것만 거르기가 꺼져 있다\n'
            '7. 목록이 게시일 최신순이고 서버 응답 1쪽 줄과 같다\n'
            '8. 한글 제목이 있는 줄은 한글 제목이 굵게, 그 아래 원문 제목이 작게 있다\n'
            '9. 한글 제목이 없는 줄은 원문 제목만 굵게 있다\n'
            '10. 공고번호는 IBM Plex Mono 글꼴이다\n'
            '11. 목록은 20줄씩 쪽으로 나뉜다\n'
            '12. 무엇을 하면 되나에 사전 공고와 사전 수요 조사의 뜻, 사전 수요 조사에 회사 소개로 답할 수 있다는 것이 세 줄로 있다\n'
            '13. 아래 띠의 채운 단추 [무료로 시작] 을 누르면 회원가입 화면으로 간다',
     judge=['가상', '가상', '가상', '서버', '가상', '가상', '서버', '가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-MKT-001'], fc=['BID-MKT-001'], screen='/bids/pre-solicitation', pri='Medium'),

dict(id='IT-06-042', flow='사전 공고와 변경 공고', kind='정상', title='사전 공고 줄의 답변 기한과 본 공고 나옴 꼬리표가 공고 상태를 따른다',
     pre='비로그인, 답변 기한이 3일 남은 공고, 20일 남은 공고, 이미 지난 공고, 같은 공고번호로 본 공고가 올라온 사전 공고가 있음',
     steps='1. /bids/pre-solicitation 을 연다\n2. 각 공고 줄을 본다',
     expect='1. 줄마다 유형 꼬리표, 제목, 공고번호, 게시일, 답변 기한이 있다\n'
            '2. 3일 남은 공고의 답변 기한은 빨강 "D-3" 이다\n'
            '3. 20일 남은 공고의 답변 기한 "D-20" 은 빨강이 아니다\n'
            '4. 답변 기한이 지난 공고는 회색 "마감" 이다\n'
            '5. 같은 공고번호로 본 공고가 올라온 줄에 "본 공고 나옴" 꼬리표가 있다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-MKT-001'], fc=['BID-MKT-001'], screen='/bids/pre-solicitation', pri='Medium'),

dict(id='IT-06-043', flow='사전 공고와 변경 공고', kind='정상', title='유형 탭과 쪽이 주소에 남아 새로고침해도 그대로다',
     pre='비로그인, 사전 수요 조사 21건 이상',
     steps='1. /bids/pre-solicitation 에서 [사전 수요 조사] 탭을 누른다\n2. 2쪽으로 넘긴다\n3. 새로고침한다\n4. [사전 공고] 탭을 누른다',
     expect='1. 사전 수요 조사 탭을 누르면 유형 꼬리표가 사전 수요 조사인 줄만 남는다\n'
            '2. 주소에 type 값이 남는다\n'
            '3. 새로고침 뒤에도 사전 수요 조사 탭과 2쪽이 그대로다\n'
            '4. 사전 공고 탭을 누르면 1쪽부터다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-MKT-001'], fc=['BID-MKT-001'], screen='/bids/pre-solicitation', pri='Medium'),

dict(id='IT-06-044', flow='사전 공고와 변경 공고', kind='정상', title='답변 기한 남은 것만 거르기는 지난 공고를 빼고 탭 건수도 따른다',
     pre='비로그인, 답변 기한이 지난 사전 공고와 남은 사전 공고가 섞여 있음',
     steps='1. /bids/pre-solicitation 에서 [답변 기한 남은 것만] 을 켠다\n2. 탭 건수를 본다\n3. 새로고침한다',
     expect='1. 목록에 "마감" 인 줄이 없다\n'
            '2. 주소에 open=1 이 남는다\n'
            '3. 탭 건수가 거르기를 켜기 전보다 지난 공고 수만큼 줄어 있다\n'
            '4. 새로고침 뒤에도 거르기가 켜져 있다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-MKT-001'], fc=['BID-MKT-001'], screen='/bids/pre-solicitation', pri='Medium'),

dict(id='IT-06-045', flow='사전 공고와 변경 공고', kind='정상', title='거른 결과가 0건이면 빈 상태와 거르기 풀기가 있다',
     pre='비로그인, 답변 기한이 남은 사전 수요 조사 0건',
     steps='1. /bids/pre-solicitation 에서 [사전 수요 조사] 탭을 누르고 [답변 기한 남은 것만] 을 켠다\n2. [거르기 풀기] 를 누른다',
     expect='1. 목록 자리에 아이콘과 "조건에 맞는 공고가 없습니다", [거르기 풀기] 가 있다\n'
            '2. 거르기 풀기를 누르면 답변 기한 남은 것만이 꺼진다',
     judge=['가상', '가상'],
     req=['BID-MKT-001'], fc=['BID-MKT-001'], screen='/bids/pre-solicitation', pri='Medium'),

dict(id='IT-06-046', flow='사전 공고와 변경 공고', kind='정상', title='본 공고 나옴을 누르면 그 공고번호만 거른 변경 공고 화면이 열린다',
     pre='비로그인, "본 공고 나옴" 꼬리표가 붙은 사전 공고 1건',
     steps='1. /bids/pre-solicitation 에서 "본 공고 나옴" 꼬리표를 누른다\n2. [모두 보기] 를 누른다',
     expect='1. 주소가 /bids/amendments?sol={그 공고번호} 로 바뀐다\n'
            '2. 변경 공고 목록에 그 공고번호 묶음 하나만 있다\n'
            '3. 모두 보기를 누르면 주소에서 sol 이 빠진다\n'
            '4. 모두 보기 뒤 다른 공고번호 묶음도 목록에 있다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-MKT-001', 'BID-MKT-002'], fc=['BID-MKT-001', 'BID-MKT-002'], screen='/bids/pre-solicitation', pri='Medium',
     issue='T3-3 은 그 공고번호만 거른 변경 공고(?sol=)로 가고 T3-4 시나리오 2-4 는 변경 공고 화면(전체)으로 간다고 적는다'),

dict(id='IT-06-047', flow='사전 공고와 변경 공고', kind='정상', title='공개 공고문이 있는 사전 공고 줄은 로그인을 거쳐 공고 상세로 간다',
     pre='비로그인에서 시작, 회원 계정 1개, 공개 한글 공고문이 있는 사전 공고 1건',
     steps='1. /bids/pre-solicitation 에서 공개 공고문이 있는 줄을 누른다\n2. 회원 계정으로 로그인한다\n3. 브라우저 뒤로 가기로 사전 공고로 돌아온다',
     expect='1. 줄을 누르면 로그인 화면으로 간다\n'
            '2. 로그인 뒤 그 공고의 공고 상세 /search/{공고문 id} 가 열린다\n'
            '3. 뒤로 오면 누르기 전의 탭, 거르기, 쪽이 그대로다\n'
            '4. 로그인한 뒤 아래 띠의 채운 단추는 [마감 전 공고] 이고 누르면 /search 로 간다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-MKT-001'], fc=['BID-MKT-001'], screen='/bids/pre-solicitation', pri='Medium'),

dict(id='IT-06-048', flow='사전 공고와 변경 공고', kind='정상', title='공개 공고문이 없는 사전 공고 줄은 원본 공고 상세로 간다',
     pre='회원, 공개 공고문이 없는 사전 공고 1건',
     steps='1. 회원 계정으로 로그인한다\n2. /bids/pre-solicitation 에서 공개 공고문이 없는 줄을 누른다\n3. 원본 공고 상세 머리의 [SAM.gov 원문 보기] 를 누른다',
     expect='1. 그 공고의 원본 공고 상세 /search/origin/{원본 공고 id} 가 열린다\n'
            '2. 줄을 누를 때 SAM.gov 원문 새 창이 바로 열리지 않는다\n'
            '3. SAM.gov 원문 보기를 누르면 그 공고의 SAM.gov 원문이 새 창에서 열린다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-001'], fc=['BID-MKT-001'], screen='/bids/pre-solicitation', pri='Medium'),

dict(id='IT-06-049', flow='사전 공고와 변경 공고', kind='정상', title='무료회원은 공개 공고문이 있는 줄을 눌러도 원본 공고 상세에 머문다',
     pre='무료회원, 공개 공고문이 있는 사전 공고 1건',
     steps='1. 무료회원 계정으로 로그인한다\n2. /bids/pre-solicitation 에서 공개 공고문이 있는 줄을 누른다',
     expect='1. 그 공고의 원본 공고 상세 /search/origin/{원본 공고 id} 에 머문다\n'
            '2. 본문 위에 "AI 공고문은 회원 전용입니다" 와 [요금 안내] 단추가 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-001', 'BID-MKT-002', 'BID-MKT-004', 'BID-MKT-005', 'BID-MKT-006'], fc=['BID-MKT-001', 'BID-MKT-002', 'BID-MKT-004', 'BID-MKT-005', 'BID-MKT-006'], screen='/bids/pre-solicitation', pri='Medium'),

dict(id='IT-06-050', flow='사전 공고와 변경 공고', kind='예외', title='사전 공고 목록을 못 받으면 다시 시도를 보인다',
     pre='비로그인, GET /opportunities/collected?group=PRE 가 실패하게 막은 상태',
     steps='1. /bids/pre-solicitation 을 연다\n2. 막은 것을 풀고 [다시 시도] 를 누른다',
     expect='1. 목록 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 있다\n'
            '2. 다시 시도를 누르면 목록 자리에 사전 공고 줄이 들어온다',
     judge=['가상', '가상'],
     req=['BID-MKT-001'], fc=['BID-MKT-001'], screen='/bids/pre-solicitation', pri='Medium'),

dict(id='IT-06-051', flow='사전 공고와 변경 공고', kind='정상', title='변경 공고는 진행 중 묶음을 마지막 게시일 최신순으로 연다',
     pre='비로그인, 같은 공고번호로 2번 이상 올라온 공고 묶음 여럿, 그중 낙찰 공지가 함께 있는 묶음 1개',
     steps='1. 입찰공고 묶음 메뉴에서 [변경 공고] 를 누른다\n2. 같은 때 GET /opportunities/amendments 응답을 받아 둔다',
     expect='1. 로그인 화면을 거치지 않고 /bids/amendments 가 열린다\n'
            '2. 제목 줄에 "변경 공고" 와 "같은 공고번호로 다시 올라온 공고입니다. 마감일과 조건이 바뀌었는지 확인하세요" 가 있다\n'
            '3. 자료 기준에 게시일 기간, n건, "바뀐 곳을 나란히 비교하는 기능은 준비하고 있습니다" 가 있다\n'
            '4. 거르기가 진행 중(마지막 판 마감 전)으로 켜져 있다\n'
            '5. 묶음이 마지막 게시일 최신순이다\n'
            '6. 묶음마다 공고번호, 마지막 판 제목, 올라온 횟수 "n번", 처음과 마지막 게시일, 마지막 판 마감일이 있다\n'
            '7. 첫 묶음이 펼친 채로 열려 있다\n'
            '8. 묶음 수와 묶음마다 올라온 횟수가 서버 응답 값과 같다\n'
            '9. 낙찰 공지는 올라온 횟수에 들지 않는다\n'
            '10. 한글 제목이 없는 묶음은 원문 제목만 굵게 있다\n'
            '11. 아래 띠의 채운 단추는 [무료로 시작] 이다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상', '서버', '서버', '가상', '가상'],
     req=['BID-MKT-002'], fc=['BID-MKT-002'], screen='/bids/amendments', pri='Medium'),

dict(id='IT-06-052', flow='사전 공고와 변경 공고', kind='정상', title='마감일이 바뀐 묶음과 낙찰 공지가 있는 묶음에 꼬리표가 붙는다',
     pre='비로그인, 판마다 마감일이 다른 묶음 1개, 같은 공고번호의 낙찰 공지가 있는 묶음 1개',
     steps='1. /bids/amendments?view=all 을 연다\n2. 판마다 마감일이 다른 묶음을 펼친다',
     expect='1. 판마다 마감일이 다른 묶음에 "마감일 바뀜" 꼬리표가 있다\n'
            '2. 낙찰 공지가 있는 묶음에 "낙찰" 꼬리표가 있다\n'
            '3. 펼친 판 목록에서 앞 판과 마감일이 다른 판의 마감일이 굵은 글자다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-002'], fc=['BID-MKT-002'], screen='/bids/amendments', pri='Medium'),

dict(id='IT-06-053', flow='사전 공고와 변경 공고', kind='정상', title='전체 거르기는 마감 지난 묶음까지 보이고 주소에 남는다',
     pre='비로그인, 마지막 판 마감이 지난 묶음 1개 이상',
     steps='1. /bids/amendments 에서 [전체] 를 누른다\n2. 새로고침한다',
     expect='1. 주소에 view=all 이 남는다\n'
            '2. 마지막 판 마감이 지난 묶음도 목록에 있다\n'
            '3. 새로고침 뒤에도 전체가 켜져 있다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-002'], fc=['BID-MKT-002'], screen='/bids/amendments', pri='Medium'),

dict(id='IT-06-054', flow='사전 공고와 변경 공고', kind='정상', title='묶음 줄을 누르면 판 목록을 펼치고 다시 누르면 접는다',
     pre='비로그인, 진행 중 묶음 2개 이상',
     steps='1. /bids/amendments 에서 접힌 둘째 묶음 줄을 누른다\n2. 둘째 묶음 줄을 다시 누른다',
     expect='1. 둘째 묶음 아래에 판 목록이 게시일 순으로 펼쳐진다\n'
            '2. 판마다 게시일, 유형, 마감일이 있다\n'
            '3. 첫 묶음은 펼친 채로 있다\n'
            '4. 다시 누르면 둘째 묶음의 판 목록이 접힌다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-MKT-002'], fc=['BID-MKT-002'], screen='/bids/amendments', pri='Medium'),

dict(id='IT-06-055', flow='사전 공고와 변경 공고', kind='정상', title='진행 중 묶음이 0건이면 빈 상태와 전체 보기가 있다',
     pre='비로그인, 마지막 판 마감 전인 묶음 0건',
     steps='1. /bids/amendments 를 연다\n2. [전체 보기] 를 누른다',
     expect='1. 목록 자리에 "조건에 맞는 공고가 없습니다" 와 [전체 보기] 가 있다\n'
            '2. 전체 보기를 누르면 주소에 view=all 이 남는다',
     judge=['가상', '가상'],
     req=['BID-MKT-002'], fc=['BID-MKT-002'], screen='/bids/amendments', pri='Medium'),

dict(id='IT-06-056', flow='사전 공고와 변경 공고', kind='정상', title='공개 공고문이 있는 판 줄은 로그인을 거쳐 공고 상세로 간다',
     pre='비로그인에서 시작, 회원 계정 1개, 공개 한글 공고문이 있는 판 1건',
     steps='1. /bids/amendments 에서 그 판이 든 묶음을 펼치고 판 줄을 누른다\n2. 회원 계정으로 로그인한다\n3. 브라우저 뒤로 가기로 변경 공고로 돌아온다',
     expect='1. 판 줄을 누르면 로그인 화면으로 간다\n'
            '2. 로그인 뒤 그 판의 공고 상세 /search/{공고문 id} 가 열린다\n'
            '3. 뒤로 오면 누르기 전의 거르기와 쪽이 그대로다\n'
            '4. 로그인한 뒤 아래 띠의 채운 단추는 [마감 전 공고] 다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-MKT-002'], fc=['BID-MKT-002'], screen='/bids/amendments', pri='Medium'),

dict(id='IT-06-057', flow='사전 공고와 변경 공고', kind='정상', title='공개 공고문이 없는 판 줄은 원본 공고 상세로 간다',
     pre='회원, 공개 공고문이 없는 판 1건',
     steps='1. 회원 계정으로 로그인한다\n2. /bids/amendments 에서 그 판이 든 묶음을 펼치고 판 줄을 누른다\n3. [SAM.gov 원문 보기] 를 누른다',
     expect='1. 그 판의 원본 공고 상세 /search/origin/{원본 공고 id} 가 열린다\n'
            '2. SAM.gov 원문 보기를 누르면 그 판의 SAM.gov 원문이 새 창에서 열린다',
     judge=['가상', '가상'],
     req=['BID-MKT-002'], fc=['BID-MKT-002'], screen='/bids/amendments', pri='Medium'),

dict(id='IT-06-058', flow='사전 공고와 변경 공고', kind='예외', title='변경 공고 목록을 못 받으면 다시 시도를 보인다',
     pre='비로그인, GET /opportunities/amendments 가 실패하게 막은 상태',
     steps='1. /bids/amendments 를 연다\n2. 막은 것을 풀고 [다시 시도] 를 누른다',
     expect='1. 목록 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 있다\n'
            '2. 다시 시도를 누르면 목록 자리에 변경 공고 묶음이 들어온다',
     judge=['가상', '가상'],
     req=['BID-MKT-002'], fc=['BID-MKT-002'], screen='/bids/amendments', pri='Medium'),

dict(id='IT-06-059', flow='공고문 예시', kind='정상', title='공고문 예시는 설정한 마감 공고 한 건의 한글 공고문을 처음부터 끝까지 보인다',
     pre='비로그인, system_settings NOTICE_SAMPLE_ID 에 마감된 공개 공고문 id 가 들어 있음',
     steps='1. 입찰공고 묶음 메뉴에서 [공고문 예시] 를 누른다\n2. 같은 때 GET /opportunities/sample 응답을 받아 둔다\n3. 같은 공고문의 공고 상세를 관리자 계정으로 열어 둔다',
     expect='1. 로그인 화면을 거치지 않고 /bids/sample 이 열린다\n'
            '2. 제목 줄에 "공고문 예시", "마감된 실제 공고 한 건을 WJbid 가 한글로 정리한 모습입니다", 꼬리표 "마감된 공고" 가 있다\n'
            '3. 응답의 공고문 id 가 NOTICE_SAMPLE_ID 값과 같다\n'
            '4. 공고문 머리에 한글 제목과 원문 제목이 있다\n'
            '5. 공고번호, 발주처, 처음 게시, 제안 마감 네 칸이 있다\n'
            '6. 공고 요약, 업무 범위, 계약 기간과 주요 일정, 참여 자격, 필요 서류, 평가 방식, 예산과 수량, 꼭 볼 것이 같은 공고문의 공고 상세와 같은 내용이다\n'
            '7. 내용이 비어 있는 분석 칸은 그 묶음이 제목째 빠져 있다\n'
            '8. 아래 띠에 "마감 전 공고도 이렇게 정리해 드립니다" 와 [무료로 시작] 이 있다',
     judge=['가상', '가상', '서버', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-MKT-003'], fc=['BID-MKT-003'], screen='/bids/sample', pri='Medium'),

dict(id='IT-06-060', flow='공고문 예시', kind='정상', title='공고문 예시에서 SAM.gov 원문을 새 창으로 열고 로그인하면 아래 띠가 바뀐다',
     pre='회원, NOTICE_SAMPLE_ID 에 마감된 공개 공고문 id 가 들어 있음',
     steps='1. 회원 계정으로 로그인한다\n2. /bids/sample 에서 SAM.gov 원문 단추를 누른다\n3. 아래 띠의 채운 단추를 누른다',
     expect='1. 그 공고의 SAM.gov 원문이 새 창에서 열린다\n'
            '2. 아래 띠의 채운 단추가 [마감 전 공고] 이고 누르면 /search 로 간다',
     judge=['가상', '가상'],
     req=['BID-MKT-003'], fc=['BID-MKT-003'], screen='/bids/sample', pri='Medium'),

dict(id='IT-06-061', flow='공고문 예시', kind='예외', title='NOTICE_SAMPLE_ID 가 비었으면 예시 없음 안내를 보인다',
     pre='비로그인, NOTICE_SAMPLE_ID 행 값을 빈 값으로 바꾼 상태',
     steps='1. GET /opportunities/sample 을 부른다\n2. /bids/sample 을 연다\n3. [마감 전 공고] 를 누른다',
     expect='1. 응답이 204 다\n'
            '2. 공고문 자리에 "예시를 준비하고 있습니다" 가 있다\n'
            '3. 마감 전 공고를 누르면 /search 로 간다',
     judge=['서버', '가상', '가상'],
     req=['BID-MKT-003'], fc=['BID-MKT-003'], screen='/bids/sample', pri='Medium'),

dict(id='IT-06-062', flow='공고문 예시', kind='예외', title='NOTICE_SAMPLE_ID 가 UUID 가 아니면 예시 없음 안내를 보인다',
     pre='비로그인, NOTICE_SAMPLE_ID 행 값을 abc 로 바꾼 상태',
     steps='1. GET /opportunities/sample 을 부른다\n2. /bids/sample 을 연다',
     expect='1. 응답이 204 다\n'
            '2. 공고문 자리에 "예시를 준비하고 있습니다" 가 있다',
     judge=['서버', '가상'],
     req=['BID-MKT-003'], fc=['BID-MKT-003'], screen='/bids/sample', pri='Medium'),

dict(id='IT-06-063', flow='공고문 예시', kind='예외', title='NOTICE_SAMPLE_ID 의 공고문이 없으면 예시 없음 안내를 보인다',
     pre='비로그인, NOTICE_SAMPLE_ID 행 값을 어느 공고문에도 없는 UUID 로 바꾼 상태',
     steps='1. GET /opportunities/sample 을 부른다\n2. /bids/sample 을 연다',
     expect='1. 응답이 204 다\n'
            '2. 공고문 자리에 "예시를 준비하고 있습니다" 가 있다',
     judge=['서버', '가상'],
     req=['BID-MKT-003'], fc=['BID-MKT-003'], screen='/bids/sample', pri='Medium'),

dict(id='IT-06-064', flow='공고문 예시', kind='예외', title='NOTICE_SAMPLE_ID 의 공고문이 숨김이면 예시 없음 안내를 보인다',
     pre='비로그인, 관리자가 NOTICE_SAMPLE_ID 의 공고문을 숨김으로 바꾼 상태',
     steps='1. GET /opportunities/sample 을 부른다\n2. /bids/sample 을 연다',
     expect='1. 응답이 204 다\n'
            '2. 공고문 자리에 "예시를 준비하고 있습니다" 가 있다',
     judge=['서버', '가상'],
     req=['BID-MKT-003'], fc=['BID-MKT-003'], screen='/bids/sample', pri='Medium'),

dict(id='IT-06-065', flow='공고문 예시', kind='예외', title='공고문 예시를 204 아닌 오류로 못 받으면 다시 시도를 보인다',
     pre='비로그인, GET /opportunities/sample 이 500 을 주게 막은 상태',
     steps='1. /bids/sample 을 연다',
     expect='1. 공고문 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 있다\n'
            '2. 공고문 자리에 "예시를 준비하고 있습니다" 가 없다',
     judge=['가상', '가상'],
     req=['BID-MKT-003'], fc=['BID-MKT-003'], screen='/bids/sample', pri='Medium'),

dict(id='IT-06-066', flow='최근 낙찰', kind='정상', title='최근 낙찰은 숫자 세 칸과 낙찰 공지 목록을 조회 결과대로 보인다',
     pre='비로그인, 낙찰 공지 여러 건, 그중 금액 없는 공지 1건 이상, 원래 공고가 없는 공지 1건 이상',
     steps='1. 낙찰정보 묶음 메뉴에서 [최근 낙찰] 을 누른다\n2. 같은 때 GET /opportunities/awards 응답을 받아 둔다',
     expect='1. 로그인 화면을 거치지 않고 /awards/recent 가 열린다\n'
            '2. 제목 줄에 "최근 낙찰" 과 "미군이 낙찰을 알린 계약입니다" 가 있다\n'
            '3. 자료 기준에 공지일 기간, 낙찰 공지 n건, 금액 공개 n건이 있다\n'
            '4. 숫자 세 칸의 낙찰 공지 수가 응답 summary.total 과 같다\n'
            '5. 숫자 세 칸의 합계가 응답 summary.amountSum 과 같다\n'
            '6. 합계는 금액이 있는 공지의 낙찰 금액만 더한 값이다\n'
            '7. 숫자 세 칸의 가장 큰 금액이 응답 summary.amountMax 와 같다\n'
            '8. 목록이 전체 달, 공지일 최신순 1쪽이고 20줄씩 나뉜다\n'
            '9. 줄마다 공지일, 제목, 공고번호, 낙찰 금액, 원래 공고 칸이 있다\n'
            '10. 금액 없는 공지의 낙찰 금액 칸은 회색 "공개 안 됨" 이다\n'
            '11. 원래 공고가 없는 공지의 원래 공고 칸은 "-" 다\n'
            '12. 한글 제목이 없는 줄은 원문 제목만 굵게 있다\n'
            '13. 아래 띠의 채운 단추는 [무료로 시작] 이다',
     judge=['가상', '가상', '가상', '서버', '서버', '서버', '서버', '가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-MKT-004'], fc=['BID-MKT-004'], screen='/awards/recent', pri='Medium'),

dict(id='IT-06-067', flow='최근 낙찰', kind='정상', title='업체 이름 있는 공지가 있으면 자료 기준 건수와 업체 꼬리표가 붙는다',
     pre='비로그인, 낙찰 업체 이름이 저장된 낙찰 공지 1건 이상',
     steps='1. /awards/recent 를 연다\n2. 같은 때 GET /opportunities/awards 응답 summary.awardeeKnownCount 를 본다',
     expect='1. 자료 기준에 "업체 이름 있는 공지 n건" 이 있다\n'
            '2. 그 n 이 응답 awardeeKnownCount 와 같다\n'
            '3. 업체 이름이 있는 공지 줄의 제목 아래에 "낙찰 업체" 와 그 업체 이름 꼬리표가 있다\n'
            '4. 업체 이름이 없는 공지 줄에는 낙찰 업체 꼬리표가 없다',
     judge=['가상', '서버', '가상', '가상'],
     req=['BID-MKT-004'], fc=['BID-MKT-004'], screen='/awards/recent', pri='Medium'),

dict(id='IT-06-068', flow='최근 낙찰', kind='정상', title='업체 이름 있는 공지가 0건이면 모으지 않는다고 밝힌다',
     pre='비로그인, 낙찰 업체 이름이 저장된 낙찰 공지 0건',
     steps='1. /awards/recent 를 연다',
     expect='1. 자료 기준에 "낙찰 업체 이름은 아직 모으지 않습니다" 가 있다\n'
            '2. 어느 줄에도 낙찰 업체 꼬리표가 없다',
     judge=['가상', '가상'],
     req=['BID-MKT-004'], fc=['BID-MKT-004'], screen='/awards/recent', pri='Medium'),

dict(id='IT-06-069', flow='최근 낙찰', kind='정상', title='달을 고르면 목록과 숫자 세 칸이 그 달 기준으로 바뀐다',
     pre='비로그인, 낙찰 공지가 있는 달 2개 이상',
     steps='1. /awards/recent 에서 달 거르기로 한 달을 고른다\n2. 같은 달로 GET /opportunities/awards?month= 응답을 받아 둔다\n3. 새로고침한다',
     expect='1. 목록에 공지일이 고른 달인 공지만 남는다\n'
            '2. 숫자 세 칸이 그 달 응답 summary 값과 같다\n'
            '3. 주소에 month 값이 남는다\n'
            '4. 새로고침 뒤에도 고른 달이 그대로다',
     judge=['가상', '서버', '가상', '가상'],
     req=['BID-MKT-004'], fc=['BID-MKT-004'], screen='/awards/recent', pri='Medium'),

dict(id='IT-06-070', flow='최근 낙찰', kind='정상', title='금액순은 금액 큰 순이고 금액 없는 공지가 맨 뒤다',
     pre='비로그인, 금액이 있는 공지와 없는 공지가 섞여 있음',
     steps='1. /awards/recent 에서 [금액순] 을 고른다\n2. 마지막 쪽까지 넘긴다',
     expect='1. 목록이 낙찰 금액 큰 순이다\n'
            '2. "공개 안 됨" 공지는 금액 있는 공지 뒤 맨 끝에 모여 있다\n'
            '3. 주소에 sort 값이 남는다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-004'], fc=['BID-MKT-004'], screen='/awards/recent', pri='Medium'),

dict(id='IT-06-071', flow='최근 낙찰', kind='정상', title='공개 공고문이 있는 낙찰 공지 줄은 로그인을 거쳐 공고 상세로 간다',
     pre='비로그인에서 시작, 회원 계정 1개, 공개 한글 공고문이 있는 낙찰 공지 1건',
     steps='1. /awards/recent 에서 그 공지 줄을 누른다\n2. 회원 계정으로 로그인한다\n3. 브라우저 뒤로 가기로 최근 낙찰로 돌아온다',
     expect='1. 줄을 누르면 로그인 화면으로 간다\n'
            '2. 로그인 뒤 그 공지의 공고 상세 /search/{공고문 id} 가 열린다\n'
            '3. 뒤로 오면 누르기 전의 달, 정렬, 쪽이 그대로다\n'
            '4. 로그인한 뒤 아래 띠의 채운 단추는 [마감 전 공고] 다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-MKT-004'], fc=['BID-MKT-004'], screen='/awards/recent', pri='Medium',
     issue='T3-3 최근 낙찰 목록 줄은 "줄: 원문" 으로 남아 있고 공통 절(CR-152)은 우리 공고 상세로 간다고 적는다'),

dict(id='IT-06-072', flow='최근 낙찰', kind='정상', title='공개 공고문이 없는 낙찰 공지 줄은 원본 공고 상세로 간다',
     pre='회원, 공개 공고문이 없는 낙찰 공지 1건',
     steps='1. 회원 계정으로 로그인한다\n2. /awards/recent 에서 그 공지 줄을 누른다\n3. [SAM.gov 원문 보기] 를 누른다',
     expect='1. 그 공지의 원본 공고 상세 /search/origin/{원본 공고 id} 가 열린다\n'
            '2. SAM.gov 원문 보기를 누르면 그 공지의 SAM.gov 원문이 새 창에서 열린다',
     judge=['가상', '가상'],
     req=['BID-MKT-004'], fc=['BID-MKT-004'], screen='/awards/recent', pri='Medium'),

dict(id='IT-06-073', flow='최근 낙찰', kind='정상', title='원래 공고를 누르면 원래 공고의 공고 상세나 원본 공고 상세로 간다',
     pre='회원, 원래 공고에 공개 공고문이 있는 낙찰 공지 1건, 원래 공고에 공개 공고문이 없는 낙찰 공지 1건',
     steps='1. 회원 계정으로 로그인한다\n2. /awards/recent 에서 첫 공지의 "원래 공고" 를 누른다\n3. 뒤로 와서 둘째 공지의 "원래 공고" 를 누른다',
     expect='1. 원래 공고에 공개 공고문이 있으면 그 원래 공고의 /search/{공고문 id} 가 열린다\n'
            '2. 원래 공고에 공개 공고문이 없으면 그 원래 공고의 /search/origin/{원본 공고 id} 가 열린다',
     judge=['가상', '가상'],
     req=['BID-MKT-004'], fc=['BID-MKT-004'], screen='/awards/recent', pri='Medium'),

dict(id='IT-06-074', flow='최근 낙찰', kind='예외', title='낙찰 공지를 못 받으면 숫자 세 칸은 - 이고 목록은 다시 시도다',
     pre='비로그인, GET /opportunities/awards 가 실패하게 막은 상태',
     steps='1. /awards/recent 를 연다',
     expect='1. 숫자 세 칸이 모두 "-" 다\n'
            '2. 목록 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-004'], fc=['BID-MKT-004'], screen='/awards/recent', pri='Medium'),

dict(id='IT-06-075', flow='월별 동향과 업종별 발주', kind='정상', title='월별 동향은 첫 게시 달부터 이번 달까지 유형 넷을 쌓은 막대와 표로 보인다',
     pre='비로그인, 모은 공고 2026-02 부터 있음',
     steps='1. 발주분석 묶음 메뉴에서 [월별 동향] 을 누른다\n2. 같은 때 GET /opportunities/stats/monthly 응답을 받아 둔다',
     expect='1. 로그인 화면을 거치지 않고 /analysis/monthly 가 열린다\n'
            '2. 제목 줄에 "월별 동향" 과 "달마다 공고가 몇 건 올라왔는지 유형별로 보여 드립니다" 가 있다\n'
            '3. 자료 기준에 게시일 기간, n건, 이번 달은 오늘까지라는 말이 있다\n'
            '4. 막대가 첫 게시 달부터 이번 달까지 달마다 하나씩 있다\n'
            '5. 막대마다 입찰, 사전, 낙찰, 그 밖 넷이 파랑의 짙고 옅음과 회색으로 쌓여 있다\n'
            '6. 범례가 그래프 위 오른쪽에 있다\n'
            '7. 막대 위 합계와 표의 달 × 유형 건수가 서버 응답 값과 같다\n'
            '8. 처음에는 합계가 가장 많은 달이 골라져 그 달 공고 목록이 아래에 있다\n'
            '9. 아래 띠의 채운 단추는 [무료로 시작] 이다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '서버', '가상', '가상'],
     req=['BID-MKT-005'], fc=['BID-MKT-005'], screen='/analysis/monthly', pri='Medium'),

dict(id='IT-06-076', flow='월별 동향과 업종별 발주', kind='정상', title='모은 공고가 없는 달은 0 으로 그리고 자료 기준에 밝힌다',
     pre='비로그인, 첫 게시 달과 이번 달 사이에 모은 공고 0건인 달이 있음',
     steps='1. /analysis/monthly 를 연다\n2. 0건인 달 막대를 누른다',
     expect='1. 그 달 막대 자리 위 합계가 0 이다\n'
            '2. 자료 기준에 모은 공고가 없는 달로 그 달이 적혀 있다\n'
            '3. 그 달을 누르면 그 달 공고 자리에 "이 달에 모은 공고가 없습니다" 가 있다',
     judge=['가상', '가상', '가상'],
     req=['BID-MKT-005'], fc=['BID-MKT-005'], screen='/analysis/monthly', pri='Medium'),

dict(id='IT-06-077', flow='월별 동향과 업종별 발주', kind='정상', title='막대에 마우스나 키보드로 가면 유형별 건수 풍선이 뜬다',
     pre='비로그인',
     steps='1. /analysis/monthly 에서 막대 하나에 마우스를 올린다\n2. 키보드 탭으로 다른 막대로 옮긴다',
     expect='1. 마우스를 올린 막대에 입찰, 사전, 낙찰, 그 밖 건수가 든 풍선이 있다\n'
            '2. 키보드로 옮긴 막대에도 그 막대의 유형별 건수 풍선이 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-005'], fc=['BID-MKT-005'], screen='/analysis/monthly', pri='Medium'),

dict(id='IT-06-078', flow='월별 동향과 업종별 발주', kind='정상', title='막대를 누르면 그 달 공고 목록이 바뀌고 주소에 남는다',
     pre='비로그인, 공고가 21건 이상인 달 1개',
     steps='1. /analysis/monthly 에서 그 달 막대를 누른다\n2. 같은 달로 GET /opportunities/collected?month= 응답을 받아 둔다\n3. 새로고침한다',
     expect='1. 누른 막대가 강조된다\n'
            '2. 그 달 공고 목록에 유형, 제목, 공고번호, 게시일이 20줄씩 있다\n'
            '3. 그 달 공고 목록 줄이 서버 응답과 같다\n'
            '4. 주소에 month 값이 남는다\n'
            '5. 새로고침 뒤에도 같은 달이 골라져 있다',
     judge=['가상', '가상', '서버', '가상', '가상'],
     req=['BID-MKT-005'], fc=['BID-MKT-005'], screen='/analysis/monthly', pri='Medium'),

dict(id='IT-06-079', flow='월별 동향과 업종별 발주', kind='정상', title='공개 공고문이 있는 그 달 공고 줄은 로그인을 거쳐 공고 상세로 간다',
     pre='비로그인에서 시작, 회원 계정 1개, 고른 달에 공개 한글 공고문이 있는 공고 1건',
     steps='1. /analysis/monthly 에서 그 달을 고르고 그 공고 줄을 누른다\n2. 회원 계정으로 로그인한다\n3. 브라우저 뒤로 가기로 월별 동향으로 돌아온다',
     expect='1. 줄을 누르면 로그인 화면으로 간다\n'
            '2. 로그인 뒤 그 공고의 /search/{공고문 id} 가 열린다\n'
            '3. 뒤로 오면 고른 달이 그대로다\n'
            '4. 로그인한 뒤 아래 띠의 채운 단추는 [마감 전 공고] 다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-MKT-005'], fc=['BID-MKT-005'], screen='/analysis/monthly', pri='Medium'),

dict(id='IT-06-080', flow='월별 동향과 업종별 발주', kind='정상', title='공개 공고문이 없는 그 달 공고 줄은 원본 공고 상세로 간다',
     pre='회원, 고른 달에 공개 공고문이 없는 공고 1건',
     steps='1. 회원 계정으로 로그인한다\n2. /analysis/monthly 에서 그 달을 고르고 그 공고 줄을 누른다',
     expect='1. 그 공고의 /search/origin/{원본 공고 id} 가 열린다\n'
            '2. 머리에 [SAM.gov 원문 보기] 단추가 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-005'], fc=['BID-MKT-005'], screen='/analysis/monthly', pri='Medium'),

dict(id='IT-06-081', flow='월별 동향과 업종별 발주', kind='예외', title='월별 집계를 못 받으면 그래프와 표 자리에 다시 시도가 있다',
     pre='비로그인, GET /opportunities/stats/monthly 가 실패하게 막은 상태',
     steps='1. /analysis/monthly 를 연다',
     expect='1. 그래프와 표 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 있다',
     judge=['가상'],
     req=['BID-MKT-005'], fc=['BID-MKT-005'], screen='/analysis/monthly', pri='Medium'),

dict(id='IT-06-082', flow='월별 동향과 업종별 발주', kind='예외', title='그 달 공고 목록을 못 받으면 그 자리만 다시 시도다',
     pre='비로그인, GET /opportunities/collected?month= 만 실패하게 막은 상태',
     steps='1. /analysis/monthly 에서 막대 하나를 누른다',
     expect='1. 그 달 공고 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 있다\n'
            '2. 막대 그래프와 표는 그대로 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-005'], fc=['BID-MKT-005'], screen='/analysis/monthly', pri='Medium'),

dict(id='IT-06-083', flow='월별 동향과 업종별 발주', kind='정상', title='업종별 발주는 대분류를 많은 순 가로 막대로 보이고 업종 표시 없음을 맨 아래에 둔다',
     pre='비로그인, 업종 코드 31, 32, 33 공고와 업종 코드 없는 공고가 섞여 있음',
     steps='1. 발주분석 묶음 메뉴에서 [업종별 발주] 를 누른다\n2. 같은 때 GET /opportunities/stats/naics 응답을 받아 둔다',
     expect='1. 로그인 화면을 거치지 않고 /analysis/naics 가 열린다\n'
            '2. 제목 줄에 "업종별 발주" 와 "업종(NAICS) 대분류마다 공고가 얼마나 나왔는지 보여 드립니다" 가 있다\n'
            '3. 자료 기준에 게시일 기간과 "n건 중 업종 코드 있는 n건" 이 있다\n'
            '4. 막대마다 대분류 한글 이름, 코드, 건수가 있고 건수 많은 순이다\n'
            '5. 맨 아래 막대가 "업종 표시 없음" 이다\n'
            '6. 코드 31, 32, 33 공고는 코드 "31-33" 막대 하나에 합쳐 있다\n'
            '7. 막대 건수가 서버 응답 값과 같다\n'
            '8. 처음에는 가장 많은 업종이 골라져 그 업종 칸이 있다',
     judge=['가상', '가상', '가상', '가상', '가상', '서버', '서버', '가상'],
     req=['BID-MKT-006'], fc=['BID-MKT-006'], screen='/analysis/naics', pri='Medium'),

dict(id='IT-06-084', flow='월별 동향과 업종별 발주', kind='정상', title='업종 막대를 누르면 세부 코드별 건수와 최근 공고 5건이 나오고 주소에 남는다',
     pre='비로그인, 같은 공고번호로 여러 판이 올라온 공고가 있는 대분류 1개',
     steps='1. /analysis/naics 에서 그 대분류 막대를 누른다\n2. 같은 대분류로 GET /opportunities/collected?sector=&latestOnly=true 응답을 받아 둔다\n3. 새로고침한다',
     expect='1. 누른 막대가 강조된다\n'
            '2. 고른 업종 칸에 여섯 자리 세부 업종 코드별 건수가 있다\n'
            '3. 최근 공고가 5건 있고 서버 응답과 같다\n'
            '4. 최근 공고 5건에 같은 공고번호가 두 번 들어 있지 않다\n'
            '5. 주소에 sector 값이 남는다\n'
            '6. 새로고침 뒤에도 같은 업종이 골라져 있다',
     judge=['가상', '가상', '서버', '가상', '가상', '가상'],
     req=['BID-MKT-006'], fc=['BID-MKT-006'], screen='/analysis/naics', pri='Medium'),

dict(id='IT-06-085', flow='월별 동향과 업종별 발주', kind='정상', title='업종 표시 없음을 누르면 세부 코드 칸 없이 최근 공고 5건만 있다',
     pre='비로그인, 업종 코드 없는 공고 5건 이상',
     steps='1. /analysis/naics 에서 "업종 표시 없음" 막대를 누른다',
     expect='1. 고른 업종 칸에 세부 업종 코드 칸이 없다\n'
            '2. 업종 코드 없는 최근 공고 5건이 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-006'], fc=['BID-MKT-006'], screen='/analysis/naics', pri='Medium'),

dict(id='IT-06-086', flow='월별 동향과 업종별 발주', kind='정상', title='공개 공고문이 있는 업종 최근 공고 줄은 로그인을 거쳐 공고 상세로 간다',
     pre='비로그인에서 시작, 회원 계정 1개, 고른 업종 최근 공고 중 공개 한글 공고문이 있는 공고 1건',
     steps='1. /analysis/naics 에서 그 업종을 고르고 그 공고 줄을 누른다\n2. 회원 계정으로 로그인한다\n3. 브라우저 뒤로 가기로 업종별 발주로 돌아온다',
     expect='1. 줄을 누르면 로그인 화면으로 간다\n'
            '2. 로그인 뒤 그 공고의 /search/{공고문 id} 가 열린다\n'
            '3. 뒤로 오면 고른 업종이 그대로다\n'
            '4. 로그인한 뒤 아래 띠의 채운 단추는 [마감 전 공고] 다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-MKT-006'], fc=['BID-MKT-006'], screen='/analysis/naics', pri='Medium'),

dict(id='IT-06-087', flow='월별 동향과 업종별 발주', kind='정상', title='공개 공고문이 없는 업종 최근 공고 줄은 원본 공고 상세로 간다',
     pre='회원, 고른 업종 최근 공고 중 공개 공고문이 없는 공고 1건',
     steps='1. 회원 계정으로 로그인한다\n2. /analysis/naics 에서 그 업종을 고르고 그 공고 줄을 누른다',
     expect='1. 그 공고의 /search/origin/{원본 공고 id} 가 열린다\n'
            '2. 머리에 [SAM.gov 원문 보기] 단추가 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-006'], fc=['BID-MKT-006'], screen='/analysis/naics', pri='Medium'),

dict(id='IT-06-088', flow='월별 동향과 업종별 발주', kind='예외', title='업종 집계를 못 받으면 막대 자리에 다시 시도가 있다',
     pre='비로그인, GET /opportunities/stats/naics 가 실패하게 막은 상태',
     steps='1. /analysis/naics 를 연다',
     expect='1. 막대 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 있다',
     judge=['가상'],
     req=['BID-MKT-006'], fc=['BID-MKT-006'], screen='/analysis/naics', pri='Medium'),

dict(id='IT-06-089', flow='월별 동향과 업종별 발주', kind='예외', title='고른 업종 공고를 못 받으면 그 자리만 다시 시도다',
     pre='비로그인, GET /opportunities/collected?sector= 만 실패하게 막은 상태',
     steps='1. /analysis/naics 에서 업종 막대 하나를 누른다',
     expect='1. 고른 업종 자리에 "불러오지 못했습니다" 와 [다시 시도] 가 있다\n'
            '2. 대분류 가로 막대는 그대로 있다',
     judge=['가상', '가상'],
     req=['BID-MKT-006'], fc=['BID-MKT-006'], screen='/analysis/naics', pri='Medium'),

dict(id='IT-06-090', flow='관리자 사진 관리', kind='정상', title='관리자가 고객 화면 사진 화면에서 자리 10곳과 올리기 규칙을 본다',
     pre='관리자, 처음 배포 상태(자리마다 처음부터 있던 사진 1장), POL-039 값이 처음 값',
     steps='1. 관리자 화면에 로그인한다\n2. 왼쪽 메뉴 기지 관리 아래 [고객 화면 사진] 을 누른다\n3. GET /admin/site-photos 응답과 site_photos 표를 본다',
     expect='1. 머리에 제목 "고객 화면 사진" 과 "사진 자리마다 여러 장을 올려 두면 손님이 들어올 때마다 그 자리의 사진 중 한 장을 골라 보여줍니다" 가 있다\n'
            '2. 왼쪽 자리 목록에 홈 첫걸음 상자, 홈 공고 0건 상자, 홈 홍보 칸 등록 대행, 홈 홍보 칸 현장 지원, 로그인, 회원가입, 서비스 안내 상담, 서비스 안내 온라인 등록, 서비스 안내 회의, 서비스 안내 세미나 10곳이 sort_order 순으로 있다\n'
            '3. 자리마다 대표 작은 사진, 자리 이름, 보이는 곳 한 줄, 보임/전체 장수가 있다\n'
            '4. 처음에는 첫 자리가 골라져 오른쪽 자리 머리에 그 자리 이름과 "보임 1장 · 숨김 0장" 이 있다\n'
            '5. 올리기 안내에 받는 형식 image/jpeg, image/png, 한 장 무게 상한 20MB, 가로 최소 1000, 저장 가로 1600, 무게 목표 400KB 가 있다\n'
            '6. 처음부터 있던 사진 카드에 순번, 파일 이름, 저장 크기와 무게, "처음부터 있던 사진" 이 있다\n'
            '7. site_photos 에 자리마다 처음부터 있던 사진 행이 1개씩 모두 10행 있다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-091', flow='관리자 사진 관리', kind='정상', title='자리를 누르면 오른쪽이 그 자리의 사진 칸으로 바뀐다',
     pre='관리자',
     steps='1. 관리자 /site-photos 에서 왼쪽 자리 목록의 [로그인] 을 누른다',
     expect='1. 자리 머리의 이름이 로그인으로 바뀐다\n'
            '2. 자리 머리에 "보임 N장 · 숨김 N장" 과 보이는 곳 설명이 있다\n'
            '3. 사진 칸에 로그인 자리의 사진 카드만 있다',
     judge=['가상', '가상', '가상'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-092', flow='관리자 사진 관리', kind='정상', title='가로 4032 사진 셋을 고르면 한 장씩 올라가 가로 1600 으로 줄어 저장된다',
     pre='관리자, 가로 4032 JPEG 사진 3장',
     steps='1. 관리자 /site-photos 에서 홈 첫걸음 상자 자리를 고른다\n2. [사진 올리기] 에서 사진 3장을 한 번에 고른다\n3. 올리는 동안 사진 칸과 네트워크 탭을 본다\n4. 다 올라간 뒤 site_photos 표와 저장 파일을 본다',
     expect='1. 파일마다 파일 이름, 원래 무게, "크기를 줄여 저장하는 중" 이 있는 올리는 중 카드가 있다\n'
            '2. POST /admin/site-photos 가 한 장씩 차례로 3번 간다\n'
            '3. 끝난 카드마다 저장 크기 1600 가로, 무게, 원본 크기 4032 가로, 올린 날이 있다\n'
            '4. 모두 끝나면 자리 장수가 3장 늘어난 값으로 바뀐다\n'
            '5. site_photos 에 그 자리 3행이 생기고 width 1600, original_width 4032, visible 참이다\n'
            '6. 저장 파일이 JPEG 이고 size_bytes 가 400KB 안팎이다\n'
            '7. 저장소에 올린 원본 파일이 남아 있지 않다',
     judge=['가상', '가상', '가상', '가상', '서버', '서버', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-093', flow='관리자 사진 관리', kind='정상', title='휴대폰 세로 사진은 촬영 방향대로 세우고 촬영 위치 정보를 지운다',
     pre='관리자, 촬영 방향 회전 값과 GPS 위치 정보가 든 휴대폰 세로 사진 1장',
     steps='1. 관리자 /site-photos 에서 로그인 자리를 고르고 그 사진을 올린다\n2. 저장된 사진 파일을 내려받아 사진 정보를 본다',
     expect='1. 사진 카드의 작은 사진이 촬영한 방향대로 서 있다\n'
            '2. 저장된 사진 파일 정보에 GPS 위치 값이 없다',
     judge=['합동', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-094', flow='관리자 사진 관리', kind='정상', title='투명한 PNG 는 흰 바탕 JPEG 로 다시 저장된다',
     pre='관리자, 투명한 부분이 있는 가로 1200 PNG 1장',
     steps='1. 관리자 /site-photos 에서 한 자리를 고르고 그 PNG 를 올린다\n2. 저장된 사진 파일을 내려받는다',
     expect='1. 저장된 사진 파일 형식이 JPEG 다\n'
            '2. 투명했던 부분이 흰 바탕으로 채워져 있다',
     judge=['서버', '합동'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-095', flow='관리자 사진 관리', kind='정상', title='저장 가로보다 좁은 사진은 줄이지 않는다',
     pre='관리자, 가로 1200 JPEG 1장',
     steps='1. 관리자 /site-photos 에서 한 자리를 고르고 그 사진을 올린다',
     expect='1. site_photos 새 행의 width 가 1200 이다\n'
            '2. 사진 카드에 원본 크기 줄이 없다',
     judge=['서버', '가상'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-096', flow='관리자 사진 관리', kind='정상', title='보임 스위치를 끄면 숨김이 되고 다시 켜면 보임이 된다',
     pre='관리자, 보이는 사진이 2장인 자리',
     steps='1. 관리자 /site-photos 에서 그 자리를 고르고 첫 사진 카드의 [보임] 스위치를 끈다\n2. GET /site-photos 응답을 본다\n3. 같은 스위치를 다시 켠다',
     expect='1. 끈 카드가 흐려지고 "숨김" 이 붙는다\n'
            '2. 자리 머리가 "보임 1장 · 숨김 1장" 으로 바뀐다\n'
            '3. site_photos 그 행 visible 이 거짓이다\n'
            '4. 공개 목록 응답의 그 자리 주소에 숨긴 사진이 없다\n'
            '5. 다시 켜면 site_photos 그 행 visible 이 참이고 자리 머리가 "보임 2장 · 숨김 0장" 으로 바뀐다',
     judge=['가상', '가상', '서버', '서버', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-097', flow='관리자 사진 관리', kind='정상', title='올린 사진을 지우면 확인 창을 거쳐 행과 파일이 지워진다',
     pre='관리자, 관리자가 올린 사진 1장이 있는 자리',
     steps='1. 그 사진 카드의 [지우기] 를 누른다\n2. 확인 창에서 [닫기] 를 누른다\n3. 다시 [지우기] 를 누르고 확인 창의 [지우기] 를 누른다\n4. 지운 사진의 GET /site-photos/{id}/image 를 부른다',
     expect='1. 확인 창에 그 작은 사진과 "고객 화면에서 바로 빠지고 되돌릴 수 없습니다. 잠시 빼 두려면 숨김을 쓰세요" 가 있다\n'
            '2. 닫기를 누르면 창이 닫히고 카드가 그대로 있다\n'
            '3. 확인 창의 지우기를 누르면 카드가 사진 칸에서 빠진다\n'
            '4. DELETE 응답이 204 이고 site_photos 그 행이 없다\n'
            '5. 지운 사진의 사진 주소가 404 를 준다',
     judge=['가상', '가상', '가상', '서버', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-098', flow='관리자 사진 관리', kind='정상', title='처음부터 있던 사진을 지우면 행만 지운다',
     pre='관리자, 처음부터 있던 사진 1장과 올린 사진 1장이 있는 자리',
     steps='1. 처음부터 있던 사진 카드의 [지우기] 를 누르고 확인 창의 [지우기] 를 누른다\n2. 그 사진의 /img/ 주소를 연다',
     expect='1. site_photos 그 행이 없다\n'
            '2. 함께 배포된 /img/ 사진 파일은 그대로 열린다',
     judge=['서버', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-099', flow='관리자 사진 관리', kind='정상', title='자리의 사진을 모두 숨기면 빈 자리 안내와 주황 장수가 보인다',
     pre='관리자, 회원가입 자리',
     steps='1. 관리자 /site-photos 에서 회원가입 자리를 고르고 사진 카드 스위치를 모두 끈다\n2. 왼쪽 자리 목록의 회원가입 줄을 본다',
     expect='1. 사진 칸에 "보이는 사진이 없습니다" 와 "지금 고객 화면에는 이 자리에 사진 없이 바탕색만 보입니다" 가 있다\n'
            '2. 빈 자리 안내에 [사진 올리기] 단추가 있다\n'
            '3. 자리 목록 회원가입 줄의 장수가 주황 글자다\n'
            '4. 자리 목록 회원가입 줄의 대표 작은 사진이 빈 그림이다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-SITE-001', 'BID-SITE-002'], fc=['BID-SITE-001', 'BID-SITE-002'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-100', flow='관리자 사진 관리', kind='정상', title='POL-039 가로 최소를 바꾸면 올리기 안내와 검사가 새 값을 쓴다',
     pre='관리자, system_settings SITE_PHOTO_MIN_WIDTH 를 1200 으로 바꾼 상태, 가로 1100 JPEG 1장',
     steps='1. 관리자 /site-photos 를 다시 연다\n2. 한 자리를 고르고 가로 1100 사진을 올린다',
     expect='1. 올리기 안내의 가로 최소가 1200 이다\n'
            '2. POST 응답이 400 이고 사유가 있다\n'
            '3. 실패 카드에 파일 이름과 서버가 준 사유가 있다',
     judge=['가상', '서버', '가상'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-101', flow='관리자 사진 관리', kind='정상', title='POL-039 값이 비었거나 읽을 수 없으면 처음 값을 쓴다',
     pre='관리자, SITE_PHOTO_MIN_WIDTH 를 빈 값으로, SITE_PHOTO_SAVE_WIDTH 를 abc 로 바꾼 상태, 가로 4032 JPEG 1장',
     steps='1. 관리자 /site-photos 를 다시 연다\n2. 한 자리를 고르고 그 사진을 올린다',
     expect='1. 올리기 안내의 가로 최소가 1000 이다\n'
            '2. 올리기 안내의 저장 가로가 1600 이다\n'
            '3. site_photos 새 행의 width 가 1600 이다',
     judge=['가상', '가상', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-102', flow='관리자 사진 관리', kind='예외', title='받는 형식이 아닌 파일은 400 과 사유로 거절된다',
     pre='관리자, GIF 파일 1개',
     steps='1. 관리자 /site-photos 에서 한 자리를 고르고 GIF 파일을 올린다',
     expect='1. POST 응답이 400 이다\n'
            '2. 그 파일 실패 카드에 서버가 준 사유와 [닫기] 가 있다\n'
            '3. site_photos 행 수가 그대로다',
     judge=['서버', '가상', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-103', flow='관리자 사진 관리', kind='예외', title='그림으로 읽을 수 없는 파일은 400 과 사유로 거절된다',
     pre='관리자, 글자만 든 파일의 확장자를 .jpg 로 바꾼 파일 1개',
     steps='1. 관리자 /site-photos 에서 한 자리를 고르고 그 파일을 올린다',
     expect='1. POST 응답이 400 이다\n'
            '2. 그 파일 실패 카드에 서버가 준 사유가 있다\n'
            '3. site_photos 행 수가 그대로다',
     judge=['서버', '가상', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-104', flow='관리자 사진 관리', kind='예외', title='한 장 무게 상한 20MB 를 넘는 사진은 400 과 사유로 거절된다',
     pre='관리자, 21MB JPEG 1장',
     steps='1. 관리자 /site-photos 에서 한 자리를 고르고 그 사진을 올린다',
     expect='1. POST 응답이 400 이다\n'
            '2. 그 파일 실패 카드에 서버가 준 사유가 있다\n'
            '3. site_photos 행 수가 그대로다',
     judge=['서버', '가상', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-105', flow='관리자 사진 관리', kind='예외', title='화소 수 상한 50백만 화소를 넘는 사진은 400 과 사유로 거절된다',
     pre='관리자, 10000 x 6000 JPEG 1장(20MB 이하)',
     steps='1. 관리자 /site-photos 에서 한 자리를 고르고 그 사진을 올린다',
     expect='1. POST 응답이 400 이다\n'
            '2. 그 파일 실패 카드에 서버가 준 사유가 있다\n'
            '3. site_photos 행 수가 그대로다',
     judge=['서버', '가상', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-106', flow='관리자 사진 관리', kind='예외', title='가로 820 사진은 흐릿하다는 사유로 거절된다',
     pre='관리자, 가로 820 JPEG 1장',
     steps='1. 관리자 /site-photos 에서 한 자리를 고르고 그 사진을 올린다',
     expect='1. POST 응답이 400 이다\n'
            '2. 실패 카드에 "가로 820 사진이라 흐릿하게 보입니다. 가로 1000 이상 사진을 올려 주세요" 가 있다\n'
            '3. site_photos 행 수가 그대로다',
     judge=['서버', '가상', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-107', flow='관리자 사진 관리', kind='예외', title='빈 파일은 400 으로 거절된다',
     pre='관리자, 0바이트 .jpg 파일 1개',
     steps='1. 관리자 /site-photos 에서 한 자리를 고르고 그 파일을 올린다',
     expect='1. POST 응답이 400 이다\n'
            '2. 그 파일 실패 카드에 서버가 준 사유가 있다',
     judge=['서버', '가상'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-108', flow='관리자 사진 관리', kind='예외', title='없는 자리 키로 올리면 404 다',
     pre='관리자 토큰',
     steps='1. POST /admin/site-photos 에 slotKey NO_SUCH_SLOT 과 가로 1600 JPEG 를 보낸다',
     expect='1. 응답이 404 다\n'
            '2. site_photos 행 수가 그대로다',
     judge=['서버', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='', pri='Medium'),

dict(id='IT-06-109', flow='관리자 사진 관리', kind='예외', title='관리자가 아닌 계정은 사진을 올리거나 숨기거나 지우지 못한다',
     pre='회원 토큰, 보이는 사진 1장의 id',
     steps='1. 회원 토큰으로 POST /admin/site-photos 를 보낸다\n2. 회원 토큰으로 PATCH /admin/site-photos/{id} 에 visible false 를 보낸다\n3. 회원 토큰으로 DELETE /admin/site-photos/{id} 를 보낸다',
     expect='1. 세 요청 모두 거부된다\n'
            '2. site_photos 행 수와 그 사진의 visible 값이 그대로다',
     judge=['서버', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='', pri='Medium'),

dict(id='IT-06-110', flow='관리자 사진 관리', kind='예외', title='사진 목록을 못 받으면 다시 시도를 보인다',
     pre='관리자, GET /admin/site-photos 가 실패하게 막은 상태',
     steps='1. 관리자 /site-photos 를 연다',
     expect='1. "불러오지 못했습니다." 와 [다시 시도] 가 있다',
     judge=['가상'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-111', flow='관리자 사진 관리', kind='예외', title='보임 바꾸기가 실패하면 스위치를 되돌린다',
     pre='관리자, PATCH /admin/site-photos/{id} 가 실패하게 막은 상태',
     steps='1. 보이는 사진 카드의 [보임] 스위치를 끈다',
     expect='1. 스위치가 켜진 자리로 돌아간다\n'
            '2. 오류 안내가 있다\n'
            '3. site_photos 그 행 visible 이 참 그대로다',
     judge=['가상', '가상', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-112', flow='관리자 사진 관리', kind='예외', title='지우기가 실패하면 확인 창에 오류를 두고 카드를 남긴다',
     pre='관리자, DELETE /admin/site-photos/{id} 가 실패하게 막은 상태',
     steps='1. 사진 카드의 [지우기] 를 누르고 확인 창의 [지우기] 를 누른다',
     expect='1. 확인 창 안에 오류 안내가 있다\n'
            '2. 사진 카드가 사진 칸에 남아 있다\n'
            '3. site_photos 그 행이 그대로 있다',
     judge=['가상', '가상', '서버'],
     req=['BID-SITE-001'], fc=['BID-SITE-001'], screen='관리자 /site-photos', pri='Medium'),

dict(id='IT-06-113', flow='고객 화면 사진', kind='정상', title='공개 사진 목록은 로그인 없이 자리 10곳을 모두 담고 보이는 사진만 준다',
     pre='비로그인, 숨긴 사진 1장이 있는 자리 1곳',
     steps='1. GET /site-photos 를 부른다\n2. 응답의 사진 주소 하나로 GET /site-photos/{id}/image 를 부른다',
     expect='1. 응답 slots 에 자리 키 10개가 모두 있다\n'
            '2. 숨긴 사진의 주소가 응답에 없다\n'
            '3. 목록 응답의 Cache-Control 이 no-cache 다\n'
            '4. 사진 파일 응답의 Cache-Control 에 immutable 이 있다',
     judge=['서버', '서버', '서버', '서버'],
     req=['BID-SITE-002'], fc=['BID-SITE-002'], screen='', pri='Medium'),

dict(id='IT-06-114', flow='고객 화면 사진', kind='정상', title='처음 방문은 사진 목록을 기다리지 않고 화면부터 그리고 목록이 오면 사진을 그린다',
     pre='비로그인, 브라우저 저장소를 비운 상태, 공개 마감 전 공고 1건 이상, GET /site-photos 응답을 5초 늦춘 상태',
     steps='1. 홈(/)을 연다\n2. 5초 안에 화면을 본다\n3. 목록이 온 뒤 첫걸음 상자 사진 주소와 브라우저 저장소를 본다',
     expect='1. 5초 안에 마감 전 공고 목록과 첫걸음 상자의 꼬리표, 글, 단추가 있다\n'
            '2. 목록이 오기 전 사진 칸은 바탕색만 있고 깨진 그림 표시가 없다\n'
            '3. 목록이 오면 첫걸음 상자 사진 주소가 HOME_INTRO 목록 주소 중 하나다\n'
            '4. 브라우저 저장소에 받은 사진 목록이 남아 있다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-SITE-002'], fc=['BID-SITE-002'], screen='/', pri='Medium'),

dict(id='IT-06-115', flow='고객 화면 사진', kind='정상', title='한 방문 동안은 화면을 옮겨도 자리마다 같은 사진이다',
     pre='비로그인, HOME_INTRO 와 SERVICE_DESK 자리에 보이는 사진 3장씩',
     steps='1. 홈(/)을 열고 첫걸음 상자 사진 주소를 적어 둔다\n2. 로그인 화면으로 옮긴다\n3. /services/sam-registration 으로 옮기고 머리 사진 주소를 적어 둔다\n4. /services/sam-renewal 로 옮긴다\n5. 홈 로고를 눌러 홈으로 돌아온다',
     expect='1. 홈으로 돌아왔을 때 첫걸음 상자 사진 주소가 1번과 같다\n'
            '2. /services/sam-renewal 머리 사진 주소가 3번과 같다',
     judge=['가상', '가상'],
     req=['BID-SITE-002'], fc=['BID-SITE-002'], screen='/', pri='Medium'),

dict(id='IT-06-116', flow='고객 화면 사진', kind='정상', title='새로고침하면 남겨 둔 목록으로 바로 고르고 새 목록은 뒤에서 받아 남겨 두기만 한다',
     pre='비로그인, 한 번 방문해 사진 목록이 브라우저에 남아 있음, GET /site-photos 응답을 5초 늦춘 상태',
     steps='1. 홈에서 새로고침한다\n2. 목록 응답이 오기 전에 첫걸음 상자를 본다\n3. 목록 응답이 온 뒤 첫걸음 상자 사진 주소와 브라우저 저장소를 본다',
     expect='1. 목록 응답이 오기 전에 첫걸음 상자 사진이 남겨 둔 HOME_INTRO 목록 주소 중 하나로 그려져 있다\n'
            '2. 목록 응답이 온 뒤에도 첫걸음 상자 사진 주소가 2번과 같다\n'
            '3. 브라우저 저장소의 목록이 새 응답으로 바뀐다',
     judge=['가상', '가상', '가상'],
     req=['BID-SITE-002'], fc=['BID-SITE-002'], screen='/', pri='Medium'),

dict(id='IT-06-117', flow='고객 화면 사진', kind='정상', title='보이는 사진 여러 장 중 방문마다 무작위로 고른다',
     pre='비로그인, HOME_INTRO 자리에 보이는 사진 3장',
     steps='1. 홈을 10번 새로고침하며 첫걸음 상자 사진 주소를 적는다',
     expect='1. 10번 동안 적은 주소가 모두 HOME_INTRO 목록 주소 중 하나다\n'
            '2. 10번 동안 적은 주소가 두 가지 이상이다',
     judge=['가상', '가상'],
     req=['BID-SITE-002'], fc=['BID-SITE-002'], screen='/', pri='Medium'),

dict(id='IT-06-118', flow='고객 화면 사진', kind='정상', title='관리자가 숨긴 사진은 새 목록을 쓰는 방문부터 고르지 않는다',
     pre='비로그인 브라우저와 관리자 계정, HOME_INTRO 자리에 보이는 사진 2장',
     steps='1. 관리자가 HOME_INTRO 사진 1장을 숨긴다\n2. 고객 브라우저에서 홈을 한 번 새로고침한다\n3. 다시 10번 새로고침하며 첫걸음 상자 사진 주소를 적는다',
     expect='1. 3번에서 적은 주소에 숨긴 사진 주소가 없다\n'
            '2. 3번에서 적은 주소가 모두 남은 보이는 사진 주소다',
     judge=['가상', '가상'],
     req=['BID-SITE-002', 'BID-SITE-001'], fc=['BID-SITE-002', 'BID-SITE-001'], screen='/', pri='Medium'),

dict(id='IT-06-119', flow='고객 화면 사진', kind='정상', title='보이는 사진이 0장인 자리는 바탕색만 남기고 꼬리표와 글, 단추는 그대로다',
     pre='비로그인, LOGIN 과 SERVICE_DESK 자리의 사진을 모두 숨긴 상태',
     steps='1. 홈을 두 번 새로고침해 새 목록을 쓴다\n2. 로그인 화면을 연다\n3. /services/sam-registration 을 연다',
     expect='1. 로그인 화면 사진 칸에 사진 없이 회색 바탕과 꼬리표가 있다\n'
            '2. 로그인 화면에 깨진 그림 표시가 없다\n'
            '3. 서비스 안내 머리 사진 칸이 비어 있고 꼬리표, 서비스 이름, 설명, [상담 신청] 단추는 그대로 있다\n'
            '4. 서비스 안내에 깨진 그림 표시가 없다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-SITE-002'], fc=['BID-SITE-002'], screen='/login', pri='Medium'),

dict(id='IT-06-120', flow='고객 화면 사진', kind='정상', title='홈의 사진 자리 넷은 정해진 자리 목록에서 고른다',
     pre='비로그인, 공개 마감 전 공고 1건 이상, 자리마다 보이는 사진이 있음',
     steps='1. GET /site-photos 응답을 받아 둔다\n2. 홈을 열고 첫걸음 상자와 요금 구역 사진 안내 두 칸의 사진 주소를 본다',
     expect='1. 첫걸음 상자 사진 주소가 HOME_INTRO 목록 주소 중 하나다\n'
            '2. 사진 안내 등록 대행 사진 주소가 HOME_PROMO_REGISTRATION 목록 주소 중 하나다\n'
            '3. 사진 안내 현장 지원 사진 주소가 HOME_PROMO_SITE_VISIT 목록 주소 중 하나다',
     judge=['가상', '가상', '가상'],
     req=['BID-SITE-002'], fc=['BID-SITE-002'], screen='/', pri='Medium'),

dict(id='IT-06-121', flow='고객 화면 사진', kind='정상', title='공고 0건 홈의 상자 사진은 HOME_EMPTY 에서 고른다',
     pre='비로그인, 공개 마감 전 공고 0건인 시험 서버',
     steps='1. 홈을 열고 공고 0건 상자 사진 주소를 본다',
     expect='1. 공고 0건 상자 사진 주소가 HOME_EMPTY 목록 주소 중 하나다',
     judge=['가상'],
     req=['BID-SITE-002'], fc=['BID-SITE-002'], screen='/', pri='Medium'),

dict(id='IT-06-122', flow='고객 화면 사진', kind='정상', title='로그인, 회원가입, SNS 가입 사진은 각 자리에서 고른다',
     pre='비로그인, LOGIN 과 REGISTER 자리에 보이는 사진이 있음',
     steps='1. 로그인 화면을 열고 사진 주소를 본다\n2. 회원가입 화면을 열고 사진 주소를 본다\n3. SNS 가입 화면을 열고 사진 주소를 본다',
     expect='1. 로그인 화면 사진 주소가 LOGIN 목록 주소 중 하나다\n'
            '2. 회원가입 화면 사진 주소가 REGISTER 목록 주소 중 하나다\n'
            '3. SNS 가입 화면 사진 주소가 REGISTER 목록 주소 중 하나다',
     judge=['가상', '가상', '가상'],
     req=['BID-SITE-002'], fc=['BID-SITE-002'], screen='/login', pri='Medium'),

dict(id='IT-06-123', flow='고객 화면 사진', kind='정상', title='서비스 안내와 항목별 준비 안내 사진은 서비스마다 정해진 자리에서 고른다',
     pre='비로그인, SERVICE_DESK, SERVICE_ONLINE, SERVICE_MEETING, SERVICE_SEMINAR 자리에 보이는 사진이 있음',
     steps='1. GET /site-photos 응답을 받아 둔다\n2. /services/sam-registration, /services/translation, /coming-soon/kakao 의 머리 사진 주소를 본다\n3. /services/piee-registration, /services/invoicing, /coming-soon/proposal-review 의 머리 사진 주소를 본다\n4. /services/site-visit, /services/teaming, /coming-soon/awardees 의 머리 사진 주소를 본다\n5. /coming-soon/base-map 의 머리 사진 주소를 본다',
     expect='1. 2번 세 화면의 사진 주소가 SERVICE_DESK 목록 주소 중 하나다\n'
            '2. 3번 세 화면의 사진 주소가 SERVICE_ONLINE 목록 주소 중 하나다\n'
            '3. 4번 세 화면의 사진 주소가 SERVICE_MEETING 목록 주소 중 하나다\n'
            '4. 5번 화면의 사진 주소가 SERVICE_SEMINAR 목록 주소 중 하나다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-SITE-002'], fc=['BID-SITE-002'], screen='/services/{서비스키}', pri='Medium'),

dict(id='IT-06-124', flow='고객 화면 사진', kind='예외', title='처음 방문에 사진 목록을 못 받으면 사진 없이 바탕색만 남는다',
     pre='비로그인, 브라우저 저장소를 비운 상태, GET /site-photos 가 실패하게 막은 상태',
     steps='1. 홈(/)을 연다\n2. 로그인 화면으로 옮긴다',
     expect='1. 홈 사진 칸이 바탕색만 있고 꼬리표, 글, 단추는 그대로 있다\n'
            '2. 홈과 로그인 화면 어디에도 깨진 그림 표시가 없다',
     judge=['가상', '가상'],
     req=['BID-SITE-002'], fc=['BID-SITE-002'], screen='/', pri='Medium'),

dict(id='IT-06-125', flow='고객 화면 사진', kind='예외', title='사진 목록을 못 받아도 남겨 둔 목록이 있으면 그것으로 고른다',
     pre='비로그인, 한 번 방문해 사진 목록이 브라우저에 남아 있음, GET /site-photos 가 실패하게 막은 상태',
     steps='1. 홈에서 새로고침한다',
     expect='1. 첫걸음 상자 사진 주소가 남겨 둔 HOME_INTRO 목록 주소 중 하나다',
     judge=['가상'],
     req=['BID-SITE-002'], fc=['BID-SITE-002'], screen='/', pri='Medium'),

dict(id='IT-06-126', flow='고객 화면 사진', kind='예외', title='고른 사진 파일을 못 받으면 그 칸 사진을 감추고 바탕색만 남긴다',
     pre='비로그인 브라우저와 관리자 계정, HOME_INTRO 에 관리자가 올린 사진 1장만 보임, 고객 브라우저에 그 목록이 남아 있음',
     steps='1. 관리자가 그 사진을 지운다\n2. 고객 브라우저에서 홈을 새로고침한다\n3. 한 번 더 새로고침한다',
     expect='1. 2번에서 첫걸음 상자 사진 칸이 바탕색만 있고 깨진 그림 표시가 없다\n'
            '2. 2번에서 첫걸음 상자의 꼬리표, 글, 단추는 그대로 있다\n'
            '3. 3번에서 고르는 주소에 지운 사진 주소가 없다',
     judge=['가상', '가상', '가상'],
     req=['BID-SITE-002'], fc=['BID-SITE-002'], screen='/', pri='Medium'),

]
