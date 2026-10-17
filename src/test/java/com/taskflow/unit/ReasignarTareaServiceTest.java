package com.taskflow.unit;

import com.taskflow.exception.TaskStateException;
import com.taskflow.exception.TaskValidationException;
import com.taskflow.model.Priority;
import com.taskflow.model.Task;
import com.taskflow.model.TaskStatus;
import com.taskflow.repository.TaskRepository;
import com.taskflow.service.TaskService;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.ArgumentCaptor;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.any;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

@ExtendWith(MockitoExtension.class)
class ReasignarTareaServiceTest {

    @Mock
    private TaskRepository taskRepository;

    @InjectMocks
    private TaskService service;

    @Test
    void reasignar_tareaTODO_guardaConNuevoResponsable() throws TaskValidationException {
        Task tarea = new Task(1L, "Tarea 1", "d", TaskStatus.TODO, Priority.MED, 1L, null, null);
        when(taskRepository.save(any(Task.class))).thenAnswer(i -> i.getArgument(0));

        service.reasignar(tarea, 2L);

        ArgumentCaptor<Task> captor = ArgumentCaptor.forClass(Task.class);
        verify(taskRepository).save(captor.capture());
        assertEquals(2L, captor.getValue().getAssigneeId());
    }

    @Test
    void reasignar_tareaDONE_lanzaTaskStateException_y_noGuarda() throws TaskValidationException {
        Task tarea = new Task(2L, "Tarea 2", "d", TaskStatus.DONE, Priority.MED, 1L, 1L, null);

        assertThrows(TaskStateException.class, () -> service.reasignar(tarea, 3L));

        verify(taskRepository, never()).save(any());
    }
}
