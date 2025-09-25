package io.ai.agent.service;

import io.ai.agent.config.ConfigProps;
import io.ai.agent.record.FlightHistoricalData;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;

import org.springframework.ai.document.Document;
import org.springframework.ai.vectorstore.SearchRequest;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.filter.Filter.Expression;
import org.springframework.stereotype.Service;

@Service
@RequiredArgsConstructor
@Slf4j
public class VectorStoreService {

	private final VectorStore vectorStore;
	private final ConfigProps props;

	public void addDocuments(List<String> contents, Map<String, Object> metadata) {
		List<Document> documents = contents.stream()
				.map(content -> new Document(content, metadata))
				.collect(Collectors.toList());

		vectorStore.add(documents);
		log.info("Added {} documents to vector store", documents.size());
	}

	public List<Document> searchSimilarDocuments(String query) {
		SearchRequest searchRequest = SearchRequest.builder()
				.query(query)
				.topK(props.getVector().getTopK())
				.similarityThreshold(props.getVector().getSimilarityThreshold())
				.build();
		return vectorStore.similaritySearch(searchRequest);
	}

	public List<Document> searchSimilarDocuments(String query, Expression expression) {
		SearchRequest searchRequest = SearchRequest.builder()
				.query(query)
				.topK(props.getVector().getTopK())
				.similarityThreshold(props.getVector().getSimilarityThreshold())
				.filterExpression(expression)
				.build();
		return vectorStore.similaritySearch(searchRequest);
	}

	public void processAndStoreFlightData(List<FlightHistoricalData> data) {
		List<String> documents = data.stream()
				.map(this::createFlightDocument)
				.collect(Collectors.toList());

		Map<String, Object> metadata = Map.of(
				"type", "flight_historical",
				"processed_at", System.currentTimeMillis()
		);

		addDocuments(documents, metadata);
	}

	private String createFlightDocument(FlightHistoricalData data) {
		return String.format("""
						Flight %s operated by %s from %s to %s on %s.
						Scheduled departure: %s, Actual departure: %s.
						Delay: %d minutes. Status: %s.
						Aircraft: %s. Weather: %s.
						Delay reason: %s.
						Historical pattern: %s
						""",
				data.flightNumber(), data.airline(),
				data.origin(), data.destination(), data.date(),
				data.scheduledTime(), data.actualTime(),
				data.delayMinutes(), data.status(),
				data.aircraftType(), data.weatherCondition(),
				data.delayReason(), data.historicalPattern()
		);
	}
}
