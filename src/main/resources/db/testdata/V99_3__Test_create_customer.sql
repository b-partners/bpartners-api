insert into "customer"
(id, id_account, "name", email, phone, website, address, zip_code, city, country)
values ('customer1_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Luc Artisan', 'bpartners.artisans@gmail.com',
        '+33 12 34 56 78', 'https://luc.website.com', '15 rue Porte d''Orange', 95160,
        'Montmorency', 'France'),
       ('customer2_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Jean' ||
                                                                ' Plombier', 'jean@email' ||
                                                                             '.com',
        '+33 12 34 56 78', 'https://jean.website.com', '4 Avenue des Près', 95160,
        'Montmorency', 'France'),
        ('customer3_id', 'beed1765-5c16-472a-b3f4-5c376ce5db58', 'Marc' ||
                                                                ' Montagnier', 'marc@email' ||
                                                                             '.com',
        '+33 12 34 56 78', 'https://marc.website.com', '4 Avenue des Près', 95160,
        'Montmorency', 'France');