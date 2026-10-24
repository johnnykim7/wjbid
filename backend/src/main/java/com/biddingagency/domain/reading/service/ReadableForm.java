package com.biddingagency.domain.reading.service;

import com.biddingagency.domain.reading.entity.ReadAs;
import com.itextpdf.io.image.ImageData;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Image;
import org.apache.poi.ss.usermodel.Cell;
import org.apache.poi.ss.usermodel.DataFormatter;
import org.apache.poi.ss.usermodel.Row;
import org.apache.poi.ss.usermodel.Sheet;
import org.apache.poi.xslf.usermodel.XMLSlideShow;
import org.apache.poi.xslf.usermodel.XSLFShape;
import org.apache.poi.xslf.usermodel.XSLFSlide;
import org.apache.poi.xslf.usermodel.XSLFTable;
import org.apache.poi.xslf.usermodel.XSLFTableCell;
import org.apache.poi.xslf.usermodel.XSLFTableRow;
import org.apache.poi.xslf.usermodel.XSLFTextShape;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.apache.poi.xwpf.usermodel.IBodyElement;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFParagraph;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.apache.poi.xwpf.usermodel.XWPFTableCell;
import org.apache.poi.xwpf.usermodel.XWPFTableRow;
import org.springframework.http.MediaType;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Locale;
import java.util.Set;

/**
 * 고객 서류를 Aimbase 가 읽을 수 있는 꼴로 바꾼다 (CR-169, BIZ-086).
 *
 * Aimbase 의 큰 입력 읽기(LARGE_INPUT)는 PDF 와 텍스트만 받으므로, 그림은 한 쪽 PDF 로 감싸고
 * DOCX, XLSX, PPTX 는 글자만 뽑는다. HWP, HWPX 는 원본 그대로 주고 작업장에서 스킬 스크립트가 MD 로 바꾼다.
 * DOC, XLS, PPT 같은 옛 형식은 서버에 읽는 라이브러리가 없어 읽지 않는다.
 */
public final class ReadableForm {

    private static final Set<String> IMAGE_EXTENSIONS = Set.of("jpg", "jpeg", "png", "gif", "bmp", "tif", "tiff");
    private static final Set<String> OFFICE_EXTENSIONS = Set.of("docx", "xlsx", "pptx");
    private static final Set<String> TEXT_EXTENSIONS = Set.of("txt", "csv", "md");
    private static final Set<String> HWP_EXTENSIONS = Set.of("hwp", "hwpx");

    private static final String DOCX_TYPE = "application/vnd.openxmlformats-officedocument.wordprocessingml.document";
    private static final String XLSX_TYPE = "application/vnd.openxmlformats-officedocument.spreadsheetml.sheet";
    private static final String PPTX_TYPE = "application/vnd.openxmlformats-officedocument.presentationml.presentation";
    private static final Set<String> HWP_TYPES = Set.of(
            "application/x-hwp", "application/haansofthwp", "application/vnd.hancom.hwp",
            "application/hwp+zip", "application/vnd.hancom.hwpx");

    private ReadableForm() {
    }

    /** 확장자로 정하고, 확장자가 없으면 content_type 으로 정한다 */
    public static ReadAs readAs(String fileName, String contentType) {
        String ext = extension(fileName);
        if (ext != null) {
            if (ext.equals("pdf") || IMAGE_EXTENSIONS.contains(ext)) return ReadAs.PDF;
            if (OFFICE_EXTENSIONS.contains(ext) || TEXT_EXTENSIONS.contains(ext)) return ReadAs.TEXT;
            if (HWP_EXTENSIONS.contains(ext)) return ReadAs.HWP;
            return ReadAs.NONE;
        }
        return readAsByType(contentType);
    }

    /** 작업장에 받을 파일의 확장자. 그림은 pdf, DOCX, XLSX, PPTX 는 txt 다 */
    public static String workspaceExtension(String fileName, String contentType) {
        String kind = kind(fileName, contentType);
        // Aimbase 큰 문서 읽기 단계는 확장자로 형식을 정하고 글은 .txt 만 받는다(.md, .csv 는 못 읽음)
        return switch (kind) {
            case "image", "pdf" -> "pdf";
            case "hwp", "hwpx", "none" -> kind;
            default -> "txt";
        };
    }

