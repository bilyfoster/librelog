package com.onelpro.librelog.rumble.service;

import java.time.Instant;

public record RotationScheduledItem(Long songId, String artistName, Instant scheduledAt) {
}
