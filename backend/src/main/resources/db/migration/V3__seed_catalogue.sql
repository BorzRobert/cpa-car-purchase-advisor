-- Catalog de pornire (D6): modele populare pe piata din Romania, cifre ORIENTATIVE rotunjite de mana, NEVERIFICATE fata
-- de listele de pret ale dealerilor. Servesc la dezvoltare si la demonstrarea uneltelor; inainte de productie (Faza 9)
-- trebuie revizuite si extinse. `data_as_of` este data la care au fost introduse; UI-ul o arata ca „date la”.
-- `euro_ncap_stars` este NULL cand nu am o sursa sigura (nu inseamna „0 stele”).

insert into vehicle (id, make, model, body_type, seats, boot_litres, euro_ncap_stars, reliability_notes) values
('dacia-sandero',    'Dacia',    'Sandero',       'HATCHBACK', 5, 328, null, 'Mecanica simpla si piese ieftine; verifica istoricul de service si starea interiorului.'),
('dacia-duster',     'Dacia',    'Duster',        'SUV',       5, 472, null, 'Mecanica simpla, retea larga de service; calitatea finisajelor este modesta.'),
('dacia-spring',     'Dacia',    'Spring',        'HATCHBACK', 4, 308, 1,    'Electrica urbana ieftina; autonomie mica pe autostrada si dotari de siguranta limitate.'),
('vw-golf',          'Volkswagen', 'Golf',        'HATCHBACK', 5, 380, 5,    'Piese si service peste tot; verifica istoricul cutiei automate si actualizarile de software.'),
('vw-id-3',          'Volkswagen', 'ID.3',        'HATCHBACK', 5, 385, null, 'Electrica compacta; verifica starea bateriei (certificat de sanatate) la masinile folosite.'),
('skoda-octavia-combi', 'Skoda', 'Octavia Combi', 'ESTATE',    5, 640, 5,    'Portbagaj foarte mare, costuri de intretinere moderate; verifica istoricul cutiei DSG.'),
('toyota-corolla',   'Toyota',   'Corolla',       'HATCHBACK', 5, 361, 5,    'Sistemul hibrid este cunoscut ca durabil; piese si service in retea oficiala.'),
('toyota-yaris',     'Toyota',   'Yaris',         'HATCHBACK', 5, 286, 5,    'Hibrid urban cu consum mic; spatiu redus pe bancheta din spate si in portbagaj.'),
('renault-clio',     'Renault',  'Clio',          'HATCHBACK', 5, 391, 5,    'Urbana confortabila; verifica istoricul de service, mai ales la versiunile turbo.'),
('peugeot-208',      'Peugeot',  '208',           'HATCHBACK', 5, 311, null, 'Urbana cu interior modern; bancheta din spate stramta pentru adulti inalti.'),
('hyundai-tucson',   'Hyundai',  'Tucson',        'SUV',       5, 539, 5,    'SUV familial cu garantie lunga; verifica pachetul de service oficial.'),
('hyundai-kona-electric', 'Hyundai', 'Kona Electric', 'SUV',   5, 466, null, 'Electrica urbana; verifica pompa de caldura si capacitatea de incarcare rapida.'),
('tesla-model-3',    'Tesla',    'Model 3',       'SEDAN',     5, 594, 5,    'Service doar prin reteaua producatorului; calitatea montajului variaza intre loturi.'),
('ford-puma',        'Ford',     'Puma',          'SUV',       5, 456, 5,    'Crossover compact cu portbagaj generos; verifica istoricul sistemului mild-hybrid.');

insert into vehicle_variant (id, vehicle_id, name, powertrain, price_eur, power_kw, consumption_l_100km, consumption_kwh_100km,
                             electric_range_km, battery_kwh, co2_g_km, annual_insurance_eur, annual_road_tax_eur,
                             annual_maintenance_eur, residual_value_pct_5y, data_as_of) values
