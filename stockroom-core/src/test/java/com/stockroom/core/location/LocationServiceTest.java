package com.stockroom.core.location;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;

import com.stockroom.core.common.InvalidStateException;
import com.stockroom.testsupport.Fixtures;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
class LocationServiceTest {

    @Autowired
    LocationService locations;

    @Test
    void binsAreAddressedByAisleAndBay() {
        Location location = locations.createLocation(Fixtures.unique("blr"), "Bengaluru DC");

        Bin bin = locations.addBin(location.getId(), "c", 12);

        assertThat(bin.getCode()).isEqualTo("C-12");
        assertThat(bin.getAisle()).isEqualTo("C");
        assertThat(bin.getBay()).isEqualTo(12);
        assertThat(bin.isPickable()).isTrue();
    }

    @Test
    void rejectsDuplicateBins() {
        Location location = locations.createLocation(Fixtures.unique("WH"), "Main");
        locations.addBin(location.getId(), "A", 1);

        assertThatThrownBy(() -> locations.addBin(location.getId(), "A", 1))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void rejectsMalformedBinAddresses() {
        Location location = locations.createLocation(Fixtures.unique("WH"), "Main");

        assertThatThrownBy(() -> locations.addBin(location.getId(), "A1", 1))
                .isInstanceOf(IllegalArgumentException.class);
        assertThatThrownBy(() -> locations.addBin(location.getId(), "A", 0))
                .isInstanceOf(IllegalArgumentException.class);
    }

    @Test
    void rejectsDuplicateLocationCodes() {
        String code = Fixtures.unique("DUP");
        locations.createLocation(code, "First");

        assertThatThrownBy(() -> locations.createLocation(code, "Second"))
                .isInstanceOf(InvalidStateException.class);
    }

    @Test
    void binsCanBeTakenOutOfPicking() {
        Location location = locations.createLocation(Fixtures.unique("WH"), "Main");
        Bin bin = locations.addBin(location.getId(), "Q", 1);

        locations.setPickable(bin.getId(), false);

        assertThat(locations.getBin(bin.getId()).isPickable()).isFalse();
    }
}
