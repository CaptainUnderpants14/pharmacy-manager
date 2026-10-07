package demo.pharma.catalog;

import java.util.List;
import java.util.UUID;

import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RestController;

import demo.pharma.common.exception.NotFoundException;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;

@RestController
@RequiredArgsConstructor
public class MasterDataController {
    private final CategoryRepository categories;
    private final ManufacturerRepository manufacturers;

    record CategoryRequest(@NotBlank String name, String description) {
    }

    record CategoryView(UUID id, String name, String description, boolean active) {
    }

    record ManufacturerRequest(@NotBlank String name, String contactPerson, String phone, String email, String address,
            String gstin) {
    }

    @GetMapping("/api/categories")
    @PreAuthorize("hasAuthority('MEDICINE_VIEW')")
    List<CategoryView> categories() {
        return categories.findAll().stream()
                .map(c -> new CategoryView(c.getId(), c.getName(), c.getDescription(), c.isActive())).toList();
    }

    @PostMapping("/api/categories")
    @PreAuthorize("hasAuthority('MEDICINE_CREATE')")
    CategoryView category(@RequestBody CategoryRequest r) {
        Category c = new Category();
        c.setName(r.name());
        c.setDescription(r.description());
        categories.save(c);
        return new CategoryView(c.getId(), c.getName(), c.getDescription(), c.isActive());
    }

    @PutMapping("/api/categories/{id}")
    @PreAuthorize("hasAuthority('MEDICINE_UPDATE')")
    CategoryView categoryUpdate(@PathVariable UUID id, @RequestBody CategoryRequest r) {
        Category c = categories.findById(id).orElseThrow(() -> new NotFoundException("Category", id));
        c.setName(r.name());
        c.setDescription(r.description());
        categories.save(c);
        return new CategoryView(c.getId(), c.getName(), c.getDescription(), c.isActive());
    }

    @GetMapping("/api/manufacturers")
    @PreAuthorize("hasAuthority('MEDICINE_VIEW')")
    List<Manufacturer> manufacturers() {
        return manufacturers.findAll();
    }

    @PostMapping("/api/manufacturers")
    @PreAuthorize("hasAuthority('MEDICINE_CREATE')")
    Manufacturer manufacturer(@RequestBody ManufacturerRequest r) {
        Manufacturer m = new Manufacturer();
        m.setName(r.name());
        m.setContactPerson(r.contactPerson());
        m.setPhone(r.phone());
        m.setEmail(r.email());
        m.setAddress(r.address());
        m.setGstin(r.gstin());
        return manufacturers.save(m);
    }
}
