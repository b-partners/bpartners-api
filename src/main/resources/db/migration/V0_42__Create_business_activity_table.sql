create table if not exists "business_activity"
(
    id                       varchar
        constraint business_activity_pk primary key default uuid_generate_v4(),
    primary_activity         varchar unique,
    constraint business_activity_template_fk1 foreign key (primary_activity)
        references "business_activity_template"(name),
    secondary_activity varchar,
    constraint business_activity_template_fk2 foreign key (primary_activity)
        references "business_activity_template"(name),
    other_primary_activity   varchar,
    other_secondary_activity varchar,
    account_holder_id varchar not null unique,
    constraint business_activity_holder_fk foreign key (account_holder_id)
    references "account_holder"(id)
);