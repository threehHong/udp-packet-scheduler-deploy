import axios from "axios";
import type { StartRequest, StatusResponse } from "./transmission.types";

const api = axios.create({
  baseURL: "http://localhost:8090",
  timeout: 10_000,
});

export async function startTransmission(payload: StartRequest) {
  // Controller: POST /api/transmission/start
  const { data } = await api.post("/api/transmission/start", payload);
  // console.log("data", data);
  return data;
}

export async function stopTransmission() {
  // Controller: POST /api/transmission/stop
  await api.post("/api/transmission/stop");
}

export async function getTransmissionStatus() {
  // Controller: GET /api/transmission/status
  const { data } = await api.get<StatusResponse>("/api/transmission/status");
  return data;
}
