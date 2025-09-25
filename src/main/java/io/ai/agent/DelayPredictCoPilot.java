package io.ai.agent;

import com.embabel.agent.api.annotation.AchievesGoal;
import com.embabel.agent.api.annotation.Action;
import com.embabel.agent.api.annotation.Agent;
import com.embabel.agent.api.common.OperationContext;
import com.embabel.agent.domain.io.UserInput;
import com.embabel.agent.prompt.persona.Persona;
import com.fasterxml.jackson.databind.ObjectMapper;
import io.ai.agent.config.ConfigProps;
import io.ai.agent.entity.Airline;
import io.ai.agent.entity.Flight;
import io.ai.agent.entity.FlightStatus;
import io.ai.agent.record.AirlinePrediction;
import io.ai.agent.record.CrewAnalysis;
import io.ai.agent.record.Critique;
import io.ai.agent.record.FlightAuxStats;
import io.ai.agent.record.FlightDocs;
import io.ai.agent.record.FlightSchedule;
import io.ai.agent.record.PredictionDetails;
import io.ai.agent.record.PredictionRequest;
import io.ai.agent.record.PredictionResponse;
import io.ai.agent.record.ScoredDocument;
import io.ai.agent.record.ScoredDocument.Relevance;
import io.ai.agent.record.WeatherAnalysis;
import io.ai.agent.repo.FlightRepository;
import io.ai.agent.service.CrewService;
import io.ai.agent.service.StatsPredictionService;
import io.ai.agent.service.VectorStoreService;
import io.ai.agent.service.WeatherService;
import java.io.IOException;
import java.text.MessageFormat;
import java.time.LocalDateTime;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Map;
import java.util.StringJoiner;
import java.util.stream.Collector;
import java.util.stream.Collectors;
import java.util.stream.Stream;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.filter.Filter;

@RequiredArgsConstructor
@Slf4j
@Agent(
		name = "delay-predict-copilot",
		provider = "embabel",
		version = "1.0.0",
		description = """
				Get the list of flights for the given source and destination and order them based on on-time departure and arrival.
				Predict the delays based on the historical flights and other contributing factors."""
)
public class DelayPredictCoPilot {

	private final static Persona VIMANA_ANALYST = new Persona(
			"vimana-analyst",
			"you are a flight delay and disruption analyst",
			"professional yet approachable, uses clear explanations",
			"your task is provide a comprehensive analysis of expected flight delays and disruptions based on the given flight data and other parameters");

	private final static Persona VIMANA_LEAD_ANALYST = new Persona(
			"vimana-lead-analyst",
			"you are the lead flight analyst. Your role is to be the final judge.",
			"professional yet approachable, uses clear explanations",
			"you have an initial prediction from your analyst and a critique from your skeptic. Your task is to synthesize both inputs to produce a final, balanced, and realistic prediction.");

	private final static Persona VIMANA_SKEPTIC = new Persona(
			"vimana-skeptic",
			"you are a skeptical flight risk analyst. Your job is to find flaws and inconsistencies in predictions.",
			"pessimistic and questioning",
			"challenge the given prediction. Find reasons why it might be wrong. Be specific and data-driven in your critique.");

	private static final String BEST_LLM = "best";
	private static final String LITE_LLM = "lite";
	private static final String FLIGHT_PATTERN = "Flights departing on %s between 00:00 and 23:59 hours with delays considering weather, crew, and aircraft factors";
	private static final String ROUTE_PATTERN = "Route %s to %s: with delays and cancellations";
	private final FlightRepository flightRepository;
	private final VectorStoreService vectorStoreService;
	private final StatsPredictionService statsPredictionService;
	private final WeatherService weatherService;
	private final CrewService crewService;
	private final ConfigProps props;
	private final ObjectMapper objectMapper = new ObjectMapper();

	@Action
	public PredictionRequest extractRequest(UserInput userInput, OperationContext context) {
		return context.ai().withDefaultLlm().createObjectIfPossible(
				"Extract origin, destination airports and travel date:%s".formatted(userInput.getContent()),
				PredictionRequest.class
		);
	}


