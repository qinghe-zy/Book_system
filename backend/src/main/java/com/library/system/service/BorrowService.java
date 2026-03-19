package com.library.system.service;

import com.library.system.dto.borrow.BorrowApplyRequest;
import com.library.system.dto.borrow.BorrowRecordResponse;
import com.library.system.dto.borrow.BorrowReviewRequest;

import java.util.List;

public interface BorrowService {
    BorrowRecordResponse apply(BorrowApplyRequest request);
    BorrowRecordResponse approve(Long recordId, BorrowReviewRequest request);
    BorrowRecordResponse reject(Long recordId, BorrowReviewRequest request);
    BorrowRecordResponse cancelApply(Long recordId);
    BorrowRecordResponse confirmPickup(Long recordId);
    BorrowRecordResponse returnBook(Long recordId);
    BorrowRecordResponse renew(Long recordId);
    List<BorrowRecordResponse> myRecords();
    List<BorrowRecordResponse> allRecords();
    List<BorrowRecordResponse> overdueRecords();
}
