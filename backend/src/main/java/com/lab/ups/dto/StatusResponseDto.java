package com.lab.ups.dto;

import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class StatusResponseDto {

    // 현재 실행 중인지 여부
    private final boolean running;

    // 실행 중이면 현재 config 값이 내려가고,
    // 실행 전이면 null이 내려갈 수 있어 Integer/String 사용
    private final String dstIp;
    private final Integer dstPort;
    private final Integer srcPort;
    private final String siteId;

    // 전송 시작 시각
    private final OffsetDateTime startedAt;

    // 마지막으로 전송된 패킷 A 시각
    private final OffsetDateTime lastSentA;

    // 마지막으로 전송된 패킷 B 시각
    private final OffsetDateTime lastSentB;
}
