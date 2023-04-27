create or replace function get_invoicing_info(date_from varchar, date_to varchar)
    returns table
            (
                account_id     varchar,
                name           varchar,
                iban           varchar,
                bic            varchar,
                amount_value   numeric,
                average        numeric,
                median         double precision,
                minimum_amount numeric,
                maximum_amount numeric
            )
as
$$
begin
    return query
        select pr.account_id               as account_id,
               a.name                      as name,
               a.iban                      as iban,
               a.bic                       as bic,
               (sum((cast(split_part(pr.amount, '/', 1) as numeric)
                   / cast(split_part(pr.amount, '/', 2) as
                         numeric))) / 100) as amount_value,
               (avg((cast(split_part(pr.amount, '/', 1) as numeric)
                   / cast(split_part(pr.amount, '/', 2) as
                         numeric))) / 100) as average,
               percentile_cont(0.5) WITHIN GROUP ( ORDER BY ((cast(split_part(pr.amount, '/', 1) as numeric)
                   / cast(split_part(pr.amount, '/', 2) as numeric))) / 100 )
                                           as median,
               min((cast(split_part(amount, '/', 1) as numeric)
                   / cast(split_part(amount, '/', 2) as numeric)) / 100)
                                           as minimum_amount,
               max((cast(split_part(amount, '/', 1) as numeric) / cast(split_part(amount, '/', 2) as numeric)) / 100)
                                           as maximum_amount
        from payment_request pr
                 inner join account a on a.id = pr.account_id
        where pr.status = 'PAID'
          and pr.created_datetime between to_timestamp(date_from, 'YYYY-MM-DD HH:MI:SS') and to_timestamp(date_to, 'YYYY-MM-DD HH:MI:SS')
        group by pr.account_id, a.name, a.bic, a.iban;
end ;
$$
    language plpgsql;

create or replace function get_invoicing_info_by_year_and_month(year integer, month integer)
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