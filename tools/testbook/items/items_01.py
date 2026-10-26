# 영역 01 인증/회원 시험항목. 근거는 T1-1 기능요구사항 명세서(BID-AUTH-001~008), T1-3 BIZ-008·062·063·064·071·072, T1-4 POL-008·037·038, T3-2 A·K, T3-3 로그인·회원가입·회원 전용 화면·회원 관리, T3-4 로그인·회원가입·공고문 회원 전용 이다.

ITEMS = [

dict(id='IT-01-001', flow='이메일 가입', kind='정상', title='인증을 마친 이메일로 회원가입한다',
     pre='비로그인, 가입되지 않은 이메일 주소(받은 편지함을 볼 수 있는 것)',
     steps='1. 고객 화면 유틸 띠에서 [회원가입] 을 누른다\n'
           '2. 이메일을 넣고 [인증번호 받기] 를 누른 뒤 메일로 받은 6자리를 넣고 [확인] 을 누른다\n'
           '3. 비밀번호 8자 이상, 비밀번호 확인, 회사명, 사업자등록번호, 담당자명, 전화번호를 넣는다\n'
           '4. [필수] 이용약관, [필수] 개인정보처리방침에 체크하고 [무료로 가입하기] 를 누른다',
     expect='1. 가입 단추 글자가 "가입 처리 중" 으로 바뀌고 다시 누를 수 없다\n'
            '2. 로그인 화면(/login)으로 가고 로그인 상자에 초록 줄 "가입이 끝났습니다. 가입한 이메일로 로그인해 주세요." 가 보인다\n'
            '3. 회원 기록이 그 이메일, 역할 CUSTOMER 로 1건 생기고 넣은 회사명, 사업자등록번호, 담당자명, 전화번호가 저장된다\n'
            '4. 가입 응답(POST /auth/register)에 JWT 토큰과 회원 정보가 담긴다\n'
            '5. 회원 기록의 이메일 확인 시각(email_verified_at)이 가입 시각으로 채워진다\n'
            '6. 그 이메일의 인증번호 기록이 가입에 쓴 것으로 바뀐다',
     judge=['가상', '가상', '서버', '서버', '서버', '서버'],
     req=['BID-AUTH-001', 'BID-AUTH-005'], fc=['BID-AUTH-001', 'BID-AUTH-005'], screen='/register', pri='High',
     issue='T1-1 출력은 JWT 토큰인데 T3-4 시나리오 1 은 가입 뒤 로그인 화면으로 보내 다시 로그인하게 한다'),

dict(id='IT-01-002', flow='이메일 가입', kind='정상', title='회원가입 화면의 구역과 칸을 본다',
     pre='비로그인, 네이버와 카카오 키가 설정되고 요금 조회에 회원 월 금액이 있는 환경',
     steps='1. 회원가입 화면(/register)을 연다',
     expect='1. 가입 상자 맨 위에 "SNS 로 간편 가입" 과 SNS 단추가 있고 그 아래 "또는 이메일로 가입" 구분선이 있다\n'
            '2. 가입 상자 머리에 제목 "회원가입" 과 "무료 회원으로 가입하면 마감 전 공고를 한글로 보고 관심 공고를 저장할 수 있습니다" 가 보인다\n'
            '3. 1 계정 구역에 이메일 칸과 그 옆 테두리 단추 "인증번호 받기", 아래 "8자 이상" 이 붙은 비밀번호 칸, 비밀번호 확인 칸이 있다\n'
            '4. 2 회사 정보 구역에 회사명, 사업자등록번호, 담당자명, 전화번호 칸이 있고 주소 칸은 없다\n'
            '5. 3 약관 동의 구역에 전체 동의, [필수] 이용약관, [필수] 개인정보처리방침, [선택] 새 공고·서비스 소식 받기 체크가 있다\n'
            '6. 가입 단추 글자가 "무료로 가입하기" 다\n'
            '7. 오른쪽 첫걸음 상자에 꼬리표 "가입하면 바로", 무료 회원 혜택 3가지, "이미 회원이신가요? 로그인" 이 있다\n'
            '8. 첫걸음 상자 회원 금액 한 줄의 금액이 요금 조회(GET /pricing)의 회원 월 금액과 같다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-AUTH-001'], fc=['BID-AUTH-001'], screen='/register', pri='High'),

dict(id='IT-01-003', flow='이메일 가입', kind='정상', title='다른 화면의 단추로 회원가입과 로그인을 오간다',
     pre='비로그인',
     steps='1. 로그인 화면(/login) 오른쪽 첫걸음 상자에서 [무료 회원가입] 을 누른다\n'
           '2. 홈(/) 첫걸음 상자에서 [무료로 시작] 을 누른다\n'
           '3. 회원가입 화면 첫걸음 상자의 [로그인] 을 누른다',
     expect='1. 1번 뒤 회원가입 화면(/register)이 열려 제목 "회원가입" 이 보인다\n'
            '2. 2번 뒤 회원가입 화면(/register)이 열려 제목 "회원가입" 이 보인다\n'
            '3. 3번 뒤 로그인 화면(/login)이 열려 제목 "로그인" 이 보인다',
     judge=['가상', '가상', '가상'],
     req=['BID-AUTH-001', 'BID-AUTH-002'], fc=['BID-AUTH-001', 'BID-AUTH-002'], screen='/register', pri='High'),

dict(id='IT-01-004', flow='이메일 가입', kind='예외', title='비밀번호가 8자 미만이면 가입을 받지 않는다',
     pre='비로그인, 이메일 인증을 마친 회원가입 화면',
     steps='1. 비밀번호와 비밀번호 확인에 같은 7자를 넣고 나머지 칸과 필수 약관을 채운다\n'
           '2. [무료로 가입하기] 를 누른다\n'
           '3. 비밀번호와 비밀번호 확인을 같은 8자로 고친다',
     expect='1. 비밀번호 칸이 빨간 테두리가 되고 칸 아래에 8자 미만이라는 이유 한 줄이 보인다\n'
            '2. 단추 위에 "빨간 칸 1곳을 고쳐 주세요." 가 보이고 화면이 비밀번호 칸으로 옮겨진다\n'
            '3. 가입 요청이 서버로 가지 않아 그 이메일의 회원 기록이 생기지 않는다\n'
            '4. 8자로 고치면 비밀번호 칸의 빨간 테두리와 이유 줄이 사라진다',
     judge=['가상', '가상', '서버', '가상'],
     req=['BID-AUTH-001'], fc=['BID-AUTH-001'], screen='/register', pri='High'),

dict(id='IT-01-005', flow='이메일 가입', kind='예외', title='비밀번호 확인이 다르면 가입을 받지 않는다',
     pre='비로그인, 이메일 인증을 마친 회원가입 화면',
     steps='1. 비밀번호에 8자, 비밀번호 확인에 다른 8자를 넣고 나머지 칸과 필수 약관을 채운다\n'
           '2. [무료로 가입하기] 를 누른다',
     expect='1. 비밀번호 확인 칸이 빨간 테두리가 되고 칸 아래에 비밀번호와 다르다는 이유 한 줄이 보인다\n'
            '2. 단추 위에 "빨간 칸 1곳을 고쳐 주세요." 가 보인다\n'
            '3. 가입 요청이 서버로 가지 않아 그 이메일의 회원 기록이 생기지 않는다',
     judge=['가상', '가상', '서버'],
     req=['BID-AUTH-001'], fc=['BID-AUTH-001'], screen='/register', pri='High'),

dict(id='IT-01-006', flow='이메일 가입', kind='예외', title='필수 약관에 동의하지 않으면 가입을 받지 않는다',
     pre='비로그인, 이메일 인증을 마친 회원가입 화면',
     steps='1. 모든 칸을 바르게 채우고 [필수] 이용약관만 체크한다\n'
           '2. [무료로 가입하기] 를 누른다',
     expect='1. [필수] 개인정보처리방침 줄이 빨간 표시가 되고 아래에 필수 약관 미동의라는 이유 한 줄이 보인다\n'
            '2. 단추 위에 "빨간 칸 1곳을 고쳐 주세요." 가 보인다\n'
            '3. 가입 요청이 서버로 가지 않아 그 이메일의 회원 기록이 생기지 않는다',
     judge=['가상', '가상', '서버'],
     req=['BID-AUTH-001'], fc=['BID-AUTH-001'], screen='/register', pri='High'),

dict(id='IT-01-007', flow='이메일 가입', kind='예외', title='이메일 인증 전에 가입을 누르면 막는다',
     pre='비로그인, 회원가입 화면(인증번호를 받지 않음)',
     steps='1. 이메일, 비밀번호, 회사 정보를 넣고 필수 약관을 체크한다\n'
           '2. 인증번호 확인 없이 [무료로 가입하기] 를 누른다',
     expect='1. 이메일 칸 아래에 "이메일 인증을 마쳐 주세요" 가 보인다\n'
            '2. 그 이메일의 회원 기록이 생기지 않는다',
     judge=['가상', '서버'],
     req=['BID-AUTH-001', 'BID-AUTH-005'], fc=['BID-AUTH-001', 'BID-AUTH-005'], screen='/register', pri='High'),

dict(id='IT-01-008', flow='이메일 가입', kind='예외', title='인증하지 않은 이메일로 가입 API 를 바로 부르면 서버가 막는다',
     pre='비로그인, 인증번호를 확인하지 않은 이메일',
     steps='1. POST /auth/register 에 그 이메일, 8자 이상 비밀번호, 회사 정보, 필수 약관 동의를 넣어 부른다',
     expect='1. 응답이 400 과 error EMAIL_NOT_VERIFIED 다\n'
            '2. 그 이메일의 회원 기록이 생기지 않는다',
     judge=['서버', '서버'],
     req=['BID-AUTH-001', 'BID-AUTH-005'], fc=['BID-AUTH-001', 'BID-AUTH-005'], screen='', pri='High'),

