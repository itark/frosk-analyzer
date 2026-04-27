#!/bin/bash
# Start frosk-analyzer Spring Boot application
cd /Users/fredrikmoller/itark/git/frosk-analyzer

# Load SDKMAN and activate project SDK
export SDKMAN_DIR="$HOME/.sdkman"
[[ -s "$HOME/.sdkman/bin/sdkman-init.sh" ]] && source "$HOME/.sdkman/bin/sdkman-init.sh"
sdk env

# Start the application
mvn spring-boot:run
