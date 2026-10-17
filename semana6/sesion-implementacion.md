# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `5f578f9f-7cea-4792-b747-045a171f4c1c`  
> - **Started:** 17/10/2026, 3:50:57 p.m.  
> - **Duration:** 2m 16s  
> - **Exported:** 17/10/2026, 3:53:14 p.m.  

---

<sub>2s</sub>

### User

/crear-endpoint-taskflow Implementa la especificación specs/assignee.md. Síguela al pie de la letra: archivos, nombres, reglas y tests. Al terminar corre mvn -q test.

---

<sub>8s</sub>

### Copilot

Ejecutando la skill "crear-endpoint-taskflow" para implementar la especificación specs/assignee.md: creará DTO record, mapper, método en service, controller sin prefijo, tests unitario y slice (@WebMvcTest) en clases nuevas, y al final correrá mvn -q test para verificar. Ahora invocando la skill con ese objetivo.

---

<sub>8s</sub>

### `skill`

**crear-endpoint-taskflow**

<details>
<summary>Arguments</summary>

```json
{
  "skill": "crear-endpoint-taskflow"
}
```

</details>

```
Skill "crear-endpoint-taskflow" loaded successfully. Follow the instructions in the skill context.
```


---

<sub>12s</sub>

### Copilot

Leyendo la especificación specs/assignee.md y las plantillas del skill para implementar el endpoint exactamente como piden; estas lecturas ayudarán a crear DTO, mapper, service, controller y tests según las convenciones del proyecto. Ahora abriendo ambos archivos.

---

<sub>12s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\specs\assignee.md.**

C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\specs\assignee.md

<details>
<summary>73 lines</summary>

````
# Especificación — `PATCH /tasks/{id}/assignee`

## Qué

Un endpoint que cambia **solo** el responsable de una tarea. Hoy la única forma es `PUT /tasks/{id}`,
que obliga a mandar la tarea completa (título, prioridad, fecha…) para tocar un campo. Es el mismo
razonamiento de `PATCH /tasks/{id}/status`: un DTO chico por operación.

```
PATCH /tasks/4/assignee
Authorization: ******
Content-Type: application/json

{ "assigneeId": 2 }
```

## Reglas

1. La tarea no existe → **`404`** con el `ErrorResponse` uniforme (`TaskNotFoundException`), resuelto
   **como en la ruta vecina** `GET /tasks/{id}`:
   `taskService.buscarPorId(id).orElseThrow(() -> new TaskNotFoundException(id))`.
2. La tarea está `DONE` → **`422`** con el `ErrorResponse` uniforme y el mensaje
   `No se puede reasignar una tarea terminada.`. Se lanza `TaskStateException`, la misma que usa
   `cambiarStatus` para su 422. Una tarea terminada conserva a quien la terminó.
3. El cuerpo no trae `assigneeId`, lo trae en `null` o trae un número menor que 1 → **`400`** por
   Bean Validation (`@NotNull` y `@Positive` en el DTO, `@Valid` en el controller). El
   `GlobalExceptionHandler` ya responde ese 400 con el detalle por campo en `errors`.
4. En cualquier otro caso: se asigna el responsable con `Task.setAssigneeId`, se guarda con el
   repositorio y responde **`200`** con el `TaskResponse` actualizado (vía `TaskMapper.aResponse`).
5. Seguridad: igual que el resto de `/tasks` (sin token → `401`). No cambies `SecurityConfig`.

**Fuera de alcance** (no lo implementes aunque parezca buena idea):

- Comprobar que el usuario `assigneeId` existe. Exigiría inyectar `UserRepository` en `TaskService`,
  y los tests existentes de `TaskService` no lo conocen.
- Quitar el responsable (`assigneeId: null`). Hoy es un 400 a propósito.

## Dónde

- DTO de entrada nuevo en `com.taskflow.dto`:
  `public record TaskAssigneeUpdateRequest(@NotNull @Positive Long assigneeId)`, con su Javadoc, en la
  misma forma que `TaskStatusUpdateRequest`.
- **Sin mapper nuevo**: la respuesta es el `TaskResponse` de siempre.
- Método nuevo en `TaskService`: `public Task reasignar(Task tarea, Long assigneeId)`. Recibe la
  tarea **ya encontrada** (el 404 lo resuelve el controller) y lanza `TaskStateException` si está
  `DONE`.
- Endpoint nuevo en `TaskController`: `@PatchMapping("/tasks/{id}/assignee")`, con
  `@PathVariable("id") Long id` y `@Valid @RequestBody TaskAssigneeUpdateRequest request`.

## Tests que deben existir al terminar

Los dos en **clases nuevas** (las que ya existen no se tocan):

- **Unit** `src/test/java/com/taskflow/unit/ReasignarTareaServiceTest.java`, con
  `@Mock TaskRepository` e `@InjectMocks TaskService`:
  - tarea `TODO` sin responsable → se llama a `save` con la tarea ya con el `assigneeId` pedido
    (compruébalo con `ArgumentCaptor`, no con lo que devuelve el mock de `save`);
  - tarea `DONE` → `TaskStateException` y `save` **nunca** se llama.
- **Slice** `src/test/java/com/taskflow/slice/ReasignarTareaControllerTest.java`, con
  `@WebMvcTest(TaskController.class)`, `@AutoConfigureMockMvc(addFilters = false)` y `@MockitoBean`
  de `TaskService`, `ProjectService` y `JwtAuthenticationFilter`:
  - `200` y `$.assigneeId` = el pedido;
  - `422` cuando `reasignar` lanza `TaskStateException`;
  - `404` cuando `buscarPorId` devuelve `Optional.empty()`;
  - `400` con el cuerpo `{}`, y `reasignar` **nunca** se llama (`verify(taskService, never())`).

## Resultado esperado con la semilla

Con la app recién arrancada con el perfil `h2` y el token de `ana`, **en este orden** (el paso 3
depende del 1):

| # | Petición | Resultado |
|---|---|---|
| 1 | `PATCH /tasks/4/assignee` con `{"assigneeId": 2}` | `200`, `assigneeId` = 2 (la tarea 4 no tenía responsable) |
| 2 | `GET /tasks/4` | `assigneeId` = 2: se guardó, no solo se respondió |
| 3 | `PATCH /tasks/4/status` con `{"status": "DONE"}` | `200`. Antes del paso 1 respondía `422` («sin responsable»): la regla de `Task.setStatus` ahora deja pasar |
| 4 | `PATCH /tasks/2/assignee` con `{"assigneeId": 3}` | `422` con `message` = `No se puede reasignar una tarea terminada.` (la 2 es `DONE`) |
| 5 | `PATCH /tasks/99/assignee` con `{"assigneeId": 2}` | `404` |
| 6 | `PATCH /tasks/6/assignee` con `{}` | `400`, y en `errors` aparece `assigneeId` |
| 7 | `PATCH /tasks/6/assignee` con `{"assigneeId": 0}` | `400` |
| 8 | el paso 1 sin token | `401` |

## Restricciones para el agente

- No modifiques, borres ni desactives ningún test existente.
- No toques archivos fuera de `TaskAssigneeUpdateRequest.java` (nuevo), `TaskService.java`,
  `TaskController.java` y las dos clases de test nuevas.
- Al terminar, `mvn -q test` tiene que pasar completo.

El pull request del proyecto final lleva además `semana6/`, `.github/skills/verificar-taskflow/casos-assignee.ps1`
y una línea nueva en `verificar.ps1`. Esos archivos los agregas tú en el proceso (PF-2 a PF-7 de la guía),
no el agente: no cuentan contra esta restricción.
````

</details>


---

<sub>17s</sub>

### Copilot

Leer archivos clave para implementar el endpoint: buscar TaskService, TaskController, Task, TaskRepository, TaskResponse y TaskStatusUpdateRequest para seguir convenciones y reutilizar excepciones. Ejecutando búsqueda profunda del código relevante.

---

<sub>17s</sub>

### search_code_subagent (Completed)

### `grep_search`

**TaskService**

<details>
<summary>Arguments</summary>

```json
{
  "query": "TaskService",
  "isRegexp": false
}
```

</details>

<details>
<summary>63 lines</summary>

````
83 matches (more results are available)
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/copilot-instructions.md" line=11>
    están en el código (mezcla de inglés y español: `TaskService.cambiarStatus()`, `Task.estaVencida()`).
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/copilot-instructions.md" line=19>
    mvn -q test "-Dtest=TaskServiceTest"                    # una sola clase de test
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/skills/crear-endpoint-taskflow/SKILL.md" line=33>
    | 3 | Service | el service que el controller **ya inyecta** (`ProjectService` para `/projects`, `TaskService` para `/tasks`) | la lógica (contar, filtrar, ordenar), sin nada de HTTP. Recibe la entidad ya encontrada (`Project`) y usa los repositorios que ya tiene |
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/README.md" line=60>
    | No se pasa a `DONE` sin responsable | `Task.setStatus` — la regla vive en el **dominio**; `TaskService` la traduce | `422` |
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docs/ARQUITECTURA.md" line=94>
    3. Si existe, el controlador delega a `TaskService.crear` (`src\main\java\com\taskflow\service\TaskService.java`), pasando el `TaskRequest` y el `projectId` del path.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docs/ARQUITECTURA.md" line=95>
    4. Dentro de `TaskService.crear`, se invoca `TaskMapper.aEntidadNueva` (`src\main\java\com\taskflow\mapper\TaskMapper.java`) que convierte el DTO en entidad nueva mediante la fábrica de dominio (`Task.crear`) aplicando las reglas (status=TODO, validación de dueDate).
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docs/ARQUITECTURA.md" line=184>
    tarea como terminada si no tiene `assigneeId`. `TaskService.cambiarStatus(...)` convierte esa
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/overdue.md" line=20>
    - Método nuevo en `TaskService`: `public List<Task> vencidas()`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/overdue.md" line=26>
    - Unit en `TaskServiceTest`: devuelve solo vencidas y en orden; una tarea `DONE` con fecha pasada NO
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/overdue.md" line=39>
    - No toques archivos fuera de `TaskService.java`, `TaskController.java`, `TaskServiceTest.java` y
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/unassigned.md" line=20>
    - Método nuevo en `TaskService`: `public List<Task> sinResponsable()`. No lo confundas con
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/unassigned.md" line=28>
    - Unit en `TaskServiceTest`, dentro de un `@Nested` llamado `SinResponsable`:
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/unassigned.md" line=36>
    `assigneeId` (`null`) de lo que devuelve `taskService.sinResponsable()`. **No pruebes el orden en el
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/unassigned.md" line=47>
    - No toques archivos fuera de `TaskService.java`, `TaskController.java`, `TaskServiceTest.java` y
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia1/verificador.txt" line=46>
    [OK]         línea 94   miembro   TaskService.crear  ->  src\main\java\com\taskflow\service\TaskService.java
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia1/verificador.txt" line=47>
    [OK]         línea 94   ruta      src\main\java\com\taskflow\service\TaskService.java  ->  src\main\java\com\taskflow\service\TaskService.java
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia1/verificador.txt" line=77>
    [OK]         línea 184  miembro   TaskService.cambiarStatus  ->  src\main\java\com\taskflow\service\TaskService.java
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=21>
    `taskService.buscarPorId(id).orElseThrow(() -> new TaskNotFoundException(id))`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=34>
    - Comprobar que el usuario `assigneeId` existe. Exigiría inyectar `UserRepository` en `TaskService`,
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=35>
    y los tests existentes de `TaskService` no lo conocen.
</match>
```
````

</details>


### `grep_search`

**TaskController**

<details>
<summary>Arguments</summary>

```json
{
  "query": "TaskController",
  "isRegexp": false
}
```

</details>

<details>
<summary>63 lines</summary>

````
65 matches (more results are available)
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/unassigned.md" line=22>
    - Endpoint nuevo en `TaskController`: `@GetMapping("/tasks/unassigned")`, junto a `GET /tasks/overdue`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/unassigned.md" line=35>
    - Slice en `TaskControllerTest`: `GET /tasks/unassigned` responde `200` y el JSON trae el `id` y el
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/unassigned.md" line=47>
    - No toques archivos fuera de `TaskService.java`, `TaskController.java`, `TaskServiceTest.java` y
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/unassigned.md" line=48>
    `TaskControllerTest.java`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docs/ARQUITECTURA.md" line=73>
    ### `TaskController`
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docs/ARQUITECTURA.md" line=75>
    `TaskController` administra las tareas. Una tarea se crea dentro de un proyecto, aunque después se
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docs/ARQUITECTURA.md" line=92>
    1. El `POST` llega al controlador `TaskController` (`src\main\java\com\taskflow\controller\TaskController.java`).
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/overdue.md" line=21>
    - Endpoint nuevo en `TaskController`: `@GetMapping("/tasks/overdue")`. Spring la elige antes que `GET /tasks/{id}` sin importar el orden de declaración: no escribas comentarios sobre ese orden (hoy `GET /tasks/overdue` responde `400` con «El
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/overdue.md" line=28>
    - Slice en `TaskControllerTest`: `GET /tasks/overdue` responde `200` y el JSON trae el id y el title de las tareas que devuelve el servicio (el orden se prueba en el unit).
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/overdue.md" line=39>
    - No toques archivos fuera de `TaskService.java`, `TaskController.java`, `TaskServiceTest.java` y
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/overdue.md" line=40>
    `TaskControllerTest.java`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia2/checklist-overdue.txt" line=2>
    .../com/taskflow/controller/TaskController.java    |  9 +++++
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia2/checklist-overdue.txt" line=4>
    .../com/taskflow/slice/TaskControllerTest.java     | 21 +++++++++++
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia2/checklist-overdue.txt" line=7>
    21	0	src/test/java/com/taskflow/slice/TaskControllerTest.java
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=47>
    - Endpoint nuevo en `TaskController`: `@PatchMapping("/tasks/{id}/assignee")`, con
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=60>
    `@WebMvcTest(TaskController.class)`, `@AutoConfigureMockMvc(addFilters = false)` y `@MockitoBean`
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=87>
    `TaskController.java` y las dos clases de test nuevas.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia1/verificador.txt" line=32>
    [OK]         línea 73   clase     TaskController  ->  src\main\java\com\taskflow\controller\TaskController.java
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia1/verificador.txt" line=33>
    [OK]         línea 76   endpoint  /tasks  ->  GET en src\main\java\com\taskflow\controller\TaskController.java
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia1/verificador.txt" line=34>
    [OK]         línea 80   endpoint  GET /tasks  ->  GET en src\main\java\com\taskflow\controller\TaskController.java
</match>
```
````

</details>


### `grep_search`

**TaskRepository**

<details>
<summary>Arguments</summary>

```json
{
  "query": "TaskRepository",
  "isRegexp": false
}
```

</details>

<details>
<summary>63 lines</summary>

