package com.seyoung5309.executorserver.domain.message.repository;

import com.seyoung5309.executorserver.domain.message.entity.Message;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;

public interface MessageRepository extends JpaRepository<Message, Long> {

    /**
     * 나와 대화한 상대마다 마지막 메시지 ID와 안 읽은 수 (마지막 메시지가 최근인 순).
     * 메시지 ID는 AUTO_INCREMENT라 가장 큰 ID를 마지막 메시지로 본다.
     */
    @Query(value = """
            SELECT t.partner_id AS partnerId,
                   MAX(t.id) AS lastMessageId,
                   CAST(SUM(CASE WHEN t.receiver_id = :userId AND t.is_read = FALSE THEN 1 ELSE 0 END) AS SIGNED) AS unreadCount
            FROM (
                SELECT CASE WHEN m.sender_id = :userId THEN m.receiver_id ELSE m.sender_id END AS partner_id,
                       m.id, m.receiver_id, m.is_read
                FROM messages m
                WHERE m.sender_id = :userId OR m.receiver_id = :userId
            ) t
            GROUP BY t.partner_id
            ORDER BY lastMessageId DESC
            """, nativeQuery = true)
    List<RoomRow> findRooms(@Param("userId") Long userId);

    /** 나와 상대가 주고받은 메시지 (정렬은 Pageable로: 최신순) */
    @Query(value = """
            select m from Message m
            where (m.sender.id = :userId and m.receiver.id = :partnerId)
               or (m.sender.id = :partnerId and m.receiver.id = :userId)
            """, countQuery = """
            select count(m) from Message m
            where (m.sender.id = :userId and m.receiver.id = :partnerId)
               or (m.sender.id = :partnerId and m.receiver.id = :userId)
            """)
    Page<Message> findConversation(@Param("userId") Long userId, @Param("partnerId") Long partnerId,
                                   Pageable pageable);

    /** 상대가 나에게 보낸 안 읽은 메시지를 모두 읽음 처리한다 */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("""
            update Message m set m.isRead = true
            where m.sender.id = :partnerId and m.receiver.id = :userId and m.isRead = false
            """)
    int markAsRead(@Param("userId") Long userId, @Param("partnerId") Long partnerId);

    /** 나에게 온 안 읽은 메시지 전체 수 */
    long countByReceiverIdAndIsReadFalse(Long receiverId);
}
