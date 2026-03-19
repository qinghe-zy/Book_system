package com.library.system.dto.borrow;

public class BorrowReviewRequest {

    private Integer loanDays;
    private String comment;

    public Integer getLoanDays() {
        return loanDays;
    }

    public void setLoanDays(Integer loanDays) {
        this.loanDays = loanDays;
    }

    public String getComment() {
        return comment;
    }

    public void setComment(String comment) {
        this.comment = comment;
    }
}
