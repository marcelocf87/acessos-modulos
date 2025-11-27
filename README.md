# Sistema de Acessos a Módulos

## Descrição do Projeto
Este projeto implementa um sistema para gerenciar usuários, departamentos e módulos, permitindo controle de acesso e relacionamentos entre eles.  
A solução foi construída seguindo boas práticas de arquitetura, testes automatizados e execução via Docker para padronização do ambiente.

---

## Tecnologias Utilizadas

Java 21  
Spring Boot 3.x  
Spring Web  
Spring Data JPA  
PostgreSQL 16  
Docker 20+  
Docker Compose 2.x  
Maven 3.9+  
JUnit 5  
Mockito 5  
JaCoCo 0.8.12  
Swagger/OpenAPI (Springdoc)

---

## Pré-requisitos

Docker Desktop  
Docker Compose  

---

## Como Executar com Docker

Executar o comando:

docker-compose up -d

Containers criados:
- PostgreSQL na porta 5432
- Aplicação Spring Boot na porta 8080

Acessar a API em:
http://localhost:8080

---

## Como executar localmente

Certificar que o PostgreSQL está rodando

Criar o banco de dados:

CREATE DATABASE acessos_modulos;

Ajustar o application.yml se necessário.

Executar o projeto:

mvn spring-boot:run

---

## Executar os Testes

mvn clean test

---

## Gerar o relatório de cobertura (Jacoco)

mvn jacoco:report

O relatório vai ser gerado em:

target/site/jacoco/index.html

Cobertura mínima obrigatória: 80%.

---

## Autenticação

O projeto utiliza autenticação baseada em **JWT (JSON Web Token)**.  
O acesso aos endpoints protegidos é realizado por meio de um token gerado após o login.

### 1. Endpoint de Login

POST /auth/login

### Corpo da requisição (LoginRequestDTO)

{
"email": "joao@supera.com",
"senha": "123456"
}

### Resposta (LoginResponseDTO)

{
"token": "jwt_gerado_pelo_sistema"
}

### 2. Uso do Token

Após o login, o token deve ser enviado no cabeçalho das requisições:

Authorization: Bearer <token>

### 3. Fluxo Resumido

- O usuário envia email e senha para `/auth/login`
- O AuthService valida as credenciais
- Um token JWT é gerado e retornado ao cliente
- As demais rotas protegidas são acessadas enviando o token no cabeçalho Authorization

---

## Exemplos de Requisições

Criar solicitações  

POST /solicitacoes

```json
{
  "moduloId": 3
}
````

Listar solicitações

GET /solicitacoes

---

## Arquitetura da Aplicação

A solução utiliza arquitetura em camadas:

controller → service → repository → entity

Explicação das camadas:
- Controller: recebe requisições HTTP
- Service: aplica regras de negócio
- Repository: abstração de persistência JPA
- Entity/Model: mapeamento das tabelas

Relacionamentos principais:

Departamento 1 -> N Usuários  
Usuário N -> N Módulos

---

## Documentação Swagger

A documentação automática dos endpoints está disponível em:

http://localhost:8080/swagger-ui/index.html