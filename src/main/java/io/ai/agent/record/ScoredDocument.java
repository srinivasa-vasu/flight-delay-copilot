package io.ai.agent.record;

import lombok.Builder;
import lombok.extern.jackson.Jacksonized;

import org.springframework.ai.document.Document;

@Builder
@Jacksonized
public record ScoredDocument(Document document, int score) {

	public record Relevance(int score) {
	}
}