````
228 matches (more results are available)
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/README.md" line=186>
    | `mvn test -Ddocker.tests=true` | **70**: añade `TaskRepositoryPostgresIT`, el mismo test de repositorio contra un Postgres 16 real y efímero (Testcontainers). Requiere Docker levantado |
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/pom.xml" line=121>
    TESTCONTAINERS — SOLO para TaskRepositoryPostgresIT (perfil docker-it, más abajo):
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/pom.xml" line=122>
    el @DataJpaTest de TaskRepositoryTest contra un Postgres REAL efímero, no H2. El BOM de
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docs/ARQUITECTURA.md" line=96>
    5. La entidad resultante se persiste con `TaskRepository.save` (`src\main\java\com\taskflow\repository\TaskRepository.java`), que asigna el `id` por la base de datos.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=55>
    `@Mock TaskRepository` e `@InjectMocks TaskService`:
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=362>
    +   - `TaskRepository.findByProjectId(id)`, `ProjectRepository.findById(id)`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=459>
    +`ProjectService` ya tiene `taskRepository`: no hace falta inyectar nada nuevo. Recibe el proyecto ya
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=468>
    +        List<Task> tareas = taskRepository.findByProjectId(proyecto.getId());
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=528>
    +import com.taskflow.repository.TaskRepository;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=551>
    +    private TaskRepository taskRepository;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=563>
    +        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=572>
    +        when(taskRepository.findByProjectId(1L)).thenReturn(List.of());
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1156>
    import com.taskflow.repository.TaskRepository;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1168>
    tareasDe(id).forEach(t -> taskRepository.deleteById(t.getId()));   // cascada manual (la FK obliga el orden)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1177>
    +        List<Task> tareas = taskRepository.findByProjectId(proyecto.getId());
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1272>
    +import com.taskflow.repository.TaskRepository;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1296>
    +    private TaskRepository taskRepository;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1309>
    +        when(taskRepository.findByProjectId(2L)).thenReturn(List.of(
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1324>
    +        when(taskRepository.findByProjectId(3L)).thenReturn(List.of());
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1433>
    import com.taskflow.repository.TaskRepository;
</match>
```
````

</details>


### `grep_search`

**Task**

<details>
<summary>Arguments</summary>

```json
{
  "query": "Task",
  "isRegexp": false
}
```

</details>

<details>
<summary>63 lines</summary>

````
3767 matches (more results are available)
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.env.example" line=12>
    POSTGRES_USER=taskflow
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.env.example" line=13>
    POSTGRES_PASSWORD=taskflow
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.env.example" line=14>
    POSTGRES_DB=taskflow
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.project" line=3>
    <name>taskflow-api</name>
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docker-compose.yml" line=1>
    # docker-compose.yml de TaskFlow — api + Postgres.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docker-compose.yml" line=16>
    image: taskflow-api            # nombra la imagen (sin esto sería '<carpeta>-api');
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docker-compose.yml" line=17>
    # queda un nombre fijo y predecible: 'taskflow-api'
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docker-compose.yml" line=22>
    DB_USER: ${POSTGRES_USER:-taskflow}          # el yml documenta ${DB_USER}; aquí se inyecta
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docker-compose.yml" line=23>
    DB_PASSWORD: ${POSTGRES_PASSWORD:-taskflow}
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docker-compose.yml" line=27>
    JWT_SECRET: ${JWT_SECRET:-cambiame-en-produccion-taskflow-jwt-secret-de-64-caracteres-min!}
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docker-compose.yml" line=28>
    # DB_HOST/DB_PORT/DB_NAME NO se ponen: los defaults del yml (db:5432/taskflow)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docker-compose.yml" line=38>
    POSTGRES_USER: ${POSTGRES_USER:-taskflow}
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docker-compose.yml" line=39>
    POSTGRES_PASSWORD: ${POSTGRES_PASSWORD:-taskflow}
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docker-compose.yml" line=40>
    POSTGRES_DB: ${POSTGRES_DB:-taskflow}
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docker-compose.yml" line=48>
    test: ["CMD-SHELL", "pg_isready -U ${POSTGRES_USER:-taskflow} -d ${POSTGRES_DB:-taskflow}"]
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.vscode/mcp.json" line=3>
    "taskflow": {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.vscode/mcp.json" line=6>
    "args": ["-jar", "${workspaceFolder}/taskflow-mcp/target/taskflow-mcp.jar"],
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.vscode/mcp.json" line=8>
    "TASKFLOW_URL": "http://localhost:8080"
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/copilot-instructions.md" line=1>
    # Instrucciones de Copilot para TaskFlow API
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/copilot-instructions.md" line=11>
    están en el código (mezcla de inglés y español: `TaskService.cambiarStatus()`, `Task.estaVencida()`).
</match>
```
````

</details>


### `grep_search`

**TaskResponse**

<details>
<summary>Arguments</summary>

```json
{
  "query": "TaskResponse",
  "isRegexp": false
}
```

</details>

<details>
<summary>63 lines</summary>

````
63 matches (more results are available)
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/postman/taskflow-api-v2.postman_collection.json" line=283>
    "description": "Tareas de un proyecto (TaskResponse). Inexistente -> 404; existente sin tareas (prueba /projects/3/tasks) -> 200 con []."
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/postman/taskflow-api-v2.postman_collection.json" line=341>
    "description": "Todas las tareas, ordenadas por urgencia (TaskResponse)."
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/postman/taskflow-api-v2.postman_collection.json" line=385>
    "description": "Una tarea por id. 200 con TaskResponse si existe."
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/unassigned.md" line=14>
    4. Respuesta: `200` con una lista de `TaskResponse` (vía `TaskMapper.aResponse`). Si no hay tareas sin
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/tester-sesion.md" line=1169>
    import com.taskflow.dto.TaskResponse;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/tester-sesion.md" line=1486>
    import com.taskflow.dto.TaskResponse;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/tester-sesion.md" line=1515>
    * (ProjectRequest / ProjectResponse); las tareas de un proyecto salen como TaskResponse.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/overdue.md" line=14>
    3. Respuesta: `200` con una lista de `TaskResponse` (el mismo DTO que `GET /tasks`, vía
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=29>
    repositorio y responde **`200`** con el `TaskResponse` actualizado (vía `TaskMapper.aResponse`).
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=43>
    - **Sin mapper nuevo**: la respuesta es el `TaskResponse` de siempre.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1050>
    import com.taskflow.dto.TaskResponse;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1367>
    import com.taskflow.dto.TaskResponse;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1396>
    * (ProjectRequest / ProjectResponse); las tareas de un proyecto salen como TaskResponse.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia1/verificador.txt" line=37>
    [OK]         línea 82   clase     TaskResponse  ->  src\main\java\com\taskflow\dto\TaskResponse.java
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/taskflow-mcp/src/main/java/com/taskflow/mcp/Tarea.java" line=6>
    * Una tarea tal como la devuelve la API de TaskFlow (TaskResponse de taskflow-api).
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=928>
    import com.taskflow.dto.TaskResponse;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1245>
    import com.taskflow.dto.TaskResponse;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1274>
    * (ProjectRequest / ProjectResponse); las tareas de un proyecto salen como TaskResponse.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1310>
    * GET /projects/{id}/tasks — las tareas de un proyecto como TaskResponse. Distinción clave:
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1315>
    description = "Tareas del proyecto como TaskResponse; 404 si el proyecto no existe, 200 con [] si no tiene tareas. Filtro opcional ?status=.")
</match>
```
````

</details>


### `grep_search`

**TaskStatusUpdateRequest**

<details>
<summary>Arguments</summary>

```json
{
  "query": "TaskStatusUpdateRequest",
  "isRegexp": false
}
```

</details>

<details>
<summary>18 lines</summary>

````
5 matches
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=42>
    misma forma que `TaskStatusUpdateRequest`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/TaskController.java" line=5>
    import com.taskflow.dto.TaskStatusUpdateRequest;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/TaskController.java" line=152>
    @Valid @RequestBody TaskStatusUpdateRequest request) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskStatusUpdateRequest.java" line=7>
    * TaskStatusUpdateRequest — el cuerpo del PATCH /tasks/{id}/status. Un DTO por OPERACIÓN: el
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskStatusUpdateRequest.java" line=15>
    public record TaskStatusUpdateRequest(
</match>
```
````

</details>


### `grep_search`

**TaskNotFoundException**

<details>
<summary>Arguments</summary>

```json
{
  "query": "TaskNotFoundException",
  "isRegexp": false
}
```

</details>

<details>
<summary>63 lines</summary>

````
26 matches (more results are available)
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docs/ARQUITECTURA.md" line=160>
    | `404 Not Found` | `TaskNotFoundException` o `ProjectNotFoundException`. |
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/README.md" line=63>
    | Una tarea que no existe se dice claro | `TaskNotFoundException` → el `@RestControllerAdvice` | `404` |
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia1/verificador.txt" line=70>
    [OK]         línea 160  clase     TaskNotFoundException  ->  src\main\java\com\taskflow\exception\TaskNotFoundException.java
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=19>
    1. La tarea no existe → **`404`** con el `ErrorResponse` uniforme (`TaskNotFoundException`), resuelto
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=21>
    `taskService.buscarPorId(id).orElseThrow(() -> new TaskNotFoundException(id))`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskServiceTest.java" line=4>
    import com.taskflow.exception.TaskNotFoundException;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskServiceTest.java" line=112>
    void cambiarStatus_idInexistente_lanzaTaskNotFoundException() {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskServiceTest.java" line=115>
    assertThrows(TaskNotFoundException.class,
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskServiceTest.java" line=151>
    void eliminar_inexistente_lanzaTaskNotFoundExceptionYNoBorra() {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskServiceTest.java" line=154>
    assertThrows(TaskNotFoundException.class, () -> service.eliminar(999L));
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskControllerTest.java" line=4>
    import com.taskflow.exception.TaskNotFoundException;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskControllerTest.java" line=174>
    doThrow(new TaskNotFoundException(999L)).when(taskService).eliminar(999L);
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/advice/GlobalExceptionHandler.java" line=5>
    import com.taskflow.exception.TaskNotFoundException;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/advice/GlobalExceptionHandler.java" line=45>
    @ExceptionHandler({TaskNotFoundException.class, ProjectNotFoundException.class})
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/TaskController.java" line=7>
    import com.taskflow.exception.TaskNotFoundException;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/TaskController.java" line=99>
    .orElseThrow(() -> new TaskNotFoundException(id));
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java" line=4>
    import com.taskflow.exception.TaskNotFoundException;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java" line=58>
    .orElseThrow(() -> new TaskNotFoundException(id));
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java" line=69>
    .orElseThrow(() -> new TaskNotFoundException(id));
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java" line=80>
    .orElseThrow(() -> new TaskNotFoundException(id));
</match>
```
````

</details>


### `grep_search`

**TaskStateException**

<details>
<summary>Arguments</summary>

```json
{
  "query": "TaskStateException",
  "isRegexp": false
}
```

</details>

<details>
<summary>63 lines</summary>

````
27 matches (more results are available)
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia1/verificador.txt" line=72>
    [OK]         línea 162  clase     TaskStateException  ->  src\main\java\com\taskflow\exception\TaskStateException.java
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docs/ARQUITECTURA.md" line=162>
    | `422 Unprocessable Entity` | `TaskStateException` cuando el estado del dominio impide una operación, como marcar `DONE` una tarea sin responsable. |
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docs/ARQUITECTURA.md" line=185>
    violación de estado en `TaskStateException`, que termina como `422 Unprocessable Entity`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/TaskController.java" line=146>
    * 422 si la regla de estado lo prohíbe (DONE sin assignee), vía TaskStateException.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/advice/GlobalExceptionHandler.java" line=6>
    import com.taskflow.exception.TaskStateException;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/advice/GlobalExceptionHandler.java" line=86>
    * de setStatus a esta TaskStateException. Es la distinción de diseño frente al 400.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/advice/GlobalExceptionHandler.java" line=88>
    @ExceptionHandler(TaskStateException.class)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/advice/GlobalExceptionHandler.java" line=89>
    public ResponseEntity<ErrorResponse> handleState(TaskStateException ex) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskServiceTest.java" line=5>
    import com.taskflow.exception.TaskStateException;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskServiceTest.java" line=122>
    void cambiarStatus_sinAssignee_lanzaTaskStateExceptionYNoGuarda() {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskServiceTest.java" line=123>
    // Sin assignee, setStatus(DONE) lanza la checked; cambiarStatus la TRADUCE a TaskStateException.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskServiceTest.java" line=128>
    assertThrows(TaskStateException.class,
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskControllerTest.java" line=5>
    import com.taskflow.exception.TaskStateException;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskControllerTest.java" line=160>
    // El service (mockeado) traduce la regla a TaskStateException; el advice la mapea a 422.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskControllerTest.java" line=162>
    .thenThrow(new TaskStateException("No se puede marcar DONE sin responsable."));
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/exception/TaskValidationException.java" line=10>
    * cambiarStatus la convierte en TaskStateException -> 422).
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java" line=5>
    import com.taskflow.exception.TaskStateException;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java" line=27>
    *     TaskStateException -> el advice la mapea a 422. Sin tocar el dominio.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java" line=76>
    * a la unchecked TaskStateException -> el advice la mapea a 422. El dominio (setStatus) no se toca.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java" line=84>
    throw new TaskStateException(e.getMessage());
</match>
```
````

</details>


### `grep_search`

**setAssigneeId**

<details>
<summary>Arguments</summary>

```json
{
  "query": "setAssigneeId",
  "isRegexp": false
}
```

</details>

<details>
<summary>21 lines</summary>

````
6 matches
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1820>
    public void setAssigneeId(Long assigneeId) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1787>
    public void setAssigneeId(Long assigneeId) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/summary-sesion.md" line=1817>
    public void setAssigneeId(Long assigneeId) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/tester-sesion.md" line=1907>
    public void setAssigneeId(Long assigneeId) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=28>
    4. En cualquier otro caso: se asigna el responsable con `Task.setAssigneeId`, se guarda con el
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/model/Task.java" line=223>
    public void setAssigneeId(Long assigneeId) {
</match>
```
````

</details>


### `grep_search`

**buscarPorId**

<details>
<summary>Arguments</summary>

```json
{
  "query": "buscarPorId",
  "isRegexp": false
}
```

</details>

<details>
<summary>63 lines</summary>

````
91 matches (more results are available)
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/skills/crear-endpoint-taskflow/plantillas.md" line=70>
    Project proyecto = projectService.buscarPorId(id)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/skills/crear-endpoint-taskflow/plantillas.md" line=187>
    when(projectService.buscarPorId(1L)).thenReturn(Optional.of(new Project(1L, "Plataforma", "d", 1L, null)));
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/skills/crear-endpoint-taskflow/plantillas.md" line=199>
    when(projectService.buscarPorId(99L)).thenReturn(Optional.empty());
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/skills/crear-endpoint-taskflow/SKILL.md" line=34>
    | 4 | Controller | el controller del recurso | ruta **sin** `/api` (`/projects/{id}/...`), `@PathVariable("id")` con el nombre escrito, `@Operation(summary, description)` en español, `@Valid` en cada `@RequestBody`, devuelve el DTO (nunca la entidad). El 404 se resuelve **como en la ruta vecina** `GET /projects/{id}/tasks`: `projectService.buscarPorId(id).orElseThrow(() -> new ProjectNotFoundException(id))`; el `GlobalExceptionHandler` lo convierte en 404 |
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/skills/crear-endpoint-taskflow/SKILL.md" line=51>
    `when(projectService.buscarPorId(99L)).thenReturn(Optional.empty())`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docs/ARQUITECTURA.md" line=93>
    2. El controlador comprueba que el proyecto exista llamando a `ProjectService.buscarPorId` (`src\main\java\com\taskflow\service\ProjectService.java`) y lanza `ProjectNotFoundException` (404) si no existe.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia1/verificador.txt" line=43>
    [OK]         línea 93   miembro   ProjectService.buscarPorId  ->  src\main\java\com\taskflow\service\ProjectService.java
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=371>
    +| 4 | Controller | el controller del recurso | ruta **sin** `/api` (`/projects/{id}/...`), `@PathVariable("id")` con el nombre escrito, `@Operation(summary, description)` en español, `@Valid` en cada `@RequestBody`, devuelve el DTO (nunca la entidad). El 404 se resuelve **como en la ruta vecina** `GET /projects/{id}/tasks`: `projectService.buscarPorId(id).orElseThrow(() -> new ProjectNotFoundException(id))`; el `GlobalExceptionHandler` lo convierte en 404 |
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=388>
    +  `when(projectService.buscarPorId(99L)).thenReturn(Optional.empty())`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=508>
    +        Project proyecto = projectService.buscarPorId(id)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=625>
    +        when(projectService.buscarPorId(1L)).thenReturn(Optional.of(new Project(1L, "Plataforma", "d", 1L, null)));
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=658>
    +        when(projectService.buscarPorId(99L)).thenReturn(Optional.empty());
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1063>
    +        Project proyecto = projectService.buscarPorId(id)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1233>
    +        when(projectService.buscarPorId(2L)).thenReturn(Optional.of(new Project(2L, "App Móvil", "d", 2L, null)));
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1250>
    +        when(projectService.buscarPorId(99L)).thenReturn(Optional.empty());
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1474>
    public Optional<Project> buscarPorId(Long id) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1482>
    * controller con buscarPorId; ESTE método, si el proyecto existe pero no tiene tareas, devuelve
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/summary.diff" line=151>
    +| 4 | Controller | el controller del recurso | ruta **sin** `/api` (`/projects/{id}/...`), `@PathVariable("id")` con el nombre escrito, `@Operation(summary, description)` en español, `@Valid` en cada `@RequestBody`, devuelve el DTO (nunca la entidad). El 404 se resuelve **como en la ruta vecina** `GET /projects/{id}/tasks`: `projectService.buscarPorId(id).orElseThrow(() -> new ProjectNotFoundException(id))`; el `GlobalExceptionHandler` lo convierte en 404 |
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/summary.diff" line=168>
    +  `when(projectService.buscarPorId(99L)).thenReturn(Optional.empty())`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/summary.diff" line=264>
    +        Project proyecto = projectService.buscarPorId(id)
</match>
```
````

</details>


### `grep_search`

**reasignar**

<details>
<summary>Arguments</summary>

```json
{
  "query": "reasignar",
  "isRegexp": false
}
```

</details>

<details>
<summary>24 lines</summary>

````
7 matches
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=23>
    `No se puede reasignar una tarea terminada.`. Se lanza `TaskStateException`, la misma que usa
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=44>
    - Método nuevo en `TaskService`: `public Task reasignar(Task tarea, Long assigneeId)`. Recibe la
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=54>
    - **Unit** `src/test/java/com/taskflow/unit/ReasignarTareaServiceTest.java`, con
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=59>
    - **Slice** `src/test/java/com/taskflow/slice/ReasignarTareaControllerTest.java`, con
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=63>
    - `422` cuando `reasignar` lanza `TaskStateException`;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=65>
    - `400` con el cuerpo `{}`, y `reasignar` **nunca** se llama (`verify(taskService, never())`).
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=77>
    | 4 | `PATCH /tasks/2/assignee` con `{"assigneeId": 3}` | `422` con `message` = `No se puede reasignar una tarea terminada.` (la 2 es `DONE`) |
</match>
```
````

</details>


### `file_search`

****/Task*.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/Task*.java"
}
```

</details>

<details>
<summary>21 lines</summary>

```
26 files found (showing first 20):
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/TaskController.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskRequest.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskResponse.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskStatusUpdateRequest.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/exception/TaskNotFoundException.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/exception/TaskStateException.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/exception/TaskValidationException.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/mapper/TaskMapper.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/model/Task.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/model/TaskStatus.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/repository/TaskRepository.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskOrders.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/TaskflowApiApplication.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/integration/TaskflowApiApplicationTests.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/integration/TaskRepositoryPostgresIT.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskControllerTest.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskRepositoryTest.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskServiceTest.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskValidationTest.java
```

</details>


### `file_search`

****/dto/**/*.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/dto/**/*.java"
}
```

</details>

<details>
<summary>12 lines</summary>

```
11 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/auth/AuthResponse.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/auth/LoginRequest.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/auth/RegisterRequest.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/auth/UserResponse.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/ErrorResponse.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/ProjectRequest.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/ProjectResponse.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/ProjectSummaryResponse.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskRequest.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskResponse.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskStatusUpdateRequest.java
```

</details>


### `file_search`

****/controller/**/*.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/controller/**/*.java"
}
```

</details>

```
4 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/AuthController.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/InfoController.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/ProjectController.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/TaskController.java
```


### `file_search`

****/service/**/*.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/service/**/*.java"
}
```

</details>

<details>
<summary>7 lines</summary>

```
6 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/AuthService.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/JpaUserDetailsService.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/ProjectService.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/ReportService.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskOrders.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java
```

</details>


### `file_search`

****/repository/**/*.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/repository/**/*.java"
}
```

</details>

```
3 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/repository/ProjectRepository.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/repository/TaskRepository.java
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/repository/UserRepository.java
```


### `file_search`

****/TaskService.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/TaskService.java"
}
```

</details>

```
1 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java
```


### `file_search`

****/TaskController.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/TaskController.java"
}
```

</details>

```
1 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/TaskController.java
```


### `file_search`

****/TaskRepository.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/TaskRepository.java"
}
```

</details>

```
1 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/repository/TaskRepository.java
```


### `file_search`

****/Task.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/Task.java"
}
```

</details>

```
1 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/model/Task.java
```


### `file_search`

****/TaskResponse.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/TaskResponse.java"
}
```

</details>

```
1 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskResponse.java
```


### `file_search`

****/TaskStatusUpdateRequest.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/TaskStatusUpdateRequest.java"
}
```

</details>

```
1 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskStatusUpdateRequest.java
```


### `file_search`

****/TaskNotFoundException.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/TaskNotFoundException.java"
}
```

</details>

```
1 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/exception/TaskNotFoundException.java
```


### `file_search`

****/TaskStateException.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/TaskStateException.java"
}
```

