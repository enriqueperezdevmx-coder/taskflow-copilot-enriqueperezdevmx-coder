# Copilot CLI Session

> [!NOTE]
> - **Session ID:** `9abc9c73-ddf3-44e5-9182-ec8786c71e4d`  
> - **Started:** 17/10/2026, 5:27:07 p.m.  
> - **Duration:** 38s  
> - **Exported:** 17/10/2026, 5:27:46 p.m.  

---

<sub>3s</sub>

### User

Revisa semana6/proyecto-final.diff contra la especificación specs/assignee.md.

---

<sub>6s</sub>

### Copilot

Reading the diff and the specification to identify changed files and then opening the current versions of those files for review. Running parallel file reads for semana6/proyecto-final.diff and specs/assignee.md.

---

<sub>6s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\semana6\proyecto-final.diff.**

C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\semana6\proyecto-final.diff

<details>
<summary>222 lines</summary>

```diff
diff --git a/src/main/java/com/taskflow/controller/TaskController.java b/src/main/java/com/taskflow/controller/TaskController.java
index 2f7f38b..fd07072 100644
--- a/src/main/java/com/taskflow/controller/TaskController.java
+++ b/src/main/java/com/taskflow/controller/TaskController.java
@@ -153,4 +153,15 @@ public class TaskController {
         Task actualizada = taskService.cambiarStatus(id, request.status());
         return TaskMapper.aResponse(actualizada);
     }
+
+    @Operation(summary = "Reasigna el responsable de una tarea",
+            description = "Cambia sólo el assigneeId de la tarea. 404 si la tarea no existe; 422 si la tarea está DONE; 400 si el body es inválido.")
+    @PatchMapping("/tasks/{id}/assignee")
+    public TaskResponse patchAssignee(@PathVariable("id") Long id,
+                                      @Valid @RequestBody com.taskflow.dto.TaskAssigneeUpdateRequest request) {
+        Task tarea = taskService.buscarPorId(id)
+                .orElseThrow(() -> new TaskNotFoundException(id));
+        Task actualizada = taskService.reasignar(tarea, request.assigneeId());
+        return TaskMapper.aResponse(actualizada);
+    }
 }
diff --git a/src/main/java/com/taskflow/dto/TaskAssigneeUpdateRequest.java b/src/main/java/com/taskflow/dto/TaskAssigneeUpdateRequest.java
new file mode 100644
index 0000000..9c84095
--- /dev/null
+++ b/src/main/java/com/taskflow/dto/TaskAssigneeUpdateRequest.java
@@ -0,0 +1,15 @@
+package com.taskflow.dto;
+
+import jakarta.validation.constraints.NotNull;
+import jakarta.validation.constraints.Positive;
+
+/**
+ * TaskAssigneeUpdateRequest — el cuerpo del PATCH /tasks/{id}/assignee.
+ */
+public record TaskAssigneeUpdateRequest(
+
+        @NotNull(message = "El responsable es obligatorio.")
+        @Positive(message = "El responsable debe ser un número mayor que cero.")
+        Long assigneeId
+) {
+}
diff --git a/src/main/java/com/taskflow/service/TaskService.java b/src/main/java/com/taskflow/service/TaskService.java
index f5734ab..43e52dc 100644
--- a/src/main/java/com/taskflow/service/TaskService.java
+++ b/src/main/java/com/taskflow/service/TaskService.java
@@ -141,5 +141,16 @@ public class TaskService {
                 .sorted(TaskOrders.POR_FECHA)
                 .toList();
     }
+
+    /**
+     * Reasigna el responsable de una tarea ya encontrada. Lanza TaskStateException si la tarea está DONE.
+     */
+    public Task reasignar(Task tarea, Long assigneeId) {
+        if (tarea.getStatus() == TaskStatus.DONE) {
+            throw new com.taskflow.exception.TaskStateException("No se puede reasignar una tarea terminada.");
+        }
+        tarea.setAssigneeId(assigneeId);
+        return repository.save(tarea);
+    }
 }
 
diff --git a/src/test/java/com/taskflow/slice/ReasignarTareaControllerTest.java b/src/test/java/com/taskflow/slice/ReasignarTareaControllerTest.java
new file mode 100644
index 0000000..c2ae2c4
--- /dev/null
+++ b/src/test/java/com/taskflow/slice/ReasignarTareaControllerTest.java
@@ -0,0 +1,96 @@
+package com.taskflow.slice;
+
+import com.taskflow.controller.TaskController;
+import com.taskflow.model.Priority;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.security.JwtAuthenticationFilter;
+import com.taskflow.service.ProjectService;
+import com.taskflow.service.TaskService;
+import org.junit.jupiter.api.Test;
+import org.springframework.beans.factory.annotation.Autowired;
+import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
+import org.springframework.boot.test.autoconfigure.web.servlet.WebMvcTest;
+import org.springframework.http.MediaType;
+import org.springframework.test.context.bean.override.mockito.MockitoBean;
+import org.springframework.test.web.servlet.MockMvc;
+
+import java.util.Optional;
+
+import static org.mockito.ArgumentMatchers.any;
+import static org.mockito.ArgumentMatchers.eq;
+import static org.mockito.Mockito.when;
+import static org.mockito.Mockito.verify;
+import static org.mockito.Mockito.never;
+import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.patch;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.jsonPath;
+import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;
+
+@WebMvcTest(TaskController.class)
+@AutoConfigureMockMvc(addFilters = false)
+class ReasignarTareaControllerTest {
+
+    @Autowired
+    private MockMvc mockMvc;
+
+    @MockitoBean
+    private TaskService taskService;
+
+    @MockitoBean
+    private ProjectService projectService;
+
+    @MockitoBean
+    private JwtAuthenticationFilter jwtAuthenticationFilter;
+
+    @Test
+    void patchAssignee_existente_devuelve200ConAssignee() throws Exception {
+        Task tareaBefore = new Task(1L, "Tarea 1", "d", TaskStatus.TODO, Priority.MED, 1L, null, null);
+        Task tareaAfter = new Task(1L, "Tarea 1", "d", TaskStatus.TODO, Priority.MED, 1L, 2L, null);
+
+        when(taskService.buscarPorId(1L)).thenReturn(Optional.of(tareaBefore));
+        when(taskService.reasignar(any(Task.class), eq(2L))).thenReturn(tareaAfter);
+
+        mockMvc.perform(patch("/tasks/1/assignee")
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content("{\"assigneeId\":2}"))
+                .andExpect(status().isOk())
+                .andExpect(jsonPath("$.assigneeId").value(2));
+    }
+
+    @Test
+    void patchAssignee_tareaDONE_devuelve422() throws Exception {
+        Task tareaDone = new Task(2L, "Tarea 2", "d", TaskStatus.DONE, Priority.MED, 1L, 1L, null);
+        when(taskService.buscarPorId(2L)).thenReturn(Optional.of(tareaDone));
+        when(taskService.reasignar(any(Task.class), any())).thenThrow(new com.taskflow.exception.TaskStateException("No se puede reasignar una tarea terminada."));
+
+        mockMvc.perform(patch("/tasks/2/assignee")
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content("{\"assigneeId\":3}"))
+                .andExpect(status().isUnprocessableEntity())
+                .andExpect(jsonPath("$.status").value(422));
+    }
+
+    @Test
+    void patchAssignee_proyectoInexistente_devuelve404() throws Exception {
+        when(taskService.buscarPorId(99L)).thenReturn(Optional.empty());
+
+        mockMvc.perform(patch("/tasks/99/assignee")
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content("{\"assigneeId\":2}"))
+                .andExpect(status().isNotFound())
+                .andExpect(jsonPath("$.status").value(404));
+    }
+
+    @Test
+    void patchAssignee_cuerpoInvalido_devuelve400_y_noLlamaReasignar() throws Exception {
+        Task tarea = new Task(6L, "Tarea 6", "d", TaskStatus.TODO, Priority.MED, 1L, null, null);
+        when(taskService.buscarPorId(6L)).thenReturn(Optional.of(tarea));
+
+        mockMvc.perform(patch("/tasks/6/assignee")
+                        .contentType(MediaType.APPLICATION_JSON)
+                        .content("{}"))
+                .andExpect(status().isBadRequest());
+
+        verify(taskService, never()).reasignar(any(Task.class), any());
+    }
+}
diff --git a/src/test/java/com/taskflow/unit/ReasignarTareaServiceTest.java b/src/test/java/com/taskflow/unit/ReasignarTareaServiceTest.java
new file mode 100644
index 0000000..1bdc1bf
--- /dev/null
+++ b/src/test/java/com/taskflow/unit/ReasignarTareaServiceTest.java
@@ -0,0 +1,53 @@
+package com.taskflow.unit;
+
+import com.taskflow.exception.TaskStateException;
+import com.taskflow.exception.TaskValidationException;
+import com.taskflow.model.Priority;
+import com.taskflow.model.Task;
+import com.taskflow.model.TaskStatus;
+import com.taskflow.repository.TaskRepository;
+import com.taskflow.service.TaskService;
+import org.junit.jupiter.api.Test;
+import org.junit.jupiter.api.extension.ExtendWith;
+import org.mockito.ArgumentCaptor;
+import org.mockito.InjectMocks;
+import org.mockito.Mock;
+import org.mockito.junit.jupiter.MockitoExtension;
+
+import static org.junit.jupiter.api.Assertions.assertEquals;
+import static org.junit.jupiter.api.Assertions.assertThrows;
+import static org.mockito.Mockito.any;
+import static org.mockito.Mockito.never;
+import static org.mockito.Mockito.verify;
+import static org.mockito.Mockito.when;
+
+@ExtendWith(MockitoExtension.class)
+class ReasignarTareaServiceTest {
+
+    @Mock
+    private TaskRepository taskRepository;
+
+    @InjectMocks
+    private TaskService service;
+
+    @Test
+    void reasignar_tareaTODO_guardaConNuevoResponsable() throws TaskValidationException {
+        Task tarea = new Task(1L, "Tarea 1", "d", TaskStatus.TODO, Priority.MED, 1L, null, null);
+        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));
+
+        service.reasignar(tarea, 2L);
+
+        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
+        verify(taskRepository).save(captor.capture());
+        assertEquals(2L, captor.getValue().getAssigneeId());
+    }
+
+    @Test
+    void reasignar_tareaDONE_lanzaTaskStateException_y_noGuarda() throws TaskValidationException {
+        Task tarea = new Task(2L, "Tarea 2", "d", TaskStatus.DONE, Priority.MED, 1L, 1L, null);
+
+        assertThrows(TaskStateException.class, () -> service.reasignar(tarea, 3L));
+
+        verify(taskRepository, never()).save(any());
+    }
+}
```

