package com.library.system.dto.borrow;

import com.library.system.entity.enums.BorrowType;
import jakarta.validation.constraints.NotNull;

public class BorrowApplyRequest {

    @NotNull(message = "图书ID不能为空")
    private Long bookId;

    @NotNull(message = "借阅类型不能为空")
    private BorrowType borrowType;

    public Long getBookId() {
        return bookId;
    }

    public void setBookId(Long bookId) {
        this.bookId = bookId;
    }

    public BorrowType getBorrowType() {
        return borrowType;
    }

    public void setBorrowType(BorrowType borrowType) {
        this.borrowType = borrowType;
    }
}
