package br.com.petServices;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.test.context.ActiveProfiles;

// sobe contra o banco local, que ja tem as migrations de exemplo do profile dev
@SpringBootTest
@ActiveProfiles("dev")
class PetServicesApplicationTests {

	@Test
	void contextLoads() {
	}

}
