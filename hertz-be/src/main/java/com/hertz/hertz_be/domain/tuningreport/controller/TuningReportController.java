package com.hertz.hertz_be.domain.tuningreport.controller;

import com.hertz.hertz_be.domain.tuningreport.dto.request.TuningReportReactionToggleRequest;
import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportListResponse;
import com.hertz.hertz_be.domain.tuningreport.dto.response.TuningReportReactionResponse;
import com.hertz.hertz_be.domain.tuningreport.entity.enums.TuningReportSortType;
import com.hertz.hertz_be.domain.tuningreport.service.TuningReportReactionService;
import com.hertz.hertz_be.domain.tuningreport.service.TuningReportService;
import com.hertz.hertz_be.global.common.ResponseCode;
import com.hertz.hertz_be.global.common.ResponseDto;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
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
    private final TuningReportReactionService tuningReportReactionService;

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
                                                                                     @RequestParam(defaultValue = "10") int size,
                                                                                     @RequestParam(defaultValue = "LATEST") TuningReportSortType sort,
                                                                                     @AuthenticationPrincipal Long userId) {

        TuningReportListResponse response = tuningReportService.getReportList(userId, page, size, sort);

        if(!response.list().isEmpty()) {
            return ResponseEntity.ok(new ResponseDto<>(
                    ResponseCode.REPORT_LIST_FETCH_SUCCESS,
                    "튜닝 리포트가 정상적으로 조회되었습니다.",
                    response
            ));
        } else {
            return ResponseEntity.ok(new ResponseDto<>(
                    ResponseCode.NO_REPORTS,
                    "새로운 튜닝 리포트가 없습니다.",
                    null
            ));
        }


    }

    /**
     * 특정 튜닝 리포트 리액션 토글 API
     * @param reportId
     * @param userId
     * @author daisy.lee
     */
    @PutMapping("/reports/{reportId}/reactions")
    @Operation(summary = "특정 튜닝 리포트 리액션 토글 API")
    public ResponseEntity<ResponseDto<TuningReportReactionResponse>> toggleTuningReport (@PathVariable Long reportId,
                                                                                         @RequestBody @Valid TuningReportReactionToggleRequest request,
                                                                                         @AuthenticationPrincipal Long userId) {

        TuningReportReactionResponse response = tuningReportReactionService.toggleReportReaction(userId, reportId, request);

        if(response.isReacted()) {
            return ResponseEntity.ok(new ResponseDto<>(
                    ResponseCode.REACTION_ADDED,
                    "리액션이 성공적으로 추가되었습니다.",
                    response
            ));
        } else {
            return ResponseEntity.ok(new ResponseDto<>(
                    ResponseCode.REACTION_REMOVED,
                    "리액션이 성공적으로 제거되었습니다.",
                    response
            ));
        }

    }
}
