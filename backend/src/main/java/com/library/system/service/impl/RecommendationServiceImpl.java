package com.library.system.service.impl;

import com.library.system.dto.recommendation.RecommendationBookStat;
import com.library.system.dto.recommendation.RecommendationResponse;
import com.library.system.dto.recommendation.RecommendationStatsResponse;
import com.library.system.entity.Book;
import com.library.system.entity.BorrowRecord;
import com.library.system.entity.Recommendation;
import com.library.system.entity.User;
import com.library.system.entity.enums.BorrowStatus;
import com.library.system.exception.BusinessException;
import com.library.system.repository.BookRepository;
import com.library.system.repository.BorrowRecordRepository;
import com.library.system.repository.RecommendationRepository;
import com.library.system.repository.UserRepository;
import com.library.system.service.DeepLearningScoreService;
import com.library.system.service.DeepSeekReasonService;
import com.library.system.service.RecommendationService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpStatus;
import org.springframework.scheduling.annotation.Scheduled;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.time.LocalDateTime;
import java.util.*;
import java.util.stream.Collectors;

@Service
public class RecommendationServiceImpl implements RecommendationService {

    private final RecommendationRepository recommendationRepository;
    private final BorrowRecordRepository borrowRecordRepository;
    private final UserRepository userRepository;
    private final BookRepository bookRepository;
    private final DeepSeekReasonService deepSeekReasonService;
    private final DeepLearningScoreService deepLearningScoreService;

    @Value("${recommendation.default-top-n:10}")
    private int defaultTopN;

    @Value("${recommendation.max-top-n:50}")
    private int maxTopN;

    public RecommendationServiceImpl(RecommendationRepository recommendationRepository,
                                     BorrowRecordRepository borrowRecordRepository,
                                     UserRepository userRepository,
                                     BookRepository bookRepository,
                                     DeepSeekReasonService deepSeekReasonService,
                                     DeepLearningScoreService deepLearningScoreService) {
        this.recommendationRepository = recommendationRepository;
        this.borrowRecordRepository = borrowRecordRepository;
        this.userRepository = userRepository;
        this.bookRepository = bookRepository;
        this.deepSeekReasonService = deepSeekReasonService;
        this.deepLearningScoreService = deepLearningScoreService;
    }

    @Override
    @Transactional(readOnly = true)
    public List<RecommendationResponse> myRecommendations(int topN) {
        Long userId = com.library.system.security.SecurityUtils.currentUser().getId();
        int limit = normalizeTopN(topN);
        return recommendationRepository.findByUser_IdOrderByGeneratedTimeDesc(userId)
                .stream()
                .limit(limit)
                .map(DtoMapper::toRecommendationResponse)
                .toList();
    }

    @Override
    @Transactional
    public List<RecommendationResponse> refreshForUser(Long userId, int topN) {
        User targetUser = userRepository.findById(userId)
                .orElseThrow(() -> new BusinessException(HttpStatus.NOT_FOUND, "用户不存在"));
        int limit = normalizeTopN(topN);

        List<BorrowRecord> validRecords = borrowRecordRepository.findByStatusIn(
                List.of(BorrowStatus.BORROWED, BorrowStatus.RETURNED, BorrowStatus.OVERDUE));
        List<Book> allBooks = bookRepository.findAll();
        if (allBooks.isEmpty()) {
            recommendationRepository.deleteByUser_Id(userId);
            return List.of();
        }

        Map<Long, Set<Long>> userBooks = buildUserBookMap(validRecords);
        Set<Long> targetBooks = userBooks.getOrDefault(userId, Set.of());
        Map<Long, Long> borrowCount = buildBorrowCount(validRecords);

        Map<Long, Double> baseScores = buildCandidateBaseScores(targetUser, targetBooks, userBooks, allBooks, borrowCount);
        List<Book> candidates = allBooks.stream()
                .filter(book -> baseScores.containsKey(book.getId()))
                .toList();

        Map<Long, Double> modelScores = deepLearningScoreService.predictScores(targetUser, candidates, targetBooks, baseScores);
        List<Map.Entry<Book, ScoreBundle>> ranked = rankCandidates(targetUser, candidates, baseScores, modelScores, borrowCount);

        recommendationRepository.deleteByUser_Id(userId);

        List<Recommendation> saved = new ArrayList<>();
        for (Map.Entry<Book, ScoreBundle> entry : ranked.stream().limit(limit).toList()) {
            Book book = entry.getKey();
            ScoreBundle bundle = entry.getValue();

            Recommendation recommendation = new Recommendation();
            recommendation.setUser(targetUser);
            recommendation.setBook(book);
            recommendation.setModelScore(bundle.modelScore());
            recommendation.setFinalScore(bundle.finalScore());
            recommendation.setGeneratedTime(LocalDateTime.now());
            recommendation.setReason(deepSeekReasonService.buildReason(targetUser, book, bundle.finalScore()));
            saved.add(recommendation);
        }
        recommendationRepository.saveAll(saved);

        return recommendationRepository.findByUser_IdOrderByGeneratedTimeDesc(userId)
                .stream()
                .limit(limit)
                .map(DtoMapper::toRecommendationResponse)
                .toList();
    }

