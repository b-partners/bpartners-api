create or replace function check_updated_datetime()
    returns trigger as $check_updated_datetime$
    begin
        if new.updated_at != (select updated_at from "invoice" where id = new.id)
            then raise exception 'There are differences between updated datetime';
        else
            raise notice 'Update successfully done';
        end if;
        return new;
    end;
    $check_updated_datetime$ language 'plpgsql';

create trigger check_updated_datetime
    before insert or update on "invoice"
        for each row execute procedure check_updated_datetime();