do
$$
    begin
        if not exists(select from pg_type where typname = 'transaction_type') then
            create type "transaction_type" as enum ('INCOME','OUTCOME');
        end if;
    end
$$;
create table if not exists "transaction"
(
    id varchar
        constraint transaction_pk primary key default uuid_generate_v4(),
    swan_id varchar unique not null,
    type transaction_type
);