</details>

```
1 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/exception/TaskStateException.java
```


### `grep_search`

**buscarPorId**

<details>
<summary>Arguments</summary>

```json
{
  "query": "buscarPorId",
  "isRegexp": false
}
```

</details>

<details>
<summary>63 lines</summary>

````
91 matches (more results are available)
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docs/ARQUITECTURA.md" line=93>
    2. El controlador comprueba que el proyecto exista llamando a `ProjectService.buscarPorId` (`src\main\java\com\taskflow\service\ProjectService.java`) y lanza `ProjectNotFoundException` (404) si no existe.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=21>
    `taskService.buscarPorId(id).orElseThrow(() -> new TaskNotFoundException(id))`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=64>
    - `404` cuando `buscarPorId` devuelve `Optional.empty()`;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia1/verificador.txt" line=43>
    [OK]         línea 93   miembro   ProjectService.buscarPorId  ->  src\main\java\com\taskflow\service\ProjectService.java
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=296>
    +| 4 | Controller | el controller del recurso | ruta **sin** `/api` (`/projects/{id}/...`), `@PathVariable("id")` con el nombre escrito, `@Operation(summary, description)` en español, `@Valid` en cada `@RequestBody`, devuelve el DTO (nunca la entidad). El 404 se resuelve **como en la ruta vecina** `GET /projects/{id}/tasks`: `projectService.buscarPorId(id).orElseThrow(() -> new ProjectNotFoundException(id))`; el `GlobalExceptionHandler` lo convierte en 404 |
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=313>
    +  `when(projectService.buscarPorId(99L)).thenReturn(Optional.empty())`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=409>
    +        Project proyecto = projectService.buscarPorId(id)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=526>
    +        when(projectService.buscarPorId(1L)).thenReturn(Optional.of(new Project(1L, "Plataforma", "d", 1L, null)));
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=559>
    +        when(projectService.buscarPorId(99L)).thenReturn(Optional.empty());
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=941>
    +        Project proyecto = projectService.buscarPorId(id)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1111>
    +        when(projectService.buscarPorId(2L)).thenReturn(Optional.of(new Project(2L, "App Móvil", "d", 2L, null)));
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1128>
    +        when(projectService.buscarPorId(99L)).thenReturn(Optional.empty());
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1304>
    Project proyecto = projectService.buscarPorId(id)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1320>
    projectService.buscarPorId(id)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1380>
    Project proyecto = projectService.buscarPorId(id)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1509>
    public Optional<Project> buscarPorId(Long id) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1517>
    * controller con buscarPorId; ESTE método, si el proyecto existe pero no tiene tareas, devuelve
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=371>
    +| 4 | Controller | el controller del recurso | ruta **sin** `/api` (`/projects/{id}/...`), `@PathVariable("id")` con el nombre escrito, `@Operation(summary, description)` en español, `@Valid` en cada `@RequestBody`, devuelve el DTO (nunca la entidad). El 404 se resuelve **como en la ruta vecina** `GET /projects/{id}/tasks`: `projectService.buscarPorId(id).orElseThrow(() -> new ProjectNotFoundException(id))`; el `GlobalExceptionHandler` lo convierte en 404 |
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=388>
    +  `when(projectService.buscarPorId(99L)).thenReturn(Optional.empty())`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=508>
    +        Project proyecto = projectService.buscarPorId(id)
</match>
```
````

</details>


### `grep_search`

**reasignar**

<details>
<summary>Arguments</summary>

```json
{
  "query": "reasignar",
  "isRegexp": false
}
```

</details>

<details>
<summary>24 lines</summary>

````
7 matches
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=23>
    `No se puede reasignar una tarea terminada.`. Se lanza `TaskStateException`, la misma que usa
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=44>
    - Método nuevo en `TaskService`: `public Task reasignar(Task tarea, Long assigneeId)`. Recibe la
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=54>
    - **Unit** `src/test/java/com/taskflow/unit/ReasignarTareaServiceTest.java`, con
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=59>
    - **Slice** `src/test/java/com/taskflow/slice/ReasignarTareaControllerTest.java`, con
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=63>
    - `422` cuando `reasignar` lanza `TaskStateException`;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=65>
    - `400` con el cuerpo `{}`, y `reasignar` **nunca** se llama (`verify(taskService, never())`).
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=77>
    | 4 | `PATCH /tasks/2/assignee` con `{"assigneeId": 3}` | `422` con `message` = `No se puede reasignar una tarea terminada.` (la 2 es `DONE`) |
</match>
```
````

</details>


### `grep_search`

**setAssigneeId**

<details>
<summary>Arguments</summary>

```json
{
  "query": "setAssigneeId",
  "isRegexp": false
}
```

</details>

<details>
<summary>21 lines</summary>

````
6 matches
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1820>
    public void setAssigneeId(Long assigneeId) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1787>
    public void setAssigneeId(Long assigneeId) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=28>
    4. En cualquier otro caso: se asigna el responsable con `Task.setAssigneeId`, se guarda con el
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/summary-sesion.md" line=1817>
    public void setAssigneeId(Long assigneeId) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/tester-sesion.md" line=1907>
    public void setAssigneeId(Long assigneeId) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/model/Task.java" line=223>
    public void setAssigneeId(Long assigneeId) {
</match>
```
````

</details>


### `file_search`

****/ReasignarTareaControllerTest.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/ReasignarTareaControllerTest.java"
}
```

</details>

```
No files found matching pattern: **/ReasignarTareaControllerTest.java
```


### `file_search`

****/ReasignarTareaServiceTest.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/ReasignarTareaServiceTest.java"
}
```

</details>

```
No files found matching pattern: **/ReasignarTareaServiceTest.java
```


### `file_search`

****/TaskControllerTest.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/TaskControllerTest.java"
}
```

</details>

```
1 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskControllerTest.java
```


### `file_search`

****/TaskServiceTest.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/TaskServiceTest.java"
}
```

</details>

```
1 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskServiceTest.java
```


### `file_search`

****/TaskRepositoryTest.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/TaskRepositoryTest.java"
}
```

</details>

```
1 files found:
  C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskRepositoryTest.java
```


### `read_file`

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java",
  "startLine": 1,
  "endLine": 400
}
```

</details>

<details>
<summary>131 lines</summary>

````
File: `C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java`. Lines 1 to 146 (146 lines total): 
```
package com.taskflow.service;

import com.taskflow.dto.TaskRequest;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.mapper.TaskMapper;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.springframework.stereotype.Service;

import java.util.List;
import java.util.Optional;

/**
 * TaskService — la capa de negocio de las tareas. Orquesta; las reglas viven en Task (título 3-120,
 * projectId obligatorio, dueDate no en el pasado al crear, no DONE sin assignee).
 *
 * Piezas clave:
 *   - crear(...) recibe el projectId del PATH (no un id fijo) y arma la entidad con el TaskMapper
 *     (que pasa por la factory Task.crear).
 *   - reemplazar (PUT), eliminar (DELETE) y cambiarStatus (PATCH); completar(...) DELEGA en
 *     cambiarStatus.
 *   - cambiarStatus TRADUCE la checked TaskValidationException que lanza setStatus a la unchecked
 *     TaskStateException -> el advice la mapea a 422. Sin tocar el dominio.
 *
 * La existencia del proyecto al crear (404) se valida en el controller (contra ProjectService): este
 * service no depende del repositorio de proyectos, y así el filtrado por proyecto vive solo en
 * ProjectService.tareasDe.
 */
@Service
public class TaskService {

    private final TaskRepository repository;

    public TaskService(TaskRepository repository) {
        this.repository = repository;
    }

    /**
     * Crea una tarea a partir del DTO de entrada y el projectId del PATH. El TaskMapper pasa por la
     * factory Task.crear (status=TODO, id=null, regla "dueDate no en el pasado"); el repositorio le
     * asigna el id en save. La checked sube al advice si el título o la fecha no cumplen (-> 400).
     */
    public Task crear(TaskRequest request, Long projectId) throws TaskValidationException {
        Task nueva = TaskMapper.aEntidadNueva(request, projectId);
        return repository.save(nueva);
    }

    /**
     * Reemplazo COMPLETO (PUT): busca la tarea (404 si no existe), y arma una tarea rehidratada con el
     * MISMO id, conservando status y projectId actuales; el resto viene del request. save hace upsert.
     */
    public Task reemplazar(Long id, TaskRequest request) throws TaskValidationException {
        Task actual = repository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        Task reemplazo = TaskMapper.aReemplazo(id, actual.getStatus(), actual.getProjectId(), request);
        return repository.save(reemplazo);
    }

    /**
     * Elimina una tarea (DELETE): existe -> borra (204 en el controller); no existe -> TaskNotFound
     * -> el advice responde 404. El findById previo hace explícito el "no existe" (vía Optional).
     */
    public void eliminar(Long id) {
        repository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        repository.deleteById(id);
    }

