# GPU Acceleration Setup Guide

## NVIDIA RTX 5060 Ti 16GB — Docker GPU Passthrough

This guide covers enabling your **NVIDIA RTX 5060 Ti 16GB** inside Docker for Ollama model inference. The setup applies to any NVIDIA GPU with 8GB+ VRAM.

---

## 1. Prerequisites

| Component | Requirement |
|-----------|-------------|
| GPU | NVIDIA RTX 5060 Ti 16GB (or any NVIDIA GPU with 8GB+ VRAM) |
| Driver | NVIDIA Game Ready or Studio Driver 551.86+ |
| OS | Windows 11 with WSL2 (recommended) or Linux |
| Docker | Docker Desktop 4.30+ (Windows) or Docker CE 24+ (Linux) |
| VRAM Note | `llama3.2:3b` runs comfortably in 4GB VRAM. The 16GB RTX 5060 Ti can run up to 13B parameter models. |

---

## 2. Windows 11 — WSL2 Setup (Recommended)

### 2.1 Verify NVIDIA Driver

```powershell
# Open PowerShell and run:
nvidia-smi
```

Expected output shows your RTX 5060 Ti:
```
+-----------------------------------------------------------------------------+
| NVIDIA-SMI 551.86        Driver Version: 551.86        CUDA Version: 12.4   |
|-------------------------------+----------------------+----------------------+
| GPU  Name            TCC/WDDM | Bus-Id        Disp.A | Volatile Uncorr. ECC |
| Fan  Temp  Perf  Pwr:Usage/Cap|         Memory-Usage | GPU-Util  Compute C. |
|===============================+======================+======================|
|   0  NVIDIA RTX 5060 Ti  WDDM | 00000000:01:00.0  On |                  N/A |
| 30%   45C    P0    45W / 180W |   1246MiB / 16384MiB |      0%      Default |
+-----------------------------------------------------------------------------+
```

### 2.2 Install WSL2

```powershell
# Enable WSL
wsl --install -d Ubuntu-24.04

# Verify WSL2 is default
wsl --set-default-version 2

# Restart your computer
Restart-Computer
```

### 2.3 Install Docker Desktop

