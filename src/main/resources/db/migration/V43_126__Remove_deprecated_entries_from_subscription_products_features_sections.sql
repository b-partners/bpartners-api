update subscription_product
set feature_sections =
        '[
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
              {"text": "Support email", "style": "NORMAL"}
            ]
          }
        ]'
where id = '4219611e-7584-4636-a3c5-ba212600715b';

update subscription_product
set feature_sections =
        '[
          {
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
            ],
            "title": "Métrés inclus"
          },
          {
            "items": [
              {
                "text": "5 € HT / analyse supplémentaire",
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
            ],
            "title": null
          }
        ]'
where id = '89f1acdd-c3b9-4717-a21d-355b2021ad58';