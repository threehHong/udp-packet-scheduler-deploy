package com.lab.ups.dto;

import lombok.AllArgsConstructor;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class StartResponseDto {

    private final boolean running;
    private final OffsetDateTime startedAt;
    private final String firstResponseHex;
    private final Integer firstResponseBytes;
    private final OffsetDateTime firstResponseAt;

    // 기존 start 응답용
    public StartResponseDto(boolean running, OffsetDateTime startedAt) {
        this.running = running;
        this.startedAt = startedAt;
        this.firstResponseHex = null;
        this.firstResponseBytes = null;
        this.firstResponseAt = null;
    }

    // 전체 필드 생성자
    public StartResponseDto(
            boolean running,
            OffsetDateTime startedAt,
            String firstResponseHex,
            Integer firstResponseBytes,
            OffsetDateTime firstResponseAt
    ) {
        this.running = running;
        this.startedAt = startedAt;
        this.firstResponseHex = firstResponseHex;
        this.firstResponseBytes = firstResponseBytes;
        this.firstResponseAt = firstResponseAt;
    }
}