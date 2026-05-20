--liquibase formatted sql

--changeset migration:202604_24_1
alter table ingest_schedule
    add column use_linkedin boolean not null default false;

--changeset migration:202604_24_2
create table audit_log
(
    id           uuid primary key,
    created_at   timestamp with time zone not null default now(),
    category     varchar(16)              not null,
    level        varchar(16)              not null,
    outcome      varchar(16),
    actor_user_id uuid,
    actor_email  varchar(320),
    actor_role   varchar(32),
    action       varchar(32)              not null,
    http_method  varchar(8),
    path         varchar(512),
    status_code  integer,
    ip_address   varchar(64),
    user_agent   varchar(256),
    duration_ms  bigint,
    request_id   varchar(64),
    message      text,
    logger_name  varchar(255)
);

--changeset migration:202604_24_3
create index idx_audit_log_created_at on audit_log (created_at desc);

--changeset migration:202604_24_4
create index idx_audit_log_category_created_at on audit_log (category, created_at desc);

--changeset migration:202604_24_5
create index idx_audit_log_actor_user_id on audit_log (actor_user_id);

--changeset migration:202604_24_6
create index idx_audit_log_action on audit_log (action);

--changeset migration:202604_24_7
create index idx_audit_log_actor_email_lower on audit_log (lower(actor_email));
