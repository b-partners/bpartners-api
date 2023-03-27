create index if not exists "transaction_swan_index" on "transaction" (id_swan);
create index if not exists "transaction_bridge_index" on "transaction" (id_bridge);
create index if not exists "transaction_account_index" on "transaction" (id_account);