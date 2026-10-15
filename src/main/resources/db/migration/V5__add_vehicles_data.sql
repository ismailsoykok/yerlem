
-- V2__seed_vehicles.sql

-- Farklı kullanım türlerinde araçlar
INSERT INTO vehicles (
    plate_number,
    make,
    model,
    current_city,
    current_latitude,
    current_longitude,
    total_range_km,
    remaining_range_km,
    status
)
VALUES
    -- Şehir içi kompakt araç
    ('06 TEST 004', 'Nissan', 'Leaf', 'Ankara',
     39.9208, 32.8541, 270.00, 240.00, 'AVAILABLE'),

    -- Uzun menzilli SUV
    ('34 TEST 005', 'Tesla', 'Model Y', 'Istanbul',
     41.0150, 28.9795, 450.00, 410.00, 'AVAILABLE'),

    -- Düşük menzilli araç: menzil kontrolü testi
    ('35 TEST 006', 'Fiat', '500e', 'Izmir',
     38.4320, 27.1350, 250.00, 20.00, 'AVAILABLE'),

    -- Bakımda olan araç
    ('06 TEST 007', 'Renault', 'Megane E-Tech', 'Ankara',
     39.9400, 32.8600, 350.00, 300.00, 'MAINTENANCE'),

    -- Kullanımda olan araç
    ('34 TEST 008', 'Togg', 'T10X', 'Istanbul',
     41.0082, 28.9784, 400.00, 320.00, 'IN_USE'),

    -- Şehirler arası kullanım için uzun menzilli araç
    ('06 TEST 009', 'Tesla', 'Model 3', 'Ankara',
     39.9334, 32.8597, 500.00, 470.00, 'AVAILABLE')

    ON CONFLICT (plate_number) DO NOTHING;


-- Araçların mevcut şehirlerini kullanım alanı olarak ekle
INSERT INTO vehicle_usage_areas (vehicle_id, city_name)
SELECT id, current_city
FROM vehicles
WHERE plate_number IN (
                       '06 TEST 004',
                       '34 TEST 005',
                       '35 TEST 006',
                       '06 TEST 007',
                       '34 TEST 008',
                       '06 TEST 009'
    )
    ON CONFLICT (vehicle_id, city_name) DO NOTHING;


-- Çok şehirli kullanım alanları
INSERT INTO vehicle_usage_areas (vehicle_id, city_name)
SELECT id, 'Ankara'
FROM vehicles
WHERE plate_number = '34 TEST 005'
    ON CONFLICT (vehicle_id, city_name) DO NOTHING;

INSERT INTO vehicle_usage_areas (vehicle_id, city_name)
SELECT id, 'Istanbul'
FROM vehicles
WHERE plate_number = '06 TEST 009'
    ON CONFLICT (vehicle_id, city_name) DO NOTHING;
