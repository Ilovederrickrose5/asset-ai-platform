package com.enterprise.asset.ai.repository;

import com.enterprise.asset.ai.entity.AiMessage;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

import java.util.List;

@Repository
public interface AiMessageRepository extends JpaRepository<AiMessage, Long> {

    /** 按自增主键排序，保证同一秒内写入的消息顺序稳定 */
    List<AiMessage> findByConversationIdOrderByIdAsc(Long conversationId);

    List<AiMessage> findByConversationIdOrderByIdDesc(Long conversationId, Pageable pageable);

    List<AiMessage> findByConversationIdAndIdLessThanOrderByIdDesc(
            Long conversationId, Long beforeId, Pageable pageable);

    @Modifying
    @Query("DELETE FROM AiMessage m WHERE m.conversationId = :conversationId")
    void deleteByConversationId(@Param("conversationId") Long conversationId);

    long countByConversationId(Long conversationId);
}
