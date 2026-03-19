package com.library.system.service;

import com.library.system.entity.Book;
import com.library.system.entity.User;

public interface DeepSeekReasonService {
    String buildReason(User user, Book book, double cfScore);
}
