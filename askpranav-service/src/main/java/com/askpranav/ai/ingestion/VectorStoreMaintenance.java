package com.askpranav.ai.ingestion;

import org.springframework.beans.factory.annotation.Value;
import org.springframework.jdbc.core.JdbcTemplate;
import org.springframework.stereotype.Component;

/**
 * Targeted deletes and counts on the vector store's table, which Spring AI's VectorStore interface does
 * not offer without building filter expressions. Everything is keyed on the metadata written at
 * ingestion time ({@code source}, {@code fileName}).
 */
@Component
public class VectorStoreMaintenance {

    private final JdbcTemplate jdbcTemplate;
    private final String table;

    public VectorStoreMaintenance(JdbcTemplate jdbcTemplate,
                                  @Value("${spring.ai.vectorstore.pgvector.table-name:vector_store}") String table) {
        if (!table.matches("[A-Za-z0-9_]+")) {
            throw new IllegalArgumentException("Unsafe vector store table name: " + table);
        }
        this.jdbcTemplate = jdbcTemplate;
        this.table = table;
    }

    public int deleteAll() {
        return jdbcTemplate.update("DELETE FROM " + table);
    }

    public int deleteBySource(String source) {
        return jdbcTemplate.update("DELETE FROM " + table + " WHERE metadata->>'source' = ?", source);
    }

    public int deleteByFileName(String fileName) {
        return jdbcTemplate.update("DELETE FROM " + table + " WHERE metadata->>'fileName' = ?", fileName);
    }

    public int countByFileName(String fileName) {
        Integer count = jdbcTemplate.queryForObject(
                "SELECT count(*) FROM " + table + " WHERE metadata->>'fileName' = ?", Integer.class, fileName);
        return count == null ? 0 : count;
    }
}
