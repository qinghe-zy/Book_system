package com.library.system.service;

import com.library.system.dto.book.BatchLocationRequest;
import com.library.system.dto.book.BookRequest;
import com.library.system.dto.book.BookDetailResponse;
import com.library.system.dto.book.BookResponse;
import org.springframework.web.multipart.MultipartFile;

import java.util.List;

public interface BookService {
    BookResponse create(BookRequest request);
    BookResponse update(Long id, BookRequest request);
    void delete(Long id);
    List<BookResponse> list(String keyword, String categoryCode);

    /**
     * 获取图书详情与相似图书。
     */
    BookDetailResponse detail(Long id, int similarLimit);
    void batchUpdateLocation(BatchLocationRequest request);
    void batchDelete(List<Long> ids);
    BookResponse uploadCover(Long id, MultipartFile file, String publicUrlPrefix);
}
