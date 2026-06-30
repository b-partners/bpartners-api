create table if not exists user_stripe_customer_email_correspondence
(
    id                 varchar default uuid_generate_v4() primary key,
    user_id            varchar      not null,
    stripe_customer_id varchar(255) not null,
    email              varchar(255) not null,
    foreign key (user_id) references "user" (id)
);
create index if not exists user_stripe_customer_email_correspondence_user_id_idx on user_stripe_customer_email_correspondence (user_id);