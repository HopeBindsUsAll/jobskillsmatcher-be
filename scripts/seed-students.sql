-- Plain SQL script (NOT a Liquibase migration). Run with:
--   psql "postgresql://jsm:jsm@localhost:5433/jobskillsmatcher" -f scripts/seed-students.sql
--
-- Creates 10 archetypal student accounts plus a local admin so the matching
-- and recommendation features have realistic demo data. All passwords are the
-- literal string 'studenttest' (bcrypt-hashed below). Idempotent: re-runs are
-- safe via ON CONFLICT DO NOTHING.
--
-- Auth:
--   admin@local       password = studenttest        (role ADMIN)
--   student1@test     password = studenttest        (role STUDENT)
--   student2@test     password = studenttest
--   ... through student10@test
--
-- The bcrypt hash below was produced with Python's bcrypt library at strength
-- 10 ($2b$10$...). Spring's BCryptPasswordEncoder accepts both $2a$ and $2b$
-- versions on verification.

BEGIN;

-- Admin + 10 students
INSERT INTO users (id, email, password_hash, role, enabled, created_at, updated_at) VALUES
('99999999-9999-9999-9999-000000000000','admin@local',     '$2b$10$o5SIxvpEBWNBCj9TGWnH3.Xf.cCmEyHq/DU/UNQ6Ng6PJ4VNaNhOq','ADMIN',  true, now(), now()),
('22222222-2222-2222-2222-000000000001','student1@test',   '$2b$10$o5SIxvpEBWNBCj9TGWnH3.Xf.cCmEyHq/DU/UNQ6Ng6PJ4VNaNhOq','STUDENT',true, now(), now()),
('22222222-2222-2222-2222-000000000002','student2@test',   '$2b$10$o5SIxvpEBWNBCj9TGWnH3.Xf.cCmEyHq/DU/UNQ6Ng6PJ4VNaNhOq','STUDENT',true, now(), now()),
('22222222-2222-2222-2222-000000000003','student3@test',   '$2b$10$o5SIxvpEBWNBCj9TGWnH3.Xf.cCmEyHq/DU/UNQ6Ng6PJ4VNaNhOq','STUDENT',true, now(), now()),
('22222222-2222-2222-2222-000000000004','student4@test',   '$2b$10$o5SIxvpEBWNBCj9TGWnH3.Xf.cCmEyHq/DU/UNQ6Ng6PJ4VNaNhOq','STUDENT',true, now(), now()),
('22222222-2222-2222-2222-000000000005','student5@test',   '$2b$10$o5SIxvpEBWNBCj9TGWnH3.Xf.cCmEyHq/DU/UNQ6Ng6PJ4VNaNhOq','STUDENT',true, now(), now()),
('22222222-2222-2222-2222-000000000006','student6@test',   '$2b$10$o5SIxvpEBWNBCj9TGWnH3.Xf.cCmEyHq/DU/UNQ6Ng6PJ4VNaNhOq','STUDENT',true, now(), now()),
('22222222-2222-2222-2222-000000000007','student7@test',   '$2b$10$o5SIxvpEBWNBCj9TGWnH3.Xf.cCmEyHq/DU/UNQ6Ng6PJ4VNaNhOq','STUDENT',true, now(), now()),
('22222222-2222-2222-2222-000000000008','student8@test',   '$2b$10$o5SIxvpEBWNBCj9TGWnH3.Xf.cCmEyHq/DU/UNQ6Ng6PJ4VNaNhOq','STUDENT',true, now(), now()),
('22222222-2222-2222-2222-000000000009','student9@test',   '$2b$10$o5SIxvpEBWNBCj9TGWnH3.Xf.cCmEyHq/DU/UNQ6Ng6PJ4VNaNhOq','STUDENT',true, now(), now()),
('22222222-2222-2222-2222-00000000000a','student10@test',  '$2b$10$o5SIxvpEBWNBCj9TGWnH3.Xf.cCmEyHq/DU/UNQ6Ng6PJ4VNaNhOq','STUDENT',true, now(), now())
ON CONFLICT (email) DO NOTHING;

