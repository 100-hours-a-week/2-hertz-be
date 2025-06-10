package com.hertz.hertz_be.global.batch;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.stereotype.Component;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
public class TuningReportReader {

    private final EntityManagerFactory entityManagerFactory;

    public JpaPagingItemReader<SignalRoom> reader(String category) {
        String jpql = """
        SELECT sr FROM SignalRoom sr
        WHERE sr.senderMatchingStatus = 'MATCHED'
          AND sr.receiverMatchingStatus = 'MATCHED'
          AND sr.category = :category
          AND sr.createdAt <= :end
    """;

        Map<String, Object> parameters = Map.of(
                "category", Category.valueOf(category),
                "end", LocalDateTime.now().minusDays(1).withHour(23).withMinute(59).withSecond(59)
        );

        return new JpaPagingItemReaderBuilder<SignalRoom>()
                .name("categoryReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(jpql)
                .parameterValues(parameters)
                .pageSize(20)
                .build();
    }
}
