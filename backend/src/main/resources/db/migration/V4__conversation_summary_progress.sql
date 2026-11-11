-- Memoria conversatiei (Faza 5d): `conversation.summary` (din V2) tine rezumatul rulant al mesajelor vechi, iar aceasta
-- coloana spune cate mesaje din inceputul conversatiei sunt deja acoperite de rezumat, ca sa nu fie rezumate de doua ori
-- si sa nu se piarda niciunul. SQL standard, ca aceeasi migrare sa treaca pe H2 (mod PostgreSQL) si pe PostgreSQL.
alter table conversation add column summary_message_count integer default 0 not null;
