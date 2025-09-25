package io.ai.agent.record;

import java.util.List;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record AirlinePrediction(String airlineCode, String airlineName, Double delayProbability,
								Integer expectedDelayMinutes, Double confidenceScore, List<String> riskFactors,
								String recommendation, HistoricalPerformance historicalPerformance,
								List<FlightSchedule> schedule) {
}
