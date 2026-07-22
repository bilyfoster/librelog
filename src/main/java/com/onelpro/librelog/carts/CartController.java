package com.onelpro.librelog.carts;

import jakarta.validation.Valid;
import jakarta.validation.constraints.NotBlank;
import lombok.RequiredArgsConstructor;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.*;

import java.util.List;
import java.util.Map;
import java.util.UUID;

@RestController
@RequiredArgsConstructor
public class CartController {

    private final CartService carts;

    public record CartDto(String id, String stationId, String name, String kind, String category,
                          String source, String orderId, String description, int rotationPointer,
                          String selectionStrategy, Integer maxAgeHours,
                          int memberCount, PolicyDto policy) {}

    public record PolicyDto(int sameCart, int sameArtist, int sameTitle, int sameSponsor, int sameProduct) {
        static PolicyDto from(SeparationPolicy p) {
            return new PolicyDto(p.getMinMinutesSameCart(), p.getMinMinutesSameArtist(),
                    p.getMinMinutesSameTitle(), p.getMinMinutesSameSponsor(), p.getMinMinutesSameProduct());
        }
    }

    public record MemberDto(String id, String cartId, int position, int weight,
                            Long librtimeFileId, String spotId,
                            String artist, String title, String sponsor, String product,
                            Integer lengthSeconds, boolean enabled) {
        static MemberDto from(CartMember m) {
            return new MemberDto(m.getId().toString(), m.getCartId().toString(),
                    m.getPosition(), m.getWeight(),
                    m.getLibrtimeFileId(), m.getSpotId() == null ? null : m.getSpotId().toString(),
                    m.getArtist(), m.getTitle(), m.getSponsor(), m.getProduct(),
                    m.getLengthSeconds(), m.isEnabled());
        }
    }

    public record CreateRequest(@NotBlank String name, @NotBlank String kind,
                                String category, String source, String orderId, String description,
                                String selectionStrategy, Integer maxAgeHours) {}

    public record UpdateRequest(String name, String description,
                                String selectionStrategy, Integer maxAgeHours, String category) {}

    public record MemberRequest(Integer position, Integer weight,
                                Long librtimeFileId, String spotId,
                                String artist, String title, String sponsor, String product,
                                Integer lengthSeconds, Boolean enabled) {
        CartService.MemberInput toInput() {
            return new CartService.MemberInput(position, weight, librtimeFileId,
                    spotId == null ? null : UUID.fromString(spotId),
                    artist, title, sponsor, product, lengthSeconds, enabled);
        }
    }

    public record PolicyRequest(Integer sameCart, Integer sameArtist, Integer sameTitle,
                                Integer sameSponsor, Integer sameProduct) {}

    @GetMapping("/api/stations/{stationId}/carts")
    public List<CartDto> list(@PathVariable UUID stationId) {
        return carts.listForStation(stationId).stream()
                .map(c -> toDto(c, carts.membersOf(c.getId()).size(), carts.policyFor(c.getId())))
                .toList();
    }

