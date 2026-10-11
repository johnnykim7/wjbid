package com.biddingagency.domain.sitephoto.service;

import com.biddingagency.domain.setting.entity.SettingKey;
import com.biddingagency.domain.setting.service.SystemSettingService;
import com.biddingagency.domain.sitephoto.entity.SitePhoto;
import com.biddingagency.domain.sitephoto.entity.SitePhotoSlot;
import com.biddingagency.domain.sitephoto.repository.SitePhotoRepository;
import com.biddingagency.domain.sitephoto.repository.SitePhotoSlotRepository;
import com.biddingagency.dto.SitePhotoResponses;
import com.biddingagency.integration.storage.StorageService;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.server.ResponseStatusException;

import java.io.IOException;
import java.io.UncheckedIOException;
import java.util.Arrays;
import java.util.Comparator;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Locale;
import java.util.Map;
import java.util.UUID;
import java.util.stream.Collectors;

/**
 * 고객 화면 사진 묶음 (CR-153, BID-SITE-001, BID-SITE-002)
 */
@Slf4j
@Service
public class SitePhotoService {

    static final List<String> DEFAULT_ALLOWED_TYPES = List.of("image/jpeg", "image/png");
    static final long DEFAULT_MAX_UPLOAD_MB = 20;
    static final long DEFAULT_MAX_MEGAPIXELS = 50;
    static final long DEFAULT_MIN_WIDTH = 1000;
    static final long DEFAULT_SAVE_WIDTH = 1600;
    static final long DEFAULT_SAVE_MAX_KB = 400;

    private static final String STORAGE_PREFIX = "site-photos";

    /**
     * 처음부터 있던 사진을 앞에, 그 뒤는 올린 순서.
     * 처음 사진은 마이그레이션이 DB 시각으로, 올린 사진은 앱 시각으로 찍혀 시간대가 다를 수 있어 시각만으로 줄 세우지 않는다.
     */
    private static final Comparator<SitePhoto> DISPLAY_ORDER =
            Comparator.comparing((SitePhoto p) -> !p.isBundled()).thenComparing(SitePhoto::getCreatedAt);

    private final SitePhotoSlotRepository slotRepository;
    private final SitePhotoRepository photoRepository;
    private final SystemSettingService settingService;
    private final StorageService storageService;
    private final SitePhotoImageProcessor imageProcessor;
    private final String contextPath;

    public SitePhotoService(SitePhotoSlotRepository slotRepository,
                            SitePhotoRepository photoRepository,
                            SystemSettingService settingService,
                            StorageService storageService,
                            SitePhotoImageProcessor imageProcessor,
                            @Value("${server.servlet.context-path:}") String contextPath) {
        this.slotRepository = slotRepository;
        this.photoRepository = photoRepository;
        this.settingService = settingService;
        this.storageService = storageService;
        this.imageProcessor = imageProcessor;
        this.contextPath = contextPath;
    }

    /** 설정값이 비었거나 읽을 수 없으면 처음 값 (POL-039) */
    @Transactional(readOnly = true)
    public SitePhotoRules rules() {
        List<String> types = settingService.findValue(SettingKey.SITE_PHOTO_ALLOWED_TYPES)
                .map(v -> Arrays.stream(v.split(","))
                        .map(t -> t.trim().toLowerCase(Locale.ROOT))
                        .filter(t -> t.contains("/"))
                        .toList())
                .filter(list -> !list.isEmpty())
                .orElse(DEFAULT_ALLOWED_TYPES);
        return new SitePhotoRules(
                types,
                positive(SettingKey.SITE_PHOTO_MAX_UPLOAD_MB, DEFAULT_MAX_UPLOAD_MB),
                positive(SettingKey.SITE_PHOTO_MAX_MEGAPIXELS, DEFAULT_MAX_MEGAPIXELS),
                (int) positive(SettingKey.SITE_PHOTO_MIN_WIDTH, DEFAULT_MIN_WIDTH),
                (int) positive(SettingKey.SITE_PHOTO_SAVE_WIDTH, DEFAULT_SAVE_WIDTH),
                (int) positive(SettingKey.SITE_PHOTO_SAVE_MAX_KB, DEFAULT_SAVE_MAX_KB));
    }

    /** 고객 화면용. 숨긴 사진은 빠지고 사진이 없는 자리도 빈 배열로 나온다 (BIZ-067) */
    @Transactional(readOnly = true)
    public SitePhotoResponses.PublicView publicView() {
        Map<String, List<SitePhoto>> bySlot = groupBySlot(photoRepository.findAllByVisibleTrue());
        Map<String, List<String>> slots = new LinkedHashMap<>();
        for (SitePhotoSlot slot : slotRepository.findAllByOrderBySortOrderAsc()) {
            slots.put(slot.getSlotKey(), bySlot.getOrDefault(slot.getSlotKey(), List.of()).stream().map(this::url).toList());
        }
        return new SitePhotoResponses.PublicView(slots);
    }

    @Transactional(readOnly = true)
    public SitePhotoResponses.AdminView adminView() {
        Map<String, List<SitePhoto>> bySlot = groupBySlot(photoRepository.findAll());
        List<SitePhotoResponses.AdminSlot> slots = slotRepository.findAllByOrderBySortOrderAsc().stream()
                .map(slot -> new SitePhotoResponses.AdminSlot(slot.getSlotKey(), slot.getName(), slot.getLocation(),
                        bySlot.getOrDefault(slot.getSlotKey(), List.of()).stream().map(this::toAdminPhoto).toList()))
                .toList();
        return new SitePhotoResponses.AdminView(rules(), slots);
    }

