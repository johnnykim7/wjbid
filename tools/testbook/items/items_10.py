# 영역 10 AI 문서 생성/편집 시험항목. 근거는 T1-1 기능요구사항 명세서 v1.19, T1-3 업무 규칙, T1-5 상태 정의, T3-1 데이터 모델, T3-2 API 설계, T3-3, T3-4 화면 설계다.

# 서버 판정이 상태를 건드릴 수 있는 호출(전환, 다시 읽기)은 시험 고객의 입찰 건에만 건다. 결함이 있어도 실제 고객 건이 움직이지 않는다
USER = 'user:qa-cr143@example.com'
ADMIN = 'admin:smoke-admin@example.com'
QA_BIDS = "JOIN members m ON m.id = br.member_id WHERE m.email = 'qa-cr143@example.com'"


def uuid_of(col):
    """바이너리 열쇠를 API 주소 꼴로 바꾼다. 조회 실행기가 INSERT, REPLACE 낱말을 막아 SUBSTR 로 잇는다"""
    return (f"LOWER(CONCAT_WS('-', SUBSTR(HEX({col}),1,8), SUBSTR(HEX({col}),9,4), SUBSTR(HEX({col}),13,4), "
            f"SUBSTR(HEX({col}),17,4), SUBSTR(HEX({col}),21,12)))")


def candidate_shas(bid):
    """그 입찰 건의 읽기 후보 내용. 공고별 서류와 만료되지 않은 서류함 서류다(DocumentReadingService.collect)"""
    return (f"(SELECT cd.content_sha256 FROM client_documents cd WHERE cd.bid_request_id = {bid} "
            f"UNION SELECT md.content_sha256 FROM member_documents md JOIN bid_requests b ON b.member_id = md.member_id "
            f"WHERE b.id = {bid} AND (md.expires_on IS NULL OR md.expires_on >= '{{today}}'))")


def reading_rows(bid):
    return (f"SELECT HEX(dr.id) AS hex, dr.status, dr.read_at FROM document_readings dr "
            f"WHERE dr.content_sha256 IN {candidate_shas(bid)} ORDER BY dr.id")


THIS_BID = "UNHEX('{val:0:hex}')"
READINGS = '/api/admin/bid-requests/{val:0:id}/document-readings'
TRANSITION = '/api/admin/bid-requests/{val:0:id}/transition'
LAST_STEP = (f"SELECT state, JSON_LENGTH(state_history) AS hist, "
             f"JSON_UNQUOTE(JSON_EXTRACT(state_history, '$[last].fromState')) AS from_state, "
             f"JSON_UNQUOTE(JSON_EXTRACT(state_history, '$[last].toState')) AS to_state FROM bid_requests WHERE id = {THIS_BID}")

# MCP 열쇠는 저장소에 둘 수 없어 실행기가 환경변수에서 싣는다(run_server 머리글 값)
MCP_KEY = {'X-API-Key': '$TESTBOOK_MCP_API_KEY'}


def mcp(tool, args):
    return ('api', 'guest', 'POST', '/api/mcp',
            {'jsonrpc': '2.0', 'id': 1, 'method': 'tools/call', 'params': {'name': tool, 'arguments': args}}, MCP_KEY)


def mcp_out(i):
    """MCP 도구 결과는 JSON 을 글자로 한 번 더 싸서 돌려준다"""
    return f"r[{i}]['body']['result']['content'][0]['text']"


def mcp_json(i):
    return f"loads({mcp_out(i)})"


def mcp_rejected(i, reading):
    return (f"r[{i}]['body']['result'].get('isError') and "
            f"('unknownReadingIds: [' + r[0][0]['{reading}'] + ']') in {mcp_out(i)}")


def save_structure(reading_cols):
    return mcp('save_proposal_structure', {'documentId': '{val:0:id}', 'chapters': [
        {'factorLabel': '1', 'factorTitle': 'Past Performance', 'sections': [
            {'subfactorLabel': '1.1', 'title': 'Relevant Contracts', 'scope': 'Recent relevant contracts',
             'requirementRefs': [], 'sourceReadingIds': [f'{{val:0:{c}}}' for c in reading_cols], 'minWords': 200}]}]})


# 근거 서류 E 의 읽기 결과를 Aimbase 가 보내는 꼴로 저장한다. 빠진 칸과 소문자 값을 서버가 저장 꼴로 맞추는지까지 본다
SAVE_E_FACTS = mcp('save_client_document_facts', {
    'bidRequestId': '{val:0:bid}',
    'targets': [{'readingId': '{val:0:e}', 'targetPath': 'docs/equipment.pdf'}],
    'results': [{'index': 0, 'facts': [{'category': 'equipment', 'key': '장비', 'value': '굴삭기 2대'},
                                       {'category': 'EQUIPMENT', 'key': '수량', 'value': '2', 'page': 3, 'readMode': 'vision'}]}]})
FACT_FIELDS = "{'category', 'key', 'value', 'page', 'anchor', 'confidence', 'readMode'}"
SECTION_SOURCES = ("SELECT ps.source_reading_ids FROM proposal_section ps JOIN proposal_chapter pc ON pc.id = ps.chapter_id "
                   f"WHERE pc.document_id = {THIS_BID}")
STRUCTURE = ("SELECT HEX(pc.id) AS chapter, pc.factor_label, HEX(ps.id) AS section, ps.subfactor_label, ps.title, ps.source_reading_ids "
             f"FROM proposal_chapter pc JOIN proposal_section ps ON ps.chapter_id = pc.id WHERE pc.document_id = {THIS_BID} "
             "ORDER BY pc.order_no, ps.order_no")

# CR-170 입찰 분석. 시딩의 분석 완료 회차도 이 본문을 서버에 저장해 얻은 값이다(seed/10-12 머리말)
ANALYSIS = '/api/admin/bid-requests/{val:0:id}/bid-analysis'
DECISION = ANALYSIS + '/decision'
RERUN = ANALYSIS + '/run'
COMPETITOR = 'Beta Facility Services'
NO_WEB = '웹 자료 미사용'


def ev(value, confidence='CONFIRMED', source='공고 본문'):
    return {'value': value, 'confidence': confidence, 'source': source}


def factor_scores(**scores):
    return {f: {'score': s, 'basis': f'{f} 근거'} for f, s in scores.items()}


REPORT_BODY = {
    'overview': {
        'projectName': ev('Camp Humphreys 배관 유지보수'),
        'agency': ev('411th CSB'),
        'budget': ev('USD 1,200,000', 'ESTIMATED', '같은 기관 과거 계약 금액'),
        'period': ev('12개월'),
        'contractType': ev('FFP'),
        'background': '노후 배관 누수가 잦아 연간 유지보수 계약으로 발주',
        'wbs': [{'task': '정기 점검', 'basis': 'PWS 3.1'}, {'task': '긴급 수리', 'basis': 'PWS 3.2'}],
        'submission': {'pageLimit': ev('20쪽'), 'format': ev('PDF'), 'method': ev('PIEE'),
                       'deadline': ev('2026-11-20'), 'presentation': ev('없음')},
        'schedule': [{'step': '질의 마감', 'date': '2026-11-05', 'source': '공고 본문'},
                     {'step': '제출 마감', 'date': '2026-11-20', 'source': '공고 본문'}]},
    'agency': {
        'overview': ev('주한미군 계약 지원 부대'),
        'trends': ev('', 'UNKNOWN', NO_WEB),
        'motive': ev('누수 민원 증가', 'ESTIMATED', '공고 배경'),
        'decisionInfo': {'office': ev('411th CSB Camp Humphreys'), 'evaluationMethod': ev('LPTA')}},
    'orderHistory': {
        'rows': [{'source': 'FEDERAL_CONTRACT', 'piid': 'W91QVN-24-C-0012', 'title': 'Plumbing maintenance',
                  'awardee': COMPETITOR, 'amountUsd': '980000', 'period': '2024-01-01 ~ 2024-12-31', 'contractType': 'FFP'}],
        'verdict': 'RECURRING',
        'incumbent': ev(COMPETITOR, 'CONFIRMED', '미 연방 계약 W91QVN-24-C-0012'),
        'incumbentAdvantage': ev('중간', 'ESTIMATED', '발주 이력 1건')},
    'requirements': {
        'themes': [{'theme': '긴급 수리 대응', 'basis': 'PWS 3.2'}, {'theme': '현장 인력', 'basis': 'PWS 4.1'}],
        'evaluation': [{'factor': '기술', 'importance': '높음', 'method': 'LPTA', 'reqIds': ['REQ-001']}],
        'questionCandidates': [{'question': '야간 출동 범위', 'reason': 'PWS 에 시간대가 없음', 'reqId': 'REQ-002'}]},
    'eligibility': [
        {'item': 'SAM 등록', 'requirement': '유효한 SAM 등록', 'status': 'MET',
         'evidence': ev('등록됨', 'CONFIRMED', '고객 회사 칸'), 'remedy': ''},
        {'item': '기계설비공사업 면허', 'requirement': '현지 면허', 'status': 'CONDITIONAL',
         'evidence': ev('보유한다고 답함', 'ESTIMATED', '참가 자격 답(고객이 스스로 답한 것)'), 'remedy': '면허증 사본 받기'}],
    'competitors': {
        'companies': [{'name': COMPETITOR, 'uei': 'SEEDUEI00001', 'likelihood': 'HIGH', 'isIncumbent': True,
                       'relevantAwards': [{'piid': 'W91QVN-24-C-0012', 'title': 'Plumbing maintenance', 'amountUsd': '980000'}],
                       'expectedStrategy': '현 계약 가격 유지', 'relationship': '현 수행사', 'basis': '발주 이력'}],
        'bcc': {'themes': ['긴급 수리 대응', '현장 인력', '가격'],
                'scores': {'OUR_COMPANY': {'긴급 수리 대응': 5, '현장 인력': 2, '가격': 3},
                           COMPETITOR: {'긴급 수리 대응': 3, '현장 인력': 4, '가격': 3}}}},
    'fairness': {'indicators': [{'no': n, 'value': 'NO', 'basis': '공고 본문'} for n in range(1, 11)], 'judgement': 'GOOD'},
    'hiddenIssues': [{'issue': '현 수행사 인력 승계', 'basis': '같은 기관 과거 공고'}],
    'insights': ['긴급 수리 대응을 앞세우고 현장 인력 약점은 협력사로 메운다']}

REPORT_QUESTIONS = [
    {'target': 'CUSTOMER', 'question': '현장 상주 가능 인원은 몇 명입니까', 'reason': '고객 서류에 인력 현황이 없음', 'field': 'competitors.bcc'},
    {'target': 'ADMIN', 'question': '발주처 최근 동향을 확인하세요', 'reason': NO_WEB, 'field': 'agency.trends'}]

# 상위 열쇠 둘이 서로 다른 이슈 묶음에 들어가 순위가 점수 합으로 갈린다. 일정 5x4 + 2x2 = 24, 품질 3x3 = 9
CAPTURE_BODY = {
    'business': {'basic': {'사업명': ev('Camp Humphreys 배관 유지보수')}, 'team': {'담당': ev('미정', 'UNKNOWN', '고객 서류 없음')},
                 'agencyInfo': {'기관': ev('411th CSB')}, 'project': {'기간': ev('12개월')}},
    'issues': {
        'stakeholderIssues': [
            {'id': 'S1', 'stakeholder': '계약관', 'issue': '일정 준수', 'influence': 5, 'importance': 4},
            {'id': 'S2', 'stakeholder': '현장 감독', 'issue': '야간 출입', 'influence': 2, 'importance': 2},
            {'id': 'S3', 'stakeholder': '사용 부대', 'issue': '수리 품질', 'influence': 3, 'importance': 3}],
        'groupedIssues': [{'issue': '품질', 'memberIssues': ['S3']}, {'issue': '일정', 'memberIssues': ['S1', 'S2']}]},
    'competitors': [{'name': COMPETITOR,
                     'competitiveness': {'overall': {'level': 'EQUAL', 'basis': '현 수행사'}, 'sales': {'level': 'ADVANTAGE', 'basis': '관계'},
                                         'technical': {'level': 'EQUAL', 'basis': '실적'}, 'price': {'level': 'EQUAL', 'basis': '가격 유지'}},
                     'solutions': [{'issue': '일정', 'solution': '현 인력 유지', 'differentiation': '없음'}]}],
    'company': {'competitiveness': {'overall': {'level': 'EQUAL', 'basis': '실적 대등'}, 'sales': {'level': 'DISADVANTAGE', 'basis': '첫 참여'},
                                    'technical': {'level': 'ADVANTAGE', 'basis': '긴급 수리 실적'}, 'price': {'level': 'EQUAL', 'basis': '시장가'}},
                'solutions': [{'issue': '일정', 'solution': '24시간 출동조', 'differentiation': '응답 2시간 보장'}]},
    'other': {'risks': ev('인력 승계 협의', 'ESTIMATED', '같은 기관 과거 공고'), 'politics': ev('', 'UNKNOWN', NO_WEB),
              'timing': ev('회계연도 초', 'CONFIRMED', '공고 게시일')}}

# 자사 72.0(Strong Go), 경쟁사 66.0. 초기 가중치 CUSTOMER_RELATION 15, SOLUTION_FIT 20, COMPETITION 15, PRICE 15,
# PAST_PERFORMANCE 15, PERSONNEL 10, ENVIRONMENT 10 으로 계산한 값이다
DECISION_BODY = {
    'scorecard': [
        {'company': 'OUR_COMPANY', 'scores': factor_scores(CUSTOMER_RELATION=3, SOLUTION_FIT=4, COMPETITION=3, PRICE=4,
                                                           PAST_PERFORMANCE=4, PERSONNEL=3, ENVIRONMENT=4)},
        {'company': COMPETITOR, 'scores': factor_scores(CUSTOMER_RELATION=4, SOLUTION_FIT=3, COMPETITION=3, PRICE=3,
                                                         PAST_PERFORMANCE=4, PERSONNEL=3, ENVIRONMENT=3)}],
    'sensitivity': {'factor': 'PRICE', 'reason': 'LPTA 라 가격 한 단계가 판정을 바꾼다'},
    'qualification': [{'item': '기계설비공사업 면허', 'resolvable': True, 'remedy': '면허증 사본 받기'}],
    'profitability': {'priceBasis': {'기준': ev('과거 계약 USD 980,000')},
                      'scenarios': [{'label': '공격', 'priceUsd': '950000', 'margin': '8%', 'basis': '현 계약보다 3% 낮게'},
                                    {'label': '기준', 'priceUsd': '1000000', 'margin': '12%', 'basis': '시장가'}],
                      'costNote': '인건비가 원가의 70%'},
    'risks': [{'area': 'FINANCIAL', 'risk': '가격 경쟁', 'probability': 'H', 'impact': 'H', 'note': 'LPTA'},
              {'area': 'TECHNICAL', 'risk': '야간 출입 승인', 'probability': 'M', 'impact': 'L', 'note': '보안 절차'}],
    'conditions': ['참여 인력 이력서를 고객에게 받는다'],
    'advice': '가격을 기준안으로 두고 긴급 수리 대응을 앞세워 참여한다'}

# eligibility 첫 줄 근거의 확신도만 뺀 본문. 서버가 그 경로를 짚어 거부해야 한다
BAD_REPORT_BODY = {**REPORT_BODY, 'eligibility': [
    {**REPORT_BODY['eligibility'][0], 'evidence': {'value': '등록됨', 'source': '고객 회사 칸'}}] + REPORT_BODY['eligibility'][1:]}


def save_analysis(part, body, analysis_col='analysis', questions=None):
    args = {'bidRequestId': '{val:0:id}', 'analysisId': f'{{val:0:{analysis_col}}}', 'part': part, 'body': body}
    if questions is not None:
        args['questions'] = questions
    return mcp('save_bid_analysis', args)


def analysis_rows(bid=THIS_BID):
    return (f"SELECT HEX(id) AS hex, status, admin_decision, admin_note, HEX(decided_by) AS decided_by, decided_at, "
            f"HEX(started_by) AS started_by, started_at, finished_at, failure_reason, workflow_run_id, "
            f"report_json IS NOT NULL AS has_report, capture_plan_json IS NOT NULL AS has_plan, decision_json IS NOT NULL AS has_decision, "
            f"MD5(report_json) AS report_md5, pwin_percent FROM bid_analyses WHERE bid_request_id = {bid} ORDER BY started_at, created_at")


ADMIN_ID = "SELECT HEX(id) AS hex FROM members WHERE email = 'smoke-admin@example.com'"


def bid_api(col, tail=''):
    """키 줄의 다른 열에 든 입찰 건(같은 장면의 두 번째 건) 관리자 주소"""
    return f'/api/admin/bid-requests/{{val:0:{col}}}' + tail


def bid_of(col):
    return f"UNHEX('{{val:0:{col}}}')"


def evidences(expr):
    """근거 값(value 와 source 가 함께 있거나 confidence 가 달린 칸)을 어느 깊이에서든 모은다.
    판정식에는 isinstance 가 없어 꼴을 __class__ 로 가른다"""
    d, l = '{}.__class__', '[].__class__'
    return (f"(lambda w: w(w, {expr}))(lambda w, n: "
            f"([n] if n.__class__ == {d} and ('confidence' in n or ('value' in n and 'source' in n)) else []) + "
            f"sum([w(w, v) for v in (n.values() if n.__class__ == {d} else n if n.__class__ == {l} else [])], []))")


def ran(i):
    """새 회차를 워크플로우가 받았는지. 받지 못한 서버(Aimbase 가 닿지 않는 로컬)는 분석 중 판정을 할 재료가 없다"""
    return f"(r[{i}][-1]['status'] == 'RUNNING' or r[{i}][-1]['workflow_run_id'])"


def wait(sec):
    """뒤에서 도는 일(읽기 회차 끝의 분석 시작, 알림 발송)이 남길 흔적을 기다린다. 끝 식이 거짓이라 한도만큼 쉰다"""
    return ('poll', ADMIN, ANALYSIS, 'False', sec, 5)


def last_entered_gap(bid=THIS_BID):
    """지금 분석 시작 시각과 마지막 상태 이력 시각의 차(마이크로초). 이력 시각은 [년, 월, 일, 시, 분, 초, 나노초] 배열이다"""
    ts = lambda k: f"JSON_EXTRACT(br.state_history, '$[last].timestamp[{k}]')"
    at = (f"STR_TO_DATE(CONCAT({ts(0)}, '-', {ts(1)}, '-', {ts(2)}, ' ', {ts(3)}, ':', {ts(4)}, ':', {ts(5)}), "
          f"'%Y-%c-%e %k:%i:%s')")
    return (f"SELECT ba.status, ba.workflow_run_id, TIMESTAMPDIFF(MICROSECOND, {at}, ba.started_at) AS gap_us "
            f"FROM bid_analyses ba JOIN bid_requests br ON br.id = ba.bid_request_id WHERE br.id = {bid} ORDER BY ba.started_at")


def notification_count(email_col):
    return (f"SELECT COUNT(*) AS n FROM notification_logs WHERE reference_id = {THIS_BID} "
            f"OR recipient_email = '{{val:0:{email_col}}}'")


LATEST = ("SELECT status, workflow_run_id, report_json, capture_plan_json, decision_json FROM bid_analyses "
          f"WHERE bid_request_id = {THIS_BID} ORDER BY started_at DESC LIMIT 1")
OUTPUTS = "('report_json', 'capture_plan_json', 'decision_json')"
NOT_RUNNING = "x['status'] != 200 or x['body']['analysis'] is None or x['body']['analysis']['status'] != 'RUNNING'"
PWIN_WEIGHTS = {'CUSTOMER_RELATION': 15, 'SOLUTION_FIT': 20, 'COMPETITION': 15, 'PRICE': 15,
                'PAST_PERFORMANCE': 15, 'PERSONNEL': 10, 'ENVIRONMENT': 10}
CLOSE_STEP = (f"SELECT state, closed_at, JSON_LENGTH(state_history) AS hist, "
              f"JSON_UNQUOTE(JSON_EXTRACT(state_history, '$[last].fromState')) AS from_state, "
              f"JSON_UNQUOTE(JSON_EXTRACT(state_history, '$[last].toState')) AS to_state FROM bid_requests WHERE id = {THIS_BID}")


