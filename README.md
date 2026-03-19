# 🍦 Ice Candy Lovers - Sistema de Gerenciamento de Geladinhos Gourmet

Sistema completo desenvolvido em **Spring Boot** para gerenciamento de produção e vendas de geladinhos gourmet, com backend robusto e frontend web interativo utilizando Thymeleaf e Bootstrap 5.

---

## 🚀 Tecnologias Utilizadas

* **Java 21**
* **Spring Boot 3.4.3**
* **Spring Data JPA**
* **Spring Security**
* **PostgreSQL**
* **Thymeleaf**
* **Bootstrap 5**
* **Chart.js**
* **Maven**

---

## 📂 Estrutura do Projeto

```
ice-candy-lovers/
│
├── src/main/java/com/icecandylovers/
│   ├── entities/                 # Entidades JPA (User, Produto, Venda, etc.)
│   ├── repositories/             # Repositórios Spring Data JPA
│   ├── services/                 # Lógica de negócio
│   ├── dtos/                    # Objetos de transferência de dados
│   └── controllers/             # Controladores REST e MVC
│
├── src/main/resources/
│   ├── static/                  # Arquivos estáticos
│   │   ├── css/                 # Estilos personalizados
│   │   ├── js/                  # Scripts JavaScript
│   │   └── img/                 # Imagens e recursos visuais
│   ├── templates/               # Páginas Thymeleaf
│   └── application.properties   # Configurações da aplicação
│
└── pom.xml                      # Configuração Maven
```

---

## 🛠 Princípios Aplicados

### **Clean Code**

* Arquitetura em camadas bem definidas
* Métodos com responsabilidades únicas
* Nomenclatura clara e consistente
* Separação entre lógica de negócio e apresentação

### **SOLID**

* **S**ingle Responsibility: cada classe com função específica
* **O**pen/Closed: fácil extensão para novas funcionalidades
* **L**iskov Substitution: consistência nas interfaces
* **I**nterface Segregation: interfaces específicas por funcionalidade
* **D**ependency Inversion: injeção de dependências com Spring

### **Design Patterns**

* **MVC Pattern**: separação clara entre model, view e controller
* **Repository Pattern**: abstração do acesso a dados
* **Service Layer**: centralização da lógica de negócio
* **DTO Pattern**: transferência eficiente de dados
* **Observer Pattern**: tratamento de eventos do sistema

---

## ✨ Funcionalidades Principais

### 🔐 Autenticação e Segurança
* Sistema de login e cadastro com Spring Security
* Senhas criptografadas com BCrypt
* Controle de acesso por roles (admin/cliente)

### 📦 Gerenciamento de Produtos
* Cadastro e edição de geladinhos gourmet
* Controle dinâmico de ingredientes
* Gestão completa de estoque

### 💰 Controle de Vendas
* Registro de vendas por diferentes canais
* Cálculo automático de totais
* Decremento automático do estoque

### 📊 Relatórios Analíticos
* Análise de vendas com filtros personalizáveis
* Métricas de desempenho (ticket médio, margem de lucro)
* Visualização com gráficos Chart.js

### 💬 Chat Interativo
* Assistente virtual Gelyto para atendimento
* Integração com frontend para comunicação em tempo real

### 🌐 Página Pública
* Catálogo de produtos para clientes
* Design responsivo com Bootstrap 5
* Seções informativas e depoimentos

---

## 🚀 Como Executar

### Pré-requisitos
* JDK 21 instalado
* PostgreSQL 13+
* Maven 3.6+

### Configuração

1. **Clonar o repositório**
```bash
git clone https://github.com/GilRossi/ice-candy-lovers.git
cd ice-candy-lovers
```

2. **Configurar banco de dados**
```bash
# Criar database no PostgreSQL
createdb ice_candy_lovers
```

3. **Configurar application.properties**
```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/ice_candy_lovers
spring.datasource.username=seu_usuario
spring.datasource.password=sua_senha
spring.jpa.hibernate.ddl-auto=update
```

4. **Executar a aplicação**
```bash
mvn clean install
mvn spring-boot:run
```

5. **Acessar o sistema**
* Área pública: http://localhost:8080/
* Área administrativa: http://localhost:8080/login

### Execução com Docker

1. **Preparar variáveis de ambiente**
```bash
cp .env.example .env
```

Edite o arquivo `.env` antes de subir os containers.
Defina credenciais reais para `POSTGRES_USER`, `POSTGRES_PASSWORD`, `DB_USERNAME` e `DB_PASSWORD`.

2. **Subir aplicação e PostgreSQL**
```bash
docker compose up --build
```

3. **Acessar o sistema**
* Aplicação: http://localhost:8080/
* Login: http://localhost:8080/login
* Banco PostgreSQL: `localhost:5432`

Observações:
* O perfil `docker` é voltado para ambiente local e usa `spring.jpa.hibernate.ddl-auto=update`.
* O perfil `prod` continua exigindo variáveis de ambiente e schema já validado.

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
Cadastro produto → Ingredientes → Produção → Estoque → Venda → Análise
```

---

## 🧪 Testes e Validações

* Validação de entradas em formulários
* Tratamento de exceções personalizado
* Prevenção de divisão por zero em cálculos
* Verificação de estoque insuficiente
* Autenticação e autorização seguras

---

## 📚 Próximos Passos

### Melhorias Web
* Implementação de testes unitários e de integração
* Dashboard com mais métricas e KPIs
* Sistema de promoções e descontos
* Integração com gateways de pagamento
* API RESTful para integração com outros sistemas

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
