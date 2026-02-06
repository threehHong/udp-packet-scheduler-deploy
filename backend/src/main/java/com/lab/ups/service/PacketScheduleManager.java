package com.lab.ups.service;

import com.lab.ups.model.TransmissionConfig;
import com.lab.ups.model.TransmissionStatus;
import com.lab.ups.packet.PacketFactory;
import com.lab.ups.packet.PacketType;
import com.lab.ups.udp.DatagramUdpSender;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.time.OffsetDateTime;
import java.util.concurrent.*;

@Slf4j
@Component
public class PacketScheduleManager {

    private final ScheduledExecutorService executor =
            Executors.newScheduledThreadPool(2, r -> {
                Thread t = new Thread(r);
                t.setName("ups-scheduler");
                t.setDaemon(true);
                return t;
            });

    private final DatagramUdpSender udpSender;
    private final PacketFactory packetFactory;

    private volatile ScheduledFuture<?> futureA;
    private volatile ScheduledFuture<?> futureB;

    public PacketScheduleManager(DatagramUdpSender udpSender, PacketFactory packetFactory) {
        this.udpSender = udpSender;
        this.packetFactory = packetFactory;
    }

    /**
     * 스케줄 시작
     * - A: (기본) 즉시(0초) 시작, 75초 주기
     * - B: 5초 지연 후 시작, 60초 주기
     *
     * @param skipImmediateA true면 A는 즉시 1회 전송하지 않고, 75초 후부터 시작
     */
    public synchronized void start(TransmissionConfig cfg, TransmissionStatus status, boolean skipImmediateA) {
        stop();

        long initialDelayA = skipImmediateA ? 75 : 0;

        futureA = executor.scheduleAtFixedRate(
                () -> send(PacketType.A, cfg, status),
                initialDelayA, 75, TimeUnit.SECONDS
        );

        futureB = executor.scheduleAtFixedRate(
                () -> send(PacketType.B, cfg, status),
                5, 60, TimeUnit.SECONDS
        );

        log.info("스케줄 시작: A=75s(initial={}s), B=60s (B initial=5s)", initialDelayA);
    }

    // 기존 호출부 호환용(기본은 즉시 A 전송)
    public synchronized void start(TransmissionConfig cfg, TransmissionStatus status) {
        start(cfg, status, false);
    }

    public synchronized void stop() {
        if (futureA != null) futureA.cancel(true);
        if (futureB != null) futureB.cancel(true);

        futureA = null;
        futureB = null;

        log.info("스케줄 중단");
    }

    private void send(PacketType type, TransmissionConfig cfg, TransmissionStatus status) {
        if (!status.isRunning()) return;

        OffsetDateTime now = OffsetDateTime.now();
        byte[] payload = packetFactory.build(type, cfg, now);

        udpSender.send(type, cfg.getDstIp(), cfg.getDstPort(), cfg.getSrcPort(), payload);

        if (type == PacketType.A) status.markSentA(now);
        else status.markSentB(now);

        log.debug("패킷 {} 전송 완료 (siteId={}, time={})", type, cfg.getSiteId(), now);
    }
}