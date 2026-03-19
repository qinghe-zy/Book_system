package com.library.system.dto.book;

import jakarta.validation.constraints.NotBlank;

public class CategoryRequest {

    @NotBlank(message = "分类名不能为空")
    private String name;

    public String getName() {
        return name;
    }

    public void setName(String name) {
        this.name = name;
    }
}
