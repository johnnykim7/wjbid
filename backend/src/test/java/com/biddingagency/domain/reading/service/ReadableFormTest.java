package com.biddingagency.domain.reading.service;

import com.biddingagency.domain.reading.entity.ReadAs;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfReader;
import org.apache.poi.xwpf.usermodel.XWPFDocument;
import org.apache.poi.xwpf.usermodel.XWPFTable;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;

import javax.imageio.ImageIO;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.nio.charset.StandardCharsets;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/** TC-DOC-214~216, 219: 고객 서류를 읽는 꼴로 바꾸기 (CR-169, BIZ-086) */
class ReadableFormTest {

    @Test
    @DisplayName("TC-DOC-214 PDF_읽는꼴PDF")
    void PDF_읽는꼴PDF() {
        assertThat(List.of(ReadableForm.readAs("report.pdf", null), ReadableForm.workspaceExtension("report.pdf", null)))
                .containsExactly(ReadAs.PDF, "pdf");
    }

    @Test
    @DisplayName("TC-DOC-215 그림_한쪽PDF로내줌")
    void 그림_한쪽PDF로내줌() throws Exception {
        BufferedImage image = new BufferedImage(40, 20, BufferedImage.TYPE_INT_RGB);
        ByteArrayOutputStream png = new ByteArrayOutputStream();
        ImageIO.write(image, "png", png);

        ReadableForm.Rendered rendered = ReadableForm.render(png.toByteArray(), "license.png", "image/png");

        int pages;
        try (PdfDocument pdf = new PdfDocument(new PdfReader(new ByteArrayInputStream(rendered.content())))) {
            pages = pdf.getNumberOfPages();
        }
        assertThat(List.of(ReadableForm.readAs("license.png", "image/png"), rendered.mediaType(), pages))
                .containsExactly(ReadAs.PDF, "application/pdf", 1);
    }

    @Test
    @DisplayName("TC-DOC-216 DOCX_글자만텍스트로")
    void DOCX_글자만텍스트로() throws Exception {
        ByteArrayOutputStream docx = new ByteArrayOutputStream();
        try (XWPFDocument doc = new XWPFDocument()) {
            XWPFTable table = doc.createTable(2, 2);
            table.getRow(0).getCell(0).setText("상호");
            table.getRow(0).getCell(1).setText("우진");
            table.getRow(1).getCell(0).setText("대표");
            table.getRow(1).getCell(1).setText("김");
            doc.write(docx);
        }

        ReadableForm.Rendered rendered = ReadableForm.render(docx.toByteArray(), "profile.docx", null);

        assertThat(List.of(ReadableForm.readAs("profile.docx", null), rendered.mediaType(),
                new String(rendered.content(), StandardCharsets.UTF_8).contains("상호\t우진\n대표\t김")))
                .containsExactly(ReadAs.TEXT, "text/plain;charset=UTF-8", true);
    }

    @Test
    @DisplayName("TC-DOC-219 확장자없음_contentType으로정함")
    void 확장자없음_contentType으로정함() {
        assertThat(ReadableForm.readAs("scan", "image/jpeg")).isEqualTo(ReadAs.PDF);
    }

    @Test
    @DisplayName("글자서류_작업장에서는_txt")
    void 글자서류_작업장에서는_txt() {
        assertThat(List.of(ReadableForm.workspaceExtension("note.md", null), ReadableForm.workspaceExtension("list.csv", null),
                ReadableForm.workspaceExtension("profile.hwp", null)))
                .containsExactly("txt", "txt", "hwp");
    }

    @Test
    @DisplayName("옛형식_읽지않음")
    void 옛형식_읽지않음() {
        assertThat(List.of(ReadableForm.readAs("old.doc", "application/msword"), ReadableForm.readAs("book.xls", null)))
                .containsExactly(ReadAs.NONE, ReadAs.NONE);
    }
}
