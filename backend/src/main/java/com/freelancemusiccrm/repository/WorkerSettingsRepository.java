package com.freelancemusiccrm.repository;

import com.freelancemusiccrm.entity.WorkerSettings;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.Optional;

public interface WorkerSettingsRepository extends JpaRepository<WorkerSettings, Long> {

    Optional<WorkerSettings> findByWorkerId(Long workerId);
}
