do
$$
begin
        if not exists(select from pg_type where typname = 'invoice_export_output_format') then
create type invoice_export_output_format
    as enum ('ZIP');
end if;
end
$$;

alter table invoice_export_request
    add column if not exists output_format invoice_export_output_format;
