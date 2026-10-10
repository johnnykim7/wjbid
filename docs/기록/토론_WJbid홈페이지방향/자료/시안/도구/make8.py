# AIDA 홈 시안(시안 1 기준)과 발주사가 고를 서비스 후보 목록
# 첫 화면은 1440x900 한 화면 안에서 왼쪽·오른쪽 칸 끝을 맞추고 다섯 칸 띠로 닫는다. 그 아래는 욕구와 행동, 나머지는 메뉴로 보낸다
# 사용: python3 -I make8.py <portal6.html> <make7.py> <출력 폴더>
import sys, os, html

P6, M7, OUT = sys.argv[1], sys.argv[2], sys.argv[3]
src = open(M7, encoding='utf-8').read()
src = src[:src.index('\nos.makedirs(OUT, exist_ok=True)\nfor name, fn in')]
ns = {'__name__': 'm7'}
sys.argv = ['make7.py', P6, OUT]
exec(compile(src, M7, 'exec'), ns)
I, e, row, ROWS, SAMPLE, FOOTER = ns['I'], ns['e'], ns['row'], ns['ROWS'], ns['SAMPLE'], ns['FOOTER']

MENUS = [
    ('입찰공고', ['마감 전 공고', '사전 공고', '변경 공고', '관심 공고', '공고문 예시']),
    ('재발주 예측', ['다음 달 공고', '재발주 달력', '옵션 연장 알림']),
    ('낙찰정보', ['최근 낙찰', '낙찰 업체', '낙찰가 범위']),
    ('발주분석', ['기지별 지도', '월별 동향', '업종별 발주']),
    ('맞춤 판정', ['회사 정보', '적격성 진단', '공고별 판정']),
    ('서류함', ['공통 서류함', '공고별 서류', '유효기간 알림', '제출 전 검사']),
    ('등록·컨설팅', ['SAM 등록 대행', 'PIEE 등록 대행', 'SAM 갱신', '제안서 자문', '현장 설명회', '대금 청구', '서류 영문 번역', '팀 구성 찾기']),
    ('입찰 교실', ['무료 입찰 교실', '교육 자료', '용어 사전', '서식 모음']),
    ('고객센터', ['공지사항', '자주 묻는 질문', '1:1 문의', '요금 안내', '카카오톡 알림']),
]
nav = ''.join(f'<a href="#">{m}</a>' for m, _ in MENUS)
cols = ''.join('<div class="col"><h5>{}</h5><ul>{}</ul></div>'.format(m, ''.join(f'<li>{x}</li>' for x in xs)) for m, xs in MENUS)
ns['GNB'] = f'''<div class="gnb">
    <div class="wrap">
      <a class="all" href="#"><svg class="i"><use href="#i-menu"/></svg>전체메뉴</a>
      <nav>{nav}</nav>
      <a class="price" href="#">요금 안내</a>
    </div>
  </div>
  <div class="mega">
    <div class="wrap">{cols}</div>
  </div>
'''

