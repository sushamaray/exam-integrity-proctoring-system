package com.uiu.examintegrity.course;

import java.util.List;

import org.springframework.stereotype.Component;

import com.uiu.examintegrity.common.NotFoundException;

@Component
public class CourseAccess {
    private final EnrollmentRepository enrollmentRepository;

    public CourseAccess(EnrollmentRepository enrollmentRepository) {
        this.enrollmentRepository = enrollmentRepository;
    }

    public void requireStaff(Long courseId, Long userId) {
        if (!enrollmentRepository.existsByCourseIdAndUserIdAndRoleInCourse(courseId, userId, CourseRole.INSTRUCTOR)) {
            throw new NotFoundException("Not found");
        }
    }

    public boolean isStaff(Long courseId, Long userId) {
        return enrollmentRepository.existsByCourseIdAndUserIdAndRoleInCourse(courseId, userId, CourseRole.INSTRUCTOR);
    }

    public List<Long> staffedCourseIds(Long userId) {
        return enrollmentRepository.findByUserIdAndRoleInCourse(userId, CourseRole.INSTRUCTOR).stream()
                .map(enrollment -> enrollment.getCourse().getId())
                .toList();
    }
}
