export type StartRequest = {
  dstIp: string;
  dstPort: number;
  srcPort: number;
  siteId: string;
};

export type TransmissionConfig = {
  dstIp: string;
  dstPort: number;
  srcPort: number;
  siteId: string;
};

export type StatusResponse = {
  running: boolean;
  dstIp: string | null;
  dstPort: number | null;
  srcPort: number | null;
  siteId: string | null;
};

export type RxType = "A" | "B" | "B2" | "UNKNOWN";

export type UdpRxEvent = {
  // 서버가 rxAt로 주면 rxAt, receivedAt로 주면 receivedAt -> 아래 정규화에서 처리
  rxAt: string;
  srcIp: string;
  srcPort: number;
  bytes: number;
  hex: string;
  type: RxType;
};
