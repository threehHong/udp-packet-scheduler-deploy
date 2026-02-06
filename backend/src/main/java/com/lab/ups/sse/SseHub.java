package com.lab.ups.sse;

import com.lab.ups.dto.UdpRxEventDto;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;

import java.io.IOException;
import java.util.List;
import java.util.concurrent.CopyOnWriteArrayList;

@Slf4j
@Component
public class SseHub {

    private final List<SseEmitter> emitters = new CopyOnWriteArrayList<>();

    public SseEmitter connect() {
        // 0L = 타임아웃 없음(필요하면 30분 등으로 바꿔도 됨)
        SseEmitter emitter = new SseEmitter(0L);

        emitters.add(emitter);

        emitter.onCompletion(() -> emitters.remove(emitter));
        emitter.onTimeout(() -> emitters.remove(emitter));
        emitter.onError(e -> emitters.remove(emitter));

        // 연결 확인용 1회 ping
        try {
            emitter.send(SseEmitter.event().name("ping").data("connected"));
        } catch (IOException e) {
            emitters.remove(emitter);
        }

        return emitter;
    }

    public void broadcast(UdpRxEventDto event) {
        for (SseEmitter emitter : emitters) {
            try {
                emitter.send(SseEmitter.event().name("udp-rx").data(event));
            } catch (Exception e) {
                emitters.remove(emitter);
            }
        }
    }
}
