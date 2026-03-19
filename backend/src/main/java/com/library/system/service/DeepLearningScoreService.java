package com.library.system.service;

import com.library.system.entity.Book;
import com.library.system.entity.User;

import java.util.List;
import java.util.Map;
import java.util.Set;

public interface DeepLearningScoreService {
    Map<Long, Double> predictScores(User user,
                                    List<Book> candidates,
                                    Set<Long> historyBookIds,
                                    Map<Long, Double> baseScores);
}
