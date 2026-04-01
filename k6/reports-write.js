import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 10,
  duration: '1m',
};

export default function () {
  const payload = JSON.stringify({
    latitude: 31.7683,
    longitude: 35.2137,
    category: 'DELAY',
    description: `Synthetic report ${__ITER}`,
  });

  const response = http.post(`${__ENV.BASE_URL}/api/v1/reports`, payload, {
    headers: {
      'Content-Type': 'application/json',
      'X-Client-Fingerprint': `k6-${__VU}`,
    },
  });

  check(response, {
    'status is 201': (r) => r.status === 201,
  });
  sleep(1);
}
