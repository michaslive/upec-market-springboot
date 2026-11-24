# Arquitectura UPEC Market — Bloques 1 a 3

## Alcance de base de datos

Se mantiene **MariaDB de XAMPP** por decisión del equipo. El contenido de persistencia del Bloque 3 pide PostgreSQL; por ello, el motor no satisface esa parte literal del temario. La arquitectura JPA reduce el acoplamiento a JDBC, pero el esquema y `V1` siguen usando sintaxis/columnas compatibles con MariaDB. No se debe cambiar el `DB_URL` a PostgreSQL sin crear y probar migraciones específicas y migrar los datos.

## Bloque 1 — Backend Spring Boot

### Organización

```text
backend/src/main/java/com/upecmarket/backend/
  ApiController.java               Endpoints REST del marketplace
  controller/                      Upload de imágenes de producto y de perfil
  service/                         Casos de uso de producto, perfil y archivos
  repository/                      Repositorios Spring Data JPA
  entity/                          Entidades JPA para users, categories y products
  dto/                             Request/response y validación de API
  config/                          OpenAPI
  AuthInterceptor.java             Autenticación Bearer existente
  ApiExceptionHandler.java         Respuestas HTTP de error centralizadas
```

Los endpoints de productos y perfiles delegan en servicios y repositorios JPA. Endpoints legados de sesión, carrito, mensajería y transacciones continúan usando `JdbcTemplate`; es un paso de transición para conservar la API y los datos existentes, no una arquitectura ya migrada por completo a JPA.

### Flujo HTTP

1. Spring MVC recibe la solicitud y resuelve la ruta de un `@RestController`.
2. `AuthInterceptor` valida el token Bearer en rutas privadas y adjunta el usuario autenticado a la solicitud.
3. Spring convierte JSON/multipart. `@Valid` aplica restricciones de los DTO con Jakarta Validation antes de entrar al caso de uso.
4. El controller delega la regla de negocio a una clase `@Service`.
5. El servicio ejecuta una transacción con `@Transactional` y consulta/actualiza entidades por interfaces `JpaRepository` (`@Repository` es proporcionado por Spring Data).
6. Hibernate persiste los cambios. La respuesta se proyecta como DTO/mapa y Spring la serializa a JSON.
7. `ApiExceptionHandler` convierte validación, autorización, conflictos y recursos inexistentes en errores HTTP con un mensaje JSON.

La inyección usa constructores, no búsqueda global de beans. Por ejemplo, `ProductService` recibe repositorios y servicios por constructor.

### Entidades, DTO y consultas

- `UserAccountEntity` mapea usuarios; `CategoryEntity` las categorías; `ProductEntity` modela publicación, vendedor, categoría, estado de moderación y URL de imagen.
- `ProductRequest` valida título, descripción, categoría, precio y enumeraciones antes de procesar el alta/edición.
- `RegisterRequest` valida email institucional `@upec.edu.ec`, contraseña y nombre. `ProfileUpdateRequest` valida el nombre de perfil.
- `ProductRepository.findPublic` es una consulta JPQL personalizada con `@Query`, filtros opcionales y `Pageable` limitado a 100 resultados.
- `ProductService` y `ProfileService` delimitan unidades de trabajo con `@Transactional`; las operaciones de lectura usan `readOnly=true`.
- La integridad referencial y unicidad se conservan en MariaDB mediante llaves primarias, foráneas e índices definidos en la migración.

### API y configuración

- Swagger UI: `http://localhost:8080/swagger-ui.html`
- OpenAPI JSON: `http://localhost:8080/api-docs`
- URL JDBC, puerto, credenciales, CORS y directorio de imágenes se inyectan mediante `application.properties` y variables de entorno (`DB_URL`, `DB_USERNAME`, `DB_PASSWORD`, `UPLOAD_DIRECTORY`, entre otras).
- Flyway crea el esquema desde `V1__create_marketplace_schema.sql` en una base vacía. Para la base XAMPP preexistente, `baseline-on-migrate` la registra en versión 1 sin volver a crear ni borrar tablas. Las migraciones nuevas deben añadirse con versión superior (`V2__...sql`).
- Hibernate tiene `ddl-auto=none`: Flyway, no Hibernate, es el responsable del esquema.

