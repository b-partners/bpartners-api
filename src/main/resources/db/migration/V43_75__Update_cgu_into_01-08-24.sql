insert into "legal_file"
(id, "name", "file_url", to_be_confirmed)
values ('a426e84b-6a51-4e25-9b20-4202e92f9cbc', 'cgu_01-08-24.pdf',
        'https://legal.birdia.fr/cgu_01-08-24.pdf', true);

update "legal_file"
set to_be_confirmed = false
where "name" <> 'cgu_01-08-24.pdf'