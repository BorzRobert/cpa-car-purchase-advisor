import { describe, expect, it } from "vitest";
import { parseAdvisorEvent, type VehicleCard } from "@/lib/advisor-events";
import { applyAdvisorEvent, newMessage } from "@/lib/message-utils";

const card = { variantId: "golf-1-5-tsi-130", make: "Volkswagen", model: "Golf" } as VehicleCard;

describe("applyAdvisorEvent", () => {
  it("concatenates token deltas", () => {
    let message = newMessage("a", "assistant");
    for (const delta of ["Sal", "ut", "!"]) message = applyAdvisorEvent(message, { type: "token", delta });
    expect(message.content).toBe("Salut!");
  });

  it("closes the last started call of the same tool when it finishes", () => {
    let message = newMessage("a", "assistant");
    const tool = (status: "started" | "finished", summary: string) => ({
      type: "tool" as const,
      name: "searchVehicles",
      status,
      summary
    });
    message = applyAdvisorEvent(message, tool("started", "Caut…"));
    message = applyAdvisorEvent(message, tool("started", "Caut din nou…"));
    message = applyAdvisorEvent(message, tool("finished", "Am găsit 5 variante"));
    expect(message.tools.map(t => [t.status, t.summary])).toEqual([
      ["started", "Caut…"],
      ["finished", "Am găsit 5 variante"]
    ]);
  });

  it("does not show the same variant twice", () => {
    let message = newMessage("a", "assistant");
    message = applyAdvisorEvent(message, { type: "vehicles", cards: [card], dataAsOf: "2026-11-11" });
    message = applyAdvisorEvent(message, { type: "vehicles", cards: [card], dataAsOf: "2026-11-11" });
    expect(message.cards).toHaveLength(1);
  });

  it("takes the persisted id from the meta event", () => {
    const message = applyAdvisorEvent(newMessage("pending", "assistant"), {
      type: "meta",
      messageId: "m-1",
      conversationId: "c-1"
    });
    expect(message.id).toBe("m-1");
  });
});

describe("parseAdvisorEvent", () => {
  it("ignores unknown events and malformed bodies", () => {
    expect(parseAdvisorEvent({ event: "something-new", data: "{}" })).toBeNull();
    expect(parseAdvisorEvent({ event: "token", data: "not json" })).toBeNull();
  });

  it("reads the problem of an error event", () => {
    const event = parseAdvisorEvent({ event: "error", data: '{"code":"advisor-unavailable","status":502}' });
    expect(event).toEqual({ type: "error", problem: { code: "advisor-unavailable", status: 502 } });
  });
});
