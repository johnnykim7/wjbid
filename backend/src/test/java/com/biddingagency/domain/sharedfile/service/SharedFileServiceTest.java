package com.biddingagency.domain.sharedfile.service;

import com.biddingagency.domain.member.entity.Member;
import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import com.biddingagency.domain.sharedfile.SharedFileError;
import com.biddingagency.domain.sharedfile.SharedFileException;
import com.biddingagency.domain.sharedfile.entity.SharedFile;
import com.biddingagency.domain.sharedfile.entity.SharedFileLog;
import com.biddingagency.domain.sharedfile.repository.SharedFileLogRepository;
import com.biddingagency.domain.sharedfile.repository.SharedFileRepository;
import com.biddingagency.dto.SharedFileResponses;
import com.biddingagency.integration.notification.BpNotificationClient;
import com.biddingagency.integration.storage.StorageService;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.data.domain.PageImpl;
import org.springframework.data.domain.Pageable;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.test.util.ReflectionTestUtils;
import org.springframework.web.multipart.MultipartFile;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.UncheckedIOException;
import java.lang.reflect.Field;
import java.nio.charset.StandardCharsets;
import java.time.Clock;
import java.time.Instant;
import java.time.ZoneId;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.Optional;
import java.util.Random;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.catchThrowableOfType;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyMap;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.BDDMockito.given;
import static org.mockito.BDDMockito.willAnswer;
import static org.mockito.Mockito.mock;

/** 복호화까지 실제로 돌려 보려고 암호화는 진짜를 쓰고, 저장소와 메일만 가짜로 둔다. */
class SharedFileServiceTest {

    private static final ZoneId ZONE = ZoneId.of("Asia/Seoul");
    private static final Instant T0 = Instant.parse("2026-11-10T01:00:00Z");
    private static final String KNOWN_TEXT = "TOP-SECRET-BID-CONTENT";
    private static final String IP = "10.0.0.1";
    private static final int CHUNK_WITH_TAG = SharedFileCrypto.CHUNK_BYTES + 16;

    private final Map<UUID, SharedFile> rows = new LinkedHashMap<>();
    private final List<SharedFileLog> logs = new ArrayList<>();
    private final FakeStorage storage = new FakeStorage();
    private final List<String> mailedPasswords = new ArrayList<>();

    private final SharedFileRepository fileRepository = mock(SharedFileRepository.class);
    private final SharedFileLogRepository logRepository = mock(SharedFileLogRepository.class);
    private final SystemSettingService settings = mock(SystemSettingService.class);
    private final BpNotificationClient mail = mock(BpNotificationClient.class);
    private final SharedFileCrypto crypto = new SharedFileCrypto();

    private Member alice;
    private Member bob;
    private SharedFileService service;

    @BeforeEach
    void setUp() {
        alice = member("alice@example.com");
        bob = member("bob@example.com");
        given(fileRepository.save(any(SharedFile.class))).willAnswer(inv -> {
            SharedFile file = inv.getArgument(0);
            if (file.getId() == null) {
                ReflectionTestUtils.setField(file, "id", UUID.randomUUID());
            }
            rows.put(file.getId(), file);
            return file;
        });
        given(fileRepository.findById(any(UUID.class))).willAnswer(inv -> Optional.ofNullable(rows.get(inv.<UUID>getArgument(0))));
        given(fileRepository.findByIdForUpdate(any(UUID.class))).willAnswer(inv -> Optional.ofNullable(rows.get(inv.<UUID>getArgument(0))));
        willAnswer(inv -> rows.remove(inv.<SharedFile>getArgument(0).getId())).given(fileRepository).delete(any(SharedFile.class));
        given(fileRepository.findAllByOrderByCreatedAtDesc(any(Pageable.class)))
                .willAnswer(inv -> new PageImpl<>(new ArrayList<>(rows.values()), inv.getArgument(0), rows.size()));
        given(logRepository.save(any(SharedFileLog.class))).willAnswer(inv -> {
            logs.add(inv.getArgument(0));
            return inv.getArgument(0);
        });
        given(logRepository.findAllByOrderByCreatedAtDescIdDesc(any(Pageable.class)))
                .willAnswer(inv -> new PageImpl<>(new ArrayList<>(logs), inv.getArgument(0), logs.size()));
        given(mail.sendEmail(anyString(), eq("BID_SHARED_FILE_PASSWORD"), anyMap())).willAnswer(inv -> {
            mailedPasswords.add(String.valueOf(inv.<Map<String, Object>>getArgument(2).get("password")));
            return new BpNotificationClient.SendResult(true, "r1", null);
        });
        service = serviceAt(T0);
    }

