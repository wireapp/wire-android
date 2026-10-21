#!/usr/bin/env python3
"""Own the pre-provisioned critical-flow emulators for one CI job."""

from __future__ import annotations

import argparse
import json
import os
import re
import shutil
import signal
import socket
import subprocess
import time
from concurrent.futures import ThreadPoolExecutor
from pathlib import Path

from resolve_device_group import parse_groups


POOL = [(f"critical-flow-{index + 1}", 5554 + index * 2) for index in range(4)]
CRASH = re.compile(r"ANR in |FATAL EXCEPTION:|Fatal signal \d+|\bam_(?:anr|crash|native_crash)\b")


def run(*args: str, timeout: int = 20) -> subprocess.CompletedProcess[str]:
    return subprocess.run(args, capture_output=True, text=True, timeout=timeout, check=False)


def adb(serial: str, *args: str, timeout: int = 20) -> subprocess.CompletedProcess[str]:
    return run("adb", "-s", serial, *args, timeout=timeout)


def identity(pid: int) -> str:
    try:
        fields = Path(f"/proc/{pid}/stat").read_text().rsplit(")", 1)[1].split()
        return fields[19] if fields[0] != "Z" else ""
    except (FileNotFoundError, ProcessLookupError):
        return ""


def alive(process: dict) -> bool:
    return bool(process["start"]) and identity(process["pid"]) == process["start"]


