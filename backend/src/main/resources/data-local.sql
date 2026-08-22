-- パスワード: password
INSERT INTO workers (email, password_hash, name, contact, is_locked, failed_login_count, created_at) VALUES
    ('worker@example.com', '$2a$10$92IXUNpkjO0rOQ5byMi.Ye4oKoEa3Ro9llC/.og/at2.uheWG/igi', '統合作業者', 'worker-contact@example.com', FALSE, 0, CURRENT_TIMESTAMP);

INSERT INTO order_categories (name, is_default, created_at, updated_at) VALUES
    ('作曲', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('ミックス', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('バンド演奏', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('楽譜作成', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP),
    ('その他', TRUE, CURRENT_TIMESTAMP, CURRENT_TIMESTAMP);