    private SharedFileService serviceAt(Instant now) {
        Clock clock = Clock.fixed(now, ZONE);
        SharedFilePolicy policy = new SharedFilePolicy(settings);
        SharedFileGate gate = new SharedFileGate(fileRepository, logRepository, crypto, policy, clock);
        return new SharedFileService(fileRepository, logRepository, gate, crypto, policy, storage, mail, clock);
    }

    private static Member member(String email) {
        Member member = Member.builder().email(email).passwordHash("x").companyName("시험").build();
        ReflectionTestUtils.setField(member, "id", UUID.randomUUID());
        return member;
    }

    private static MultipartFile fileOf(String name, byte[] content) {
        return new MockMultipartFile("file", name, "application/octet-stream", content);
    }

    private static MultipartFile sample(String name) {
        return fileOf(name, KNOWN_TEXT.getBytes(StandardCharsets.UTF_8));
    }

    private static byte[] randomBytes(int size) {
        byte[] bytes = new byte[size];
        new Random(7).nextBytes(bytes);
        return bytes;
    }

    private SharedFileResponses.Uploaded upload(String name) {
        return service.upload(sample(name), alice, IP);
    }

    private String lastPassword() {
        return mailedPasswords.get(mailedPasswords.size() - 1);
    }

    private byte[] download(SharedFileService target, UUID id, String password) throws IOException {
        SharedFileService.Download granted = target.authorizeDownload(id, password, bob, IP);
        ByteArrayOutputStream out = new ByteArrayOutputStream();
        target.writeDecrypted(granted, out);
        return out.toByteArray();
    }

    private SharedFileException failingUpload(MultipartFile file) {
        return catchThrowableOfType(() -> service.upload(file, alice, IP), SharedFileException.class);
    }

    private SharedFileException failingDownload(UUID id, String password) {
        return catchThrowableOfType(() -> download(service, id, password), SharedFileException.class);
    }

    private List<SharedFileLog.Action> actions() {
        return logs.stream().map(SharedFileLog::getAction).toList();
    }

    private void limitAttemptsTo(long limit) {
        given(settings.findLong(SettingKey.SHARED_FILE_MAX_ATTEMPTS)).willReturn(Optional.of(limit));
    }

    @Test
    @DisplayName("TC-SHARE-001")
    void 올리기_정상_메일에만비밀번호_응답에없음() {
        SharedFileResponses.Uploaded response = upload("a.pdf");

        assertThat(lastPassword()).matches("[2-9A-HJ-NP-Z]{5}(-[2-9A-HJ-NP-Z]{5}){3}");
        assertThat(response.toString()).doesNotContain(lastPassword());
    }

    @Test
    @DisplayName("TC-SHARE-002")
    void 올리기_저장파일_원문없음() {
        upload("a.pdf");

        byte[] saved = storage.files.values().iterator().next();
        assertThat(new String(saved, StandardCharsets.ISO_8859_1)).doesNotContain(KNOWN_TEXT);
    }

    @Test
    @DisplayName("TC-SHARE-003")
    void 올리기_행에_비밀번호와키없음() throws IllegalAccessException {
        upload("a.pdf");

        SharedFile row = rows.values().iterator().next();
        String normalized = SharedFileCrypto.normalize(lastPassword());
        for (Field field : SharedFile.class.getDeclaredFields()) {
            field.setAccessible(true);
            Object value = field.get(row);
            String text = value instanceof byte[] bytes ? new String(bytes, StandardCharsets.ISO_8859_1) : String.valueOf(value);
            assertThat(text).doesNotContain(lastPassword()).doesNotContain(normalized);
        }
    }

    @Test
    @DisplayName("TC-SHARE-004")
    void 올리기_파일마다_다른비밀번호와소금() {
        UUID first = upload("a.pdf").id();
        String firstPassword = lastPassword();
        byte[] firstSaved = storage.files.get(rows.get(first).getStoragePath());
        UUID second = upload("a.pdf").id();

        assertThat(lastPassword()).isNotEqualTo(firstPassword);
        assertThat(rows.get(second).getKeySalt()).isNotEqualTo(rows.get(first).getKeySalt());
        assertThat(storage.files.get(rows.get(second).getStoragePath())).isNotEqualTo(firstSaved);
    }

