"""이번 스모크 결과를 읽어 통과하지 못한 항목이 있으면 알림 서버로 관리자 메일을 보낸다.

스모크는 전에 통과한 적이 있는 줄이 실패할 때만 깨짐으로 친다. 매일 점검은 상용이 지금 도는지를 보는 것이라
원래 실패와 자동으로 못 봄도 알린다. 결과가 없으면 점검 자체가 돌지 못한 것이라 그것도 알린다.
"""
import glob
import html
import json
import os
import sys
from datetime import datetime, timedelta, timezone
from urllib.request import Request, urlopen

KST = timezone(timedelta(hours=9))
HERE = os.path.dirname(os.path.realpath(__file__))
SMOKE_DIR = os.path.join(HERE, 'testbook', 'results', '스모크')
TEMPLATE = 'BID_SMOKE_ALERT'


def this_report():
    started = os.environ.get('SMOKE_STARTED', '')
    dirs = sorted(d for d in glob.glob(os.path.join(SMOKE_DIR, '*')) if os.path.basename(d) >= started)
    for d in reversed(dirs):
        path = os.path.join(d, 'report.json')
        if os.path.exists(path):
            with open(path, encoding='utf-8') as f:
                return d, json.load(f)
    return None, None


def detail_of(row):
    lines = [f'{row["id"]} {row["title"]} : {row["status"]}']
    for c in row.get('checks') or []:
        if c['verdict'] == 'fail':
            lines.append(f'  - {c["line"]}번 {c["expect"]}')
            lines.append(f'    이번 결과 {c["got"][:300]}')
    for u in row.get('unseen') or []:
        lines.append(f'  - {u}')
    return '\n'.join(lines)


def send(to, variables):
    body = json.dumps({'email': to, 'templateCode': TEMPLATE, 'variables': variables}).encode()
    req = Request(os.environ['BP_NOTIFICATION_URL'].rstrip('/') + '/messages/email', data=body, method='POST',
                  headers={'Content-Type': 'application/json', 'X-API-Key': os.environ['BP_NOTIFICATION_API_KEY']})
    with urlopen(req, timeout=30) as r:
        return r.status


def main():
    outdir, report = this_report()
    checked = datetime.now(KST).strftime('%Y-%m-%d %H:%M')
    if report is None:
        fail_count, summary, details, path = 1, '점검이 결과를 남기지 못했습니다', '로그를 확인해 주십시오.', os.path.join(HERE, 'logs')
        target = ''
    else:
        bad = [r for r in report['items'] if r['status'] != '통과']
        if not bad:
            print(f'전부 통과 {len(report["items"])}건, 메일 없음')
            return 0
        fail_count = len(bad)
        summary = f'{report["set"]}건 중 통과 {report["set"] - fail_count}건, 통과 못 함 {fail_count}건'
        details = '\n\n'.join(detail_of(r) for r in bad)
        path = os.path.join(outdir, 'report.md')
        target = ', '.join(sorted(set(report.get('target', {}).values())))
    # 알림 서버가 값을 그대로 HTML 본문에 넣는다
    variables = {'checkedAt': checked, 'target': target, 'summary': summary,
                 'details': html.escape(details, quote=False), 'reportPath': path, 'failCount': str(fail_count)}
    sent = 0
    for to in [x.strip() for x in os.environ.get('SMOKE_ALERT_TO', '').split(',') if x.strip()]:
        try:
            print(f'알림 {to} {send(to, variables)}')
            sent += 1
        except Exception as e:  # 한 사람에게 못 보내도 나머지에게는 보낸다
            print(f'알림 실패 {to} {e}', file=sys.stderr)
    return 0 if sent else 1


if __name__ == '__main__':
    sys.exit(main())
