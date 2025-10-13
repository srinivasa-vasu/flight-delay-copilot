INSERT INTO airline (code, name, on_time_performance, total_flights, delayed_flights, average_delay) VALUES
('AA', 'American Airlines', 78.5, 5000, 1075, 22.3),
('DL', 'Delta Air Lines', 82.3, 4800, 850, 18.5),
('UA', 'United Airlines', 75.8, 4500, 1080, 25.2),
('SW', 'Southwest Airlines', 80.2, 6000, 1188, 19.8),
('B6', 'JetBlue Airways', 72.5, 2500, 688, 27.5),
('AS', 'Alaska Airlines', 84.1, 1800, 286, 15.9),
('NK', 'Spirit Airlines', 69.8, 2200, 664, 30.2),
('F9', 'Frontier Airlines', 71.2, 1900, 548, 28.8);


-- Insert Airports
INSERT INTO airport (code, name, city, country, timezone, latitude, longitude, congestion_level, average_delay) VALUES
('JFK', 'John F. Kennedy International', 'New York', 'USA', 'America/New_York', 40.6413, -73.7781, 8, 28.5),
('LAX', 'Los Angeles International', 'Los Angeles', 'USA', 'America/Los_Angeles', 33.9425, -118.4081, 9, 25.3),
('ORD', 'O''Hare International', 'Chicago', 'USA', 'America/Chicago', 41.9742, -87.9073, 9, 30.2),
('DFW', 'Dallas Fort Worth International', 'Dallas', 'USA', 'America/Chicago', 32.8998, -97.0403, 7, 20.1),
('ATL', 'Hartsfield-Jackson Atlanta International', 'Atlanta', 'USA', 'America/New_York', 33.6407, -84.4277, 10, 32.5),
('DEN', 'Denver International', 'Denver', 'USA', 'America/Denver', 39.8561, -104.6737, 6, 18.7),
('SFO', 'San Francisco International', 'San Francisco', 'USA', 'America/Los_Angeles', 37.6213, -122.3790, 8, 26.4),
('SEA', 'Seattle-Tacoma International', 'Seattle', 'USA', 'America/Los_Angeles', 47.4502, -122.3088, 6, 17.9),
('BOS', 'Logan International', 'Boston', 'USA', 'America/New_York', 42.3656, -71.0096, 7, 22.8),
('MIA', 'Miami International', 'Miami', 'USA', 'America/New_York', 25.7959, -80.2870, 7, 24.6);

-- weather data
INSERT INTO weather_data (airport_id, date_time, temperature, wind_speed, wind_direction, visibility, conditions, description, severe_weather, precipitation_probability)
SELECT
    a.id,
    CURRENT_TIMESTAMP - INTERVAL '30 days' + (s.i || ' hours')::INTERVAL,
    20 + random() * 15,
    random() * 30,
    CASE floor(random() * 8)::INTEGER
        WHEN 0 THEN 'N'
        WHEN 1 THEN 'NE'
        WHEN 2 THEN 'E'
        WHEN 3 THEN 'SE'
        WHEN 4 THEN 'S'
        WHEN 5 THEN 'SW'
        WHEN 6 THEN 'W'
        ELSE 'NW'
    END,
    5 + random() * 10,
    CASE floor(random() * 5)::INTEGER
        WHEN 0 THEN 'Clear'
        WHEN 1 THEN 'Cloudy'
        WHEN 2 THEN 'Rain'
        WHEN 3 THEN 'Snow'
        ELSE 'Fog'
    END,
    'Weather conditions for flight operations',
    random() < 0.05,
    floor(random() * 100)::INTEGER
FROM airport a
CROSS JOIN generate_series(0, 1000) s(i);


-- Insert sample crew schedules
INSERT INTO crew_schedule (crew_id, duty_start, duty_end, flight_hours, rest_hours, base, available, airline_id)
SELECT
    'CREW' || a.id || '-' || s.i,
    CURRENT_TIMESTAMP - INTERVAL '7 days' + (s.i || ' days')::INTERVAL,
    CURRENT_TIMESTAMP - INTERVAL '7 days' + (s.i || ' days')::INTERVAL + INTERVAL '12 hours',
    8,
    12,
    (SELECT code FROM airport ORDER BY random() LIMIT 1),
    random() < 0.9,
    a.id
FROM airline a
CROSS JOIN generate_series(1, 1000) s(i);


-- historical flight info
DO $$
DECLARE
    airline_rec RECORD;
    origin_rec RECORD;
    dest_rec RECORD;
    flight_date TIMESTAMP;
    delay_min INTEGER;
    flight_status VARCHAR(20);
    delay_reason_text VARCHAR(500);
    rand_val text;
