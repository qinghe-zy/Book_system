package com.library.system.dto.book;

import jakarta.validation.constraints.NotBlank;

import java.util.List;

public class BatchLocationRequest {

    private List<Long> bookIds;

    @NotBlank(message = "位置不能为空")
    private String location;

    public List<Long> getBookIds() {
        return bookIds;
    }

    public void setBookIds(List<Long> bookIds) {
        this.bookIds = bookIds;
    }

    public String getLocation() {
        return location;
    }

    public void setLocation(String location) {
        this.location = location;
    }
}
