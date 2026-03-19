package com.library.system.dto.book;

import java.util.ArrayList;
import java.util.List;

/**
 * 图书详情响应。
 * 在基础图书信息外，补充简介文案与相似图书列表。
 */
public class BookDetailResponse extends BookResponse {

    private String summary;
    private String availabilityText;
    private List<BookResponse> similarBooks = new ArrayList<>();

    public String getSummary() {
        return summary;
    }

    public void setSummary(String summary) {
        this.summary = summary;
    }

    public String getAvailabilityText() {
        return availabilityText;
    }

    public void setAvailabilityText(String availabilityText) {
        this.availabilityText = availabilityText;
    }

    public List<BookResponse> getSimilarBooks() {
        return similarBooks;
    }

    public void setSimilarBooks(List<BookResponse> similarBooks) {
        this.similarBooks = similarBooks;
    }
}
