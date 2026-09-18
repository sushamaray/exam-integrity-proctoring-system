package com.uiu.examintegrity.course;

import java.util.List;

import org.springframework.data.jpa.repository.JpaRepository;

public interface EnrollmentRepository extends JpaRepository<Enrollment, Long> {
    List<Enrollment> findByCourseIdAndRoleInCourse(Long courseId, CourseRole roleInCourse);

    List<Enrollment> findByUserIdAndRoleInCourse(Long userId, CourseRole roleInCourse);

    boolean existsByCourseIdAndUserIdAndRoleInCourse(Long courseId, Long userId, CourseRole roleInCourse);
}
