# Spring Google OAuth2 + JWT (Example)

This project is an example Spring Boot application that:
- Uses Google OAuth2 login
- Issues Access JWTs and stores Refresh Tokens in MySQL
- Supports refresh token endpoint and logout (invalidates refresh token)
- Comes with Dockerfile and docker-compose.yml

## Configurar credenciales de google

Google Cloud Console

`https://console.cloud.google.com/apis/credentials`

Crea un OAuth2 Client ID:

Tipo: Aplicación Web

Redirect URI:
```
http://localhost:8080/login/oauth2/code/google
```

Obtendrás:

clientId

clientSecret

## Setup

1. Copy `.env.example` to `.env` and fill:
   ```
   cp .env.example .env
   ```

2. Provide Google OAuth2 credentials in Google Cloud Console with redirect URI:
   `http://localhost:8080/login/oauth2/code/google`

3. Build & run with Docker Compose:
   ```
   docker compose up --build
   ```

4. Access `http://localhost:8080/` and hit a protected endpoint to start OAuth2 flow.

## Test

# Probar en navegador

http://localhost:8080/oauth2/authorization/google

GET
http://localhost:8080/api/user/me

# Probar en postman

POST
http://localhost:8080/auth/refresh

BODY
{
    "refreshToken":"b9793139-f070-4778-81e2-5ebe43ed68cb"
}

POST
http://localhost:8080/auth/logout

BODY
{
    "refreshToken":"b9793139-f070-4778-81e2-5ebe43ed68cb"
}