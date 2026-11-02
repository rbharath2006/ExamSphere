package com.onlineexam.backend;

import org.junit.jupiter.api.Disabled;
import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest
@Disabled("Requires a configured PostgreSQL test database; service tests are database-independent")
class BackendApplicationTests {

	@Test
	void contextLoads() {
	}

}
