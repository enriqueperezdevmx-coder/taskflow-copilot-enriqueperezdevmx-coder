# Arquitectura de TaskFlow API

TaskFlow API es una aplicación REST con Spring Boot organizada por capas. La separación busca que
cada capa tenga una responsabilidad clara: HTTP en los controladores, casos de uso en los servicios,
persistencia en los repositorios y reglas de negocio en el modelo de dominio.

## Capas y flujo de una petición

| Capa | Paquete | Responsabilidad |
|---|---|---|
| Controlador | `com.taskflow.controller` | Recibe y devuelve HTTP, valida los cuerpos con `@Valid`, delega en servicios y construye respuestas HTTP. |
| Servicio | `com.taskflow.service` | Implementa los casos de uso, coordina entidades y repositorios, y traduce reglas de cambio de estado cuando corresponde. |
| Repositorio | `com.taskflow.repository` | Accede a la base de datos mediante Spring Data JPA. |
| Modelo | `com.taskflow.model` | Define las entidades JPA y conserva las invariantes y el comportamiento del dominio. |
| DTO | `com.taskflow.dto` | Define los contratos de entrada y salida de la API; los controladores no exponen entidades. |
| Mapper | `com.taskflow.mapper` | Convierte explícitamente entre DTOs y entidades, por ejemplo `TaskMapper.aResponse`. |

El recorrido habitual es:

1. El cliente llama a una ruta HTTP y, si es protegida, envía el JWT en la cabecera
   `Authorization`.
2. Spring Security procesa la petición. El filtro JWT autentica al usuario cuando el token es
   válido; los endpoints no públicos requieren una autenticación válida.
3. El controlador recibe path variables, parámetros y DTOs de petición. `@Valid` verifica las
   restricciones estructurales del DTO.
4. El controlador delega el caso de uso al servicio. El servicio consulta o persiste entidades a
   través de los repositorios.
5. Las reglas que pertenecen al dominio se aplican dentro de las entidades, especialmente en
   `Task`; no se duplican en controladores ni servicios.
6. El servicio devuelve entidades o resultados al controlador. Este las transforma a DTOs de
   respuesta mediante los mappers y construye la respuesta HTTP.
7. Las excepciones conocidas atraviesan las capas hasta `GlobalExceptionHandler`, que devuelve un
   cuerpo de error uniforme con el código adecuado.

Las entidades no se devuelven directamente. Además de mantener estable el contrato HTTP, los DTOs
evitan exponer detalles de persistencia, como relaciones JPA de carga diferida.

## Controladores y rutas

Todos los controladores usan rutas sin el prefijo `/api`.

### `AuthController`

`AuthController` está bajo `/auth` y gestiona el registro, el inicio de sesión y la consulta del
usuario autenticado.

| Método | Ruta | Respuesta normal |
|---|---|---|
| `POST` | `/auth/register` | `201 Created` con `UserResponse`. |
| `POST` | `/auth/login` | `200 OK` con `AuthResponse`, incluido el JWT. |
| `GET` | `/auth/me` | `200 OK` con el usuario identificado por el token. |

El registro es público y no devuelve el hash de contraseña. La implementación actual responde
`201 Created` para el registro, pero no añade cabecera `Location`.

### `ProjectController`

`ProjectController` expone el CRUD de proyectos y la consulta de sus tareas.

| Método | Ruta | Respuesta normal |
|---|---|---|
| `GET` | `/projects` | `200 OK` con la lista de proyectos. |
| `GET` | `/projects/{id}` | `200 OK` con un proyecto. |
| `GET` | `/projects/{id}/tasks` | `200 OK` con las tareas del proyecto; admite `?status=`. |
| `POST` | `/projects` | `201 Created` con `ProjectResponse` y `Location: /projects/{id}`. |
| `PUT` | `/projects/{id}` | `200 OK` con el proyecto reemplazado. |
| `DELETE` | `/projects/{id}` | `204 No Content`. |

El propietario de un proyecto se determina a partir del usuario autenticado, no de un dato enviado
por el cliente. El borrado elimina también las tareas asociadas mediante la relación en cascada.

### `TaskController`

`TaskController` administra las tareas. Una tarea se crea dentro de un proyecto, aunque después se
consulta individualmente bajo `/tasks`.

| Método | Ruta | Respuesta normal |
|---|---|---|
| `GET` | `/tasks` | `200 OK` con tareas; admite `?status=` o `?priority=`. |
| `GET` | `/tasks/{id}` | `200 OK` con una tarea. |
| `POST` | `/projects/{projectId}/tasks` | `201 Created` con `TaskResponse` y `Location: /tasks/{id}`. |
| `PUT` | `/tasks/{id}` | `200 OK` con la tarea reemplazada. |
| `PATCH` | `/tasks/{id}/status` | `200 OK` con la tarea cuyo estado cambió. |
| `DELETE` | `/tasks/{id}` | `204 No Content`. |

Antes de crear una tarea, el controlador verifica que el proyecto del path exista. Por tanto, un
`projectId` inexistente produce `404 Not Found`.

### `InfoController`

`InfoController` expone `GET /info`, una ruta pública que devuelve `200 OK` con el nombre y la
versión del servicio. Se utiliza como comprobación sencilla tras un despliegue.

## Seguridad

