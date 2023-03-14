alter table transaction
    alter column amount type varchar using
        case when amount is null then '0/1'else (amount * 100) || '/1' end;