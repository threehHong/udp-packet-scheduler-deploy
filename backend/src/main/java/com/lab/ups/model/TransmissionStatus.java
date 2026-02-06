package com.lab.ups.model;

import lombok.Getter;

import java.time.OffsetDateTime;

@Getter
public class TransmissionStatus {

    // 실행 중인지 여부 (스레드 간 최신값 보이도록 volatile)
    private volatile boolean running;

    // 현재 전송 설정 (start 후 설정됨)
    private volatile TransmissionConfig config;

    // 시작 시각
    private volatile OffsetDateTime startedAt;

    // 마지막 패킷 A 전송 시각
    private volatile OffsetDateTime lastSentA;

    // 마지막 패킷 B 전송 시각
    private volatile OffsetDateTime lastSentB;

    /**
     * 전송 시작 상태 세팅
     * synchronized: start/stop 등이 동시에 호출될 때 상태 꼬임 방지
     */
    public synchronized void start(TransmissionConfig cfg, OffsetDateTime now) {
        this.config = cfg;          // 설정 저장
        this.running = true;        // 실행 상태로 전환
        this.startedAt = now;       // 시작 시각 기록
        this.lastSentA = null;      // 새 시작이므로 마지막 전송 시간 초기화
        this.lastSentB = null;
    }

    /**
     * 전송 중단 처리
     * - 스케줄러는 running 체크로 전송을 막고,
     * - 스케줄 자체는 PacketScheduleManager.stop()에서 cancel
     */
    public synchronized void stop() {
        this.running = false;
    }

    // A 전송 성공 시각 기록
    public void markSentA(OffsetDateTime now) {
        this.lastSentA = now;
    }

    // B 전송 성공 시각 기록
    public void markSentB(OffsetDateTime now) {
        this.lastSentB = now;
    }
}
