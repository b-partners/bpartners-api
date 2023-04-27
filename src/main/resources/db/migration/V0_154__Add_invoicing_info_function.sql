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

create or replace function get_annual_invoicing_info(year integer)
    returns table
            (
                account_id varchar,
                name       varchar,
                bic        varchar,
                iban       varchar,
                january    numeric,
                february   numeric,
                march      numeric,
                april      numeric,
                may        numeric,
                june       numeric,
                july       numeric,
                august     numeric,
                september  numeric,
                october    numeric,
                november   numeric,
                december   numeric
            )
as
$$
DECLARE
    rec RECORD;
BEGIN
    create temporary table annual_invoicing_info
    (
        account_id varchar,
        name       varchar,
        bic        varchar,
        iban       varchar,
        january    numeric default 0,
        february   numeric default 0,
        march      numeric default 0,
        april      numeric default 0,
        may        numeric default 0,
        june       numeric default 0,
        july       numeric default 0,
        august     numeric default 0,
        september  numeric default 0,
        october    numeric default 0,
        november   numeric default 0,
        december   numeric default 0
    );
    insert into annual_invoicing_info (account_id, name, bic, iban)
    select distinct pr.account_id, a.name, a.bic, a.iban
    from payment_request pr
             inner join account a on a.id = pr.account_id;
    FOR rec in select * from get_invoicing_info_by_year_and_month(year, 1)
        LOOP
            UPDATE annual_invoicing_info as a
            set january =
                    case
                        when rec.account_id is not null then rec.amount_value
                        else a.january end
            where a.account_id = rec.account_id;
        end loop;
    FOR rec in select * from get_invoicing_info_by_year_and_month(year, 2)
        LOOP
            UPDATE annual_invoicing_info as a
            set february =
                    case
                        when rec.account_id is not null then rec.amount_value
                        else a.february end
            where a.account_id = rec.account_id;
        end loop;
    FOR rec in select * from get_invoicing_info_by_year_and_month(year, 3)
        LOOP
            UPDATE annual_invoicing_info as a
            set march =
                    case
                        when rec.account_id is not null then rec.amount_value
                        else a.march end
            where a.account_id = rec.account_id;
        end loop;
    FOR rec in select * from get_invoicing_info_by_year_and_month(year, 4)
        LOOP
            UPDATE annual_invoicing_info as a
            set april =
                    case
                        when rec.account_id is not null then rec.amount_value
                        else a.april end
            where a.account_id = rec.account_id;
        end loop;
    FOR rec in select * from get_invoicing_info_by_year_and_month(year, 5)
        LOOP
            UPDATE annual_invoicing_info as a
            set may =
                    case
                        when rec.account_id is not null then rec.amount_value
                        else a.may end
            where a.account_id = rec.account_id;
        end loop;
    FOR rec in select * from get_invoicing_info_by_year_and_month(year, 6)
        LOOP
            UPDATE annual_invoicing_info as a
            set june =
                    case
                        when rec.account_id is not null then rec.amount_value
                        else a.june end
            where a.account_id = rec.account_id;
        end loop;
    FOR rec in select * from get_invoicing_info_by_year_and_month(year, 7)
        LOOP
            UPDATE annual_invoicing_info as a
            set july =
                    case
                        when rec.account_id is not null then rec.amount_value
                        else a.july end
            where a.account_id = rec.account_id;
        end loop;
    FOR rec in select * from get_invoicing_info_by_year_and_month(year, 8)
        LOOP
            UPDATE annual_invoicing_info as a
            set august =
                    case
                        when rec.account_id is not null then rec.amount_value
                        else a.august end
            where a.account_id = rec.account_id;
        end loop;
    FOR rec in select * from get_invoicing_info_by_year_and_month(year, 9)
        LOOP
            UPDATE annual_invoicing_info as a
            set september =
                    case
                        when rec.account_id is not null then rec.amount_value
                        else a.september end
            where a.account_id = rec.account_id;
        end loop;
    FOR rec in select * from get_invoicing_info_by_year_and_month(year, 10)
        LOOP
            UPDATE annual_invoicing_info as a
            set october =
                    case
                        when rec.account_id is not null then rec.amount_value
                        else a.october end
            where a.account_id = rec.account_id;
        end loop;
    FOR rec in select * from get_invoicing_info_by_year_and_month(year, 11)
        LOOP
            UPDATE annual_invoicing_info as a
            set november =
                    case
                        when rec.account_id is not null then rec.amount_value
                        else a.november end
            where a.account_id = rec.account_id;
        end loop;
    FOR rec in select * from get_invoicing_info_by_year_and_month(year, 12)
        LOOP
            UPDATE annual_invoicing_info as a
            set december =
                    case
                        when rec.account_id is not null then rec.amount_value
                        else a.december end
            where a.account_id = rec.account_id;
        end loop;
    return query select * from annual_invoicing_info;
    drop table annual_invoicing_info;
end;
$$
    language plpgsql;