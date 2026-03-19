USE library_system;

INSERT INTO category(name) VALUES
('计算机'),('文学'),('历史'),('管理'),('艺术'),('科学'),('经济'),('心理')
ON DUPLICATE KEY UPDATE name = VALUES(name);

DROP TEMPORARY TABLE IF EXISTS tmp_seq;
CREATE TEMPORARY TABLE tmp_seq (
  n INT PRIMARY KEY
);
INSERT INTO tmp_seq(n) VALUES (1),(2),(3),(4),(5),(6),(7),(8),(9),(10),(11),(12),(13),(14),(15),(16),(17),(18),(19),(20),(21),(22),(23),(24),(25),(26),(27),(28),(29),(30),(31),(32),(33),(34),(35),(36),(37),(38),(39),(40),(41),(42),(43),(44),(45),(46),(47),(48),(49),(50),(51),(52),(53),(54),(55),(56),(57),(58),(59),(60),(61),(62),(63),(64),(65),(66),(67),(68),(69),(70),(71),(72),(73),(74),(75),(76),(77),(78),(79),(80),(81),(82),(83),(84),(85),(86),(87),(88),(89),(90),(91),(92),(93),(94),(95),(96),(97),(98),(99),(100);

-- 清理旧测试数据
DELETE br
FROM borrow_record br
JOIN users u ON br.user_id = u.id
WHERE u.username LIKE 'test_user_%';

DELETE r
FROM recommendation r
JOIN users u ON r.user_id = u.id
WHERE u.username LIKE 'test_user_%';

DELETE FROM users WHERE username LIKE 'test_user_%';
DELETE FROM book WHERE title LIKE '测试图书-%';

-- 插入 10 个测试用户
INSERT INTO users(username, password, email, interest_tags, register_time, role)
SELECT
  CONCAT('test_user_', LPAD(n, 3, '0')),
  '',
  CONCAT('test_user_', LPAD(n, 3, '0'), '@example.com'),
  ELT((n % 8) + 1, 'java,mysql', '文学,小说', '历史,文化', '管理,效率', '艺术,设计', '科学,物理', '经济,金融', '心理,成长'),
  NOW() - INTERVAL n DAY,
  'USER'
FROM tmp_seq
WHERE n <= 10;

-- 插入 100 本测试图书
INSERT INTO book(title, author, category_code, stock, location, image_url, created_time, category_id)
SELECT
  CONCAT('测试图书-', LPAD(n, 3, '0')),
  CONCAT('作者-', LPAD(((n - 1) % 25) + 1, 2, '0')),
  CONCAT('CAT-', LPAD(((n - 1) % 8) + 1, 2, '0')),
  5 + (n % 20),
  CONCAT('A-', LPAD(((n - 1) % 10) + 1, 2, '0'), '-', LPAD(((n - 1) % 6) + 1, 2, '0')),
  NULL,
  NOW() - INTERVAL n DAY,
  (
    SELECT c.id
    FROM category c
    WHERE c.name = ELT(((n - 1) % 8) + 1, '计算机', '文学', '历史', '管理', '艺术', '科学', '经济', '心理')
    LIMIT 1
  )
FROM tmp_seq;

-- 插入 100 条借阅记录
INSERT INTO borrow_record(user_id, book_id, borrow_time, return_time, due_time, status, borrow_type, renew_count)
SELECT
  u.id,
  b.id,
  NOW() - INTERVAL ((s.n % 20) + 1) DAY,
  CASE
    WHEN s.n % 5 = 0 THEN NULL
    WHEN s.n % 9 = 0 THEN NULL
    ELSE NOW() - INTERVAL ((s.n % 20) - 3) DAY
  END,
  NOW() + INTERVAL (14 - (s.n % 12)) DAY,
  CASE
    WHEN s.n % 9 = 0 THEN 'OVERDUE'
    WHEN s.n % 5 = 0 THEN 'BORROWED'
    ELSE 'RETURNED'
  END,
  CASE WHEN s.n % 2 = 0 THEN 'ONLINE' ELSE 'OFFLINE' END,
  CASE WHEN s.n % 7 = 0 THEN 1 ELSE 0 END
FROM tmp_seq s
JOIN users u ON u.username = CONCAT('test_user_', LPAD(((s.n - 1) % 10) + 1, 3, '0'))
JOIN book b ON b.title = CONCAT('测试图书-', LPAD(((s.n - 1) % 100) + 1, 3, '0'));

SELECT 'seed finished' AS status,
       (SELECT COUNT(*) FROM users WHERE username LIKE 'test_user_%') AS test_users,
       (SELECT COUNT(*) FROM book WHERE title LIKE '测试图书-%') AS test_books,
       (SELECT COUNT(*) FROM borrow_record br JOIN users u ON br.user_id = u.id WHERE u.username LIKE 'test_user_%') AS test_borrows;
