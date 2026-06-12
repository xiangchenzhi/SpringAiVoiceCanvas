package com.xcodez.springaivoicecanvas.repository;

import com.xcodez.springaivoicecanvas.model.Conversation;
import org.springframework.data.jpa.repository.JpaRepository;

public interface ConversationRepository extends JpaRepository<Conversation, String> {
}
