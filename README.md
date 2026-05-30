# Ecommerse (Spring Boot + MySQL)

## Local setup (Windows / PowerShell)

1. Ensure MySQL is running on `localhost:3306`.
2. Create a MySQL user (or use an existing one) that can create/use the `ecommerse` database.
3. Set environment variables (recommended):

```powershell
$env:MYSQL_USER="root"
$env:MYSQL_PASSWORD="your_mysql_password"
```

4. Run the app:

```powershell
./mvnw -DskipTests spring-boot:run
```

## Database migrations

Flyway migrations live in:

- `src/main/resources/db/migration/`

On app startup, Flyway applies migrations automatically.

