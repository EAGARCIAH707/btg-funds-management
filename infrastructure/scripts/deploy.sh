#!/bin/bash
set -euo pipefail

STACK_NAME="${1:-btg-funds-management-stack}"
ENVIRONMENT="${2:-dev}"
REGION="${3:-us-east-1}"
TEMPLATE_FILE="$(dirname "$0")/../cloudformation/template.yaml"

echo "========================================="
echo " BTG Funds - CloudFormation Deploy"
echo "========================================="
echo " Stack:       $STACK_NAME"
echo " Environment: $ENVIRONMENT"
echo " Region:      $REGION"
echo "========================================="

# --- Step 1: Deploy infrastructure (ECR, VPC, ECS, DynamoDB, SNS) ---
echo "==> Deploying CloudFormation stack..."
aws cloudformation deploy \
  --template-file "$TEMPLATE_FILE" \
  --stack-name "$STACK_NAME" \
  --parameter-overrides Environment="$ENVIRONMENT" \
  --capabilities CAPABILITY_NAMED_IAM \
  --region "$REGION" \
  --no-fail-on-empty-changeset

# --- Step 2: Get ECR URI and build/push image ---
ECR_URI=$(aws cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --query "Stacks[0].Outputs[?OutputKey=='EcrRepositoryUri'].OutputValue" \
  --output text)

echo "==> ECR Repository: $ECR_URI"

ACCOUNT_ID=$(aws sts get-caller-identity --query Account --output text)
aws ecr get-login-password --region "$REGION" | docker login --username AWS --password-stdin "${ACCOUNT_ID}.dkr.ecr.${REGION}.amazonaws.com"

IMAGE_TAG="${ECR_URI}:latest"
echo "==> Building Docker image..."
docker build -t "$IMAGE_TAG" "$(dirname "$0")/../.."

echo "==> Pushing image to ECR..."
docker push "$IMAGE_TAG"

# --- Step 3: Update stack with the image URI ---
echo "==> Updating stack with image: $IMAGE_TAG"
aws cloudformation deploy \
  --template-file "$TEMPLATE_FILE" \
  --stack-name "$STACK_NAME" \
  --parameter-overrides Environment="$ENVIRONMENT" AppImage="$IMAGE_TAG" \
  --capabilities CAPABILITY_NAMED_IAM \
  --region "$REGION" \
  --no-fail-on-empty-changeset

echo ""
echo "==> Stack outputs:"
aws cloudformation describe-stacks \
  --stack-name "$STACK_NAME" \
  --region "$REGION" \
  --query "Stacks[0].Outputs" \
  --output table
