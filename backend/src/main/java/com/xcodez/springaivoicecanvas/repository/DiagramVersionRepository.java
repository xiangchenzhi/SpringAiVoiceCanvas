package com.xcodez.springaivoicecanvas.repository;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.transaction.annotation.Transactional;

import com.xcodez.springaivoicecanvas.model.DiagramVersion;

public interface DiagramVersionRepository extends JpaRepository<DiagramVersion, String> {
    List<DiagramVersion> findByConversationIdOrderByCreatedAtAsc(String conversationId);
    List<DiagramVersion> findByConversationIdAndParentVersionId(String conversationId, String parentVersionId);

    @Transactional
    void deleteByConversationId(String conversationId);
}