    /**
     * Cambia el estado (PATCH): busca la tarea (404 si no existe) y aplica setStatus, que puede lanzar
     * la CHECKED TaskValidationException por la regla "no DONE sin assignee". La CAPTURA y la TRADUCE
     * a la unchecked TaskStateException -> el advice la mapea a 422. El dominio (setStatus) no se toca.
     */
    public Task cambiarStatus(Long id, TaskStatus nuevoStatus) {
        Task task = repository.findById(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        try {
            task.setStatus(nuevoStatus);
        } catch (TaskValidationException e) {
            throw new TaskStateException(e.getMessage());
        }
        return repository.save(task);
    }

    /**
     * Completa una tarea (la pasa a DONE). DELEGA en cambiarStatus(id, DONE): un
     * solo camino para "cambiar de estado", que además hereda la traducción a 422.
     */
    public Task completar(Long id) {
        return cambiarStatus(id, TaskStatus.DONE);
    }

    /**
     * Lista todas las tareas ordenadas con la estrategia POR_URGENCIA (reuso LITERAL de la Strategy de
     * TaskOrders): vencidas primero, luego prioridad HIGH->LOW, luego fecha ascendente (nulls al final).
     */
    public List<Task> listar() {
        return repository.findAll().stream()
                .sorted(TaskOrders.POR_URGENCIA)
                .toList();
    }

    /** Filtra las tareas por estado (?status=). '==' entre enums es seguro. */
    public List<Task> porEstado(TaskStatus status) {
        return repository.findAll().stream()
                .filter(t -> t.getStatus() == status)
                .toList();
    }

    /** Busca una tarea por id: delega en el repositorio y deja subir el Optional TAL CUAL. */
    public Optional<Task> buscarPorId(Long id) {
        return repository.findById(id);
    }

    /** Filtro por prioridad (?priority=): MISMO patrón que porEstado. */
    public List<Task> porPrioridad(Priority priority) {
        return repository.findAll().stream()
                .filter(t -> t.getPriority() == priority)
                .toList();
    }

    /** Lista las tareas vencidas según Task.estaVencida() y ordenadas POR_FECHA. */
    public List<Task> vencidas() {
        return repository.findAll().stream()
                .filter(Task::estaVencida)
                .sorted(TaskOrders.POR_FECHA)
                .toList();
    }

    /**
     * Lista las tareas sin responsable (assigneeId == null) en todos los proyectos.
     * Filtra usando el predicado ReportService.SIN_ASIGNAR y ordena con TaskOrders.POR_FECHA.
     */
    public List<Task> sinResponsable() {
        return repository.findAll().stream()
                .filter(ReportService.SIN_ASIGNAR)
                .sorted(TaskOrders.POR_FECHA)
                .toList();
    }
}


```
````

</details>


### `read_file`

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/TaskController.java",
  "startLine": 1,
  "endLine": 400
}
```

</details>

<details>
<summary>146 lines</summary>

````
File: `C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/TaskController.java`. Lines 1 to 157 (157 lines total): 
```
package com.taskflow.controller;

import com.taskflow.dto.TaskRequest;
import com.taskflow.dto.TaskResponse;
import com.taskflow.dto.TaskStatusUpdateRequest;
import com.taskflow.exception.ProjectNotFoundException;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.mapper.TaskMapper;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.service.ProjectService;
import com.taskflow.service.TaskService;
import io.swagger.v3.oas.annotations.Operation;
import io.swagger.v3.oas.annotations.tags.Tag;
import jakarta.validation.Valid;
import org.springframework.http.ResponseEntity;
import org.springframework.web.bind.annotation.DeleteMapping;
import org.springframework.web.bind.annotation.GetMapping;
import org.springframework.web.bind.annotation.PatchMapping;
import org.springframework.web.bind.annotation.PathVariable;
import org.springframework.web.bind.annotation.PostMapping;
import org.springframework.web.bind.annotation.PutMapping;
import org.springframework.web.bind.annotation.RequestBody;
import org.springframework.web.bind.annotation.RequestParam;
import org.springframework.web.bind.annotation.RestController;
import org.springframework.web.servlet.support.ServletUriComponentsBuilder;

import java.net.URI;
import java.util.List;

/**
 * TaskController — la PUERTA HTTP de las tareas: CRUD completo, y la entidad Task NO sale por la
 * puerta: toda firma habla en DTOs (TaskRequest / TaskResponse).
 *
 * Contrato de responsabilidades: el controller traduce HTTP <-> dominio y delega. Además de
 * TaskService, inyecta ProjectService para el cableo Task↔Project (validar que el
 * proyecto exista al crear una tarea bajo su path).
 *
 * La checked TaskValidationException se declara con throws y SUBE al GlobalExceptionHandler (que la
 * mapea a 400). No se atrapa aquí porque tiene un handler global esperándola: una excepción se
 * maneja en la capa que PUEDE hacer algo con ella, y ésa es el advice.
 */
@RestController
@Tag(name = "Tasks", description = "CRUD de tareas: crear bajo un proyecto, leer, reemplazar, cambiar estado y borrar.")
public class TaskController {

    private final TaskService taskService;
    private final ProjectService projectService;

    public TaskController(TaskService taskService, ProjectService projectService) {
        this.taskService = taskService;
        this.projectService = projectService;
    }

    /** GET /tasks — todas o filtradas por ?status= / ?priority=. Devuelve TaskResponse. */
    @Operation(summary = "Lista tareas",
            description = "Todas las tareas, o filtradas por ?status= / ?priority=. Devuelve TaskResponse.")
    @GetMapping("/tasks")
    public List<TaskResponse> getTasks(
            @RequestParam(name = "status", required = false) TaskStatus status,
            @RequestParam(name = "priority", required = false) Priority priority) {
        List<Task> tareas;
        if (status != null) {
            tareas = taskService.porEstado(status);
        } else if (priority != null) {                 // mismo patrón que ?status=
            tareas = taskService.porPrioridad(priority);
        } else {
            tareas = taskService.listar();
        }
        return tareas.stream().map(TaskMapper::aResponse).toList();
    }

    /** GET /tasks/overdue — lista tareas vencidas de todos los proyectos. */
    @Operation(summary = "Lista tareas vencidas",
            description = "Lista las tareas vencidas de todos los proyectos (Task.estaVencida()).")
    @GetMapping("/tasks/overdue")
    public List<TaskResponse> getOverdueTasks() {
        List<Task> vencidas = taskService.vencidas();
        return vencidas.stream().map(TaskMapper::aResponse).toList();
    }

    /** GET /tasks/unassigned — lista tareas sin responsable de todos los proyectos. */
    @Operation(summary = "Lista tareas sin responsable",
            description = "Lista tareas sin responsable (assigneeId null) de todos los proyectos.")
    @GetMapping("/tasks/unassigned")
    public List<TaskResponse> getUnassignedTasks() {
        List<Task> sin = taskService.sinResponsable();
        return sin.stream().map(TaskMapper::aResponse).toList();
    }

    /** GET /tasks/{id} — 200 con TaskResponse, o 404 uniforme (orElseThrow -> advice). */
    @Operation(summary = "Obtiene una tarea por id",
            description = "200 con el TaskResponse; 404 uniforme si el id no existe.")
    @GetMapping("/tasks/{id}")
    public TaskResponse getTaskPorId(@PathVariable("id") Long id) {
        Task task = taskService.buscarPorId(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        return TaskMapper.aResponse(task);
    }

    /**
     * POST /projects/{projectId}/tasks — crea una tarea bajo un proyecto. 201 + header Location
     * apuntando a donde el recurso SE LEE (/tasks/{id}), no a la URL de creación. @Valid dispara Bean
     * Validation (400 si falla). Cableo: si el proyecto no existe -> 404 (ProjectNotFoundException).
     */
    @Operation(summary = "Crea una tarea dentro de un proyecto",
            description = "El proyecto debe existir (404 si no). La tarea nace en estado TODO.")
    @PostMapping("/projects/{projectId}/tasks")
    public ResponseEntity<TaskResponse> createTask(@PathVariable("projectId") Long projectId,
                                                   @Valid @RequestBody TaskRequest request)
            throws TaskValidationException {
        projectService.buscarPorId(projectId)
                .orElseThrow(() -> new ProjectNotFoundException(projectId));
        Task creada = taskService.crear(request, projectId);
        URI location = ServletUriComponentsBuilder.fromCurrentContextPath()
                .path("/tasks/{id}")
                .buildAndExpand(creada.getId())
                .toUri();
        return ResponseEntity.created(location).body(TaskMapper.aResponse(creada));
    }

    /** PUT /tasks/{id} — reemplazo COMPLETO. 200 con TaskResponse; 404 si no existe. */
    @Operation(summary = "Reemplaza una tarea por completo",
            description = "Semántica PUT: conserva id, status y projectId; el resto viene del cuerpo.")
    @PutMapping("/tasks/{id}")
    public TaskResponse updateTask(@PathVariable("id") Long id,
                                   @Valid @RequestBody TaskRequest request)
            throws TaskValidationException {
        Task actualizada = taskService.reemplazar(id, request);
        return TaskMapper.aResponse(actualizada);
    }

    /** DELETE /tasks/{id} — 204 No Content; 404 uniforme si no existe (advice). */
    @Operation(summary = "Borra una tarea",
            description = "204 No Content; 404 uniforme si la tarea no existe.")
    @DeleteMapping("/tasks/{id}")
    public ResponseEntity<Void> deleteTask(@PathVariable("id") Long id) {
        taskService.eliminar(id);
        return ResponseEntity.noContent().build();
    }

    /**
     * PATCH /tasks/{id}/status — cambia SOLO el estado. 200 con TaskResponse; 404 si no existe; y
     * 422 si la regla de estado lo prohíbe (DONE sin assignee), vía TaskStateException.
     */
    @Operation(summary = "Cambia el estado de una tarea",
            description = "TODO/IN_PROGRESS/DONE. Pasar a DONE sin responsable responde 422.")
    @PatchMapping("/tasks/{id}/status")
    public TaskResponse patchStatus(@PathVariable("id") Long id,
                                    @Valid @RequestBody TaskStatusUpdateRequest request) {
        Task actualizada = taskService.cambiarStatus(id, request.status());
        return TaskMapper.aResponse(actualizada);
    }
}

```
````

</details>


### `read_file`

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/repository/TaskRepository.java",
  "startLine": 1,
  "endLine": 400
}
```

</details>

<details>
<summary>36 lines</summary>

````
File: `C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/repository/TaskRepository.java`. Lines 1 to 43 (43 lines total): 
```
package com.taskflow.repository;

import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import org.springframework.data.jpa.repository.JpaRepository;

import java.util.List;

/**
 * TaskRepository — extiende JpaRepository&lt;Task, Long&gt;: save/findById/findAll/deleteById VIENEN
 * GRATIS (+ count/existsById/etc.). NO se implementa nadie: un PROXY de runtime lo hace (la misma
 * "magia" de la inyección de dependencias).
 *
 * La paga de programar contra una interfaz: TaskService/ReportService dependen de ESTE contrato, y
 * sus firmas (Optional&lt;Task&gt; findById, void deleteById) son las de Spring Data. Si cambiara la
 * implementación, el servicio no se tocaría: depende de la interfaz, no de JPA.
 *
 * DERIVED QUERIES: el NOMBRE del método es la query. Gramática findBy + propiedad + And/Not/
 * Containing/IgnoreCase/OrderBy, y countBy. Se validan AL ARRANCAR: un nombre con una propiedad que
 * no existe (p.ej. findByEstado) lanza QueryCreationException y la app NO levanta — error de arranque,
 * no de runtime. Estas seis se prueban en TaskRepositoryTest (@DataJpaTest).
 */
public interface TaskRepository extends JpaRepository<Task, Long> {

    /** Tareas en un estado dado (hoy la usan los tests; queda para vistas por estado). */
    List<Task> findByStatus(TaskStatus status);

    /** Tareas de un proyecto (por la columna project_id). La consume ProjectService.tareasDe. */
    List<Task> findByProjectId(Long projectId);

    /** Combina dos criterios con And: tareas de un asignado en un estado. */
    List<Task> findByAssigneeIdAndStatus(Long assigneeId, TaskStatus status);

    /** LIKE case-insensitive: título que CONTIENE el fragmento (la BD hace el filtrado). */
    List<Task> findByTitleContainingIgnoreCase(String fragment);

    /** El complemento de findByStatus: todas MENOS las de ese estado (Not). Sirve a "pendientes". */
    List<Task> findByStatusNot(TaskStatus status);

    /** Cuenta en la BD (no trae filas para contarlas): conteo por estado. */
    long countByStatus(TaskStatus status);
}

```
````

</details>


### `read_file`

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/model/Task.java",
  "startLine": 1,
  "endLine": 400
}
```

</details>

<details>
<summary>228 lines</summary>

````
File: `C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/model/Task.java`. Lines 1 to 265 (265 lines total): 
```
package com.taskflow.model;

import com.taskflow.exception.TaskValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Task — entidad central del dominio TaskFlow (persistida con JPA).
 *
 * Es una clase de dominio normal (8 campos, validaciones, comportamiento): NO hay un "modelo de
 * BD" aparte. Lo que la hace entidad, todo por anotación:
 *   - @Entity + @Table(name = "tasks"): esta clase mapea a la tabla TASKS.
 *   - @Id + @GeneratedValue(IDENTITY): el id lo asigna la BD (columna autoincremental). Nada de
 *     secuencia manual en Java: el id lo pone la BD y nadie más.
 *   - @Enumerated(STRING) en status y priority: se guardan como TEXTO ('TODO'), NUNCA como ORDINAL
 *     (0,1,2): reordenar o insertar un enum corrompería datos históricos. El orden LOW,MED,HIGH
 *     es contrato del SORT (compareTo), no del almacenamiento.
 *   - relación @ManyToOne Task->Project (patrón "asociación de solo lectura"): la COLUMNA la
 *     manda el escalar 'projectId' (por eso Task.crear, TaskService, mappers y DTOs hablan de projectId);
 *     el objeto 'project' solo NAVEGA. Frase ancla: "la columna manda; el objeto navega".
 *
 * Tensión JPA vs validaciones: JPA rehidrata
 * por reflexión con un constructor sin args protegido, SIN pasar por las validaciones. Está bien:
 * Task.crear(...) sigue siendo el ÚNICO camino de CREACIÓN de negocio; JPA solo REHIDRATA datos que
 * ya existían (la distinción crear/rehidratar).
 *
 * OJO: las columnas persistentes NO pueden ser 'final' (JPA las escribe por reflexión tras el no-arg);
 * por eso title/description/projectId/dueDate no son 'final'. La inmutabilidad
 * de negocio la sigue garantizando la ausencia de setters públicos para esos campos.
 */
@Entity
@Table(name = "tasks")
public class Task implements Comparable<Task> {

    // Reglas de longitud del título (regla de negocio).
    private static final int TITULO_MIN = 3;
    private static final int TITULO_MAX = 120;

    // id nace null; lo asigna la BD (IDENTITY). Ver setId (uso interno de JPA/repo).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    private String description;                 // por convención -> columna DESCRIPTION

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;                  // texto 'TODO'/'IN_PROGRESS'/'DONE' (nunca 0/1/2)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;                  // texto 'LOW'/'MED'/'HIGH'

    // La COLUMNA project_id la posee este escalar (insertable/updatable por default). Sigue siendo
    // el dueño: Task.crear, el mapper y el servicio hablan con projectId, no con el objeto.
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    // assigneeId se queda Long a secas (juicio explícito): es nullable, NUNCA navegamos al User
    // completo al leer tareas, y las derived queries operan igual. No toda FK necesita ser objeto:
    // mapea objeto cuando NAVEGAS, deja el id cuando solo REFERENCIAS.
    @Column(name = "assignee_id")
    private Long assigneeId;

    private LocalDate dueDate;                  // por convención -> columna DUE_DATE (camelCase->snake_case)

    /**
     * Asociación de SOLO LECTURA hacia el proyecto. Comparte la MISMA columna project_id con
     * el escalar de arriba, pero marcada insertable=false/updatable=false: no escribe la columna (el
     * escalar lo hace), solo permite NAVEGAR (task.getProject().getName()). LAZY: no se carga hasta
     * que se toca — y fuera de una sesión abierta, tocarla lanza LazyInitializationException (por eso
     * la API responde con DTOs, nunca con la entidad).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    /**
     * Constructor NO-ARG protegido: EXCLUSIVO de JPA (rehidratación por reflexión). Nunca es un camino
     * de creación de negocio. Su existencia es obligatoria para cualquier @Entity: sin él, al arrancar
     * Hibernate lanza "No default constructor for entity: com.taskflow.model.Task".
     */
    protected Task() {
        // solo para JPA: rehidratación, nunca creación de negocio
    }

    /**
     * Constructor de REHIDRATACIÓN: reconstruye una tarea que YA existía (semilla del DataSeeder,
     * datos releídos). Valida las INVARIANTES que siempre deben cumplirse (título y proyecto), pero
     * NO la regla temporal de dueDate: una tarea vencida es un dato válido que hay que poder releer.
     */
    public Task(Long id, String title, String description, TaskStatus status,
                Priority priority, Long projectId, Long assigneeId, LocalDate dueDate)
            throws TaskValidationException {
        // Invariante 1: título obligatorio, 3-120 chars.
        if (title == null || title.isBlank()) {
            throw new TaskValidationException("El título es obligatorio (no puede ir vacío).");
        }
        if (title.length() < TITULO_MIN || title.length() > TITULO_MAX) {
            throw new TaskValidationException(
                    "El título debe tener entre " + TITULO_MIN + " y " + TITULO_MAX
                            + " caracteres; recibí " + title.length() + ": \"" + title + "\".");
        }
        // Invariante 2: una tarea no puede existir sin proyecto (regla de negocio).
        if (projectId == null) {
            throw new TaskValidationException(
                    "Una tarea no puede existir sin proyecto (projectId == null).");
        }
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.projectId = projectId;
        this.assigneeId = assigneeId;
        this.dueDate = dueDate;
    }

    /**
     * Factory de CREACIÓN de negocio: añade la regla temporal (dueDate no en el pasado) y delega en
     * el constructor. id nace null, status nace TODO. La usan el mapper y el servicio tal cual: que
     * Task sea @Entity no cambia esta firma.
     */
    public static Task crear(String title, String description, Priority priority,
                             LocalDate dueDate, Long projectId, Long assigneeId)
            throws TaskValidationException {
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            throw new TaskValidationException(
                    "La fecha límite no puede estar en el pasado: " + dueDate + ".");
        }
        return new Task(null, title, description, TaskStatus.TODO, priority, projectId, assigneeId, dueDate);
    }

    // ---- Comportamiento: vive CON los datos ----

    /** true si tiene fecha, ya pasó, y la tarea aún no está terminada. */
    public boolean estaVencida() {
        return dueDate != null && dueDate.isBefore(LocalDate.now()) && status != TaskStatus.DONE;
    }

    // ---- Getters ----

    public Long getId() {
        return id;
    }

    /** Uso interno (JPA/repo) para el id autoincremental. Nadie más lo llama a mano. */
    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Navega al proyecto asociado (asociación de solo lectura). Fuera de una sesión JPA abierta puede
     * lanzar LazyInitializationException (por eso la API expone DTOs, no la entidad). En el runtime
     * nadie del camino principal lo usa; existe para NAVEGAR cuando haga falta.
     */
    public Project getProject() {
        return project;
    }

    // ---- Setters SOLO donde el dominio muta ----

    /**
     * Cambia el estado aplicando la regla de negocio: no se puede pasar a DONE una tarea sin
     * responsable (assigneeId). La regla vive AQUÍ, no en el servicio ni en el controller.
     */
    public void setStatus(TaskStatus status) throws TaskValidationException {
        if (status == TaskStatus.DONE && assigneeId == null) {
            throw new TaskValidationException(
                    "No se puede marcar como " + TaskStatus.DONE
                            + " una tarea sin responsable: \"" + title + "\".");
        }
        this.status = status;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    // ---- Identidad de ENTIDAD por id (literalmente lo que hace JPA) ----

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Task otra = (Task) o;
        return Objects.equals(id, otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ---- Orden natural por prioridad (Comparable) ----

    @Override
    public int compareTo(Task otra) {
        return this.priority.compareTo(otra.priority);
    }

    @Override
    public String toString() {
        return "Task{id=" + id
                + ", title='" + title + '\''
                + ", status=" + status
                + ", priority=" + priority
                + ", assigneeId=" + (assigneeId == null ? "sin asignar" : assigneeId)
                + ", dueDate=" + (dueDate == null ? "sin fecha" : dueDate)
                + (estaVencida() ? " *VENCIDA*" : "")
                + '}';
    }
}

```
````

