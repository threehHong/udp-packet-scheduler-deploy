# UDP 기반 패킷 주기 전송 모듈 (UDP Periodic Packet Sender)

## 개요

입력받은 **Dst IP / Dst Port / Src Port / Site ID**를 기반으로, 미리 정의된 **패킷 A/B**를 정해진 규칙에 따라 **UDP로 주기 전송**하는 모듈입니다.  
장비(목적지)로 부터 수신되는 UDP 응답 패킷을 실시간으로 수신하고, 이를 SSE(Server-Sent Events) 스트림(/stream)으로 프론트에 실시간으로 전달합니다.

## 주요 기능

### 1) **전송 시작/중지**

- 사용자 입력(Dst IP, Dst Port, Src Port, Site ID)을 받아 전송 시작
- 전송 중지 시 스케줄 즉시 종료

### 2) **패킷 전송 규칙(요구사항)**

1. 전송 시작 시 **패킷 A 즉시 전송**
2. **5초 뒤 패킷 B 전송**
3. **패킷 A 전송 시점 기준 1분 15초 후 패킷 A 재전송**
4. **패킷 B 전송 시점 기준 1분 후 패킷 B 재전송**
5. 이후 3, 4번 동일 패턴 반복

### 3) UDP 응답 수신 및 타입 구분

- `Src Port`로 UDP 소켓을 바인딩하여 **응답 패킷 수신**
- 수신 패킷을 **A / B / B2 / UNKNOWN** 으로 구분하여 이벤트에 포함

### 4) 실시간 응답 스트리밍(SSE)

- 수신된 UDP 응답을 **SSE(/api/transmission/stream)** 로 실시간 스트리밍
- 프론트에서 `EventSource`로 구독하여 **실시간 로그 UI**로 표시

### 5) **상태 조회**

- 현재 전송 중 여부(running)
- 현재 전송 설정값(dstIp, dstPort, srcPort, siteId)
- 최근 전송 시각(lastSentA, lastSentB)

## 기술 스택

### Backend

- Java / Spring Boot
- Gradle
- UDP 전송: `DatagramSocket`
- 실시간 스트림: SSE(Server-Sent Events) + SseEmitter

### Frontend

- Next.js (App Router)
- TypeScript
- Tailwind CSS
- SSE 구독: EventSource

## 프로젝트 구조 (예시)

```bash
backend/
  src/main/java/...
    controller/
      TransmissionController.java
    dto/
      StartRequestDto.java
      StartResponseDto.java
      StatusResponseDto.java
      UdpRxEventDto.java
    global/
      security/
        ApiSecurityConfig.java
      webMvc
        WebMvcConfig.java
    model/
      TransmissionConfig.java
      TransmissionStatus.java
    packet/
      PacketFactory.java
      PacketType.java
    service/
      PacketScheduleManager.java
      TransmissionService.java
    sse
      SseHub.java
    udp/
      DatagramUdpReceiver.java
      DatagramUdpSender.java
    UpsApplication.java
frontend/
  app/
    features/
      transmission/
        transmission.api.ts
        transmission.types.ts
        TransmissionForm.tsx
    transmission/
      page.tsx
docker-compose.yml
```

## 실행 방법

### 1) 로컬 실행

#### Backend

```bash
cd backend
./gradlew bootRun
```

#### Frontend

```bash
cd frontend
npm install
npm run dev
```

- Frontend: `http://localhost:3000`
- Backend: `http://localhost:8090`

### 2) Docker로 실행

```bash
docker compose up -d --build
```

## API 명세

Base URL: `/api/transmission`

### 1) 전송 시작

- **POST** `/start`

Request Body (예시)

```json
{
  "firstResponseAt": null,
  "firstResponseBytes": null,
  "firstResponseHex": null,
  "running": true,
  "startedAt": "2026-02-06T01:46:25.869264951Z"
}
```

Response Body (예시)

```json
{
  "running": true,
  "startedAt": "2026-02-05T10:57:55.7666432+09:00"
}
```

| start 응답은 전송 시작 결과이며 UDP 응답 로그는 /stream(SSE)로 실시간 수신

### 2) 전송 중지

- **POST** `/stop`

Response

- `200 OK`

### 3) 상태 조회

- **GET** `/status`

Response Body (예시)

```json
{
  "dstIp": "172.30.1.123",
  "dstPort": 20000,
  "lastSentA": "2026-02-06T01:47:40.871801696Z",
  "lastSentB": "2026-02-06T01:48:30.868403208Z",
  "running": true,
  "siteId": "1020492030",
  "srcPort": 40000,
  "startedAt": "2026-02-06T01:46:25.869264951Z"
}
```

### 4) UDP 응답 스트림 (SSE)

- **GET** `/stream`
- `Content-Type: text/event-stream`

브라우저/클라이언트는 `EventSource`로 구독합니다.

SSE 이벤트 예시:

```
event: udp-rx
data: {"bytes":23,"hex":"FF 00 41 32 53 ...","receivedAt":"2026-02-05T15:12:04.2881851+09:00","srcIp":"172.30.1.123","srcPort":20000,"type":"A"}
```

`udp-rx` 데이터 필드:

- `type`: `A | B | B2 | UNKNOWN`
- `srcIp`, `srcPort`: 응답을 보낸 상대(장비) 정보
- `receivedAt`: 수신 시각(ISO-8601)
- `hex`: 수신 payload를 HEX 문자열로 변환한 값
- `bytes`: payload 길이

## 입력값 설명

| 항목     | 설명                          | 예시           |
| -------- | ----------------------------- | -------------- |
| Dst IP   | UDP 목적지 IP                 | `192.168.0.10` |
| Dst Port | UDP 목적지 Port               | `9000`         |
| Src Port | UDP 송신 Port                 | `9001`         |
| Site ID  | 패킷에 포함되는 사이트 식별자 | `SITE-001`     |

## 동작 흐름

1. 사용자가 Dst IP / Dst Port / Src Port / Site ID 입력 후 **전송 버튼 클릭**
2. Backend가 스케줄 시작
   - 즉시 **Packet A 전송**
   - **5초 후 Packet B 전송**
   - **패킷 A 전송 시점 기준 1분 15초 후 Packet A 재전송**
   - **패킷 B 전송 시점 기준 1분 후 Packet B 재전송**

3. Backend는 `Src Port`로 UDP 응답을 수신하고, 수신 이벤트를 SSE로 푸시
4. Frontend는 `EventSource`로 `/api/transmission/stream`을 구독하여 **실시간 응답 로그 표시**
5. 사용자가 **취소 버튼 클릭** 또는 `/stop` 호출 시 스케줄 종료
   - (UI) 기존 로그는 유지되며, 필요 시 **로그 비우기**로 초기화 가능

## 개발/운영 시 참고

- /start 응답은 “전송 시작” 결과만 반환하며, 주기 전송 중 발생하는 응답은 /stream로 수신합니다.
- 필요 시 다음 항목을 확장할 수 있습니다.
  - 전송/수신 이력 저장(메모리/DB)
  - 재전송/에러 핸들링 강화
  - 멀티 Site 동시 전송 지원
  - 스트림 인증/인가 적용(운영 환경)
