-- Migration Part 2: Update FK constraints to ON DELETE SET NULL
-- Uses the constraint names discovered from part 1

-- 1) Drop and recreate orders.user_id FK with ON DELETE SET NULL
ALTER TABLE orders DROP FOREIGN KEY fk_orders_user;
ALTER TABLE orders MODIFY COLUMN user_id BIGINT NULL;
ALTER TABLE orders ADD CONSTRAINT fk_orders_user 
    FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE SET NULL;

-- 2) Drop and recreate order_items.book_id FK with ON DELETE SET NULL
ALTER TABLE order_items DROP FOREIGN KEY fk_order_items_book;
ALTER TABLE order_items MODIFY COLUMN book_id BIGINT NULL;
ALTER TABLE order_items ADD CONSTRAINT fk_order_items_book 
    FOREIGN KEY (book_id) REFERENCES books(id) ON DELETE SET NULL;

SELECT 'Migration part 2 complete! FK constraints now use ON DELETE SET NULL.' AS status;
