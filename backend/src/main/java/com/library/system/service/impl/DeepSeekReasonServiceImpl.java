package com.library.system.service.impl;

import com.library.system.entity.Book;
import com.library.system.entity.User;
import com.library.system.service.DeepSeekReasonService;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.stereotype.Service;
import org.springframework.web.client.RestClient;

import java.util.List;
import java.util.Map;

@Service
public class DeepSeekReasonServiceImpl implements DeepSeekReasonService {

    private final RestClient restClient;

    @Value("${deepseek.api-url}")
    private String apiUrl;

    @Value("${deepseek.api-key:}")
    private String apiKey;

    @Value("${deepseek.model:deepseek-chat}")
    private String model;

    public DeepSeekReasonServiceImpl(RestClient restClient) {
        this.restClient = restClient;
    }

    @Override
    public String buildReason(User user, Book book, double cfScore) {
        if (apiKey == null || apiKey.isBlank()) {
            return fallbackReason(book, cfScore);
        }

        String prompt = "你是图书推荐助手。请用一句中文解释为什么推荐《" + book.getTitle() + "》，"
                + "要求包含用户历史偏好与协同过滤得分，简洁且不超过50字。协同过滤得分: " + cfScore;

        Map<String, Object> body = Map.of(
                "model", model,
                "temperature", 0.3,
                "messages", List.of(
                        Map.of("role", "system", "content", "你负责生成图书推荐理由"),
                        Map.of("role", "user", "content", prompt)
                )
        );

        try {
            Map response = restClient.post()
                    .uri(apiUrl)
                    .header(HttpHeaders.AUTHORIZATION, "Bearer " + apiKey)
                    .contentType(MediaType.APPLICATION_JSON)
                    .body(body)
                    .retrieve()
                    .body(Map.class);

            if (response != null && response.get("choices") instanceof List choices && !choices.isEmpty()) {
                Object first = choices.getFirst();
                if (first instanceof Map choice && choice.get("message") instanceof Map message) {
                    Object content = message.get("content");
                    if (content instanceof String text && !text.isBlank()) {
                        return text.strip();
                    }
                }
            }
        } catch (Exception ignored) {
            // API 异常时回退到本地理由
        }

        return fallbackReason(book, cfScore);
    }

    private String fallbackReason(Book book, double cfScore) {
        return "你近期偏好同类主题，且协同过滤匹配度 " + String.format("%.2f", cfScore)
                + "，推荐《" + book.getTitle() + "》";
    }
}
