package com.paginass.repository;

import com.paginass.entity.JobRun;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;
import java.util.UUID;

public interface JobRunRepository extends JpaRepository<JobRun, UUID> {

    List<JobRun> findTop50ByOrderByStartedAtDesc();
}
