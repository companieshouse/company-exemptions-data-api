#!/bin/bash
#
# Start script for company-exemptions-data-api

PORT=8080
exec java -jar -Dserver.port="${PORT}" "company-exemptions-data-api.jar"
