package com.library.system.service;

import com.library.system.dto.book.CategoryRequest;
import com.library.system.dto.book.CategoryResponse;

import java.util.List;

public interface CategoryService {
    CategoryResponse create(CategoryRequest request);
    CategoryResponse update(Long id, CategoryRequest request);
    void delete(Long id);
    List<CategoryResponse> list();
}
