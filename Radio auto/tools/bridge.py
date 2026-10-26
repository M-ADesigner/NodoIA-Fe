"""Puente celular <-> radio emulada.

Lee la sesión de música del celular real (adb dumpsys media_session) y se la manda
al launcher que corre en el emulador, con el broadcast que el launcher ya entiende
cuando no hay sesión propia (com.android.music.metachanged).

Uso: python tools/bridge.py [serial_celular] [serial_emulador]
"""
import os
import re
import subprocess
import sys
import threading
import time

ADB = os.path.expandvars(r"%LOCALAPPDATA%\Android\Sdk\platform-tools\adb.exe")
PHONE = sys.argv[1] if len(sys.argv) > 1 else None
EMU = sys.argv[2] if len(sys.argv) > 2 else "emulator-5554"


def adb(serial, *args):
    cmd = [ADB] + (["-s", serial] if serial else []) + list(args)
    return subprocess.run(cmd, capture_output=True, text=True, encoding="utf-8", errors="replace").stdout


def phone_serial():
    for line in adb(None, "devices").splitlines()[1:]:
        parts = line.split()
        if len(parts) == 2 and parts[1] == "device" and not parts[0].startswith("emulator-"):
            return parts[0]
    return None


def now_playing(serial):
    """(title, artist, playing) de la mejor sesión: la que suena, y si no la primera con metadata."""
    out = adb(serial, "shell", "dumpsys", "media_session")
    sessions = []
    # Un bloque por sesión: "package=X", "state=PlaybackState {state=PLAYING(3)", "metadata: ... description=T, A, ..."
    for block in re.split(r"(?=^\s*package=)", out, flags=re.M):
        pkg = re.match(r"\s*package=([\w.]+)", block)
        state = re.search(r"state=PlaybackState \{state=(\w+)\(", block)
        d = re.search(r"metadata:.*?description=(.*)", block)
        if not (pkg and state and d) or pkg.group(1) == "com.android.server.telecom":
            continue
        parts = [p.strip() for p in d.group(1).split(", ")]
        if not parts[0] or parts[0] == "null":
            continue
        sessions.append((state.group(1) == "PLAYING", parts[0], parts[1] if len(parts) > 1 and parts[1] != "null" else ""))
    if not sessions:
        return None
    best = next((s for s in sessions if s[0]), sessions[0])
    return best[1], best[2], best[0]


def sh_quote(text):
    return "'" + text.replace("'", "'\\''") + "'"


def send(title, artist, playing):
    cmd = (
        "am broadcast -a com.android.music.metachanged"
        f" --es track {sh_quote(title)} --es artist {sh_quote(artist)}"
        f" --ez playing {'true' if playing else 'false'}"
    )
    adb(EMU, "shell", cmd)


KEYS = {"next": "87", "prev": "88", "playpause": "85"}


def forward_commands(phone):
    """Botones de la radio (o volante simulado) -> teclas de media en el celular real."""
    adb(EMU, "logcat", "-c")
    proc = subprocess.Popen(
        [ADB, "-s", EMU, "logcat", "-v", "raw", "-s", "RadioCmd:I"],
        stdout=subprocess.PIPE, text=True, encoding="utf-8", errors="replace",
    )
    for line in proc.stdout:
        key = KEYS.get(line.strip())
        if key:
            adb(phone, "shell", "input", "keyevent", key)
            print(f"[radio -> celular] {line.strip()}", flush=True)


def main():
    phone = PHONE or phone_serial()
    if not phone:
        sys.exit("No encuentro el celular por adb.")
    threading.Thread(target=forward_commands, args=(phone,), daemon=True).start()
    print(f"Celular {phone} -> emulador {EMU}. Ctrl+C para salir.")
    last = None
    sent_at = 0.0
    while True:
        cur = now_playing(phone)
        # Se reenvía cada 10 s aunque no cambie: si la radio se reinicia, recupera la canción.
        if cur and (cur != last or time.time() - sent_at > 10):
            send(*cur)
            if cur != last:
                print(("[play] " if cur[2] else "[pausa] ") + f"{cur[0]} - {cur[1]}".encode("ascii", "replace").decode(), flush=True)
            last = cur
            sent_at = time.time()
        time.sleep(1.5)


if __name__ == "__main__":
    main()
