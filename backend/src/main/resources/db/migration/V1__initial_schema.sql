-- V1__initial_schema.sql
-- Initial schema creation for FreelanceMusicCRM

CREATE TABLE workers (
    id                  BIGSERIAL PRIMARY KEY,
    email               VARCHAR(255) NOT NULL UNIQUE,
    password_hash       VARCHAR(255) NOT NULL,
    name                VARCHAR(255) NOT NULL,
    contact             VARCHAR(255),
    is_locked           BOOLEAN      NOT NULL DEFAULT FALSE,
    failed_login_count  INT          NOT NULL DEFAULT 0,
    locked_at           TIMESTAMP,
    created_at          TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE order_categories (
    id          BIGSERIAL PRIMARY KEY,
    name        VARCHAR(255) NOT NULL,
    is_default  BOOLEAN      NOT NULL DEFAULT FALSE,
    created_at  TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at  TIMESTAMP
);

CREATE TABLE quote_requests (
    id                    BIGSERIAL PRIMARY KEY,
    subject               VARCHAR(255) NOT NULL,
    client_name           VARCHAR(255) NOT NULL,
    client_email          VARCHAR(255),
    category_id           BIGINT       NOT NULL REFERENCES order_categories(id),
    desired_delivery_date DATE         NOT NULL,
    file_path_url         TEXT,
    comment               TEXT,
    status                VARCHAR(50)  NOT NULL DEFAULT 'PENDING',
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP,
    updated_at            TIMESTAMP
);

CREATE TABLE quote_responses (
    id                     BIGSERIAL PRIMARY KEY,
    quote_request_id       BIGINT         NOT NULL UNIQUE REFERENCES quote_requests(id),
    amount                 DECIMAL(10, 2) NOT NULL,
    response_delivery_date DATE           NOT NULL,
    response_comment       TEXT,
    approval_token         VARCHAR(255)   NOT NULL UNIQUE,
    token_status           VARCHAR(50)    NOT NULL DEFAULT 'ACTIVE',
    created_at             TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE orders (
    id                    BIGSERIAL PRIMARY KEY,
    quote_response_id     BIGINT       NOT NULL UNIQUE REFERENCES quote_responses(id),
    subject               VARCHAR(255) NOT NULL,
    client_name           VARCHAR(255) NOT NULL,
    client_email          VARCHAR(255),
    category_id           BIGINT       NOT NULL REFERENCES order_categories(id),
    desired_delivery_date DATE         NOT NULL,
    file_path_url         TEXT,
    comment               TEXT,
    status                VARCHAR(50)  NOT NULL,
    created_at            TIMESTAMP    NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE tasks (
    id                BIGSERIAL PRIMARY KEY,
    order_id          BIGINT      NOT NULL UNIQUE REFERENCES orders(id),
    status            VARCHAR(50) NOT NULL DEFAULT 'NOT_STARTED',
    status_updated_at TIMESTAMP,
    created_at        TIMESTAMP   NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE invoices (
    id             BIGSERIAL PRIMARY KEY,
    task_id        BIGINT         NOT NULL UNIQUE REFERENCES tasks(id),
    subject        VARCHAR(255)   NOT NULL,
    client_name    VARCHAR(255)   NOT NULL,
    client_email   VARCHAR(255),
    category_name  VARCHAR(255)   NOT NULL,
    delivery_date  DATE           NOT NULL,
    amount         DECIMAL(10, 2) NOT NULL,
    issue_date     DATE           NOT NULL,
    worker_name    VARCHAR(255)   NOT NULL,
    worker_contact VARCHAR(255),
    created_at     TIMESTAMP      NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE TABLE worker_settings (
    id                       BIGSERIAL PRIMARY KEY,
    worker_id                BIGINT       NOT NULL REFERENCES workers(id),
    discord_webhook_url      VARCHAR(255),
    discord_enabled          BOOLEAN      NOT NULL DEFAULT FALSE,
    smtp_host                VARCHAR(255),
    smtp_port                INT,
    smtp_username            VARCHAR(255),
    smtp_password_encrypted  VARCHAR(255),
    mail_enabled             BOOLEAN      NOT NULL DEFAULT FALSE,
    updated_at               TIMESTAMP
);
