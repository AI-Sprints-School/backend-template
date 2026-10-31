-- Урок 15: вход через внешнего провайдера (Keycloak). Только модуль Spring — Ktor-версию не меняет.
-- auth_provider — откуда учётка: LOCAL (почта и пароль) или registrationId клиента OAuth
-- в верхнем регистре (KEYCLOAK); external_subject — `sub` из id_token провайдера.
-- Пользователь провайдера ищется по паре «провайдер + sub»: почта у провайдера может смениться,
-- sub — нет. Столбец google_id из V1 остаётся как есть: им пользуется Ktor-версия.
ALTER TABLE users
    ADD COLUMN IF NOT EXISTS auth_provider VARCHAR(32) NOT NULL DEFAULT 'LOCAL',
    ADD COLUMN IF NOT EXISTS external_subject VARCHAR(255);

ALTER TABLE users
    ADD CONSTRAINT ux_users_provider_subject UNIQUE (auth_provider, external_subject);
