package com.library.system.service.impl;

import com.library.system.dto.book.BookResponse;
import com.library.system.dto.borrow.BorrowRecordResponse;
import com.library.system.dto.dashboard.DashboardNoticeSummary;
import com.library.system.dto.dashboard.DashboardOverviewResponse;
import com.library.system.dto.dashboard.DashboardStatCard;
import com.library.system.dto.recommendation.RecommendationResponse;
import com.library.system.dto.recommendation.RecommendationStatsResponse;
import com.library.system.entity.Book;
import com.library.system.entity.BorrowRecord;
import com.library.system.entity.Recommendation;
import com.library.system.entity.enums.BorrowStatus;
import com.library.system.entity.enums.Role;
import com.library.system.repository.BookRepository;
import com.library.system.repository.BorrowRecordRepository;
import com.library.system.repository.RecommendationRepository;
import com.library.system.repository.UserRepository;
import com.library.system.security.LoginUser;
import com.library.system.security.SecurityUtils;
import com.library.system.service.DashboardService;
import com.library.system.service.RecommendationService;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.Comparator;
import java.util.List;

/**
 * Dashboard 聚合服务实现。
 * 这里复用现有仓储与推荐统计能力，为首页提供稳定的单接口输出。
 */
@Service
public class DashboardServiceImpl implements DashboardService {

    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final RecommendationRepository recommendationRepository;
    private final RecommendationService recommendationService;

    public DashboardServiceImpl(UserRepository userRepository,
                                BookRepository bookRepository,
                                BorrowRecordRepository borrowRecordRepository,
                                RecommendationRepository recommendationRepository,
                                RecommendationService recommendationService) {
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.recommendationRepository = recommendationRepository;
        this.recommendationService = recommendationService;
    }

    @Override
    @Transactional(readOnly = true)
    public DashboardOverviewResponse currentOverview() {
        LoginUser currentUser = SecurityUtils.currentUser();
        return currentUser.getRole() == Role.ADMIN ? buildAdminOverview() : buildUserOverview(currentUser.getId());
    }

    private DashboardOverviewResponse buildUserOverview(Long userId) {
        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setAdmin(false);

        List<BorrowRecord> myRecords = borrowRecordRepository.findByUser_IdOrderByIdDesc(userId);
        List<Book> allBooks = bookRepository.findAll();
        List<RecommendationResponse> myRecommendations = recommendationRepository.findByUser_IdOrderByGeneratedTimeDesc(userId)
                .stream()
                .limit(4)
                .map(DtoMapper::toRecommendationResponse)
                .toList();

        long activeCount = myRecords.stream().filter(record -> List.of(BorrowStatus.APPLIED, BorrowStatus.APPROVED, BorrowStatus.BORROWED, BorrowStatus.OVERDUE).contains(record.getStatus())).count();
        long dueSoonCount = myRecords.stream().filter(record -> record.getStatus() == BorrowStatus.BORROWED && record.getDueTime() != null && !record.getDueTime().isBefore(LocalDateTime.now()) && record.getDueTime().isBefore(LocalDateTime.now().plusDays(3))).count();
        long availableCount = allBooks.stream().filter(book -> book.getStock() != null && book.getStock() > 0).count();

        response.setStatCards(List.of(
                new DashboardStatCard("当前借阅", String.valueOf(activeCount), "包含待审核、待取书与借阅中"),
                new DashboardStatCard("即将到期", String.valueOf(dueSoonCount), "建议优先查看续借与归还"),
                new DashboardStatCard("推荐图书", String.valueOf(myRecommendations.size()), "基于借阅与兴趣生成"),
                new DashboardStatCard("可借馆藏", String.valueOf(availableCount), "当前库存大于 0 的图书")
        ));

        DashboardNoticeSummary notice = new DashboardNoticeSummary();
        notice.setTitle("当前提醒");
        notice.setPrimaryCount(activeCount);
        notice.setSecondaryCount(dueSoonCount);
        notice.setContent("你当前有 " + activeCount + " 条借阅相关记录，其中 " + dueSoonCount + " 条即将到期。建议先查看借阅中心，再继续找书。");
        response.setNoticeSummary(notice);

        response.setRecentBorrowRecords(myRecords.stream().limit(4).map(DtoMapper::toBorrowResponse).toList());
        response.setHighlightBooks(allBooks.stream()
                .sorted(Comparator.comparing(Book::getCreatedTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(3)
                .map(DtoMapper::toBookResponse)
                .toList());
        response.setRecommendations(myRecommendations);
        return response;
    }

    private DashboardOverviewResponse buildAdminOverview() {
        DashboardOverviewResponse response = new DashboardOverviewResponse();
        response.setAdmin(true);

        List<Book> books = bookRepository.findAll();
        List<BorrowRecord> allRecords = borrowRecordRepository.findAll();
        RecommendationStatsResponse stats = recommendationService.recommendationStats();

        long pendingCount = allRecords.stream().filter(record -> record.getStatus() == BorrowStatus.APPLIED).count();
        long borrowingCount = allRecords.stream().filter(record -> record.getStatus() == BorrowStatus.BORROWED).count();
        long overdueCount = allRecords.stream().filter(record -> record.getStatus() == BorrowStatus.OVERDUE).count();
        long lowStockCount = books.stream().filter(book -> book.getStock() != null && book.getStock() <= 2).count();

        response.setStatCards(List.of(
                new DashboardStatCard("图书总量", String.valueOf(books.size()), "当前系统馆藏规模"),
                new DashboardStatCard("用户总量", String.valueOf(userRepository.count()), "已注册账号数量"),
                new DashboardStatCard("待审核", String.valueOf(pendingCount), "优先处理借阅申请"),
                new DashboardStatCard("逾期记录", String.valueOf(overdueCount), "建议尽快联系用户处理")
        ));

        DashboardNoticeSummary notice = new DashboardNoticeSummary();
        notice.setTitle("当前待办");
        notice.setPrimaryCount(pendingCount);
        notice.setSecondaryCount(lowStockCount);
        notice.setContent("待审核申请 " + pendingCount + " 条，库存紧张图书 " + lowStockCount + " 本，当前借阅中记录 " + borrowingCount + " 条。建议先处理借阅，再关注库存补位。");
        response.setNoticeSummary(notice);

        response.setRecentBooks(books.stream()
                .sorted(Comparator.comparing(Book::getCreatedTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(4)
                .map(DtoMapper::toBookResponse)
                .toList());
        response.setHighlightBooks(books.stream()
                .filter(book -> book.getStock() != null && book.getStock() <= 2)
                .sorted(Comparator.comparing(Book::getStock).thenComparing(Book::getCreatedTime, Comparator.nullsLast(Comparator.reverseOrder())))
                .limit(4)
                .map(DtoMapper::toBookResponse)
                .toList());
        response.setRecentBorrowRecords(allRecords.stream()
                .sorted(Comparator.comparing(BorrowRecord::getId, Comparator.reverseOrder()))
                .limit(4)
                .map(DtoMapper::toBorrowResponse)
                .toList());
        response.setRecommendationStats(stats);
        return response;
    }
}
