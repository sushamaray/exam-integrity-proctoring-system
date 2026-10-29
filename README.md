# Exam Integrity & Automated Proctoring System

An online examination platform designed to improve academic integrity through automated proctoring, suspicious activity detection, answer similarity analysis, and automated evaluation.

## Project Overview

The Exam Integrity & Automated Proctoring System is an academic project developed using Object-Oriented Programming principles. It provides separate portals for students and faculty, supports different examination formats, and helps faculty monitor suspicious activities during online exams.

**Project:** Exam Integrity & Automated Proctoring System  
**Team:** Team Forerunner  
**Course:** CSE 2118 — Advanced Object-Oriented Programming  
**Institution:** United International University (UIU)  
**Section:** B  
**Trimester:** Summer 2026

## Key Features

### Authentication & Role Management
- Student and faculty roles with role-based access control.
- Session-based authentication and role-specific portal access.

### Examination Management
- Multiple-choice, written, and coding examinations.
- Randomized question selection and topic-balanced question distribution.
- Automatic grading for multiple-choice questions.

### Automated Proctoring
- Tab-switch detection.
- Fullscreen-exit detection.
- Window-focus loss detection.
- Paste-burst detection.
- Heartbeat-gap monitoring and risk scoring.
- Real-time event monitoring through WebSockets.

### Answer Similarity & Collusion Detection
- TF-IDF cosine similarity for written answers.
- Wrong-answer agreement analysis for MCQ responses.
- Union-Find-based clustering to identify potentially related submissions.

### Faculty Dashboard & Item Analysis
- Exam creation and scheduling.
- Live proctoring and suspicious-event review.
- Answer comparison and similarity visualization.
- Question difficulty and discrimination analysis.

### Student Portal
- Course enrollment and exam access.
- Exam instructions and integrity guidelines.
- Coding exam interface.
- Post-exam score review.
- Practice and self-assessment.

## Technology Stack

- **Backend:** Java, Spring Boot
- **Frontend:** React
- **Communication:** REST APIs, WebSockets
- **Core Concepts:** Object-Oriented Programming, Strategy Pattern, Encapsulation, Abstraction, Polymorphism, Domain-Driven Design
- **Algorithms & Analysis:** TF-IDF Cosine Similarity, Wrong Answer Agreement, Union-Find

## Future Improvements

- AI-based audio and video proctoring using computer vision and audio processing.
- Secure sandboxed code execution using Docker and a code execution service such as Judge0.
- Machine-learning-based risk scoring to reduce false positives.
- Mobile and cross-platform support.

## Team Members

- Sushama Ray
- Ashique Imam Hossain
- Shakib Hasan

## Academic Purpose

This project was developed as part of the CSE 2118 coursework at United International University. It demonstrates the application of object-oriented design and software engineering concepts to online examination security.

---

