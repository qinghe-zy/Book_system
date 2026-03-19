package com.library.system.repository;

import com.library.system.entity.BorrowRecord;
import com.library.system.entity.enums.BorrowStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.time.LocalDateTime;
import java.util.List;

public interface BorrowRecordRepository extends JpaRepository<BorrowRecord, Long> {
    List<BorrowRecord> findByUser_IdOrderByIdDesc(Long userId);
    List<BorrowRecord> findByStatusIn(List<BorrowStatus> statuses);
    List<BorrowRecord> findByStatusAndDueTimeBefore(BorrowStatus status, LocalDateTime dueTime);
}
