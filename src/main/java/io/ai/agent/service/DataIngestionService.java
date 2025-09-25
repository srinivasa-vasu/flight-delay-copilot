package io.ai.agent.service;

import io.ai.agent.entity.Airline;
import io.ai.agent.entity.Airport;
import io.ai.agent.entity.Flight;
import io.ai.agent.entity.FlightStatus;
import io.ai.agent.repo.AirlineRepository;
import io.ai.agent.repo.AirportRepository;
import io.ai.agent.repo.FlightRepository;
import java.io.BufferedReader;
import java.io.InputStreamReader;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.scheduling.annotation.Async;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;
import org.springframework.web.multipart.MultipartFile;

@Service
@RequiredArgsConstructor
@Slf4j
public class DataIngestionService {

	private final FlightRepository flightRepository;
	private final AirlineRepository airlineRepository;
	private final AirportRepository airportRepository;
	private final VectorStoreService vectorStoreService;

	@Async
	@Transactional
	public void ingestData(MultipartFile file, String dataType) {
		log.info("Starting data ingestion for type: {}", dataType);

		try {
			switch (dataType.toLowerCase()) {
			case "flights":
				ingestFlightData(file);
				break;
			case "weather":
				ingestWeatherData(file);
				break;
			case "crew":
				ingestCrewData(file);
				break;
			case "documents":
				ingestDocuments(file);
				break;
			default:
				log.error("Unknown data type: {}", dataType);
			}
		}
		catch (Exception e) {
			log.error("Error during data ingestion", e);
		}
	}