</details>


### `read_file`

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskResponse.java",
  "startLine": 1,
  "endLine": 260
}
```

</details>

<details>
<summary>27 lines</summary>

````
File: `C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskResponse.java`. Lines 1 to 28 (28 lines total): 
```
package com.taskflow.dto;

import com.taskflow.model.Priority;
import com.taskflow.model.TaskStatus;

import java.time.LocalDate;

/**
 * TaskResponse — el CONTRATO de SALIDA de una tarea (lo que la API devuelve). Es un record: Jackson
 * lo serializa a JSON por sus componentes.
 *
 * "¿Si trae los mismos 8 campos que la entidad, para qué el DTO?" — son casi iguales; el valor
 * es el DESACOPLE. La entidad Task lleva anotaciones JPA (@Entity/@Id...) que NO tienen por qué
 * asomar en el JSON, y hay entidades con campos que JAMÁS deben salir (el passwordHash de User). El
 * contrato de salida se mantiene estable aunque la entidad cambie por dentro.
 */
public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Priority priority,
        Long projectId,
        Long assigneeId,
        LocalDate dueDate
) {
}

```
````

</details>


### `read_file`

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskStatusUpdateRequest.java",
  "startLine": 1,
  "endLine": 200
}
```

</details>

<details>
<summary>20 lines</summary>

````
File: `C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskStatusUpdateRequest.java`. Lines 1 to 21 (21 lines total): 
```
package com.taskflow.dto;

import com.taskflow.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * TaskStatusUpdateRequest — el cuerpo del PATCH /tasks/{id}/status. Un DTO por OPERACIÓN: el
 * único campo que este endpoint puede tocar es el estado. Es la respuesta al riesgo del PUT: en vez
 * de reemplazar TODA la tarea para cambiar un campo, un DTO chico y preciso.
 *
 * @NotNull sobre el enum: si el JSON trae {"status": null} o no trae la clave, la validación da 400.
 * OJO al orden parseo -> validación: {"status":"FINISHED"} (valor que no matchea el enum) NUNCA llega
 * a @NotNull; truena antes, en la deserialización de Jackson (HttpMessageNotReadableException -> 400).
 */
public record TaskStatusUpdateRequest(

        @NotNull(message = "El estado es obligatorio.")
        TaskStatus status
) {
}

```
````

</details>


### `read_file`

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/exception/TaskNotFoundException.java",
  "startLine": 1,
  "endLine": 200
}
```

</details>

<details>
<summary>28 lines</summary>

````
File: `C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/exception/TaskNotFoundException.java`. Lines 1 to 30 (30 lines total): 
```
package com.taskflow.exception;

/**
 * TaskNotFoundException — se lanza cuando se busca una tarea por id y NO existe.
 *
 * A diferencia de TaskValidationException (checked, para reglas de negocio del input),
 * esta es UNCHECKED (extiende RuntimeException): un id inexistente suele ser un caso de
 * "no encontrado" que se maneja en un punto concreto (el advice REST), no una condición que TODO
 * método que la toque deba declarar. Se lanza con el patrón Optional/orElseThrow:
 *
 *     Task t = repo.findById(id).orElseThrow(() -> new TaskNotFoundException(id));
 *
 * Guarda el id que no se encontró (útil para el mensaje y para el log). El GlobalExceptionHandler
 * la mapea a un 404.
 */
public class TaskNotFoundException extends RuntimeException {

    private final Long id;

    public TaskNotFoundException(Long id) {
        super("No existe tarea con id " + id + ".");
        this.id = id;
    }

    /** El id que se buscó y no existía. */
    public Long getId() {
        return id;
    }
}

```
````

</details>


### `read_file`

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/exception/TaskStateException.java",
  "startLine": 1,
  "endLine": 260
}
```

</details>

<details>
<summary>23 lines</summary>

````
File: `C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/exception/TaskStateException.java`. Lines 1 to 23 (23 lines total): 
```
package com.taskflow.exception;

/**
 * TaskStateException — el request está BIEN FORMADO, pero el ESTADO del dominio no permite la
 * operación. El GlobalExceptionHandler la mapea a 422 Unprocessable Entity.
 *
 * Por qué existe (decisión de diseño, 422 vs 400): "pasar a DONE una tarea sin
 * responsable" no es un error de FORMATO (eso sería 400) ni de "recurso inexistente" (404): el JSON
 * es impecable, pero la regla de negocio dice "todavía no". 422 comunica exactamente eso: "no
 * reintentes igual — primero asigna a alguien".
 *
 * Es UNCHECKED (extiende RuntimeException): sube sola hasta el advice sin ensuciar las firmas. La
 * regla de negocio sigue viviendo en Task.setStatus (que lanza la CHECKED TaskValidationException);
 * TaskService.cambiarStatus la CAPTURA y la TRADUCE a esta excepción. Así TaskValidationException
 * conserva su significado (400 en creación/edición) y el 422 queda reservado a la regla de estado.
 */
public class TaskStateException extends RuntimeException {

    public TaskStateException(String mensaje) {
        super(mensaje);
    }
}

```
````

</details>


### `read_file`

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskServiceTest.java",
  "startLine": 1,
  "endLine": 360
}
```

</details>

<details>
<summary>194 lines</summary>

````
File: `C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskServiceTest.java`. Lines 1 to 231 (231 lines total): 
```
package com.taskflow.unit;

import com.taskflow.dto.TaskRequest;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Nested;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.Optional;
import java.util.List;
import java.time.LocalDate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

/**
 * TaskServiceTest — UNIT PURO con Mockito: aquí NO arranca Spring — la clase corre en MILISEGUNDOS.
 *
 * Por qué Mockito: TaskRepository es una interfaz cuya única implementación es un proxy de Spring Data
 * respaldado por una BD. Un *fake* en memoria (rápido y determinista) también sería legítimo, pero no
 * existe: para probar el service EN AISLAMIENTO no hay implementación de juguete que instanciar
 * -> por eso existe Mockito: el mock SUSTITUYE al fake cuando el colaborador real exige infraestructura.
 *
 * Reglas aplicadas:
 *   - @ExtendWith(MockitoExtension.class): sin ella, @Mock queda null.
 *   - Los DATOS (Task) se construyen DE VERDAD con el constructor de rehidratación; SOLO el COLABORADOR
 *     (el repository) se mockea.
 *   - MockitoExtension corre en strict-stubs: un when(...) que ningún camino usa FALLA la clase
 *     (UnnecessaryStubbingException) -> aquí cada stub tiene propósito.
 */
@ExtendWith(MockitoExtension.class)
class TaskServiceTest {

    @Mock
    private TaskRepository repository;

    @InjectMocks
    private com.taskflow.service.TaskService service;

    private static final Long PROYECTO = 1L;

    @Nested
    @DisplayName("crear")
    class Crear {

        @Test
        void crear_valida_delegaEnSaveYDevuelveTareaConId() throws TaskValidationException {
            // El request pasa por TaskMapper.aEntidadNueva -> Task.crear (status=TODO, id=null). El
            // repositorio, mockeado, "devuelve" una tarea YA con id (lo que la BD real haría en save).
            TaskRequest request = new TaskRequest("Redactar informe", "desc", Priority.HIGH, 5L, null);
            Task tareaConId = new Task(99L, "Redactar informe", "desc",
                    TaskStatus.TODO, Priority.HIGH, PROYECTO, 5L, null);
            when(repository.save(any(Task.class))).thenReturn(tareaConId);

            Task creada = service.crear(request, PROYECTO);

            assertNotNull(creada.getId());
            assertEquals(99L, creada.getId());
        }

        @Test
        @DisplayName("thenThrow: el mock simula una BD caída — lo que el colaborador real no te da nunca")
        void crear_conBdCaida_propagaLaExcepcion() {
            // thenThrow: con la BD real NO puedes provocar una caída a demanda; el mock sí.
            TaskRequest request = new TaskRequest("Tarea válida", "desc", Priority.MED, 5L, null);
            when(repository.save(any(Task.class)))
                    .thenThrow(new RuntimeException("Conexión a la BD perdida"));

            assertThrows(RuntimeException.class, () -> service.crear(request, PROYECTO));
        }
    }

    @Nested
    @DisplayName("cambiarStatus")
    class CambiarStatus {

        @Test
        void cambiarStatus_conAssignee_guardaDone() {
            // El dato se construye REAL (constructor de rehidratación), con assignee -> SÍ puede pasar a
            // DONE. Solo el repository (colaborador) se mockea.
            Task tareaConAssignee = tarea(1L, "Con responsable", 5L);
            when(repository.findById(1L)).thenReturn(Optional.of(tareaConAssignee));
            when(repository.save(any(Task.class))).thenAnswer(inv -> inv.getArgument(0));

            service.cambiarStatus(1L, TaskStatus.DONE);

            // ArgumentCaptor: capturar el Task que VIAJÓ a save. El retorno no basta cuando lo que
            // importa es el OBJETO MUTADO que se persistió.
            ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
            verify(repository).save(captor.capture());
            assertEquals(TaskStatus.DONE, captor.getValue().getStatus());
        }

        @Test
        void cambiarStatus_idInexistente_lanzaTaskNotFoundException() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class,
                    () -> service.cambiarStatus(999L, TaskStatus.DONE));
            // No se llegó a guardar nada.
            verify(repository, never()).save(any(Task.class));
        }

        @Test
        void cambiarStatus_sinAssignee_lanzaTaskStateExceptionYNoGuarda() {
            // Sin assignee, setStatus(DONE) lanza la checked; cambiarStatus la TRADUCE a TaskStateException.
            // La traducción, probada EN AISLAMIENTO (sin HTTP, sin BD).
            Task sinAssignee = tarea(2L, "Sin responsable", null);
            when(repository.findById(2L)).thenReturn(Optional.of(sinAssignee));

            assertThrows(TaskStateException.class,
                    () -> service.cambiarStatus(2L, TaskStatus.DONE));
            // El never() es el assert más valioso del bloque: la regla PROHIBIÓ persistir.
            verify(repository, never()).save(any(Task.class));
        }
    }

    @Nested
    @DisplayName("eliminar")
    class Eliminar {

        @Test
        void eliminar_existente_llamaDeleteById() {
            Task existente = tarea(1L, "Borrable", 5L);
            when(repository.findById(1L)).thenReturn(Optional.of(existente));

            service.eliminar(1L);

            // verify sobre una operación MUDA (void): el estado no lo delata, la interacción sí.
            verify(repository).deleteById(1L);
        }

        @Test
        void eliminar_inexistente_lanzaTaskNotFoundExceptionYNoBorra() {
            when(repository.findById(999L)).thenReturn(Optional.empty());

            assertThrows(TaskNotFoundException.class, () -> service.eliminar(999L));
            // never() + anyLong(): NO se borró nada. (Regla "todos matchers o ninguno": aquí anyLong()).
            verify(repository, never()).deleteById(anyLong());
        }
    }

    /** Fabrica una Task de rehidratación REAL (dato, no mock). assigneeId null = sin responsable. */
    private Task tarea(Long id, String title, Long assigneeId) {
        try {
            return new Task(id, title, "desc", TaskStatus.TODO, Priority.MED, PROYECTO, assigneeId, null);
        } catch (TaskValidationException e) {
            throw new IllegalStateException("dato de prueba inválido", e);
        }
    }

    @Test
    void vencidas_devuelveSoloVencidasYOrdenadas() throws TaskValidationException {
        // Repo devuelve en este orden: vencida hace 1 día, due en 3 días, DONE vencida hace 10 días,
        // sin dueDate, vencida hace 5 días
        Task a = new Task(11L, "VOne", "desc", TaskStatus.IN_PROGRESS, Priority.MED, PROYECTO, 1L, LocalDate.now().minusDays(1));
        Task b = new Task(12L, "Futura", "desc", TaskStatus.IN_PROGRESS, Priority.MED, PROYECTO, 1L, LocalDate.now().plusDays(3));
        Task c = new Task(13L, "HechaAntigua", "desc", TaskStatus.DONE, Priority.MED, PROYECTO, 1L, LocalDate.now().minusDays(10));
        Task d = new Task(14L, "SinFecha", "desc", TaskStatus.IN_PROGRESS, Priority.MED, PROYECTO, 1L, null);
        Task e = new Task(15L, "VFive", "desc", TaskStatus.IN_PROGRESS, Priority.MED, PROYECTO, 1L, LocalDate.now().minusDays(5));

        when(repository.findAll()).thenReturn(List.of(a, b, c, d, e));

        List<Task> res = service.vencidas();

        // Debe devolver solo las vencidas no-DONE (e y a), ordenadas POR_FECHA ascendente => e (5d) antes que a (1d)
        assertEquals(2, res.size());
        assertEquals(15L, res.get(0).getId());
        assertEquals(11L, res.get(1).getId());
    }

    @Nested
    @DisplayName("SinResponsable")
    class SinResponsable {

        @Test
        void devuelveSoloSinResponsableYOrdenadasPorFecha() {
            Task t10 = null;
            Task conResp = null;
            Task sinFecha = null;
            Task t2 = null;
            try {
                t10 = new Task(1L, "T10d", "desc", TaskStatus.TODO, Priority.MED, PROYECTO, null, LocalDate.now().plusDays(10));
                conResp = new Task(2L, "ConResp", "desc", TaskStatus.TODO, Priority.MED, PROYECTO, 5L, LocalDate.now().plusDays(5));
                sinFecha = new Task(3L, "SinFecha", "desc", TaskStatus.TODO, Priority.MED, PROYECTO, null, null);
                t2 = new Task(4L, "T2d", "desc", TaskStatus.TODO, Priority.MED, PROYECTO, null, LocalDate.now().plusDays(2));
            } catch (TaskValidationException e) {
                throw new IllegalStateException("dato de prueba inválido", e);
            }

            when(repository.findAll()).thenReturn(List.of(t10, conResp, sinFecha, t2));

            List<Task> res = service.sinResponsable();

            var ids = res.stream().map(Task::getId).toList();
            assertEquals(List.of(4L, 1L, 3L), ids);
        }

        @Test
        void devuelveListaVaciaSiTodasConResponsable() {
            Task con = null;
            try {
                con = new Task(11L, "Con", "desc", TaskStatus.TODO, Priority.MED, PROYECTO, 1L, null);
            } catch (TaskValidationException e) {
                throw new IllegalStateException("dato de prueba inválido", e);
            }
            when(repository.findAll()).thenReturn(List.of(con));

            List<Task> res = service.sinResponsable();
            assertEquals(List.of(), res.stream().map(Task::getId).toList());
        }
    }
}

```
````

</details>


### `read_file`

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskControllerTest.java",
  "startLine": 1,
  "endLine": 360
}
```

</details>

<details>
<summary>203 lines</summary>