</details>


---

<sub>6s</sub>

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

<sub>21s</sub>

### Copilot

Opening current TaskController.java, TaskService.java and the existing TaskStatusUpdateRequest DTO to compare implementations and conventions with the spec. Then produce the review table.

---

<sub>21s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\controller\TaskController.java.**

C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\controller\TaskController.java

<details>
<summary>153 lines</summary>

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

    @Operation(summary = "Reasigna el responsable de una tarea",
            description = "Cambia sólo el assigneeId de la tarea. 404 si la tarea no existe; 422 si la tarea está DONE; 400 si el body es inválido.")
    @PatchMapping("/tasks/{id}/assignee")
    public TaskResponse patchAssignee(@PathVariable("id") Long id,
                                      @Valid @RequestBody com.taskflow.dto.TaskAssigneeUpdateRequest request) {
        Task tarea = taskService.buscarPorId(id)
                .orElseThrow(() -> new TaskNotFoundException(id));
        Task actualizada = taskService.reasignar(tarea, request.assigneeId());
        return TaskMapper.aResponse(actualizada);
    }
}
```

</details>


---

<sub>21s</sub>

### `view`

**view the file at C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\service\TaskService.java.**

C:\Users\Lenovo\taskflow-copilot-enriqueperezdevmx-coder\src\main\java\com\taskflow\service\TaskService.java

<details>
<summary>138 lines</summary>

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

    /**
     * Reasigna el responsable de una tarea ya encontrada. Lanza TaskStateException si la tarea está DONE.
     */
    public Task reasignar(Task tarea, Long assigneeId) {
        if (tarea.getStatus() == TaskStatus.DONE) {
            throw new com.taskflow.exception.TaskStateException("No se puede reasignar una tarea terminada.");
        }
        tarea.setAssigneeId(assigneeId);
        return repository.save(tarea);
    }
}
```

