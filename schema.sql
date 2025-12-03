-- This script creates the database schema for the Online Bookstore project.
-- Database: MySQL

DROP TABLE IF EXISTS Order_Items;
DROP TABLE IF EXISTS Orders;
DROP TABLE IF EXISTS Books;
DROP TABLE IF EXISTS Users;

--
-- Table structure for table `Users`
--
CREATE TABLE Users (
    user_id INT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(50) UNIQUE NOT NULL,
    password_hash VARCHAR(60) NOT NULL,
    email VARCHAR(100) UNIQUE NOT NULL,
    role ENUM('CUSTOMER', 'MANAGER') NOT NULL DEFAULT 'CUSTOMER',
    created_at TIMESTAMP DEFAULT CURRENT_TIMESTAMP
);

--
-- Table structure for table `Books`
--
CREATE TABLE Books (
    book_id INT AUTO_INCREMENT PRIMARY KEY,
    title VARCHAR(255) NOT NULL,
    author VARCHAR(255) NOT NULL,
    buy_price DECIMAL(10, 2) NOT NULL,
    rent_price DECIMAL(10, 2) NOT NULL,
    is_available_for_rent BOOLEAN DEFAULT true,
    INDEX(title),
    INDEX(author)
);

--
-- Table structure for table `Orders`
--
CREATE TABLE Orders (
    order_id INT AUTO_INCREMENT PRIMARY KEY,
    user_id INT NOT NULL,
    order_date TIMESTAMP DEFAULT CURRENT_TIMESTAMP,
    total_amount DECIMAL(10, 2) NOT NULL,
    payment_status ENUM('Pending', 'Paid') NOT NULL DEFAULT 'Pending',
    FOREIGN KEY (user_id) REFERENCES Users(user_id)
);

--
-- Table structure for table `Order_Items`
--
CREATE TABLE Order_Items (
    item_id INT AUTO_INCREMENT PRIMARY KEY,

    order_id INT NOT NULL,

    book_id INT NOT NULL,

    type ENUM('BUY', 'RENT') NOT NULL,

    price_at_purchase DECIMAL(10, 2) NOT NULL,
    FOREIGN KEY (order_id) REFERENCES Orders(order_id) ON DELETE CASCADE,
    FOREIGN KEY (book_id) REFERENCES Books(book_id)
);


INSERT INTO Books (title, author, buy_price, rent_price)
VALUES
('The Pragmatic Programmer', 'David Thomas', 45.00, 10.50),
('Clean Code', 'Robert C. Martin', 38.50, 9.99),
('Cracking the Coding Interview', 'Gayle Laakmann McDowell', 30.00, 12.00),
('Designing Data-Intensive Applications', 'Martin Kleppmann', 55.20, 15.00),
('Head First Design Patterns', 'Eric Freeman', 42.00, 11.25);


INSERT INTO Users (username, password_hash, email, role)
VALUES
('manager', '$2a$10$e.P0.w1/v0i8N9yv..fHgeIEiFLqjW.27.N.E.qJBeA0yM1be.1Ea', 'manager@bookstore.com', 'MANAGER');

INSERT INTO Users (username, password_hash, email, role)
VALUES
('customer', '$2a$10$4B./E.0696.R6y8/30.1P.iA31.C/iS7.7g.j/o.K.X.Y.Y.4/0.G', 'customer@example.com', 'CUSTOMER');
