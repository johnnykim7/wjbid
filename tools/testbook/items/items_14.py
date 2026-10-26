# 영역 14 MCP/알림 시험항목. 근거는 T1-1 기능요구사항 명세서, T1-3 BIZ-012·013·061, T1-4 POL-009·011, T1-6 이벤트 계약, T3-1, T3-2 MCP 서버 절이다.


MCP_KEY = {'X-API-Key': '$TESTBOOK_MCP_API_KEY'}
THIS_BID = "UNHEX('{val:0:hex}')"


def mcp(tool, args):
    return ('api', 'guest', 'POST', '/api/mcp',
            {'jsonrpc': '2.0', 'id': 1, 'method': 'tools/call', 'params': {'name': tool, 'arguments': args}}, MCP_KEY)


TO_GENERATING = mcp('transition_bid_state', {'bidRequestId': '{val:0:id}', 'targetState': 'GENERATING', 'notes': 'IT-14-043 서버 판정'})

ITEMS = [

dict(id='IT-14-001', flow='MCP 연결', kind='정상', title='POST /mcp 로 initialize 를 보내 서버 정보를 받는다',
     pre='계정 로그인 없음, 서버 열쇠(MCP_API_KEY)를 아는 호출자',
     steps='1. POST /mcp 에 X-API-Key 머리글(서버 열쇠)과 본문 {"jsonrpc":"2.0","id":1,"method":"initialize","params":{}} 를 보낸다',
     expect='1. 같은 HTTP 응답 본문으로 jsonrpc "2.0", id 1 인 JSON-RPC 응답이 온다\n'
            '2. 응답 result 에 서버 정보와 capabilities 가 있다',
     judge=['서버', '서버'],
     req=['BID-MCP-001'], fc=['BID-MCP-001'], screen='', pri='High'),

dict(id='IT-14-002', flow='MCP 연결', kind='정상', title='tools/list 로 설계된 도구 이름을 모두 받는다',
     pre='계정 로그인 없음, 서버 열쇠(MCP_API_KEY)를 아는 호출자',
     steps='1. POST /mcp 에 X-API-Key 머리글과 본문 {"jsonrpc":"2.0","id":2,"method":"tools/list"} 를 보낸다',
     expect='1. result 의 도구 목록에 get_opportunity, search_opportunities, get_opportunity_requirements, get_bid_request, '
            'save_document_version, get_document_template, transition_bid_state, get_opportunity_analysis, save_opportunity_analysis, '
            'get_past_submissions, get_reference_samples, save_pattern_guide, get_proposal_design_input, save_proposal_structure, '
            'get_section_context, save_section_blocks, get_section_verify_input, get_notice_verify_input, save_verification_result 19개 이름이 모두 있다',
     judge=['서버'],
     req=['BID-MCP-001'], fc=['BID-MCP-001'], screen='', pri='High'),

dict(id='IT-14-003', flow='MCP 연결', kind='정상', title='SSE 로 연결해 메시지 주소를 받고 응답을 SSE 이벤트로 받는다',
     pre='계정 로그인 없음, 서버 열쇠(MCP_API_KEY)를 아는 호출자',
     steps='1. GET /mcp/sse 에 X-API-Key 머리글로 연결해 둔다\n'
           '2. 받은 endpoint 이벤트의 주소(POST /mcp/message?sessionId={id})로 X-API-Key 머리글과 tools/list 요청을 보낸다\n'
           '3. 같은 tools/list 를 POST /mcp 로도 보낸다',
     expect='1. 연결 직후 endpoint 이벤트로 sessionId 가 붙은 /mcp/message 주소가 온다\n'
            '2. tools/list 의 JSON-RPC 응답이 열어 둔 SSE 연결의 message 이벤트로 온다\n'
            '3. message 이벤트의 도구 목록이 POST /mcp 의 tools/list 결과와 같다',
     judge=['서버', '서버', '서버'],
     req=['BID-MCP-001'], fc=['BID-MCP-001'], screen='', pri='High'),

dict(id='IT-14-004', flow='MCP 연결', kind='정상', title='SSE 연결에 30초마다 heartbeat 가 온다',
     pre='계정 로그인 없음, 서버 열쇠(MCP_API_KEY)를 아는 호출자',
     steps='1. GET /mcp/sse 에 X-API-Key 머리글로 연결한다\n2. 요청을 보내지 않고 70초 동안 받는 이벤트를 적는다',
     expect='1. 요청 없이도 30초 간격으로 heartbeat 가 와 70초 동안 2번 이상 받는다',
     judge=['서버'],
     req=['BID-MCP-001'], fc=['BID-MCP-001'], screen='', pri='High'),

dict(id='IT-14-005', flow='MCP 연결', kind='정상', title='같은 도구 호출은 어느 전송으로 몇 번 보내도 같은 결과다',
     pre='계정 로그인 없음, 서버 열쇠(MCP_API_KEY)를 아는 호출자, 공고 1건의 id',
     steps='1. POST /mcp 로 get_opportunity(opportunityId 그 공고)를 두 번 보낸다\n'
           '2. SSE 세션(POST /mcp/message?sessionId={id})으로 같은 요청을 보낸다',
     expect='1. POST /mcp 두 번의 result 가 같다\n'
            '2. SSE message 이벤트로 받은 result 가 POST /mcp 의 result 와 같다',
     judge=['서버', '서버'],
     req=['BID-MCP-001'], fc=['BID-MCP-001'], screen='', pri='High'),

dict(id='IT-14-006', flow='MCP 열쇠', kind='예외', title='열쇠 없이 /mcp 를 부르면 401 이고 도구가 돌지 않는다',
     pre='계정 로그인 없음, 상태 CREATED 입찰 요청 1건',
     steps='1. X-API-Key 머리글 없이 POST /mcp 에 tools/call 본문(name transition_bid_state, bidRequestId 그 건, targetState DOCS_PENDING)을 보낸다\n'
           '2. 서버 로그의 막힘 기록을 본다',
     expect='1. 응답이 401 이고 본문이 없다\n'
            '2. 그 입찰 요청 state 가 CREATED 그대로이고 state_history 줄 수가 늘지 않는다\n'
            '3. 막힘 기록에 방법, 경로, 보낸 곳이 있고 열쇠 값은 없다',
     judge=['서버', '서버', '단위'],
     req=['BID-MCP-001'], fc=['BID-MCP-001'], screen='', pri='High'),

dict(id='IT-14-007', flow='MCP 열쇠', kind='예외', title='틀린 열쇠로는 SSE 연결과 메시지 보내기가 막힌다',
     pre='계정 로그인 없음, 서버 열쇠와 다른 열쇠 값, 맞는 열쇠로 연 SSE 세션 id',
     steps='1. 틀린 열쇠를 X-API-Key 머리글에 넣어 GET /mcp/sse 에 연결한다\n'
           '2. 틀린 열쇠로 POST /mcp/message?sessionId={id} 에 tools/list 를 보낸다',
     expect='1. GET /mcp/sse 가 401 이고 endpoint 이벤트가 오지 않는다\n'
            '2. POST /mcp/message 가 401 이고 본문이 없다',
     judge=['서버', '서버'],
     req=['BID-MCP-001'], fc=['BID-MCP-001'], screen='', pri='High'),

dict(id='IT-14-008', flow='MCP 열쇠', kind='예외', title='서버 열쇠가 비어 있으면 /mcp 요청을 모두 막는다',
     pre='계정 로그인 없음, MCP_API_KEY 를 비운 채 띄운 서버',
     steps='1. X-API-Key 머리글을 빈 값으로 POST /mcp 에 tools/list 를 보낸다\n2. 서버 기동 로그를 본다',
     expect='1. 응답이 401 이다\n2. 기동 로그에 서버 열쇠가 비었다는 경고가 남는다',
     judge=['단위', '단위'],
     req=['BID-MCP-001'], fc=['BID-MCP-001'], screen='', pri='High'),

dict(id='IT-14-009', flow='MCP 열쇠', kind='정상', title='서명 주소로 머리글 열쇠 없이 원본을 내려받는다',
     pre='계정 로그인 없음, 서버 열쇠(MCP_API_KEY)를 아는 호출자, 파일이 있는 GROUND_MAINTENANCE 성공 제안서, 내려받기 기한 처음 값 1시간',
     steps='1. POST /mcp 로 get_reference_samples(industryType GROUND_MAINTENANCE)를 보내고 받은 시각을 적는다\n'
           '2. 응답 files 의 downloadUrl 하나를 X-API-Key 머리글 없이 GET 으로 연다',
     expect='1. downloadUrl 이 {SELF_BASE_URL}/mcp/rfp-files/{id}/download?exp=...&sig=... 꼴이고 exp 가 받은 시각에서 1시간 뒤다\n'
            '2. 머리글 없이 연 주소로 그 원본 파일이 올린 파일과 같은 크기로 내려받아진다',
     judge=['서버', '서버'],
     req=['BID-MCP-001'], fc=['BID-MCP-001'], screen='', pri='High'),

dict(id='IT-14-010', flow='MCP 열쇠', kind='정상', title='머리글 열쇠로는 서명 없이도 공고 첨부 원본을 내려받는다',
     pre='계정 로그인 없음, 서버 열쇠(MCP_API_KEY)를 아는 호출자, 첨부가 있는 공고의 첨부 id',
     steps='1. GET /mcp/opportunity-attachments/{id}/download 를 exp, sig 없이 X-API-Key 머리글(서버 열쇠)로 부른다',
     expect='1. 그 첨부 원본이 저장된 파일과 같은 크기로 내려받아진다',
     judge=['서버'],
     req=['BID-MCP-001', 'BID-MCP-002'], fc=['BID-MCP-001', 'BID-MCP-002'], screen='', pri='High'),

dict(id='IT-14-011', flow='MCP 열쇠', kind='예외', title='서명도 머리글 열쇠도 없으면 내려받기가 막힌다',
     pre='계정 로그인 없음, 공고 첨부 id',
     steps='1. GET /mcp/opportunity-attachments/{id}/download 를 exp, sig, X-API-Key 머리글 없이 부른다',
     expect='1. 응답이 401 이고 본문이 없다',
     judge=['서버'],
     req=['BID-MCP-001'], fc=['BID-MCP-001'], screen='', pri='High'),

dict(id='IT-14-012', flow='MCP 열쇠', kind='예외', title='기한이 지난 서명 주소로는 내려받지 못한다',
     pre='계정 로그인 없음, 내려받기 기한(MCP_DOWNLOAD_URL_TTL)을 60초로 띄운 서버, 성공 제안서 원본 파일',
     steps='1. POST /mcp 로 get_reference_samples 를 보내 downloadUrl 을 받는다\n2. 90초 기다린 뒤 그 주소를 머리글 없이 연다',
     expect='1. 응답이 401 이고 본문이 없다',
     judge=['서버'],
     req=['BID-MCP-001'], fc=['BID-MCP-001'], screen='', pri='High'),

dict(id='IT-14-013', flow='MCP 열쇠', kind='예외', title='한 파일의 서명을 다른 파일 주소에 쓰면 막힌다',
     pre='계정 로그인 없음, 서로 다른 성공 제안서 원본 파일 A, B',
     steps='1. POST /mcp 로 get_reference_samples 를 보내 파일 A 의 downloadUrl 을 받는다\n'
           '2. 그 주소의 파일 id 만 파일 B 의 id 로 바꿔 머리글 없이 연다',
     expect='1. 응답이 401 이고 파일 B 가 내려받아지지 않는다',
     judge=['서버'],
     req=['BID-MCP-001'], fc=['BID-MCP-001'], screen='', pri='High'),

dict(id='IT-14-014', flow='공고 도구', kind='정상', title='get_opportunity 로 공고 상세를 받는다',
     pre='계정 로그인 없음, 서버 열쇠(MCP_API_KEY)를 아는 호출자, 공고 1건의 id',
     steps='1. POST /mcp 에 tools/call 본문(name get_opportunity, opportunityId 그 공고 id)을 보낸다',
     expect='1. result 에 그 공고의 공고번호와 제목이 있고 opportunities 그 행의 값과 같다',
     judge=['서버'],
     req=['BID-MCP-002'], fc=['BID-MCP-002'], screen='', pri='High'),

dict(id='IT-14-015', flow='공고 도구', kind='정상', title='search_opportunities 로 키워드 공고를 개수 한도 안에서 받는다',
     pre='계정 로그인 없음, 서버 열쇠(MCP_API_KEY)를 아는 호출자, USFK 가 들어간 공고 6건 이상',
     steps='1. POST /mcp 에 tools/call 본문(name search_opportunities, keyword "USFK", limit 5)을 보낸다',
     expect='1. 결과 공고가 5건 이하다\n'
            '2. 결과 공고마다 제목, 본문, 공고번호 중 하나에 USFK 가 들어 있다',
     judge=['서버', '서버'],
     req=['BID-MCP-002'], fc=['BID-MCP-002'], screen='', pri='High'),

dict(id='IT-14-016', flow='공고 도구', kind='정상', title='get_opportunity_requirements 는 그 공고의 제출 서류 요구 항목만 준다',
     pre='계정 로그인 없음, 서버 열쇠(MCP_API_KEY)를 아는 호출자, 공고 분석으로 req_id 가 빈 제출 서류 줄과 req_id 가 있는 매트릭스 줄이 함께 채워진 공고',
     steps='1. POST /mcp 에 tools/call 본문(name get_opportunity_requirements, opportunityId 그 공고 id)을 보낸다',
     expect='1. result 의 요구 항목이 모두 그 공고의 제출 서류 줄이고 매트릭스 줄은 없다\n'
            '2. result 줄 수가 그 공고의 제출 서류 요구 항목 줄 수와 같다',
     judge=['서버', '서버'],
     req=['BID-MCP-002'], fc=['BID-MCP-002'], screen='', pri='High'),

dict(id='IT-14-017', flow='공고 도구', kind='정상', title='get_opportunity_analysis 로 사전 분석 결과를 받는다',
     pre='계정 로그인 없음, 서버 열쇠(MCP_API_KEY)를 아는 호출자, 사전 분석이 COMPLETED 인 공고, 관리자 계정',
     steps='1. POST /mcp 에 tools/call 본문(name get_opportunity_analysis, opportunityId 그 공고 id)을 보낸다\n'
           '2. 관리자 화면 [원본 공고] 에서 그 공고 상세의 사전 분석 결과를 본다',
     expect='1. result 의 요약, 양식, 필요서류가 그 공고의 사전 분석 저장 값과 같다\n'
            '2. 관리자 공고 상세의 요약이 result 의 요약과 같다',
     judge=['서버', '가상'],
     req=['BID-MCP-002'], fc=['BID-MCP-002'], screen='관리자 /opportunities/{id}', pri='High'),

dict(id='IT-14-018', flow='공고 도구', kind='정상', title='save_opportunity_analysis 로 분석 결과를 저장하면 요구 항목이 채워진다',
     pre='서버 열쇠(MCP_API_KEY)를 아는 호출자, 사전 분석이 ANALYZING 인 공고, 그 공고에 입찰한 회원 고객 계정, 관리자 계정',
     steps='1. POST /mcp 에 tools/call 본문(name save_opportunity_analysis, opportunityId 그 공고, summary, documentFormats, '
           'requiredDocuments 에 고객 올림 서류 "사업자등록증", llmPromptPreset)을 보낸다\n'
           '2. 관리자 화면 그 공고 상세의 사전 분석 결과를 본다\n'
           '3. 고객 계정으로 고객 화면 입찰 건별 제출 전 검사(/documents/pre-check/{입찰 건 id})를 연다',
     expect='1. 응답에 error 가 없다\n'
            '2. 그 공고 사전 분석의 요약, 양식, 필요서류, 프롬프트 프리셋이 보낸 값으로 저장된다\n'
            '3. 관리자 공고 상세의 사전 분석 요약이 보낸 summary 와 같다\n'
            '4. 보낸 필요서류 "사업자등록증" 이 그 공고의 요구 항목 줄로 생긴다\n'
            '5. 입찰 건별 제출 전 검사의 올릴 서류에 "사업자등록증" 줄이 보인다',
     judge=['서버', '서버', '가상', '서버', '가상'],
     req=['BID-MCP-002'], fc=['BID-MCP-002'], screen='/documents/pre-check/:bidRequestId', pri='High'),

dict(id='IT-14-019', flow='공고 도구', kind='정상', title='save_opportunity_analysis 로 매트릭스와 평가 항목 구조를 공고문에 담는다',
     pre='계정 로그인 없음, 서버 열쇠(MCP_API_KEY)를 아는 호출자, 공고문이 있고 사전 분석이 ANALYZING 인 공고',
     steps='1. POST /mcp 에 tools/call 본문(name save_opportunity_analysis, opportunityId 그 공고, summary, complianceMatrix 에 level SHALL 줄 2개, factorTree)을 보낸다',
     expect='1. 그 공고 공고문의 compliance_matrix_json 에 보낸 complianceMatrix 가 담긴다\n'
            '2. 그 공고문의 factor_tree_json 에 보낸 factorTree 가 담긴다\n'
            '3. 그 공고의 요구 항목에 req_id 가 채워진 매트릭스 줄이 2줄 생기고 두 줄 모두 req_level 이 SHALL 이다',
     judge=['서버', '서버', '서버'],
     req=['BID-MCP-002'], fc=['BID-MCP-002'], screen='', pri='High'),

dict(id='IT-14-020', flow='공고 도구', kind='정상', title='get_past_submissions 로 그 회원의 제출 이력만 받는다',
     pre='서버 열쇠(MCP_API_KEY)를 아는 호출자, 제출 이력이 3건인 회원 A, 제출 이력이 있는 회원 B',
     steps='1. POST /mcp 에 tools/call 본문(name get_past_submissions, memberId 회원 A, limit 2)을 보낸다',
     expect='1. 결과가 2건 이하다\n2. 결과가 모두 회원 A 의 제출 이력이고 회원 B 의 것이 없다',
     judge=['서버', '서버'],
     req=['BID-MCP-002'], fc=['BID-MCP-002'], screen='', pri='High'),

dict(id='IT-14-021', flow='공고 도구', kind='예외', title='걷어낸 save_requirements 도구는 쓸 수 없다',
     pre='서버 열쇠(MCP_API_KEY)를 아는 호출자, 공고 1건',
     steps='1. POST /mcp 로 tools/list 를 보낸다\n2. tools/call 본문(name save_requirements, opportunityId 그 공고)을 보낸다',
     expect='1. tools/list 결과에 save_requirements 가 없다\n'
            '2. save_requirements 호출 응답에 JSON-RPC error 가 있고 그 공고의 요구 항목 줄 수가 늘지 않는다',
     judge=['서버', '서버'],
     req=['BID-MCP-002'], fc=['BID-MCP-002'], screen='', pri='High'),

dict(id='IT-14-022', flow='문서 도구', kind='정상', title='save_document_version 으로 새 문서와 첫 버전을 만든다',
     pre='서버 열쇠(MCP_API_KEY)를 아는 호출자, COVER_LETTER 문서가 없는 상태 GENERATING 입찰 요청, 관리자 계정',
     steps='1. POST /mcp 에 tools/call 본문(name save_document_version, bidRequestId 그 건, documentType COVER_LETTER, contentJson)을 보낸다\n'
           '2. 관리자 화면 그 입찰 요청의 입찰 상세 관리를 연다',
     expect='1. bid_documents 에 그 입찰 요청의 COVER_LETTER 문서가 1줄 생긴다\n'
            '2. 그 문서의 첫 버전이 보낸 contentJson 으로 생긴다\n'
            '3. 입찰 상세 관리 생성 문서 목록에 COVER_LETTER 문서 줄이 보인다',
     judge=['서버', '서버', '가상'],
     req=['BID-MCP-003'], fc=['BID-MCP-003'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-14-023', flow='문서 도구', kind='정상', title='save_document_version 을 다시 부르면 새 버전만 더한다',
     pre='서버 열쇠(MCP_API_KEY)를 아는 호출자, 버전 1 이 있는 COVER_LETTER 문서의 입찰 요청',
     steps='1. POST /mcp 로 같은 bidRequestId, documentType COVER_LETTER 에 다른 contentJson 을 보내 save_document_version 을 부른다',
     expect='1. 그 문서에 버전 번호가 1 늘어난 새 버전이 보낸 contentJson 으로 생긴다\n'
            '2. 앞 버전의 contentJson 이 바뀌지 않는다',
     judge=['서버', '서버'],
     req=['BID-MCP-003'], fc=['BID-MCP-003'], screen='', pri='High'),

dict(id='IT-14-024', flow='문서 도구', kind='정상', title='get_document_template 로 문서 양식을 받는다',
     pre='서버 열쇠(MCP_API_KEY)를 아는 호출자, COVER_LETTER 양식이 등록된 상태, 관리자 계정',
     steps='1. POST /mcp 에 tools/call 본문(name get_document_template, documentType COVER_LETTER)을 보낸다\n'
           '2. 관리자 화면 [문서 템플릿] 에서 COVER_LETTER 양식을 연다',
     expect='1. result 의 양식 본문이 관리자 문서 템플릿 화면의 COVER_LETTER 양식 본문과 같다',
     judge=['서버'],
     req=['BID-MCP-003'], fc=['BID-MCP-003'], screen='', pri='High'),

dict(id='IT-14-025', flow='상태 도구', kind='정상', title='get_bid_request 로 입찰 요청과 문서 목록을 받는다',
     pre='서버 열쇠(MCP_API_KEY)를 아는 호출자, 생성 문서 2건이 있는 입찰 요청',
     steps='1. POST /mcp 에 tools/call 본문(name get_bid_request, bidRequestId 그 건)을 보낸다',
     expect='1. result 의 상태가 그 입찰 요청 state 와 같다\n'
            '2. result 의 문서 목록에 그 입찰 요청의 문서 2건이 있다',
     judge=['서버', '서버'],
     req=['BID-MCP-004'], fc=['BID-MCP-004'], screen='', pri='High'),

dict(id='IT-14-026', flow='상태 도구', kind='정상', title='transition_bid_state 로 허용된 상태 전이를 한다',
     pre='서버 열쇠(MCP_API_KEY)를 아는 호출자, 상태 CREATED 입찰 요청, 관리자 계정',
     steps='1. POST /mcp 에 tools/call 본문(name transition_bid_state, bidRequestId 그 건, targetState DOCS_PENDING)을 보낸다\n'
           '2. 관리자 화면 그 입찰 요청의 입찰 상세 관리를 연다',
     expect='1. 그 입찰 요청 state 가 DOCS_PENDING 이고 state_history 에 CREATED 에서 DOCS_PENDING 으로의 줄이 더해진다\n'
            '2. 입찰 상세 관리 상태 패널의 현재 상태가 DOCS_PENDING 이다',
     judge=['서버', '가상'],
     req=['BID-MCP-004'], fc=['BID-MCP-004'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-14-027', flow='상태 도구', kind='예외', title='transition_bid_state 로도 허용 목록 밖 전이는 거부된다',
     pre='서버 열쇠(MCP_API_KEY)를 아는 호출자, 상태 ANALYZING 입찰 요청',
     steps='1. POST /mcp 에 tools/call 본문(name transition_bid_state, bidRequestId 그 건, targetState REVIEW)을 보낸다',
     expect='1. 응답에 오류가 담긴다\n'
            '2. 그 입찰 요청 state 가 ANALYZING 그대로이고 state_history 줄 수가 늘지 않는다',
     judge=['서버', '서버'],
     req=['BID-MCP-004'], fc=['BID-MCP-004'], screen='', pri='High'),

dict(id='IT-14-028', flow='워크플로우 실행', kind='정상', title='GENERATING 전이 때 Aimbase 문서 생성 워크플로우가 자동으로 돈다',
     pre='관리자 계정, 상태 ANALYZING 이고 공고 사전 분석이 COMPLETED 이며 입찰 분석의 관리자 결정이 Go 인 입찰 요청, Aimbase 에 닿는 배포 환경',
     steps='1. 관리자 화면 그 입찰 요청의 입찰 상세 관리에서 GENERATING 으로 전이한다\n'
           '2. Aimbase 실행 기록에서 그 입찰 요청의 문서 설계 실행을 연다\n3. 실행이 끝날 때까지 기다린다',
     expect='1. Aimbase 에 문서 설계 워크플로우(proposal-design) 실행이 그 입찰 요청이 만들 문서 종류마다 1건씩 생기고, 입력에 그 입찰 요청 id 와 그 문서 종류가 있다\n'
            '2. 절 작성 실행이 이 플랫폼의 MCP 도구 save_section_blocks 로 절 본문을 저장하고, 플랫폼이 그 절들을 묶어 문서마다 새 버전 1개(버전 2)를 저장한다\n'
            '3. 모든 문서 종류의 작성이 끝나면 그 입찰 요청 state 가 REVIEW 로 바뀐다\n'
            '4. 플랫폼이 실행 결과를 5초 간격(설정 app.aimbase.polling.interval-ms 기본값)으로 묻는다\n'
            '5. 플랫폼의 실행 요청과 결과 묻기 요청에 X-API-Key 머리글이 실린다',
     judge=['합동', '합동', '서버', '단위', '단위'],
     req=['BID-MCP-005'], fc=['BID-MCP-005'], screen='관리자 /bid-requests/{id}', pri='High',
     issue='BID-MCP-005 는 REQUIREMENT_ANALYSIS, DOCUMENT_DRAFTING 전이 때 실행한다고 적지만 그런 상태가 BidRequestState 에 없다. T1-5 CR-141 은 ANALYZING 진입 때 AI 작업이 없다고 적는다'),

dict(id='IT-14-029', flow='워크플로우 실행', kind='예외', title='워크플로우 결과 묻기는 60회에서 멈춘다',
     pre='관리자 계정, 60회 묻는 동안 끝나지 않도록 만든 Aimbase 워크플로우 실행',
     steps='1. 문서 생성 워크플로우를 시작시키고 결과 묻기가 멈출 때까지 기다린다',
     expect='1. 결과 묻기를 60회 하고 더 묻지 않는다',
     judge=['단위'],
     req=['BID-MCP-005'], fc=['BID-MCP-005'], screen='', pri='High'),

dict(id='IT-14-030', flow='워크플로우 실행', kind='예외', title='워크플로우가 실패하면 관리자 알림을 남긴다',
     pre='관리자 계정, 실패로 끝나도록 만든 Aimbase 문서 생성 워크플로우, 상태 ANALYZING 이고 입찰 분석의 관리자 결정이 Go 인 입찰 요청',
     steps='1. 그 입찰 요청을 GENERATING 으로 전이해 워크플로우를 시작시킨다\n2. 실행이 failed 로 끝날 때까지 기다린다',
     expect='1. notification_logs 에 AI_WORKFLOW_FAILED 줄이 관리자 주소로 생긴다\n'
            '2. 그 입찰 요청 state 가 REVIEW 로 바뀌지 않는다',
     judge=['단위', '단위'],
     req=['BID-MCP-005'], fc=['BID-MCP-005'], screen='', pri='High'),

dict(id='IT-14-031', flow='워크플로우 실행', kind='예외', title='Aimbase 가 부른 상태 전이로 워크플로우가 다시 돌지 않는다',
     pre='서버 열쇠(MCP_API_KEY)를 아는 호출자, 문서 생성 워크플로우가 도는 중인 상태 GENERATING 입찰 요청',
     steps='1. 워크플로우 실행 안에서 transition_bid_state 로 그 입찰 요청을 GENERATING 진입으로 이어지는 전이를 부른다',
     expect='1. 그 전이로 새 문서 생성 워크플로우 실행이 시작되지 않는다',
     judge=['단위'],
     req=['BID-MCP-005'], fc=['BID-MCP-005'], screen='', pri='High'),

dict(id='IT-14-032', flow='이메일 발송', kind='정상', title='알림 메일을 보내고 발송 이력을 남긴다',
     pre='SMTP 설정이 된 서버, 관리자 계정(메일을 받을 수 있는 주소), 회원 고객 계정',
     steps='1. 고객 화면 고객센터(/support)에서 회원 계정으로 1:1 문의를 보낸다\n2. 관리자 메일함을 본다\n'
           '3. notification_logs 에서 그 문의를 참조하는 줄을 본다',
     expect='1. 관리자 메일함에 문의 접수 메일이 HTML 본문으로 온다\n'
            '2. notification_logs 에 notification_type INQUIRY_RECEIVED, recipient_email 그 관리자 주소, subject, sent_at, success true, '
            'idempotency_key 가 채워진 줄이 생긴다',
     judge=['합동', '서버'],
     req=['BID-NOTIFY-001'], fc=['BID-NOTIFY-001'], screen='/support', pri='High',
     issue='T1-1 BID-NOTIFY-001 은 Spring Mail 기반이라 적고 T3-1 알림 유형은 bp-notification 템플릿으로 보낸다고 적는다'),

dict(id='IT-14-033', flow='이메일 발송', kind='예외', title='메일 발송이 실패하면 실패 이력을 남긴다',
     pre='SMTP 가 오류를 내도록 만든 서버, 알림을 일으키는 사건 1건',
     steps='1. SMTP 오류 상태에서 알림을 일으킨다\n2. notification_logs 의 그 줄을 본다',
     expect='1. notification_logs 에 success false 이고 error_message 에 실패 사유가 있는 줄이 생긴다',
     judge=['단위'],
     req=['BID-NOTIFY-001'], fc=['BID-NOTIFY-001'], screen='', pri='High'),

dict(id='IT-14-034', flow='이메일 발송', kind='예외', title='메일 발송 실패 때 3회까지 다시 보낸다',
     pre='SMTP 가 계속 오류를 내도록 만든 서버, 알림을 일으키는 사건 1건',
     steps='1. SMTP 오류 상태에서 알림을 일으킨다\n2. 발송 시도 횟수와 간격을 적는다',
     expect='1. 처음 시도 뒤 3회까지 다시 보내고 그 뒤에는 보내지 않는다\n'
            '2. 다시 보내는 간격이 지수 백오프로 늘어난다',
     judge=['단위', '단위'],
     req=['BID-NOTIFY-001'], fc=['BID-NOTIFY-001'], screen='', pri='High'),

dict(id='IT-14-035', flow='마감 알림', kind='정상', title='마감 7일 전 진행 중 입찰 건에 관리자 전원과 고객에게 마감 알림을 보낸다',
     pre='DEADLINE_ALERT_DAYS 가 7,3,1, 관리자 계정 2개, 회원 고객 A 의 상태 DOCS_PENDING 입찰 요청(공고 마감이 서울 날짜로 7일 뒤)',
     steps='1. 매일 09:00(Asia/Seoul) 마감 알림 예약 작업이 돌 때까지 기다린다(시험 서버는 app.deadline-alert.cron 을 가까운 시각으로 바꾼다)\n'
           '2. 관리자 두 명과 고객 A 의 메일함을 본다\n3. notification_logs 를 본다',
     expect='1. notification_logs 에 DEADLINE_ALERT 줄이 관리자 두 명과 고객 A 에게 1줄씩 3줄 생기고 reference 가 그 입찰 요청이다\n'
            '2. 세 사람 메일함에 그 입찰 요청의 마감 알림 메일이 1통씩 온다',
     judge=['서버', '합동'],
     req=['BID-NOTIFY-002'], fc=['BID-NOTIFY-002'], screen='', pri='Medium'),

dict(id='IT-14-036', flow='마감 알림', kind='정상', title='마감 3일 전과 1일 전에도 보내고 다른 날에는 보내지 않는다',
     pre='DEADLINE_ALERT_DAYS 가 7,3,1, 마감이 서울 날짜로 3일, 1일, 2일, 5일, 8일 남은 진행 중 입찰 요청 각 1건',
     steps='1. 마감 알림 예약 작업이 돌 때까지 기다린다\n2. notification_logs 를 입찰 요청별로 본다',
     expect='1. 3일, 1일 남은 입찰 요청에 DEADLINE_ALERT 줄이 생긴다\n'
            '2. 2일, 5일, 8일 남은 입찰 요청에는 DEADLINE_ALERT 줄이 생기지 않는다',
     judge=['서버', '서버'],
     req=['BID-NOTIFY-002'], fc=['BID-NOTIFY-002'], screen='', pri='Medium'),

dict(id='IT-14-037', flow='마감 알림', kind='정상', title='알림 날 설정을 바꾸면 바꾼 날에만 보낸다',
     pre='system_settings DEADLINE_ALERT_DAYS 를 14 로 바꾼 상태, 마감이 14일, 7일 남은 진행 중 입찰 요청 각 1건',
     steps='1. 마감 알림 예약 작업이 돌 때까지 기다린다\n2. notification_logs 를 입찰 요청별로 본다',
     expect='1. 14일 남은 입찰 요청에 DEADLINE_ALERT 줄이 생긴다\n'
            '2. 7일 남은 입찰 요청에는 DEADLINE_ALERT 줄이 생기지 않는다',
     judge=['서버', '서버'],
     req=['BID-NOTIFY-002'], fc=['BID-NOTIFY-002'], screen='', pri='Medium'),

dict(id='IT-14-038', flow='마감 알림', kind='정상', title='남은 날은 오늘과 마감일을 서울 날짜로 세어 정한다',
     pre='DEADLINE_ALERT_DAYS 가 7,3,1, 서버 시간대 UTC, 마감 시각이 서울 시각 02:00 이고 서울 날짜로 7일 뒤인 진행 중 입찰 요청',
     steps='1. 서울 시각 09:00 에 마감 알림 예약 작업을 돌린다',
     expect='1. 그 입찰 요청을 7일 전으로 세어 DEADLINE_ALERT 를 보낸다',
     judge=['단위'],
     req=['BID-NOTIFY-002'], fc=['BID-NOTIFY-002'], screen='', pri='Medium'),

dict(id='IT-14-039', flow='마감 알림', kind='정상', title='마감일이 바뀌면 새 일정으로 다시 보낸다',
     pre='DEADLINE_ALERT_DAYS 가 7,3,1, 7일 전 마감 알림을 이미 받은 진행 중 입찰 요청',
     steps='1. 그 공고 마감일을 하루 뒤로 바꿔 다음 날 다시 7일 남게 한다\n2. 다음 날 마감 알림 예약 작업이 돌 때까지 기다린다\n'
           '3. notification_logs 를 본다',
     expect='1. 그 입찰 요청에 새 마감일로 DEADLINE_ALERT 줄이 받는 사람마다 다시 생긴다\n'
            '2. 새 줄의 idempotency_key 가 앞 줄과 다르다',
     judge=['서버', '서버'],
     req=['BID-NOTIFY-002'], fc=['BID-NOTIFY-002'], screen='', pri='Medium'),

dict(id='IT-14-040', flow='마감 알림', kind='예외', title='진행 중이 아닌 입찰 건에는 마감 알림을 보내지 않는다',
     pre='DEADLINE_ALERT_DAYS 가 7,3,1, 마감이 7일 남은 상태 SUBMITTED 입찰 요청과 상태 CLOSED 입찰 요청, 각 고객 계정',
     steps='1. 마감 알림 예약 작업이 돌 때까지 기다린다\n2. notification_logs 와 두 고객 메일함을 본다',
     expect='1. 두 입찰 요청에 DEADLINE_ALERT 줄이 생기지 않는다\n'
            '2. 두 고객 메일함에 마감 알림 메일이 오지 않는다',
     judge=['서버', '합동'],
     req=['BID-NOTIFY-002'], fc=['BID-NOTIFY-002'], screen='', pri='Medium'),

dict(id='IT-14-041', flow='마감 알림', kind='예외', title='같은 날 예약 작업이 두 번 돌아도 한 번만 보낸다',
     pre='DEADLINE_ALERT_DAYS 가 7,3,1, 마감이 7일 남은 진행 중 입찰 요청, 오늘 이미 마감 알림을 보낸 상태',
     steps='1. 같은 날 마감 알림 예약 작업을 한 번 더 돌린다\n2. notification_logs 와 받는 사람 메일함을 본다',
     expect='1. 같은 입찰 요청, 마감일, 남은 날, 받는 사람의 DEADLINE_ALERT 줄이 1줄뿐이다\n'
            '2. 받는 사람마다 그 마감 알림 메일이 1통만 와 있다',
     judge=['서버', '합동'],
     req=['BID-NOTIFY-002'], fc=['BID-NOTIFY-002'], screen='', pri='Medium'),

dict(id='IT-14-042', flow='마감 알림', kind='예외', title='알림 날 설정이 비었거나 읽을 수 없으면 마감 알림을 보내지 않는다',
     pre='마감이 7일 남은 진행 중 입찰 요청',
     steps='1. system_settings DEADLINE_ALERT_DAYS 를 빈 값으로 두고 예약 작업을 돌린다\n'
           '2. 값을 "7,a,1" 로 바꾸고 예약 작업을 다시 돌린다\n3. 두 번 모두 notification_logs 를 본다',
     expect='1. 빈 값일 때 그 입찰 요청에 DEADLINE_ALERT 줄이 생기지 않는다\n'
            '2. "7,a,1" 일 때도 DEADLINE_ALERT 줄이 생기지 않는다',
     judge=['서버', '서버'],
     req=['BID-NOTIFY-002'], fc=['BID-NOTIFY-002'], screen='', pri='Medium'),

dict(id='IT-14-043', flow='상태 도구', kind='예외', title='transition_bid_state 로도 Bid/No-Bid 결정 없이는 문서 작성 중으로 전환되지 않는다',
     pre='서버 열쇠(MCP_API_KEY)를 아는 호출자, 입찰 분석이 분석 완료이고 관리자 결정이 없는 상태 ANALYZING 입찰 요청',
     steps='1. POST /mcp 에 tools/call 본문(name transition_bid_state, bidRequestId 그 건, targetState GENERATING)을 보낸다',
     expect='1. 응답에 오류가 담긴다\n'
            '2. 그 입찰 요청 state 가 ANALYZING 그대로이고 state_history 줄 수가 늘지 않는다',
     judge=['서버', '서버'],
     req=['BID-MCP-004'], fc=['BID-MCP-004', 'BID-DOC-007'], screen='', pri='High',
     probe={1: dict(steps=[('key', '14.b043_bid'), TO_GENERATING],
                    check="r[1]['body']['result'].get('isError')"),
            2: dict(steps=[('key', '14.b043_bid'), TO_GENERATING,
                           ('sql', f"SELECT state, JSON_LENGTH(state_history) AS hist FROM bid_requests WHERE id = {THIS_BID}")],
                    check="r[2][0]['state'] == 'ANALYZING' and int(r[2][0]['hist']) == int(r[0][0]['hist'])")}),

]
