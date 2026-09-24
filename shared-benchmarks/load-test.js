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
  // Disables HTTP keep-alive connection reuse across iterations. Without
  // this, k6 reuses a persistent connection per VU, and with the sleep(0.5)
  // below creating real idle gaps between requests, any backend whose
  // default idle-connection timeout is shorter than that gap can close the
  // connection from its side right as k6 tries to reuse it — a classic
  // client/server keep-alive race that produces a small, consistent
  // connection-reset failure rate under sustained load rather than an
  // outright broken backend. Forcing a fresh connection per request
  // sidesteps the race entirely, uniformly across every backend under
  // test, regardless of that backend's specific idle-timeout default.
  noConnectionReuse: true,
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
