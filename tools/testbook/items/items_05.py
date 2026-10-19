# 영역 05 고객센터 시험항목. 근거는 T1-1 기능요구사항 명세서(BID-SUPPORT-001~005), T1-3 BIZ-012·023·024, T1-4 POL-013·014, T1-5 Inquiry 상태, T3-1, T3-2, T3-3, T3-4 화면 설계다.

ITEMS = [

dict(id='IT-05-001', flow='1:1 문의 접수', kind='정상', title='비로그인 방문자가 메뉴로 1:1 문의를 열어 문의를 보낸다',
     pre='비로그인, 문의 유형 6개 모두 사용 중',
     steps='1. 고객 화면 메뉴 고객센터 > 1:1 문의 를 누른다\n2. 문의 유형 고르기를 펼쳐 본다\n3. 이름 "홍길동", 이메일 "you@example.com", 유형 서비스 상담, 제목 "등록 대행 상담", 내용 "견적이 궁금합니다" 를 적는다\n4. [문의 보내기] 를 누른다',
     expect='1. 주소가 /support 이고 왼쪽 묶음 메뉴에서 1:1 문의 항목이 옅은 파랑 바탕과 왼쪽 파란 줄로 켜져 있다\n'
            '2. 위치 줄이 홈, 고객센터, 1:1 문의 순서다\n'
            '3. 폼에 이름, 이메일, 문의 유형, 제목, 내용 칸이 있고 이름과 이메일 칸이 비어 있다\n'
            '4. 폼 위에 "문의 유형과 제목을 채워 두었습니다" 초록 줄이 없다\n'
            '5. 오른쪽에 "메일로도 받습니다" 상자(고객센터 이메일)와 "먼저 찾아보세요" 상자(자주 묻는 질문, 요금 안내, 용어 사전)가 있다\n'
            '6. 유형 고르기가 서비스 이용 문의, 서비스 상담, SAM.gov 등록 문의, 제안서 자문·검토 문의, 결제·환불 문의, 기타 순서다\n'
            '7. 4번 뒤 폼 자리에 "문의가 접수되었습니다." 와 "답변은 you@example.com 로 보내 드립니다." 가 보인다\n'
            '8. 문의 접수(POST /inquiries) 응답이 201 이고 status 가 RECEIVED 다\n'
            '9. 문의 기록(inquiries)에 1행이 생기고 유형 CONSULTING, 제목, 내용, 이름, 이메일이 적은 값이며 member_id 가 비어 있고 상태가 RECEIVED 다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상', '서버', '서버'],
     req=['BID-SUPPORT-001'], fc=['BID-SUPPORT-001'], screen='/support', pri='High',
     issue='보내기 단추 이름이 설계는 "문의 보내기", 화면 소스는 "문의 남기기" 다'),

dict(id='IT-05-002', flow='1:1 문의 접수', kind='정상', title='접수 뒤 새 문의 쓰기를 누르면 빈 폼이 다시 나온다',
     pre='비로그인, 방금 문의를 보내 접수 완료 안내가 떠 있음',
     steps='1. 접수 완료 안내의 [새 문의 쓰기] 를 누른다',
     expect='1. 접수 완료 안내 자리에 이름, 이메일, 문의 유형, 제목, 내용 칸이 있는 폼이 다시 있다\n'
            '2. 이름, 이메일, 제목, 내용 칸이 모두 비어 있다',
     judge=['가상', '가상'],
     req=['BID-SUPPORT-001'], fc=['BID-SUPPORT-001'], screen='/support', pri='High'),

dict(id='IT-05-003', flow='1:1 문의 접수', kind='정상', title='로그인 고객은 이름과 이메일이 채워지고 문의가 회원과 묶인다',
     pre='담당자 이름 "김담당", 이메일 "kim@corp.com" 인 고객 계정',
     steps='1. 고객 계정으로 로그인해 /support 를 연다\n2. 이름 칸을 "김대리" 로 고친다\n3. 유형, 제목, 내용을 적고 [문의 보내기] 를 누른다\n4. [새 문의 쓰기] 를 누른다',
     expect='1. 1번에서 이름 칸이 "김담당", 이메일 칸이 "kim@corp.com" 으로 채워져 있다\n'
            '2. 2번에서 이름 칸 글을 고칠 수 있다\n'
            '3. 3번 뒤 "답변은 kim@corp.com 로 보내 드립니다." 가 보인다\n'
            '4. 새 문의 기록의 member_id 가 그 고객 계정 id 이고 이름이 "김대리" 다\n'
            '5. 4번 뒤 이름 칸이 "김담당", 이메일 칸이 "kim@corp.com" 으로 다시 채워지고 제목과 내용 칸은 비어 있다',
     judge=['가상', '가상', '가상', '서버', '가상'],
     req=['BID-SUPPORT-001'], fc=['BID-SUPPORT-001'], screen='/support', pri='High'),

dict(id='IT-05-004', flow='1:1 문의 접수', kind='정상', title='담당자 이름이 없는 로그인 고객은 이름 칸에 회사명이 채워진다',
     pre='담당자 이름이 비어 있고 회사명 "한빛시설관리" 인 고객 계정',
     steps='1. 그 계정으로 로그인해 /support 를 연다',
     expect='1. 이름 칸이 "한빛시설관리" 로 채워져 있다\n'
            '2. 이메일 칸이 그 계정 이메일로 채워져 있다',
     judge=['가상', '가상'],
     req=['BID-SUPPORT-001'], fc=['BID-SUPPORT-001'], screen='/support', pri='High'),

