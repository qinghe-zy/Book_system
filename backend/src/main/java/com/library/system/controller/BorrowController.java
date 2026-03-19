package com.library.system.controller;

import com.library.system.dto.borrow.BorrowApplyRequest;
import com.library.system.dto.borrow.BorrowRecordResponse;
import com.library.system.dto.borrow.BorrowReviewRequest;
import com.library.system.dto.common.ApiResponse;
import com.library.system.service.BorrowService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;

import java.util.List;

@RestController
@RequestMapping("/api/borrow")
public class BorrowController {

    private final BorrowService borrowService;

    public BorrowController(BorrowService borrowService) {
        this.borrowService = borrowService;
    }

    @PostMapping("/apply")
    public ApiResponse<BorrowRecordResponse> apply(@Valid @RequestBody BorrowApplyRequest request) {
        return ApiResponse.success("申请成功", borrowService.apply(request));
    }

    @GetMapping("/my")
    public ApiResponse<List<BorrowRecordResponse>> my() {
        return ApiResponse.success(borrowService.myRecords());
    }

    @PutMapping("/{recordId}/cancel")
    public ApiResponse<BorrowRecordResponse> cancel(@PathVariable Long recordId) {
        return ApiResponse.success("已撤销申请", borrowService.cancelApply(recordId));
    }

    @PutMapping("/{recordId}/pickup")
    public ApiResponse<BorrowRecordResponse> pickup(@PathVariable Long recordId) {
        return ApiResponse.success("已确认取书", borrowService.confirmPickup(recordId));
    }

    @PutMapping("/{recordId}/return")
    public ApiResponse<BorrowRecordResponse> returnBook(@PathVariable Long recordId) {
        return ApiResponse.success("归还成功", borrowService.returnBook(recordId));
    }

    @PutMapping("/{recordId}/renew")
    public ApiResponse<BorrowRecordResponse> renew(@PathVariable Long recordId) {
        return ApiResponse.success("续借成功", borrowService.renew(recordId));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/all")
    public ApiResponse<List<BorrowRecordResponse>> all() {
        return ApiResponse.success(borrowService.allRecords());
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{recordId}/approve")
    public ApiResponse<BorrowRecordResponse> approve(@PathVariable Long recordId,
                                                     @RequestBody(required = false) BorrowReviewRequest request) {
        return ApiResponse.success("审核通过", borrowService.approve(recordId, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{recordId}/reject")
    public ApiResponse<BorrowRecordResponse> reject(@PathVariable Long recordId,
                                                    @RequestBody(required = false) BorrowReviewRequest request) {
        return ApiResponse.success("处理完成", borrowService.reject(recordId, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @GetMapping("/admin/overdue")
    public ApiResponse<List<BorrowRecordResponse>> overdue() {
        return ApiResponse.success(borrowService.overdueRecords());
    }
}