dict(id='IT-01-009', flow='이메일 가입', kind='예외', title='인증을 마친 사이 다른 곳에서 같은 이메일이 가입되면 가입을 받지 않는다',
     pre='비로그인, 가입되지 않은 이메일 하나와 회원가입 화면 두 창(창 A, 창 B)',
     steps='1. 창 A 에서 그 이메일로 인증번호를 받고 확인을 마친다\n'
           '2. 창 B 에서 같은 이메일로 인증번호를 받아 확인하고 가입을 마친다\n'
           '3. 창 A 에서 나머지 칸을 채우고 [무료로 가입하기] 를 누른다\n'
           '4. 이메일 칸 아래 [로그인하기] 를 누른다',
     expect='1. 창 A 이메일 칸이 빨간 테두리가 되고 칸 아래에 "이미 가입된 이메일입니다." 와 [로그인하기] 링크가 보인다\n'
            '2. 창 A 의 가입 응답이 409 와 error EMAIL_ALREADY_EXISTS 다\n'
            '3. 그 이메일의 회원 기록은 창 B 에서 만든 1건뿐이다\n'
            '4. [로그인하기] 를 누르면 로그인 화면(/login)이 열린다',
     judge=['가상', '서버', '서버', '가상'],
     req=['BID-AUTH-001'], fc=['BID-AUTH-001'], screen='/register', pri='High'),

dict(id='IT-01-010', flow='이메일 가입', kind='예외', title='가입 중 그 밖의 서버 오류는 정해진 문구로 알린다',
     pre='비로그인, 이메일 인증을 마친 회원가입 화면, 가입 요청이 500 으로 실패하게 한 환경',
     steps='1. 모든 칸을 채우고 [무료로 가입하기] 를 누른다',
     expect='1. 단추 위에 빨간 줄 "가입을 마치지 못했습니다. 잠시 뒤 다시 시도해 주세요." 가 보인다\n'
            '2. 서버가 보낸 오류 문장은 화면 어디에도 없다',
     judge=['가상', '가상'],
     req=['BID-AUTH-001'], fc=['BID-AUTH-001'], screen='/register', pri='High'),

dict(id='IT-01-011', flow='이메일 인증번호', kind='정상', title='가입하려는 이메일로 인증번호를 받는다',
     pre='비로그인, 가입되지 않은 이메일, POL-037 처음 값(유효 10분, 다시 받기 60초)',
     steps='1. 회원가입 화면(/register)에서 이메일을 넣고 [인증번호 받기] 를 누른다\n'
           '2. 받은 편지함을 연다',
     expect='1. 누른 동안 단추 글자가 "보내는 중" 으로 바뀐다\n'
            '2. 이메일 칸 아래에 인증번호 칸, 남은 시간, [확인], [인증번호 다시 받기], "메일이 오지 않으면 스팸함을 확인해 주세요" 가 보인다\n'
            '3. 남은 시간이 10분에서 1초씩 줄어든다\n'
            '4. [인증번호 다시 받기] 가 받은 뒤 60초 동안 눌리지 않는다\n'
            '5. 보내기 응답(POST /auth/email-codes)이 200 과 expiresInSeconds 600, resendAfterSeconds 60 이다\n'
            '6. 받은 편지함에 6자리 인증번호가 든 메일이 온다\n'
            '7. 알림 서버 발송 기록의 틀이 BID_EMAIL_CODE 다\n'
            '8. 인증번호 기록에 보낸 6자리 숫자가 그대로 있지 않고 해시 값만 있다',
     judge=['가상', '가상', '가상', '가상', '서버', '합동', '서버', '서버'],
     req=['BID-AUTH-005'], fc=['BID-AUTH-005'], screen='/register', pri='High'),

dict(id='IT-01-012', flow='이메일 인증번호', kind='정상', title='맞는 인증번호를 넣어 이메일을 확인한다',
     pre='비로그인, 인증번호를 받은 회원가입 화면',
     steps='1. 메일로 받은 6자리를 인증번호 칸에 넣고 [확인] 을 누른다\n'
           '2. 이메일 칸을 눌러 글자를 고쳐 본다',
     expect='1. 초록 줄 "이메일이 확인되었습니다" 가 보인다\n'
            '2. 이메일 칸이 잠겨 글자를 고칠 수 없다\n'
            '3. 이메일 칸 옆에 [이메일 바꾸기] 가 보인다\n'
            '4. 확인 응답(POST /auth/email-codes/verify)이 200 이다',
     judge=['가상', '가상', '가상', '서버'],
     req=['BID-AUTH-005'], fc=['BID-AUTH-005'], screen='/register', pri='High'),

dict(id='IT-01-013', flow='이메일 인증번호', kind='정상', title='이메일 바꾸기로 확인을 지우고 다른 주소를 다시 인증한다',
     pre='비로그인, 이메일 확인을 마친 회원가입 화면',
     steps='1. [이메일 바꾸기] 를 누른다\n'
           '2. 다른 이메일을 넣고 확인 없이 나머지 칸을 채워 [무료로 가입하기] 를 누른다\n'
           '3. [인증번호 받기] 를 누른다',
     expect='1. 초록 줄 "이메일이 확인되었습니다" 가 사라지고 이메일 칸이 다시 열려 고칠 수 있다\n'
            '2. 2번 뒤 이메일 칸 아래에 "이메일 인증을 마쳐 주세요" 가 보이고 회원 기록이 생기지 않는다\n'
            '3. 3번 뒤 새 이메일로 인증번호 칸과 남은 시간이 다시 보인다',
     judge=['가상', '가상', '가상'],
     req=['BID-AUTH-005'], fc=['BID-AUTH-005'], screen='/register', pri='High'),

dict(id='IT-01-014', flow='이메일 인증번호', kind='정상', title='다시 받기 간격이 지나면 인증번호를 다시 받는다',
     pre='비로그인, 가입되지 않은 이메일, POL-037 처음 값',
     steps='1. [인증번호 받기] 를 누르고 60초를 기다린다\n'
           '2. [인증번호 다시 받기] 를 누른다\n'
           '3. 새로 받은 6자리를 넣고 [확인] 을 누른다',
     expect='1. 60초가 지나면 [인증번호 다시 받기] 가 눌린다\n'
            '2. 다시 받으면 남은 시간이 10분부터 다시 줄어든다\n'
            '3. 받은 편지함에 새 6자리 인증번호 메일이 한 통 더 온다\n'
            '4. 새 번호로 확인하면 초록 줄 "이메일이 확인되었습니다" 가 보인다',
     judge=['가상', '가상', '합동', '가상'],
     req=['BID-AUTH-005'], fc=['BID-AUTH-005'], screen='/register', pri='High'),

dict(id='IT-01-015', flow='이메일 인증번호', kind='정상', title='인증번호 설정값을 DB 에서 바꾸고, 비면 처음 값을 쓴다',
     pre='비로그인, 가입되지 않은 이메일 둘, system_settings 를 고칠 수 있는 환경',
     steps='1. system_settings 의 EMAIL_CODE_TTL_MINUTES 와 EMAIL_CODE_RESEND_SECONDS 를 비운다\n'
           '2. POST /auth/email-codes 에 첫 이메일을 넣어 부른다\n'
           '3. EMAIL_CODE_TTL_MINUTES 를 5, EMAIL_CODE_RESEND_SECONDS 를 30 으로 바꾼다\n'
           '4. POST /auth/email-codes 에 둘째 이메일을 넣어 부른다',
     expect='1. 2번 응답의 expiresInSeconds 가 600 이고 resendAfterSeconds 가 60 이다\n'
            '2. 4번 응답의 expiresInSeconds 가 300 이다\n'
            '3. 4번 응답의 resendAfterSeconds 가 30 이다',
     judge=['서버', '서버', '서버'],
     req=['BID-AUTH-005'], fc=['BID-AUTH-005'], screen='', pri='High'),

dict(id='IT-01-016', flow='이메일 인증번호', kind='예외', title='이메일 형식이 틀리면 인증번호를 보내지 않는다',
     pre='비로그인, 회원가입 화면',
     steps='1. 이메일 칸에 "abc@" 를 넣고 [인증번호 받기] 를 누른다',
     expect='1. 이메일 칸 아래에 "이메일 형식이 맞지 않습니다" 가 보인다\n'
            '2. 보내기 요청이 서버로 가지 않아 인증번호 기록이 생기지 않는다',
     judge=['가상', '서버'],
     req=['BID-AUTH-005'], fc=['BID-AUTH-005'], screen='/register', pri='High'),

dict(id='IT-01-017', flow='이메일 인증번호', kind='예외', title='이미 가입된 이메일에는 인증번호를 보내지 않는다',
     pre='비로그인, 이미 가입된 회원의 이메일',
     steps='1. 회원가입 화면에서 그 이메일을 넣고 [인증번호 받기] 를 누른다',
     expect='1. 이메일 칸 아래에 "이미 가입된 이메일입니다." 와 [로그인하기] 링크가 보인다\n'
            '2. 보내기 응답이 409 와 error EMAIL_ALREADY_EXISTS 다\n'
            '3. 그 이메일로 인증번호 기록과 알림 서버 발송 기록이 생기지 않는다',
     judge=['가상', '서버', '서버'],
     req=['BID-AUTH-005', 'BID-AUTH-001'], fc=['BID-AUTH-005', 'BID-AUTH-001'], screen='/register', pri='High'),