    @Override
    @Scheduled(cron = "0 0 2 * * ?")
    public void refreshAllBySchedule() {
        List<Long> userIds = userRepository.findAll().stream().map(User::getId).toList();
        for (Long userId : userIds) {
            refreshForUser(userId, defaultTopN);
        }
    }

    @Override
    @Transactional(readOnly = true)
    public RecommendationStatsResponse recommendationStats() {
        List<Recommendation> recommendations = recommendationRepository.findAll();
        RecommendationStatsResponse response = new RecommendationStatsResponse();
        if (recommendations.isEmpty()) {
            response.setTotalRecommendations(0L);
            response.setHitRecommendations(0L);
            response.setOverallHitRate(0D);
            response.setCoveredUsers(0L);
            response.setTopBooks(List.of());
            return response;
        }

        List<BorrowRecord> borrowRecords = borrowRecordRepository.findByStatusIn(
                List.of(BorrowStatus.BORROWED, BorrowStatus.RETURNED, BorrowStatus.OVERDUE));
        Map<String, List<LocalDateTime>> userBookBorrowTimes = buildUserBookBorrowTimes(borrowRecords);

        long total = recommendations.size();
        long hits = 0L;
        Set<Long> coveredUsers = new HashSet<>();
        Map<Long, Long> recommendedCount = new HashMap<>();
        Map<Long, Long> hitCount = new HashMap<>();
        Map<Long, String> bookTitle = new HashMap<>();

        for (Recommendation recommendation : recommendations) {
            Long userId = recommendation.getUser().getId();
            Long bookId = recommendation.getBook().getId();
            coveredUsers.add(userId);
            recommendedCount.merge(bookId, 1L, Long::sum);
            bookTitle.put(bookId, recommendation.getBook().getTitle());

            boolean isHit = isRecommendationHit(userId, bookId, recommendation.getGeneratedTime(), userBookBorrowTimes);
            if (isHit) {
                hits++;
                hitCount.merge(bookId, 1L, Long::sum);
            }
        }

        List<RecommendationBookStat> topBooks = recommendedCount.entrySet().stream()
                .sorted((a, b) -> Long.compare(b.getValue(), a.getValue()))
                .limit(10)
                .map(entry -> {
                    Long bookId = entry.getKey();
                    long recCount = entry.getValue();
                    long hCount = hitCount.getOrDefault(bookId, 0L);
                    RecommendationBookStat stat = new RecommendationBookStat();
                    stat.setBookId(bookId);
                    stat.setTitle(bookTitle.get(bookId));
                    stat.setRecommendedCount(recCount);
                    stat.setHitCount(hCount);
                    stat.setHitRate(recCount == 0 ? 0D : hCount * 1.0 / recCount);
                    return stat;
                })
                .toList();

        response.setTotalRecommendations(total);
        response.setHitRecommendations(hits);
        response.setOverallHitRate(total == 0 ? 0D : hits * 1.0 / total);
        response.setCoveredUsers((long) coveredUsers.size());
        response.setTopBooks(topBooks);
        return response;
    }

    private int normalizeTopN(int topN) {
        if (topN <= 0) {
            return defaultTopN;
        }
        return Math.min(topN, maxTopN);
    }

    private Map<Long, Set<Long>> buildUserBookMap(List<BorrowRecord> records) {
        Map<Long, Set<Long>> userBooks = new HashMap<>();
        for (BorrowRecord record : records) {
            userBooks.computeIfAbsent(record.getUser().getId(), k -> new HashSet<>())
                    .add(record.getBook().getId());
        }
        return userBooks;
    }

    private Map<Long, Long> buildBorrowCount(List<BorrowRecord> records) {
        return records.stream().collect(Collectors.groupingBy(r -> r.getBook().getId(), Collectors.counting()));
    }

    private Map<Long, Double> buildCandidateBaseScores(User targetUser,
                                                        Set<Long> targetBooks,
                                                        Map<Long, Set<Long>> userBooks,
                                                        List<Book> allBooks,
                                                        Map<Long, Long> borrowCount) {
        Map<Long, Double> collaborativeScores = calculateCollaborativeScores(targetUser.getId(), userBooks, targetBooks);
        Map<Long, Double> result = new HashMap<>(collaborativeScores);

        if (!targetBooks.isEmpty()) {
            Map<String, Long> prefCategory = allBooks.stream()
                    .filter(book -> targetBooks.contains(book.getId()))
                    .collect(Collectors.groupingBy(Book::getCategoryCode, Collectors.counting()));

            for (Book book : allBooks) {
                if (targetBooks.contains(book.getId())) {
                    continue;
                }
                long catWeight = prefCategory.getOrDefault(book.getCategoryCode(), 0L);
                if (catWeight > 0) {
                    result.merge(book.getId(), catWeight * 0.2, Double::sum);
                }
            }
        } else {
            // 冷启动用户：热门图书 + 兴趣标签匹配
            for (Book book : allBooks) {
                double score = borrowCount.getOrDefault(book.getId(), 0L);
                if (score > 0) {
                    result.put(book.getId(), score);
                }
            }
            Set<String> tags = parseTags(targetUser.getInterestTags());
            if (!tags.isEmpty()) {
                for (Book book : allBooks) {
                    if (matchesTags(book, tags)) {
                        result.merge(book.getId(), 5.0, Double::sum);
                    }
                }
            }
        }

        // 新书冷启动：借阅次数低的新书给基础加权
        for (Book book : allBooks) {
            long count = borrowCount.getOrDefault(book.getId(), 0L);
            if (count <= 1 && !targetBooks.contains(book.getId())) {
                result.merge(book.getId(), 0.8, Double::sum);
            }
        }

        if (result.isEmpty()) {
            return allBooks.stream().collect(Collectors.toMap(Book::getId, b -> 1.0));
        }
        return result;
    }

