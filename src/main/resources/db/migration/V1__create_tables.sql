
-- V1__create_tables.sql

-- 1. USERS
CREATE TABLE users (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       full_name VARCHAR(150) NOT NULL,
                       phone VARCHAR(20) NOT NULL UNIQUE,
                       identity_number VARCHAR(50) UNIQUE,
                       profile_photo_url TEXT,
                       status VARCHAR(20) NOT NULL DEFAULT 'PENDING',
                       phone_verified BOOLEAN NOT NULL DEFAULT FALSE,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT chk_users_status
                           CHECK (status IN ('PENDING', 'ACTIVE', 'BLOCKED'))
);

-- 2. OTP CODES
CREATE TABLE otp_codes (
                           id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                           user_id UUID NOT NULL REFERENCES users(id),
                           code_hash VARCHAR(255) NOT NULL,
                           expires_at TIMESTAMP NOT NULL,
                           consumed_at TIMESTAMP,
                           created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP
);

CREATE INDEX idx_otp_codes_user_id ON otp_codes(user_id);
CREATE INDEX idx_otp_codes_expires_at ON otp_codes(expires_at);

-- 3. VEHICLES
CREATE TABLE vehicles (
                          id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                          plate_number VARCHAR(20) NOT NULL UNIQUE,
                          make VARCHAR(100) NOT NULL,
                          model VARCHAR(100) NOT NULL,
                          current_city VARCHAR(100) NOT NULL,
                          current_latitude NUMERIC(10, 7),
                          current_longitude NUMERIC(10, 7),
                          total_range_km NUMERIC(10, 2) NOT NULL,
                          remaining_range_km NUMERIC(10, 2) NOT NULL,
                          status VARCHAR(20) NOT NULL DEFAULT 'AVAILABLE',
                          last_location_at TIMESTAMP,
                          created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                          CONSTRAINT chk_vehicles_status
                              CHECK (status IN ('AVAILABLE', 'IN_USE', 'MAINTENANCE')),
                          CONSTRAINT chk_vehicles_total_range
                              CHECK (total_range_km >= 0),
                          CONSTRAINT chk_vehicles_remaining_range
                              CHECK (remaining_range_km >= 0 AND remaining_range_km <= total_range_km),
                          CONSTRAINT chk_vehicles_latitude
                              CHECK (current_latitude IS NULL OR current_latitude BETWEEN -90 AND 90),
                          CONSTRAINT chk_vehicles_longitude
                              CHECK (current_longitude IS NULL OR current_longitude BETWEEN -180 AND 180)
);

-- 4. VEHICLE USAGE AREAS
CREATE TABLE vehicle_usage_areas (
                                     id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                     vehicle_id UUID NOT NULL REFERENCES vehicles(id),
                                     city_name VARCHAR(100) NOT NULL,
                                     created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                     CONSTRAINT uq_vehicle_usage_area UNIQUE (vehicle_id, city_name)
);

CREATE INDEX idx_vehicle_usage_areas_city ON vehicle_usage_areas(city_name);

-- 5. CITY FARES
CREATE TABLE city_fares (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            city_name VARCHAR(100) NOT NULL UNIQUE,
                            price_per_km NUMERIC(10, 2) NOT NULL,
                            currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
                            active BOOLEAN NOT NULL DEFAULT TRUE,
                            created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT chk_city_fares_price CHECK (price_per_km >= 0)
);