    @PostMapping("/api/stations/{stationId}/carts")
    public ResponseEntity<?> create(@PathVariable UUID stationId, @Valid @RequestBody CreateRequest req) {
        try {
            UUID orderId = req.orderId() == null ? null : UUID.fromString(req.orderId());
            Cart c = carts.create(stationId, req.name(), req.kind(), req.category(),
                    req.source(), orderId, req.description(),
                    req.selectionStrategy(), req.maxAgeHours());
            return ResponseEntity.ok(toDto(c, carts.membersOf(c.getId()).size(), carts.policyFor(c.getId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @GetMapping("/api/cart-categories")
    public Map<String, Object> categories() {
        return Map.of(
                "library", CartService.LIBRARY_CATEGORIES.stream().sorted().toList(),
                "commercial", CartService.COMMERCIAL_CATEGORIES.stream().sorted().toList()
        );
    }

    @PutMapping("/api/carts/{cartId}")
    public ResponseEntity<?> update(@PathVariable UUID cartId, @RequestBody UpdateRequest req) {
        try {
            Cart c = carts.update(cartId, req.name(), req.description(),
                    req.selectionStrategy(), req.maxAgeHours(), req.category());
            return ResponseEntity.ok(toDto(c, carts.membersOf(c.getId()).size(), carts.policyFor(c.getId())));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/carts/{cartId}")
    public ResponseEntity<Void> delete(@PathVariable UUID cartId) {
        carts.delete(cartId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/carts/{cartId}/members")
    public List<MemberDto> members(@PathVariable UUID cartId) {
        return carts.membersOf(cartId).stream().map(MemberDto::from).toList();
    }

    @PostMapping("/api/carts/{cartId}/members")
    public ResponseEntity<?> addMember(@PathVariable UUID cartId, @RequestBody MemberRequest req) {
        try {
            CartMember m = carts.addMember(cartId, req.toInput());
            return ResponseEntity.ok(MemberDto.from(m));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @PostMapping("/api/carts/{cartId}/library-batch")
    public ResponseEntity<?> addBulkLibraryFiles(@PathVariable UUID cartId,
                                                    @RequestBody BulkFilesRequest req) {
        try {
            var r = carts.addLibraryFilesBulk(cartId, req.fileIds() == null ? List.of() : req.fileIds());
            return ResponseEntity.ok(Map.of(
                    "added", r.added(),
                    "skippedAlreadyInCart", r.skippedAlreadyInCart(),
                    "skippedNotFound", r.skippedNotFound(),
                    "skippedDuplicateInRequest", r.skippedDuplicateInRequest(),
                    "uniqueIdsRequested", r.uniqueIdsRequested()));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    public record BulkFilesRequest(List<Long> fileIds) {}

    @PutMapping("/api/cart-members/{memberId}")
    public ResponseEntity<?> updateMember(@PathVariable UUID memberId, @RequestBody MemberRequest req) {
        try {
            CartMember m = carts.updateMember(memberId, req.toInput());
            return ResponseEntity.ok(MemberDto.from(m));
        } catch (IllegalArgumentException e) {
            return ResponseEntity.badRequest().body(Map.of("error", e.getMessage()));
        }
    }

    @DeleteMapping("/api/cart-members/{memberId}")
    public ResponseEntity<Void> deleteMember(@PathVariable UUID memberId) {
        carts.removeMember(memberId);
        return ResponseEntity.noContent().build();
    }

    @GetMapping("/api/carts/{cartId}/policy")
    public PolicyDto policy(@PathVariable UUID cartId) {
        return PolicyDto.from(carts.policyFor(cartId));
    }

    @PutMapping("/api/carts/{cartId}/policy")
    public PolicyDto savePolicy(@PathVariable UUID cartId, @RequestBody PolicyRequest req) {
        SeparationPolicy p = SeparationPolicy.builder()
                .cartId(cartId)
                .minMinutesSameCart(or(req.sameCart(), 0))
                .minMinutesSameArtist(or(req.sameArtist(), 0))
                .minMinutesSameTitle(or(req.sameTitle(), 0))
                .minMinutesSameSponsor(or(req.sameSponsor(), 0))
                .minMinutesSameProduct(or(req.sameProduct(), 0))
                .build();
        return PolicyDto.from(carts.savePolicy(cartId, p));
    }

    @PostMapping("/api/carts/{cartId}/sync-order")
    public ResponseEntity<?> syncOrder(@PathVariable UUID cartId) {
        Cart c = carts.get(cartId).orElse(null);
        if (c == null) return ResponseEntity.notFound().build();
        carts.syncOrderCart(c);
        return ResponseEntity.ok(Map.of("synced", carts.membersOf(cartId).size()));
    }

    private static int or(Integer v, int dflt) { return v == null ? dflt : Math.max(0, v); }

    private static CartDto toDto(Cart c, int memberCount, SeparationPolicy p) {
        return new CartDto(c.getId().toString(), c.getStationId().toString(),
                c.getName(), c.getKind(), c.getCategory(), c.getSource(),
                c.getOrderId() == null ? null : c.getOrderId().toString(),
                c.getDescription(), c.getRotationPointer(),
                c.getSelectionStrategy(), c.getMaxAgeHours(), memberCount, PolicyDto.from(p));
    }
}
