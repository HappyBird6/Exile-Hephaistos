import fs from "node:fs";
import http from "node:http";
import https from "node:https";
import os from "node:os";
import { performance } from "node:perf_hooks";
import { gzipSync, gunzipSync } from "node:zlib";
import { randomUUID } from "node:crypto";

// Explicit target only. Never discover or probe a production deployment automatically.
const target = process.env.PERF_API_URL;
if (!target) throw Error("Set PERF_API_URL to the authorized test server");
const origin = new URL(target);
if (!["http:", "https:"].includes(origin.protocol))
  throw Error("Expected HTTP(S) target");
const workerCounts = (process.env.PERF_CONCURRENCY ?? "1,4,8,16")
  .split(",")
  .map(Number);
const requestsPerWorker = Number(process.env.PERF_REQUESTS_PER_WORKER ?? 20);
if (
  workerCounts.some((n) => !Number.isInteger(n) || n < 1 || n > 64) ||
  !Number.isInteger(requestsPerWorker) ||
  requestsPerWorker < 1 ||
  requestsPerWorker > 1000
)
  throw Error("Invalid bounded load parameters");
const clients = {
  "http:": new http.Agent({ keepAlive: true }),
  "https:": new https.Agent({ keepAlive: true }),
};
async function call(path, body, identity) {
  const started = performance.now(),
    url = new URL(path, origin);
  return new Promise((resolve, reject) => {
    const request = (url.protocol === "https:" ? https : http).request(
      url,
      {
        agent: clients[url.protocol],
        method: body ? "POST" : "GET",
        headers: {
          "Accept-Encoding": "gzip",
          ...(body
            ? {
                "Content-Type": "application/json",
                "X-Crafting-Ruleset": identity,
              }
            : {}),
        },
        timeout: 30_000,
      },
      (response) => {
        const buffers = [];
        response.on("data", (data) => buffers.push(data));
        response.on("error", reject);
        response.on("end", () => {
          try {
            const bytes = Buffer.concat(buffers),
              decoded =
                response.headers["content-encoding"] === "gzip"
                  ? gunzipSync(bytes)
                  : bytes;
            resolve({
              status: response.statusCode,
              bytes: bytes.length,
              decodedBytes: decoded.length,
              ms: performance.now() - started,
              text: decoded.toString(),
              headers: response.headers,
            });
          } catch (error) {
            reject(error);
          }
        });
      },
    );
    request.on("timeout", () =>
      request.destroy(Error("Request deadline exceeded")),
    );
    request.on("error", reject);
    request.end(body ? JSON.stringify(body) : undefined);
  });
}
const initialPath =
  "/api/v1/crafting/workbench/initial?base=solar&itemLevel=100&manualReveal=true&catalog=separate";
const response = await call(initialPath);
if (response.status !== 200) throw Error(`Initial: ${response.status}`);
const initial = JSON.parse(response.text),
  identity = response.headers["x-crafting-ruleset"] ?? initial.rulesetIdentity;
let state = {
  snapshotId: initial.state.snapshotId,
  baseItemId: initial.state.baseItemId,
  itemLevel: 100,
  rarity: "RARE",
  implicits: initial.state.implicits,
  explicits: [],
  conditions: [],
};
if (process.env.PERF_ITEM_FIXTURE)
  state = JSON.parse(fs.readFileSync(process.env.PERF_ITEM_FIXTURE, "utf8"));
else
  for (let i = 0; i < 3; i++) {
    const applied = await call(
      "/api/v1/crafting/workbench/apply",
      { state, action: "EXALTED", activeOmens: [] },
      identity,
    );
    if (applied.status !== 200 || !JSON.parse(applied.text).applied)
      throw Error("Could not prepare workload");
    state = JSON.parse(applied.text).state;
  }
const processStats = () => {
  if (!process.env.PERF_SERVER_PID) return undefined;
  const stat = fs
    .readFileSync(`/proc/${Number(process.env.PERF_SERVER_PID)}/stat`, "utf8")
    .split(") ")[1]
    .split(" ");
  return {
    userTicks: Number(stat[11]),
    systemTicks: Number(stat[12]),
    residentPages: Number(stat[21]),
  };
};
const result = {
  target,
  at: new Date().toISOString(),
  runtime: process.version,
  cpus: os.availableParallelism(),
  requestsPerWorker,
  item: state,
  assets: [],
  scenarios: [],
};
if (fs.existsSync("frontend/dist/assets"))
  for (const name of fs.readdirSync("frontend/dist/assets"))
    if (name.endsWith(".js")) {
      const bytes = fs.readFileSync(`frontend/dist/assets/${name}`);
      result.assets.push({
        name,
        bytes: bytes.length,
        gzip: gzipSync(bytes).length,
      });
    }
const scenarios = ["initial", "pools", "apply", "mixed"];
function workload(scenario, index) {
  if (scenario === "initial") return call(initialPath);
  if (scenario === "mixed" && index % 10 === 0)
    return call(
      "/api/v1/crafting/workbench/chaos-repeat",
      {
        requestId: randomUUID(),
        state: { kind: "CONCRETE", item: state },
        action: "CHAOS",
        activeOmens: [],
        pattern: "__never_matches__",
        locale: "en",
        maximumUses: 1000,
      },
      identity,
    );
  return call(
    `/api/v1/crafting/workbench/${scenario === "pools" ? "modifier-pools" : "apply"}`,
    scenario === "pools"
      ? { state, dynamic: true }
      : { state, action: "EXALTED", activeOmens: [] },
    identity,
  );
}
try {
  // Warm catalog hashes, serializers and JIT before timed comparisons.
  for (const scenario of scenarios)
    for (let i = 0; i < 10; i++) await workload(scenario, i);
  for (const concurrency of workerCounts)
    for (const scenario of scenarios) {
      const durations = [],
        success = [],
        statuses = {};
      let bytes = 0,
        decodedBytes = 0;
      const started = performance.now(),
        cpuBefore = processStats();
      await Promise.all(
        Array.from({ length: concurrency }, async (_, worker) => {
          for (let n = 0; n < requestsPerWorker; n++) {
            try {
              const response = await workload(
                scenario,
                worker * requestsPerWorker + n,
              );
              bytes += response.bytes;
              decodedBytes += response.decodedBytes;
              statuses[response.status] = (statuses[response.status] ?? 0) + 1;
              durations.push(response.ms);
              if (response.status === 200) success.push(response.ms);
            } catch {
              statuses.NETWORK_ERROR = (statuses.NETWORK_ERROR ?? 0) + 1;
            }
          }
        }),
      );
      const percentiles = (values) => {
        values.sort((a, b) => a - b);
        return Object.fromEntries(
          [50, 95, 99].map((p) => [
            `p${p}`,
            values[
              Math.min(values.length - 1, Math.floor((values.length * p) / 100))
            ] ?? null,
          ]),
        );
      };
      result.scenarios.push({
        scenario,
        concurrency,
        requests: concurrency * requestsPerWorker,
        statuses,
        bytes,
        decodedBytes,
        ...percentiles(durations),
        successfulLatency: percentiles(success),
        elapsedMs: performance.now() - started,
        cpuBefore,
        cpuAfter: processStats(),
      });
    }
} finally {
  for (const agent of Object.values(clients)) agent.destroy();
}
console.log(JSON.stringify(result, null, 2));
