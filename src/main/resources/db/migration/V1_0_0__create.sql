-- CREATE DATABASE "flight_pilot";
CREATE EXTENSION IF NOT EXISTS "uuid-ossp";
CREATE EXTENSION IF NOT EXISTS "vector";

CREATE TABLE IF NOT EXISTS airline (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    code VARCHAR(10) UNIQUE NOT NULL,
    name VARCHAR(100) NOT NULL,
    on_time_performance DECIMAL(5,2),
    total_flight INTEGER,
    delayed_flight INTEGER,
    average_delay DECIMAL(6,2)
);

CREATE INDEX IF NOT EXISTS idx_airline_on_time_performance ON airline(on_time_performance);

-- airport table
CREATE TABLE IF NOT EXISTS  airport (
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

CREATE INDEX IF NOT EXISTS idx_airport_congestion_level ON airport(congestion_level);

-- flight table
CREATE TABLE IF NOT EXISTS  flight (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    flight_number VARCHAR(20),
    airline_id UUID REFERENCES airline(id),
    origin_id UUID REFERENCES airport(id),
    destination_id UUID REFERENCES airport(id),
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

CREATE INDEX IF NOT EXISTS idx_flight_origin_dest_departure ON flight(origin_id, destination_id, scheduled_departure);
CREATE INDEX IF NOT EXISTS idx_flight_airline_departure ON flight(airline_id, scheduled_departure);

-- Flight embeddings table
CREATE TABLE IF NOT EXISTS flight_embedding (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    content TEXT,
    embedding vector(768),
    document_type VARCHAR(50),
    created_at TIMESTAMP,
    flight_id UUID REFERENCES flight(id),
    metadata JSONB
);

CREATE INDEX IF NOT EXISTS idx_embeddings_vector ON flight_embedding USING ybhnsw (embedding vector_cosine_ops);
CREATE INDEX IF NOT EXISTS idx_embeddings_flight_id ON flight_embedding (flight_id);

-- Weather data table
CREATE TABLE IF NOT EXISTS weather_data (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    airport_id UUID REFERENCES airport(id),
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
CREATE TABLE IF NOT EXISTS crew_schedule (
    id UUID PRIMARY KEY DEFAULT uuid_generate_v4(),
    crew_id VARCHAR(50),
    duty_start TIMESTAMP,
    duty_end TIMESTAMP,
    flight_hours INTEGER,
    rest_hours INTEGER,
    base VARCHAR(10),
    available BOOLEAN,
    airline_id UUID REFERENCES airline(id)
);

CREATE INDEX IF NOT EXISTS idx_crew_schedule_airline_id ON crew_schedule(airline_id);