alter table "legal_file"
    add constraint legal_file_unique_url unique (file_url);
alter table "legal_file"
    add column created_datetime timestamp default current_timestamp;