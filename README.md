# 🏋️ Site de Treinos

Aplicação web pessoal para **registrar, gerenciar e visualizar treinos de academia** em um só lugar, com histórico e acompanhamento da evolução.

> Projeto desenvolvido individualmente, seguindo a metodologia ágil **Scrum**.

---

## 📑 Sumário

- [Sobre o projeto](#-sobre-o-projeto)
- [Funcionalidades](#-funcionalidades)
- [Tecnologias](#-tecnologias)
- [Estrutura do projeto](#-estrutura-do-projeto)
- [Modelo de dados](#-modelo-de-dados)
- [Como executar](#-como-executar)
- [Metodologia (Scrum)](#-metodologia-scrum)
- [Roadmap](#-roadmap)
- [Autor](#-autor)

---

## 📖 Sobre o projeto

O **Site de Treinos** nasceu da necessidade de substituir anotações soltas (papel, bloco de notas, planilhas) por uma ferramenta própria, simples e organizada para acompanhar a rotina de academia.

Com ele é possível cadastrar treinos e exercícios, registrar séries, repetições e cargas, e visualizar o histórico para acompanhar a evolução ao longo do tempo.

**Objetivos do projeto:**

- Centralizar o registro dos treinos em uma única aplicação;
- Facilitar a consulta e a edição do histórico;
- Praticar desenvolvimento full stack (front-end, back-end e banco de dados);
- Aplicar Scrum na prática em um projeto individual.

---

## ✨ Funcionalidades

> Ajuste esta lista conforme o que já está implementado no seu backlog.

- [ ] Cadastro de treinos (ex.: Treino A, B, C)
- [ ] Cadastro de exercícios por treino
- [ ] Registro de séries, repetições e carga
- [ ] Edição e exclusão de treinos e exercícios
- [ ] Histórico de treinos realizados
- [ ] Visualização da evolução de carga por exercício
- [ ] Filtros por data, grupo muscular ou exercício

---

## 🛠 Tecnologias

| Camada | Tecnologias |
| --- | --- |
| Front-end | HTML, CSS, JavaScript |
| Back-end | Java |
| Banco de dados | SQL / MySQL |
| Gestão do projeto | Scrum (backlog, sprints) |

---

## 📂 Estrutura do projeto

> Exemplo de organização. Adapte à estrutura real do seu repositório.

```
site-treinos/
├── frontend/
│   ├── index.html
│   ├── css/
│   │   └── style.css
│   └── js/
│       └── main.js
├── backend/
│   └── src/
│       └── ...            # código Java (controllers, services, DAOs/models)
├── database/
│   ├── schema.sql         # criação das tabelas
│   └── seed.sql           # dados de exemplo (opcional)
├── docs/
│   └── backlog.md         # backlog do produto e sprints
└── README.md
```

---

## 🗄 Modelo de dados

> Sugestão inicial de modelagem. Ajuste conforme sua implementação.

```sql
CREATE TABLE treino (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    nome      VARCHAR(100) NOT NULL,
    descricao VARCHAR(255)
);

CREATE TABLE exercicio (
    id             INT AUTO_INCREMENT PRIMARY KEY,
    nome           VARCHAR(100) NOT NULL,
    grupo_muscular VARCHAR(50)
);

CREATE TABLE registro_treino (
    id        INT AUTO_INCREMENT PRIMARY KEY,
    treino_id INT NOT NULL,
    data      DATE NOT NULL,
    FOREIGN KEY (treino_id) REFERENCES treino(id)
);

CREATE TABLE serie (
    id                INT AUTO_INCREMENT PRIMARY KEY,
    registro_treino_id INT NOT NULL,
    exercicio_id      INT NOT NULL,
    repeticoes        INT NOT NULL,
    carga_kg          DECIMAL(6,2),
    FOREIGN KEY (registro_treino_id) REFERENCES registro_treino(id),
    FOREIGN KEY (exercicio_id) REFERENCES exercicio(id)
);
```

---

## 🚀 Como executar

### Pré-requisitos

- [Java JDK](https://adoptium.net/) (versão 17 ou superior recomendada)
- [MySQL](https://dev.mysql.com/downloads/) 8+
- Um navegador atualizado
- Git

### Passo a passo

1. **Clone o repositório**

   ```bash
   git clone https://github.com/SEU-USUARIO/site-treinos.git
   cd site-treinos
   ```

2. **Crie o banco de dados**

   ```bash
   mysql -u root -p -e "CREATE DATABASE site_treinos;"
   mysql -u root -p site_treinos < database/schema.sql
   ```

3. **Configure a conexão com o banco**

   Edite o arquivo de configuração do back-end com suas credenciais:

   ```
   url:      jdbc:mysql://localhost:3306/site_treinos
   usuario:  seu_usuario
   senha:    sua_senha
   ```

4. **Execute o back-end**

   ```bash
   cd backend
   # comando de execução conforme sua ferramenta de build (Maven, Gradle, IDE...)
   ```

5. **Abra o front-end**

   Abra `frontend/index.html` no navegador ou sirva a pasta com um servidor local.

---

## 🔄 Metodologia (Scrum)

O projeto é conduzido com **Scrum adaptado para um desenvolvedor solo**:

- **Product Backlog:** lista priorizada de funcionalidades e melhorias;
- **Sprints:** ciclos curtos com objetivo definido e itens selecionados do backlog;
- **Revisão e retrospectiva:** ao fim de cada sprint, avaliação do que foi entregue e do que pode melhorar.

O backlog e o histórico das sprints ficam em `docs/`.

---

## 🗺 Roadmap

- [x] Definição da stack e da metodologia
- [ ] Construção do Product Backlog
- [ ] Modelagem e criação do banco de dados
- [ ] CRUD de treinos e exercícios
- [ ] Registro de séries, repetições e cargas
- [ ] Histórico e filtros
- [ ] Gráficos de evolução
- [ ] Deploy

---

## 👤 Autor

**Seu Nome**

- GitHub: [@seu-usuario](https://github.com/seu-usuario)
- LinkedIn: [seu-perfil](https://www.linkedin.com/in/seu-perfil)

---

## 📄 Licença

Este projeto é de uso pessoal e educacional. Defina aqui a licença desejada (ex.: MIT).
