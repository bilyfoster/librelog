package com.onelpro.librelog.rumble.service;

import org.springframework.stereotype.Service;

import java.time.Duration;
import java.util.List;
import java.util.Objects;

@Service
public class RotationSchedulerEngine {

    public static final Duration ARTIST_SEPARATION = Duration.ofMinutes(90);
    public static final Duration SONG_SEPARATION = Duration.ofMinutes(240);

    public boolean validateRotationRules(List<RotationScheduledItem> currentTimeline,
                                         RotationScheduledItem candidateItem) {
        Objects.requireNonNull(candidateItem, "candidateItem is required");
        if (candidateItem.scheduledAt() == null) {
            throw new IllegalArgumentException("candidate scheduledAt is required");
        }
        if (currentTimeline == null || currentTimeline.isEmpty()) {
            return true;
        }

        for (RotationScheduledItem historicalItem : currentTimeline) {
            if (historicalItem == null || historicalItem.scheduledAt() == null) {
                continue;
            }
            Duration spacing = Duration.between(historicalItem.scheduledAt(), candidateItem.scheduledAt()).abs();

            if (Objects.equals(historicalItem.songId(), candidateItem.songId())
                    && spacing.compareTo(SONG_SEPARATION) < 0) {
                return false;
            }

            if (sameArtist(historicalItem.artistName(), candidateItem.artistName())
                    && spacing.compareTo(ARTIST_SEPARATION) < 0) {
                return false;
            }
        }
        return true;
    }

    private static boolean sameArtist(String left, String right) {
        return left != null && right != null && left.equalsIgnoreCase(right);
    }
}
