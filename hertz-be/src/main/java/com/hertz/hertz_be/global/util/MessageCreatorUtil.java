package com.hertz.hertz_be.global.util;

public class MessageCreatorUtil {
    public static String createMatchingSuccessMessage(String nickname) {
        return String.format("🎉 축하드려요, ‘%s’님과 매칭에 성공했어요!", nickname);
    }

    public static String createMatchingFailureMessage(String nickname) {
        return String.format("😥 아쉽지만, ‘%s’님과의 매칭은 성사되지 않았어요.", nickname);
    }

    public static String createTuningReportMessage() {
        return "이번 주 튜닝 결과가 왔어요! 👈확인하러가기";
    }

    public static String createAlertMessageForInappropriateContent() {
        return "부적절한 표현이 담긴 메시지를 전송하여 경고를 받았어요!";
    }

    private MessageCreatorUtil() {}
}