ns['CSS'] += r'''
/* AIDA 첫 화면: 한 화면 안에서 닫는다 */
.fs{align-items:stretch;margin-top:20px}
.fs > div{display:flex;flex-direction:column;min-width:0}
.fs > div > [data-st]:not([hidden]){display:flex;flex-direction:column;flex:1}
.lstw{display:flex;flex-direction:column;flex:1}
.bids li{min-height:63px;padding:7px 0}
.bids .t span{margin-top:1px}
.bids .t .fit{display:inline-flex;align-items:center;height:22px;padding:0 8px;margin-left:10px;border-radius:5px;font-size:13px;font-weight:800;font-style:normal;vertical-align:1px}
.fit.ok{background:var(--green-l);color:var(--green)}
.fit.lack{background:var(--amber-l);color:var(--amber)}
.fit.no{background:var(--surface);color:var(--sub)}
.lft{margin-top:auto;display:flex;align-items:center;justify-content:space-between;height:46px;padding:0 4px;font-size:15px;color:var(--sub)}
.lft a{display:inline-flex;align-items:center;font-weight:800;color:var(--blue)}
.lft a svg{width:16px;height:16px;stroke-width:2.4}
.empty{flex:1;display:grid;grid-template-columns:minmax(0,1fr) 300px;margin-top:16px;border:1px solid var(--line);border-radius:14px;overflow:hidden;background:var(--blue-ll)}
.empty .tx{padding:34px 36px;display:flex;flex-direction:column}
.empty h3{margin:12px 0 10px;font-size:27px;line-height:1.35;font-weight:800;letter-spacing:-.035em;color:var(--navy)}
.empty p{margin:0;font-size:16.5px;line-height:1.65;color:#2c3850}
.empty .lk{margin-top:auto;display:flex;gap:10px}
.empty .lk a{height:48px;padding:0 18px;border-radius:10px;border:1.5px solid var(--line);background:#fff;display:inline-flex;align-items:center;gap:6px;font-size:16px;font-weight:700}
.empty .lk a svg{width:16px;height:16px;stroke-width:2.4}
.empty img{width:100%;height:100%;object-fit:cover;display:block}
.smp{display:block;margin:18px 0 18px;padding:16px 18px;border:1px solid var(--line);border-radius:10px;background:#fff}
.smp small{display:block;font-size:13.5px;font-weight:700;color:var(--blue)}
.smp b{display:block;margin-top:6px;font-size:18px;font-weight:800;letter-spacing:-.03em;color:var(--ink)}
.smp i{display:block;margin-top:2px;font-style:normal;font-size:13.5px;color:var(--mute)}
.smp dl{margin:12px 0 0;padding-top:10px;border-top:1px solid var(--line-l);display:grid;grid-template-columns:1fr 1fr;gap:8px 16px}
.smp dl div{min-width:0}
.smp dt{font-size:12.5px;color:var(--mute)}
.smp dd{margin:1px 0 0;font-size:15px;font-weight:700;color:var(--ink);white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.empty{margin-top:12px}
.empty .tx{padding:24px 32px}
.empty h3{margin:10px 0 6px;font-size:25px}
.smp{margin:12px 0 14px;padding:13px 16px}
.smp dl{margin-top:9px;padding-top:9px;gap:5px 16px}
.side{flex:1}
.intro{flex:1;display:flex;flex-direction:column}
.intro .im{height:106px}
.intro .bx{flex:1;display:flex;flex-direction:column;padding:18px 22px 18px}
.intro h3{font-size:21.5px}
.fr{margin-top:14px}
.intro .bigb{margin-top:auto}
.intro .up{margin:12px 0 0;padding:10px 12px;border-radius:9px;border:1px dashed #c7d6ef;font-size:14.5px;color:var(--ink);display:flex;align-items:center;justify-content:space-between;gap:8px}
.intro .up b{color:var(--blue)}
.intro .up a{font-weight:700;color:var(--blue);white-space:nowrap;display:inline-flex;align-items:center}
.intro .up a svg{width:14px;height:14px;stroke-width:2.4}
.intro .lg{margin-top:10px}
.intro .sam{margin-top:14px;padding-top:13px;border-top:1px solid var(--line-l);display:flex;align-items:center;gap:7px;font-size:14.5px;color:var(--sub)}
.intro .sam > svg{width:16px;height:16px;color:var(--blue);stroke-width:2.4;flex:none}
.intro .sam span{flex:1}
.intro .sam a{display:inline-flex;align-items:center;font-weight:700;color:var(--blue);white-space:nowrap}
.intro .sam a svg{width:14px;height:14px;stroke-width:2.4}
.mine h6{margin:10px 0 0;font-size:14.5px;font-weight:800;color:var(--sub)}
.mine{flex:1}
.fitsum{display:grid;grid-template-columns:repeat(3,1fr);gap:8px;margin:6px 0 4px}
.fitsum div{border-radius:10px;padding:10px 10px 8px;text-align:center}
.fitsum b{display:block;font-size:24px;font-weight:800;letter-spacing:-.02em}
.fitsum span{font-size:13.5px;font-weight:700}
.fitsum .ok{background:var(--green-l);color:var(--green)}
.fitsum .lack{background:var(--amber-l);color:var(--amber)}
.fitsum .no{background:var(--surface);color:var(--sub)}
.fitnote{margin:6px 0 8px;font-size:13.5px;color:var(--mute)}

/* 첫 화면을 닫는 다섯 칸 띠 */
.flow{margin-top:16px;display:grid;grid-template-columns:repeat(5,minmax(0,1fr));border:1px solid var(--line);border-radius:14px;background:#fff}
.flow a{position:relative;display:flex;align-items:center;gap:14px;padding:18px 20px;min-height:94px}
.flow a + a{border-left:1px solid var(--line-l)}
.flow a + a:before{content:"";position:absolute;left:-7px;top:50%;width:12px;height:12px;margin-top:-6px;background:#fff;border-top:1px solid var(--line);border-right:1px solid var(--line);transform:rotate(45deg)}
.flow .ic{flex:none;width:48px;height:48px;border-radius:14px;background:var(--blue-l);color:var(--blue);display:grid;place-items:center}
.flow .ic svg{width:24px;height:24px;stroke-width:1.8}
.flow b{display:block;font-size:17.5px;font-weight:800;letter-spacing:-.03em}
.flow b em{font-style:normal;color:var(--blue);margin-right:6px}
.flow span{display:block;margin-top:2px;font-size:14.5px;color:var(--sub);line-height:1.4}

/* 서비스 지도 */
.smap{display:grid;grid-template-columns:repeat(5,minmax(0,1fr));gap:14px}
.sc{background:#fff;border:1px solid var(--line);border-radius:14px;padding:20px 18px 14px;display:flex;flex-direction:column}
.sc .hd2{display:flex;align-items:center;gap:10px;padding-bottom:12px;border-bottom:2px solid var(--ink)}
.sc .hd2 i{width:30px;height:30px;border-radius:50%;background:var(--blue);color:#fff;font-style:normal;font-weight:800;font-size:14px;display:grid;place-items:center}
.sc .hd2 b{font-size:19px;font-weight:800;letter-spacing:-.03em}
.sc p{margin:10px 0 4px;font-size:14.5px;color:var(--sub);line-height:1.5;min-height:44px}
.sc ul{list-style:none;margin:0;padding:0}
.sc li{padding:10px 0;border-bottom:1px solid var(--line-l);font-size:15.5px;font-weight:700;line-height:1.4}
.sc li:last-child{border-bottom:0}
.tier{display:inline-flex;align-items:center;height:21px;padding:0 7px;margin-top:4px;border-radius:5px;font-size:12.5px;font-weight:800;letter-spacing:0}
.tier.f{background:var(--green-l);color:var(--green)}
.tier.m{background:var(--blue-l);color:var(--blue)}
.tier.c{background:var(--amber-l);color:var(--amber)}
.tier.k{background:#f3effc;color:#6b46c1}
.legend2{display:flex;justify-content:center;gap:16px;margin-top:20px;font-size:14.5px;color:var(--sub)}
.legend2 span{display:flex;align-items:center;gap:6px}

/* 회원 혜택 세 장 */
.b3{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:22px}
.fc{border:1px solid var(--line);border-radius:14px;padding:24px 24px 20px;display:flex;flex-direction:column;background:#fff}
.fc .ic{width:46px;height:46px;border-radius:13px;background:var(--blue-l);color:var(--blue);display:grid;place-items:center}
.fc .ic svg{width:23px;height:23px}
.fc h3{margin:14px 0 6px;font-size:21px;font-weight:800;letter-spacing:-.035em}
.fc > p{margin:0 0 16px;font-size:15.5px;color:var(--sub);line-height:1.55}
.pv{margin-top:auto;border-radius:12px;background:var(--surface);padding:14px 16px;font-size:14.5px}
.pv .cmp{display:grid;grid-template-columns:1fr 1fr;gap:10px}
.pv .cmp div{background:#fff;border-radius:9px;padding:10px 12px;border:1px solid var(--line-l)}
.pv .cmp small{display:block;font-size:12.5px;font-weight:800;color:var(--mute);margin-bottom:4px}
.pv .cmp .m small{color:var(--blue)}
.pv .cmp p{margin:0;font-size:14px;line-height:1.5}
.pv .cmp ul{margin:0;padding-left:16px;font-size:14px;line-height:1.6}
.pv .co{display:flex;flex-wrap:wrap;gap:6px;margin-bottom:10px}
.pv .co span{height:26px;padding:0 9px;border-radius:6px;background:#fff;border:1px solid var(--line);font-size:13px;font-weight:600;display:flex;align-items:center}
.pv .rs2 div{display:flex;align-items:center;justify-content:space-between;gap:10px;padding:8px 0;border-top:1px solid var(--line)}
.pv .rs2 b{font-size:14.5px;font-weight:700;white-space:nowrap;overflow:hidden;text-overflow:ellipsis}
.pv .rs2 .fit{flex:none;display:inline-flex;align-items:center;height:24px;padding:0 8px;border-radius:5px;font-size:13px;font-weight:800}
.pv .cal2 div{display:grid;grid-template-columns:64px 1fr auto;gap:10px;align-items:center;padding:8px 0;border-top:1px solid var(--line)}
.pv .cal2 div:first-child{border-top:0}
.pv .cal2 em{font-style:normal;font-weight:800;color:var(--navy)}
.pv .cal2 b{font-size:14.5px;font-weight:700}
.pv .cal2 b small{display:block;font-size:12.5px;font-weight:500;color:var(--mute)}
.pv .cal2 i{font-style:normal;height:24px;padding:0 8px;border-radius:5px;background:var(--blue-l);color:var(--blue);font-size:12.5px;font-weight:800;display:flex;align-items:center;gap:4px}
.pv .ex{margin:8px 0 0;font-size:12.5px;color:var(--mute)}
.fc .go{margin-top:14px;display:inline-flex;align-items:center;font-size:15px;font-weight:800;color:var(--blue)}
.fc .go svg{width:16px;height:16px;stroke-width:2.4}

/* 요금 */
.plans{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px}
.pc{background:#fff;border:1px solid var(--line);border-radius:14px;padding:26px 26px 22px;display:flex;flex-direction:column;position:relative}
.pc.on{border:2px solid var(--blue)}
.pc .rec{position:absolute;top:-13px;left:24px;height:26px;padding:0 10px;border-radius:6px;background:var(--blue);color:#fff;font-size:13px;font-weight:800;display:flex;align-items:center}
.pc h4{margin:0;font-size:18px;font-weight:800;color:var(--sub)}
.pc.on h4{color:var(--blue)}
.pc .pr{margin:6px 0 4px;font-size:34px;font-weight:800;letter-spacing:-.03em}
.pc .pr small{font-size:17px;font-weight:700;margin-left:2px}
.pc .pr.s{font-size:26px;padding:5px 0 3px}
.pc .who{font-size:15px;color:var(--sub);margin-bottom:14px}
.pc ul{list-style:none;margin:0 0 18px;padding:14px 0 0;border-top:1px solid var(--line-l);display:grid;gap:9px}
.pc li{display:flex;align-items:flex-start;gap:8px;font-size:15.5px;line-height:1.45}
.pc li svg{width:17px;height:17px;margin-top:2px;color:var(--green);stroke-width:2.6}
.pc .bigb{margin-top:auto;width:100%;height:52px;font-size:16.5px}
.promos3{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:18px;margin-top:22px}
.promos3 .promo{height:170px}
.promos3 .promo .tx{width:58%}
.promos3 .promo b{font-size:20px}
.help{display:flex;flex-direction:column;gap:12px}
.help a{display:flex;align-items:center;gap:14px;padding:18px 20px;border:1px solid var(--line);border-radius:14px}
.help .fi{width:40px;height:40px;background:var(--blue-l);color:var(--blue)}
.help .fi svg{width:20px;height:20px}
.help b{flex:1;font-size:17px;font-weight:800}
.help b span{display:block;margin-top:2px;font-size:14.5px;font-weight:500;color:var(--sub)}

/* 전체메뉴: 글자를 키우고 제목은 파랑 대신 먹색 */
.mega .wrap{padding:28px 0 34px}
.mega .col{padding:0 14px}
.mega h5{margin-bottom:14px;font-size:18px;color:var(--ink)}
.mega ul{gap:12px}
.mega li{font-size:16px;color:#2c3445;padding-right:0}

/* 색은 먹색, 파랑 하나, 마감 임박 빨강만 쓴다. 초록·주황·보라 칩과 파란 아이콘 상자를 걷어 낸다 */
.bd,.bd.r,.bd.p,.bd.s{background:#fff;border:1px solid var(--line);color:var(--sub)}
.bd.r{color:var(--ink)}
.fit.ok,.pv .rs2 .fit.ok{background:var(--blue-l);color:var(--blue)}
.fit.lack,.pv .rs2 .fit.lack{background:#fff;border:1px solid var(--line);color:var(--ink)}
.fit.no,.pv .rs2 .fit.no{background:none;color:var(--mute);font-weight:600}
.bids .t .fit.lack{height:22px}
.fitsum div,.fitsum .ok,.fitsum .lack,.fitsum .no{background:var(--surface);color:var(--sub)}
.fitsum b{color:var(--ink)}
.fitsum .ok b{color:var(--blue)}
.tier,.tier.f,.tier.c,.tier.k{background:#fff;border:1px solid var(--line);color:var(--sub)}
.tier.m{background:#fff;border:1px solid #b9cdf3;color:var(--blue)}
.fr li svg,.pc li svg{color:var(--blue)}
.flow .ic{display:none}
.flow a{padding:20px 24px;align-items:flex-start}
.flow b{font-size:18.5px}
.flow b em{color:var(--blue)}
.sc .hd2 i{width:auto;height:auto;background:none;color:var(--blue);font-size:19px}
.fc .ic{display:none}
.fc h3{margin-top:0}
.help .fi{width:auto;height:auto;background:none;color:var(--ink)}
.help .fi svg{width:24px;height:24px}

/* 신한은행식 정돈: 흰 메뉴 띠에 진한 큰 글자, 얇은 선, 덜 둥근 모서리 */
.gnb{background:#fff;height:64px;border-top:1px solid var(--line-l);border-bottom:1px solid var(--line)}
.gnb .wrap{height:64px}
.gnb nav{order:1;padding:0 28px 0 0}
.gnb nav a{color:var(--ink);font-size:19px;font-weight:700;letter-spacing:-.03em;height:64px;display:flex;align-items:center}
.gnb nav a:hover:after{content:"";position:absolute;left:0;right:0;bottom:-1px;height:3px;background:var(--blue)}
.gnb .price{order:2;margin-left:0;height:38px;padding:0 14px;border-radius:6px;background:#fff;border:1px solid var(--line);color:var(--ink);font-size:15px;font-weight:700}
.gnb .all{order:3;width:64px;height:64px;margin-left:14px;background:var(--navy);font-size:0;gap:0}
.gnb .all svg{width:28px;height:28px;color:#fff;stroke-width:1.8}
.mega{top:192px}
.sch .box{border-radius:10px}
.sch .go{border-radius:8px}
.tag{border-radius:4px}
.ph{border-bottom-width:2px}
.ph .tabs a{border-radius:6px}
.bigb{border-radius:6px}
.rel a{border-radius:6px}
.intro .up{border-radius:6px;border-style:solid;border-color:var(--line)}
.fc > p{order:-1;margin:0 0 6px;font-size:15.5px}
.fc h3{margin:0 0 16px;font-size:22px}
.pc .rec{border-radius:4px}
.float .box{border-radius:8px}
'''

