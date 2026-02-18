package com.stockroom.core.picking;

import java.util.List;
import java.util.Optional;
import java.util.UUID;

import org.springframework.data.jpa.repository.JpaRepository;

public interface PickListRepository extends JpaRepository<PickList, Long> {

    Optional<PickList> findByPublicId(UUID publicId);

    List<PickList> findByStatusOrderByCreatedAtAsc(PickListStatus status);

    boolean existsByOrderRefAndStatus(String orderRef, PickListStatus status);
}
