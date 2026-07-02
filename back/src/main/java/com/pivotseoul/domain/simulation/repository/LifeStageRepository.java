package com.pivotseoul.domain.simulation.repository;

import org.springframework.data.jpa.repository.JpaRepository;
import com.pivotseoul.domain.simulation.entity.LifeStage;

import java.util.Optional;

public interface LifeStageRepository extends JpaRepository<LifeStage, Long> {
    Optional<LifeStage> findByStageCode(String stageCode);
}
