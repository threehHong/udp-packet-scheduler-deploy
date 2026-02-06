package com.lab.ups.controller;

import com.lab.ups.dto.StartRequestDto;
import com.lab.ups.dto.StartResponseDto;
import com.lab.ups.dto.StatusResponseDto;
import com.lab.ups.model.TransmissionConfig;
import com.lab.ups.model.TransmissionStatus;
import com.lab.ups.service.TransmissionService;
import com.lab.ups.sse.SseHub;
import jakarta.validation.Valid;
import lombok.extern.slf4j.Slf4j;
import org.springframework.web.bind.annotation.*;
import org.springframework.web.servlet.mvc.method.annotation.SseEmitter;


@RestController
@RequestMapping("/api/transmission") 
public class TransmissionController {

    private final TransmissionService transmissionService;

    private final SseHub sseHub;

    public TransmissionController(TransmissionService transmissionService, SseHub sseHub) {
        this.transmissionService = transmissionService;
        this.sseHub = sseHub;
    }

    @PostMapping("/start")
    public StartResponseDto start(@RequestBody @Valid StartRequestDto req) {
        return transmissionService.start(req);
    }

    @PostMapping("/stop")
    public void stop() {
        transmissionService.stop();
    }

    @GetMapping("/status")
    public StatusResponseDto status() {
        TransmissionStatus s = transmissionService.getStatus();

        TransmissionConfig cfg = s.getConfig();

        return StatusResponseDto.builder()
                .running(s.isRunning())
                .dstIp(cfg != null ? cfg.getDstIp() : null)
                .dstPort(cfg != null ? cfg.getDstPort() : null)
                .srcPort(cfg != null ? cfg.getSrcPort() : null)
                .siteId(cfg != null ? cfg.getSiteId() : null)
                .startedAt(s.getStartedAt())
                .lastSentA(s.getLastSentA())
                .lastSentB(s.getLastSentB())
                .build();
    }

    @GetMapping(value = "/stream", produces = "text/event-stream")
    public SseEmitter stream() {
        return sseHub.connect();
    }
}
