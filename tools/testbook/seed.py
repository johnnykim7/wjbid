"""시험 재료(시딩)를 대상 DB 에 넣고 지운다.

상태를 바꾸는 시험(전환, 다시 읽기, 제안서 설계 저장)은 실제 고객 건에 걸 수 없고, 한 번 돌면 재료가 다음 상태로
넘어가 다음 회차에 다시 쓸 수 없다. 그래서 회차마다 이 도구로 시험 전용 회원과 입찰 건을 처음 상태로 다시 넣는다.

파일 하나가 시험 장면 하나다. seed/NN-M_이름.sql (NN 영역, M 순번)

    -- @marker SEED-10-1
    -- 대상 IT-10-041 IT-10-054
    -- @key 10.b041_bid SELECT '[[uuid:03:1]]' AS id, ... FROM bid_requests WHERE id = [[id:03:1]]
    INSERT INTO ... ;

자리 글자는 파일 번호로 풀린다. 번호가 파일마다 달라 장면끼리 섞이지 않고, 지울 때 그 번호로만 지운다.
    [[id:TT:S]]    시딩 행 열쇠 UNHEX(...). TT 는 표 구분(01 회원 02 공고 03 입찰 건 04 공고별 서류 05 서류함 서류
                   06 읽기 07 입찰 문서 08 장 09 절 0a 요구 항목 0b 충족 매핑 0c 입찰 분석 회차 0d 미 연방 계약), S 는 순번
    [[uuid:TT:S]]  같은 열쇠의 API 주소 꼴 글자
    [[sha:S]]      서류 내용 해시. 같은 내용을 두 서류에 주면 같은 번호를 쓴다
    [[email:이름]] 시딩 회원 메일
    [[notice:n]]   시딩 공고 번호
    [[now]]        서울 시각 지금. 서버가 상태 시각을 서울 시각으로 적어 DB 시간대와 상관없이 맞춘다

    python3 tools/testbook/seed.py check  --target 로컬
    python3 tools/testbook/seed.py apply  --target 로컬 [--file 10-1 ...] [--item IT-10-041]
    python3 tools/testbook/seed.py revert --target 로컬 [--file 10-1 ...]
    python3 tools/testbook/seed.py which IT-10-041

쓰기 DB 명령은 TESTBOOK_SEED_DB_CMD 다(SQL 을 표준입력으로 받아 머리행 있는 탭 구분 결과를 내는 명령).
서버 판정의 TESTBOOK_DB_CMD 는 읽기만 하므로 따로 받는다. apply 는 지우고 넣으므로 다시 넣으면 처음 상태가 된다.
넣은 뒤 키 값을 seed/ids/<대상>.json 에 쓴다. 화면 시험이 그 파일로 시딩 건을 찾는다.

서류를 실제로 읽어야 하는 장면은 저장소에 파일도 놓는다. 시딩 파일에 이렇게 적는다.
    -- @file seed/10-7/1.pdf past-performance.pdf
앞은 서류 행 storage_url 과 같은 저장소 안 경로, 뒤는 seed/files/ 의 재료 파일이다. 놓는 명령은
TESTBOOK_SEED_FILE_CMD 다(저장소 안 경로를 첫 인자로, 파일 내용을 표준입력으로 받아 대상 저장소에 쓰는 명령).
명령이 없으면 놓지 않고 알리기만 한다. 그 서류는 읽기가 실패해 재료 없음이 된다. 지울 때 파일은 남긴다.
다음 apply 가 같은 경로에 덮어쓰고, 시딩 행이 없으면 아무 서류도 그 파일을 가리키지 않는다.
"""
import argparse
import json
import os
import re
import shlex
import subprocess
import sys

HERE = os.path.dirname(os.path.abspath(__file__))
SEED_DIR = os.path.join(HERE, 'seed')
FILES_DIR = os.path.join(SEED_DIR, 'files')
IDS_DIR = os.path.join(SEED_DIR, 'ids')
FILE_NAME = re.compile(r'^(\d{2})-(\d{1,2})_[^.]+\.sql$')
TOKEN = re.compile(r'\[\[(id|uuid|sha|email|notice|now)(?::([0-9a-zA-Z_.-]+))?(?::(\d+))?\]\]')
KST_NOW = '(UTC_TIMESTAMP(6) + INTERVAL 9 HOUR)'


