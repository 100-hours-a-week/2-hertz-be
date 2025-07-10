package com.hertz.hertz_be.domain.tuningreport.repository;

import com.fasterxml.jackson.core.JsonProcessingException;
import com.fasterxml.jackson.databind.ObjectMapper;
import com.fasterxml.jackson.databind.SerializationFeature;
import com.fasterxml.jackson.datatype.jsr310.JavaTimeModule;
import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportListResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.ReactionType;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.TuningReportSortType;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.data.redis.core.*;
import org.springframework.stereotype.Component;

import java.time.Duration;
import java.util.*;

@Slf4j
@Component
@RequiredArgsConstructor
public class TuningReportCacheManager {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final Duration PAGE_TTL = Duration.ofMinutes(35);
    private static final String DIRTY_SET = "dirty:reports";

    public String pageListKey() {
        return String.format("reports:page=0:size=10:sort=%s:list", TuningReportSortType.LATEST);
    }

    public String reportItemKey(Long reportId) {
        return String.format("report:item:%d", reportId);
    }

    public String userKey(Long reportId, Long userId) {
        return String.format("reports:%d:user:%d", reportId, userId);
    }

    // Redis에서 reports:page=* 형식으로 저장된 모든 페이지 캐시의 key 목록 조회
    public Set<String> scanPageKeys() {
        return redisTemplate.execute((RedisCallback<Set<String>>) conn -> {
            Set<String> result = new HashSet<>();
            Cursor<byte[]> cursor = conn.scan(ScanOptions.scanOptions()
                    .match("reports:page=*:list").count(1000).build());
            cursor.forEachRemaining(b -> result.add(new String(b)));
            return result;
        });
    }

    // === 최신 게시글 10에 대한 Cache : 페이지별 리포트 목록 (Hash) ===
    public List<TuningReportListResponse.ReportItem> getCachedReportList() {
        String listKey = pageListKey();
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(listKey))) return null;

        List<String> idList = redisTemplate.opsForList().range(listKey, 0, -1);
        if (idList == null || idList.isEmpty()) return null;

        List<TuningReportListResponse.ReportItem> result = new ArrayList<>();
        for (String reportId : idList) {
            String reportKey = reportItemKey(Long.parseLong(reportId));
            String json = redisTemplate.opsForValue().get(reportKey);
            if (json != null) {
                try {
                    result.add(objectMapper.readValue(json, TuningReportListResponse.ReportItem.class));
                } catch (JsonProcessingException e) {
                    log.warn("❌ ReportItem 역직렬화 실패: {}", e.getMessage());
                }
            }
        }
        return result;
    }

    public void cacheReportList(List<TuningReportListResponse.ReportItem> items) {
        String listKey = pageListKey();
        redisTemplate.delete(listKey);

        for (TuningReportListResponse.ReportItem item : items) {
            try {
                String reportKey = reportItemKey(item.getReportId());
                String json = objectMapper.writeValueAsString(item);
                redisTemplate.opsForValue().set(reportKey, json, PAGE_TTL);
                redisTemplate.opsForList().rightPush(listKey, item.getReportId().toString());
            } catch (JsonProcessingException e) {
                log.warn("❌ ReportItem 직렬화 실패: {}", e.getMessage());
            }
        }
        redisTemplate.expire(listKey, PAGE_TTL);
    }

    public boolean isReportCached(Long reportId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(reportItemKey(reportId)));
    }

    // === 튜닝 레포트 반응 관리를 위한 Cache : 유저별 반응 상태 (Hash) ===
    public void setUserReaction(Long reportId, Long userId, ReactionType type, boolean reacted) {
        String key = userKey(reportId, userId);
        try {
            redisTemplate.opsForHash().put(key, type.name(), reacted ? "1" : "0");
            redisTemplate.expire(key, PAGE_TTL);
        } catch (Exception e) {
            log.warn("❌ 유저 리액션 캐싱 실패: {}", e.getMessage());
        }
    }

    public Boolean getUserReaction(Long reportId, Long userId, ReactionType type) {
        String key = userKey(reportId, userId);
        Object v = redisTemplate.opsForHash().get(key, type.name());
        return v != null && "1".equals(v);
    }

    public boolean hasUserReactionCached(Long reportId, Long userId) {
        String key = userKey(reportId, userId);
        return Boolean.TRUE.equals(redisTemplate.hasKey(key));
    }

    public void markDirty(Long reportId) {
        redisTemplate.opsForSet().add(DIRTY_SET, reportId.toString());
    }

    public Set<String> getDirtyReportIds() {
        return redisTemplate.opsForSet().members(DIRTY_SET);
    }

    public void clearDirtyReportId(String reportId) {
        redisTemplate.opsForSet().remove(DIRTY_SET, reportId);
    }
}