dict(id='IT-05-005', flow='1:1 문의 접수', kind='정상', title='보내는 동안 단추가 "보내는 중" 으로 잠긴다',
     pre='비로그인, 문의 접수 응답이 늦게 오는 상태',
     steps='1. 모든 칸을 적고 [문의 보내기] 를 누른다\n2. 응답이 오기 전 단추를 다시 누른다',
     expect='1. 1번 뒤 단추 글이 "보내는 중" 으로 바뀌고 비활성이다\n'
            '2. 2번을 해도 문의 기록이 1행만 생긴다',
     judge=['가상', '서버'],
     req=['BID-SUPPORT-001'], fc=['BID-SUPPORT-001'], screen='/support', pri='High'),

dict(id='IT-05-006', flow='1:1 문의 접수', kind='정상', title='사용 여부를 끈 문의 유형은 고르기에서 빠진다',
     pre='비로그인, 문의 유형 코드 목록에서 PAYMENT 행의 사용 여부를 끔',
     steps='1. 문의 유형 조회(GET /inquiries/types)를 부른다\n2. /support 를 열어 유형 고르기를 펼친다',
     expect='1. 1번 응답이 SERVICE, CONSULTING, SAM_REGISTRATION, PROPOSAL, ETC 5개를 그 순서로 담고 PAYMENT 가 없다\n'
            '2. 2번 유형 고르기에 "결제·환불 문의" 가 없다',
     judge=['서버', '가상'],
     req=['BID-SUPPORT-001'], fc=['BID-SUPPORT-001'], screen='/support', pri='High'),

dict(id='IT-05-007', flow='1:1 문의 접수', kind='예외', title='없는 유형 코드나 사용하지 않는 유형으로 보낸 문의는 거부한다',
     pre='비로그인, PAYMENT 유형의 사용 여부를 끔',
     steps='1. 문의 접수(POST /inquiries)를 typeCode "NOPE" 로 보낸다\n2. 문의 접수를 typeCode "PAYMENT" 로 보낸다',
     expect='1. 1번 응답이 400 INQUIRY_TYPE_INVALID 다\n'
            '2. 2번 응답이 400 INQUIRY_TYPE_INVALID 다\n'
            '3. 두 요청 뒤 문의 기록 행 수가 늘지 않는다',
     judge=['서버', '서버', '서버'],
     req=['BID-SUPPORT-001'], fc=['BID-SUPPORT-001'], screen='', pri='High'),

dict(id='IT-05-008', flow='1:1 문의 접수', kind='예외', title='저장에 실패하면 접수 안내 없이 입력을 그대로 두고 다시 보낼 수 있다',
     pre='비로그인, 문의 접수가 서버 오류로 답하는 상태',
     steps='1. 모든 칸을 적고 [문의 보내기] 를 누른다\n2. 서버가 되살아난 뒤 [문의 보내기] 를 다시 누른다',
     expect='1. 1번 뒤 단추 위에 빨간 줄 "문의를 보내지 못했습니다. 잠시 뒤 다시 보내 주세요." 가 보인다\n'
            '2. 1번 뒤 "문의가 접수되었습니다." 가 없다\n'
            '3. 1번 뒤 이름, 이메일, 유형, 제목, 내용 칸이 적은 값 그대로다\n'
            '4. 1번 뒤 [문의 보내기] 가 활성이다\n'
            '5. 2번 뒤 "문의가 접수되었습니다." 가 보이고 문의 기록이 1행 생긴다',
     judge=['가상', '가상', '가상', '가상', '서버'],
     req=['BID-SUPPORT-001'], fc=['BID-SUPPORT-001'], screen='/support', pri='High'),

dict(id='IT-05-009', flow='1:1 문의 접수', kind='예외', title='칸 검사에 걸리는 문의는 접수하지 않는다',
     pre='비로그인',
     steps='1. 문의 접수를 제목 201자로 보낸다\n2. 내용 5001자로 보낸다\n3. 이름 101자로 보낸다\n4. 이메일 "abc" 로 보낸다\n5. 화면에서 서버가 400 으로 답하게 한 뒤 [문의 보내기] 를 누른다',
     expect='1. 1번부터 4번 응답이 모두 400 이다\n'
            '2. 1번부터 4번 뒤 문의 기록 행 수가 늘지 않는다\n'
            '3. 5번 뒤 단추 위 빨간 줄 "문의를 보내지 못했습니다. 잠시 뒤 다시 보내 주세요." 가 보이고 "문의가 접수되었습니다." 가 없다',
     judge=['서버', '서버', '가상'],
     req=['BID-SUPPORT-001'], fc=['BID-SUPPORT-001'], screen='/support', pri='High'),

dict(id='IT-05-010', flow='1:1 문의 접수', kind='예외', title='문의 유형을 못 불러오면 보내기를 막는다',
     pre='비로그인, 문의 유형 조회가 오류로 답하는 상태',
     steps='1. /support 를 연다',
     expect='1. 유형 고르기가 비어 있다\n'
            '2. 단추 위에 "문의 유형을 불러오지 못했습니다. 잠시 뒤 다시 열어 주세요." 가 보인다\n'
            '3. [문의 보내기] 가 비활성이다',
     judge=['가상', '가상', '가상'],
     req=['BID-SUPPORT-001'], fc=['BID-SUPPORT-001'], screen='/support', pri='High'),

