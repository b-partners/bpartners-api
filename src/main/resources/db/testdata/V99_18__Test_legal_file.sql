insert into "legal_file"
    (id, "name", "file_url")
values ('legal_file1_id', 'CGU-November-2022-version-1',
        'https://s3.eu-west-3.amazonaws.com/legal.bpartners.app/cgu.pdf'),
       ('legal_file2_id', 'CGU-November-2022-version-2', 'https://s3.eu-west-3.amazonaws' ||
                                                         '.com/legal.bpartners.app/cgu.pdf'),
       ('legal_file3_id', 'CGU-November-2022-version-3', 'https://s3.eu-west-3.amazonaws' ||
                                                         '.com/legal.bpartners.app/cgu.pdf'),
       ('legal_file4_id', 'CGU-November-2022-version-4', 'https://s3.eu-west-3.amazonaws' ||
                                                         '.com/legal.bpartners.app/cgu.pdf');