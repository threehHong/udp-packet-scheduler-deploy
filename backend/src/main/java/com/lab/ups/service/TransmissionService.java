package com.lab.ups.service;

import com.lab.ups.dto.StartRequestDto;
import com.lab.ups.dto.StartResponseDto;
import com.lab.ups.dto.UdpRxEventDto;
import com.lab.ups.model.TransmissionConfig;
import com.lab.ups.model.TransmissionStatus;
import com.lab.ups.packet.PacketType;
import com.lab.ups.sse.SseHub;
import com.lab.ups.udp.DatagramUdpReceiver;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Service;

import java.time.OffsetDateTime;

@Slf4j
@Service
public class TransmissionService {

    private final TransmissionStatus status = new TransmissionStatus();

    private final PacketScheduleManager scheduleManager;
    private final DatagramUdpReceiver receiver;
    private final SseHub sseHub;

    private static PacketType detectType(byte[] payload) {
        /**
         * SECURITY: 실제 패킷 타입 감지 로직 제거
         * 
         * 패킷 타입 감지 로직이 존재하나,
         * 보안 정책에 따라 공개 저장소에서는 제거했습니다.
         */
    }

    public TransmissionService(PacketScheduleManager scheduleManager, DatagramUdpReceiver receiver, SseHub sseHub) {
        this.scheduleManager = scheduleManager;
        this.receiver = receiver;
        this.sseHub = sseHub;
    }

    public TransmissionStatus getStatus() {
        return status;
    }

    public synchronized StartResponseDto start(StartRequestDto req) {
        if (status.isRunning()) {
            log.info("이미 실행 중 → 기존 스케줄 중단 후 재시작");
            stop();
        }

        TransmissionConfig cfg = new TransmissionConfig(
                req.getDstIp(),
                req.getDstPort(),
                req.getSrcPort(),
                req.getSiteId()
        );

        OffsetDateTime now = OffsetDateTime.now();
        status.start(cfg, now);

        // 수신 시작 (srcPort로 바인딩)
        receiver.start(cfg.getSrcPort());

        // 응답 들어올 때마다 SSE로 푸시
        receiver.setOnReceive((bytes, srcIp, srcPort) -> {
            String hex = toHex(bytes);
            PacketType type = detectType(bytes);

            UdpRxEventDto event = UdpRxEventDto.builder()
                    .receivedAt(OffsetDateTime.now())
                    .srcIp(srcIp)
                    .srcPort(srcPort)
                    .bytes(bytes != null ? bytes.length : 0)
                    .hex(hex)
                    .type(type)
                    .build();

            sseHub.broadcast(event);

            log.info("UDP RX from {}:{} bytes={}", srcIp, srcPort, bytes.length);
            log.info("응답: {}", hex);
        });

        // 주기 전송 시작
        scheduleManager.start(cfg, status);

        return new StartResponseDto(true, now);
    }

    public synchronized void stop() {
        status.stop();
        scheduleManager.stop();
        receiver.stop();
    }

    private static String toHex(byte[] data) {
        if (data == null) return null;
        StringBuilder sb = new StringBuilder();
        for (byte b : data) sb.append(String.format("%02X ", b));
        return sb.toString().trim();
    }
}