La seguridad usa JWT sin estado. El servidor no conserva una sesión: cada petición a un recurso
protegido debe incluir un token válido. `JwtAuthenticationFilter` se ejecuta antes de
`UsernamePasswordAuthenticationFilter`, identifica al usuario desde el token y deja su
autenticación disponible para la petición.

`SecurityConfig` configura `SessionCreationPolicy.STATELESS`, deshabilita CSRF para este esquema de
token en cabecera y usa BCrypt para las contraseñas. También define respuestas JSON diferenciadas:

| Situación | Código | Significado |
|---|---|---|
| No hay token válido en una ruta protegida | `401 Unauthorized` | El cliente no está autenticado. |
| Hay token válido, pero faltan permisos | `403 Forbidden` | El usuario está autenticado, pero no está autorizado. |

Son públicos:

- `/auth/**`
- `/info`
- Swagger: `/swagger-ui/**`, `/swagger-ui.html` y `/v3/api-docs/**`
- Consola H2: `/h2-console/**`
- La interfaz estática servida por la aplicación (`/`, HTML, CSS, JavaScript y favicon) mediante
  solicitudes `GET`

Todo endpoint no incluido en esas reglas requiere autenticación.

Los usuarios tienen roles, entre ellos `USER` y `ADMIN`. La autorización general se aplica en la
cadena de seguridad y la autorización que depende de datos se aplica a nivel de método. En
particular, borrar un proyecto requiere ser `ADMIN` o propietario del proyecto:

```java
@PreAuthorize("hasRole('ADMIN') or @projectSecurity.esOwner(#id, authentication.name)")
```

`ProjectSecurity` consulta el proyecto y el usuario para verificar la propiedad. Cuando el
proyecto no existe, permite que el flujo alcance el servicio para responder `404`, en lugar de
informar erróneamente un `403`.

## Manejo de excepciones

`GlobalExceptionHandler` es un `@RestControllerAdvice` que transforma excepciones conocidas en
`ErrorResponse`. El resultado mantiene un formato uniforme con marca temporal, código de estado,
mensaje y lista de errores.

| Código | Origen principal |
|---|---|
| `400 Bad Request` | Fallos de Bean Validation, JSON malformado, tipos de parámetros ilegibles y `TaskValidationException`, como una fecha límite pasada al crear. |
| `401 Unauthorized` | Credenciales inválidas durante el login; las rutas protegidas sin token válido las resuelve el `AuthenticationEntryPoint` de seguridad. |
| `403 Forbidden` | Usuario autenticado sin permiso, resuelto por el `AccessDeniedHandler`, por ejemplo al intentar borrar un proyecto ajeno. |
| `404 Not Found` | `TaskNotFoundException` o `ProjectNotFoundException`. |
| `409 Conflict` | `UsernameAlreadyExistsException` al registrar un nombre de usuario repetido. |
| `422 Unprocessable Entity` | `TaskStateException` cuando el estado del dominio impide una operación, como marcar `DONE` una tarea sin responsable. |

No existe un manejador genérico para `Exception`. Solo se traducen excepciones conocidas para no
ocultar ni convertir en errores genéricos las respuestas que Spring puede resolver correctamente.

## Reglas de dominio de `Task`

`Task` es una entidad JPA y también el lugar donde residen sus reglas de negocio. La fábrica
`Task.crear(...)` es el camino de creación de negocio:

- crea la entidad con identificador nulo;
- la inicia siempre en estado `TODO`;
- rechaza una fecha límite anterior al día actual;
- conserva las invariantes de título y proyecto exigidas por el constructor.

El constructor público se usa para rehidratar tareas existentes y permite fechas pasadas. Esto es
necesario porque una tarea existente puede vencer después de haber sido creada correctamente.

`Task.estaVencida()` devuelve `true` solo si la tarea tiene fecha límite, esa fecha ya pasó y su
estado no es `DONE`. Una tarea terminada no se considera vencida.

El cambio de estado también es responsabilidad de la entidad: `setStatus(DONE)` rechaza marcar una
tarea como terminada si no tiene `assigneeId`. `TaskService.cambiarStatus(...)` convierte esa
violación de estado en `TaskStateException`, que termina como `422 Unprocessable Entity`.

Los estados incluyen `TODO`, `IN_PROGRESS` y `DONE`. La creación inicia en `TODO`; para alcanzar
`DONE` es obligatorio que exista un responsable.

## Convenciones de implementación y pruebas

- Los DTOs se declaran como `record` y usan Bean Validation. Los controladores reciben cuerpos con
  `@Valid @RequestBody`.
- La inyección de dependencias es por constructor. No se usa Lombok.
- Los mappers manuales separan entidades y contratos HTTP. Por ejemplo,
  `TaskMapper.aResponse(...)` convierte una entidad `Task` en `TaskResponse`.
- Un `POST` de creación de proyectos o tareas responde `201 Created` e incluye la cabecera
  `Location` del recurso creado. Los `DELETE` exitosos responden `204 No Content`.
- Las pruebas unitarias usan JUnit 5 y Mockito sin levantar Spring.
- Las pruebas de capa usan `@WebMvcTest` o `@DataJpaTest`.
- Las pruebas de integración usan `@SpringBootTest` con el perfil `test`.
- Las clases `*IT.java` usan Testcontainers y solo se ejecutan al activar explícitamente
  `-Ddocker.tests=true`; no forman parte de la ejecución normal de `mvn test`.
