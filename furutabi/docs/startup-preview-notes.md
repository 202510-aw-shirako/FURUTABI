# Startup Preview Notes

This note covers the current minimum backend preview setup.

## Scope

- `PreviewController` exposes `GET /preview/login`
- `templates/auth/login.html` is a preview connection of the existing wireframe
- `static/css/style.css` and `static/assets/scripts/shared-ui.js` are served by Spring Boot
- `docs/` is only for GitHub Pages static publishing and is not the backend source of truth

## Run

From `furutabi/`:

```powershell
.\mvnw.cmd test
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev"
```

If port `8080` is already in use on the machine, run on another port:

```powershell
.\mvnw.cmd spring-boot:run -Dspring-boot.run.arguments="--spring.profiles.active=dev --server.port=8081"
```

## Preview URLs

- `http://localhost:8080/preview/login`
- `http://localhost:8080/preview/auth/login.html`
- `http://localhost:8080/preview/public/bridge.html`
- `http://localhost:8080/preview/public/index.html`
- `http://localhost:8080/css/style.css`
- `http://localhost:8080/assets/scripts/shared-ui.js`
- `http://localhost:8080/h2-console`

When using another port, replace `8080` with that port.

## What This Does Not Mean

- `/preview/login` is not the real `/login`
- `returnTo` is not implemented on the backend
- role, visibility, proposal, okatte, and chat flows are not implemented here
