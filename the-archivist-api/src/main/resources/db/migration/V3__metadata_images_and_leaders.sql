-- URLs de imágenes que publica la Metadata de la Halo API. No se descargan:
-- el navegador las carga directo desde el CDN oficial.
ALTER TABLE maps ADD COLUMN image_url TEXT;

-- Líderes tal como los entrega la Metadata (ID del juego, nombre, facción e imagen).
-- Sirve para completar las imágenes del catálogo fijo y para verificar los IDs.
CREATE TABLE leader_metadata (
    leader_id  INTEGER PRIMARY KEY,
    name       VARCHAR(255) NOT NULL,
    faction    VARCHAR(32),
    image_url  TEXT,
    synced_at  TIMESTAMPTZ NOT NULL DEFAULT now()
);
