package com.xcodez.springaivoicecanvas.repository;

import com.xcodez.springaivoicecanvas.model.ExecutionLog;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface ExecutionLogRepository extends JpaRepository<ExecutionLog, Long> {
    List<ExecutionLog> findByVersionIdOrderBySortOrderAsc(String versionId);
    void deleteByVersionId(String versionId);
}
