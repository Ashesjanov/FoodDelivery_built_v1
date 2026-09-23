-- Food Delivery MySQL 8 schema and minimum usable seed data.
CREATE DATABASE IF NOT EXISTS `food_delivery`
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_0900_ai_ci;
USE `food_delivery`;

CREATE TABLE IF NOT EXISTS `user_account` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `username` VARCHAR(50) NOT NULL,
  `phone` VARCHAR(20) NULL,
  `email` VARCHAR(120) NULL,
  `password_hash` VARCHAR(255) NOT NULL,
  `nickname` VARCHAR(50) NOT NULL,
  `avatar_url` VARCHAR(500) NULL,
  `role` VARCHAR(20) NOT NULL DEFAULT 'CUSTOMER',
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `last_login_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_user_account_username` (`username`),
  UNIQUE KEY `uk_user_account_phone` (`phone`),
  UNIQUE KEY `uk_user_account_email` (`email`),
  KEY `idx_user_account_role_status` (`role`, `status`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `user_address` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `contact_name` VARCHAR(32) NOT NULL,
  `phone` VARCHAR(20) NOT NULL,
  `province` VARCHAR(32) NOT NULL,
  `city` VARCHAR(32) NOT NULL,
  `district` VARCHAR(32) NULL,
  `detail` VARCHAR(255) NOT NULL,
  `longitude` DECIMAL(10,7) NULL,
  `latitude` DECIMAL(10,7) NULL,
  `label` VARCHAR(20) NULL,
  `is_default` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_user_address_user` (`user_id`, `is_default`),
  CONSTRAINT `fk_user_address_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `merchant_category` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(50) NOT NULL,
  `icon_url` VARCHAR(500) NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `enabled` TINYINT(1) NOT NULL DEFAULT 1,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_category_name` (`name`),
  KEY `idx_merchant_category_enabled_sort` (`enabled`, `sort_order`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `merchant` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `owner_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500) NULL,
  `logo_url` VARCHAR(500) NULL,
  `contact_name` VARCHAR(32) NOT NULL,
  `contact_phone` VARCHAR(20) NOT NULL,
  `province` VARCHAR(32) NOT NULL,
  `city` VARCHAR(32) NOT NULL,
  `district` VARCHAR(32) NULL,
  `address` VARCHAR(255) NOT NULL,
  `longitude` DECIMAL(10,7) NULL,
  `latitude` DECIMAL(10,7) NULL,
  `business_status` VARCHAR(20) NOT NULL DEFAULT 'PREPARING',
  `business_hours` VARCHAR(100) NULL,
  `min_order_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `delivery_fee` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `packaging_fee` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `rating` DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  `monthly_sales` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_merchant_owner` (`owner_id`),
  KEY `idx_merchant_city_status` (`city`, `business_status`),
  CONSTRAINT `fk_merchant_owner` FOREIGN KEY (`owner_id`) REFERENCES `user_account` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `merchant_category_rel` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `merchant_id` BIGINT UNSIGNED NOT NULL,
  `category_id` BIGINT UNSIGNED NOT NULL,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_merchant_category_rel` (`merchant_id`, `category_id`),
  KEY `idx_merchant_category_rel_category` (`category_id`, `sort_order`),
  CONSTRAINT `fk_merchant_category_rel_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_merchant_category_rel_category` FOREIGN KEY (`category_id`) REFERENCES `merchant_category` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `dish` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `merchant_id` BIGINT UNSIGNED NOT NULL,
  `category_id` BIGINT UNSIGNED NULL,
  `name` VARCHAR(100) NOT NULL,
  `description` VARCHAR(500) NULL,
  `image_url` VARCHAR(500) NULL,
  `price` DECIMAL(12,2) NOT NULL,
  `original_price` DECIMAL(12,2) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ON_SALE',
  `stock` INT NOT NULL DEFAULT 0,
  `sales` INT NOT NULL DEFAULT 0,
  `sort_order` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dish_merchant_status` (`merchant_id`, `status`, `sort_order`),
  KEY `idx_dish_category` (`category_id`),
  CONSTRAINT `fk_dish_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_dish_category` FOREIGN KEY (`category_id`) REFERENCES `merchant_category` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `dish_spec` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `dish_id` BIGINT UNSIGNED NOT NULL,
  `group_name` VARCHAR(50) NOT NULL DEFAULT '规格',
  `name` VARCHAR(50) NOT NULL,
  `price_offset` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `sort_order` INT NOT NULL DEFAULT 0,
  `is_default` TINYINT(1) NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_dish_spec_dish` (`dish_id`, `sort_order`),
  CONSTRAINT `fk_dish_spec_dish` FOREIGN KEY (`dish_id`) REFERENCES `dish` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `cart_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `merchant_id` BIGINT UNSIGNED NOT NULL,
  `dish_id` BIGINT UNSIGNED NOT NULL,
  `dish_spec_id` BIGINT UNSIGNED NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'ACTIVE',
  `quantity` INT NOT NULL DEFAULT 1,
  `unit_price` DECIMAL(12,2) NOT NULL,
  `selected` TINYINT(1) NOT NULL DEFAULT 1,
  `note` VARCHAR(255) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_cart_item_user_status` (`user_id`, `status`, `merchant_id`),
  KEY `idx_cart_item_dish` (`dish_id`, `dish_spec_id`),
  CONSTRAINT `fk_cart_item_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cart_item_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cart_item_dish` FOREIGN KEY (`dish_id`) REFERENCES `dish` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_cart_item_spec` FOREIGN KEY (`dish_spec_id`) REFERENCES `dish_spec` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `orders` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_no` VARCHAR(40) NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `merchant_id` BIGINT UNSIGNED NOT NULL,
  `address_id` BIGINT UNSIGNED NULL,
  `coupon_claim_id` BIGINT UNSIGNED NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING_PAYMENT',
  `total_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `delivery_fee` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `packaging_fee` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `discount_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `payable_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `user_note` VARCHAR(500) NULL,
  `merchant_note` VARCHAR(500) NULL,
  `cancel_reason` VARCHAR(255) NULL,
  `contact_phone` VARCHAR(20) NOT NULL,
  `delivery_address_snapshot` VARCHAR(500) NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `accepted_at` DATETIME NULL,
  `ready_at` DATETIME NULL,
  `picked_at` DATETIME NULL,
  `delivered_at` DATETIME NULL,
  `cancelled_at` DATETIME NULL,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_orders_order_no` (`order_no`),
  KEY `idx_orders_user_created` (`user_id`, `created_at`),
  KEY `idx_orders_merchant_status` (`merchant_id`, `status`, `created_at`),
  KEY `idx_orders_coupon_claim` (`coupon_claim_id`),
  CONSTRAINT `fk_orders_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_orders_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_orders_address` FOREIGN KEY (`address_id`) REFERENCES `user_address` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `order_item` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `dish_id` BIGINT UNSIGNED NOT NULL,
  `dish_spec_id` BIGINT UNSIGNED NULL,
  `dish_name` VARCHAR(100) NOT NULL,
  `spec_name` VARCHAR(50) NULL,
  `unit_price` DECIMAL(12,2) NOT NULL,
  `quantity` INT NOT NULL,
  `subtotal` DECIMAL(12,2) NOT NULL,
  `note` VARCHAR(255) NULL,
  PRIMARY KEY (`id`),
  KEY `idx_order_item_order` (`order_id`),
  KEY `idx_order_item_dish` (`dish_id`),
  CONSTRAINT `fk_order_item_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_order_item_dish` FOREIGN KEY (`dish_id`) REFERENCES `dish` (`id`) ON DELETE RESTRICT,
  CONSTRAINT `fk_order_item_spec` FOREIGN KEY (`dish_spec_id`) REFERENCES `dish_spec` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `coupon` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `name` VARCHAR(100) NOT NULL,
  `code` VARCHAR(40) NOT NULL,
  `coupon_type` VARCHAR(20) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'UPCOMING',
  `threshold_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `discount_amount` DECIMAL(12,2) NULL,
  `discount_rate` DECIMAL(5,2) NULL,
  `total_quantity` INT NOT NULL DEFAULT 0,
  `claimed_quantity` INT NOT NULL DEFAULT 0,
  `per_user_limit` INT NOT NULL DEFAULT 1,
  `start_time` DATETIME NOT NULL,
  `end_time` DATETIME NOT NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_coupon_code` (`code`),
  KEY `idx_coupon_status_time` (`status`, `start_time`, `end_time`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `coupon_claim` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `coupon_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `order_id` BIGINT UNSIGNED NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'UNUSED',
  `claimed_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `used_at` DATETIME NULL,
  `expires_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  KEY `idx_coupon_claim_coupon_user` (`coupon_id`, `user_id`, `status`),
  KEY `idx_coupon_claim_user_status` (`user_id`, `status`, `expires_at`),
  KEY `idx_coupon_claim_order` (`order_id`),
  CONSTRAINT `fk_coupon_claim_coupon` FOREIGN KEY (`coupon_id`) REFERENCES `coupon` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_coupon_claim_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_coupon_claim_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `delivery_rider` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `name` VARCHAR(50) NOT NULL,
  `phone` VARCHAR(20) NOT NULL,
  `vehicle_type` VARCHAR(20) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'OFFLINE',
  `current_longitude` DECIMAL(10,7) NULL,
  `current_latitude` DECIMAL(10,7) NULL,
  `rating` DECIMAL(3,2) NOT NULL DEFAULT 0.00,
  `completed_count` INT NOT NULL DEFAULT 0,
  `active_order_count` INT NOT NULL DEFAULT 0,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_delivery_rider_user` (`user_id`),
  KEY `idx_delivery_rider_status` (`status`, `active_order_count`),
  CONSTRAINT `fk_delivery_rider_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `delivery_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `rider_id` BIGINT UNSIGNED NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'WAITING_RIDER',
  `accepted_at` DATETIME NULL,
  `picked_up_at` DATETIME NULL,
  `delivered_at` DATETIME NULL,
  `pickup_code` VARCHAR(20) NULL,
  `delivery_note` VARCHAR(255) NULL,
  `distance_km` DECIMAL(8,2) NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_delivery_record_order` (`order_id`),
  KEY `idx_delivery_record_rider_status` (`rider_id`, `status`),
  CONSTRAINT `fk_delivery_record_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_delivery_record_rider` FOREIGN KEY (`rider_id`) REFERENCES `delivery_rider` (`id`) ON DELETE SET NULL
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `payment_record` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `payment_no` VARCHAR(40) NOT NULL,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `amount` DECIMAL(12,2) NOT NULL,
  `payment_method` VARCHAR(20) NOT NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'PENDING',
  `transaction_no` VARCHAR(80) NULL,
  `failure_reason` VARCHAR(255) NULL,
  `refunded_amount` DECIMAL(12,2) NOT NULL DEFAULT 0.00,
  `paid_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_payment_record_payment_no` (`payment_no`),
  KEY `idx_payment_record_order` (`order_id`, `status`),
  KEY `idx_payment_record_user` (`user_id`, `created_at`),
  CONSTRAINT `fk_payment_record_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_payment_record_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE RESTRICT
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

CREATE TABLE IF NOT EXISTS `review` (
  `id` BIGINT UNSIGNED NOT NULL AUTO_INCREMENT,
  `order_id` BIGINT UNSIGNED NOT NULL,
  `user_id` BIGINT UNSIGNED NOT NULL,
  `merchant_id` BIGINT UNSIGNED NOT NULL,
  `rating` TINYINT UNSIGNED NOT NULL,
  `content` VARCHAR(1000) NULL,
  `image_url` VARCHAR(500) NULL,
  `reply_content` VARCHAR(1000) NULL,
  `status` VARCHAR(20) NOT NULL DEFAULT 'VISIBLE',
  `replied_at` DATETIME NULL,
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP,
  `updated_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP ON UPDATE CURRENT_TIMESTAMP,
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_review_order` (`order_id`),
  KEY `idx_review_merchant_status` (`merchant_id`, `status`, `created_at`),
  KEY `idx_review_user` (`user_id`, `created_at`),
  CONSTRAINT `fk_review_order` FOREIGN KEY (`order_id`) REFERENCES `orders` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_review_user` FOREIGN KEY (`user_id`) REFERENCES `user_account` (`id`) ON DELETE CASCADE,
  CONSTRAINT `fk_review_merchant` FOREIGN KEY (`merchant_id`) REFERENCES `merchant` (`id`) ON DELETE CASCADE,
  CONSTRAINT `chk_review_rating` CHECK (`rating` BETWEEN 1 AND 5)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_0900_ai_ci;

