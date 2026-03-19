# 🍦 Ice Candy Lovers - Sistema de Gerenciamento de Geladinhos Gourmet

Sistema completo desenvolvido em **Spring Boot** para gerenciamento de produção e vendas de geladinhos gourmet, com backend robusto e frontend web interativo utilizando Thymeleaf e Bootstrap 5.

---

## 🚀 Tecnologias Utilizadas

* **Java 21** (com preview features)
* **Spring Boot 3.4.3**
* **Spring Data JPA / Hibernate 6**
* **Spring Security 6** (BCrypt, CSRF, Session Management)
* **PostgreSQL 16** (produção) / **H2** (desenvolvimento)
* **Thymeleaf** + **Bootstrap 5**
* **Chart.js 4.4** (gráficos analíticos)
* **Parcel 2.9** (bundler JS) + **SASS** (CSS)
* **Playwright 1.58** (testes E2E)
* **JUnit 5 + Mockito** (testes unitários)
* **JaCoCo 0.8.12** (cobertura de código)
* **Maven** + **frontend-maven-plugin**
* **Docker / Docker Compose**

---

## 📂 Estrutura do Projeto

```
ice-candy-lovers/
│
├── src/main/java/com/icecandylovers/
│   ├── config/                  # SecurityConfig, DevSecurityConfig, AppConfig
│   ├── controllers/             # Controladores REST e MVC (9 controllers)
│   ├── services/                # Lógica de negócio (5 services)
│   ├── repositories/            # Repositórios Spring Data JPA (7 repos)
│   ├── entities/                # Entidades JPA com Lombok (10 entities)
│   ├── dtos/                    # Objetos de transferência de dados
│   └── exceptions/              # Exceções customizadas + GlobalRestExceptionHandler
│
├── src/test/java/com/icecandylovers/
│   ├── services/                # Testes unitários dos services (5 classes)
│   └── controllers/             # Testes unitários dos controllers (8 classes)
│
├── src/main/frontend/
│   ├── src/js/                  # Módulos JavaScript (dashboard, vendas, etc.)
│   ├── scss/                    # Estilos SASS
│   └── tests/e2e/               # Testes E2E com Playwright
│
├── src/main/resources/
│   ├── templates/               # Páginas Thymeleaf
│   ├── static/                  # CSS/JS compilados, imagens
│   └── application*.properties  # Configurações por perfil (dev, docker, prod)
│
├── Dockerfile                   # Build multi-stage (Maven → JRE slim)
├── docker-compose.yml           # Orquestração app + PostgreSQL
└── pom.xml                      # Build Maven com JaCoCo, Surefire, frontend-plugin
```

---

## 🛠 Princípios Aplicados

### **Clean Code**

* Arquitetura em camadas bem definidas (Controller → Service → Repository)
* Controllers apenas delegam para services — sem lógica de negócio
* Tratamento centralizado de exceções via `GlobalRestExceptionHandler`
* Constructor injection com `final` fields em todos os services
* Nomenclatura clara e consistente (português para domínio, inglês para framework)

### **SOLID**

* **S**ingle Responsibility: controllers só tratam HTTP; services só lógica; exceptions só erros
* **O**pen/Closed: novas exceções (`InsufficientStockException`) sem modificar classes existentes
* **L**iskov Substitution: exceções customizadas estendem `RuntimeException` corretamente
* **I**nterface Segregation: repositories com queries específicas por domínio
* **D**ependency Inversion: injeção via construtor, dependendo de interfaces Spring Data

### **Design Patterns**

* **MVC Pattern**: separação clara entre model, view e controller
* **Repository Pattern**: abstração do acesso a dados via Spring Data JPA
* **Service Layer**: centralização da lógica de negócio com `@Transactional`
* **DTO Pattern**: transferência segura de dados (records imutáveis)
* **Strategy Pattern**: canais de venda (`Vendido` enum) com queries especializadas

---

## 🔐 Segurança

