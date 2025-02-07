create table if not exists "user_link" (
    id serial primary key,
    user_id varchar not null references "user"(id),
    linked_user_id varchar not null references "user"(id),
    unique(user_id, linked_user_id)
);