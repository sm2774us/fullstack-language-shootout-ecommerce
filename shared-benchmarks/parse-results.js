// Aggregates every benchmarking-results/<backend>-report.json produced by
// load-test.js into a single benchmarking-results/summary.md table.
const fs = require("fs");
const path = require("path");

const targetDir = path.join(__dirname, "..", "benchmarking-results");
const files = fs
  .readdirSync(targetDir)
  .filter((f) => f.endsWith("-report.json"))
  .sort();

if (files.length === 0) {
  console.error(`No *-report.json files found in ${targetDir}. Run the k6-test target first.`);
  process.exit(1);
}

let markdown = "## Comparative Performance Matrix Run Summary\n\n";
markdown += "| Backend Stack | Total Requests | Requests/Sec | P(95) Latency | Failure Rate |\n";
markdown += "| ------------- | --------------- | ------------- | -------------- | ------------ |\n";

for (const file of files) {
  const lang = file.replace("-report.json", "").toUpperCase();
  const raw = fs.readFileSync(path.join(targetDir, file), "utf8");
  const parsed = JSON.parse(raw);

  const m = parsed.metrics ?? {};
  const totalReqs = m.http_reqs?.values?.count ?? "n/a";
  const reqRate = m.http_reqs?.values?.rate?.toFixed(2) ?? "n/a";
  const p95 = m.http_req_duration?.values?.["p(95)"]?.toFixed(2) ?? "n/a";
  const failRate = m.http_req_failed?.values?.rate != null
    ? (m.http_req_failed.values.rate * 100).toFixed(2) + "%"
    : "n/a";

  markdown += `| **${lang}** | ${totalReqs} | ${reqRate}/s | ${p95}ms | ${failRate} |\n`;
}

fs.writeFileSync(path.join(targetDir, "summary.md"), markdown);
console.log(markdown);
