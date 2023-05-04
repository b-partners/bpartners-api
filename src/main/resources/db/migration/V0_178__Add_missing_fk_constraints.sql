alter table "account_holder"
    add constraint ac_holder_user_fk foreign key (id_user) references "user" (id);

alter table "account"
    add constraint ac_holder_fk foreign key (id_account_holder) references "account_holder" (id);