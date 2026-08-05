update "subscription_product"
set price_in_cents_without_vat=0,
    overage_unit_price_in_cents=1000,
    features = '[
      "Paiement à l''analyse, sans engagement",
      "Analyse IA toiture complète",
      "Export PDF + emprise GeoJSON",
      "Assistance par courriel"
    ]'
where id = '4219611e-7584-4636-a3c5-ba212600715b';

update "subscription_product"
set annual_discount_percent=1000,
    price_in_cents_without_vat=4900,
    trial_period_days=7,
    name = 'Essentiel',
    features = '[
      "10 analyses incluses / mois",
      "5 € HT / analyse supplémentaire",
      "Module de génération de prospects sur votre site",
      "Communauté BIRDIA (chantiers locaux)",
      "Assistance 7j/7 par courriel"
    ]'
where id = '89f1acdd-c3b9-4717-a21d-355b2021ad58';

update "subscription_product"
set annual_discount_percent=1000,
    price_in_cents_without_vat=9900,
    features = '[
      "25 analyses incluses / mois",
      "4 € HT / analyse supplémentaire",
      "3 utilisateurs inclus",
      "Marque blanche / cobranding",
      "Passerelles vers votre GRC (HubSpot, Pipedrive...)",
      "Module de génération de devis"
    ]'
where id = 'c5f57306-a7b1-43f4-90fc-204ccd4c0ce2';

update "subscription_product"
set annual_discount_percent=1000,
    price_in_cents_without_vat=19900,
    features = '[
      "60 analyses incluses / mois",
      "3 € HT / analyse supplémentaire",
      "Utilisateurs illimités",
      "Accès API & notifications automatiques",
      "Suivi annuel (nouvelle passe automatique)",
      "Assistance dédiée 4 h ouvrées"
    ]'
where id = '37b9639e-d058-4222-8a2a-d78d5fe7b6b1';
