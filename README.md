# code-with-quarkus

Este projeto utiliza o **Quarkus**, o framework Java Supersônico Subatômico.

## 💳 Sobre o Projeto

Aplicação web full stack para gerenciar **pontuação de cartão de crédito**, calculada automaticamente com base no **histórico de compras dos usuários**.

Desenvolvida como estudo prático e também como **projeto demonstrativo profissional**, destacando o uso de tecnologias modernas como:
- Java com Quarkus (back-end reativo, leve e rápido)
- Hibernate com Panache (persistência simplificada)
- PostgreSQL (banco de dados relacional)
- Angular (front-end SPA moderno)

**Desenvolvedor:** [Caio Aurélio Cardoso Nunes](https://www.linkedin.com/in/caio-nunes-dev-java/)  
📧 Email: agronomocardoso@gmail.com  
📱 Telefone: (61) 994-23-5825

---

## 🧱 Arquitetura da Aplicação

### 🔹 Back-end (Java Quarkus)
A aplicação segue a arquitetura em camadas:

- **Controller / Resource**: Camada de exposição (REST), responde às requisições HTTP e aciona os serviços.
  - Ex: `@Path("/usuarios")`, `@GET`, `@POST`
- **Service**: Contém a lógica de negócio, validações e regras específicas.
- **Repository**: Camada de persistência (usando Panache), realiza acesso direto ao banco via JPA.
- **DTOs**: Objetos de transferência de dados para desacoplar API das entidades.
- **Entities**: Representações JPA das tabelas do banco de dados.

### 🔸 Front-end (Angular)
- Estrutura modular com:
  - **Components**: Interfaces visuais e interação com o usuário
  - **Services**: Comunicação com a API REST (via `HttpClient`)
  - **Models**: Interfaces TypeScript compatíveis com os DTOs do back-end
  - **Rotas protegidas e reativas**, com feedback visual ao usuário

---

## ▶️ Como executar em modo de desenvolvimento

### Back-end

Comando para rodar o projeto com hot reload:

```bash
./mvnw quarkus:dev
🔗 Acesse o Dev UI do Quarkus (somente em modo dev):
http://localhost:8080/q/dev/

Front-end
Dentro do diretório do projeto Angular:

bash
Copiar
Editar
npm install
ng serve
🔗 Acesse o Angular: http://localhost:4200

📦 Empacotando a aplicação
Gerar o JAR padrão:
bash
Copiar
Editar
./mvnw package
Arquivo gerado: target/quarkus-app/quarkus-run.jar

Executar o JAR:
bash
Copiar
Editar
java -jar target/quarkus-app/quarkus-run.jar
Gerar um über-jar (com dependências):
bash
Copiar
Editar
./mvnw package -Dquarkus.package.jar.type=uber-jar
java -jar target/*-runner.jar
🧊 Criando um executável nativo
Requer GraalVM instalado localmente ou uso de container:

bash
Copiar
Editar
./mvnw package -Dnative
Ou com container (sem GraalVM local):

bash
Copiar
Editar
./mvnw package -Dnative -Dquarkus.native.container-build=true
Executável gerado:

bash
Copiar
Editar
./target/code-with-quarkus-1.0.0-SNAPSHOT-runner
📘 Guia completo: Maven Tooling

📚 Extensões Utilizadas
Quarkus REST
Guia: REST Reactive

Jackson (JSON)
Guia: REST Jackson

Hibernate ORM com Panache
Guia: Panache

PostgreSQL JDBC
Guia: PostgreSQL

✅ Código Inicial Gerado (Quarkus)
Hibernate ORM
Exemplo de entidade JPA:

java
Copiar
Editar
@Entity
public class Usuario extends PanacheEntity {
    public String nome;
    public String email;
    public Integer pontuacao;
}
REST
Exemplo de endpoint REST:

java
Copiar
Editar
@Path("/usuarios")
public class UsuarioResource {

    @GET
    @Produces(MediaType.APPLICATION_JSON)
    public List<Usuario> listarTodos() {
        return Usuario.listAll();
    }
}
🎯 Boas Práticas Adotadas
Separação clara entre entidades (JPA) e DTOs (API)

Utilização de Response e Status nos endpoints

Validações com javax.validation

Documentação REST com OpenAPI (Swagger)

Angular desacoplado, consumindo apenas endpoints REST

Nomenclatura consistente para entidades, controllers e serviços

Banco relacional modelado com integridade referencial

Deploy local, em container ou como binário nativo

🚀 Comandos úteis (Angular)
Dentro do projeto Angular:

Ação	Comando
Instalar dependências	npm install
Rodar em modo dev	ng serve
Rodar testes unitários	ng test
Build para produção	ng build --prod

🧠 Considerações Finais
Este projeto é um exemplo completo de aplicação moderna Java + Angular, ideal para estudos, entrevistas técnicas, ou como base para aplicações reais.