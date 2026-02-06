package com.lab.ups.dto;

import jakarta.validation.constraints.Max;
import jakarta.validation.constraints.Min;
import jakarta.validation.constraints.NotBlank;
import lombok.Getter;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
public class StartRequestDto {

    // 목적지 IP (필수)
    @NotBlank(message = "dstIp는 필수입니다.")
    private String dstIp;

    // 목적지 포트 (1~65535 범위)
    @Min(value = 1, message = "dstPort는 1~65535 범위여야 합니다.")
    @Max(value = 65535, message = "dstPort는 1~65535 범위여야 합니다.")
    private int dstPort;

    // 소스 포트 (1~65535 범위)
    @Min(value = 1, message = "srcPort는 1~65535 범위여야 합니다.")
    @Max(value = 65535, message = "srcPort는 1~65535 범위여야 합니다.")
    private int srcPort;

    // 사이트 ID (필수)
    @NotBlank(message = "siteId는 필수입니다.")
    private String siteId;
}