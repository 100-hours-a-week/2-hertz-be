package com.hertz.hertz_be.domain.interests.repository;

import com.hertz.hertz_be.domain.interests.entity.InterestsCategoryItem;
import com.hertz.hertz_be.domain.interests.entity.UserInterests;
import com.hertz.hertz_be.domain.user.entity.User;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

@Repository
public interface UserInterestsRepository extends JpaRepository<UserInterests, Long> {
    boolean existsByUserAndCategoryItem(User user, InterestsCategoryItem item);
}
