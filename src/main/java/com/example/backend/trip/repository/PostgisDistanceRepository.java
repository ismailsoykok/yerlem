package com.example.backend.trip.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;

@Repository
public class PostgisDistanceRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public BigDecimal calculateDistanceKm(
            BigDecimal startLongitude,
            BigDecimal startLatitude,
            BigDecimal destinationLongitude,
            BigDecimal destinationLatitude) {

        String sql = """
                SELECT ST_Distance(
                    ST_SetSRID(
                        ST_MakePoint(:startLongitude, :startLatitude),
                        4326
                    )::geography,
                    ST_SetSRID(
                        ST_MakePoint(:destinationLongitude, :destinationLatitude),
                        4326
                    )::geography
                ) / 1000.0
                """;

        Object result = entityManager
                .createNativeQuery(sql)
                .setParameter("startLongitude", startLongitude)
                .setParameter("startLatitude", startLatitude)
                .setParameter("destinationLongitude", destinationLongitude)
                .setParameter("destinationLatitude", destinationLatitude)
                .getSingleResult();

        return new BigDecimal(result.toString());
    }
}