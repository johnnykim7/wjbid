// AX 제안서 DOCX 표준 스타일 세트 v2.1 — 한국 인쇄물 관행 기반
// v2.1 변경: 줄간격 140%, 표 좌우 외곽선 제거, 주제문 박스 무테(음영만),
//            전체 폰트 Pretendard 통일, 개조식 레벨별 크기·굵기 차등
// 주의: Pretendard는 수신자 PC에 설치가 필요 — 대외 제출은 PDF 병행 필수

const COLORS = {
  navy:  "1E3A5F",
  black: "000000",
  text:  "111111",
  sub:   "444444",   // 개조식 3레벨(-) 텍스트
  gray:  "595959",
  shade: "E9E9E9",
  shadeNavy: "DCE4EC",
  boxfill: "F2F3F5", // 주제문 박스 (무테이므로 반 톤 진하게)
  line:  "808080",
  border:"404040",
};

const FONTS = {
  base: { ascii: "Pretendard", eastAsia: "Pretendard", hAnsi: "Pretendard" },
  semi: { ascii: "Pretendard SemiBold", eastAsia: "Pretendard SemiBold", hAnsi: "Pretendard SemiBold" },
};

const PAGE = {
  margin: { top: 1134, bottom: 1021, left: 1247, right: 1134, header: 680, footer: 680 },
  bodyWidthDXA: 9525,
  bodyWidthPx: 635,
};

const LINE_130 = { line: 312, lineRule: "auto" };  // 줄간격 130%

function bodyFont(){ return FONTS.base; }
function bodySize(profile){ return profile==="enterprise" ? 22 : 24; } // 11pt / 12pt

// 개조식 레벨별 크기·굵기 차등 (half-points)
function gaejoSpec(profile){
  const isPub = profile==="public";
  return [
    { size: isPub?25:23, bold:true,  font:FONTS.base, color:COLORS.black }, // □
    { size: isPub?23:21, bold:false, font:FONTS.base, color:COLORS.text  }, // ○
    { size: isPub?21:20, bold:false, font:FONTS.base, color:COLORS.sub   }, // -
  ];
}

function buildStyles(docx, profile){
  const bs = bodySize(profile);
  const isPub = profile==="public";
  return {
    default: { document: { run: { font: FONTS.base, size: bs, color: COLORS.text } } },
    paragraphStyles: [
      { id:"AXH2", name:"AX-H2", basedOn:"Normal", next:"AXBody",
        run:{ font:FONTS.base, size:30, bold:true, color: isPub? COLORS.black : "FFFFFF" },
        paragraph:{ outlineLevel:1,
          spacing:{ before:360, after:280, ...LINE_130 },
          shading:{ type:docx.ShadingType.CLEAR, fill: isPub? COLORS.shade : COLORS.navy },
          // 음영색 동일 테두리 = 보이지 않는 상하 패딩 (space 단위: pt)
          border:{
            top:{ color: isPub? COLORS.shade : COLORS.navy, size:2, style:docx.BorderStyle.SINGLE, space:7 },
            bottom:{ color: isPub? COLORS.shade : COLORS.navy, size:2, style:docx.BorderStyle.SINGLE, space:7 },
          } },
      },
      { id:"AXH3", name:"AX-H3", basedOn:"Normal", next:"AXBody",
        run:{ font:FONTS.semi, size:26, color:COLORS.black },
        paragraph:{ outlineLevel:2, spacing:{ before:280, after:140 } },
      },

      { id:"AXBody", name:"AX-Body", basedOn:"Normal",
        run:{ font:FONTS.base, size:bs },
        paragraph:{ spacing:{ after:120, ...LINE_130 }, alignment:docx.AlignmentType.JUSTIFIED },
      },
      { id:"AXCaption", name:"AX-Caption", basedOn:"Normal",
        run:{ font:FONTS.base, size:20, color:COLORS.gray },
        paragraph:{ spacing:{ before:80, after:240 }, alignment:docx.AlignmentType.CENTER },
      },
      { id:"AXHeaderText", name:"AX-HeaderText", basedOn:"Normal",
        run:{ font:FONTS.base, size:18, color:COLORS.gray } },
      { id:"AXNote", name:"AX-Note", basedOn:"Normal",
        run:{ font:FONTS.base, size:bs-2, color:COLORS.gray },
        paragraph:{ spacing:{ after:120, line:280, lineRule:"auto" }, indent:{ left:340 } } },
    ],
  };
}

// 개조식 □ ○ - : 기호도 레벨별 크기·굵기 동조
function buildNumbering(docx, profile){
  const spec = gaejoSpec(profile);
  const lv = (level, text, left) => ({
    level, format: docx.LevelFormat.BULLET, text,
    alignment: docx.AlignmentType.LEFT,
    style:{ paragraph:{ indent:{ left, hanging:280 } },
            run:{ size:spec[level].size, bold:spec[level].bold, color:spec[level].color } },
  });
  return { config:[{ reference:"ax-gaejo",
    levels:[ lv(0,"□",280), lv(1,"○",640), lv(2,"-",1000) ] }]};
}

// 표: 상하 굵은 외곽선만, 좌우 무선. 내부 가로 0.5pt, 세로 0.5pt 회색
function tableBorders(docx){
  const thick = { style:docx.BorderStyle.SINGLE, size:12, color:COLORS.black };
  const inner = { style:docx.BorderStyle.SINGLE, size:4,  color:COLORS.line };
  const none  = { style:docx.BorderStyle.NONE, size:0, color:"FFFFFF" };
  return { top:thick, bottom:thick, left:none, right:none,
           insideHorizontal:inner, insideVertical:inner };
}
function tableHeadFill(profile){ return profile==="public" ? COLORS.shade : COLORS.shadeNavy; }

// 주제문 박스: 무테 1셀 표 — 본문 컬럼(9525 DXA)과 정확히 정렬, 셀 여백으로 상하 패딩
function headlineBox(docx, profile, text){
  const none = { style:docx.BorderStyle.NONE, size:0, color:"FFFFFF" };
  return new docx.Table({
    columnWidths:[PAGE.bodyWidthDXA],
    width:{ size:PAGE.bodyWidthDXA, type:docx.WidthType.DXA },
    borders:{ top:none, bottom:none, left:none, right:none,
              insideHorizontal:none, insideVertical:none },
    rows:[ new docx.TableRow({ children:[ new docx.TableCell({
      width:{ size:PAGE.bodyWidthDXA, type:docx.WidthType.DXA },
      shading:{ type:docx.ShadingType.CLEAR, fill:COLORS.boxfill },
      margins:{ top:170, bottom:170, left:170, right:170 },
      children:[ new docx.Paragraph({
        spacing:{ line:300, lineRule:"auto" },
        children:[ new docx.TextRun({ text, font:FONTS.base, size:24, bold:true, color:COLORS.black }) ],
      }) ],
    }) ] }) ],
  });
}

module.exports = { COLORS, FONTS, PAGE, LINE_130, bodyFont, bodySize, gaejoSpec,
                   buildStyles, buildNumbering, tableBorders, tableHeadFill, headlineBox };
