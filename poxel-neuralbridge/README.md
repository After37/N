# POXelOS NeuralBridge — persistent NNAPI probe/service

Target: POCO X3 Pro (vayu), Android 14. This is a **native, persistent, root-only IPC service** for querying NNAPI devices and running a hardware-backed ADD test on qti-dsp.

## Installation
Download the successful GitHub Actions artifact, extract the **inner** `POXelOS_NeuralBridge_arm64.zip`, install that ZIP in Magisk, and reboot. Module autostarts after 25 seconds.

## Commands
Run as root:
```sh
su -c '/data/adb/modules/poxel_neuralbridge/bin/neuralbridge client list'
su -c '/data/adb/modules/poxel_neuralbridge/bin/neuralbridge client test'
cat /data/local/tmp/poxel_neuralbridge.log
```
The daemon uses an abstract Unix socket `poxel_neuralbridge_v1` and checks peer credentials (UID 0 required). It accepts only `list` and `test`, with no arbitrary command execution.

## Important limitations
This is **not a universal NPU offload layer**. It does not intercept app CPU instructions, OpenGL, Vulkan, or GPU shaders. It provides a stable root-only diagnostic IPC entry point; real app inference requires an app-side adapter, a model interface, model validation and backend support. HIDL qti-dsp registration alone does not prove NNAPI NDK exposes that device. Test must succeed on-device to confirm backend functionality.

No system/vendor replacement, SELinux bypass or thermal modifications. Uninstall via Magisk or recovery. Build success does not establish on-device execution or battery savings.

Reference: https://developer.android.com/ndk/guides/neuralnetworks
