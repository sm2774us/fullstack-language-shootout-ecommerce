import http from "k6/http";
import { check, sleep } from "k6";

const targetUrl = __ENV.TARGET_URL || "http://localhost:8080";
const backendName = __ENV.BACKEND_NAME || "unknown";

export const options = {
  stages: [
    { duration: "15s", target: 30 },
    { duration: "30s", target: 30 },
    { duration: "15s", target: 0 },
  ],
  thresholds: {
    http_req_failed: ["rate<0.01"],
    http_req_duration: ["p(95)<250"],
  },
};

export default function () {
  const res = http.get(`${targetUrl}/api/perf`);
  check(res, { "status is 200": (r) => r.status === 200 });
  sleep(0.5);
}

export function handleSummary(data) {
  const filepath = `benchmarking-results/${backendName}-report.json`;
  return {
    stdout: `\n[k6] Evaluation completed for backend: ${backendName}. Writing ${filepath}\n`,
    [filepath]: JSON.stringify(data, null, 2),
  };
}
