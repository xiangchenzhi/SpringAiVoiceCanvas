package com.xcodez.springaivoicecanvas.repository;

import com.xcodez.springaivoicecanvas.model.DiagramVersion;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

public interface DiagramVersionRepository extends JpaRepository<DiagramVersion, String> {
    List<DiagramVersion> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    List<DiagramVersion> findByConversationIdAndParentVersionId(String conversationId, String parentVersionId);
}
