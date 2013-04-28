\connect agosite

ALTER TABLE element ADD COLUMN bus_busid character varying(255);

ALTER TABLE element RENAME parentid  TO parent_elementid;
ALTER TABLE element
   ALTER COLUMN parent_elementid DROP NOT NULL;

UPDATE element SET parent_elementid = NULL;

INSERT INTO schemaversion VALUES (NOW(), 'agosite', '0002');
