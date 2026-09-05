# 🤝 Acompáñame — Backend

**Acompáñame** es una plataforma de marketplace que conecta a familias que necesitan cuidados para un ser querido con cuidadores profesionales verificados. Nace para cubrir el hueco entre la atención residencial a tiempo completo y las listas de espera de la atención formal, dando una alternativa flexible tanto a personas mayores como a familias que viven lejos de sus seres queridos.

Este repositorio contiene la **API REST** del proyecto, desarrollada con **Spring Boot**. El frontend (Vue.js) vive en un repositorio independiente, enlazado más abajo.

Proyecto final del bootcamp de Desarrollo Web Full Stack (850h) en **Factoría F5 — Digital Academy**.

---

## 📑 Tabla de contenidos

- [Descripción del proyecto](#-descripción-del-proyecto)
- [Tecnologías utilizadas](#-tecnologías-utilizadas)
- [Arquitectura y patrones de diseño](#-arquitectura-y-patrones-de-diseño)
- [Modelo de datos](#-modelo-de-datos)
- [Instalación y puesta en marcha](#-instalación-y-puesta-en-marcha)
- [Variables de entorno](#-variables-de-entorno)
- [Endpoints de la API](#-endpoints-de-la-api)
- [Seguridad](#-seguridad)
- [Testing](#-testing)
- [Diagramas técnicos](#-diagramas-técnicos)
- [Capturas del prototipo](#-capturas-del-prototipo)
- [Gestión del proyecto](#-gestión-del-proyecto)
- [Decisiones técnicas](#-decisiones-técnicas)
- [Limitaciones conocidas](#-limitaciones-conocidas)
- [Roadmap / Mejoras futuras](#-roadmap--mejoras-futuras)
- [Enlaces del proyecto](#-enlaces-del-proyecto)
- [Autoría](#-autoría)

---

## 📖 Descripción del proyecto

**Acompáñame** resuelve un problema real: encontrar apoyo puntual y de confianza para el cuidado de personas mayores o dependientes, sin necesidad de contratar servicios residenciales completos.

La plataforma permite a las **familias**:
- Registrarse y gestionar su cuenta
- Buscar y consultar perfiles de cuidadores
- Crear solicitudes de servicio detallando el tipo de cuidado necesario
- Valorar el servicio recibido una vez completado

Y a los **cuidadores**:
- Registrarse y crear su perfil profesional (especialidad, experiencia, tarifa, disponibilidad)
- Consultar y gestionar las solicitudes que reciben
- Aceptar, rechazar o marcar como completadas las solicitudes

---

## 🛠 Tecnologías utilizadas

| Categoría | Tecnología |
|---|---|
| Lenguaje | Java 21 |
| Framework | Spring Boot 4.1.1 |
| Seguridad | Spring Security (autenticación Basic Auth + roles) |
| Persistencia | Spring Data JPA / Hibernate |
| Base de datos | MySQL 8.0 (dockerizada) |
| Build tool | Maven |
| Reducción de boilerplate | Lombok |
| Testing | JUnit 5, Mockito, MockMvc |
| Contenerización | Docker / Docker Compose |
| Control de versiones | Git / GitHub |

---

## 🏗 Arquitectura y patrones de diseño

El backend sigue una **arquitectura en capas** (MVC) clásica de Spring Boot, aplicando principios SOLID y buenas prácticas de diseño orientado a objetos:

```
Cliente (HTTP)
      │
      ▼
 Controller   ──►  recibe la petición, valida el DTO de entrada, delega en el Service
      │
      ▼
   Service    ──►  contiene la lógica de negocio; interfaz + implementación
      │
      ▼
  Repository  ──►  acceso a datos (Spring Data JPA)
      │
      ▼
   Entity     ──►  representación de las tablas en base de datos
```

**Patrones y principios aplicados:**

- **DTO (Data Transfer Object):** las entidades JPA nunca se exponen directamente en la API. Cada recurso tiene su `DTORequest` (datos de entrada, con validación) y `DTOResponse` (datos de salida, sin campos sensibles como la contraseña).
- **Mapper:** clases estáticas dedicadas a convertir entre `Entity` ↔ `DTO`, manteniendo el resto del código libre de esa lógica de transformación.
- **Interfaces genéricas:** `InterfaceGenericService<Entity, DTORequest, DTOResponse>` centraliza la firma de los métodos CRUD comunes a las cuatro entidades, evitando duplicación de código.
- **Inyección de dependencias por constructor:** en todos los `Service` y `Controller`, favoreciendo la inmutabilidad y la facilidad de testeo (frente a `@Autowired` en campo).
- **Excepciones personalizadas:** cada entidad cuenta con su propia jerarquía de excepciones (`XException` → `XExceptionNotFound`), capturadas de forma centralizada por un `GlobalExceptionHandler` (`@RestControllerAdvice`) que devuelve respuestas claras y con el código HTTP correcto.
- **Encapsulación:** todos los campos de las entidades son privados, con acceso exclusivo a través de getters/setters.

---

## 🗄 Modelo de datos

El sistema gira en torno a **cinco entidades principales**:

| Entidad | Descripción |
|---|---|
| `UsuarioEntity` | Usuario del sistema (familia o cuidador). Relación `@ManyToMany` con `RoleEntity`. |
| `RoleEntity` | Rol del usuario (`FAMILIA` / `CUIDADOR`). |
| `PerfilCuidadorEntity` | Perfil profesional de un cuidador. Relación `@OneToOne` con `UsuarioEntity`. |
| `SolicitudEntity` | Solicitud de servicio de una familia a un cuidador. Relaciones `@ManyToOne` con `UsuarioEntity` (familia) y `PerfilCuidadorEntity` (cuidador). |
| `ValoracionEntity` | Valoración de una solicitud completada. Relación `@OneToOne` con `SolicitudEntity`. |

> 📌 **[Aquí va el diagrama Entidad-Relación (ER)]**
>
> _(Pendiente de insertar imagen: `docs/diagrama-er.png`)_

> 📌 **[Aquí va el diagrama de clases UML]**
>
> _(Pendiente de insertar imagen: `docs/diagrama-clases.png`)_

---

## 🚀 Instalación y puesta en marcha

### Requisitos previos

- Java 21
- Maven (o usar el wrapper `./mvnw` incluido)
- Docker y Docker Compose
- Git

### Pasos

1. **Clona el repositorio**
   ```bash
   git clone https://github.com/AndreaVaGo/acompaname-backend.git
   cd acompaname-backend
   ```

2. **Levanta la base de datos con Docker Compose**
   ```bash
   docker compose up -d
   ```
   Esto crea un contenedor MySQL con la base de datos `acompaname_db`, disponible en el puerto `3306`.

3. **Configura las variables de entorno** (ver sección siguiente)

4. **Arranca la aplicación**
   ```bash
   ./mvnw spring-boot:run
   ```
   La API quedará disponible en `http://localhost:8080/api/v1`

5. **Ejecuta los tests** (opcional, pero recomendado)
   ```bash
   ./mvnw test
   ```

---

## 🔐 Variables de entorno

| Variable | Descripción | Ejemplo |
|---|---|---|
| `MYSQL_DATABASE` | Nombre de la base de datos | `acompaname_db` |
| `MYSQL_USER` | Usuario de la base de datos | `acompaname_user` |
| `MYSQL_PASSWORD` | Contraseña del usuario | `acompaname_pass` |
| `MYSQL_ROOT_PASSWORD` | Contraseña de root de MySQL | `root` |

> ⚠️ Ninguna credencial se sube al repositorio. Las variables se gestionan mediante configuración local (`application.properties` no versionado) o variables de entorno del sistema.

---

## 📡 Endpoints de la API

Todos los endpoints tienen como prefijo base: `/api/v1`

### Usuarios (`/usuarios`)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| `POST` | `/usuarios` | Registro de un nuevo usuario | Público |
| `GET` | `/usuarios` | Listar todos los usuarios | Autenticado |
| `GET` | `/usuarios/{id}` | Obtener un usuario por id | Autenticado |
| `PUT` | `/usuarios/{id}` | Actualizar un usuario | Autenticado |
| `DELETE` | `/usuarios/{id}` | Eliminar un usuario | Autenticado |

### Autenticación (`/login`)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| `GET` | `/login` | Inicio de sesión (Basic Auth) | Público (requiere credenciales válidas) |

### Perfiles de cuidador (`/cuidadores`)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| `POST` | `/cuidadores` | Crear perfil de cuidador | Rol `CUIDADOR` |
| `GET` | `/cuidadores` | Listar todos los perfiles | Autenticado |
| `GET` | `/cuidadores/{id}` | Obtener un perfil por id | Autenticado |
| `PUT` | `/cuidadores/{id}` | Actualizar un perfil | Autenticado |
| `DELETE` | `/cuidadores/{id}` | Eliminar un perfil | Autenticado |

### Solicitudes (`/solicitudes`)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| `POST` | `/solicitudes` | Crear una solicitud de servicio | Rol `FAMILIA` |
| `GET` | `/solicitudes` | Listar todas las solicitudes | Autenticado |
| `GET` | `/solicitudes/{id}` | Obtener una solicitud por id | Autenticado |
| `PUT` | `/solicitudes/{id}` | Actualizar una solicitud | Autenticado |
| `DELETE` | `/solicitudes/{id}` | Eliminar una solicitud | Autenticado |
| `PATCH` | `/solicitudes/{id}/estado` | Cambiar el estado de una solicitud (`PENDIENTE`, `ACEPTADA`, `RECHAZADA`, `COMPLETADA`) | Autenticado |

### Valoraciones (`/valoraciones`)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| `POST` | `/valoraciones` | Crear una valoración | Autenticado |
| `GET` | `/valoraciones` | Listar todas las valoraciones | Autenticado |
| `GET` | `/valoraciones/{id}` | Obtener una valoración por id | Autenticado |
| `PUT` | `/valoraciones/{id}` | Actualizar una valoración | Autenticado |
| `DELETE` | `/valoraciones/{id}` | Eliminar una valoración | Autenticado |

### Ejemplo de petición — Registro de usuario

```http
POST /api/v1/usuarios
Content-Type: application/json

{
  "nombre": "Ana",
  "email": "ana@ejemplo.com",
  "telefono": "600123456",
  "password": "contraseñaSegura123",
  "rolesIds": [1]
}
```

**Respuesta (`201 Created`):**
```json
{
  "id": 1,
  "nombre": "Ana",
  "email": "ana@ejemplo.com",
  "telefono": "600123456",
  "roles": ["FAMILIA"]
}
```

---

## 🔒 Seguridad

El backend implementa autenticación y autorización mediante **Spring Security**:

- **Autenticación:** HTTP Basic Auth sobre las credenciales del usuario (email + contraseña).
- **Almacenamiento de contraseñas:** cifradas con `BCryptPasswordEncoder`; nunca se guardan ni se devuelven en texto plano.
- **Gestión de usuarios:** `JpaUserDetailsService` propio, que consulta la tabla `usuarios` real (sin usuarios hardcodeados en memoria).
- **Roles:** cada usuario tiene uno o varios roles (`FAMILIA`, `CUIDADOR`) almacenados en una tabla `roles` con relación `@ManyToMany`.
- **Autorización por endpoint:** determinadas acciones están restringidas por rol (p. ej. solo un `CUIDADOR` puede crear su perfil profesional; solo una `FAMILIA` puede crear una solicitud).
- **Rutas públicas:** registro de usuario (`POST /usuarios`) y login (`GET /login`); el resto de rutas requieren autenticación.
- **Manejo de errores:** respuestas normalizadas y sin exposición de detalles internos (`GlobalExceptionHandler`).

> 🔜 **Próxima mejora:** migración de Basic Auth a autenticación mediante **JWT** (token de clave simétrica).

---

## 🧪 Testing

El proyecto cuenta con una suite de tests que cubre tanto la lógica de negocio como la capa de exposición HTTP:

- **Tests unitarios de Service** (`Mockito`): cada `ServiceImpl` está testeado de forma aislada, mockeando sus repositorios.
- **Tests de Controller** (`MockMvc` + `@WebMvcTest`): verifican que cada endpoint responde con el código de estado y el cuerpo JSON esperados.

```bash
./mvnw test
```

> 🔜 **Pendiente:** tests de integración con **TestContainers** contra una base de datos MySQL real, y tests específicos de seguridad (`@WithMockUser`) que validen las reglas de autorización por rol.

---

## 📊 Diagramas técnicos

> 📌 **[Diagrama Entidad-Relación]**
> _(Pendiente de insertar: `docs/diagrama-er.png`)_

> 📌 **[Diagrama de clases]**
> _(Pendiente de insertar: `docs/diagrama-clases.png`)_

---

## 🎨 Capturas del prototipo

> 📌 **[Wireframes / mockups de Figma]**
> _(Pendiente de insertar capturas)_

> 📌 **[Capturas del prototipo funcional]**
> _(Pendiente de insertar capturas)_

---

## 📋 Gestión del proyecto

La planificación y el seguimiento del proyecto se han gestionado en **JIRA**, organizados en:

- 4 épicas: Autenticación y Seguridad, Gestión de Cuidadores, Solicitudes de Servicio, Valoraciones
- 11 historias de usuario con criterios de aceptación en formato Gherkin
- 3 sprints

🔗 Enlace al tablero de JIRA: _[pendiente de añadir]_

---

## 🧭 Decisiones técnicas

Documentar el porqué de las decisiones, no solo el qué, para dejar constancia del proceso de desarrollo:

| Decisión | Motivo |
|---|---|
| **Roles en tabla `roles` (`@ManyToMany`) en lugar de un enum simple** | Un enum bastaba funcionalmente (un usuario = un rol), pero modelar los roles como entidad propia sigue el patrón estándar de Spring Security, permite añadir permisos/roles nuevos sin tocar código, y es coherente con proyectos reales. |
| **Lombok** | Reduce drásticamente el código repetitivo de getters, setters y constructores en las entidades, mejorando la legibilidad sin perder funcionalidad. |
| **`UsuarioEntity` no se separa en "datos de autenticación" y "datos de perfil"** | Se valoró separar en dos tablas (una solo para login, otra para el resto de datos), pero para el alcance de este proyecto se optó por mantenerlos juntos, priorizando simplicidad sobre una normalización que no aportaba valor funcional inmediato. |
| **DTOs explícitos en cada capa de entrada/salida** | Evita exponer las entidades JPA directamente (y con ellas, campos sensibles como la contraseña), y protege frente a *mass assignment* al aceptar solo los campos declarados explícitamente. |
| **Interfaz genérica `InterfaceGenericService<Entity, DTORequest, DTOResponse>`** | Las cuatro entidades comparten las mismas operaciones CRUD; centralizar su firma en una interfaz genérica evita repetir el mismo contrato cuatro veces. |
| **Excepciones personalizadas por entidad + `GlobalExceptionHandler` centralizado** | Permite devolver códigos HTTP y mensajes claros y específicos (`404`, `409`...) en lugar de errores genéricos, mejorando la experiencia de quien consume la API. |
| **Autorización por rol en la creación de recursos** (`POST /cuidadores` solo `CUIDADOR`, `POST /solicitudes` solo `FAMILIA`) | Refleja la lógica de negocio real: cada rol solo debe poder generar el tipo de recurso que le corresponde dentro del flujo de la plataforma. |
| **`ddl-auto=update` en lugar de `create-drop`** | Al trabajar contra una base de datos persistente en Docker (no en memoria), se prioriza no perder datos entre reinicios de la aplicación durante el desarrollo. |

---

## ⚠️ Limitaciones conocidas

De forma transparente, estas son las áreas identificadas como pendientes de mejora en la versión actual:

- **Autenticación mediante Basic Auth**, no JWT. La migración a JWT está prevista como siguiente paso.
- **Sin comprobación de propiedad de recursos (IDOR):** cualquier usuario autenticado puede consultar o modificar recursos que no le pertenecen (p. ej., una familia podría acceder a los datos de otra). Pendiente de implementar una validación de propietario en la capa de Service.
- **Autorización por rol solo en la creación (`POST`)** de `cuidadores` y `solicitudes`; las operaciones de actualización y borrado no distinguen todavía por rol.
- **Sin tests de integración con base de datos real** (p. ej. TestContainers); la suite actual cubre unidad (Service) y capa web (Controller con MockMvc), ambas con dependencias mockeadas.
- **Sin límite de intentos de login** (protección básica frente a fuerza bruta pendiente).

---

## 🗺 Roadmap / Mejoras futuras

Funcionalidades identificadas como **Fase 2**, fuera del alcance del PMV entregado:

- Autenticación mediante JWT
- Sistema de pagos entre familia y cuidador
- Geolocalización de cuidadores en tiempo real
- Videollamadas y chat en tiempo real
- Notificaciones push
- Generación de informes en PDF
- Filtros de búsqueda avanzados (especialidad, tarifa, disponibilidad)

---

## 🔗 Enlaces del proyecto

| Recurso | Enlace |
|---|---|
| Repositorio Backend | [github.com/AndreaVaGo/acompaname-backend](https://github.com/AndreaVaGo/acompaname-backend) |
| Repositorio Frontend | _[pendiente de añadir]_ |
| Presentación | _[pendiente de añadir]_ |
| Tablero JIRA | _[pendiente de añadir]_ |
| Diseño en Figma | _[pendiente de añadir]_ |

---

## 👩‍💻 Autoría

Proyecto desarrollado por **Andrea** como Proyecto Final del bootcamp de Desarrollo Web Full Stack en **Factoría F5 — Digital Academy**.

- GitHub: [@AndreaVaGo](https://github.com/AndreaVaGo)
- LinkedIn: _[pendiente de añadir]_

---
