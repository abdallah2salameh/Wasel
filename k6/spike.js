import http from 'k6/http';
import { check } from 'k6';

export const options = {
  stages: [
    { duration: '15s', target: 10 },
    { duration: '15s', target: 100 },
    { duration: '30s', target: 100 },
    { duration: '15s', target: 10 },
  ],
};

export default function () {
  const response = http.get(`${__ENV.BASE_URL}/api/v1/incidents?page=0&size=20`);
  check(response, {
    'status is 200': (r) => r.status === 200,
  });
}
