UPDATE subscription_product
SET name                                   = 'Pro',
    description                            = 'Pour les PME en croissance et les équipes multi-utilisateurs.',
    features                               = '[
      "25 analyses incluses / mois",
      "4 € HT / analyse supplémentaire",
      "3 utilisateurs inclus",
      "Marque blanche / cobranding",
      "Passerelles vers votre GRC (HubSpot, Pipedrive...)",
      "Module de génération de devis"
    ]',
    image_url                              = null,
    type                                   = 'MONTHLY',
    consumption_type_attached              = null,
    plan_code                              = 'PRO',
    billing_type                           = 'COMMITMENT',
    free_usage_threshold                   = 25,
    overage_unit_price_in_cents            = 400,
    trial_period_days                      = 0,
    annual_discount_percent                = 1000,
    vat_percent                            = 2000,
    price_in_cents_without_vat             = 9900,
    most_chosen                            = true,
    deprecated                             = false,
    display_position                       = 3,
    annual_price_in_cents_with_vat         = 128280,
    credit_unit_price_in_cents_without_vat = 400,
    credit_cost_per_analysis               = 1,
    included_credits_per_billing_period    = 25
WHERE id = 'c5f57306-a7b1-43f4-90fc-204ccd4c0ce2';

UPDATE subscription_product
SET name                                   = 'À l''usage',
    description                            = null,
    features                               = '[
      "Paiement à l''analyse, sans engagement",
      "Analyse IA toiture complète",
      "Export PDF + emprise GeoJSON",
      "Assistance par courriel"
    ]',
    image_url                              = null,
    type                                   = 'MONTHLY',
    consumption_type_attached              = null,
    plan_code                              = 'USAGE',
    billing_type                           = 'USAGE_BASED',
    free_usage_threshold                   = 0,
    overage_unit_price_in_cents            = 1000,
    trial_period_days                      = 0,
    annual_discount_percent                = null,
    vat_percent                            = 2000,
    price_in_cents_without_vat             = 0,
    most_chosen                            = false,
    deprecated                             = false,
    display_position                       = 1,
    annual_e2_price_id                     = null,
    annual_price_in_cents_with_vat         = null,
    credit_unit_price_in_cents_without_vat = 1000,
    credit_cost_per_analysis               = 1,
    included_credits_per_billing_period    = 0
WHERE id = '4219611e-7584-4636-a3c5-ba212600715b';


UPDATE subscription_product
SET name                                   = 'Expert',
    description                            = 'Pour les multi-agences et les intégrations par API.',
    features                               = '[
      "60 analyses incluses / mois",
      "3 € HT / analyse supplémentaire",
      "Utilisateurs illimités",
      "Accès API & notifications automatiques",
      "Suivi annuel (nouvelle passe automatique)",
      "Assistance dédiée 4 h ouvrées"
    ]',
    image_url                              = null,
    type                                   = 'MONTHLY',
    consumption_type_attached              = null,
    plan_code                              = 'EXPERT',
    billing_type                           = 'COMMITMENT',
    free_usage_threshold                   = 60,
    overage_unit_price_in_cents            = 300,
    trial_period_days                      = 0,
    annual_discount_percent                = 1000,
    vat_percent                            = 2000,
    price_in_cents_without_vat             = 19900,
    most_chosen                            = false,
    deprecated                             = false,
    display_position                       = 4,
    annual_price_in_cents_with_vat         = 257880,
    credit_unit_price_in_cents_without_vat = 300,
    credit_cost_per_analysis               = 1,
    included_credits_per_billing_period    = 60
WHERE id = '37b9639e-d058-4222-8a2a-d78d5fe7b6b1';

UPDATE subscription_product
SET name                                   = 'Essentiel',
    description                            = 'Le pack tout-en-un pour les artisans et TPE.',
    features                               = '[
      "10 analyses incluses / mois",
      "5 € HT / analyse supplémentaire",
      "Module de génération de prospects sur votre site",
      "Communauté BIRDIA (chantiers locaux)",
      "Assistance 7j/7 par courriel"
    ]',
    image_url                              = null,
    type                                   = 'MONTHLY',
    consumption_type_attached              = null,
    plan_code                              = 'ESSENTIAL_V2',
    billing_type                           = 'COMMITMENT',
    free_usage_threshold                   = 10,
    overage_unit_price_in_cents            = 500,
    trial_period_days                      = 7,
    annual_discount_percent                = 1000,
    vat_percent                            = 2000,
    price_in_cents_without_vat             = 4900,
    most_chosen                            = false,
    deprecated                             = false,
    display_position                       = 2,
    annual_price_in_cents_with_vat         = 63480,
    credit_unit_price_in_cents_without_vat = 500,
    credit_cost_per_analysis               = 1,
    included_credits_per_billing_period    = 10
WHERE id = '89f1acdd-c3b9-4717-a21d-355b2021ad58';