	@Action(description = "Get supporting documents related to route and flight performance")
	public FlightDocs buildContext(PredictionRequest request) {
		return FlightDocs.builder().documents(Stream.of(vectorStoreService.searchSimilarDocuments(
						String.format(FLIGHT_PATTERN,
								request.travelDate().getDayOfWeek())
				), vectorStoreService.searchSimilarDocuments(
						String.format(ROUTE_PATTERN, request.origin(), request.destination()),
						new Filter.Expression(Filter.ExpressionType.EQ, new Filter.Key("route"), new Filter.Value(request.origin() + "-" + request.destination()))))
				.flatMap(List::stream).toList()).build();

	}

	@Action(description = "Retrieve optimized supporting docs")
	private FlightDocs optimizeContext(FlightDocs docs, PredictionRequest request, OperationContext ctx) {
		if (docs.documents().isEmpty()) {
			return docs;
		}

		List<ScoredDocument> scoredDocuments = docs.documents().parallelStream().map(doc -> {
			String prompt = String.format("On a scale of 1 to 10, how relevant is this document for predicting delays for a flight from %s to %s on %s?\n\nDOCUMENT:\n%s",
					request.origin(), request.destination(), request.travelDate(), doc.getText());

			try {
				var relevance = ctx.ai().withAutoLlm().createObjectIfPossible(prompt, Relevance.class);
				return new ScoredDocument(doc, relevance != null ? relevance.score() : 0);
			}
			catch (Exception e) {
				log.warn("Failed to get relevance score for document: {}", e.getMessage());
				return new ScoredDocument(doc, 0); // Score 0 if LLM fails
			}
		}).toList();

		// Sort by score descending
		List<ScoredDocument> sortedDocuments = new ArrayList<>(scoredDocuments);
		sortedDocuments.sort((a, b) -> Integer.compare(b.score(), a.score()));

		// Return the top N documents, making it configurable would be a good improvement
		return FlightDocs.builder().documents(sortedDocuments.stream()
				.limit(props.getVector().getRankedK())
				.map(ScoredDocument::document)
				.collect(Collectors.toList())).build();
	}

	@Action(description = "Get weather data")
	public WeatherAnalysis weatherAnalysis(PredictionRequest request, OperationContext context) {
		String weatherData = MessageFormat.format("{0}\n{1}",
				weatherService.getWeatherForecast(request.origin(), request.travelDate()),
				weatherService.getWeatherForecast(request.destination(), request.travelDate()));
		try {
			return context.ai().withAutoLlm().createObjectIfPossible(String.format(props.getPrompt()
					.getWeatherPrompt(), request.origin(), request.destination(), request.travelDate(), weatherData), WeatherAnalysis.class);
		}
		catch (Exception e) {
			log.error("Error getting weather data", e);
			return WeatherAnalysis.builder().build();
		}
	}