dict(id='IT-05-011', flow='1:1 문의 접수', kind='예외', title='로그인 고객의 내 정보 조회가 실패하면 이름과 이메일을 비워 둔다',
     pre='고객 계정으로 로그인, 내 정보 조회(GET /members/me)가 오류로 답하는 상태',
     steps='1. /support 를 연다',
     expect='1. 이름 칸과 이메일 칸이 비어 있다\n'
            '2. 이름과 이메일을 직접 적어 [문의 보내기] 를 누르면 "문의가 접수되었습니다." 가 보인다',
     judge=['가상', '가상'],
     req=['BID-SUPPORT-001'], fc=['BID-SUPPORT-001'], screen='/support', pri='High'),

dict(id='IT-05-012', flow='유형·제목 채워 열기', kind='정상', title='서비스 안내의 상담 신청으로 1:1 문의를 열면 유형과 제목이 채워진다',
     pre='비로그인',
     steps='1. SAM 등록 대행 서비스 안내(/services/{서비스키}) 머리의 [상담 신청] 을 누른다',
     expect='1. 주소가 /support?type=SAM_REGISTRATION&title= 로 시작한다\n'
            '2. 유형 고르기가 SAM.gov 등록 문의로 골라져 있다\n'
            '3. 제목 칸이 "{그 서비스 이름} 상담 신청" 이다\n'
            '4. 폼 위에 초록 줄 "문의 유형과 제목을 채워 두었습니다" 가 보인다\n'
            '5. 화면이 폼 위치로 내려와 있다',
     judge=['가상', '가상', '가상', '가상', '가상'],
     req=['BID-SUPPORT-001'], fc=['BID-SUPPORT-001'], screen='/support', pri='High'),

dict(id='IT-05-013', flow='유형·제목 채워 열기', kind='정상', title='주소의 제목이 200자를 넘으면 앞 200자만 채운다',
     pre='비로그인',
     steps='1. /support?type=CONSULTING&title={250자 글} 을 연다',
     expect='1. 유형 고르기가 서비스 상담으로 골라져 있다\n'
            '2. 제목 칸 글자 수가 200자이고 주소 제목의 앞 200자와 같다',
     judge=['가상', '가상'],
     req=['BID-SUPPORT-001'], fc=['BID-SUPPORT-001'], screen='/support', pri='High'),

dict(id='IT-05-014', flow='유형·제목 채워 열기', kind='예외', title='주소의 유형 코드가 없거나 쓰지 않는 유형이면 첫 유형으로 둔다',
     pre='비로그인, PAYMENT 유형의 사용 여부를 끔',
     steps='1. /support?type=NOPE&title=테스트 를 연다\n2. /support?type=PAYMENT 를 연다',
     expect='1. 1번 유형 고르기가 목록 첫 번째인 서비스 이용 문의로 골라져 있다\n'
            '2. 1번 제목 칸이 "테스트" 다\n'
            '3. 2번 유형 고르기가 서비스 이용 문의로 골라져 있다',
     judge=['가상', '가상', '가상'],
     req=['BID-SUPPORT-001'], fc=['BID-SUPPORT-001'], screen='/support', pri='High'),

dict(id='IT-05-015', flow='관리자 알림 메일', kind='정상', title='알림 주소 설정의 주소마다 접수 알림 메일이 1통씩 간다',
     pre='설정 INQUIRY_NOTIFY_EMAILS 값 "ops1@wj.com,ops2@wj.com", 두 주소의 메일함을 볼 수 있음',
     steps='1. 비로그인으로 /support 에서 문의를 보낸다\n2. 두 메일함을 연다\n3. 관리자로 그 문의 상세(/inquiries/{id})를 연다',
     expect='1. ops1@wj.com 과 ops2@wj.com 에 접수 알림 메일이 1통씩 온다\n'
            '2. 메일 본문에 유형, 제목, 내용, 이름, 이메일, 접수 시각이 고객이 적은 값으로 있다\n'
            '3. 알림 이력(notification_log)에 유형 INQUIRY_RECEIVED, reference_type Inquiry, reference_id 가 그 문의 id 인 행이 2행이고 성공 여부가 모두 true 다\n'
            '4. 3번 문의자 칸 관리자 알림에 보낸 시각과 받은 사람 수 2 가 보인다',
     judge=['합동', '합동', '서버', '가상'],
     req=['BID-SUPPORT-002'], fc=['BID-SUPPORT-002'], screen='/support', pri='High'),

dict(id='IT-05-016', flow='관리자 알림 메일', kind='정상', title='알림 주소 설정이 비어 있으면 관리자 계정 전원에게 보낸다',
     pre='설정 INQUIRY_NOTIFY_EMAILS 값 비움, ADMIN 계정 2개',
     steps='1. 비로그인으로 문의를 보낸다',
     expect='1. 알림 이력에 그 문의의 INQUIRY_RECEIVED 행이 2행이고 받는 주소가 두 ADMIN 계정 이메일이다\n'
            '2. 두 ADMIN 계정 메일함에 접수 알림 메일이 1통씩 온다',
     judge=['서버', '합동'],
     req=['BID-SUPPORT-002'], fc=['BID-SUPPORT-002'], screen='', pri='High'),