dict(id='IT-01-018', flow='이메일 인증번호', kind='예외', title='다시 받기 간격 전에 다시 보내면 받지 않는다',
     pre='비로그인, 가입되지 않은 이메일, POL-037 처음 값',
     steps='1. [인증번호 받기] 를 누른다\n'
           '2. 60초가 지나기 전에 화면을 새로고침하고 같은 이메일로 [인증번호 받기] 를 다시 누른다',
     expect='1. "잠시 뒤 다시 받을 수 있습니다" 가 보인다\n'
            '2. 보내기 응답이 429 와 error EMAIL_CODE_TOO_SOON, retryAfterSeconds 다\n'
            '3. [인증번호 다시 받기] 가 retryAfterSeconds 초가 지난 뒤에 눌린다\n'
            '4. 2번에서 새 인증번호 메일이 오지 않는다',
     judge=['가상', '서버', '가상', '합동'],
     req=['BID-AUTH-005'], fc=['BID-AUTH-005'], screen='/register', pri='High'),

dict(id='IT-01-019', flow='이메일 인증번호', kind='예외', title='메일 하나에 하루 보낼 수 있는 횟수를 넘으면 보내지 않는다',
     pre='비로그인, 오늘(서울 날짜) 인증번호를 10번 받은 가입되지 않은 이메일',
     steps='1. 회원가입 화면에서 그 이메일로 [인증번호 받기] 를 누른다',
     expect='1. "오늘 받을 수 있는 횟수를 넘었습니다. 내일 다시 시도해 주세요." 가 보인다\n'
            '2. 보내기 응답이 429 와 error EMAIL_CODE_DAILY_LIMIT 다\n'
            '3. 그 이메일의 인증번호 기록이 10건 그대로다',
     judge=['가상', '서버', '서버'],
     req=['BID-AUTH-005'], fc=['BID-AUTH-005'], screen='/register', pri='High'),

dict(id='IT-01-020', flow='이메일 인증번호', kind='예외', title='메일 발송이 실패하면 보내지 못했다고 알린다',
     pre='비로그인, 가입되지 않은 이메일, 알림 서버 발송이 실패하게 한 환경',
     steps='1. 회원가입 화면에서 [인증번호 받기] 를 누른다',
     expect='1. "인증번호를 보내지 못했습니다. 잠시 뒤 다시 시도해 주세요." 가 보인다\n'
            '2. 보내기 응답이 502 와 error EMAIL_SEND_FAILED 다',
     judge=['가상', '단위'],
     req=['BID-AUTH-005'], fc=['BID-AUTH-005'], screen='/register', pri='High'),

dict(id='IT-01-021', flow='이메일 인증번호', kind='예외', title='틀린 인증번호는 남은 횟수와 함께 거부한다',
     pre='비로그인, 인증번호를 받은 회원가입 화면, POL-037 처음 값(틀릴 수 있는 횟수 5)',
     steps='1. 받은 번호와 다른 6자리를 넣고 [확인] 을 누른다',
     expect='1. 인증번호 칸이 빨간 테두리가 되고 "인증번호가 맞지 않습니다. 4번 더 넣을 수 있습니다" 가 보인다\n'
            '2. 확인 응답이 400 과 error EMAIL_CODE_MISMATCH, remainingAttempts 4 다\n'
            '3. 초록 줄 "이메일이 확인되었습니다" 는 나오지 않고 이메일 칸은 열려 있다',
     judge=['가상', '서버', '가상'],
     req=['BID-AUTH-005'], fc=['BID-AUTH-005'], screen='/register', pri='High'),

dict(id='IT-01-022', flow='이메일 인증번호', kind='예외', title='유효 시간이 지난 인증번호는 받지 않는다',
     pre='비로그인, 인증번호를 받고 10분이 지난 회원가입 화면',
     steps='1. 남은 시간이 0 이 될 때까지 기다린다\n'
           '2. POST /auth/email-codes/verify 에 그 이메일과 받은 번호를 넣어 부른다',
     expect='1. 남은 시간이 0 이 되면 "인증 시간이 지났습니다. 다시 받아 주세요" 가 보인다\n'
            '2. [확인] 이 눌리지 않고 [인증번호 다시 받기] 가 눌린다\n'
            '3. 확인 응답이 410 과 error EMAIL_CODE_EXPIRED 다',
     judge=['가상', '가상', '서버'],
     req=['BID-AUTH-005'], fc=['BID-AUTH-005'], screen='/register', pri='High'),

dict(id='IT-01-023', flow='이메일 인증번호', kind='예외', title='틀린 횟수를 넘으면 그 번호는 맞아도 쓰지 못한다',
     pre='비로그인, 인증번호를 받은 회원가입 화면, POL-037 처음 값(틀릴 수 있는 횟수 5)',
     steps='1. 틀린 6자리로 [확인] 을 5번 누른다\n'
           '2. 맞는 번호를 넣고 [확인] 을 누른다\n'
           '3. [인증번호 다시 받기] 로 새 번호를 받아 넣고 [확인] 을 누른다',
     expect='1. 2번 뒤 "틀린 횟수를 넘었습니다. 인증번호를 다시 받아 주세요" 가 보인다\n'
            '2. 2번 확인 응답이 429 와 error EMAIL_CODE_LOCKED 다\n'
            '3. 3번 뒤 초록 줄 "이메일이 확인되었습니다" 가 보인다',
     judge=['가상', '서버', '가상'],
     req=['BID-AUTH-005'], fc=['BID-AUTH-005'], screen='/register', pri='High'),

dict(id='IT-01-024', flow='이메일 인증번호', kind='예외', title='확인 뒤 가입까지 남는 시간이 지나면 다시 인증하게 한다',
     pre='비로그인, 이메일 확인을 마치고 30분이 지난 회원가입 화면, POL-037 처음 값',
     steps='1. 나머지 칸과 필수 약관을 채우고 [무료로 가입하기] 를 누른다',
     expect='1. 이메일 칸 아래에 "이메일 인증을 다시 해 주세요" 가 보이고 화면이 이메일 칸으로 옮겨진다\n'
            '2. 초록 줄 "이메일이 확인되었습니다" 가 사라지고 이메일 칸이 다시 열린다\n'
            '3. 가입 응답이 400 과 error EMAIL_NOT_VERIFIED 다\n'
            '4. 그 이메일의 회원 기록이 생기지 않는다',
     judge=['가상', '가상', '서버', '서버'],
     req=['BID-AUTH-005', 'BID-AUTH-001'], fc=['BID-AUTH-005', 'BID-AUTH-001'], screen='/register', pri='High'),

dict(id='IT-01-025', flow='이메일 인증번호', kind='예외', title='확인 중 그 밖의 오류와 6자리 미만 입력을 막는다',
     pre='비로그인, 인증번호를 받은 회원가입 화면, 확인 요청이 500 으로 실패하게 한 환경',
     steps='1. 인증번호 칸에 5자리만 넣는다\n'
           '2. 6자리를 채우고 [확인] 을 누른다',
     expect='1. 5자리일 때 [확인] 이 눌리지 않는다\n'
            '2. 2번 뒤 "인증번호를 확인하지 못했습니다. 잠시 뒤 다시 시도해 주세요." 가 보인다',
     judge=['가상', '가상'],
     req=['BID-AUTH-005'], fc=['BID-AUTH-005'], screen='/register', pri='High'),

dict(id='IT-01-026', flow='로그인', kind='정상', title='이메일과 비밀번호로 로그인한다',
     pre='이메일로 가입한 무료회원 계정(로그아웃 상태)',
     steps='1. 고객 화면 유틸 띠에서 [로그인] 을 누른다\n'
           '2. 가입한 이메일과 비밀번호를 넣고 [로그인] 을 누른다',
     expect='1. 로그인되어 공고 검색(/search)이 열린다\n'
            '2. 유틸 띠 오른쪽에 "{회사명} 님", [로그아웃], 고객센터가 보이고 회원가입과 로그인은 없다\n'
            '3. 로그인 응답(POST /auth/login)에 Access Token 과 Refresh Token 이 담긴다\n'
            '4. Access Token 의 만료 시각이 발급 시각에서 15분 뒤다\n'
            '5. Refresh Token 의 만료 시각이 발급 시각에서 7일 뒤다',
     judge=['가상', '가상', '서버', '서버', '서버'],
     req=['BID-AUTH-002'], fc=['BID-AUTH-002'], screen='/login', pri='High'),

dict(id='IT-01-027', flow='로그인', kind='정상', title='로그인 화면의 상자와 비밀번호 찾기 안내를 본다',
     pre='비로그인, 네이버와 카카오 키가 설정되고 요금 조회에 회원 월 금액이 있는 환경',
     steps='1. 로그인 화면(/login)을 연다\n'
           '2. 비밀번호 찾기 안내의 고객센터 이메일을 누른다',
     expect='1. 왼쪽 로그인 상자에 제목 "로그인", 안내 한 줄, 이메일 칸, 비밀번호 칸, [로그인] 이 있다\n'
            '2. [로그인] 아래에 "또는" 구분선과 SNS 단추가 있다\n'
            '3. 로그인 상자 아래에 "비밀번호를 잊으셨나요?" 와 고객센터 이메일로 가입 이메일을 보내 달라는 한 줄이 있다\n'
            '4. 그 고객센터 이메일이 꼬리말의 이메일과 같다\n'
            '5. 오른쪽 첫걸음 상자에 꼬리표 "USFK 입찰 첫걸음", "아직 회원이 아니신가요? 가입은 무료입니다", 무료 회원 혜택 3가지, 테두리 단추 [무료 회원가입] 이 있다\n'
            '6. 첫걸음 상자 회원 금액 한 줄의 금액이 요금 조회(GET /pricing)의 회원 월 금액과 같다\n'
            '7. 2번 뒤 메일 쓰기 창이 받는 사람 칸에 그 고객센터 이메일을 넣고 열린다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '합동'],
     req=['BID-AUTH-002'], fc=['BID-AUTH-002'], screen='/login', pri='High'),

