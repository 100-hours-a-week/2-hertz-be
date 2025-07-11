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
import java.util.function.Function;

@Slf4j
@Component
@RequiredArgsConstructor
public class TuningReportCacheManager {
    private final RedisTemplate<String, String> redisTemplate;
    private final ObjectMapper objectMapper = new ObjectMapper()
            .registerModule(new JavaTimeModule())
            .disable(SerializationFeature.WRITE_DATES_AS_TIMESTAMPS);

    private static final Duration TUNING_REPORT_TTL = Duration.ofMinutes(35);
    private static final String DIRTY_SET = "dirty:reports";

    public String pageListKey(String domain) {
        return String.format("reports:domain=%s:page=0:size=10:sort=%s:list", domain, TuningReportSortType.LATEST);
    }

    public String userDomainKey(Long userId) {
        return "user:" + userId + ":domain";
    }

    public String reportItemKey(Long reportId) {
        return String.format("report:item:%d", reportId);
    }

    public String userKey(Long reportId, Long userId) {
        return String.format("reports:%d:user:%d", reportId, userId);
    }

    // 특정 사용자의 userDomainKey TTL 갱신
    public void refreshUserDomainTTL(Long userId) {
        String key = userDomainKey(userId);
        if (Boolean.TRUE.equals(redisTemplate.hasKey(key))) {
            redisTemplate.expire(key, getTTLDurForTuningReport());
        }
    }


    // 특정 사용자의 모든 userKey(reportId:reaction) TTL 갱신
    public void refreshAllUserReactionTTLByScan(Long userId) {
        String matchPattern = String.format("reports:*:user:%d", userId);
        ScanOptions options = ScanOptions.scanOptions().match(matchPattern).count(100).build();

        Cursor<byte[]> cursor = redisTemplate.getConnectionFactory()
                .getConnection()
                .scan(options);

        while (cursor.hasNext()) {
            String key = new String(cursor.next());
            redisTemplate.expire(key, getTTLDurForTuningReport());
        }
    }

    // 특정 domain에 해당하는 페이지 캐시의 모든 reportItemKey TTL 갱신
    public void refreshReportListTTL(String domain) {
        String listKey = pageListKey(domain);
        List<String> reportIds = redisTemplate.opsForList().range(listKey, 0, -1);
        if (reportIds == null || reportIds.isEmpty()) return;

        for (String reportId : reportIds) {
            String reportKey = reportItemKey(Long.parseLong(reportId));
            if (Boolean.TRUE.equals(redisTemplate.hasKey(reportKey))) {
                redisTemplate.expire(reportKey, getTTLDurForTuningReport());
            }
        }

        // 리스트 자체의 TTL도 갱신
        redisTemplate.expire(listKey, getTTLDurForTuningReport());
    }

    public String getUserDomain(Long userId, Function<Long, String> dbFetcher) {
        String key = userDomainKey(userId);
        String domain = redisTemplate.opsForValue().get(key);
        if (domain == null) {
            domain = dbFetcher.apply(userId);
            if (domain != null) {
                redisTemplate.opsForValue().set(key, domain, TUNING_REPORT_TTL);
            }
        }
        return domain;
    }

    // === 최신 게시글 10에 대한 Cache : 페이지별 리포트 목록 (List) ===
    public List<TuningReportListResponse.ReportItem> getCachedReportList(String domain) {
        String listKey = pageListKey(domain);
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

    public void cacheReportList(String domain, List<TuningReportListResponse.ReportItem> items) {
        String listKey = pageListKey(domain);
        redisTemplate.delete(listKey);

        for (TuningReportListResponse.ReportItem item : items) {
            try {
                String reportKey = reportItemKey(item.getReportId());
                String json = objectMapper.writeValueAsString(item);
                redisTemplate.opsForValue().set(reportKey, json, getTTLDurForTuningReport());
                redisTemplate.opsForList().rightPush(listKey, item.getReportId().toString());
            } catch (JsonProcessingException e) {
                log.warn("❌ ReportItem 직렬화 실패: {}", e.getMessage());
            }
        }
        redisTemplate.expire(listKey, getTTLDurForTuningReport());
    }

    public boolean isReportCached(Long reportId) {
        return Boolean.TRUE.equals(redisTemplate.hasKey(reportItemKey(reportId)));
    }

    // === 튜닝 레포트 반응 관리를 위한 Cache : 유저별 반응 상태 (Hash) ===
    public void setUserReaction(Long reportId, Long userId, ReactionType type, boolean reacted) {
        String key = userKey(reportId, userId);
        try {
            redisTemplate.opsForHash().put(key, type.name(), reacted ? "1" : "0");
            redisTemplate.expire(key, getTTLDurForTuningReport());
        } catch (Exception e) {
            log.warn("❌ 유저 리액션 캐싱 실패: {}", e.getMessage());
        }
    }

    public Boolean getUserReaction(Long reportId, Long userId, ReactionType type) {
        String key = userKey(reportId, userId);
        Object v = redisTemplate.opsForHash().get(key, type.name());
        return v != null && "1".equals(v);
    }

    public Duration getTTLDurForTuningReport() {
        return TUNING_REPORT_TTL;
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