````
File: `C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskControllerTest.java`. Lines 1 to 235 (235 lines total): 
```
package com.taskflow.slice;

import com.taskflow.controller.TaskController;
import com.taskflow.exception.TaskNotFoundException;
import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.service.ProjectService;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.http.MediaType;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.List;
import java.time.LocalDate;
import java.util.Optional;

import static org.hamcrest.Matchers.matchesPattern;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.delete;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.header;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/**
 * TaskControllerTest — SLICE web. El CRUD no necesita un @SpringBootTest lento: le basta
 * @WebMvcTest. Aquí NO se levanta JPA, ni el seeder, ni la seguridad real: solo la
 * capa web (controller + advice + Jackson + validación). El service es un @MockitoBean.
 *
 * Las 3 anotaciones CLAVE del slice:
 *   1. @WebMvcTest(TaskController.class) -> levanta SOLO la capa web de este controller.
 *   2. @MockitoBean del/los service(s) -> el colaborador ausente, programado con when(...).
 *   3. @AutoConfigureMockMvc(addFilters = false) -> la seguridad NO se prueba en el slice: se prueba
 *      donde vive, en integration/. Sin esto, el slice aplica la seguridad default de Boot y TODO da 401.
 *
 * Tropiezo típico: el slice ESCANEA los Filter @Component, y JwtAuthenticationFilter arrastra
 * JwtService/JpaUserDetailsService al contexto -> "No qualifying bean of type 'JwtService'". Solución:
 * @MockitoBean JwtAuthenticationFilter (se mockea el filtro para que el slice no cargue sus dependencias).
 *
 * @Mock vs @MockitoBean: aquí HAY contexto Spring -> @MockitoBean (sustituye un bean). En
 * unit/ NO hay Spring -> @Mock. Regla mecánica: ¿la clase tiene una anotación @...Test de Spring? -> bean.
 *
 * Aquí no hay token: ningún filtro lo exige (addFilters = false).
 */
@WebMvcTest(TaskController.class)
@AutoConfigureMockMvc(addFilters = false)
class TaskControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private TaskService taskService;

    @MockitoBean
    private ProjectService projectService;   // TaskController también inyecta ProjectService (cableo Task<->Project)

    // Sin esto: el slice intenta construir el Filter @Component real -> No qualifying bean 'JwtService'.
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    // ==================== GET (contra el stub del service, no una semilla) ====================

    @Test
    void getTasks_sinFiltro_retorna200YLista() throws Exception {
        when(taskService.listar()).thenReturn(List.of(
                tarea(1L, "Primera", TaskStatus.TODO),
                tarea(2L, "Segunda", TaskStatus.IN_PROGRESS)));

        mockMvc.perform(get("/tasks"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].title").value("Primera"));
    }

    @Test
    void getTaskPorId_existente_retorna200ConElTitulo() throws Exception {
        when(taskService.buscarPorId(1L)).thenReturn(Optional.of(tarea(1L, "Diseñar esquema de BD", TaskStatus.TODO)));

        mockMvc.perform(get("/tasks/1"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.title").value("Diseñar esquema de BD"))
                .andExpect(jsonPath("$.projectId").value(1));
    }

    @Test
    void getTaskPorId_inexistente_retorna404() throws Exception {
        // El service dice "no está" (Optional vacío); el controller lanza -> el advice (SÍ vive en el slice) responde 404.
        when(taskService.buscarPorId(999L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ==================== Escritura (la validación y el advice SÍ viven en el slice) ====================

    @Test
    void postTask_valida_devuelve201ConLocation() throws Exception {
        when(projectService.buscarPorId(1L)).thenReturn(Optional.of(new Project(1L, "P", "d", 1L, null)));
        when(taskService.crear(any(), eq(1L)))
                .thenReturn(tareaCon(10L, "Redactar el informe trimestral", TaskStatus.TODO, 1L));

        String body = """
                {
                  "title": "Redactar el informe trimestral",
                  "description": "Con las métricas del quarter",
                  "priority": "HIGH",
                  "assigneeId": 1,
                  "dueDate": "2099-12-31"
                }
                """;

        mockMvc.perform(post("/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isCreated())
                .andExpect(header().string("Location", matchesPattern(".*/tasks/\\d+")))
                .andExpect(jsonPath("$.title").value("Redactar el informe trimestral"))
                .andExpect(jsonPath("$.status").value("TODO"));
    }

    /**
     * Frontera de la API, ESPEJO del parametrizado de dominio (TaskValidationTest): títulos que
     * violan @NotBlank/@Size -> 400. La validación de forma vive en el slice; ni siquiera se llama al
     * service. 0 -> vacío; 1,2 -> cortos; 121 -> largo.
     */
    @ParameterizedTest(name = "título de longitud {0} → 400")
    @ValueSource(ints = {0, 1, 2, 121})
    void postTask_tituloInvalido_devuelve400(int longitud) throws Exception {
        String titulo = "a".repeat(longitud);
        String body = """
                { "title": "%s", "priority": "LOW" }
                """.formatted(titulo);

        mockMvc.perform(post("/projects/1/tasks")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content(body))
                .andExpect(status().isBadRequest());
    }

    @Test
    void patchStatus_sinAssignee_devuelve422() throws Exception {
        // El service (mockeado) traduce la regla a TaskStateException; el advice la mapea a 422.
        when(taskService.cambiarStatus(6L, TaskStatus.DONE))
                .thenThrow(new TaskStateException("No se puede marcar DONE sin responsable."));

        mockMvc.perform(patch("/tasks/6/status")
                        .contentType(MediaType.APPLICATION_JSON)
                        .content("{ \"status\": \"DONE\" }"))
                .andExpect(status().isUnprocessableEntity())
                .andExpect(jsonPath("$.status").value(422));
    }

    @Test
    void deleteTask_inexistente_devuelve404() throws Exception {
        // eliminar es void -> doThrow(...).when(...) para simular el "no existe".
        doThrow(new TaskNotFoundException(999L)).when(taskService).eliminar(999L);

        mockMvc.perform(delete("/tasks/999"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }

    // ---- helpers de datos (reales, no mocks) ----

    private Task tarea(Long id, String title, TaskStatus status) {
        return tareaCon(id, title, status, 1L);
    }

    private Task tareaCon(Long id, String title, TaskStatus status, Long projectId) {
        try {
            return new Task(id, title, "desc", status, Priority.MED, projectId, 1L, null);
        } catch (TaskValidationException e) {
            throw new IllegalStateException("dato de prueba inválido", e);
        }
    }

    private Task tareaConConFecha(Long id, String title, TaskStatus status, Long projectId, LocalDate dueDate) {
        try {
            return new Task(id, title, "desc", status, Priority.MED, projectId, 1L, dueDate);
        } catch (TaskValidationException e) {
            throw new IllegalStateException("dato de prueba inválido", e);
        }
    }

    @Test
    void getOverdue_returns200YListaOrdenada() throws Exception {
        when(taskService.vencidas()).thenReturn(List.of(
                tareaConConFecha(7L, "Corregir bug de fechas", TaskStatus.IN_PROGRESS, 1L, LocalDate.now().minusDays(1))
        ));

        mockMvc.perform(get("/tasks/overdue"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(1))
                .andExpect(jsonPath("$[0].title").value("Corregir bug de fechas"));
    }

    @Test
    void getUnassigned_returns200AndAssigneeNull() throws Exception {
        Task sin1;
        Task sin2;
        try {
            sin1 = new Task(4L, "Escribir tests MockMvc", "desc", TaskStatus.TODO, Priority.MED, 1L, null, LocalDate.now().plusDays(7));
            sin2 = new Task(6L, "Publicar en la tienda", "desc", TaskStatus.TODO, Priority.MED, 1L, null, LocalDate.now().plusDays(10));
        } catch (TaskValidationException e) {
            throw new IllegalStateException("dato de prueba inválido", e);
        }

        when(taskService.sinResponsable()).thenReturn(List.of(sin1, sin2));

        mockMvc.perform(get("/tasks/unassigned"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.length()").value(2))
                .andExpect(jsonPath("$[0].id").value(4))
                .andExpect(jsonPath("$[0].assigneeId").value(org.hamcrest.Matchers.nullValue()));
    }
}

```
````

</details>


### `read_file`

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskRepositoryTest.java",
  "startLine": 1,
  "endLine": 260
}
```

</details>

<details>
<summary>146 lines</summary>

````
File: `C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskRepositoryTest.java`. Lines 1 to 178 (178 lines total): 
```
package com.taskflow.slice;

import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.EnumSource;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.orm.jpa.DataJpaTest;
import org.springframework.boot.test.autoconfigure.orm.jpa.TestEntityManager;
import org.springframework.test.context.jdbc.Sql;

import java.time.LocalDate;
import java.util.List;

import static org.assertj.core.api.Assertions.assertThat;

/**
 * TaskRepositoryTest — SLICE de JPA (@DataJpaTest).
 *
 * @DataJpaTest arranca solo la capa JPA: H2 EN MEMORIA con esquema FRESCO por test (auto-reemplaza el
 * datasource), Hibernate y TestEntityManager. NO carga el DataSeeder ni los @Service: la BD empieza
 * VACÍA y cada test siembra lo suyo. Es transaccional -> rollback por test (aislamiento).
 *
 * Técnicas que usa:
 *   - flush()+clear() para tests de MAPEO: sin ellos, releer devuelve el mismo objeto del caché de 1er
 *     nivel (el mapeo pudo estar roto y el test verde — el falso verde).
 *   - @Sql: escenarios grandes se DECLARAN en un script, no se construyen a mano.
 *   - @EnumSource: findByStatus probado para CADA estado con un solo método parametrizado.
 *
 * La FK task.project_id enseña ORDEN: persistir el Project ANTES que sus tareas.
 */
@DataJpaTest
class TaskRepositoryTest {

    @Autowired
    private TestEntityManager em;

    @Autowired
    private TaskRepository taskRepository;

    // ---- helpers ----

    private Long nuevoProyecto(String name) {
        Project p = em.persistFlushFind(new Project(null, name, "desc", 1L, LocalDate.now().minusDays(1)));
        return p.getId();
    }

    private Task persistirTarea(String title, TaskStatus status, Priority priority,
                                Long projectId, Long assigneeId, LocalDate due) throws TaskValidationException {
        return em.persistFlushFind(
                new Task(null, title, "desc", status, priority, projectId, assigneeId, due));
    }

    // ==================== Tests base ====================

    @Test
    void save_asignaIdDesdeLaBaseDeDatos() throws TaskValidationException {
        Long pid = nuevoProyecto("Proyecto A");

        Task guardada = taskRepository.save(
                new Task(null, "Primera tarea", "desc", TaskStatus.TODO, Priority.MED, pid, 1L, null));

        assertThat(guardada.getId()).isNotNull();
    }

    @Test
    void findById_inexistente_optionalVacio() {
        assertThat(taskRepository.findById(9_999L)).isEmpty();
    }

    @Test
    void findByStatus_filtraCorrecto() throws TaskValidationException {
        Long pid = nuevoProyecto("Proyecto B");
        persistirTarea("TODO uno", TaskStatus.TODO, Priority.LOW, pid, 1L, null);
        persistirTarea("TODO dos", TaskStatus.TODO, Priority.MED, pid, 1L, null);
        persistirTarea("Hecha", TaskStatus.DONE, Priority.HIGH, pid, 1L, null);

        List<Task> todo = taskRepository.findByStatus(TaskStatus.TODO);

        assertThat(todo).hasSize(2);
        assertThat(todo).allMatch(t -> t.getStatus() == TaskStatus.TODO);
    }

    @Test
    void findByProjectId_soloLasDelProyecto() throws TaskValidationException {
        Long pidA = nuevoProyecto("Proyecto con dos");
        Long pidB = nuevoProyecto("Proyecto con una");
        persistirTarea("A-1", TaskStatus.TODO, Priority.LOW, pidA, 1L, null);
        persistirTarea("A-2", TaskStatus.IN_PROGRESS, Priority.MED, pidA, 1L, null);
        persistirTarea("B-1", TaskStatus.TODO, Priority.HIGH, pidB, 1L, null);

        List<Task> deA = taskRepository.findByProjectId(pidA);

        assertThat(deA).hasSize(2);
        assertThat(deA).allMatch(t -> t.getProjectId().equals(pidA));
    }

    @Test
    void findByAssigneeIdAndStatus_combinaCriterios() throws TaskValidationException {
        Long pid = nuevoProyecto("Proyecto C");
        persistirTarea("De 5, TODO", TaskStatus.TODO, Priority.MED, pid, 5L, null);
        persistirTarea("De 5, DONE", TaskStatus.DONE, Priority.MED, pid, 5L, null);
        persistirTarea("De 6, TODO", TaskStatus.TODO, Priority.MED, pid, 6L, null);

        List<Task> resultado = taskRepository.findByAssigneeIdAndStatus(5L, TaskStatus.TODO);

        assertThat(resultado).hasSize(1);
        assertThat(resultado.get(0).getTitle()).isEqualTo("De 5, TODO");
    }

    @Test
    void countByStatus_yTituloContaining() throws TaskValidationException {
        Long pid = nuevoProyecto("Proyecto D");
        persistirTarea("Documentar la API", TaskStatus.TODO, Priority.HIGH, pid, 1L, null);
        persistirTarea("api gateway", TaskStatus.TODO, Priority.LOW, pid, 1L, null);
        persistirTarea("Sin coincidencia", TaskStatus.DONE, Priority.MED, pid, 1L, null);

        assertThat(taskRepository.countByStatus(TaskStatus.TODO)).isEqualTo(2);
        assertThat(taskRepository.findByTitleContainingIgnoreCase("api")).hasSize(2);
    }

    // ==================== flush/clear, @Sql, @EnumSource ====================

    /**
     * La regla del mapeo: flush()+clear() ANTES de leer. persistFlushFind deja el objeto en el
     * caché de 1er nivel; clear() lo DETACHA, así el findById siguiente hace el viaje REAL a SQL y valida
     * que el @Enumerated(STRING) fue y volvió como texto. (Sin el clear(), un mapeo roto a propósito
     * seguiría dando verde: el falso verde.)
     */
    @Test
    void mapeoDeStatus_sobreviveFlushYClear() throws TaskValidationException {
        Long pid = nuevoProyecto("Proyecto flush");
        Task persistida = em.persistFlushFind(
                new Task(null, "Mapeo honesto", "desc", TaskStatus.IN_PROGRESS, Priority.HIGH, pid, 1L, null));
        Long id = persistida.getId();

        em.clear();   // vacía el caché de 1er nivel: el siguiente find NO puede mentir con memoria vieja

        Task releida = taskRepository.findById(id).orElseThrow();
        assertThat(releida.getStatus()).isEqualTo(TaskStatus.IN_PROGRESS);
    }

    /**
     * Escenario DECLARATIVO con @Sql: 2 proyectos + 8 tareas cargadas por script. El filtro
     * combinado assignee=10 AND status=TODO cae exactamente sobre las tareas 1 y 2 del script. Los ids
     * fijos valen SOLO aquí (esquema fresco por test); jamás se asumen en otra clase.
     */
    @Test
    @Sql("/sql/escenario-tareas.sql")
    void findByAssigneeIdAndStatus_sobreEscenarioSql_filtraCombinado() {
        List<Task> resultado = taskRepository.findByAssigneeIdAndStatus(10L, TaskStatus.TODO);

        assertThat(resultado).hasSize(2);
        assertThat(resultado).allMatch(t -> t.getAssigneeId() == 10L && t.getStatus() == TaskStatus.TODO);
    }

    /**
     * @EnumSource: findByStatus probado para CADA valor de TaskStatus en un
     * solo método. Cada estado siembra su tarea y verifica que la derived query la trae (y solo esa).
     */
    @ParameterizedTest
    @EnumSource(TaskStatus.class)
    void findByStatus_paraCadaEstado_devuelveSoloEseEstado(TaskStatus status) throws TaskValidationException {
        Long pid = nuevoProyecto("Proyecto " + status);
        persistirTarea("Tarea " + status, status, Priority.MED, pid, 1L, null);

        List<Task> encontradas = taskRepository.findByStatus(status);

        assertThat(encontradas).hasSize(1);
        assertThat(encontradas.get(0).getStatus()).isEqualTo(status);
    }
}

