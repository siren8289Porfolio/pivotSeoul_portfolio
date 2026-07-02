package com.pivotseoul.global.observability;

import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Service;

import java.sql.Timestamp;
import java.time.Instant;
import java.util.Optional;

/**
 * §15 파이프라인 실행 로그 — analytics.pipeline_run_log 적재.
 */
@Service
public class PipelineRunLogService {

    private final JdbcTemplate jdbcTemplate;

    public PipelineRunLogService(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public long start(String jobName) {
        return jdbcTemplate.queryForObject(
                """
                INSERT INTO analytics.pipeline_run_log (job_name, status, started_at)
                VALUES (?, 'RUNNING', CURRENT_TIMESTAMP)
                RETURNING run_log_id
                """,
                Long.class,
                jobName
        );
    }

    public void complete(
            long runLogId,
            long processedRowCount,
            long queryRuntimeMs,
            Instant sourceMaxTs
    ) {
        jdbcTemplate.update(
                """
                UPDATE analytics.pipeline_run_log
                SET status = 'SUCCESS',
                    completed_at = CURRENT_TIMESTAMP,
                    processed_row_count = ?,
                    query_runtime_ms = ?,
                    source_max_ts = ?,
                    target_max_ts = CURRENT_TIMESTAMP
                WHERE run_log_id = ?
                """,
                processedRowCount,
                queryRuntimeMs,
                Optional.ofNullable(sourceMaxTs).map(Timestamp::from).orElse(null),
                runLogId
        );
    }

    public void fail(long runLogId, String errorMessage, long queryRuntimeMs) {
        jdbcTemplate.update(
                """
                UPDATE analytics.pipeline_run_log
                SET status = 'FAILED',
                    completed_at = CURRENT_TIMESTAMP,
                    failed_row_count = 1,
                    query_runtime_ms = ?,
                    error_message = ?
                WHERE run_log_id = ?
                """,
                queryRuntimeMs,
                errorMessage,
                runLogId
        );
    }
}
