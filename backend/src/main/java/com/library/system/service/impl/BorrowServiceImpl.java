package com.library.system.service.impl;

import com.library.system.dto.borrow.BorrowApplyRequest;
import com.library.system.dto.borrow.BorrowRecordResponse;
import com.library.system.dto.borrow.BorrowReviewRequest;
import com.library.system.entity.Book;
import com.library.system.entity.BorrowRecord;
import com.library.system.entity.User;
import com.library.system.entity.enums.BorrowStatus;
import com.library.system.entity.enums.BorrowType;
import com.library.system.entity.enums.Role;
import com.library.system.exception.BusinessException;
import com.library.system.repository.BookRepository;
import com.library.system.repository.BorrowRecordRepository;
import com.library.system.repository.UserRepository;
import com.library.system.security.LoginUser;
import com.library.system.security.SecurityUtils;
import com.library.system.service.BorrowService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.List;
import java.util.UUID;

@Service
public class BorrowServiceImpl implements BorrowService {

    private final BorrowRecordRepository borrowRecordRepository;
    private final BookRepository bookRepository;
    private final UserRepository userRepository;

    @Value("${recommendation.overdue-days:14}")
    private long overdueDays;

    public BorrowServiceImpl(BorrowRecordRepository borrowRecordRepository,
                             BookRepository bookRepository,
                             UserRepository userRepository) {
        this.borrowRecordRepository = borrowRecordRepository;
        this.bookRepository = bookRepository;
        this.userRepository = userRepository;
    }

    @Override
    @Transactional
    public BorrowRecordResponse apply(BorrowApplyRequest request) {
        LoginUser loginUser = SecurityUtils.currentUser();
        User user = userRepository.findById(loginUser.getId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        Book book = bookRepository.findById(request.getBookId())
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "图书不存在"));

        BorrowRecord record = new BorrowRecord();
        record.setUser(user);
        record.setBook(book);
        record.setBorrowType(request.getBorrowType());
        record.setRenewCount(0);

        if (request.getBorrowType() == BorrowType.OFFLINE) {
            // 线下：现场完成借阅，直接借出
            if (book.getStock() <= 0) {
                throw new BusinessException("库存不足");
            }
            book.setStock(book.getStock() - 1);
            bookRepository.save(book);

            LocalDateTime now = LocalDateTime.now();
            record.setStatus(BorrowStatus.BORROWED);
            record.setBorrowTime(now);
            record.setDueTime(now.plusDays(overdueDays));
            record.setReviewComment("线下借阅已登记");
            borrowRecordRepository.save(record);
            return DtoMapper.toBorrowResponse(record);
        }

