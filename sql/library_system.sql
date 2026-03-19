-- 智能图书管理系统数据库脚本
CREATE DATABASE IF NOT EXISTS library_system DEFAULT CHARACTER SET utf8mb4 COLLATE utf8mb4_unicode_ci;
USE library_system;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    username VARCHAR(50) NOT NULL UNIQUE,
    password VARCHAR(255) NOT NULL,
    email VARCHAR(120) NOT NULL UNIQUE,
    interest_tags VARCHAR(255),
    register_time DATETIME NOT NULL,
    role VARCHAR(20) NOT NULL
);

CREATE TABLE IF NOT EXISTS category (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    name VARCHAR(80) NOT NULL UNIQUE
);

CREATE TABLE IF NOT EXISTS book (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(120) NOT NULL,
    category_code VARCHAR(80) NOT NULL,
    stock INT NOT NULL,
    location VARCHAR(120) NOT NULL,
    image_url VARCHAR(500),
    created_time DATETIME NOT NULL,
    category_id BIGINT,
    CONSTRAINT fk_book_category FOREIGN KEY (category_id) REFERENCES category(id)
);

CREATE TABLE IF NOT EXISTS borrow_record (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    borrow_time DATETIME,
    return_time DATETIME,
    due_time DATETIME,
    approved_time DATETIME,
    status VARCHAR(30) NOT NULL,
    borrow_type VARCHAR(20) NOT NULL,
    renew_count INT NOT NULL DEFAULT 0,
    pickup_code VARCHAR(40),
    review_comment VARCHAR(500),
    CONSTRAINT fk_borrow_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_borrow_book FOREIGN KEY (book_id) REFERENCES book(id)
);

CREATE INDEX idx_borrow_user_status ON borrow_record(user_id, status);
CREATE INDEX idx_book_search ON book(title, author, category_code);

CREATE TABLE IF NOT EXISTS recommendation (
    id BIGINT PRIMARY KEY AUTO_INCREMENT,
    user_id BIGINT NOT NULL,
    book_id BIGINT NOT NULL,
    reason VARCHAR(500) NOT NULL,
    model_score DOUBLE,
    final_score DOUBLE,
    generated_time DATETIME NOT NULL,
    CONSTRAINT fk_rec_user FOREIGN KEY (user_id) REFERENCES users(id),
    CONSTRAINT fk_rec_book FOREIGN KEY (book_id) REFERENCES book(id)
);

CREATE INDEX idx_rec_user_time ON recommendation(user_id, generated_time);

-- 初始化管理员账号（密码是 BCrypt 加密后的 123456）
INSERT INTO users(username, password, email, register_time, role)
SELECT 'admin', '$2a$10$eM8fdtvV17Qf2R6N8Yf7kON9jA6P2h3IZy9lEi5Vf3L8mQf5V7vqu', 'admin@library.com', NOW(), 'ADMIN'
WHERE NOT EXISTS (SELECT 1 FROM users WHERE username='admin');
