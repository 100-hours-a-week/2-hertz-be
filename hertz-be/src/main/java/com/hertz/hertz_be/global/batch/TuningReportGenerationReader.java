package com.hertz.hertz_be.global.batch;

import com.hertz.hertz_be.domain.channel.entity.SignalRoom;
import com.hertz.hertz_be.domain.channel.entity.enums.Category;
import jakarta.persistence.EntityManagerFactory;
import lombok.RequiredArgsConstructor;
import org.springframework.batch.core.configuration.annotation.StepScope;
import org.springframework.batch.item.database.JpaPagingItemReader;
import org.springframework.batch.item.database.builder.JpaPagingItemReaderBuilder;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.context.annotation.Bean;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Map;

@Component
@RequiredArgsConstructor
@Transactional(readOnly = true)
public class TuningReportGenerationReader {

    private final EntityManagerFactory entityManagerFactory;

    @Bean
    @StepScope
    public JpaPagingItemReader<SignalRoom> reader(
            @Value("#{jobParameters['category']}") String category,
            @Value("#{jobParameters['timestamp']}") Long timestamp
    ) {
        String jpql = """
            SELECT DISTINCT sr FROM SignalRoom sr
                JOIN FETCH sr.senderUser sender
                JOIN FETCH sr.receiverUser receiver
                LEFT JOIN FETCH sr.messages msg
                LEFT JOIN FETCH sr.tuningReport tr
                WHERE sr.category = :category
                  AND sr.senderMatchingStatus = 'MATCHED'
                  AND sr.receiverMatchingStatus = 'MATCHED'
                  AND (
                       tr IS NULL
                       OR tr.isVisible = false
                       OR tr.deletedAt IS NOT NULL
                  )
                  AND sr.createdAt <= :end
        """;

        LocalDateTime endDateTime = LocalDateTime.ofInstant(
                java.time.Instant.ofEpochMilli(timestamp),
                java.time.ZoneId.systemDefault()
        );

        return new JpaPagingItemReaderBuilder<SignalRoom>()
                .name("tuningReportReader")
                .entityManagerFactory(entityManagerFactory)
                .queryString(jpql)
                .parameterValues(Map.of(
                        "category", Category.valueOf(category),
                        "end", endDateTime
                ))
                .pageSize(20)
                .saveState(false)
                .transacted(false)
                .build();
    }
}