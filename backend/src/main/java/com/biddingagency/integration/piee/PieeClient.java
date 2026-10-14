package com.biddingagency.integration.piee;

import lombok.extern.slf4j.Slf4j;
import org.apache.hc.client5.http.classic.methods.HttpGet;
import org.apache.hc.client5.http.classic.methods.HttpPost;
import org.apache.hc.client5.http.config.RequestConfig;
import org.apache.hc.client5.http.cookie.BasicCookieStore;
import org.apache.hc.client5.http.entity.UrlEncodedFormEntity;
import org.apache.hc.client5.http.impl.classic.CloseableHttpClient;
import org.apache.hc.client5.http.impl.classic.HttpClients;
import org.apache.hc.client5.http.protocol.HttpClientContext;
import org.apache.hc.client5.http.protocol.RedirectLocations;
import org.apache.hc.core5.http.HttpEntity;
import org.apache.hc.core5.http.NameValuePair;
import org.apache.hc.core5.http.io.entity.EntityUtils;
import org.apache.hc.core5.http.message.BasicNameValuePair;
import org.apache.hc.core5.util.Timeout;
import org.springframework.stereotype.Component;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.net.URI;
import java.net.URISyntaxException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

/**
 * PIEE 공개 공고 화면의 Download All Attachments 를 서버에서 누른다(BIZ-079, CR-160).
 *
 * <p>PIEE 는 입찰서류를 받는 정식 API 가 없어 브라우저가 보내는 요청 두 번을 그대로 보낸다.
 * 공고번호 주소를 열면 쿠키를 받고 공개 공고 화면으로 넘어간다. 같은 쿠키로 그 화면의 양식을 받기 단추 값으로 보내면 압축이 온다.
 * 로그인 없이 열리는 공고만 받는다. 화면이 바뀌면 아래 화면 이름들을 맞춘다.
 */
@Slf4j
@Component
public class PieeClient {

    /** 받기 결과. 실패는 {@link PieeFetchException} 으로 낸다 */
    public enum Outcome {
        /** 공고번호로 열면 PIEE 첫 화면으로 넘어간다 */
        NOT_FOUND,
        /** 공고 화면은 열리지만 받기 단추가 없다 */
        NO_ATTACHMENTS,
        /** 압축을 받아 target 에 썼다 */
        DOWNLOADED
    }

    static final String NOTICE_PAGE = "viewPublicNotice.xhtml";
    static final String DOWNLOAD_ALL_BUTTON = "downloadAllAttachments";
    static final String CLICKED_FIELD = "form:_idcl";

