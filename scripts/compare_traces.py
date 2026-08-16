import os
import subprocess
import sys
import tempfile

trace_processor = r"c:\Users\James\AndroidStudioProjects\ANVIL\scripts\trace_processor.py"
trace_before = r"c:\Users\James\AndroidStudioProjects\ANVIL\scripts\cpu-perfetto-20260816T224331.trace"
trace_after = r"c:\Users\James\AndroidStudioProjects\ANVIL\scripts\cpu-perfetto-20260816T225257.trace"

def analyze_single_trace(trace_file):
    query = """
    SELECT 'max_frame_ms' as metric, ROUND(MAX(slice.dur) / 1e6, 2) as val
    FROM slice JOIN thread_track ON slice.track_id = thread_track.id JOIN thread USING (utid) JOIN process USING (upid)
    WHERE process.name = 'com.james.anvil' AND slice.name LIKE 'Choreographer#doFrame%'
    UNION ALL
    SELECT 'avg_frame_ms', ROUND(AVG(slice.dur) / 1e6, 2)
    FROM slice JOIN thread_track ON slice.track_id = thread_track.id JOIN thread USING (utid) JOIN process USING (upid)
    WHERE process.name = 'com.james.anvil' AND slice.name LIKE 'Choreographer#doFrame%'
    UNION ALL
    SELECT 'shader_compile_ms', ROUND(SUM(dur) / 1e6, 2)
    FROM slice WHERE name = 'shader_compile' OR name = 'driver_link_program'
    UNION ALL
    SELECT 'shader_compile_count', COUNT(*)
    FROM slice WHERE name = 'shader_compile' OR name = 'driver_link_program'
    UNION ALL
    SELECT 'recompose_ms', ROUND(SUM(dur) / 1e6, 2)
    FROM slice JOIN thread_track ON slice.track_id = thread_track.id JOIN thread USING (utid) JOIN process USING (upid)
    WHERE process.name = 'com.james.anvil' AND slice.name LIKE 'Compose:recompose%'
    UNION ALL
    SELECT 'recompose_count', COUNT(*)
    FROM slice JOIN thread_track ON slice.track_id = thread_track.id JOIN thread USING (utid) JOIN process USING (upid)
    WHERE process.name = 'com.james.anvil' AND slice.name LIKE 'Compose:recompose%'
    UNION ALL
    SELECT 'measure_layout_ms', ROUND(SUM(dur) / 1e6, 2)
    FROM slice JOIN thread_track ON slice.track_id = thread_track.id JOIN thread USING (utid) JOIN process USING (upid)
    WHERE process.name = 'com.james.anvil' AND slice.name = 'AndroidOwner:measureAndLayout'
    UNION ALL
    SELECT 'image_decode_ms', ROUND(SUM(dur) / 1e6, 2)
    FROM slice WHERE name LIKE 'ImageDecoder#decodeDrawable%' OR name LIKE 'Decoding % bitmap'
    UNION ALL
    SELECT 'image_decode_count', COUNT(*)
    FROM slice WHERE name LIKE 'ImageDecoder#decodeDrawable%' OR name LIKE 'Decoding % bitmap'
    UNION ALL
    SELECT 'lock_contention_ms', ROUND(SUM(dur) / 1e6, 2)
    FROM slice WHERE name LIKE '%InvalidationTracker.syncTriggers%'
    UNION ALL
    SELECT 'lock_contention_count', COUNT(*)
    FROM slice WHERE name LIKE '%InvalidationTracker.syncTriggers%'
    UNION ALL
    SELECT 'main_thread_ms', ROUND(SUM(slice.dur) / 1e6, 2)
    FROM slice JOIN thread_track ON slice.track_id = thread_track.id JOIN thread USING (utid)
    WHERE thread.name = 'com.james.anvil'
    UNION ALL
    SELECT 'render_thread_ms', ROUND(SUM(slice.dur) / 1e6, 2)
    FROM slice JOIN thread_track ON slice.track_id = thread_track.id JOIN thread USING (utid) JOIN process USING (upid)
    WHERE process.name = 'com.james.anvil' AND thread.name = 'RenderThread';
    """
    with tempfile.NamedTemporaryFile("w", suffix=".sql", delete=False) as tf:
        tf.write(query)
        tf_path = tf.name
    try:
        res = subprocess.run(
            [sys.executable, trace_processor, "-q", tf_path, trace_file],
            capture_output=True,
            text=True,
            check=True
        )
        data = {}
        for line in res.stdout.strip().split("\n"):
            if "," in line:
                parts = [p.strip('"') for p in line.split(",")]
                if len(parts) == 2:
                    try:
                        data[parts[0]] = float(parts[1])
                    except ValueError:
                        data[parts[0]] = 0.0
        return data
    finally:
        if os.path.exists(tf_path):
            os.remove(tf_path)

print("Ingesting Trace A (Before)...")
res_b = analyze_single_trace(trace_before)

print("Ingesting Trace B (After)...")
res_a = analyze_single_trace(trace_after)

print("\n" + "="*85)
print(f" {'PERFETTO TRACE COMPARISON: BEFORE vs AFTER OPTIMIZATIONS':^83}")
print("="*85)
print(f"{'Performance Metric':<38} | {'Before (224331)':<16} | {'After (225257)':<16} | {'Improvement / Delta'}")
print("-" * 85)

metrics_meta = [
    ("max_frame_ms", "Max Cold Start Frame Jank", "ms", True),
    ("avg_frame_ms", "Average Frame Duration", "ms", True),
    ("shader_compile_ms", "GPU Shader Compilation Time", "ms", True),
    ("shader_compile_count", "GPU Shader Compiles", "compiles", True),
    ("recompose_ms", "Compose Recomposition Time", "ms", True),
    ("recompose_count", "Compose Recompositions", "passes", True),
    ("measure_layout_ms", "Measure & Layout Time", "ms", True),
    ("image_decode_ms", "Bitmap/Icon Decoding Time", "ms", True),
    ("image_decode_count", "Bitmap/Icon Decodes", "decodes", True),
    ("lock_contention_ms", "Room DB Lock Contention Time", "ms", True),
    ("lock_contention_count", "Room DB Lock Contention Events", "events", True),
    ("main_thread_ms", "Main Thread CPU Workload", "ms", True),
    ("render_thread_ms", "RenderThread Total Workload", "ms", True),
]

for key, label, unit, lower_better in metrics_meta:
    b = res_b.get(key, 0.0)
    a = res_a.get(key, 0.0)
    diff = a - b
    pct = (diff / b * 100) if b != 0 else 0
    sign = "+" if diff > 0 else ""
    
    if diff < 0:
        badge = "[-] "
    elif diff > 0:
        badge = "[+] "
    else:
        badge = "[=] "

    b_str = f"{b:.2f} {unit}" if "ms" in unit else f"{int(b)} {unit}"
    a_str = f"{a:.2f} {unit}" if "ms" in unit else f"{int(a)} {unit}"
    delta_str = f"{badge}{sign}{diff:.2f} {unit} ({sign}{pct:.1f}%)" if "ms" in unit else f"{badge}{sign}{int(diff)} ({sign}{pct:.1f}%)"

    print(f"{label:<38} | {b_str:<16} | {a_str:<16} | {delta_str}")

print("="*85 + "\n")
