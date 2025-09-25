package io.ai.agent.repo;

import io.ai.agent.entity.FlightEmbedding;
import java.util.List;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;
import org.springframework.data.repository.query.Param;
import org.springframework.stereotype.Repository;

@Repository
public interface FlightEmbeddingRepository extends JpaRepository<FlightEmbedding, UUID> {

	@Query(value = "SELECT * FROM flight_embeddings ORDER BY embedding <-> cast(:queryEmbedding as vector) LIMIT :limit",
			nativeQuery = true)
	List<FlightEmbedding> findSimilarEmbeddings(@Param("queryEmbedding") String queryEmbedding,
			@Param("limit") int limit);
}

