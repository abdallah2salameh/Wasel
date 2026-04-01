import http from 'k6/http';
import { check, sleep } from 'k6';

export const options = {
  vus: 20,
  duration: '1m',
};

export default function () {
  const response = http.get(`${__ENV.BASE_URL}/api/v1/incidents?page=0&size=20`);
  check(response, {
    'status is 200': (r) => r.status === 200,
  });
  sleep(1);
}
