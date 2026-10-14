package com.biddingagency.integration.storage;

import org.springframework.web.multipart.MultipartFile;

/**
 * 파일 스토리지 추상화 (CR-013).
 * 1차 구현 = 로컬 디스크(LocalFileStorageService). 향후 S3StorageService 추가만으로 교체.
 */
public interface StorageService {

    /** 파일을 저장하고 식별 가능한 storageUrl(상대경로 또는 URI)을 반환 */
    String store(String keyPrefix, MultipartFile file);

    /**
     * CR-025: 바이트 배열을 저장하고 storageUrl을 반환.
     * 자동 다운로드(MultipartFile 없이 받은 첨부 바이트) 저장 경로.
     */
    String store(String keyPrefix, String fileName, byte[] content);

    /** CR-160: 흐름을 그대로 저장한다. 수백 MB 짜리 PIEE 압축 안 파일을 메모리에 올리지 않으려고 둔다 */
    String store(String keyPrefix, String fileName, java.io.InputStream content);

    /** storageUrl로 저장된 바이트를 로드 */
    byte[] load(String storageUrl);

    /** storageUrl로 저장된 파일 삭제 */
    void delete(String storageUrl);
}
