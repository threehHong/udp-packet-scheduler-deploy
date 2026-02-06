"use client";

import { useEffect, useMemo, useRef, useState } from "react";

import {
  getTransmissionStatus,
  startTransmission,
  stopTransmission,
} from "./transmission.api";

import type {
  RxType,
  StartRequest,
  StatusResponse,
  UdpRxEvent,
} from "./transmission.types";

function isValidIp(ip: string) {
  const parts = ip.trim().split(".");
  if (parts.length !== 4) return false;
  return parts.every((p) => {
    if (!/^\d+$/.test(p)) return false;
    const n = Number(p);
    return n >= 0 && n <= 255;
  });
}

function isValidPort(n: number) {
  return Number.isInteger(n) && n >= 1 && n <= 65535;
}

function typeBadgeClass(t: RxType) {
  switch (t) {
    case "A":
      return "bg-sky-100 text-sky-800 border-sky-200";
    case "B":
      return "bg-amber-100 text-amber-800 border-amber-200";
    case "B2":
      return "bg-violet-100 text-violet-800 border-violet-200";
    default:
      return "bg-slate-200 text-slate-700 border-slate-300";
  }
}

function formatRxTime(iso: string) {
  const d = new Date(iso);

  const yyyy = d.getFullYear();
  const mm = String(d.getMonth() + 1).padStart(2, "0");
  const dd = String(d.getDate()).padStart(2, "0");
  const hh = String(d.getHours()).padStart(2, "0");
  const mi = String(d.getMinutes()).padStart(2, "0");
  const sec = String(d.getSeconds()).padStart(2, "0");

  return `${yyyy}-${mm}-${dd} ${hh}:${mi}:${sec}`;
}

