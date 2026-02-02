-- Migration: Update FK constraints to allow SET NULL on delete
-- Compatible with MySQL 5.7+ and 8.0+

-- Helper: Use information_schema to conditionally add columns

-- 1) Add raw_password to users (if not exists)
SET @dbname = DATABASE();
SET @tablename = 'users';
SET @columnname = 'raw_password';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE users ADD COLUMN raw_password VARCHAR(200)'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 2) Add customer_username to orders (if not exists)
SET @tablename = 'orders';
SET @columnname = 'customer_username';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN customer_username VARCHAR(100)'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 3) Add customer_email to orders (if not exists)
SET @columnname = 'customer_email';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE orders ADD COLUMN customer_email VARCHAR(200)'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 4) Add book_title to order_items (if not exists)
SET @tablename = 'order_items';
SET @columnname = 'book_title';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE order_items ADD COLUMN book_title VARCHAR(255)'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 5) Add book_author to order_items (if not exists)
SET @columnname = 'book_author';
SET @preparedStatement = (SELECT IF(
  (SELECT COUNT(*) FROM INFORMATION_SCHEMA.COLUMNS WHERE TABLE_SCHEMA = @dbname AND TABLE_NAME = @tablename AND COLUMN_NAME = @columnname) > 0,
  'SELECT 1',
  'ALTER TABLE order_items ADD COLUMN book_author VARCHAR(255)'
));
PREPARE alterIfNotExists FROM @preparedStatement;
EXECUTE alterIfNotExists;
DEALLOCATE PREPARE alterIfNotExists;

-- 6) Populate denormalized data from existing relationships
UPDATE orders o 
JOIN users u ON o.user_id = u.id 
SET o.customer_username = u.username, o.customer_email = u.email
WHERE o.customer_username IS NULL;

UPDATE order_items oi 
JOIN books b ON oi.book_id = b.id 
SET oi.book_title = b.title, oi.book_author = b.author
WHERE oi.book_title IS NULL;

-- 7) Show current FK constraints (for reference)
SELECT 'Current FK constraints on orders:' AS info;
SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'orders' AND CONSTRAINT_TYPE = 'FOREIGN KEY';

SELECT 'Current FK constraints on order_items:' AS info;
SELECT CONSTRAINT_NAME FROM INFORMATION_SCHEMA.TABLE_CONSTRAINTS 
WHERE TABLE_SCHEMA = DATABASE() AND TABLE_NAME = 'order_items' AND CONSTRAINT_TYPE = 'FOREIGN KEY';

-- Done with column additions and data population!
-- NOTE: To update FK constraints to ON DELETE SET NULL, you need to:
--   1. Run this script to see the FK constraint names above
--   2. Then run migration_update_fk_part2.sql with the correct constraint names

SELECT 'Migration part 1 complete! Check FK names above, then run part 2.' AS status;
