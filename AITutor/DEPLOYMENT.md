# Deployment Guide

## Prerequisites

- **Docker** 24+ with Compose plugin
- **Kubernetes** cluster (Minikube, kind, or cloud-based)
- **kubectl** configured
- **Helm** 3+ (optional, for ingress controller)
- **NVIDIA GPU** (optional, see [GPU Setup](GPU_SETUP.md) for acceleration)

---

## 1. Docker Compose (Local / Single Host)

### 1.1 Build and Start

```bash
cd AITutor

# Build and start all services
docker compose up --build -d

# Check logs
docker compose logs -f
```

How to use
# Docker compose with GPU
cd AITutor
docker compose -f docker-compose.yml -f docker-compose.gpu.yml up --build -d

# Verify
docker exec aitutor-ollama nvidia-smi
Your RTX 5060 Ti 16GB will give ~85 tok/s for llama3.2:3b vs 8 tok/s on CPU. The guide also covers VRAM limits — you can run models up to 13B params on 16GB.

### 1.2 Access

| Service  | URL                         |
|----------|-----------------------------|
| Frontend | http://localhost:3000       |
| Backend  | http://localhost:8080       |
| Ollama   | http://localhost:11434      |

### 1.3 Enable GPU Acceleration (NVIDIA)

If you have an NVIDIA GPU (e.g., RTX 5060 Ti), see [GPU_SETUP.md](GPU_SETUP.md) for full setup.

```bash
# Start with GPU support (compose override file)
docker compose -f docker-compose.yml -f docker-compose.gpu.yml up --build -d

# Verify GPU is used
docker exec aitutor-ollama nvidia-smi
```

### 1.4 Stop

```bash
docker compose down
# Remove volumes (deletes DB + models)
docker compose down -v
```

### 1.5 Architecture

```
┌──────────────┐     ┌──────────────┐     ┌──────────────┐
│   Frontend   │────▶│   Backend    │────▶│    Ollama    │
│  nginx:80    │     │  Java:8080   │     │  :11434      │
│  React SPA   │     │  Spring Boot │     │  llama3.2:3b │
└──────────────┘     └──────────────┘     └──────────────┘
```

- Frontend nginx proxies `/api/*` to backend at `aitutor-backend:8080`
- Backend waits for Ollama healthcheck, then pulls `llama3.2:3b` on startup
- H2 database stored in `app_data` Docker volume

---

## 2. Kubernetes

### 2.1 Deploy

```bash
cd AITutor/k8s

# Create namespace and deploy all resources
kubectl apply -f namespace.yaml
kubectl apply -f ollama-deployment.yaml
kubectl apply -f backend-deployment.yaml
kubectl apply -f frontend-deployment.yaml
kubectl apply -f ingress.yaml

# Check status
kubectl get all -n aitutor
kubectl get pods -n aitutor -w
```

### 2.2 Access

With ingress controller installed (`ingress-nginx`):

```bash
# Add to /etc/hosts or C:\Windows\System32\drivers\etc\hosts
# 127.0.0.1 aitutor.local

# If using Minikube
minikube tunnel

# Then visit: http://aitutor.local
```

Without ingress (port-forwarding):

```bash
# Frontend
kubectl port-forward -n aitutor svc/aitutor-frontend 3000:80

# Backend
kubectl port-forward -n aitutor svc/aitutor-backend 8080:8080
```

### 2.3 Cleanup

```bash
kubectl delete namespace aitutor
```

### 2.4 GPU Acceleration (NVIDIA)

For clusters with NVIDIA GPUs:

```bash
# Install NVIDIA device plugin
kubectl apply -f https://raw.githubusercontent.com/NVIDIA/k8s-device-plugin/v0.16.0/nvidia-device-plugin.yml

# Uncomment GPU resources in ollama-deployment.yaml, then reapply
kubectl apply -f ollama-deployment.yaml
```

Uncomment these sections in `k8s/ollama-deployment.yaml`:
- `nodeSelector:` → pins Ollama to GPU nodes
- `resources.limits.nvidia.com/gpu: 1` → requests 1 GPU

Full NVIDIA Docker setup: see [GPU_SETUP.md](GPU_SETUP.md).

### 2.5 Manual Ollama Model Pull

If the init container fails, pull the model manually:

```bash
kubectl exec -n aitutor -it deploy/ollama -- ollama pull llama3.2:3b
```

---

## 3. Building Docker Images Manually

```bash
# Backend
cd AITutor
docker build -t aitutor-backend:latest .

# Frontend
cd aitutor-ui
docker build -t aitutor-frontend:latest .

# Tag and push to a registry (optional, for K8s)
docker tag aitutor-backend:latest your-registry/aitutor-backend:latest
docker push your-registry/aitutor-backend:latest
```

---

## 4. AWS Deployment

### 4.1 ECS (Elastic Container Service)

Deploy via AWS Copilot:

```bash
# Install Copilot
curl -Lo copilot https://github.com/aws/copilot-cli/releases/latest/download/copilot-linux
chmod +x copilot && sudo mv copilot /usr/local/bin/

# Initialize and deploy
cd AITutor
copilot init --app aitutor \
  --name backend \
  --type "Backend Service" \
  --dockerfile ./Dockerfile

copilot init --app aitutor \
  --name ollama \
  --type "Backend Service" \
  --image ollama/ollama

# Deploy ollama first (needs GPU)
copilot env init --app aitutor --name prod
copilot deploy --app aitutor --env prod
```