-- Admin profile
INSERT INTO admin_profile (user_id, display_name, version, created_at, updated_at) VALUES
('99999999-9999-9999-9999-000000000000','Local Admin', 0, now(), now())
ON CONFLICT (user_id) DO NOTHING;

-- Student profiles
INSERT INTO student_profile (user_id, full_name, preferred_role, country, city, remote_preference, version, created_at, updated_at) VALUES
('22222222-2222-2222-2222-000000000001','Alex Tan',     'Java Backend Engineer',      'MY','Kuala Lumpur','ANY',0,now(),now()),
('22222222-2222-2222-2222-000000000002','Bella Lim',    'Frontend Developer',         'MY','Petaling Jaya','ANY',0,now(),now()),
('22222222-2222-2222-2222-000000000003','Chen Wei',     'DevOps Engineer',            'SG','Singapore','ANY',0,now(),now()),
('22222222-2222-2222-2222-000000000004','Divya Rao',    'Data Scientist',             'MY','Kuala Lumpur','ANY',0,now(),now()),
('22222222-2222-2222-2222-000000000005','Ethan Wong',   'Mobile Developer',           'MY','Penang','ANY',0,now(),now()),
('22222222-2222-2222-2222-000000000006','Farah Aziz',   'Full Stack Developer',       'SG','Singapore','ANY',0,now(),now()),
('22222222-2222-2222-2222-000000000007','Gabriel Cruz', 'Security Engineer',          'GB','London','REMOTE',0,now(),now()),
('22222222-2222-2222-2222-000000000008','Hana Suzuki',  'QA Engineer',                'MY','Cyberjaya','ONSITE',0,now(),now()),
('22222222-2222-2222-2222-000000000009','Ivy Park',     'UI/UX Designer',             'SG','Singapore','ANY',0,now(),now()),
('22222222-2222-2222-2222-00000000000a','Jordan Reed',  'Machine Learning Engineer',  'US','San Francisco','REMOTE',0,now(),now())
ON CONFLICT (user_id) DO NOTHING;

-- Per-student skills (all MANUAL source so the profiles read like hand-edited
-- portfolios). Mix of BEGINNER, INTERMEDIATE, ADVANCED proficiencies.

INSERT INTO student_skill (student_id, skill_id, proficiency, source, created_at, updated_at) VALUES
-- Student 1 — Java Backend Engineer (strong Java grad)
('22222222-2222-2222-2222-000000000001','esco/skill/java',                       'ADVANCED',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000001','esco/skill/spring-boot',                'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000001','esco/skill/postgresql',                 'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000001','esco/skill/sql',                        'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000001','esco/skill/rest-api',                   'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000001','esco/skill/git',                        'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000001','esco/skill/unit-testing',               'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000001','esco/skill/object-oriented-programming','ADVANCED',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000001','esco/skill/junit5',                     'INTERMEDIATE','MANUAL',now(),now()),

-- Student 2 — Frontend Developer (React-focused)
('22222222-2222-2222-2222-000000000002','esco/skill/javascript', 'ADVANCED',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000002','esco/skill/typescript', 'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000002','esco/skill/react',      'ADVANCED',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000002','esco/skill/html',       'ADVANCED',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000002','esco/skill/css',        'ADVANCED',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000002','esco/skill/git',        'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000002','esco/skill/tailwind',   'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000002','esco/skill/vite',       'BEGINNER',    'MANUAL',now(),now()),

-- Student 3 — DevOps Engineer (cloud-leaning intern)
('22222222-2222-2222-2222-000000000003','esco/skill/linux',           'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000003','esco/skill/docker',          'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000003','esco/skill/aws',             'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000003','esco/skill/aws-s3',          'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000003','esco/skill/aws-ec2',         'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000003','esco/skill/shell-scripting', 'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000003','esco/skill/git',             'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000003','esco/skill/kubernetes',      'BEGINNER',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000003','esco/skill/terraform',       'BEGINNER',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000003','esco/skill/ci-cd',           'BEGINNER',    'MANUAL',now(),now()),