('sandero-tce-90',             'dacia-sandero',    'TCe 90 Expression',          'PETROL',         14500,  67,  5.6, null, null, null,   127,  550,  40,  350, 48, date '2026-11-11'),
('duster-tce-130',             'dacia-duster',     'TCe 130 Expression',         'PETROL',         20500,  96,  6.4, null, null, null,   145,  750,  80,  450, 48, date '2026-11-11'),
('duster-hybrid-140',          'dacia-duster',     'Hybrid 140 Extreme',         'HYBRID',         24900, 104,  4.9, null, null, null,   108,  800,  80,  450, 52, date '2026-11-11'),
('spring-electric-65',         'dacia-spring',     'Electric 65',                'ELECTRIC',       16900,  48, null, 13.9,  225, 26.8,     0,  500,   0,  250, 40, date '2026-11-11'),
('golf-1-5-tsi-130',           'vw-golf',          '1.5 TSI 130 Life',           'PETROL',         29900,  96,  5.6, null, null, null,   127,  900,  90,  450, 50, date '2026-11-11'),
('golf-2-0-tdi-115',           'vw-golf',          '2.0 TDI 115 Life',           'DIESEL',         31500,  85,  4.7, null, null, null,   124,  950, 100,  480, 47, date '2026-11-11'),
('golf-gte-phev',              'vw-golf',          '1.4 eHybrid GTE',            'PLUG_IN_HYBRID', 41900, 180,  1.5, 14.5,   60, 13.0,    34, 1200, 120,  520, 42, date '2026-11-11'),
('id3-pro-58',                 'vw-id-3',          'Pro 58 kWh',                 'ELECTRIC',       38900, 150, null, 15.0,  426, 58.0,     0, 1050,   0,  280, 40, date '2026-11-11'),
('octavia-combi-1-5-tsi-150',  'skoda-octavia-combi', '1.5 TSI 150 Selection', 'PETROL',         30900, 110,  5.7, null, null, null,   129,  950,  90,  460, 50, date '2026-11-11'),
('octavia-combi-2-0-tdi-150',  'skoda-octavia-combi', '2.0 TDI 150 Selection', 'DIESEL',         34500, 110,  4.8, null, null, null,   125, 1000, 100,  490, 47, date '2026-11-11'),
('corolla-1-8-hybrid-140',     'toyota-corolla',   '1.8 Hybrid 140 Comfort',     'HYBRID',         28900, 103,  4.5, null, null, null,   102,  850,  90,  380, 56, date '2026-11-11'),
('corolla-2-0-hybrid-196',     'toyota-corolla',   '2.0 Hybrid 196 Style',       'HYBRID',         32400, 144,  5.0, null, null, null,   113,  950, 100,  400, 55, date '2026-11-11'),
('yaris-1-5-hybrid-116',       'toyota-yaris',     '1.5 Hybrid 116 Comfort',     'HYBRID',         23900,  85,  4.1, null, null, null,    93,  700,  70,  330, 55, date '2026-11-11'),
('clio-tce-100',               'renault-clio',     'TCe 100 Equilibre',          'PETROL',         18900,  74,  5.5, null, null, null,   125,  600,  50,  340, 47, date '2026-11-11'),
('clio-e-tech-160',            'renault-clio',     'E-Tech Full Hybrid 160',     'HYBRID',         25900, 118,  4.5, null, null, null,   102,  750,  80,  360, 52, date '2026-11-11'),
('208-puretech-100',           'peugeot-208',      'PureTech 100 Active',        'PETROL',         19500,  74,  5.5, null, null, null,   125,  620,  50,  350, 46, date '2026-11-11'),
('e-208-51',                   'peugeot-208',      'e-208 51 kWh Active',        'ELECTRIC',       34900, 115, null, 15.0,  410, 51.0,     0,  900,   0,  250, 40, date '2026-11-11'),
('tucson-1-6-hybrid-215',      'hyundai-tucson',   '1.6 T-GDi Hybrid 215',       'HYBRID',         38900, 158,  5.6, null, null, null,   127, 1100, 120,  520, 52, date '2026-11-11'),
('kona-electric-65',           'hyundai-kona-electric', 'Electric 65 kWh',     'ELECTRIC',       41900, 160, null, 14.7,  500, 65.4,     0, 1100,   0,  290, 42, date '2026-11-11'),
('model-3-rwd',                'tesla-model-3',    'Rear-Wheel Drive',           'ELECTRIC',       41990, 208, null, 14.0,  513, 60.0,     0, 1300,   0,  300, 45, date '2026-11-11'),
('puma-1-0-ecoboost-125',      'ford-puma',        '1.0 EcoBoost 125 mHEV',      'PETROL',         24900,  92,  5.7, null, null, null,   129,  800,  70,  400, 48, date '2026-11-11');
