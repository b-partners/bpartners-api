update "subscription_product"
set features         = '[
  "Métrés 3D — surface, pente, périmètre",
  "Métrés détaillés — faîtage, rives, égouts, noues",
  "Maquette 3D des pans (visualisation)",
  "Export CAO / BIM (DXF, IFC)",
  "Export PDF + emprise GeoJSON",
  "Support email"
]',
    feature_sections = '[
      {
        "title": "Métrés inclus",
        "items": [
          {"text": "Métrés 3D — surface, pente, périmètre", "style": "HIGHLIGHTED"},
          {"text": "Métrés détaillés — faîtage, rives, égouts, noues", "style": "HIGHLIGHTED"},
          {"text": "Maquette 3D des pans (visualisation)", "style": "HIGHLIGHTED"},
          {"text": "Export CAO / BIM (DXF, IFC)", "style": "HIGHLIGHTED"}
        ]
      },
      {
        "title": null,
        "items": [
          {"text": "Export PDF + emprise GeoJSON", "style": "NORMAL"},
          {"text": "Support email", "style": "NORMAL"},
          {"text": "Marque blanche / co-branding", "style": "EXCLUDED"}
        ]
      }
    ]'
where id = '4219611e-7584-4636-a3c5-ba212600715b';

update "subscription_product"
set features         = '[
  "Métrés 3D — surface, pente, périmètre",
  "Métrés détaillés — faîtage, rives, égouts, noues",
  "Maquette 3D des pans (visualisation)",
  "5 € HT / analyse supplémentaire",
  "Marque blanche / co-branding du rapport",
  "Bouton sur votre site pour génération de prospects",
  "Communauté BIRDIA — 1 chantier proposé / mois (particulier, entretien)",
  "Support 7j/7 par email"
]',
    feature_sections = '[
      {
        "title": "Métrés inclus",
        "items": [
          {
            "text": "Métrés 3D — surface, pente, périmètre",
            "style": "HIGHLIGHTED"
          },
          {
            "text": "Métrés détaillés — faîtage, rives, égouts, noues",
            "style": "HIGHLIGHTED"
          },
          {
            "text": "Maquette 3D des pans (visualisation)",
            "style": "HIGHLIGHTED"
          }
        ]
      },
      {
        "title": null,
        "items": [
          {
            "text": "5 € HT / analyse supplémentaire",
            "style": "NORMAL"
          },
          {
            "text": "Marque blanche / co-branding du rapport",
            "style": "NORMAL"
          },
          {
            "text": "Bouton sur votre site pour génération de prospects",
            "style": "NORMAL"
          },
          {
            "text": "**Communauté BIRDIA** — 1 chantier proposé / mois (particulier, entretien)",
            "style": "NORMAL"
          },
          {
            "text": "Support 7j/7 par email",
            "style": "NORMAL"
          }
        ]
      }
    ]'
where id = '89f1acdd-c3b9-4717-a21d-355b2021ad58';

update "subscription_product"
set description='Pour l''entreprise 3–10 personnes — équipe et intégration CRM.',
    features         = '[
  "Communauté BIRDIA — +2 chantiers / mois (particuliers, entretiens)",
  "Outil d''aide aux appels d''offres publics ou grands groupes",
  "4 € HT / analyse supplémentaire",
  "Support prioritaire"
]',
    feature_sections = '[
      {
        "title": null,
        "items": [
          {
            "text": "**Communauté BIRDIA** — +2 chantiers / mois (particuliers, entretiens)",
            "style": "NORMAL"
          },
          {
            "text": "**Outil d''aide aux appels d''offres** publics ou grands groupes",
            "style": "NORMAL"
          },
          {
            "text": "4 € HT / analyse supplémentaire",
            "style": "NORMAL"
          },
          {
            "text": "Support prioritaire",
            "style": "NORMAL"
          }
        ]
      }
    ]'
where id = 'c5f57306-a7b1-43f4-90fc-204ccd4c0ce2';