--liquibase formatted sql

--changeset migration:202603_31_1
create table if not exists readiness_snapshot (
    id           uuid primary key,
    student_id   uuid not null references users(id) on delete cascade,
    captured_at  timestamptz not null,
    score        double precision not null,
    top_job_ids  uuid[] not null default '{}',
    created_at   timestamptz not null default now()
);

create index if not exists ix_readiness_snapshot_student_captured
    on readiness_snapshot(student_id, captured_at desc);
