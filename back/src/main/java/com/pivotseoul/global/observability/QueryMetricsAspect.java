package com.pivotseoul.global.observability;

import io.micrometer.core.instrument.MeterRegistry;
import io.micrometer.core.instrument.Timer;
import org.aspectj.lang.ProceedingJoinPoint;
import org.aspectj.lang.annotation.Around;
import org.aspectj.lang.annotation.Aspect;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

/**
 * §0 지표 측정 — repository/service 쿼리 실행 시간 기록.
 */
@Aspect
@Component
public class QueryMetricsAspect {

    private static final Logger log = LoggerFactory.getLogger(QueryMetricsAspect.class);

    private final MeterRegistry meterRegistry;

    public QueryMetricsAspect(MeterRegistry meterRegistry) {
        this.meterRegistry = meterRegistry;
    }

    @Around("execution(* com.pivotseoul.domain.simulation.repository..*(..))")
    public Object timeRepository(ProceedingJoinPoint joinPoint) throws Throwable {
        return time("repository", joinPoint);
    }

    @Around("execution(* com.pivotseoul.domain.simulation.service.SimulationResultService.*(..))")
    public Object timeResultService(ProceedingJoinPoint joinPoint) throws Throwable {
        return time("service.result", joinPoint);
    }

    private Object time(String layer, ProceedingJoinPoint joinPoint) throws Throwable {
        String name = joinPoint.getSignature().toShortString();
        Timer.Sample sample = Timer.start(meterRegistry);
        try {
            return joinPoint.proceed();
        } finally {
            long nanos = sample.stop(Timer.builder("pivot.query.duration")
                    .tag("layer", layer)
                    .tag("method", name)
                    .register(meterRegistry));
            long millis = nanos / 1_000_000;
            if (millis >= 50) {
                log.info("slow_query layer={} method={} duration_ms={}", layer, name, millis);
            }
        }
    }
}
