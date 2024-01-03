create table if not exists "transaction_supporting_docs"
(
    id             varchar
        constraint transaction_supporting_docs_pk primary key not null default uuid_generate_v4
        (),
    id_file_info   varchar,
    id_transaction varchar,
    foreign key (id_file_info) references file_info (id),
    foreign key (id_transaction) references "transaction" (id)
)