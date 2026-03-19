# CLAUDE.md

This file provides guidance to Claude Code (claude.ai/code) when working with code in this repository.

## Project Overview

Ice Candy Lovers is a full-stack inventory and sales management system for gourmet ice cream products. Built with Spring Boot 3.4.3 (Java 21) backend, Thymeleaf + Bootstrap 5 frontend, and PostgreSQL database.

## Build & Run Commands

### Full build (backend + frontend)
```bash
mvn clean install
```

### Run locally (dev profile with H2 in-memory DB)
```bash
mvn spring-boot:run
```

### Run with Docker (PostgreSQL)
```bash
cp .env.example .env   # configure credentials first
docker compose up --build
```

### Frontend only (from `src/main/frontend/`)
```bash
npm run build          # compile SCSS + bundle JS with Parcel
npm run watch          # watch mode for development
```

### E2E tests (requires running app on port 8080)
```bash
cd src/main/frontend
npm run test:e2e                    # headless Playwright
npm run test:e2e:headed             # headed mode
npx playwright test tests/e2e/app.spec.js   # single test file
```

### Java tests
```bash
mvn test
```

## Architecture

### Layered backend structure
```
Controllers (MVC + REST) -> Services -> Repositories -> JPA Entities -> PostgreSQL
```

All backend code lives under `src/main/java/com/icecandylovers/`:
- `config/` - SecurityConfig (Spring Security, CORS, CSRF), AppConfig (RestTemplate)
- `controllers/` - MVC controllers (Thymeleaf pages) and REST API controllers
- `services/` - Business logic with `@Transactional` management
- `repositories/` - Spring Data JPA interfaces
- `entities/` - JPA entities with Lombok annotations
- `dtos/` - Request/response transfer objects
- `exceptions/` - Custom exceptions + `GlobalRestExceptionHandler`

### Frontend build pipeline
Frontend source is in `src/main/frontend/`. The Maven build automatically:
1. Installs Node v18.17.1 + npm 9.6.7 (via `frontend-maven-plugin`)
2. Runs `npm install` and `npm run build`
3. Outputs compiled CSS/JS to `src/main/resources/static/`

Frontend stack: SCSS -> sass -> CSS, JS -> Parcel bundler -> bundled JS, Chart.js for analytics.

### Key domain model
- **Produto** (Product) has stock tracking, cost, and category (CategoriaProduto enum)
- **Ingrediente** -> **LoteIngrediente** (batches with cost per unit)
- **ProdutoIngrediente** links products to ingredient batches with quantities
- **Venda** (Sale) -> **VendaItem** line items; sales decrement product stock
- **Vendido** enum tracks sales channels (PRAIA, LOJA, DELIVERY, etc.)

### Spring profiles
- `dev` (default) - H2 in-memory DB, verbose logging, H2 Console at `/h2-console`
- `docker` - PostgreSQL via Docker network, DDL auto-update
- `prod` - PostgreSQL with env vars, DDL validate-only, Thymeleaf caching on

### Security
- Spring Security with form login, BCrypt passwords, session management
- Public endpoints: `/`, `/login`, `/register`, `/css/**`, `/js/**`, `/api/chat/**`
- All other endpoints require authentication
- CSRF via `CookieCsrfTokenRepository`

### AI chat feature (Gelyto)
Optional OpenAI integration (GPT-4o-mini) toggled via `CHAT_OPENAI_ENABLED` env var. Has template-based fallback responses when disabled.

## E2E Test Details

Tests use Playwright in serial mode (data-dependent). Auth setup (`auth.setup.js`) runs first, stores session to `playwright/.auth/user.json`. Tests cover dashboard, ingredient CRUD, product management, and sales flow. Base URL defaults to `http://127.0.0.1:8080` (override with `PLAYWRIGHT_BASE_URL`).

## Deployment

GitHub Actions workflow (`.github/workflows/deploy.yml`) builds the JAR and deploys to EC2 via SSH on push to `main`. Docker Compose orchestration uses multi-stage Dockerfile (Maven build -> slim JRE, non-root `spring` user).

## Language

The codebase uses Portuguese for domain terms (Produto, Venda, Ingrediente, Lote, etc.), UI text, and most comments. Code structure and framework conventions follow English patterns.