```
````

</details>


### `grep_search`

**buscarPorId**

<details>
<summary>Arguments</summary>

```json
{
  "query": "buscarPorId",
  "isRegexp": false
}
```

</details>

<details>
<summary>63 lines</summary>

````
91 matches (more results are available)
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/docs/ARQUITECTURA.md" line=93>
    2. El controlador comprueba que el proyecto exista llamando a `ProjectService.buscarPorId` (`src\main\java\com\taskflow\service\ProjectService.java`) y lanza `ProjectNotFoundException` (404) si no existe.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=21>
    `taskService.buscarPorId(id).orElseThrow(() -> new TaskNotFoundException(id))`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=64>
    - `404` cuando `buscarPorId` devuelve `Optional.empty()`;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/skills/crear-endpoint-taskflow/plantillas.md" line=70>
    Project proyecto = projectService.buscarPorId(id)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/skills/crear-endpoint-taskflow/plantillas.md" line=187>
    when(projectService.buscarPorId(1L)).thenReturn(Optional.of(new Project(1L, "Plataforma", "d", 1L, null)));
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/skills/crear-endpoint-taskflow/plantillas.md" line=199>
    when(projectService.buscarPorId(99L)).thenReturn(Optional.empty());
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/skills/crear-endpoint-taskflow/SKILL.md" line=34>
    | 4 | Controller | el controller del recurso | ruta **sin** `/api` (`/projects/{id}/...`), `@PathVariable("id")` con el nombre escrito, `@Operation(summary, description)` en español, `@Valid` en cada `@RequestBody`, devuelve el DTO (nunca la entidad). El 404 se resuelve **como en la ruta vecina** `GET /projects/{id}/tasks`: `projectService.buscarPorId(id).orElseThrow(() -> new ProjectNotFoundException(id))`; el `GlobalExceptionHandler` lo convierte en 404 |
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/.github/skills/crear-endpoint-taskflow/SKILL.md" line=51>
    `when(projectService.buscarPorId(99L)).thenReturn(Optional.empty())`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia1/verificador.txt" line=43>
    [OK]         línea 93   miembro   ProjectService.buscarPorId  ->  src\main\java\com\taskflow\service\ProjectService.java
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=296>
    +| 4 | Controller | el controller del recurso | ruta **sin** `/api` (`/projects/{id}/...`), `@PathVariable("id")` con el nombre escrito, `@Operation(summary, description)` en español, `@Valid` en cada `@RequestBody`, devuelve el DTO (nunca la entidad). El 404 se resuelve **como en la ruta vecina** `GET /projects/{id}/tasks`: `projectService.buscarPorId(id).orElseThrow(() -> new ProjectNotFoundException(id))`; el `GlobalExceptionHandler` lo convierte en 404 |
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=313>
    +  `when(projectService.buscarPorId(99L)).thenReturn(Optional.empty())`.
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=409>
    +        Project proyecto = projectService.buscarPorId(id)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=526>
    +        when(projectService.buscarPorId(1L)).thenReturn(Optional.of(new Project(1L, "Plataforma", "d", 1L, null)));
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=559>
    +        when(projectService.buscarPorId(99L)).thenReturn(Optional.empty());
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=941>
    +        Project proyecto = projectService.buscarPorId(id)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1111>
    +        when(projectService.buscarPorId(2L)).thenReturn(Optional.of(new Project(2L, "App Móvil", "d", 2L, null)));
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1128>
    +        when(projectService.buscarPorId(99L)).thenReturn(Optional.empty());
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1304>
    Project proyecto = projectService.buscarPorId(id)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1320>
    projectService.buscarPorId(id)
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1380>
    Project proyecto = projectService.buscarPorId(id)
</match>
```
````

</details>


### `grep_search`

**reasignar**

<details>
<summary>Arguments</summary>

```json
{
  "query": "reasignar",
  "isRegexp": false
}
```

</details>

<details>
<summary>24 lines</summary>

````
7 matches
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=23>
    `No se puede reasignar una tarea terminada.`. Se lanza `TaskStateException`, la misma que usa
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=44>
    - Método nuevo en `TaskService`: `public Task reasignar(Task tarea, Long assigneeId)`. Recibe la
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=54>
    - **Unit** `src/test/java/com/taskflow/unit/ReasignarTareaServiceTest.java`, con
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=59>
    - **Slice** `src/test/java/com/taskflow/slice/ReasignarTareaControllerTest.java`, con
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=63>
    - `422` cuando `reasignar` lanza `TaskStateException`;
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=65>
    - `400` con el cuerpo `{}`, y `reasignar` **nunca** se llama (`verify(taskService, never())`).
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=77>
    | 4 | `PATCH /tasks/2/assignee` con `{"assigneeId": 3}` | `422` con `message` = `No se puede reasignar una tarea terminada.` (la 2 es `DONE`) |
</match>
```
````

</details>


### `grep_search`

**setAssigneeId**

<details>
<summary>Arguments</summary>

```json
{
  "query": "setAssigneeId",
  "isRegexp": false
}
```

</details>

<details>
<summary>21 lines</summary>

````
6 matches
```txt
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/specs/assignee.md" line=28>
    4. En cualquier otro caso: se asigna el responsable con `Task.setAssigneeId`, se guarda con el
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revision.md" line=1820>
    public void setAssigneeId(Long assigneeId) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/revisor-no-edita.md" line=1787>
    public void setAssigneeId(Long assigneeId) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/summary-sesion.md" line=1817>
    public void setAssigneeId(Long assigneeId) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/evidencia/dia4/tester-sesion.md" line=1907>
    public void setAssigneeId(Long assigneeId) {
</match>
<match path="C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/model/Task.java" line=223>
    public void setAssigneeId(Long assigneeId) {
</match>
```
````

</details>


### `file_search`

****/ReasignarTareaServiceTest.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/ReasignarTareaServiceTest.java"
}
```

</details>

```
No files found matching pattern: **/ReasignarTareaServiceTest.java
```


### `file_search`

****/ReasignarTareaControllerTest.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/ReasignarTareaControllerTest.java"
}
```

</details>

```
No files found matching pattern: **/ReasignarTareaControllerTest.java
```


### `file_search`

****/ReasignarTareaServiceTest.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/ReasignarTareaServiceTest.java"
}
```

</details>

```
No files found matching pattern: **/ReasignarTareaServiceTest.java
```


### `file_search`

****/ReasignarTareaControllerTest.java**

<details>
<summary>Arguments</summary>

```json
{
  "query": "**/ReasignarTareaControllerTest.java"
}
```

</details>

```
No files found matching pattern: **/ReasignarTareaControllerTest.java
```


### `read_file`

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java",
  "startLine": 146,
  "endLine": 260
}
```

</details>

````
File: `C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java`. Lines 146 to 146 (146 lines total): 
```

```
````


### `read_file`

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/TaskController.java",
  "startLine": 157,
  "endLine": 260
}
```

</details>

````
File: `C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/TaskController.java`. Lines 157 to 157 (157 lines total): 
```

```
````


### `read_file`

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/model/Task.java",
  "startLine": 265,
  "endLine": 360
}
```

</details>

````
File: `C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/model/Task.java`. Lines 265 to 265 (265 lines total): 
```

```
````


### `read_file` — Failed

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/ReasignarTareaServiceTest.java",
  "startLine": 1,
  "endLine": 260
}
```

</details>

```
Error: File 'C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/ReasignarTareaServiceTest.java' does not exist
```


### `read_file` — Failed

<details>
<summary>Arguments</summary>

```json
{
  "filePath": "C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/ReasignarTareaControllerTest.java",
  "startLine": 1,
  "endLine": 260
}
```

</details>

```
Error: File 'C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/ReasignarTareaControllerTest.java' does not exist
```


### Copilot

\<final_answer>
C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java:1-146
C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/service/TaskService.java:146-210
C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/TaskController.java:1-157
C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/controller/TaskController.java:157-220
C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/model/Task.java:1-265
C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/model/Task.java:265-340
C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskResponse.java:1-40
C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/dto/TaskStatusUpdateRequest.java:1-30
C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/exception/TaskNotFoundException.java:1-40
C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/main/java/com/taskflow/exception/TaskStateException.java:1-30
C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/unit/TaskServiceTest.java:1-231
C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskControllerTest.java:1-235
C:/Users/Lenovo/taskflow-copilot-enriqueperezdevmx-coder/src/test/java/com/taskflow/slice/TaskRepositoryTest.java:1-178
\</final_answer>


---

<sub>39s</sub>

### Copilot

Reading templates and domain classes to implement methods and tests; fetching Task.java, TaskResponse.java, TaskMapper.java, TaskService.java, TaskController.java, TaskStatusUpdateRequest.java, JwtAuthenticationFilter and existing tests to copy conventions.

---

<sub>39s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\.github\skills\crear-endpoint-taskflow\plantillas.md.**

C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\.github\skills\crear-endpoint-taskflow\plantillas.md

<details>
<summary>167 lines</summary>

````
# Plantillas de la skill `crear-endpoint-taskflow`

> **Ejemplo, no el endpoint que te piden.** Todo este archivo implementa un endpoint inventado,
> `GET /projects/{id}/carga`, que cuenta cuántas tareas de un proyecto tienen responsable y cuántas
> no. Úsalo para copiar la **forma** (anotaciones, imports, nombres, estructura de los tests), no el
> contenido. El código de este archivo compila y sus tests pasan contra `taskflow-api` (verificado
> con Spring Boot 3.5.3).

Respuesta del ejemplo:

```json
{"projectId":1,"conResponsable":4,"sinResponsable":1}
```

## 1. DTO de salida — `src/main/java/com/taskflow/dto/CargaProyectoResponse.java`

```java
package com.taskflow.dto;

/**
 * CargaProyectoResponse — contrato de salida de GET /projects/{id}/carga.
 * Un record: Jackson lo serializa por sus componentes, en este orden.
 */
public record CargaProyectoResponse(
        Long projectId,
        long conResponsable,
        long sinResponsable
) {
}
```

## 2. Mapper — método nuevo en `src/main/java/com/taskflow/mapper/ProjectMapper.java`

```java
    /** Conteos ya calculados por el service -> DTO de salida de GET /projects/{id}/carga. */
    public static CargaProyectoResponse aCarga(Long projectId, long conResponsable, long sinResponsable) {
        return new CargaProyectoResponse(projectId, conResponsable, sinResponsable);
    }
```

(y el import `com.taskflow.dto.CargaProyectoResponse` arriba del archivo).

## 3. Service — método nuevo en `src/main/java/com/taskflow/service/ProjectService.java`

`ProjectService` ya tiene `taskRepository`: no hace falta inyectar nada nuevo. Recibe el proyecto ya
encontrado (el 404 lo resuelve el controller, como en `GET /projects/{id}/tasks`).

```java
    /**
     * Carga de un proyecto: cuántas de sus tareas tienen responsable y cuántas no.
     * Reutiliza ReportService.SIN_ASIGNAR (no reescribe la regla).
     */
    public CargaProyectoResponse carga(Project proyecto) {
        List<Task> tareas = taskRepository.findByProjectId(proyecto.getId());
        long sinResponsable = tareas.stream().filter(ReportService.SIN_ASIGNAR).count();
        return ProjectMapper.aCarga(proyecto.getId(), tareas.size() - sinResponsable, sinResponsable);
    }
```

(imports: `com.taskflow.dto.CargaProyectoResponse`, `com.taskflow.mapper.ProjectMapper`).

## 4. Controller — método nuevo en `src/main/java/com/taskflow/controller/ProjectController.java`

```java
    /** GET /projects/{id}/carga — 200 con los conteos; 404 uniforme si el proyecto no existe. */
    @Operation(summary = "Carga de un proyecto",
            description = "Cuántas tareas del proyecto tienen responsable y cuántas no. 404 si el proyecto no existe.")
    @GetMapping("/projects/{id}/carga")
    public CargaProyectoResponse getCarga(@PathVariable("id") Long id) {
        Project proyecto = projectService.buscarPorId(id)
                .orElseThrow(() -> new ProjectNotFoundException(id));
        return projectService.carga(proyecto);
    }
```

(import: `com.taskflow.dto.CargaProyectoResponse`; `Project` y `ProjectNotFoundException` ya están importados).

## 5. Test unitario (clase nueva) — `src/test/java/com/taskflow/unit/CargaProyectoServiceTest.java`

```java
package com.taskflow.unit;

import com.taskflow.dto.CargaProyectoResponse;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Project;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.ProjectRepository;
import com.taskflow.repository.TaskRepository;
import com.taskflow.repository.UserRepository;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.mockito.Mockito.when;

/** Unit de ProjectService.carga: sin Spring, repositorios mockeados, tareas reales. */
@ExtendWith(MockitoExtension.class)
class CargaProyectoServiceTest {

    // Un @Mock por CADA dependencia del constructor de ProjectService.
    @Mock
    private ProjectRepository projectRepository;

    @Mock
    private TaskRepository taskRepository;

    @Mock
    private UserRepository userRepository;

    @InjectMocks
    private ProjectService service;

    private final Project proyecto = new Project(1L, "Plataforma", "d", 1L, null);

    @Test
    void carga_cuentaConYSinResponsable() throws TaskValidationException {
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of(
                tarea(1L, 1L), tarea(2L, null), tarea(3L, 2L)));

        // 3 tareas y solo la 2 sin responsable -> 2 con, 1 sin (contado a mano, no copiado del código)
        assertEquals(new CargaProyectoResponse(1L, 2, 1), service.carga(proyecto));
    }

    @Test
    void carga_proyectoSinTareas_todoEnCero() {
        when(taskRepository.findByProjectId(1L)).thenReturn(List.of());

        assertEquals(new CargaProyectoResponse(1L, 0, 0), service.carga(proyecto));
    }

    private Task tarea(Long id, Long assigneeId) throws TaskValidationException {
        return new Task(id, "Tarea " + id, "d", TaskStatus.TODO, Priority.MED, 1L, assigneeId, null);
    }
}
```

## 6. Test slice (clase nueva) — `src/test/java/com/taskflow/slice/CargaProyectoControllerTest.java`

```java
package com.taskflow.slice;

import com.taskflow.controller.ProjectController;
import com.taskflow.dto.CargaProyectoResponse;
import com.taskflow.model.Project;
import com.taskflow.security.JwtAuthenticationFilter;
import com.taskflow.service.ProjectService;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
import org.springframework.test.context.bean.override.mockito.MockitoBean;
import org.springframework.test.web.servlet.MockMvc;

import java.util.Optional;

import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.when;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

/** Slice web de GET /projects/{id}/carga: solo la capa HTTP; la lógica se prueba en el unit. */
@WebMvcTest(ProjectController.class)
@AutoConfigureMockMvc(addFilters = false)   // la seguridad se prueba en integration/, no aquí
class CargaProyectoControllerTest {

    @Autowired
    private MockMvc mockMvc;

    @MockitoBean
    private ProjectService projectService;

    // Sin este mock el slice intenta crear el filtro JWT real y falla por JwtService.
    @MockitoBean
    private JwtAuthenticationFilter jwtAuthenticationFilter;

    @Test
    void getCarga_existente_devuelve200ConCadaCampo() throws Exception {
        when(projectService.buscarPorId(1L)).thenReturn(Optional.of(new Project(1L, "Plataforma", "d", 1L, null)));
        when(projectService.carga(any(Project.class))).thenReturn(new CargaProyectoResponse(1L, 4, 1));

        mockMvc.perform(get("/projects/1/carga"))
                .andExpect(status().isOk())
                .andExpect(jsonPath("$.projectId").value(1))
                .andExpect(jsonPath("$.conResponsable").value(4))
                .andExpect(jsonPath("$.sinResponsable").value(1));
    }

    @Test
    void getCarga_proyectoInexistente_devuelve404() throws Exception {
        when(projectService.buscarPorId(99L)).thenReturn(Optional.empty());

        mockMvc.perform(get("/projects/99/carga"))
                .andExpect(status().isNotFound())
                .andExpect(jsonPath("$.status").value(404));
    }
}
```

## 7. Comprobar

```powershell
mvn -q test
```

Termina sin líneas `[ERROR]` y con código de salida 0. `git status --porcelain src/test` muestra solo
líneas que empiezan por `??` (archivos nuevos): ningún test existente cambió.
````

</details>


---

<sub>45s</sub>

### Copilot

Leyendo Task.java, TaskMapper, TaskResponse, TaskStatusUpdateRequest, TaskService y TaskController para seguir las convenciones de nombres y métodos antes de crear/editar archivos. Luego crearé DTO, añadiré TaskService.reasignar, y el endpoint en TaskController; terminaré con los dos tests y ejecutaré mvn -q test. Ahora leyendo los archivos necesarios.

---

<sub>45s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\model\Task.java.**

C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\model\Task.java

<details>
<summary>225 lines</summary>

