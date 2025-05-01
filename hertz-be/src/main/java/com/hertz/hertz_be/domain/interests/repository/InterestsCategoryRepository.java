package com.hertz.hertz_be.domain.interests.repository;

import com.hertz.hertz_be.domain.interests.entity.InterestsCategory;
import com.hertz.hertz_be.domain.interests.entity.enums.InterestsCategoryType;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.stereotype.Repository;

import java.util.Optional;

@Repository
public interface InterestsCategoryRepository extends JpaRepository<InterestsCategory, Long> {
    Optional<InterestsCategory> findByCategoryTypeAndName(InterestsCategoryType categoryType, String categoryName);

    InterestsCategory save(InterestsCategory newCategory);
}