THEMES = [
    ('색9_올리브', '올리브 카키', dict(a='#5b6b2a', d='#46531f', l='#eef1e4', ll='#f7f8f1', n='#262b1a', t='#cdd5ae', f='#cfdc9c')),
    ('색10_가지', '가지색', dict(a='#6b2d5c', d='#521f46', l='#f4e9f1', ll='#faf5f8', n='#2a1a26', t='#dcbad2', f='#e7b8d9')),
    ('색11_검정노랑', '검정과 노랑', dict(a='#1f2329', d='#000000', l='#fff6d6', ll='#fffbeb', n='#111418', t='#f0d27a', f='#ffd43b',
     x='.bigb{background:#f5c400;color:#141d30}.bigb:hover{background:#e3b500}.bigb.o{background:#fff;color:var(--ink)}'
       '.sch .go{background:#f5c400;color:#141d30}.gnb nav a:hover:after{background:#f5c400}'
       '.pc .rec{background:#f5c400;color:#141d30}.fit.ok,.pv .rs2 .fit.ok{background:#fff3c4;color:#141d30}'
       '.logo b em{color:#d9a300}.ph .tabs a.on{background:#141d30}')),
    ('색3_차콜주황', '차콜과 주황', dict(a='#c2410c', d='#9a3412', l='#fdf0e7', ll='#fef8f3', n='#23272e', t='#f3c3a3', f='#fdba8c')),
    ('색7_먹색', '먹색 한 가지', dict(a='#1f2329', d='#000000', l='#eef0f2', ll='#f7f8f9', n='#111418', t='#c4c9d0', f='#d0d4da')),
    ('색8_모카', '모카 갈색', dict(a='#7a4b2a', d='#5e3920', l='#f5ede6', ll='#faf6f2', n='#2a211b', t='#dbc3ae', f='#e2c3a6')),
]


