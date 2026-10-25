# POXelOS NeuralBridge 0.4.0

ARM64 Android NNAPI model-execution service for POCO X3 Pro (vayu), Snapdragon 860.

**Implemented:** persistent root-only local Unix socket daemon, NNAPI device enumeration, hardware-only NNAPI model compilation and execution for FLOAT32 tensor ADD, output result over IPC. An explicit DSP/HTA device is required; execution fails rather than silently falling back to CPU. Magisk starts/restarts daemon after boot.

**Not implemented:** generic ONNX/TFLite/SNPE model loading, arbitrary operator graphs, app hooks, global CPU/GPU offload, energy savings guarantee.

## Build/install

GitHub Actions builds `POXelOS_NeuralBridge_arm64.zip`. Download successful run artifact, extract inner ZIP, install via Magisk, reboot.

## Commands

```sh
su -c '/data/adb/modules/poxel_neuralbridge/bin/neuralbridge client list'
su -c '/data/adb/modules/poxel_neuralbridge/bin/neuralbridge client test'
su -c '/data/adb/modules/poxel_neuralbridge/bin/neuralbridge client add 1.25 2.75'
su -c '/data/adb/modules/poxel_neuralbridge/bin/neuralbridge add 1.25 2.75'
cat /data/local/tmp/poxel_neuralbridge.log
```

Expected for working DSP backend: `result=4 expected=4`. If NNAPI does not expose Qualcomm DSP, or its driver rejects the ADD model, an error is expected and is **not** evidence of successful DSP execution.

The socket is in Linux abstract namespace, with SO_PEERCRED requiring UID 0. This is a minimal proof of a real computation API, **not** a general model runtime. For useful workloads, add supported operators, model parsing and application integration. Recovery removal is possible, but does not validate device compatibility.