	private void ingestFlightData(MultipartFile file) throws Exception {
		BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
		String line;
		boolean isFirstLine = true;

		while ((line = reader.readLine()) != null) {
			if (isFirstLine) {
				isFirstLine = false;
				continue; // Skip header
			}

			String[] parts = line.split(",");
			if (parts.length < 10) continue;

			// Parse flight data
			String flightNumber = parts[0];
			String airlineCode = parts[1];
			String originCode = parts[2];
			String destCode = parts[3];
			LocalDateTime scheduledDep = LocalDateTime.parse(parts[4], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			LocalDateTime actualDep = LocalDateTime.parse(parts[5], DateTimeFormatter.ISO_LOCAL_DATE_TIME);
			int delayMinutes = Integer.parseInt(parts[6]);
			String status = parts[7];
			String delayReason = parts[8];
			String aircraftType = parts[9];

			// Get or create entities
			Airline airline = airlineRepository.findByCode(airlineCode)
					.orElseGet(() -> {
						Airline newAirline = Airline.builder()
								.code(airlineCode)
								.name(airlineCode + " Airlines")
								.build();
						return airlineRepository.save(newAirline);
					});

			Airport origin = airportRepository.findByCode(originCode)
					.orElseGet(() -> {
						Airport newAirport = Airport.builder()
								.code(originCode)
								.name(originCode + " Airport")
								.city(originCode)
								.country("USA")
								.build();
						return airportRepository.save(newAirport);
					});

			Airport destination = airportRepository.findByCode(destCode)
					.orElseGet(() -> {
						Airport newAirport = Airport.builder()
								.code(destCode)
								.name(destCode + " Airport")
								.city(destCode)
								.country("USA")
								.build();
						return airportRepository.save(newAirport);
					});

			// Create flight
			Flight flight = Flight.builder()
					.flightNumber(flightNumber)
					.airline(airline)
					.origin(origin)
					.destination(destination)
					.scheduledDeparture(scheduledDep)
					.actualDeparture(actualDep)
					.delayMinutes(delayMinutes)
					.status(FlightStatus.valueOf(status))
					.delayReason(delayReason)
					.aircraftType(aircraftType)
					.build();

			flightRepository.save(flight);

			// Create embedding for this flight
			createFlightEmbedding(flight);
		}

		log.info("Flight data ingestion completed");
	}

	private void createFlightEmbedding(Flight flight) {
		String content = String.format(
				"Flight %s by %s from %s to %s on %s. Delay: %d minutes. Status: %s. Reason: %s",
				flight.getFlightNumber(),
				flight.getAirline().getName(),
				flight.getOrigin().getCode(),
				flight.getDestination().getCode(),
				flight.getScheduledDeparture(),
				flight.getDelayMinutes(),
				flight.getStatus(),
				flight.getDelayReason()
		);

		// Add to vector store
		vectorStoreService.addDocuments(
				List.of(content),
				Map.of(
						"type", "flight",
						"flightId", flight.getId(),
						"airline", flight.getAirline().getCode(),
						"route", flight.getOrigin().getCode() + "-" + flight.getDestination().getCode()
				)
		);
	}

	@Async
	public void processEmbeddings() {
		log.info("Starting embedding processing");

		try {
			// Process historical patterns
			processHistoricalPatterns();

			// Process route-specific patterns
			processRoutePatterns();

			// Process airline performance patterns
			processAirlinePatterns();

			log.info("Embedding processing completed");
		}
		catch (Exception e) {
			log.error("Error processing embeddings", e);
		}
	}

	private void processHistoricalPatterns() {
		// Analyze patterns by time of day
		Map<Integer, List<Flight>> flightsByHour = flightRepository.findAll().stream()
				.collect(Collectors.groupingBy(f -> f.getScheduledDeparture().getHour()));

		for (Map.Entry<Integer, List<Flight>> entry : flightsByHour.entrySet()) {
			int hour = entry.getKey();
			List<Flight> flights = entry.getValue();

			double avgDelay = flights.stream()
					.mapToInt(Flight::getDelayMinutes)
					.average()
					.orElse(0.0);

			String pattern = String.format(
					"Flights departing at %d:00 hours have an average delay of %.1f minutes based on %d flights. " +
							"Peak delay reasons: %s",
					hour, avgDelay, flights.size(),
					getTopDelayReasons(flights)
			);

			vectorStoreService.addDocuments(
					List.of(pattern),
					Map.of("type", "pattern", "category", "hourly", "hour", hour)
			);
		}
	}

	private void processRoutePatterns() {
		// Get all unique routes
		List<Object[]> routes = flightRepository.findAll().stream()
				.map(f -> new Object[] {f.getOrigin().getCode(), f.getDestination().getCode()})
				.distinct()
				.toList();

		for (Object[] route : routes) {
			String origin = (String) route[0];
			String destination = (String) route[1];

			List<Flight> routeFlights = flightRepository.findByRoute(
					origin, destination,
					LocalDateTime.now().minusYears(1),
					LocalDateTime.now()
			);

			if (routeFlights.isEmpty()) continue;

			double avgDelay = routeFlights.stream()
					.mapToInt(Flight::getDelayMinutes)
					.average()
					.orElse(0.0);

			long cancelledCount = routeFlights.stream()
					.filter(f -> f.getStatus() == FlightStatus.CANCELLED)
					.count();

			String pattern = String.format(
					"Route %s to %s: Average delay %.1f minutes, %.1f%% cancellation rate, %d total flights. " +
							"Best performing airline: %s. Common issues: %s",
					origin, destination, avgDelay,
					(cancelledCount * 100.0) / routeFlights.size(),
					routeFlights.size(),
					getBestAirlineForRoute(routeFlights),
					getTopDelayReasons(routeFlights)
			);

			vectorStoreService.addDocuments(
					List.of(pattern),
					Map.of("type", "pattern", "category", "route", "route", origin + "-" + destination)
			);
		}
	}

	private void processAirlinePatterns() {
		List<Airline> airlines = airlineRepository.findAll();

		for (Airline airline : airlines) {
			List<Flight> airlineFlights = flightRepository.findByAirlineAndDateRange(
					airline.getCode(),
					LocalDateTime.now().minusYears(1),
					LocalDateTime.now()
			);

			if (airlineFlights.isEmpty()) continue;

			double avgDelay = airlineFlights.stream()
					.mapToInt(Flight::getDelayMinutes)
					.average()
					.orElse(0.0);

			long onTimeCount = airlineFlights.stream()
					.filter(f -> f.getDelayMinutes() <= 15)
					.count();

			// Update airline statistics
			airline.setAverageDelay(avgDelay);
			airline.setOnTimePerformance((onTimeCount * 100.0) / airlineFlights.size());
			airline.setTotalFlights(airlineFlights.size());
			airline.setDelayedFlights((int) (airlineFlights.size() - onTimeCount));
			airlineRepository.save(airline);

			String pattern = String.format(
					"%s airline performance: %.1f%% on-time, average delay %.1f minutes. " +
							"Total flights: %d. Common delay reasons: %s. " +
							"Best performing routes: %s",
					airline.getName(),
					airline.getOnTimePerformance(),
					avgDelay,
					airlineFlights.size(),
					getTopDelayReasons(airlineFlights),
					getTopRoutesForAirline(airlineFlights)
			);

			vectorStoreService.addDocuments(
					List.of(pattern),
					Map.of("type", "pattern", "category", "airline", "airline", airline.getCode())
			);
		}
	}

	private String getTopDelayReasons(List<Flight> flights) {
		return flights.stream()
				.filter(f -> f.getDelayReason() != null && !f.getDelayReason().isEmpty())
				.collect(Collectors.groupingBy(Flight::getDelayReason, Collectors.counting()))
				.entrySet().stream()
				.sorted(Map.Entry.<String, Long>comparingByValue().reversed())
				.limit(3)
				.map(Map.Entry::getKey)
				.collect(Collectors.joining(", "));
	}

	private String getBestAirlineForRoute(List<Flight> flights) {
		return flights.stream()
				.collect(Collectors.groupingBy(f -> f.getAirline().getName(),
						Collectors.averagingInt(Flight::getDelayMinutes)))
				.entrySet().stream()
				.min(Map.Entry.comparingByValue())
				.map(Map.Entry::getKey)
				.orElse("Unknown");
	}

	private String getTopRoutesForAirline(List<Flight> flights) {
		return flights.stream()
				.collect(Collectors.groupingBy(
						f -> f.getOrigin().getCode() + "-" + f.getDestination().getCode(),
						Collectors.averagingInt(Flight::getDelayMinutes)))
				.entrySet().stream()
				.sorted(Map.Entry.comparingByValue())
				.limit(3)
				.map(Map.Entry::getKey)
				.collect(Collectors.joining(", "));
	}

	private void ingestWeatherData(MultipartFile file) throws Exception {
		// Implementation for weather data ingestion
		log.info("Weather data ingestion not yet implemented");
	}

	private void ingestCrewData(MultipartFile file) throws Exception {
		// Implementation for crew data ingestion
		log.info("Crew data ingestion not yet implemented");
	}

	private void ingestDocuments(MultipartFile file) throws Exception {
		// Process text documents for RAG
		BufferedReader reader = new BufferedReader(new InputStreamReader(file.getInputStream()));
		StringBuilder content = new StringBuilder();
		String line;

		while ((line = reader.readLine()) != null) {
			content.append(line).append("\n");
		}

		// Split into chunks and add to vector store
		String fullContent = content.toString();
		List<String> chunks = splitIntoChunks(fullContent);

		vectorStoreService.addDocuments(
				chunks,
				Map.of("type", "document", "source", Objects.requireNonNull(file.getOriginalFilename()))
		);

		log.info("Document ingestion completed: {}", file.getOriginalFilename());
	}

	private List<String> splitIntoChunks(String text) {
		List<String> chunks = new ArrayList<>();
		String[] sentences = text.split("\\. ");
		StringBuilder currentChunk = new StringBuilder();

		for (String sentence : sentences) {
			if (currentChunk.length() + sentence.length() > 500 && !currentChunk.isEmpty()) {
				chunks.add(currentChunk.toString());
				currentChunk = new StringBuilder();
			}
			currentChunk.append(sentence).append(". ");
		}

		if (!currentChunk.isEmpty()) {
			chunks.add(currentChunk.toString());
		}

		return chunks;
	}
}