* **Autenticação**: Spring Security com form login, BCrypt (strength 10), session management
* **Mass Assignment Protection**: `UserRegistrationDTO` impede injeção de campos como `id` e `role`
* **CSRF**: `CookieCsrfTokenRepository` com SameSite=Lax
* **Profile Isolation**: H2 Console (`/h2-console`) acessível **apenas** no perfil `dev` via `DevSecurityConfig`
* **Cache Bounded**: Chat LRU limitado a 500 entradas para prevenir DoS de memória
* **Error Sanitization**: mensagens de erro genéricas para o cliente, detalhes apenas no log
* **Endpoints públicos**: `/`, `/login`, `/register`, `/css/**`, `/js/**`, `/api/chat/**`

---

## ✨ Funcionalidades Principais

### 📦 Gerenciamento de Produtos
* CRUD completo de geladinhos gourmet com categorias (`CategoriaProduto`)
* Composição dinâmica de ingredientes por produto
* Controle de estoque (inicial, atual) com validação de estoque insuficiente
* Cálculo automático de preço de custo unitário
* Produção com consumo FIFO de ingredientes

### 🧂 Gerenciamento de Ingredientes
* CRUD com lotes rastreáveis (custo por unidade, data de compra)
* **Custo médio ponderado**: recalculado automaticamente a cada novo lote
* **Consumo FIFO**: lotes mais antigos consumidos primeiro na produção
* Atualização de estoque com validação de saldo

### 💰 Controle de Vendas
* Registro de vendas por canal (Praia, Estabelecimento Parceiro, Evento)
* Cálculo automático de totais (quantidade × valor unitário)
* Decremento automático do estoque do produto
* Reversão de estoque ao editar/deletar vendas

### 📊 Relatórios Analíticos
* Análise de vendas com filtros por período e canal
* KPIs: ticket médio, margem de lucro bruto, taxa de conversão
* Gráficos por período, canal, forma de pagamento (Chart.js)
* Status de estoque (em estoque, baixo, sem estoque)

### 💬 Chat Interativo (Gelyto)
* Assistente virtual com integração OpenAI (GPT-4o-mini) opcional
* Respostas template quando AI desabilitada
* Cache LRU (máx. 500) para respostas frequentes

### 🌐 Página Pública
* Catálogo responsivo com Bootstrap 5
* Seções informativas e depoimentos

---

## 🧪 Testes e Qualidade

### Cobertura de Código (JaCoCo)

| Métrica | Cobertura |
|---|---|
| **Instruções** | **84.2%** |
| **Linhas** | **88.3%** |
| **Métodos** | **73.8%** |
| **Branches** | **66.8%** |

### Cobertura por Classe (Services + Controllers)

| Classe | Cobertura |
|---|---|
| UserService | 100% |
| AuthController | 100% |
| DashboardController | 100% |
| IndexController | 100% |
| RelatorioController | 100% |
| IngredienteController | 98.4% |
| IngredienteService | 97.1% |
| VendaService | 93.9% |
| RelatorioService | 89.3% |
| ProdutoService | 88.3% |
| VendaController | 83.3% |

### Testes Unitários (155 testes)

| Classe de Teste | Testes | Cobre |
|---|---|---|
| `IngredienteServiceTest` | 33 | FIFO, custo médio ponderado, lotes, CRUD, validações |
| `ProdutoServiceTest` | 27 | CRUD, estoque, produção, custo, ingredientes, duplicatas |
| `VendaServiceTest` | 18 | Registro, atualização, deleção, reversão de estoque |
| `RelatorioServiceTest` | 12 | Relatório completo, KPIs, métricas, canais |
| `UserServiceTest` | 5 | Registro, duplicata, loadUserByUsername |
| `ProdutoControllerTest` | 20 | REST endpoints, erros, categorias |
| `VendaControllerTest` | 12 | CRUD REST, formulários, erros |
| `IngredienteControllerTest` | 10 | CRUD REST, lotes, estoque |
| `GlobalRestExceptionHandlerTest` | 6 | Todos os 6 handlers de exceção |
| `AuthControllerTest` | 5 | Register, login, validação, duplicata |
| `RelatorioControllerTest` | 4 | View, JSON API, erros |
| `DashboardControllerTest` | 2 | Dashboard OK, erro |
| `IndexControllerTest` | 1 | Página index |

