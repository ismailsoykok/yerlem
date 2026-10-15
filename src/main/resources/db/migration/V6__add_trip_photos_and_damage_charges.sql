
-- V6__add_trip_photos_and_damage_charges.sql

-- 13. TRIP PHOTOS (mock dosya yolu / URL saklar)
CREATE TABLE trip_photos (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             trip_id UUID NOT NULL REFERENCES trips(id),
                             photo_type VARCHAR(20) NOT NULL,
                             stage VARCHAR(10) NOT NULL,
                             photo_url TEXT NOT NULL,
                             uploaded_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT chk_trip_photos_type
                                 CHECK (photo_type IN ('FRONT_SEAT', 'BACK_SEAT', 'EXTERIOR')),
                             CONSTRAINT chk_trip_photos_stage
                                 CHECK (stage IN ('START', 'END'))
);

CREATE INDEX idx_trip_photos_trip_stage
    ON trip_photos(trip_id, stage);

-- 14. TRIP DAMAGE CHARGES (hasar tespiti sonucu ek ücret kalemleri)
CREATE TABLE trip_damage_charges (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     trip_id UUID NOT NULL REFERENCES trips(id),
                                     photo_type VARCHAR(20) NOT NULL,
                                     description VARCHAR(255) NOT NULL,
                                     amount NUMERIC(12, 2) NOT NULL,
                                     currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     CONSTRAINT chk_trip_damage_charges_type
                                         CHECK (photo_type IN ('FRONT_SEAT', 'BACK_SEAT', 'EXTERIOR')),
                                     CONSTRAINT chk_trip_damage_charges_amount
                                         CHECK (amount >= 0)
);

CREATE INDEX idx_trip_damage_charges_trip
    ON trip_damage_charges(trip_id);
