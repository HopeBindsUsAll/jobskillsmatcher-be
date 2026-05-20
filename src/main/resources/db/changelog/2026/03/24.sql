--liquibase formatted sql

--changeset migration:202603_24_1
create table job
(
    id           uuid                     primary key,
    external_id  varchar(255)             not null unique,
    title        varchar(500)             not null,
    company      varchar(500)             not null default '',
    country      varchar(2)               not null default '',
    city         varchar(255)             not null default '',
    remote       boolean                  not null default false,
    seniority    varchar(32)              not null default '',
    description  text                     not null default '',
    source_url   varchar(2000)            not null default '',
    posted_at    timestamp with time zone,
    raw_payload  jsonb,
    created_at   timestamp with time zone not null default now(),
    updated_at   timestamp with time zone not null default now()
);

--changeset migration:202603_24_2
create index idx_job_country on job (country);
create index idx_job_posted_at on job (posted_at desc nulls last);
create index idx_job_title_trgm on job using gin (title gin_trgm_ops);

--changeset migration:202603_24_3
create table job_skill
(
    job_id      uuid         not null references job (id) on delete cascade,
    skill_id    varchar(255) not null references skill (id) on delete cascade,
    requirement varchar(16)  not null,
    primary key (job_id, skill_id)
);

--changeset migration:202603_24_4
create index idx_job_skill_skill_id on job_skill (skill_id);

--changeset migration:202603_24_5
create table ingest_schedule
(
    id              uuid                     primary key,
    name            varchar(200)             not null,
    query           varchar(500)             not null,
    country         varchar(2)               not null default '',
    city            varchar(255)             not null default '',
    remote          boolean                  not null default false,
    cron_expression varchar(120)             not null,
    enabled         boolean                  not null default true,
    last_run_at     timestamp with time zone,
    created_at      timestamp with time zone not null default now(),
    updated_at      timestamp with time zone not null default now()
);

--changeset migration:202603_24_6
create table ingest_run
(
    id            uuid                     primary key,
    schedule_id   uuid references ingest_schedule (id) on delete set null,
    query         varchar(500)             not null,
    country       varchar(2)               not null default '',
    started_at    timestamp with time zone not null default now(),
    finished_at   timestamp with time zone,
    status        varchar(16)              not null,
    fetched_count integer                  not null default 0,
    stored_count  integer                  not null default 0,
    error         text
);

--changeset migration:202603_24_7
create index idx_ingest_run_schedule on ingest_run (schedule_id);
create index idx_ingest_run_started_at on ingest_run (started_at desc);
