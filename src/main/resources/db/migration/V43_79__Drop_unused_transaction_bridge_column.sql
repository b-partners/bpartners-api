-- Legacy one-shot dedup views from V0_171 that still depend on transaction.id_bridge
drop view if exists min_duplicated_bridge_transactions;
drop view if exists duplicated_bridge_transactions_id;
drop view if exists min_bridge_transaction_id;

alter table "transaction"
    drop constraint if exists "unique_bridge_transaction_id";

drop index if exists "transaction_bridge_index";

alter table "transaction"
    drop column if exists id_bridge;
