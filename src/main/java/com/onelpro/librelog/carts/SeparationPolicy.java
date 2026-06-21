package com.onelpro.librelog.carts;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

@Entity
@Table(name = "separation_policy")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class SeparationPolicy {

    @Id
    @Column(name = "cart_id")
    private UUID cartId;

    @Column(name = "min_minutes_same_cart", nullable = false)
    private int minMinutesSameCart;

    @Column(name = "min_minutes_same_artist", nullable = false)
    private int minMinutesSameArtist;

    @Column(name = "min_minutes_same_title", nullable = false)
    private int minMinutesSameTitle;

    @Column(name = "min_minutes_same_sponsor", nullable = false)
    private int minMinutesSameSponsor;

    @Column(name = "min_minutes_same_product", nullable = false)
    private int minMinutesSameProduct;
}
