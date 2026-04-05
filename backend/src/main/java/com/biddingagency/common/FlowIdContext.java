package com.biddingagency.common;

/**
 * FlowGuard X-Flow-Id 전파용 ThreadLocal 컨텍스트.
 * 요청 스레드 내에서 flowId를 어디서든 참조할 수 있도록 한다.
 */
public final class FlowIdContext {

    private static final ThreadLocal<String> FLOW_ID = new ThreadLocal<>();

    private FlowIdContext() {}

    public static void set(String flowId) {
        FLOW_ID.set(flowId);
    }

    public static String get() {
        return FLOW_ID.get();
    }

    public static void clear() {
        FLOW_ID.remove();
    }
}
