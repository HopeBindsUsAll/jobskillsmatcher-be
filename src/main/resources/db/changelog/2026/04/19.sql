--liquibase formatted sql

--changeset migration:202604_19_1
create table app_auth
(
    id         uuid primary key,
    user_id    uuid                     not null references users (id) on delete cascade,
    jti        uuid                     not null unique,
    created_at timestamp with time zone not null default now()
);
create index idx_app_auth_user on app_auth (user_id);