### Testes E2E (Playwright - 5 testes)

| Teste | Fluxo |
|---|---|
| Auth Setup | Registro → Login → Session salva |
| Dashboard | Carrega sem erros, exibe produtos |
| Ingredientes | Criar → Editar → Adicionar Lote |
| Produtos | Criar com ingrediente → Editar estoque |
| Vendas + Relatórios | Registrar venda → Verificar relatório |

### Executar Testes

```bash
# Testes unitários com cobertura
mvn test

# Relatório de cobertura (abrir target/site/jacoco/index.html)
mvn test jacoco:report

# Testes E2E (requer app rodando em localhost:8080)
cd src/main/frontend
npx playwright test                    # headless
npx playwright test --headed           # navegador visível
```

---

## 🚀 Como Executar

### Pré-requisitos
* JDK 21 instalado
* PostgreSQL 13+ (ou use H2 com perfil dev)
* Maven 3.6+

### Desenvolvimento Local (H2)

```bash
git clone https://github.com/GilRossi/ice-candy-lovers.git
cd ice-candy-lovers
mvn clean install
mvn spring-boot:run
```

* Área pública: http://localhost:8080/
* Login: http://localhost:8080/login
* H2 Console: http://localhost:8080/h2-console

### Execução com Docker (PostgreSQL)

```bash
cp .env.example .env
# Edite .env com credenciais reais
docker compose up --build
```

* Aplicação: http://localhost:8080/
* PostgreSQL: `localhost:5432`

### Perfis Spring

| Perfil | Banco | DDL | Uso |
|---|---|---|---|
| `dev` (padrão) | H2 in-memory | auto update | Desenvolvimento local |
| `docker` | PostgreSQL via rede Docker | auto update | Docker Compose |
| `prod` | PostgreSQL com env vars | validate | Produção |

---

## 📋 Fluxos Principais

### Fluxo de Vendas
```
Cliente → Catálogo → Seleção → Chat → Venda → Estoque atualizado → Relatório
```

### Fluxo Administrativo
```
Login → Dashboard → Gerenciamento → (Produtos/Vendas/Relatórios) → Logout
```

### Fluxo de Produção
```
Cadastro produto → Ingredientes → Produção (FIFO) → Estoque → Venda → Análise
```

---

## 🚢 Deploy

GitHub Actions workflow (`.github/workflows/deploy.yml`) executa build + deploy automático para EC2 via SSH no push para `main`.

Docker Compose usa Dockerfile multi-stage (Maven build → JRE slim, usuário não-root `spring`).

---

## 📚 Próximos Passos

### Melhorias Planejadas
* Testes do ChatController (extrair ChatService)
* Role-based access control (`@PreAuthorize` ADMIN/USER)
* Rate limiting no endpoint `/api/chat/**`
* Dashboard com mais métricas e KPIs
* Sistema de promoções e descontos
* Integração com gateways de pagamento

### Aplicativo Mobile (Proposta)
* Desenvolvimento nativo Android com Kotlin
* Sync offline com Room database
* Notificações push para novas promoções
* Scanner de código de barras para vendas
* Geolocalização para entregas

---

## 👨‍💻 Autor

**Gil Rossi Aguiar**
📧 [gilrossi.aguiar@live.com](mailto:gilrossi.aguiar@live.com)
💼 [LinkedIn](https://www.linkedin.com/in/gil-rossi-5814659b/)
🐙 [GitHub](https://github.com/GilRossi)

---

## 📄 Licença

© 2025 Ice Candy Lovers. Todos os direitos reservados.
