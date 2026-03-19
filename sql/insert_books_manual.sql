USE library_system;
INSERT INTO book(title, author, category_code, stock, location, image_url, created_time, category_id)
VALUES
('现代软件架构设计', '张三', 'CAT-01', 15, 'A-01-01', NULL, NOW(), (SELECT id FROM category WHERE name='计算机' LIMIT 1)),
('高性能 MySQL 实战', '李四', 'CAT-01', 12, 'A-01-02', NULL, NOW(), (SELECT id FROM category WHERE name='计算机' LIMIT 1)),
('管理心理学导论', '王五', 'CAT-08', 8, 'B-02-01', NULL, NOW(), (SELECT id FROM category WHERE name='心理' LIMIT 1)),
('艺术史与现代设计', '赵六', 'CAT-05', 10, 'C-03-01', NULL, NOW(), (SELECT id FROM category WHERE name='艺术' LIMIT 1)),
('经济学原理应用', '孙七', 'CAT-07', 9, 'D-04-02', NULL, NOW(), (SELECT id FROM category WHERE name='经济' LIMIT 1));

SELECT id,title,author,category_code,stock,location FROM book ORDER BY id DESC LIMIT 5;
