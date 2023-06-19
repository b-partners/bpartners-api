drop function insert_account_info_in_temp_table();

create or replace function insert_user_info_into_temp_table()
    returns void
as
$$
BEGIN
    insert into annual_billing_info (user_id, first_name, last_name, email)
    select distinct pr.id_user, u.first_name, u.last_name, u.email
    from payment_request pr
             inner join public."user" u on u.id = pr.id_user;
end ;
$$
    language plpgsql;