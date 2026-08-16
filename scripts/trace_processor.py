#!/usr/bin/env python3
"""
Perfetto Trace Processor Runner
Downloads and runs the official standalone trace_processor executable for Windows/macOS/Linux.
"""

import os
import platform
import subprocess
import sys
import urllib.request
import stat

PERFETTO_VERSION = "v46.0"
CACHE_DIR = os.path.join(os.path.expanduser("~"), ".perfetto_bin")

def get_trace_processor_url():
    system = platform.system().lower()
    machine = platform.machine().lower()

    if "windows" in system:
        return f"https://commondatastorage.googleapis.com/perfetto-luci-artifacts/{PERFETTO_VERSION}/windows-amd64/trace_processor_shell.exe"
    elif "darwin" in system:
        arch = "arm64" if "arm" in machine or "aarch64" in machine else "amd64"
        return f"https://commondatastorage.googleapis.com/perfetto-luci-artifacts/{PERFETTO_VERSION}/mac-{arch}/trace_processor_shell"
    elif "linux" in system:
        arch = "arm64" if "arm" in machine or "aarch64" in machine else "amd64"
        return f"https://commondatastorage.googleapis.com/perfetto-luci-artifacts/{PERFETTO_VERSION}/linux-{arch}/trace_processor_shell"
    else:
        raise OSError(f"Unsupported OS: {system}")

def get_executable_path():
    os.makedirs(CACHE_DIR, exist_ok=True)
    ext = ".exe" if platform.system().lower() == "windows" else ""
    binary_path = os.path.join(CACHE_DIR, f"trace_processor_shell_{PERFETTO_VERSION}{ext}")

    if not os.path.exists(binary_path):
        url = get_trace_processor_url()
        print(f"Downloading trace_processor_shell from {url}...")
        try:
            urllib.request.urlretrieve(url, binary_path)
            if not binary_path.endswith(".exe"):
                st = os.stat(binary_path)
                os.chmod(binary_path, st.st_mode | stat.S_IEXEC)
            print("Download complete.")
        except Exception as e:
            print(f"Failed to download trace processor: {e}", file=sys.stderr)
            sys.exit(1)

    return binary_path

def main():
    if len(sys.argv) < 2:
        print("Usage: trace_processor.py <trace_file> [-q query_file | --query 'SQL_QUERY']")
        sys.exit(1)

    binary = get_executable_path()
    cmd = [binary] + sys.argv[1:]

    try:
        res = subprocess.run(cmd, check=True)
        sys.exit(res.returncode)
    except subprocess.CalledProcessError as e:
        sys.exit(e.returncode)
    except KeyboardInterrupt:
        sys.exit(130)

if __name__ == "__main__":
    main()
