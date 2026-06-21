package com.onelpro.librelog.carts;

import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.ArrayList;
import java.util.List;
import java.util.UUID;

@Service
@RequiredArgsConstructor
public class ClockService {

    private final ClockTemplateRepository templates;
    private final ClockTemplateSlotRepository slots;
    private final CartRepository carts;

    public List<ClockTemplate> listForStation(UUID stationId) {
        return templates.findByStationIdOrderByNameAsc(stationId);
    }

    public List<ClockTemplateSlot> slotsOf(UUID templateId) {
        return slots.findByClockTemplateIdOrderByPositionAsc(templateId);
    }

    @Transactional
    public ClockTemplate create(UUID stationId, String name, String description) {
        ClockTemplate t = ClockTemplate.builder()
                .stationId(stationId).name(name).description(description).build();
        return templates.save(t);
    }

    @Transactional
    public ClockTemplate rename(UUID templateId, String name, String description) {
        ClockTemplate t = templates.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Clock not found"));
        if (name != null) t.setName(name);
        if (description != null) t.setDescription(description);
        return templates.save(t);
    }

    @Transactional
    public void delete(UUID templateId) { templates.deleteById(templateId); }

    @Transactional
    public List<ClockTemplateSlot> setSlots(UUID templateId, List<ClockTemplateSlot> incoming) {
        ClockTemplate tpl = templates.findById(templateId)
                .orElseThrow(() -> new IllegalArgumentException("Clock not found"));
        UUID stationId = tpl.getStationId();
        slots.deleteByClockTemplateId(templateId);
        slots.flush();
        List<ClockTemplateSlot> saved = new ArrayList<>();
        int pos = 0;
        for (ClockTemplateSlot s : incoming) {
            s.setId(null);
            s.setClockTemplateId(templateId);
            s.setPosition(pos++);
            if (s.getKind() == null) s.setKind("NOTE");
            validateSlot(s, stationId);
            saved.add(slots.save(s));
        }
        return saved;
    }

    private void validateSlot(ClockTemplateSlot s, UUID stationId) {
        String cat = s.getCartCategory() == null ? null : s.getCartCategory().trim();
        s.setCartCategory(cat == null || cat.isEmpty() ? null : cat);
        if (!"MUSIC_CART".equals(s.getKind()) && !"COMMERCIAL_CART".equals(s.getKind())) {
            if (s.getCartId() != null || s.getCartCategory() != null) {
                throw new IllegalArgumentException("Only music/commercial cart slots may reference a cart or category");
            }
            return;
        }
        if (s.getCartId() != null && s.getCartCategory() != null) {
            throw new IllegalArgumentException("Use either a specific cart or a category for slot " + (s.getPosition() + 1) + ", not both");
        }
        if ("MUSIC_CART".equals(s.getKind())) {
            if (s.getCartId() == null && s.getCartCategory() == null) {
                throw new IllegalArgumentException("Music cart slot " + (s.getPosition() + 1) + " needs a cart or a category");
            }
        }
        if (s.getCartCategory() != null) {
            if ("MUSIC_CART".equals(s.getKind())) {
                if (!CartService.LIBRARY_CATEGORIES.contains(s.getCartCategory())) {
                    throw new IllegalArgumentException("Invalid library category: " + s.getCartCategory());
                }
            } else {
                if (!CartService.COMMERCIAL_CATEGORIES.contains(s.getCartCategory())) {
                    throw new IllegalArgumentException("Invalid commercial category: " + s.getCartCategory());
                }
            }
        }
        if (s.getCartId() != null) {
            Cart c = carts.findById(s.getCartId())
                    .orElseThrow(() -> new IllegalArgumentException("Cart not found for slot " + (s.getPosition() + 1)));
            if (!c.getStationId().equals(stationId)) {
                throw new IllegalArgumentException("Cart \"" + c.getName() + "\" is not on this station");
            }
            if ("MUSIC_CART".equals(s.getKind()) && !"MUSIC".equals(c.getKind())) {
                throw new IllegalArgumentException("Music cart slot must reference a library (MUSIC) cart");
            }
            if ("COMMERCIAL_CART".equals(s.getKind()) && !"COMMERCIAL".equals(c.getKind())) {
                throw new IllegalArgumentException("Commercial cart slot must reference a commercial cart");
            }
        }
    }
}
