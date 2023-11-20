insert into "legal_file"
    (id, "name", "file_url", to_be_confirmed)
values ('05c96052-c2a1-4f1e-9e43-53ecd8642099', 'cgu_20-11-23.pdf',
        'https://legal.bpartners.app/cgu_20-11-23.pdf', true);

update "legal_file"
set to_be_confirmed = false
where "name" <> 'cgu_20-11-23.pdf'