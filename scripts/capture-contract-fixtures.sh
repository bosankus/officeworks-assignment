#!/usr/bin/env bash
# Captures real responses from the running backend as test fixtures for core:data.
# Usage: start the backend (cd backend && npm run dev), then: ./scripts/capture-contract-fixtures.sh
set -euo pipefail

BASE="${1:-http://localhost:3000/api/v1}"
OUT="$(cd "$(dirname "$0")/.." && pwd)/core/data/src/commonTest/fixtures"
mkdir -p "$OUT"
echo "Capturing from $BASE into $OUT"

capture() { # name expected_status curl_args...
  local name=$1 expected=$2; shift 2
  local status
  status=$(curl -s -o "$OUT/$name" -w '%{http_code}' "$@")
  if [[ "$status" != "$expected" ]]; then
    echo "FAIL $name: expected HTTP $expected, got $status" >&2
    exit 1
  fi
  echo "ok   $name ($status)"
}

capture featured.json             200 "$BASE/products/featured"
capture categories.json           200 "$BASE/categories"
capture category-electronics.json 200 "$BASE/categories/electronics/products"
capture search-jacket.json        200 "$BASE/products?query=jacket"
capture product-3.json            200 "$BASE/products/3"
capture product-999.json          404 "$BASE/products/999"
# Invalid on purpose (blank name, bad email), so no email is sent and no rate-limit slot matters.
capture enquiry-invalid.json      400 -X POST "$BASE/enquiries" \
  -H 'Content-Type: application/json' -H 'Idempotency-Key: fixture-capture' \
  -d '{"productId":3,"name":"","email":"not-an-email"}'

echo "Fixtures written to $OUT"