dict(id='IT-05-017', flow='관리자 알림 메일', kind='정상', title='알림 주소 설정의 이메일 형식이 아닌 값은 건너뛴다',
     pre='설정 INQUIRY_NOTIFY_EMAILS 값 "ops1@wj.com,잘못된값"',
     steps='1. 비로그인으로 문의를 보낸다',
     expect='1. 알림 이력에 그 문의의 INQUIRY_RECEIVED 행이 1행이고 받는 주소가 ops1@wj.com 이다\n'
            '2. 문의 접수 응답이 201 이다',
     judge=['서버', '서버'],
     req=['BID-SUPPORT-002'], fc=['BID-SUPPORT-002'], screen='', pri='High'),

dict(id='IT-05-018', flow='관리자 알림 메일', kind='정상', title='같은 문의의 접수 알림을 같은 주소로 두 번 보내지 않는다',
     pre='설정 INQUIRY_NOTIFY_EMAILS 값 "ops1@wj.com", 접수 알림이 이미 1통 나간 문의 1건',
     steps='1. 같은 문의의 접수 알림 발송을 한 번 더 일으킨다',
     expect='1. 알림 이력에 그 문의와 ops1@wj.com 의 INQUIRY_RECEIVED 행이 1행 그대로다\n'
            '2. ops1@wj.com 메일함에 그 문의 알림이 1통뿐이다',
     judge=['단위', '단위'],
     req=['BID-SUPPORT-002'], fc=['BID-SUPPORT-002'], screen='', pri='High'),

dict(id='IT-05-019', flow='관리자 알림 메일', kind='예외', title='알림 메일 발송이 실패해도 문의는 접수된다',
     pre='설정 INQUIRY_NOTIFY_EMAILS 값 "ops1@wj.com", 메일 발송이 실패하는 상태',
     steps='1. 비로그인으로 /support 에서 문의를 보낸다\n2. 관리자로 그 문의 상세를 연다',
     expect='1. 고객 화면에 "문의가 접수되었습니다." 가 보인다\n'
            '2. 문의 기록에 그 문의가 RECEIVED 로 저장된다\n'
            '3. 알림 이력에 그 문의의 INQUIRY_RECEIVED 행이 성공 여부 false 와 오류 내용으로 남는다\n'
            '4. 2번 문의자 칸 관리자 알림 자리에 실패가 보인다',
     judge=['단위', '단위', '단위', '단위'],
     req=['BID-SUPPORT-002'], fc=['BID-SUPPORT-002'], screen='관리자 /inquiries/{id}', pri='High'),

