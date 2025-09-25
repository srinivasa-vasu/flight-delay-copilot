-- CREATE DATABASE "flight_pilot";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

CREATE TABLE IF NOT EXISTS airlines (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(10) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    on_time_performance DECIMAL(5,2),
    total_flights INTEGER,
    delayed_flights INTEGER,
    average_delay DECIMAL(6,2)
);

CREATE INDEX IF NOT EXISTS idx_airlines_on_time_performance ON airlines(on_time_performance);

-- Airports table
CREATE TABLE IF NOT EXISTS  airports (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(10) UNIQUE NOT NULL,
    name VARCHAR(200) NOT NULL,
    city VARCHAR(100),
    country VARCHAR(100),
    timezone VARCHAR(50),
    latitude DECIMAL(10,6),
    longitude DECIMAL(10,6),
    congestion_level INTEGER,
    average_delay DECIMAL(6,2)
);

CREATE INDEX IF NOT EXISTS idx_airports_congestion_level ON airports(congestion_level);

-- Flights table
CREATE TABLE IF NOT EXISTS  flights (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    flight_number VARCHAR(20),
    airline_id UUID REFERENCES airlines(id),
    origin_id UUID REFERENCES airports(id),
    destination_id UUID REFERENCES airports(id),
    scheduled_departure TIMESTAMP,
    actual_departure TIMESTAMP,
    scheduled_arrival TIMESTAMP,
    actual_arrival TIMESTAMP,
    delay_minutes INTEGER default 0,
    status VARCHAR(20),
    delay_reason VARCHAR(500),
    aircraft_type VARCHAR(50),
    weather_condition VARCHAR(100),
    crew_issue BOOLEAN,
    maintenance_issue BOOLEAN,
    gate_delay INTEGER default 0,
    taxi_delay INTEGER default 0
);

CREATE INDEX IF NOT EXISTS idx_flights_origin_dest_departure ON flights(origin_id, destination_id, scheduled_departure);
CREATE INDEX IF NOT EXISTS idx_flights_airline_departure ON flights(airline_id, scheduled_departure);

-- Flight embeddings table
CREATE TABLE IF NOT EXISTS flight_embeddings (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT,
    embedding vector(768),
    document_type VARCHAR(50),
    created_at TIMESTAMP,
    flight_id UUID REFERENCES flights(id),
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_embeddings_vector ON flight_embeddings USING ybhnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_embeddings_flight_id ON flight_embeddings (flight_id);

-- Weather data table
CREATE TABLE IF NOT EXISTS weather_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    airport_id UUID REFERENCES airports(id),
    date_time TIMESTAMP,
    temperature DECIMAL(10,2),
    wind_speed DECIMAL(10,2),
    wind_direction VARCHAR(10),
    visibility DECIMAL(10,2),
    conditions VARCHAR(100),
    description VARCHAR(200),
    severe_weather BOOLEAN,
    precipitation_probability INTEGER
);

CREATE INDEX IF NOT EXISTS idx_weather_airport_time ON weather_data(airport_id, date_time);

-- Crew schedules table
CREATE TABLE IF NOT EXISTS crew_schedules (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    crew_id VARCHAR(50),
    duty_start TIMESTAMP,
    duty_end TIMESTAMP,
    flight_hours INTEGER,
    rest_hours INTEGER,
    base VARCHAR(10),
    available BOOLEAN,
    airline_id UUID REFERENCES airlines(id)
);

CREATE INDEX IF NOT EXISTS idx_crew_schedules_airline_id ON crew_schedules(airline_id);