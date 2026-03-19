package com.library.system.repository;

import com.library.system.entity.Book;
import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;

import java.util.List;
import java.util.Optional;

public interface BookRepository extends JpaRepository<Book, Long> {

    @Query("SELECT b FROM Book b LEFT JOIN FETCH b.category WHERE b.id = :id")
    Optional<Book> findDetailById(@Param("id") Long id);

    @Query("""
        SELECT b FROM Book b
        LEFT JOIN FETCH b.category c
        WHERE (:keyword IS NULL OR LOWER(b.title) LIKE LOWER(CONCAT('%', :keyword, '%'))
           OR LOWER(b.author) LIKE LOWER(CONCAT('%', :keyword, '%')))
          AND (:categoryCode IS NULL OR b.categoryCode = :categoryCode)
        ORDER BY b.createdTime DESC
        """)
    List<Book> search(@Param("keyword") String keyword, @Param("categoryCode") String categoryCode);

    @Query("""
        SELECT b FROM Book b
        LEFT JOIN FETCH b.category c
        WHERE b.id <> :bookId
          AND ((:categoryCode IS NOT NULL AND b.categoryCode = :categoryCode)
            OR (:author IS NOT NULL AND LOWER(b.author) = LOWER(:author)))
        ORDER BY CASE WHEN b.categoryCode = :categoryCode THEN 0 ELSE 1 END, b.createdTime DESC
        """)
    List<Book> findSimilarBooks(@Param("bookId") Long bookId,
                                @Param("categoryCode") String categoryCode,
                                @Param("author") String author);
}