1. Download [Docker Desktop for Windows](https://docs.docker.com/desktop/install/windows-install/)
2. During installation, select **"Use WSL 2 instead of Hyper-V"**
3. After install, open Docker Desktop → Settings → Resources → WSL Integration
4. Enable integration with your Ubuntu WSL distro

### 2.4 Install NVIDIA Container Toolkit in WSL2

Inside your WSL2 Ubuntu terminal:

```bash
# Add NVIDIA package repositories
curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey | sudo gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg

curl -s -L https://nvidia.github.io/libnvidia-container/stable/deb/nvidia-container-toolkit.list | \
  sed 's#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g' | \
  sudo tee /etc/apt/sources.list.d/nvidia-container-toolkit.list

# Install
sudo apt-get update
sudo apt-get install -y nvidia-container-toolkit

# Configure Docker to use NVIDIA runtime
sudo nvidia-ctk runtime configure --runtime=docker

# Restart Docker daemon
sudo service docker restart
```

### 2.5 Verify GPU in Docker

```powershell
# From PowerShell (Docker Desktop handles WSL2 automatically):
docker run --rm --gpus all nvidia/cuda:12.4.0-base-ubuntu22.04 nvidia-smi
```

Expected: You should see the same `nvidia-smi` output as step 2.1 but running **inside Docker**.

---

## 3. Linux Native Setup

```bash
# Install NVIDIA drivers
sudo apt-get install -y nvidia-driver-550

# Install NVIDIA Container Toolkit
curl -fsSL https://nvidia.github.io/libnvidia-container/gpgkey | sudo gpg --dearmor -o /usr/share/keyrings/nvidia-container-toolkit-keyring.gpg

curl -s -L https://nvidia.github.io/libnvidia-container/stable/deb/nvidia-container-toolkit.list | \
  sed 's#deb https://#deb [signed-by=/usr/share/keyrings/nvidia-container-toolkit-keyring.gpg] https://#g' | \
  sudo tee /etc/apt/sources.list.d/nvidia-container-toolkit.list

sudo apt-get update
sudo apt-get install -y nvidia-container-toolkit

# Configure Docker
sudo nvidia-ctk runtime configure --runtime=docker
sudo systemctl restart docker

# Verify
docker run --rm --gpus all nvidia/cuda:12.4.0-base-ubuntu22.04 nvidia-smi
```

---

## 4. Run with GPU Support

### 4.1 Using Docker Compose with GPU override

```bash
cd AITutor

# Start with GPU enabled (uses docker-compose.gpu.yml overlay)
docker compose -f docker-compose.yml -f docker-compose.gpu.yml up --build -d

# Or use the GPU-specific compose file directly
docker compose -f docker-compose.gpu.yml -f docker-compose.yml up --build -d
```

### 4.2 Verify GPU is used by Ollama

```bash
# Check Ollama logs for GPU detection
docker compose logs ollama | grep -i "gpu\|cuda\|metal"

# Or enter the container and run
docker exec -it aitutor-ollama ollama run llama3.2:3b

# Inside the container, check GPU visibility
docker exec aitutor-ollama nvidia-smi
```

Expected Ollama log output showing GPU:
```
time=2026-06-23T... level=INFO msg="LLM server using GPU"
time=2026-06-23T... level=INFO msg="CUDA available: true"
```

### 4.3 Performance Benchmark

```bash
# Quick inference speed test
docker exec aitutor-ollama ollama run llama3.2:3b "Explain quantum computing in one sentence."

# With GPU: ~50-100 tokens/sec
# Without GPU: ~5-15 tokens/sec
```

---

## 5. GPU Configuration Reference

### docker-compose.gpu.yml Breakdown

```yaml
services:
  ollama:
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia          # NVIDIA driver
              count: 1                # Use 1 GPU
              capabilities: [gpu]     # GPU compute capability
    environment:
      - OLLAMA_KEEP_ALIVE=24h         # Keep model loaded in VRAM
```

### Controlling Which GPU to Use

If you have multiple GPUs:

```yaml
services:
  ollama:
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              device_ids: ['0']      # Use specific GPU (index 0)
              capabilities: [gpu]
```

Or via environment variable on the host:

```powershell
# PowerShell (before docker compose up)
$env:NVIDIA_VISIBLE_DEVICES="0"
```

### Ollama Model VRAM Requirements

| Model | Parameters | VRAM Required | Runs on RTX 5060 Ti 16GB? |
|-------|-----------|---------------|--------------------------|
| llama3.2:3b | 3B | ~4GB | ✅ Yes (fastest) |
| llama3.1:8b | 8B | ~8GB | ✅ Yes |
| deepseek-r1:7b | 7B | ~7GB | ✅ Yes |
| mistral:7b | 7B | ~7GB | ✅ Yes |
| llama3:70b | 70B | ~40GB | ❌ No (CPU only) |
| qwen2.5:14b | 14B | ~14GB | ✅ Yes (tight fit) |

---

## 6. Troubleshooting

### "nvidia-smi" not found inside container

```powershell
# Check NVIDIA Container Toolkit is installed
wsl -d Ubuntu nvidia-smi

# Reconfigure runtime
wsl -d Ubuntu sudo nvidia-ctk runtime configure --runtime=docker
wsl -d Ubuntu sudo service docker restart

# Restart Docker Desktop from Windows
# Settings → Troubleshoot → Reset to factory defaults → Restart
```

### Docker reports "could not select device driver "nvidia" with capabilities: [gpu]"

```powershell
# Verify the NVIDIA runtime is registered
docker info | Select-String "nvidia"

# You should see: Runtimes: nvidia
# If not, reinstall NVIDIA Container Toolkit:
# 1. Uninstall Docker Desktop
# 2. Restart Windows
# 3. Reinstall Docker Desktop
# 4. Install NVIDIA Container Toolkit in WSL2 (step 2.4)
```

### Ollama runs on CPU despite GPU being available

```powershell
# Force CUDA visibility
docker compose down
$env:NVIDIA_VISIBLE_DEVICES="all"
$env:CUDA_VISIBLE_DEVICES="0"
docker compose -f docker-compose.yml -f docker-compose.gpu.yml up --build -d
```

### "Ollama is ready but model pull fails" during backend startup

The backend entrypoint pulls the model and waits for completion. If the model is large, the download can take 5-10 minutes on first run:

```powershell
# Check download progress
docker logs aitutor-backend -f

# If download times out, pull manually:
docker exec aitutor-ollama ollama pull llama3.2:3b
docker compose restart backend
```

### CUDA Out of Memory

```powershell
# Check VRAM usage
nvidia-smi

# Free VRAM by stopping ollama
docker compose stop ollama
docker compose rm ollama

# Restart with memory limit
```

---

## 7. Advanced: Multi-GPU / Model Parallelism

For large models that don't fit on one RTX 5060 Ti:

```yaml
services:
  ollama:
    deploy:
      resources:
        reservations:
          devices:
            - driver: nvidia
              count: 1          # Set to 2 if you add a second GPU
              capabilities: [gpu]
    environment:
      - OLLAMA_NUM_GPU=1        # Number of GPUs to use
      - OLLAMA_GPU_LAYERS=999   # Max layers on GPU (0 = CPU only)
```

---

## 8. Comparison: With vs Without GPU

| Metric | CPU Only | RTX 5060 Ti (GPU) |
|--------|----------|-------------------|
| llama3.2:3b inference | ~8 tok/s | ~85 tok/s |
| Concept generation (one prompt) | 15-25s | 2-4s |
| MCQ generation | 10-20s | 1-3s |
| Doubt chat response | 5-15s | 1-3s |
| Power consumption | ~30W | ~80W |
