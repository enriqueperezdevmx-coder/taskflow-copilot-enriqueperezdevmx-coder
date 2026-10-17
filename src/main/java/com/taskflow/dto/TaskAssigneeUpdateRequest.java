package com.taskflow.dto;

import jakarta.validation.constraints.NotNull;
import jakarta.validation.constraints.Positive;

/**
 * TaskAssigneeUpdateRequest — el cuerpo del PATCH /tasks/{id}/assignee.
 */
public record TaskAssigneeUpdateRequest(

        @NotNull(message = "El responsable es obligatorio.")
        @Positive(message = "El responsable debe ser un número mayor que cero.")
        Long assigneeId
) {
}
