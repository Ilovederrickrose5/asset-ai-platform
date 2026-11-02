package com.enterprise.asset.ai.repository;

import com.enterprise.asset.ai.entity.AiConversation;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiConversationRepository extends JpaRepository<AiConversation, Long> {

    List<AiConversation> findByUserIdOrderByUpdateTimeDesc(Long userId);

    boolean existsByIdAndUserId(Long id, Long userId);
}
