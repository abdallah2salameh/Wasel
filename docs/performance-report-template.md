# Performance Report Template

## Scenarios
- Read-heavy: `k6 run -e BASE_URL=http://localhost:8080 k6/incidents-read.js`
- Write-heavy: `k6 run -e BASE_URL=http://localhost:8080 k6/reports-write.js`
- Mixed: `k6 run -e BASE_URL=http://localhost:8080 k6/mixed.js`
- Spike: `k6 run -e BASE_URL=http://localhost:8080 k6/spike.js`
- Soak: `k6 run -e BASE_URL=http://localhost:8080 k6/soak.js`

## Metrics to capture
- Average response time
- p95 latency
- Throughput
- Error rate
- Bottlenecks observed

## Analysis
- Limitations observed
- Root causes
- Optimizations applied
- Before/after comparison
