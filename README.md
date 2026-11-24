# UPEC Market

Marketplace universitario para compra, venta e intercambio entre estudiantes de la UPEC. Incluye frontend React/Vite, API REST Spring Boot y PostgreSQL/Supabase.

## Iniciar la aplicación local

### 1. Preparar la base de datos PostgreSQL

1. Copia `backend/.env.example` a `backend/.env`.
2. Configura `DB_URL`, `DB_USERNAME` y `DB_PASSWORD` con los datos de tu proyecto Supabase/PostgreSQL.
3. Inicia el backend: Flyway crea o actualiza el esquema automáticamente.

El esquema crea roles y categorías iniciales, sesiones, usuarios, productos, carrito, transacciones, mensajes, reseñas, denuncias, soporte y auditoría en PostgreSQL/Supabase. Copia `backend/.env.example` a `backend/.env` y configura `DB_URL`, `DB_USERNAME` y `DB_PASSWORD`.

Si ya habías importado una versión anterior del SQL, importa también `database/migrate-backend.sql` sobre esa base para añadir las tablas/campos nuevos sin borrar datos.

### 2. Iniciar el backend

Se requiere Java 25 o superior. Desde PowerShell en la raíz:

```powershell
.\backend\mvnw.cmd -f .\backend\pom.xml spring-boot:run
```

La API estará en **http://localhost:8080** y la comprobación pública en [http://localhost:8080/api/health](http://localhost:8080/api/health). La API depende de que PostgreSQL/Supabase esté disponible y accesible desde la configuración de `DB_URL`.

### Despliegue

