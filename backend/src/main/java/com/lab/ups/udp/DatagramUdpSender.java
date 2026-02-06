package com.lab.ups.udp;

import com.lab.ups.packet.PacketType;
import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.*;

@Slf4j
@Component
public class DatagramUdpSender {

    // 패킷 디버깅
    private static String toHex(byte[] data) {
        StringBuilder sb = new StringBuilder();
        for (byte b : data) {
            sb.append(String.format("%02X ", b));
        }
        return sb.toString();
    }

    /**
     * UDP 전송
     *
     * @param type    패킷 타입
     * @param dstIp   목적지 IP
     * @param dstPort 목적지 Port
     * @param srcPort 송신에 사용할 소스 Port (bind)
     * @param payload 전송 데이터(byte[])
     */
    public void send(PacketType type, String dstIp, int dstPort, int srcPort, byte[] payload) {
        // try-with-resources: 메서드 끝나면 socket 자동 close
        // new DatagramSocket(null): 아직 bind 하지 않은 상태로 생성(직접 bind하려고)
        try (DatagramSocket socket = new DatagramSocket(null)) {

            socket.setReuseAddress(true);

            socket.bind(new InetSocketAddress(srcPort));

            InetAddress address = InetAddress.getByName(dstIp);

            DatagramPacket packet = new DatagramPacket(payload, payload.length, address, dstPort);

            // System.out.println("UDP Payload HEX: " + toHex(payload));

            socket.send(packet);

            // 성공 로그
            // log.info("UDP 전송 성공: {}:{} (srcPort={}) bytes={}", dstIp, dstPort, srcPort, payload.length);

            // 성공 로그(패킷 타입 표시 로그)
            log.info(
                    "[{}] UDP 전송 성공: {}:{} (Src Port={}) bytes={}",
                    type, dstIp, dstPort, srcPort, payload.length
            );

        } catch (BindException e) {
            throw new IllegalStateException("Src Port 바인딩 실패(이미 사용 중?): " + srcPort, e);
        } catch (Exception e) {
            throw new RuntimeException("UDP 전송 실패", e);
        }
    }
}