-- 6. TRIPS
CREATE TABLE trips (
                       id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                       user_id UUID NOT NULL REFERENCES users(id),
                       vehicle_id UUID NOT NULL REFERENCES vehicles(id),
                       origin_city VARCHAR(100) NOT NULL,
                       origin_latitude NUMERIC(10, 7) NOT NULL,
                       origin_longitude NUMERIC(10, 7) NOT NULL,
                       destination_city VARCHAR(100) NOT NULL,
                       destination_latitude NUMERIC(10, 7) NOT NULL,
                       destination_longitude NUMERIC(10, 7) NOT NULL,
                       status VARCHAR(20) NOT NULL DEFAULT 'CREATED',
                       estimated_distance_km NUMERIC(10, 2),
                       actual_distance_km NUMERIC(10, 2),
                       estimated_duration_minutes INTEGER,
                       actual_duration_minutes INTEGER,
                       started_at TIMESTAMP,
                       completed_at TIMESTAMP,
                       created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                       CONSTRAINT chk_trips_status
                           CHECK (status IN ('CREATED', 'IN_PROGRESS', 'COMPLETED', 'CANCELLED')),
                       CONSTRAINT chk_trips_estimated_distance
                           CHECK (estimated_distance_km IS NULL OR estimated_distance_km >= 0),
                       CONSTRAINT chk_trips_actual_distance
                           CHECK (actual_distance_km IS NULL OR actual_distance_km >= 0),
                       CONSTRAINT chk_trips_estimated_duration
                           CHECK (estimated_duration_minutes IS NULL OR estimated_duration_minutes >= 0),
                       CONSTRAINT chk_trips_actual_duration
                           CHECK (actual_duration_minutes IS NULL OR actual_duration_minutes >= 0),
                       CONSTRAINT chk_trips_origin_latitude
                           CHECK (origin_latitude BETWEEN -90 AND 90),
                       CONSTRAINT chk_trips_origin_longitude
                           CHECK (origin_longitude BETWEEN -180 AND 180),
                       CONSTRAINT chk_trips_destination_latitude
                           CHECK (destination_latitude BETWEEN -90 AND 90),
                       CONSTRAINT chk_trips_destination_longitude
                           CHECK (destination_longitude BETWEEN -180 AND 180),
                       CONSTRAINT chk_trips_dates
                           CHECK (completed_at IS NULL OR started_at IS NULL OR completed_at >= started_at)
);

CREATE INDEX idx_trips_user_id ON trips(user_id);
CREATE INDEX idx_trips_vehicle_id ON trips(vehicle_id);
CREATE INDEX idx_trips_status ON trips(status);

CREATE UNIQUE INDEX uq_trips_active_vehicle
    ON trips(vehicle_id)
    WHERE status = 'IN_PROGRESS';

-- 7. TRIP LOCATIONS
CREATE TABLE trip_locations (
                                id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                trip_id UUID NOT NULL REFERENCES trips(id),
                                latitude NUMERIC(10, 7) NOT NULL,
                                longitude NUMERIC(10, 7) NOT NULL,
                                city_name VARCHAR(100),
                                speed_kmh NUMERIC(8, 2),
                                recorded_at TIMESTAMP NOT NULL,
                                created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                CONSTRAINT chk_trip_locations_latitude CHECK (latitude BETWEEN -90 AND 90),
                                CONSTRAINT chk_trip_locations_longitude CHECK (longitude BETWEEN -180 AND 180),
                                CONSTRAINT chk_trip_locations_speed CHECK (speed_kmh IS NULL OR speed_kmh >= 0)
);

CREATE INDEX idx_trip_locations_trip_time
    ON trip_locations(trip_id, recorded_at);

-- 8. TRIP FARES
CREATE TABLE trip_fares (
                            id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                            trip_id UUID NOT NULL UNIQUE REFERENCES trips(id),
                            city_fare_id UUID REFERENCES city_fares(id),
                            distance_km NUMERIC(10, 2) NOT NULL,
                            price_per_km NUMERIC(10, 2) NOT NULL,
                            total_amount NUMERIC(12, 2) NOT NULL,
                            currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
                            status VARCHAR(20) NOT NULL DEFAULT 'ESTIMATED',
                            calculated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                            CONSTRAINT chk_trip_fares_distance CHECK (distance_km >= 0),
                            CONSTRAINT chk_trip_fares_price CHECK (price_per_km >= 0),
                            CONSTRAINT chk_trip_fares_amount CHECK (total_amount >= 0),
                            CONSTRAINT chk_trip_fares_status
                                CHECK (status IN ('ESTIMATED', 'FINALIZED', 'FAILED'))
);

