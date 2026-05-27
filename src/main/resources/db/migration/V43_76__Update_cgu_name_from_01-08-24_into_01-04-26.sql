UPDATE "legal_file"
SET "name"          = 'cgu_01-04-26.pdf',
    to_be_confirmed = true,
    file_url        = 'https://legal.birdia.fr/cgu_01-04-26.pdf'
WHERE "name" = 'cgu_01-08-24.pdf'
  AND (
    "name" IS DISTINCT FROM 'cgu_01-04-26.pdf'
        OR to_be_confirmed IS DISTINCT FROM true
        OR file_url IS DISTINCT FROM 'https://legal.birdia.fr/cgu_01-04-26.pdf'
    );

UPDATE "legal_file"
SET to_be_confirmed = false
WHERE "name" <> 'cgu_01-04-26.pdf'
  AND to_be_confirmed IS DISTINCT FROM false;