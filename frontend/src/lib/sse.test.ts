import { describe, expect, it } from "vitest";
import { SseParser, readSse } from "@/lib/sse";

function streamOf(...chunks: Uint8Array[]): ReadableStream<Uint8Array> {
  return new ReadableStream({
    start(controller) {
      chunks.forEach(chunk => controller.enqueue(chunk));
      controller.close();
    }
  });
}

describe("SseParser", () => {
  it("returns an event only after its blank line, even when chunks split it anywhere", () => {
    const parser = new SseParser();
    expect(parser.push('event: token\ndata: {"delta":"Sal')).toEqual([]);
    expect(parser.push('ut"}\n')).toEqual([]);
    expect(parser.push("\n")).toEqual([{ event: "token", data: '{"delta":"Salut"}' }]);
  });

  it("ignores comment lines, which the server uses as heartbeat", () => {
    const frames = new SseParser().push(": keep-alive\n\nevent: done\ndata: {}\n\n");
    expect(frames).toEqual([{ event: "done", data: "{}" }]);
  });

  it("handles CRLF even when \\r and \\n arrive in different chunks", () => {
    const parser = new SseParser();
    expect(parser.push("event: meta\r")).toEqual([]);
    expect(parser.push("\ndata: 1\r\n\r")).toEqual([]);
    expect(parser.push("\n")).toEqual([{ event: "meta", data: "1" }]);
  });

  it("joins multiple data lines with a newline and defaults the event name to message", () => {
    expect(new SseParser().push("data: a\ndata: b\n\n")).toEqual([{ event: "message", data: "a\nb" }]);
  });

  it("drops an event that was never terminated", () => {
    expect(new SseParser().push("event: token\ndata: {}\n")).toEqual([]);
  });
});

describe("readSse", () => {
  it("decodes a multi-byte character split across two chunks", async () => {
    const bytes = new TextEncoder().encode('event: token\ndata: {"delta":"mașină"}\n\n');
    const split = bytes.indexOf(0xc8) + 1; // la mijlocul lui „ș” (2 octeți)
    const frames = [];
    for await (const frame of readSse(streamOf(bytes.slice(0, split), bytes.slice(split)))) frames.push(frame);
    expect(frames).toEqual([{ event: "token", data: '{"delta":"mașină"}' }]);
  });
});