dict(id='IT-01-028', flow='로그인', kind='예외', title='요금 조회를 못 받아도 로그인과 가입 화면의 첫걸음 상자를 그린다',
     pre='비로그인, GET /pricing 이 실패하게 한 환경',
     steps='1. 로그인 화면(/login)을 연다\n'
           '2. 회원가입 화면(/register)을 연다',
     expect='1. 로그인 화면 첫걸음 상자의 금액 자리에 "요금 안내 준비 중" 이 보인다\n'
            '2. 로그인 화면의 이메일 칸, 비밀번호 칸, [로그인] 은 그대로 있다\n'
            '3. 회원가입 화면 첫걸음 상자의 금액 자리에 "요금 안내 준비 중" 이 보인다',
     judge=['가상', '가상', '가상'],
     req=['BID-AUTH-002', 'BID-AUTH-001'], fc=['BID-AUTH-002', 'BID-AUTH-001'], screen='/login', pri='High'),

dict(id='IT-01-029', flow='로그인', kind='예외', title='SNS 목록을 못 받으면 SNS 단추를 숨기고 이메일 로그인과 가입은 그대로 둔다',
     pre='비로그인, GET /auth/sns/providers 가 실패하게 한 환경',
     steps='1. 로그인 화면(/login)을 연다\n'
           '2. 회원가입 화면(/register)을 연다',
     expect='1. 로그인 화면에 "또는" 구분선과 SNS 단추가 없다\n'
            '2. 로그인 화면의 이메일 칸, 비밀번호 칸, [로그인] 은 그대로 있다\n'
            '3. 회원가입 화면에 "SNS 로 간편 가입" 과 SNS 단추와 "또는 이메일로 가입" 구분선이 없다\n'
            '4. 회원가입 화면의 이메일 칸과 [인증번호 받기] 는 그대로 있다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-AUTH-007', 'BID-AUTH-006'], fc=['BID-AUTH-007', 'BID-AUTH-006'], screen='/login', pri='High'),

dict(id='IT-01-030', flow='로그인', kind='예외', title='이메일이나 비밀번호가 틀리면 로그인되지 않는다',
     pre='이메일로 가입한 무료회원 계정(로그아웃 상태)',
     steps='1. 로그인 화면에서 맞는 이메일과 틀린 비밀번호로 [로그인] 을 누른다\n'
           '2. 가입되지 않은 이메일과 아무 비밀번호로 [로그인] 을 누른다',
     expect='1. 1번 뒤 로그인 상자에 빨간 줄 "이메일 또는 비밀번호가 맞지 않습니다. 다시 확인해 주세요." 가 보인다\n'
            '2. 이메일 칸에 넣은 이메일이 남는다\n'
            '3. 비밀번호 칸이 비고 커서가 비밀번호 칸에 있다\n'
            '4. 2번 뒤에도 같은 빨간 줄 "이메일 또는 비밀번호가 맞지 않습니다. 다시 확인해 주세요." 가 보인다\n'
            '5. 두 로그인 응답 모두 실패이고 토큰이 오지 않는다',
     judge=['가상', '가상', '가상', '가상', '서버'],
     req=['BID-AUTH-002'], fc=['BID-AUTH-002'], screen='/login', pri='High'),

dict(id='IT-01-031', flow='로그인', kind='예외', title='SNS 로만 가입한 회원은 비밀번호로 로그인되지 않는다',
     pre='카카오로만 가입한 회원 계정(비밀번호 없음)',
     steps='1. 로그인 화면에서 그 회원의 이메일과 아무 8자 비밀번호로 [로그인] 을 누른다',
     expect='1. 틀린 비밀번호와 같은 빨간 줄 "이메일 또는 비밀번호가 맞지 않습니다. 다시 확인해 주세요." 가 보인다\n'
            '2. SNS 로 가입했다는 말이나 SNS 로그인을 권하는 다른 문구가 없다\n'
            '3. 로그인 응답이 실패이고 토큰이 오지 않는다',
     judge=['가상', '가상', '서버'],
     req=['BID-AUTH-002', 'BID-AUTH-006'], fc=['BID-AUTH-002', 'BID-AUTH-006'], screen='/login', pri='High'),

dict(id='IT-01-032', flow='토큰 갱신', kind='정상', title='Refresh Token 으로 새 Access Token 을 받는다',
     pre='회원 계정으로 로그인해 받은 Refresh Token',
     steps='1. POST /auth/refresh 에 Refresh Token 을 넣어 부른다\n'
           '2. 받은 새 Access Token 으로 GET /members/me 를 부른다',
     expect='1. 새 Access Token 이 오고 로그인 때 받은 Access Token 과 값이 다르다\n'
            '2. 새 Access Token 의 만료 시각이 발급 시각에서 15분 뒤다\n'
            '3. GET /members/me 가 200 과 그 회원의 이메일을 준다',
     judge=['서버', '서버', '서버'],
     req=['BID-AUTH-003'], fc=['BID-AUTH-003'], screen='', pri='High'),

dict(id='IT-01-033', flow='토큰 갱신', kind='정상', title='Access Token 이 끝나도 화면이 토큰을 새로 받아 이어 간다',
     pre='회원 계정으로 로그인하고 15분이 지나 Access Token 만 끝난 상태, 공개 공고문 1건',
     steps='1. 그 공고문의 공고 상세(/search/{id})를 연다',
     expect='1. 로그인 화면으로 가지 않고 그 공고문의 공고 요약이 보인다\n'
            '2. 공고 조회가 401 LOGIN_REQUIRED 를 받은 뒤 POST /auth/refresh 가 불려 새 Access Token 이 발급된다',
     judge=['가상', '서버'],
     req=['BID-AUTH-003', 'BID-AUTH-008'], fc=['BID-AUTH-003', 'BID-AUTH-008'], screen='/search/{id}', pri='High'),

dict(id='IT-01-034', flow='토큰 갱신', kind='예외', title='만료된 Refresh Token 은 거부한다',
     pre='회원 계정, 7일이 지나 끝난 Refresh Token(유효기간을 줄인 환경에서 끝낸 것도 된다)',
     steps='1. POST /auth/refresh 에 끝난 Refresh Token 을 넣어 부른다\n'
           '2. Access Token 과 Refresh Token 이 모두 끝난 채로 고객 화면에서 공고 상세(/search/{id})를 연다',
     expect='1. 새 Access Token 을 주지 않고 실패 응답이 온다\n'
            '2. 2번 뒤 로그인 화면(/login)이 열린다',
     judge=['서버', '가상'],
     req=['BID-AUTH-003'], fc=['BID-AUTH-003'], screen='/search/{id}', pri='High'),

dict(id='IT-01-035', flow='로그아웃', kind='정상', title='로그아웃하면 세션이 끝나고 유틸 띠가 비로그인 모양으로 바뀐다',
     pre='무료회원 계정으로 로그인한 상태, 관심 공고 1건',
     steps='1. 유틸 띠에서 [로그아웃] 을 누른다\n'
           '2. 로그아웃 전에 받은 Access Token 으로 GET /members/me 를 부른다',
     expect='1. 유틸 띠 오른쪽이 회원가입(강조), 로그인, 이용 안내, 고객센터로 바뀐다\n'
            '2. 머리말 관심 공고 아이콘 위의 숫자 동그라미가 사라진다\n'
            '3. 로그아웃 요청이 성공 응답을 받는다\n'
            '4. 로그아웃 전 Access Token 으로 부른 GET /members/me 가 401 이다',
     judge=['가상', '가상', '서버', '서버'],
     req=['BID-AUTH-004'], fc=['BID-AUTH-004'], screen='/', pri='Medium',
     issue='T3-2 A 절에 로그아웃 API 가 없고 BID-AUTH-004 는 /members/me 에 이어져 있어 세션 무효화를 서버가 하는지 설계가 비었다'),

dict(id='IT-01-036', flow='SNS 가입', kind='정상', title='카카오가 확인한 이메일로 인증번호 없이 SNS 가입한다',
     pre='비로그인, 카카오 키 설정 환경, 우리 서비스에 이어지지 않은 카카오 계정(이메일 확인 표시를 주는 것), 그 이메일로 가입한 회원 없음',
     steps='1. 로그인 화면(/login)에서 카카오 단추를 누른다\n'
           '2. 카카오 로그인 창에서 허락한다\n'
           '3. SNS 가입 화면에서 회사명, 사업자등록번호, 담당자명, 전화번호를 넣고 필수 약관 두 가지에 체크한다\n'
           '4. [가입하고 시작하기] 를 누른다',
     expect='1. 1번 뒤 카카오 로그인 창이 열린다\n'
            '2. 허락 뒤 SNS 가입 화면(/register/sns)이 열려 머리에 "카카오로 가입" 과 "회사 정보만 넣으면 가입이 끝납니다" 가 보인다\n'
            '3. 1 이메일 구역에 카카오가 준 이메일이 "카카오에서 확인된 이메일" 로 보이고 인증번호 칸이 없다\n'
            '4. 비밀번호 칸이 없다\n'
            '5. [가입하고 시작하기] 를 누르면 단추 글자가 "가입 처리 중" 으로 바뀐다\n'
            '6. 가입 뒤 로그인된 채로 공고 검색(/search)이 열리고 유틸 띠에 "{회사명} 님" 이 보인다\n'
            '7. 회원 기록이 그 이메일로 1건 생기고 비밀번호가 비어 있다\n'
            '8. 회원 기록의 이메일 확인 시각이 채워진다\n'
            '9. 그 회원에 카카오 계정 연결 1건이 남는다\n'
            '10. 그 회원의 약관 동의 이력이 남는다',
     judge=['합동', '가상', '가상', '가상', '가상', '가상', '서버', '서버', '서버', '서버'],
     req=['BID-AUTH-006', 'BID-AUTH-007'], fc=['BID-AUTH-006', 'BID-AUTH-007'], screen='/register/sns', pri='High'),