-- Minimum usable development data. Password hashes are placeholders; application
-- authentication replaces them with BCrypt hashes when users register normally.
INSERT INTO `user_account`
  (`id`, `username`, `phone`, `email`, `password_hash`, `nickname`, `avatar_url`, `role`, `status`, `last_login_at`, `created_at`, `updated_at`)
VALUES
  (1, 'admin', '13800000001', 'admin@example.test', '$2a$10$developmenthashdevelopmenthashdevelopmenthash', '平台管理员', NULL, 'ADMIN', 'ACTIVE', NULL, NOW(), NOW()),
  (2, 'customer', '13800000002', 'customer@example.test', '$2a$10$developmenthashdevelopmenthashdevelopmenthash', '演示顾客', NULL, 'CUSTOMER', 'ACTIVE', NULL, NOW(), NOW()),
  (3, 'merchant', '13800000003', 'merchant@example.test', '$2a$10$developmenthashdevelopmenthashdevelopmenthash', '演示商家', NULL, 'MERCHANT', 'ACTIVE', NULL, NOW(), NOW()),
  (4, 'rider', '13800000004', 'rider@example.test', '$2a$10$developmenthashdevelopmenthashdevelopmenthash', '演示骑手', NULL, 'RIDER', 'ACTIVE', NULL, NOW(), NOW())
