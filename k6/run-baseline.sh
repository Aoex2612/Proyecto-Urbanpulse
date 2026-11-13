#!/usr/bin/env bash
# ==============================================================================
# Script de automatización para la Baseline de UrbanPulse (Experimento 1)
# ==============================================================================

set -e

BASE_URL="${BASE_URL:-http://localhost:8080}"
VUS="${VUS:-10}"
DURATION="${DURATION:-2m}"
OUTPUT_DIR="k6/reports"

mkdir -p "$OUTPUT_DIR"

echo "=========================================================="
echo " URBANPULSE - Medición de Baseline con Grafana k6"
echo " URL: $BASE_URL | VUs: $VUS | Duración: $DURATION"
echo "=========================================================="

ENDPOINTS=("list_incidents" "get_incident" "search_incidents" "analytics_summary" "create_incident")

if [ -n "$1" ]; then
    ENDPOINTS=("$1")
fi

for ep in "${ENDPOINTS[@]}"; do
    echo ""
    echo ">> Iniciando medición de baseline para: $ep"
    REPORT_FILE="$OUTPUT_DIR/baseline_${ep}.html"
    
    K6_WEB_DASHBOARD=true \
    K6_WEB_DASHBOARD_EXPORT="$REPORT_FILE" \
    ENDPOINT="$ep" \
    VUS="$VUS" \
    DURATION="$DURATION" \
    BASE_URL="$BASE_URL" \
    k6 run k6/baseline.js
    
    echo ">> Informe generado en: $REPORT_FILE"
done

echo ""
echo "=========================================================="
echo " Baseline completada. Revisa los informes en $OUTPUT_DIR"
echo "=========================================================="