def saved_all():
    return [save_analysis('REPORT', REPORT_BODY, questions=REPORT_QUESTIONS), save_analysis('CAPTURE_PLAN', CAPTURE_BODY),
            save_analysis('DECISION', DECISION_BODY), ('api', ADMIN, 'GET', ANALYSIS)]


A4 = "r[4]['body']['analysis']"
SAVED = f"r[4]['status'] == 200 and {A4} is not None and {A4}['status'] == 'DONE' and {A4}['analysisId'] == r[0][0]['analysis']"


def mcp_error(i):
    return f"r[{i}]['body']['result'].get('isError')"


def refused(i, code, reason=None):
    s = f"r[{i}]['status'] == 409 and r[{i}]['body'].get('error') == '{code}'"
    return s + (f" and r[{i}]['body'].get('reason') == '{reason}'" if reason else '')

ITEMS = [

dict(id='IT-10-001', flow='제안서 자동 작성', kind='정상', title='공고 유형에 맞는 성공 제안서가 있는 입찰 건의 제안서를 AI 가 작성한다',
     pre='관리자 계정, 사전 분석 COMPLETED 이고 industry_type 이 있으며 그 유형에 성공 제안서 1건 이상이 있는 공고의 입찰 건(ANALYZING, 고객 서류 접수 완료, 고객 서류 읽기가 끝나 읽음 서류가 있음, 입찰 분석의 관리자 결정이 Go), Aimbase 문서 생성 워크플로우가 도는 상태',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 연다\n2. [상태 전환] 에서 GENERATING 을 골라 전환한다\n3. 생성이 끝날 때까지 기다린 뒤 화면을 새로고침한다\n4. 생성 문서의 [편집] 으로 본문을 연다',
     expect='1. 입찰 건 상태가 GENERATING 을 거쳐 REVIEW 가 되고 상태 이력에 GENERATING, REVIEW 두 줄이 더해진다\n'
            '2. 그 입찰 건에 입찰 문서(bid_documents) 행이 상태 DRAFT 로 생기고 현재 버전의 content_json 이 본문이 든 TipTap JSON 이다. 버전 1 은 설계 단계가 만든 빈 문서다\n'
            '3. 입찰 상세 관리 생성 문서 목록에 그 문서가 문서 유형과 상태 초안으로 보인다\n'
            '4. 제안서의 회사명, 실적, 인력, 장비가 고객이 낸 서류의 내용이고 성공 제안서 원본의 회사명이나 실적이 섞이지 않는다\n'
            '5. 제안서 구성과 서술 방식이 그 공고 유형의 성공 제안서 가이드를 따른다\n'
            '6. 제안서가 공고 사전 분석의 문서 양식과 필요 서류를 반영한다',
     judge=['서버', '서버', '가상', '합동', '합동', '합동'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={1: dict(steps=[('key', '10.b001_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-001 서버 판정'}),
                           ('poll', ADMIN, '/api/admin/bid-requests/{val:0:id}', "x['body']['state'] != 'GENERATING'", 3600, 30),
                           ('sql', f"SELECT state, JSON_LENGTH(state_history) AS hist, "
                                   f"JSON_UNQUOTE(JSON_EXTRACT(state_history, '$[last-1].toState')) AS prev_state, "
                                   f"JSON_UNQUOTE(JSON_EXTRACT(state_history, '$[last].toState')) AS to_state FROM bid_requests WHERE id = {THIS_BID}")],
                    need="r[0][0]['state'] == 'ANALYZING'",
                    check="r[1]['status'] == 200 and r[3][0]['state'] == 'REVIEW' and int(r[3][0]['hist']) == int(r[0][0]['hist']) + 2 "
                          "and r[3][0]['prev_state'] == 'GENERATING' and r[3][0]['to_state'] == 'REVIEW'"),
            2: dict(steps=[('key', '10.b001_bid'),
                           ('sql', f"SELECT bd.status, JSON_UNQUOTE(JSON_EXTRACT(v.content_json, '$.type')) AS t, "
                                   f"JSON_LENGTH(v.content_json, '$.content') AS n FROM bid_documents bd "
                                   f"LEFT JOIN bid_document_versions v ON v.document_id = bd.id AND v.version_no = bd.current_version_no "
                                   f"WHERE bd.bid_request_id = {THIS_BID}")],
                    need="r[0][0]['state'] != 'ANALYZING'",
                    check="len(r[1]) > 0 and all(x['status'] == 'DRAFT' and x['t'] == 'doc' and int(x['n'] or 0) > 0 for x in r[1])")},
     issue='T1-1 은 "고객이 필요 문서를 제출하면" 자동 작성이라 하고 T1-5 는 CR-141 뒤 ANALYZING 다음 상태로 관리자가 전환한다고 해 생성이 시작되는 때가 어긋난다. Aimbase 가 만드는 최종 Word, PDF 를 어디서 받는지 T3-2, T3-3 에 없다'),

dict(id='IT-10-002', flow='제안서 자동 작성', kind='정상', title='미분류 공고의 입찰 건은 성공 제안서 자산 없이 제안서를 작성한다',
     pre='관리자 계정, 사전 분석 COMPLETED 이고 industry_type 이 NULL 인 공고의 입찰 건(ANALYZING, 입찰 분석의 관리자 결정이 Go), Aimbase 문서 생성 워크플로우가 도는 상태',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 연다\n2. [상태 전환] 에서 GENERATING 을 골라 전환한다\n3. 생성이 끝날 때까지 기다린다',
     expect='1. 입찰 건 상태가 REVIEW 가 되고 입찰 문서 행이 상태 DRAFT 로 생긴다\n'
            '2. 문서 생성 워크플로우 입력의 successGuide 와 referenceSamples 가 비어 있다\n'
            '3. 제안서에 성공 제안서 원본의 내용이 섞이지 않는다',
     judge=['서버', '단위', '합동'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-10-003', flow='제안서 자동 작성', kind='예외', title='사전 분석이 COMPLETED 가 아닌 공고의 입찰 건은 제안서를 작성하지 않는다',
     pre='관리자 계정, 사전 분석이 FAILED 인 공고의 입찰 건(ANALYZING, 입찰 분석의 관리자 결정이 Go)',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 연다\n2. [상태 전환] 에서 GENERATING 을 골라 전환한다\n3. 몇 분 기다린 뒤 그 입찰 건의 입찰 문서를 조회한다',
     expect='1. 그 입찰 건에 입찰 문서 행이 생기지 않는다\n'
            '2. 입찰 건 상태가 REVIEW 로 넘어가지 않는다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /bid-requests/{id}', pri='High',
     issue='사전 분석이 COMPLETED 가 아닐 때 GENERATING 전환을 막는지, 전환은 되고 생성만 하지 않는지 설계서가 적지 않는다'),

dict(id='IT-10-004', flow='제안서 자동 작성', kind='예외', title='제안서 생성이 실패하면 관리자에게 알린다',
     pre='관리자 계정, 문서 생성 워크플로우가 실패하도록 막은 환경, GENERATING 으로 들어가는 입찰 건 1건',
     steps='1. 그 입찰 건의 제안서 생성을 시작한다',
     expect='1. 알림 이력에 notification_type AI_WORKFLOW_FAILED 인 관리자 알림 행이 1건 더해진다',
     judge=['단위'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='', pri='High'),

dict(id='IT-10-005', flow='제안서 설계와 REQ', kind='정상', title='제안서 설계 저장이 절마다 매트릭스의 REQ ID 를 받고 어느 절에도 없는 REQ ID 를 돌려준다',
     pre='관리자 계정과 MCP 열쇠, Shall 이면서 플랫폼 생성인 매트릭스 줄 REQ-001, REQ-002, REQ-003 이 있는 공고의 기술 제안서 문서 1건',
     steps='1. MCP save_proposal_structure 를 그 문서 id 와 장 1개, 절 2개(절 A 는 REQ-001, 절 B 는 REQ-002)로 부른다\n2. 관리자 토큰으로 GET /admin/bid-documents/{documentId}/chapters 를 부른다\n3. 관리자 화면 /documents/{documentId}/sections 를 열어 절 A 를 고른다',
     expect='1. 저장이 받아들여지고 응답의 unassignedReqIds 에 REQ-003 만 있다\n'
            '2. 장과 절 트리 응답에서 절 A 의 requirementRefs 가 REQ-001, 절 B 의 requirementRefs 가 REQ-002 이다\n'
            '3. 우측 절 상세의 대응 REQ ID 에 REQ-001 이 보인다',
     judge=['서버', '서버', '가상'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /documents/{id}/sections', pri='High'),

dict(id='IT-10-006', flow='제안서 설계와 REQ', kind='예외', title='매트릭스에 없는 REQ ID 를 든 제안서 설계는 전체가 저장되지 않는다',
     pre='관리자 계정과 MCP 열쇠, 매트릭스 줄 REQ-001 부터 REQ-003 이 있는 공고의 기술 제안서 문서 1건(IT-10-005 로 저장한 트리가 있음)',
     steps='1. 장과 절 트리를 조회해 적어 둔다\n2. MCP save_proposal_structure 를 절 2개(절 A 는 REQ-001, 절 B 는 REQ-999)로 부른다\n3. 장과 절 트리를 다시 조회한다',
     expect='1. 저장이 거부되고 응답에 매트릭스에 없는 ID 로 REQ-999 가 들어 있다\n'
            '2. 3단계의 장과 절 트리가 1단계와 같다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='', pri='High'),

dict(id='IT-10-007', flow='제안서 설계와 REQ', kind='정상', title='매트릭스가 없는 공고의 제안서 설계는 REQ ID 검사 없이 저장된다',
     pre='관리자 계정과 MCP 열쇠, 매트릭스 줄이 없는 공고의 기술 제안서 문서 1건',
     steps='1. MCP save_proposal_structure 를 절 1개(requirementRefs 에 REQ-001)로 부른다\n2. 장과 절 트리를 조회한다',
     expect='1. 저장이 받아들여진다\n'
            '2. 장과 절 트리에 1단계에서 보낸 절이 들어 있다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='', pri='High'),

dict(id='IT-10-008', flow='제안서 설계와 REQ', kind='정상', title='제안서 설계, 절 작성, 절 검증 입력에 대응 REQ 의 원문, 번역, 요구 동사, 평가항목이 들어 있다',
     pre='관리자 계정과 MCP 열쇠, IT-10-005 로 절 A(REQ-001)가 저장된 기술 제안서 문서',
     steps='1. MCP get_proposal_design_input 을 그 문서 id 로 부른다\n2. MCP get_section_context 를 절 A id 로 부른다\n3. MCP get_section_verify_input 을 절 A id 로 부른다',
     expect='1. 제안서 설계 입력의 complianceMatrix 에 매트릭스 줄마다 reqId, level, text, verb, fulfillmentParty, factorId, factorTitle 이 들어 있다\n'
            '2. 절 작성 입력의 requirements 에 REQ-001 한 줄이 reqId, text, translation, verb, level, factorId, factorTitle 과 함께 들어 있다\n'
            '3. 절 검증 입력의 requirements 가 2단계의 requirements 와 같다',
     judge=['서버', '서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='', pri='High'),

dict(id='IT-10-009', flow='제안서 절 관리', kind='정상', title='제안서 Section 관리에서 장과 절 트리와 절 상세를 본다',
     pre='관리자 계정, 장과 절 트리와 절 본문이 저장된 제안서 문서 1건',
     steps='1. 관리자 화면 문서 편집에서 [Section 관리] 를 눌러 /documents/{documentId}/sections 를 연다\n2. 좌측 트리에서 절 하나를 고른다',
     expect='1. 좌측 트리에 장(평가 항목) 이름 아래 그 장의 절 제목이 저장된 순서대로 보인다\n'
            '2. 트리의 절마다 그 절의 상태와 잠금 여부가 보인다\n'
            '3. 우측 절 상세에 고른 절의 제목, 범위, 대응 REQ ID, 본문 블록, 검증 결과가 보인다',
     judge=['가상', '가상', '가상'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /documents/{id}/sections', pri='High'),

dict(id='IT-10-010', flow='제안서 절 관리', kind='정상', title='절 하나만 다시 작성한다',
     pre='관리자 계정, 잠기지 않은 절이 둘 이상인 제안서 문서 1건, Aimbase 절 작성 워크플로우가 도는 상태',
     steps='1. 관리자 화면 /documents/{documentId}/sections 에서 절 A 를 고르고 본문 블록을 적어 둔다\n2. 절 B 의 본문 블록도 적어 둔다\n3. 절 A 에서 [다시 작성] 을 누르고 끝날 때까지 기다린다',
     expect='1. 절 A 의 본문 블록이 1단계와 다른 새 본문으로 바뀐다\n'
            '2. 절 B 의 본문 블록은 2단계와 같다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /documents/{id}/sections', pri='High'),

dict(id='IT-10-011', flow='제안서 절 관리', kind='정상', title='절 본문은 두고 검증만 다시 한다',
     pre='관리자 계정, 본문과 검증 결과가 있는 절이 있는 제안서 문서 1건, Aimbase 절 검증 워크플로우가 도는 상태',
     steps='1. 관리자 화면 /documents/{documentId}/sections 에서 그 절을 고르고 본문 블록과 검증 결과를 적어 둔다\n2. [다시 검증] 을 누르고 끝날 때까지 기다린 뒤 절 검증 결과(GET .../sections/{sectionId}/verification)를 부른다',
     expect='1. 그 절의 본문 블록이 1단계와 같다\n'
            '2. 절 검증 결과 응답이 1단계 뒤에 만든 새 검증 결과 1건이다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /documents/{id}/sections', pri='High'),

dict(id='IT-10-012', flow='제안서 절 관리', kind='정상', title='절을 잠그고 잠금을 푼다',
     pre='관리자 계정, 잠기지 않은 절이 있는 제안서 문서 1건',
     steps='1. 관리자 화면 /documents/{documentId}/sections 에서 그 절을 고른다\n2. [잠금] 을 누른다\n3. [잠금 해제] 를 누른다',
     expect='1. 2단계 뒤 좌측 트리의 그 절에 잠금 표시가 붙는다\n'
            '2. 3단계 뒤 좌측 트리의 그 절에서 잠금 표시가 사라진다\n'
            '3. 3단계 뒤 그 절에 다시 작성 요청이 받아들여진다',
     judge=['가상', '가상', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /documents/{id}/sections', pri='High'),

dict(id='IT-10-013', flow='제안서 절 관리', kind='예외', title='잠긴 절은 다시 작성되지 않는다',
     pre='관리자 계정, 잠긴 절이 있는 제안서 문서 1건',
     steps='1. 그 절의 본문 블록을 적어 둔다\n2. 관리자 토큰으로 POST /admin/bid-documents/{documentId}/sections/{sectionId}/regenerate 를 부른다',
     expect='1. 다시 작성 요청이 거부된다(2xx 가 아니다)\n'
            '2. 그 절의 본문 블록이 1단계와 같다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='', pri='High'),

dict(id='IT-10-014', flow='생성 완료 알림', kind='정상', title='제안서 생성이 끝나면 관리자에게 완료 메일이 간다',
     pre='관리자 계정과 관리자 메일함, IT-10-001 과 같은 조건의 입찰 건(ANALYZING, 입찰 분석의 관리자 결정이 Go), Aimbase 문서 생성 워크플로우가 도는 상태',
     steps='1. 관리자 화면에서 그 입찰 건을 GENERATING 으로 전환하고 생성이 끝날 때까지 기다린다\n2. 관리자 메일함을 연다\n3. 메일의 바로가기 링크를 누른다',
     expect='1. 관리자 메일함에 문서 생성 완료 메일이 묶인 문서마다 1통씩 온다(문서가 둘이면 2통)\n'
            '2. 메일에 생성된 문서 유형과 그 입찰 건의 공고 제목이 있다\n'
            '3. 바로가기 링크를 누르면 관리자 화면에서 그 입찰 건이나 그 문서가 열린다\n'
            '4. 알림 이력에 notification_type DOCUMENT_GENERATED, success true 인 행이 그 입찰 건을 참조로 관리자 메일 주소마다 묶인 문서 수만큼 더해진다',
     judge=['합동', '합동', '합동', '서버'],
     req=['BID-DOC-002'], fc=['BID-DOC-002'], screen='', pri='High',
     probe={4: dict(steps=[('key', '10.b001_bid'),
                           ('sql', f"SELECT (SELECT COUNT(*) FROM members WHERE role = 'ADMIN') AS admins, "
                                   f"(SELECT COUNT(*) FROM bid_documents WHERE bid_request_id = {THIS_BID}) AS docs, "
                                   f"COUNT(*) AS n, COUNT(DISTINCT n.recipient_email) AS emails FROM notification_logs n "
                                   f"WHERE n.reference_id = {THIS_BID} AND n.notification_type = 'DOCUMENT_GENERATED' AND n.success = 1 "
                                   f"AND n.created_at >= (SELECT MIN(v.created_at) FROM bid_documents bd JOIN bid_document_versions v "
                                   f"ON v.document_id = bd.id AND v.version_no = bd.current_version_no WHERE bd.bid_request_id = {THIS_BID})")],
                    need="r[0][0]['state'] != 'ANALYZING'",
                    check="int(r[1][0]['admins']) > 0 and int(r[1][0]['docs']) > 0 "
                          "and int(r[1][0]['n']) == int(r[1][0]['admins']) * int(r[1][0]['docs']) "
                          "and int(r[1][0]['emails']) == int(r[1][0]['admins'])")}),

dict(id='IT-10-015', flow='문서 편집', kind='정상', title='관리자가 초안 문서를 고쳐 저장하면 새 버전이 생긴다',
     pre='관리자 계정, REVIEW 상태 입찰 건의 DRAFT 문서 1건(버전 1)',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 열고 생성 문서의 [편집] 을 누른다\n2. 에디터에서 문단 하나를 고친다\n3. [저장] 을 누른다\n4. 변경 요약에 "문단 수정" 을 넣고 확인한다',
     expect='1. 고치기 전에는 [저장] 이 꺼져 있고 2단계에서 고치면 켜진다\n'
            '2. [저장] 을 누르면 변경 요약을 넣는 입력 칸이 있는 확인 창이 뜬다\n'
            '3. 확인 뒤 "v2 저장되었습니다" 알림이 뜬다\n'
            '4. 버전 사이드바에 v2 가 "문단 수정" 변경 요약과 함께 더해진다\n'
            '5. bid_document_versions 에 version_no 2 행이 생기고 content_json 이 고친 본문, edited_by 가 그 관리자이며 문서의 current_version_no 가 2 이다\n'
            '6. 버전 1 행의 content_json 은 고치기 전 본문 그대로이다',
     judge=['가상', '가상', '가상', '가상', '서버', '서버'],
     req=['BID-EDIT-001', 'BID-DOC-003'], fc=['BID-EDIT-001', 'BID-DOC-003'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High',
     issue='화면 소스의 문서 편집 경로는 /documents/{id}/edit 이고, 변경 요약은 창이 아닌 입력 칸, 저장 알림은 "저장되었습니다." 로 설계와 다르다(소스 기준)'),

dict(id='IT-10-016', flow='문서 편집', kind='정상', title='잠긴 문서는 읽기 전용으로 열리고 잠금 해제와 PDF 만 켜져 있다',
     pre='관리자 계정, CONFIRMED 상태 입찰 건의 LOCKED 문서 1건',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 열고 생성 문서의 문서 편집으로 들어간다\n2. 에디터 본문을 고치려 해 본다',
     expect='1. 에디터가 읽기 전용이라 본문 글자가 바뀌지 않는다\n'
            '2. [저장] 이 꺼져 있다\n'
            '3. [확정] 이 없다\n'
            '4. [잠금 해제] 와 [PDF] 가 켜져 있다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-EDIT-001', 'BID-EDIT-002'], fc=['BID-EDIT-001', 'BID-EDIT-002'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High',
     issue='T3-4 문서 편집 진입 조건은 BidRequest REVIEW, BidDocument DRAFT 인데 같은 절의 상태별 UI 분기는 LOCKED, ARCHIVED 화면을 적어 서로 어긋난다'),

dict(id='IT-10-017', flow='문서 편집', kind='정상', title='보관된 문서는 읽기 전용이고 PDF 만 켜져 있다',
     pre='관리자 계정, SUBMITTED 상태 입찰 건의 ARCHIVED 문서 1건',
     steps='1. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세를 열고 생성 문서의 문서 편집으로 들어간다',
     expect='1. 에디터가 읽기 전용이라 본문 글자가 바뀌지 않는다\n'
            '2. [저장] 이 꺼져 있다\n'
            '3. [확정] 과 [잠금 해제] 가 없다\n'
            '4. [PDF] 가 켜져 있다',
     judge=['가상', '가상', '가상', '가상'],
     req=['BID-EDIT-001'], fc=['BID-EDIT-001'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High'),

dict(id='IT-10-018', flow='문서 편집', kind='예외', title='잠긴 문서에 새 버전 저장 요청은 거부된다',
     pre='관리자 계정, LOCKED 문서 1건',
     steps='1. 그 문서의 버전 수를 적어 둔다\n2. 관리자 토큰으로 POST /bid-documents/{id}/versions 에 고친 본문을 실어 부른다',
     expect='1. 저장 요청이 거부된다(2xx 가 아니다)\n'
            '2. 그 문서의 버전 수와 current_version_no 가 1단계와 같다',
     judge=['서버', '서버'],
     req=['BID-EDIT-001'], fc=['BID-EDIT-001'], screen='', pri='High'),

dict(id='IT-10-019', flow='문서 편집', kind='예외', title='입찰 건이 REVIEW 가 아니면 문서 편집에 들어가지 못하고 입찰 상세로 돌아간다',
     pre='관리자 계정, GENERATING 상태 입찰 건의 DRAFT 문서 1건',
     steps='1. 관리자 화면 주소창에 그 문서의 문서 편집 주소를 넣어 연다',
     expect='1. 문서를 편집할 수 없다는 오류 안내가 뜬다\n'
            '2. 그 입찰 건의 입찰 상세 관리(/bid-requests/{id}) 화면이 열린다',
     judge=['가상', '가상'],
     req=['BID-EDIT-001'], fc=['BID-EDIT-001'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High'),

dict(id='IT-10-020', flow='문서 편집', kind='예외', title='문서를 불러오지 못하면 입찰 상세로 돌아간다',
     pre='관리자 계정, REVIEW 입찰 건의 DRAFT 문서 1건, 문서 조회 요청이 실패하도록 막은 브라우저',
     steps='1. 관리자 화면 입찰 상세 관리에서 생성 문서의 [편집] 을 누른다',
     expect='1. 문서를 불러오지 못했다는 오류 안내가 뜬다\n'
            '2. 그 입찰 건의 입찰 상세 관리(/bid-requests/{id}) 화면이 열린다',
     judge=['가상', '가상'],
     req=['BID-EDIT-001'], fc=['BID-EDIT-001'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High'),

dict(id='IT-10-021', flow='문서 편집', kind='예외', title='버전 목록을 불러오지 못하면 버전 사이드바가 빈 상태다',
     pre='관리자 계정, REVIEW 입찰 건의 DRAFT 문서 1건, 버전 목록 조회 요청만 실패하도록 막은 브라우저',
     steps='1. 관리자 화면 입찰 상세 관리에서 생성 문서의 [편집] 을 누른다',
     expect='1. 에디터에 그 문서의 현재 본문이 열린다\n'
            '2. 버전 사이드바에 버전 줄이 하나도 없다',
     judge=['가상', '가상'],
     req=['BID-EDIT-001', 'BID-DOC-003'], fc=['BID-EDIT-001', 'BID-DOC-003'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High'),

dict(id='IT-10-022', flow='문서 편집', kind='예외', title='관리자가 아니면 문서를 열거나 저장할 수 없다',
     pre='회원 계정 로그인 토큰, DRAFT 문서 1건',
     steps='1. 회원 토큰으로 GET /bid-documents/{id} 를 부른다\n2. 회원 토큰으로 POST /bid-documents/{id}/versions 에 본문을 실어 부른다',
     expect='1. 문서 조회가 403 으로 거부되고 본문이 응답에 없다\n'
            '2. 저장 요청이 403 으로 거부되고 그 문서의 버전 수가 늘지 않는다',
     judge=['서버', '서버'],
     req=['BID-EDIT-001'], fc=['BID-EDIT-001'], screen='', pri='High'),

dict(id='IT-10-023', flow='문서 잠금', kind='정상', title='관리자가 초안 문서를 확정해 잠근다',
     pre='관리자 계정, REVIEW 상태 입찰 건의 DRAFT 문서 1건',
     steps='1. 관리자 화면 입찰 상세 관리에서 그 문서의 문서 편집으로 들어간다\n2. [확정] 을 누른다\n3. 확인 창에서 확인을 누른다\n4. 입찰 상세 관리로 돌아간다',
     expect='1. 확인 창에 "확정 후 편집이 불가합니다. 확정하시겠습니까?" 가 보인다\n'
            '2. 문서 상태가 LOCKED 로 바뀐다\n'
            '3. 에디터가 읽기 전용이 되어 본문 글자가 바뀌지 않는다\n'
            '4. [확정] 자리에 [잠금 해제] 가 보인다\n'
            '5. 입찰 상세 관리 생성 문서 목록의 그 문서 상태가 잠김으로 보인다',
     judge=['가상', '서버', '가상', '가상', '가상'],
     req=['BID-EDIT-002'], fc=['BID-EDIT-002'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High',
     issue='화면 소스의 단추 이름은 [확정] 이 아닌 [잠금] 이고 확인 창이 없다(소스 기준)'),

dict(id='IT-10-024', flow='문서 잠금', kind='정상', title='관리자가 잠긴 문서의 잠금을 풀면 문서는 초안, 입찰 건은 REVIEW 로 돌아간다',
     pre='관리자 계정, CONFIRMED 상태 입찰 건의 LOCKED 문서 1건',
     steps='1. 관리자 화면 입찰 상세 관리에서 그 문서의 문서 편집으로 들어간다\n2. [잠금 해제] 를 누른다\n3. 확인 창에서 확인을 누른다',
     expect='1. [잠금 해제] 를 누르면 잠금 해제를 묻는 확인 창이 뜬다\n'
            '2. 문서 상태가 DRAFT 로 바뀐다\n'
            '3. 입찰 건 상태가 REVIEW 로 바뀌고 상태 이력에 REVIEW 한 줄이 더해진다\n'
            '4. 에디터가 편집 상태로 돌아와 [저장] 과 [확정] 이 다시 보인다',
     judge=['가상', '서버', '서버', '가상'],
     req=['BID-EDIT-002'], fc=['BID-EDIT-002'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High'),

dict(id='IT-10-025', flow='문서 잠금', kind='예외', title='이미 잠긴 문서와 보관된 문서는 다시 잠기지 않는다',
     pre='관리자 계정, LOCKED 문서 1건, ARCHIVED 문서 1건',
     steps='1. 관리자 토큰으로 LOCKED 문서에 POST /bid-documents/{id}/lock 을 부른다\n2. 관리자 토큰으로 ARCHIVED 문서에 POST /bid-documents/{id}/lock 을 부른다',
     expect='1. LOCKED 문서의 잠금 요청이 거부되고 상태가 LOCKED 그대로이다\n'
            '2. ARCHIVED 문서의 잠금 요청이 거부되고 상태가 ARCHIVED 그대로이다',
     judge=['서버', '서버'],
     req=['BID-EDIT-002'], fc=['BID-EDIT-002'], screen='', pri='High'),

dict(id='IT-10-026', flow='문서 잠금', kind='예외', title='보관된 문서는 잠금을 풀 수 없다',
     pre='관리자 계정, SUBMITTED 입찰 건의 ARCHIVED 문서 1건',
     steps='1. 관리자 토큰으로 POST /bid-documents/{id}/unlock 을 부른다',
     expect='1. 잠금 해제 요청이 거부된다(2xx 가 아니다)\n'
            '2. 문서 상태가 ARCHIVED 그대로이고 입찰 건 상태가 SUBMITTED 그대로이다',
     judge=['서버', '서버'],
     req=['BID-EDIT-002'], fc=['BID-EDIT-002'], screen='', pri='High'),

dict(id='IT-10-027', flow='문서 잠금', kind='예외', title='관리자가 아니면 문서를 잠그거나 풀 수 없다',
     pre='회원 계정 로그인 토큰, DRAFT 문서 1건과 LOCKED 문서 1건',
     steps='1. 회원 토큰으로 DRAFT 문서에 POST /bid-documents/{id}/lock 을 부른다\n2. 회원 토큰으로 LOCKED 문서에 POST /bid-documents/{id}/unlock 을 부른다',
     expect='1. 잠금 요청이 403 으로 거부되고 문서 상태가 DRAFT 그대로이다\n'
            '2. 잠금 해제 요청이 403 으로 거부되고 문서 상태가 LOCKED 그대로이다',
     judge=['서버', '서버'],
     req=['BID-EDIT-002'], fc=['BID-EDIT-002'], screen='', pri='High'),

dict(id='IT-10-028', flow='PDF 내보내기', kind='정상', title='문서를 PDF 로 내려받는다',
     pre='관리자 계정, 제목과 목록이 들어 있는 DRAFT 문서 1건',
     steps='1. 관리자 화면 입찰 상세 관리에서 그 문서의 문서 편집으로 들어간다\n2. [PDF] 를 누른다\n3. 내려받은 파일을 연다',
     expect='1. 확장자가 pdf 인 파일 하나가 내려받아진다\n'
            '2. PDF 에 에디터의 현재 본문과 같은 글이 제목, 목록 서식대로 들어 있다',
     judge=['가상', '합동'],
     req=['BID-EDIT-003'], fc=['BID-EDIT-003'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='Medium'),

dict(id='IT-10-029', flow='문서 버전', kind='정상', title='버전 이력과 이전 버전 본문을 본다',
     pre='관리자 계정, 버전이 3개 있는 DRAFT 문서 1건',
     steps='1. 관리자 화면 입찰 상세 관리에서 그 문서의 문서 편집으로 들어간다\n2. 버전 사이드바에서 버전 1 을 누른다\n3. 관리자 토큰으로 GET /bid-documents/{id}/versions 를 부른다',
     expect='1. 버전 사이드바에 버전 3줄이 있고 줄마다 버전 번호, 편집자, 편집 시각, 변경 요약이 보인다\n'
            '2. 버전 1 을 누르면 그 버전의 content_json 본문이 보인다\n'
            '3. 버전 목록 응답이 bid_document_versions 의 그 문서 행 3개와 버전 번호, 편집자, 시각, 변경 요약이 같다',
     judge=['가상', '가상', '서버'],
     req=['BID-DOC-003'], fc=['BID-DOC-003'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='Medium',
     issue='T1-1 의 이전 버전 비교가 T3-2, T3-3, T3-4 에 없다'),

dict(id='IT-10-030', flow='문서 버전', kind='예외', title='만들어진 버전은 고치거나 지울 수 없다',
     pre='관리자 계정, 버전이 2개 있는 DRAFT 문서 1건',
     steps='1. 버전 1 행의 content_json 과 change_summary 를 적어 둔다\n2. 관리자 토큰으로 PUT, PATCH /bid-documents/{id}/versions/1 에 다른 본문을 실어 부른다\n3. 관리자 토큰으로 DELETE /bid-documents/{id}/versions/1 을 부른다',
     expect='1. 세 요청 모두 받아들여지지 않는다(2xx 가 아니다)\n'
            '2. 버전 1 행이 남아 있고 content_json 과 change_summary 가 1단계와 같다',
     judge=['서버', '서버'],
     req=['BID-DOC-003'], fc=['BID-DOC-003'], screen='', pri='Medium'),

dict(id='IT-10-031', flow='템플릿 등록', kind='정상', title='관리자가 새 템플릿을 등록한다',
     pre='관리자 계정, 쓰지 않은 템플릿 이름 하나와 TipTap JSON 본문',
     steps='1. 관리자 화면 메뉴 [문서 템플릿] 을 연다\n2. [새 템플릿 등록] 을 누른다\n3. 템플릿 이름, 문서 타입 기술 제안서, TipTap JSON 을 넣고 [저장] 을 누른다',
     expect='1. 양식 목록에 넣은 템플릿 이름이 문서 유형 기술 제안서, 버전 1, 활성 상태로 한 줄 더해진다\n'
            '2. document_templates 에 그 이름, document_type TECHNICAL_PROPOSAL, 넣은 content_json, active true 인 행이 생긴다',
     judge=['가상', '서버'],
     req=['BID-TPL-001'], fc=['BID-TPL-001'], screen='관리자 /templates', pri='High',
     issue='T1-1 입력의 태그/분류와 마크다운 본문이 T3-3 양식 등록 칸과 T3-1 문서 양식 표에 없다'),

dict(id='IT-10-032', flow='템플릿 등록', kind='정상', title='같은 문서 유형에 템플릿을 여러 개 등록한다',
     pre='관리자 계정, 기술 제안서 유형 템플릿이 1개 있는 서버',
     steps='1. 관리자 화면 메뉴 [문서 템플릿] 에서 [새 템플릿 등록] 을 누른다\n2. 다른 이름, 문서 타입 기술 제안서로 저장한다\n3. 관리자 토큰으로 GET /admin/document-templates/TECHNICAL_PROPOSAL 을 부른다',
     expect='1. 양식 목록에 기술 제안서 유형 템플릿 두 줄이 각각의 이름으로 보인다\n'
            '2. 문서 유형별 조회 응답에 두 템플릿이 모두 들어 있다',
     judge=['가상', '서버'],
     req=['BID-TPL-001'], fc=['BID-TPL-001'], screen='관리자 /templates', pri='High'),

dict(id='IT-10-033', flow='템플릿 등록', kind='예외', title='같은 이름의 템플릿은 등록되지 않는다',
     pre='관리자 계정, 이미 등록된 템플릿 1개',
     steps='1. 관리자 화면 메뉴 [문서 템플릿] 에서 [새 템플릿 등록] 을 누른다\n2. 이미 있는 템플릿과 같은 이름을 넣고 [저장] 을 누른다',
     expect='1. 양식 목록에 그 이름의 줄이 더해지지 않고 줄 수가 그대로이다\n'
            '2. document_templates 에 그 이름의 행이 하나뿐이다',
     judge=['가상', '서버'],
     req=['BID-TPL-001'], fc=['BID-TPL-001'], screen='관리자 /templates', pri='High'),

dict(id='IT-10-034', flow='템플릿 등록', kind='예외', title='관리자가 아니면 템플릿을 등록할 수 없다',
     pre='회원 계정 로그인 토큰',
     steps='1. 회원 토큰으로 POST /admin/document-templates 에 새 이름의 템플릿을 실어 부른다',
     expect='1. 요청이 403 으로 거부된다\n'
            '2. document_templates 에 그 이름의 행이 없다',
     judge=['서버', '서버'],
     req=['BID-TPL-001'], fc=['BID-TPL-001'], screen='', pri='High'),

dict(id='IT-10-035', flow='템플릿 조회', kind='정상', title='템플릿 목록과 문서 유형별 템플릿을 본다',
     pre='관리자 계정, 문서 유형이 다른 활성 템플릿 3개 이상',
     steps='1. 관리자 화면 메뉴 [문서 템플릿] 을 연다\n2. 관리자 토큰으로 GET /admin/document-templates/COVER_LETTER 를 부른다',
     expect='1. 양식 목록 줄마다 양식명, 문서 유형, 버전, 활성 상태가 보인다\n'
            '2. document_templates 의 활성 템플릿이 모두 양식 목록에 있다\n'
            '3. 문서 유형별 조회 응답에 문서 유형이 COVER_LETTER 인 템플릿만 있다',
     judge=['가상', '가상', '서버'],
     req=['BID-TPL-002'], fc=['BID-TPL-002'], screen='관리자 /templates', pri='High',
     issue='T1-1 입력의 태그 필터와 페이지 정보가 T3-2 템플릿 목록 조회와 T3-3 에 없다'),

dict(id='IT-10-036', flow='템플릿 조회', kind='정상', title='템플릿 하나의 내용을 연다',
     pre='관리자 계정, TipTap JSON 이 저장된 템플릿 1개',
     steps='1. 관리자 화면 메뉴 [문서 템플릿] 에서 그 템플릿 줄의 [편집] 을 누른다',
     expect='1. 열린 창에 그 템플릿의 양식명, 문서 유형, 저장된 TipTap JSON 이 채워져 있다',
     judge=['가상'],
     req=['BID-TPL-002'], fc=['BID-TPL-002'], screen='관리자 /templates', pri='High'),

dict(id='IT-10-037', flow='템플릿 수정', kind='정상', title='관리자가 템플릿 내용을 고친다',
     pre='관리자 계정, 활성 템플릿 1개',
     steps='1. 관리자 화면 메뉴 [문서 템플릿] 에서 그 템플릿 줄의 [편집] 을 누른다\n2. TipTap JSON 의 문구 하나를 바꾸고 [저장] 을 누른다\n3. 관리자 토큰으로 그 문서 유형의 템플릿 조회(GET /admin/document-templates/{documentType})를 부른다\n4. 다시 [편집] 을 누른다',
     expect='1. 문서 유형별 조회 응답의 그 템플릿 content_json 에 2단계에서 바꾼 문구가 들어 있다\n'
            '2. 다시 연 창의 TipTap JSON 에 2단계에서 바꾼 문구가 들어 있다',
     judge=['서버', '가상'],
     req=['BID-TPL-003'], fc=['BID-TPL-003'], screen='관리자 /templates', pri='Medium'),

dict(id='IT-10-038', flow='템플릿 수정', kind='정상', title='관리자가 템플릿을 비활성화한다',
     pre='관리자 계정, 어느 입찰 문서도 쓰지 않는 활성 템플릿 1개',
     steps='1. 관리자 화면 메뉴 [문서 템플릿] 에서 그 템플릿 줄의 [비활성화] 를 누르고 확인한다',
     expect='1. 그 템플릿 줄의 활성 상태가 비활성으로 보인다\n'
            '2. document_templates 의 그 행이 남아 있고 active 가 false 이다',
     judge=['가상', '서버'],
     req=['BID-TPL-003'], fc=['BID-TPL-003'], screen='관리자 /templates', pri='Medium'),

dict(id='IT-10-039', flow='템플릿 수정', kind='예외', title='입찰 문서가 쓰는 템플릿은 지워지지 않고 비활성화만 된다',
     pre='관리자 계정, 입찰 문서의 template_id 가 가리키는 활성 템플릿 1개',
     steps='1. 관리자 토큰으로 DELETE /admin/document-templates/{id} 를 부른다\n2. 그 템플릿 행과 그 템플릿을 쓰는 입찰 문서 행을 조회한다',
     expect='1. document_templates 의 그 행이 지워지지 않고 남아 있으며 active 가 false 이다\n'
            '2. 그 템플릿을 쓰는 입찰 문서의 template_id 가 그대로이다',
     judge=['서버', '서버'],
     req=['BID-TPL-003'], fc=['BID-TPL-003'], screen='', pri='Medium',
     issue='T3-2 의 DELETE 는 늘 비활성화라 쓰지 않는 템플릿을 지우는 길이 설계에 없다'),

dict(id='IT-10-040', flow='템플릿 수정', kind='예외', title='관리자가 아니면 템플릿을 고치거나 비활성화할 수 없다',
     pre='회원 계정 로그인 토큰, 활성 템플릿 1개',
     steps='1. 회원 토큰으로 PATCH /admin/document-templates/{id} 에 다른 내용을 실어 부른다\n2. 회원 토큰으로 DELETE /admin/document-templates/{id} 를 부른다',
     expect='1. 수정 요청이 403 으로 거부되고 그 템플릿 content_json 이 그대로이다\n'
            '2. 비활성화 요청이 403 으로 거부되고 그 템플릿 active 가 true 그대로이다',
     judge=['서버', '서버'],
     req=['BID-TPL-003'], fc=['BID-TPL-003'], screen='', pri='Medium'),

dict(id='IT-10-041', flow='고객 서류 읽기', kind='정상', title='분석 단계로 전환하면 공고별 서류와 서류함 서류를 읽어 회사 사실을 남긴다',
     pre='관리자 계정, 상태 DOCS_RECEIVED 입찰 건 1건(공고별 서류로 글자 층이 있는 실적 증명서 PDF 1개를 올림), 그 회원 공통 서류함에 만료일 없는 사업자등록증 PDF 1개와 만료일이 어제인 보험증권 PDF 1개, Aimbase 고객 서류 읽기 워크플로우가 도는 상태',
     steps='1. 관리자 화면 [입찰 요청 관리] 에서 그 입찰 건의 입찰 상세 관리를 연다\n2. 다음 상태 ANALYZING 으로 전환한다\n'
           '3. 고객 서류 읽기 영역을 본다\n4. 읽기가 끝날 때까지 기다린다\n5. 실적 증명서 줄을 누른다',
     expect='1. 3단계에서 고객 서류 읽기 영역에 실적 증명서(출처 공고별 서류)와 사업자등록증(출처 공통 서류함) 두 줄이 읽는 중으로 보인다\n'
            '2. 3단계에서 만료일이 지난 보험증권 줄은 영역에 없다\n'
            '3. 4단계 뒤 두 줄의 상태가 읽음이고 사실 수가 1 이상이다\n'
            '4. document_readings 에 두 파일의 content_sha256 줄이 status DONE 으로 있고 fact_count 가 화면의 사실 수와 같다\n'
            '5. 5단계에서 펼친 사실 목록에 실적 증명서에 적힌 사업명, 발주처, 계약 금액이 원문 값 그대로 쪽 번호와 함께 보인다',
     judge=['가상', '가상', '가상', '서버', '합동'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={4: dict(steps=[('key', '10.b041_done_bid'),
                           ('api', ADMIN, 'GET', READINGS),
                           ('sql', f"SELECT {uuid_of('dr.id')} AS id, dr.status, dr.fact_count FROM document_readings dr "
                                   f"WHERE dr.content_sha256 IN {candidate_shas(THIS_BID)}")],
                    need="r[1]['status'] == 200 and any(i['status'] == 'DONE' for i in r[1]['body']['items'])",
                    check="all(any(d['id'] == i['readingId'] and d['status'] == 'DONE' and int(d['fact_count']) == i['factCount'] "
                          "for d in r[2]) for i in r[1]['body']['items'] if i['status'] == 'DONE')")}),

dict(id='IT-10-042', flow='고객 서류 읽기', kind='정상', title='같은 내용의 서류는 한 번만 읽고 다른 입찰 건에서도 그 결과를 쓴다',
     pre='관리자 계정, IT-10-041 을 마친 회원의 두 번째 입찰 건(DOCS_RECEIVED, 서류함의 사업자등록증을 그 건의 요구 항목에 넣어 공고별 서류로 복사됨), Aimbase 고객 서류 읽기 워크플로우가 도는 상태',
     steps='1. document_readings 의 줄 수를 적어 둔다\n2. 그 입찰 건의 입찰 상세 관리에서 다음 상태 ANALYZING 으로 전환한다\n'
           '3. 고객 서류 읽기 영역을 본다\n4. document_readings 의 줄 수를 다시 센다',
     expect='1. 3단계 영역에 사업자등록증 줄이 하나만 있고 출처가 공고별 서류다\n'
            '2. 3단계에서 그 줄의 상태가 읽는 중을 거치지 않고 바로 읽음이며 사실 수가 IT-10-041 의 사업자등록증 줄과 같다\n'
            '3. 4단계 줄 수가 1단계와 같다',
     judge=['가상', '가상', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={3: dict(steps=[('key', '10.b042_server_bid'),
                           ('sql', f"SELECT COUNT(*) AS n FROM document_readings WHERE content_sha256 IN {candidate_shas(THIS_BID)}"),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'ANALYZING', 'notes': 'IT-10-042 서버 판정'}),
                           ('sql', f"SELECT COUNT(*) AS n FROM document_readings WHERE content_sha256 IN {candidate_shas(THIS_BID)}")],
                    check="r[2]['status'] == 200 and int(r[3][0]['n']) == int(r[1][0]['n'])")}),

dict(id='IT-10-043', flow='고객 서류 읽기', kind='정상', title='글자 층이 없는 스캔 서류는 글자 인식으로 읽는다',
     pre='관리자 계정, 상태 DOCS_RECEIVED 입찰 건 1건(공고별 서류로 글자 층이 없는 스캔 PDF 면허증 1개를 올림), Aimbase 고객 서류 읽기 워크플로우가 도는 상태',
     steps='1. 그 입찰 건의 입찰 상세 관리에서 다음 상태 ANALYZING 으로 전환한다\n2. 읽기가 끝날 때까지 기다린다\n3. 면허증 줄을 누른다',
     expect='1. 면허증 줄의 상태가 "읽음(글자 인식)" 이다\n'
            '2. 3단계 사실 목록에 면허증의 면허 종류와 면허 번호가 스캔 원본에 적힌 값 그대로 보인다\n'
            '3. document_readings 그 줄의 vision_used 가 true 이고 facts_json 에 readMode VISION 인 사실이 있다',
     judge=['가상', '합동', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={3: dict(steps=[('sql', "SELECT HEX(id) AS hex, file_name, "
                                   "(JSON_SEARCH(facts_json, 'one', 'VISION', NULL, '$[*].readMode') IS NOT NULL) AS has_vision "
                                   "FROM document_readings WHERE status = 'DONE' AND vision_used = 1")],
                    need="len(r[0]) > 0",
                    check="all(x['has_vision'] == '1' for x in r[0])")}),

dict(id='IT-10-044', flow='고객 서류 읽기', kind='정상', title='그림, 엑셀, 워드, 한글 파일 서류도 읽는다',
     pre='관리자 계정, 상태 DOCS_RECEIVED 입찰 건 1건(공고별 서류로 인력 현황 JPG 1개, 장비 목록 XLSX 1개, 회사 소개 DOCX 1개, 회사 소개 HWP 1개를 올림), Aimbase 고객 서류 읽기 워크플로우가 도는 상태',
     steps='1. 그 입찰 건의 입찰 상세 관리에서 다음 상태 ANALYZING 으로 전환한다\n2. 읽기가 끝날 때까지 기다린다\n3. 네 줄을 하나씩 눌러 사실 목록을 본다',
     expect='1. JPG 줄의 사실 목록에 그림 속 인력 현황의 직책과 인원이 원본 값 그대로 있다\n'
            '2. XLSX 줄의 사실 목록에 장비 목록 표의 장비 종류와 수량이 원본 값 그대로 있다\n'
            '3. DOCX 줄의 사실 목록에 회사 소개의 상호와 설립일이 원본 값 그대로 있다\n'
            '4. HWP 줄의 사실 목록에 회사 소개의 상호와 설립일이 원본 값 그대로 있다',
     judge=['합동', '합동', '합동', '합동'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-10-045', flow='고객 서류 읽기', kind='예외', title='읽을 수 없는 형식은 읽지 않고 표시하며 문서 작성 중 전환을 막지 않는다',
     pre='관리자 계정, 상태 DOCS_RECEIVED 입찰 건 1건(공고별 서류로 옛 워드 DOC 1개와 글자 층이 있는 PDF 1개를 올림), Aimbase 고객 서류 읽기와 입찰 분석 워크플로우가 도는 상태',
     steps='1. 그 입찰 건의 입찰 상세 관리에서 다음 상태 ANALYZING 으로 전환한다\n2. PDF 줄이 읽음이 될 때까지 기다린다\n'
           '3. 입찰 분석 영역이 분석 완료가 될 때까지 기다린 뒤 관리자 결정에서 Go 를 선택하고 [저장] 을 누른다\n4. 다음 상태 GENERATING 으로 전환한다',
     expect='1. 2단계에서 DOC 줄의 상태가 "읽을 수 없는 형식" 이다\n'
            '2. document_readings 의 DOC 줄이 status UNSUPPORTED, read_as NONE 이다\n'
            '3. 4단계 전환이 거부되지 않고 state 가 GENERATING 이 된다',
     judge=['가상', '서버', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='Medium',
     probe={2: dict(steps=[('key', '10.b045_server2_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'ANALYZING', 'notes': 'IT-10-045 서버 판정'}),
                           ('sql', "SELECT status, read_as FROM document_readings WHERE content_sha256 = '{val:0:doc_sha}'")],
                    check="r[1]['status'] == 200 and len(r[2]) == 1 and r[2][0]['status'] == 'UNSUPPORTED' and r[2][0]['read_as'] == 'NONE'"),
            3: dict(steps=[('key', '10.b045_server3_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'ANALYZING', 'notes': 'IT-10-045 서버 판정'}),
                           ('poll', ADMIN, ANALYSIS, "x['status'] == 200 and x['body']['analysis'] is not None "
                                                     "and x['body']['analysis']['status'] != 'RUNNING' and not x['body']['readingInProgress']", 1800, 20),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'GO', 'note': None}),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-045 서버 판정'}),
                           ('sql', f"SELECT state FROM bid_requests WHERE id = {THIS_BID}")],
                    check="r[1]['status'] == 200 and r[3]['status'] == 200 and r[4]['status'] == 200 and r[5][0]['state'] == 'GENERATING'")}),

dict(id='IT-10-046', flow='고객 서류 읽기', kind='예외', title='고객 서류를 읽는 중이면 문서 작성 중으로 전환되지 않는다',
     pre='관리자 계정, 상태 ANALYZING 이고 후보 서류 한 줄이 READING 이며 상태가 바뀐 지 5분이 안 된 입찰 건(입찰 분석 회차가 없어 관리자 결정도 없음)',
     steps='1. 그 입찰 건의 입찰 상세 관리를 연다\n2. 상태 패널의 다음 상태 GENERATING 을 본다\n'
           '3. 관리자 토큰으로 POST /admin/bid-requests/{id}/transition 에 GENERATING 을 실어 부른다\n4. 그 입찰 건의 state 와 state_history 를 조회한다',
     expect='1. 2단계에서 GENERATING 전환이 눌리지 않고 "고객 서류를 읽는 중입니다" 가 보인다\n'
            '2. 3단계 응답이 409 이고 코드가 DOCUMENT_READING_IN_PROGRESS 이다\n'
            '3. 4단계에서 state 가 ANALYZING 그대로이고 state_history 에 줄이 더해지지 않았다',
     judge=['가상', '서버', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={2: dict(steps=[('key', '10.b046_server2_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-046 서버 판정'})],
                    check="r[1]['status'] == 409 and r[1]['body'].get('error') == 'DOCUMENT_READING_IN_PROGRESS'"),
            3: dict(steps=[('key', '10.b046_server3_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-046 서버 판정'}),
                           ('sql', f"SELECT state, JSON_LENGTH(state_history) AS hist FROM bid_requests WHERE id = {THIS_BID}")],
                    check="r[2][0]['state'] == 'ANALYZING' and r[2][0]['hist'] == r[0][0]['hist']")}),

dict(id='IT-10-047', flow='고객 서류 읽기', kind='예외', title='읽기가 워크플로우 대기 한도를 넘게 끝나지 않은 서류는 읽지 못함이 되어 전환을 막지 않는다',
     pre='관리자 계정, 상태 ANALYZING 이고 후보 서류 한 줄이 READING 이며 status_at 이 워크플로우 대기 한도(설정 app.aimbase.polling 간격과 횟수, 10/25 상용 60분)보다 오래 전이고 입찰 분석의 관리자 결정이 Go 인 입찰 건',
     steps='1. 그 입찰 건의 입찰 상세 관리를 연다\n2. 다음 상태 GENERATING 으로 전환한다',
     expect='1. 1단계에서 그 줄의 상태가 "읽지 못함" 이고 사유 "읽기 시간 초과" 가 보인다\n'
            '2. 2단계 전환이 거부되지 않고 state 가 GENERATING 이 된다',
     judge=['가상', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='Medium',
     probe={2: dict(steps=[('key', '10.b047_server_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-047 서버 판정'}),
                           ('sql', f"SELECT state FROM bid_requests WHERE id = {THIS_BID}")],
                    check="r[1]['status'] == 200 and r[2][0]['state'] == 'GENERATING'")}),

dict(id='IT-10-048', flow='고객 서류 읽기', kind='정상', title='읽지 못한 서류를 다시 읽는다',
     pre='관리자 계정, 상태 ANALYZING 이고 후보 서류가 읽지 못함 1줄(글자 층이 있는 PDF)과 읽음 1줄인 입찰 건, Aimbase 고객 서류 읽기 워크플로우가 도는 상태',
     steps='1. 읽음 줄의 document_readings read_at 을 적어 둔다\n2. 그 입찰 건의 입찰 상세 관리 고객 서류 읽기 영역에서 [다시 읽기] 를 누른다\n3. 읽기가 끝날 때까지 기다린다',
     expect='1. 읽지 못함이던 줄이 읽는 중을 거쳐 읽음이 되고 사실 수가 1 이상이다\n'
            '2. 읽음이던 줄의 read_at 이 1단계와 같다',
     judge=['가상', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='Medium',
     probe={2: dict(steps=[('key', '10.b048_server_bid'),
                           ('sql', f"SELECT HEX(dr.id) AS hex, dr.read_at FROM document_readings dr WHERE dr.status = 'DONE' "
                                   f"AND dr.content_sha256 IN {candidate_shas(THIS_BID)} ORDER BY dr.id"),
                           ('api', ADMIN, 'POST', READINGS + '/run', {'readingIds': None}),
                           ('poll', ADMIN, READINGS, "x['status'] == 200 and not x['body']['inProgress']", 1800, 20),
                           ('sql', f"SELECT HEX(dr.id) AS hex, dr.read_at FROM document_readings dr "
                                   f"WHERE dr.content_sha256 IN {candidate_shas(THIS_BID)} ORDER BY dr.id")],
                    check="r[2]['status'] == 200 and [x for x in r[4] if x['hex'] in [y['hex'] for y in r[1]]] == r[1]")}),

dict(id='IT-10-049', flow='고객 서류 읽기', kind='예외', title='분석 단계가 아닌 입찰 건은 다시 읽기를 할 수 없다',
     pre='관리자 계정, 상태 GENERATING 이고 후보 서류에 읽지 못함 1줄이 있는 입찰 건',
     steps='1. 그 입찰 건의 입찰 상세 관리를 연다\n2. 관리자 토큰으로 POST /admin/bid-requests/{id}/document-readings/run 을 부른다',
     expect='1. 1단계 고객 서류 읽기 영역에 [다시 읽기] 단추가 없다\n'
            '2. 2단계 응답이 409 이고 코드가 NOT_ANALYZING 이다\n'
            '3. 2단계 뒤 그 줄의 status 가 FAILED 그대로다',
     judge=['가상', '서버', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='Medium',
     probe={2: dict(steps=[('key', '10.b049_bid'),
            ('sql', reading_rows(THIS_BID)),
            ('api', ADMIN, 'POST', READINGS + '/run', {'readingIds': None}),
            ('sql', reading_rows(THIS_BID))],
                    check="r[2]['status'] == 409 and r[2]['body'].get('error') == 'NOT_ANALYZING'"),
            3: dict(steps=[('key', '10.b049_bid'),
            ('sql', reading_rows(THIS_BID)),
            ('api', ADMIN, 'POST', READINGS + '/run', {'readingIds': None}),
            ('sql', reading_rows(THIS_BID))],
                    check="r[3] == r[1] and all(x['status'] == 'FAILED' for x in r[3] if x['hex'] in [y['hex'] for y in r[1] if y['status'] == 'FAILED'])")}),

dict(id='IT-10-050', flow='절별 근거 서류', kind='정상', title='제안서 설계가 읽은 고객 서류를 절마다 근거 서류로 받는다',
     pre='관리자 계정과 MCP 열쇠, 고객 서류 읽기가 끝난 입찰 건의 제안서 문서(읽음 서류 A 는 요구 항목 1개에 충족 매핑된 실적 증명서, 읽음 서류 B, 읽지 못함 서류 C)',
     steps='1. MCP get_proposal_design_input 을 그 문서 id 로 부른다\n2. MCP save_proposal_structure 를 절 1 의 sourceReadingIds [A] 로 부른다\n'
           '3. 관리자 화면 /documents/{documentId}/sections 에서 절 1 을 고른다',
     expect='1. 1단계 clientDocuments 에 A, B, C 세 줄이 status DONE, DONE, FAILED 로 있다\n'
            '2. 1단계 A 줄의 fulfills 에 A 가 충족하는 요구 항목 이름이 있다\n'
            '3. 2단계 저장이 되고 응답의 unusedReadingIds 가 [B] 이다\n'
            '4. proposal_section 절 1 의 source_reading_ids 가 [A] 이다\n'
            '5. 3단계 우측 절 상세의 근거 서류에 A 의 파일 이름 꼬리표가 A 의 사실 수와 함께 보인다',
     judge=['서버', '서버', '서버', '서버', '가상'],
     req=['BID-DOC-005'], fc=['BID-DOC-005'], screen='관리자 /documents/{id}/sections', pri='High',
     probe={1: dict(steps=[('key', '10.b050_doc'), mcp('get_proposal_design_input', {'documentId': '{val:0:id}'})],
                    check=f"len({mcp_json(1)}['clientDocuments']) == 3 and "
                          f"{{(d['readingId'], d['status']) for d in {mcp_json(1)}['clientDocuments']}} == "
                          "{(r[0][0]['a'], 'DONE'), (r[0][0]['b'], 'DONE'), (r[0][0]['c'], 'FAILED')}"),
            2: dict(steps=[('key', '10.b050_doc'), mcp('get_proposal_design_input', {'documentId': '{val:0:id}'})],
                    check=f"any(f['requirementTitle'] == r[0][0]['req'] for d in {mcp_json(1)}['clientDocuments'] "
                          "if d['readingId'] == r[0][0]['a'] for f in d['fulfills'])"),
            3: dict(steps=[('key', '10.b050_doc'), save_structure(['a'])],
                    check=f"not r[1]['body']['result'].get('isError') and {mcp_json(1)}['unusedReadingIds'] == [r[0][0]['b']]"),
            4: dict(steps=[('key', '10.b050_doc'), save_structure(['a']), ('sql', SECTION_SOURCES)],
                    check="len(r[2]) == 1 and loads(r[2][0]['source_reading_ids']) == [r[0][0]['a']]")}),

dict(id='IT-10-051', flow='절별 근거 서류', kind='예외', title='읽음이 아니거나 다른 입찰 건의 서류를 근거로 든 제안서 설계는 저장되지 않는다',
     pre='관리자 계정과 MCP 열쇠, IT-10-050 을 마친 제안서 문서, 다른 입찰 건의 읽음 서류 D',
     steps='1. 그 문서의 장과 절을 적어 둔다\n2. MCP save_proposal_structure 를 절 1 의 sourceReadingIds [A, C] 로 부른다\n'
           '3. MCP save_proposal_structure 를 절 1 의 sourceReadingIds [D] 로 부른다\n4. 그 문서의 장과 절을 다시 조회한다',
     expect='1. 2단계 응답이 거부이고 unknownReadingIds 가 [C] 이다\n'
            '2. 3단계 응답이 거부이고 unknownReadingIds 가 [D] 이다\n'
            '3. 4단계 장과 절이 1단계와 같다',
     judge=['서버', '서버', '서버'],
     req=['BID-DOC-005'], fc=['BID-DOC-005'], screen='', pri='High',
     probe={1: dict(steps=[('key', '10.b051_doc'), save_structure(['a', 'c'])],
                    check=mcp_rejected(1, 'c')),
            2: dict(steps=[('key', '10.b051_doc'), save_structure(['d'])],
                    check=mcp_rejected(1, 'd')),
            3: dict(steps=[('key', '10.b051_doc'), ('sql', STRUCTURE), save_structure(['a', 'c']), save_structure(['d']), ('sql', STRUCTURE)],
                    check="len(r[1]) > 0 and r[4] == r[1]")}),

dict(id='IT-10-052', flow='절별 근거 서류', kind='정상', title='절 작성과 절 검증 입력에 근거 서류의 사실이 들어 있다',
     pre='관리자 계정과 MCP 열쇠, IT-10-050 으로 절 1 의 근거 서류가 [A] 인 제안서 문서',
     steps='1. MCP get_section_context 를 절 1 id 로 부른다\n2. MCP get_section_verify_input 을 절 1 id 로 부른다',
     expect='1. 1단계 sourceDocuments 에 A 한 줄이 있고 facts 수가 document_readings A 줄의 fact_count 와 같다\n'
            '2. 1단계 facts 줄마다 category, key, value, page, anchor, confidence, readMode 칸이 있다\n'
            '3. 2단계 sourceDocuments 가 1단계 sourceDocuments 와 같다',
     judge=['서버', '서버', '서버'],
     req=['BID-DOC-005'], fc=['BID-DOC-005'], screen='', pri='High',
     probe={1: dict(steps=[('key', '10.b052_section'), SAVE_E_FACTS, mcp('get_section_context', {'sectionId': '{val:0:id}'}),
                           ('sql', "SELECT fact_count FROM document_readings WHERE id = UNHEX('{val:0:e_hex}')")],
                    check=f"len({mcp_json(2)}['sourceDocuments']) == 1 and {mcp_json(2)}['sourceDocuments'][0]['readingId'] == r[0][0]['e'] "
                          f"and int(r[3][0]['fact_count']) > 0 and len({mcp_json(2)}['sourceDocuments'][0]['facts']) == int(r[3][0]['fact_count'])"),
            2: dict(steps=[('key', '10.b052_section'), SAVE_E_FACTS, mcp('get_section_context', {'sectionId': '{val:0:id}'})],
                    check=f"len({mcp_json(2)}['sourceDocuments']) > 0 and "
                          f"all(d['facts'] and all({FACT_FIELDS} <= set(f) for f in d['facts']) for d in {mcp_json(2)}['sourceDocuments'])"),
            3: dict(steps=[('key', '10.b052_section'), SAVE_E_FACTS, mcp('get_section_context', {'sectionId': '{val:0:id}'}),
                           mcp('get_section_verify_input', {'sectionId': '{val:0:id}'})],
                    check=f"len({mcp_json(2)}['sourceDocuments']) > 0 and {mcp_json(3)}['sourceDocuments'] == {mcp_json(2)}['sourceDocuments']")}),

dict(id='IT-10-053', flow='절별 근거 서류', kind='정상', title='제안서 본문의 회사 사실은 근거 서류에서 오고 근거가 없으면 자리표시가 들어간다',
     pre='관리자 계정, 고객 서류 읽기가 끝난 ANALYZING 입찰 건(실적 증명서는 읽음, 인력 서류는 내지 않음, 공고 평가 항목에 수행 실적과 인력이 있음, 입찰 분석의 관리자 결정이 Go), Aimbase 제안서 워크플로우가 도는 상태',
     steps='1. 그 입찰 건의 입찰 상세 관리에서 다음 상태 GENERATING 으로 전환한다\n2. 생성이 끝날 때까지 기다린다\n'
           '3. 그 제안서의 /documents/{documentId}/sections 에서 수행 실적 절과 인력 절을 차례로 고른다',
     expect='1. 수행 실적 절의 근거 서류에 실적 증명서가 걸려 있다\n'
            '2. 수행 실적 절 본문의 사업명, 발주처, 계약 금액이 실적 증명서 사실 목록의 값과 같다\n'
            '3. 인력 절 본문에 지어낸 인원이나 자격 대신 "[Client data required:" 로 시작하는 자리표시가 있다\n'
            '4. 인력 절 검증 결과의 지어낸 말 목록에 자리표시가 들어 있지 않다',
     judge=['가상', '합동', '합동', '합동'],
     req=['BID-DOC-005', 'BID-DOC-001'], fc=['BID-DOC-005', 'BID-DOC-001'], screen='관리자 /documents/{id}/sections', pri='High'),

dict(id='IT-10-054', flow='고객 서류 읽기', kind='예외', title='관리자가 아니면 고객 서류 읽기 결과를 보거나 다시 읽힐 수 없고 서명 없는 서류 내려받기는 막힌다',
     pre='회원 계정 로그인 토큰, 읽음 줄이 있는 ANALYZING 입찰 건',
     steps='1. 회원 토큰으로 GET /admin/bid-requests/{id}/document-readings 를 부른다\n2. 회원 토큰으로 POST /admin/bid-requests/{id}/document-readings/run 을 부른다\n'
           '3. X-API-Key 머리글과 sig 없이 GET /mcp/document-readings/{readingId}/download 를 부른다',
     expect='1. 1단계 응답이 403 이다\n'
            '2. 2단계 응답이 403 이고 document_readings 줄들의 status 가 부르기 전과 같다\n'
            '3. 3단계 응답이 401 이고 본문이 비어 있다',
     judge=['서버', '서버', '서버'],
     req=['BID-DOC-004'], fc=['BID-DOC-004'], screen='', pri='High',
     probe={1: dict(steps=[('key', '10.b041_done_bid'),
                           ('api', USER, 'GET', '/api/admin/bid-requests/{val:0:id}/document-readings')],
                    check="r[1]['status'] == 403"),
            2: dict(steps=[('key', '10.b041_done_bid'),
                           ('sql', reading_rows(THIS_BID)),
                           ('api', USER, 'POST', '/api/admin/bid-requests/{val:0:id}/document-readings/run', {'readingIds': None}),
                           ('sql', reading_rows(THIS_BID))],
                    check="r[2]['status'] == 403 and r[3] == r[1]"),
            3: dict(steps=[('key', '10.b054_reading'),
                           ('api', 'guest', 'GET', '/api/mcp/document-readings/{val:0:id}/download')],
                    check="r[1]['status'] == 401 and not r[1]['body']")}),

dict(id='IT-10-055', flow='입찰 분석', kind='정상', title='고객 서류를 다 읽으면 입찰 분석이 돌아 종합 분석 리포트, 캡처플랜, Bid/No-Bid 결정이 차례로 채워진다',
     pre='관리자 계정, 공고 분석이 COMPLETED 인 공고의 상태 DOCS_RECEIVED 입찰 건 1건(공고별 서류로 글자 층이 있는 실적 증명서 PDF 1개를 올림), 그 공고 NAICS 의 미 연방 계약이 federal_contracts 에 있음, Aimbase 고객 서류 읽기와 입찰 분석 워크플로우가 도는 상태',
     steps='1. 관리자 화면 [입찰 요청 관리] 에서 그 입찰 건의 입찰 상세 관리를 열고 다음 상태 ANALYZING 으로 전환한다\n'
           '2. 고객 서류 읽기 영역의 실적 증명서 줄이 읽음이 될 때까지 기다리며 입찰 분석 영역을 본다\n'
           '3. 입찰 분석 영역 머리가 분석 완료가 될 때까지 기다린다\n'
           '4. 종합 분석 리포트, 캡처플랜, Bid/No-Bid 결정 탭을 차례로 연다',
     expect='1. 2단계에서 서류를 읽는 동안 입찰 분석 영역에 "고객 서류를 다 읽으면 입찰 분석을 시작합니다" 가 보인다\n'
            '2. 실적 증명서 줄이 읽음이 된 뒤 bid_analyses 에 그 입찰 건의 status RUNNING 줄이 1줄 생긴다\n'
            '3. 3단계 뒤 그 줄의 status 가 DONE 이고 report_json, capture_plan_json, decision_json, finished_at 이 모두 채워져 있다\n'
            '4. 3단계 뒤 머리에 Pwin(%)과 판정(Strong Go, 조건부 Go, Weak, No-Go 가운데 하나)과 판정 이유가 보인다\n'
            '5. 종합 분석 리포트 탭의 사업명, 발주처, 마감이 그 공고의 값과 같고 발주 이력 표에 그 공고 NAICS 의 미 연방 계약이 계약번호와 수주사로 들어 있다\n'
            '6. 캡처플랜 탭에 양식 1장부터 5장까지가 차례로 있고 2장 이슈 정리에 핵심이슈 순위 표가 있다\n'
            '7. Bid/No-Bid 결정 탭의 스코어카드에 자사와 경쟁사마다 일곱 팩터(고객 관계, 솔루션 적합도, 경쟁 환경, 가격 경쟁력, 과거 실적, 참여 인력, 환경 요인)의 점수와 근거, 가중 점수가 있다\n'
            '8. 종합 분석 리포트의 자사 실적(사업명, 발주처)이 실적 증명서의 값이고 다른 회사의 실적이 섞이지 않는다',
     judge=['가상', '서버', '서버', '가상', '합동', '합동', '합동', '합동'],
     req=['BID-DOC-006'], fc=['BID-DOC-006'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={2: dict(steps=[('key', '10.b055_server_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'ANALYZING', 'notes': 'IT-10-055 서버 판정'}),
                           ('poll', ADMIN, ANALYSIS, "x['status'] != 200 or x['body']['analysis'] is not None", 1800, 20),
                           ('sql', reading_rows(THIS_BID)),
                           ('sql', analysis_rows())],
                    need="r[1]['status'] == 200 and len(r[3]) > 0 and all(x['status'] == 'DONE' for x in r[3])",
                    check="len(r[4]) == 1 and r[4][0]['status'] == 'RUNNING' and r[4][0]['started_at'] >= max(x['read_at'] for x in r[3])"),
            3: dict(steps=[('key', '10.b055_server_bid'),
                           ('poll', ADMIN, ANALYSIS, NOT_RUNNING, 3600, 30),
                           ('sql', analysis_rows())],
                    need="len(r[2]) > 0 and r[2][-1]['workflow_run_id']",
                    check="r[2][-1]['status'] == 'DONE' and int(r[2][-1]['has_report']) == 1 and int(r[2][-1]['has_plan']) == 1 "
                          "and int(r[2][-1]['has_decision']) == 1 and r[2][-1]['finished_at'] is not None")}),

dict(id='IT-10-056', flow='입찰 분석', kind='정상', title='읽을 서류가 없는 입찰 건은 분석 단계로 전환하자마자 입찰 분석이 시작된다',
     pre='관리자 계정, 공고에 필수 서류 칸이 없어 공고별 서류도 만료되지 않은 서류함 서류도 없이 DOCS_RECEIVED 가 된 입찰 건, Aimbase 입찰 분석 워크플로우가 도는 상태',
     steps='1. 관리자 화면에서 그 입찰 건의 입찰 상세 관리를 연다\n2. 다음 상태 ANALYZING 으로 전환한다\n3. 입찰 분석 영역을 본다',
     expect='1. 1단계에서 입찰 상세 관리에 입찰 분석 영역이 없다\n'
            '2. 2단계 직후 bid_analyses 에 그 입찰 건의 status RUNNING 줄이 1줄 있고 started_at 이 전환 시각이다\n'
            '3. 3단계에서 입찰 분석 영역 머리가 분석 중이고 [다시 분석] 과 관리자 결정 [저장] 이 꺼져 있다',
     judge=['가상', '서버', '가상'],
     req=['BID-DOC-006'], fc=['BID-DOC-006'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={2: dict(steps=[('key', '10.b056_server_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'ANALYZING', 'notes': 'IT-10-056 서버 판정'}),
                           ('sql', last_entered_gap())],
                    need=f"len(r[2]) != 1 or {ran(2)}",
                    check="r[1]['status'] == 200 and len(r[2]) == 1 and r[2][0]['status'] == 'RUNNING' and abs(int(r[2][0]['gap_us'])) < 5000000")}),

dict(id='IT-10-057', flow='입찰 분석', kind='정상', title='다른 입찰 건이 같은 내용의 서류를 읽어 끝나도 그 건의 입찰 분석이 시작된다',
     pre='관리자 계정, 같은 회원의 상태 ANALYZING 입찰 건 A 와 DOCS_RECEIVED 입찰 건 B(둘 다 같은 내용의 실적 증명서 PDF 를 공고별 서류로 올림), A 의 그 서류를 읽는 중',
     steps='1. B 를 다음 상태 ANALYZING 으로 전환한다\n2. A 의 읽기 회차가 끝날 때까지 기다린다\n3. A 와 B 의 입찰 상세 관리에서 입찰 분석 영역을 본다',
     expect='1. 1단계 직후 B 의 bid_analyses 줄이 없다\n'
            '2. 2단계 뒤 A 와 B 에 bid_analyses status RUNNING 줄이 각 1줄 생긴다\n'
            '3. 3단계에서 두 건의 입찰 분석 영역 머리가 분석 중이다',
     judge=['서버', '서버', '가상'],
     req=['BID-DOC-006'], fc=['BID-DOC-006'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={1: dict(steps=[('key', '10.b057_server'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'ANALYZING', 'notes': 'IT-10-057 서버 판정 A'}),
                           ('api', ADMIN, 'POST', bid_api('b', '/transition'), {'toState': 'ANALYZING', 'notes': 'IT-10-057 서버 판정 B'}),
                           ('sql', "SELECT status FROM document_readings WHERE content_sha256 = '{val:0:sha}'"),
                           ('sql', analysis_rows(bid_of('b_hex')))],
                    need="r[1]['status'] == 200 and len(r[3]) == 1 and r[3][0]['status'] == 'READING'",
                    check="r[2]['status'] == 200 and len(r[4]) == 0"),
            2: dict(steps=[('key', '10.b057_server'),
                           ('sql', f"SELECT state FROM bid_requests WHERE id = {THIS_BID}"),
                           ('poll', ADMIN, ANALYSIS, "x['status'] != 200 or x['body']['analysis'] is not None", 1800, 20),
                           ('poll', ADMIN, bid_api('b', '/bid-analysis'), "x['status'] != 200 or x['body']['analysis'] is not None", 120, 10),
                           ('sql', "SELECT status FROM document_readings WHERE content_sha256 = '{val:0:sha}'"),
                           ('sql', analysis_rows()),
                           ('sql', analysis_rows(bid_of('b_hex')))],
                    need="r[1][0]['state'] == 'ANALYZING' and len(r[4]) == 1 and r[4][0]['status'] == 'DONE'",
                    check="len(r[5]) == 1 and r[5][0]['status'] == 'RUNNING' and len(r[6]) == 1 and r[6][0]['status'] == 'RUNNING'")}),

dict(id='IT-10-058', flow='입찰 분석', kind='정상', title='입찰 분석 회차가 이미 있는 건은 서류를 다시 읽어도 입찰 분석이 자동으로 다시 돌지 않는다',
     pre='관리자 계정, 상태 ANALYZING 이고 입찰 분석이 분석 완료이며 후보 서류에 읽지 못함 1줄이 있는 입찰 건',
     steps='1. 그 입찰 건의 입찰 상세 관리에서 고객 서류 읽기 영역의 [다시 읽기] 를 누른다\n2. 그 줄을 다시 읽어 끝날 때까지 기다린다\n3. 입찰 분석 영역을 본다',
     expect='1. 2단계 뒤 bid_analyses 의 그 입찰 건 줄 수가 1단계 전과 같다\n'
            '2. 3단계에서 머리가 분석 완료 그대로이고 [다시 분석] 이 살아 있다',
     judge=['서버', '가상'],
     req=['BID-DOC-006'], fc=['BID-DOC-006'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={1: dict(steps=[('key', '10.b058_server_bid'),
                           ('sql', analysis_rows()),
                           ('api', ADMIN, 'POST', READINGS + '/run', {'readingIds': None}),
                           ('poll', ADMIN, READINGS, "x['status'] == 200 and not x['body']['inProgress']", 1800, 20),
                           wait(60),
                           ('sql', analysis_rows())],
                    check="r[2]['status'] == 200 and len(r[5]) == len(r[1])")}),

dict(id='IT-10-059', flow='입찰 분석', kind='정상', title='조사한 값마다 확신도와 출처가 붙고 웹 자료가 필요한 칸은 미상으로 확인 필요에 오른다',
     pre='관리자 계정, 입찰 분석이 분석 완료인 ANALYZING 입찰 건',
     steps='1. 입찰 분석 영역의 종합 분석 리포트 탭을 연다\n2. 발주처 칸과 발주 이력 표를 본다\n3. 확인 필요 목록을 본다',
     expect='1. 1단계에서 근거 값마다 확정, 추정, 미상 가운데 하나의 확신도 꼬리표와 출처가 함께 보인다\n'
            '2. report_json, capture_plan_json, decision_json 의 근거 값마다 confidence 가 CONFIRMED, ESTIMATED, UNKNOWN 가운데 하나이고 source 가 비어 있지 않다\n'
            '3. 세 산출물에서 source 가 "웹 자료 미사용" 인 근거 값은 모두 confidence 가 UNKNOWN 이다\n'
            '4. 3단계 확인 필요 목록에 source 가 "웹 자료 미사용" 인 칸마다 그 칸을 묻는 줄이 있다\n'
            '5. 3단계 확인 필요 목록이 고객에게 물을 것과 관리자가 볼 것 두 묶음으로 나뉘어 있고 줄마다 질문과 이유가 있다\n'
            '6. 리포트의 고객 회사 사실은 출처가 고객 회사 칸, 고객 서류 사실, 참가 자격 답 가운데 하나이고 참가 자격 답에서 온 값의 출처에 고객이 스스로 답한 것이라는 말이 있다',
     judge=['가상', '서버', '서버', '합동', '가상', '합동'],
     req=['BID-DOC-006'], fc=['BID-DOC-006'], screen='관리자 /bid-requests/{id}', pri='High',
     issue='참가 자격 답에서 온 값을 고객이 스스로 답한 것으로 표시하는 문구가 설계서(T3-4)에 없다',
     probe={2: dict(steps=[('key', '10.b055_server_bid'), ('sql', LATEST)],
                    need="len(r[1]) == 1 and r[1][0]['status'] == 'DONE' and r[1][0]['workflow_run_id']",
                    check="(lambda E: len(E) > 0 and all(e.get('confidence') in ('CONFIRMED', 'ESTIMATED', 'UNKNOWN') "
                          "and str(e.get('source') or '').strip() != '' for e in E))"
                          f"([e for c in {OUTPUTS} for e in {evidences('loads(r[1][0][c])')}])"),
            3: dict(steps=[('key', '10.b055_server_bid'), ('sql', LATEST)],
                    need="len(r[1]) == 1 and r[1][0]['status'] == 'DONE' and r[1][0]['workflow_run_id']",
                    check=f"all(e.get('confidence') == 'UNKNOWN' for c in {OUTPUTS} for e in {evidences('loads(r[1][0][c])')} "
                          f"if e.get('source') == {NO_WEB!r})")}),

dict(id='IT-10-060', flow='입찰 분석', kind='정상', title='Pwin, 판정, 강점과 약점, 핵심이슈 순위는 서버가 점수로 계산한 값이다',
     pre='관리자 계정, 입찰 분석이 분석 완료인 ANALYZING 입찰 건, system_settings BID_PWIN_WEIGHTS 와 BID_PWIN_THRESHOLDS 가 초기값',
     steps='1. 입찰 분석 영역 머리와 Bid/No-Bid 결정 탭의 스코어카드를 본다\n2. 캡처플랜 탭의 핵심이슈 순위 표를 본다\n'
           '3. 관리자 토큰으로 GET /admin/bid-requests/{id}/bid-analysis 를 부른다',
     expect='1. 3단계 응답의 pwinPercent 가 scorecard 의 OUR_COMPANY 일곱 팩터 점수에 초기 가중치를 곱해 더하고 5 로 나눈 값(소수 첫째 자리)과 같다\n'
            '2. 3단계 응답의 qualification 에 resolvable false 가 없으면 aiVerdict 가 pwinPercent 70 이상 STRONG_GO, 50 이상 CONDITIONAL_GO, 30 이상 WEAK, 그 아래 NO_GO 와 맞다\n'
            '3. 3단계 응답의 strengths 와 weaknesses 가 BCC 테마마다 자사 점수에서 경쟁사 최고점을 뺀 값이 +1 이상인 테마, -1 이하인 테마와 같다\n'
            '4. 3단계 응답의 issueRanking 이 묶은 이슈마다 속한 관련자 이슈의 영향력 x 중요도를 합한 값의 높은 순이다\n'
            '5. 1단계 머리의 Pwin 과 판정이 3단계 응답의 pwinPercent, aiVerdict 와 같다\n'
            '6. 2단계 순위 표의 차례가 3단계 응답의 issueRanking 과 같다\n'
            '7. 해결할 수 없는 자격 미달이 한 줄이라도 있으면 Pwin 이 70 이상이어도 판정이 NO_GO 이고 판정 이유에 그 항목이 있다',
     judge=['서버', '서버', '서버', '서버', '가상', '가상', '단위'],
     req=['BID-DOC-006'], fc=['BID-DOC-006'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={1: dict(steps=[('key', '10.b060_server_bid')] + saved_all(), need=SAVED,
                    check=f"(lambda o: abs(float({A4}['pwinPercent']) - int(sum(o[f]['score'] * w for f, w in {PWIN_WEIGHTS!r}.items()) * 2 + 0.5) / 10) < 0.001)"
                          f"([s['scores'] for s in {A4}['decision']['scorecard'] if s['company'] == 'OUR_COMPANY'][0])"),
            2: dict(steps=[('key', '10.b060_server_bid')] + saved_all(),
                    need=SAVED + f" and not any(q.get('resolvable') is False for q in {A4}['decision'].get('qualification') or [])",
                    check=f"{A4}['aiVerdict'] == (lambda p: 'STRONG_GO' if p >= 70 else 'CONDITIONAL_GO' if p >= 50 else 'WEAK' if p >= 30 "
                          f"else 'NO_GO')(float({A4}['pwinPercent']))"),
            3: dict(steps=[('key', '10.b060_server_bid')] + saved_all(), need=SAVED,
                    check=f"(lambda b: (lambda d: sorted(x['theme'] for x in {A4}['strengths']) == sorted(t for t in b['themes'] if d(t) >= 1) "
                          f"and sorted(x['theme'] for x in {A4}['weaknesses']) == sorted(t for t in b['themes'] if d(t) <= -1))"
                          "(lambda t: b['scores']['OUR_COMPANY'][t] - max(v[t] for k, v in b['scores'].items() if k != 'OUR_COMPANY')))"
                          f"({A4}['report']['competitors']['bcc'])"),
            4: dict(steps=[('key', '10.b060_server_bid')] + saved_all(), need=SAVED,
                    check=f"(lambda c: (lambda s: [(x['issue'], x['score']) for x in {A4}['issueRanking']] == "
                          "sorted([(g['issue'], sum(s[i] for i in g['memberIssues'])) for g in c['groupedIssues']], key=lambda y: -y[1]))"
                          "({i['id']: i['influence'] * i['importance'] for i in c['stakeholderIssues']}))"
                          f"({A4}['capturePlan']['issues'])")}),

dict(id='IT-10-061', flow='입찰 분석', kind='정상', title='Pwin 가중치를 바꾸면 다음 계산부터 맞고 이미 계산한 회차는 그대로다',
     pre='관리자 계정, 입찰 분석이 분석 완료인 ANALYZING 입찰 건(읽는 중인 서류 없음), system_settings BID_PWIN_WEIGHTS 가 초기값',
     steps='1. 그 회차의 pwin_percent 를 적어 둔다\n'
           '2. system_settings 의 BID_PWIN_WEIGHTS 를 합 100 인 다른 값(CUSTOMER_RELATION:40,SOLUTION_FIT:10,COMPETITION:10,PRICE:10,PAST_PERFORMANCE:10,PERSONNEL:10,ENVIRONMENT:10)으로 바꾼다\n'
           '3. 입찰 분석 영역의 [다시 분석] 으로 신규 회차를 돌려 분석 완료가 될 때까지 기다린다\n'
           '4. BID_PWIN_WEIGHTS 를 초기값으로 되돌린다',
     expect='1. 3단계 뒤 앞 회차의 pwin_percent 가 1단계에 적은 값 그대로다\n'
            '2. 신규 회차의 pwin_percent 가 2단계에서 바꾼 가중치로 계산한 값이다\n'
            '3. BID_PWIN_WEIGHTS 의 합이 100 이 아니거나 팩터가 빠지면 초기 가중치로 계산하고 경고 로그가 1줄 남는다',
     judge=['단위', '단위', '단위'],
     req=['BID-DOC-006'], fc=['BID-DOC-006'], screen='', pri='High'),

dict(id='IT-10-062', flow='입찰 분석', kind='정상', title='다시 분석하면 신규 회차가 지금 분석이 되고 앞 회차의 결정은 이어지지 않는다',
     pre='관리자 계정, 입찰 분석이 분석 완료이고 관리자 결정이 Go 인 ANALYZING 입찰 건(읽는 중인 서류 없음), Aimbase 입찰 분석 워크플로우가 도는 상태',
     steps='1. 입찰 분석 영역의 [다시 분석] 을 누른다\n2. 확인 창에서 확인을 누른다\n3. 분석 완료가 될 때까지 기다린다\n'
           '4. 관리자 토큰으로 POST /admin/bid-requests/{id}/transition 에 GENERATING 을 실어 부른다',
     expect='1. 1단계에서 "다시 분석하면 지금 결정은 이어지지 않습니다" 확인 창이 뜬다\n'
            '2. 2단계 뒤 bid_analyses 에 그 입찰 건 줄이 하나 늘고 늘어난 줄이 status RUNNING, admin_decision 빈 값, started_by 그 관리자다\n'
            '3. 3단계 뒤 머리의 관리자 결정이 비어 있고 상태 패널의 GENERATING 전환이 막혀 있다\n'
            '4. 4단계 응답이 409 이고 코드가 BID_DECISION_REQUIRED 이며 state 가 ANALYZING 그대로다',
     judge=['가상', '서버', '가상', '서버'],
     req=['BID-DOC-006'], fc=['BID-DOC-006', 'BID-DOC-007'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={2: dict(steps=[('key', '10.b062_server_bid'),
                           ('sql', analysis_rows()),
                           ('api', ADMIN, 'POST', RERUN),
                           ('sql', analysis_rows()),
                           ('sql', ADMIN_ID)],
                    need=f"len(r[3]) <= len(r[1]) or {ran(3)}",
                    check="r[2]['status'] == 200 and len(r[3]) == len(r[1]) + 1 and r[3][-1]['status'] == 'RUNNING' "
                          "and r[3][-1]['admin_decision'] is None and r[3][-1]['started_by'] == r[4][0]['hex']"),
            4: dict(steps=[('key', '10.b062_server_bid'),
                           ('poll', ADMIN, ANALYSIS, NOT_RUNNING, 1800, 20),
                           ('sql', analysis_rows()),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-062 서버 판정'}),
                           ('sql', f"SELECT state FROM bid_requests WHERE id = {THIS_BID}")],
                    need="len(r[2]) >= 2",
                    check=refused(3, 'BID_DECISION_REQUIRED') + " and r[4][0]['state'] == 'ANALYZING'")}),

dict(id='IT-10-063', flow='입찰 분석 도구', kind='정상', title='get_bid_analysis_input 은 단계마다 정해진 자료를 돌려보낸다',
     pre='서버 열쇠(MCP_API_KEY)를 아는 호출자, 입찰 분석이 분석 완료인 ANALYZING 입찰 건(그 공고번호의 정정 공고 1건, 같은 기관 공고 21건 이상, 값이 300자를 넘는 고객 서류 사실 1개 이상)',
     steps='1. POST /mcp 에 tools/call 본문(name get_bid_analysis_input, bidRequestId 그 건, analysisId 그 회차, step REPORT)을 보낸다\n'
           '2. 같은 본문을 step CAPTURE_PLAN 으로 보낸다\n3. 같은 본문을 step DECISION 으로 보낸다',
     expect='1. 1단계 결과에 같은 공고번호의 다른 공고 1건과 같은 기관 최근 공고 20건이 있다\n'
            '2. 1단계 결과의 고객 서류 사실 값이 모두 300자 이하다\n'
            '3. 2단계 결과에 그 회차의 종합 분석 리포트가 있다\n'
            '4. 3단계 결과에 그 회차의 종합 분석 리포트와 캡처플랜, BID_PWIN_WEIGHTS 의 가중치가 있다',
     judge=['서버', '서버', '서버', '서버'],
     req=['BID-DOC-006'], fc=['BID-DOC-006'], screen='', pri='High',
     probe={1: dict(steps=[('key', '10.b063_bid'),
                           mcp('get_bid_analysis_input', {'bidRequestId': '{val:0:id}', 'analysisId': '{val:0:analysis}', 'step': 'REPORT'})],
                    check=f"len({mcp_json(1)}['sameSolicitationNotices']) == 1 and len({mcp_json(1)}['agencyRecentNotices']) == 20"),
            2: dict(steps=[('key', '10.b063_bid'),
                           mcp('get_bid_analysis_input', {'bidRequestId': '{val:0:id}', 'analysisId': '{val:0:analysis}', 'step': 'REPORT'}),
                           ('sql', f"SELECT dr.facts_json FROM document_readings dr WHERE dr.status = 'DONE' AND dr.content_sha256 IN {candidate_shas(THIS_BID)}")],
                    need="any(len(str(f['value'])) > 300 for x in r[2] for f in loads(x['facts_json']))",
                    check=f"len([f for d in {mcp_json(1)}['clientDocuments'] for f in d.get('facts') or []]) > 0 and "
                          f"all(len(str(f['value'])) <= 300 for d in {mcp_json(1)}['clientDocuments'] for f in d.get('facts') or [])"),
            3: dict(steps=[('key', '10.b063_bid'),
                           mcp('get_bid_analysis_input', {'bidRequestId': '{val:0:id}', 'analysisId': '{val:0:analysis}', 'step': 'CAPTURE_PLAN'}),
                           ('sql', LATEST)],
                    check=f"{mcp_json(1)}['report'] is not None and {mcp_json(1)}['report'] == loads(r[2][0]['report_json'])"),
            4: dict(steps=[('key', '10.b063_bid'),
                           mcp('get_bid_analysis_input', {'bidRequestId': '{val:0:id}', 'analysisId': '{val:0:analysis}', 'step': 'DECISION'}),
                           ('sql', LATEST),
                           ('sql', "SELECT setting_value FROM system_settings WHERE setting_key = 'BID_PWIN_WEIGHTS'")],
                    check=f"{mcp_json(1)}['report'] is not None and all({mcp_json(1)}['report'][k] == loads(r[2][0]['report_json']).get(k) "
                          f"for k in {mcp_json(1)}['report']) and {mcp_json(1)}['capturePlan'] == loads(r[2][0]['capture_plan_json']) "
                          f"and {mcp_json(1)}['pwinPolicy']['weights'] == "
                          "{p.split(':')[0]: int(p.split(':')[1]) for p in r[3][0]['setting_value'].split(',')}")}),

dict(id='IT-10-064', flow='입찰 분석 도구', kind='정상', title='search_award_history 는 미 연방 계약과 낙찰 공고를 합쳐 출처를 달아 찾는다',
     pre='서버 열쇠(MCP_API_KEY)를 아는 호출자, federal_contracts 와 낙찰 공고에 모두 있는 NAICS 코드 하나(합쳐 51건 이상)',
     steps='1. POST /mcp 에 tools/call 본문(name search_award_history, naicsCodes 그 코드)을 보낸다\n2. 같은 본문에 limit 100 을 실어 보낸다',
     expect='1. 1단계 결과 줄마다 source 가 FEDERAL_CONTRACT 나 SAM_AWARD_NOTICE 이고 두 값이 모두 나온다\n'
            '2. 1단계 결과가 20줄이다\n'
            '3. 1단계 결과 줄에 원본 JSON 칸이 없다\n'
            '4. 2단계 결과가 50줄이다',
     judge=['서버', '서버', '서버', '서버'],
     req=['BID-DOC-006'], fc=['BID-DOC-006'], screen='', pri='High',
     probe={1: dict(steps=[('key', '10.b064_naics'), mcp('search_award_history', {'naicsCodes': ['{val:0:naics}']})],
                    check=f"set(x['source'] for x in {mcp_json(1)}['rows']) == {{'FEDERAL_CONTRACT', 'SAM_AWARD_NOTICE'}}"),
            2: dict(steps=[('key', '10.b064_naics'), mcp('search_award_history', {'naicsCodes': ['{val:0:naics}']})],
                    check=f"len({mcp_json(1)}['rows']) == 20"),
            3: dict(steps=[('key', '10.b064_naics'), mcp('search_award_history', {'naicsCodes': ['{val:0:naics}']})],
                    check=f"len({mcp_json(1)}['rows']) > 0 and all('rawJson' not in x and 'raw_json' not in x for x in {mcp_json(1)}['rows'])"),
            4: dict(steps=[('key', '10.b064_naics'), mcp('search_award_history', {'naicsCodes': ['{val:0:naics}'], 'limit': 100})],
                    check=f"len({mcp_json(1)}['rows']) == 50")}),

dict(id='IT-10-065', flow='입찰 분석', kind='예외', title='분석 중이 워크플로우 대기 한도를 넘으면 분석 실패 "분석 시간 초과" 가 된다',
     pre='관리자 계정, 상태 ANALYZING 이고 입찰 분석 회차가 RUNNING 이며 started_at 이 워크플로우 대기 한도(설정 app.aimbase.polling 간격과 횟수, 10/25 상용 60분)보다 오래 전인 입찰 건',
     steps='1. 그 입찰 건의 입찰 상세 관리를 연다\n2. 관리자 토큰으로 GET /admin/bid-requests/{id}/bid-analysis 를 부른다\n'
           '3. 입찰 분석 영역의 [다시 분석] 을 누르고 확인한다',
     expect='1. 1단계 입찰 분석 영역 머리가 분석 실패이고 사유 "분석 시간 초과" 가 보이며 [다시 분석] 이 살아 있다\n'
            '2. 2단계 응답의 analysis.status 가 FAILED 이고 failureReason 이 "분석 시간 초과" 다\n'
            '3. 3단계 뒤 bid_analyses 에 그 입찰 건의 status RUNNING 줄이 하나 늘어 있다',
     judge=['가상', '서버', '서버'],
     req=['BID-DOC-006'], fc=['BID-DOC-006'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={2: dict(steps=[('key', '10.b065_server2_bid'), ('api', ADMIN, 'GET', ANALYSIS)],
                    check="r[1]['status'] == 200 and r[1]['body']['analysis']['status'] == 'FAILED' "
                          "and r[1]['body']['analysis']['failureReason'] == '분석 시간 초과'"),
            3: dict(steps=[('key', '10.b065_server3_bid'),
                           ('sql', analysis_rows()),
                           ('api', ADMIN, 'POST', RERUN),
                           ('sql', analysis_rows())],
                    need=f"len(r[3]) <= len(r[1]) or {ran(3)}",
                    check="r[2]['status'] == 200 and len(r[3]) == len(r[1]) + 1 and r[3][-1]['status'] == 'RUNNING'")}),

dict(id='IT-10-066', flow='입찰 분석', kind='예외', title='Bid/No-Bid 결정을 저장하지 못하고 끝난 입찰 분석은 분석 실패가 되고 앞 단계 탭은 남는다',
     pre='관리자 계정, 입찰 분석 워크플로우가 종합 분석 리포트와 캡처플랜만 저장하고 끝난 ANALYZING 입찰 건',
     steps='1. 워크플로우가 끝난 뒤 그 입찰 건의 입찰 상세 관리를 연다\n2. 종합 분석 리포트와 캡처플랜, Bid/No-Bid 결정 탭을 차례로 연다',
     expect='1. 그 회차의 status 가 FAILED 이고 failure_reason 이 "Bid/No-Bid 결정이 저장되지 않음" 이다\n'
            '2. 1단계 머리가 분석 실패이고 사유 "Bid/No-Bid 결정이 저장되지 않음" 이 보인다\n'
            '3. 2단계에서 종합 분석 리포트와 캡처플랜 탭에 저장된 내용이 보이고 Bid/No-Bid 결정 탭에는 스코어카드가 없다',
     judge=['단위', '가상', '가상'],
     req=['BID-DOC-006'], fc=['BID-DOC-006'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-10-067', flow='입찰 분석', kind='예외', title='분석 중이거나 서류를 읽는 중이면 다시 분석이 막히고 분석 중이면 결정 저장도 막힌다',
     pre='관리자 계정, 입찰 분석 회차가 RUNNING(대기 한도 안)인 ANALYZING 입찰 건 A, 입찰 분석이 분석 완료이고 후보 서류 한 줄이 READING 인 ANALYZING 입찰 건 B',
     steps='1. A 의 입찰 상세 관리를 열어 입찰 분석 영역을 본다\n'
           '2. 관리자 토큰으로 A 에 POST /admin/bid-requests/{id}/bid-analysis/run 을 부른다\n'
           '3. 관리자 토큰으로 A 에 POST /admin/bid-requests/{id}/bid-analysis/decision 에 GO 를 실어 부른다\n'
           '4. B 의 입찰 상세 관리를 열어 입찰 분석 영역을 본다\n'
           '5. 관리자 토큰으로 B 에 POST /admin/bid-requests/{id}/bid-analysis/run 을 부른다',
     expect='1. 1단계에서 [다시 분석] 과 관리자 결정 [저장] 이 꺼져 있다\n'
            '2. 2단계 응답이 409 BID_ANALYSIS_NOT_ALLOWED 이고 사유가 ANALYSIS_RUNNING 이다\n'
            '3. 3단계 응답이 409 BID_ANALYSIS_NOT_ALLOWED 이고 A 의 bid_analyses 줄 수와 admin_decision 이 부르기 전과 같다\n'
            '4. 4단계에서 [다시 분석] 이 꺼져 있다\n'
            '5. 5단계 응답이 409 BID_ANALYSIS_NOT_ALLOWED 이고 사유가 READING_IN_PROGRESS 다',
     judge=['가상', '서버', '서버', '가상', '서버'],
     req=['BID-DOC-006'], fc=['BID-DOC-006', 'BID-DOC-007'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={2: dict(steps=[('key', '10.b067_a_bid'), ('api', ADMIN, 'POST', RERUN)],
                    check=refused(1, 'BID_ANALYSIS_NOT_ALLOWED', 'ANALYSIS_RUNNING')),
            3: dict(steps=[('key', '10.b067_a_bid'),
                           ('sql', analysis_rows()),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'GO', 'note': None}),
                           ('sql', analysis_rows())],
                    check=refused(2, 'BID_ANALYSIS_NOT_ALLOWED') + " and r[3] == r[1]"),
            5: dict(steps=[('key', '10.b067_b_bid'), ('api', ADMIN, 'POST', RERUN)],
                    check=refused(1, 'BID_ANALYSIS_NOT_ALLOWED', 'READING_IN_PROGRESS'))}),

dict(id='IT-10-068', flow='입찰 분석', kind='예외', title='분석 단계가 아닌 입찰 건은 다시 분석과 관리자 결정을 할 수 없다',
     pre='관리자 계정, 입찰 분석이 분석 완료이고 관리자 결정이 Go 인 채 GENERATING 으로 전환된 입찰 건',
     steps='1. 그 입찰 건의 입찰 상세 관리를 연다\n'
           '2. 관리자 토큰으로 POST /admin/bid-requests/{id}/bid-analysis/run 을 부른다\n'
           '3. 관리자 토큰으로 POST /admin/bid-requests/{id}/bid-analysis/decision 에 NO_GO 와 사유를 실어 부른다',
     expect='1. 1단계 입찰 분석 영역에 Pwin, 판정, 관리자 결정 Go 가 보이고 [다시 분석] 과 관리자 결정 폼이 없다\n'
            '2. 2단계 응답이 409 BID_ANALYSIS_NOT_ALLOWED 이고 사유가 NOT_ANALYZING 이다\n'
            '3. 3단계 응답이 409 BID_ANALYSIS_NOT_ALLOWED 이고 admin_decision 이 GO 그대로다',
     judge=['가상', '서버', '서버'],
     req=['BID-DOC-006'], fc=['BID-DOC-006', 'BID-DOC-007'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={2: dict(steps=[('key', '10.b068_bid'), ('api', ADMIN, 'POST', RERUN)],
                    check=refused(1, 'BID_ANALYSIS_NOT_ALLOWED', 'NOT_ANALYZING')),
            3: dict(steps=[('key', '10.b068_bid'),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'NO_GO', 'note': 'IT-10-068 서버 판정'}),
                           ('sql', analysis_rows())],
                    check=refused(1, 'BID_ANALYSIS_NOT_ALLOWED') + " and len(r[2]) > 0 and r[2][-1]['admin_decision'] == 'GO'")}),

dict(id='IT-10-069', flow='입찰 분석 도구', kind='예외', title='입찰 분석 저장과 낙찰 이력 찾기는 틀린 호출을 거부한다',
     pre='서버 열쇠(MCP_API_KEY)를 아는 호출자, 입찰 분석이 분석 완료인 입찰 건 A 의 회차, 회차가 RUNNING 이고 아무것도 저장하지 않은 입찰 건 B 의 회차',
     steps='1. POST /mcp 에 tools/call 본문(name save_bid_analysis, A 회차, part REPORT, 꼴이 맞는 본문)을 보낸다\n'
           '2. 같은 도구에 B 회차, part CAPTURE_PLAN 을 보낸다\n'
           '3. 같은 도구에 B 회차, part REPORT 이고 eligibility 첫 줄 evidence 에 confidence 가 없는 본문을 보낸다\n'
           '4. POST /mcp 에 tools/call 본문(name search_award_history, 인자 없음)을 보낸다',
     expect='1. 1단계 결과가 오류이고 A 회차의 report_json 이 부르기 전과 같다\n'
            '2. 2단계 결과가 오류이고 B 회차의 capture_plan_json 이 비어 있다\n'
            '3. 3단계 결과가 오류이고 틀린 곳 eligibility[0].evidence.confidence 가 담겨 있으며 B 회차의 report_json 이 비어 있다\n'
            '4. 4단계 결과가 오류다',
     judge=['서버', '서버', '서버', '서버'],
     req=['BID-DOC-006'], fc=['BID-DOC-006'], screen='', pri='High',
     probe={1: dict(steps=[('key', '10.b069_a'), ('sql', analysis_rows()), save_analysis('REPORT', REPORT_BODY), ('sql', analysis_rows())],
                    check=f"{mcp_error(2)} and r[1][-1]['report_md5'] is not None and r[3][-1]['report_md5'] == r[1][-1]['report_md5']"),
            2: dict(steps=[('key', '10.b069_b'), save_analysis('CAPTURE_PLAN', CAPTURE_BODY), ('sql', analysis_rows())],
                    check=f"{mcp_error(1)} and int(r[2][-1]['has_plan']) == 0"),
            3: dict(steps=[('key', '10.b069_b'), save_analysis('REPORT', BAD_REPORT_BODY), ('sql', analysis_rows())],
                    check=f"{mcp_error(1)} and 'eligibility[0].evidence.confidence' in {mcp_out(1)} and int(r[2][-1]['has_report']) == 0"),
            4: dict(steps=[mcp('search_award_history', {})],
                    check=mcp_error(0))}),

dict(id='IT-10-070', flow='입찰 분석', kind='예외', title='관리자가 아니면 입찰 분석을 보거나 다시 분석하거나 결정할 수 없고 고객 화면에 입찰 분석이 없다',
     pre='회원 계정 로그인 토큰, 그 회원의 입찰 분석이 분석 완료인 ANALYZING 입찰 건',
     steps='1. 회원 토큰으로 GET /admin/bid-requests/{id}/bid-analysis 를 부른다\n'
           '2. 회원 토큰으로 POST /admin/bid-requests/{id}/bid-analysis/run 을 부른다\n'
           '3. 회원 토큰으로 POST /admin/bid-requests/{id}/bid-analysis/decision 에 GO 를 실어 부른다\n'
           '4. 회원 토큰으로 PATCH /bid-requests/{id}/state 에 CLOSED 를 실어 부른다\n'
           '5. 회원 토큰으로 GET /bid-requests/{id} 를 부른다\n'
           '6. 고객 화면에서 그 입찰 건의 입찰 상세를 연다',
     expect='1. 1단계 응답이 403 이다\n'
            '2. 2단계 응답이 403 이고 bid_analyses 의 그 입찰 건 줄 수가 부르기 전과 같다\n'
            '3. 3단계 응답이 403 이고 admin_decision 이 부르기 전과 같다\n'
            '4. 4단계 응답이 403 이고 코드가 CUSTOMER_TRANSITION_NOT_ALLOWED 이며 state 가 ANALYZING 그대로다\n'
            '5. 5단계 응답에 Pwin, 판정, 관리자 결정 칸이 없다\n'
            '6. 6단계 입찰 상세에 입찰 분석, Pwin, 판정, Bid/No-Bid 결정이 어디에도 없다',
     judge=['서버', '서버', '서버', '서버', '서버', '가상'],
     req=['BID-DOC-006'], fc=['BID-DOC-006', 'BID-DOC-007'], screen='고객 /bid-requests/{id}', pri='High',
     probe={1: dict(steps=[('key', '10.b070_bid'), ('api', USER, 'GET', ANALYSIS)],
                    check="r[1]['status'] == 403"),
            2: dict(steps=[('key', '10.b070_bid'), ('sql', analysis_rows()), ('api', USER, 'POST', RERUN), ('sql', analysis_rows())],
                    check="r[2]['status'] == 403 and len(r[3]) == len(r[1])"),
            3: dict(steps=[('key', '10.b070_bid'), ('sql', analysis_rows()),
                           ('api', USER, 'POST', DECISION, {'decision': 'GO', 'note': None}), ('sql', analysis_rows())],
                    check="r[2]['status'] == 403 and [x['admin_decision'] for x in r[3]] == [x['admin_decision'] for x in r[1]]"),
            4: dict(steps=[('key', '10.b070_bid'),
                           ('api', USER, 'PATCH', '/api/bid-requests/{val:0:id}/state', {'toState': 'CLOSED', 'notes': 'IT-10-070 서버 판정'}),
                           ('sql', f"SELECT state FROM bid_requests WHERE id = {THIS_BID}")],
                    check="r[1]['status'] == 403 and r[1]['body'].get('error') == 'CUSTOMER_TRANSITION_NOT_ALLOWED' and r[2][0]['state'] == 'ANALYZING'"),
            5: dict(steps=[('key', '10.b070_bid'), ('api', USER, 'GET', '/api/bid-requests/{val:0:id}')],
                    check="r[1]['status'] == 200 and all(k not in str(r[1]['body']) for k in "
                          "('pwin', 'Pwin', 'aiVerdict', 'verdict', 'adminDecision', 'bidAnalysis', 'decidedBy'))")}),

dict(id='IT-10-071', flow='Bid/No-Bid 결정', kind='정상', title='Go 로 정하면 정한 사람과 시각이 남고 문서 작성 중으로 전환된다',
     pre='관리자 계정, 입찰 분석이 분석 완료이고 관리자 결정이 없는 ANALYZING 입찰 건(읽는 중인 서류 없음), Aimbase 제안서 워크플로우가 도는 상태',
     steps='1. 관리자 화면 [입찰 요청 관리] 에서 그 입찰 건의 입찰 상세 관리를 연다\n'
           '2. 상태 패널의 GENERATING 전환을 본다\n'
           '3. 입찰 분석 영역 관리자 결정에서 Go 를 선택하고 [저장] 을 누른다\n'
           '4. 상태 패널에서 GENERATING 으로 전환한다\n'
           '5. 화면을 새로고침한다',
     expect='1. 2단계에서 GENERATING 전환이 막혀 있고 "Bid/No-Bid 결정을 Go 나 조건부 Go 로 정해야 문서 작성으로 넘어갑니다" 가 보인다\n'
            '2. 3단계 뒤 머리에 관리자 결정 Go, 정한 관리자 이름, 저장 시각이 보이고 GENERATING 전환이 풀린다\n'
            '3. bid_analyses 지금 회차의 admin_decision 이 GO, decided_by 가 그 관리자, decided_at 이 저장 시각이다\n'
            '4. 4단계 뒤 state 가 GENERATING 이고 state_history 에 ANALYZING 에서 GENERATING 으로의 줄이 더해진다\n'
            '5. 5단계 뒤 입찰 분석 영역에 관리자 결정 Go 와 정한 관리자 이름이 그대로 보이고 관리자 결정 폼과 [다시 분석] 이 없다\n'
            '6. 3단계 결정 저장으로 notification_logs 에 줄이 생기지 않는다',
     judge=['합동', '합동', '서버', '서버', '가상', '서버'],
     req=['BID-DOC-007'], fc=['BID-DOC-007'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={3: dict(steps=[('key', '10.b071_server_bid'),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'GO', 'note': None}),
                           ('sql', analysis_rows()),
                           ('sql', ADMIN_ID)],
                    check="r[1]['status'] == 200 and r[2][-1]['admin_decision'] == 'GO' and r[2][-1]['decided_by'] == r[3][0]['hex'] "
                          "and r[2][-1]['decided_at'][:19] == r[1]['body']['analysis']['decidedAt'].replace('T', ' ')[:19]"),
            4: dict(steps=[('key', '10.b071_server_bid'),
                           ('sql', analysis_rows()),
                           ('sql', LAST_STEP),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-071 서버 판정'}),
                           ('sql', LAST_STEP)],
                    need="len(r[1]) > 0 and r[1][-1]['admin_decision'] == 'GO' and r[2][0]['state'] == 'ANALYZING'",
                    check="r[3]['status'] == 200 and r[4][0]['state'] == 'GENERATING' and int(r[4][0]['hist']) == int(r[2][0]['hist']) + 1 "
                          "and r[4][0]['from_state'] == 'ANALYZING' and r[4][0]['to_state'] == 'GENERATING'"),
            6: dict(steps=[('key', '10.b071_server6_bid'),
                           ('sql', notification_count('email')),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'GO', 'note': None}),
                           wait(15),
                           ('sql', notification_count('email'))],
                    check="r[2]['status'] == 200 and r[4][0]['n'] == r[1][0]['n']")}),

dict(id='IT-10-072', flow='Bid/No-Bid 결정', kind='정상', title='조건부 Go 로 조건을 기재해 정하면 문서 작성 중으로 전환된다',
     pre='관리자 계정, 입찰 분석이 분석 완료이고 관리자 결정이 없는 ANALYZING 입찰 건(읽는 중인 서류 없음)',
     steps='1. 관리자 결정에서 조건부 Go 를 선택하고 조건 "참여 인력 이력서를 고객에게 받는 조건" 을 기재해 [저장] 을 누른다\n'
           '2. 상태 패널에서 GENERATING 으로 전환한다',
     expect='1. 1단계 뒤 머리에 관리자 결정 조건부 Go 와 조건 "참여 인력 이력서를 고객에게 받는 조건" 이 보인다\n'
            '2. 지금 회차의 admin_decision 이 CONDITIONAL_GO 이고 admin_note 가 기재한 조건이다\n'
            '3. 2단계 전환이 거부되지 않고 state 가 GENERATING 이 된다',
     judge=['가상', '서버', '서버'],
     req=['BID-DOC-007'], fc=['BID-DOC-007'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={2: dict(steps=[('key', '10.b072_server_bid'),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'CONDITIONAL_GO', 'note': '참여 인력 이력서를 고객에게 받는 조건'}),
                           ('sql', analysis_rows())],
                    check="r[1]['status'] == 200 and r[2][-1]['admin_decision'] == 'CONDITIONAL_GO' "
                          "and r[2][-1]['admin_note'] == '참여 인력 이력서를 고객에게 받는 조건'"),
            3: dict(steps=[('key', '10.b072_server_bid'),
                           ('sql', analysis_rows()),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-072 서버 판정'}),
                           ('sql', f"SELECT state FROM bid_requests WHERE id = {THIS_BID}")],
                    need="len(r[1]) > 0 and r[1][-1]['admin_decision'] == 'CONDITIONAL_GO'",
                    check="r[2]['status'] == 200 and r[3][0]['state'] == 'GENERATING'")}),

dict(id='IT-10-073', flow='Bid/No-Bid 결정', kind='정상', title='No-Go 로 정하면 문서 작성 중은 막히고 종료해도 고객에게 알리지 않는다',
     pre='관리자 계정, 입찰 분석이 분석 완료인 ANALYZING 입찰 건, 그 고객의 메일 주소',
     steps='1. 관리자 결정에서 No-Go 를 선택하고 사유 "자격 미달" 을 기재해 [저장] 을 누른다\n'
           '2. 관리자 토큰으로 POST /admin/bid-requests/{id}/transition 에 GENERATING 을 실어 부른다\n'
           '3. 상태 패널에서 CLOSED 로 전환한다\n'
           '4. 입찰 상세 관리를 새로고침한다\n'
           '5. 관리자 토큰으로 GET /admin/bid-requests/{id}/document-readings 를 부른다',
     expect='1. 1단계 뒤 머리에 No-Go 와 사유 "자격 미달" 이 보이고 GENERATING 전환이 막혀 있다\n'
            '2. 2단계 응답이 409 이고 코드가 BID_DECISION_REQUIRED 다\n'
            '3. 3단계 뒤 state 가 CLOSED 이고 closed_at 이 채워지며 state_history 에 ANALYZING 에서 CLOSED 로의 줄이 더해진다\n'
            '4. 3단계 전환으로 notification_logs 에 그 고객 주소의 줄이 생기지 않는다\n'
            '5. 4단계에서 고객 서류 읽기 영역과 입찰 분석 영역이 그대로 보이고 [다시 읽기], [다시 분석], 관리자 결정 폼이 없다\n'
            '6. 5단계 응답의 readingStarted 가 true 다',
     judge=['가상', '서버', '서버', '서버', '가상', '서버'],
     req=['BID-DOC-007'], fc=['BID-DOC-007', 'BID-DOC-004'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={2: dict(steps=[('key', '10.b073_server2_bid'),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'NO_GO', 'note': '자격 미달'}),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-073 서버 판정'})],
                    check="r[1]['status'] == 200 and " + refused(2, 'BID_DECISION_REQUIRED')),
            3: dict(steps=[('key', '10.b073_server3_bid'),
                           ('sql', LAST_STEP),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'NO_GO', 'note': '자격 미달'}),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'CLOSED', 'notes': 'IT-10-073 서버 판정'}),
                           ('sql', CLOSE_STEP)],
                    need="r[1][0]['state'] == 'ANALYZING'",
                    check="r[2]['status'] == 200 and r[3]['status'] == 200 and r[4][0]['state'] == 'CLOSED' and r[4][0]['closed_at'] is not None "
                          "and int(r[4][0]['hist']) == int(r[1][0]['hist']) + 1 and r[4][0]['from_state'] == 'ANALYZING' "
                          "and r[4][0]['to_state'] == 'CLOSED'"),
            4: dict(steps=[('key', '10.b073_server4_bid'),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'NO_GO', 'note': '자격 미달'}),
                           ('sql', "SELECT COUNT(*) AS n FROM notification_logs WHERE recipient_email = '{val:0:email}'"),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'CLOSED', 'notes': 'IT-10-073 서버 판정'}),
                           wait(15),
                           ('sql', "SELECT COUNT(*) AS n FROM notification_logs WHERE recipient_email = '{val:0:email}'")],
                    check="r[1]['status'] == 200 and r[3]['status'] == 200 and r[5][0]['n'] == r[2][0]['n']"),
            6: dict(steps=[('key', '10.b073_server3_bid'),
                           ('sql', f"SELECT state FROM bid_requests WHERE id = {THIS_BID}"),
                           ('api', ADMIN, 'GET', READINGS)],
                    need="r[1][0]['state'] == 'CLOSED'",
                    check="r[2]['status'] == 200 and r[2]['body']['readingStarted'] is True")}),

dict(id='IT-10-074', flow='Bid/No-Bid 결정', kind='정상', title='입찰 분석이 실패했거나 회차가 없는 건도 결정을 기록할 수 있다',
     pre='관리자 계정, 입찰 분석이 분석 실패인 ANALYZING 입찰 건 A, CR-170 배포 전에 분석 단계에 들어가 입찰 분석 회차가 없고 읽는 중인 서류도 없는 ANALYZING 입찰 건 B',
     steps='1. A 의 입찰 분석 영역 관리자 결정에서 Go 를 선택하고 [저장] 을 누른다\n'
           '2. B 의 입찰 상세 관리를 연다\n'
           '3. B 의 관리자 결정에서 Go 를 선택하고 [저장] 을 누른다',
     expect='1. 1단계 뒤 A 의 지금 회차 status 가 FAILED 그대로이고 admin_decision 이 GO 다\n'
            '2. 2단계에서 B 의 입찰 분석 영역에 "입찰 분석이 없습니다" 가 보이고 [다시 분석] 과 관리자 결정 [저장] 이 살아 있다\n'
            '3. 3단계 뒤 B 에 bid_analyses 줄이 1줄 생기고 status 가 DECISION_ONLY, admin_decision 이 GO 다\n'
            '4. 3단계 뒤 B 의 머리가 결정만이고 종합 분석 리포트, 캡처플랜, Bid/No-Bid 결정 탭이 없다',
     judge=['서버', '가상', '서버', '가상'],
     req=['BID-DOC-007'], fc=['BID-DOC-007'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={1: dict(steps=[('key', '10.b074_a_bid'),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'GO', 'note': None}),
                           ('sql', analysis_rows())],
                    check="r[1]['status'] == 200 and r[2][-1]['status'] == 'FAILED' and r[2][-1]['admin_decision'] == 'GO'"),
            3: dict(steps=[('key', '10.b074_b_bid'),
                           ('sql', analysis_rows()),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'GO', 'note': None}),
                           ('sql', analysis_rows())],
                    need="len(r[1]) == 0",
                    check="r[2]['status'] == 200 and len(r[3]) == 1 and r[3][0]['status'] == 'DECISION_ONLY' and r[3][0]['admin_decision'] == 'GO'")}),

dict(id='IT-10-075', flow='Bid/No-Bid 결정', kind='정상', title='결정을 다시 정하면 덮어쓰고 정한 시각이 바뀐다',
     pre='관리자 계정, 관리자 결정이 Go 인 ANALYZING 입찰 건(분석 중 아님)',
     steps='1. 관리자 결정에서 No-Go 를 선택하고 사유 "가격 경쟁력 부족" 을 기재해 [저장] 을 누른다\n'
           '2. 관리자 토큰으로 GET /admin/bid-requests/{id}/bid-analysis 를 부른다',
     expect='1. 1단계 뒤 머리의 관리자 결정이 No-Go 와 사유 "가격 경쟁력 부족" 으로 바뀐다\n'
            '2. 2단계 응답의 adminDecision 이 NO_GO, adminNote 가 "가격 경쟁력 부족" 이고 decidedAt 이 Go 로 정한 시각보다 뒤다\n'
            '3. 1단계 뒤 bid_analyses 의 그 입찰 건 줄 수가 1단계 전과 같다',
     judge=['가상', '서버', '서버'],
     req=['BID-DOC-007'], fc=['BID-DOC-007'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={2: dict(steps=[('key', '10.b075_server2_bid'),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'NO_GO', 'note': '가격 경쟁력 부족'}),
                           ('api', ADMIN, 'GET', ANALYSIS)],
                    check="r[1]['status'] == 200 and r[2]['body']['analysis']['adminDecision'] == 'NO_GO' "
                          "and r[2]['body']['analysis']['adminNote'] == '가격 경쟁력 부족' "
                          "and r[2]['body']['analysis']['decidedAt'].replace('T', ' ') > r[0][0]['go_at']"),
            3: dict(steps=[('key', '10.b075_server3_bid'),
                           ('sql', analysis_rows()),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'NO_GO', 'note': '가격 경쟁력 부족'}),
                           ('sql', analysis_rows())],
                    check="r[2]['status'] == 200 and len(r[3]) == len(r[1])")}),

dict(id='IT-10-076', flow='Bid/No-Bid 결정', kind='예외', title='결정을 선택하지 않거나 조건부 Go 의 조건, No-Go 의 사유가 비면 결정이 저장되지 않는다',
     pre='관리자 계정, 입찰 분석이 분석 완료이고 관리자 결정이 없는 ANALYZING 입찰 건',
     steps='1. 관리자 결정에서 아무것도 선택하지 않고 [저장] 을 누른다\n'
           '2. 조건부 Go 를 선택하고 조건을 비운 채 [저장] 을 누른다\n'
           '3. No-Go 를 선택하고 사유를 비운 채 [저장] 을 누른다\n'
           '4. 관리자 토큰으로 POST /admin/bid-requests/{id}/bid-analysis/decision 에 CONDITIONAL_GO 와 빈 note 를 실어 부른다\n'
           '5. 같은 요청에 NO_GO 와 빈 note 를 실어 부른다',
     expect='1. 1단계에서 "결정을 선택하세요" 가 보이고 요청이 가지 않는다\n'
            '2. 2단계에서 저장 버튼 아래 "조건을 기재하세요" 가 보이고 요청이 가지 않는다\n'
            '3. 3단계에서 "사유를 기재하세요" 가 보이고 요청이 가지 않는다\n'
            '4. 4단계 응답이 400 이고 코드가 DECISION_NOTE_REQUIRED 다\n'
            '5. 5단계 응답이 400 이고 코드가 DECISION_NOTE_REQUIRED 이며 admin_decision 이 빈 값 그대로다',
     judge=['가상', '가상', '가상', '서버', '서버'],
     req=['BID-DOC-007'], fc=['BID-DOC-007'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={4: dict(steps=[('key', '10.b076_server_bid'),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'CONDITIONAL_GO', 'note': ''})],
                    check="r[1]['status'] == 400 and r[1]['body'].get('error') == 'DECISION_NOTE_REQUIRED'"),
            5: dict(steps=[('key', '10.b076_server_bid'),
                           ('api', ADMIN, 'POST', DECISION, {'decision': 'NO_GO', 'note': ''}),
                           ('sql', analysis_rows())],
                    check="r[1]['status'] == 400 and r[1]['body'].get('error') == 'DECISION_NOTE_REQUIRED' "
                          "and len(r[2]) > 0 and r[2][-1]['admin_decision'] is None")}),

dict(id='IT-10-077', flow='Bid/No-Bid 결정', kind='예외', title='관리자 결정이 없으면 문서 작성 중으로 전환되지 않는다',
     pre='관리자 계정, 입찰 분석이 분석 완료이고 관리자 결정이 없는 ANALYZING 입찰 건(읽는 중인 서류 없음)',
     steps='1. 그 입찰 건의 입찰 상세 관리에서 상태 패널의 GENERATING 전환을 본다\n'
           '2. 관리자 토큰으로 POST /admin/bid-requests/{id}/transition 에 GENERATING 을 실어 부른다\n'
           '3. 그 입찰 건의 state 와 state_history 를 조회한다',
     expect='1. 1단계에서 GENERATING 전환이 막혀 있고 "Bid/No-Bid 결정을 Go 나 조건부 Go 로 정해야 문서 작성으로 넘어갑니다" 가 보인다\n'
            '2. 2단계 응답이 409 이고 코드가 BID_DECISION_REQUIRED 다\n'
            '3. 3단계에서 state 가 ANALYZING 그대로이고 state_history 줄 수가 늘지 않았다',
     judge=['가상', '서버', '서버'],
     req=['BID-DOC-007'], fc=['BID-DOC-007'], screen='관리자 /bid-requests/{id}', pri='High',
     probe={2: dict(steps=[('key', '10.b077_server_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-077 서버 판정'})],
                    check=refused(1, 'BID_DECISION_REQUIRED')),
            3: dict(steps=[('key', '10.b077_server_bid'),
                           ('api', ADMIN, 'POST', TRANSITION, {'toState': 'GENERATING', 'notes': 'IT-10-077 서버 판정'}),
                           ('sql', LAST_STEP)],
                    check="r[2][0]['state'] == 'ANALYZING' and int(r[2][0]['hist']) == int(r[0][0]['hist'])")}),

dict(id='IT-10-078', flow='제안서 자동 작성', kind='정상', title='AI 문서 작성에서 커버레터는 1쪽 제출 편지로 쓰고 요구사항은 기술 제안서에만 걸린다',
     pre='관리자 계정, IT-10-001 로 문서 작성이 끝나 REVIEW 가 된 입찰 건(매트릭스 대상 줄이 있고 문서 구성이 없는 옛 공고, 평가 항목 구조에 문서 종류와 커버레터 요구 여부가 없음)',
     steps='1. 관리자 토큰으로 GET /admin/bid-documents/{커버레터 문서 id}/chapters 를 부르고, 그 입찰 건의 입찰 문서(bid_documents) 행을 조회한다\n'
           '2. 그 입찰 건의 요구사항 충족 기록(requirement_fulfillment_maps)을 조회한다\n'
           '3. 관리자 토큰으로 GET /admin/bid-documents/{기술 제안서 문서 id}/coverage 를 부른다\n'
           '4. 관리자 화면 문서 편집에서 커버레터 본문을 연다',
     expect='1. 커버레터의 장과 절이 장 1개, 절 1개이고 그 절의 requirementRefs 가 비어 있다\n'
            '2. 충족 기록 가운데 커버레터 문서의 것이 0건이다\n'
            '3. 기술 제안서 커버리지의 targetCount 가 그 공고 매트릭스의 대상(Shall, 플랫폼 생성) 줄 수와 같다\n'
            '4. 기술 제안서 커버리지의 missing 가운데 unmapped 가 true 인 줄이 0건이다\n'
            '5. 커버레터 본문이 1쪽을 넘지 않는 제출 편지이고 평가 항목별 장이 없다\n'
            '6. 그 입찰 건의 입찰 문서가 커버레터와 기술 제안서 2건이다',
     judge=['서버', '서버', '서버', '서버', '합동', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='관리자 /bid-requests/{id}/documents/{docId}', pri='High'),

dict(id='IT-10-079', flow='제안서 설계와 REQ', kind='정상', title='커버레터의 제안서 설계 입력에는 평가 항목과 매트릭스가 없고 기술 제안서에는 있다',
     pre='관리자 계정과 MCP 열쇠, 평가 항목 트리와 매트릭스 줄이 있는 공고의 입찰 건에 커버레터 문서와 기술 제안서 문서가 1건씩',
     steps='1. MCP get_proposal_design_input 을 커버레터 문서 id 로 부른다\n2. MCP get_proposal_design_input 을 기술 제안서 문서 id 로 부른다',
     expect='1. 1단계 응답의 evaluatedDocument 가 false 이고 factorTree, priceItems 가 null, complianceMatrix 가 빈 배열이다\n'
            '2. 2단계 응답의 evaluatedDocument 가 true 이고 complianceMatrix 줄 수가 그 공고 매트릭스 줄 수와 같다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='', pri='High'),

dict(id='IT-10-080', flow='제안서 설계와 REQ', kind='예외', title='커버레터 설계에 대응 REQ ID 가 있으면 전체가 저장되지 않는다',
     pre='관리자 계정과 MCP 열쇠, 매트릭스 줄 REQ-001 부터 REQ-003 이 있는 공고의 커버레터 문서 1건',
     steps='1. 장과 절 트리를 조회해 적어 둔다\n2. MCP save_proposal_structure 를 그 커버레터 문서 id 와 장 1개, 절 1개(requirementRefs 에 REQ-001)로 부른다\n3. 장과 절 트리를 다시 조회한다',
     expect='1. 저장이 거부되고 오류 문구에 "평가받지 않는 문서" 와 REQ-001 이 들어 있다\n'
            '2. 3단계의 장과 절 트리가 1단계와 같다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='', pri='High'),

dict(id='IT-10-081', flow='제안서 설계와 REQ', kind='정상', title='커버레터 설계는 대응 REQ ID 없이 저장되고 어느 절에도 없는 REQ 를 돌려주지 않는다',
     pre='관리자 계정과 MCP 열쇠, 매트릭스 줄 REQ-001 부터 REQ-003 이 있는 공고의 커버레터 문서 1건',
     steps='1. MCP save_proposal_structure 를 그 커버레터 문서 id 와 장 1개, 절 1개(requirementRefs 없음)로 부른다\n2. 장과 절 트리를 조회한다',
     expect='1. 저장이 받아들여지고 응답의 unassignedReqIds 가 빈 배열이다\n'
            '2. 장과 절 트리가 장 1개, 절 1개다',
     judge=['서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='', pri='High'),

dict(id='IT-10-082', flow='제안서 자동 작성', kind='정상', title='AI 문서 작성이 공고가 요구하는 문서 구성대로 문서를 만들고 문서마다 자기 평가 항목의 요구사항만 센다',
     pre='관리자 계정, IT-10-001 로 문서 작성이 끝나 REVIEW 가 된 입찰 건(IT-09-046 으로 문서 구성이 기술 제안서, 과거 수행 실적, 가격 요약이고 커버레터 요구가 없는 공고)',
     steps='1. 그 입찰 건의 입찰 문서(bid_documents) 행을 조회한다\n'
           '2. 관리자 토큰으로 문서마다 GET /admin/bid-documents/{documentId}/coverage 를 부른다\n'
           '3. 그 공고 매트릭스 줄(opportunity_requirement_items)의 평가 항목을 조회한다\n'
           '4. 관리자 화면 메뉴 [입찰 요청 관리] 에서 그 입찰 건 상세의 생성 문서 목록을 본다',
     expect='1. 입찰 문서가 기술 제안서, 과거 수행 실적, 가격 요약 3건이고 커버레터가 없다\n'
            '2. 세 문서 커버리지의 targetCount 합이 그 공고 매트릭스의 대상(Shall, 플랫폼 생성) 줄 수와 같다\n'
            '3. 과거 수행 실적 커버리지의 대상 줄(missing 과 sections 의 refs)이 모두 과거 실적 평가 항목의 줄이다\n'
            '4. 기술 제안서 커버리지의 대상 줄에 과거 실적, 가격 평가 항목의 줄이 없다\n'
            '5. 세 문서 모두 커버리지 missing 가운데 unmapped 가 true 인 줄이 0건이다\n'
            '6. 생성 문서 목록에 세 문서가 각각의 문서 유형 이름으로 보이고 커버레터 줄이 없다',
     judge=['서버', '서버', '서버', '서버', '서버', '가상'],
     req=['BID-DOC-001', 'BID-COMP-002'], fc=['BID-DOC-001', 'BID-COMP-002'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-10-083', flow='제안서 설계와 REQ', kind='정상', title='문서마다 제안서 설계 입력에 자기 평가 항목과 그 항목의 매트릭스 줄만 온다',
     pre='관리자 계정과 MCP 열쇠, 문서 구성이 기술 제안서(F1), 과거 수행 실적(F2), 가격 요약(F3)인 공고의 입찰 건에 세 문서가 1건씩, 공고에 평가 항목이 없는 매트릭스 줄과 가격 항목이 있음',
     steps='1. MCP get_proposal_design_input 을 기술 제안서, 과거 수행 실적, 가격 요약 문서 id 로 한 번씩 부른다',
     expect='1. 과거 수행 실적 응답의 factorTree.factors 가 F2 하나이고 complianceMatrix 줄이 모두 F2 줄이다\n'
            '2. 가격 요약 응답의 factorTree.factors 가 F3 하나이고 complianceMatrix 줄이 모두 F3 줄이다\n'
            '3. 기술 제안서 응답의 factorTree.factors 가 F1 하나이고 complianceMatrix 에 F1 줄과 평가 항목이 없는 줄이 있으며 F2, F3 줄이 없다\n'
            '4. 세 응답의 complianceMatrix 줄을 합치면 그 공고 매트릭스 줄 전부이고 겹치는 줄이 없다\n'
            '5. priceItems 가 가격 요약 응답에만 있고 기술 제안서와 과거 수행 실적 응답은 null 이다',
     judge=['서버', '서버', '서버', '서버', '서버'],
     req=['BID-DOC-001'], fc=['BID-DOC-001'], screen='', pri='High'),

dict(id='IT-10-084', flow='제안서 설계와 REQ', kind='예외', title='과거 수행 실적 설계에 다른 문서가 받는 REQ ID 가 있으면 전체가 저장되지 않는다',
     pre='관리자 계정과 MCP 열쇠, IT-10-083 과 같은 공고의 과거 수행 실적 문서 1건, F1 줄 REQ ID 하나와 F2 줄 REQ ID 하나를 앎',
     steps='1. 장과 절 트리를 조회해 적어 둔다\n2. MCP save_proposal_structure 를 그 과거 수행 실적 문서 id 와 장 1개, 절 1개(requirementRefs 에 F2 줄 REQ ID 와 F1 줄 REQ ID)로 부른다\n3. 장과 절 트리를 다시 조회한다',
     expect='1. 저장이 거부되고 오류 문구에 F1 줄 REQ ID 가 들어 있다\n'
            '2. 오류 문구에 F2 줄 REQ ID 가 들어 있지 않다\n'
            '3. 3단계의 장과 절 트리가 1단계와 같다',
     judge=['서버', '서버', '서버'],
     req=['BID-DOC-001', 'BID-COMP-002'], fc=['BID-DOC-001', 'BID-COMP-002'], screen='', pri='High'),

dict(id='IT-10-085', flow='제안서 설계와 REQ', kind='정상', title='과거 수행 실적 설계는 자기 줄만 다 걸면 어느 절에도 없는 REQ 를 돌려주지 않는다',
     pre='관리자 계정과 MCP 열쇠, IT-10-083 과 같은 공고의 과거 수행 실적 문서 1건, F2 대상(Shall, 플랫폼 생성) 줄 REQ ID 전부를 앎',
     steps='1. MCP save_proposal_structure 를 그 과거 수행 실적 문서 id 와 장 1개, 절 1개(requirementRefs 에 F2 대상 줄 REQ ID 전부)로 부른다\n2. 관리자 토큰으로 GET /admin/bid-documents/{documentId}/coverage 를 부른다',
     expect='1. 저장이 받아들여지고 응답의 unassignedReqIds 가 빈 배열이다\n'
            '2. 커버리지의 targetCount 가 F2 대상 줄 수와 같다',
     judge=['서버', '서버'],
     req=['BID-DOC-001', 'BID-COMP-002'], fc=['BID-DOC-001', 'BID-COMP-002'], screen='', pri='High'),

dict(id='IT-10-086', flow='입찰 분석', kind='정상', title='PDF 다운로드를 누르면 지금 회차의 머리와 네 부분이 한 문서로 이어진 인쇄 창이 열리고 PDF 로 저장된다',
     pre='관리자 계정, 입찰 분석이 분석 완료이고 관리자 결정이 조건부 Go 로 저장된 ANALYZING 입찰 건(확인 필요가 고객에게 물을 것과 관리자가 볼 것에 각 1건 이상, REQ ID 가 셋 이상인 요구 항목 칸이 있음), 크롬 브라우저',
     steps='1. 관리자 화면에서 그 입찰 건의 입찰 상세 관리를 열고 입찰 분석 영역의 캡처플랜 탭을 연다\n'
           '2. 입찰 분석 영역 머리의 [PDF 다운로드] 를 누른다\n'
           '3. 인쇄 창 미리보기를 끝까지 넘겨 본다\n'
           '4. 대상을 PDF 로 저장으로 고르고 저장한 뒤 받은 파일을 연다\n'
           '5. 인쇄 창이 닫힌 뒤 화면을 본다',
     expect='1. 2단계 뒤 브라우저 인쇄 창이 열린다\n'
            '2. 2단계에서 입찰 분석 조회(/bid-analysis)가 새로 나가지 않는다\n'
            '3. 미리보기 머리에 「입찰 분석」, 그 회차 종합 분석 리포트의 사업명, 상태 분석 완료와 시작, 끝 시각, Pwin 과 판정과 이유, 관리자 결정 조건부 Go 와 정한 사람, 시각, 조건이 있다\n'
            '4. 머리 다음에 종합 분석 리포트, 캡처플랜, Bid/No-Bid 결정, 확인 필요가 이 순서로 있다\n'
            '5. 캡처플랜, Bid/No-Bid 결정, 확인 필요 부분이 저마다 새 쪽 맨 위에서 시작한다\n'
            '6. 종합 분석 리포트 부분의 사업명, 발주처, 발주 이력 표 줄 수가 화면의 종합 분석 리포트 탭과 같다\n'
            '7. 캡처플랜 부분에 양식 1장부터 5장까지가 차례로 있다\n'
            '8. Bid/No-Bid 결정 부분의 스코어카드 팩터 줄 수와 가중 점수가 화면의 Bid/No-Bid 결정 탭과 같다\n'
            '9. 요구 항목 칸마다 REQ ID 와 한글 요구 문장이 전부 있고 "건 더 보기" 버튼은 하나도 없다\n'
            '10. 스코어카드 팩터마다 그 아래에 업체별 점수와 근거가 펼쳐져 있다\n'
            '11. 확인 필요 부분에 고객에게 물을 것 묶음과 관리자가 볼 것 묶음이 차례로 있고 묶음별 건수가 화면 확인 필요 탭 이름의 건수와 같다\n'
            '12. 미리보기 어느 쪽에도 왼쪽 메뉴, 머리 막대, 고객 서류 읽기 영역, [다시 분석], [PDF 다운로드], 관리자 결정 폼이 없다\n'
            '13. 인쇄 창의 파일 이름 기본값이 입찰분석_사업명_회차시작일(YYYYMMDD 여덟 자리)이다\n'
            '14. 받은 PDF 파일을 열면 한글이 네모나 물음표 없이 화면과 같은 글자로 보인다\n'
            '15. 받은 PDF 파일의 확신도 꼬리표와 스코어카드 점수 칸이 화면과 같은 색으로 찍혀 있다\n'
            '16. 받은 PDF 파일에서 표의 한 줄이나 카드 하나가 두 쪽에 나뉘어 찍힌 곳이 없다\n'
            '17. 5단계에서 입찰 분석 영역에 캡처플랜 탭이 열려 있다\n'
            '18. 5단계에서 브라우저 탭 제목이 2단계 전과 같다',
     judge=['가상', '가상', '가상', '가상', '합동', '가상', '가상', '가상', '가상', '가상', '가상', '가상', '가상', '합동', '합동', '합동', '가상', '가상'],
     req=['BID-DOC-006'], fc=['BID-DOC-006'], screen='관리자 /bid-requests/{id}', pri='High'),

dict(id='IT-10-087', flow='입찰 분석', kind='예외', title='결정만이거나 회차가 없는 건에는 PDF 다운로드가 없고 분석 중인 건은 저장된 부분만 담긴다',
     pre='관리자 계정, 입찰 분석이 결정만인 입찰 건 A, 입찰 분석 회차가 없는 ANALYZING 입찰 건 C, 입찰 분석이 분석 중이고 종합 분석 리포트만 저장된 입찰 건 B',
     steps='1. A 의 입찰 상세 관리에서 입찰 분석 영역 머리를 본다\n'
           '2. C 의 입찰 상세 관리에서 입찰 분석 영역 머리를 본다\n'
           '3. B 의 입찰 상세 관리에서 종합 분석 리포트 탭을 연 채 [PDF 다운로드] 를 누르고 인쇄 창 미리보기를 본다\n'
           '4. 인쇄 창을 취소하고 20초 동안 화면을 둔다',
     expect='1. 1단계에서 A 의 입찰 분석 영역 머리에 [PDF 다운로드] 가 없다\n'
            '2. 2단계에서 C 의 입찰 분석 영역 머리에 [PDF 다운로드] 가 없다\n'
            '3. 3단계 미리보기의 종합 분석 리포트 부분에 B 의 리포트 사업명과 발주처가 있다\n'
            '4. 3단계 미리보기의 캡처플랜 부분과 Bid/No-Bid 결정 부분에 저마다 "아직 저장되지 않았습니다" 가 있다\n'
            '5. 4단계 뒤 입찰 분석 영역에 종합 분석 리포트 탭이 열려 있다\n'
            '6. 4단계 20초 동안 입찰 분석 조회(/bid-analysis)가 10초 간격으로 두 번 이상 나간다',
     judge=['가상', '가상', '가상', '가상', '가상', '가상'],
     req=['BID-DOC-006'], fc=['BID-DOC-006'], screen='관리자 /bid-requests/{id}', pri='Medium'),

]
