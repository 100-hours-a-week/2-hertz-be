package com.hertz.hertz_be.domain.interests.repository;

import com.hertz.hertz_be.domain.interests.entity.InterestsCategoryItem;
import com.hertz.hertz_be.domain.interests.entity.UserInterests;
import com.hertz.hertz_be.domain.user.entity.User;
import io.lettuce.core.dynamic.annotation.Param;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.stereotype.Repository;

import java.util.List;
import java.util.Optional;

@Repository
public interface UserInterestsRepository extends JpaRepository<UserInterests, Long> {
    boolean existsByUserAndCategoryItem(User user, InterestsCategoryItem item);
    boolean existsByUser(User user);

    @Query("SELECT ui FROM UserInterests ui JOIN FETCH ui.categoryItem ci JOIN FETCH ci.category WHERE ui.user.id = :userId")
    List<UserInterests> findByUserId(@Param("userId") Long userId);

    void deleteAllByUser(User user);
}
