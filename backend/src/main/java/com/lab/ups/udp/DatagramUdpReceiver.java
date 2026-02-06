package com.lab.ups.udp;

import lombok.extern.slf4j.Slf4j;
import org.springframework.stereotype.Component;

import java.net.DatagramPacket;
import java.net.DatagramSocket;
import java.net.InetSocketAddress;
import java.net.SocketTimeoutException;

@Slf4j
@Component
public class DatagramUdpReceiver {

    private DatagramSocket socket;
    private Thread thread;
    private volatile boolean running = false;

    // payload만 받던 Consumer<byte[]> 대신, 송신자 정보까지 받는 핸들러 추가
    @FunctionalInterface
    public interface UdpReceiveHandler {
        void onReceive(byte[] payload, String fromIp, int fromPort);
    }

    // 기본 no-op 핸들러
    private UdpReceiveHandler onReceive = (payload, ip, port) -> {};

    // setter도 새 시그니처로 변경
    public void setOnReceive(UdpReceiveHandler onReceive) {
        this.onReceive = (onReceive != null) ? onReceive : (payload, ip, port) -> {};
    }

    public synchronized void start(int bindPort) {
        if (running) return;

        try {
            socket = new DatagramSocket(null);
            socket.setReuseAddress(true);
            socket.bind(new InetSocketAddress(bindPort));
            socket.setSoTimeout(1000);

            running = true;

            thread = new Thread(this::runLoop, "ups-udp-receiver");
            thread.setDaemon(true);
            thread.start();

            log.info("UDP Receiver started. bindPort={}", bindPort);
        } catch (Exception e) {
            running = false;
            throw new RuntimeException("UDP Receiver start 실패", e);
        }
    }

    public synchronized void stop() {
        running = false;

        if (socket != null) {
            try {
                socket.close();
            } catch (Exception ignored) {}
        }
        socket = null;

        if (thread != null) {
            try {
                thread.interrupt();
            } catch (Exception ignored) {}
        }
        thread = null;

        log.info("UDP Receiver stopped.");
    }

    private void runLoop() {
        // 수신 버퍼
        byte[] buf = new byte[2048];

        while (running) {
            try {
                DatagramPacket packet = new DatagramPacket(buf, buf.length);
                socket.receive(packet);

                // payload만 정확히 복사
                int len = packet.getLength();
                byte[] payload = new byte[len];
                System.arraycopy(packet.getData(), packet.getOffset(), payload, 0, len);

                // 송신자 정보
                String srcIp = packet.getAddress().getHostAddress();
                int srcPort = packet.getPort();

                // payload + fromIp + srcIp 콜백으로 전달
                onReceive.onReceive(payload, srcIp, srcPort);

                log.debug("UDP from {}:{} bytes={}", srcIp, srcPort, len);

            } catch (SocketTimeoutException e) {
                // 타임아웃은 정상 (running 체크하며 반복)
            } catch (Exception e) {
                if (running) {
                    log.warn("UDP Receiver error", e);
                }
                // socket.close()로 깨진 경우는 stop 과정일 수 있음
            }
        }
    }
}