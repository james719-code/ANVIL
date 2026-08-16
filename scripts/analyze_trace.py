#!/usr/bin/env python3
"""
ANVIL Perfetto Trace Analyzer
Runs automated SQL queries via trace_processor to detect Main Thread jank,
Compose recompositions, database delays, and worker execution times.
"""

import os
import subprocess
import sys
import tempfile

def run_query(trace_processor_path, trace_path, query_sql):
    with tempfile.NamedTemporaryFile("w", suffix=".sql", delete=False) as f:
        f.write(query_sql)
        temp_sql_path = f.name

    try:
        cmd = [sys.executable, trace_processor_path, "-q", temp_sql_path, trace_path]
        res = subprocess.run(cmd, capture_output=True, text=True, check=True)
        return res.stdout.strip()
    except subprocess.CalledProcessError as e:
        print(f"Error querying trace: {e.stderr}", file=sys.stderr)
        return ""
    finally:
        if os.path.exists(temp_sql_path):
            os.remove(temp_sql_path)

def print_table(csv_output, title):
    print(f"\n{'='*75}")
    print(f" {title}")
    print(f"{'='*75}")
    if not csv_output:
        print("No matching trace events found.")
        return

    lines = csv_output.split("\n")
    if not lines or len(lines) < 2:
        print("No rows returned.")
        return

    headers = [h.strip('"') for h in lines[0].split(",")]
    rows = []
    for line in lines[1:]:
        if not line.strip():
            continue
        rows.append([col.strip('"') for col in line.split(",")])

    col_widths = [len(h) for h in headers]
    for row in rows:
        for i, val in enumerate(row):
            if i < len(col_widths):
                col_widths[i] = max(col_widths[i], len(val))

    header_fmt = " | ".join(f"{{:<{w}}}" for w in col_widths)
    separator = "-+-".join("-" * w for w in col_widths)

    print(header_fmt.format(*headers))
    print(separator)
    for row in rows:
        # Pad row if fewer columns
        while len(row) < len(col_widths):
            row.append("")
        print(header_fmt.format(*row[:len(col_widths)]))

def main():
    if len(sys.argv) < 2:
        print("Usage: python analyze_trace.py <path_to_trace_file>")
        sys.exit(1)

    trace_file = sys.argv[1]
    if not os.path.exists(trace_file):
        print(f"Error: Trace file '{trace_file}' not found.", file=sys.stderr)
        sys.exit(1)

    script_dir = os.path.dirname(os.path.abspath(__file__))
    trace_processor = os.path.join(script_dir, "trace_processor.py")

    print(f"Analyzing ANVIL Trace: {os.path.basename(trace_file)}...")

    # Query 1: ANVIL Custom Instrumented Slices
    q_anvil = """
    SELECT 
        name as slice_name,
        COUNT(*) as count,
        ROUND(AVG(dur) / 1e6, 2) as avg_ms,
        ROUND(MAX(dur) / 1e6, 2) as max_ms,
        ROUND(SUM(dur) / 1e6, 2) as total_ms
    FROM slice
    WHERE name LIKE 'Anvil%' 
       OR name LIKE 'TaskViewModel%' 
       OR name LIKE 'ReminderWorker%' 
       OR name LIKE 'DailyTask%'
       OR name LIKE 'MidnightContribution%'
    GROUP BY name
    ORDER BY total_ms DESC;
    """
    out_anvil = run_query(trace_processor, trace_file, q_anvil)
    print_table(out_anvil, "1. ANVIL APPLICATION HOT PATHS")

    # Query 2: Compose & UI Rendering Pipeline
    q_ui = """
    SELECT 
        slice.name as slice_name,
        COUNT(*) as count,
        ROUND(AVG(slice.dur) / 1e6, 2) as avg_ms,
        ROUND(MAX(slice.dur) / 1e6, 2) as max_ms,
        ROUND(SUM(slice.dur) / 1e6, 2) as total_ms
    FROM slice
    WHERE slice.name LIKE 'Compose:%' 
       OR slice.name LIKE 'Recomposer:%' 
       OR slice.name LIKE 'Record View#draw()' 
       OR slice.name LIKE 'draw-VRI%' 
       OR slice.name LIKE 'traversal'
       OR slice.name LIKE 'Choreographer#doFrame%'
    GROUP BY slice.name
    ORDER BY total_ms DESC
    LIMIT 15;
    """
    out_ui = run_query(trace_processor, trace_file, q_ui)
    print_table(out_ui, "2. JETPACK COMPOSE & UI RENDERING PASSES")

    # Query 3: JIT Stalls
    q_jit = """
    SELECT 
        name as compiler_slice,
        COUNT(*) as occurrences,
        ROUND(SUM(dur) / 1e6, 2) as total_stall_ms,
        ROUND(MAX(dur) / 1e6, 2) as max_stall_ms
    FROM slice
    WHERE name LIKE '%JIT%' OR name LIKE '%dex2oat%'
    GROUP BY name;
    """
    out_jit = run_query(trace_processor, trace_file, q_jit)
    print_table(out_jit, "3. ART JIT COMPILATION STALLS")

    # Query 4: Top CPU Consuming Threads
    q_threads = """
    SELECT 
        thread.name as thread_name,
        COUNT(slice.id) as slice_count,
        ROUND(SUM(slice.dur) / 1e6, 2) as total_ms
    FROM slice
    JOIN thread_track ON slice.track_id = thread_track.id
    JOIN thread USING (utid)
    GROUP BY thread.name
    ORDER BY total_ms DESC
    LIMIT 8;
    """
    out_threads = run_query(trace_processor, trace_file, q_threads)
    print_table(out_threads, "4. TOP ACTIVE THREADS")

    print(f"\n{'='*75}")
    print(" Analysis Complete! For deep timeline inspection, visit https://ui.perfetto.dev/")
    print(f"{'='*75}\n")

if __name__ == "__main__":
    main()
