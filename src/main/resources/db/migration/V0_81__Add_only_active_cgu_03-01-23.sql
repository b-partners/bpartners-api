insert into "legal_file"
    (id, "name", "file_url", to_be_confirmed)
values ('a56627a8-2493-4506-8443-65afdcd9773b', 'cgu_03-01-23.pdf',
        'https://legal.bpartners.app/cgu_03-01-23.pdf', true);

update "legal_file"
set to_be_confirmed = false
where "name" <> 'cgu_03-01-23.pdf'