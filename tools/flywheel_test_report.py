#!/usr/bin/env python3
"""Report for the one-off Flywheel Speed Test (TeamCode/.../tests/FlywheelSpeedTest.java).

Usage:  python3 tools/flywheel_test_report.py flywheel-20261112-180000.csv [more.csv ...]

Copy the CSV from the Control Hub first (it is in /sdcard/FIRST/flywheel-test/), for example:
    adb pull /sdcard/FIRST/flywheel-test/ .

For each burst of 4 it prints: the speed when each ball was pushed, how far the speed fell, and how long it
took to get back within 100 RPM (and stay there 50 ms). Then it compares the two modes (HUB and BANG).
Bursts with a slow loop (95th percentile over 25 ms) are left out: bang-bang needs a fast loop.
Standard library only: no packages to install.
"""
import csv
import statistics
import sys

READY_RPM = 100      # same as the OpMode
HOLD_SEC = 0.05
SLOW_LOOP_MS = 25
LAST_SHOT_WINDOW = 1.5   # seconds after the last push to look for recovery


def load(path):
    rows = []
    with open(path) as f:
        lines = [ln for ln in f if not ln.startswith("#")]
    for r in csv.DictReader(lines):
        rows.append({
            "t": float(r["t"]), "loop_ms": float(r["loop_ms"]), "mode": r["mode"],
            "target": float(r["target_rpm"]), "rpm": float(r["rpm"]),
            "volts": float(r["volts"]) if r["volts"] not in ("", "NaN") else float("nan"),
            "burst": int(r["burst"]), "event": r["event"],
        })
    return rows


def bursts(rows):
    """Groups the 'push' rows by burst number, with the burst's rows for loop time."""
    out = {}
    for i, r in enumerate(rows):
        if r["burst"] == 0:
            continue
        b = out.setdefault(r["burst"], {"mode": r["mode"], "target": r["target"], "pushes": [], "loop": [], "volts": []})
        b["loop"].append(r["loop_ms"])
        if r["volts"] == r["volts"]:
            b["volts"].append(r["volts"])
        if r["event"] == "push":
            b["pushes"].append(i)
    return out


def shot_result(rows, i, end_t, target):
    """From push row i to end_t: lowest speed, and time until back within READY_RPM for HOLD_SEC."""
    t0 = rows[i]["t"]
    low = rows[i]["rpm"]
    dropped = False
    since = None
    back = None
    j = i
    while j < len(rows) and rows[j]["t"] <= end_t:
        rpm = rows[j]["rpm"]
        low = min(low, rpm)
        if target - low > READY_RPM:
            dropped = True
        if dropped and abs(rpm - target) <= READY_RPM:
            since = rows[j]["t"] if since is None else since
            if rows[j]["t"] - since >= HOLD_SEC:
                back = since - t0
                break
        else:
            since = None
        j += 1
    return {"at_push": rows[i]["rpm"], "drop": target - low, "back": back, "dropped": dropped}


def p95(values):
    s = sorted(values)
    return s[min(len(s) - 1, int(0.95 * len(s)))] if s else float("nan")


def main(paths):
    by_mode = {"HUB": [], "BANG": []}
    for path in paths:
        rows = load(path)
        print(f"\n== {path}")
        for bid, b in sorted(bursts(rows).items()):
            pushes = b["pushes"]
            shots = []
            for k, i in enumerate(pushes):
                end_t = rows[pushes[k + 1]]["t"] if k + 1 < len(pushes) else rows[i]["t"] + LAST_SHOT_WINDOW
                shots.append(shot_result(rows, i, end_t, b["target"]))
            loop95 = p95(b["loop"])
            volts = statistics.mean(b["volts"]) if b["volts"] else float("nan")
            slow = loop95 > SLOW_LOOP_MS
            text = "  ".join(
                f"[{s['at_push']:.0f} rpm, drop {s['drop']:.0f}, back "
                + (f"{s['back']:.2f}s" if s["back"] is not None else ("never left" if not s["dropped"] else "NOT BACK"))
                + "]" for s in shots)
            print(f"burst {bid:2d} {b['mode']:4s} {b['target']:.0f} rpm, {volts:.2f} V, loop p95 {loop95:.0f} ms"
                  f"{'  SLOW LOOP: left out' if slow else ''}\n   {text}")
            if not slow and b["mode"] in by_mode:
                by_mode[b["mode"]].append({"shots": shots, "target": b["target"]})

    print("\n== Comparison (bursts with a fast loop only)")
    for mode, bl in by_mode.items():
        if not bl:
            print(f"{mode}: no bursts")
            continue
        backs = [s["back"] for b in bl for s in b["shots"] if s["back"] is not None]
        not_back = sum(1 for b in bl for s in b["shots"] if s["dropped"] and s["back"] is None)
        drops = [s["drop"] for b in bl for s in b["shots"]]
        # The speed error when balls 2-4 go in: what the next shot really sees.
        errs = [s["at_push"] - b["target"] for b in bl for s in b["shots"][1:]]
        print(f"{mode}: {len(bl)} bursts")
        if backs:
            print(f"   back to speed: median {statistics.median(backs):.2f} s, worst {max(backs):.2f} s"
                  f" ({not_back} shots not back before the next ball)")
        print(f"   drop: median {statistics.median(drops):.0f} rpm")
        if len(errs) > 1:
            print(f"   speed when balls 2-4 go in: {statistics.mean(errs):+.0f} rpm from target on average,"
                  f" spread (std) {statistics.stdev(errs):.0f} rpm")
    print("\nBANG is worth building if it gets back to speed faster AND the spread for balls 2-4 is the same or"
          " smaller. Check the misses you counted at the net too.")


if __name__ == "__main__":
    if len(sys.argv) < 2:
        print(__doc__)
        sys.exit(1)
    main(sys.argv[1:])
