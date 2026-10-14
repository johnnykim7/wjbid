package com.biddingagency.domain.opportunity.service;

import com.sun.net.httpserver.HttpExchange;
import com.sun.net.httpserver.HttpServer;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.net.InetSocketAddress;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

/**
 * PIEE 공개 공고 화면 흉내(CR-160). 공고번호 주소는 쿠키를 주고 공고 화면으로 넘기고,
 * 공고 화면 양식을 같은 쿠키와 받기 단추 값으로 보내면 압축을 준다.
 */
class FakePieeServer implements AutoCloseable {

    static final String LINK_PATH = "/sol/xhtml/unauth/search/oppMgmtLink.xhtml";
    static final String NOTICE_PATH = "/sol/xhtml/unauth/search/viewPublicNotice.xhtml";
    static final String INDEX_PATH = "/sol/xhtml/unauth/index.xhtml";
    private static final String SESSION = "JSESSIONID=fake-session";

    private final HttpServer server;
    final AtomicInteger postCount = new AtomicInteger();
    volatile boolean noticeExists = true;
    volatile boolean downloadButton = true;
    volatile byte[] postBody;
    volatile String lastPostForm;

    FakePieeServer() throws IOException {
        server = HttpServer.create(new InetSocketAddress("127.0.0.1", 0), 0);
        server.createContext(LINK_PATH, ex -> {
            ex.getResponseHeaders().add("Set-Cookie", SESSION + "; Path=/sol");
            ex.getResponseHeaders().add("Location", noticeExists ? NOTICE_PATH : INDEX_PATH);
            send(ex, 302, "moved".getBytes(StandardCharsets.UTF_8), "text/html");
        });
        server.createContext(INDEX_PATH, ex -> send(ex, 200, "<html><body>PIEE</body></html>".getBytes(), "text/html"));
        server.createContext(NOTICE_PATH, ex -> {
            if ("POST".equals(ex.getRequestMethod())) {
                postCount.incrementAndGet();
                lastPostForm = new String(ex.getRequestBody().readAllBytes(), StandardCharsets.UTF_8);
                String cookie = ex.getRequestHeaders().getFirst("Cookie");
                boolean pressed = lastPostForm.contains("form%3A_idcl=downloadAllAttachments")
                        && lastPostForm.contains("javax.faces.ViewState=view%26state")
                        && cookie != null && cookie.contains(SESSION);
                if (pressed) {
                    send(ex, 200, postBody, "application/zip");
                } else {
                    send(ex, 200, noticePage().getBytes(StandardCharsets.UTF_8), "text/html");
                }
                return;
            }
            send(ex, 200, noticePage().getBytes(StandardCharsets.UTF_8), "text/html");
        });
        server.start();
    }

    String noticeUrl(String solNo) {
        return "http://127.0.0.1:" + server.getAddress().getPort() + LINK_PATH + "?solNo=" + solNo;
    }

    private String noticePage() {
        return "<html><body>"
                + "<form id=\"form\" name=\"form\" method=\"post\" action=\"" + NOTICE_PATH + "\">"
                + "<input type=\"hidden\" name=\"form_SUBMIT\" value=\"1\" />"
                + "<input type=\"hidden\" name=\"javax.faces.ViewState\" value=\"view&amp;state\" />"
                + "<input type=\"text\" name=\"visibleField\" value=\"typed\" />"
                + (downloadButton ? "<a id=\"downloadAllAttachments\" href=\"#\">Download All Attachments</a>" : "")
                + "</form></body></html>";
    }

    private static void send(HttpExchange ex, int status, byte[] body, String contentType) throws IOException {
        ex.getResponseHeaders().add("Content-Type", contentType);
        ex.sendResponseHeaders(status, body.length);
        try (OutputStream out = ex.getResponseBody()) {
            out.write(body);
        }
    }

    static byte[] zip(Map<String, byte[]> files) throws IOException {
        ByteArrayOutputStream bytes = new ByteArrayOutputStream();
        try (ZipOutputStream zip = new ZipOutputStream(bytes)) {
            for (Map.Entry<String, byte[]> f : files.entrySet()) {
                zip.putNextEntry(new ZipEntry(f.getKey()));
                zip.write(f.getValue());
                zip.closeEntry();
            }
        }
        return bytes.toByteArray();
    }

    static Map<String, byte[]> files(Object... nameAndText) {
        Map<String, byte[]> files = new LinkedHashMap<>();
        for (int i = 0; i < nameAndText.length; i += 2) {
            files.put((String) nameAndText[i], ((String) nameAndText[i + 1]).getBytes(StandardCharsets.UTF_8));
        }
        return files;
    }

    @Override
    public void close() {
        server.stop(0);
    }
}