def theme_css(v):
    return (f'\n:root{{--blue:{v["a"]};--blue-d:{v["d"]};--blue-l:{v["l"]};--blue-ll:{v["ll"]};--navy:{v["n"]}}}\n'
            f'.tier.m{{border-color:{v["t"]}}}\nfooter .lk a:nth-child(2){{color:{v["f"]}}}\n' + v.get('x', '') + '\n')


CANDS = ('색3_차콜주황', '색7_먹색', '색8_모카')


def compare():
    cards = ''.join(f'''<a class="cd" href="홈시안_{k}.html#s1"><img src="shots/{k}_s1_fold.jpg" alt="{lb} 첫 화면"><div><b>{lb}{' (후보)' if k in CANDS else ''}</b><span>{v["a"]}</span><i style="background:{v["a"]}"></i><i style="background:{v["n"]}"></i><i style="background:{v["l"]}"></i></div></a>'''
                    for k, lb, v in THEMES)
    css = r'''
:root{--ink:#141d30;--sub:#3d4657;--line:#e1e5ec}
*{box-sizing:border-box}html,body{margin:0}
body{background:#f6f7f9;color:var(--ink);font-family:"Pretendard Variable",Pretendard,system-ui,sans-serif;word-break:keep-all;-webkit-font-smoothing:antialiased}
.wrap{max-width:1600px;margin:0 auto;padding:32px 20px 60px}
h1{margin:0;font-size:28px;font-weight:800;letter-spacing:-.03em}
p.lead{margin:8px 0 22px;font-size:16.5px;color:var(--sub)}
.g{display:grid;grid-template-columns:repeat(3,minmax(0,1fr));gap:20px}
.cd{display:block;background:#fff;border:1px solid var(--line);border-radius:12px;overflow:hidden;color:inherit;text-decoration:none}
.cd img{display:block;width:100%;border-bottom:1px solid var(--line)}
.cd div{display:flex;align-items:center;gap:8px;padding:14px 16px}
.cd b{font-size:18px;font-weight:800}
.cd span{flex:1;font-size:14px;color:var(--sub)}
.cd i{width:22px;height:22px;border-radius:4px;border:1px solid var(--line)}
@media (max-width:900px){.g{grid-template-columns:1fr}}
'''
    return ('<!doctype html>\n<html lang="ko">\n<head>\n<meta charset="utf-8">\n<meta name="viewport" content="width=device-width, initial-scale=1">\n'
            '<title>WJbid 홈 색상 비교</title>\n'
            '<link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable.min.css">\n'
            f'<style>{css}</style>\n</head>\n<body>\n<div class="wrap">\n<h1>홈 시안 색상 비교</h1>\n'
            '<p class="lead">윗줄 셋이 새 계열, 아랫줄 셋이 후보로 둔 안입니다. 구성과 글자는 같고 색만 다릅니다. 그림을 누르면 시안이 열립니다.</p>\n'
            f'<div class="g">{cards}</div>\n</div>\n</body>\n</html>\n')

FLOW = [('search', '찾기', '마감 전 공고, 알림, 재발주 예측'),
        ('badge', '판단', '상세 번역, 우리 회사 맞춤 판정'),
        ('folder', '준비', '등록 대행, 서류함, 서류 영문 번역'),
        ('pen', '제출', '제안서 자문·검토, 제출 전 검사'),
        ('check', '낙찰 뒤', '대금 청구 대행, 옵션 연장 알림')]
flow = '<div class="flow">' + ''.join(
    f'<a href="#svc"><span class="ic">{I(ic)}</span><span><b><em>{k + 1}</em>{a}</b><span>{b}</span></span></a>' for k, (ic, a, b) in enumerate(FLOW)) + '</div>'

