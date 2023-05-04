-- Product template
alter table "product_template"
    add column if not exists "id_user" varchar;

update "product_template"
set id_user = u_id
from (select u.id as u_id, a.id as a_id
      from "user" u
               join account a on u.id = a.id_user
               join product_template p on p.id_account = a.id) u_a
where u_a.a_id = "product_template".id_account;

alter table "product_template"
    drop constraint if exists product_template_user_fk;
alter table "product_template"
    add constraint product_template_user_fk
        foreign key (id_user) references "user" (id);

alter table "product_template"
    drop column if exists id_account;

-- Customer
alter table "customer"
    add column if not exists "id_user" varchar;

update "customer"
set id_user = u_id
from (select u.id as u_id, a.id as a_id
      from "user" u
               join account a on u.id = a.id_user
               join customer c on c.id_account = a.id) u_a
where u_a.a_id = "customer".id_account;

alter table "customer"
    drop constraint if exists customer_user_fk;
alter table "customer"
    add constraint customer_user_fk
        foreign key (id_user) references "user" (id);

alter table "customer"
    drop column if exists id_account;

-- Invoice
alter table "invoice"
    add column if not exists "id_user" varchar;

update "invoice"
set id_user = u_id
from (select u.id as u_id, a.id as a_id
      from "user" u
               join account a on u.id = a.id_user
               join invoice i on i.id_account = a.id) u_a
where u_a.a_id = "invoice".id_account;

alter table "invoice"
    drop constraint if exists invoice_user_fk;
alter table "invoice"
    add constraint invoice_user_fk
        foreign key (id_user) references "user" (id);

alter table "invoice"
    drop column if exists id_account;

-- File Info
alter table "file_info"
    add column if not exists "id_user" varchar;

update "file_info"
set id_user = u_id
from (select u.id as u_id, a.id as a_id
      from "user" u
               join account a on u.id = a.id_user
               join file_info f on f.account_id = a.id) u_a
where u_a.a_id = "file_info".account_id;

alter table "file_info"
    drop constraint if exists file_info_user_fk;
alter table "file_info"
    add constraint file_info_user_fk
        foreign key (id_user) references "user" (id);

alter table "file_info"
    drop column if exists account_id;

-- Invoice relaunch conf
alter table "account_invoice_relaunch_conf"
    rename to "user_invoice_relaunch_conf";

alter table user_invoice_relaunch_conf
    add column if not exists id_user varchar;

update user_invoice_relaunch_conf
set id_user = u_id
from (select u.id as u_id, a.id as a_id
      from "user" u
               join account a on u.id = a.id_user
               join user_invoice_relaunch_conf uirc on uirc.account_id = a.id) u_a
where u_a.a_id = user_invoice_relaunch_conf.account_id;

alter table user_invoice_relaunch_conf
    drop constraint if exists user_invoice_relaunch_conf_fk;
alter table user_invoice_relaunch_conf
    add constraint user_invoice_relaunch_conf_fk
        foreign key (id_user) references "user" (id);

alter table user_invoice_relaunch_conf
    drop column if exists account_id;

-- Payment request
alter table "payment_request"
    add column if not exists "id_user" varchar;

update "payment_request"
set id_user = u_id
from (select u.id as u_id, a.id as a_id
      from "user" u
               join account a on u.id = a.id_user
               join payment_request pr on pr.account_id = a.id) u_a
where u_a.a_id = "payment_request".account_id;

alter table "payment_request"
    drop constraint if exists payment_request_user_fk;
alter table "payment_request"
    add constraint payment_request_user_fk
        foreign key (id_user) references "user" (id);

alter table "payment_request"
    drop column if exists account_id;