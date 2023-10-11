insert into "legal_file"
    (id, "name", "file_url", to_be_confirmed)
values ('9214432d-7b7a-4fa2-9660-d00d4e0109a0', 'cgu_09-10-23.pdf', 'https://legal.bpartners.app/cgu_09-10-23.pdf',
        true);

update "legal_file"
set to_be_confirmed = false
where "name" <> 'cgu_09-10-23.pdf';