package com.stockroom.core.sku;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockroom.core.common.NotFoundException;
import com.stockroom.testsupport.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class SkuServiceTest {

    @Autowired
    SkuService skus;

    @Test
    void normalisesCodeOnCreate() {
        String code = Fixtures.unique("abc");

        Sku sku = skus.create(new NewSku("  " + code + " ", "Gloves", "Nitrile, size M", "BOX", 20));

        assertThat(sku.getCode()).isEqualTo(code.toUpperCase());
        assertThat(skus.getByCode(code).getId()).isEqualTo(sku.getId());
    }

    @Test
    void rejectsDuplicateCodes() {
        String code = Fixtures.unique("DUP");
        skus.create(new NewSku(code, "One", null, "EA", 0));

        assertThatThrownBy(() -> skus.create(new NewSku(code.toLowerCase(), "Two", null, "EA", 0)))
                .isInstanceOf(DuplicateSkuException.class);
    }

    @Test
    void patchChangesOnlyTheGivenScalarFields() {
        Sku sku = skus.create(new NewSku(Fixtures.unique("PAT"), "Tape", "Packing tape", "ROLL", 5));

        Sku patched = skus.patch(sku.getId(), new SkuPatch("Clear tape", "Packing tape", null, null, null));

        assertThat(patched.getName()).isEqualTo("Clear tape");
        assertThat(patched.getUnitOfMeasure()).isEqualTo("ROLL");
        assertThat(patched.getReorderPoint()).isEqualTo(5);
        assertThat(patched.isActive()).isTrue();
    }

    @Test
    void patchRejectsNegativeReorderPoint() {
        Sku sku = skus.create(new NewSku(Fixtures.unique("NEG"), "Labels", null, "EA", 5));

        assertThatThrownBy(() -> skus.patch(sku.getId(), new SkuPatch(null, null, null, -1, null)))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void deactivatedSkusDropOutOfTheActiveList() {
        Sku sku = skus.create(new NewSku(Fixtures.unique("OLD"), "Old item", null, "EA", 0));

        skus.patch(sku.getId(), new SkuPatch(null, null, null, null, false));

        assertThat(skus.listActive()).extracting(Sku::getCode).doesNotContain(sku.getCode());
    }

    @Test
    void unknownSkuIsNotFound() {
        assertThatThrownBy(() -> skus.get(-1L)).isInstanceOf(NotFoundException.class);
    }
}
