# Frontend React

Aplicación React con Vite, independiente del backend y desplegable en otro servidor.

## Desarrollo

Desde esta carpeta:

```powershell
npm install
npm run dev
```

La app consulta `http://localhost:8080/api/health` por defecto. Copia `.env.example` a `.env` y ajusta `VITE_API_URL` si el backend está en otra URL. Para producción, define esa variable al construir la app (`npm run build`); Vite genera los archivos estáticos en `dist/`.

El servidor que aloje React debe estar permitido por `CORS_ALLOWED_ORIGINS` en el backend. Consulta el README de la raíz para ejecutar y configurar Spring Boot.
