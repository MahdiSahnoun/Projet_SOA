-- =====================================================================
--  Données fictives du registre national (registre_db, PostgreSQL)
--  Chargées au démarrage après la création des tables par Hibernate
--  (spring.jpa.defer-datasource-initialization=true, spring.sql.init.mode=always).
--
--  Identifiants au format du service Dons : TN-00000001 ... TN-00000015.
--  Groupes au format de l'enum GroupeSanguin (= noms envoyés par Dons) : O_NEGATIF, ...
--  Toutes les dates sont relatives à CURRENT_DATE : l'éligibilité de chaque
--  donneur reste la même quel que soit le jour où l'on lance la démo.
--
--  Règles appliquées par RegistreService : 18-65 ans, >= 56 jours depuis
--  le dernier don, >= 50 kg, pas de contre-indication.
--  Chaque donneur inéligible ne viole qu'UNE seule règle.
-- =====================================================================

-- ---------------------------------------------------------------------
-- TN-00000001 = compte de démo donneur@test.tn du service Dons (O-).
-- DO UPDATE : après un EnregistrerDon pendant la démo, il redevient
-- éligible au prochain démarrage (date_dernier_don remise à NULL).
-- ---------------------------------------------------------------------
INSERT INTO donneur (id, nom, prenom, cin, date_naissance, sexe, groupe_sanguin, poids,
                     date_dernier_don, contre_indication, ville, telephone)
VALUES
('TN-00000001', 'Ben Salah',  'Ahmed',   '08123401', (CURRENT_DATE - INTERVAL '30 years')::date, 'M', 'O_NEGATIF',  75, NULL,               FALSE, 'Tunis',    '+216 20 123 401')
ON CONFLICT (id) DO UPDATE SET date_dernier_don = EXCLUDED.date_dernier_don;

-- ---------------------------------------------------------------------
-- Les 14 autres : ON CONFLICT (id) DO NOTHING (aucun doublon au redémarrage)
-- ---------------------------------------------------------------------
INSERT INTO donneur (id, nom, prenom, cin, date_naissance, sexe, groupe_sanguin, poids,
                     date_dernier_don, contre_indication, ville, telephone)
VALUES
-- Éligibles (TN-00000002 à TN-00000010) : avec TN-00000001, les 8 groupes sont représentés
('TN-00000002', 'Trabelsi',   'Mariem',  '08123402', (CURRENT_DATE - INTERVAL '42 years')::date, 'F', 'O_POSITIF',  62, CURRENT_DATE - 120, FALSE, 'Sfax',     '+216 22 123 402'),
('TN-00000003', 'Gharbi',     'Youssef', '08123403', (CURRENT_DATE - INTERVAL '25 years')::date, 'M', 'A_NEGATIF',  80, NULL,               FALSE, 'Sousse',   '+216 23 123 403'),
('TN-00000004', 'Jlassi',     'Salma',   '08123404', (CURRENT_DATE - INTERVAL '35 years')::date, 'F', 'A_POSITIF',  58, CURRENT_DATE - 90,  FALSE, 'Nabeul',   '+216 24 123 404'),
('TN-00000005', 'Mejri',      'Karim',   '08123405', (CURRENT_DATE - INTERVAL '28 years')::date, 'M', 'B_NEGATIF',  70, NULL,               FALSE, 'Bizerte',  '+216 25 123 405'),
('TN-00000006', 'Hammami',    'Ines',    '08123406', (CURRENT_DATE - INTERVAL '50 years')::date, 'F', 'B_POSITIF',  65, CURRENT_DATE - 200, FALSE, 'Monastir', '+216 26 123 406'),
('TN-00000007', 'Bouazizi',   'Hamza',   '08123407', (CURRENT_DATE - INTERVAL '33 years')::date, 'M', 'AB_NEGATIF', 85, NULL,               FALSE, 'Kairouan', '+216 27 123 407'),
('TN-00000008', 'Chaabane',   'Nour',    '08123408', (CURRENT_DATE - INTERVAL '45 years')::date, 'F', 'AB_POSITIF', 60, CURRENT_DATE - 60,  FALSE, 'Gabès',    '+216 28 123 408'),
('TN-00000009', 'Mansouri',   'Mohamed', '08123409', (CURRENT_DATE - INTERVAL '60 years')::date, 'M', 'O_NEGATIF',  78, CURRENT_DATE - 365, FALSE, 'Ariana',   '+216 29 123 409'),
('TN-00000010', 'Khelifi',    'Amira',   '08123410', (CURRENT_DATE - INTERVAL '22 years')::date, 'F', 'A_POSITIF',  55, NULL,               FALSE, 'Ben Arous','+216 50 123 410'),

-- Inéligibles (une seule règle violée par donneur)
-- TN-00000011 : dernier don il y a 20 jours (< 56) → prochainDonPossible = J+36
('TN-00000011', 'Saidi',      'Walid',   '08123411', (CURRENT_DATE - INTERVAL '31 years')::date, 'M', 'O_POSITIF',  74, CURRENT_DATE - 20,  FALSE, 'Tunis',    '+216 51 123 411'),
-- TN-00000012 : 17 ans (< 18)
('TN-00000012', 'Ayari',      'Rania',   '08123412', (CURRENT_DATE - INTERVAL '17 years')::date, 'F', 'A_POSITIF',  56, NULL,               FALSE, 'Sfax',     '+216 52 123 412'),
-- TN-00000013 : 68 ans (> 65)
('TN-00000013', 'Baccouche',  'Habib',   '08123413', (CURRENT_DATE - INTERVAL '68 years')::date, 'M', 'B_POSITIF',  72, NULL,               FALSE, 'Sousse',   '+216 53 123 413'),
-- TN-00000014 : contre-indication médicale
('TN-00000014', 'Ferchichi',  'Leila',   '08123414', (CURRENT_DATE - INTERVAL '40 years')::date, 'F', 'O_NEGATIF',  63, NULL,               TRUE,  'Nabeul',   '+216 54 123 414'),
-- TN-00000015 : 45 kg (< 50)
('TN-00000015', 'Zouari',     'Sarra',   '08123415', (CURRENT_DATE - INTERVAL '27 years')::date, 'F', 'AB_POSITIF', 45, NULL,               FALSE, 'Monastir', '+216 55 123 415')

ON CONFLICT (id) DO NOTHING;
