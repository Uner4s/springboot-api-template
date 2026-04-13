-- ─────────────────────────────────────────────────────────────────────────────
-- V1 — Schema inicial
-- ─────────────────────────────────────────────────────────────────────────────

CREATE TABLE roles (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    name        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE
);

CREATE TABLE role_permissions (
    role_id    VARCHAR(36)  NOT NULL,
    permission VARCHAR(100) NOT NULL,
    PRIMARY KEY (role_id, permission),
    CONSTRAINT fk_role_permissions_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

CREATE TABLE users (
    id          VARCHAR(36)  NOT NULL PRIMARY KEY,
    email       VARCHAR(255) NOT NULL UNIQUE,
    password    VARCHAR(255) NOT NULL,
    first_name  VARCHAR(100) NOT NULL,
    last_name   VARCHAR(100) NOT NULL,
    role_id     VARCHAR(36),
    created_at  TIMESTAMP    NOT NULL,
    updated_at  TIMESTAMP    NOT NULL,
    is_deleted  BOOLEAN      NOT NULL DEFAULT FALSE,
    CONSTRAINT fk_users_role FOREIGN KEY (role_id) REFERENCES roles(id)
);

-- ─────────────────────────────────────────────────────────────────────────────
-- Seed: rol ADMIN con todos los permisos
-- ─────────────────────────────────────────────────────────────────────────────

INSERT INTO roles (id, name, created_at, updated_at, is_deleted)
VALUES ('00000000-0000-0000-0000-000000000001', 'ADMIN', NOW(), NOW(), FALSE);

INSERT INTO role_permissions (role_id, permission) VALUES
    ('00000000-0000-0000-0000-000000000001', 'READ_USERS'),
    ('00000000-0000-0000-0000-000000000001', 'MANAGE_USERS'),
    ('00000000-0000-0000-0000-000000000001', 'READ_ROLES'),
    ('00000000-0000-0000-0000-000000000001', 'MANAGE_ROLES'),
    ('00000000-0000-0000-0000-000000000001', 'READ_AUDITS'),
    ('00000000-0000-0000-0000-000000000001', 'MANAGE_AUDITS'),
    ('00000000-0000-0000-0000-000000000001', 'READ_REPORTS'),
    ('00000000-0000-0000-0000-000000000001', 'EXPORT_REPORTS'),
    ('00000000-0000-0000-0000-000000000001', 'SUPER_ADMIN');
