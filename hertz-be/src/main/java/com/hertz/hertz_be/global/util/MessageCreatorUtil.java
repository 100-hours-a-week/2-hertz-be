package com.hertz.hertz_be.global.util;

public class MessageCreatorUtil {
    public static String createMatchingSuccessMessage(String nickname) {
        return String.format("🎉 축하드려요, ‘%s’님과 매칭에 성공했습니다!", nickname);
    }

    public static String createMatchingFailureMessage(String nickname) {
        return String.format("😥 아쉽지만, ‘%s’님과의 매칭은 성사되지 않았습니다.", nickname);
    }

    private MessageCreatorUtil() {}
}
