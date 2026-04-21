# Wasel Frontend

Next.js App Router frontend for the Wasel Palestine backend.

## Run locally

1. Start the backend on `http://localhost:8080`
2. Copy `.env.example` to `.env.local`
3. Install dependencies
4. Start the frontend

```bash
npm install
npm run dev
```

The app will run on `http://localhost:3000`.

## Environment

- `NEXT_PUBLIC_API_BASE_URL=http://localhost:8080`

## Main pages

- `/` overview dashboard
- `/incidents` incident browser
- `/checkpoints` checkpoint registry
- `/routes` route estimation workspace
- `/reports` public report submission + moderator queue
- `/alerts` alert subscription management
- `/auth` login and registration
