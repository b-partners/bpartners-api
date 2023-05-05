create or replace function insert_account_info_in_temp_table()
    returns void
as
$$BEGIN
    insert into annual_billing_info (account_id, name, bic, iban)
    select distinct pr.account_id, a.name, a.bic, a.iban
    from payment_request pr
             inner join account a on a.id = pr.account_id;
end;
$$
    language plpgsql;