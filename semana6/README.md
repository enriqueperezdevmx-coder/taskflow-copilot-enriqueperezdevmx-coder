# Proyecto final · Semana 6 · GitHub Copilot

> Plantilla del documento del proyecto final. Se copia a `semana6/README.md` de tu repo
> `taskflow-copilot-enriqueperezdevmx-coder` y se llena **con datos de tu repo**: cada sección dice de dónde
> sale cada valor. Borra las líneas que empiezan con `>` cuando termines.

**Alumno:** `Enrique Pérez Sánchez` · **Usuario de GitHub:** `enriqueperezdevmx-coder`

## 1. Qué construí

> Marca **una** fila con `x` y borra las otras dos.

| | Feature | Especificación |
|---|---|---|
| [ ] | `GET /tasks/search?q=` — buscar tareas por título | [`specs/search.md`](../specs/search.md) |
| [ ] | `PATCH /tasks/{id}/assignee` — cambiar el responsable | [`specs/assignee.md`](../specs/assignee.md) |
| [ ] | `GET /reports/progress` — avance por proyecto | [`specs/progress.md`](../specs/progress.md) |

## 2. El pull request

- **URL del PR (mergeado):** `https://github.com/enriqueperezdevmx-coder/taskflow-copilot-enriqueperezdevmx-coder/pull/5`
- **Commit del merge en `main`:** `dc71bdf Merge pull request #5 from enriqueperezdevmx-coder/feature/assignee`
- **Comentarios de Copilot code review:** `<número de comentarios que dejó Copilot en el PR>`

## 3. Cómo lo hice

> Una fila por paso. La columna «Evidencia» apunta a un archivo de esta carpeta o a un comando que
> cualquiera puede repetir.

| Paso | Qué hice | Evidencia |
|---|---|---|
| Rama y spec | `git switch -c feature/assignee` y copié la spec a `specs/` | `git log --oneline main..feature/assignee` (antes del merge) |
| Implementación | `copilot -p "/crear-endpoint-taskflow …"` con `gpt-5-mini` | `semana6/sesion-implementacion.md` (tiene la línea `Skill "crear-endpoint-taskflow" loaded successfully`) |
| Revisión | agente `revisor` sobre `semana6/proyecto-final.diff` | `semana6/revision.md` (termina con `Veredicto:`) |
| Tests | `mvn test` en verde | `Tests run: 82, Failures: 0, Errors: 0, Skipped: 0` |
| Comprobación REST | `verificar.ps1` con `casos-assignee.ps1` | sección 5 de este documento |
| Code review | Copilot en el PR | la pestaña *Files changed* del PR |

## 4. Qué hizo el agente y qué corregí yo

> Una fila por cada cosa que **no** quedó como la pedía la spec o la checklist: la encontró el
> revisor, Copilot code review, un test, `verificar.ps1` o tú leyendo el diff. Si el agente la
> corrigió con un prompt tuyo, dilo; si la corregiste a mano, también. Los hallazgos **falsos**
> (el revisor o Copilot pidieron algo que no hacía falta) también van: en «Qué hizo mal» escribe lo que
> pidieron y en «Cómo quedó» cómo comprobaste que era falso.

| # | Qué hizo mal el agente (archivo) | Quién lo detectó | Cómo quedó corregido |
|---|---|---|---|
| 1 | `<ejemplo: usó @MockBean en vez de @MockitoBean — ProgresoProyectosControllerTest.java>` | `<revisor / Copilot review / test / yo>` | `<prompt de corrección o cambio a mano, y commit>` |

**Lo que el agente hizo bien a la primera** (una o dos líneas): `<qué archivos o casos no tuviste que tocar>`

## 5. Comprobaciones REST

> Pega completa la salida de
> `pwsh -NoProfile -File .github/skills/verificar-taskflow/verificar.ps1` en la rama `main` después
> del merge (la última línea es `RESULTADO:`).

```text
[OK]   GET /tasks/overdue devuelve solo la tarea 7
[OK]   GET /tasks/unassigned devuelve las tareas 4 y 6
[OK]   GET /projects/1/summary
[OK]   GET /projects/2/summary
[OK]   GET /projects/3/summary
[OK]   GET /projects/99/summary responde 404
[OK]   GET /projects/1/summary sin token responde 401
[OK]   PATCH /tasks/4/assignee asigna a luis
[OK]   GET /tasks/4 conserva el responsable nuevo
[OK]   PATCH /tasks/4/status a DONE ahora responde 200
[OK]   PATCH /tasks/2/assignee (DONE) responde 422
[OK]   PATCH /tasks/99/assignee responde 404
[OK]   PATCH /tasks/6/assignee con {} responde 400 y nombra assigneeId
[OK]   PATCH /tasks/6/assignee con 0 responde 400
[OK]   PATCH /tasks/6/assignee sin token responde 401
App detenida (PID 10640).
[OK]   App apagada: el puerto 8080 ya no responde
RESULTADO: 16/16 OK
```

## 6. Créditos de la semana

> **De dónde sale cada número.** El total del mes: `https://github.com/settings/billing`, en el
> resumen por producto, el de Copilot (con **View details** ves el detalle). Los del proyecto final:
> la línea `AI Credits` que imprime cada `copilot -p` al terminar.

| Qué | AI credits |
|---|---|
| Usados en octubre según github.com (incluye semanas anteriores si usaste Copilot antes) | `<número>` |
| Implementación con la skill (`AI Credits` del PF-2) | `<número>` |
| Revisión del `revisor` (`AI Credits` del PF-3) | `<número>` |
| Correcciones del PF-4 y del PF-6, si hubo (`AI Credits`) | `<número o 0>` |
