
-- 1. Şehir tarifeleri
INSERT INTO city_fares (city_name, price_per_km, currency)
VALUES
    ('Ankara', 10.00, 'TRY'),
    ('Istanbul', 15.00, 'TRY'),
    ('Izmir', 12.00, 'TRY');

-- 2. Beş doğrulanmış kullanıcı
INSERT INTO users (
    full_name,
    phone,
    identity_number,
    status,
    phone_verified
)
VALUES
    ('Ahmet Yilmaz', '5550000001', 'TESTID000001', 'ACTIVE', TRUE),
    ('Ayse Demir', '5550000002', 'TESTID000002', 'ACTIVE', TRUE),
    ('Mehmet Kaya', '5550000003', 'TESTID000003', 'ACTIVE', TRUE),
    ('Zeynep Sahin', '5550000004', 'TESTID000004', 'ACTIVE', TRUE),
    ('Mustafa Celik', '5550000005', 'TESTID000005', 'ACTIVE', TRUE);

-- 3. Araçlar
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
    ('06 TEST 001', 'Renault', 'Zoe', 'Ankara',
     39.9334, 32.8597, 300.00, 280.00, 'AVAILABLE'),

    ('34 TEST 002', 'Togg', 'T10X', 'Istanbul',
     41.0082, 28.9784, 400.00, 350.00, 'AVAILABLE'),

    ('35 TEST 003', 'Fiat', '500e', 'Izmir',
     38.4237, 27.1428, 250.00, 220.00, 'AVAILABLE');

-- 4. Araçların kullanım alanları
INSERT INTO vehicle_usage_areas (vehicle_id, city_name)
SELECT id, current_city
FROM vehicles;

-- 5. Kullanıcı cüzdanları
INSERT INTO wallets (user_id, balance, currency)
SELECT id, 500.00, 'TRY'
FROM users;
