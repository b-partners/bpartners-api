alter table "business_activity"
    drop constraint business_activity_template_fk1,
    drop constraint business_activity_template_fk2;

delete
from "business_activity_template";

INSERT INTO business_activity_template(name)
VALUES ('Agenceur');
INSERT INTO business_activity_template(name)
VALUES ('Architecte');
INSERT INTO business_activity_template(name)
VALUES ('Architecte d''intérieur');
INSERT INTO business_activity_template(name)
VALUES ('Armurier');
INSERT INTO business_activity_template(name)
VALUES ('Artisan tout corps d''état');
INSERT INTO business_activity_template(name)
VALUES ('Barbier');
INSERT INTO business_activity_template(name)
VALUES ('Bottier');
INSERT INTO business_activity_template(name)
VALUES ('Boucher-charcutier');
INSERT INTO business_activity_template(name)
VALUES ('Boulanger');
INSERT INTO business_activity_template(name)
VALUES ('Brasseur');
INSERT INTO business_activity_template(name)
VALUES ('Carreleur');
INSERT INTO business_activity_template(name)
VALUES ('Céramiste');
INSERT INTO business_activity_template(name)
VALUES ('Chapiste');
INSERT INTO business_activity_template(name)
VALUES ('Charpentier');
INSERT INTO business_activity_template(name)
VALUES ('Chauffagiste');
INSERT INTO business_activity_template(name)
VALUES ('Chocolatier');
INSERT INTO business_activity_template(name)
VALUES ('Coiffeur/coiffeurse');
INSERT INTO business_activity_template(name)
VALUES ('Concepteur aménageur d''espaces intérieurs');
INSERT INTO business_activity_template(name)
VALUES ('Conducteur de travaux tout corps d''état');
INSERT INTO business_activity_template(name)
VALUES ('Confiseur');
INSERT INTO business_activity_template(name)
VALUES ('Constructeur Bois');
INSERT INTO business_activity_template(name)
VALUES ('Constructeur maison individuelle');
INSERT INTO business_activity_template(name)
VALUES ('Constructeur modulaire');
INSERT INTO business_activity_template(name)
VALUES ('Contractant travaux');
INSERT INTO business_activity_template(name)
VALUES ('Cordonnier');
INSERT INTO business_activity_template(name)
VALUES ('Coutelier');
INSERT INTO business_activity_template(name)
VALUES ('Couvreur');
INSERT INTO business_activity_template(name)
VALUES ('Cuisiniste');
INSERT INTO business_activity_template(name)
VALUES ('Décapeur');
INSERT INTO business_activity_template(name)
VALUES ('Décorateur');
INSERT INTO business_activity_template(name)
VALUES ('Décorateur ensemblier d''intérieur');
INSERT INTO business_activity_template(name)
VALUES ('Démolisseur');
INSERT INTO business_activity_template(name)
VALUES ('Dératiseur');
INSERT INTO business_activity_template(name)
VALUES ('Désamianteur');
INSERT INTO business_activity_template(name)
VALUES ('Designer d''intérieur');
INSERT INTO business_activity_template(name)
VALUES ('Designer textile');
INSERT INTO business_activity_template(name)
VALUES ('Dessinateur Projeteur');
INSERT INTO business_activity_template(name)
VALUES ('Diagnostiqueur humidité');
INSERT INTO business_activity_template(name)
VALUES ('Diagnostiqueur immobilier');
INSERT INTO business_activity_template(name)
VALUES ('Domotique');
INSERT INTO business_activity_template(name)
VALUES ('Ebeniste');
INSERT INTO business_activity_template(name)
VALUES ('Elagueur');
INSERT INTO business_activity_template(name)
VALUES ('Électricien');
INSERT INTO business_activity_template(name)
VALUES ('Electricien');
INSERT INTO business_activity_template(name)
VALUES ('Électronicien');
INSERT INTO business_activity_template(name)
VALUES ('Entreprise de rénovation et décoration');
INSERT INTO business_activity_template(name)
VALUES ('Entreprise générale du bâtiment');
INSERT INTO business_activity_template(name)
VALUES ('Etanchéiste');
INSERT INTO business_activity_template(name)
VALUES ('Etancheur');
INSERT INTO business_activity_template(name)
VALUES ('Façadier');
INSERT INTO business_activity_template(name)
VALUES ('Ferronnier');
INSERT INTO business_activity_template(name)
VALUES ('Fleuriste');
INSERT INTO business_activity_template(name)
VALUES ('Frigoriste');
INSERT INTO business_activity_template(name)
VALUES ('Fromager-Crémier');
INSERT INTO business_activity_template(name)
VALUES ('Gantier');
INSERT INTO business_activity_template(name)
VALUES ('Gemmogue');
INSERT INTO business_activity_template(name)
VALUES ('Génie civil');
INSERT INTO business_activity_template(name)
VALUES ('Géomètre');
INSERT INTO business_activity_template(name)
VALUES ('Glacier');
INSERT INTO business_activity_template(name)
VALUES ('Horloger');
INSERT INTO business_activity_template(name)
VALUES ('Installateur sanitaire');
INSERT INTO business_activity_template(name)
VALUES ('Jardinier');
INSERT INTO business_activity_template(name)
VALUES ('Jointeur');
INSERT INTO business_activity_template(name)
VALUES ('Luthier');
INSERT INTO business_activity_template(name)
VALUES ('Maçon');
INSERT INTO business_activity_template(name)
VALUES ('Maître d''œuvre');
INSERT INTO business_activity_template(name)
VALUES ('Marbrier');
INSERT INTO business_activity_template(name)
VALUES ('Maréchal Ferrant');
INSERT INTO business_activity_template(name)
VALUES ('Maroquinier');
INSERT INTO business_activity_template(name)
VALUES ('Menuisier');
INSERT INTO business_activity_template(name)
VALUES ('Métallier');
INSERT INTO business_activity_template(name)
VALUES ('Métallurgiste');
INSERT INTO business_activity_template(name)
VALUES ('Miroitier');
INSERT INTO business_activity_template(name)
VALUES ('Monteur d''échafaudages');
INSERT INTO business_activity_template(name)
VALUES ('Nettoyeur');
INSERT INTO business_activity_template(name)
VALUES ('Œnologue');
INSERT INTO business_activity_template(name)
VALUES ('Parfumeur');
INSERT INTO business_activity_template(name)
VALUES ('Parqueteur');
INSERT INTO business_activity_template(name)
VALUES ('Patissier');
INSERT INTO business_activity_template(name)
VALUES ('Paysagiste');
INSERT INTO business_activity_template(name)
VALUES ('Peintre');
INSERT INTO business_activity_template(name)
VALUES ('Peintre en bâtiment');
INSERT INTO business_activity_template(name)
VALUES ('Pisciniste');
INSERT INTO business_activity_template(name)
VALUES ('Plaquiste');
INSERT INTO business_activity_template(name)
VALUES ('Plasticien');
INSERT INTO business_activity_template(name)
VALUES ('Plâtrier');
INSERT INTO business_activity_template(name)
VALUES ('Plombier');
INSERT INTO business_activity_template(name)
VALUES ('Plombier-chauffagiste');
INSERT INTO business_activity_template(name)
VALUES ('Poissonnier');
INSERT INTO business_activity_template(name)
VALUES ('Ponceur');
INSERT INTO business_activity_template(name)
VALUES ('Ramoneur');
INSERT INTO business_activity_template(name)
VALUES ('Ravalement de façade');
INSERT INTO business_activity_template(name)
VALUES ('Rénovation d''intérieur');
INSERT INTO business_activity_template(name)
VALUES ('Restaurateur d''œuvres d''art');
INSERT INTO business_activity_template(name)
VALUES ('Revêtement de sol');
INSERT INTO business_activity_template(name)
VALUES ('Scieur-Carotteur de béton');
INSERT INTO business_activity_template(name)
VALUES ('Sellier');
INSERT INTO business_activity_template(name)
VALUES ('Sellier-harnacheur');
INSERT INTO business_activity_template(name)
VALUES ('Serrurier');
INSERT INTO business_activity_template(name)
VALUES ('Shaper');
INSERT INTO business_activity_template(name)
VALUES ('Staffeur');
INSERT INTO business_activity_template(name)
VALUES ('Staffeur - ornemaniste');
INSERT INTO business_activity_template(name)
VALUES ('Storiste');
INSERT INTO business_activity_template(name)
VALUES ('Tailleur de pierre');
INSERT INTO business_activity_template(name)
VALUES ('Tapissier');
INSERT INTO business_activity_template(name)
VALUES ('Taxidermiste');
INSERT INTO business_activity_template(name)
VALUES ('Technicien de curage');
INSERT INTO business_activity_template(name)
VALUES ('Technicien de sécurité');
INSERT INTO business_activity_template(name)
VALUES ('Technicien froid et climatisation');
INSERT INTO business_activity_template(name)
VALUES ('Technicien tout corps d''état');
INSERT INTO business_activity_template(name)
VALUES ('Terrassiers');
INSERT INTO business_activity_template(name)
VALUES ('Tonnelier');
INSERT INTO business_activity_template(name)
VALUES ('Tout corps d''état');
INSERT INTO business_activity_template(name)
VALUES ('Travaux d''aménagement urbain');
INSERT INTO business_activity_template(name)
VALUES ('Travaux d''isolation');
INSERT INTO business_activity_template(name)
VALUES ('Travaux publics');
INSERT INTO business_activity_template(name)
VALUES ('Vernisseur');
INSERT INTO business_activity_template(name)
VALUES ('Vitrailliste');
INSERT INTO business_activity_template(name)
VALUES ('Vitrier');
INSERT INTO business_activity_template(name)
VALUES ('Zingueur');
