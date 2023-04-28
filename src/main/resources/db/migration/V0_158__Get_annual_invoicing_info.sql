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
    perform create_annual_invoicing_info_table();
    perform insert_account_info_in_temp_table();
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
