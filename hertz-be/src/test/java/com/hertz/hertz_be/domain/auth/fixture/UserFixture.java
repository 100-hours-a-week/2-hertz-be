package com.hertz.hertz_be.domain.auth.fixture;

import com.hertz.hertz_be.domain.user.entity.User;
import com.hertz.hertz_be.domain.user.entity.enums.AgeGroup;
import com.hertz.hertz_be.domain.user.entity.enums.Gender;

import java.util.UUID;

public class UserFixture {

    public static User createTestUser() {
        return User.builder()
                .ageGroup(AgeGroup.AGE_20S)
                .gender(Gender.MALE)
                .email(UUID.randomUUID() + "@test.com")
                .profileImageUrl("http://example.com/profile.png")
                .nickname("test-user")
                .oneLineIntroduction("테스트 유저입니다")
                .build();
    }
}
