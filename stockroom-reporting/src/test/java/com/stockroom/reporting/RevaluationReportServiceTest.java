package com.stockroom.reporting;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockroom.core.common.NotFoundException;
import com.stockroom.core.sku.Sku;
import com.stockroom.testsupport.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class RevaluationReportServiceTest {

    @Autowired
    RevaluationReportService revaluations;

    @Autowired
    Fixtures fixtures;

    @Test
    void skuWithoutReceiptsHasNoHistory() {
        Sku sku = fixtures.sku(0);

        assertThat(revaluations.forSku(sku.getCode().toLowerCase()).entries()).isEmpty();
    }

    @Test
    void unknownSkuIsNotFound() {
        assertThatThrownBy(() -> revaluations.forSku("NOPE-404")).isInstanceOf(NotFoundException.class);
    }
}