	@Action(description = "Get flight delay probabilities", pre = "hasRun_io.ai.agent.DelayPredictCoPilot.optimizeContext")
	@AchievesGoal(description = "Flight delay probabilities are predicted.")
	public PredictionResponse predictFlightDelay(PredictionRequest request, FlightDocs rogDocs, WeatherAnalysis weatherAnalysis, OperationContext ctx) throws IOException {
		log.info("Predicting delays for route: {} to {}", request.origin(), request.destination());

		// Fetch relevant historical data
		List<Flight> historicalFlights = flightRepository.findByRoute(
				request.origin(),
				request.destination(),
				request.travelDate().toLocalDate().minusMonths(props.getHistoryPeriodMonths()).atStartOfDay(),
				request.travelDate().toLocalDate().minusDays(1).atTime(23, 59, 59)
		);

		// Fetch flight schedule for the given day
		List<Flight> scheduledFlights = flightRepository.findByRouteAndDepartureDate(
				request.origin(),
				request.destination(),
				request.travelDate().toLocalDate().atStartOfDay(),
				request.travelDate().toLocalDate().atTime(23, 59, 59)
		);

		String ragContext = rogDocs.documents().stream()
				.map(Document::getText)
				.collect(Collectors.joining("\n"));

		// Prepare data for each airline
		List<AirlinePrediction> predictions = new ArrayList<>();
		Map<Airline, List<Flight>> flightsByAirline = historicalFlights.stream()
				.collect(Collectors.groupingBy(Flight::getAirline));

		Map<Airline, List<Flight>> scheduleByAirline = scheduledFlights.stream()
				.collect(Collectors.groupingBy(Flight::getAirline));

		flightsByAirline.forEach((airline, airlineFlights) -> {
			// 0. Get supporting flight stats
			FlightAuxStats flightAuxStats = flightAuxStats(airlineFlights);
			List<FlightSchedule> schedule = scheduleByAirline.getOrDefault(airline, Collections.emptyList()).stream()
					.map(f -> new FlightSchedule(f.getFlightNumber(), f.getScheduledDeparture(), f.getScheduledArrival(), f.getAircraftType()))
					.toList();

			// 1. Initial analysis by the Analyst
			PredictionDetails initialDetails = initialPrediction(request, airline, flightAuxStats, weatherAnalysis, ragContext, ctx);
			// 2. Skeptic critiques the analysis
			Critique critique = reviewPrediction(initialDetails, request, flightAuxStats, weatherAnalysis, ragContext, ctx);
			// 3. Judge produces the final prediction based on the critique
			PredictionDetails finalDetails = finalPrediction(initialDetails, critique, ctx);
			predictions.add(finalDetails.toAirlinePrediction(schedule));
		});

		// Sort by confidence score
		predictions.sort((a, b) -> {
			double scoreA = (100 - a.delayProbability()) * a.confidenceScore() / 100;
			double scoreB = (100 - b.delayProbability()) * b.confidenceScore() / 100;
			return Double.compare(scoreB, scoreA);
		});

		return PredictionResponse.builder()
				.origin(request.origin())
				.destination(request.destination())
				.travelDate(request.travelDate())
				.predictions(predictions)
				.generatedAt(LocalDateTime.now())
				.build();
	}

	private PredictionDetails initialPrediction(PredictionRequest request, Airline airline, FlightAuxStats flightAuxStats,
			WeatherAnalysis weatherAnalysis, String ragContext, OperationContext ctx) {
		String prompt = String.format(props.getPrompt().getAnalystPrompt(), request.origin(), request.destination(),
				airline.getId(), airline.getCode(), airline.getName(),
				request.travelDate(), request.travelDate().getDayOfWeek(),
				flightAuxStats.avgDelay(), flightAuxStats.onTimePercentage(), flightAuxStats.totalFlights(),
				flightAuxStats.cancelRate(), ragContext, weatherAnalysis, flightAuxStats.pastIncidents());

		try {
			PredictionDetails details = ctx.ai().withAutoLlm()
					.withSystemPrompt(VIMANA_ANALYST.contribution())
					.withToolObjects(crewService) // optional, this can be pre-computed as well
					.createObjectIfPossible(prompt, PredictionDetails.class);

			if (details != null) {
				return details;
			}
			log.warn("Primary prediction failed, falling back to statistical model.");
		}
		catch (Exception e) {
			log.error("Error getting LLM prediction, falling back to statistical model", e);
		}

		// Fallback to statistical prediction
		CrewAnalysis crewAnalysis = crewService.crewAvailability(airline, request.travelDate());
		return statsPredictionService.createStatisticalPrediction(airline, flightAuxStats, weatherAnalysis, crewAnalysis);
	}

	private Critique reviewPrediction(PredictionDetails initialDetails, PredictionRequest request, FlightAuxStats flightAuxStats,
			WeatherAnalysis weatherAnalysis, String ragContext, OperationContext ctx) {
		try {
			String predictionJson = objectMapper.writeValueAsString(initialDetails);
			String critiquePrompt = String.format(props.getPrompt().getCritiquePrompt(),
					request.origin(), request.destination(),
					flightAuxStats.avgDelay(), flightAuxStats.onTimePercentage(), flightAuxStats.cancelRate(),
					ragContext, weatherAnalysis, flightAuxStats.pastIncidents(), predictionJson);

			Critique critique = ctx.ai().withAutoLlm()
					.withSystemPrompt(VIMANA_SKEPTIC.contribution())
					.createObjectIfPossible(critiquePrompt, Critique.class);

			if (critique != null) {
				log.info("Received critique: {}", critique);
				return critique;
			}
		}
		catch (Exception e) {
			log.error("Error during critique step.", e);
		}
		return new Critique(true, List.of(), "Critique agent failed to respond.");
	}

