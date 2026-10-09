package demo.pharma;

import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import com.fasterxml.jackson.databind.ObjectMapper;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.http.MediaType;
import org.springframework.test.web.servlet.MockMvc;

import demo.pharma.catalog.Category;
import demo.pharma.catalog.CategoryRepository;
import demo.pharma.catalog.Manufacturer;
import demo.pharma.catalog.ManufacturerRepository;
import demo.pharma.catalog.Medicine;
import demo.pharma.catalog.MedicineRepository;

import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest(properties = {
		"spring.datasource.url=jdbc:h2:mem:pharma;MODE=PostgreSQL;DB_CLOSE_DELAY=-1",
		"spring.datasource.driver-class-name=org.h2.Driver",
		"spring.datasource.username=sa",
		"spring.datasource.password=",
		"app.jwt.secret=test-secret-that-is-longer-than-thirty-two-characters",
		"app.initial-admin.username=admin",
		"app.initial-admin.password=strong-test-password",
		"app.initial-admin.email=admin@test.invalid"
})
@AutoConfigureMockMvc
class PharmaApplicationTests {

	@Autowired
	private MockMvc mvc;

	@Autowired
	private ObjectMapper objectMapper;

	@Autowired
	private CategoryRepository categories;

	@Autowired
	private ManufacturerRepository manufacturers;

	@Autowired
	private MedicineRepository medicines;

	@Test
	void contextLoads() {
	}

	@Test
	void loginReturnsJwtForInitialOwner() throws Exception {
		mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"strong-test-password\"}"))
				.andExpect(status().isOk()).andExpect(jsonPath("$.token").isNotEmpty())
				.andExpect(jsonPath("$.user.roles[0]").value("OWNER"));
	}

	@Test
	void protectedEndpointRejectsAnonymousRequest() throws Exception {
		mvc.perform(get("/api/dashboard")).andExpect(status().isUnauthorized());
	}

	@Test
	void ownerCanListMedicinesAndSuppliers() throws Exception {
		Category category = new Category();
		category.setName("Test category");
		categories.save(category);
		Manufacturer manufacturer = new Manufacturer();
		manufacturer.setName("Test manufacturer");
		manufacturers.save(manufacturer);
		Medicine medicine = new Medicine();
		medicine.setMedicineCode("TEST-500");
		medicine.setName("Test medicine");
		medicine.setCategory(category);
		medicine.setManufacturer(manufacturer);
		medicines.save(medicine);

		String token = ownerToken();

		mvc.perform(get("/api/medicines").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk()).andExpect(jsonPath("$.content[0].category").value("Test category"))
				.andExpect(jsonPath("$.content[0].manufacturer").value("Test manufacturer"));
		mvc.perform(get("/api/suppliers").header("Authorization", "Bearer " + token))
				.andExpect(status().isOk());
	}

	private String ownerToken() throws Exception {
		String response = mvc.perform(post("/api/auth/login").contentType(MediaType.APPLICATION_JSON)
				.content("{\"username\":\"admin\",\"password\":\"strong-test-password\"}"))
				.andExpect(status().isOk()).andReturn().getResponse().getContentAsString();
		return objectMapper.readTree(response).path("token").asText();
	}

}
