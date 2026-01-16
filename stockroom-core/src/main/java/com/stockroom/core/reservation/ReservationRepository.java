package com.stockroom.core.reservation;

import java.time.LocalDateTime;
import java.util.List;
import java.util.Optional;

import org.springframework.data.jpa.repository.JpaRepository;
import org.springframework.data.jpa.repository.Query;

public interface ReservationRepository extends JpaRepository<Reservation, Long> {

    Optional<Reservation> findByReference(String reference);

    List<Reservation> findByOrderRefAndStatus(String orderRef, ReservationStatus status);

    @Query("select r from Reservation r where r.status = 'ACTIVE' and r.expiresAt < :cutoff order by r.expiresAt")
    List<Reservation> findActiveExpiringBefore(LocalDateTime cutoff);

    @Query("select r from Reservation r where r.status = 'ACTIVE' order by r.expiresAt")
    List<Reservation> findAllActive();

    long countBySkuIdAndStatus(Long skuId, ReservationStatus status);
}
