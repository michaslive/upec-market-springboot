# Modelo Entidad-Relación y normalización

Base de datos: `basededatosupeckmarquetspringboot` (MariaDB / InnoDB).

## Diagrama Entidad-Relación

```mermaid
erDiagram
    USERS {
        BIGINT id PK
        VARCHAR email UK
        VARCHAR password_hash
        VARCHAR full_name
        VARCHAR profile_photo_url
        ENUM account_status
        BOOLEAN two_factor_enabled
    }
    AUTH_SESSIONS {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR token_hash UK
        DATETIME expires_at
    }
    ROLES {
        SMALLINT id PK
        ENUM name UK
    }
    USER_ROLES {
        BIGINT user_id PK, FK
        SMALLINT role_id PK, FK
        TIMESTAMP assigned_at
    }
    CATEGORIES {
        BIGINT id PK
        VARCHAR name UK
        VARCHAR slug UK
        BOOLEAN active
    }
    PRODUCTS {
        BIGINT id PK
        BIGINT seller_id FK
        BIGINT category_id FK
        BIGINT reserved_by FK
        BIGINT reviewed_by FK
        VARCHAR title
        DECIMAL price
        ENUM status
    }
    FAVORITES {
        BIGINT user_id PK, FK
        BIGINT product_id PK, FK
        TIMESTAMP created_at
    }
    CART_ITEMS {
        BIGINT id PK
        BIGINT user_id FK
        BIGINT product_id FK
        TIMESTAMP added_at
    }
    TRANSACTIONS {
        BIGINT id PK
        BIGINT product_id FK
        BIGINT buyer_id FK
        BIGINT seller_id FK
        ENUM transaction_type
        ENUM status
        DECIMAL amount
        VARCHAR payment_method
    }
    MESSAGES {
        BIGINT id PK
        BIGINT transaction_id FK
        BIGINT sender_id FK
        BIGINT recipient_id FK
        TEXT body
        DATETIME read_at
    }
    REVIEWS {
        BIGINT id PK
        BIGINT transaction_id FK
        BIGINT reviewer_id FK
        BIGINT reviewee_id FK
        TINYINT rating
    }
    REPORTS {
        BIGINT id PK
        BIGINT reporter_id FK
        BIGINT reported_user_id FK
        BIGINT product_id FK
        BIGINT resolved_by FK
        ENUM status
    }
    SUPPORT_REQUESTS {
        BIGINT id PK
        BIGINT user_id FK
        VARCHAR email
        VARCHAR subject
        TEXT message
        ENUM status
    }
    SYSTEM_SETTINGS {
        VARCHAR setting_key PK
        TEXT setting_value
        BIGINT updated_by FK
    }
    SECURITY_AUDIT_LOG {
        BIGINT id PK
        BIGINT actor_id FK
        VARCHAR action
        VARCHAR target_type
        BIGINT target_id
        JSON details
    }

    USERS ||--o{ AUTH_SESSIONS : tiene
    USERS ||--o{ USER_ROLES : recibe
    ROLES ||--o{ USER_ROLES : asigna
    USERS ||--o{ PRODUCTS : publica
    CATEGORIES ||--o{ PRODUCTS : clasifica
    USERS o|--o{ PRODUCTS : reserva
    USERS o|--o{ PRODUCTS : modera
    USERS ||--o{ FAVORITES : guarda
    PRODUCTS ||--o{ FAVORITES : es_guardado
    USERS ||--o{ CART_ITEMS : agrega
    PRODUCTS ||--o{ CART_ITEMS : esta_en
    PRODUCTS ||--o{ TRANSACTIONS : genera
    USERS ||--o{ TRANSACTIONS : compra
    USERS ||--o{ TRANSACTIONS : vende
    TRANSACTIONS o|--o{ MESSAGES : contextualiza
    USERS ||--o{ MESSAGES : envia
    USERS ||--o{ MESSAGES : recibe
    TRANSACTIONS ||--o{ REVIEWS : recibe
    USERS ||--o{ REVIEWS : escribe
    USERS ||--o{ REVIEWS : es_calificado
    USERS ||--o{ REPORTS : reporta
    USERS o|--o{ REPORTS : es_reportado
    PRODUCTS o|--o{ REPORTS : es_denunciado
    USERS o|--o{ REPORTS : resuelve
    USERS o|--o{ SUPPORT_REQUESTS : solicita
    USERS ||--o{ SYSTEM_SETTINGS : actualiza
    USERS o|--o{ SECURITY_AUDIT_LOG : ejecuta
```

