![Acompáñame — Backend](docs/banner-back.png)

# 🤝 Acompáñame — Backend

**Acompáñame** es una plataforma de marketplace que conecta a familias que necesitan cuidados para un ser querido con cuidadores profesionales verificados. Nace para cubrir el hueco entre la atención residencial a tiempo completo y las listas de espera de la atención formal, dando una alternativa flexible tanto a personas mayores como a familias que viven lejos de sus seres queridos.

Este repositorio contiene la **API REST** del proyecto, desarrollada con **Spring Boot**. El frontend (Vue.js) vive en un repositorio independiente, enlazado más abajo.

Proyecto final del bootcamp de Desarrollo Web Full Stack (850h) en **Factoría F5 — Digital Academy**.

![Java](https://img.shields.io/badge/Java-21-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F) ![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1) ![Docker](https://img.shields.io/badge/Docker-compose-2496ED) ![Tests](https://img.shields.io/badge/tests-152-success)

---

## 📑 Tabla de contenidos

- [Descripción del proyecto](#-descripción-del-proyecto)
- [Tecnologías utilizadas](#-tecnologías-utilizadas)
- [Arquitectura y patrones de diseño](#-arquitectura-y-patrones-de-diseño)
- [Modelo de datos](#-modelo-de-datos)
- [Instalación y puesta en marcha](#-instalación-y-puesta-en-marcha)
- [Configuración](#-configuración)
- [Endpoints de la API](#-endpoints-de-la-api)
- [Seguridad](#-seguridad)
- [Testing](#-testing)
- [Diagramas técnicos](#-diagramas-técnicos)
- [Diseño y prototipo](#-diseño-y-prototipo)
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
- 📝 Registrarse y gestionar su cuenta
- 🔎 Buscar y consultar perfiles de cuidadores
- 📋 Crear solicitudes de servicio detallando el tipo de cuidado necesario
- 💳 Pagar el servicio (pago simulado)
- ⭐ Valorar el servicio recibido una vez completado

Y a los **cuidadores**:
- 🧑‍⚕️ Registrarse y crear su perfil profesional (especialidad, experiencia, tarifa, disponibilidad)
- 📥 Consultar y gestionar las solicitudes que reciben
- ✅ Aceptar, rechazar o marcar como completadas las solicitudes

---

## 🛠 Tecnologías utilizadas

| Categoría | Tecnología |
|---|---|
| ☕ Lenguaje | Java 21 |
| 🍃 Framework | Spring Boot 4.1.1 |
| 🔐 Seguridad | Spring Security (autenticación Basic Auth + roles) |
| 🗃️ Persistencia | Spring Data JPA / Hibernate |
| 🐬 Base de datos | MySQL 8.0 (dockerizada) |
| 🔨 Build tool | Maven |
| ✂️ Menos código repetido (getters, setters, constructores) | Lombok |
| 🧪 Testing | JUnit 5, Mockito, MockMvc, Testcontainers |
| 🐳 Contenerización | Docker / Docker Compose |
| 🌿 Control de versiones | Git / GitHub |

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

Un paquete por funcionalidad:

```
dev.andrea.acompaname_backend
├── auth            Login / logout
├── generics        InterfaceGenericService (contrato CRUD común)
├── globals         GlobalExceptionHandler
├── pago            Pagos
├── perfilcuidador  Perfiles de cuidador
├── role            Roles
├── security        SecurityConfiguration, JpaUserDetailsService
├── solicitud       Solicitudes de servicio
├── usuario         Usuarios
└── valoracion      Valoraciones
```

![Arquitectura por capas](docs/diagrams/capas.png)

**Patrones y principios aplicados:**

- 📦 **DTO (Data Transfer Object):** las entidades JPA nunca se exponen directamente en la API. Cada recurso tiene su <ins>DTORequest</ins> (datos de entrada, con validación) y <ins>DTOResponse</ins> (datos de salida, sin campos sensibles como la contraseña).
- 🔄 **Mapper:** clases estáticas dedicadas a convertir entre <ins>Entity</ins> ↔ <ins>DTO</ins>, manteniendo el resto del código libre de esa lógica de transformación.
- 🧬 **Interfaces genéricas:** <ins>InterfaceGenericService&lt;Entity, DTORequest, DTOResponse&gt;</ins> centraliza la firma de los métodos CRUD comunes a las entidades, evitando duplicación de código.
- 💉 **Inyección de dependencias por constructor:** en todos los <ins>Service</ins> y <ins>Controller</ins>, así las dependencias no se pueden cambiar por error y es más fácil probarlas en los tests (en vez de usar <ins>@Autowired</ins> en el atributo).
- 🚨 **Excepciones personalizadas:** cada entidad cuenta con su propia jerarquía de excepciones (<ins>XException</ins> → <ins>XExceptionNotFound</ins>, <ins>XExceptionAccesoDenegado</ins>), capturadas de forma centralizada por un <ins>GlobalExceptionHandler</ins> (<ins>@RestControllerAdvice</ins>) que devuelve respuestas claras y con el código HTTP correcto.
- 🔒 **Encapsulación:** todos los campos de las entidades son privados, con acceso exclusivo a través de getters/setters.

---

## 🗄 Modelo de datos

El sistema gira en torno a **seis entidades principales**:

| Entidad | Descripción |
|---|---|
| 👤 <ins>UsuarioEntity</ins> | Usuario del sistema (familia o cuidador). Relación <ins>@ManyToMany</ins> con <ins>RoleEntity</ins>. |
| 🎭 <ins>RoleEntity</ins> | Rol del usuario (<ins>FAMILIA</ins> / <ins>CUIDADOR</ins>). |
| 🧑‍⚕️ <ins>PerfilCuidadorEntity</ins> | Perfil profesional de un cuidador. Relación <ins>@OneToOne</ins> con <ins>UsuarioEntity</ins>. |
| 📋 <ins>SolicitudEntity</ins> | Solicitud de servicio de una familia a un cuidador. Relaciones <ins>@ManyToOne</ins> con <ins>UsuarioEntity</ins> (familia) y <ins>PerfilCuidadorEntity</ins> (cuidador). |
| ⭐ <ins>ValoracionEntity</ins> | Valoración de una solicitud completada. Relación <ins>@OneToOne</ins> con <ins>SolicitudEntity</ins>. |
| 💳 <ins>PagoEntity</ins> | Pago de una solicitud (<ins>PENDIENTE</ins>, <ins>COMPLETADO</ins>, <ins>CANCELADO</ins>). Relación <ins>@OneToOne</ins> con <ins>SolicitudEntity</ins>. |

### Diagrama Entidad-Relación

![Diagrama Entidad-Relación](docs/diagrams/er.png)

### Diagrama de clases

![Diagrama de clases](docs/diagrams/clases.png)

---

## 🚀 Instalación y puesta en marcha

### Requisitos previos

- Java 21
- Maven (o usar el wrapper <ins>./mvnw</ins> incluido)
- Docker y Docker Compose
- Git

> [!IMPORTANT]
> Docker tiene que estar en marcha antes de arrancar la aplicación y de ejecutar los tests: los necesitan la base de datos y los tests de integración.

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
   Esto crea un contenedor MySQL (<ins>acompaname-mysql</ins>) con la base de datos <ins>acompaname_db</ins>, disponible en el puerto <ins>3306</ins>.

3. **Arranca la aplicación**
   ```bash
   ./mvnw spring-boot:run
   ```
   La API quedará disponible en <ins>http://localhost:8080/api/v1</ins>

   Al arrancar, la aplicación crea sola los roles <ins>FAMILIA</ins> y <ins>CUIDADOR</ins> si no existen (clase <ins>RoleInitializer</ins>). Así el registro funciona desde la primera vez, con una base de datos nueva, sin insertar nada a mano.

4. **Ejecuta los tests** (opcional, pero recomendado; los de integración necesitan la base de datos del paso 2 arrancada con <ins>docker compose up -d</ins>)
   ```bash
   ./mvnw test
   ```

---

## 🔐 Configuración

La conexión a la base de datos se lee de variables de entorno. Si no existen, se usan los valores de desarrollo local, así que el proyecto arranca sin configurar nada.

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| <ins>DB_HOST</ins> | <ins>localhost</ins> | Servidor de la base de datos |
| <ins>DB_PORT</ins> | <ins>3306</ins> | Puerto |
| <ins>DB_NAME</ins> | <ins>acompaname_db</ins> | Nombre de la base de datos |
| <ins>DB_USER</ins> | <ins>acompaname_user</ins> | Usuario |
| <ins>DB_PASSWORD</ins> | <ins>acompaname_pass</ins> | Contraseña del usuario |
| <ins>DB_ROOT_PASSWORD</ins> | <ins>root</ins> | Contraseña root (solo la usa Docker Compose) |

Otros ajustes:

| Ajuste | Valor |
|---|---|
| Contenedor | <ins>acompaname-mysql</ins> (mysql:8.0) |
| Volumen | <ins>acompaname_data</ins> |
| Prefijo de la API (<ins>api-endpoint</ins>) | <ins>api/v1</ins> |
| Origen CORS permitido | <ins>http://localhost:5173</ins> (frontend) |

> [!TIP]
> Para usar otros valores, copia <ins>.env.example</ins> como <ins>.env</ins> en la raíz y cámbialos. Docker Compose lo lee solo. El archivo <ins>.env</ins> está en el <ins>.gitignore</ins>, así que no se sube a GitHub.

```bash
cp .env.example .env
```

> [!WARNING]
> Los valores por defecto son **solo para desarrollo local**. En un despliegue real hay que definir otras contraseñas en el entorno.

---

## 📡 Endpoints de la API

Todos los endpoints tienen como prefijo base: <ins>/api/v1</ins>

> [!TIP]
> **Leyenda de métodos:** 🟢 <ins>GET</ins> leer · 🟠 <ins>POST</ins> crear · 🔵 <ins>PUT</ins> actualizar · 🟣 <ins>PATCH</ins> cambiar un dato · 🔴 <ins>DELETE</ins> borrar.
>
> **Leyenda de acceso:** 🔓 público · 🔒 hay que iniciar sesión · 🧑‍⚕️ solo cuidadores · 👨‍👩‍👧 solo familias.

### Autenticación

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| 🟢 <ins>GET</ins> | <ins>/login</ins> | Inicio de sesión (Basic Auth) | 🔓 Público (requiere credenciales válidas) |
| 🟢 <ins>GET</ins> | <ins>/logout</ins> | Cierre de sesión | 🔒 Autenticado |

### Usuarios (`/usuarios`)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| 🟠 <ins>POST</ins> | <ins>/usuarios</ins> | Registro de un nuevo usuario | 🔓 Público |
| 🟢 <ins>GET</ins> | <ins>/usuarios</ins> | Listar todos los usuarios | 🔒 Autenticado |
| 🟢 <ins>GET</ins> | <ins>/usuarios/{id}</ins> | Obtener un usuario por id | 🔒 Autenticado (propietario) |
| 🔵 <ins>PUT</ins> | <ins>/usuarios/{id}</ins> | Actualizar un usuario | 🔒 Autenticado (propietario) |
| 🔴 <ins>DELETE</ins> | <ins>/usuarios/{id}</ins> | Eliminar un usuario | 🔒 Autenticado (propietario) |

### Roles (`/roles`)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| 🟢 <ins>GET</ins> | <ins>/roles</ins> | Listar los roles | 🔓 Público |

Los roles no se crean desde la API: se crean al arrancar la aplicación (<ins>RoleInitializer</ins>).

### Perfiles de cuidador (`/cuidadores`)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| 🟠 <ins>POST</ins> | <ins>/cuidadores</ins> | Crear perfil de cuidador | 🧑‍⚕️ Rol <ins>CUIDADOR</ins> |
| 🟢 <ins>GET</ins> | <ins>/cuidadores</ins> | Listar todos los perfiles | 🔓 Público |
| 🟢 <ins>GET</ins> | <ins>/cuidadores/{id}</ins> | Obtener un perfil por id | 🔓 Público |
| 🟢 <ins>GET</ins> | <ins>/cuidadores/mi-perfil</ins> | Obtener el perfil del cuidador autenticado | 🔒 Autenticado |
| 🔵 <ins>PUT</ins> | <ins>/cuidadores/{id}</ins> | Actualizar un perfil | 🧑‍⚕️ Rol <ins>CUIDADOR</ins> (propietario) |
| 🔴 <ins>DELETE</ins> | <ins>/cuidadores/{id}</ins> | Eliminar un perfil | 🧑‍⚕️ Rol <ins>CUIDADOR</ins> (propietario) |

### Solicitudes (`/solicitudes`)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| 🟠 <ins>POST</ins> | <ins>/solicitudes</ins> | Crear una solicitud de servicio | 👨‍👩‍👧 Rol <ins>FAMILIA</ins> |
| 🟢 <ins>GET</ins> | <ins>/solicitudes</ins> | Listar todas las solicitudes | 🔒 Autenticado |
| 🟢 <ins>GET</ins> | <ins>/solicitudes/{id}</ins> | Obtener una solicitud por id | 🔒 Autenticado |
| 🟢 <ins>GET</ins> | <ins>/solicitudes/mis-solicitudes</ins> | Solicitudes del usuario autenticado | 🔒 Autenticado |
| 🔵 <ins>PUT</ins> | <ins>/solicitudes/{id}</ins> | Actualizar una solicitud | 👨‍👩‍👧 Rol <ins>FAMILIA</ins> (propietario) |
| 🔴 <ins>DELETE</ins> | <ins>/solicitudes/{id}</ins> | Eliminar una solicitud | 👨‍👩‍👧 Rol <ins>FAMILIA</ins> (propietario) |
| 🟣 <ins>PATCH</ins> | <ins>/solicitudes/{id}/estado</ins> | Cambiar el estado (<ins>PENDIENTE</ins>, <ins>ACEPTADA</ins>, <ins>RECHAZADA</ins>, <ins>COMPLETADA</ins>) | 🔒 Autenticado |

### Valoraciones (`/valoraciones`)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| 🟠 <ins>POST</ins> | <ins>/valoraciones</ins> | Crear una valoración | 🔒 Autenticado |
| 🟢 <ins>GET</ins> | <ins>/valoraciones</ins> | Listar todas las valoraciones | 🔒 Autenticado |
| 🟢 <ins>GET</ins> | <ins>/valoraciones/{id}</ins> | Obtener una valoración por id | 🔒 Autenticado |
| 🔵 <ins>PUT</ins> | <ins>/valoraciones/{id}</ins> | Actualizar una valoración | 🔒 Autenticado (propietario) |
| 🔴 <ins>DELETE</ins> | <ins>/valoraciones/{id}</ins> | Eliminar una valoración | 🔒 Autenticado (propietario) |

### Pagos (`/pagos`)

| Método | Ruta | Descripción | Acceso |
|---|---|---|---|
| 🟠 <ins>POST</ins> | <ins>/pagos</ins> | Crear un pago | 🔒 Autenticado |
| 🟢 <ins>GET</ins> | <ins>/pagos</ins> | Listar todos los pagos | 🔒 Autenticado |
| 🟢 <ins>GET</ins> | <ins>/pagos/{id}</ins> | Obtener un pago por id | 🔒 Autenticado |
| 🔵 <ins>PUT</ins> | <ins>/pagos/{id}</ins> | Actualizar un pago | 🔒 Autenticado |
| 🔴 <ins>DELETE</ins> | <ins>/pagos/{id}</ins> | Eliminar un pago | 🔒 Autenticado |
| 🟣 <ins>PATCH</ins> | <ins>/pagos/{id}/pagar</ins> | Marcar el pago como completado (simulado) | 🔒 Autenticado |

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

**Respuesta (<ins>201 Created</ins>):**
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

- 🔑 **Autenticación:** HTTP Basic Auth sobre las credenciales del usuario (email + contraseña).
- 🔐 **Almacenamiento de contraseñas:** cifradas con <ins>BCryptPasswordEncoder</ins>; nunca se guardan ni se devuelven en texto plano.
- 👥 **Gestión de usuarios:** <ins>JpaUserDetailsService</ins> propio, que consulta la tabla <ins>usuarios</ins> real (sin usuarios hardcodeados en memoria).
- 🎭 **Roles:** cada usuario tiene uno o varios roles (<ins>FAMILIA</ins>, <ins>CUIDADOR</ins>) almacenados en una tabla <ins>roles</ins> con relación <ins>@ManyToMany</ins>.
- 🚦 **Autorización por endpoint:** <ins>POST</ins>, <ins>PUT</ins> y <ins>DELETE</ins> de <ins>/cuidadores</ins> solo para <ins>CUIDADOR</ins>; <ins>POST</ins>, <ins>PUT</ins> y <ins>DELETE</ins> de <ins>/solicitudes</ins> solo para <ins>FAMILIA</ins>.
- 🛡️ **Protección de los datos de otros usuarios:** los servicios comprueban que el recurso pertenece al usuario que ha iniciado sesión y lanzan <ins>XExceptionAccesoDenegado</ins> (Usuario, PerfilCuidador, Solicitud y Valoración). Así nadie puede leer ni modificar datos ajenos probando ids.
- 🌐 **CORS:** solo se permite el origen <ins>http://localhost:5173</ins>, con los métodos <ins>GET</ins>, <ins>POST</ins>, <ins>PUT</ins>, <ins>DELETE</ins> y <ins>PATCH</ins>.
- 🔓 **Rutas públicas:** registro de usuario (<ins>POST /usuarios</ins>), listado de roles (<ins>GET /roles</ins>, lo necesita el formulario de registro), login (<ins>GET /login</ins>) y consulta de cuidadores (<ins>GET /cuidadores</ins> y <ins>GET /cuidadores/{id}</ins>, para que se puedan ver sin iniciar sesión); el resto de rutas requieren autenticación.
- 🚨 **Manejo de errores:** respuestas normalizadas y sin exposición de detalles internos (<ins>GlobalExceptionHandler</ins>).

> [!NOTE]
> **Próxima mejora:** migración de Basic Auth a autenticación mediante **JWT** (token de clave simétrica).

---

## 🧪 Testing

El proyecto cuenta con **152 tests** que cubren la lógica de negocio, la capa de exposición HTTP, la seguridad y la integración con base de datos real:

| Tipo | Tests |
|---|---|
| 🧩 Servicios (unitarios, con Mockito) | 89 |
| 🌐 Controladores (MockMvc) | 51 |
| 🐳 Integración (Testcontainers + seguridad) | 11 |
| 🚀 Arranque de la aplicación | 1 |

- 🧩 **Tests unitarios de Service** (<ins>Mockito</ins>): cada <ins>ServiceImpl</ins> está testeado de forma aislada, mockeando sus repositorios. Hay una clase para los casos normales y otra para los errores (<ins>...ErroresTest</ins>).
- 🌐 **Tests de Controller** (<ins>MockMvc</ins> + <ins>@WebMvcTest</ins>): verifican que cada endpoint responde con el código de estado y el cuerpo JSON esperados.
- 🐳 **Tests de integración** (<ins>Testcontainers</ins>): <ins>UsuarioRepositoryIntegrationTest</ins> y <ins>SecurityIntegrationTest</ins> se ejecutan contra un MySQL real que Testcontainers arranca en un contenedor de Docker solo mientras dura el test. Este último comprueba las reglas de seguridad con peticiones reales.

```bash
./mvnw test
```

![Resultado de los tests del backend](docs/screenshots/tests-backend.png)

### Cobertura

La cobertura se mide con **JaCoCo**. Al ejecutar <ins>./mvnw test</ins> se genera el informe en <ins>target/site/jacoco/index.html</ins>. El resultado actual es de un **90 % de instrucciones** y un **96 % de ramas**, por encima del 70 % mínimo que se pedía.

![Cobertura de los tests del backend](docs/screenshots/cobertura-backend.png)

Además de los casos normales, hay tests pensados para los **casos límite y de seguridad**:

- 🔎 **404:** pedir, editar o borrar algo que no existe (pagos, solicitudes, usuarios, perfiles y valoraciones).
- 🚫 **403:** que un usuario no pueda ver, editar ni borrar las solicitudes, el perfil, los datos o las valoraciones de otra persona. Se comprueba también que en esos casos no se guarda ni se borra nada.
- ⚔️ **409:** registrar dos veces el mismo email.
- ⚠️ **400:** enviar un pago sin importe o sin solicitud.
- 🔒 **401:** llamar sin iniciar sesión a rutas privadas (pagos, solicitudes, <ins>mi-perfil</ins>). El listado y el perfil público de los cuidadores sí se pueden ver sin cuenta.
- 💳 **Pago automático:** al aceptar una solicitud se crea el pago pendiente con la tarifa del cuidador, y al rechazarla no se crea ninguno.

> [!TIP]
> Al escribir estos tests encontré varios fallos reales y los arreglé: un pago que no existía devolvía error 500 en vez de 404, cualquier usuario podía editar o borrar la valoración de otra persona, y aceptar una solicitud no generaba el pago. Además, los errores no controlados ahora se escriben en el log para poder saber qué ha pasado.

---

## 📊 Diagramas técnicos

### Casos de uso

| Visitante | Familia | Cuidador |
|---|---|---|
| ![Visitante](docs/diagrams/casos-visitante.png) | ![Familia](docs/diagrams/casos-familia.png) | ![Cuidador](docs/diagrams/casos-cuidador.png) |

### Secuencia: registro y login

![Secuencia registro y login](docs/diagrams/sec1.png)

### Secuencia: solicitar un servicio

![Secuencia solicitud](docs/diagrams/sec2.png)

### Modelo Chen

![Diagrama de Chen](docs/diagrams/chen.png)

---

## 🎨 Diseño y prototipo

Bocetos, wireframes y mockups para móvil, tablet y escritorio. El conjunto completo está en [<ins>docs/design</ins>](docs/design).

| Landing | Buscar | Perfil del cuidador |
|---|---|---|
| ![Landing](docs/design/01-landing_escritorio.png) | ![Buscar](docs/design/04-buscar_escritorio.png) | ![Perfil](docs/design/05-perfil-cuidador_escritorio.png) |

- 🎨 **Figma:** https://www.figma.com/design/iNrrCJOe6CkQXTxhGzK7rj/Acomp%C3%A1%C3%B1ame
- 💜 **Prototipo en Lovable:** https://care-connection-hub-18.lovable.app

---

## 📋 Gestión del proyecto

La planificación y el seguimiento del proyecto se han gestionado en **JIRA** (proyecto <ins>ACOM</ins>), organizados en:

- 🗂️ 8 épicas: Autenticación y Seguridad, Gestión de Cuidadores, Solicitudes de Servicio, Valoraciones, Pagos, Calidad y pruebas, Diseño y UX, Infraestructura y entrega
- 📖 19 historias de usuario con criterios de aceptación en formato Gherkin
- 🔧 16 tareas técnicas
- 🏃 5 sprints entre agosto y octubre de 2026

🔗 Enlace al tablero de JIRA: https://saludosalamanecer-1780468848301.atlassian.net/jira/software/projects/ACOM/boards/34/timeline?rangeMode=MONTHS

![Cronograma de JIRA](docs/jira/jira-cronograma.png)


---

## 🧭 Decisiones técnicas

📄 Documento completo con el proceso y los problemas encontrados: [docs/decisiones-tecnicas.pdf](docs/decisiones-tecnicas.pdf)

Documentar el porqué de las decisiones, no solo el qué, para dejar constancia del proceso de desarrollo:

| Decisión | Motivo |
|---|---|
| ✅ **Roles en tabla <ins>roles</ins> (<ins>@ManyToMany</ins>) en lugar de un enum simple** | Un enum bastaba funcionalmente (un usuario = un rol), pero modelar los roles como entidad propia sigue el patrón estándar de Spring Security, permite añadir permisos/roles nuevos sin tocar código, y es coherente con proyectos reales. |
| ✅ **Lombok** | Reduce drásticamente el código repetitivo de getters, setters y constructores en las entidades, mejorando la legibilidad sin perder funcionalidad. |
| ✅ **<ins>UsuarioEntity</ins> no se separa en "datos de autenticación" y "datos de perfil"** | Se valoró separar en dos tablas (una solo para login, otra para el resto de datos), pero para el alcance de este proyecto se optó por mantenerlos juntos, priorizando simplicidad sobre una normalización que no aportaba valor funcional inmediato. |
| ✅ **DTOs explícitos en cada capa de entrada/salida** | Evita exponer las entidades JPA directamente (y con ellas, campos sensibles como la contraseña), y protege frente a *mass assignment* al aceptar solo los campos declarados explícitamente. |
| ✅ **Interfaz genérica <ins>InterfaceGenericService&lt;Entity, DTORequest, DTOResponse&gt;</ins>** | Las entidades comparten las mismas operaciones CRUD; centralizar su firma en una interfaz genérica evita repetir el mismo contrato en cada módulo. |
| ✅ **Excepciones personalizadas por entidad + <ins>GlobalExceptionHandler</ins> centralizado** | Permite devolver códigos HTTP y mensajes claros y específicos (<ins>403</ins>, <ins>404</ins>, <ins>409</ins>...) en lugar de errores genéricos, mejorando la experiencia de quien consume la API. |
| ✅ **Autorización por rol en la creación, edición y borrado** | Refleja la lógica de negocio real: cada rol solo debe poder generar y gestionar el tipo de recurso que le corresponde dentro del flujo de la plataforma. |
| ✅ **Comprobar en el Service que el recurso es del usuario** | Un usuario autenticado solo puede tocar sus propios datos, aunque conozca el id de otro recurso. |
| ✅ **<ins>ddl-auto=update</ins> en lugar de <ins>create-drop</ins>** | Al trabajar contra una base de datos persistente en Docker (no en memoria), se prioriza no perder datos entre reinicios de la aplicación durante el desarrollo. |
| ✅ **Testcontainers para los tests de integración** | Prueban con un MySQL real, el mismo que usa la aplicación, en vez de una base de datos distinta en memoria. |

---

## ⚠️ Limitaciones conocidas

> [!WARNING]
> De forma transparente, estas son las áreas identificadas como pendientes de mejora en la versión actual.

- 🔑 **Autenticación mediante Basic Auth**, no JWT. La migración a JWT está prevista como siguiente paso.
- 🗝️ **Credenciales por defecto de desarrollo:** se pueden cambiar con variables de entorno, pero los valores por defecto siguen en <ins>application.properties</ins> y <ins>docker-compose.yml</ins>.
- 💳 **Pagos simulados:** no hay pasarela de pago real. El pago se crea al aceptar la solicitud y se marca como pagado con <ins>PATCH /pagos/{id}/pagar</ins>.
- 🚧 **Sin límite de intentos de login** (falta proteger la aplicación de quien pruebe contraseñas una y otra vez).

---

## 🗺 Roadmap / Mejoras futuras

> [!TIP]
> Funcionalidades identificadas como **Fase 2**, fuera del alcance del MVP entregado.

- 🔑 Autenticación mediante JWT
- 💳 Pasarela de pago real (por ejemplo Stripe en modo test)
- 📍 Geolocalización de cuidadores en tiempo real
- 💬 Videollamadas y chat en tiempo real
- 🔔 Notificaciones push
- 📄 Generación de informes en PDF
- 🔍 Filtros de búsqueda avanzados (especialidad, tarifa, disponibilidad)
- ☁️ Despliegue (imagen Docker y base de datos en la nube)

---

## 🔗 Enlaces del proyecto

| Recurso | Enlace |
|---|---|
| 🍃 Repositorio Backend | [github.com/AndreaVaGo/acompaname-backend](https://github.com/AndreaVaGo/acompaname-backend) |
| 🖥️ Repositorio Frontend | [github.com/AndreaVaGo/acompaname-frontend](https://github.com/AndreaVaGo/acompaname-frontend) |
| 🎤 Presentación | [docs/presentacion.pdf](docs/presentacion.pdf) |
| 📊 Tablero JIRA | https://saludosalamanecer-1780468848301.atlassian.net/jira/software/projects/ACOM/boards/34/timeline?rangeMode=MONTHS |
| 🎨 Diseño en Figma | https://www.figma.com/design/iNrrCJOe6CkQXTxhGzK7rj/Acomp%C3%A1%C3%B1ame |
| 💜 Prototipo Lovable | https://care-connection-hub-18.lovable.app |

---

## 👩‍💻 Autoría

Proyecto desarrollado por **Andrea Vallina González** como Proyecto Final del bootcamp de Desarrollo Web Full Stack en **Factoría F5 — Digital Academy**.

- 🐙 GitHub: [@AndreaVaGo](https://github.com/AndreaVaGo)
- 💼 LinkedIn: [Andrea Vallina González](https://www.linkedin.com/in/andrea-vallina-gonzalez/)

---