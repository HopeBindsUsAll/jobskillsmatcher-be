--liquibase formatted sql

--changeset migration:202604_07_1
create table learning_resource
(
    id                uuid primary key,
    type              varchar(16)              not null,
    difficulty        varchar(16)              not null,
    title             varchar(300)             not null,
    description       text                     not null default '',
    url               varchar(2000)            not null,
    provider          varchar(200)             not null default '',
    url_alive         boolean                  not null default true,
    last_validated_at timestamp with time zone,
    created_at        timestamp with time zone not null default now(),
    updated_at        timestamp with time zone not null default now()
);

--changeset migration:202604_07_2
create index idx_learning_resource_type on learning_resource (type);
create index idx_learning_resource_difficulty on learning_resource (difficulty);
create index idx_learning_resource_url_alive on learning_resource (url_alive);

--changeset migration:202604_07_3
create table resource_skill
(
    resource_id uuid         not null references learning_resource (id) on delete cascade,
    skill_id    varchar(255) not null references skill (id) on delete cascade,
    primary key (resource_id, skill_id)
);

--changeset migration:202604_07_4
create index idx_resource_skill_skill_id on resource_skill (skill_id);

--changeset migration:202604_07_5
create table cv_upload
(
    id                uuid primary key,
    student_id        uuid                     not null references users (id) on delete cascade,
    filename          varchar(500)             not null,
    content_type      varchar(200)             not null default '',
    size_bytes        bigint                   not null default 0,
    extracted_skills  jsonb,
    uploaded_at       timestamp with time zone not null default now()
);

--changeset migration:202604_07_6
create index idx_cv_upload_student on cv_upload (student_id, uploaded_at desc);
