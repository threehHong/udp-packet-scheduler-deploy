package com.lab.ups.packet;

import com.lab.ups.model.TransmissionConfig;
import org.springframework.stereotype.Component;

import java.nio.ByteBuffer;
import java.nio.charset.StandardCharsets;
import java.time.OffsetDateTime;
import java.time.ZoneId;
import java.time.format.DateTimeFormatter;

@Component
public class PacketFactory {
    /**
     * SECURITY: 실제 패킷 생성 로직 제거
     * 
     * 내부 프로토콜 규격에 따른
     * 패킷 생성 로직이 존재하나,
     * 보안 정책에 따라 공개 저장소에서는 제거했습니다.
     */
}