    private static final Pattern FORM = Pattern.compile("<form\\b[^>]*\\bid=\"form\"[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern ACTION = Pattern.compile("\\baction=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern INPUT = Pattern.compile("<input\\b[^>]*>", Pattern.CASE_INSENSITIVE);
    private static final Pattern HIDDEN = Pattern.compile("\\btype=\"hidden\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern NAME = Pattern.compile("\\bname=\"([^\"]+)\"", Pattern.CASE_INSENSITIVE);
    private static final Pattern VALUE = Pattern.compile("\\bvalue=\"([^\"]*)\"", Pattern.CASE_INSENSITIVE);
    private static final Timeout CONNECT_TIMEOUT = Timeout.ofSeconds(30);
    private static final int COPY_BUFFER = 64 * 1024;

    /**
     * 공고번호 주소를 열어 받기 단추를 누르고, 압축이면 target 에 쓴다.
     *
     * @param maxBytes 압축 무게 상한. 넘으면 받다가 끊는다
     * @param timeout  두 요청을 합친 시간 상한
     */
    public Outcome download(String noticeUrl, Path target, long maxBytes, Duration timeout) {
        long deadline = System.nanoTime() + timeout.toNanos();
        RequestConfig config = RequestConfig.custom()
                .setConnectionRequestTimeout(CONNECT_TIMEOUT)
                .setResponseTimeout(Timeout.of(timeout))
                .build();
        try (CloseableHttpClient client = HttpClients.custom()
                .setDefaultCookieStore(new BasicCookieStore())
                .setDefaultRequestConfig(config)
                .build()) {
            HttpClientContext context = HttpClientContext.create();
            HttpGet open = new HttpGet(noticeUrl);
            String html = client.execute(open, context, response -> {
                if (response.getCode() != 200) {
                    throw new PieeFetchException("공고 화면 응답 HTTP " + response.getCode());
                }
                return EntityUtils.toString(response.getEntity(), StandardCharsets.UTF_8);
            });
            URI pageUri = finalUri(open, context);
            Matcher form = FORM.matcher(html);
            if (!pageUri.getPath().endsWith(NOTICE_PAGE) || !form.find()) {
                log.info("[CR-160] PIEE 에 없는 공고: {} 에서 {} 로 넘어감", noticeUrl, pageUri);
                return Outcome.NOT_FOUND;
            }
            if (!html.contains("id=\"" + DOWNLOAD_ALL_BUTTON + "\"")) {
                return Outcome.NO_ATTACHMENTS;
            }
            HttpPost press = new HttpPost(actionUri(pageUri, form.group()));
            press.setEntity(new UrlEncodedFormEntity(formFields(html, form.end()), StandardCharsets.UTF_8));
            return client.execute(press, context, response -> {
                if (response.getCode() != 200) {
                    throw new PieeFetchException("받기 응답 HTTP " + response.getCode());
                }
                writeZip(response.getEntity(), target, maxBytes, deadline);
                return Outcome.DOWNLOADED;
            });
        } catch (IOException e) {
            throw new PieeFetchException("통신 오류 " + e.getClass().getSimpleName() + ": " + e.getMessage(), e);
        }
    }

    private void writeZip(HttpEntity entity, Path target, long maxBytes, long deadline) throws IOException {
        if (entity == null) {
            throw new PieeFetchException("받기 응답이 비어 있음");
        }
        if (entity.getContentLength() > maxBytes) {
            throw new PieeFetchException("압축 무게 상한을 넘음(" + entity.getContentLength() + " bytes)");
        }
        long written = 0;
        byte[] buffer = new byte[COPY_BUFFER];
        try (InputStream in = entity.getContent(); OutputStream out = Files.newOutputStream(target)) {
            int read;
            while ((read = in.read(buffer)) != -1) {
                written += read;
                if (written > maxBytes) {
                    throw new PieeFetchException("압축 무게 상한을 넘음(" + maxBytes + " bytes 초과)");
                }
                if (System.nanoTime() > deadline) {
                    throw new PieeFetchException("시간 상한을 넘음");
                }
                out.write(buffer, 0, read);
            }
        }
        if (!isZip(target)) {
            throw new PieeFetchException("받은 응답이 압축이 아님");
        }
    }

    private static boolean isZip(Path file) throws IOException {
        try (InputStream in = Files.newInputStream(file)) {
            byte[] head = in.readNBytes(4);
            return head.length == 4 && head[0] == 'P' && head[1] == 'K' && head[2] == 3 && head[3] == 4;
        }
    }

    private static URI finalUri(HttpGet request, HttpClientContext context) {
        RedirectLocations redirects = context.getRedirectLocations();
        if (redirects != null && redirects.size() > 0) {
            return redirects.get(redirects.size() - 1);
        }
        try {
            return request.getUri();
        } catch (URISyntaxException e) {
            throw new PieeFetchException("PIEE 공고 주소가 잘못됨: " + e.getMessage(), e);
        }
    }

    private static URI actionUri(URI pageUri, String formTag) {
        Matcher action = ACTION.matcher(formTag);
        if (!action.find()) {
            return pageUri;
        }
        return pageUri.resolve(unescape(action.group(1)));
    }

    /** 브라우저처럼 양식 안의 이름 있는 숨은 칸을 모두 보내고, 누른 단추를 알린다 */
    static List<NameValuePair> formFields(String html, int formStart) {
        int formEnd = html.indexOf("</form>", formStart);
        String body = html.substring(formStart, formEnd < 0 ? html.length() : formEnd);
        List<NameValuePair> fields = new ArrayList<>();
        Matcher input = INPUT.matcher(body);
        while (input.find()) {
            String tag = input.group();
            Matcher name = NAME.matcher(tag);
            if (!HIDDEN.matcher(tag).find() || !name.find()) {
                continue;
            }
            Matcher value = VALUE.matcher(tag);
            fields.add(new BasicNameValuePair(unescape(name.group(1)), value.find() ? unescape(value.group(1)) : ""));
        }
        fields.add(new BasicNameValuePair(CLICKED_FIELD, DOWNLOAD_ALL_BUTTON));
        return fields;
    }

    private static String unescape(String s) {
        return s.replace("&quot;", "\"").replace("&#39;", "'").replace("&lt;", "<").replace("&gt;", ">")
                .replace("&amp;", "&");
    }
}