dict(id='IT-01-037', flow='SNS 가입', kind='정상', title='SNS 확인이 없는 이메일이면 인증번호를 거쳐 네이버로 가입한다',
     pre='비로그인, 네이버 키 설정 환경, 우리 서비스에 이어지지 않은 네이버 계정(이메일 확인 표시가 없는 것)',
     steps='1. 회원가입 화면(/register)의 "SNS 로 간편 가입" 에서 네이버 단추를 누르고 허락한다\n'
           '2. SNS 가입 화면에서 [인증번호 받기] 로 받은 번호를 넣고 [확인] 을 누른다\n'
           '3. 회사 정보와 필수 약관을 채우고 [가입하고 시작하기] 를 누른다',
     expect='1. 1번 뒤 네이버 로그인 창을 거쳐 SNS 가입 화면(/register/sns)이 열리고 머리에 "네이버로 가입" 이 보인다\n'
            '2. 1 이메일 구역이 처음부터 이메일 칸과 [인증번호 받기] 가 있는 인증번호 흐름이다\n'
            '3. 2번 뒤 초록 줄 "이메일이 확인되었습니다" 가 보인다\n'
            '4. 가입 뒤 로그인된 채로 공고 검색(/search)이 열린다\n'
            '5. 회원 기록 1건과 그 회원의 네이버 계정 연결 1건이 생긴다',
     judge=['합동', '가상', '가상', '가상', '서버'],
     req=['BID-AUTH-006', 'BID-AUTH-005'], fc=['BID-AUTH-006', 'BID-AUTH-005'], screen='/register/sns', pri='High'),

dict(id='IT-01-038', flow='SNS 가입', kind='정상', title='SNS 가입에서 다른 이메일 쓰기로 인증번호 흐름으로 바꾼다',
     pre='비로그인, 카카오가 확인한 이메일이 채워진 SNS 가입 화면, 가입되지 않은 다른 이메일',
     steps='1. [다른 이메일 쓰기] 를 누른다\n'
           '2. 다른 이메일을 넣고 인증번호를 받아 확인한다\n'
           '3. 회사 정보와 필수 약관을 채우고 [가입하고 시작하기] 를 누른다',
     expect='1. "카카오에서 확인된 이메일" 표시가 사라지고 이메일 칸과 [인증번호 받기] 가 있는 인증번호 흐름으로 바뀐다\n'
            '2. 가입 뒤 회원 기록의 이메일이 새로 넣은 이메일이다',
     judge=['가상', '서버'],
     req=['BID-AUTH-006', 'BID-AUTH-005'], fc=['BID-AUTH-006', 'BID-AUTH-005'], screen='/register/sns', pri='High'),

dict(id='IT-01-039', flow='SNS 가입', kind='정상', title='키가 설정된 SNS 만 단추가 보인다',
     pre='비로그인, 네이버 키만 설정하고 카카오 키는 비운 환경',
     steps='1. GET /auth/sns/providers 를 부른다\n'
           '2. 로그인 화면(/login)을 연다\n'
           '3. 회원가입 화면(/register)을 연다',
     expect='1. 응답이 [NAVER] 하나다\n'
            '2. 로그인 화면 간편 로그인에 네이버 단추만 있고 카카오와 구글 단추는 없다\n'
            '3. 회원가입 화면 간편 가입에도 네이버 단추만 있다',
     judge=['서버', '가상', '가상'],
     req=['BID-AUTH-006', 'BID-AUTH-007'], fc=['BID-AUTH-006', 'BID-AUTH-007'], screen='/login', pri='High'),

dict(id='IT-01-040', flow='SNS 가입', kind='예외', title='유효 시간이 지난 SNS 가입 표로는 가입 화면을 열지 않는다',
     pre='비로그인, SNS 에서 돌아와 받은 /register/sns?ticket= 주소, 받은 뒤 30분이 지남(POL-038 처음 값)',
     steps='1. 그 주소를 연다\n'
           '2. [로그인] 을 누른다',
     expect='1. "다시 SNS 로 시작해 주세요" 와 [로그인] 단추가 보이고 회사 정보 칸은 없다\n'
            '2. GET /auth/sns/signup/{ticket} 응답이 410 과 error SNS_TICKET_EXPIRED 다\n'
            '3. 2번 뒤 로그인 화면(/login)이 열린다',
     judge=['가상', '서버', '가상'],
     req=['BID-AUTH-006'], fc=['BID-AUTH-006'], screen='/register/sns', pri='High'),

dict(id='IT-01-041', flow='SNS 가입', kind='예외', title='SNS 가입 화면을 연 채 표가 끝난 뒤 가입하면 받지 않는다',
     pre='비로그인, SNS 가입 화면을 열고 30분이 지난 상태',
     steps='1. 회사 정보와 필수 약관을 채우고 [가입하고 시작하기] 를 누른다',
     expect='1. "다시 SNS 로 시작해 주세요" 와 [로그인] 단추가 보인다\n'
            '2. 가입 응답(POST /auth/sns/register)이 410 과 error SNS_TICKET_EXPIRED 다\n'
            '3. 회원 기록과 SNS 연결이 생기지 않는다',
     judge=['가상', '서버', '서버'],
     req=['BID-AUTH-006'], fc=['BID-AUTH-006'], screen='/register/sns', pri='High'),

dict(id='IT-01-042', flow='SNS 가입', kind='예외', title='SNS 가입에서 넣은 이메일이 그 사이 가입되면 받지 않는다',
     pre='비로그인, SNS 가입 화면에서 [다른 이메일 쓰기] 로 이메일 X 를 인증한 상태',
     steps='1. 다른 창의 회원가입 화면에서 이메일 X 로 인증하고 가입을 마친다\n'
           '2. SNS 가입 화면으로 돌아와 회사 정보와 필수 약관을 채우고 [가입하고 시작하기] 를 누른다',
     expect='1. 이메일 칸이 빨간 테두리가 되고 칸 아래에 "이미 가입된 이메일입니다." 와 [로그인하기] 링크가 보인다\n'
            '2. 가입 응답이 409 와 error EMAIL_ALREADY_EXISTS 다\n'
            '3. 이메일 X 의 회원 기록은 1번에서 만든 1건뿐이고 SNS 연결이 생기지 않는다',
     judge=['가상', '서버', '서버'],
     req=['BID-AUTH-006'], fc=['BID-AUTH-006'], screen='/register/sns', pri='High'),

dict(id='IT-01-043', flow='SNS 가입', kind='예외', title='같은 SNS 계정이 다른 창에서 먼저 가입을 마치면 받지 않는다',
     pre='비로그인, 같은 카카오 계정으로 SNS 가입 화면을 두 창(창 A, 창 B)에 연 상태',
     steps='1. 창 A 에서 회사 정보와 필수 약관을 채우고 [가입하고 시작하기] 를 누른다\n'
           '2. 창 B 에서 회사 정보와 필수 약관을 채우고 [가입하고 시작하기] 를 누른다',
     expect='1. 창 B 단추 위에 빨간 줄 "이 SNS 계정은 이미 가입되어 있습니다. 로그인 화면에서 SNS 로 로그인해 주세요." 가 보인다\n'
            '2. 창 B 가입 응답이 409 와 error SNS_ACCOUNT_IN_USE 다\n'
            '3. 그 카카오 계정에 이어진 회원은 창 A 에서 만든 1명뿐이다',
     judge=['가상', '서버', '서버'],
     req=['BID-AUTH-006'], fc=['BID-AUTH-006'], screen='/register/sns', pri='High'),

dict(id='IT-01-044', flow='SNS 가입', kind='예외', title='SNS 이메일로 이미 이메일 가입한 회원이 있으면 잇지 않고 이메일 로그인을 안내한다',
     pre='비로그인, 이메일로 가입한 회원과 같은 이메일을 주는 카카오 계정(우리 서비스에 이어지지 않음)',
     steps='1. 로그인 화면에서 카카오 단추를 누르고 허락한다',
     expect='1. 로그인 화면(/login?sns=EMAIL_IN_USE)으로 돌아와 로그인 상자에 "이 이메일은 이미 가입되어 있습니다. 이메일과 비밀번호로 로그인해 주세요." 가 보인다\n'
            '2. 그 이메일 회원에 카카오 연결이 생기지 않는다\n'
            '3. 새 회원 기록이 생기지 않는다',
     judge=['가상', '서버', '서버'],
     req=['BID-AUTH-006', 'BID-AUTH-007'], fc=['BID-AUTH-006', 'BID-AUTH-007'], screen='/login', pri='High'),

dict(id='IT-01-045', flow='SNS 가입', kind='예외', title='SNS 확인이 없는 이메일로 인증 없이 SNS 가입 API 를 부르면 막는다',
     pre='비로그인, 이메일 확인 표시가 없는 SNS 가입 표, 인증번호를 확인하지 않은 이메일',
     steps='1. POST /auth/sns/register 에 그 표, 그 이메일, 회사 정보, 필수 약관 동의를 넣어 부른다',
     expect='1. 응답이 400 과 error EMAIL_NOT_VERIFIED 다\n'
            '2. 회원 기록과 SNS 연결이 생기지 않는다',
     judge=['서버', '서버'],
     req=['BID-AUTH-006'], fc=['BID-AUTH-006'], screen='', pri='High'),