F, M, C, K = ('f', '무료'), ('m', '회원'), ('c', '건별'), ('k', '컨설팅')
SVC = [
    ('찾기', '지금 낼 수 있는 일감과 곧 나올 일감을 먼저 압니다', [
        ('마감 전 공고 목록', '영문 공고를 SAM 번역 수준의 한글로 마감순 정리', [F], ''),
        ('기지·업종 맞춤 새 공고 알림', '고른 기지와 업종에 새 공고가 나오면 이메일로 알림', [M], ''),
        ('카카오톡 알림', '새 공고와 마감 3일 전을 카카오톡으로 알림', [M], ''),
        ('사전 공고·수요 조사 알림', '본 공고 전에 나오는 사전 공고로 미리 준비', [M], ''),
        ('변경 공고 비교', '변경 공고에서 바뀐 곳만 표시', [M], ''),
        ('재발주 예측 달력', '지난 계약 기간과 옵션이 끝나는 때로 다시 나올 시기를 예측', [M], '새로 만듦, 예측 근거 자료(낙찰 공지와 계약 기간) 수집 필요'),
    ]),
    ('판단', '이 공고, 우리 회사가 할 수 있는지 판단합니다', [
        ('공고문 상세 번역', '첨부(PWS 등)까지 작업 범위, 자격, 서류, 평가 방식을 한글로', [M], ''),
        ('적격성 진단', '참가 자격을 우리 회사에 맞춰 하나씩 확인', [M], ''),
        ('우리 회사 맞춤 판정', '회사 정보를 한 번 넣으면 공고마다 참여 가능, 서류 부족 표시', [M], '새로 만듦'),
        ('비슷한 공고 낙찰가 범위', '지난 낙찰가로 입찰가를 정하는 데 참고', [M], '낙찰 공지 자료로 만듦'),
        ('기지별 발주 동향', '기지와 업종별로 어떤 공고가 얼마나 나오는지', [F], ''),
    ]),
    ('준비', '등록과 서류를 빠짐없이 갖춥니다', [
        ('SAM.gov·UEI·NCAGE·PIEE 등록 대행', '처음 등록하는 회사도 순서대로 대신 처리', [C], ''),
        ('SAM 등록 만료 알림과 갱신', '해마다 갱신, 만료 전에 알리고 갱신까지 대행', [M, C], ''),
        ('회사 공통 서류함', '등기부등본, 면허, 실적 서류를 한곳에, 유효기간 알림', [M], ''),
        ('회사 서류 영문 번역', '등기부등본, 재무제표, 실적 증명을 영문으로', [C], ''),
        ('팀 구성 짝 찾기', '허가증이 있는 회사와 실적이 있는 회사를 이어 줌(공동 수급, 하도급)', [M], '새로 만듦, 회원 간 정보 공개 동의 필요'),
        ('서식·체크리스트', '공고 유형별 준비물 목록과 서식', [F], ''),
    ]),
    ('제출', '제출 전에 빠진 것을 잡습니다', [
        ('제안서 자문·검토', '고객이 쓴 제안서를 전문가가 검토하고 보완점 안내', [C], '6/30 결정대로 대행이 아닌 자문·검토로 표기, 법무 확인 뒤 출시'),
        ('가격 제안서 도움', '원가 항목 정리와 작성 방법 안내', [C], ''),
        ('제출 전 검사', '빠진 서류, 쪽수, 서식을 제출 전에 확인', [M], ''),
        ('계약관 영문 메일·질의 대행', '질문서와 연락을 영문으로 작성', [C], ''),
        ('현장 설명회 일정 알림', '설명회와 현장 방문 일정을 알림', [M], ''),
        ('현장 설명회 동행·기지 출입 지원', '기지 출입 절차 안내, 설명회 동행과 통역', [K], ''),
    ]),
    ('낙찰 뒤', '계약을 지키고 다음 공고로 잇습니다', [
        ('계약 서류·대금 청구 대행', '계약 서류 정리와 PIEE 대금 청구', [C], ''),
        ('옵션 연장·재발주 알림', '내 계약의 옵션 연장과 재발주 시기를 알림', [M], ''),
    ]),
    ('배우기', '처음이어도 따라올 수 있게', [
        ('무료 온라인 입찰 교실', '월 1회 온라인으로 미군 입찰 기초 설명', [F], '새로 만듦, 운영 인력 필요'),
        ('입찰 교육 자료·용어 사전', '단계별 안내 글과 조달 용어 풀이', [F], ''),
        ('1:1 상담', '우리 회사 상황에 맞춘 상담', [K], ''),
    ]),
    ('결제', '', [
        ('수출바우처 결제 연계', '정부 바우처로 컨설팅 비용 결제', [K], 'WJbid 가 수행기관으로 등록될 수 있는지 먼저 확인'),
    ]),
]


def tiers(ts):
    return ''.join(f'<span class="tier {c}">{t}</span> ' for c, t in ts)


def smap():
    cards = []
    for k, (stage, desc, items) in enumerate(SVC[:5]):
        lis = ''.join(f'<li>{n}<br>{tiers(ts)}</li>' for n, _, ts, _ in items[:5])
        cards.append(f'<div class="sc"><div class="hd2"><i>{k + 1}</i><b>{stage}</b></div><p>{desc}</p><ul>{lis}</ul></div>')
    leg = ''.join(f'<span><span class="tier {c}">{t}</span>{d}</span>' for (c, t), d in
                  ((F, '누구나'), (M, '월 50,000원'), (C, '건마다 견적'), (K, '상담 후 결정')))
    return f'<div class="smap">{"".join(cards)}</div><div class="legend2">{leg}</div>'


def fitrow(r, fit):
    s = row(r)
    cls, txt = fit
    return s.replace('</i></span></div>', f'</i><em class="fit {cls}">{txt}</em></span></div>', 1)


FITS = [('ok', '참여 가능'), ('no', '면허 없음'), ('no', '해당 업종 아님'), ('lack', '서류 1건 부족'), ('no', '실적 부족'), ('lack', '서류 2건 부족'), ('ok', '참여 가능')]


def list_block(member=False):
    rows = '\n        '.join((fitrow(r, f) if member else row(r)) for r, f in zip(ROWS[:7], FITS))
    return f'''<section class="lstw">
      <div class="ph">
        <h3>마감 전 공고</h3>
        <div class="tabs"><a class="on" href="#">마감 임박</a><a href="#">최신 공고</a>{'<a href="#">참여 가능만</a>' if member else ''}</div>
        <span class="cnt">13건 · 7일 안 마감 <b>8건</b></span>
      </div>
      <ul class="bids">
        {rows}
      </ul>
      <div class="lft"><span>{'맞춤 판정은 등록하신 회사 정보 기준입니다' if member else '한글 공고문 · SAM.gov 원문 함께 보기'}</span><a href="#">마감 전 공고 13건 모두 보기{I("cr")}</a></div>
    </section>'''


