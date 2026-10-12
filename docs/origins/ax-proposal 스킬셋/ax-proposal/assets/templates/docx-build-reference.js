// docx-build-reference.js — AX 표준 서식 v2 조립 참조 예시
// 표지(페이지 프레임 섹션) → 목차 → 간지(풀블리드 섹션) → 본문(음영 제목,
// headlineBox 주제문, □○− 개조식, 상하선 표, 핵심 우려 대응 박스, 도식+Action Caption)
// 실제 제안서 작성 시 이 구조를 그대로 확장한다. 스타일 정의는 docx-style.js만 사용.

const docx = require("docx");
const fs = require("fs");
const S = require("./docx-style");
const {
  Document, Packer, Paragraph, TextRun, Table, TableRow, TableCell,
  WidthType, ShadingType, AlignmentType, HeadingLevel, PageBreak,
  Header, Footer, PageNumber, ImageRun, BorderStyle, VerticalAlign,
  PositionalTab, PositionalTabAlignment, PositionalTabRelativeTo, PositionalTabLeader,
  TabStopType,
} = docx;

const PROJECT = "○○공사 AI 에이전트 기반 업무 자동화 플랫폼 구축";
const NOTICE = "공고 제2026-000호";

const run = (text, opts={}) => new TextRun({ text, ...opts });

