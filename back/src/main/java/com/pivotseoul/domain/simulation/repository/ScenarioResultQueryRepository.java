package com.pivotseoul.domain.simulation.repository;

import com.pivotseoul.domain.simulation.dto.ScenarioResultBundle;
import com.pivotseoul.domain.simulation.dto.ThresholdResultResponse;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.sql.ResultSet;
import java.sql.SQLException;
import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * §4 SQL 최적화 — 결과 + 임계값을 JOIN 1회로 조회 (N+1 제거).
 */
@Repository
public class ScenarioResultQueryRepository {

    private static final String RESULT_WITH_THRESHOLDS_SQL = """
            SELECT sr.scenario_result_id,
                   sr.simulation_run_id,
                   sr.scenario_id,
                   sr.result_status,
                   sr.total_score,
                   sr.risk_score,
                   sr.confidence_score,
                   sr.housing_score,
                   tr.threshold_result_id,
                   tr.threshold_type_id,
                   tr.threshold_status,
                   tr.calculated_value,
                   tr.threshold_value,
                   tr.is_red_zone,
                   tr.calculation_summary
            FROM scenario_result sr
            LEFT JOIN threshold_result tr
                   ON tr.scenario_result_id = sr.scenario_result_id
            WHERE sr.scenario_result_id = ?
            ORDER BY tr.threshold_result_id ASC NULLS LAST
            """;

    private static final String HISTORY_SQL = """
            SELECT sr.scenario_result_id,
                   sr.simulation_run_id,
                   sr.scenario_id,
                   sr.result_status,
                   sr.total_score,
                   sr.risk_score,
                   sr.confidence_score,
                   sr.housing_score,
                   tr.threshold_result_id,
                   tr.threshold_type_id,
                   tr.threshold_status,
                   tr.calculated_value,
                   tr.threshold_value,
                   tr.is_red_zone,
                   tr.calculation_summary
            FROM scenario_result sr
            LEFT JOIN threshold_result tr
                   ON tr.scenario_result_id = sr.scenario_result_id
            WHERE sr.simulation_run_id = ?
            ORDER BY sr.scenario_result_id DESC, tr.threshold_result_id ASC NULLS LAST
            """;

    private final JdbcTemplate jdbcTemplate;

    public ScenarioResultQueryRepository(JdbcTemplate jdbcTemplate) {
        this.jdbcTemplate = jdbcTemplate;
    }

    public Optional<ScenarioResultBundle> findBundleById(Long scenarioResultId) {
        List<ScenarioResultBundle> bundles = jdbcTemplate.query(
                RESULT_WITH_THRESHOLDS_SQL,
                (rs, rowNum) -> mapRow(rs),
                scenarioResultId
        );
        return mergeRows(bundles);
    }

    public List<ScenarioResultBundle> findBundlesByRunId(Long simulationRunId) {
        List<ScenarioResultBundle> partial = jdbcTemplate.query(
                HISTORY_SQL,
                (rs, rowNum) -> mapRow(rs),
                simulationRunId
        );
        return mergeHistory(partial);
    }

    private ScenarioResultBundle mapRow(ResultSet rs) throws SQLException {
        Long thresholdId = getLong(rs, "threshold_result_id");
        List<ThresholdResultResponse> thresholds = new ArrayList<>();
        if (thresholdId != null) {
            thresholds.add(new ThresholdResultResponse(
                    thresholdId,
                    getLong(rs, "threshold_type_id"),
                    rs.getString("threshold_status"),
                    rs.getBigDecimal("calculated_value"),
                    rs.getBigDecimal("threshold_value"),
                    rs.getBoolean("is_red_zone"),
                    rs.getString("calculation_summary")
            ));
        }
        return new ScenarioResultBundle(
                rs.getLong("scenario_result_id"),
                getLong(rs, "simulation_run_id"),
                getLong(rs, "scenario_id"),
                rs.getString("result_status"),
                rs.getBigDecimal("total_score"),
                rs.getBigDecimal("risk_score"),
                rs.getBigDecimal("confidence_score"),
                rs.getBigDecimal("housing_score"),
                thresholds
        );
    }

    private Optional<ScenarioResultBundle> mergeRows(List<ScenarioResultBundle> rows) {
        if (rows.isEmpty()) {
            return Optional.empty();
        }
        ScenarioResultBundle first = rows.get(0);
        List<ThresholdResultResponse> thresholds = new ArrayList<>();
        for (ScenarioResultBundle row : rows) {
            thresholds.addAll(row.thresholds());
        }
        return Optional.of(first.withThresholds(thresholds));
    }

    private List<ScenarioResultBundle> mergeHistory(List<ScenarioResultBundle> rows) {
        List<ScenarioResultBundle> result = new ArrayList<>();
        ScenarioResultBundle current = null;
        List<ThresholdResultResponse> thresholds = new ArrayList<>();

        for (ScenarioResultBundle row : rows) {
            if (current == null || !current.scenarioResultId().equals(row.scenarioResultId())) {
                if (current != null) {
                    result.add(current.withThresholds(thresholds));
                }
                current = row;
                thresholds = new ArrayList<>(row.thresholds());
            } else {
                thresholds.addAll(row.thresholds());
            }
        }
        if (current != null) {
            result.add(current.withThresholds(thresholds));
        }
        return result;
    }

    private static Long getLong(ResultSet rs, String column) throws SQLException {
        long value = rs.getLong(column);
        return rs.wasNull() ? null : value;
    }
}
