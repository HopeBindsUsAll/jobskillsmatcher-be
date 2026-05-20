--liquibase formatted sql

--changeset migration:202604_14_1
alter table job add column min_salary numeric(12,2);
alter table job add column max_salary numeric(12,2);
alter table job add column currency varchar(3);
alter table job add column salary_period varchar(16);

--changeset migration:202604_14_2
create table region_request
(
    id           uuid primary key,
    student_id   uuid                     not null references users (id) on delete cascade,
    country      varchar(2)               not null default '',
    city         varchar(255)             not null default '',
    requested_at timestamp with time zone not null default now()
);
create index idx_region_request_student on region_request (student_id);
create index idx_region_request_requested_at on region_request (requested_at desc);
