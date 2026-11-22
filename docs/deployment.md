# Deployment

Production runs on one VM with Docker: Caddy (HTTPS) → nginx (SPA + `/api` proxy) → Spring Boot → PostgreSQL / Redis.
Secrets live only in `~/pocketlens/.env` on the server. After every successful pipeline on `main`, the `deploy` job
connects over SSH, pulls the latest commit and rebuilds the containers (`SSH_HOST`, `SSH_USER`, `SSH_KEY` repository
secrets).

Manual deployment / first start:

```bash
git clone https://github.com/Matvey-N/pocketlens.git && cd pocketlens
cp .env.example .env   # strong secrets, COOKIE_SECURE=true, DOMAIN=<ip-with-dashes>.sslip.io
docker compose -f docker-compose.yml -f docker-compose.prod.yml up -d --build
```

Health: `https://$DOMAIN/actuator/health` is not exposed publicly; check with
`docker compose ps` and `docker compose logs backend`.