dict(id='IT-01-046', flow='SNS 로그인', kind='정상', title='카카오로 가입한 회원이 카카오로 로그인한다',
     pre='카카오로 가입한 회원(로그아웃 상태), 로그인 전에 다른 화면을 보지 않음',
     steps='1. 고객 화면 유틸 띠에서 [로그인] 을 누른다\n'
           '2. 카카오 단추를 누르고 카카오 로그인 창에서 허락한다',
     expect='1. SNS 로그인 처리 화면(/auth/sns/done)에 "로그인하는 중" 한 줄만 보인다\n'
            '2. 로그인된 채로 공고 검색(/search)이 열리고 유틸 띠에 "{회사명} 님" 이 보인다\n'
            '3. POST /auth/sns/login 응답의 Access Token 만료가 발급에서 15분 뒤다\n'
            '4. 같은 응답의 Refresh Token 만료가 발급에서 7일 뒤다',
     judge=['가상', '가상', '서버', '서버'],
     req=['BID-AUTH-007'], fc=['BID-AUTH-007'], screen='/auth/sns/done', pri='High'),

dict(id='IT-01-047', flow='SNS 로그인', kind='정상', title='네이버로 로그인하면 로그인 전에 보던 화면으로 돌아간다',
     pre='네이버로 가입한 회원(로그아웃 상태)',
     steps='1. 비로그인으로 변경 공고(/bids/amendments)를 연다\n'
           '2. 유틸 띠에서 [로그인] 을 누르고 네이버 단추를 눌러 허락한다',
     expect='1. "로그인하는 중" 뒤에 변경 공고(/bids/amendments)가 열린다\n'
            '2. 유틸 띠에 "{회사명} 님" 이 보인다',
     judge=['가상', '가상'],
     req=['BID-AUTH-007'], fc=['BID-AUTH-007'], screen='/auth/sns/done', pri='High'),

dict(id='IT-01-048', flow='SNS 로그인', kind='예외', title='한 번 쓴 SNS 로그인 표는 다시 받지 않는다',
     pre='카카오로 가입한 회원, SNS 로그인을 마친 뒤 그때의 /auth/sns/done?ticket= 주소',
     steps='1. POST /auth/sns/login 에 쓴 표를 다시 넣어 부른다\n'
           '2. 그 /auth/sns/done?ticket= 주소를 로그아웃한 창에서 다시 연다',
     expect='1. 응답이 410 과 error SNS_TICKET_EXPIRED 이고 토큰이 오지 않는다\n'
            '2. 2번 뒤 로그인 화면에 "SNS 로그인을 마치지 못했습니다. 잠시 뒤 다시 시도해 주세요." 가 보인다',
     judge=['서버', '가상'],
     req=['BID-AUTH-007'], fc=['BID-AUTH-007'], screen='/auth/sns/done', pri='High'),

dict(id='IT-01-049', flow='SNS 로그인', kind='예외', title='유효 시간이 지난 SNS 로그인 표는 받지 않는다',
     pre='카카오로 가입한 회원, SNS 에서 돌아올 때 받은 쓰지 않은 로그인 표(네트워크 기록에서 얻음), POL-038 처음 값(60초)',
     steps='1. 표를 받고 61초를 기다린다\n'
           '2. POST /auth/sns/login 에 그 표를 넣어 부른다',
     expect='1. 응답이 410 과 error SNS_TICKET_EXPIRED 다\n'
            '2. 토큰이 오지 않는다',
     judge=['서버', '서버'],
     req=['BID-AUTH-007'], fc=['BID-AUTH-007'], screen='', pri='High'),

dict(id='IT-01-050', flow='SNS 로그인', kind='예외', title='SNS 로그인 창에서 취소하면 로그인 화면에 알린다',
     pre='카카오로 가입한 회원(로그아웃 상태)',
     steps='1. 로그인 화면에서 카카오 단추를 누른다\n'
           '2. 카카오 로그인 창에서 취소한다',
     expect='1. 로그인 화면(/login?sns=CANCELLED)으로 돌아와 로그인 상자에 "SNS 로그인을 취소했습니다." 가 보인다\n'
            '2. 유틸 띠에 [로그인] 이 그대로 있다',
     judge=['합동', '가상'],
     req=['BID-AUTH-007'], fc=['BID-AUTH-007'], screen='/login', pri='High'),

dict(id='IT-01-051', flow='SNS 로그인', kind='예외', title='SNS 로그인이 실패하면 로그인 화면에 알린다',
     pre='비로그인, 카카오 비밀키를 틀리게 넣은 환경',
     steps='1. 로그인 화면에서 카카오 단추를 누르고 허락한다\n'
           '2. 로그인 화면(/login?sns=FAILED)을 주소로 연다',
     expect='1. 1번 뒤 로그인 화면(/login?sns=FAILED)으로 돌아온다\n'
            '2. 로그인 상자에 "SNS 로그인을 마치지 못했습니다. 잠시 뒤 다시 시도해 주세요." 가 보인다',
     judge=['합동', '가상'],
     req=['BID-AUTH-007'], fc=['BID-AUTH-007'], screen='/login', pri='High'),

dict(id='IT-01-052', flow='회원 구분', kind='정상', title='오늘이 회원 기간 안인 고객은 회원으로 본다',
     pre='오늘(서울 날짜)을 품은 회원 기간 하나(끝일 E)가 있는 고객 계정, 공개 공고문 1건',
     steps='1. 고객 화면에 로그인한다\n'
           '2. 내 프로필(/profile)을 연다\n'
           '3. 요금 안내(/pricing)를 연다\n'
           '4. 그 공고문의 공고 상세(/search/{id})를 연다\n'
           '5. GET /members/me 를 부른다',
     expect='1. 내 프로필에 "회원, YYYY.MM.DD 까지" 꼴로 끝일 E 가 보인다\n'
            '2. 요금 안내 회원 단계에 "지금 단계" 가 붙고 무료 단계에는 없다\n'
            '3. 공고 상세에 그 공고문의 공고 요약과 [제안서 작성] 이 보인다\n'
            '4. GET /members/me 의 membership.member 가 true 다\n'
            '5. 같은 응답의 membership.endsOn 이 E(YYYY-MM-DD)다',
     judge=['가상', '가상', '가상', '서버', '서버'],
     req=['BID-AUTH-008'], fc=['BID-AUTH-008'], screen='/profile', pri='High'),

dict(id='IT-01-053', flow='회원 구분', kind='정상', title='회원 기간이 없는 고객은 무료회원으로 본다',
     pre='회원 기간이 하나도 없는 무료회원 계정, 공개 공고문 1건',
     steps='1. 고객 화면에 로그인한다\n'
           '2. 내 프로필(/profile)을 열고 요금 안내 연결을 누른다\n'
           '3. 그 공고문의 공고 상세(/search/{id})를 연다\n'
           '4. GET /members/me 를 부른다',
     expect='1. 내 프로필에 "무료회원" 과 요금 안내 연결이 보인다\n'
            '2. 요금 안내 연결을 누르면 요금 안내(/pricing)가 열리고 무료 단계에 "지금 단계" 가 붙는다\n'
            '3. 공고 상세에 공고문은 회원 전용이라는 회원 안내와 [요금 안내] 가 보이고 공고 요약은 없다\n'
            '4. GET /members/me 의 membership.member 가 false 다\n'
            '5. 같은 응답의 membership.endsOn 이 null 이다',
     judge=['가상', '가상', '가상', '서버', '서버'],
     req=['BID-AUTH-008'], fc=['BID-AUTH-008'], screen='/profile', pri='High'),

dict(id='IT-01-054', flow='회원 구분', kind='정상', title='관리자는 회원 기간 없이 회원과 같이 본다',
     pre='회원 기간이 없는 관리자 계정, 공개 공고문 1건',
     steps='1. 관리자 계정으로 고객 화면에 로그인한다\n'
           '2. 그 공고문의 공고 상세(/search/{id})를 연다\n'
           '3. 요금 안내(/pricing)를 연다\n'
           '4. GET /members/me 를 부른다',
     expect='1. 공고 상세에 그 공고문의 공고 요약이 보인다\n'
            '2. 요금 안내 회원 단계에 "지금 단계" 가 붙는다\n'
            '3. GET /members/me 의 membership.member 가 true 다\n'
            '4. 같은 응답의 membership.endsOn 이 null 이다',
     judge=['가상', '가상', '서버', '서버'],
     req=['BID-AUTH-008'], fc=['BID-AUTH-008'], screen='/search/{id}', pri='High'),

dict(id='IT-01-055', flow='회원 구분', kind='정상', title='회원 기간의 시작일과 끝일 당일은 회원이다',
     pre='시작일이 오늘(서울 날짜)인 기간만 있는 고객 A, 끝일이 오늘인 기간만 있는 고객 B',
     steps='1. A 로 GET /members/me 를 부르고 내 프로필을 연다\n'
           '2. B 로 GET /members/me 를 부르고 내 프로필을 연다',
     expect='1. A 의 membership.member 가 true 이고 endsOn 이 A 기간의 끝일이다\n'
            '2. A 의 내 프로필에 "회원, YYYY.MM.DD 까지" 로 A 기간의 끝일이 보인다\n'
            '3. B 의 membership.member 가 true 이고 endsOn 이 오늘이다\n'
            '4. B 의 내 프로필에 "회원, YYYY.MM.DD 까지" 로 오늘 날짜가 보인다',
     judge=['서버', '가상', '서버', '가상'],
     req=['BID-AUTH-008'], fc=['BID-AUTH-008'], screen='/profile', pri='High'),

