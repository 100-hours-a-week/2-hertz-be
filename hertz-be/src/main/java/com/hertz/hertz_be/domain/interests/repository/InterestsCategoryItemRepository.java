package com.hertz.hertz_be.domain.interests.repository;

import com.hertz.hertz_be.domain.interests.entity.InterestsCategory;
import com.hertz.hertz_be.domain.interests.entity.InterestsCategoryItem;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InterestsCategoryItemRepository extends JpaRepository<InterestsCategory, Long> {
    Optional<InterestsCategoryItem> findByCategoryTypeAndName(InterestsCategory category, String itemName);

    InterestsCategoryItem save(InterestsCategoryItem newItem);
}
