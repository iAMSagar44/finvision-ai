package dev.sagar.batch_job_mcp.job;

import java.util.List;

import org.springframework.jdbc.core.simple.JdbcClient;
import org.springframework.stereotype.Service;

import jakarta.annotation.PostConstruct;

/**
 *
 * @author sagar.bhat
 */
@Service
class CategoryService {
    private static final org.slf4j.Logger logger = org.slf4j.LoggerFactory.getLogger(CategoryService.class);

    private List<String> categories;

    private final JdbcClient jdbcClient;

    public CategoryService(JdbcClient jdbcClient) {
        this.jdbcClient = jdbcClient;
    }

    @PostConstruct
    public void loadCategories() {
        categories = jdbcClient.sql("SELECT category FROM categories")
                .query(String.class)
                .list();
        logger.debug("Loaded categories -> {}", categories);
    }

    public String getCategories() {
        return String.join(", ", categories);
    }
}
