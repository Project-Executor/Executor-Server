package com.seyoung5309.executorserver.domain.profile.repository;

import com.seyoung5309.executorserver.domain.profile.entity.Profile;
import org.springframework.data.domain.Page;
import org.springframework.data.domain.Pageable;
import org.springframework.data.jpa.repository.EntityGraph;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Modifying;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.Optional;

public interface ProfileRepository extends JpaRepository<Profile, Long> {

    boolean existsByNickname(String nickname);

    /** 코인을 더한다 (coin = coin + amount). 엔티티를 읽지 않고 DB에서 바로 더해서 동시 요청에도 값이 유실되지 않는다. */
    @Modifying(flushAutomatically = true, clearAutomatically = true)
    @Query("update Profile p set p.coin = p.coin + :amount where p.userId = :userId")
    int addCoin(@Param("userId") Long userId, @Param("amount") int amount);

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
