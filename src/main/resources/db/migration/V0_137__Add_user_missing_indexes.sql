create index if not exists "user_email_index" on "user" (email);
create index if not exists "user_bridge_id_index" on "user" (bridge_user_id);
create index if not exists "user_swan_id_index" on "user" (swan_user_id);
create index if not exists "user_token_index" on "user" (access_token);