class SeedFile:
    def __init__(self, path):
        self.path = path
        m = FILE_NAME.match(os.path.basename(path))
        self.stem = f'{m.group(1)}-{m.group(2)}'
        self.code = f'{m.group(1)}{int(m.group(2)):02d}'
        self.marker = None
        self.items = []
        self.keys = {}
        self.files = []
        body = []
        with open(path, encoding='utf-8') as f:
            for line in f:
                s = line.strip()
                if s.startswith('-- @marker '):
                    self.marker = s.split(None, 2)[2]
                elif s.startswith('-- 대상 '):
                    self.items += s.split()[2:]
                elif s.startswith('-- @key '):
                    _, _, name, sql = s.split(None, 3)
                    self.keys[name] = self.expand(sql)
                elif s.startswith('-- @file '):
                    _, _, dest, src = s.split()
                    self.files.append((dest, src))
                elif not s.startswith('--'):
                    body.append(line)
        self.insert = self.expand(''.join(body)).strip()

    def hex(self, table, seq):
        return f'5EED00{table.upper():0>2}{self.code}00000000{int(seq):012d}'

    def expand(self, text):
        def one(m):
            kind, a, b = m.group(1), m.group(2), m.group(3)
            if kind == 'id':
                return f"UNHEX('{self.hex(a, b)}')"
            if kind == 'uuid':
                h = self.hex(a, b).lower()
                return f'{h[:8]}-{h[8:12]}-{h[12:16]}-{h[16:20]}-{h[20:]}'
            if kind == 'sha':
                return f'5eed{self.code}{int(a):04d}'.ljust(64, '0')
            if kind == 'email':
                return f'seed.{self.stem}.{a}@example.com'
            if kind == 'notice':
                return f'SEED-{self.stem}-{a}'
            return KST_NOW
        return TOKEN.sub(one, text)

    def cleanup(self):
        """시딩 회원, 시딩 공고, 시딩 해시에 딸린 행을 자식부터 지운다. 시험 중 서버가 그 입찰 건에 만든 행(제안서 장과 절,
        문서 버전, 검증 기록)도 함께 지워야 다시 넣을 수 있다"""
        member = f"m.email LIKE 'seed.{self.stem}.%@example.com'"
        opps = f"SELECT o.id FROM opportunities o WHERE o.notice_id LIKE 'SEED-{self.stem}-%'"
        # 시험 계정이 고객으로 보는 장면은 입찰 건 회원이 시딩 회원이 아니다. 시딩 공고에 건 입찰 건도 지운다
        bids = (f'SELECT br.id FROM bid_requests br JOIN members m ON m.id = br.member_id WHERE {member} '
                f'OR br.opportunity_id IN ({opps})')
        docs = f'SELECT bd.id FROM bid_documents bd WHERE bd.bid_request_id IN ({bids})'
        chapters = f'SELECT pc.id FROM proposal_chapter pc WHERE pc.document_id IN ({docs})'
        sections = f'SELECT ps.id FROM proposal_section ps WHERE ps.chapter_id IN ({chapters})'
        return [
            f'DELETE FROM proposal_block WHERE section_id IN ({sections})',
            f'DELETE FROM proposal_section WHERE chapter_id IN ({chapters})',
            f'DELETE FROM proposal_chapter WHERE document_id IN ({docs})',
            f'DELETE FROM proposal_attachment WHERE document_id IN ({docs})',
            f'DELETE FROM bid_document_versions WHERE document_id IN ({docs})',
            f'DELETE FROM requirement_fulfillment_maps WHERE bid_request_id IN ({bids})',
            f'DELETE FROM bid_documents WHERE bid_request_id IN ({bids})',
            f'DELETE FROM bid_authorizations WHERE bid_request_id IN ({bids})',
            f'DELETE FROM bid_packages WHERE bid_request_id IN ({bids})',
            f'DELETE FROM submission_attempts WHERE bid_request_id IN ({bids})',
            f'DELETE FROM validation_runs WHERE bid_request_id IN ({bids})',
            f'DELETE FROM bid_analyses WHERE bid_request_id IN ({bids})',
            f'DELETE FROM client_documents WHERE member_id IN (SELECT m.id FROM members m WHERE {member}) OR bid_request_id IN ({bids})',
            f'DELETE FROM member_documents WHERE member_id IN (SELECT m.id FROM members m WHERE {member})',
            f'DELETE FROM bid_requests WHERE member_id IN (SELECT m.id FROM members m WHERE {member})',
            f'DELETE FROM bid_requests WHERE opportunity_id IN ({opps})',
            f"DELETE FROM document_readings WHERE content_sha256 LIKE '5eed{self.code}%'",
            f'DELETE FROM opportunity_requirement_items WHERE opportunity_id IN ({opps})',
            f'DELETE FROM notices WHERE opportunity_id IN ({opps})',
            f"DELETE FROM opportunities WHERE notice_id LIKE 'SEED-{self.stem}-%'",
            f"DELETE FROM federal_contracts WHERE award_key LIKE 'SEED-{self.stem}-%'",
            f'DELETE m FROM members m WHERE {member}',
        ]


