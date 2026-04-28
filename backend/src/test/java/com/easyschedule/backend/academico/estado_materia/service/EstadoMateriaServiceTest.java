package com.easyschedule.backend.academico.estado_materia.service;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import com.easyschedule.backend.academico.estado_materia.dto.EstadoMateriaRequest;
import com.easyschedule.backend.academico.estado_materia.model.EstadoMateria;
import com.easyschedule.backend.academico.estado_materia.repository.EstadoMateriaRepository;
import com.easyschedule.backend.academico.materia.model.Materia;
import com.easyschedule.backend.academico.materia.model.Prerequisito;
import com.easyschedule.backend.academico.materia.repository.PrerequisitoRepository;
import com.easyschedule.backend.academico.malla.model.MallaMateria;
import com.easyschedule.backend.auth.models.User;
import java.time.OffsetDateTime;
import java.util.List;
import java.util.Optional;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.InjectMocks;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

@ExtendWith(MockitoExtension.class)
class EstadoMateriaServiceTest {

    @Mock
    private EstadoMateriaRepository estadoMateriaRepository;
    
    @Mock
    private PrerequisitoRepository prerequisitoRepository;

    @InjectMocks
    private EstadoMateriaService estadoMateriaService;

    @Test
    void getEstadoMateriaReturnsNullWhenNoRowExists() {
        when(estadoMateriaRepository.findByUserIdAndMallaMateria_Id(7L, 10L)).thenReturn(Optional.empty());

        String result = estadoMateriaService.getEstadoMateria(7L, 10L);

        assertNull(result);
    }

    @Test
    void getEstadoMateriaReturnsValueWhenRowExists() {
        EstadoMateria estado = new EstadoMateria();
        estado.setEstado("aprobada");
        when(estadoMateriaRepository.findByUserIdAndMallaMateria_Id(7L, 10L)).thenReturn(Optional.of(estado));

        String result = estadoMateriaService.getEstadoMateria(7L, 10L);

        assertEquals("aprobada", result);
    }

    @Test
    void syncEstadoFromTomaMapsInscritaToCursando() {
        estadoMateriaService.syncEstadoFromToma(3L, 40L, "inscrita");

        verify(estadoMateriaRepository).upsertEstado(3L, 40L, "cursando");
    }

    @Test
    void syncEstadoFromTomaMapsAprobadaToAprobada() {
        estadoMateriaService.syncEstadoFromToma(3L, 40L, "aprobada");

        verify(estadoMateriaRepository).upsertEstado(3L, 40L, "aprobada");
    }

    @Test
    void syncEstadoFromTomaMapsRetiradaToPendiente() {
        estadoMateriaService.syncEstadoFromToma(3L, 40L, "retirada");

        verify(estadoMateriaRepository).upsertEstado(3L, 40L, "pendiente");
    }

    @Test
    void markPendienteAlwaysUpsertsPendiente() {
        estadoMateriaService.markPendiente(9L, 80L);

        verify(estadoMateriaRepository).upsertEstado(9L, 80L, "pendiente");
    }

    @Test
    void saveEstadoThrowsExceptionWhenPrerequisiteIncomplete() {
        // Setup: Materia with prerequisitos
        MallaMateria mallaMateria = new MallaMateria();
        mallaMateria.setId(5L);
        
        MallaMateria prereqMateria = new MallaMateria();
        prereqMateria.setId(4L);
        Materia materia = new Materia();
        materia.setNombre("Algebra");
        prereqMateria.setMateria(materia);
        
        Prerequisito prereq = new Prerequisito();
        prereq.setMallaMateria(mallaMateria);
        prereq.setPrerequisito(prereqMateria);

        when(prerequisitoRepository.findByMallaMateria_Id(5L)).thenReturn(List.of(prereq));
        when(estadoMateriaRepository.findByUserIdAndMallaMateria_Id(1L, 4L)).thenReturn(Optional.empty());

        EstadoMateriaRequest request = new EstadoMateriaRequest(5L, "aprobada");

        IllegalArgumentException exception = assertThrows(
            IllegalArgumentException.class,
            () -> estadoMateriaService.saveEstado(1L, request)
        );

        assertEquals(
            "No se puede cambiar a completado. Primero debe completar el prerequisito: Algebra",
            exception.getMessage()
        );
    }

    @Test
    void saveEstadoSucceedsWhenAllPrerequisitesComplete() {
        // Setup: Materia with completed prerequisitos
        User user = new User();
        user.setId(1L);
        
        MallaMateria mallaMateria = new MallaMateria();
        mallaMateria.setId(5L);
        
        MallaMateria prereqMateria = new MallaMateria();
        prereqMateria.setId(4L);
        
        Prerequisito prereq = new Prerequisito();
        prereq.setMallaMateria(mallaMateria);
        prereq.setPrerequisito(prereqMateria);

        EstadoMateria completedPrereq = new EstadoMateria();
        completedPrereq.setId(2L);
        completedPrereq.setUser(user);
        completedPrereq.setMallaMateria(prereqMateria);
        completedPrereq.setEstado("aprobada");
        completedPrereq.setFechaActualizacion(OffsetDateTime.now());

        when(prerequisitoRepository.findByMallaMateria_Id(5L)).thenReturn(List.of(prereq));
        when(estadoMateriaRepository.findByUserIdAndMallaMateria_Id(1L, 4L)).thenReturn(Optional.of(completedPrereq));
        
        EstadoMateria savedEstado = new EstadoMateria();
        savedEstado.setId(1L);
        savedEstado.setUser(user);
        savedEstado.setMallaMateria(mallaMateria);
        savedEstado.setEstado("aprobada");
        savedEstado.setFechaActualizacion(OffsetDateTime.now());
        when(estadoMateriaRepository.findByUserIdAndMallaMateria_Id(1L, 5L)).thenReturn(Optional.of(savedEstado));

        EstadoMateriaRequest request = new EstadoMateriaRequest(5L, "aprobada");

        // Should not throw exception
        var result = estadoMateriaService.saveEstado(1L, request);
        
        assertEquals("aprobada", result.estado());
        verify(estadoMateriaRepository).upsertEstado(1L, 5L, "aprobada");
    }

    @Test
    void saveEstadoSucceedsWhenCourseHasNoPrerequisites() {
        User user = new User();
        user.setId(1L);
        
        MallaMateria mallaMateria = new MallaMateria();
        mallaMateria.setId(5L);

        when(prerequisitoRepository.findByMallaMateria_Id(5L)).thenReturn(List.of());
        
        EstadoMateria savedEstado = new EstadoMateria();
        savedEstado.setId(1L);
        savedEstado.setUser(user);
        savedEstado.setMallaMateria(mallaMateria);
        savedEstado.setEstado("aprobada");
        savedEstado.setFechaActualizacion(OffsetDateTime.now());
        when(estadoMateriaRepository.findByUserIdAndMallaMateria_Id(1L, 5L)).thenReturn(Optional.of(savedEstado));

        EstadoMateriaRequest request = new EstadoMateriaRequest(5L, "aprobada");

        // Should not throw exception
        var result = estadoMateriaService.saveEstado(1L, request);
        
        assertEquals("aprobada", result.estado());
        verify(estadoMateriaRepository).upsertEstado(1L, 5L, "aprobada");
    }
}
