package com.library.system.service.impl;

import com.library.system.dto.book.CategoryRequest;
import com.library.system.dto.book.CategoryResponse;
import com.library.system.entity.Category;
import com.library.system.exception.BusinessException;
import com.library.system.repository.CategoryRepository;
import com.library.system.service.CategoryService;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;

import java.util.List;

@Service
public class CategoryServiceImpl implements CategoryService {

    private final CategoryRepository categoryRepository;

    public CategoryServiceImpl(CategoryRepository categoryRepository) {
        this.categoryRepository = categoryRepository;
    }

    @Override
    public CategoryResponse create(CategoryRequest request) {
        categoryRepository.findByName(request.getName()).ifPresent(c -> {
            throw new BusinessException("分类已存在");
        });

        Category category = new Category();
        category.setName(request.getName());
        categoryRepository.save(category);

        return DtoMapper.toCategoryResponse(category);
    }

    @Override
    public CategoryResponse update(Long id, CategoryRequest request) {
        Category category = categoryRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "分类不存在"));
        category.setName(request.getName());
        categoryRepository.save(category);
        return DtoMapper.toCategoryResponse(category);
    }

    @Override
    public void delete(Long id) {
        categoryRepository.deleteById(id);
    }

    @Override
    public List<CategoryResponse> list() {
        return categoryRepository.findAll().stream().map(DtoMapper::toCategoryResponse).toList();
    }
}
