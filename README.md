![Acompáñame — Backend](docs/banner-back.png)

# 🤝 Acompáñame — Backend

**Acompáñame** es una plataforma de marketplace que conecta a familias que necesitan cuidados para un ser querido con cuidadores profesionales verificados. Nace para cubrir el hueco entre la atención residencial a tiempo completo y las listas de espera de la atención formal, dando una alternativa flexible tanto a personas mayores como a familias que viven lejos de sus seres queridos.

Este repositorio contiene la **API REST** del proyecto, desarrollada con **Spring Boot**. El frontend (Vue.js) vive en un repositorio independiente, enlazado más abajo.

Proyecto final del bootcamp de Desarrollo Web Full Stack (850h) en **Factoría F5 — Digital Academy**.

![Java](https://img.shields.io/badge/Java-21-orange) ![Spring Boot](https://img.shields.io/badge/Spring%20Boot-4.1.1-6DB33F) ![MySQL](https://img.shields.io/badge/MySQL-8.0-4479A1) ![Docker](https://img.shields.io/badge/Docker-compose-2496ED) ![Tests](https://img.shields.io/badge/tests-436-success)

> [!TIP]
> 📚 **Documentación completa en la [wiki del proyecto](https://github.com/AndreaVaGo/acompaname-backend/wiki).** Aquí está lo imprescindible para entender, arrancar y probar el backend; el detalle (arquitectura, modelo de datos, endpoints, seguridad, tests, diagramas y decisiones) está en la wiki.

---

## 📑 Tabla de contenidos

- [Descripción del proyecto](#-descripción-del-proyecto)
- [Tecnologías utilizadas](#-tecnologías-utilizadas)
- [Instalación y puesta en marcha](#-instalación-y-puesta-en-marcha)
- [Configuración](#-configuración)
- [Arquitectura y patrones de diseño](#-arquitectura-y-patrones-de-diseño)
- [Modelo de datos](#-modelo-de-datos)
- [Endpoints de la API](#-endpoints-de-la-api)
- [Panel de administración](#-panel-de-administración)
- [Seguridad](#-seguridad)
- [Testing](#-testing)
- [Diagramas técnicos](#-diagramas-técnicos)
- [Diseño y gestión del proyecto](#-diseño-y-gestión-del-proyecto)
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

A los **cuidadores**:
- 🧑‍⚕️ Registrarse y crear su perfil profesional (especialidad, experiencia, tarifa, disponibilidad y foto)
- 📥 Consultar y gestionar las solicitudes que reciben
- ✅ Aceptar, rechazar o marcar como completadas las solicitudes

Y al **administrador**:
- 📊 Ver un resumen de la actividad (familias, cuidadores, solicitudes, ingresos y valoración media) y los avisos que requieren atención
- 🚫 Bloquear y desbloquear cuentas de usuario
- 🗑️ Eliminar valoraciones inadecuadas

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

## 🚀 Instalación y puesta en marcha

### Requisitos previos

- Java 21
- Maven (o usar el wrapper <mark>./mvnw</mark> incluido)
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
   Esto crea un contenedor MySQL (<mark>acompaname-mysql</mark>) con la base de datos <mark>acompaname_db</mark>, disponible en el puerto <mark>3306</mark>.

3. **Arranca la aplicación**
```bash
   ./mvnw spring-boot:run
```
   La API quedará disponible en <mark>http://localhost:8080/api/v1</mark>

   Al arrancar, la aplicación crea sola los roles <mark>FAMILIA</mark>, <mark>CUIDADOR</mark> y <mark>ADMIN</mark> si no existen (clase <mark>RoleInitializer</mark>). Así el registro funciona desde la primera vez, con una base de datos nueva, sin insertar nada a mano.

   Si quieres que se cree también el **usuario administrador**, define antes las variables <mark>ADMIN_EMAIL</mark> y <mark>ADMIN_PASSWORD</mark> (ver [Panel de administración](#-panel-de-administración)).

4. **Ejecuta los tests** (opcional, pero recomendado; los de integración necesitan la base de datos del paso 2 arrancada con <mark>docker compose up -d</mark>)
```bash
   ./mvnw test
```

---

## 🔐 Configuración

La conexión a la base de datos se lee de variables de entorno. Si no existen, se usan los valores de desarrollo local, así que el proyecto arranca sin configurar nada.

| Variable | Valor por defecto | Para qué sirve |
|---|---|---|
| <mark>DB_HOST</mark> | <mark>localhost</mark> | Servidor de la base de datos |
| <mark>DB_PORT</mark> | <mark>3306</mark> | Puerto |
| <mark>DB_NAME</mark> | <mark>acompaname_db</mark> | Nombre de la base de datos |
| <mark>DB_USER</mark> | <mark>acompaname_user</mark> | Usuario |
| <mark>DB_PASSWORD</mark> | <mark>acompaname_pass</mark> | Contraseña del usuario |
| <mark>DB_ROOT_PASSWORD</mark> | <mark>root</mark> | Contraseña root (solo la usa Docker Compose) |
| <mark>CORS_ORIGINS</mark> | <mark>http://localhost:5173</mark> | Dirección del frontend que puede llamar a la API |
| <mark>JPA_DDL_AUTO</mark> | <mark>update</mark> | Cómo Hibernate gestiona las tablas (en producción se pondría <mark>validate</mark>) |
| <mark>SHOW_SQL</mark> | <mark>false</mark> | Si Hibernate escribe en la consola el SQL que ejecuta |
| <mark>ADMIN_EMAIL</mark> | _(vacío)_ | Email del administrador que se crea al arrancar. Si está vacío, **no se crea ningún administrador** |
| <mark>ADMIN_PASSWORD</mark> | _(vacío)_ | Contraseña del administrador (se guarda con hash BCrypt) |

Otros ajustes:

| Ajuste | Valor |
|---|---|
| Contenedor | <mark>acompaname-mysql</mark> (mysql:8.0) |
| Volumen | <mark>acompaname_data</mark> |
| Prefijo de la API (<mark>api-endpoint</mark>) | <mark>api/v1</mark> |
| Origen CORS permitido (<mark>cors.allowed-origins</mark>) | <mark>http://localhost:5173</mark> por defecto; se cambia con la variable de entorno <mark>CORS_ORIGINS</mark> |

> [!TIP]
> Para usar otros valores de la base de datos, copia <mark>.env.example</mark> como <mark>.env</mark> en la raíz y cámbialos. Docker Compose lo lee solo. El archivo <mark>.env</mark> está en el <mark>.gitignore</mark>, así que no se sube a GitHub.

```bash
cp .env.example .env
```

> [!NOTE]
> El archivo <mark>.env</mark> lo lee **Docker Compose**, no Spring Boot. Por eso <mark>ADMIN_EMAIL</mark> y <mark>ADMIN_PASSWORD</mark> se definen como variables de entorno en la terminal desde la que se arranca la aplicación (ver [Panel de administración](#-panel-de-administración)).

> [!WARNING]
> Los valores por defecto son **solo para desarrollo local**. En un despliegue real hay que definir otras contraseñas en el entorno.

---

## 🏗 Arquitectura y patrones de diseño

El backend sigue una **arquitectura en capas** clásica de Spring Boot (<mark>Controller</mark> → <mark>Service</mark> → <mark>Repository</mark> → <mark>Entity</mark>), con DTOs y mappers entre el controlador y las entidades, y un <mark>GlobalExceptionHandler</mark> que convierte las excepciones en códigos HTTP claros.

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

👉 Estructura de paquetes, patrones y detalle de cada capa: [Arquitectura y patrones](https://github.com/AndreaVaGo/acompaname-backend/wiki/Arquitectura-y-patrones).

---

## 🗄 Modelo de datos

Seis entidades: <mark>Usuario</mark>, <mark>Role</mark>, <mark>PerfilCuidador</mark>, <mark>Solicitud</mark>, <mark>Pago</mark> y <mark>Valoracion</mark>. Un usuario tiene uno o varios roles (<mark>FAMILIA</mark>, <mark>CUIDADOR</mark> o <mark>ADMIN</mark>) y un campo <mark>activo</mark> que permite bloquear su cuenta; un usuario con rol <mark>CUIDADOR</mark> tiene un perfil; una familia crea solicitudes para un cuidador; cada solicitud puede tener un pago y una valoración.

👉 Diagramas entidad-relación y de clases: [Modelo de datos](https://github.com/AndreaVaGo/acompaname-backend/wiki/Modelo-de-datos).

---

## 📡 Endpoints de la API

Todos los endpoints tienen como prefijo base <mark>/api/v1</mark>.

| Recurso | Ruta base | Qué permite |
|---|---|---|
| 🔑 Autenticación | <mark>/login</mark> | Iniciar sesión (Basic Auth) |
| 👤 Usuarios | <mark>/usuarios</mark> | Registro (público); ver, editar y borrar solo el propio usuario |
| 🎭 Roles | <mark>/roles</mark> | Listar los roles (público) |
| 🧑‍⚕️ Cuidadores | <mark>/cuidadores</mark> | Listar y ver perfiles (público); crear, editar y borrar solo el rol <mark>CUIDADOR</mark> |
| 📋 Solicitudes | <mark>/solicitudes</mark> | Crear, editar y borrar solo la <mark>FAMILIA</mark>; cambiar estado; cuidadores ocupados (público, solo ids) |
| ⭐ Valoraciones | <mark>/valoraciones</mark> | Crear y gestionar las propias; valoraciones públicas de un cuidador |
| 💳 Pagos | <mark>/pagos</mark> | Solo la familia de la solicitud crea, edita, borra y paga |
| 🛡️ Administración | <mark>/admin</mark> | Solo el rol <mark>ADMIN</mark>: estadísticas, avisos, listados, bloquear usuarios y borrar valoraciones |

👉 Todos los endpoints con método, descripción y nivel de acceso, más un ejemplo de petición: [Endpoints de la API](https://github.com/AndreaVaGo/acompaname-backend/wiki/Endpoints-de-la-API).

---

## 🛡 Panel de administración

El **administrador observa y modera**: ve estadísticas y listados de toda la plataforma, bloquea o desbloquea cuentas y elimina valoraciones inadecuadas. No edita ni borra solicitudes, pagos ni usuarios, y no puede hacerse pasar por otra cuenta.

### Crear el administrador

El administrador se crea al arrancar la aplicación (clase <mark>AdminInitializer</mark>) a partir de dos variables de entorno. Si no se definen, no se crea ninguno, así que el repositorio no contiene ninguna credencial de administrador.

```bash
# Git Bash / Linux / macOS
export ADMIN_EMAIL=admin@acompaname.com
export ADMIN_PASSWORD=elige-una-contraseña
./mvnw spring-boot:run
```

```powershell
# PowerShell
$env:ADMIN_EMAIL="admin@acompaname.com"
$env:ADMIN_PASSWORD="elige-una-contraseña"
./mvnw spring-boot:run
```

Si el administrador ya existe, no se vuelve a crear. Después se entra con ese email y esa contraseña desde la pantalla de login del frontend, que lleva directamente al panel (<mark>/admin</mark>).

### Endpoints del administrador

Todos cuelgan de <mark>/api/v1/admin</mark> y exigen el rol <mark>ADMIN</mark> (cualquier otro usuario recibe <mark>403</mark>).

| Método | Ruta | Qué hace |
|---|---|---|
| <mark>GET</mark> | <mark>/estadisticas</mark> | Totales de familias, cuidadores y solicitudes por estado y tipo, ingresos cobrados y pendientes, valoración media y tiempo medio hasta aceptar |
| <mark>GET</mark> | <mark>/avisos</mark> | Cosas que requieren atención (por ejemplo, solicitudes pendientes) |
| <mark>GET</mark> | <mark>/cuidadores/mejor-valorados</mark> | Cuidadores con mejor nota media |
| <mark>GET</mark> | <mark>/valoraciones/ultimas</mark> | Últimas valoraciones de la plataforma |
| <mark>DELETE</mark> | <mark>/valoraciones/{id}</mark> | Elimina una valoración (<mark>204</mark>) |
| <mark>GET</mark> | <mark>/usuarios</mark> | Lista de usuarios con su rol y si están activos |
| <mark>PATCH</mark> | <mark>/usuarios/{id}/activo</mark> | Bloquea (<mark>activo: false</mark>) o desbloquea (<mark>activo: true</mark>) una cuenta |
| <mark>GET</mark> | <mark>/solicitudes?estado=</mark> | Lista de solicitudes, con filtro opcional por estado |

---

## 🔒 Seguridad

- 🔑 **Autenticación:** Spring Security con HTTP Basic Auth (email + contraseña).
- 🔐 **Contraseñas:** guardadas con hash <mark>BCrypt</mark>; nunca se guardan ni se devuelven en texto plano.
- 🎭 **Roles:** <mark>FAMILIA</mark>, <mark>CUIDADOR</mark> y <mark>ADMIN</mark> controlan qué endpoints puede usar cada usuario. Todo lo que cuelga de <mark>/admin</mark> es solo para <mark>ADMIN</mark>.
- 🚫 **Cuentas bloqueadas:** un usuario con <mark>activo = false</mark> no puede iniciar sesión (<mark>401</mark>). Como Basic Auth comprueba las credenciales en cada petición, el bloqueo se aplica al instante. El administrador no puede bloquear su propia cuenta.
- 🛡️ **Propietario:** los servicios comprueban que el recurso es del usuario que ha iniciado sesión y, si no, devuelven <mark>403</mark>; así nadie puede ver ni modificar datos ajenos probando ids.
- 🌐 **CORS:** solo se permite el origen del frontend (propiedad <mark>cors.allowed-origins</mark>, variable de entorno <mark>CORS_ORIGINS</mark>).
- 🔓 **Rutas públicas:** solo las que no exponen datos privados (registro, roles, listado y perfil de cuidadores, sus valoraciones públicas y los ids de cuidadores ocupados).

> [!NOTE]
> **Próxima mejora:** migrar de Basic Auth a autenticación con **JWT**.

👉 Detalle completo: [Seguridad](https://github.com/AndreaVaGo/acompaname-backend/wiki/Seguridad).

---

## 🧪 Testing

El proyecto cuenta con **436 tests** que cubren la lógica de negocio, la capa HTTP, la seguridad y la integración con una base de datos real:

| Tipo | Qué comprueba |
|---|---|
| 🧩 Servicios (unitarios, con Mockito) | La lógica de negocio de cada módulo, incluidos los errores, la privacidad de las valoraciones y la de la dirección |
| 🌐 Controladores (MockMvc) | Códigos de estado y JSON de cada endpoint |
| 🧱 Entidades (unitarios) | Constructores, getters y setters de las seis entidades |
| 🐳 Integración (Testcontainers + seguridad) | Repositorios contra MySQL real y reglas de acceso con peticiones reales |
| 🛡️ Administración | Servicio y controlador del panel, seguridad de <mark>/admin</mark> (solo <mark>ADMIN</mark>), bloqueo de usuarios y creación del administrador al arrancar contra MySQL real |
| 🛡️ Excepciones y seguridad (unitarios) | Códigos de error y datos del usuario que inicia sesión |
| 🚀 Arranque de la aplicación | Que el contexto de Spring carga |

```bash
./mvnw test
```

La cobertura se mide con **JaCoCo**: **96 % de instrucciones** y **95 % de ramas** (el mínimo pedido era el 70 %).

![Resultado de los tests del backend](docs/screenshots/tests-backend.png)
![Cobertura de los tests del backend](docs/screenshots/cobertura-backend.png)

👉 Qué prueba cada grupo de tests y los casos límite (404, 403, 409, 400 y 401): [Testing](https://github.com/AndreaVaGo/acompaname-backend/wiki/Testing).

---

## 📊 Diagramas técnicos

Casos de uso, diagramas de secuencia (registro y login, solicitar un servicio) y modelo de Chen, todos en Mermaid y visibles directamente en GitHub.

👉 [Diagramas técnicos](https://github.com/AndreaVaGo/acompaname-backend/wiki/Diagramas-tecnicos)

---

## 🎨 Diseño y gestión del proyecto

Proceso de diseño: bocetos → wireframes → mockups → prototipo, para móvil, tablet y escritorio. El proyecto se ha planificado en **JIRA** (épicas, historias de usuario con criterios de aceptación y tareas técnicas, organizadas en sprints).

- 🎨 **Figma:** https://www.figma.com/design/iNrrCJOe6CkQXTxhGzK7rj/Acomp%C3%A1%C3%B1ame
- 💜 **Prototipo en Lovable:** https://care-connection-hub-18.lovable.app
- 📊 **Tablero de JIRA:** https://saludosalamanecer-1780468848301.atlassian.net/jira/software/projects/ACOM/boards/34/timeline?rangeMode=MONTHS

| <ins>Landing</ins> | <ins>Buscar</ins> | <ins>Perfil del cuidador</ins> |
|---|---|---|
| ![Landing](docs/design/01-landing_escritorio.png) | ![Buscar](docs/design/04-buscar_escritorio.png) | ![Perfil](docs/design/05-perfil-cuidador_escritorio.png) |

👉 Más detalle: [Diseño y gestión](https://github.com/AndreaVaGo/acompaname-backend/wiki/Diseno-y-gestion).

---

## 🧭 Decisiones técnicas

📄 Documento completo con el proceso y los problemas encontrados: [docs/decisiones-tecnicas.pdf](docs/decisiones-tecnicas.pdf)

Algunas de las decisiones principales:

- ✅ **Roles en tabla propia** (<mark>@ManyToMany</mark>) en vez de un enum, siguiendo el patrón estándar de Spring Security.
- ✅ **DTOs en cada capa de entrada y salida**, para no exponer entidades (ni la contraseña) y evitar *mass assignment*.
- ✅ **Excepciones por entidad + <mark>GlobalExceptionHandler</mark> centralizado**, con códigos <mark>400</mark>, <mark>403</mark>, <mark>404</mark> y <mark>409</mark> claros.
- ✅ **Comprobar en el Service que el recurso es del usuario**, aunque conozca el id de otro.
- ✅ **Testcontainers** para los tests de integración, con el mismo MySQL que usa la aplicación.
- ✅ **Filtrar las valoraciones en el servidor**, no en el frontend.
- ✅ **Administrador que observa y modera:** solo bloquea cuentas y borra valoraciones; no edita solicitudes ni pagos ni suplanta a nadie.
- ✅ **El administrador se crea desde variables de entorno**, sin dejar ninguna credencial en el código ni en el repositorio.
- ✅ **Cuentas bloqueadas con el mismo 401 que una contraseña mala**, para no revelar qué emails existen.
- ✅ **Un test de integración con MySQL real para el arranque del administrador:** los tests con repositorios simulados no detectaron un fallo de sesión de Hibernate que solo aparecía al arrancar de verdad; se corrigió con <mark>@Transactional</mark> y se dejó el test que lo vigila.

👉 Todas las decisiones con su motivo: [Decisiones y roadmap](https://github.com/AndreaVaGo/acompaname-backend/wiki/Decisiones-y-roadmap).

---

## ⚠️ Limitaciones conocidas

> [!WARNING]
> De forma transparente, estas son las áreas identificadas como pendientes de mejora en la versión actual.

- 🔑 **Autenticación mediante Basic Auth**, no JWT. La migración a JWT está prevista como siguiente paso.
- 🗝️ **Credenciales por defecto de desarrollo:** se pueden cambiar con variables de entorno, pero los valores por defecto siguen en <mark>application.properties</mark> y <mark>docker-compose.yml</mark>.
- 💳 **Pagos simulados:** no hay pasarela de pago real. El pago se crea al aceptar la solicitud y se marca como pagado con <mark>PATCH /pagos/{id}/pagar</mark>.
- 🪪 **Basic Auth envía las credenciales en cada petición:** es sencillo pero menos seguro que un token; por eso JWT es la siguiente mejora.
- 🚧 **Sin límite de intentos de login** (falta proteger la aplicación de quien pruebe contraseñas una y otra vez).
- 🐢 **Algunas consultas cargan todos los registros y filtran en Java** (<mark>findAll()</mark>), por ejemplo la de los cuidadores ocupados. Con pocos datos no se nota; con muchos habría que pasarlas a consultas del repositorio.
- 🕘 **Las reservas son por días completos, no por horas.** Si un cuidador tiene un servicio aceptado de 9 a 12, la aplicación lo considera ocupado todo ese día. La mejora sería añadir hora de inicio y de fin a la solicitud y que el solapamiento se compruebe también por horas.
- 🛡️ **Un único administrador**, creado desde variables de entorno: no hay pantalla para crear más administradores.
- 🔒 **El bloqueo de una cuenta no dice el motivo:** quien está bloqueado recibe el mismo 401 que con una contraseña incorrecta. Es intencionado por privacidad, pero la persona bloqueada no sabe por qué.

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
- 🖼️ Guardar las fotos de perfil como archivos en un almacenamiento externo (ahora se guardan reducidas, como texto)
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