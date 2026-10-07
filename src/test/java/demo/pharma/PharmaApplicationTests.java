package demo.pharma;

import org.junit.jupiter.api.Test;
import org.springframework.boot.test.context.SpringBootTest;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:pharma;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"app.jwt.secret=test-secret-that-is-longer-than-thirty-two-characters"
})
class PharmaApplicationTests {

	@Test
	void contextLoads() {
	}

}
