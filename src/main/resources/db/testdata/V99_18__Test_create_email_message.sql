insert into "email_message"
    (id, message, id_account)
values ('default_message', '<p>Bonjour,</p>\n <p>\n Retrouvez-ci joint votre %s enregistré ' ||
                           'à la référence %s\n </p>\n', 'all_account'),
       ('message1_id', '<p>Bonjour Mr %s ,</p>\n <p>\n Retrouvez-ci joint votre %s enregistré ' ||
                           'à la référence %s\n </p>\n', 'beed1765-5c16-472a-b3f4-5c376ce5db58');