-- 9. WALLETS
CREATE TABLE wallets (
                         id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                         user_id UUID NOT NULL UNIQUE REFERENCES users(id),
                         balance NUMERIC(12, 2) NOT NULL DEFAULT 0,
                         currency VARCHAR(3) NOT NULL DEFAULT 'TRY',
                         created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         updated_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                         CONSTRAINT chk_wallets_balance CHECK (balance >= 0)
);

-- 10. WALLET MOVEMENTS
CREATE TABLE wallet_movements (
                                  id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                                  wallet_id UUID NOT NULL REFERENCES wallets(id),
                                  trip_id UUID REFERENCES trips(id),
                                  movement_type VARCHAR(30) NOT NULL,
                                  amount NUMERIC(12, 2) NOT NULL,
                                  balance_after NUMERIC(12, 2) NOT NULL,
                                  description VARCHAR(255),
                                  idempotency_key VARCHAR(100) NOT NULL UNIQUE,
                                  created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                                  CONSTRAINT chk_wallet_movements_type
                                      CHECK (movement_type IN ('TOP_UP', 'TRIP_CHARGE', 'REFUND', 'ADJUSTMENT')),
                                  CONSTRAINT chk_wallet_movements_amount CHECK (amount > 0),
                                  CONSTRAINT chk_wallet_movements_balance CHECK (balance_after >= 0)
);

CREATE UNIQUE INDEX uq_wallet_movement_trip_charge
    ON wallet_movements(trip_id)
    WHERE movement_type = 'TRIP_CHARGE';

CREATE INDEX idx_wallet_movements_wallet_time
    ON wallet_movements(wallet_id, created_at);

-- 11. FAULT REPORTS
CREATE TABLE fault_reports (
                               id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                               trip_id UUID NOT NULL REFERENCES trips(id),
                               vehicle_id UUID NOT NULL REFERENCES vehicles(id),
                               reported_by_user_id UUID NOT NULL REFERENCES users(id),
                               fault_type VARCHAR(30) NOT NULL,
                               description TEXT,
                               reported_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                               resolved_at TIMESTAMP,
                               status VARCHAR(20) NOT NULL DEFAULT 'OPEN',
                               CONSTRAINT chk_fault_reports_type
                                   CHECK (fault_type IN ('TIRE', 'BATTERY', 'DOOR', 'MOTOR', 'OTHER')),
                               CONSTRAINT chk_fault_reports_status
                                   CHECK (status IN ('OPEN', 'IN_PROGRESS', 'RESOLVED')),
                               CONSTRAINT chk_fault_reports_dates
                                   CHECK (resolved_at IS NULL OR resolved_at >= reported_at)
);

CREATE INDEX idx_fault_reports_vehicle_status
    ON fault_reports(vehicle_id, status);

-- 12. TRIP ALERTS
CREATE TABLE trip_alerts (
                             id UUID PRIMARY KEY DEFAULT gen_random_uuid(),
                             trip_id UUID NOT NULL REFERENCES trips(id),
                             alert_type VARCHAR(40) NOT NULL,
                             message TEXT NOT NULL,
                             recorded_value NUMERIC(10, 2),
                             threshold_value NUMERIC(10, 2),
                             created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
                             CONSTRAINT chk_trip_alerts_type
                                 CHECK (
                                     alert_type IN (
                                                    'SPEED_LIMIT_EXCEEDED',
                                                    'DURATION_EXCEEDED',
                                                    'FAULT_REPORTED',
                                                    'OTHER'
                                         )
                                     )
);

CREATE INDEX idx_trip_alerts_trip_time
    ON trip_alerts(trip_id, created_at);
