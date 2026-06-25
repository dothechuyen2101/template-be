package com.example.template;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;
import org.springframework.test.context.ActiveProfiles;
import org.testcontainers.containers.OracleContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.testcontainers.utility.DockerImageName;

import javax.sql.DataSource;

import static org.assertj.core.api.Assertions.assertThat;

@SpringBootTest(properties = "spring.flyway.locations=classpath:db/migration/oracle")
@ActiveProfiles("test")
@Testcontainers(disabledWithoutDocker = true)
class OracleContainerIntegrationTest {

    @Container
    @ServiceConnection
    static final OracleContainer ORACLE = new OracleContainer(
            DockerImageName.parse("gvenzl/oracle-free:23-slim-faststart")
                    .asCompatibleSubstituteFor("gvenzl/oracle-xe")
    ) {
        @Override
        public String getDatabaseName() {
            return "FREEPDB1";
        }
    };

    @Autowired
    private DataSource dataSource;

    @Test
    void applicationStartsAndFlywayMigratesOracle() throws Exception {
        assertThat(dataSource.getConnection().getMetaData().getDatabaseProductName())
                .isEqualTo("Oracle");
    }
}
