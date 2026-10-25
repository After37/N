#!/system/bin/sh
ui_print "- NNAPI probe; no global interception"
[ "$ARCH" = arm64 ] || abort "Requires arm64"
[ -f "$MODPATH/bin/neuralbridge" ] || abort "Binary missing"
set_perm "$MODPATH/bin/neuralbridge" 0 0 0755
set_perm "$MODPATH/service.sh" 0 0 0755
