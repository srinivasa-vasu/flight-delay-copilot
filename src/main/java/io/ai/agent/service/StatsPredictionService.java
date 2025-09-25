package io.ai.agent.service;

import io.ai.agent.config.ConfigProps;
import io.ai.agent.entity.Airline;
import io.ai.agent.record.CrewAnalysis;
import io.ai.agent.record.FlightAuxStats;
import io.ai.agent.record.PredictionDetails;
import io.ai.agent.record.WeatherAnalysis;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.List;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class StatsPredictionService {

	private final ConfigProps props;

	public PredictionDetails createStatisticalPrediction(Airline airline, FlightAuxStats flightAuxStats,
			WeatherAnalysis weatherAnalysis, CrewAnalysis crewAnalysis) {
		double delayProbability = 100 - flightAuxStats.onTimePercentage();
		int expectedDelay = (int) Math.round(flightAuxStats.avgDelay());

		List<String> riskFactors = new ArrayList<>(Arrays.asList(
				"Historical performance",
				"Statistical analysis",
				"Route complexity"
		));

		if (weatherAnalysis != null && weatherAnalysis.isSevereWeather()) {
			delayProbability += 10; // Increase delay probability by 10% for severe weather
			riskFactors.add("Severe weather conditions");
		}

		if (crewAnalysis != null && !crewAnalysis.crewAvailable()) {
			delayProbability += 15; // Increase delay probability by 15% for crew unavailability
			riskFactors.add("Potential crew scheduling issues");
		}


		String recommendation = generateRecommendation(delayProbability);

		return PredictionDetails.builder()
				.airlineCode(airline.getCode())
				.airlineName(airline.getName())
				.delayProbability(delayProbability)
				.expectedDelay(expectedDelay)
				.riskFactors(riskFactors)
				.cancellationPercentage(flightAuxStats.cancelRate())
				.confidenceScore(props.getConfidenceScore()) // Default confidence for statistical predictions
				.recommendation(recommendation)
				.historicalAvgDelay(flightAuxStats.avgDelay())
				.onTimePercentage(flightAuxStats.onTimePercentage())
				.build();
	}

	private String generateRecommendation(double delayProbability) {
		if (delayProbability < 20) {
			return "Excellent choice with minimal delay risk. This airline shows strong on-time performance.";
		}
		else if (delayProbability < 35) {
			return "Good option with moderate delay risk. Consider arriving at the airport with extra time.";
		}
		else if (delayProbability < 50) {
			return "Higher delay risk observed. Consider alternative flights or build in buffer time for connections.";
		}
		else {
			return "Significant delay risk. Strongly consider alternative options or prepare for potential delays.";
		}
	}
}
