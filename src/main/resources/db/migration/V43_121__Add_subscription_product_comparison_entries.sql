alter table "subscription_product"
    add column if not exists comparison_entries json;

update "subscription_product"
set comparison_entries = '[
  {"sectionTitle": "Métrés — coeur BIRDIA", "label": "Surface rampant, pente, périmètre", "kind": "INCLUDED"},
  {"sectionTitle": "Métrés — coeur BIRDIA", "label": "Faîtage, rives, égouts, noues (linéaires)", "kind": "INCLUDED"},
  {"sectionTitle": "Métrés — coeur BIRDIA", "label": "Maquette 3D des pans (visualisation)", "kind": "INCLUDED"},
  {"sectionTitle": "Métrés — coeur BIRDIA", "label": "Export CAO / BIM (DXF, IFC)", "kind": "INCLUDED"},
  {"sectionTitle": "Livrables & formats", "label": "Rapport PDF + emprise GeoJSON", "kind": "INCLUDED"},
  {"sectionTitle": "Livrables & formats", "label": "Marque blanche / co-branding rapport", "kind": "EXCLUDED"},
  {"sectionTitle": "Équipe & process", "label": "Bouton sur votre site pour génération de prospects", "kind": "EXCLUDED"},
  {"sectionTitle": "Équipe & process", "label": "Module devis automatisé", "kind": "EXCLUDED"},
  {"sectionTitle": "Intégration & monitoring", "label": "Accès API & webhooks", "kind": "EXCLUDED"},
  {"sectionTitle": "Intégration & monitoring", "label": "Monitoring annuel (re-scan auto)", "kind": "EXCLUDED"},
  {"sectionTitle": "Communauté BIRDIA — chantiers proposés", "label": "Chantiers proposés / mois", "kind": "EXCLUDED"},
  {"sectionTitle": "Communauté BIRDIA — chantiers proposés", "label": "Outil d''aide aux appels d''offres publics ou grands groupes", "kind": "EXCLUDED"},
  {"sectionTitle": "Support", "label": "Support", "kind": "TEXT", "text": "Email"}
]'
where id = '4219611e-7584-4636-a3c5-ba212600715b';

update "subscription_product"
set comparison_entries = '[
  {"sectionTitle": "Métrés — coeur BIRDIA", "label": "Surface rampant, pente, périmètre", "kind": "INCLUDED"},
  {"sectionTitle": "Métrés — coeur BIRDIA", "label": "Faîtage, rives, égouts, noues (linéaires)", "kind": "INCLUDED"},
  {"sectionTitle": "Métrés — coeur BIRDIA", "label": "Maquette 3D des pans (visualisation)", "kind": "INCLUDED"},
  {"sectionTitle": "Métrés — coeur BIRDIA", "label": "Export CAO / BIM (DXF, IFC)", "kind": "INCLUDED"},
  {"sectionTitle": "Livrables & formats", "label": "Rapport PDF + emprise GeoJSON", "kind": "INCLUDED"},
  {"sectionTitle": "Livrables & formats", "label": "Marque blanche / co-branding rapport", "kind": "INCLUDED"},
  {"sectionTitle": "Équipe & process", "label": "Bouton sur votre site pour génération de prospects", "kind": "INCLUDED"},
  {"sectionTitle": "Équipe & process", "label": "Module devis automatisé", "kind": "EXCLUDED"},
  {"sectionTitle": "Intégration & monitoring", "label": "Accès API & webhooks", "kind": "EXCLUDED"},
  {"sectionTitle": "Intégration & monitoring", "label": "Monitoring annuel (re-scan auto)", "kind": "EXCLUDED"},
  {"sectionTitle": "Communauté BIRDIA — chantiers proposés", "label": "Chantiers proposés / mois", "kind": "TEXT", "text": "1 (particulier, entretien)"},
  {"sectionTitle": "Communauté BIRDIA — chantiers proposés", "label": "Outil d''aide aux appels d''offres publics ou grands groupes", "kind": "EXCLUDED"},
  {"sectionTitle": "Support", "label": "Support", "kind": "TEXT", "text": "7j/7 email"}
]'
where id = '89f1acdd-c3b9-4717-a21d-355b2021ad58';

update "subscription_product"
set comparison_entries = '[
  {"sectionTitle": "Métrés — coeur BIRDIA", "label": "Surface rampant, pente, périmètre", "kind": "INCLUDED"},
  {"sectionTitle": "Métrés — coeur BIRDIA", "label": "Faîtage, rives, égouts, noues (linéaires)", "kind": "INCLUDED"},
  {"sectionTitle": "Métrés — coeur BIRDIA", "label": "Maquette 3D des pans (visualisation)", "kind": "INCLUDED"},
  {"sectionTitle": "Métrés — coeur BIRDIA", "label": "Export CAO / BIM (DXF, IFC)", "kind": "INCLUDED"},
  {"sectionTitle": "Livrables & formats", "label": "Rapport PDF + emprise GeoJSON", "kind": "INCLUDED"},
  {"sectionTitle": "Livrables & formats", "label": "Marque blanche / co-branding rapport", "kind": "INCLUDED"},
  {"sectionTitle": "Équipe & process", "label": "Bouton sur votre site pour génération de prospects", "kind": "INCLUDED"},
  {"sectionTitle": "Équipe & process", "label": "Module devis automatisé", "kind": "INCLUDED"},
  {"sectionTitle": "Intégration & monitoring", "label": "Accès API & webhooks", "kind": "EXCLUDED"},
  {"sectionTitle": "Intégration & monitoring", "label": "Monitoring annuel (re-scan auto)", "kind": "EXCLUDED"},
  {"sectionTitle": "Communauté BIRDIA — chantiers proposés", "label": "Chantiers proposés / mois", "kind": "TEXT", "text": "+2 (particuliers, entretiens)"},
  {"sectionTitle": "Communauté BIRDIA — chantiers proposés", "label": "Outil d''aide aux appels d''offres publics ou grands groupes", "kind": "INCLUDED"},
  {"sectionTitle": "Support", "label": "Support", "kind": "TEXT", "text": "Prioritaire"}
]'
where id = 'c5f57306-a7b1-43f4-90fc-204ccd4c0ce2';

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
    "kind": "INCLUDED"
  },
  {
    "sectionTitle": "Intégration & monitoring",
    "label": "Accès API & webhooks",
    "kind": "INCLUDED"
  },
  {
    "sectionTitle": "Intégration & monitoring",
    "label": "Monitoring annuel (re-scan auto)",
    "kind": "INCLUDED"
  },
  {
    "sectionTitle": "Communauté BIRDIA — chantiers proposés",
    "label": "Chantiers proposés / mois",
    "kind": "TEXT",
    "text": "+5 (particuliers, entretiens, AO)"
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
    "text": "Dédié 4h ouvrées"
  }
]'
where id = '37b9639e-d058-4222-8a2a-d78d5fe7b6b1';
