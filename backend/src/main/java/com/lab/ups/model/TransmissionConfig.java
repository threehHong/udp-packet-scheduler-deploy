package com.lab.ups.model;

import lombok.AllArgsConstructor;
import lombok.Getter;

@AllArgsConstructor
@Getter
public class TransmissionConfig {

    // 목적지 IP
    private final String dstIp;

    // 목적지 포트
    private final int dstPort;

    // 송신 소스 포트
    private final int srcPort;

    // 사이트 ID
    private final String siteId;
}