	private PredictionDetails finalPrediction(PredictionDetails initialDetails, Critique critique, OperationContext ctx) {
		if (critique.consistent()) {
			log.info("Skeptic found the prediction to be consistent. No changes made.");
			return initialDetails;
		}

		try {
			String initialJson = objectMapper.writeValueAsString(initialDetails);
			String critiqueJson = objectMapper.writeValueAsString(critique);

			String finalPrompt = String.format(props.getPrompt().getLeadAnalystPrompt(), initialJson, critiqueJson);

			PredictionDetails finalDetails = ctx.ai().withAutoLlm()
					.withSystemPrompt(VIMANA_LEAD_ANALYST.contribution())
					.createObjectIfPossible(finalPrompt, PredictionDetails.class);

			if (finalDetails != null) {
				log.info("Final prediction generated after review.");
				return finalDetails;
			}
			log.warn("Judge agent failed to produce a final prediction. Returning initial prediction.");
		}
		catch (Exception e) {
			log.error("Error during final prediction step.", e);
		}

		return initialDetails;
	}

	private String recentIncidents(List<Flight> flights) {
		CharSequence delay = flights.stream()
				.filter(f -> f.getDelayMinutes() > props.getDelayMinutes() || f.getStatus() == FlightStatus.DELAYED)
				.sorted((a, b) -> b.getScheduledDeparture().compareTo(a.getScheduledDeparture()))
				.limit(props.getDelayLimit())
				.map(f -> String.format("Departure date: %s,  Status: %s,  Delay: %d (min delay), Day Of week: %s",
						f.getScheduledDeparture().toLocalDate(),
						f.getStatus(),
						f.getDelayMinutes(),
						f.getScheduledDeparture().getDayOfWeek()))
				.collect(Collectors.joining("; "));
		CharSequence cancel = flights.stream()
				.filter(f -> (f.getStatus() == (FlightStatus.DIVERTED) || (f.getStatus() == FlightStatus.CANCELLED)))
				.map(f -> String.format("Departure Date: %s, Status: %s:,  Day of week: %s:",
						f.getScheduledDeparture().toLocalDate(),
						f.getStatus(),
						f.getScheduledDeparture().getDayOfWeek()))
				.collect(Collectors.joining("; "));

		return new StringJoiner("\n").add(delay).add(cancel).toString();
	}

	private FlightAuxStats flightAuxStats(List<Flight> flights) {
		return flights.stream()
				.collect(
						Collector.of(
								() -> new Object() {
									double sumDelay = 0;
									int count = 0;
									int onTimeCount = 0;
									int cancelledCount = 0;
								},
								(acc, f) -> {
									acc.sumDelay += f.getDelayMinutes();
									acc.count++;
									if (f.getDelayMinutes() <= props.getAcceptableDelayMinutes()) {
										acc.onTimeCount++;
									}
									if (f.getStatus() == FlightStatus.CANCELLED) {
										acc.cancelledCount++;
									}
								},
								(acc1, acc2) -> {
									acc1.sumDelay += acc2.sumDelay;
									acc1.count += acc2.count;
									acc1.onTimeCount += acc2.onTimeCount;
									acc1.cancelledCount += acc2.cancelledCount;
									return acc1;
								},
								acc -> {
									double avgDelay = acc.count == 0 ? 0.0 : acc.sumDelay / acc.count;
									double onTimePercentage = acc.count == 0 ? 0.0 : (acc.onTimeCount * 100.0) / acc.count;
									double cancelRate = acc.count == 0 ? 0.0 : (acc.cancelledCount * 100.0) / acc.count;
									return new FlightAuxStats(avgDelay, onTimePercentage, cancelRate, recentIncidents(flights), acc.count);
								}
						)
				);
	}

}