    private Map<Long, Double> calculateCollaborativeScores(Long targetUserId,
                                                            Map<Long, Set<Long>> userBooks,
                                                            Set<Long> targetBooks) {
        Map<Long, Double> scoreMap = new HashMap<>();
        for (Map.Entry<Long, Set<Long>> entry : userBooks.entrySet()) {
            Long otherUserId = entry.getKey();
            if (Objects.equals(otherUserId, targetUserId)) {
                continue;
            }
            Set<Long> otherBooks = entry.getValue();
            double similarity = jaccard(targetBooks, otherBooks);
            if (similarity <= 0) {
                continue;
            }
            for (Long bookId : otherBooks) {
                if (targetBooks.contains(bookId)) {
                    continue;
                }
                scoreMap.merge(bookId, similarity, Double::sum);
            }
        }
        return scoreMap;
    }

    private List<Map.Entry<Book, ScoreBundle>> rankCandidates(User targetUser,
                                                               List<Book> candidates,
                                                               Map<Long, Double> baseScores,
                                                               Map<Long, Double> modelScores,
                                                               Map<Long, Long> borrowCount) {
        Set<String> tags = parseTags(targetUser.getInterestTags());
        List<Map.Entry<Book, ScoreBundle>> ranked = new ArrayList<>();
        for (Book book : candidates) {
            double base = baseScores.getOrDefault(book.getId(), 0D);
            double model = modelScores.getOrDefault(book.getId(), base);
            double coldBoost = 0D;
            if (borrowCount.getOrDefault(book.getId(), 0L) <= 1) {
                coldBoost += 0.4;
            }
            if (!tags.isEmpty() && matchesTags(book, tags)) {
                coldBoost += 0.6;
            }
            double finalScore = base * 0.45 + model * 0.45 + coldBoost;
            ranked.add(Map.entry(book, new ScoreBundle(model, finalScore)));
        }

        ranked.sort((a, b) -> Double.compare(b.getValue().finalScore(), a.getValue().finalScore()));
        return ranked;
    }

    private Map<String, List<LocalDateTime>> buildUserBookBorrowTimes(List<BorrowRecord> records) {
        Map<String, List<LocalDateTime>> map = new HashMap<>();
        for (BorrowRecord record : records) {
            if (record.getBorrowTime() == null) {
                continue;
            }
            String key = recommendationKey(record.getUser().getId(), record.getBook().getId());
            map.computeIfAbsent(key, k -> new ArrayList<>()).add(record.getBorrowTime());
        }
        return map;
    }

    private boolean isRecommendationHit(Long userId,
                                        Long bookId,
                                        LocalDateTime generatedTime,
                                        Map<String, List<LocalDateTime>> userBookBorrowTimes) {
        List<LocalDateTime> borrowTimes = userBookBorrowTimes.get(recommendationKey(userId, bookId));
        if (borrowTimes == null || borrowTimes.isEmpty()) {
            return false;
        }
        for (LocalDateTime time : borrowTimes) {
            if (!time.isBefore(generatedTime)) {
                return true;
            }
        }
        return false;
    }

    private String recommendationKey(Long userId, Long bookId) {
        return userId + "-" + bookId;
    }

    private Set<String> parseTags(String raw) {
        if (raw == null || raw.isBlank()) {
            return Set.of();
        }
        return Arrays.stream(raw.split("[,;\\s]+"))
                .map(String::trim)
                .filter(s -> !s.isBlank())
                .map(String::toLowerCase)
                .collect(Collectors.toSet());
    }

    private boolean matchesTags(Book book, Set<String> tags) {
        String text = (book.getTitle() + " " + book.getAuthor() + " " + book.getCategoryCode()).toLowerCase();
        for (String tag : tags) {
            if (text.contains(tag)) {
                return true;
            }
        }
        return false;
    }

    private double jaccard(Set<Long> a, Set<Long> b) {
        if (a.isEmpty() || b.isEmpty()) {
            return 0;
        }
        Set<Long> intersection = new HashSet<>(a);
        intersection.retainAll(b);
        Set<Long> union = new HashSet<>(a);
        union.addAll(b);
        return union.isEmpty() ? 0 : (double) intersection.size() / union.size();
    }

    private record ScoreBundle(Double modelScore, Double finalScore) {
    }
}
