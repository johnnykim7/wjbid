package com.biddingagency.domain.reading;

import lombok.Getter;
import org.springframework.http.HttpStatus;

/**
 * 고객 서류 읽기가 요청을 받지 못할 때 (CR-169).
 * 화면은 code 로 갈라 안내한다.
 */
@Getter
public class DocumentReadingException extends RuntimeException {

    private final HttpStatus status;
    private final String code;

    private DocumentReadingException(HttpStatus status, String code, String message) {
        super(message);
        this.status = status;
        this.code = code;
    }

    /** BIZ-087: 읽는 중인 후보가 있으면 문서 작성 중으로 전환하지 않는다 */
    public static DocumentReadingException inProgress() {
        return new DocumentReadingException(HttpStatus.CONFLICT, "DOCUMENT_READING_IN_PROGRESS",
                "고객 서류를 읽는 중입니다. 다 읽은 뒤 전환하세요");
    }

    public static DocumentReadingException notAnalyzing() {
        return new DocumentReadingException(HttpStatus.CONFLICT, "NOT_ANALYZING",
                "분석 단계에서만 다시 읽을 수 있습니다");
    }

    public static DocumentReadingException notCandidate() {
        return new DocumentReadingException(HttpStatus.BAD_REQUEST, "NOT_CANDIDATE",
                "이 입찰 건의 고객 서류가 아닙니다");
    }

    public static DocumentReadingException notFound() {
        return new DocumentReadingException(HttpStatus.NOT_FOUND, "READING_NOT_FOUND",
                "읽기 결과를 찾을 수 없습니다");
    }
}