- **Backend (Render):** crea el servicio desde `render.yaml` con el Dockerfile de `backend/`. Configura `DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `CORS_ALLOWED_ORIGINS` y las cuatro variables `SUPABASE_S3_*`.
- **Frontend (Vercel):** importa la raíz del repositorio. El archivo `vercel.json` ejecuta el build de `frontend/`. Configura `VITE_API_URL` con la URL pública de Render, sin `/api` al final.
- **Supabase:** usa el bucket público `product-images`. El backend sube productos y comprobantes por S3 compatible y guarda la URL pública en PostgreSQL.

Para crear un Super Admin inicial, define correo institucional y contraseña antes del primer inicio (la contraseña requiere al menos 12 caracteres):

```powershell
$env:SUPER_ADMIN_EMAIL = "desarrollador@upec.edu.ec"
$env:SUPER_ADMIN_PASSWORD = "define-una-clave-segura-de-12"
$env:SUPER_ADMIN_NAME = "Administrador UPEC"
.\backend\mvnw.cmd -f .\backend\pom.xml spring-boot:run
```

No hay cuenta ni contraseña administrativa predeterminada. El inicializador crea esa cuenta y le asigna el rol `SUPER_ADMIN`; si ya existe, exige que la contraseña configurada coincida con la contraseña actual y no la reemplaza.

### 3. Iniciar el frontend

En otra terminal:

```powershell
Push-Location .\frontend
npm install
npm run dev
Pop-Location
```

La interfaz local se abre en **http://localhost:5173**. Axios consume la API con Bearer token y manejo de errores/timeout; Tailwind CSS 4 se utiliza junto a los estilos del marketplace. El frontend permite cargar publicaciones, editar cuenta, subir fotos y conversar. Los pagos siguen siendo simulados.

## Arquitectura y documentación

- [Arquitectura de los bloques 1–3](docs/arquitectura-bloques-1-3.md): capas, flujo HTTP/JSON, JPA, validación, Axios/Tailwind, Flyway e integridad.
- [Modelo ER y normalización](database/modelo-entidad-relacion.md).
- Swagger UI: [http://localhost:8080/swagger-ui.html](http://localhost:8080/swagger-ui.html).
- OpenAPI JSON: [http://localhost:8080/api-docs](http://localhost:8080/api-docs).

La persistencia usa PostgreSQL/Supabase. JPA/Hibernate se usa para productos y perfiles, y JDBC permanece en algunos flujos existentes. Flyway administra el esquema desde las migraciones V1, V2 y V3.

## API disponible

Las rutas privadas requieren el encabezado `Authorization: Bearer <token>`. Registro y acceso responden con token y perfil.

| Funcionalidad | Rutas |
|---|---|
| Salud y acceso | `GET /api/health`, `POST /api/auth/register`, `POST /api/auth/login`, `GET/PATCH /api/auth/me`, `POST /api/auth/logout` |
| Catálogo y perfil | `GET /api/categories`, `GET /api/products`, `GET /api/products/{id}`, `GET /api/users/{id}/profile`, `PATCH /api/auth/me` |
| Imágenes | `POST /api/products/{id}/image`, `POST /api/auth/me/photo` (multipart; JPG/PNG/WebP, 5 MB máximo) |
| Publicaciones | `POST /api/products`, `PUT/DELETE /api/products/{id}`, `GET /api/products/mine` |
| Moderación | `GET /api/admin/products/pending`, `PATCH /api/admin/products/{id}/moderation` |
| Carrito y reservas | `GET /api/cart`, `POST/DELETE /api/cart/{productId}`, `POST /api/cart/{productId}/reserve` |
| Transacciones | `POST /api/transactions`, `POST /api/transactions/{id}/simulate-payment`, `GET /api/transactions`, `PATCH /api/transactions/{id}/status` |
| Comunidad | `GET/POST /api/messages`, `POST /api/reviews`, `POST /api/reports`, `POST /api/support` |
| Administración | `GET /api/admin/users`, `PATCH /api/admin/users/{id}/status`, `PATCH /api/admin/users/{id}/roles`, reportes y configuración en `/api/admin` |

El registro solo admite `@upec.edu.ec` y asigna simultáneamente `BUYER` y `SELLER`. Las publicaciones quedan pendientes hasta aprobación. El carrito no reserva; la reserva dura 10 minutos por defecto y se libera automáticamente. Las opciones de tarjeta, transferencia y Deuna! son **simulaciones** que no procesan dinero. El chat se actualiza periódicamente mediante REST, no usa WebSocket. Los cambios globales requieren confirmar la contraseña. Las rutas administrativas se autorizan también en el backend, independientemente de lo que renderice el frontend.

### Ejemplo de registro

```powershell
$body = @{ fullName = "Estudiante UPEC"; email = "estudiante@upec.edu.ec"; password = "clave-segura-123" } | ConvertTo-Json
Invoke-RestMethod -Method Post -Uri http://localhost:8080/api/auth/register -ContentType "application/json" -Body $body
```

## Configuración

- `DB_URL`: URL JDBC PostgreSQL; predeterminada `jdbc:postgresql://localhost:5432/postgres`.
- `DB_USERNAME`, `DB_PASSWORD`: usuario y contraseña de PostgreSQL/Supabase.
- `SERVER_PORT`: puerto HTTP; predeterminado `8080`.
- `CORS_ALLOWED_ORIGINS`: orígenes frontend separados por coma.
- `SESSION_EXPIRATION_HOURS`: vida de las sesiones Bearer; predeterminado 24.
- `RESERVATION_DURATION_MINUTES`: expiración de reservas; máximo 30.
- `SUPER_ADMIN_EMAIL`, `SUPER_ADMIN_PASSWORD`, `SUPER_ADMIN_NAME`: bootstrap seguro de Super Admin; contraseña sin valor predeterminado.
- `UPLOAD_DIRECTORY`: carpeta persistente de imágenes; predeterminada `uploads`.
- `UPLOAD_MAX_BYTES`: máximo de carga; predeterminado 5 MiB.
- `VITE_API_URL`: origen backend que usa Axios en el frontend; predeterminado `http://localhost:8080`.

## Pruebas

```powershell
.\backend\mvnw.cmd -f .\backend\pom.xml test
Push-Location .\frontend
npm run lint
npm run build
Pop-Location
```

En producción configura credenciales fuertes, CORS con el origen exacto y TLS. Para cobros reales se requiere integrar una pasarela y verificar sus callbacks; el modo simulado no debe utilizarse para cobrar.
