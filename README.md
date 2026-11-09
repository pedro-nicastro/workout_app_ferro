# FERRO — XAMPP + Java + MariaDB

## Arquitetura

- Apache (XAMPP): HTML, CSS e JavaScript
- MariaDB (XAMPP): database `ferro_db`
- Java + Spring Boot: API REST em `127.0.0.1:8081`
- JDBC: conexão Java -> MariaDB

## 1. Database

No XAMPP, inicie **MySQL** (o seu ambiente mostrou MariaDB 10.4.32 na port 3306).

No MySQL Workbench, execute:

```sql
CREATE DATABASE IF NOT EXISTS ferro_db
  CHARACTER SET utf8mb4
  COLLATE utf8mb4_unicode_ci;
```

Depois execute todo o arquivo:

`database/ferro_database.sql`

Teste:

```sql
USE ferro_db;
SHOW TABLES;
SELECT COUNT(*) FROM usuarios;
```

## 2. Java backend

Requisitos:

- JDK 17 ou superior
- Maven

Na raiz do project existe:

`start-backend.bat`

Ou, dentro de `backend/`:

```bat
mvn spring-boot:run
```

A API deve ficar em:

`http://127.0.0.1:8081`

## 3. Teste pelo navegador

Antes de testar cadastro/login, abra:

`http://127.0.0.1:8081/api/health`

Você deve receber JSON com:

- `api: online`
- `conexaoBanco: ok`
- `database: ferro_db`
- `servidor` contendo a versão do MariaDB/MySQL

Também existe `api-test.html` para um diagnóstico visual.

## 4. Frontend

Copie a folder do project para:

`C:\xampp\htdocs\TrainingWebSite_DB\`

Open:

`http://localhost/TrainingWebSite_DB/`

Não abra os HTML com `file:///`.

## 5. Configuração do database

O padrão já está configurado para o seu XAMPP/MariaDB:

```properties
spring.datasource.url=jdbc:mariadb://127.0.0.1:3306/ferro_db
spring.datasource.username=root
spring.datasource.password=
```

Para alterar sem editar o código, podem ser usadas as variáveis:

- `DB_URL`
- `DB_USER`
- `DB_PASSWORD`
- `SERVER_PORT`

## 6. Cadastro e login

O cadastro envia os dados para:

`POST /api/auth/register`

O login envia os dados para:

`POST /api/auth/login`

As passwords são armazenadas como hash PBKDF2, não em texto puro.

## 7. Planos e exercícios

Já integrados ao database:

- listar planos
- criar plano
- editar plano
- excluir plano
- ativar plano
- gerenciar exercícios

## 8. Automatic load saving during workouts

The workout screen automatically saves each exercise load to MariaDB after a short pause while typing. Saved values are restored when the same workout day is opened again. Completing the workout copies the final values into the permanent workout history and removes that day's temporary drafts.

For an existing database, run this migration once in MySQL Workbench:

`database/workout_load_drafts_migration.sql`

For a fresh database, the table is already included in `database/ferro_database.sql`.

The UI shows `Saving...`, `Saved`, or an error beside each exercise load. A load is considered saved only after the API confirms the database write.

## 9. Se aparecer erro

Teste primeiro:

`http://127.0.0.1:8081/api/health`

Se não abrir, o problema é no backend Java.

Se abrir com `conexaoBanco: erro`, o problema é entre Java e MariaDB.

Se abrir com `conexaoBanco: ok`, então o database e o backend estão funcionando e o próximo ponto a verificar é o frontend.

## 10. Interface

Os estilos do aplicativo ficam consolidados em `assets/styles/ferro.css`. A camada visual mantém o tema escuro verde e é compartilhada pelas telas do produto. A página auxiliar `api-test.html` mantém seus estilos de diagnóstico isolados.
