USE library_system;

-- users.interest_tags
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'users'
              AND COLUMN_NAME = 'interest_tags'
        ),
        'SELECT 1',
        'ALTER TABLE users ADD COLUMN interest_tags VARCHAR(255) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- book.image_url
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'book'
              AND COLUMN_NAME = 'image_url'
        ),
        'SELECT 1',
        'ALTER TABLE book ADD COLUMN image_url VARCHAR(500) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- borrow_record.approved_time
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'borrow_record'
              AND COLUMN_NAME = 'approved_time'
        ),
        'SELECT 1',
        'ALTER TABLE borrow_record ADD COLUMN approved_time DATETIME NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- borrow_record.pickup_code
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'borrow_record'
              AND COLUMN_NAME = 'pickup_code'
        ),
        'SELECT 1',
        'ALTER TABLE borrow_record ADD COLUMN pickup_code VARCHAR(40) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- borrow_record.review_comment
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'borrow_record'
              AND COLUMN_NAME = 'review_comment'
        ),
        'SELECT 1',
        'ALTER TABLE borrow_record ADD COLUMN review_comment VARCHAR(500) NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- recommendation.model_score
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'recommendation'
              AND COLUMN_NAME = 'model_score'
        ),
        'SELECT 1',
        'ALTER TABLE recommendation ADD COLUMN model_score DOUBLE NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;

-- recommendation.final_score
SET @sql = (
    SELECT IF(
        EXISTS (
            SELECT 1
            FROM information_schema.COLUMNS
            WHERE TABLE_SCHEMA = DATABASE()
              AND TABLE_NAME = 'recommendation'
              AND COLUMN_NAME = 'final_score'
        ),
        'SELECT 1',
        'ALTER TABLE recommendation ADD COLUMN final_score DOUBLE NULL'
    )
);
PREPARE stmt FROM @sql;
EXECUTE stmt;
DEALLOCATE PREPARE stmt;
