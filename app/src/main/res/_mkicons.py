import os, glob, re
RES = r"C:/Users/Administrator/WorkBuddy/2026-09-22-23-47-41/BBspace-src/app/src/main/res"
ADAPTIVE = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@drawable/ic_launcher_bb_background" />
    <foreground android:drawable="@drawable/ic_launcher_bb_foreground" />
    <monochrome android:drawable="@drawable/ic_launcher_bb_monet_foreground" />
</adaptive-icon>
"""
MONO = """<?xml version="1.0" encoding="utf-8"?>
<adaptive-icon xmlns:android="http://schemas.android.com/apk/res/android">
    <background android:drawable="@android:color/white" />
    <foreground android:drawable="@drawable/ic_launcher_bb_monet_foreground" />
{%mono%}</adaptive-icon>
"""
# 所有 mipmap-anydpi-v26 下的 ic_launcher*.xml（含 round）
targets = []
for d in ["mipmap-anydpi-v26","mipmap-night-anydpi-v26"]:
    dd = os.path.join(RES, d)
    if os.path.isdir(dd):
        for f in os.listdir(dd):
            if f.startswith("ic_launcher") and f.endswith(".xml"):
                targets.append(os.path.join(dd, f))
log=[]
for t in targets:
    with open(t, "w", encoding="utf-8", newline="\n") as f:
        f.write(ADAPTIVE)
    log.append(f"wrote {os.path.relpath(t, RES)}")
print("\n".join(log))
print("total:", len(targets))
