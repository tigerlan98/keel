package io.github.keel;

import io.github.keel.support.IntegrationTestSupport;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class KeelApplicationTests extends IntegrationTestSupport {

    @Test
    void contextLoads() {
    }
}