ON DUPLICATE KEY UPDATE `updated_at` = NOW();

INSERT INTO `user_address`
  (`id`, `user_id`, `contact_name`, `phone`, `province`, `city`, `district`, `detail`, `longitude`, `latitude`, `label`, `is_default`, `created_at`, `updated_at`)
VALUES
  (1, 2, '演示顾客', '13800000002', '上海市', '上海市', '浦东新区', '世纪大道 100 号', 121.5060000, 31.2450000, '公司', 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `updated_at` = NOW();

INSERT INTO `merchant_category` (`id`, `name`, `icon_url`, `sort_order`, `enabled`, `created_at`, `updated_at`)
VALUES (1, '快餐便当', NULL, 1, 1, NOW(), NOW()), (2, '饮品甜点', NULL, 2, 1, NOW(), NOW())
ON DUPLICATE KEY UPDATE `updated_at` = NOW();

INSERT INTO `merchant`
  (`id`, `owner_id`, `name`, `description`, `logo_url`, `contact_name`, `contact_phone`, `province`, `city`, `district`, `address`, `longitude`, `latitude`, `business_status`, `business_hours`, `min_order_amount`, `delivery_fee`, `packaging_fee`, `rating`, `monthly_sales`, `created_at`, `updated_at`)
VALUES
  (1, 3, '邻里便当', '本地演示商家，提供快餐便当。', NULL, '演示商家', '13800000003', '上海市', '上海市', '浦东新区', '世纪大道 200 号', 121.5070000, 31.2460000, 'OPEN', '09:00-21:00', 20.00, 5.00, 1.00, 4.80, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `updated_at` = NOW();

INSERT INTO `merchant_category_rel` (`id`, `merchant_id`, `category_id`, `sort_order`, `created_at`)
VALUES (1, 1, 1, 1, NOW())
ON DUPLICATE KEY UPDATE `sort_order` = VALUES(`sort_order`);

INSERT INTO `dish`
  (`id`, `merchant_id`, `category_id`, `name`, `description`, `image_url`, `price`, `original_price`, `status`, `stock`, `sales`, `sort_order`, `created_at`, `updated_at`)
VALUES
  (1, 1, 1, '招牌鸡腿饭', '香煎鸡腿配时蔬和米饭。', NULL, 28.00, 32.00, 'ON_SALE', 100, 0, 1, NOW(), NOW()),
  (2, 1, 2, '柠檬茶', '冰爽柠檬茶。', NULL, 10.00, NULL, 'ON_SALE', 100, 0, 2, NOW(), NOW())
ON DUPLICATE KEY UPDATE `updated_at` = NOW();

INSERT INTO `dish_spec`
  (`id`, `dish_id`, `group_name`, `name`, `price_offset`, `sort_order`, `is_default`, `created_at`, `updated_at`)
VALUES
  (1, 1, '份量', '标准份', 0.00, 1, 1, NOW(), NOW()),
  (2, 1, '份量', '加大份', 5.00, 2, 0, NOW(), NOW())
ON DUPLICATE KEY UPDATE `updated_at` = NOW();

INSERT INTO `coupon`
  (`id`, `name`, `code`, `coupon_type`, `status`, `threshold_amount`, `discount_amount`, `discount_rate`, `total_quantity`, `claimed_quantity`, `per_user_limit`, `start_time`, `end_time`, `created_at`, `updated_at`)
VALUES
  (1, '新客满减券', 'WELCOME10', 'FIXED', 'ACTIVE', 50.00, 10.00, NULL, 1000, 0, 1, DATE_SUB(NOW(), INTERVAL 1 DAY), DATE_ADD(NOW(), INTERVAL 30 DAY), NOW(), NOW())
ON DUPLICATE KEY UPDATE `updated_at` = NOW();
