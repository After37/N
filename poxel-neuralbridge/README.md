# POXelOS NeuralBridge (experimental)

POCO X3 Pro (vayu), Android 14. This is a **real NNAPI hardware execution probe**, not a universal NPU interception layer. It does not redirect arbitrary Android CPU/GPU workloads.

GitHub Actions builds an ARM64 Magisk ZIP. Download the Actions artifact, extract the inner ZIP and install through Magisk. After reboot inspect `/data/local/tmp/poxel_neuralbridge.log`.

Detected HIDL qti-dsp/qti-hta services do **not** guarantee visibility via the NDK NNAPI runtime. The program lists NDK devices and attempts a minimal ADD operation specifically on qti-dsp. No changes to thermal controls, SELinux or vendor libraries. Remove module via Magisk to uninstall.
