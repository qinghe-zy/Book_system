package com.library.system.service.impl;

import com.library.system.dto.book.BookResponse;
import com.library.system.dto.book.CategoryResponse;
import com.library.system.dto.borrow.BorrowRecordResponse;
import com.library.system.dto.recommendation.RecommendationResponse;
import com.library.system.dto.user.UserResponse;
import com.library.system.entity.*;

public final class DtoMapper {

    private DtoMapper() {
    }

    public static UserResponse toUserResponse(User user) {
        UserResponse response = new UserResponse();
        response.setId(user.getId());
        response.setUsername(user.getUsername());
        response.setEmail(user.getEmail());
        response.setInterestTags(user.getInterestTags());
        response.setRole(user.getRole().name());
        response.setRegisterTime(user.getRegisterTime());
        return response;
    }

    public static CategoryResponse toCategoryResponse(Category category) {
        CategoryResponse response = new CategoryResponse();
        response.setId(category.getId());
        response.setName(category.getName());
        return response;
    }

    public static BookResponse toBookResponse(Book book) {
        BookResponse response = new BookResponse();
        response.setId(book.getId());
        response.setTitle(book.getTitle());
        response.setAuthor(book.getAuthor());
        response.setCategoryCode(book.getCategoryCode());
        response.setStock(book.getStock());
        response.setLocation(book.getLocation());
        response.setImageUrl(book.getImageUrl());
        response.setCreatedTime(book.getCreatedTime());
        if (book.getCategory() != null) {
            try {
                response.setCategoryId(book.getCategory().getId());
                response.setCategoryName(book.getCategory().getName());
            } catch (Exception ignored) {
                // 在关闭 open-in-view 时，兜底避免懒加载代理导致接口500
            }
        }
        return response;
    }

    public static BorrowRecordResponse toBorrowResponse(BorrowRecord record) {
        BorrowRecordResponse response = new BorrowRecordResponse();
        response.setId(record.getId());
        response.setUserId(record.getUser().getId());
        response.setUsername(record.getUser().getUsername());
        response.setBookId(record.getBook().getId());
        response.setBookTitle(record.getBook().getTitle());
        response.setBorrowTime(record.getBorrowTime());
        response.setReturnTime(record.getReturnTime());
        response.setDueTime(record.getDueTime());
        response.setApprovedTime(record.getApprovedTime());
        response.setStatus(record.getStatus().name());
        response.setBorrowType(record.getBorrowType().name());
        response.setRenewCount(record.getRenewCount());
        response.setPickupCode(record.getPickupCode());
        response.setReviewComment(record.getReviewComment());
        return response;
    }

    public static RecommendationResponse toRecommendationResponse(Recommendation recommendation) {
        RecommendationResponse response = new RecommendationResponse();
        response.setUserId(recommendation.getUser().getId());
        response.setBookId(recommendation.getBook().getId());
        response.setBookTitle(recommendation.getBook().getTitle());
        response.setImageUrl(recommendation.getBook().getImageUrl());
        response.setReason(recommendation.getReason());
        response.setModelScore(recommendation.getModelScore());
        response.setFinalScore(recommendation.getFinalScore());
        response.setGeneratedTime(recommendation.getGeneratedTime());
        return response;
    }
}
