package io.ai.agent.config;

import com.embabel.common.ai.model.DefaultOptionsConverter;
import com.embabel.common.ai.model.EmbeddingService;
import com.embabel.common.ai.model.Llm;
import com.embabel.common.ai.prompt.KnowledgeCutoffDate;
import java.time.Duration;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import org.springframework.ai.document.MetadataMode;
import org.springframework.ai.model.NoopApiKey;
import org.springframework.ai.openai.OpenAiChatModel;
import org.springframework.ai.openai.OpenAiChatOptions;
import org.springframework.ai.openai.OpenAiEmbeddingModel;
import org.springframework.ai.openai.OpenAiEmbeddingOptions;
import org.springframework.ai.openai.api.OpenAiApi;
import org.springframework.ai.retry.TransientAiException;
import org.springframework.ai.vectorstore.VectorStore;
import org.springframework.ai.vectorstore.pgvector.PgVectorStore;
import org.springframework.beans.factory.annotation.Qualifier;
import org.springframework.beans.factory.config.ConfigurableListableBeanFactory;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.context.annotation.Bean;
import org.springframework.context.annotation.Configuration;
import org.springframework.context.annotation.DependsOn;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.retry.RetryCallback;
import org.springframework.retry.RetryContext;
import org.springframework.retry.RetryListener;
import org.springframework.retry.support.RetryTemplate;
import org.springframework.web.client.ResourceAccessException;

import static io.ai.agent.config.LlmConfiguration.LlmLabel.ModelType.EMBEDDING;
import static io.ai.agent.config.LlmConfiguration.LlmLabel.Provider.DOCKER;
import static io.ai.agent.config.LlmConfiguration.LlmLabel.Provider.GOOGLE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgDistanceType.COSINE_DISTANCE;
import static org.springframework.ai.vectorstore.pgvector.PgVectorStore.PgIndexType.HNSW;

@Configuration
public class LlmConfiguration {

	private static final Logger logger = LoggerFactory.getLogger(LlmConfiguration.class);

	@Bean
	public OpenAiApi openAiGeminiApi() {
		return OpenAiApi.builder().apiKey(System.getenv("GEMINI_API_KEY"))
				.baseUrl("https://generativelanguage.googleapis.com/v1beta/openai")
				.embeddingsPath("/embeddings")
				.completionsPath("/chat/completions")
				.build();
	}

	@Bean
	public OpenAiApi openAiDockerApi() {
		return OpenAiApi.builder().apiKey(new NoopApiKey())
				.baseUrl("http://localhost:12434/engines")
				.embeddingsPath("/v1/embeddings")
				.completionsPath("/v1/chat/completions")
				.build();
	}

	@Bean
	@ConditionalOnProperty(name = "GEMINI_API_KEY")
	public List<Llm> geminiLlms(ConfigProps configProps) {
		LocalDate cutoff = LocalDate.now().minusDays(1);
		List<Llm> result = new ArrayList<>();

		for (LlmLabel label : LlmLabel.values()) {
			if (label.modelType == LlmLabel.ModelType.CHAT && label.provider == GOOGLE) {
				result.add(new Llm(label.name, label.provider.name(), OpenAiChatModel.builder()
						.openAiApi(openAiGeminiApi())
						.retryTemplate(retryTemplate(configProps))
						.defaultOptions(OpenAiChatOptions.builder().model(label.model).build())
						.build(), DefaultOptionsConverter.INSTANCE,
						cutoff, Collections.singletonList(new KnowledgeCutoffDate(cutoff, DateTimeFormatter.ofPattern("yyyy-MM-dd"))), null));
			}
		}
		return result;
	}

	@Bean
	@ConditionalOnProperty(name = "GEMINI_API_KEY")
	public List<EmbeddingService> geminiEmbeddingServices(ConfigurableListableBeanFactory factory) {
		List<EmbeddingService> result = new ArrayList<>();
		for (LlmLabel label : LlmLabel.values()) {
			if (label.modelType == EMBEDDING && label.provider == GOOGLE) {
				EmbeddingService service = new EmbeddingService(label.name, label.provider.name(),
						new OpenAiEmbeddingModel(openAiGeminiApi(), MetadataMode.ALL,
								OpenAiEmbeddingOptions.builder().model(label.model).build()));
				result.add(service);
				factory.registerSingleton(label.name, service);
			}
		}
		return result;
	}

	@Bean
	public List<EmbeddingService> dockerEmbeddingServices(ConfigurableListableBeanFactory factory) {
		List<EmbeddingService> result = new ArrayList<>();
		for (LlmLabel label : LlmLabel.values()) {
			if (label.modelType == EMBEDDING && label.provider == DOCKER) {
				EmbeddingService service = new EmbeddingService(label.name, label.provider.name(),
						new OpenAiEmbeddingModel(openAiDockerApi(), MetadataMode.ALL,
								OpenAiEmbeddingOptions.builder().model(label.model).build()));
				result.add(service);
				factory.registerSingleton(label.name, service);
			}
		}
		return result;
	}

	@Bean
	@DependsOn({"geminiEmbeddingServices", "dockerEmbeddingServices"})
	public VectorStore pgVectorStore(JdbcTemplate jdbcTemplate, ConfigProps props, @Qualifier("ai/embeddinggemma") EmbeddingService embeddingService) {
		return PgVectorStore.builder(jdbcTemplate, embeddingService.getModel())
				.dimensions(props.getVector().getDimension())
				.distanceType(COSINE_DISTANCE)
				.indexType(HNSW)
				.initializeSchema(false)
				.schemaName(props.getVector().getSchema())
				.vectorTableName(props.getVector().getTable())
				.maxDocumentBatchSize(props.getVector().getBatchSize())
				.build();
	}

	@Bean
	public RetryTemplate retryTemplate(ConfigProps props) {
		return RetryTemplate.builder()
				.maxAttempts(props.getRetryAttempts())
				.retryOn(TransientAiException.class)
				.retryOn(ResourceAccessException.class)
				.exponentialBackoff(Duration.ofMillis(2000), 5, Duration.ofMillis(3 * 60000))
				.withListener(new RetryListener() {

					@Override
					public <T, E extends Throwable> void onError(RetryContext context,
							RetryCallback<T, E> callback, Throwable throwable) {
						logger.warn("Retry error. Retry count:{}", context.getRetryCount(), throwable);
					}
				})
				.build();
	}

	enum LlmLabel {
		CGOOGLE("openai/chat/gemini", GOOGLE, "models/gemini-2.5-flash", ModelType.CHAT),
		EGOOGLE("openai/embedding/gemini", GOOGLE, "models/text-embedding-004", EMBEDDING),
		CGOOGLELITE("openai/chat/geminilite", GOOGLE, "models/gemini-2.5-flash-lite", ModelType.CHAT),
		EGOOGLELITE("openai/embedding/geminilite", GOOGLE, "models/text-embedding-004", EMBEDDING),
		EDOCKERLOCALLITE("ai/embeddinggemma", DOCKER, "ai/embeddinggemma:latest", EMBEDDING);

		final String name;
		final Provider provider;
		final String model;
		final ModelType modelType;

		LlmLabel(String name, Provider provider, String model, ModelType modelType) {
			this.name = name;
			this.provider = provider;
			this.model = model;
			this.modelType = modelType;
		}

		enum ModelType {
			CHAT, EMBEDDING
		}

		enum Provider {
			GOOGLE, DOCKER
		}

	}

}
