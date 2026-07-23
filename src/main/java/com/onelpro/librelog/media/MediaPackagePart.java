package com.onelpro.librelog.media;

import jakarta.persistence.*;
import lombok.AllArgsConstructor;
import lombok.Builder;
import lombok.Data;
import lombok.NoArgsConstructor;

import java.util.UUID;

/**
 * One airable window of a package. Cue fields null = play the whole file (multi-file
 * workflow); cue fields set = play [cueIn, cueOut) of a shared file (break points).
 */
@Entity
@Table(name = "media_package_part")
@Data
@NoArgsConstructor
@AllArgsConstructor
@Builder
public class MediaPackagePart {

    @Id
    private UUID id;

    @Column(name = "package_id", nullable = false)
    private UUID packageId;

    /** 1-based air order; matched by FEATURE slots' featureSequence. */
    @Column(nullable = false)
    private int sequence;

    @Column(name = "librtime_file_id", nullable = false)
    private Long librtimeFileId;

    @Column(name = "cue_in_seconds")
    private Integer cueInSeconds;

    @Column(name = "cue_out_seconds")
    private Integer cueOutSeconds;

    /** Played seconds of this part (cueOut - cueIn, or the file length). */
    @Column(name = "length_seconds", nullable = false)
    private int lengthSeconds;

    private String title;

    @PrePersist
    void prePersist() {
        if (id == null) id = UUID.randomUUID();
    }
}
