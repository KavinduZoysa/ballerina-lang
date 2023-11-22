#!/bin/bash

# Number of times to run the command
num_runs=10

# Initialize sum variable
sum=0

# Run the command multiple times
for ((i=1; i<=$num_runs; i++)); do
    output=$(./gradlew :language-server:language-server-core:test --tests "org.ballerinalang.langserver.codeaction.CodeActionPerformanceTest")  # Replace "your_command_here" with your actual command
    response_time=$(echo "$output" | grep -oP 'actualResponseTime: \K\d+')
    expected_response_time=$(echo "$output" | grep -oP 'expectedResponseTime: \K\d+')
    
    if [[ -n $response_time ]]; then
        sum=$((sum + response_time))
        echo "Run $i - actualResponseTime: $response_time"
    else
        echo "Run $i - actualResponseTime not found in output"
    fi
    echo "expectedResponseTime: $expected_response_time"
done

# Calculate the average
average=$((sum / num_runs))

echo "Average actualResponseTime: $average"
