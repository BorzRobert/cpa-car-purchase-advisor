import { describe, expect, it } from "vitest";
import type { ComparisonReport, VehicleCard } from "@/lib/advisor-events";
import { clampBudget } from "@/lib/budget";
import { journeyStates } from "@/lib/journey";
import { profileChips } from "@/lib/profile-chips";
import { applyProfilePatch } from "@/lib/profile-patch";
import { tcoSegments } from "@/lib/tco-breakdown";
import { vehicleBadges } from "@/lib/vehicle-badges";
import { migrateAppState } from "@/store/useAppStore";

describe("applyProfilePatch", () => {
  it("overwrites changed fields and turns null into a cleared field", () => {
    const next = applyProfilePatch(
      { budgetAmount: 20000, budgetKind: "TOTAL", mustHaves: ["CarPlay"] },
      { budgetAmount: 25000, budgetKind: null, mustHaves: null }
    );
    expect(next).toEqual({ budgetAmount: 25000, mustHaves: [] });
  });
});

describe("profileChips", () => {
  it("shows only what is filled in", () => {
    const chips = profileChips({ budgetAmount: 20000, budgetKind: "TOTAL", yearlyKm: 15000, powertrains: [] });
    expect(chips.map(chip => chip.id)).toEqual(["budget", "yearlyKm"]);
  });
});

describe("journeyStates", () => {
  it("marks the first unreached stage as current", () => {
    const states = journeyStates({ hasProfile: true, hasVehicles: true, hasComparison: false, shortlistCount: 0 });
    expect(states.map(item => item.state)).toEqual(["done", "done", "current", "upcoming"]);
  });
});

describe("clampBudget", () => {
  it("snaps to the step and stays within the range of the chosen kind", () => {
    expect(clampBudget(20240, "TOTAL")).toBe(20000);
    expect(clampBudget(20000, "MONTHLY")).toBe(1500);
    expect(clampBudget(10, "MONTHLY")).toBe(100);
  });
});

describe("tcoSegments", () => {
  it("makes the segments add up to the total", () => {
    const segments = tcoSegments({
      totalCostEur: 30000,
      costPerMonthEur: 500,
      costPerKmEur: 0.4,
      annualRunningCostEur: 2500,
      depreciationEur: 15000,
      energyEur: 5000
    });
    expect(segments.map(segment => segment.valueEur)).toEqual([15000, 5000, 10000]);
    expect(segments.reduce((sum, segment) => sum + segment.share, 0)).toBeCloseTo(1);
  });
});

describe("vehicleBadges", () => {
  const golf = { variantId: "golf", euroNcapStars: 5 } as VehicleCard;
  const report = {
    scores: [
      { variantId: "golf", total: 80, tco: { costPerMonthEur: 400 } },
      { variantId: "polo", total: 70, tco: { costPerMonthEur: 350 } }
    ]
  } as ComparisonReport;

  it("derives badges from the ranking and the card data", () => {
    expect(vehicleBadges(golf, report).map(badge => badge.id)).toEqual(["best-score", "ncap"]);
    expect(vehicleBadges({ variantId: "polo" } as VehicleCard, report).map(badge => badge.id)).toEqual(["cheapest"]);
  });
});

describe("migrateAppState", () => {
  it("keeps the session token from v8 and shows onboarding once", () => {
    expect(migrateAppState({ sessionToken: "t" }, 8)).toEqual({ sessionToken: "t", onboardingSeen: false });
    expect(migrateAppState({ sessionToken: "t" }, 7)).toEqual({ sessionToken: null, onboardingSeen: false });
  });
});
