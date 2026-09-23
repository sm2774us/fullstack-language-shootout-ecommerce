// Standalone Node HTTP entry point for the production image.
//
// TanStack Start's Vite build (see vite.config.ts) emits dist/server/server.js
// as a Web-Fetch-API `{ fetch }` handler (its h3-v2/nitro-less server
// target) — it is NOT a self-starting listener. Running it directly with
// `node dist/server/server.js` imports the module and exits immediately
// with no output and no error, which looks deceptively like success.
// This file is the real entry point: it bridges Node's http module to
// that fetch handler using Node's built-in (undici-backed) Request/
// Response globals, and is what the Dockerfile's CMD actually runs.
import http from "node:http";
import handler from "./dist/server/server.js";

const port = process.env.PORT ? Number(process.env.PORT) : 3000;

http
  .createServer(async (req, res) => {
    try {
      const url = `http://${req.headers.host ?? `localhost:${port}`}${req.url}`;
      const hasBody = req.method !== "GET" && req.method !== "HEAD";
      const request = new Request(url, {
        method: req.method,
        headers: req.headers,
        body: hasBody ? req : undefined,
        duplex: hasBody ? "half" : undefined,
      });

      const response = await handler.fetch(request);

      res.statusCode = response.status;
      response.headers.forEach((value, key) => res.setHeader(key, value));

      if (response.body) {
        for await (const chunk of response.body) res.write(chunk);
      }
      res.end();
    } catch (err) {
      console.error(err);
      res.statusCode = 500;
      res.end("Internal Server Error");
    }
  })
  .listen(port, "0.0.0.0", () => {
    console.log(`listening on http://0.0.0.0:${port}`);
  });
