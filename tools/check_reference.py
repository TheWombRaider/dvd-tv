"""Compare production Java simulation output against independently measured video frames.

Run after the Gradle build, with JAVA_HOME pointing to a JDK. No Android device needed.
The clip's unresolved final-frame diagnostic is reported separately, never counted as a pass.
"""
import json
import os
from pathlib import Path
import shutil
import subprocess

ROOT = Path(__file__).resolve().parents[1]


def main():
    fixture = json.loads((ROOT / "docs/reference/measurements.json").read_text())
    palette = json.loads((ROOT / "docs/reference/palette.json").read_text())["colors"]
    workspace = ROOT / "work/reference-java"
    workspace.mkdir(parents=True, exist_ok=True)
    source = ROOT / "app/src/main/java/io/github/thewombraider/dvdtv"
    jdk = Path(os.environ["JAVA_HOME"]) / "bin" if "JAVA_HOME" in os.environ else None
    javac = str(jdk / "javac") if jdk else shutil.which("javac")
    java = str(jdk / "java") if jdk else shutil.which("java")
    if not javac or not java:
        raise SystemExit("Set JAVA_HOME to the JDK used for the Android build.")
    values = ",".join(f"0xff{r:02x}{g:02x}{b:02x}" for r, g, b in palette)
    generated = workspace / "MeasuredPalette.java"
    generated.write_text("package io.github.thewombraider.dvdtv; final class MeasuredPalette { static final int[] COLORS = {" + values + "}; }", encoding="utf-8")
    probe = workspace / "ReferenceProbe.java"
    probe.write_text("""package io.github.thewombraider.dvdtv;
public final class ReferenceProbe {
    public static void main(String[] args) {
        DvdSimulation simulation = new DvdSimulation();
        for (String arg : args) {
            long tick = Long.parseLong(arg);
            simulation.seekToTick(tick);
            System.out.println(tick + " " + simulation.x() + " " + simulation.y() + " " + Integer.toUnsignedString(simulation.color()));
        }
    }
}
""", encoding="utf-8")
    subprocess.run([javac, "-d", str(workspace), str(source / "DvdSimulation.java"), str(source / "ColorSequence.java"), str(generated), str(probe)], check=True)
    result = subprocess.run([java, "-cp", str(workspace), "io.github.thewombraider.dvdtv.ReferenceProbe", *[str(s["frame"]) for s in fixture["samples"]]], check=True, capture_output=True, text=True)
    observed = {int(parts[0]): list(map(int, parts[1:])) for line in result.stdout.splitlines() if (parts := line.split())}
    errors = []
    position_error = 0
    color_error = 0
    for sample in fixture["samples"]:
        x, y, color = observed[sample["frame"]]
        expected = sample["bbox"]
        delta = max(abs(x - expected[0]), abs(y - expected[1]))
        rgb = [(color >> shift) & 255 for shift in (16, 8, 0)]
        rgb_delta = max(abs(a - b) for a, b in zip(rgb, sample["rgb"]))
        position_error = max(position_error, delta)
        color_error = max(color_error, rgb_delta)
        if delta > 1 or rgb_delta > 3:
            errors.append({"frame": sample["frame"], "positionErrorPx": delta, "maxColorError": rgb_delta})
    report = {"samples": len(fixture["samples"]), "heldOutSamples": sum(s["split"] == "held-out" for s in fixture["samples"]), "maxPositionErrorPx": position_error, "maxColorError": color_error, "failures": errors, "excludedDiagnostics": fixture.get("excludedSamples", [])}
    (ROOT / "work/reference-check.json").write_text(json.dumps(report, indent=2) + "\n")
    print(json.dumps(report, indent=2))
    if errors:
        raise SystemExit(1)


if __name__ == "__main__":
    main()
