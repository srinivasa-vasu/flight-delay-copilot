package io.ai.agent.record;

public record FlightAuxStats(double avgDelay, double onTimePercentage, double cancelRate, String pastIncidents,
							 int totalFlights) {
}
