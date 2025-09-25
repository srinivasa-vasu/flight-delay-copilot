package io.ai.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.FetchType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;
import java.time.LocalDateTime;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;

@Table(name = "flight_embeddings")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class FlightEmbedding {
	@Id
	@UuidGenerator
	private UUID id;

	@Column(columnDefinition = "TEXT")
	private String content;

	@Column(columnDefinition = "vector(768)")
	private float[] embedding;

	private String documentType;
	private LocalDateTime createdAt;

	@ManyToOne(fetch = FetchType.LAZY)
	@JoinColumn(name = "flight_id")
	private Flight flight;

	@Column(columnDefinition = "jsonb")
	private String metadata;
}