    /** 읽는 꼴로 바꾼 바이트와 그 형식. NONE 이면 부르지 않는다 */
    public static Rendered render(byte[] content, String fileName, String contentType) {
        String kind = kind(fileName, contentType);
        try {
            return switch (kind) {
                case "image" -> new Rendered(imageToPdf(content), MediaType.APPLICATION_PDF_VALUE);
                case "docx" -> text(docxText(content));
                case "xlsx" -> text(xlsxText(content));
                case "pptx" -> text(pptxText(content));
                case "pdf" -> new Rendered(content, MediaType.APPLICATION_PDF_VALUE);
                case "txt", "csv", "md" -> new Rendered(content, MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8");
                case "hwp", "hwpx" -> new Rendered(content, MediaType.APPLICATION_OCTET_STREAM_VALUE);
                default -> throw new IllegalArgumentException("읽을 수 없는 형식: " + fileName);
            };
        } catch (IOException e) {
            throw new IllegalStateException("읽는 꼴로 바꾸지 못함: " + fileName, e);
        }
    }

    public record Rendered(byte[] content, String mediaType) {
    }

    private static String kind(String fileName, String contentType) {
        String ext = extension(fileName);
        if (ext != null) {
            if (IMAGE_EXTENSIONS.contains(ext)) return "image";
            if (ext.equals("pdf") || OFFICE_EXTENSIONS.contains(ext) || TEXT_EXTENSIONS.contains(ext)
                    || HWP_EXTENSIONS.contains(ext)) return ext;
            return "none";
        }
        String type = normalizedType(contentType);
        if (type == null) return "none";
        if (type.equals(MediaType.APPLICATION_PDF_VALUE)) return "pdf";
        if (type.startsWith("image/")) return "image";
        if (type.equals(DOCX_TYPE)) return "docx";
        if (type.equals(XLSX_TYPE)) return "xlsx";
        if (type.equals(PPTX_TYPE)) return "pptx";
        if (type.equals("text/csv")) return "csv";
        if (type.equals("text/markdown")) return "md";
        if (type.equals(MediaType.TEXT_PLAIN_VALUE)) return "txt";
        if (type.equals("application/vnd.hancom.hwpx") || type.equals("application/hwp+zip")) return "hwpx";
        if (HWP_TYPES.contains(type)) return "hwp";
        return "none";
    }

    private static ReadAs readAsByType(String contentType) {
        return switch (kind(null, contentType)) {
            case "pdf", "image" -> ReadAs.PDF;
            case "docx", "xlsx", "pptx", "txt", "csv", "md" -> ReadAs.TEXT;
            case "hwp", "hwpx" -> ReadAs.HWP;
            default -> ReadAs.NONE;
        };
    }

    private static String extension(String fileName) {
        if (fileName == null) return null;
        int dot = fileName.lastIndexOf('.');
        if (dot < 0 || dot == fileName.length() - 1) return null;
        return fileName.substring(dot + 1).toLowerCase(Locale.ROOT);
    }

    private static String normalizedType(String contentType) {
        if (contentType == null || contentType.isBlank()) return null;
        int semi = contentType.indexOf(';');
        String type = semi < 0 ? contentType : contentType.substring(0, semi);
        return type.trim().toLowerCase(Locale.ROOT);
    }

    private static Rendered text(String body) {
        return new Rendered(body.getBytes(StandardCharsets.UTF_8), MediaType.TEXT_PLAIN_VALUE + ";charset=UTF-8");
    }

    /** 그림 크기 그대로 한 쪽을 만든다. 글자 인식은 Aimbase 가 쪽 그림을 보고 한다 */
    private static byte[] imageToPdf(byte[] content) {
        ImageData data = ImageDataFactory.create(content);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        PdfDocument pdf = new PdfDocument(new PdfWriter(out));
        try (Document document = new Document(pdf, new PageSize(data.getWidth(), data.getHeight()))) {
            document.setMargins(0, 0, 0, 0);
            document.add(new Image(data));
        }
        return out.toByteArray();
    }

    private static String docxText(byte[] content) throws IOException {
        try (XWPFDocument doc = new XWPFDocument(new ByteArrayInputStream(content))) {
            List<String> lines = new ArrayList<>();
            for (IBodyElement element : doc.getBodyElements()) {
                if (element instanceof XWPFParagraph p) {
                    lines.add(p.getText());
                } else if (element instanceof XWPFTable table) {
                    for (XWPFTableRow row : table.getRows()) {
                        List<String> cells = new ArrayList<>();
                        for (XWPFTableCell cell : row.getTableCells()) {
                            cells.add(oneLine(cell.getText()));
                        }
                        lines.add(String.join("\t", cells));
                    }
                }
            }
            return String.join("\n", lines);
        }
    }

    private static String xlsxText(byte[] content) throws IOException {
        DataFormatter formatter = new DataFormatter(Locale.ROOT);
        try (XSSFWorkbook workbook = new XSSFWorkbook(new ByteArrayInputStream(content))) {
            List<String> lines = new ArrayList<>();
            for (Sheet sheet : workbook) {
                lines.add("[" + sheet.getSheetName() + "]");
                for (Row row : sheet) {
                    List<String> cells = new ArrayList<>();
                    for (int i = 0; i < row.getLastCellNum(); i++) {
                        Cell cell = row.getCell(i);
                        cells.add(cell == null ? "" : oneLine(formatter.formatCellValue(cell)));
                    }
                    lines.add(String.join("\t", cells));
                }
            }
            return String.join("\n", lines);
        }
    }

    private static String pptxText(byte[] content) throws IOException {
        try (XMLSlideShow show = new XMLSlideShow(new ByteArrayInputStream(content))) {
            List<String> lines = new ArrayList<>();
            int no = 1;
            for (XSLFSlide slide : show.getSlides()) {
                lines.add("[" + no++ + "]");
                for (XSLFShape shape : slide.getShapes()) {
                    if (shape instanceof XSLFTextShape textShape) {
                        lines.add(textShape.getText());
                    } else if (shape instanceof XSLFTable table) {
                        for (XSLFTableRow row : table.getRows()) {
                            List<String> cells = new ArrayList<>();
                            for (XSLFTableCell cell : row.getCells()) {
                                cells.add(oneLine(cell.getText()));
                            }
                            lines.add(String.join("\t", cells));
                        }
                    }
                }
            }
            return String.join("\n", lines);
        }
    }

    /** 칸 안의 줄바꿈과 탭이 표의 줄과 칸 경계를 흐리지 않게 한 줄로 편다 */
    private static String oneLine(String text) {
        return text == null ? "" : text.replaceAll("[\\t\\r\\n]+", " ").trim();
    }
}
