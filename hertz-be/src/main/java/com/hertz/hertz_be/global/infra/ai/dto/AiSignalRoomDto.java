package com.hertz.hertz_be.global.infra.ai.dto;


import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.user.entity.User;

public record AiSignalRoomDto(
        Long id,
        Category category
) {
    public static AiSignalRoomDto from(SignalRoom room) {
        return new AiSignalRoomDto(
                room.getId(),
                room.getCategory()
        );
    }

    public SignalRoom toEntity(User sender, User receiver) {
        return SignalRoom.builder()
                .id(id)
                .category(category)
                .build();
    }
}