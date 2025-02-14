alter table "user"
    add column if not exists parent_user_id varchar references "user"(id);