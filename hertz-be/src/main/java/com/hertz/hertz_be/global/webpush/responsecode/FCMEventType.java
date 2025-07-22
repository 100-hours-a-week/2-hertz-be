package com.hertz.hertz_be.global.webpush.responsecode;

import lombok.Getter;
import lombok.RequiredArgsConstructor;

@Getter
@RequiredArgsConstructor
public enum FCMEventType {
    MESSAGE_RECEIVED("새 메세지 수신", null),
    MATCHING_CONVERTED("매칭 상태 전환 알림", " 님과의 매칭이 가능합니다!"),
    MATCHING_DECIDED_BY_PARTNER("상대방 매칭 결정 알림", " 님이 매칭 여부를 결정하였습니다."),
    MATCHING_SUCCESS("매칭 성공", " 님과의 매칭이 성사되었습니다~"),
    MATCHING_FAIL("매칭 실패", " 님과의 매칭에 실패하였습니다..."),
    TUNING_REPORT_ARRIVED("튜닝 리포트 알림", null),
    SYSTEM_NOTIFICATION("시스템 내 공지", null);

    private final String label;
    private final String content;

}
