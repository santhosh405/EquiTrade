package com.example.demo;

import com.example.demo.config.DataInitializer;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.mock.mockito.MockBean;
import org.springframework.test.context.ActiveProfiles;

@SpringBootTest
@ActiveProfiles("test")
class DemoApplicationTests {

    // Suppress the heavy DataInitializer (requires seeded DB data) in context load test
    @MockBean
    private DataInitializer dataInitializer;

    @Test
    void contextLoads() {
    }
}