dict(id='IT-05-020', flow='관리자 문의 목록', kind='정상', title='관리자가 1:1 문의 메뉴로 들어가 미답변 문의 목록을 본다',
     pre='관리자 계정, 문의 미답변 3건(회원 1, 비회원 2)과 답변완료 2건',
     steps='1. 관리자 화면에 로그인한다\n2. 왼쪽 메뉴 맨 아래 "1:1 문의" 를 누른다',
     expect='1. 1번 뒤 왼쪽 메뉴 "1:1 문의" 옆에 빨간 숫자 3 이 있다\n'
            '2. 주소가 /inquiries 이고 제목 "1:1 문의", 부제 "고객센터로 들어온 문의를 확인하고 답변합니다." 가 보인다\n'
            '3. 상태 단추가 전체 5, 미답변 3, 답변완료 2 이고 미답변이 골라져 있다\n'
            '4. 유형 고르기가 "유형 전체" 와 유형 6개이고 오른쪽에 "최근 접수 순" 이 있다\n'
            '5. 표 머리가 접수 일시, 유형, 제목, 이름, 이메일, 구분, 상태 순서다\n'
            '6. 표에 미답변 3행이 접수 일시 최근 순으로 있고 상태 칸이 모두 "미답변" 이다\n'
            '7. 미답변 행의 제목이 굵다\n'
            '8. 구분 칸이 회원 문의 1행은 "회원", 나머지 2행은 "비회원" 이다\n'
            '9. 쪽 넘김에 "3건 중 1-3" 이 있다\n'
            '10. 건수 조회(GET /admin/inquiries/counts) 응답이 all 5, received 3, answered 2 다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상', '서버'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries', pri='High'),

dict(id='IT-05-021', flow='관리자 문의 목록', kind='정상', title='상태와 유형으로 거르면 맞는 문의만 첫 쪽부터 나온다',
     pre='관리자 계정, 답변완료 문의 25건(그중 결제·환불 문의 3건), 쪽당 20행',
     steps='1. /inquiries 에서 상태 [답변완료] 를 누른다\n2. 2쪽으로 간다\n3. 유형 고르기에서 결제·환불 문의 를 고른다',
     expect='1. 1번 뒤 표의 상태 칸이 모두 "답변완료" 이고 "25건 중 1-20" 이 있다\n'
            '2. 3번 뒤 1쪽으로 돌아와 "3건 중 1-3" 이 있다\n'
            '3. 3번 뒤 표의 유형 칸이 모두 "결제·환불 문의" 다\n'
            '4. 3번 목록 조회 요청의 status 가 ANSWERED, typeCode 가 PAYMENT 다',
     judge=['가상', '가상', '가상', '서버'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries', pri='High'),

dict(id='IT-05-022', flow='관리자 문의 목록', kind='정상', title='거른 조건에 맞는 문의가 없으면 빈 안내를 보인다',
     pre='관리자 계정, 기타 유형 미답변 문의 0건',
     steps='1. /inquiries 에서 미답변이 골라진 채 유형 기타 를 고른다',
     expect='1. 표 자리에 "조건에 맞는 문의가 없습니다." 가 보인다',
     judge=['가상'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries', pri='High'),

dict(id='IT-05-023', flow='관리자 문의 목록', kind='정상', title='상세에서 목록으로 돌아오면 거르기, 쪽, 스크롤 위치가 그대로다',
     pre='관리자 계정, 전체 문의 30건 이상',
     steps='1. /inquiries 에서 [전체] 를 누르고 2쪽으로 가서 아래로 내린다\n2. 아래쪽 행을 눌러 상세를 연다\n3. [목록으로] 를 누른다',
     expect='1. 2번에서 주소가 /inquiries/{그 문의 id} 이고 머리 제목이 누른 행의 제목이다\n'
            '2. 3번 뒤 상태 [전체] 가 골라져 있다\n'
            '3. 3번 뒤 2쪽이 열려 있다\n'
            '4. 3번 뒤 누르기 전 스크롤 위치로 돌아와 누른 행이 화면 안에 있다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries', pri='High'),

dict(id='IT-05-024', flow='관리자 문의 목록', kind='정상', title='미답변 문의 상세에 문의 내용, 답변 칸, 문의자 정보가 보인다',
     pre='관리자 계정, 회원 "한빛시설관리" 가 줄바꿈 2개를 넣어 보낸 미답변 문의 1건, 접수 알림 2통 성공',
     steps='1. /inquiries 에서 그 문의 행을 누른다',
     expect='1. 머리에 [목록으로], 상태 표 "미답변", 유형 표, 문의 제목이 있다\n'
            '2. 문의 내용 본문이 고객이 넣은 줄바꿈 그대로 3줄로 보인다\n'
            '3. 답변 입력 칸 아래 "보내면 {고객 이메일} 로 메일이 가고 상태가 답변완료로 바뀝니다. 보낸 답변은 고칠 수 없습니다." 가 보인다\n'
            '4. 답변 칸이 빈 동안 [답변 보내기] 가 비활성이다\n'
            '5. 문의자 칸에 이름, 이메일, 구분 "한빛시설관리", 접수 일시가 있다\n'
            '6. 관리자 알림에 보낸 시각과 받은 사람 수 2 가 있다\n'
            '7. 상세 조회 응답의 adminNotice 가 total 2, success 2 이고 answer, answeredAt, answeredByEmail 이 null 이다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '서버'],
     req=['BID-SUPPORT-003', 'BID-SUPPORT-002'], fc=['BID-SUPPORT-003', 'BID-SUPPORT-002'], screen='관리자 /inquiries/{id}', pri='High'),

dict(id='IT-05-025', flow='관리자 문의 목록', kind='정상', title='비회원 문의 상세의 구분은 비회원이다',
     pre='관리자 계정, 비로그인으로 보낸 미답변 문의 1건',
     steps='1. 그 문의 상세를 연다',
     expect='1. 문의자 칸 구분이 "비회원" 이다\n'
            '2. 상세 조회 응답의 memberId 와 memberCompanyName 이 null 이다',
     judge=['가상', '서버'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries/{id}', pri='High'),

dict(id='IT-05-026', flow='관리자 문의 목록', kind='정상', title='고객이 새로 보낸 문의가 관리자 목록 맨 위와 메뉴 숫자에 더해진다',
     pre='관리자 계정, 미답변 문의 3건, 관리자 화면 /inquiries 를 열어 둠',
     steps='1. 다른 브라우저에서 비로그인으로 /support 에서 문의를 보낸다\n2. 관리자 화면 /inquiries 를 새로고침한다',
     expect='1. 2번 뒤 메뉴 "1:1 문의" 옆 빨간 숫자가 4 다\n'
            '2. 2번 뒤 미답변 목록 첫 행이 방금 보낸 문의 제목이다',
     judge=['가상', '가상'],
     req=['BID-SUPPORT-003', 'BID-SUPPORT-001'], fc=['BID-SUPPORT-003', 'BID-SUPPORT-001'], screen='관리자 /inquiries', pri='High'),

dict(id='IT-05-027', flow='관리자 문의 목록', kind='정상', title='미답변 문의가 0건이면 메뉴에 빨간 숫자가 없다',
     pre='관리자 계정, 모든 문의가 답변완료',
     steps='1. 관리자 화면에 로그인한다',
     expect='1. 왼쪽 메뉴 "1:1 문의" 옆에 빨간 숫자가 없다',
     judge=['가상'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries', pri='High'),

dict(id='IT-05-028', flow='관리자 문의 목록', kind='예외', title='문의 목록을 못 불러오면 다시 시도를 준다',
     pre='관리자 계정, 문의 목록 조회가 오류로 답하는 상태',
     steps='1. /inquiries 를 연다\n2. 조회가 되살아난 뒤 [다시 시도] 를 누른다',
     expect='1. 1번 표 자리에 "문의를 불러오지 못했습니다." 와 [다시 시도] 가 보인다\n'
            '2. 2번 뒤 표에 미답변 문의 행이 접수 일시 최근 순으로 채워진다',
     judge=['가상', '가상'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries', pri='High'),

dict(id='IT-05-029', flow='관리자 문의 목록', kind='예외', title='건수를 못 불러오면 메뉴 숫자와 목록 건수를 감춘다',
     pre='관리자 계정, 미답변 문의 3건, 건수 조회가 오류로 답하는 상태',
     steps='1. 관리자 화면에 로그인해 /inquiries 를 연다',
     expect='1. 메뉴 "1:1 문의" 옆에 빨간 숫자가 없다\n'
            '2. 상태 단추 전체, 미답변, 답변완료 옆 건수 자리가 비어 있다',
     judge=['가상', '가상'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries', pri='High'),

dict(id='IT-05-030', flow='관리자 문의 목록', kind='예외', title='문의 유형을 못 불러오면 유형 고르기에 유형 전체만 둔다',
     pre='관리자 계정, 문의 유형 조회가 오류로 답하는 상태',
     steps='1. /inquiries 를 열어 유형 고르기를 펼친다',
     expect='1. 유형 고르기 항목이 "유형 전체" 하나뿐이다\n'
            '2. 표에 미답변 문의 행이 있다',
     judge=['가상', '가상'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries', pri='High'),

dict(id='IT-05-031', flow='관리자 문의 목록', kind='예외', title='없는 문의 상세는 없는 문의 안내와 목록으로를 보인다',
     pre='관리자 계정',
     steps='1. /inquiries/{없는 id} 를 연다\n2. [목록으로] 를 누른다',
     expect='1. "없는 문의입니다." 가 보인다\n'
            '2. 상세 조회 응답이 404 INQUIRY_NOT_FOUND 다\n'
            '3. 2번 뒤 /inquiries 가 열린다',
     judge=['가상', '서버', '가상'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries/{id}', pri='High'),

dict(id='IT-05-032', flow='관리자 문의 목록', kind='예외', title='ADMIN 이 아닌 계정은 문의 목록과 답변을 쓸 수 없다',
     pre='고객 계정, 미답변 문의 1건',
     steps='1. 로그인하지 않고 관리자 화면 /inquiries 를 연다\n2. 고객 토큰으로 문의 목록 조회(GET /admin/inquiries)를 부른다\n3. 고객 토큰으로 답변 보내기(POST /admin/inquiries/{id}/answer)를 부른다',
     expect='1. 1번에서 관리자 로그인 화면이 열린다\n'
            '2. 2번 응답이 403 이다\n'
            '3. 3번 응답이 403 이고 그 문의 상태가 RECEIVED 그대로다',
     judge=['가상', '서버', '서버'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries', pri='High'),

dict(id='IT-05-033', flow='답변 보내기', kind='정상', title='관리자가 답변을 보내면 고객에게 메일이 가고 답변완료로 바뀐다',
     pre='관리자 계정 admin@wj.com, 고객 이메일 메일함을 볼 수 있는 미답변 문의 1건, 미답변 3건',
     steps='1. /inquiries 에서 그 미답변 행을 누른다\n2. 답변 칸에 "견적서를 첨부해 드립니다" 를 쓰고 [답변 보내기] 를 누른다\n3. 응답이 온 뒤 화면을 본다',
     expect='1. 2번 뒤 단추 글이 "보내는 중" 으로 바뀌고 답변 칸과 단추가 비활성이다\n'
            '2. 3번에서 답변 칸 자리가 보낸 시각, 보낸 관리자 admin@wj.com, 고객 메일 발송 성공, 답변 본문 "견적서를 첨부해 드립니다" 로 바뀐다\n'
            '3. 상태 표가 "답변완료" 다\n'
            '4. 메뉴 "1:1 문의" 옆 빨간 숫자가 3 에서 2 로 준다\n'
            '5. 고객 메일함에 답변 본문이 담긴 답변 메일이 온다\n'
            '6. 문의 기록의 상태가 ANSWERED 이고 answer 가 보낸 본문, answered_at 이 보낸 시각, answered_by 가 그 관리자 계정이다\n'
            '7. 알림 이력에 유형 INQUIRY_ANSWERED, 받는 주소가 고객 이메일인 행이 성공 true 로 남는다',
     judge=['가상', '가상', '가상', '가상', '합동', '서버', '서버'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries/{id}', pri='High'),

dict(id='IT-05-034', flow='답변 보내기', kind='정상', title='답변완료 문의를 목록 답변완료 거르기에서 다시 열면 보낸 답변만 보인다',
     pre='관리자 계정, 방금 답변을 보낸 문의 1건',
     steps='1. [목록으로] 를 누른다\n2. 상태 [답변완료] 를 누르고 그 문의 행을 누른다',
     expect='1. 1번 뒤 미답변 목록에 그 문의가 없다\n'
            '2. 2번 목록에서 그 문의 행 상태 칸이 "답변완료" 이고 제목이 굵지 않다\n'
            '3. 2번 상세에 보낸 시각, 보낸 관리자, 고객 메일 발송 성공, 답변 본문이 있다\n'
            '4. 2번 상세에 답변 입력 칸과 [답변 보내기] 가 없다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries/{id}', pri='High'),

dict(id='IT-05-035', flow='답변 보내기', kind='예외', title='빈 답변이나 5000자를 넘는 답변은 보내지 않는다',
     pre='관리자 계정, 미답변 문의 1건',
     steps='1. 상세에서 답변 칸을 비운 채 본다\n2. 답변 보내기를 본문 "" 로 부른다\n3. 답변 보내기를 본문 5001자로 부른다',
     expect='1. 1번에서 [답변 보내기] 가 비활성이다\n'
            '2. 2번 응답이 400 이다\n'
            '3. 3번 응답이 400 이다\n'
            '4. 문의 기록 상태가 RECEIVED 이고 answer 가 비어 있다',
     judge=['가상', '서버', '서버', '서버'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries/{id}', pri='High'),

dict(id='IT-05-036', flow='답변 보내기', kind='예외', title='고객 메일 발송이 실패하면 답변을 저장하지 않고 다시 보낼 수 있게 둔다',
     pre='관리자 계정, 미답변 문의 1건, 메일 발송이 실패하는 상태',
     steps='1. 답변 칸에 "확인했습니다" 를 쓰고 [답변 보내기] 를 누른다\n2. 메일 발송이 되살아난 뒤 [답변 보내기] 를 다시 누른다',
     expect='1. 1번 응답이 502 INQUIRY_ANSWER_MAIL_FAILED 다\n'
            '2. 1번 뒤 단추 위 빨간 줄 "고객에게 메일을 보내지 못했습니다. 답변은 저장되지 않았습니다. 잠시 뒤 다시 보내 주세요." 가 보인다\n'
            '3. 1번 뒤 답변 칸에 "확인했습니다" 가 그대로 있고 상태 표가 "미답변" 이다\n'
            '4. 1번 뒤 문의 기록 상태가 RECEIVED 이고 answer, answered_at, answered_by 가 비어 있다\n'
            '5. 2번 뒤 상태 표가 "답변완료" 이고 문의 기록 상태가 ANSWERED 다',
     judge=['단위', '단위', '단위', '단위', '단위'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries/{id}', pri='High'),

dict(id='IT-05-037', flow='답변 보내기', kind='예외', title='다른 관리자가 먼저 답한 문의에 다시 답하면 거부한다',
     pre='관리자 계정 A 와 B, 둘이 같은 미답변 문의 상세를 열어 둠',
     steps='1. A 가 답변 "A 답변" 을 보낸다\n2. B 가 열어 둔 화면에서 답변 "B 답변" 을 쓰고 [답변 보내기] 를 누른다',
     expect='1. 2번 응답이 409 INQUIRY_ALREADY_ANSWERED 다\n'
            '2. B 화면에 "이미 답변된 문의입니다." 가 보인다\n'
            '3. B 화면이 상세를 다시 불러 답변 본문 "A 답변" 과 보낸 관리자 A 를 보인다\n'
            '4. 문의 기록의 answer 가 "A 답변", answered_by 가 A 그대로다\n'
            '5. 고객 메일함에 "B 답변" 메일이 오지 않는다',
     judge=['서버', '가상', '가상', '서버', '합동'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries/{id}', pri='High'),

dict(id='IT-05-038', flow='답변 보내기', kind='예외', title='답변완료 문의는 미답변으로 되돌리거나 답변을 고칠 수 없다',
     pre='관리자 계정, 답변완료 문의 1건',
     steps='1. 그 문의 상세를 연다\n2. 답변 보내기(POST /admin/inquiries/{id}/answer)를 새 본문으로 부른다',
     expect='1. 상세에 답변을 고치는 단추와 미답변으로 되돌리는 단추가 없다\n'
            '2. 2번 응답이 409 INQUIRY_ALREADY_ANSWERED 다\n'
            '3. 문의 기록 상태가 ANSWERED, answer 가 처음 보낸 본문 그대로다',
     judge=['가상', '서버', '서버'],
     req=['BID-SUPPORT-003'], fc=['BID-SUPPORT-003'], screen='관리자 /inquiries/{id}', pri='High'),

dict(id='IT-05-039', flow='공지사항', kind='정상', title='메뉴로 공지사항을 열면 공지가 없을 때 빈 안내를 보인다',
     pre='비로그인, 화면 정의 파일의 공지 목록 0건',
     steps='1. 고객 화면 메뉴 고객센터 > 공지사항 을 누른다',
     expect='1. 주소가 /notices 이고 위치 줄이 홈, 고객센터, 공지사항 순서다\n'
            '2. 왼쪽 묶음 메뉴에서 공지사항 항목이 옅은 파랑 바탕과 왼쪽 파란 줄로 켜져 있다\n'
            '3. 머리에 제목 "공지사항" 과 "WJbid 서비스 소식과 점검 안내를 알려 드립니다" 가 보인다\n'
            '4. 표 머리가 번호, 제목, 날짜 순서다\n'
            '5. 표 안에 종 아이콘, "아직 공지가 없습니다", "새 소식이 생기면 이곳에 올립니다" 가 보인다\n'
            '6. 견본 공지 6건의 제목이 하나도 없다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-SUPPORT-004'], fc=['BID-SUPPORT-004'], screen='/notices', pri='Medium'),

dict(id='IT-05-040', flow='공지사항', kind='정상', title='홈 도움 구역 공지사항 탭이 공지사항 화면과 같은 목록을 읽는다',
     pre='비로그인, 화면 정의 파일의 공지 목록 0건',
     steps='1. 홈(/)의 도움 구역에서 공지사항 탭을 누른다',
     expect='1. 탭 상자에 "아직 공지가 없습니다" 가 보인다\n'
            '2. 탭 상자에 견본 공지 제목이 하나도 없다',
     judge=['가상', '가상'],
     req=['BID-SUPPORT-004'], fc=['BID-SUPPORT-004'], screen='/', pri='Medium'),

dict(id='IT-05-041', flow='공지사항', kind='정상', title='공지가 있으면 공지사항 화면과 홈 탭에 같은 공지가 번호, 제목, 날짜로 나온다',
     pre='비로그인, 화면 정의 파일 공지 목록에 공지 2건을 넣은 시험 환경',
     steps='1. /notices 를 연다\n2. 홈 도움 구역 공지사항 탭을 누른다',
     expect='1. 1번 표에 2행이 있고 줄마다 번호, 제목, 날짜가 넣은 값이다\n'
            '2. 1번에 "아직 공지가 없습니다" 가 없다\n'
            '3. 2번 탭 상자의 공지 제목 2개가 1번 표의 제목과 같다',
     judge=['가상', '가상', '가상'],
     req=['BID-SUPPORT-004'], fc=['BID-SUPPORT-004'], screen='/notices', pri='Medium'),

dict(id='IT-05-042', flow='자주 묻는 질문', kind='정상', title='메뉴로 자주 묻는 질문을 열면 전체 탭에 첫 질문만 펼쳐져 있다',
     pre='비로그인',
     steps='1. 고객 화면 메뉴 고객센터 > 자주 묻는 질문 을 누른다',
     expect='1. 주소가 /faq 이고 위치 줄이 홈, 고객센터, 자주 묻는 질문 순서다\n'
            '2. 머리에 "찾는 답이 없으면 1:1 문의로 남겨 주세요" 가 보인다\n'
            '3. 분류 탭이 전체, 가입·요금, 공고 보기, 등록·서류 순서이고 전체가 골라져 있다\n'
            '4. 질문 줄마다 Q 동그라미, 질문, 오른쪽 분류 이름과 꺾쇠가 있다\n'
            '5. 첫 질문만 펼쳐져 질문 글이 파랑이고 아래 A 동그라미와 답이 있다\n'
            '6. 둘째 질문부터는 답이 접혀 있다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-SUPPORT-005'], fc=['BID-SUPPORT-005'], screen='/faq', pri='Medium'),

dict(id='IT-05-043', flow='자주 묻는 질문', kind='정상', title='분류 탭을 바꾸면 그 분류 질문만 남고 펼침이 모두 접힌다',
     pre='비로그인',
     steps='1. /faq 에서 [가입·요금] 탭을 누른다',
     expect='1. 질문 줄 오른쪽 분류 이름이 모두 "가입·요금" 이다\n'
            '2. 펼쳐진 질문이 하나도 없다',
     judge=['가상', '가상'],
     req=['BID-SUPPORT-005'], fc=['BID-SUPPORT-005'], screen='/faq', pri='Medium'),

dict(id='IT-05-044', flow='자주 묻는 질문', kind='정상', title='질문 줄을 누르면 그 질문만 펼침과 접힘이 바뀐다',
     pre='비로그인, /faq 전체 탭에 첫 질문이 펼쳐진 상태',
     steps='1. 셋째 질문 줄을 누른다\n2. 첫 질문 줄을 누른다',
     expect='1. 1번 뒤 셋째 질문 답이 펼쳐지고 첫 질문 답도 펼쳐진 채다\n'
            '2. 2번 뒤 첫 질문 답이 접히고 셋째 질문 답은 펼쳐진 채다',
     judge=['가상', '가상'],
     req=['BID-SUPPORT-005'], fc=['BID-SUPPORT-005'], screen='/faq', pri='Medium'),

dict(id='IT-05-045', flow='자주 묻는 질문', kind='정상', title='답 안 링크를 누르면 관련 화면으로 간다',
     pre='비로그인, 답에 링크가 있는 질문 1개',
     steps='1. /faq 에서 그 질문을 펼친다\n2. 답 안 파랑 굵은 링크를 누른다',
     expect='1. 링크 글자가 파랑 굵게 보인다\n'
            '2. 2번 뒤 링크 글이 가리키는 화면 주소로 바뀌고 그 화면 제목이 보인다',
     judge=['가상', '가상'],
     req=['BID-SUPPORT-005'], fc=['BID-SUPPORT-005'], screen='/faq', pri='Medium'),

dict(id='IT-05-046', flow='자주 묻는 질문', kind='정상', title='홈 도움 구역 자주 묻는 질문 탭이 자주 묻는 질문 화면과 같은 목록을 읽는다',
     pre='비로그인',
     steps='1. 홈(/) 도움 구역에서 자주 묻는 질문 탭을 본다\n2. /faq 전체 탭 질문을 본다',
     expect='1. 1번 탭 상자의 질문 글이 2번 전체 탭 앞쪽 질문과 같은 글이고 같은 순서다',
     judge=['가상'],
     req=['BID-SUPPORT-005'], fc=['BID-SUPPORT-005'], screen='/', pri='Medium'),

dict(id='IT-05-047', flow='자주 묻는 질문', kind='정상', title='답에 지금 없는 기능을 있는 것처럼 쓰지 않는다',
     pre='비로그인',
     steps='1. /faq 전체 탭의 질문을 하나씩 모두 펼쳐 답을 읽는다',
     expect='1. 답 어디에도 "무료 입찰 교실", "재발주 예측 달력" 처럼 폐기되었거나 없는 기능을 쓸 수 있다는 글이 없다\n'
            '2. SAM.gov 등록 비용과 기간 같은 사실을 적은 답마다 공식 안내와 같은 값이다',
     judge=['QA', 'QA'],
     req=['BID-SUPPORT-005'], fc=['BID-SUPPORT-005'], screen='/faq', pri='Medium'),

]
