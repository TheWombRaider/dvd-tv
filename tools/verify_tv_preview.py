"""Verify the installed app's preview without changing the selected screensaver.

Usage: python tools/verify_tv_preview.py --serial <adb-device> [--wake-and-restore]
Writes private local evidence under ignored work/device-validation/.
Only this app is launched/stopped. A sleeping TV is restored to sleep when requested.
"""
import argparse
import json
from pathlib import Path
import subprocess
import time

import cv2
import numpy as np

ROOT = Path(__file__).resolve().parents[1]
PACKAGE = "io.github.thewombraider.dvdtv"
COMPONENT = PACKAGE + "/.PreviewActivity"


def foreground(frame):
    mask = frame.max(axis=2) > 70
    y, x = np.where(mask)
    if not len(x):
        return None
    return {"bbox": [int(x.min()), int(y.min()), int(x.max() + 1), int(y.max() + 1)], "rgb": np.median(frame[mask], axis=0)[::-1].astype(int).tolist()}


def main():
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--serial", required=True)
    parser.add_argument("--wake-and-restore", action="store_true")
    args = parser.parse_args()
    out = ROOT / "work/device-validation"
    out.mkdir(parents=True, exist_ok=True)

    def adb(*parts, binary=False):
        result = subprocess.run(["adb", "-s", args.serial, *parts], check=True, capture_output=True, timeout=30)
        return result.stdout if binary else result.stdout.decode("utf-8", errors="replace").strip()

    def settings():
        return {key: adb("shell", "settings", "get", "secure", key) for key in ("screensaver_components", "screensaver_enabled")}

    def capture():
        data = adb("exec-out", "screencap", "-p", binary=True)
        image = cv2.imdecode(np.frombuffer(data, dtype=np.uint8), cv2.IMREAD_COLOR)
        if image is None:
            raise RuntimeError("TV screenshot could not be decoded")
        return image

    before = settings()
    power = adb("shell", "dumpsys", "power")
    was_asleep = "mWakefulness=Asleep" in power
    if was_asleep and not args.wake_and_restore:
        raise SystemExit("TV is asleep. Use --wake-and-restore only when a temporary wake is authorized.")
    report = {"settingsBefore": before, "temporarilyWokeTV": was_asleep, "captures": [], "errors": []}
    fixture = json.loads((ROOT / "docs/reference/measurements.json").read_text())
    samples = {s["frame"]: s for s in fixture["samples"]}
    try:
        if was_asleep:
            adb("shell", "input", "keyevent", "KEYCODE_WAKEUP")
        for tick in (0, 108, 231, 600, 1500, 5555, 8765):
            result = adb("shell", "am", "start", "-W", "-S", "-n", COMPONENT, "--el", "capture_tick", str(tick), "--ez", "capture_freeze", "true")
            if "Error:" in result:
                raise RuntimeError("Preview launch failed: " + result)
            expected = samples[tick]
            deadline = time.monotonic() + 15
            actual = None
            while time.monotonic() < deadline:
                image = capture()
                actual = foreground(image)
                # A matching position can appear during the system's fade-in. Wait for the
                # reference color too, so transition alpha is not mistaken for app output.
                if actual and max(abs(a - b) for a, b in zip(actual["bbox"], expected["bbox"])) <= 1 and max(abs(a - b) for a, b in zip(actual["rgb"], expected["rgb"])) <= 3:
                    break
                time.sleep(.15)
            if actual is None:
                raise RuntimeError(f"No logo rendered at frame {tick}")
            bbox_error = max(abs(a - b) for a, b in zip(actual["bbox"], expected["bbox"]))
            color_error = max(abs(a - b) for a, b in zip(actual["rgb"], expected["rgb"]))
            path = out / f"frame-{tick:05d}.png"
            cv2.imwrite(str(path), image)
            reference_cap = cv2.VideoCapture(str(ROOT / "work/reference/reference-1080p.mp4"))
            reference_cap.set(cv2.CAP_PROP_POS_FRAMES, tick)
            ok, reference = reference_cap.read()
            reference_cap.release()
            overlap = None
            if ok and reference.shape == image.shape:
                left = image.max(axis=2) > 70
                right = reference.max(axis=2) > 70
                overlap = float((left & right).sum() / max(1, (left | right).sum()))
            row = {"tick": tick, **actual, "bboxErrorPx": bbox_error, "maxColorError": color_error, "maskIoU": overlap}
            report["captures"].append(row)
            print(json.dumps(row), flush=True)
            if bbox_error > 1 or color_error > 3 or (overlap is not None and overlap < .98):
                report["errors"].append(f"Frame {tick} differs beyond stated tolerance")

        adb("shell", "am", "start", "-W", "-S", "-n", COMPONENT)
        deadline = time.monotonic() + 15
        first = None
        while time.monotonic() < deadline:
            first = foreground(capture())
            if first and abs(first["bbox"][2] - first["bbox"][0] - 336) <= 1 and abs(first["bbox"][3] - first["bbox"][1] - 148) <= 1:
                break
            time.sleep(.15)
        time.sleep(.4)
        second = foreground(capture())
        report["motionObserved"] = bool(first and second and first["bbox"] != second["bbox"])
        if not report["motionObserved"]:
            report["errors"].append("Live motion was not observed")
        adb("shell", "input", "keyevent", "KEYCODE_BACK")
        deadline = time.monotonic() + 10
        while time.monotonic() < deadline:
            state = adb("shell", "dumpsys", "activity", "activities")
            resumed = [line for line in state.splitlines() if "ResumedActivity" in line or "mResumedActivity" in line]
            if resumed and not any(PACKAGE in line for line in resumed):
                break
            time.sleep(.2)
        report["backDismissedPreview"] = bool(resumed and not any(PACKAGE in line for line in resumed))
        if not report["backDismissedPreview"]:
            report["errors"].append("Back dismissal was not verified")
    finally:
        adb("shell", "am", "force-stop", PACKAGE)
        if was_asleep:
            adb("shell", "input", "keyevent", "KEYCODE_SLEEP")
            deadline = time.monotonic() + 10
            while time.monotonic() < deadline:
                if "mWakefulness=Asleep" in adb("shell", "dumpsys", "power"):
                    break
                time.sleep(.2)
        report["settingsAfter"] = settings()
        report["screensaverSettingsUnchanged"] = report["settingsAfter"] == before
        report["sleepRestored"] = not was_asleep or "mWakefulness=Asleep" in adb("shell", "dumpsys", "power")
        if not report["screensaverSettingsUnchanged"]:
            report["errors"].append("Screensaver settings changed; inspect before proceeding")
        if not report["sleepRestored"]:
            report["errors"].append("Original sleep state was not restored")
        (out / "results.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps({k: v for k, v in report.items() if k != "captures"}, indent=2))
    if report["errors"]:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