```
package com.taskflow.model;

import com.taskflow.exception.TaskValidationException;
import jakarta.persistence.Column;
import jakarta.persistence.Entity;
import jakarta.persistence.EnumType;
import jakarta.persistence.Enumerated;
import jakarta.persistence.FetchType;
import jakarta.persistence.GeneratedValue;
import jakarta.persistence.GenerationType;
import jakarta.persistence.Id;
import jakarta.persistence.JoinColumn;
import jakarta.persistence.ManyToOne;
import jakarta.persistence.Table;

import java.time.LocalDate;
import java.util.Objects;

/**
 * Task — entidad central del dominio TaskFlow (persistida con JPA).
 *
 * Es una clase de dominio normal (8 campos, validaciones, comportamiento): NO hay un "modelo de
 * BD" aparte. Lo que la hace entidad, todo por anotación:
 *   - @Entity + @Table(name = "tasks"): esta clase mapea a la tabla TASKS.
 *   - @Id + @GeneratedValue(IDENTITY): el id lo asigna la BD (columna autoincremental). Nada de
 *     secuencia manual en Java: el id lo pone la BD y nadie más.
 *   - @Enumerated(STRING) en status y priority: se guardan como TEXTO ('TODO'), NUNCA como ORDINAL
 *     (0,1,2): reordenar o insertar un enum corrompería datos históricos. El orden LOW,MED,HIGH
 *     es contrato del SORT (compareTo), no del almacenamiento.
 *   - relación @ManyToOne Task->Project (patrón "asociación de solo lectura"): la COLUMNA la
 *     manda el escalar 'projectId' (por eso Task.crear, TaskService, mappers y DTOs hablan de projectId);
 *     el objeto 'project' solo NAVEGA. Frase ancla: "la columna manda; el objeto navega".
 *
 * Tensión JPA vs validaciones: JPA rehidrata
 * por reflexión con un constructor sin args protegido, SIN pasar por las validaciones. Está bien:
 * Task.crear(...) sigue siendo el ÚNICO camino de CREACIÓN de negocio; JPA solo REHIDRATA datos que
 * ya existían (la distinción crear/rehidratar).
 *
 * OJO: las columnas persistentes NO pueden ser 'final' (JPA las escribe por reflexión tras el no-arg);
 * por eso title/description/projectId/dueDate no son 'final'. La inmutabilidad
 * de negocio la sigue garantizando la ausencia de setters públicos para esos campos.
 */
@Entity
@Table(name = "tasks")
public class Task implements Comparable<Task> {

    // Reglas de longitud del título (regla de negocio).
    private static final int TITULO_MIN = 3;
    private static final int TITULO_MAX = 120;

    // id nace null; lo asigna la BD (IDENTITY). Ver setId (uso interno de JPA/repo).
    @Id
    @GeneratedValue(strategy = GenerationType.IDENTITY)
    private Long id;

    @Column(nullable = false, length = 120)
    private String title;

    private String description;                 // por convención -> columna DESCRIPTION

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private TaskStatus status;                  // texto 'TODO'/'IN_PROGRESS'/'DONE' (nunca 0/1/2)

    @Enumerated(EnumType.STRING)
    @Column(nullable = false)
    private Priority priority;                  // texto 'LOW'/'MED'/'HIGH'

    // La COLUMNA project_id la posee este escalar (insertable/updatable por default). Sigue siendo
    // el dueño: Task.crear, el mapper y el servicio hablan con projectId, no con el objeto.
    @Column(name = "project_id", nullable = false)
    private Long projectId;

    // assigneeId se queda Long a secas (juicio explícito): es nullable, NUNCA navegamos al User
    // completo al leer tareas, y las derived queries operan igual. No toda FK necesita ser objeto:
    // mapea objeto cuando NAVEGAS, deja el id cuando solo REFERENCIAS.
    @Column(name = "assignee_id")
    private Long assigneeId;

    private LocalDate dueDate;                  // por convención -> columna DUE_DATE (camelCase->snake_case)

    /**
     * Asociación de SOLO LECTURA hacia el proyecto. Comparte la MISMA columna project_id con
     * el escalar de arriba, pero marcada insertable=false/updatable=false: no escribe la columna (el
     * escalar lo hace), solo permite NAVEGAR (task.getProject().getName()). LAZY: no se carga hasta
     * que se toca — y fuera de una sesión abierta, tocarla lanza LazyInitializationException (por eso
     * la API responde con DTOs, nunca con la entidad).
     */
    @ManyToOne(fetch = FetchType.LAZY)
    @JoinColumn(name = "project_id", insertable = false, updatable = false)
    private Project project;

    /**
     * Constructor NO-ARG protegido: EXCLUSIVO de JPA (rehidratación por reflexión). Nunca es un camino
     * de creación de negocio. Su existencia es obligatoria para cualquier @Entity: sin él, al arrancar
     * Hibernate lanza "No default constructor for entity: com.taskflow.model.Task".
     */
    protected Task() {
        // solo para JPA: rehidratación, nunca creación de negocio
    }

    /**
     * Constructor de REHIDRATACIÓN: reconstruye una tarea que YA existía (semilla del DataSeeder,
     * datos releídos). Valida las INVARIANTES que siempre deben cumplirse (título y proyecto), pero
     * NO la regla temporal de dueDate: una tarea vencida es un dato válido que hay que poder releer.
     */
    public Task(Long id, String title, String description, TaskStatus status,
                Priority priority, Long projectId, Long assigneeId, LocalDate dueDate)
            throws TaskValidationException {
        // Invariante 1: título obligatorio, 3-120 chars.
        if (title == null || title.isBlank()) {
            throw new TaskValidationException("El título es obligatorio (no puede ir vacío).");
        }
        if (title.length() < TITULO_MIN || title.length() > TITULO_MAX) {
            throw new TaskValidationException(
                    "El título debe tener entre " + TITULO_MIN + " y " + TITULO_MAX
                            + " caracteres; recibí " + title.length() + ": \"" + title + "\".");
        }
        // Invariante 2: una tarea no puede existir sin proyecto (regla de negocio).
        if (projectId == null) {
            throw new TaskValidationException(
                    "Una tarea no puede existir sin proyecto (projectId == null).");
        }
        this.id = id;
        this.title = title;
        this.description = description;
        this.status = status;
        this.priority = priority;
        this.projectId = projectId;
        this.assigneeId = assigneeId;
        this.dueDate = dueDate;
    }

    /**
     * Factory de CREACIÓN de negocio: añade la regla temporal (dueDate no en el pasado) y delega en
     * el constructor. id nace null, status nace TODO. La usan el mapper y el servicio tal cual: que
     * Task sea @Entity no cambia esta firma.
     */
    public static Task crear(String title, String description, Priority priority,
                             LocalDate dueDate, Long projectId, Long assigneeId)
            throws TaskValidationException {
        if (dueDate != null && dueDate.isBefore(LocalDate.now())) {
            throw new TaskValidationException(
                    "La fecha límite no puede estar en el pasado: " + dueDate + ".");
        }
        return new Task(null, title, description, TaskStatus.TODO, priority, projectId, assigneeId, dueDate);
    }

    // ---- Comportamiento: vive CON los datos ----

    /** true si tiene fecha, ya pasó, y la tarea aún no está terminada. */
    public boolean estaVencida() {
        return dueDate != null && dueDate.isBefore(LocalDate.now()) && status != TaskStatus.DONE;
    }

    // ---- Getters ----

    public Long getId() {
        return id;
    }

    /** Uso interno (JPA/repo) para el id autoincremental. Nadie más lo llama a mano. */
    public void setId(Long id) {
        this.id = id;
    }

    public String getTitle() {
        return title;
    }

    public String getDescription() {
        return description;
    }

    public TaskStatus getStatus() {
        return status;
    }

    public Priority getPriority() {
        return priority;
    }

    public Long getProjectId() {
        return projectId;
    }

    public Long getAssigneeId() {
        return assigneeId;
    }

    public LocalDate getDueDate() {
        return dueDate;
    }

    /**
     * Navega al proyecto asociado (asociación de solo lectura). Fuera de una sesión JPA abierta puede
     * lanzar LazyInitializationException (por eso la API expone DTOs, no la entidad). En el runtime
     * nadie del camino principal lo usa; existe para NAVEGAR cuando haga falta.
     */
    public Project getProject() {
        return project;
    }

    // ---- Setters SOLO donde el dominio muta ----

    /**
     * Cambia el estado aplicando la regla de negocio: no se puede pasar a DONE una tarea sin
     * responsable (assigneeId). La regla vive AQUÍ, no en el servicio ni en el controller.
     */
    public void setStatus(TaskStatus status) throws TaskValidationException {
        if (status == TaskStatus.DONE && assigneeId == null) {
            throw new TaskValidationException(
                    "No se puede marcar como " + TaskStatus.DONE
                            + " una tarea sin responsable: \"" + title + "\".");
        }
        this.status = status;
    }

    public void setPriority(Priority priority) {
        this.priority = priority;
    }

    public void setAssigneeId(Long assigneeId) {
        this.assigneeId = assigneeId;
    }

    // ---- Identidad de ENTIDAD por id (literalmente lo que hace JPA) ----

    @Override
    public boolean equals(Object o) {
        if (this == o) {
            return true;
        }
        if (o == null || getClass() != o.getClass()) {
            return false;
        }
        Task otra = (Task) o;
        return Objects.equals(id, otra.id);
    }

    @Override
    public int hashCode() {
        return Objects.hash(id);
    }

    // ---- Orden natural por prioridad (Comparable) ----

    @Override
    public int compareTo(Task otra) {
        return this.priority.compareTo(otra.priority);
    }

    @Override
    public String toString() {
        return "Task{id=" + id
                + ", title='" + title + '\''
                + ", status=" + status
                + ", priority=" + priority
                + ", assigneeId=" + (assigneeId == null ? "sin asignar" : assigneeId)
                + ", dueDate=" + (dueDate == null ? "sin fecha" : dueDate)
                + (estaVencida() ? " *VENCIDA*" : "")
                + '}';
    }
}
```

</details>


---

<sub>48s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\dto\TaskResponse.java.**

C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\dto\TaskResponse.java

<details>
<summary>24 lines</summary>

```
package com.taskflow.dto;

import com.taskflow.model.Priority;
import com.taskflow.model.TaskStatus;

import java.time.LocalDate;

/**
 * TaskResponse — el CONTRATO de SALIDA de una tarea (lo que la API devuelve). Es un record: Jackson
 * lo serializa a JSON por sus componentes.
 *
 * "¿Si trae los mismos 8 campos que la entidad, para qué el DTO?" — son casi iguales; el valor
 * es el DESACOPLE. La entidad Task lleva anotaciones JPA (@Entity/@Id...) que NO tienen por qué
 * asomar en el JSON, y hay entidades con campos que JAMÁS deben salir (el passwordHash de User). El
 * contrato de salida se mantiene estable aunque la entidad cambie por dentro.
 */
public record TaskResponse(
        Long id,
        String title,
        String description,
        TaskStatus status,
        Priority priority,
        Long projectId,
        Long assigneeId,
        LocalDate dueDate
) {
}
```

</details>


---

<sub>50s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\dto\TaskStatusUpdateRequest.java.**

C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\dto\TaskStatusUpdateRequest.java

<details>
<summary>17 lines</summary>

```
package com.taskflow.dto;

import com.taskflow.model.TaskStatus;
import jakarta.validation.constraints.NotNull;

/**
 * TaskStatusUpdateRequest — el cuerpo del PATCH /tasks/{id}/status. Un DTO por OPERACIÓN: el
 * único campo que este endpoint puede tocar es el estado. Es la respuesta al riesgo del PUT: en vez
 * de reemplazar TODA la tarea para cambiar un campo, un DTO chico y preciso.
 *
 * @NotNull sobre el enum: si el JSON trae {"status": null} o no trae la clave, la validación da 400.
 * OJO al orden parseo -> validación: {"status":"FINISHED"} (valor que no matchea el enum) NUNCA llega
 * a @NotNull; truena antes, en la deserialización de Jackson (HttpMessageNotReadableException -> 400).
 */
public record TaskStatusUpdateRequest(

        @NotNull(message = "El estado es obligatorio.")
        TaskStatus status
) {
}
```

</details>


---

<sub>1m 29s</sub>

### Copilot

Aplicando cambios de código: crear DTO TaskAssigneeUpdateRequest, añadir TaskService.reasignar(...) y endpoint PATCH /tasks/{id}/assignee en TaskController, y crear los tests unit y slice. Luego ejecutar mvn -q test para verificar la suite. Ahora editando/creando los archivos necesarios.

---

<sub>1m 29s</sub>

### `edit`

**edit the file at C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\service\TaskService.java.**

C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\service\TaskService.java

```
File C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\service\TaskService.java updated with changes.
```


---

<sub>1m 29s</sub>

### `edit`

**edit the file at C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\controller\TaskController.java.**

C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\controller\TaskController.java

```
File C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\controller\TaskController.java updated with changes.
```


---

<sub>1m 29s</sub>

### `create`

**create a new file at C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\dto\TaskAssigneeUpdateRequest.java.**

C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\dto\TaskAssigneeUpdateRequest.java

```
Created file C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\dto\TaskAssigneeUpdateRequest.java with 433 characters
```


---

<sub>1m 29s</sub>

### `create`

**create a new file at C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\test\java\com\taskflow\unit\ReasignarTareaServiceTest.java.**

C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\test\java\com\taskflow\unit\ReasignarTareaServiceTest.java

```
Created file C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\test\java\com\taskflow\unit\ReasignarTareaServiceTest.java with 1945 characters
```


---

<sub>1m 29s</sub>

### `create`

**create a new file at C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\test\java\com\taskflow\slice\ReasignarTareaControllerTest.java.**

C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\test\java\com\taskflow\slice\ReasignarTareaControllerTest.java

```
Created file C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\test\java\com\taskflow\slice\ReasignarTareaControllerTest.java with 4194 characters
```


---

<sub>1m 33s</sub>

### Copilot

Running mvn -q test to verify the new endpoint and tests pass (executes full test suite quietly).

---

<sub>2m 3s</sub>

### `powershell`

**Run Maven tests quietly**

<details>
<summary>Arguments</summary>

```json
{
  "command": "mvn -q test",
  "description": "Run Maven tests quietly",
  "initial_wait": 120
}
```

</details>

<details>
<summary>6 lines</summary>

```
Output too large to read at once (29.9 KB). Saved to: C:\Users\Lenovo\AppData\Local\Temp\1792273979951-copilot-tool-output-13944-e1305a35-e401-434f-874b-fb4d6a53266b.txt
Consider using tools like grep (for searching), head/tail (for viewing start/end), view with view_range (for specific sections), or jq (for JSON) to examine portions of the output.

Preview (first 500 chars):
15:52:41.734 [main] INFO org.springframework.test.context.support.AnnotationConfigContextLoaderUtils -- Could not detect default configuration classes for test class [com.taskflow.integration.AuthControllerTest]: AuthControllerTest does not declare any static, non-private, non-final, nested classes annotated with @Configuration.
15:52:42.069 [main] INFO org.springframework.boot.test.context.SpringBootTestContextBootstrapper -- Found @SpringBootConfiguration com.taskflow.TaskflowApiApplication fo
<shellId: 0 completed with exit code 0>
```

</details>


---

<sub>2m 7s</sub>

### `view`

**view the file at C:\Users\Lenovo\AppData\Local\Temp\1792273979951-copilot-tool-output-13944-e1305a35-e401-434f-874b-fb4d6a53266b.txt.**

C:\Users\Lenovo\AppData\Local\Temp\1792273979951-copilot-tool-output-13944-e1305a35-e401-434f-874b-fb4d6a53266b.txt

```
File too large to read at once (29.9 KB). Consider using the grep tool to search within the file, or view with view_range to read specific sections.
```


---

<sub>2m 10s</sub>

### `read_powershell`

**Waiting up to 1 second for command output**

<details>
<summary>Arguments</summary>

```json
{
  "shellId": "0",
  "delay": 1
}
```

</details>

```

<shellId: 0 completed with exit code 0>
```


---

<sub>2m 16s</sub>

### Copilot

Hecho — endpoint PATCH /tasks/{id}/assignee implementado y tests añadidos.

Archivos creados:
- src/main/java/com/taskflow/dto/TaskAssigneeUpdateRequest.java
- src/test/java/com/taskflow/unit/ReasignarTareaServiceTest.java
- src/test/java/com/taskflow/slice/ReasignarTareaControllerTest.java

Archivos modificados:
- src/main/java/com/taskflow/service/TaskService.java
- src/main/java/com/taskflow/controller/TaskController.java

Resultado de la verificación:
- mvn -q test → exit code 0 (la suite de tests pasó correctamente).

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
