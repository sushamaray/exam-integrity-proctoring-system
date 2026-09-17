# Setting up on a new machine

Everything needed to go from a bare computer to a running app. Follow it in
order; nothing here is optional unless it says so.

Repository: <https://github.com/neel2214/AOOP.git>

---

## What you are installing, and why

| Tool | Version to install | Why |
|---|---|---|
| **JDK 21** | 21 (not 17, not 25) | `pom.xml` sets `<java.version>21</java.version>`. Verified on 21.0.9 |
| **Node.js** | 22.12+ or 24.x | Vite 8 requires `^20.19.0 \|\| >=22.12.0`; ESLint 10 requires `>=24` for the v24 line. Verified on 24.14.0 |
| **PostgreSQL** | 17 | Verified on 17.10. 14+ should work |
| **Git** | any recent | To clone |

**You do NOT need to install Maven.** The repo ships the Maven wrapper
(`mvnw` / `mvnw.cmd`), which downloads Maven 3.9.16 by itself on first run.

---

## 1. Install the JDK

**Windows**

```powershell
winget install EclipseAdoptium.Temurin.21.JDK
```

Or download the `.msi` from <https://adoptium.net/temurin/releases/?version=21>.

**macOS**

```bash
brew install --cask temurin@21
```

**Linux (Debian/Ubuntu)**

```bash
sudo apt install openjdk-21-jdk
```

**Verify — close and reopen your terminal first**, or the PATH change won't apply:

```
java -version
```

Must print `version "21.x"`. If it prints 17 or 24, you have another JDK ahead of
it on PATH — fix that before continuing, or the build fails with a confusing
class-version error.

---

## 2. Install Node.js

**Windows**

```powershell
winget install OpenJS.NodeJS.LTS
```

**macOS**

```bash
brew install node
```

**Verify:**

```
node -v
npm -v
```

`node -v` must be 22.12 or higher. Node 20 or below will fail on `npm run dev`.

---

## 3. Install PostgreSQL

**Windows** — download the installer from
<https://www.postgresql.org/download/windows/> and run it.

During the installer, two things matter:

1. **The password you set for the `postgres` superuser.** Write it down. The repo
   is configured for `password` — if you use that, step 5 needs no changes.
2. **Tick "Command Line Tools"** so `psql` is installed.

**macOS**

```bash
brew install postgresql@17
brew services start postgresql@17
```

**Verify the service is running:**

```powershell
# Windows
Get-Service postgresql*        # Status must be "Running"
```

```bash
# macOS / Linux
pg_isready
```

If `psql` is "not recognised" on Windows, add it to PATH or use the full path:
`"C:\Program Files\PostgreSQL\17\bin\psql.exe"`.

---

## 4. Clone the repository

```bash
git clone https://github.com/neel2214/AOOP.git
cd AOOP
```

You should see three folders: `backend/`, `frontend/`, and `.github/`.

---

## 5. Create the database

The app connects to a database named **`examdb`** on `localhost:5432` as user
**`postgres`**. Create it:

```bash
psql -U postgres -c "CREATE DATABASE examdb;"
```

It will prompt for the postgres password from step 3.

**Do not create any tables.** Flyway does that in step 7. Creating them by hand
will make Flyway fail.

**Verify:**

```bash
psql -U postgres -l
```

`examdb` must appear in the list.

> Prefer a GUI? In pgAdmin: right-click **Databases → Create → Database**, name it
> `examdb`, leave everything else default.

---

## 6. Point the app at your database password

Open `backend/src/main/resources/application-local.properties`:

```properties
spring.datasource.password=password
```

**If your postgres password is `password`, change nothing.** Otherwise put yours
here.

The connection URL and username live in `application.properties` and only need
editing if PostgreSQL is not on port 5432 or your user is not `postgres`:

```properties
spring.datasource.url=jdbc:postgresql://localhost:5432/examdb
spring.datasource.username=postgres
```

> ⚠️ **Note for the team:** `application-local.properties` is currently committed
> to the repo, password and all. That is survivable for a throwaway local
> database, but it is a bad habit and it must never hold a real credential. If we
> ever point this at a shared server, add the file to `.gitignore` and use an
> environment variable instead.

---

## 7. Start the backend

```powershell
cd backend
.\mvnw.cmd spring-boot:run     # Windows PowerShell
```

```bash
cd backend
./mvnw spring-boot:run         # macOS / Linux / Git Bash
```

**The first run takes several minutes and needs internet** — it downloads Maven
3.9.16 and every dependency into `~/.m2`. Later runs take seconds.

**What happens automatically, so you never run SQL by hand:**

1. **Flyway** applies `db/migration/V1__create_users_table.sql`, creating the
   `users` table.
2. **`DemoDataSeeder`** inserts three demo accounts — but *only if the table is
   empty*, so restarting never duplicates them.

**Success looks like:**

```
 :: Spring Boot ::                (v4.1.1)
...
o.f.core.internal.command.DbMigrate  : Migrating schema "public" to version "1 - create users table"
o.s.boot.tomcat.TomcatWebServer      : Tomcat started on port 8080 (http)
c.uiu.examintegrity.BackendApplication : Started BackendApplication in 3.6 seconds
```

**Verify the API:**

```bash
curl http://localhost:8080/api/ping
# -> backend alive
```

**Verify the migration and seed actually landed:**

```bash
psql -U postgres -d examdb -c "SELECT email, role FROM users;"
```

Three rows. If you get zero rows or "relation does not exist", the backend did
not start cleanly — scroll up in its output for the real error.

