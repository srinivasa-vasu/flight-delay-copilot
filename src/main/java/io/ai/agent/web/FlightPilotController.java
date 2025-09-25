package io.ai.agent.web;

import com.embabel.agent.api.common.autonomy.AgentInvocation;
import com.embabel.agent.core.AgentPlatform;
import io.ai.agent.DelayPredictCoPilot;
import io.ai.agent.record.AirlineRecord;
import io.ai.agent.record.AirportRecord;
import io.ai.agent.record.PredictionRequest;
import io.ai.agent.record.PredictionResponse;
import io.ai.agent.service.DataIngestionService;
import io.ai.agent.service.ReferenceDataService;
import java.util.List;
import java.util.Map;
import lombok.RequiredArgsConstructor;

import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestMapping;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.multipart.MultipartFile;

@RestController
@RequestMapping("/api/v1")
@RequiredArgsConstructor
public class FlightPilotController {

	private final DelayPredictCoPilot delayPredictCoPilot;
	private final DataIngestionService dataIngestionService;
	private final AgentPlatform agentPlatform;
	private final ReferenceDataService referenceDataService;

	@PostMapping("/predict")
	public ResponseEntity<PredictionResponse> predictDelays(@RequestBody PredictionRequest request) {
		AgentInvocation<PredictionResponse> agentCall = AgentInvocation.builder(agentPlatform)
				.options(option -> option.verbosity(v -> v.showPrompts(true).showPlanning(true).showLlmResponses(true)))
				.build(PredictionResponse.class);
		return ResponseEntity.ok(agentCall.invoke(request));
	}

	@GetMapping("/airports")
	public ResponseEntity<List<AirportRecord>> getAllAirports() {
		return ResponseEntity.ok(referenceDataService.getAllAirports());
	}

	@GetMapping("/airlines")
	public ResponseEntity<List<AirlineRecord>> getAllAirlines() {
		return ResponseEntity.ok(referenceDataService.getAllAirlines());
	}

	@PostMapping("/data/ingest")
	public ResponseEntity<Map<String, String>> ingestHistoricalData(@RequestParam("file") MultipartFile file,
			@RequestParam("type") String dataType) {
		dataIngestionService.ingestData(file, dataType);
		return ResponseEntity.ok(Map.of("status", "Data ingestion started"));
	}

	@PostMapping("/data/process-embeddings")
	public ResponseEntity<Map<String, String>> processEmbeddings() {
		dataIngestionService.processEmbeddings();
		return ResponseEntity.ok(Map.of("status", "Embedding processing started"));
	}

	@GetMapping("/")
	public String index() {
		return "index";
	}

}
