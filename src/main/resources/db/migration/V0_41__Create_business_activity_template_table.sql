create table if not exists "business_activity_template"
(
    id   varchar
        constraint business_activity_template_pk primary key default uuid_generate_v4(),
    name varchar unique
);