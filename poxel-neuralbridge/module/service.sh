#!/system/bin/sh
MODDIR=${0%/*}
(sleep 25; "$MODDIR/bin/neuralbridge" list > /data/local/tmp/poxel_neuralbridge.log 2>&1; "$MODDIR/bin/neuralbridge" test qti-dsp >> /data/local/tmp/poxel_neuralbridge.log 2>&1) &