function buildDoc(profile){
  const isPub = profile==="public";
  const bf = S.bodyFont(profile), bs = S.bodySize(profile);
  const accent = isPub ? S.COLORS.black : S.COLORS.navy;

  // ---------- 머리글/바닥글 ----------
  const header = new Header({ children:[ new Paragraph({
    style:"AXHeaderText",
    tabStops:[{ type:TabStopType.RIGHT, position:S.PAGE.bodyWidthDXA }],
    border:{ bottom:{ color:S.COLORS.line, size:4, style:BorderStyle.SINGLE, space:4 } },
    children:[ run(PROJECT), new TextRun({ text:"\t주식회사 에이엑스" }) ],
  })]});
  const footer = new Footer({ children:[ new Paragraph({
    alignment: AlignmentType.CENTER,
    children:[ new TextRun({ font:S.FONTS.heading, size:18, color:S.COLORS.gray,
      children:["- ", PageNumber.CURRENT, " -"] }) ],
  })]});
  const empty = { header:new Header({children:[]}), footer:new Footer({children:[]}) };

  // ---------- 표지 (별도 섹션: 페이지 테두리 프레임) ----------
  const cover = [];
  cover.push(new Paragraph({ spacing:{ before:2200 }, alignment:AlignmentType.CENTER,
    children:[ new TextRun({ text:NOTICE, font:S.FONTS.heading, size:22, color:S.COLORS.gray }) ] }));
  cover.push(new Paragraph({ spacing:{ before:600 }, alignment:AlignmentType.CENTER,
    children:[ new TextRun({ text:PROJECT, font:S.FONTS.heading, size:48, bold:true, color:accent }) ] }));
  cover.push(new Paragraph({ spacing:{ before:500 }, alignment:AlignmentType.CENTER,
    children:[ new TextRun({ text:"기 술 제 안 서", font:S.FONTS.heading, size:40, bold:true, color:S.COLORS.black }) ] }));
  cover.push(new Paragraph({ spacing:{ before:4400 }, alignment:AlignmentType.CENTER,
    children:[ new TextRun({ text:"2026.  7.", font:S.FONTS.heading, size:28, color:S.COLORS.text }) ] }));
  cover.push(new Paragraph({ spacing:{ before:400 }, alignment:AlignmentType.CENTER,
    children:[ new TextRun({ text:"주식회사 에이엑스", font:S.FONTS.heading, size:32, bold:true, color:S.COLORS.black }) ] }));

  // ---------- 목차 ----------
  const tocLine = (num, title, page, level) => new Paragraph({
    spacing:{ after: level===0?180:110, before: level===0?220:0 },
    indent:{ left: level*340 },
    children:[
      new TextRun({ text:`${num}  ${title}`, font:S.FONTS.heading,
        size: level===0?24:22, bold:level===0, color:S.COLORS.text }),
      new TextRun({ font:S.FONTS.heading, size:22, color:S.COLORS.gray,
        children:[ new PositionalTab({ alignment:PositionalTabAlignment.RIGHT,
          relativeTo:PositionalTabRelativeTo.MARGIN, leader:PositionalTabLeader.DOT }), ` ${page}` ] }),
    ],
  });
  const toc = [
    new Paragraph({ alignment:AlignmentType.CENTER, spacing:{ after:500 },
      children:[ new TextRun({ text:"목    차", font:S.FONTS.heading, size:36, bold:true, color:S.COLORS.black }) ],
      border:{ bottom:{ color:S.COLORS.black, size:8, style:BorderStyle.SINGLE, space:12 } } }),
    tocLine("Ⅰ.","사업의 이해","1",0),
    tocLine("1.","추진 배경 및 필요성","1",1),
    tocLine("2.","사업 범위 및 요구사항 분석","4",1),
    tocLine("Ⅱ.","사업 수행 전략","7",0),
    tocLine("1.","제안 전략 및 차별화 방안","7",1),
    tocLine("Ⅲ.","기술 부문","12",0),
    tocLine("Ⅳ.","사업 관리 부문","24",0),
  ];

  // ---------- 간지 (풀블리드 배경 이미지, 별도 섹션) ----------
  const ganjiImg = fs.readFileSync(`diagrams/ganji-1-${isPub?"pub":"ent"}.png`);
  const ganji = [ new Paragraph({ children:[ new ImageRun({
    type:"png", data:ganjiImg,
    transformation:{ width:794, height:1123 },  // A4 전면 (96dpi 기준)
    floating:{
      horizontalPosition:{ relative: docx.HorizontalPositionRelativeFrom.PAGE, offset: 0 },
      verticalPosition:{ relative: docx.VerticalPositionRelativeFrom.PAGE, offset: 0 },
      behindDocument: true, allowOverlap: true,
    },
  }) ] }) ];

  // ---------- 본문 ----------
  const body = [];
  body.push(new Paragraph({ style:"AXH2", heading:HeadingLevel.HEADING_2,
    children:[ run("  1. 추진 배경 및 필요성") ] }));

  // 주제문 박스 (합쇼체 완결문) — 본문 폭 정렬 무테 박스
  body.push(S.headlineBox(docx, profile, "귀 기관은 검증된 업무 DNA 클로닝 방법론으로 반복 업무 처리시간을 70% 단축하고, 월 126시간의 핵심 인력 여력을 확보하게 됩니다."));
  body.push(new Paragraph({ spacing:{ after:120 }, children:[] }));

  // 개조식 전개 (□ ○ -)
  const spec = S.gaejoSpec(profile);
  const g = (level, text) => new Paragraph({
    style:"AXBody", numbering:{ reference:"ax-gaejo", level },
    alignment:AlignmentType.LEFT,
    spacing:{ after: level===0?100:60, line:312, lineRule:"auto" },
    children:[ run(text, { size:spec[level].size, bold:spec[level].bold, color:spec[level].color }) ],
  });
  body.push(g(0,"(현황) 3개 부서 반복 업무가 전체 업무시간의 42%를 차지"));
  body.push(g(1,"월 단위 정산·보고 업무의 87%가 시스템 간 수작업 데이터 이관으로 처리됨"));
  body.push(g(1,"담당자별 처리 방식 상이로 동일 업무의 처리시간 편차 최대 3.2배 발생"));
  body.push(g(2,"숙련 인력 퇴직 시 업무 지식 유실 — 최근 2년간 인수인계 공백 사례 6건"));
  body.push(g(0,"(필요성) 표준 절차 기반 자동 실행 체계로의 전환이 시급"));
  body.push(g(1,"업무 지식을 조직 자산으로 축적하여 인력 변동에도 업무 연속성 확보 필요"));
  body.push(g(1,"기간계·그룹웨어 등 기존 시스템과의 실시간 연동 체계 구축 필요"));

  // ※ 참고
  body.push(new Paragraph({ style:"AXNote",
    children:[ run("※ 현황 수치는 2026. 5월 제안사 사전 진단(3개 부서, 업무 47종) 결과 기준") ] }));

  // 표 — 캡션 상단, HWP식 무채 표
  body.push(new Paragraph({ style:"AXCaption", spacing:{ before:200, after:80 },
    children:[
      new TextRun({ text:"< 표 1 >  ", bold:true, font:S.FONTS.heading, size:20, color:S.COLORS.text }),
      new TextRun({ text:"자동화 대상 업무 선정 결과", font:S.FONTS.heading, size:20, color:S.COLORS.text }),
    ] }));
  const colW=[2680,3040,1900,1905];
  const th=(t,w)=> new TableCell({
    width:{ size:w, type:WidthType.DXA }, verticalAlign:VerticalAlign.CENTER,
    shading:{ type:ShadingType.CLEAR, fill:S.tableHeadFill(profile) },
    margins:{ top:90, bottom:90, left:80, right:80 },
    children:[ new Paragraph({ alignment:AlignmentType.CENTER,
      children:[ new TextRun({ text:t, font:S.FONTS.heading, size:20, bold:true, color:S.COLORS.black }) ] }) ],
  });
  const td=(t,w,center)=> new TableCell({
    width:{ size:w, type:WidthType.DXA }, verticalAlign:VerticalAlign.CENTER,
    margins:{ top:90, bottom:90, left:80, right:80 },
    children:[ new Paragraph({ alignment:center?AlignmentType.CENTER:AlignmentType.LEFT,
      children:[ new TextRun({ text:t, font:S.FONTS.heading, size:20, color:S.COLORS.text }) ] }) ],
  });
  body.push(new Table({
    columnWidths:colW, width:{ size:9525, type:WidthType.DXA },
    borders:S.tableBorders(docx),
    rows:[
      new TableRow({ tableHeader:true, children:[ th("대상 업무",colW[0]), th("현행 방식",colW[1]), th("전환 방식",colW[2]), th("절감 효과",colW[3]) ] }),
      new TableRow({ children:[ td("월 정산 보고",colW[0]), td("4개 시스템 수작업 취합",colW[1]), td("AWorker 자동 실행",colW[2],true), td("월 48시간",colW[3],true) ] }),
      new TableRow({ children:[ td("계약 현황 관리",colW[0]), td("담당자 개별 엑셀 관리",colW[1]), td("K-MCP 실시간 연동",colW[2],true), td("월 52시간",colW[3],true) ] }),
      new TableRow({ children:[ td("민원 1차 분류",colW[0]), td("전건 수동 검토·배정",colW[1]), td("에이전트 자동 분류",colW[2],true), td("월 26시간",colW[3],true) ] }),
    ],
  }));

  // 핵심 우려 대응 박스 — 헤더행 음영 + 전체 테두리 (2행 표)
  body.push(new Paragraph({ spacing:{ before:240 }, children:[] }));
  const boxBorder = { style:BorderStyle.SINGLE, size:6, color:S.COLORS.border };
  const noB = { style:BorderStyle.NONE, size:0, color:"FFFFFF" };
  body.push(new Table({
    columnWidths:[9525], width:{ size:9525, type:WidthType.DXA },
    borders:{ top:boxBorder, bottom:boxBorder, left:noB, right:noB,
      insideHorizontal:{ style:BorderStyle.SINGLE, size:4, color:S.COLORS.line },
      insideVertical:noB },
    rows:[
      new TableRow({ children:[ new TableCell({
        width:{ size:9525, type:WidthType.DXA },
        shading:{ type:ShadingType.CLEAR, fill:S.tableHeadFill(profile) },
        margins:{ top:80, bottom:80, left:140, right:140 },
        children:[ new Paragraph({ children:[
          new TextRun({ text:"【 핵심 우려 대응 】  담당 인력 없이 운영이 가능한가?", font:S.FONTS.heading, size:21, bold:true, color:S.COLORS.black }) ] }) ],
      }) ] }),
      new TableRow({ children:[ new TableCell({
        width:{ size:9525, type:WidthType.DXA },
        margins:{ top:120, bottom:120, left:140, right:140 },
        children:[ new Paragraph({ spacing:{ line:312, lineRule:"auto" }, children:[
          new TextRun({ text:"구축 후 3개월 안정화 기간 동안 전담 인력을 상주 배치하고, 현업 담당자 대상 에이전트 운영 교육 12시간을 제공합니다. 운영 이관 후에도 원격 관제로 에이전트 실행 성공률 ", font:bf, size:bs, color:S.COLORS.text }),
          new TextRun({ text:"99% 이상을 SLA로 보장", font:bf, size:bs, bold:true, underline:{}, color:S.COLORS.text }),
          new TextRun({ text:"합니다.", font:bf, size:bs, color:S.COLORS.text }),
        ] }) ],
      }) ] }),
    ],
  }));

  // 도식 + 캡션 (그림 캡션은 하단)
  // 도식 삽입 규격: 폭 635px 고정(=168mm), 높이는 PNG 실측 비율로 계산
  // const {width:iw, height:ih} = require('image-size')(path) 또는 PIL로 사전 확인
  const img = fs.readFileSync("diagrams/D-1.1-asis-tobe.png");
  const imgW = 1984, imgH = 876; // ← 캡처 PNG 실측값으로 교체
  body.push(new Paragraph({ spacing:{ before:280 }, alignment:AlignmentType.CENTER,
    children:[ new ImageRun({ type:"png", data:img,
      transformation:{ width:635, height:Math.round(635*imgH/imgW) } }) ] }));
  body.push(new Paragraph({ style:"AXCaption",
    children:[
      new TextRun({ text:"< 그림 1 >  ", bold:true, font:S.FONTS.heading, size:20, color:S.COLORS.text }),
      new TextRun({ text:"업무 DNA 클로닝 기반 전환으로 반복 업무 처리시간 70% 단축", font:S.FONTS.heading, size:20, color:S.COLORS.text }),
    ] }));

  return new Document({
    styles: S.buildStyles(docx, profile),
    numbering: S.buildNumbering(docx, profile),
    sections:[
      { // 표지: 페이지 테두리 프레임 (페이지 가장자리 기준 24pt 안쪽)
        properties:{ page:{ margin:S.PAGE.margin,
          borders:{
            pageBorders:{ display: docx.PageBorderDisplay.ALL_PAGES,
                          offsetFrom: docx.PageBorderOffsetFrom.PAGE,
                          zOrder: docx.PageBorderZOrder.FRONT },
            pageBorderTop:{ style:BorderStyle.SINGLE, size:12, color:S.COLORS.black, space:24 },
            pageBorderBottom:{ style:BorderStyle.SINGLE, size:12, color:S.COLORS.black, space:24 },
            pageBorderLeft:{ style:BorderStyle.SINGLE, size:12, color:S.COLORS.black, space:24 },
            pageBorderRight:{ style:BorderStyle.SINGLE, size:12, color:S.COLORS.black, space:24 },
          } } },
        headers:{ default:empty.header }, footers:{ default:empty.footer },
        children: cover,
      },
      { properties:{ page:{ margin:S.PAGE.margin } },
        headers:{ default:header }, footers:{ default:footer },
        children: toc,
      },
      { // 간지: 풀블리드 — 머리글·바닥글·여백 영향 없음
        properties:{ page:{ margin:S.PAGE.margin } },
        headers:{ default:empty.header }, footers:{ default:empty.footer },
        children: ganji,
      },
      { properties:{ page:{ margin:S.PAGE.margin } },
        headers:{ default:header }, footers:{ default:footer },
        children: body,
      },
    ],
  });
}

(async()=>{
  for(const p of ["public","enterprise"]){
    const buf = await Packer.toBuffer(buildDoc(p));
    fs.writeFileSync(`AX-제안서표준v2-샘플-${p==="public"?"공공":"기업"}.docx`, buf);
    console.log("built:", p);
  }
})();
