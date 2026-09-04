package dev.rainbow.aidevelop.persistence;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.test.context.DynamicPropertyRegistry;
import org.springframework.test.context.DynamicPropertySource;
import org.testcontainers.containers.PostgreSQLContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest
@Testcontainers
class SchemaMigrationTest {

    @Container
    static final PostgreSQLContainer<?> POSTGRES =
            new PostgreSQLContainer<>("postgres:16-alpine")
                    .withDatabaseName("rainbow")
                    .withUsername("rainbow")
                    .withPassword("rainbow-local");

    @DynamicPropertySource
    static void databaseProperties(DynamicPropertyRegistry registry) {
        registry.add("spring.datasource.url", POSTGRES::getJdbcUrl);
        registry.add("spring.datasource.username", POSTGRES::getUsername);
        registry.add("spring.datasource.password", POSTGRES::getPassword);
    }

    @Autowired
    private JdbcTemplate jdbcTemplate;

    @Test
    void createsOnlyTheProjectConversationSchema() {
        assertThat(tableNames()).contains("projects", "conversations", "messages", "agent_runs", "agent_events");
        assertThat(tableNames()).doesNotContain("knowledge_bases", "documents");
        assertThat(vectorExtensionCount()).isZero();
        assertThat(columnNames("conversations")).contains("updated_at");
        assertThat(columnNames("messages")).contains("request_id", "metadata_json");
    }

    private java.util.List<String> tableNames() {
        return jdbcTemplate.queryForList("""
                SELECT table_name
                FROM information_schema.tables
                WHERE table_schema = 'public'
                """, String.class);
    }

    private int vectorExtensionCount() {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT COUNT(*) FROM pg_extension WHERE extname = 'vector'",
                Integer.class
        );
        return count == null ? 0 : count;
    }

    private java.util.List<String> columnNames(String table) {
        return jdbcTemplate.queryForList("""
                SELECT column_name
                FROM information_schema.columns
                WHERE table_schema = 'public' AND table_name = ?
                """, String.class, table);
    }
}