dict(id='IT-01-056', flow='회원 구분', kind='정상', title='끝일 다음 날과 시작일 전은 무료회원이다',
     pre='끝일이 어제(서울 날짜)인 기간만 있는 고객 C, 시작일이 내일인 기간만 있는 고객 D',
     steps='1. C 로 GET /members/me 를 부르고 내 프로필을 연다\n'
           '2. D 로 GET /members/me 를 부른다',
     expect='1. C 의 membership.member 가 false 이고 endsOn 이 null 이다\n'
            '2. C 의 내 프로필에 "무료회원" 이 보인다\n'
            '3. D 의 membership.member 가 false 이고 endsOn 이 null 이다',
     judge=['서버', '가상', '서버'],
     req=['BID-AUTH-008'], fc=['BID-AUTH-008'], screen='/profile', pri='High'),

dict(id='IT-01-057', flow='회원 구분', kind='정상', title='기간이 겹치면 오늘을 품은 기간 중 가장 늦은 끝일을 보인다',
     pre='오늘을 품은 기간 둘(끝일 E1, E1 보다 늦은 E2)과 오늘을 품지 않은 미래 기간(끝일 E3, E2 보다 늦음)이 있는 고객',
     steps='1. GET /members/me 를 부른다\n'
           '2. 내 프로필(/profile)을 연다',
     expect='1. membership.endsOn 이 E2 다\n'
            '2. 내 프로필에 "회원, YYYY.MM.DD 까지" 로 E2 가 보인다',
     judge=['서버', '가상'],
     req=['BID-AUTH-008'], fc=['BID-AUTH-008'], screen='/profile', pri='High'),

dict(id='IT-01-058', flow='회원 구분', kind='정상', title='서버 시간대와 상관없이 서울 날짜로 회원을 가른다',
     pre='서울 시각 00:00~08:59 사이(UTC 로는 전날), 시작일이 서울 오늘인 기간만 있는 고객 F, 끝일이 서울 어제인 기간만 있는 고객 G',
     steps='1. F 로 GET /members/me 를 부른다\n'
           '2. G 로 GET /members/me 를 부른다',
     expect='1. F 의 membership.member 가 true 다\n'
            '2. G 의 membership.member 가 false 다',
     judge=['서버', '서버'],
     req=['BID-AUTH-008'], fc=['BID-AUTH-008'], screen='', pri='High'),

dict(id='IT-01-059', flow='회원 구분', kind='예외', title='내 정보를 못 받으면 회원도 무료회원처럼 그린다',
     pre='회원 계정, GET /members/me 가 실패하게 한 환경, 공개 공고문이 있는 마감 전 모은 공고와 없는 모은 공고',
     steps='1. 회원 계정으로 로그인해 마감 전 공고(/search)를 연다\n'
           '2. 공개 공고문이 있는 줄을 누른다',
     expect='1. 우리 회사 칸에 공개 공고문이 있는 줄은 "회원" 꼬리표, 없는 줄은 "-" 가 보인다\n'
            '2. 2번 뒤 원본 공고 상세(/search/origin/{opportunityId})에 머물고 본문 위에 "AI 공고문은 회원 전용입니다" 안내가 보인다',
     judge=['가상', '가상'],
     req=['BID-AUTH-008'], fc=['BID-AUTH-008'], screen='/search', pri='High'),

dict(id='IT-01-060', flow='회원 기간 넣기', kind='정상', title='관리자가 넣은 회원 기간이 고객 화면과 회원 목록에 반영된다',
     pre='관리자 계정, 회원 기간이 없는 고객 H(다른 창에서 고객 화면에 로그인해 둠)',
     steps='1. 관리자 화면 회원 관리(/members)에서 H 줄의 [상세] 를 누른다\n'
           '2. 시작일 오늘, 끝일 30일 뒤, 메모 "입금 확인" 을 넣고 [넣기] 를 누른다\n'
           '3. H 의 고객 화면 창을 새로고침하고 내 프로필(/profile)을 연다',
     expect='1. 회원 상세 창 아래 회원 기간 영역에 시작일, 끝일, 메모 칸과 [넣기] 가 있고 그 아래에 기간 이력이 있다\n'
            '2. 넣은 뒤 기간 이력 맨 위에 시작일, 끝일, 메모 "입금 확인", 넣은 관리자, 넣은 시각, 지금 기간 표시가 있는 줄이 생긴다\n'
            '3. 목록 H 줄의 회원 칸이 "무료회원" 에서 "회원, YYYY.MM.DD 까지"(넣은 끝일)로 바뀐다\n'
            '4. H 의 회원 기간 기록이 넣은 관리자 이메일과 함께 1줄 생긴다(POST 응답 201)\n'
            '5. H 의 내 프로필에 "회원, YYYY.MM.DD 까지" 로 넣은 끝일이 보인다',
     judge=['가상', '가상', '가상', '서버', '가상'],
     req=['BID-AUTH-008', 'BID-ADMIN-002'], fc=['BID-AUTH-008', 'BID-ADMIN-002'], screen='관리자 /members', pri='High'),

dict(id='IT-01-061', flow='회원 기간 넣기', kind='정상', title='관리자가 회원 기간을 지우면 고객이 무료회원으로 돌아간다',
     pre='관리자 계정, 오늘을 품은 기간 하나만 있는 고객 H(다른 창에서 로그인해 둠)',
     steps='1. 회원 관리에서 H 줄의 [상세] 를 누른다\n'
           '2. 기간 이력 줄의 [지우기] 를 누르고 확인한다\n'
           '3. H 의 고객 화면 창을 새로고침하고 내 프로필을 연다',
     expect='1. 기간 이력 줄에는 [지우기] 만 있고 기간을 고치는 단추가 없다\n'
            '2. 확인 뒤 기간 이력에서 그 줄이 사라진다\n'
            '3. 목록 H 줄의 회원 칸이 "무료회원" 으로 바뀐다\n'
            '4. H 의 그 회원 기간 기록이 지워진다(DELETE 응답 204)\n'
            '5. H 의 내 프로필에 "무료회원" 이 보인다',
     judge=['가상', '가상', '가상', '서버', '가상'],
     req=['BID-AUTH-008', 'BID-ADMIN-002'], fc=['BID-AUTH-008', 'BID-ADMIN-002'], screen='관리자 /members', pri='High'),

dict(id='IT-01-062', flow='회원 기간 넣기', kind='정상', title='회원 관리 목록의 회원 칸이 회원, 무료회원, 관리자를 가른다',
     pre='관리자 계정, 회원 고객(끝일 E), 무료회원 고객, 관리자 계정이 목록에 있음',
     steps='1. 관리자 화면 회원 관리(/members)를 연다\n'
           '2. GET /admin/members 를 부른다',
     expect='1. 회원 고객 줄의 회원 칸에 "회원, YYYY.MM.DD 까지" 로 E 가 보인다\n'
            '2. 무료회원 고객 줄의 회원 칸에 "무료회원" 이 보인다\n'
            '3. 관리자 줄의 회원 칸에 "관리자" 가 보인다\n'
            '4. 응답의 회원 고객 줄 member 가 true, membershipEndsOn 이 E 이고 관리자 줄 member 가 true 다',
     judge=['가상', '가상', '가상', '서버'],
     req=['BID-AUTH-008', 'BID-ADMIN-002'], fc=['BID-AUTH-008', 'BID-ADMIN-002'], screen='관리자 /members', pri='High'),

dict(id='IT-01-063', flow='회원 기간 넣기', kind='예외', title='끝일이 시작일보다 앞인 회원 기간은 넣지 않는다',
     pre='관리자 계정, 회원 기간이 없는 고객 H',
     steps='1. 회원 관리에서 H 줄의 [상세] 를 누른다\n'
           '2. 시작일 오늘, 끝일 어제를 넣고 [넣기] 를 누른다',
     expect='1. "끝일은 시작일과 같거나 뒤여야 합니다" 가 보인다\n'
            '2. 넣기 응답이 400 과 error MEMBERSHIP_PERIOD_INVALID 다\n'
            '3. H 의 회원 기간 기록이 생기지 않고 목록 H 줄은 "무료회원" 그대로다',
     judge=['가상', '서버', '서버'],
     req=['BID-AUTH-008', 'BID-ADMIN-002'], fc=['BID-AUTH-008', 'BID-ADMIN-002'], screen='관리자 /members', pri='High'),

dict(id='IT-01-064', flow='회원 구분', kind='예외', title='무료회원이 회원 전용 API 를 부르면 서버가 403 으로 막는다',
     pre='무료회원 토큰, 공개 공고문 1건(id N)',
     steps='1. GET /opportunities/{N} 을 부른다\n'
           '2. GET /opportunities 를 부른다\n'
           '3. GET /opportunities/recent 를 부른다\n'
           '4. GET /opportunities/{N}/requirements 를 부른다\n'
           '5. GET /eligibility/notices 를 부른다\n'
           '6. POST /bid-requests 에 공고 N 을 넣어 부른다',
     expect='1. 1번 응답이 403 과 error MEMBERSHIP_REQUIRED, message "회원 전용입니다" 다\n'
            '2. 2번 응답이 403 과 error MEMBERSHIP_REQUIRED 다\n'
            '3. 3번 응답이 403 과 error MEMBERSHIP_REQUIRED 다\n'
            '4. 4번 응답이 403 과 error MEMBERSHIP_REQUIRED 다\n'
            '5. 5번 응답이 403 과 error MEMBERSHIP_REQUIRED 다\n'
            '6. 6번 응답이 403 과 error MEMBERSHIP_REQUIRED 이고 입찰 요청 기록이 생기지 않는다',
     judge=['서버', '서버', '서버', '서버', '서버', '서버'],
     req=['BID-AUTH-008', 'BID-BROWSE-002', 'BID-BROWSE-003', 'BID-REQ-001'],
     fc=['BID-AUTH-008', 'BID-BROWSE-002', 'BID-BROWSE-003', 'BID-REQ-001'], screen='', pri='High'),

