#!/usr/bin/env bash
# Runs the Gradle wrapper and turns Gradle's failure report and any warnings into
# GitHub annotations, so they show on the run's summary page instead of only in the log.
set -o pipefail
log="${RUNNER_TEMP:-/tmp}/gradle-$$.log"
./gradlew "$@" 2>&1 | tee "$log"
status=${PIPESTATUS[0]}

encode() { sed -e 's/%/%25/g' -e 's/\r/%0D/g' | awk 'BEGIN{ORS="%0A"} {print}'; }

grep -E '^(w: |warning: |WARNING: )|[Dd]eprecated|Deprecation' "$log" | sort -u | head -20 |
  while IFS= read -r line; do echo "::warning title=Gradle warning::$(printf '%s' "$line" | encode)"; done

if [ "$status" -ne 0 ]; then
  report=$(awk '/What went wrong/{f=1} f{print} /^\* Try:/{exit}' "$log" | head -60)
  [ -z "$report" ] && report=$(tail -60 "$log")
  echo "::error title=Gradle build failed::$(printf '%s' "$report" | encode)"
  grep -E '(^e: |error: |ERROR: )' "$log" | head -10 |
    while IFS= read -r line; do echo "::error title=Compiler or lint error::$(printf '%s' "$line" | encode)"; done
fi
exit "$status"
