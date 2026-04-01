import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  stages: [
    { duration: '30s', target: 15 },
    { duration: '1m', target: 15 },
    { duration: '30s', target: 0 },
  ],
};

export default function () {
  const read = http.get(`${__ENV.BASE_URL}/api/v1/checkpoints?page=0&size=10`);
  check(read, { 'read ok': (r) => r.status === 200 });

  const payload = JSON.stringify({
    latitude: 31.9,
    longitude: 35.1,
    category: 'ACCIDENT',
    description: `Mixed workload report ${__ITER}`,
  });

  const write = http.post(`${__ENV.BASE_URL}/api/v1/reports`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'X-Client-Fingerprint': `mixed-${__VU}`,
    },
  });
  check(write, { 'write accepted': (r) => r.status === 201 });
  sleep(1);
}
