-- allow restricted-use parking spot types alongside the existing STANDARD/EV types
ALTER TABLE parking_spots DROP CONSTRAINT ck_parking_spots_type;
ALTER TABLE parking_spots ADD CONSTRAINT ck_parking_spots_type CHECK (type IN ('STANDARD', 'EV', 'HANDICAPPED'));

INSERT INTO parking_spots (code, type) VALUES
    ('A-1', 'STANDARD'),
    ('A-2', 'STANDARD'),
    ('EV-1', 'EV'),
    ('HANDI-1', 'HANDICAPPED')
ON CONFLICT (code) DO NOTHING;