        // 线上：提交申请，等待管理员审核
        record.setStatus(BorrowStatus.APPLIED);
        record.setReviewComment("等待管理员审核");
        borrowRecordRepository.save(record);
        return DtoMapper.toBorrowResponse(record);
    }

    @Override
    @Transactional
    public BorrowRecordResponse approve(Long recordId, BorrowReviewRequest request) {
        BorrowRecord record = getRecord(recordId);
        if (record.getStatus() != BorrowStatus.APPLIED || record.getBorrowType() != BorrowType.ONLINE) {
            throw new BusinessException("仅线上申请记录可审核通过");
        }

        Book book = record.getBook();
        if (book.getStock() <= 0) {
            throw new BusinessException("库存不足，无法通过");
        }

        // 审核通过即预占库存，等待用户确认取书
        book.setStock(book.getStock() - 1);
        bookRepository.save(book);

        record.setStatus(BorrowStatus.APPROVED);
        record.setApprovedTime(LocalDateTime.now());
        record.setPickupCode(generatePickupCode());
        record.setReviewComment(buildApproveComment(request));
        borrowRecordRepository.save(record);
        return DtoMapper.toBorrowResponse(record);
    }

    @Override
    @Transactional
    public BorrowRecordResponse reject(Long recordId, BorrowReviewRequest request) {
        BorrowRecord record = getRecord(recordId);
        if (record.getBorrowType() != BorrowType.ONLINE) {
            throw new BusinessException("仅线上申请记录可驳回");
        }
        if (record.getStatus() == BorrowStatus.APPLIED) {
            record.setStatus(BorrowStatus.REJECTED);
            record.setReviewComment(request != null ? request.getComment() : null);
            borrowRecordRepository.save(record);
            return DtoMapper.toBorrowResponse(record);
        }
        if (record.getStatus() == BorrowStatus.APPROVED) {
            // 撤销已通过申请时回补预占库存
            Book book = record.getBook();
            book.setStock(book.getStock() + 1);
            bookRepository.save(book);

            record.setStatus(BorrowStatus.REJECTED);
            String comment = request != null && request.getComment() != null && !request.getComment().isBlank()
                    ? request.getComment().trim()
                    : "管理员撤销通过";
            record.setReviewComment(comment);
            borrowRecordRepository.save(record);
            return DtoMapper.toBorrowResponse(record);
        }
        throw new BusinessException("当前状态不可驳回");
    }

    @Override
    @Transactional
    public BorrowRecordResponse cancelApply(Long recordId) {
        BorrowRecord record = getRecord(recordId);
        LoginUser loginUser = SecurityUtils.currentUser();
        if (!record.getUser().getId().equals(loginUser.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能撤销自己的申请");
        }

        if (record.getStatus() == BorrowStatus.APPLIED) {
            record.setStatus(BorrowStatus.CANCELED);
            record.setReviewComment("用户撤销申请");
            borrowRecordRepository.save(record);
            return DtoMapper.toBorrowResponse(record);
        }

        if (record.getStatus() == BorrowStatus.APPROVED) {
            // 已预占库存的审批单可撤销并回补库存
            Book book = record.getBook();
            book.setStock(book.getStock() + 1);
            bookRepository.save(book);

            record.setStatus(BorrowStatus.CANCELED);
            record.setReviewComment("用户撤销已通过申请");
            borrowRecordRepository.save(record);
            return DtoMapper.toBorrowResponse(record);
        }

        throw new BusinessException("当前状态不可撤销");
    }

    @Override
    @Transactional
    public BorrowRecordResponse confirmPickup(Long recordId) {
        BorrowRecord record = getRecord(recordId);
        LoginUser loginUser = SecurityUtils.currentUser();
        if (!record.getUser().getId().equals(loginUser.getId())) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "只能确认自己的借阅");
        }
        if (record.getStatus() != BorrowStatus.APPROVED || record.getBorrowType() != BorrowType.ONLINE) {
            throw new BusinessException("当前记录不可确认取书");
        }

        LocalDateTime now = LocalDateTime.now();
        int loanDays = extractLoanDays(record.getReviewComment());
        record.setStatus(BorrowStatus.BORROWED);
        record.setBorrowTime(now);
        record.setDueTime(now.plusDays(loanDays));
        record.setReviewComment("用户已确认取书");
        borrowRecordRepository.save(record);
        return DtoMapper.toBorrowResponse(record);
    }

    @Override
    @Transactional
    public BorrowRecordResponse returnBook(Long recordId) {
        BorrowRecord record = getRecord(recordId);
        ensureOwnerOrAdmin(record);
        if (record.getStatus() != BorrowStatus.BORROWED && record.getStatus() != BorrowStatus.OVERDUE) {
            throw new BusinessException("当前状态不可归还");
        }

        record.setStatus(BorrowStatus.RETURNED);
        record.setReturnTime(LocalDateTime.now());

        Book book = record.getBook();
        book.setStock(book.getStock() + 1);
        bookRepository.save(book);

        borrowRecordRepository.save(record);
        return DtoMapper.toBorrowResponse(record);
    }

    @Override
    @Transactional
    public BorrowRecordResponse renew(Long recordId) {
        BorrowRecord record = getRecord(recordId);
        ensureOwnerOrAdmin(record);
        if (record.getStatus() != BorrowStatus.BORROWED && record.getStatus() != BorrowStatus.OVERDUE) {
            throw new BusinessException("当前状态不可续借");
        }
        if (record.getRenewCount() >= 2) {
            throw new BusinessException("最多续借2次");
        }
        record.setRenewCount(record.getRenewCount() + 1);
        record.setDueTime(record.getDueTime().plusDays(7));
        record.setStatus(BorrowStatus.BORROWED);
        record.setReviewComment("续借 +7 天");
        borrowRecordRepository.save(record);
        return DtoMapper.toBorrowResponse(record);
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowRecordResponse> myRecords() {
        LoginUser loginUser = SecurityUtils.currentUser();
        return borrowRecordRepository.findByUser_IdOrderByIdDesc(loginUser.getId())
                .stream().map(DtoMapper::toBorrowResponse).toList();
    }

    @Override
    @Transactional(readOnly = true)
    public List<BorrowRecordResponse> allRecords() {
        return borrowRecordRepository.findAll().stream()
                .map(DtoMapper::toBorrowResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<BorrowRecordResponse> overdueRecords() {
        markOverdueRecords();
        return borrowRecordRepository.findByStatusIn(List.of(BorrowStatus.OVERDUE)).stream()
                .map(DtoMapper::toBorrowResponse)
                .toList();
    }

    @Scheduled(cron = "0 0/30 * * * ?")
    public void markOverdueRecords() {
        List<BorrowRecord> overdue = borrowRecordRepository.findByStatusAndDueTimeBefore(
                BorrowStatus.BORROWED, LocalDateTime.now());
        for (BorrowRecord record : overdue) {
            record.setStatus(BorrowStatus.OVERDUE);
            record.setReviewComment("系统标记逾期");
        }
        borrowRecordRepository.saveAll(overdue);
    }

    private BorrowRecord getRecord(Long recordId) {
        return borrowRecordRepository.findById(recordId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "借阅记录不存在"));
    }

    private String generatePickupCode() {
        String raw = UUID.randomUUID().toString().replace("-", "").toUpperCase();
        return raw.substring(0, 8);
    }

    private String buildApproveComment(BorrowReviewRequest request) {
        int loanDays = overdueDaysAsInt();
        if (request != null && request.getLoanDays() != null && request.getLoanDays() >= 1 && request.getLoanDays() <= 60) {
            loanDays = request.getLoanDays();
        }
        String comment = request == null ? "" : (request.getComment() == null ? "" : request.getComment().trim());
        String prefix = "loanDays=" + loanDays;
        if (comment.isBlank()) {
            return prefix;
        }
        return prefix + "; " + comment;
    }

    private int overdueDaysAsInt() {
        return (int) Math.max(1, Math.min(overdueDays, 60));
    }

    private int extractLoanDays(String comment) {
        if (comment == null || comment.isBlank()) {
            return overdueDaysAsInt();
        }
        String[] items = comment.split(";");
        for (String item : items) {
            String trimmed = item.trim();
            if (trimmed.startsWith("loanDays=")) {
                try {
                    int days = Integer.parseInt(trimmed.substring("loanDays=".length()).trim());
                    if (days >= 1 && days <= 60) {
                        return days;
                    }
                } catch (NumberFormatException ignored) {
                }
            }
        }
        return overdueDaysAsInt();
    }

    private void ensureOwnerOrAdmin(BorrowRecord record) {
        LoginUser loginUser = SecurityUtils.currentUser();
        boolean owner = record.getUser().getId().equals(loginUser.getId());
        boolean admin = loginUser.getRole() == Role.ADMIN;
        if (!owner && !admin) {
            throw new BusinessException(HttpStatus.FORBIDDEN, "无权限操作该借阅记录");
        }
    }
}