class EmulatorPool:
    def __init__(self) -> None:
        run_key = "-".join(os.environ.get(key, "local") for key in (
            "GITHUB_RUN_ID", "GITHUB_RUN_ATTEMPT", "GITHUB_JOB"
        ))
        self.root = Path(os.environ["RUNNER_TEMP"]) / f"emulator-pool-{run_key}"
        self.state_path = self.root / "state.json"
        self.logs = self.root / "diagnostics"
        self.state = json.loads(self.state_path.read_text()) if self.state_path.exists() else []

    def save(self) -> None:
        temporary = self.state_path.with_suffix(".tmp")
        temporary.write_text(json.dumps(self.state, indent=2) + "\n")
        temporary.replace(self.state_path)

    def spawn(self, command: list[str], log: Path) -> dict:
        with log.open("w") as output:
            process = subprocess.Popen(command, stdout=output, stderr=subprocess.STDOUT, start_new_session=True)
        return {"pid": process.pid, "start": identity(process.pid)}

    def preflight(self) -> Path:
        groups = parse_groups(os.environ.get("DEVICE_GROUPS_JSON", ""))
        expected = {f"emulator-{port}" for _, port in POOL}
        if set(groups.get("criticalFlow", [])) != expected:
            raise RuntimeError("criticalFlow must contain exactly emulator-5554, emulator-5556, "
                               "emulator-5558 and emulator-5560 in ANDROID_DEVICE_GROUPS_JSON.")
        if self.state:
            raise RuntimeError(f"Pool state already exists: {self.state_path}; refusing to adopt another run.")
        if not os.access("/dev/kvm", os.R_OK | os.W_OK):
            raise RuntimeError("The CI account requires read/write access to /dev/kvm.")
        available_kib = int(re.search(r"MemAvailable:\s+(\d+)", Path("/proc/meminfo").read_text())[1])
        if len(os.sched_getaffinity(0)) < 16 or available_kib < 40 * 1024**2:
            raise RuntimeError("Four emulators require 16 available CPUs and at least 40 GiB available RAM.")
        avd_home = Path(os.environ.get("ANDROID_AVD_HOME", str(Path.home() / ".android/avd")))
        if shutil.disk_usage(avd_home).free < 30 * 1024**3:
            raise RuntimeError("At least 30 GiB of free space is required on the AVD filesystem.")
        emulator = Path(os.environ["ANDROID_HOME"]) / "emulator/emulator"
        version = run(str(emulator), "-version")
        if version.returncode or "37.2.12" not in version.stdout:
            raise RuntimeError("Use the validated Android Emulator 37.2.12 runtime.")
        acceleration = run(str(emulator), "-accel-check")
        if acceleration.returncode:
            raise RuntimeError(f"KVM acceleration is unavailable: {acceleration.stdout} {acceleration.stderr}")
        for name, port in POOL:
            config = dict(line.split("=", 1) for line in
                          (avd_home / f"{name}.avd/config.ini").read_text().splitlines() if "=" in line)
            required = {"hw.cpu.ncore": "4", "hw.ramSize": "6144", "hw.gpu.mode": "software",
                        "image.sysdir.1": "system-images/android-36/google_apis/x86_64/"}
            if any(config.get(key) != value for key, value in required.items()):
                raise RuntimeError(f"{name} does not match the validated Android 16 / 4 CPU / 6 GiB configuration.")
            for candidate in (port, port + 1):
                with socket.socket() as connection:
                    # A completed previous run can leave TIME_WAIT sockets. Match
                    # the emulator's reuse policy while still rejecting listeners.
                    connection.setsockopt(socket.SOL_SOCKET, socket.SO_REUSEADDR, 1)
                    try:
                        connection.bind(("127.0.0.1", candidate))
                    except OSError as error:
                        raise RuntimeError(f"Port {candidate} is already in use; refusing to borrow an emulator.") from error
        return emulator

    def ready(self, entry: dict) -> bool:
        if not alive(entry["emulator"]):
            raise RuntimeError(f"{entry['serial']} exited. See startup.log.")
        serial = entry["serial"]
        boot = adb(serial, "shell", "getprop", "sys.boot_completed", timeout=5)
        if boot.returncode or boot.stdout.strip() != "1":
            return False
        packages = adb(serial, "shell", "pm", "path", "android", timeout=10)
        return packages.returncode == 0 and "package:" in packages.stdout

    def interactive(self, entry: dict) -> bool:
        if not self.ready(entry):
            return False
        started = time.monotonic()
        launch = adb(entry["serial"], "shell", "am", "start", "-W", "-a", "android.settings.SETTINGS", timeout=10)
        home = adb(entry["serial"], "shell", "input", "keyevent", "KEYCODE_HOME", timeout=5)
        return (launch.returncode == 0 and "Status: ok" in launch.stdout and "Error:" not in launch.stdout
                and home.returncode == 0 and time.monotonic() - started <= 2)

    def wait_for_responsiveness(self) -> None:
        pending = list(self.state)
        responsive_since = {}
        deadline = time.monotonic() + 60
        while pending:
            for entry in list(pending):
                if not alive(entry["logcat"]):
                    raise RuntimeError(f"{entry['serial']} lost log capture during startup.")
                try:
                    responsive = self.interactive(entry)
                except subprocess.TimeoutExpired:
                    responsive = False
                if responsive:
                    since = responsive_since.setdefault(entry["serial"], time.monotonic())
                    if time.monotonic() - since >= 5:
                        pending.remove(entry)
                else:
                    responsive_since.pop(entry["serial"], None)
            self.check_logs()
            if pending and time.monotonic() >= deadline:
                raise RuntimeError("Emulator UI did not remain responsive during startup validation.")
            if pending:
                time.sleep(1)

    def start(self) -> None:
        self.logs.mkdir(parents=True, exist_ok=True)
        os.chmod(self.root, 0o700)
        emulator = self.preflight()
        if run("adb", "start-server").returncode:
            raise RuntimeError("Could not start ADB.")
        pool_started = time.monotonic()
        try:
            # Two booting guests leave capacity for the host and already-running
            # guests. Completion checks replace unconditional settling delays.
            for offset in range(0, len(POOL), 2):
                pending = []
                started_at = {}
                for name, port in POOL[offset:offset + 2]:
                    serial = f"emulator-{port}"
                    directory = self.logs / serial
                    directory.mkdir()
                    entry = {"serial": serial, "avd": name}
                    entry["emulator"] = self.spawn([
                        str(emulator), "-avd", name, "-port", str(port), "-no-window", "-no-audio",
                        "-no-snapshot", "-gpu", "software", "-accel", "on"
                    ], directory / "startup.log")
                    self.state.append(entry)
                    self.save()
                    entry["logcat"] = self.spawn([
                        "adb", "-s", serial, "logcat", "-b", "all", "-v", "threadtime"
                    ], directory / "logcat.txt")
                    self.save()
                    started_at[serial] = time.monotonic()
                    pending.append(entry)
                while pending:
                    for entry in list(pending):
                        try:
                            booted = self.ready(entry)
                        except subprocess.TimeoutExpired:
                            booted = False
                        if not booted:
                            if time.monotonic() - started_at[entry["serial"]] >= 180:
                                raise RuntimeError(f"{entry['serial']} did not become ready within 180 seconds.")
                            continue
                        serial = entry["serial"]
                        entry["boot_seconds"] = round(time.monotonic() - started_at[serial], 1)
                        self.save()
                        print(f"{serial} booted in {entry['boot_seconds']} seconds", flush=True)
                        for setting in ("window_animation_scale", "transition_animation_scale", "animator_duration_scale"):
                            result = adb(serial, "shell", "settings", "put", "global", setting, "0")
                            if result.returncode:
                                raise RuntimeError(f"Could not disable animations on {serial}.")
                        if adb(serial, "shell", "input", "keyevent", "KEYCODE_WAKEUP").returncode:
                            raise RuntimeError(f"Could not wake {serial}.")
                        if adb(serial, "shell", "wm", "dismiss-keyguard").returncode:
                            raise RuntimeError(f"Could not dismiss keyguard on {serial}.")
                        pending.remove(entry)
                    if pending:
                        time.sleep(1)
            self.wait_for_responsiveness()
            for entry in self.state:
                if not self.ready(entry) or not alive(entry["logcat"]):
                    raise RuntimeError(f"{entry['serial']} lost readiness or log capture during startup.")
                descriptors = Path(f"/proc/{entry['emulator']['pid']}/fd")
                if not any(os.readlink(fd) == "/dev/kvm" for fd in descriptors.iterdir() if fd.exists()):
                    raise RuntimeError(f"{entry['serial']} is not using KVM.")
            self.check_logs()
            elapsed = round(time.monotonic() - pool_started, 1)
            (self.logs / "startup-timing.txt").write_text(f"Four-emulator startup: {elapsed} seconds\n")
            with open(os.environ["GITHUB_ENV"], "a", encoding="utf-8") as output:
                output.write(f"EMULATOR_DIAGNOSTICS_DIR={self.logs}\n")
            print(f"All four emulators are ready for test selection in {elapsed} seconds.", flush=True)
        except BaseException as error:
            if isinstance(error, (InterruptedError, KeyboardInterrupt)):
                self.stop()
            else:
                try:
                    self.collect()
                finally:
                    self.stop()
            raise

    def check_logs(self) -> None:
        failures = []
        for entry in self.state:
            path = self.logs / entry["serial"] / "logcat.txt"
            if path.exists():
                with path.open(errors="replace") as log:
                    matches = [line.rstrip() for line in log if CRASH.search(line)]
                (path.parent / "crash-markers.txt").write_text("\n".join(matches) + ("\n" if matches else ""))
                if matches:
                    failures.append(entry["serial"])
        if failures:
            raise RuntimeError(f"ANR/crash markers detected on {', '.join(failures)}; inspect the emulator diagnostic logs.")

    def collect(self) -> None:
        if not self.state:
            return
        self.logs.mkdir(parents=True, exist_ok=True)
        with open(os.environ["GITHUB_ENV"], "a", encoding="utf-8") as output:
            output.write(f"EMULATOR_DIAGNOSTICS_DIR={self.logs}\n")

        def capture(entry: dict) -> None:
            directory = self.logs / entry["serial"]
            directory.mkdir(exist_ok=True)
            if not alive(entry["emulator"]):
                (directory / "emulator-exited.txt").write_text("Emulator exited before cleanup.\n")
                return
            commands = {
                "activity.txt": ("shell", "dumpsys", "activity", "lastanr"),
                "dropbox-app-anr.txt": ("shell", "dumpsys", "dropbox", "--print", "data_app_anr"),
                "dropbox-system-anr.txt": ("shell", "dumpsys", "dropbox", "--print", "system_app_anr"),
                "dropbox-server-anr.txt": ("shell", "dumpsys", "dropbox", "--print", "system_server_anr"),
                "anr-pull.txt": ("pull", "/data/anr", str(directory / "anr")),
                "logcat-final.txt": ("logcat", "-b", "all", "-d", "-v", "threadtime"),
            }
            for filename, command in commands.items():
                try:
                    result = adb(entry["serial"], *command, timeout=30)
                    content = result.stdout + result.stderr
                    failed = result.returncode != 0
                except subprocess.TimeoutExpired:
                    content = "Diagnostic command timed out.\n"
                    failed = True
                (directory / filename).write_text(content)
                if filename == "logcat-final.txt" and failed:
                    raise RuntimeError(f"Could not capture final logcat from {entry['serial']}; inspect the diagnostic logs.")

        with ThreadPoolExecutor(max_workers=4) as workers:
            list(workers.map(capture, self.state))
        self.check_logs()
        if any(not alive(entry["emulator"]) or not alive(entry["logcat"]) for entry in self.state):
            raise RuntimeError("An emulator or log capture process exited unexpectedly; inspect the emulator diagnostic logs.")

    def stop(self) -> None:
        processes = [entry[key] for entry in self.state for key in ("emulator", "logcat") if key in entry]
        for sig, wait_seconds in ((signal.SIGTERM, 25), (signal.SIGKILL, 5)):
            for process in processes:
                if alive(process):
                    try:
                        os.killpg(process["pid"], sig)
                    except ProcessLookupError:
                        pass
            deadline = time.monotonic() + wait_seconds
            while any(alive(process) for process in processes) and time.monotonic() < deadline:
                time.sleep(0.5)
        if any(alive(process) for process in processes):
            raise RuntimeError("Could not stop all job-owned emulator processes.")
        print("All job-owned emulator and log capture processes stopped.", flush=True)


def main() -> None:
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("command", choices=("start", "collect", "stop"))
    args = parser.parse_args()
    # GitHub sends SIGINT/SIGTERM on cancellation. Startup must clean partial boots.
    def interrupted(signum: int, _frame: object) -> None:
        raise InterruptedError(f"Emulator lifecycle interrupted by signal {signum}")
    if args.command == "start":
        signal.signal(signal.SIGTERM, interrupted)
        signal.signal(signal.SIGINT, interrupted)
    try:
        getattr(EmulatorPool(), args.command)()
    except (RuntimeError, ValueError, OSError, subprocess.TimeoutExpired) as error:
        raise SystemExit(f"ERROR: {error}") from error


if __name__ == "__main__":
    main()