**GPU Support on ECS:**

- Use `g4dn.xlarge` or `p3.2xlarge` instance types
- Add `"requiresCompatibilities": ["EC2"]` and `"device": "/dev/nvidia0"` in task definition
- For Fargate: GPU is not supported; use EC2 launch type

### 4.2 EKS (Elastic Kubernetes Service)

```bash
# Create EKS cluster
eksctl create cluster --name aitutor --region us-east-1 \
  --nodegroup-name ng-ollama --node-type g4dn.xlarge \
  --nodes 1 --nodes-min 1 --nodes-max 2 \
  --managed

# Configure kubectl
aws eks update-kubeconfig --region us-east-1 --name aitutor

# Install NVIDIA device plugin for GPU nodes
kubectl apply -f https://raw.githubusercontent.com/NVIDIA/k8s-device-plugin/v0.14.0/nvidia-device-plugin.yml

# Deploy AI Tutor
kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/ollama-deployment.yaml
kubectl apply -f k8s/backend-deployment.yaml
kubectl apply -f k8s/frontend-deployment.yaml

# Install ALB Ingress Controller
helm repo add eks https://aws.github.io/eks-charts
helm install aws-load-balancer-controller eks/aws-load-balancer-controller \
  -n kube-system --set clusterName=aitutor

kubectl apply -f k8s/ingress.yaml
```

### 4.3 GPU Configuration for Ollama on AWS

Add to `ollama-deployment.yaml` for GPU access (see [GPU_SETUP.md](GPU_SETUP.md) for general setup):

```yaml
spec:
  template:
    spec:
      nodeSelector:
        nvidia.com/gpu.present: "true"
      containers:
        - name: ollama
          resources:
            limits:
              nvidia.com/gpu: 1
```

Install the NVIDIA device plugin before deploying:

```bash
kubectl apply -f https://raw.githubusercontent.com/NVIDIA/k8s-device-plugin/v0.16.0/nvidia-device-plugin.yml
```

### 4.4 AWS Tips

| Tip | Details |
|-----|---------|
| **RDS instead of H2** | Switch to PostgreSQL with Amazon RDS for production. Add `spring.datasource.url=jdbc:postgresql://rds-endpoint:5432/aitutor` |
| **S3 for models** | Store ollama models on EFS or S3 with FUSE mount to avoid re-pulling on restart |
| **Secrets Manager** | Store OpenAI API keys in AWS Secrets Manager, inject via `spring.cloud.aws.secretsmanager` |
| **Auto Scaling** | Use HPA: `kubectl autoscale deployment aitutor-backend -n aitutor --cpu-percent=70 --min=2 --max=10` |
| **CloudFront** | Place CloudFront in front of ALB for caching static assets |
| **Cost Optimization** | Use Spot Instances for worker nodes (with `spotAllocationStrategy: capacity-optimized`) |
| **Monitoring** | Enable Container Insights: `eksctl utils update-cluster-logging --enable-types=all` |
| **Backup** | Schedule Velero backups for PVCs: `velero install --provider aws --bucket aitutor-backups --backup-location-config region=us-east-1` |

### 4.5 Alternative: Lightsail Containers

For smaller deployments, AWS Lightsail containers are simpler:

```bash
# Push images to Lightsail
aws lightsail create-container-service --service-name aitutor --power small --scale 1
aws lightsail create-container-service-deployment \
  --service-name aitutor \
  --containers file://lightsail-deploy.json
```

Create `lightsail-deploy.json`:

```json
{
  "backend": {
    "image": "aitutor-backend:latest",
    "environment": {
      "SPRING_PROFILES_ACTIVE": "docker",
      "SPRING_AI_OLLAMA_BASE_URL": "http://ollama:11434"
    },
    "ports": { "8080": "HTTP" }
  },
  "frontend": {
    "image": "aitutor-frontend:latest",
    "ports": { "80": "HTTP" }
  },
  "ollama": {
    "image": "ollama/ollama:latest",
    "command": ["serve"],
    "ports": { "11434": "HTTP" }
  }
}
```

---

## 5. Environment Variables Reference

| Variable | Default | Description |
|----------|---------|-------------|
| `REACT_APP_API_BASE` | `http://localhost:8080` | Backend URL (empty = relative for nginx proxy) |
| `SPRING_PROFILES_ACTIVE` | - | Spring profile (`docker` for containerized deploy) |
| `SPRING_AI_OLLAMA_BASE_URL` | `http://localhost:11434` | Ollama endpoint |
| `SPRING_AI_OPENAI_API_KEY` | - | OpenAI API key |
| `OLLAMA_HOST` | `http://ollama:11434` | Used by backend entrypoint to wait for Ollama |
| `OLLAMA_MODEL` | `llama3.2:3b` | Model to pull on startup |

---

## 6. Troubleshooting

### Backend won't start

```bash
# Check if Ollama is healthy
docker compose ps ollama
curl http://localhost:11434/api/tags

# Check backend logs
docker compose logs backend

# Manually pull the model
docker compose exec ollama ollama pull llama3.2:3b
```

### Frontend shows blank page

```bash
# Check nginx is serving
curl http://localhost:3000

# Check API proxy works (expected: 401 for unauthenticated)
curl http://localhost:3000/api/subjects
```

### Kubernetes Pods crash-looping

```bash
kubectl describe pod -n aitutor <pod-name>
kubectl logs -n aitutor <pod-name>
kubectl logs -n aitutor <pod-name> -c model-puller  # init container logs
```
