package com.hertz.hertz_be.domain.tuningreport.controller;

import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportListResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.TuningReportSortType;
import com.hertz.hertz_be.domain.tuningreport.service.TuningReportService;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.security.core.annotation.AuthenticationPrincipal;
import org.springframework.web.bind.annotation.*;

@RestController
@RequestMapping("/api/v2")
@RequiredArgsConstructor
@Tag(name = "튜닝 리포트 관련 API")
public class TuningReportController {

    private final TuningReportService tuningReportService;

    /**
     * 튜닝 리포트 목록 반환 API
     * @param page
     * @param size
     * @param sort
     * @param userId
     * @author daisy.lee
     */
    @GetMapping("/reports")
    @Operation(summary = "튜닝 리포트 목록 반환 API")
    public ResponseEntity<ResponseDto<TuningReportListResponse>> createTuningReport (@RequestParam(defaultValue = "0") int page,
                                                                                     @RequestParam(defaultValue = "20") int size,
                                                                                     @RequestParam(defaultValue = "LATEST") TuningReportSortType sort,
                                                                                     @AuthenticationPrincipal Long userId) {

        TuningReportListResponse response = tuningReportService.getReportList(userId, page, size, sort);
        return ResponseEntity.ok(new ResponseDto<>(
                ResponseCode.REPORT_LIST_FETCH_SUCCESS,
                "튜닝 리포트가 정상적으로 조회되었습니다.",
                response
        ));
    }
}