def load(stem=None):
    files = []
    for name in sorted(os.listdir(SEED_DIR)) if os.path.isdir(SEED_DIR) else []:
        if FILE_NAME.match(name):
            sf = SeedFile(os.path.join(SEED_DIR, name))
            if not stem or sf.stem in stem:
                files.append(sf)
    return files


def all_keys():
    """run_server 의 key 단계가 읽는다. 키 이름 -> (SELECT 문, 파일)"""
    out = {}
    for sf in load():
        for name, sql in sf.keys.items():
            if name in out:
                raise ValueError(f'키 {name} 가 {out[name][1]} 와 {sf.stem} 에 겹친다')
            out[name] = (sql, sf.stem)
    return out


def which(item):
    return [sf for sf in load() if item in sf.items]


def db(sql):
    cmd = os.environ.get('TESTBOOK_SEED_DB_CMD')
    if not cmd:
        raise SystemExit('TESTBOOK_SEED_DB_CMD 가 없다. 쓰기 DB 명령을 환경변수로 준다')
    p = subprocess.run(cmd, shell=True, input=sql, capture_output=True, text=True)
    if p.returncode != 0:
        raise SystemExit(f'DB 명령 실패: {p.stderr.strip()[:500]}')
    lines = [x for x in p.stdout.split('\n') if x]
    if not lines:
        return []
    head = lines[0].split('\t')
    return [dict(zip(head, [None if v == 'NULL' else v for v in row.split('\t')])) for row in lines[1:]]


def place_files(sf):
    cmd = os.environ.get('TESTBOOK_SEED_FILE_CMD')
    for dest, src in sf.files:
        if not cmd:
            print(f'{sf.stem} TESTBOOK_SEED_FILE_CMD 가 없어 {dest} 를 놓지 않음')
            continue
        with open(os.path.join(FILES_DIR, src), 'rb') as f:
            p = subprocess.run(f'{cmd} {shlex.quote(dest)}', shell=True, input=f.read(), capture_output=True)
        if p.returncode != 0:
            raise SystemExit(f'파일 놓기 실패 {dest}: {p.stderr.decode(errors="replace").strip()[:500]}')
        print(f'{sf.stem} {dest} 놓음')


def run_tx(statements):
    db('START TRANSACTION;\n' + ';\n'.join(statements) + ';\nCOMMIT;\n')


def key_values(files):
    out = {}
    for sf in files:
        for name, sql in sf.keys.items():
            rows = db(sql + ';\n')
            ids = [r.get('id') for r in rows if r.get('id')]
            out[name] = ids[0] if len(ids) == 1 else ids
    return out


def write_ids(target, values):
    os.makedirs(IDS_DIR, exist_ok=True)
    path = os.path.join(IDS_DIR, f'{target}.json')
    old = {}
    if os.path.exists(path):
        with open(path, encoding='utf-8') as f:
            old = json.load(f)
    old.update(values)
    with open(path, 'w', encoding='utf-8') as f:
        json.dump(old, f, ensure_ascii=False, indent=2, sort_keys=True)
        f.write('\n')
    return path


def pick(args):
    files = load(stem=args.file)
    if getattr(args, 'item', None):
        files = [sf for sf in files if args.item in sf.items]
    if not files:
        raise SystemExit('고른 시딩 파일이 없다')
    return files


def main():
    ap = argparse.ArgumentParser(description='시험 재료를 넣고 지운다')
    sub = ap.add_subparsers(dest='cmd', required=True)
    for name in ('check', 'apply', 'revert'):
        p = sub.add_parser(name)
        p.add_argument('--target', required=True, help='대상 서버 이름. 키 값 파일 이름이 된다')
        p.add_argument('--file', nargs='*', help='시딩 파일 번호(10-1 꼴)')
        if name == 'apply':
            p.add_argument('--item', help='이 항목이 쓰는 시딩만')
    w = sub.add_parser('which')
    w.add_argument('item')
    args = ap.parse_args()

    if args.cmd == 'which':
        for sf in which(args.item):
            print(sf.stem, sf.marker, ' '.join(sf.keys))
        return

    files = pick(args)
    if args.cmd == 'apply':
        for sf in files:
            run_tx(sf.cleanup() + [sf.insert.rstrip().rstrip(';')])
            place_files(sf)
            print(f'{sf.stem} {sf.marker} 넣음')
        print('키 값', write_ids(args.target, key_values(files)))
    elif args.cmd == 'revert':
        for sf in files:
            run_tx(sf.cleanup())
            print(f'{sf.stem} {sf.marker} 지움')
        write_ids(args.target, {name: [] for sf in files for name in sf.keys})
    else:
        values = key_values(files)
        for sf in files:
            for name in sf.keys:
                print(f'{sf.stem} {name} {"있음" if values[name] else "없음"} {values[name]}')


if __name__ == '__main__':
    main()
