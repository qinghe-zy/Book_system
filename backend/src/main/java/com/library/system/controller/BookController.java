package com.library.system.controller;

import com.library.system.dto.book.BatchLocationRequest;
import com.library.system.dto.book.BookDetailResponse;
import com.library.system.dto.book.BookRequest;
import com.library.system.dto.book.BookResponse;
import com.library.system.dto.common.ApiResponse;
import com.library.system.service.BookService;
import jakarta.validation.Valid;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.multipart.MultipartFile;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.util.List;

@RestController
@RequestMapping("/api/books")
public class BookController {

    private final BookService bookService;

    public BookController(BookService bookService) {
        this.bookService = bookService;
    }

    @GetMapping
    public ApiResponse<List<BookResponse>> list(@RequestParam(required = false) String keyword,
                                                @RequestParam(required = false) String categoryCode) {
        return ApiResponse.success(bookService.list(keyword, categoryCode));
    }


    @GetMapping("/{id}")
    public ApiResponse<BookDetailResponse> detail(@PathVariable Long id,
                                                  @RequestParam(required = false, defaultValue = "4") int similarLimit) {
        return ApiResponse.success(bookService.detail(id, similarLimit));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping
    public ApiResponse<BookResponse> create(@Valid @RequestBody BookRequest request) {
        return ApiResponse.success("创建成功", bookService.create(request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/{id}")
    public ApiResponse<BookResponse> update(@PathVariable Long id, @Valid @RequestBody BookRequest request) {
        return ApiResponse.success("更新成功", bookService.update(id, request));
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/{id}")
    public ApiResponse<Void> delete(@PathVariable Long id) {
        bookService.delete(id);
        return ApiResponse.success("删除成功", null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PutMapping("/batch/location")
    public ApiResponse<Void> batchLocation(@Valid @RequestBody BatchLocationRequest request) {
        bookService.batchUpdateLocation(request);
        return ApiResponse.success("批量更新成功", null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @DeleteMapping("/batch")
    public ApiResponse<Void> batchDelete(@RequestBody List<Long> ids) {
        bookService.batchDelete(ids);
        return ApiResponse.success("批量删除成功", null);
    }

    @PreAuthorize("hasRole('ADMIN')")
    @PostMapping("/{id}/cover")
    public ApiResponse<BookResponse> uploadCover(@PathVariable Long id, @RequestPart("file") MultipartFile file) {
        String publicPrefix = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/api/files/book-covers")
                .toUriString();
        return ApiResponse.success("封面上传成功", bookService.uploadCover(id, file, publicPrefix));
    }
}
