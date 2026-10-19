# 영역 13 성공 제안서 패턴 시험항목. 근거는 T1-1 기능요구사항 명세서, T1-3 BIZ-017~019, T3-1, T3-2, T3-3 화면 설계다.

ITEMS = [

dict(id='IT-13-001', flow='성공 제안서 등록', kind='정상', title='관리자가 필수와 선택 메타를 넣어 성공 제안서를 등록한다',
     pre='관리자 계정, 공고번호 W90VN725RA012 로 등록된 성공 제안서 없음',
     steps='1. 관리자 화면에 관리자 계정으로 로그인한다\n2. 왼쪽 메뉴 [성공 제안서 패턴] 을 누른다\n'
           '3. 신규 등록 창을 열어 공고번호 "W90VN725RA012", 사업유형 GROUND_MAINTENANCE, 결과 WON, 업체 "녹화창조", '
           '발주처 "군산 AB", 낙찰액 3800000000, 회계연도 2025 를 넣고 [등록] 을 누른다\n4. 등록 목록에서 그 줄을 누른다',
     expect='1. 등록 목록에 공고번호 W90VN725RA012, 사업유형 GROUND_MAINTENANCE, 결과 WON, 업체 녹화창조, 파일수 0 인 줄이 생긴다\n'
            '2. rfp_samples 에 그 공고번호 줄이 1줄 생기고 industry_type, outcome, company, agency, award_amount, fiscal_year 가 넣은 값이다\n'
            '3. 성공 제안서 상세(/rfp-samples/{id})가 열려 메타 정보의 공고번호, 사업유형, 결과, 업체, 발주처, 낙찰액이 넣은 값으로 보인다',
     judge=['가상', '서버', '가상'],
     req=['BID-RFP-001'], fc=['BID-RFP-001'], screen='관리자 /rfp-samples', pri='High'),

dict(id='IT-13-002', flow='성공 제안서 등록', kind='정상', title='필수 메타만 넣어 성공 제안서를 등록한다',
     pre='관리자 계정',
     steps='1. [성공 제안서 패턴] 에서 신규 등록 창을 연다\n'
           '2. 공고번호 "TEST-RFP-002", 사업유형 CUSTODIAL, 결과 SUBMITTED 만 넣고 [등록] 을 누른다',
     expect='1. 등록 목록에 공고번호 TEST-RFP-002, 사업유형 CUSTODIAL, 결과 SUBMITTED 인 줄이 생기고 업체 칸이 비어 있다\n'
            '2. rfp_samples 의 그 줄 company, agency, award_amount, fiscal_year 가 NULL 이다',
     judge=['가상', '서버'],
     req=['BID-RFP-001'], fc=['BID-RFP-001'], screen='관리자 /rfp-samples', pri='High'),

dict(id='IT-13-003', flow='성공 제안서 등록', kind='정상', title='결과 OTHER 로 성공 제안서를 등록한다',
     pre='관리자 계정',
     steps='1. [성공 제안서 패턴] 에서 신규 등록 창을 연다\n'
           '2. 결과 칸의 고를 수 있는 값을 본다\n3. 공고번호 "TEST-RFP-003", 사업유형 HVAC, 결과 OTHER 를 넣고 [등록] 을 누른다',
     expect='1. 결과 칸에서 WON, SUBMITTED, OTHER 세 값을 고를 수 있다\n'
            '2. 등록 목록의 그 줄 결과가 OTHER 다\n'
            '3. rfp_samples 의 그 줄 outcome 이 OTHER 다',
     judge=['가상', '가상', '서버'],
     req=['BID-RFP-001'], fc=['BID-RFP-001'], screen='관리자 /rfp-samples', pri='High',
     issue='T3-3 등록 목록과 신규 등록 창은 결과를 WON/SUBMITTED 로만 적고 T1-1 과 T3-1 Outcome 은 OTHER 를 더 둔다'),

dict(id='IT-13-004', flow='성공 제안서 등록', kind='예외', title='필수 메타가 빠지면 등록되지 않는다',
     pre='관리자 계정',
     steps='1. [성공 제안서 패턴] 에서 신규 등록 창을 연다\n2. 공고번호를 비우고 사업유형 CUSTODIAL, 결과 WON 을 넣어 [등록] 을 누른다',
     expect='1. 등록 목록 줄 수가 누르기 전과 같다\n2. rfp_samples 에 줄이 늘지 않는다',
     judge=['가상', '서버'],
     req=['BID-RFP-001'], fc=['BID-RFP-001'], screen='관리자 /rfp-samples', pri='High'),

dict(id='IT-13-005', flow='성공 제안서 등록', kind='예외', title='고객 계정은 성공 제안서를 등록하지 못한다',
     pre='회원 고객 계정의 로그인 토큰',
     steps='1. 고객 토큰으로 GET /api/admin/rfp-samples 를 부른다\n'
           '2. 같은 토큰으로 POST /api/admin/rfp-samples 에 공고번호, 사업유형, 결과를 넣어 보낸다',
     expect='1. 목록 요청이 권한 없음으로 거부되고 응답에 성공 제안서 목록이 없다\n'
            '2. 등록 요청이 거부되고 rfp_samples 에 줄이 늘지 않는다',
     judge=['서버', '서버'],
     req=['BID-RFP-001'], fc=['BID-RFP-001'], screen='', pri='High'),

dict(id='IT-13-006', flow='원본 파일', kind='정상', title='성공 제안서 상세에서 제안서 원본 파일을 올린다',
     pre='관리자 계정, 파일이 없는 성공 제안서 1건, PDF, XLSX, DOCX 파일 하나씩',
     steps='1. [성공 제안서 패턴] 에서 그 성공 제안서 줄을 눌러 상세를 연다\n'
           '2. 제안서 파일 업로드로 PDF, XLSX, DOCX 파일을 하나씩 올린다\n3. [성공 제안서 패턴] 목록으로 돌아간다',
     expect='1. 원본 파일 목록에 올린 세 파일 이름이 원래 파일명 그대로 보인다\n'
            '2. 세 파일 줄에 PWS 배지가 없다\n'
            '3. 등록 목록 그 줄의 파일수가 3 이다\n'
            '4. rfp_sample_files 에 세 줄이 is_pws false, file_name 원래 이름, file_size 원래 크기로 생긴다',
     judge=['가상', '가상', '가상', '서버'],
     req=['BID-RFP-001'], fc=['BID-RFP-001'], screen='관리자 /rfp-samples/{id}', pri='High'),

dict(id='IT-13-007', flow='원본 파일', kind='정상', title='PWS 공고문을 따로 올리면 PWS 로 표시된다',
     pre='관리자 계정, 성공 제안서 1건, PWS 공고문 PDF 1개',
     steps='1. 그 성공 제안서 상세를 연다\n2. PWS 업로드로 PWS 공고문 PDF 를 올린다',
     expect='1. 원본 파일 목록의 그 파일 줄에 PWS 배지가 붙는다\n2. rfp_sample_files 의 그 줄 is_pws 가 true 다',
     judge=['가상', '서버'],
     req=['BID-RFP-001'], fc=['BID-RFP-001'], screen='관리자 /rfp-samples/{id}', pri='High'),

dict(id='IT-13-008', flow='원본 파일', kind='정상', title='올린 파일의 텍스트 추출 상태를 본다',
     pre='관리자 계정, 성공 제안서 1건, 글자가 들어 있는 PDF 1개',
     steps='1. 그 성공 제안서 상세에서 제안서 파일 업로드로 그 PDF 를 올린다\n2. 원본 파일 목록의 그 파일 줄을 본다',
     expect='1. 원본 파일 목록의 그 파일 줄에 텍스트 추출 상태가 있다\n'
            '2. 그 파일의 텍스트가 업로드 때 한 번 추출되어 저장되어 있다',
     judge=['가상', '서버'],
     req=['BID-RFP-001'], fc=['BID-RFP-001'], screen='관리자 /rfp-samples/{id}', pri='High',
     issue='T3-1 RfpSampleFile 에 추출 텍스트와 extraction_status 칸이 없고 텍스트 추출은 이 플랫폼이 하지 않고 Aimbase 가 한다고 적어 T1-1 과 어긋난다. T3-3 원본 파일 영역에도 추출 상태가 없다'),

dict(id='IT-13-009', flow='원본 파일', kind='정상', title='텍스트를 뽑지 못하는 브로슈어형 PDF 도 받아 FAILED 로 표시한다',
     pre='관리자 계정, 성공 제안서 1건, 글자 없이 그림으로만 된 브로슈어형 PDF 1개',
     steps='1. 그 성공 제안서 상세에서 제안서 파일 업로드로 브로슈어형 PDF 를 올린다',
     expect='1. 업로드가 받아져 원본 파일 목록에 그 파일 이름이 생긴다\n'
            '2. 그 파일 줄의 텍스트 추출 상태가 FAILED 다\n'
            '3. 그 파일의 extraction_status 가 FAILED 로 저장된다',
     judge=['가상', '가상', '서버'],
     req=['BID-RFP-001'], fc=['BID-RFP-001'], screen='관리자 /rfp-samples/{id}', pri='High',
     issue='T1-1 의 extraction_status 칸이 T3-1 RfpSampleFile 에 없다'),

dict(id='IT-13-010', flow='원본 파일', kind='정상', title='원본 파일 하나를 지운다',
     pre='관리자 계정, 파일 2개가 있는 성공 제안서',
     steps='1. 그 성공 제안서 상세에서 파일 하나를 지운다\n2. [성공 제안서 패턴] 목록으로 돌아간다',
     expect='1. 원본 파일 목록에서 그 파일 줄이 빠지고 남은 파일 1개만 있다\n'
            '2. 등록 목록 그 줄의 파일수가 1 이다\n'
            '3. rfp_sample_files 에서 그 파일 줄이 지워진다',
     judge=['가상', '가상', '서버'],
     req=['BID-RFP-001'], fc=['BID-RFP-001'], screen='관리자 /rfp-samples/{id}', pri='High'),

dict(id='IT-13-011', flow='원본 파일', kind='정상', title='성공 제안서를 지우면 파일도 함께 지워진다',
     pre='관리자 계정, 파일 2개가 있는 성공 제안서',
     steps='1. 그 성공 제안서 상세에서 메타 정보의 삭제를 누르고 확인한다',
     expect='1. 등록 목록에서 그 공고번호 줄이 빠진다\n'
            '2. rfp_samples 의 그 줄과 그 성공 제안서의 rfp_sample_files 두 줄이 모두 지워진다',
     judge=['가상', '서버'],
     req=['BID-RFP-001'], fc=['BID-RFP-001'], screen='관리자 /rfp-samples/{id}', pri='High'),

dict(id='IT-13-012', flow='원본 파일', kind='정상', title='보관한 원본 파일이 올린 파일과 똑같이 내려받아진다',
     pre='관리자 계정, 성공 제안서에 올린 PDF 1개와 그 원본 파일, 서버 열쇠(MCP_API_KEY)',
     steps='1. 그 파일 id 로 GET /mcp/rfp-files/{fileId}/download 를 X-API-Key 머리글과 함께 부른다',
     expect='1. 내려받은 파일의 크기와 내용이 올린 원본 파일과 같다',
     judge=['서버'],
     req=['BID-RFP-001'], fc=['BID-RFP-001'], screen='', pri='High'),

dict(id='IT-13-013', flow='원본 파일', kind='예외', title='50MB 를 넘는 원본 파일은 올라가지 않는다',
     pre='관리자 계정, 성공 제안서 1건, 51MB PDF 파일',
     steps='1. 그 성공 제안서 상세에서 제안서 파일 업로드로 51MB PDF 를 올린다',
     expect='1. 업로드가 거부되고 원본 파일 목록에 그 파일 이름이 없다\n'
            '2. rfp_sample_files 에 줄이 늘지 않는다',
     judge=['가상', '서버'],
     req=['BID-RFP-001'], fc=['BID-RFP-001'], screen='관리자 /rfp-samples/{id}', pri='High'),

dict(id='IT-13-014', flow='패턴 추출', kind='정상', title='공고유형별 가이드 표에 8개 유형의 상태, 출처, 추출 건수가 있다',
     pre='관리자 계정',
     steps='1. 관리자 화면 [성공 제안서 패턴] 을 연다\n2. 관리자 토큰으로 GET /api/admin/pattern-guides 를 부른다',
     expect='1. 공고유형별 가이드에 GROUND_MAINTENANCE, CUSTODIAL, LAUNDRY, HVAC, WASTE, PIPELINE, SECURITY, FACILITY_LEASE 8개 유형 줄이 있다\n'
            '2. 줄마다 상태(PENDING, EXTRACTING, COMPLETED, FAILED 중 하나), 출처(AI 또는 사람편집), 추출 건수가 있다\n'
            '3. 가이드 목록 응답의 유형별 status, source, sampleCount 가 화면 값과 같다',
     judge=['가상', '가상', '서버'],
     req=['BID-RFP-003', 'BID-RFP-004'], fc=['BID-RFP-003', 'BID-RFP-004'], screen='관리자 /rfp-samples', pri='High'),

dict(id='IT-13-015', flow='패턴 추출', kind='정상', title='성공 제안서가 있는 유형의 패턴 추출을 시작한다',
     pre='관리자 계정, 파일이 있는 GROUND_MAINTENANCE 성공 제안서 2건, Aimbase 에 닿는 배포 환경',
     steps='1. [성공 제안서 패턴] 의 공고유형별 가이드에서 GROUND_MAINTENANCE 줄의 [추출] 을 누른다',
     expect='1. GROUND_MAINTENANCE 줄 상태가 EXTRACTING 으로 바뀐다\n'
            '2. 추출 요청 응답 status 가 EXTRACTING 이고 pattern_guides 의 GROUND_MAINTENANCE 줄 status 가 EXTRACTING, workflow_run_id 가 채워진다',
     judge=['가상', '서버'],
     req=['BID-RFP-003'], fc=['BID-RFP-003'], screen='관리자 /rfp-samples', pri='High'),

dict(id='IT-13-016', flow='패턴 추출', kind='정상', title='패턴 추출이 끝나면 유형 가이드가 저장된다',
     pre='관리자 계정, IT-13-015 로 GROUND_MAINTENANCE 추출을 시작한 상태',
     steps='1. Aimbase 추출이 끝날 때까지 기다린다\n2. [성공 제안서 패턴] 을 새로고침한다\n3. GROUND_MAINTENANCE 줄의 [편집] 을 눌러 가이드 본문을 본다',
     expect='1. GROUND_MAINTENANCE 줄이 상태 COMPLETED, 출처 AI, 추출 건수 2 다\n'
            '2. pattern_guides 그 줄이 status COMPLETED, source AI_EXTRACTED, sample_count 2 이고 extracted_at 이 채워진다\n'
            '3. guide_json 에 골격, 체크리스트, 금기 세 묶음이 있다\n'
            '4. 가이드 본문이 두 성공 제안서에 공통된 구성과 전략을 담는다',
     judge=['가상', '서버', '서버', '합동'],
     req=['BID-RFP-003'], fc=['BID-RFP-003'], screen='관리자 /rfp-samples', pri='High'),

dict(id='IT-13-017', flow='패턴 추출', kind='정상', title='Aimbase 가 save_pattern_guide 도구로 유형 가이드를 저장한다',
     pre='서버 열쇠(MCP_API_KEY), 관리자 계정, AI_EXTRACTED 이거나 미생성인 CUSTODIAL 가이드',
     steps='1. POST /mcp 에 X-API-Key 머리글과 tools/call 본문(name save_pattern_guide, industryType CUSTODIAL, '
           'guide 의 골격 "1장 회사 소개", 체크리스트 "현장 인원표", 금기 "타사 실적 인용")을 보낸다\n'
           '2. 관리자 화면 [성공 제안서 패턴] 에서 CUSTODIAL 줄의 [편집] 을 누른다',
     expect='1. JSON-RPC 응답에 error 가 없다\n'
            '2. pattern_guides 의 CUSTODIAL 줄 guide_json 에 보낸 골격, 체크리스트, 금기가 들어 있다\n'
            '3. 편집 창 가이드 본문에 "1장 회사 소개" 가 보인다',
     judge=['서버', '서버', '가상'],
     req=['BID-RFP-003'], fc=['BID-RFP-003'], screen='관리자 /rfp-samples', pri='High'),

dict(id='IT-13-018', flow='패턴 추출', kind='정상', title='get_reference_samples 도구가 유형의 성공 제안서 파일과 서명 주소를 준다',
     pre='서버 열쇠(MCP_API_KEY), GROUND_MAINTENANCE 성공 제안서 1건에 제안서 파일 2개와 PWS 파일 1개',
     steps='1. POST /mcp 에 X-API-Key 머리글과 tools/call 본문(name get_reference_samples, industryType GROUND_MAINTENANCE)을 보낸다\n'
           '2. 응답의 downloadUrl 하나를 X-API-Key 머리글 없이 GET 으로 연다',
     expect='1. 응답에 그 성공 제안서의 rfpSampleId 와 제안서 파일 2개의 fileName, downloadUrl 이 있다\n'
            '2. 응답 파일 목록에 PWS 파일이 없다\n'
            '3. downloadUrl 이 /mcp/rfp-files/{id}/download 경로에 exp 와 sig 가 붙은 주소다\n'
            '4. 머리글 없이 연 주소로 그 원본 파일이 올린 파일과 같은 크기로 내려받아진다',
     judge=['서버', '서버', '서버', '서버'],
     req=['BID-RFP-003', 'BID-RFP-005'], fc=['BID-RFP-003', 'BID-RFP-005'], screen='', pri='High'),

dict(id='IT-13-019', flow='패턴 추출', kind='예외', title='패턴 추출이 실패하면 FAILED 와 실패 사유가 남는다',
     pre='관리자 계정, 성공 제안서가 있는 WASTE 유형, Aimbase 추출이 실패하도록 만든 상태',
     steps='1. [성공 제안서 패턴] 에서 WASTE 줄의 [추출] 을 누른다\n2. 추출 실패 뒤 화면을 새로고침한다',
     expect='1. WASTE 줄 상태가 FAILED 로 보인다\n'
            '2. pattern_guides 의 WASTE 줄 status 가 FAILED 이고 error_message 에 실패 사유가 남는다',
     judge=['가상', '단위'],
     req=['BID-RFP-003'], fc=['BID-RFP-003'], screen='관리자 /rfp-samples', pri='High'),

dict(id='IT-13-020', flow='패턴 추출', kind='예외', title='성공 제안서가 없는 유형은 추출이 시작되지 않는다',
     pre='관리자 계정, LAUNDRY 성공 제안서 0건',
     steps='1. [성공 제안서 패턴] 에서 LAUNDRY 줄의 [추출] 을 누른다\n'
           '2. 관리자 토큰으로 POST /api/admin/pattern-guides/LAUNDRY/extract 를 보낸다',
     expect='1. 추출하지 못했다는 안내가 뜬다\n'
            '2. LAUNDRY 줄 상태가 누르기 전과 같다\n'
            '3. 추출 요청 응답이 400 이고 pattern_guides 의 LAUNDRY 상태가 EXTRACTING 이 되지 않는다',
     judge=['가상', '가상', '서버'],
     req=['BID-RFP-003'], fc=['BID-RFP-003'], screen='관리자 /rfp-samples', pri='High'),

dict(id='IT-13-021', flow='패턴 추출', kind='예외', title='고객 계정은 패턴 추출을 시작하지 못한다',
     pre='회원 고객 계정의 로그인 토큰, 성공 제안서가 있는 GROUND_MAINTENANCE 유형',
     steps='1. 고객 토큰으로 POST /api/admin/pattern-guides/GROUND_MAINTENANCE/extract 를 보낸다',
     expect='1. 요청이 권한 없음으로 거부된다\n2. pattern_guides 의 GROUND_MAINTENANCE 상태가 보내기 전과 같다',
     judge=['서버', '서버'],
     req=['BID-RFP-003'], fc=['BID-RFP-003'], screen='', pri='High'),

dict(id='IT-13-022', flow='가이드 편집', kind='정상', title='유형 가이드 본문과 출처, 추출 건수를 조회한다',
     pre='관리자 계정, 추출이 끝난 GROUND_MAINTENANCE 가이드',
     steps='1. [성공 제안서 패턴] 에서 GROUND_MAINTENANCE 줄의 [편집] 을 누른다\n'
           '2. 관리자 토큰으로 GET /api/admin/pattern-guides/GROUND_MAINTENANCE 를 부른다',
     expect='1. 편집 창에 저장된 가이드 본문이 들어 있다\n'
            '2. 가이드 상세 응답에 guideJson, markdown, source, sampleCount 가 있고 pattern_guides 그 줄 값과 같다',
     judge=['가상', '서버'],
     req=['BID-RFP-004'], fc=['BID-RFP-004'], screen='관리자 /rfp-samples', pri='High'),

dict(id='IT-13-023', flow='가이드 편집', kind='정상', title='전문가가 가이드를 고치면 출처가 사람편집으로 바뀐다',
     pre='관리자 계정, 출처 AI_EXTRACTED 인 GROUND_MAINTENANCE 가이드',
     steps='1. GROUND_MAINTENANCE 줄의 [편집] 을 누른다\n2. 본문 끝에 "현장 사진을 절마다 넣는다" 줄을 더하고 [저장] 을 누른다\n'
           '3. 다시 [편집] 을 누른다',
     expect='1. GROUND_MAINTENANCE 줄의 출처가 "사람편집" 으로 바뀐다\n'
            '2. pattern_guides 의 그 줄 source 가 HUMAN_EDITED 이고 guide_markdown 에 "현장 사진을 절마다 넣는다" 가 들어 있다\n'
            '3. 다시 연 편집 창 본문 끝에 "현장 사진을 절마다 넣는다" 가 있다',
     judge=['가상', '서버', '가상'],
     req=['BID-RFP-004'], fc=['BID-RFP-004'], screen='관리자 /rfp-samples', pri='High'),

dict(id='IT-13-024', flow='가이드 편집', kind='정상', title='가이드가 없는 유형에 전문가가 직접 가이드를 쓴다',
     pre='관리자 계정, 가이드가 미생성인 HVAC 유형',
     steps='1. HVAC 줄의 [편집] 을 누른다\n2. 본문에 "냉난방 점검 주기표를 첫 장에 둔다" 를 쓰고 [저장] 을 누른다',
     expect='1. HVAC 줄의 출처가 "사람편집" 이다\n'
            '2. pattern_guides 에 HVAC 줄이 생기고 source 가 HUMAN_ADDED, guide_markdown 이 쓴 본문이다',
     judge=['가상', '서버'],
     req=['BID-RFP-004'], fc=['BID-RFP-004'], screen='관리자 /rfp-samples', pri='High',
     issue='T3-1 GuideSource 는 사람이 직접 추가한 가이드를 HUMAN_ADDED 로 두지만 T3-2 PUT 은 저장하면 HUMAN_EDITED 로 바꾼다고만 적는다'),

dict(id='IT-13-025', flow='가이드 편집', kind='정상', title='AI 가 만든 가이드는 성공 제안서가 쌓이면 재추출로 갱신된다',
     pre='관리자 계정, 출처 AI_EXTRACTED, 추출 건수 1 인 CUSTODIAL 가이드, 새로 등록해 파일을 올린 CUSTODIAL 성공 제안서 1건, Aimbase 에 닿는 배포 환경',
     steps='1. [성공 제안서 패턴] 에서 CUSTODIAL 줄의 [재추출] 을 누른다\n2. 추출이 끝날 때까지 기다리고 새로고침한다',
     expect='1. CUSTODIAL 줄의 추출 건수가 2 로 바뀐다\n'
            '2. pattern_guides 의 CUSTODIAL 줄 sample_count 가 2, extracted_at 이 재추출 시각, source 가 AI_EXTRACTED 다\n'
            '3. 갱신된 가이드 본문에 새 성공 제안서의 구성이 반영된다',
     judge=['가상', '서버', '합동'],
     req=['BID-RFP-004', 'BID-RFP-003'], fc=['BID-RFP-004', 'BID-RFP-003'], screen='관리자 /rfp-samples', pri='High'),

dict(id='IT-13-026', flow='가이드 편집', kind='예외', title='사람이 고친 가이드는 재추출이 덮어쓰지 않는다',
     pre='관리자 계정, 출처 HUMAN_EDITED 인 GROUND_MAINTENANCE 가이드, GROUND_MAINTENANCE 성공 제안서 1건 이상, Aimbase 에 닿는 배포 환경',
     steps='1. 재추출 전 pattern_guides 의 GROUND_MAINTENANCE guide_json, guide_markdown 을 적어 둔다\n'
           '2. [성공 제안서 패턴] 에서 GROUND_MAINTENANCE 줄의 [재추출] 을 누른다\n3. 추출이 끝날 때까지 기다리고 새로고침한다',
     expect='1. pattern_guides 의 GROUND_MAINTENANCE guide_json, guide_markdown 이 재추출 전 값과 같다\n'
            '2. source 가 HUMAN_EDITED 그대로다\n'
            '3. 화면 GROUND_MAINTENANCE 줄의 출처가 "사람편집" 그대로다',
     judge=['서버', '서버', '가상'],
     req=['BID-RFP-004', 'BID-RFP-003'], fc=['BID-RFP-004', 'BID-RFP-003'], screen='관리자 /rfp-samples', pri='High'),

dict(id='IT-13-027', flow='가이드 편집', kind='예외', title='고객 계정은 가이드를 고치지 못한다',
     pre='회원 고객 계정의 로그인 토큰, 출처 AI_EXTRACTED 인 CUSTODIAL 가이드',
     steps='1. 고객 토큰으로 PUT /api/admin/pattern-guides/CUSTODIAL 에 새 본문을 보낸다',
     expect='1. 요청이 권한 없음으로 거부된다\n2. pattern_guides 의 CUSTODIAL guide_markdown 과 source 가 보내기 전과 같다',
     judge=['서버', '서버'],
     req=['BID-RFP-004'], fc=['BID-RFP-004'], screen='', pri='High'),

dict(id='IT-13-028', flow='성공 자산 주입', kind='정상', title='분류된 공고의 제안서 작성에 그 유형 성공 자산이 들어간다',
     pre='관리자 계정, industry_type 이 GROUND_MAINTENANCE 인 공고의 상태 ANALYZING 입찰 요청, '
         '추출이 끝난 GROUND_MAINTENANCE 가이드, 제안서 파일과 PWS 파일이 있는 GROUND_MAINTENANCE 성공 제안서, Aimbase 에 닿는 배포 환경',
     steps='1. 관리자 화면에서 그 입찰 요청을 GENERATING 으로 전이한다\n2. Aimbase 실행 기록에서 그 문서 생성 워크플로우의 입력을 연다',
     expect='1. 입력의 successGuide 가 GROUND_MAINTENANCE 가이드의 guideJson 과 같다\n'
            '2. 입력의 referenceSamples 에 GROUND_MAINTENANCE 성공 제안서의 rfpSampleId 와 files(fileName, downloadUrl)가 있다\n'
            '3. 입력에 사실 복붙을 금지하는 referenceUsagePolicy 문구가 있다\n'
            '4. referenceSamples 의 files 에 PWS 파일이 없다',
     judge=['합동', '합동', '합동', '합동'],
     req=['BID-RFP-005'], fc=['BID-RFP-005'], screen='', pri='High'),

dict(id='IT-13-029', flow='성공 자산 주입', kind='정상', title='미분류 공고는 성공 자산 없이 제안서를 만든다',
     pre='관리자 계정, industry_type 이 NULL 인 공고의 상태 ANALYZING 입찰 요청, Aimbase 에 닿는 배포 환경',
     steps='1. 관리자 화면에서 그 입찰 요청을 GENERATING 으로 전이한다\n2. Aimbase 실행 기록에서 그 문서 생성 워크플로우의 입력을 연다\n'
           '3. 생성이 끝날 때까지 기다린다',
     expect='1. 입력의 successGuide 가 비어 있다\n'
            '2. 입력의 referenceSamples 가 빈 목록이다\n'
            '3. 생성이 끝나 그 입찰 요청 state 가 REVIEW 로 바뀐다',
     judge=['합동', '합동', '서버'],
     req=['BID-RFP-005'], fc=['BID-RFP-005'], screen='', pri='High'),

dict(id='IT-13-030', flow='성공 자산 주입', kind='예외', title='성공 제안서 원본의 사실이 새 제안서에 섞이지 않는다',
     pre='관리자 계정, 업체 "녹화창조" 의 GROUND_MAINTENANCE 성공 제안서, 회사명 "테스트전기" 고객의 GROUND_MAINTENANCE 공고 입찰 요청(제출 서류 있음)',
     steps='1. 그 입찰 요청을 GENERATING 으로 전이하고 생성이 끝날 때까지 기다린다\n2. 입찰 상세 관리 생성 문서 목록에서 제안서를 열어 읽는다',
     expect='1. 제안서 본문에 성공 제안서 원본의 회사명 "녹화창조" 가 없다\n'
            '2. 제안서 본문의 실적, 수치가 고객이 낸 서류의 값이고 성공 제안서 원본의 실적, 수치가 들어 있지 않다',
     judge=['합동', '합동'],
     req=['BID-RFP-005'], fc=['BID-RFP-005'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-13-031', flow='사업유형 분류', kind='정상', title='NAICS 코드로 공고 사업유형을 정한다',
     pre='관리자 계정, NAICS 코드가 분류 규칙의 한 유형에 맞는 공고가 SAM.gov 에 있는 날',
     steps='1. 관리자 화면 [SAM.gov 수집] 에서 수동 수집을 실행한다\n2. 수집된 그 공고의 opportunities 행을 본다\n'
           '3. 같은 공고를 다시 수집한 뒤 그 행을 다시 본다',
     expect='1. 그 공고의 industry_type 이 그 NAICS 코드에 맞는 유형이다\n'
            '2. 다시 수집해도 industry_type 이 같은 값이다',
     judge=['서버', '서버'],
     req=['BID-OPP-010'], fc=['BID-OPP-010'], screen='', pri='Medium'),

dict(id='IT-13-032', flow='사업유형 분류', kind='정상', title='NAICS 가 맞지 않으면 PSC 로 사업유형을 정한다',
     pre='관리자 계정, NAICS 코드는 어느 유형에도 맞지 않고 PSC(classificationCode)가 한 유형에 맞는 공고',
     steps='1. [SAM.gov 수집] 에서 수동 수집을 실행한다\n2. 수집된 그 공고의 opportunities 행을 본다',
     expect='1. 그 공고의 industry_type 이 그 PSC 에 맞는 유형이다',
     judge=['서버'],
     req=['BID-OPP-010'], fc=['BID-OPP-010'], screen='', pri='Medium'),

dict(id='IT-13-033', flow='사업유형 분류', kind='정상', title='NAICS 와 PSC 가 맞지 않으면 제목 낱말로 사업유형을 정한다',
     pre='관리자 계정, NAICS 와 PSC 는 어느 유형에도 맞지 않고 제목에 한 유형의 키워드가 있는 공고',
     steps='1. [SAM.gov 수집] 에서 수동 수집을 실행한다\n2. 수집된 그 공고의 opportunities 행을 본다',
     expect='1. 그 공고의 industry_type 이 제목 키워드에 맞는 유형이다',
     judge=['서버'],
     req=['BID-OPP-010'], fc=['BID-OPP-010'], screen='', pri='Medium'),

dict(id='IT-13-034', flow='사업유형 분류', kind='정상', title='NAICS 와 PSC 가 서로 다른 유형이면 NAICS 를 따른다',
     pre='관리자 계정, NAICS 는 유형 A 에 맞고 PSC 와 제목은 유형 B 에 맞는 공고',
     steps='1. [SAM.gov 수집] 에서 수동 수집을 실행한다\n2. 수집된 그 공고의 opportunities 행을 본다',
     expect='1. 그 공고의 industry_type 이 유형 A 다',
     judge=['서버'],
     req=['BID-OPP-010'], fc=['BID-OPP-010'], screen='', pri='Medium'),

dict(id='IT-13-035', flow='사업유형 분류', kind='정상', title='어느 규칙에도 맞지 않는 공고는 미분류로 둔다',
     pre='관리자 계정, NAICS, PSC, 제목 어느 것도 8개 유형에 맞지 않는 공고',
     steps='1. [SAM.gov 수집] 에서 수동 수집을 실행한다\n2. 수집된 그 공고의 opportunities 행을 본다\n'
           '3. opportunities 의 industry_type 값 종류를 센다',
     expect='1. 그 공고의 industry_type 이 NULL 이다\n'
            '2. opportunities 의 industry_type 값에 UNKNOWN 이 하나도 없다',
     judge=['서버', '서버'],
     req=['BID-OPP-010'], fc=['BID-OPP-010'], screen='', pri='Medium'),

]
