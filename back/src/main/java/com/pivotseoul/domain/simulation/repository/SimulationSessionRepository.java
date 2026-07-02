package com.pivotseoul.domain.simulation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pivotseoul.domain.simulation.entity.SimulationSession;

import java.util.Optional;

public interface SimulationSessionRepository extends JpaRepository<SimulationSession, Long> {

    Optional<SimulationSession> findBySessionUuid(String sessionUuid);
}
