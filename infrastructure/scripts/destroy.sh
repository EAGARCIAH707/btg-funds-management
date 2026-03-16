#!/bin/bash
set -euo pipefail

ENVIRONMENT="${1:-dev}"
REGION="${2:-us-east-1}"
INFRA_STACK="${ENVIRONMENT}-btg-funds-infra"
PIPELINE_STACK="${ENVIRONMENT}-btg-funds-pipeline"

echo "========================================="
echo " BTG Funds - Destroy ALL Resources"
echo "========================================="
echo " Infra stack:    $INFRA_STACK"
echo " Pipeline stack: $PIPELINE_STACK"
echo " Environment:    $ENVIRONMENT"
echo " Region:         $REGION"
echo "========================================="
echo ""
read -p "Are you sure you want to DELETE all resources? (yes/no): " CONFIRM
if [[ "$CONFIRM" != "yes" ]]; then
  echo "Aborted."
  exit 0
fi

# --- Helper: delete stack and wait ---
delete_stack() {
  local stack_name=$1
  local stack_status
  stack_status=$(aws cloudformation describe-stacks --stack-name "$stack_name" --region "$REGION" --query 'Stacks[0].StackStatus' --output text 2>/dev/null || echo "NOT_FOUND")

  if [[ "$stack_status" == "NOT_FOUND" ]]; then
    echo "    Stack '$stack_name' does not exist. Skipping."
    return
  fi

  echo "==> Deleting stack: $stack_name"
  aws cloudformation delete-stack --stack-name "$stack_name" --region "$REGION"
  echo "    Waiting for deletion..."
  aws cloudformation wait stack-delete-complete --stack-name "$stack_name" --region "$REGION"
  echo "    Done."
}

# --- Step 1: Empty ECR repository ---
ECR_REPO="${ENVIRONMENT}-btg-funds-api"
echo "==> Cleaning ECR repository: $ECR_REPO"
IMAGE_IDS=$(aws ecr list-images --repository-name "$ECR_REPO" --region "$REGION" --query 'imageIds[*]' --output json 2>/dev/null || echo "[]")
if [[ "$IMAGE_IDS" != "[]" ]]; then
  aws ecr batch-delete-image \
    --repository-name "$ECR_REPO" \
    --image-ids "$IMAGE_IDS" \
    --region "$REGION" > /dev/null
  echo "    ECR images deleted."
else
  echo "    No images to delete."
fi

# --- Step 2: Delete infra stack first ---
delete_stack "$INFRA_STACK"

# --- Step 3: Empty pipeline artifacts bucket and delete pipeline stack ---
BUCKET="${ENVIRONMENT}-btg-funds-pipeline-artifacts-$(aws sts get-caller-identity --query Account --output text)"
echo "==> Emptying bucket: $BUCKET"
aws s3 rm "s3://$BUCKET" --recursive 2>/dev/null || echo "    Bucket not found or already empty."

delete_stack "$PIPELINE_STACK"

echo ""
echo "========================================="
echo " All resources deleted successfully"
echo "========================================="
