package com.lab.ups.dto;

import com.lab.ups.packet.PacketType;
import lombok.Builder;
import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
@Builder
public class UdpRxEventDto {
    private final OffsetDateTime receivedAt;
    private final String srcIp;
    private final Integer srcPort;
    private final Integer bytes;
    private final String hex; // 응답 payload HEX
    private final PacketType type;
}

