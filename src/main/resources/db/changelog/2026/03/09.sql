--liquibase formatted sql

--changeset migration:202603_09_1
create table users
(
    id            uuid primary key,
    email         varchar(320)             not null unique,
    password_hash varchar(100),
    google_sub    varchar(255) unique,
    role          varchar(32)              not null default 'STUDENT',
    enabled       boolean                  not null default true,
    created_at    timestamp with time zone not null default now(),
    updated_at    timestamp with time zone not null default now()
);

--changeset migration:202603_09_2
create table student_profile
(
    user_id           uuid primary key references users (id) on delete cascade,
    full_name         varchar(200),
    preferred_role    varchar(120),
    country           varchar(2),
    city              varchar(120),
    remote_preference varchar(32),
    version           bigint                   not null default 0,
    created_at        timestamp with time zone not null default now(),
    updated_at        timestamp with time zone not null default now()
);

--changeset migration:202603_09_3
create table admin_profile
(
    user_id      uuid primary key references users (id) on delete cascade,
    display_name varchar(200),
    version      bigint                   not null default 0,
    created_at   timestamp with time zone not null default now(),
    updated_at   timestamp with time zone not null default now()
);

--changeset migration:202603_09_4
create index idx_users_email on users (email);
