-- Migration to add raw_password column to users table
-- Run this manually if you have existing data in the database

-- Add the raw_password column if it doesn't exist
-- MySQL 8.0+ supports "IF NOT EXISTS" in ALTER TABLE
-- For older versions, just run the ALTER TABLE and ignore the error if column exists

ALTER TABLE users ADD COLUMN raw_password VARCHAR(200);

-- Note: For existing users, the raw_password will be NULL since we can't 
-- reverse the bcrypt hash. New users created through the admin panel will 
-- have their raw passwords stored.
-- 
-- For the default 'admin' user, you can manually update it:
-- UPDATE users SET raw_password = 'admin' WHERE username = 'admin';
