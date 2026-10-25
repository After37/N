#!/system/bin/sh
MODDIR=${0%/*}
LOG=/data/local/tmp/poxel_neuralbridge.log
(
  sleep 25
  "$MODDIR/bin/neuralbridge" list > "$LOG" 2>&1
  "$MODDIR/bin/neuralbridge" test >> "$LOG" 2>&1
  echo "Starting persistent root-only IPC daemon" >> "$LOG"
  exec "$MODDIR/bin/neuralbridge" daemon >> "$LOG" 2>&1
) &
