update "subscription_product"
set comparison_entries = '[
  {
    "sectionTitle": "Métrés — coeur BIRDIA",
    "label": "Surface rampant, pente, périmètre",
    "kind": "INCLUDED"
  },
  {
    "sectionTitle": "Métrés — coeur BIRDIA",
    "label": "Faîtage, rives, égouts, noues (linéaires)",
    "kind": "INCLUDED"
  },
  {
    "sectionTitle": "Métrés — coeur BIRDIA",
    "label": "Maquette 3D des pans (visualisation)",
    "kind": "INCLUDED"
  },
  {
    "sectionTitle": "Métrés — coeur BIRDIA",
    "label": "Export CAO / BIM (DXF, IFC)",
    "kind": "INCLUDED"
  },
  {
    "sectionTitle": "Livrables & formats",
    "label": "Rapport PDF + emprise GeoJSON",
    "kind": "INCLUDED"
  },
  {
    "sectionTitle": "Livrables & formats",
    "label": "Marque blanche / co-branding rapport",
    "kind": "INCLUDED"
  },
  {
    "sectionTitle": "Équipe & process",
    "label": "Bouton sur votre site pour génération de prospects",
    "kind": "INCLUDED"
  },
  {
    "sectionTitle": "Équipe & process",
    "label": "Module devis automatisé",
    "kind": "EXCLUDED"
  },
  {
    "sectionTitle": "Intégration & monitoring",
    "label": "Accès API & webhooks",
    "kind": "EXCLUDED"
  },
  {
    "sectionTitle": "Intégration & monitoring",
    "label": "Monitoring annuel (re-scan auto)",
    "kind": "EXCLUDED"
  },
  {
    "sectionTitle": "Communauté BIRDIA — chantiers proposés",
    "label": "Chantiers proposés / mois",
    "kind": "TEXT",
    "text": "+2 (particuliers, entretiens)"
  },
  {
    "sectionTitle": "Communauté BIRDIA — chantiers proposés",
    "label": "Outil d''aide aux appels d''offres publics ou grands groupes",
    "kind": "INCLUDED"
  },
  {
    "sectionTitle": "Support",
    "label": "Support",
    "kind": "TEXT",
    "text": "Prioritaire"
  }
]'
where id = 'c5f57306-a7b1-43f4-90fc-204ccd4c0ce2';