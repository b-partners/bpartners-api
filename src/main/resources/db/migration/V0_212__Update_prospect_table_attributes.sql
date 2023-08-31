do
$$
    begin
        if not exists(select from pg_type where typname = 'prospect_feedback') then
            create type prospect_feedback as enum (
                'NOT_INTERESTED', 'INTERESTED', 'PROPOSAL_SENT',
                'PROPOSAL_ACCEPTED', 'PROPOSAL_DECLINED', 'INVOICE_SENT');
        end if;
    end
$$;

alter table "prospect" add column if not exists new_name varchar;
alter table "prospect" add column if not exists new_email varchar;
alter table "prospect" add column if not exists new_phone varchar;
alter table "prospect" add column if not exists new_address varchar;
alter table "prospect" add column if not exists comment varchar;
alter table "prospect" add column if not exists contract_amount varchar;
alter table "prospect" add column if not exists id_invoice varchar;
alter table "prospect" add column if not exists prospect_feedback prospect_feedback;

alter table "prospect" add constraint invoice_prospect_fk foreign key (id_invoice) references "invoice"(id);
alter table "prospect" alter column id_account_holder drop not null ;