Leave this terminal running.

---

## 8. Start the frontend

**In a second terminal:**

```bash
cd frontend
npm ci
npm run dev
```

Use `npm ci`, not `npm install` — it installs the exact versions in
`package-lock.json`, so everyone on the team gets an identical tree.

**Success looks like:**

```
  VITE v8.2.2  ready in 324 ms
  ➜  Local:   http://localhost:5173/
```

If it says `Port 5173 is in use, trying another one...` and gives you 5174,
something else is already running — see troubleshooting.

---

## 9. Sign in

Open <http://localhost:5173>. Click any demo account row to fill the form, or
type one in:

| Email | Password | Lands on |
|---|---|---|
| `student@uiu.ac.bd` | `password123` | `/student` |
| `faculty@uiu.ac.bd` | `password123` | `/faculty` |
| `invigilator@uiu.ac.bd` | `password123` | `/invigilator` |

**Then refresh the page.** You should stay signed in — that proves the session
cookie and `/api/auth/me` are both working, which is the real end-to-end check.

---

## 10. Run the tests

```powershell
cd backend
.\mvnw.cmd test
```

Expect `Tests run: 5, Failures: 0, Errors: 0`.

These are integration tests: **PostgreSQL must be running**, because they hit the
real `examdb`. They are transactional and roll back, so they leave no rows behind.
The backend itself does not need to be running (they use MockMvc, not the port).

---

## How the two halves fit together

```
browser ──> http://localhost:5173  (Vite dev server, React)
                    │
                    │  requests to /api/*  are proxied by Vite
                    ▼
             http://localhost:8080  (Spring Boot, Tomcat)
                    │
                    ▼
             PostgreSQL 5432 / examdb
```

The proxy is configured in `frontend/vite.config.js`. It is why the browser
address bar only ever shows 5173 even though every login goes to Spring Boot —
and it is what keeps the `JSESSIONID` cookie same-origin, so no CORS setup is
needed.

**Both servers must be running.** Stop the backend and every login returns 502.

---

## Working with migrations

Schema changes go through Flyway. Never `CREATE TABLE` by hand.

- **Location:** `backend/src/main/resources/db/migration/`
- **Naming:** `V2__add_questions_table.sql`, `V3__...` — capital `V`, number,
  **two** underscores, snake_case description.
- **They run automatically** at startup, in version order, once each. Applied
  migrations are recorded in the `flyway_schema_history` table.

Three rules that will otherwise cost you an afternoon:

1. **Never edit a migration that has already run.** Flyway stores a checksum and
   will refuse to start with a validation error. Write a new migration instead.
2. **`spring.jpa.hibernate.ddl-auto=validate`** means Hibernate *checks* that
   entities match the schema and never creates anything. Add a `@Entity` field
   without a matching migration and the app fails to start — that is intentional,
   it stops the schema and the code drifting apart.
3. **To wipe and start over** (destroys all data):

   ```bash
   psql -U postgres -c "DROP DATABASE examdb;"
   psql -U postgres -c "CREATE DATABASE examdb;"
   ```

   Then restart the backend — Flyway rebuilds the schema and the seeder refills
   the demo accounts.

---

## Troubleshooting

### `Failed to execute goal ... spring-boot-maven-plugin ... exit code: 1`

That line is only the wrapper. **Scroll up** for the real cause, which is usually:

```
***************************
APPLICATION FAILED TO START
***************************
Description:
Web server failed to start. Port 8080 was already in use.
```

Find and stop whatever holds the port:

```powershell
# Windows - see what it is
Get-NetTCPConnection -LocalPort 8080 -State Listen |
  ForEach-Object { Get-Process -Id $_.OwningProcess }

# then stop it
Stop-Process -Id <PID> -Force
```

```bash
# macOS / Linux
lsof -i :8080
kill -9 <PID>
```

Most often it is a backend you started earlier and forgot, possibly launched by
your IDE rather than your terminal.

### `Vite: Port 5173 is in use, trying another one...`

Another dev server is running. Harmless, but you end up on 5174 and can confuse
yourself about which copy you are looking at. Same fix as above with port 5173.

### `FATAL: password authentication failed for user "postgres"`

The password in `application-local.properties` does not match what you set when
installing PostgreSQL. Fix step 6.

### `FATAL: database "examdb" does not exist`

You skipped step 5.

### `Connection to localhost:5432 refused`

PostgreSQL is not running.

```powershell
Get-Service postgresql*
Start-Service postgresql-x64-17
```

### `Schema-validation: missing table [...]`

An entity has no matching migration, or the migration never ran. Check
`db/migration/` and the startup log for the Flyway lines.

### Login shows `Request failed (502)` in the browser

The backend is not running, or crashed. Check its terminal.

### Signed in but the page looks stale after stopping the backend

Expected. The signed-in user is held in React state and only re-checked on load,
so an open tab keeps rendering until you refresh. **Refresh before concluding
anything about whether the backend is up.**

### `npm ci` fails with an engine or syntax error

Node is too old. `node -v` must be 22.12+.

### `java.lang.UnsupportedClassVersionError` or "release version 21 not supported"

Wrong JDK on PATH. `java -version` must say 21.

---

## Daily routine, once set up

```powershell
# terminal 1
cd backend ; .\mvnw.cmd spring-boot:run

# terminal 2
cd frontend ; npm run dev
```

Then <http://localhost:5173>. `npm ci` is only needed again when
`package-lock.json` changes; `mvnw` re-resolves dependencies automatically when
`pom.xml` changes.
