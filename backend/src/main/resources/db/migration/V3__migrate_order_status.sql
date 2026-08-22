-- V3__migrate_order_status.sql
-- Normalize legacy order status text values to enum-compatible values.

UPDATE orders
SET status = 'RECEIVED'
WHERE status = '受注済み';
