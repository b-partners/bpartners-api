insert into "customer"
(id, id_user, first_name, last_name, email, phone, website, address, zip_code, city, country,
 comment)
values ('customer1_id', 'joe_doe_id', 'Luc', 'Artisan', 'bpartners.artisans@gmail.com',
        '+33 12 34 56 78', 'https://luc.website.com', '15 rue Porte d''Orange', 95160,
        'Metz', null, 'Rencontre avec Luc'),
       ('customer2_id', 'joe_doe_id', 'Jean',
        'Plombier', null,
        '+33 12 34 56 78', 'https://jean.website.com', '4 Avenue des Près', 95160,
        'Montmorency', 'France', 'Rencontre avec le plombier'),
       ('customer3_id', 'joe_doe_id', 'Marc',
        ' Montagnier', 'marc@email' ||
                       '.com',
        '+33 12 34 15 79', 'https://marc.website.com', '4 Avenue des Près', 95160,
        'Montmorency', 'France', 'Nouvelle rencontre'),
       ('customer4_id', 'joe_doe_id', 'Jean Olivier',
        ' LeBlanc', 'olivier.jean@email' ||
                    '.com',
        '+33 12 34 58 79', 'https://olivier.website.com', '4 Avenue des Près', 95160,
        'Berlin', 'Allemagne', 'Rencontre à Berlin'),
       ('customer5_id', 'jane_doe_id', 'Frank', 'Dubois', 'frank@email.com',
        '+33 12 34 53 78', 'https://frank.website.com', '5 Avenue des Près', 95160,
        'Montmorency', 'France', 'Belle rencontre')