def page():
    s = ns['head']('WJbid 홈 시안 AIDA')
    s += ns['mockbar']('홈 시안 · AIDA', [('s1', '공고문이 있을 때'), ('s0', '지금 (0건)'), ('s2', '회원 로그인 후'), ('s1-menu', '전체메뉴')],
                       '공고 목록과 판정은 가상 자료')
    s += ns['util']().replace('○○건설 님', '○○환경 님')
    s += ns['header']('icons')
    left = f'''<div>
      <div data-st="s0">
        <div class="ph"><h3>마감 전 공고</h3><span class="cnt">공개된 한글 공고문 <b>0건</b> · 10.10 기준</span></div>
        <div class="empty">
          <div class="tx">
            <span class="tag" style="align-self:flex-start">준비 중</span>
            <h3>마감 전 공고를<br>한글 공고문으로 정리하고 있습니다</h3>
            <p>올라오는 대로 이 자리에 마감순으로 보입니다.</p>
            <a class="smp" href="#">
              <small>이렇게 정리해 드립니다 · 마감된 공고 예시</small>
              <b>{e(SAMPLE["title"])}</b>
              <i class="mono">{SAMPLE["no"]}</i>
              <dl><div><dt>계약 방식</dt><dd>확정고정가격</dd></div><div><dt>계약 기간</dt><dd>기초 1년 + 옵션 4년</dd></div><div><dt>평가 방식</dt><dd>최저가 (LPTA)</dd></div><div><dt>참가 자격</dt><dd>{len(SAMPLE["elig"])}가지 · 제출 서류 {len(SAMPLE["docs"])}건</dd></div></dl>
            </a>
            <div class="lk"><a href="#">새 공고 알림 받기{I("cr")}</a><a href="#">공고문 예시 전체 보기{I("cr")}</a></div>
          </div>
          <img src="img/army_17a23e72.jpg" alt="">
        </div>
      </div>
      <div data-st="s1">{list_block(False)}</div>
      <div data-st="s2">{list_block(True)}</div>
    </div>'''
    mine = ''.join(f'<li><span class="dd u">D-{r[2]}</span><div><b>{e(r[4])}</b><small>{r[0]}({r[1]}) 마감 · {r[6]}</small></div></li>' for r in (ROWS[0], ROWS[6]))
    right = f'''<div>
      <div data-st="s0,s1" class="side">
        <div class="intro">
          <div class="im"><img src="img/army_5decc7b0.jpg" alt=""><span class="tag">USFK 입찰 첫걸음</span></div>
          <div class="bx">
            <h3>주한미군 공사·용역 입찰,<br>한글로 찾고 순서대로 준비하세요</h3>
            <div class="fr"><b>무료 회원</b><ul><li>{I("ok")}마감 전 공고 한글로 보기</li><li>{I("ok")}관심 공고 저장</li><li>{I("ok")}입찰 신청</li></ul></div>
            <div class="up"><span>상세 번역·적격성 판정은 <b>회원</b></span><a href="#">월 50,000원{I("cr")}</a></div>
            <a class="bigb" href="#" style="margin-top:14px">무료로 시작</a>
            <div class="lg">이미 회원이신가요?<a href="#">로그인</a></div>
            <div class="sam">{I("check")}<span>제출 때 SAM 등록이 필요할 수 있습니다</span><a href="#">등록 대행{I("cr")}</a></div>
          </div>
        </div>
      </div>
      <div data-st="s2" class="side">
        <div class="today me">
          <h4>○○환경 님<span>회원 · 오늘 확인할 것</span></h4>
          <div class="g">
            <a href="#"><b>3</b><span>관심 공고</span></a>
            <a href="#"><b>2</b><span>입찰 신청</span></a>
            <a href="#"><b class="r">5</b><span>새 알림</span></a>
          </div>
        </div>
        <div class="mine"><h4>우리 회사 맞춤 판정<a href="#">회사 정보 고치기</a></h4>
          <div class="fitsum"><div class="ok"><b>4</b><span>참여 가능</span></div><div class="lack"><b>3</b><span>서류 부족</span></div><div class="no"><b>6</b><span>어려움</span></div></div>
          <p class="fitnote">마감 전 13건 기준, 서류 부족은 무엇이 빠졌는지 알려 드립니다</p>
          <h6>참여 가능한 공고</h6><ul>{mine}</ul>
        </div>
        <div class="qk">
          <a href="#"><span class="fi">{I("folder")}</span>서류함</a>
          <a href="#"><span class="fi">{I("cal")}</span>재발주 달력</a>
        </div>
      </div>
    </div>'''
    s += f'<main class="wrap">\n  {ns["FLOAT"]}\n  <div class="fs">\n    {left}\n    {right}\n  </div>\n  {flow}\n</main>\n'

    s += f'''<section class="band g" id="svc">
  <div class="wrap">
    <div class="bh"><small>서비스</small><h2>입찰하는 순서대로 도와 드립니다</h2><p>찾기부터 낙찰 뒤까지, 단계마다 필요한 것만 고르세요</p></div>
    {smap()}
  </div>
</section>
'''
    s1 = SAMPLE
    s += f'''<section class="band">
  <div class="wrap">
    <div class="bh"><small>회원 혜택</small><h2>회원은 한 걸음 더 봅니다</h2><p>월 50,000원으로 공고를 끝까지 읽고, 할 수 있는 공고만 고릅니다</p></div>
    <div class="b3">
      <div class="fc">
        <span class="ic">{I("book")}</span><h3>공고문 상세 번역</h3>
        <p>첨부 문서까지 읽어 작업 범위, 참가 자격, 제출 서류, 평가 방식을 한글로 정리합니다.</p>
        <div class="pv"><div class="cmp">
          <div><small>무료 · SAM 번역 수준</small><p>{e(s1["title"])}</p><p style="color:var(--mute);margin-top:4px">제목과 기본 정보</p></div>
          <div class="m"><small>회원 · 상세 번역</small><ul><li>작업 범위</li><li>참가 자격 {len(s1["elig"])}가지</li><li>제출 서류 {len(s1["docs"])}건</li><li>평가 방식 LPTA</li></ul></div>
        </div></div>
        <a class="go" href="#">공고문 예시 보기{I("cr")}</a>
      </div>
      <div class="fc">
        <span class="ic">{I("badge")}</span><h3>우리 회사 맞춤 판정</h3>
        <p>회사 정보를 한 번 넣으면 공고마다 참여할 수 있는지, 무엇이 모자란지 알려 드립니다.</p>
        <div class="pv">
          <div class="co"><span>폐기물 수집운반업</span><span>유사 실적 3년</span><span>부산</span></div>
          <div class="rs2">
            <div><b>부산지역 쓰레기 수거 및 처리</b><span class="fit ok">참여 가능</span></div>
            <div><b>대구지역 쓰레기 수거 및 처리</b><span class="fit lack">서류 1건 부족</span></div>
            <div><b>막사 P-6821 수리</b><span class="fit no">면허 없음</span></div>
          </div>
          <p class="ex">화면 예시입니다</p>
        </div>
        <a class="go" href="#">회사 정보 넣어 보기{I("cr")}</a>
      </div>
      <div class="fc">
        <span class="ic">{I("cal")}</span><h3>재발주 예측 달력</h3>
        <p>지난 계약이 끝나는 때를 계산해 같은 일이 다시 공고될 시기를 미리 알려 드립니다.</p>
        <div class="pv"><div class="cal2">
          <div><em>11월</em><b>미화 용역<small>오산 · 지난 계약 끝 2026.12</small></b><i>{I("eye")}회원 공개</i></div>
          <div><em>12월</em><b>시설 유지관리<small>험프리스 · 옵션 끝 2027.01</small></b><i>{I("eye")}회원 공개</i></div>
          <div><em>1월</em><b>쓰레기 수거<small>대구 · 지난 계약 끝 2027.02</small></b><i>{I("eye")}회원 공개</i></div>
        </div><p class="ex">화면 예시입니다. 실제 예측이 아닙니다</p></div>
        <a class="go" href="#">재발주 달력 보기{I("cr")}</a>
      </div>
    </div>
  </div>
</section>
'''
    s += f'''<section class="band g">
  <div class="wrap">
    <div class="bh"><small>요금</small><h2>무료로 시작하고, 필요할 때 넓히세요</h2><p>회원은 월 단위, 대행과 컨설팅은 건마다 상담합니다</p></div>
    <div class="plans">
      <div class="pc"><h4>무료</h4><div class="pr">0<small>원</small></div><div class="who">처음 둘러보는 회사</div>
        <ul><li>{I("ok")}마감 전 공고 한글로 보기 (SAM 번역 수준)</li><li>{I("ok")}관심 공고 저장</li><li>{I("ok")}입찰 신청</li><li>{I("ok")}입찰 교육 자료, 무료 입찰 교실</li></ul>
        <a class="bigb o" href="#">무료로 시작</a></div>
      <div class="pc on"><span class="rec">추천</span><h4>회원</h4><div class="pr">50,000<small>원/월</small></div><div class="who">입찰을 꾸준히 하는 회사</div>
        <ul><li>{I("ok")}공고문 상세 번역 (첨부 포함)</li><li>{I("ok")}적격성 진단, 우리 회사 맞춤 판정</li><li>{I("ok")}재발주 예측 달력</li><li>{I("ok")}새 공고·카카오톡 알림</li><li>{I("ok")}서류함, 제출 전 검사</li></ul>
        <a class="bigb" href="#">회원으로 시작</a></div>
      <div class="pc"><h4>대행·컨설팅</h4><div class="pr s">상담 후 결정</div><div class="who">맡기고 싶은 일이 있는 회사</div>
        <ul><li>{I("ok")}SAM.gov·PIEE 등록 대행</li><li>{I("ok")}제안서 자문·검토</li><li>{I("ok")}현장 설명회 동행, 기지 출입 지원</li><li>{I("ok")}대금 청구 대행, 서류 영문 번역</li></ul>
        <a class="bigb o" href="#">상담 신청</a></div>
    </div>
    <div class="promos3">
      <a class="promo a" href="#"><img src="img/army_71dae7c5.jpg" alt=""><div class="tx"><small>등록 대행</small><b>SAM.gov·PIEE 등록, 맡기세요</b><span>처음 등록하는 회사도 순서대로</span></div></a>
      <a class="promo b" href="#"><img src="img/army_5decc7b0.jpg" alt=""><div class="tx"><small>현장 지원</small><b>설명회 동행과 기지 출입</b><span>출입 절차부터 통역까지</span></div></a>
      <a class="promo a" href="#"><img src="img/army_d68e5213.jpg" alt=""><div class="tx"><small>무료 입찰 교실</small><b>매달 한 번, 온라인으로</b><span>미군 입찰 기초를 한 시간에</span></div></a>
    </div>
  </div>
</section>
'''
    faq = ['SAM.gov 등록이 꼭 있어야 입찰할 수 있나요?', '무료와 회원은 무엇이 다른가요?', '우리 회사 맞춤 판정은 어떻게 하나요?',
           '재발주 예측은 얼마나 맞나요?', '제안서는 누가 쓰나요?', '마감된 공고도 볼 수 있나요?']
    fl = ''.join(f'<li><span class="q">Q</span><span class="t">{x}</span></li>' for x in faq)
    s += f'''<section class="band" style="padding-bottom:20px">
  <div class="wrap">
    <div class="b2">
      <div class="tbox">
        <div class="tt"><a class="on" href="#">자주 묻는 질문</a><a href="#">공지사항</a><a class="more" href="#">더 보기{I("cr")}</a></div>
        <ul class="lst">{fl}</ul>
      </div>
      <div class="help">
        <a href="#"><span class="fi">{I("headset")}</span><b>1:1 문의<span>궁금한 점을 남겨 주시면 답변 드립니다</span></b>{I("cr")}</a>
        <a href="#"><span class="fi">{I("chat")}</span><b>카카오톡 알림 받기<span>새 공고와 마감 3일 전을 카카오톡으로</span></b>{I("cr")}</a>
        <a href="#"><span class="fi">{I("book")}</span><b>이용 안내<span>가입부터 입찰 신청까지 순서대로</span></b>{I("cr")}</a>
      </div>
    </div>
    <div class="rel">
      <h4>관련 사이트</h4>
      <a href="#">SAM.gov{I("ext")}</a><a href="#">PIEE{I("ext")}</a><a href="#">DLA{I("ext")}</a><a href="#">USAspending{I("ext")}</a><a href="#">Acquisition.gov (FAR){I("ext")}</a><a href="#">주한미군 USFK{I("ext")}</a>
    </div>
  </div>
</section>
'''
    s += '\n' + FOOTER + '\n' + ns['SCRIPT'] + '</body>\n</html>\n'
    return s


