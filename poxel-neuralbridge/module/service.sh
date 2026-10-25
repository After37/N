#!/system/bin/sh
MODDIR=${0%/*}
LOG=/data/local/tmp/poxel_neuralbridge.log
(
  sleep 25
  "$MODDIR/bin/neuralbridge" list > "$LOG" 2>&1
  "$MODDIR/bin/neuralbridge" test >> "$LOG" 2>&1
  while [ -x "$MODDIR/bin/neuralbridge" ]; do
    echo "Starting NeuralBridge IPC daemon at $(date)" >> "$LOG"
    "$MODDIR/bin/neuralbridge" daemon >> "$LOG" 2>&1
    echo "Daemon exited; restarting in 10 seconds" >> "$LOG"
    sleep 10
  done
) &
