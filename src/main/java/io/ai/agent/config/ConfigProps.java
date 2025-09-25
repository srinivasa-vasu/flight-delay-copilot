package io.ai.agent.config;

import io.micrometer.core.instrument.util.IOUtils;
import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.ClassPathResource;
import org.springframework.stereotype.Component;
import org.springframework.util.Assert;

@ConfigurationProperties(prefix = "prediction")
@Component
@Data
public class ConfigProps {

	private int retryAttempts = 3;
	private int historyPeriodMonths = 1;
	private int delayLimit;
	private double confidenceScore;
	private int delayMinutes;
	private int acceptableDelayMinutes;
	private Vector vector;
	private Prompt prompt;

	@PostConstruct
	void loadPrompts() throws IOException {
		String[] resources = prompt.getResources();
		for (String resource : resources) {
			String content = IOUtils.toString(new ClassPathResource(resource).getInputStream(), Charset.defaultCharset());
			if (resource.equals(prompt.getAnalystPath())) {
				prompt.setAnalystPrompt(content);
			}
			else if (resource.equals(prompt.getWeatherPath())) {
				prompt.setWeatherPrompt(content);
			}
			else if (resource.equals(prompt.leadAnalystPath)) {
				prompt.setLeadAnalystPrompt(content);
			}
			else if (resource.equals(prompt.getCritiquePath())) {
				prompt.setCritiquePrompt(content);
			}
			else if (resource.equals(prompt.rankedKPath)) {
				prompt.setRankedKPrompt(content);
			}
			else {
				// do nothing
			}
		}
	}

	@PostConstruct
	void validate() {
		Assert.isTrue(vector.rankedK <= vector.topK, "vector.rankedK must be less than or equal to vector.topK");
	}

	@Data
	public static class Vector {
		private String schema;
		private String table;
		private int batchSize;
		private int dimension;
		private double similarityThreshold;
		private int topK;
		private int rankedK;
	}


	@Data
	public static class Prompt {

		@Setter(AccessLevel.PRIVATE)
		private String analystPrompt;
		@Setter(AccessLevel.PRIVATE)
		private String leadAnalystPrompt;
		@Setter(AccessLevel.PRIVATE)
		private String critiquePrompt;
		@Setter(AccessLevel.PRIVATE)
		private String weatherPrompt;
		@Setter(AccessLevel.PRIVATE)
		private String rankedKPrompt;
		private String analystPath;
		private String leadAnalystPath;
		private String critiquePath;
		private String weatherPath;
		private String rankedKPath;

		private String[] getResources() {
			return new String[] {analystPath, critiquePath, leadAnalystPath, weatherPath, rankedKPath};
		}
	}

}
