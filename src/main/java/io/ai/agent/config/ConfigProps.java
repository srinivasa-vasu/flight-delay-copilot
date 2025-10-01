package io.ai.agent.config;

import jakarta.annotation.PostConstruct;
import java.io.IOException;
import java.nio.charset.Charset;
import lombok.AccessLevel;
import lombok.Data;
import lombok.Setter;

import org.springframework.boot.context.properties.ConfigurationProperties;
import org.springframework.core.io.Resource;
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
		private final Resource analystPath;
		private final Resource leadAnalystPath;
		private final Resource critiquePath;
		private final Resource weatherPath;
		private final Resource rankedKPath;

		Prompt(Resource analystPath, Resource leadAnalystPath, Resource critiquePath, Resource weatherPath, Resource rankedKPath) throws IOException {
			this.analystPath = analystPath;
			this.leadAnalystPath = leadAnalystPath;
			this.critiquePath = critiquePath;
			this.weatherPath = weatherPath;
			this.rankedKPath = rankedKPath;
			setAnalystPrompt(analystPath.getContentAsString(Charset.defaultCharset()));
			setCritiquePrompt(critiquePath.getContentAsString(Charset.defaultCharset()));
			setLeadAnalystPrompt(leadAnalystPath.getContentAsString(Charset.defaultCharset()));
			setWeatherPrompt(weatherPath.getContentAsString(Charset.defaultCharset()));
			setRankedKPrompt(rankedKPath.getContentAsString(Charset.defaultCharset()));
		}
	}

}
