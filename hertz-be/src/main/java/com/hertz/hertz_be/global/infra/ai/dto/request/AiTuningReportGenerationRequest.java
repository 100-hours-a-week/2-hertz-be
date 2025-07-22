package com.hertz.hertz_be.global.infra.ai.dto.request;

import com.fasterxml.jackson.annotation.JsonProperty;
import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.global.infra.ai.dto.object.AiSignalRoomDto;

import java.util.List;

public record AiTuningReportGenerationRequest(
        String category,
        int chatCount,
        UserData userA,
        UserData userB,
        String emailDomain,
        AiSignalRoomDto signalRoom
) {
    public static AiTuningReportGenerationRequest of(SignalRoom room, int chatCount, UserData userA, UserData userB, String emailDomain) {
        return new AiTuningReportGenerationRequest(
                room.getCategory().name(),
                chatCount,
                userA,
                userB,
                emailDomain,
                AiSignalRoomDto.from(room)
        );
    }

    public record UserData(
            String gender,
            String emailDomain,
            @JsonProperty("MBTI")
            String mbti,
            String religion,
            String smoking,
            String drinking,
            List<String> personality,
            List<String> preferredPeople,
            List<String> currentInterests,
            List<String> favoriteFoods,
            List<String> likedSports,
            List<String> pets,
            List<String> selfDevelopment,
            List<String> hobbies
    ){}
}
