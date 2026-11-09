package com.biddingagency.domain.sharedfile;

import java.util.Map;

public class SharedFileException extends RuntimeException {

    private final SharedFileError error;
    private final transient Map<String, Object> details;

    public SharedFileException(SharedFileError error) {
        this(error, Map.of());
    }

    public SharedFileException(SharedFileError error, Map<String, Object> details) {
        super(error.message());
        this.error = error;
        this.details = details;
    }

    public SharedFileError getError() {
        return error;
    }

    public Map<String, Object> getDetails() {
        return details;
    }
}