dict(id='IT-01-065', flow='회원 구분', kind='예외', title='로그인하지 않았거나 토큰이 끝났으면 회원 전용 API 를 401 로 막는다',
     pre='비로그인(토큰 없음)과 회원 계정의 끝난 Access Token, 공개 공고문 1건(id N)',
     steps='1. 토큰 없이 GET /opportunities/{N} 을 부른다\n'
           '2. 끝난 Access Token 으로 GET /opportunities/{N} 을 부른다',
     expect='1. 1번 응답이 401 과 error LOGIN_REQUIRED, message "로그인이 필요합니다" 다\n'
            '2. 2번 응답이 401 과 error LOGIN_REQUIRED 다',
     judge=['서버', '서버'],
     req=['BID-AUTH-008', 'BID-BROWSE-002'], fc=['BID-AUTH-008', 'BID-BROWSE-002'], screen='', pri='High'),

dict(id='IT-01-066', flow='회원 상세', kind='정상', title='회원 관리 줄의 상세로 회원 한 명의 가입 정보를 본다',
     pre='관리자 계정, 사업자등록번호가 있고 전화번호와 주소가 비어 있는 고객 H(오늘을 품은 회원 기간 하나, 끝일 E)',
     steps='1. 관리자 화면 회원 관리(/members)를 열고 H 가 있는 쪽으로 간다\n'
           '2. H 줄의 [상세] 를 누른다\n'
           '3. [닫기] 를 누른다\n'
           '4. GET /admin/members/{H} 를 부른다',
     expect='1. 목록의 모든 줄(고객과 관리자) 끝에 [상세] 단추가 있다\n'
            '2. 목록 화면에 머문 채 그 위에 "회원 상세, H 의 이메일" 창이 열린다\n'
            '3. 회원 정보에 이메일, 이메일 확인, 회사명, 사업자등록번호, 담당자, 전화번호, 주소, 역할, 회원, 가입일, 최근 로그인 칸이 이 차례로 있다\n'
            '4. 사업자등록번호 칸에 H 가 가입 때 넣은 번호가 보인다\n'
            '5. 전화번호 칸과 주소 칸에 "-" 가 보인다\n'
            '6. 회원 칸에 "회원, YYYY.MM.DD 까지" 로 E 가 보이고 아래 회원 기간 영역의 기간 이력에 그 기간 줄이 있다\n'
            '7. 닫은 뒤 목록이 연 때와 같은 쪽에 머문다\n'
            '8. 4번 응답이 200 이고 businessRegistrationNumber 가 H 의 번호, phone 과 address 가 없고 member 가 true, membershipEndsOn 이 E 다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상', '가상', '서버'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='High'),

dict(id='IT-01-067', flow='회원 상세', kind='정상', title='관리자 줄의 상세에는 회원 정보만 있다',
     pre='관리자 계정, 다른 관리자 계정 A2 가 목록에 있음',
     steps='1. 관리자 화면 회원 관리(/members)에서 A2 줄의 [상세] 를 누른다',
     expect='1. "회원 상세, A2 의 이메일" 창의 회원 정보에 역할 "관리자", 회원 "관리자" 가 보인다\n'
            '2. 창에 회원 기간 제목, [넣기], 기간 이력이 없다',
     judge=['가상', '가상'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='Medium'),

dict(id='IT-01-068', flow='회원 정보 수정', kind='정상', title='관리자가 회원 상세에서 회원 정보를 고치면 창, 목록, 고객 화면에 반영된다',
     pre='관리자 계정, 전화번호가 있는 고객 H(다른 창에서 고객 화면에 로그인해 둠)',
     steps='1. 관리자 화면 회원 관리(/members)에서 H 줄의 [상세] 를 누른다\n'
           '2. 회원 정보 제목 오른쪽 [수정] 을 누른다\n'
           '3. 회사명 "시험 고친 회사", 사업자등록번호 "111-22-33333", 담당자 "시험 담당" 으로 바꾸고 전화번호를 비운 뒤 [저장] 을 누른다\n'
           '4. [닫기] 를 누르고 H 의 고객 화면 창에서 회사 정보(/profile)를 새로 연다',
     expect='1. 2번 뒤 회사명, 사업자등록번호, 담당자, 전화번호, 주소가 지금 값을 담은 입력칸이 되고 [취소], [저장] 이 나온다\n'
            '2. 2번 뒤 이메일, 이메일 확인, 역할, 회원, 가입일, 최근 로그인은 입력칸이 아니라 글자로 남는다\n'
            '3. 저장 뒤 "저장했습니다" 알림이 뜬다\n'
            '4. 저장 뒤 입력칸이 글자로 돌아가 회사명 "시험 고친 회사", 사업자등록번호 "111-22-33333", 담당자 "시험 담당" 이 보이고 전화번호 칸은 "-" 다\n'
            '5. 목록 H 줄의 회사명이 "시험 고친 회사", 담당자가 "시험 담당" 이다\n'
            '6. H 의 회원 기록이 회사명 "시험 고친 회사", 사업자등록번호 "111-22-33333", 담당자 "시험 담당" 이고 전화번호가 비어 있다(PATCH 응답 200)\n'
            '7. H 의 회사 정보에 회사명 "시험 고친 회사" 와 사업자등록번호 "111-22-33333" 이 보인다',
     judge=['가상', '가상', '가상', '가상', '가상', '서버', '가상'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='High'),

dict(id='IT-01-069', flow='회원 정보 수정', kind='예외', title='회사명을 비우면 회원 정보를 저장하지 않는다',
     pre='관리자 계정, 고객 H',
     steps='1. 회원 관리에서 H 줄의 [상세] 를 누르고 [수정] 을 누른다\n'
           '2. 회사명을 지우고 [저장] 을 누른다\n'
           '3. PATCH /admin/members/{H} 에 companyName " "(빈칸 하나)를 넣어 부른다',
     expect='1. 회사명 칸 아래 "회사명을 넣으세요" 가 보이고 입력칸이 그대로 남는다\n'
            '2. H 의 회사명이 고치기 전 값 그대로다\n'
            '3. 3번 응답이 400 이고 H 의 회사명이 그대로다',
     judge=['가상', '서버', '서버'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='High'),

dict(id='IT-01-070', flow='회원 정보 수정', kind='예외', title='취소하거나 고치는 중에 창을 닫으면 고친 값을 버린다',
     pre='관리자 계정, 고객 H(회사명 C)',
     steps='1. 회원 관리에서 H 줄의 [상세] 를 누르고 [수정] 을 누른다\n'
           '2. 회사명을 "버릴 회사" 로 바꾸고 [취소] 를 누른다\n'
           '3. 다시 [수정] 을 누르고 회사명을 "버릴 회사" 로 바꾼 뒤 창 오른쪽 위 닫기를 누른다\n'
           '4. H 줄의 [상세] 를 다시 누른다',
     expect='1. 2번 뒤 입력칸이 글자로 돌아가 회사명 칸에 C 가 보인다\n'
            '2. 4번에 다시 연 창의 회사명 칸에 C 가 보인다\n'
            '3. H 의 회사명이 C 그대로다',
     judge=['가상', '가상', '서버'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='관리자 /members', pri='Medium'),

dict(id='IT-01-071', flow='회원 정보 수정', kind='예외', title='없는 회원과 고객 토큰의 회원 상세, 회원 정보 수정은 서버가 막는다',
     pre='관리자 토큰, 고객 H 의 토큰, 없는 회원 번호 X',
     steps='1. 관리자 토큰으로 GET /admin/members/{X} 를 부른다\n'
           '2. 관리자 토큰으로 PATCH /admin/members/{X} 에 companyName "시험" 을 넣어 부른다\n'
           '3. H 의 토큰으로 PATCH /admin/members/{H} 에 companyName "시험" 을 넣어 부른다',
     expect='1. 1번 응답이 404 와 error MEMBER_NOT_FOUND, message "회원이 없습니다" 다\n'
            '2. 2번 응답이 404 와 error MEMBER_NOT_FOUND 다\n'
            '3. 3번 응답이 403 이고 H 의 회사명이 그대로다',
     judge=['서버', '서버', '서버'],
     req=['BID-ADMIN-002'], fc=['BID-ADMIN-002'], screen='', pri='High'),

dict(id='IT-01-072', flow='회원 정보 수정', kind='정상', title='고객이 회사 정보에서 고친 사업자등록번호가 저장된다',
     pre='고객 계정 H(사업자등록번호 "123-45-67890"), 관리자 계정',
     steps='1. H 로 고객 화면 회사 정보(/profile)를 열고 사업자등록번호를 "222-33-44444" 로 바꿔 [저장하기] 를 누른다\n'
           '2. 새로고침한다\n'
           '3. 관리자 화면 회원 관리에서 H 줄의 [상세] 를 누른다\n'
           '4. H 의 토큰으로 PATCH /members/me 에 사업자등록번호 칸 없이 지금 회사명, 담당자, 전화번호, 주소를 넣어 부른다',
     expect='1. 저장 뒤 단추가 "저장됨" 으로 바뀐다\n'
            '2. 새로고침 뒤 사업자등록번호 칸에 "222-33-44444" 가 보인다\n'
            '3. 관리자 회원 상세의 사업자등록번호 칸에 "222-33-44444" 가 보인다\n'
            '4. 4번 응답이 200 이고 H 의 사업자등록번호가 "222-33-44444" 그대로다',
     judge=['가상', '가상', '가상', '서버'],
     req=['BID-AUTH-001', 'BID-AUTH-004'], fc=['BID-AUTH-001', 'BID-AUTH-004'], screen='/profile', pri='High'),

]