BEGIN
    FOR airline_rec IN SELECT * FROM airline LOOP
        FOR origin_rec IN SELECT * FROM airport LIMIT 5 LOOP
            FOR dest_rec IN SELECT * FROM airport WHERE id != origin_rec.id LIMIT 3 LOOP
                FOR i IN 0..364 LOOP
                    flight_date := CURRENT_DATE - INTERVAL '365 days' + (i || ' days')::INTERVAL;

                    -- Generate realistic delay patterns
                    delay_min := CASE
                        WHEN random() < 0.65 THEN 0  -- 65% on time
                        WHEN random() < 0.85 THEN floor(random() * 30 + 1)::INTEGER  -- 20% minor delays
                        WHEN random() < 0.95 THEN floor(random() * 90 + 31)::INTEGER  -- 10% moderate delays
                        WHEN random() < 0.99 THEN floor(random() * 180 + 91)::INTEGER  -- 4% severe delays
                        ELSE -1  -- 1% cancelled
                    END;

                    flight_status := CASE
                        WHEN delay_min = -1 THEN 'CANCELLED'
                        WHEN delay_min = 0 THEN 'ON_TIME'
                        WHEN delay_min <= 15 THEN 'ON_TIME'
                        ELSE 'DELAYED'
                    END;

                    delay_reason_text := CASE
                        WHEN delay_min > 60 THEN 'Weather conditions and ATC delays'
                        WHEN delay_min > 30 THEN 'Aircraft maintenance'
                        WHEN delay_min > 15 THEN 'Late incoming aircraft'
                        WHEN delay_min > 0 THEN 'Minor ground operations delay'
                        ELSE NULL
                    END;

                    IF delay_min >= 0 THEN
                       rand_val = floor(random() * 10)::TEXT || ' hours';
                        INSERT INTO flight (
                            flight_number, airline_id, origin_id, destination_id,
                            scheduled_departure, actual_departure, scheduled_arrival, actual_arrival,
                            delay_minutes, status, delay_reason, aircraft_type, weather_condition,
                            crew_issue, maintenance_issue, gate_delay, taxi_delay
                        ) VALUES (
                            airline_rec.code || floor(random() * 9000 + 1000)::TEXT,
                            airline_rec.id,
                            origin_rec.id,
                            dest_rec.id,
                            flight_date + TIME '08:00:00' + rand_val::INTERVAL,
                            flight_date + TIME '08:00:00' + rand_val::INTERVAL + (delay_min || ' minutes')::INTERVAL,
                            flight_date + TIME '10:00:00' + rand_val::INTERVAL,
                            flight_date + TIME '10:00:00' + rand_val::INTERVAL + (delay_min || ' minutes')::INTERVAL,
                            delay_min,
                            flight_status,
                            delay_reason_text,
                            CASE floor(random() * 4)::INTEGER
                                WHEN 0 THEN 'Boeing 737'
                                WHEN 1 THEN 'Airbus A320'
                                WHEN 2 THEN 'Boeing 787'
                                ELSE 'Airbus A350'
                            END,
                            CASE floor(random() * 5)::INTEGER
                                WHEN 0 THEN 'Clear'
                                WHEN 1 THEN 'Cloudy'
                                WHEN 2 THEN 'Rain'
                                WHEN 3 THEN 'Snow'
                                ELSE 'Fog'
                            END,
                            random() < 0.1,  -- 10% crew issues
                            random() < 0.15,  -- 15% maintenance issues
                            CASE WHEN delay_min > 0 THEN floor(random() * 15)::INTEGER ELSE 0 END,
                            CASE WHEN delay_min > 0 THEN floor(random() * 10)::INTEGER ELSE 0 END
                        );
                    END IF;
                END LOOP;
            END LOOP;
        END LOOP;
    END LOOP;
END $$;


-- flight schedule
DO $$
DECLARE
    airline_rec RECORD;
    origin_rec RECORD;
    dest_rec RECORD;
    flight_date TIMESTAMP;
    delay_min INTEGER;
    flight_status VARCHAR(20);
    delay_reason_text VARCHAR(500);
    rand_val text;
BEGIN
    FOR airline_rec IN SELECT * FROM airline LOOP
        FOR origin_rec IN SELECT * FROM airport LIMIT 5 LOOP
            FOR dest_rec IN SELECT * FROM airport WHERE id != origin_rec.id LIMIT 3 LOOP
                FOR i IN 0..31 LOOP
                    flight_date := CURRENT_DATE + INTERVAL '30 days' - (i || ' days')::INTERVAL;
                    rand_val := floor(random() * 10)::TEXT || ' hours';

                    INSERT INTO flight (
                        flight_number, airline_id, origin_id, destination_id,
                        scheduled_departure, scheduled_arrival,
                        aircraft_type, weather_condition
                    ) VALUES (
                        airline_rec.code || floor(random() * 9000 + 1000)::TEXT,
                        airline_rec.id,
                        origin_rec.id,
                        dest_rec.id,
                        flight_date + TIME '08:00:00' + (rand_val)::INTERVAL,
                        flight_date + TIME '10:00:00' + (rand_val)::INTERVAL + (floor(random() * 10)::TEXT || ' minutes')::INTERVAL,
                        CASE floor(random() * 4)::INTEGER
                            WHEN 0 THEN 'Boeing 737'
                            WHEN 1 THEN 'Airbus A320'
                            WHEN 2 THEN 'Boeing 787'
                            ELSE 'Airbus A350'
                        END,
                        CASE floor(random() * 5)::INTEGER
                            WHEN 0 THEN 'Clear'
                            WHEN 1 THEN 'Cloudy'
                            WHEN 2 THEN 'Rain'
                            WHEN 3 THEN 'Snow'
                            ELSE 'Fog'
                        END
                    );
                END LOOP;
            END LOOP;
        END LOOP;
    END LOOP;
END $$;