## Bloque 2 — Frontend React, Axios y Tailwind

- `src/App.jsx` contiene la pantalla/catálogo y `src/api/client.js` centraliza Axios. `BrowserRouter` ofrece páginas URL para Inicio, Explorar, Favoritos, Mis publicaciones, Mensajes, Mis compras, Ayuda, Mi cuenta, Administración y Configuración.
- El interceptor de Axios adjunta el Bearer token a solicitudes autenticadas. Ante 401 elimina la sesión y notifica a React; el helper muestra errores HTTP, de conexión y timeout.
- La app usa estado React para autenticación, catálogo, formularios, conversación y diálogos de acciones puntuales. Mensajería, publicaciones, compras, ayuda, cuenta, moderación y configuración se muestran en páginas propias. El menú administrativo se muestra con roles retornados por el servidor.
- Las rutas privadas tienen guardas del cliente; además el interceptor/backend verifican token y rol. Guardar una configuración global también requiere confirmar contraseña y crea una entrada de auditoría.
- Tailwind CSS 4 se integra con Vite. El tema define tokens `upec-ink`, `upec-green`, `upec-mint` y `font-display`; edición de cuenta y cargas usan utilidades responsive/componentes junto con estilos existentes.
- `VITE_API_URL` permite cambiar el origen de API.

### Imágenes y edición de cuenta

- El formulario de publicación/edición permite seleccionar JPG, PNG o WebP; al crear/editar producto, la imagen se manda como `multipart/form-data` a `POST /api/products/{id}/image`.
- La foto de perfil se sube con `POST /api/auth/me/photo`; los campos del perfil se actualizan con `PATCH /api/auth/me`.
- Se generan nombres aleatorios, se comprueba tipo/contenido y el límite de tamaño es 5 MB. Se guardan en `UPLOAD_DIRECTORY` (por defecto `uploads/`) y se sirven mediante `/uploads/**`.
- Al cambiar la foto de un producto aprobado, la publicación vuelve a `pending` para moderación.

## Bloque 3 — Persistencia, normalización e integridad

- ER y normalización detallados: [modelo-entidad-relacion.md](../database/modelo-entidad-relacion.md).
- El esquema contempla usuarios, roles, categorías, publicaciones, favoritos, carrito, transacciones, mensajes, reseñas, reportes, soporte y auditoría.
- `V1__create_marketplace_schema.sql` es la migración inicial para instancias vacías; la base actual de XAMPP no se elimina ni se sobrescribe por el baseline.
- Los índices principales cubren catálogo (`status, category_id, created_at`), publicaciones por vendedor, reservas vencidas, historial de comprador/vendedor, conversaciones y bandeja de reportes.
- El catálogo se filtra con JPQL y se pagina en el repositorio. Operaciones sensibles como reserva y creación de compra mantienen la lógica existente con transacciones en el servidor.
- Para comprobar el estado de conexión: `GET http://localhost:8080/api/health`. Para listar productos aprobados: `GET /api/products`.

### Demostración local

1. Inicia MySQL desde XAMPP y verifica que exista `basededatosupeckmarquetspringboot`.
2. Inicia el backend: `.\backend\mvnw.cmd -f .\backend\pom.xml spring-boot:run`.
3. Inicia React: `cd frontend; npm run dev`.
4. Abre `http://localhost:5173`, registra/inicia sesión, sube una imagen, y comprueba la URL `/uploads/...` en la respuesta del API y la fila correspondiente en `products.image_url`.
5. Examina las rutas y esquemas JSON en Swagger UI.

Las imágenes se guardan en disco local, no en la base de datos. Para producción, el directorio debe ser persistente y estar servido detrás del mismo host o de un almacenamiento de objetos.