    @Test
    @DisplayName("TC-SHARE-005")
    void 올리기_메일실패_저장되돌림() {
        given(mail.sendEmail(anyString(), anyString(), anyMap()))
                .willReturn(new BpNotificationClient.SendResult(false, null, "down"));

        SharedFileException failure = failingUpload(sample("a.pdf"));

        assertThat(failure.getError()).isEqualTo(SharedFileError.MAIL_FAILED);
        assertThat(rows).isEmpty();
        assertThat(storage.files).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"a.exe", "a.html"})
    @DisplayName("TC-SHARE-006")
    void 올리기_막는확장자_거부(String name) {
        SharedFileException failure = failingUpload(sample(name));

        assertThat(failure.getError()).isEqualTo(SharedFileError.BLOCKED_EXTENSION);
        assertThat(storage.files).isEmpty();
    }

    @ParameterizedTest
    @ValueSource(strings = {"A.HTML", "a.zip.exe", "a.exe.", "a.exe  "})
    @DisplayName("TC-SHARE-007")
    void 올리기_대문자와겹친확장자_거부(String name) {
        assertThat(failingUpload(sample(name)).getError()).isEqualTo(SharedFileError.BLOCKED_EXTENSION);
    }

    @Test
    @DisplayName("TC-SHARE-008")
    void 올리기_상한초과_거부() {
        MultipartFile big = mock(MultipartFile.class);
        given(big.getOriginalFilename()).willReturn("big.zip");
        given(big.getSize()).willReturn(50L * 1024 * 1024 + 1);

        assertThat(failingUpload(big).getError()).isEqualTo(SharedFileError.TOO_LARGE);
    }

    @Test
    @DisplayName("TC-SHARE-009")
    void 올리기_ZIP_받음() {
        assertThat(rows.get(upload("a.zip").id()).getFileName()).isEqualTo("a.zip");
    }

    @Test
    @DisplayName("TC-SHARE-010")
    void 올리기_정책값변경_상한바뀜() {
        given(settings.findLong(SettingKey.SHARED_FILE_MAX_UPLOAD_MB)).willReturn(Optional.of(1L));

        assertThat(failingUpload(fileOf("a.zip", new byte[2 * 1024 * 1024])).getError()).isEqualTo(SharedFileError.TOO_LARGE);
    }

    @Test
    @DisplayName("TC-SHARE-011")
    void 받기_맞는비밀번호_원본과같음() throws IOException {
        byte[] original = randomBytes(2 * 1024 * 1024 + 123);
        UUID id = service.upload(fileOf("a.zip", original), alice, IP).id();

        assertThat(download(service, id, lastPassword())).isEqualTo(original);
        assertThat(actions()).contains(SharedFileLog.Action.DOWNLOAD);
    }

    @Test
    @DisplayName("한 조각에 꼭 맞는 파일도 원본으로 돌아온다")
    void 받기_조각크기와같은파일_원본과같음() throws IOException {
        byte[] original = randomBytes(SharedFileCrypto.CHUNK_BYTES * 2);
        UUID id = service.upload(fileOf("a.zip", original), alice, IP).id();

        assertThat(download(service, id, lastPassword())).isEqualTo(original);
    }

    @Test
    @DisplayName("TC-SHARE-012")
    void 받기_붙임표공백소문자_통과() throws IOException {
        UUID id = upload("a.pdf").id();

        String typed = " " + lastPassword().toLowerCase(Locale.ROOT).replace("-", " ");
        assertThat(download(service, id, typed)).isEqualTo(KNOWN_TEXT.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("TC-SHARE-013")
    void 받기_틀림_횟수증가() {
        UUID id = upload("a.pdf").id();

        SharedFileException failure = failingDownload(id, "WRONG");

        assertThat(failure.getError()).isEqualTo(SharedFileError.WRONG_PASSWORD);
        assertThat(failure.getDetails()).containsEntry("remainingAttempts", 4);
        assertThat(actions()).contains(SharedFileLog.Action.WRONG_PASSWORD);
    }

    @Test
    @DisplayName("TC-SHARE-014")
    void 받기_한도_잠김_맞아도거부() {
        UUID id = upload("a.pdf").id();
        for (int i = 0; i < 5; i++) {
            failingDownload(id, "WRONG");
        }

        SharedFileException failure = failingDownload(id, lastPassword());

        assertThat(failure.getError()).isEqualTo(SharedFileError.LOCKED);
        assertThat(failure.getDetails()).containsEntry("retryAfterMinutes", 30L);
        assertThat(actions()).contains(SharedFileLog.Action.LOCKED);
    }

    @Test
    @DisplayName("TC-SHARE-015")
    void 받기_잠금지남_가능() throws IOException {
        UUID id = upload("a.pdf").id();
        for (int i = 0; i < 5; i++) {
            failingDownload(id, "WRONG");
        }

        byte[] received = download(serviceAt(T0.plusSeconds(30 * 60 + 1)), id, lastPassword());

        assertThat(received).isEqualTo(KNOWN_TEXT.getBytes(StandardCharsets.UTF_8));
    }

    @Test
    @DisplayName("TC-SHARE-016")
    void 받기_성공_횟수지움() throws IOException {
        UUID id = upload("a.pdf").id();
        failingDownload(id, "WRONG");

        download(service, id, lastPassword());

        assertThat(rows.get(id).getFailedAttempts()).isZero();
    }

    @Test
    @DisplayName("TC-SHARE-017")
    void 받기_정책값변경_한도바뀜() {
        limitAttemptsTo(2);
        UUID id = upload("a.pdf").id();
        failingDownload(id, "WRONG");

        assertThat(failingDownload(id, "WRONG").getError()).isEqualTo(SharedFileError.LOCKED);
    }

    @Test
    @DisplayName("TC-SHARE-018")
    void 받기_저장파일변조_실패() throws IOException {
        UUID id = upload("a.pdf").id();
        storage.files.get(rows.get(id).getStoragePath())[3] ^= 1;
        SharedFileService.Download granted = service.authorizeDownload(id, lastPassword(), bob, IP);
        ByteArrayOutputStream out = new ByteArrayOutputStream();

        SharedFileException failure = catchThrowableOfType(() -> service.writeDecrypted(granted, out), SharedFileException.class);

        assertThat(failure.getError()).isEqualTo(SharedFileError.CORRUPT);
        assertThat(out.size()).isZero();
    }

    @Test
    @DisplayName("TC-SHARE-019")
    void 받기_저장파일잘림_실패() {
        UUID id = service.upload(fileOf("a.zip", randomBytes(2 * 1024 * 1024 + 123)), alice, IP).id();
        String path = rows.get(id).getStoragePath();
        storage.files.put(path, Arrays.copyOf(storage.files.get(path), CHUNK_WITH_TAG * 2));

        assertThat(failingDownload(id, lastPassword()).getError()).isEqualTo(SharedFileError.CORRUPT);
    }

    @Test
    @DisplayName("TC-SHARE-020")
    void 목록_관리자전부_mine구분() {
        upload("mine.pdf");
        service.upload(sample("other.pdf"), bob, IP);

        SharedFileResponses.ListView view = service.list(0, 20, alice);

        assertThat(view.files()).extracting(SharedFileResponses.Item::fileName, SharedFileResponses.Item::mine)
                .containsExactly(org.assertj.core.groups.Tuple.tuple("mine.pdf", true), org.assertj.core.groups.Tuple.tuple("other.pdf", false));
        assertThat(Arrays.stream(SharedFileResponses.Item.class.getRecordComponents()).map(c -> c.getName()))
                .doesNotContain("keySalt", "passwordCheck", "storagePath", "password");
    }

    @Test
    @DisplayName("TC-SHARE-021")
    void 지우기_본인_파일과행제거() {
        UUID id = upload("a.pdf").id();

        service.delete(id, alice, IP);

        assertThat(rows).isEmpty();
        assertThat(storage.files).isEmpty();
        assertThat(actions()).contains(SharedFileLog.Action.DELETE);
    }

    @Test
    @DisplayName("TC-SHARE-022")
    void 지우기_다른사람_거부() {
        UUID id = upload("a.pdf").id();

        SharedFileException failure = catchThrowableOfType(() -> service.delete(id, bob, IP), SharedFileException.class);

        assertThat(failure.getError()).isEqualTo(SharedFileError.NOT_OWNER);
        assertThat(rows).containsKey(id);
        assertThat(storage.files).hasSize(1);
    }

    @Test
    @DisplayName("TC-SHARE-023")
    void 기록_파일지운뒤_남음() {
        UUID id = upload("a.pdf").id();
        service.delete(id, alice, IP);

        assertThat(service.logs(0, 20).logs()).extracting(SharedFileResponses.LogItem::action)
                .containsExactly("UPLOAD", "DELETE");
    }

    private static final class FakeStorage implements StorageService {

        private final Map<String, byte[]> files = new HashMap<>();
        private int sequence;

        @Override
        public String store(String keyPrefix, MultipartFile file) {
            throw new UnsupportedOperationException();
        }

        @Override
        public String store(String keyPrefix, String fileName, byte[] content) {
            String path = keyPrefix + "/" + (++sequence) + "_" + fileName;
            files.put(path, content);
            return path;
        }

        @Override
        public String store(String keyPrefix, String fileName, InputStream content) {
            try {
                return store(keyPrefix, fileName, content.readAllBytes());
            } catch (IOException e) {
                throw new UncheckedIOException(e);
            }
        }

        @Override
        public byte[] load(String storageUrl) {
            return files.get(storageUrl);
        }

        @Override
        public InputStream open(String storageUrl) {
            return new ByteArrayInputStream(files.get(storageUrl));
        }

        @Override
        public void delete(String storageUrl) {
            files.remove(storageUrl);
        }
    }
}
