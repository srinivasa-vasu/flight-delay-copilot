package io.ai.agent.record;

import java.util.List;
import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import org.springframework.ai.document.Document;

@Builder
@Jacksonized
public record FlightDocs(List<Document> documents) {

}
