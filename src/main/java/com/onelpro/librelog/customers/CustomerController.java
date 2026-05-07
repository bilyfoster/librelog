package com.onelpro.librelog.customers;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.UUID;

@RestController
@RequestMapping("/api/customers")
@RequiredArgsConstructor
public class CustomerController {

    private final CustomerRepository customers;

    public record CustomerDto(String id, String stationId, String name, String contact, String notes) {
        static CustomerDto from(Customer c) {
            return new CustomerDto(c.getId().toString(), c.getStationId().toString(),
                    c.getName(), c.getContact(), c.getNotes());
        }
    }

    public record CustomerRequest(@NotBlank String stationId, @NotBlank String name, String contact, String notes) {}

    @GetMapping
    public List<CustomerDto> list(@RequestParam(required = false) UUID stationId) {
        var list = stationId == null ? customers.findAll() : customers.findByStationIdOrderByNameAsc(stationId);
        return list.stream().map(CustomerDto::from).toList();
    }

    @GetMapping("/{id}")
    public ResponseEntity<CustomerDto> get(@PathVariable UUID id) {
        return customers.findById(id).map(CustomerDto::from)
                .map(ResponseEntity::ok).orElseGet(() -> ResponseEntity.notFound().build());
    }

    @PostMapping
    public CustomerDto create(@Valid @RequestBody CustomerRequest req) {
        Customer c = Customer.builder()
                .stationId(UUID.fromString(req.stationId()))
                .name(req.name())
                .contact(req.contact())
                .notes(req.notes())
                .build();
        return CustomerDto.from(customers.save(c));
    }

    @PutMapping("/{id}")
    public ResponseEntity<CustomerDto> update(@PathVariable UUID id, @Valid @RequestBody CustomerRequest req) {
        var c = customers.findById(id).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        c.setName(req.name());
        c.setContact(req.contact());
        c.setNotes(req.notes());
        return ResponseEntity.ok(CustomerDto.from(customers.save(c)));
    }

    @DeleteMapping("/{id}")
    public ResponseEntity<Void> delete(@PathVariable UUID id) {
        if (!customers.existsById(id)) return ResponseEntity.notFound().build();
        customers.deleteById(id);
        return ResponseEntity.noContent().build();
    }
}
