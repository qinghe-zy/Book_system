package com.library.system.service.impl;

import com.library.system.entity.Book;
import com.library.system.entity.User;
import com.library.system.service.DeepLearningScoreService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.*;

@Service
public class DeepLearningScoreServiceImpl implements DeepLearningScoreService {

    private final RestClient restClient;

    @Value("${recommendation.remote-score-url:}")
    private String remoteScoreUrl;

    @Value("${recommendation.remote-score-api-key:}")
    private String remoteScoreApiKey;

    @Value("${recommendation.remote-timeout-ms:6000}")
    private int timeoutMs;

    public DeepLearningScoreServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public Map<Long, Double> predictScores(User user,
                                           List<Book> candidates,
                                           Set<Long> historyBookIds,
                                           Map<Long, Double> baseScores) {
        Map<Long, Double> fallback = new HashMap<>(baseScores);
        if (candidates.isEmpty()) {
            return fallback;
        }
        if (remoteScoreUrl == null || remoteScoreUrl.isBlank()) {
            return fallback;
        }

        List<Map<String, Object>> candidatePayload = new ArrayList<>();
        for (Book book : candidates) {
            Map<String, Object> item = new HashMap<>();
            item.put("bookId", book.getId());
            item.put("title", book.getTitle());
            item.put("author", book.getAuthor());
            item.put("categoryCode", book.getCategoryCode());
            item.put("imageUrl", Optional.ofNullable(book.getImageUrl()).orElse(""));
            candidatePayload.add(item);
        }

        Map<String, Object> body = new HashMap<>();
        body.put("userId", user.getId());
        body.put("interestTags", Optional.ofNullable(user.getInterestTags()).orElse(""));
        body.put("historyBookIds", historyBookIds);
        body.put("candidates", candidatePayload);
        body.put("timeoutMs", timeoutMs);

        try {
            RestClient.RequestBodySpec request = restClient.post()
                    .uri(remoteScoreUrl)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body);

            if (remoteScoreApiKey != null && !remoteScoreApiKey.isBlank()) {
                request.header(HttpHeaders.AUTHORIZATION, "Bearer " + remoteScoreApiKey);
            }

            Map<String, Object> response = request.retrieve().body(Map.class);
            Map<Long, Double> parsed = parseScoreResponse(response, fallback);
            if (!parsed.isEmpty()) {
                return parsed;
            }
        } catch (Exception ignored) {
            // 远程调用异常时自动回退本地协同过滤分数
        }

        return fallback;
    }

    private Map<Long, Double> parseScoreResponse(Map<String, Object> response, Map<Long, Double> fallback) {
        Map<Long, Double> result = new HashMap<>(fallback);
        if (response == null) {
            return result;
        }

        Object scoresObj = response.get("scores");
        if (scoresObj instanceof List<?> scoreList) {
            for (Object item : scoreList) {
                if (!(item instanceof Map<?, ?> scoreMap)) {
                    continue;
                }
                Long bookId = toLong(scoreMap.get("bookId"));
                Double score = toDouble(scoreMap.get("score"));
                if (bookId != null && score != null) {
                    result.put(bookId, score);
                }
            }
            return result;
        }

        Object dataObj = response.get("data");
        if (dataObj instanceof Map<?, ?> dataMap) {
            for (Map.Entry<?, ?> entry : dataMap.entrySet()) {
                Long bookId = toLong(entry.getKey());
                Double score = toDouble(entry.getValue());
                if (bookId != null && score != null) {
                    result.put(bookId, score);
                }
            }
        }

        return result;
    }

    private Long toLong(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.longValue();
        }
        try {
            return Long.parseLong(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }

    private Double toDouble(Object value) {
        if (value == null) {
            return null;
        }
        if (value instanceof Number number) {
            return number.doubleValue();
        }
        try {
            return Double.parseDouble(String.valueOf(value));
        } catch (NumberFormatException ex) {
            return null;
        }
    }
}