def sheet():
    css = r'''
:root{--ink:#141d30;--sub:#3d4657;--mute:#6b7384;--line:#e1e5ec;--line-l:#eef1f5;--surface:#f6f7f9;--blue:#1a56db;--blue-l:#edf3ff;--green:#12805c;--green-l:#e8f6ef;--amber:#b45309;--amber-l:#fff4e5;--purple:#6b46c1;--purple-l:#f3effc}
*{box-sizing:border-box}html,body{margin:0}
body{background:var(--surface);color:var(--ink);font-family:"Pretendard Variable",Pretendard,system-ui,sans-serif;font-size:16px;line-height:1.55;word-break:keep-all;-webkit-font-smoothing:antialiased}
.wrap{max-width:1080px;margin:0 auto;padding:36px 20px 80px}
h1{margin:0;font-size:30px;font-weight:800;letter-spacing:-.03em}
.lead{margin:8px 0 18px;font-size:17px;color:var(--sub)}
.leg{display:flex;flex-wrap:wrap;gap:14px;margin-bottom:22px;font-size:15px;color:var(--sub)}
.leg span{display:flex;align-items:center;gap:6px}
.tier{display:inline-flex;align-items:center;height:23px;padding:0 8px;border-radius:5px;font-size:13px;font-weight:800;white-space:nowrap}
.tier,.tier.f,.tier.c,.tier.k{background:#fff;border:1px solid var(--line);color:var(--sub)}
.tier.m{border-color:#b9cdf3;color:var(--blue)}
section{background:#fff;border:1px solid var(--line);border-radius:14px;padding:6px 22px 8px;margin-bottom:16px}
section h2{margin:14px 0 2px;font-size:21px;font-weight:800;letter-spacing:-.03em;display:flex;align-items:baseline;gap:10px}
section h2 small{font-size:15px;font-weight:500;color:var(--sub)}
table{width:100%;border-collapse:collapse}
th{font-size:13.5px;font-weight:700;color:var(--mute);text-align:left;padding:10px 8px;border-bottom:1px solid var(--line)}
td{padding:12px 8px;border-bottom:1px solid var(--line-l);vertical-align:top;font-size:15.5px}
tr:last-child td{border-bottom:0}
td.ck{width:44px}
td.ck input{width:20px;height:20px;accent-color:var(--blue);margin:2px 0 0}
td.nm{width:260px;font-weight:800}
td.tr{width:120px}
td.ds{color:var(--sub)}
td.nt{width:250px;font-size:14px;color:var(--sub)}
tr.on td{background:#f5f8ff}
.bar{position:sticky;bottom:0;margin-top:18px;display:flex;align-items:center;justify-content:space-between;padding:14px 20px;border-radius:12px;background:#1f2633;color:#fff;font-size:16px}
.bar b{font-size:20px;color:#8fb3ff}
.bar button{height:40px;padding:0 16px;border:0;border-radius:8px;background:#fff;color:var(--ink);font:inherit;font-weight:700;cursor:pointer}
@media print{.bar{display:none}body{background:#fff}section{break-inside:avoid}}
@media (max-width:760px){td.nt,th.nt{display:none}td.nm{width:auto}}
'''
    secs = []
    total = 0
    for stage, desc, items in SVC:
        rows = []
        for n, d, ts, note in items:
            total += 1
            rows.append(f'<tr><td class="ck"><input type="checkbox" aria-label="{n} 넣기"></td><td class="nm">{n}</td><td class="tr">{tiers(ts)}</td><td class="ds">{d}</td><td class="nt">{note}</td></tr>')
        secs.append(f'<section><h2>{stage}<small>{desc}</small></h2><table><thead><tr><th>넣기</th><th>서비스</th><th>대상</th><th>내용</th><th class="nt">확인할 것</th></tr></thead><tbody>{"".join(rows)}</tbody></table></section>')
    leg = ''.join(f'<span><span class="tier {c}">{t}</span>{d}</span>' for (c, t), d in
                  ((F, '누구나 무료'), (M, '회원 월 50,000원'), (C, '건마다 견적'), (K, '상담 후 결정')))
    script = '''<script>
(function () {
  var boxes = document.querySelectorAll('td.ck input');
  function upd() {
    var n = 0;
    boxes.forEach(function (b) { b.closest('tr').classList.toggle('on', b.checked); if (b.checked) n++; });
    document.getElementById('cnt').textContent = n;
  }
  boxes.forEach(function (b) { b.addEventListener('change', upd); });
  document.getElementById('pr').addEventListener('click', function () { window.print(); });
  upd();
})();
</script>'''
    return ('<!doctype html>\n<html lang="ko">\n<head>\n<meta charset="utf-8">\n<meta name="viewport" content="width=device-width, initial-scale=1">\n'
            '<title>WJbid 서비스 후보</title>\n'
            '<link rel="stylesheet" href="https://cdn.jsdelivr.net/gh/orioncactus/pretendard@v1.3.9/dist/web/variable/pretendardvariable.min.css">\n'
            f'<style>{css}</style>\n</head>\n<body>\n<div class="wrap">\n'
            '<h1>WJbid 서비스 후보 목록</h1>\n'
            f'<p class="lead">홈 시안에 넣은 서비스 {total}가지 전부입니다. 사이트에 넣을 것을 골라 체크해 주세요. 회원 요금은 월 50,000원 안팎으로 잡았습니다.</p>\n'
            f'<div class="leg">{leg}</div>\n' + '\n'.join(secs) +
            f'\n<div class="bar"><span>고른 서비스 <b id="cnt">0</b> / {total}</span><button id="pr" type="button">인쇄하기</button></div>\n</div>\n{script}\n</body>\n</html>\n')


os.makedirs(OUT, exist_ok=True)
for name, fn in (('홈시안_AIDA.html', page), ('서비스후보목록.html', sheet)):
    out = fn()
    bad = [c for c in ('—', '→', '←') if c in out]
    open(os.path.join(OUT, name), 'w', encoding='utf-8').write(out)
    print('wrote', name, len(out), 'bad' if bad else '')
base = page()
for k, lb, v in THEMES:
    out = (base.replace('</style>', theme_css(v) + '</style>', 1)
           .replace('<title>WJbid 홈 시안 AIDA</title>', f'<title>WJbid 홈 {lb}</title>')
           .replace('<strong>홈 시안 · AIDA</strong>', f'<strong>홈 시안 · {lb}</strong>'))
    open(os.path.join(OUT, f'홈시안_{k}.html'), 'w', encoding='utf-8').write(out)
    print('wrote', k, len(out))
open(os.path.join(OUT, '색상비교.html'), 'w', encoding='utf-8').write(compare())
