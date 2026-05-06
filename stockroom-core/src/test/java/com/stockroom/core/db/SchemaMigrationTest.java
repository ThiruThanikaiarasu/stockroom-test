package com.stockroom.core.db;

import static org.assertj.core.api.Assertions.assertThat;

import java.util.List;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.jdbc.core.JdbcTemplate;

@DataJpaTest
class SchemaMigrationTest {

    @Autowired
    JdbcTemplate jdbc;

    @Test
    void allMigrationsApplied() {
        List<String> versions = jdbc.queryForList(
                "select \"version\" from \"flyway_schema_history\" where \"success\" = true and \"version\" is not null order by \"installed_rank\"",
                String.class);

        assertThat(versions).containsExactly("1", "2", "3", "4");
    }

    @Test
    void averageCostDefaultsToZero() {
        jdbc.update("insert into skus (code, name, unit_of_measure, reorder_point, created_at) "
                + "values ('MIG-1', 'Migration check', 'EA', 0, current_timestamp)");

        assertThat(jdbc.queryForObject("select average_cost from skus where code = 'MIG-1'", java.math.BigDecimal.class))
                .isEqualByComparingTo("0");
    }

    @Test
    void negativeOnHandIsRejectedByTheDatabase() {
        jdbc.update("insert into skus (code, name, unit_of_measure, reorder_point, created_at) "
                + "values ('MIG-2', 'Constraint check', 'EA', 0, current_timestamp)");
        jdbc.update("insert into locations (code, name) values ('MIG', 'Migration')");
        jdbc.update("insert into bins (location_id, code, aisle, bay) "
                + "select id, 'A-1', 'A', 1 from locations where code = 'MIG'");

        org.assertj.core.api.Assertions.assertThatThrownBy(() -> jdbc.update(
                        "insert into stock_levels (sku_id, bin_id, on_hand, reserved) "
                                + "select s.id, b.id, -1, 0 from skus s, bins b where s.code = 'MIG-2'"))
                .isInstanceOf(org.springframework.dao.DataIntegrityViolationException.class);
    }
}
