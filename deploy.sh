#!/bin/bash
set -e

NAMESPACE="cic-togive-dev"
PROJECT_DIR="$(cd "$(dirname "$0")" && pwd)"

echo "=== Deploying CIC ToGive to OpenShift ==="
echo "Namespace: $NAMESPACE"

# Ensure correct project
oc project "$NAMESPACE"

# Step 1: Deploy PostgreSQL
echo ""
echo "--- Step 1: PostgreSQL ---"
oc apply -f "$PROJECT_DIR/k8s/postgresql.yaml"
echo "Waiting for PostgreSQL to be ready..."
oc rollout status deployment/postgresql --timeout=120s

# Step 2: Build & Deploy Backend
echo ""
echo "--- Step 2: Backend ---"
oc apply -f "$PROJECT_DIR/k8s/backend.yaml"
echo "Building backend image..."
oc start-build backend --from-dir="$PROJECT_DIR/backend" --follow --wait
echo "Waiting for backend rollout..."
oc rollout status deployment/backend --timeout=180s

# Step 3: Build & Deploy Frontend
echo ""
echo "--- Step 3: Frontend ---"
oc apply -f "$PROJECT_DIR/k8s/frontend.yaml"
echo "Building frontend image..."
oc start-build frontend --from-dir="$PROJECT_DIR/Frontend" --follow --wait
echo "Waiting for frontend rollout..."
oc rollout status deployment/frontend --timeout=120s

# Done
echo ""
echo "=== Deployment complete ==="
ROUTE=$(oc get route frontend -o jsonpath='{.spec.host}')
echo "App URL: https://$ROUTE"
