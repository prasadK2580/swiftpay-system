# SwiftPay on Kubernetes

## Prerequisites

- Minikube or any Kubernetes cluster
- `kubectl`
- Docker

## Deploy

```bash
# Build image into Minikube's Docker daemon
minikube start
eval $(minikube docker-env)          # Git Bash / macOS / Linux
# minikube -p minikube docker-env    # PowerShell: minikube docker-env | Invoke-Expression

docker build -f ledger-service/Dockerfile -t swiftpay-ledger:local .
docker build -f gateway-service/Dockerfile -t swiftpay-gateway:local .

kubectl apply -f k8s/namespace.yaml
kubectl apply -f k8s/postgres.yaml
kubectl apply -f k8s/redis.yaml
kubectl apply -f k8s/kafka.yaml

kubectl wait --for=condition=ready pod -l app=postgres -n swiftpay --timeout=180s
kubectl wait --for=condition=ready pod -l app=redis -n swiftpay --timeout=120s
kubectl wait --for=condition=ready pod -l app=kafka -n swiftpay --timeout=300s

kubectl apply -f k8s/app.yaml
kubectl wait --for=condition=ready pod -l app=swiftpay-ledger -n swiftpay --timeout=300s
kubectl wait --for=condition=ready pod -l app=swiftpay-gateway -n swiftpay --timeout=300s
```

## Access

```bash
minikube service swiftpay-gateway -n swiftpay
# Or NodePort: http://$(minikube ip):30080/swagger-ui.html
```

## Teardown

```bash
kubectl delete namespace swiftpay
```
