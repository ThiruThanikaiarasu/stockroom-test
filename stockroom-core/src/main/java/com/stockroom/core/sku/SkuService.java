package com.stockroom.core.sku;

import java.time.Clock;
import java.time.LocalDateTime;
import java.util.List;
import java.util.Locale;

import com.stockroom.core.common.NotFoundException;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

@Service
@Transactional
public class SkuService {

    private final SkuRepository skus;
    private final Clock clock;

    public SkuService(SkuRepository skus, Clock clock) {
        this.skus = skus;
        this.clock = clock;
    }

    public Sku create(NewSku request) {
        String code = request.code().trim().toUpperCase(Locale.ROOT);
        if (skus.existsByCode(code)) {
            throw new DuplicateSkuException(code);
        }
        Sku sku = new Sku(code, request.name(), request.unitOfMeasure(), request.reorderPoint(),
                LocalDateTime.now(clock));
        sku.setDescription(request.description());
        return skus.save(sku);
    }

    @Transactional(readOnly = true)
    public Sku get(Long id) {
        return skus.findById(id).orElseThrow(() -> new NotFoundException("SKU", id));
    }

    @Transactional(readOnly = true)
    public Sku getByCode(String code) {
        return skus.findByCode(code.toUpperCase(Locale.ROOT)).orElseThrow(() -> new NotFoundException("SKU", code));
    }

    @Transactional(readOnly = true)
    public List<Sku> listActive() {
        return skus.findByActiveTrueOrderByCodeAsc();
    }

    public Sku patch(Long id, SkuPatch patch) {
        Sku sku = get(id);
        if (patch.name() != null) {
            sku.setName(patch.name());
        }
        // description is nullable: sending null clears it
        sku.setDescription(patch.description());
        if (patch.unitOfMeasure() != null) {
            sku.setUnitOfMeasure(patch.unitOfMeasure());
        }
        if (patch.reorderPoint() != null) {
            if (patch.reorderPoint() < 0) {
                throw new IllegalArgumentException("reorderPoint must not be negative");
            }
            sku.setReorderPoint(patch.reorderPoint());
        }
        if (patch.active() != null) {
            sku.setActive(patch.active());
        }
        return sku;
    }
}
