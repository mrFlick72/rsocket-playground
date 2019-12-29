#!/usr/bin/env bash

echo "how many events?"
read METRICS_NUMBER

echo "what kind of metrics whould you like emit?"
read METRICS_NAME

echo "what value of metrics whould you like emit?"
read METRICS_VALUE

echo "METRICS_NUMBER= $METRICS_NUMBER, METRICS_NAME= $METRICS_NAME, METRICS_VALUE= $METRICS_VALUE"

for i in $(seq $METRICS_NUMBER); do

  CURL_BODY="{\"name\":\"$METRICS_NAME\", \"value\":\"$METRICS_VALUE\"}"

  curl -d "$CURL_BODY" \
    -H "Content-Type: application/json" \
    -X PUT http://localhost:8080/metrics/temperature

done;