    /** 올린 사진을 줄여 다시 저장한다. 원본은 남기지 않는다 (BIZ-068) */
    @Transactional
    public SitePhotoResponses.AdminPhoto upload(String slotKey, MultipartFile file) {
        if (slotKey == null || !slotRepository.existsById(slotKey)) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사진 자리를 찾을 수 없습니다");
        }
        if (file == null || file.isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "올릴 사진을 골라 주세요");
        }
        SitePhotoRules rules = rules();
        if (file.getSize() > rules.maxUploadBytes()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "한 장 " + rules.maxUploadMb() + "MB 까지 올릴 수 있습니다");
        }
        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase(Locale.ROOT);
        if (!rules.allowedTypes().contains(contentType)) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, rules.allowedTypesLabel() + " 사진만 올릴 수 있습니다");
        }

        SitePhotoImageProcessor.Result result = imageProcessor.process(readBytes(file), rules);
        String storageUrl = storageService.store(STORAGE_PREFIX + "/" + slotKey, jpgName(file.getOriginalFilename()), result.jpeg());
        try {
            SitePhoto saved = photoRepository.saveAndFlush(SitePhoto.builder()
                    .slotKey(slotKey)
                    .storageUrl(storageUrl)
                    .fileName(jpgName(file.getOriginalFilename()))
                    .width(result.width())
                    .height(result.height())
                    .originalWidth(result.originalWidth())
                    .originalHeight(result.originalHeight())
                    .sizeBytes(result.jpeg().length)
                    .visible(true)
                    .build());
            log.info("[site-photo] 올림: slot={}, id={}, {}x{} -> {}x{}, {} bytes", slotKey, saved.getId(),
                    result.originalWidth(), result.originalHeight(), result.width(), result.height(), result.jpeg().length);
            return toAdminPhoto(saved);
        } catch (RuntimeException e) {
            storageService.delete(storageUrl);
            throw e;
        }
    }

    @Transactional
    public SitePhotoResponses.AdminPhoto changeVisible(UUID id, Boolean visible) {
        if (visible == null) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "보임 여부를 보내 주세요");
        }
        SitePhoto photo = find(id);
        photo.changeVisible(visible);
        return toAdminPhoto(photo);
    }

    /** 함께 배포된 사진은 고객 화면 배포물이라 행만 지운다 */
    @Transactional
    public void delete(UUID id) {
        SitePhoto photo = find(id);
        photoRepository.delete(photo);
        photoRepository.flush();
        if (!photo.isBundled()) {
            try {
                storageService.delete(photo.getStorageUrl());
            } catch (RuntimeException e) {
                log.warn("[site-photo] 저장 파일을 지우지 못했습니다: id={}, url={}", id, photo.getStorageUrl(), e);
            }
        }
    }

    /** 올린 사진 파일. 함께 배포된 사진은 고객 포털이 직접 주므로 404 */
    @Transactional(readOnly = true)
    public byte[] loadImage(UUID id) {
        SitePhoto photo = find(id);
        if (photo.isBundled()) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사진 파일을 찾을 수 없습니다");
        }
        try {
            return storageService.load(photo.getStorageUrl());
        } catch (UncheckedIOException e) {
            throw new ResponseStatusException(HttpStatus.NOT_FOUND, "사진 파일을 찾을 수 없습니다");
        }
    }

    private SitePhoto find(UUID id) {
        return photoRepository.findById(id)
                .orElseThrow(() -> new ResponseStatusException(HttpStatus.NOT_FOUND, "사진을 찾을 수 없습니다"));
    }

    private Map<String, List<SitePhoto>> groupBySlot(List<SitePhoto> photos) {
        return photos.stream()
                .sorted(DISPLAY_ORDER)
                .collect(Collectors.groupingBy(SitePhoto::getSlotKey, LinkedHashMap::new, Collectors.toList()));
    }

    private String url(SitePhoto photo) {
        return photo.isBundled() ? photo.getBundledPath() : contextPath + "/site-photos/" + photo.getId() + "/image";
    }

    private SitePhotoResponses.AdminPhoto toAdminPhoto(SitePhoto p) {
        return new SitePhotoResponses.AdminPhoto(p.getId(), p.getSlotKey(), url(p), p.getFileName(), p.getWidth(),
                p.getHeight(), p.getOriginalWidth(), p.getOriginalHeight(), p.getSizeBytes(), p.isBundled(),
                p.isVisible(), p.getCreatedAt());
    }

    private long positive(String key, long defaultValue) {
        return settingService.findLong(key).filter(v -> v >= 1).orElse(defaultValue);
    }

    private byte[] readBytes(MultipartFile file) {
        try {
            return file.getBytes();
        } catch (IOException e) {
            throw new UncheckedIOException("올린 사진을 읽지 못했습니다", e);
        }
    }

    /** 다시 저장한 JPEG 라 확장자를 .jpg 로 바꾼다 */
    static String jpgName(String originalName) {
        String name = originalName == null ? "" : originalName.replaceAll(".*[/\\\\]", "").trim();
        int dot = name.lastIndexOf('.');
        String base = dot > 0 ? name.substring(0, dot) : name;
        return (base.isBlank() ? "photo" : base) + ".jpg";
    }
}
