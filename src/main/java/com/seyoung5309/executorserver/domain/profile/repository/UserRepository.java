package com.seyoung5309.executorserver.domain.profile.repository;

import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    boolean existsByNickname(String nickname);

    /** 코인을 더한다 (coin = coin + amount). 엔티티를 읽지 않고 DB에서 바로 더해서 동시 요청에도 값이 유실되지 않는다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Profile p set p.coin = p.coin + :amount where p.userId = :userId")
    int addCoin(@Param("userId") Long userId, @Param("amount") int amount);

    /**
     * 코인이 충분할 때만 뺀다 (coin >= amount 조건부 UPDATE). 확인과 차감이 한 문장이라 동시 구매에도 코인이 음수가 되지 않는다.
     *
     * @return 뺐으면 1, 코인이 모자라면 0
     */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Profile p set p.coin = p.coin - :amount where p.userId = :userId and p.coin >= :amount")
    int spendCoin(@Param("userId") Long userId, @Param("amount") int amount);

    /**
     * 두 사용자의 프로필 행을 ID 순서대로 잠근다 (SELECT ... FOR UPDATE). 트랜잭션 안에서 호출해야 한다.
     * 같은 두 사람 사이의 친구 요청·수락·삭제를 한 번에 하나씩 처리하기 위해 쓴다.
     * 항상 작은 ID부터 잠그므로 A->B, B->A 요청이 동시에 와도 교착 상태가 생기지 않는다.
     *
     * @return 잠근 사용자 ID (없는 사용자는 빠진다)
     */
    @Query(value = """
            SELECT user_id FROM profiles
            WHERE user_id IN (:a, :b)
            ORDER BY user_id
            FOR UPDATE
            """, nativeQuery = true)
    List<Long> lockPair(@Param("a") Long a, @Param("b") Long b);

    /**
     * 한 사용자의 프로필 행을 잠근다 (SELECT ... FOR UPDATE). 트랜잭션 안에서 호출해야 한다.
     * 같은 사용자의 동시 요청을 한 번에 하나씩 처리하기 위해 쓴다 (예: 힌트 사용).
     *
     * @return 잠근 사용자 ID (없는 사용자면 빈 값)
     */
    @Query(value = "SELECT user_id FROM profiles WHERE user_id = :userId FOR UPDATE", nativeQuery = true)
    Optional<Long> lockOne(@Param("userId") Long userId);

    @Query("select p.coin from Profile p where p.userId = :userId")
    Optional<Integer> findCoinByUserId(@Param("userId") Long userId);

    /** 내 프로필 조회용: 이메일·권한이 필요하므로 users를 함께 읽는다 */
    @EntityGraph(attributePaths = "user")
    @Query("select p from Profile p where p.userId = :userId")
    Optional<Profile> findWithUserById(@Param("userId") Long userId);

    /**
     * 닉네임에 검색어가 포함된 프로필 (나 자신 제외).
     * keyword는 LIKE 특수문자(%, _, \)를 이스케이프한 값이어야 한다.
     */
    @Query("""
            select p from Profile p
            where p.nickname like concat('%', :keyword, '%') escape '\\'
              and p.userId <> :excludeUserId
            order by p.nickname asc
            """)
    Page<Profile> searchByNickname(@Param("keyword") String keyword,
                                   @Param("excludeUserId") Long excludeUserId,
                                   Pageable pageable);
}
