-- Test schema: create tables and FKs before Hibernate runs to avoid DDL race conditions
CREATE TABLE IF NOT EXISTS users (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  username VARCHAR(255) NOT NULL UNIQUE,
  password VARCHAR(255) NOT NULL,
  email VARCHAR(255),
  roles VARCHAR(255),
  enabled BOOLEAN,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  version BIGINT
);

CREATE TABLE IF NOT EXISTS books (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  title VARCHAR(255) NOT NULL,
  author VARCHAR(255),
  isbn VARCHAR(50),
  description VARCHAR(2000),
  price DECIMAL(10,2) NOT NULL,
  stock INT DEFAULT 0,
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  version INT DEFAULT 0
);

CREATE TABLE IF NOT EXISTS orders (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  user_id BIGINT,
  total_amount DECIMAL(19,2),
  payment_status VARCHAR(50),
  order_status VARCHAR(50),
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  version BIGINT,
  CONSTRAINT fk_orders_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS order_items (
  id BIGINT AUTO_INCREMENT PRIMARY KEY,
  order_id BIGINT,
  book_id BIGINT,
  quantity INT,
  item_type VARCHAR(50),
  rental_days INT,
  unit_price DECIMAL(10,2),
  subtotal DECIMAL(10,2),
  created_at TIMESTAMP,
  updated_at TIMESTAMP,
  version BIGINT,
  CONSTRAINT fk_order_items_order FOREIGN KEY (order_id) REFERENCES orders(id),
  CONSTRAINT fk_order_items_book FOREIGN KEY (book_id) REFERENCES books(id)
);
