-- V2__seed_order_categories.sql
-- Seed data: initial 5 order categories

INSERT INTO order_categories (name, is_default, created_at, updated_at) VALUES
    ('作曲',       TRUE,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ミックス',   TRUE,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('バンド演奏', TRUE,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('楽譜作成',   TRUE,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('その他',     TRUE,  CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);
