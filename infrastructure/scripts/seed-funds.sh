#!/bin/bash
set -euo pipefail

ENVIRONMENT="${1:-dev}"
REGION="${2:-us-east-1}"
TABLE_NAME="${ENVIRONMENT}-funds"

echo "==> Seeding funds catalog into table: $TABLE_NAME"

aws dynamodb batch-write-item --region "$REGION" --request-items "{
  \"$TABLE_NAME\": [
    {\"PutRequest\": {\"Item\": {\"id\": {\"S\": \"1\"}, \"name\": {\"S\": \"FPV_BTG_PACTUAL_RECAUDADORA\"}, \"minimumAmount\": {\"N\": \"75000\"}, \"category\": {\"S\": \"FPV\"}}}},
    {\"PutRequest\": {\"Item\": {\"id\": {\"S\": \"2\"}, \"name\": {\"S\": \"FPV_BTG_PACTUAL_ECOPETROL\"}, \"minimumAmount\": {\"N\": \"125000\"}, \"category\": {\"S\": \"FPV\"}}}},
    {\"PutRequest\": {\"Item\": {\"id\": {\"S\": \"3\"}, \"name\": {\"S\": \"DEUDAPRIVADA\"}, \"minimumAmount\": {\"N\": \"50000\"}, \"category\": {\"S\": \"FIC\"}}}},
    {\"PutRequest\": {\"Item\": {\"id\": {\"S\": \"4\"}, \"name\": {\"S\": \"FDO-ACCIONES\"}, \"minimumAmount\": {\"N\": \"250000\"}, \"category\": {\"S\": \"FIC\"}}}},
    {\"PutRequest\": {\"Item\": {\"id\": {\"S\": \"5\"}, \"name\": {\"S\": \"FPV_BTG_PACTUAL_DINAMICA\"}, \"minimumAmount\": {\"N\": \"100000\"}, \"category\": {\"S\": \"FPV\"}}}}
  ]
}"

echo "==> Seeding test client (balance: COP 500.000)..."

aws dynamodb put-item --region "$REGION" \
  --table-name "${ENVIRONMENT}-clients" \
  --item '{
    "id": {"S": "client-001"},
    "name": {"S": "Juan Pérez"},
    "email": {"S": "juan.perez@email.com"},
    "phone": {"S": "+573001234567"},
    "balance": {"N": "500000"},
    "notificationPreference": {"S": "EMAIL"}
  }'

echo "==> Seed complete!"
