package uk.gov.companieshouse.exemptions;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultHandlers.print;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.content;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.boot.test.context.SpringBootTest.WebEnvironment;
import org.springframework.boot.test.web.server.LocalServerPort;
import org.springframework.test.web.servlet.MockMvc;
import org.testcontainers.containers.MongoDBContainer;
import org.testcontainers.junit.jupiter.Container;
import org.testcontainers.junit.jupiter.Testcontainers;
import org.springframework.boot.testcontainers.service.connection.ServiceConnection;

@Testcontainers
@AutoConfigureMockMvc
@SpringBootTest(webEnvironment = WebEnvironment.RANDOM_PORT)
class ExemptionsApplicationTest {
	@LocalServerPort
	int port;

	@Container
	@ServiceConnection
	static final MongoDBContainer MONGO = new MongoDBContainer("mongo:8.0");

	@Autowired
	private MockMvc mockMvc;

	@Test
	void shouldStartApplication() {
	}

	@Test
	void shouldReturn200FromGetHealthEndpoint() throws Exception {
		this.mockMvc.perform(get("/healthcheck"))
				.andDo(print())
				.andExpect(status().isOk())
				.andExpect(content().string("{\"status\":\"UP\"}"));
	}
}
