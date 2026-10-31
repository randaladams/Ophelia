#!/usr/bin/env bash
# Runs the Gradle wrapper and turns Gradle's failure report and any warnings into
# GitHub annotations, so they show on the run's summary page instead of only in the log.
set -o pipefail
log="${RUNNER_TEMP:-/tmp}/gradle-$$.log"
./gradlew "$@" 2>&1 | tee "$log"
status=${PIPESTATUS[0]}

encode() { sed -e 's/%/%25/g' -e 's/\r/%0D/g' | awk 'BEGIN{ORS="%0A"} {print}'; }

grep -E '^(w: |warning: |WARNING: )|[Dd]eprecated|Deprecation' "$log" | grep -v '^\s*at ' | sort -u | head -20 |
  while IFS= read -r line; do echo "::warning title=Gradle warning::$(printf '%s' "$line" | encode)"; done

# With -Dorg.gradle.deprecation.trace=true, Gradle prints where each deprecated call came from.
# Report the first frames outside Gradle itself, which name the plugin or script responsible.
awk '/has been deprecated/{f=1; n=0; next} f && /^\s*at /{ if ($0 !~ /org\.gradle\.|java\.base|jdk\.internal|groovy\.|org\.codehaus/) { print; n++ } if (n>=6) f=0; next } f && !/^\s*at /{f=0}' "$log" | sort -u | head -12 |
  while IFS= read -r line; do echo "::notice title=Deprecated call made from::$(printf '%s' "$line" | encode)"; done

if [ "$status" -ne 0 ]; then
  report=$(awk '/What went wrong/{f=1} f{print} /^\* Try:/{exit}' "$log" | head -60)
  [ -z "$report" ] && report=$(tail -60 "$log")
  echo "::error title=Gradle build failed::$(printf '%s' "$report" | encode)"
  grep -E '(^e: |error: |ERROR: )' "$log" | head -10 |
    while IFS= read -r line; do echo "::error title=Compiler or lint error::$(printf '%s' "$line" | encode)"; done
fi
exit "$status"
