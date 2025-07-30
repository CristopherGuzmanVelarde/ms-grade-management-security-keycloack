package pe.edu.vallegrande.vg_ms_grade_management.infrastructure.rest;

import lombok.RequiredArgsConstructor;
import org.springframework.http.HttpStatus;
import org.springframework.http.MediaType;
import org.springframework.security.access.prepost.PreAuthorize;
import org.springframework.web.bind.annotation.*;
import pe.edu.vallegrande.vg_ms_grade_management.application.service.GradeService;
import pe.edu.vallegrande.vg_ms_grade_management.application.service.NotificationService;
import pe.edu.vallegrande.vg_ms_grade_management.domain.model.Grade;
import pe.edu.vallegrande.vg_ms_grade_management.infrastructure.dto.response.NotificationResponse;
import reactor.core.publisher.Flux;
import reactor.core.publisher.Mono;
import org.springframework.web.server.ResponseStatusException;

/**
 * REST controller for managing grades.
 */
@RestController
@RequestMapping("/api/v1/grades")
@RequiredArgsConstructor
public class GradeRest {

    private final GradeService gradeService;
    private final NotificationService notificationService;

    /**
     * Retrieves all grades. Only accessible by ADMIN and TEACHER roles.
     * @return Flux of grades.
     */
    @GetMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public Flux<Grade> getAllGrades() {
        return gradeService.findAll();
    }

    /**
     * Retrieves a grade by its ID. Accessible by all authenticated users.
     * @param id Grade ID.
     * @return Mono with the found grade.
     */
    @GetMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public Mono<Grade> getGradeById(@PathVariable String id) {
        return gradeService.findById(id);
    }

    /**
     * Retrieves grades by student ID. Students can only see their own grades.
     * @param studentId Student ID.
     * @return Flux of grades.
     */
    @GetMapping(value = "/student/{studentId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or (hasRole('STUDENT') and authentication.token.claims['sub'] == #studentId)")
    public Flux<Grade> getGradesByStudentId(@PathVariable String studentId) {
        return gradeService.findByStudentId(studentId);
    }

    /**
     * Retrieves grades by course ID. Only accessible by ADMIN and TEACHER roles.
     * @param courseId Course ID.
     * @return Flux of grades.
     */
    @GetMapping(value = "/course/{courseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public Flux<Grade> getGradesByCourseId(@PathVariable String courseId) {
        return gradeService.findByCourseId(courseId);
    }

    /**
     * Retrieves grades by student ID and course ID. Students can only see their own grades.
     * @param studentId Student ID.
     * @param courseId Course ID.
     * @return Flux of grades.
     */
    @GetMapping(value = "/student/{studentId}/course/{courseId}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or (hasRole('STUDENT') and authentication.token.claims['sub'] == #studentId)")
    public Flux<Grade> getGradesByStudentIdAndCourseId(
            @PathVariable String studentId,
            @PathVariable String courseId) {
        return gradeService.findByStudentIdAndCourseId(studentId, courseId);
    }

    /**
     * Retrieves notifications related to a specific grade. Accessible by all authenticated users.
     * @param id Grade ID.
     * @return Flux of notifications related to the grade.
     */
    @GetMapping(value = "/{id}/notifications", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER') or hasRole('STUDENT')")
    public Flux<NotificationResponse> getGradeNotifications(@PathVariable String id) {
        return gradeService.findById(id)
                .flatMapMany(grade -> notificationService.findAll()
                        .filter(notification -> 
                            notification.getRecipientId() != null &&
                            grade.getStudentId() != null &&
                            notification.getRecipientId().trim().equals(grade.getStudentId().trim()) &&
                            (notification.getNotificationType().equals("Calificación Publicada") ||
                             notification.getNotificationType().equals("Calificación Actualizada") ||
                             notification.getNotificationType().equals("Bajo Rendimiento"))
                        )
                        .map(notification -> {
                            NotificationResponse response = new NotificationResponse();
                            response.setId(notification.getId());
                            response.setRecipientId(notification.getRecipientId());
                            response.setRecipientType(notification.getRecipientType());
                            response.setMessage(notification.getMessage());
                            response.setNotificationType(notification.getNotificationType());
                            response.setStatus(notification.getStatus());
                            response.setChannel(notification.getChannel());
                            response.setCreatedAt(notification.getCreatedAt());
                            response.setSentAt(notification.getSentAt());
                            return response;
                        }));
    }

    /**
     * Creates a new grade. Only accessible by ADMIN and TEACHER roles.
     * @param grade Grade to create.
     * @return Mono with the created grade.
     */
    @PostMapping(produces = MediaType.APPLICATION_JSON_VALUE)
    @ResponseStatus(HttpStatus.CREATED)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public Mono<Grade> createGrade(@RequestBody Grade grade) {
        // Validar que los campos requeridos no sean nulos o vacíos
        if (grade.getStudentId() == null || grade.getStudentId().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El studentId es requerido");
        }
        if (grade.getCourseId() == null || grade.getCourseId().trim().isEmpty()) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "El courseId es requerido");
        }
        if (grade.getGrade() == null || grade.getGrade() < 0 || grade.getGrade() > 20) {
            throw new ResponseStatusException(HttpStatus.BAD_REQUEST, "La calificación debe estar entre 0 y 20");
        }
        
        return gradeService.save(grade)
            .onErrorResume(e -> {
                return Mono.error(new ResponseStatusException(HttpStatus.BAD_REQUEST, "Ocurrió un error inesperado al guardar la calificación: " + e.getMessage()));
            });
    }

    /**
     * Updates an existing grade. Only accessible by ADMIN and TEACHER roles.
     * @param id Grade ID to update.
     * @param grade Updated grade data.
     * @return Mono with the updated grade.
     */
    @PutMapping(value = "/{id}", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN') or hasRole('TEACHER')")
    public Mono<Grade> updateGrade(@PathVariable String id, @RequestBody Grade grade) {
        return gradeService.update(id, grade);
    }

    /**
     * Logically deletes a grade by its ID. Only accessible by ADMIN role.
     * @param id Grade ID to delete.
     * @return Mono with the logically deleted grade.
     */
    @DeleteMapping("/{id}")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Grade> deleteGrade(@PathVariable String id) {
        return gradeService.deleteById(id);
    }

    /**
     * Restores a logically deleted grade by its ID. Only accessible by ADMIN role.
     * @param id Grade ID to restore.
     * @return Mono with the restored grade.
     */
    @PutMapping("/{id}/restore")
    @PreAuthorize("hasRole('ADMIN')")
    public Mono<Grade> restoreGrade(@PathVariable String id) {
        return gradeService.restoreById(id);
    }

    /**
     * Retrieves all inactive grades. Only accessible by ADMIN role.
     * @return Flux of inactive grades.
     */
    @GetMapping(value = "/inactive", produces = MediaType.APPLICATION_JSON_VALUE)
    @PreAuthorize("hasRole('ADMIN')")
    public Flux<Grade> getAllInactiveGrades() {
        return gradeService.findAllInactive();
    }
}