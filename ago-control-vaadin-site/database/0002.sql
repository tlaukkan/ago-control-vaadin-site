ALTER TABLE element ADD COLUMN busid character varying(255);
INSERT INTO schemaversion VALUES (NOW(), 'agosite', '0002');
