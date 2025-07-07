package com.hertz.hertz_be.global.batch;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import org.springframework.stereotype.Component;

import java.util.List;
import java.util.Objects;
import java.util.stream.Collectors;
import java.util.stream.Stream;

@Component
public class TuningReportFilterService {

    public List<SignalRoom> filterTopRooms(List<SignalRoom> rooms, Category category) {
        if (category == Category.FRIEND) {
            return rooms.stream()
                    .collect(Collectors.groupingBy(room -> {
                        String g1 = room.getSenderUser().getGender().name();
                        String g2 = room.getReceiverUser().getGender().name();
                        return Stream.of(g1, g2).sorted().collect(Collectors.joining("-"));
                    }))
                    .entrySet().stream()
                    .filter(e -> List.of("MALE-MALE", "MALE-FEMALE", "FEMALE-FEMALE").contains(e.getKey()))
                    .map(e -> e.getValue().stream()
                            .sorted((a, b) -> Integer.compare(
                                    b.getMessages().size(),
                                    a.getMessages().size()
                            ))
                            .findFirst().orElse(null)
                    )
                    .filter(Objects::nonNull)
                    .toList();

        } else if (category == Category.COUPLE) {
            return rooms.stream()
                    .sorted((a, b) -> Integer.compare(
                            b.getMessages().size(),
                            a.getMessages().size()
                    ))
                    .limit(3)
                    .toList();
        }

        return List.of();
    }
}
