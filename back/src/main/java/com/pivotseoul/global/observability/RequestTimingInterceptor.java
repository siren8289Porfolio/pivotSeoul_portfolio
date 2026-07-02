package com.pivotseoul.global.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import jakarta.servlet.http.HttpServletRequest;
import jakarta.servlet.http.HttpServletResponse;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.HandlerInterceptor;

/**
 * §0 API 응답 시간 메트릭.
 */
@Component
public class RequestTimingInterceptor implements HandlerInterceptor {

    private static final String START_ATTR = "pivot.request.start";

    private final MeterRegistry meterRegistry;

    public RequestTimingInterceptor(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Override
    public boolean preHandle(HttpServletRequest request, HttpServletResponse response, Object handler) {
        request.setAttribute(START_ATTR, Timer.start(meterRegistry));
        return true;
    }

    @Override
    public void afterCompletion(
            HttpServletRequest request,
            HttpServletResponse response,
            Object handler,
            Exception ex
    ) {
        Object sample = request.getAttribute(START_ATTR);
        if (!(sample instanceof Timer.Sample timerSample)) {
            return;
        }
        timerSample.stop(Timer.builder("pivot.http.server.requests")
                .tag("uri", request.getRequestURI())
                .tag("method", request.getMethod())
                .tag("status", String.valueOf(response.getStatus()))
                .register(meterRegistry));
    }
}
