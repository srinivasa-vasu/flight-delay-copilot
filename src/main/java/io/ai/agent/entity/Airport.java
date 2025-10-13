package io.ai.agent.entity;

import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.Id;
import jakarta.persistence.Table;
import java.util.UUID;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;
import org.hibernate.annotations.UuidGenerator;


@Table(name = "airport")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
@Entity
public class Airport {
	@Id
	@UuidGenerator
	private UUID id;

	@Column(unique = true, nullable = false)
	private String code;

	@Column(nullable = false)
	private String name;

	private String city;
	private String country;
	private String timezone;
	private Double latitude;
	private Double longitude;
	private Integer congestionLevel;
	private Double averageDelay;
}
