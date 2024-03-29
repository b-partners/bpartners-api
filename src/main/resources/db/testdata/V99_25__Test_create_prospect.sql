insert into prospect (id, old_name, old_phone, old_email, old_address, id_account_holder, town_code, rating,
                      last_evaluation_date)
values ('prospect1_id', 'John doe', null, null, null,
        'b33e6eb0-e262-4596-a91f-20c6a7bfd343', 92002, 9.993, '2023-01-01T00:00:00.00Z'),
       ('prospect2_id', 'jane doe', '+261340465339', 'janeDoe@gmail.com', '30 Rue de la Montagne Sainte-Genevieve',
        'b33e6eb0-e262-4596-a91f-20c6a7bfd343', 92002, -1, null),
       ('prospect3_id', 'markus adams', '+261340465340', 'markusAdams@gmail.com',
        '30 Rue de la Montagne Sainte-Genevieve',
        'b33e6eb0-e262-4596-a91f-20c6a7bfd343', 92001, 0, '2023-01-01T00:00:00.00Z'),
       ('prospect4_id', 'Alyssa Hain', '+261340465341', 'alyssaHain@gmail.com',
        '30 Rue de la Montagne Sainte-Genevieve',
        'b33e6eb0-e262-4596-a91f-20c6a7bfd343', null, -1, null),
       ('prospect5_id', 'Michele Klaffs', '+261340465342', 'micheleKlaffs@gmail.com',
        '30 Rue de la Montagne Sainte-Genevieve',
        'b33e6eb0-e262-4596-a91f-20c6a7bfd343', null, -1, null),
       ('prospect6_id', 'Timmie	Accombe', '+261340465343', 'timmieAccombe@gmail.com',
        '30 Rue de la Montagne Sainte-Genevieve',
        'b33e6eb0-e262-4596-a91f-20c6a7bfd343', null, -1, null),
       ('prospect7_id', 'Killy	Waddilove', '+261340465344', 'killyWaddilove@gmail.com',
        '30 Rue de la Montagne Sainte-Genevieve',
        'b33e6eb0-e262-4596-a91f-20c6a7bfd343', null, -1, null);

insert into "prospect_status_history"(id, id_prospect, status, updated_at)
values ('prospect_status1_id', 'prospect1_id', 'TO_CONTACT', '2023-01-01T00:00:00.00Z'),
       ('prospect_status2_id', 'prospect2_id', 'TO_CONTACT', '2023-01-01T00:00:00.00Z'),
       ('prospect_status3_id', 'prospect3_id', 'TO_CONTACT', '2023-01-01T00:00:00.00Z'),
       ('prospect_status4_id', 'prospect4_id', 'CONTACTED', '2023-01-01T00:00:00.00Z'),
       ('prospect_status5_id', 'prospect5_id', 'CONTACTED', '2023-01-01T00:00:00.00Z'),
       ('prospect_status6_id', 'prospect6_id', 'CONVERTED', '2023-01-01T00:00:00.00Z'),
       ('prospect_status7_id', 'prospect7_id', 'CONVERTED', '2023-01-01T00:00:00.00Z')
;