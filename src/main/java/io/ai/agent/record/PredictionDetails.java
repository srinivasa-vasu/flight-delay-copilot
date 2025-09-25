package io.ai.agent.record;

import java.util.List;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

@Builder
@Jacksonized
public record PredictionDetails(String airlineCode, String airlineName, Double delayProbability,
								Integer expectedDelay, List<String> riskFactors, Double confidenceScore,
								String recommendation, Double historicalAvgDelay, Double onTimePercentage,
								Double cancellationPercentage, int totalFlights) {

	public AirlinePrediction toAirlinePrediction(List<FlightSchedule> schedule) {
		return AirlinePrediction.builder()
				.airlineCode(airlineCode)
				.airlineName(airlineName)
				.delayProbability(delayProbability)
				.expectedDelayMinutes(expectedDelay)
				.confidenceScore(confidenceScore)
				.riskFactors(riskFactors)
				.recommendation(recommendation)
				.historicalPerformance(HistoricalPerformance.builder()
						.averageDelay(historicalAvgDelay)
						.onTimePercentage(onTimePercentage)
						.cancellationRate(cancellationPercentage)
						.totalFlights(totalFlights)
						.build())
				.schedule(schedule)
				.build();
	}
}
