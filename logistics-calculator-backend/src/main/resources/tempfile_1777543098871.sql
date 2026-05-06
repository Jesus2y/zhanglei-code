-- ============================================
-- 跨境物流计算器数据库初始化脚本
-- 数据库名称: logistics_calculator
-- 字符集: utf8mb4
-- 排序规则: utf8mb4_unicode_ci
-- ============================================

-- 创建数据库
CREATE DATABASE IF NOT EXISTS `logistics_calculator` 
DEFAULT CHARACTER SET utf8mb4 
COLLATE utf8mb4_unicode_ci;

USE `logistics_calculator`;

-- ============================================
-- 1. 用户表 (users)
-- 存储微信小程序用户信息和会员状态
-- ============================================
DROP TABLE IF EXISTS `users`;
CREATE TABLE `users` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '用户唯一标识（自增主键）',
  `openid` VARCHAR(100) NOT NULL COMMENT '微信用户OpenID（唯一标识）',
  `nickname` VARCHAR(50) DEFAULT NULL COMMENT '用户昵称',
  `avatar_url` VARCHAR(200) DEFAULT NULL COMMENT '用户头像URL',
  `phone_number` VARCHAR(20) DEFAULT NULL COMMENT '用户手机号码（可选，用于会员联系和订单通知）',
  `is_member` TINYINT(1) NOT NULL DEFAULT 0 COMMENT '是否为会员（0-普通用户，1-会员）',
  `membership_expire_date` DATETIME DEFAULT NULL COMMENT '会员过期时间',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '用户创建时间（自动填充）',
  `last_calculation_date` DATETIME DEFAULT NULL COMMENT '最后一次计算日期（用于判断每日免费次数重置）',
  `calculation_count_today` INT DEFAULT 0 COMMENT '今日计算次数（普通用户每日限制3次）',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_openid` (`openid`),
  KEY `idx_phone_number` (`phone_number`),
  KEY `idx_is_member` (`is_member`),
  KEY `idx_created_at` (`created_at`)
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='用户信息表';

-- ============================================
-- 2. 计算历史记录表 (calculation_history)
-- 保存用户的物流计算历史记录
-- ============================================
DROP TABLE IF EXISTS `calculation_history`;
CREATE TABLE `calculation_history` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '历史记录唯一标识（自增主键）',
  `user_id` BIGINT NOT NULL COMMENT '用户ID（关联users表）',
  `calculation_data` TEXT NOT NULL COMMENT '计算请求数据（JSON格式字符串，保存完整的计算参数）',
  `total_cost` DECIMAL(10,2) NOT NULL COMMENT '计算得出的总费用（单位：人民币元）',
  `country` VARCHAR(10) NOT NULL COMMENT '目的国家代码（US/DE/JP）',
  `shipping_method` VARCHAR(10) NOT NULL COMMENT '物流方式（AIR/SEA）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '记录创建时间（自动填充）',
  PRIMARY KEY (`id`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_country` (`country`),
  KEY `idx_shipping_method` (`shipping_method`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_history_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='计算历史记录表';

-- ============================================
-- 3. 支付记录表 (payment_records)
-- 保存会员购买的订单和支付信息
-- ============================================
DROP TABLE IF EXISTS `payment_records`;
CREATE TABLE `payment_records` (
  `id` BIGINT NOT NULL AUTO_INCREMENT COMMENT '支付记录唯一标识（自增主键）',
  `order_no` VARCHAR(50) NOT NULL COMMENT '订单号（唯一标识，格式：M+时间戳）',
  `user_id` BIGINT NOT NULL COMMENT '用户ID（关联users表）',
  `amount` DECIMAL(10,2) NOT NULL COMMENT '支付金额（单位：人民币元）',
  `currency` VARCHAR(20) NOT NULL DEFAULT 'CNY' COMMENT '货币类型（默认：CNY-人民币）',
  `status` VARCHAR(20) NOT NULL COMMENT '订单状态（CREATED-已创建，SUCCESS-支付成功，FAILED-支付失败）',
  `transaction_id` VARCHAR(100) DEFAULT NULL COMMENT '微信支付交易号（支付成功后由微信返回）',
  `created_at` DATETIME NOT NULL DEFAULT CURRENT_TIMESTAMP COMMENT '订单创建时间（自动填充）',
  `paid_at` DATETIME DEFAULT NULL COMMENT '支付完成时间',
  PRIMARY KEY (`id`),
  UNIQUE KEY `uk_order_no` (`order_no`),
  KEY `idx_user_id` (`user_id`),
  KEY `idx_status` (`status`),
  KEY `idx_transaction_id` (`transaction_id`),
  KEY `idx_created_at` (`created_at`),
  CONSTRAINT `fk_payment_user` FOREIGN KEY (`user_id`) REFERENCES `users` (`id`) ON DELETE CASCADE
) ENGINE=InnoDB DEFAULT CHARSET=utf8mb4 COLLATE=utf8mb4_unicode_ci COMMENT='支付记录表';

-- ============================================
-- 初始化数据（可选）
-- ============================================

-- 插入测试用户数据（可选）
INSERT INTO `users` (`openid`, `nickname`, `phone_number`, `is_member`, `membership_expire_date`) 
VALUES 
('wx_test_001', '测试用户1', '13800138001', 0, NULL),
('wx_test_002', '测试用户2', '13800138002', 1, DATE_ADD(NOW(), INTERVAL 30 DAY));

-- ============================================
-- 视图和统计查询（可选）
-- ============================================

-- 创建用户统计视图
CREATE OR REPLACE VIEW `v_user_statistics` AS
SELECT 
  u.id AS user_id,
  u.openid,
  u.nickname,
  u.phone_number,
  u.is_member,
  u.membership_expire_date,
  COUNT(ch.id) AS total_calculations,
  MAX(ch.created_at) AS last_calculation_time,
  COUNT(pr.id) AS total_orders,
  SUM(CASE WHEN pr.status = 'SUCCESS' THEN pr.amount ELSE 0 END) AS total_payment
FROM users u
LEFT JOIN calculation_history ch ON u.id = ch.user_id
LEFT JOIN payment_records pr ON u.id = pr.user_id
GROUP BY u.id, u.openid, u.nickname, u.phone_number, u.is_member, u.membership_expire_date;

-- ============================================
-- 索引优化建议
-- ============================================

-- 为常用查询添加复合索引
-- 1. 用户查询今日计算次数
ALTER TABLE `users` ADD INDEX `idx_user_daily_calc` (`last_calculation_date`, `calculation_count_today`);

-- 2. 历史记录按用户和时间查询
ALTER TABLE `calculation_history` ADD INDEX `idx_user_created` (`user_id`, `created_at` DESC);

-- 3. 支付记录按用户和状态查询
ALTER TABLE `payment_records` ADD INDEX `idx_user_status` (`user_id`, `status`);

-- ============================================
-- 授权（根据实际情况修改用户名和密码）
-- ============================================

-- 创建专用数据库用户（可选，生产环境建议使用）
-- CREATE USER 'logistics_user'@'localhost' IDENTIFIED BY 'your_secure_password';
-- GRANT ALL PRIVILEGES ON `logistics_calculator`.* TO 'logistics_user'@'localhost';
-- FLUSH PRIVILEGES;

-- ============================================
-- 脚本执行完成
-- ============================================

SELECT '数据库初始化完成！' AS message;
SELECT COUNT(*) AS user_count FROM users;
SELECT COUNT(*) AS history_count FROM calculation_history;
SELECT COUNT(*) AS payment_count FROM payment_records;
