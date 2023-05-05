create or replace function get_billing_info_by_year_and_month(year integer, month integer)
    returns table
            (
                account_id   varchar,
                name         varchar,
                iban         varchar,
                bic          varchar,
                amount_value numeric
            )
as
$$
BEGIN
    return query
        select pr.account_id,
               a.name,
               a.iban,
               a.bic,
               (sum((cast(split_part(pr.amount, '/', 1) as numeric)
                   / cast(split_part(pr.amount, '/', 2) as
                         numeric))) / 100) as amount_value
        from (select * from payment_request where status = 'PAID' and extract(year from created_datetime) = year)
                 as pr
                 inner join account a on a.id = pr.account_id
        where extract(month from pr.created_datetime) = month
        group by pr.account_id, a.name, a.iban, a.bic;
end;
$$
    language plpgsql;