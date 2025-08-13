# QUARKUS | valida-pontos-cartao
<img src="https://img.shields.io/badge/Dev-Caio_Aurelio-informational?style=flat-square&logoColor=white&color=cdcdcd" />

<img src="https://img.shields.io/badge/Docker-information?style=flat-square&logo=docker&logoColor=2496ED&color=cdcdcd" /> <img src="https://img.shields.io/badge/Quarkus-information?style=flat-square&logo=quarkus&logoColor=4695EB&color=cdcdcd" />

Este projeto utiliza o **Quarkus**, o framework Java Supersônico Subatômico.

> Caio Aurélio Cardoso Nunes [LinkedIn][link-linkedin] 
> e-mail: agronomocardoso@gmail.com 
> Telefone: +55 (61) 994-23-5825 


## Sobre o Projeto
 
Aplicação web full stack para gerenciar **pontuação de cartão de crédito**, calculada automaticamente com base no **histórico de compras dos usuários**.
 
Desenvolvida como estudo prático e também como **projeto demonstrativo profissional**, destacando o uso de tecnologias modernas como:

* Java com Quarkus (back-end reativo, leve e rápido)
* Hibernate com Panache (persistência simplificada)
* PostgreSQL (banco de dados relacional)
* Angular (front-end SPA moderno)

 
##  Arquitetura da Aplicação
 
### Back-end (Java Quarkus)

A aplicação segue a arquitetura em camadas:
 
- **Controller / Resource**: Camada de exposição (REST), responde às requisições HTTP e aciona os serviços.
  - Ex: `@Path("/usuarios")`, `@GET`, `@POST`
- **Service**: Contém a lógica de negócio, validações e regras específicas.
- **Repository**: Camada de persistência (usando Panache), realiza acesso direto ao banco via JPA.
- **DTOs**: Objetos de transferência de dados para desacoplar API das entidades.
- **Entities**: Representações JPA das tabelas do banco de dados.
 
### Front-end (Angular)

- Estrutura modular com:
  - **Components**: Interfaces visuais e interação com o usuário
  - **Services**: Comunicação com a API REST (via `HttpClient`)
  - **Models**: Interfaces TypeScript compatíveis com os DTOs do back-end
  - **Rotas protegidas e reativas**, com feedback visual ao usuário
 
 
## Como executar este projecto
 
1.  Clonar
 
```bash
git clone https://github.com/CaioACN/valida-pontos-cartao.git
```
 
2.  Subir o Docker compose

```bash
docker compose up --build -d
```

3. Acesse o Angular: `http://localhost:4200`
 

 
🧠 Considerações Finais
Este projeto é um exemplo completo de aplicação moderna Java + Angular, ideal para estudos, pode ser usado como base para aplicações reais.
 

<!-- links --->
[link-linkedin]:https://www.linkedin.com/in/caio-nunes-dev-java/
 