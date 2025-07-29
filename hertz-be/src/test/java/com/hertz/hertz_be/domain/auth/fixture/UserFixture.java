package com.hertz.hertz_be.domain.auth.fixture;

import com.hertz.hertz_be.domain.channel.entity.enums.Category;
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


    public static User create(Long id, String nickname, String email) {
        return User.builder()
                .id(id)
                .nickname(nickname)
                .email(email)
                .profileImageUrl("http://image.url/" + id)
                .build();
    }

    public static User createDefaultSender() {
        return create(1L, "sender", "sender@test.com");
    }

    public static User createDefaultReceiver() {
        return create(2L, "receiver", "receiver@test.com");
    }

    public static User createReceiverThatAllows(Category allowedCategory) {
        return new User() {
            @Override
            public Long getId() {
                return 2L;
            }

            @Override
            public String getNickname() {
                return "receiver";
            }

            @Override
            public String getEmail() {
                return "receiver@test.com";
            }

            @Override
            public String getProfileImageUrl() {
                return "http://image.url/2";
            }

            @Override
            public boolean isCategoryAllowed(Category input) {
                return input == allowedCategory;
            }

            @Override
            public String getOneLineIntroduction() {
                return "기본 소개";
            }

            // 기타 필요한 override가 있다면 추가
        };
    }

    public static User createReceiverThatRejects(Category rejectedCategory) {
        return new User() {
            @Override
            public Long getId() {
                return 2L;
            }

            @Override
            public String getNickname() {
                return "receiver";
            }

            @Override
            public String getEmail() {
                return "receiver@test.com";
            }

            @Override
            public String getProfileImageUrl() {
                return "http://image.url/2";
            }

            @Override
            public boolean isCategoryAllowed(Category input) {
                return false;
            }

            @Override
            public String getOneLineIntroduction() {
                return "기본 소개";
            }
        };
    }

}
