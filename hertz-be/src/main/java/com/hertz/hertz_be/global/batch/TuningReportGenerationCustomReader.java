package com.hertz.hertz_be.global.batch;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import com.hertz.hertz_be.domain.channel.repository.SignalMessageRepository;
import com.hertz.hertz_be.domain.channel.repository.SignalRoomRepository;
import jakarta.annotation.PostConstruct;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.ItemReader;
import org.springframework.transaction.annotation.Transactional;

import java.time.Instant;
import java.time.LocalDateTime;
import java.time.ZoneId;
import java.util.*;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@StepScope
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TuningReportGenerationCustomReader implements ItemReader<SignalRoom> {

    private final SignalRoomRepository signalRoomRepository;
    private final SignalMessageRepository signalMessageRepository;
    private final String categoryParam;
    private final Long timestamp;

    private List<SignalRoom> selectedRooms;
    private int currentIndex = 0;

    @PostConstruct
    public void init() {
        Category category = Category.valueOf(categoryParam);
        LocalDateTime endDateTime = LocalDateTime.ofInstant(
                Instant.ofEpochMilli(timestamp), ZoneId.systemDefault()
        );

        List<SignalRoom> candidates = signalRoomRepository.findEligibleRooms(category, endDateTime);

        // 채팅 수 계산 후 임시 보관
        candidates.forEach(room ->
                room.setTempMessageCount(signalMessageRepository.countBySignalRoom(room))
        );

        if (category == Category.FRIEND) {
            selectedRooms = candidates.stream()
                    .collect(Collectors.groupingBy(room -> {
                        String g1 = room.getSenderUser().getGender().name();
                        String g2 = room.getReceiverUser().getGender().name();
                        return Stream.of(g1, g2).sorted().collect(Collectors.joining("-"));
                    }))
                    .entrySet().stream()
                    .filter(e -> List.of("MALE-MALE", "MALE-FEMALE", "FEMALE-FEMALE").contains(e.getKey()))
                    .map(e -> e.getValue().stream()
                            .sorted((a, b) -> Integer.compare(b.getTempMessageCount(), a.getTempMessageCount()))
                            .findFirst().orElse(null)
                    )
                    .filter(Objects::nonNull)
                    .toList();

        } else if (category == Category.COUPLE) {
            selectedRooms = candidates.stream()
                    .sorted((a, b) -> Integer.compare(b.getTempMessageCount(), a.getTempMessageCount()))
                    .limit(3)
                    .toList();
        } else {
            selectedRooms = List.of();
        }
    }

    @Override
    public SignalRoom read() {
        if (selectedRooms == null || currentIndex >= selectedRooms.size()) {
            return null;
        }
        return selectedRooms.get(currentIndex++);
    }
}