export default function TransmissionForm() {
  const [dstIp, setDstIp] = useState("172.30.1.123");
  const [dstPort, setDstPort] = useState<number>(20000);
  const [srcPort, setSrcPort] = useState<number>(40000);
  const [siteId, setSiteId] = useState("1387787777");

  const [status, setStatus] = useState<StatusResponse | null>(null);
  const [loading, setLoading] = useState(false);
  const [errMsg, setErrMsg] = useState<string | null>(null);

  const running = status?.running ?? false;

  // ===== SSE RX LOG STATE =====
  const [rxLogs, setRxLogs] = useState<UdpRxEvent[]>([]);
  const [streamConnected, setStreamConnected] = useState(false);
  const esRef = useRef<EventSource | null>(null);

  // 로그 필터
  const [typeFilter, setTypeFilter] = useState<RxType | "ALL">("ALL");

  const payload: StartRequest = useMemo(
    () => ({
      dstIp: dstIp.trim(),
      dstPort: Number(dstPort),
      srcPort: Number(srcPort),
      siteId: siteId.trim(),
    }),
    [dstIp, dstPort, srcPort, siteId],
  );

  const canSubmit = useMemo(() => {
    if (!payload.dstIp || !isValidIp(payload.dstIp)) return false;
    if (!isValidPort(payload.dstPort)) return false;
    if (!isValidPort(payload.srcPort)) return false;
    if (!payload.siteId) return false;
    return true;
  }, [payload]);

  async function refreshStatus() {
    try {
      const s = await getTransmissionStatus();
      setStatus(s);
    } catch (e: any) {
      setErrMsg(e?.response?.data?.message ?? e?.message ?? "status 조회 실패");
    }
  }

  // ===== SSE 연결/해제 =====
  function connectStream() {
    if (esRef.current) return;

    const es = new EventSource("http://localhost:8090/api/transmission/stream");
    esRef.current = es;

    es.addEventListener("ping", () => {
      setStreamConnected(true);
    });

    es.addEventListener("udp-rx", (e: MessageEvent) => {
      try {
        const raw = JSON.parse(e.data);

        // 정규화: 서버 키 이름이 rxAt/receivedAt 섞여도 OK
        const typeRaw = (raw.type ?? "UNKNOWN") as string;
        const normalizedType: RxType =
          typeRaw === "A" || typeRaw === "B" || typeRaw === "B2"
            ? (typeRaw as RxType)
            : "UNKNOWN";

        const normalized: UdpRxEvent = {
          rxAt: raw.rxAt ?? raw.receivedAt,
          srcIp: raw.srcIp ?? raw.fromIp,
          srcPort: raw.srcPort ?? raw.fromPort,
          bytes: raw.bytes,
          hex: raw.hex,
          type: normalizedType,
        };

        if (!normalized.rxAt || !normalized.srcIp || !normalized.srcPort)
          return;

        setRxLogs((prev) => [normalized, ...prev].slice(0, 300)); // 최근 300개 유지
      } catch {
        // ignore
      }
    });

    es.onerror = () => {
      setStreamConnected(false);
      es.close();
      esRef.current = null;
    };
  }

  function disconnectStream() {
    if (esRef.current) {
      esRef.current.close();
      esRef.current = null;
    }
    setStreamConnected(false);
  }

  useEffect(() => {
    refreshStatus();
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, []);

  useEffect(() => {
    if (!running) return;
    const t = setInterval(() => {
      refreshStatus();
    }, 1000);
    return () => clearInterval(t);
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [running]);

  useEffect(() => {
    // if (running) connectStream();
    // else disconnectStream();

    connectStream();

    return () => {
      disconnectStream();
    };
    // eslint-disable-next-line react-hooks/exhaustive-deps
  }, [running]);

  async function onClickStart() {
    setErrMsg(null);
    if (!canSubmit) {
      setErrMsg("입력값을 확인해줘 (IP/Port/Site ID)");
      return;
    }

    setLoading(true);
    try {
      await startTransmission(payload);
      await refreshStatus();
    } catch (e: any) {
      setErrMsg(e?.response?.data?.message ?? e?.message ?? "전송 시작 실패");
    } finally {
      setLoading(false);
    }
  }

  async function onClickStop() {
    setErrMsg(null);
    setLoading(true);
    try {
      await stopTransmission();
      await refreshStatus();
      // 로그는 유지(요구사항)
    } catch (e: any) {
      setErrMsg(e?.response?.data?.message ?? e?.message ?? "전송 중지 실패");
    } finally {
      setLoading(false);
    }
  }

  const filteredLogs = useMemo(() => {
    if (typeFilter === "ALL") return rxLogs;
    return rxLogs.filter((x) => x.type === typeFilter);
  }, [rxLogs, typeFilter]);

  const counts = useMemo(() => {
    let a = 0,
      b = 0,
      b2 = 0,
      u = 0;
    for (const x of rxLogs) {
      if (x.type === "A") a++;
      else if (x.type === "B") b++;
      else if (x.type === "B2") b2++;
      else u++;
    }
    return { a, b, b2, u, total: rxLogs.length };
  }, [rxLogs]);

  return (
    <div className="max-w-3xl mx-auto p-6">
      <h1 className="text-xl font-bold mb-4">UDP 패킷 전송 설정</h1>

      <div className="rounded-xl border border-slate-200 bg-white shadow-sm p-5">
        {/* 1행: Dst IP / Dst Port / Src Port */}
        <div className="grid grid-cols-3 gap-4">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Dst IP
            </label>
            <input
              value={dstIp}
              onChange={(e) => setDstIp(e.target.value)}
              placeholder="예: 192.168.0.10"
              className="w-full rounded-md border border-slate-300 px-3 py-2 outline-none focus:ring-2 focus:ring-sky-200"
              disabled={loading || running}
            />
            {!isValidIp(dstIp.trim()) && dstIp.trim().length > 0 && (
              <p className="mt-1 text-xs text-rose-600">IP 형식이 아니야</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Dst Port
            </label>
            <input
              value={dstPort}
              onChange={(e) => setDstPort(Number(e.target.value))}
              type="number"
              className="w-full rounded-md border border-slate-300 px-3 py-2 outline-none focus:ring-2 focus:ring-sky-200"
              disabled={loading || running}
            />
            {!isValidPort(Number(dstPort)) && (
              <p className="mt-1 text-xs text-rose-600">1~65535</p>
            )}
          </div>

          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Src Port
            </label>
            <input
              value={srcPort}
              onChange={(e) => setSrcPort(Number(e.target.value))}
              type="number"
              className="w-full rounded-md border border-slate-300 px-3 py-2 outline-none focus:ring-2 focus:ring-sky-200"
              disabled={loading || running}
            />
            {!isValidPort(Number(srcPort)) && (
              <p className="mt-1 text-xs text-rose-600">1~65535</p>
            )}
          </div>
        </div>

        {/* 2행: Site ID + 버튼 */}
        <div className="mt-5 grid grid-cols-[1fr_auto] gap-4 items-end">
          <div>
            <label className="block text-sm font-semibold text-slate-700 mb-1">
              Site ID
            </label>
            <input
              value={siteId}
              onChange={(e) => setSiteId(e.target.value)}
              placeholder="예: SITE-001"
              className="w-full rounded-md border border-slate-300 px-3 py-2 outline-none focus:ring-2 focus:ring-sky-200"
              disabled={loading || running}
            />
          </div>

          {!running ? (
            <button
              onClick={onClickStart}
              disabled={loading || !canSubmit}
              className="h-[42px] px-6 rounded-md bg-sky-600 text-white font-semibold disabled:opacity-50"
            >
              {loading ? "처리중..." : "전송"}
            </button>
          ) : (
            <button
              onClick={onClickStop}
              disabled={loading}
              className="h-[42px] px-6 rounded-md bg-rose-600 text-white font-semibold disabled:opacity-50"
            >
              {loading ? "처리중..." : "취소"}
            </button>
          )}
        </div>

        {/* 상태 표시 */}
        <div className="mt-5 rounded-lg bg-slate-50 border border-slate-200 p-4">
          <div className="flex items-center gap-2">
            <span className="text-sm font-semibold text-slate-700">상태</span>
            <span
              className={[
                "text-xs font-bold px-2 py-1 rounded-full",
                running
                  ? "bg-emerald-100 text-emerald-700"
                  : "bg-slate-200 text-slate-700",
              ].join(" ")}
            >
              {running ? "RUNNING" : "STOPPED"}
            </span>
          </div>

          <div className="mt-3 text-sm text-slate-700 grid grid-cols-2 gap-2">
            <div>
              <span className="font-semibold">Dst:</span> {status?.dstIp ?? "-"}
              :{status?.dstPort ?? "-"}
            </div>
            <div>
              <span className="font-semibold">Src Port:</span>{" "}
              {status?.srcPort ?? "-"}
            </div>
            <div className="col-span-2">
              <span className="font-semibold">Site ID:</span>{" "}
              {status?.siteId ?? "-"}
            </div>
          </div>

          {errMsg && (
            <p className="mt-3 text-sm text-rose-700 font-semibold">{errMsg}</p>
          )}

          {/* ===== 응답 로그(SSE) ===== */}
          <div className="mt-4 rounded-lg bg-white border border-slate-200">
            <div className="flex items-center gap-2 px-4 py-2 border-b border-slate-200 flex-wrap">
              <span className="text-sm font-semibold text-slate-700">
                응답 로그
              </span>

              {/* <span
                className={[
                  "text-xs font-bold px-2 py-1 rounded-full",
                  streamConnected
                    ? "bg-emerald-100 text-emerald-700"
                    : "bg-slate-200 text-slate-700",
                ].join(" ")}
              >
                {streamConnected ? "STREAM ON" : "STREAM OFF"}
              </span> */}

              <span className="text-xs text-slate-500">
                (TOTAL {counts.total} / A {counts.a} / B {counts.b} / B2{" "}
                {counts.b2} / U {counts.u})
              </span>

              <div className="ml-auto flex items-center gap-2">
                <select
                  value={typeFilter}
                  onChange={(e) => setTypeFilter(e.target.value as any)}
                  className="text-xs border border-slate-300 rounded-md px-2 py-1 bg-white"
                >
                  <option value="ALL">ALL</option>
                  <option value="A">A</option>
                  <option value="B">B</option>
                  <option value="B2">B2</option>
                  <option value="UNKNOWN">UNKNOWN</option>
                </select>

                <button
                  type="button"
                  onClick={() => setRxLogs([])}
                  className="text-xs px-3 py-1 rounded-md border border-slate-300 bg-white disabled:opacity-50"
                  disabled={!rxLogs.length}
                >
                  로그 비우기
                </button>
              </div>
            </div>

            <div className="max-h-64 overflow-auto">
              {filteredLogs.length === 0 ? (
                <div className="px-4 py-3 text-sm text-slate-500">
                  아직 수신된 응답이 없어
                </div>
              ) : (
                <ul className="divide-y divide-slate-200">
                  {filteredLogs.map((x, idx) => (
                    <li key={`${x.rxAt}-${idx}`} className="px-4 py-3">
                      <div className="text-xs text-slate-600 flex flex-wrap gap-x-3 gap-y-1 items-center">
                        <span className="inline-flex items-center">
                          <span
                            className={[
                              "text-[11px] font-extrabold px-2 py-0.5 rounded-full border",
                              typeBadgeClass(x.type),
                            ].join(" ")}
                          >
                            {x.type}
                          </span>
                        </span>

                        <span className="font-semibold">
                          {formatRxTime(x.rxAt)}
                        </span>

                        <span>
                          <span className="font-semibold">SRC</span> {x.srcIp}:
                          {x.srcPort}
                        </span>

                        <span>
                          <span className="font-semibold">BYTES</span> {x.bytes}
                        </span>
                      </div>

                      <pre className="mt-2 text-xs bg-slate-50 border border-slate-200 rounded-md p-2 whitespace-pre-wrap break-words">
                        {x.hex}
                      </pre>
                    </li>
                  ))}
                </ul>
              )}
            </div>
          </div>
          {/* ===== /응답 로그 ===== */}
        </div>
      </div>
    </div>
  );
}
