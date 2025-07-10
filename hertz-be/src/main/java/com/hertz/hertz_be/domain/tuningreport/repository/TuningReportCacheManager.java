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
import java.util.stream.Collectors;

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

    // 페이지별 Hash 키 생성
    public String pageKey() {
        return String.format("reports:page=%d:size=%d:sort=%s", 0, 10, TuningReportSortType.LATEST);
    }

    // 사용자별 Hash 키 생성
    public String userKey(Long reportId, Long userId) {
        return String.format("reports:%d:user:%d", reportId, userId);
    }

    // Redis에서 reports:page=* 형식으로 저장된 모든 페이지 캐시의 key 목록 조회
    public Set<String> scanPageKeys() {
        return redisTemplate.execute((RedisCallback<Set<String>>) conn -> {
            Set<String> result = new HashSet<>();
            Cursor<byte[]> cursor = conn.scan(ScanOptions.scanOptions()
                    .match("reports:page=*" ).count(1000).build());
            cursor.forEachRemaining(b -> result.add(new String(b)));
            return result;
        });
    }

    // === 최신 게시글 10에 대한 Cache : 페이지별 리포트 목록 (Hash) ===

    // 현재 페이지(0페이지, size=10, 정렬 기준=LATEST)의 게시글 목록을 Redis에서 반환
    public List<TuningReportListResponse.ReportItem> getCachedReportList() {
        String key = pageKey();
        if (!Boolean.TRUE.equals(redisTemplate.hasKey(key))) return null;
        BoundHashOperations<String, String, String> hash = redisTemplate.boundHashOps(key);
        List<TuningReportListResponse.ReportItem> items = new ArrayList<>();
        for (String json : hash.values()) {
            try {
                // 각각의 JSON 문자열을 ReportItem 객체로 변환합니다 (역직렬화)
                items.add(objectMapper.readValue(json, TuningReportListResponse.ReportItem.class));
            } catch (JsonProcessingException ignored) {
                log.info("❌캐싱된 튜닝레포트 목록을 redis에서 불러오기 실패 {}", ignored);
            }
        }
        return items;
    }

    // 현재 페이지에 해당하는 게시글 목록을 Redis에 캐시
    public void cacheReportList(List<TuningReportListResponse.ReportItem> items) {
        String key = pageKey();
        // 해당 key에 연결된 Redis `Hash`에 대해 편리하게 조작할 수 있는 인터페이스 참조
        BoundHashOperations<String, String, String> hash = redisTemplate.boundHashOps(key);
        hash.getOperations().expire(key, PAGE_TTL);

        for (String field : hash.keys()) {
            hash.delete(field);
        }

        for (TuningReportListResponse.ReportItem item : items) {
            try {
                String json = objectMapper.writeValueAsString(item);
                hash.put(item.getReportId().toString(), json);
            } catch (JsonProcessingException ignored) {
                log.info("❌특정 튜닝레포트  redis에 캐싱 실패 {}", ignored);
            }
        }
    }

    public boolean isReportCached(Long reportId) {
        return Boolean.TRUE.equals(
                redisTemplate.boundHashOps(pageKey()).hasKey(reportId.toString())
        );
    }

    // === 튜닝 레포트 반응 관리를 위한 Cache : 유저별 반응 상태 (Hash) ===

    // 유저가 해당 게시글에 특정 반응을 했는지 여부를 Redis에 저장
    public void setUserReaction(Long reportId, Long userId, ReactionType type, boolean reacted) {
        String key = userKey(reportId, userId);
        try {
            log.info("✅ 유저별 반응 상태 캐싱 되기 직전 reportId:{}", reportId);
            redisTemplate.opsForHash().put(key, type.name(), reacted ? "1" : "0");
            redisTemplate.expire(key, PAGE_TTL);
        } catch (Exception e) {
            log.info("❌ 유저별 반응 상태 캐싱 되기 직전에 redis에 저장 실패 {}", e);
        }
    }

    // 해당 유저가 특정 게시글에 특정 반응을 눌렀는지 확인
    public Boolean getUserReaction(Long reportId, Long userId, ReactionType type) {
        String key = userKey(reportId, userId);
        Object v = redisTemplate.opsForHash().get(key, type.name());
        return v != null && "1".equals(v);
    }

    // 유저가 해당 게시글에 모든 반응 타입을 눌렀는지 한꺼번
    public Map<ReactionType, Boolean> getUserReactionMap(Long reportId, Long userId) {
        String key = userKey(reportId, userId);
        Map<Object, Object> raw = redisTemplate.opsForHash().entries(key);
        return Arrays.stream(ReactionType.values())
                .collect(Collectors.toMap(
                        r -> r,
                        r -> "1".equals(raw.getOrDefault(r.name(), "0"))
                ));
    }

    // MyReactions DTO 변환
    public TuningReportListResponse.MyReactions toMyReactions(Map<ReactionType, Boolean> map) {
        return new TuningReportListResponse.MyReactions(
                map.getOrDefault(ReactionType.CELEBRATE, false),
                map.getOrDefault(ReactionType.THUMBS_UP, false),
                map.getOrDefault(ReactionType.LAUGH, false),
                map.getOrDefault(ReactionType.EYES, false),
                map.getOrDefault(ReactionType.HEART, false)
        );
    }

    // Dirty Set
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