-- Student 4 — Data Scientist (ML-curious grad)
('22222222-2222-2222-2222-000000000004','esco/skill/python',           'ADVANCED',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000004','esco/skill/pandas',           'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000004','esco/skill/numpy',            'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000004','esco/skill/data-analysis',    'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000004','esco/skill/machine-learning', 'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000004','esco/skill/scikit-learn',     'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000004','esco/skill/sql',              'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000004','esco/skill/git',              'BEGINNER',    'MANUAL',now(),now()),

-- Student 5 — Mobile Developer (cross-platform)
('22222222-2222-2222-2222-000000000005','esco/skill/kotlin',              'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000005','esco/skill/swift',               'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000005','esco/skill/android-development', 'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000005','esco/skill/ios-development',     'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000005','esco/skill/react-native',        'BEGINNER',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000005','esco/skill/dart',                'BEGINNER',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000005','esco/skill/flutter',             'BEGINNER',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000005','esco/skill/git',                 'INTERMEDIATE','MANUAL',now(),now()),

-- Student 6 — Full Stack Developer (balanced web dev)
('22222222-2222-2222-2222-000000000006','esco/skill/javascript', 'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000006','esco/skill/typescript', 'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000006','esco/skill/react',      'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000006','esco/skill/nodejs',     'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000006','esco/skill/express',    'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000006','esco/skill/postgresql', 'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000006','esco/skill/git',        'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000006','esco/skill/rest-api',   'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000006','esco/skill/docker',     'BEGINNER',    'MANUAL',now(),now()),

-- Student 7 — Security Engineer (security-focused)
('22222222-2222-2222-2222-000000000007','esco/skill/python',         'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000007','esco/skill/linux',          'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000007','esco/skill/cryptography',   'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000007','esco/skill/web-security',   'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000007','esco/skill/authentication', 'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000007','esco/skill/oauth2',         'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000007','esco/skill/jwt',            'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000007','esco/skill/git',            'INTERMEDIATE','MANUAL',now(),now()),

-- Student 8 — QA Engineer (testing specialist)
('22222222-2222-2222-2222-000000000008','esco/skill/java',                     'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000008','esco/skill/selenium',                 'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000008','esco/skill/unit-testing',             'ADVANCED',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000008','esco/skill/integration-testing',      'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000008','esco/skill/test-driven-development',  'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000008','esco/skill/junit5',                   'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000008','esco/skill/mockito',                  'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000008','esco/skill/git',                      'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000008','esco/skill/cypress',                  'BEGINNER',    'MANUAL',now(),now()),

-- Student 9 — UI/UX Designer/Engineer (design-leaning)
('22222222-2222-2222-2222-000000000009','esco/skill/figma',           'ADVANCED',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000009','esco/skill/html',            'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000009','esco/skill/css',             'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000009','esco/skill/accessibility',   'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000009','esco/skill/user-research',   'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000009','esco/skill/tailwind',        'BEGINNER',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000009','esco/skill/javascript',      'BEGINNER',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-000000000009','esco/skill/git',             'BEGINNER',    'MANUAL',now(),now()),

-- Student 10 — ML Engineer (deep-learning practitioner)
('22222222-2222-2222-2222-00000000000a','esco/skill/python',              'ADVANCED',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-00000000000a','esco/skill/tensorflow',          'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-00000000000a','esco/skill/pytorch',             'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-00000000000a','esco/skill/deep-learning',       'ADVANCED',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-00000000000a','esco/skill/machine-learning',    'ADVANCED',    'MANUAL',now(),now()),
('22222222-2222-2222-2222-00000000000a','esco/skill/mlops',               'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-00000000000a','esco/skill/distributed-systems', 'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-00000000000a','esco/skill/huggingface',         'INTERMEDIATE','MANUAL',now(),now()),
('22222222-2222-2222-2222-00000000000a','esco/skill/llm-engineering',     'INTERMEDIATE','MANUAL',now(),now())
ON CONFLICT (student_id, skill_id) DO NOTHING;

COMMIT;

-- Sanity-check queries:
-- SELECT COUNT(*) FROM users WHERE role = 'STUDENT';     -- expected: 10
-- SELECT COUNT(*) FROM users WHERE role = 'ADMIN';       -- expected: 1
-- SELECT COUNT(*) FROM student_profile;                  -- expected: 10
-- SELECT COUNT(*) FROM student_skill;                    -- expected: ~85
