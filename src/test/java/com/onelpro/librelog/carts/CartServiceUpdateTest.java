package com.onelpro.librelog.carts;

import com.onelpro.librelog.librtime.LibreTimeService;
import com.onelpro.librelog.orders.SpotRepository;
import com.onelpro.librelog.station.DayPartRepository;
import com.onelpro.librelog.station.StationRepository;
import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import java.util.Optional;
import java.util.UUID;

import static org.assertj.core.api.Assertions.assertThat;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

/**
 * Cart update: category is editable (validated against the cart's kind); kind is not.
 */
class CartServiceUpdateTest {

    private CartRepository carts;
    private CartService service;
    private final UUID cartId = UUID.randomUUID();

    @BeforeEach
    void setUp() {
        carts = mock(CartRepository.class);
        service = new CartService(carts, mock(CartMemberRepository.class),
                mock(SeparationPolicyRepository.class), mock(CartPlayHistoryRepository.class),
                mock(SpotRepository.class), mock(LibreTimeService.class),
                mock(StationRepository.class), mock(DayPartRepository.class));
        when(carts.save(any())).thenAnswer(inv -> inv.getArgument(0));
    }

    private Cart musicCart() {
        return Cart.builder().id(cartId).stationId(UUID.randomUUID()).name("Songs")
                .kind("MUSIC").category("MUSIC").source("MANUAL").rotationPointer(0)
                .selectionStrategy(Cart.STRATEGY_ROTATION).build();
    }

    @Test
    void categoryCanChangeWithinLibraryCategories() {
        when(carts.findById(cartId)).thenReturn(Optional.of(musicCart()));
        Cart updated = service.update(cartId, null, null, null, null, "IMAGING");
        assertThat(updated.getCategory()).isEqualTo("IMAGING");
    }

    @Test
    void interviewIsALibraryCategory() {
        when(carts.findById(cartId)).thenReturn(Optional.of(musicCart()));
        Cart updated = service.update(cartId, null, null, null, null, "INTERVIEW");
        assertThat(updated.getCategory()).isEqualTo("INTERVIEW");
    }

    @Test
    void commercialCategoryRejectedOnMusicCart() {
        when(carts.findById(cartId)).thenReturn(Optional.of(musicCart()));
        assertThrows(IllegalArgumentException.class,
                () -> service.update(cartId, null, null, null, null, "COMMERCIAL"));
    }

    @Test
    void libraryCategoryRejectedOnCommercialCart() {
        Cart commercial = musicCart();
        commercial.setKind("COMMERCIAL");
        commercial.setCategory("COMMERCIAL");
        when(carts.findById(cartId)).thenReturn(Optional.of(commercial));
        assertThrows(IllegalArgumentException.class,
                () -> service.update(cartId, null, null, null, null, "NEWS"));
    }

    @Test
    void nullCategoryLeavesItUnchanged() {
        when(carts.findById(cartId)).thenReturn(Optional.of(musicCart()));
        Cart updated = service.update(cartId, null, null, null, null, null);
        assertThat(updated.getCategory()).isEqualTo("MUSIC");
    }
}
