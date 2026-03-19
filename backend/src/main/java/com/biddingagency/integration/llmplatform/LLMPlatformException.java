package com.biddingagency.integration.llmplatform;

public class LLMPlatformException extends RuntimeException {

    public LLMPlatformException(String message) {
        super(message);
    }

    public LLMPlatformException(String message, Throwable cause) {
        super(message, cause);
    }
}
