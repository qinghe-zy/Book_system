package com.library.system.service.impl;

import com.library.system.dto.book.BatchLocationRequest;
import com.library.system.dto.book.BookDetailResponse;
import com.library.system.dto.book.BookRequest;
import com.library.system.dto.book.BookResponse;
import com.library.system.entity.Book;
import com.library.system.entity.Category;
import com.library.system.exception.BusinessException;
import com.library.system.repository.BookRepository;
import com.library.system.repository.CategoryRepository;
import com.library.system.service.BookService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Objects;
import java.util.Set;
import java.util.UUID;

@Service
public class BookServiceImpl implements BookService {

    private final BookRepository bookRepository;
    private final CategoryRepository categoryRepository;

    @Value("${file.upload-dir:uploads/book-covers}")
    private String uploadDir;

    @Value("${file.max-image-size-mb:2}")
    private long maxImageSizeMb;

    private static final Set<String> ALLOWED_EXTENSIONS = Set.of("jpg", "jpeg", "png");

    public BookServiceImpl(BookRepository bookRepository, CategoryRepository categoryRepository) {
        this.bookRepository = bookRepository;
        this.categoryRepository = categoryRepository;
    }

    @Override
    @Transactional
    public BookResponse create(BookRequest request) {
        Book book = new Book();
        setBookFields(book, request);
        book.setCreatedTime(LocalDateTime.now());
        bookRepository.save(book);
        return DtoMapper.toBookResponse(book);
    }

    @Override
    @Transactional
    public BookResponse update(Long id, BookRequest request) {
        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "图书不存在"));
        setBookFields(book, request);
        bookRepository.save(book);
        return DtoMapper.toBookResponse(book);
    }

    @Override
    public void delete(Long id) {
        bookRepository.deleteById(id);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BookResponse> list(String keyword, String categoryCode) {
        String k = (keyword == null || keyword.isBlank()) ? null : keyword;
        String c = (categoryCode == null || categoryCode.isBlank()) ? null : categoryCode;
        return bookRepository.search(k, c).stream().map(DtoMapper::toBookResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public BookDetailResponse detail(Long id, int similarLimit) {
        Book book = bookRepository.findDetailById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "图书不存在"));

        BookDetailResponse response = new BookDetailResponse();
        BookResponse base = DtoMapper.toBookResponse(book);
        response.setId(base.getId());
        response.setTitle(base.getTitle());
        response.setAuthor(base.getAuthor());
        response.setCategoryCode(base.getCategoryCode());
        response.setStock(base.getStock());
        response.setLocation(base.getLocation());
        response.setImageUrl(base.getImageUrl());
        response.setCategoryId(base.getCategoryId());
        response.setCategoryName(base.getCategoryName());
        response.setCreatedTime(base.getCreatedTime());
        response.setAvailabilityText(book.getStock() != null && book.getStock() > 0
                ? "当前可借，馆藏状态稳定，可直接发起借阅。"
                : "当前库存不足，建议查看同类图书或稍后再试。");
        response.setSummary(buildSummary(book));
        response.setSimilarBooks(bookRepository.findSimilarBooks(book.getId(), book.getCategoryCode(), book.getAuthor()).stream()
                .filter(candidate -> !Objects.equals(candidate.getId(), book.getId()))
                .limit(Math.max(1, Math.min(similarLimit, 8)))
                .map(DtoMapper::toBookResponse)
                .toList());
        return response;
    }


    @Override
    @Transactional
    public void batchUpdateLocation(BatchLocationRequest request) {
        if (request.getBookIds() == null || request.getBookIds().isEmpty()) {
            throw new BusinessException("bookIds 不能为空");
        }
        List<Book> books = bookRepository.findAllById(request.getBookIds());
        books.forEach(book -> book.setLocation(request.getLocation()));
        bookRepository.saveAll(books);
    }

    @Override
    @Transactional
    public void batchDelete(List<Long> ids) {
        bookRepository.deleteAllById(ids);
    }

    @Override
    @Transactional
    public BookResponse uploadCover(Long id, MultipartFile file, String publicUrlPrefix) {
        if (file == null || file.isEmpty()) {
            throw new BusinessException("请选择图片文件");
        }

        long maxBytes = maxImageSizeMb * 1024 * 1024;
        if (file.getSize() > maxBytes) {
            throw new BusinessException("图片大小不能超过 " + maxImageSizeMb + "MB");
        }

        String originalName = file.getOriginalFilename() == null ? "" : file.getOriginalFilename();
        String extension = extractExtension(originalName);
        if (!ALLOWED_EXTENSIONS.contains(extension)) {
            throw new BusinessException("仅支持 jpg/jpeg/png 图片");
        }

        String contentType = file.getContentType() == null ? "" : file.getContentType().toLowerCase();
        if (!contentType.startsWith("image/")) {
            throw new BusinessException("上传文件不是图片类型");
        }

        Book book = bookRepository.findById(id)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "图书不存在"));

        String filename = UUID.randomUUID().toString().replace("-", "") + "." + extension;
        Path targetDir = Path.of(uploadDir).normalize().toAbsolutePath();
        Path targetPath = targetDir.resolve(filename).normalize();

        if (!targetPath.startsWith(targetDir)) {
            throw new BusinessException("非法文件路径");
        }

        try {
            Files.createDirectories(targetDir);
            Files.copy(file.getInputStream(), targetPath, StandardCopyOption.REPLACE_EXISTING);
        } catch (IOException ex) {
            throw new BusinessException("图片保存失败: " + ex.getMessage());
        }

        String publicUrl = publicUrlPrefix + "/" + filename;
        book.setImageUrl(publicUrl);
        bookRepository.save(book);
        return DtoMapper.toBookResponse(book);
    }

    /**
     * 统一维护图书基础字段，保证新增与编辑逻辑一致。
     */
    private void setBookFields(Book book, BookRequest request) {
        book.setTitle(request.getTitle());
        book.setAuthor(request.getAuthor());
        book.setCategoryCode(request.getCategoryCode());
        book.setStock(request.getStock());
        book.setLocation(request.getLocation());

        if (request.getCategoryId() != null) {
            Category category = categoryRepository.findById(request.getCategoryId())
                    .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "分类不存在"));
            book.setCategory(category);
        } else {
            book.setCategory(null);
        }
    }


    /**
     * 根据现有图书字段生成可展示的简介文案，避免详情页出现生硬空白。
     */
    private String buildSummary(Book book) {
        String categoryName = book.getCategory() != null ? book.getCategory().getName() : "综合馆藏";
        String availability = book.getStock() != null && book.getStock() > 0
                ? "当前仍有库存，可直接进入借阅流程。"
                : "当前库存不足，建议结合相似图书继续浏览。";
        return "《" + book.getTitle() + "》由 " + book.getAuthor() + " 著，归属“" + categoryName + "”分类，分类号为 "
                + book.getCategoryCode() + "，馆藏位置在 " + book.getLocation() + "。" + availability;
    }

    private String extractExtension(String filename) {
        int dotIndex = filename.lastIndexOf('.');
        if (dotIndex < 0 || dotIndex == filename.length() - 1) {
            return "";
        }
        return filename.substring(dotIndex + 1).toLowerCase();
    }
}
