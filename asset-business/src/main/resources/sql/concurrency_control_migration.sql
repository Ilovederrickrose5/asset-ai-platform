-- =================================================================
-- 资产并发控制迁移脚本
-- 作用: 为 asset 表新增乐观锁版本号 + 申请占用字段
-- 执行时机: 部署新代码前必须先执行此脚本(ddl-auto=none,JPA不会自动建列)
-- 兼容: MySQL 8.0+
-- =================================================================

-- 1. 乐观锁版本号: @Version 字段不允许为 NULL,初始化为 0
--    用于并发审批时检测资产状态是否被其他事务修改
ALTER TABLE asset
    ADD COLUMN version BIGINT NOT NULL DEFAULT 0 COMMENT '乐观锁版本号';

-- 2. 当前未决申请占用: 同一资产同时只允许一个未决申请占用
--    通过条件 UPDATE 原子占用,防止并发重复申请
ALTER TABLE asset
    ADD COLUMN current_application_id BIGINT NULL COMMENT '当前未决申请ID(占用锁)';

-- 3. 占用字段索引: 加速 lockAsset/unlockAsset 的条件更新(走索引而非全表锁)
ALTER TABLE asset
    ADD INDEX idx_current_application_id (current_application_id);