</details>


---

<sub>21s</sub>

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

<sub>38s</sub>

### Copilot

Resumen breve: el diff implementa exactamente lo pedido por la specs/assignee.md. Los cambios siguen las convenciones y los tests exigidos están presentes. Revisión detallada:

| # | Severidad | Archivo:línea | Punto checklist | Qué pasa | Qué harías |
|---|---:|---|---|---|---|
| 1 | Sugerencia | src/main/java/com/taskflow/controller/TaskController.java:~153 | 4,5 (Contrato/Convenciones) | Se usa el nombre de clase totalmente cualificado en el parámetro (@RequestBody com.taskflow.dto.TaskAssigneeUpdateRequest) — funciona pero rompe la consistencia con el resto del archivo que usa imports. | Importar TaskAssigneeUpdateRequest y usar el nombre simple para legibilidad. |
| 2 | Sugerencia | src/main/java/com/taskflow/service/TaskService.java:~141 | 3,5 (Reglas/Convenciones) | Se lanza la excepción con nombre fully-qualified pese a tenerla importada; redundante. | Usar la clase importada (TaskStateException) para consistencia. |

Casos sin test: ninguno — los 2 unit tests y los 4 slice cases de la especificación están presentes.

Veredicto: APROBADO.

---

<sub>Generated by [GitHub Copilot CLI](https://github.com/features/copilot/cli)</sub>