`o|` representa una relación opcional (FK nullable). En `products`, los roles de vendedor, reservante y moderador corresponden a tres FK distintas hacia `users`.

## Relaciones principales

- **Usuarios y roles:** N:M, resuelta con `user_roles`. El registro asigna `BUYER` y `SELLER`; permisos administrativos se asignan explícitamente.
- **Usuarios y productos:** 1:N como vendedor. Cada publicación pertenece a una categoría y guarda por separado quién la reservó y quién la moderó.
- **Favoritos:** N:M entre usuarios y productos, con clave primaria compuesta `(user_id, product_id)`.
- **Carrito:** un usuario puede agregar varios productos; la clave única `(user_id, product_id)` evita duplicados. Agregar no reserva el artículo.
- **Transacciones:** cada transacción corresponde a un producto, comprador y vendedor; un usuario puede participar en varias como comprador o vendedor.
- **Mensajes:** cada mensaje tiene remitente y destinatario; opcionalmente se vincula a una transacción.
- **Reseñas:** cada reseña pertenece a una transacción y tiene revisor y evaluado. La restricción única `(transaction_id, reviewer_id)` limita a una reseña por participante y transacción.
- **Reportes:** pueden señalar un usuario, un producto o ambos; el administrador que resuelve el reporte es opcional.

## Normalización aplicada

### Primera Forma Normal (1FN)

- Cada tabla tiene una clave primaria y columnas con valores individuales; no se guardan listas de roles, categorías, artículos del carrito o mensajes en una sola columna.
- Las relaciones repetibles se representan con filas (`user_roles`, `cart_items`, `favorites`, `messages`).
- `security_audit_log.details` es la excepción deliberada: un objeto JSON para metadatos variables de auditoría. No se usa para atributos centrales que requieran joins, filtros relacionales o claves foráneas.

### Segunda Forma Normal (2FN)

- Las tablas con clave compuesta (`user_roles` y `favorites`) guardan datos que dependen de la combinación completa de sus claves.
- En `user_roles`, `assigned_at` corresponde a la asignación específica usuario-rol; en `favorites`, `created_at` corresponde al favorito usuario-producto.
- `cart_items` usa una clave sustituta, y además una restricción única sobre usuario-producto para representar la dependencia funcional de ese par.

### Tercera Forma Normal (3FN)

- Los datos descriptivos de roles y categorías están separados de usuarios y productos. Por ejemplo, el nombre de la categoría se guarda en `categories`, y `products` almacena `category_id`.
- Los atributos propios del usuario, producto, transacción y reporte se mantienen en sus respectivas tablas; no se replican nombres de roles o categorías en cada publicación.
- Las tablas de unión evitan repetir grupos de atributos para relaciones N:M.
- Los estados y tipos con conjunto controlado se restringen con `ENUM`; las categorías y roles, que además tienen identidad y atributos, usan tablas maestras independientes.

## Decisión de historial transaccional

`transactions.seller_id` y `transactions.amount` guardan una copia del vendedor y del precio al concretarse la operación. Aunque el vendedor puede derivarse de `products.seller_id` y el monto del precio actual, conservarlos en la transacción es intencional: mantiene el historial contable si luego cambia el precio o se modifica la publicación. Por esa misma razón, la aplicación debe tomar ambos valores desde el producto reservado al crear la transacción, no confiar en valores suministrados por el cliente.

## Consideraciones

- El índice público de confianza se calcula desde `reviews`; no se almacena como dato duplicado.
- La reserva se representa en `products` (`status`, `reserved_by`, `reservation_expires_at`) para que varios compradores no obtengan reservas simultáneas del mismo producto.
- Los índices y restricciones únicos refuerzan búsquedas frecuentes e integridad referencial; las FK usan InnoDB.
