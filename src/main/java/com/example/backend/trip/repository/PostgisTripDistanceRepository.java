package com.example.backend.trip.repository;

import jakarta.persistence.EntityManager;
import jakarta.persistence.PersistenceContext;
import org.springframework.stereotype.Repository;

import java.math.BigDecimal;
import java.math.RoundingMode;
import java.util.UUID;

@Repository
public class PostgisTripDistanceRepository {

    @PersistenceContext
    private EntityManager entityManager;

    public BigDecimal calculateActualDistanceKm(UUID tripId) {

        String sql = """
                WITH trip_points AS (
                    SELECT
                        t.origin_latitude AS latitude,
                        t.origin_longitude AS longitude,
                        t.started_at AS recorded_at,
                        0 AS point_order
                    FROM trips t
                    WHERE t.id = :tripId

                    UNION ALL

                    SELECT
                        tl.latitude,
                        tl.longitude,
                        tl.recorded_at,
                        1 AS point_order
                    FROM trip_locations tl
                    WHERE tl.trip_id = :tripId
                ),
                ordered_points AS (
                    SELECT
                        latitude,
                        longitude,
                        LAG(latitude) OVER (
                            ORDER BY recorded_at, point_order
                        ) AS previous_latitude,
                        LAG(longitude) OVER (
                            ORDER BY recorded_at, point_order
                        ) AS previous_longitude
                    FROM trip_points
                )
                SELECT COALESCE(
                    SUM(
                        ST_Distance(
                            ST_SetSRID(
                                ST_MakePoint(
                                    previous_longitude,
                                    previous_latitude
                                ),
                                4326
                            )::geography,
                            ST_SetSRID(
                                ST_MakePoint(longitude, latitude),
                                4326
                            )::geography
                        )
                    ) / 1000.0,
                    0
                )
                FROM ordered_points
                WHERE previous_latitude IS NOT NULL
                """;

        Object result = entityManager
                .createNativeQuery(sql)
                .setParameter("tripId", tripId)
                .getSingleResult();

        return new BigDecimal(result.toString())
                .setScale(2, RoundingMode.HALF_UP);
    }
}
