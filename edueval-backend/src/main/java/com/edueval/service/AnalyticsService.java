package com.edueval.service;

import com.edueval.dto.response.ClassroomAnalyticsResponse;
import com.edueval.dto.response.ExamAnalyticsResponse;
import com.edueval.dto.response.StudentProgressResponse;
import com.edueval.entity.*;
import com.edueval.enums.SubmissionStatus;
import com.edueval.exception.ResourceNotFoundException;
import com.edueval.exception.UnauthorizedActionException;
import com.edueval.repository.*;
import lombok.RequiredArgsConstructor;
import org.springframework.security.core.context.SecurityContextHolder;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.util.List;
import java.util.UUID;
import java.util.stream.Collectors;

@Service
@RequiredArgsConstructor
public class AnalyticsService {

    private final ClassroomRepository classroomRepository;
    private final ClassroomMemberRepository classroomMemberRepository;
    private final ExamRepository examRepository;
    private final SubmissionRepository submissionRepository;
    private final EvaluationRepository evaluationRepository;
    private final QuestionEvaluationRepository questionEvaluationRepository;
    private final UserRepository userRepository;

    // ── Teacher: per-exam analytics ──────────────────────────────────────────

    @Transactional(readOnly = true)
    public ExamAnalyticsResponse getExamAnalytics(UUID examId) {
        Exam exam = examRepository.findById(examId)
                .orElseThrow(() -> new ResourceNotFoundException("Exam not found: " + examId));
        requireTeacherOwnership(exam.getClassroom());
        return buildExamAnalytics(exam);
    }

    // ── Teacher: classroom-wide analytics ────────────────────────────────────

    @Transactional(readOnly = true)
    public ClassroomAnalyticsResponse getClassroomAnalytics(UUID classroomId) {
        Classroom classroom = classroomRepository.findById(classroomId)
                .orElseThrow(() -> new ResourceNotFoundException("Classroom not found: " + classroomId));
        requireTeacherOwnership(classroom);

        List<Exam> exams = examRepository.findByClassroom(classroom);
        List<ExamAnalyticsResponse> examBreakdown = exams.stream()
                .map(this::buildExamAnalytics)
                .collect(Collectors.toList());

        double overallAvg = examBreakdown.stream()
                .filter(e -> e.averageMarks() != null)
                .mapToDouble(ExamAnalyticsResponse::averageMarks)
                .average()
                .orElse(0.0);

        long totalStudents = classroomMemberRepository.countByClassroom(classroom);

        return new ClassroomAnalyticsResponse(
                classroom.getId(),
                classroom.getClassName(),
                totalStudents,
                exams.size(),
                overallAvg,
                examBreakdown
        );
    }

    // ── Student: personal progress ───────────────────────────────────────────

    @Transactional(readOnly = true)
    public List<StudentProgressResponse> getStudentProgress() {
        User student = currentUser();
        List<Submission> submissions = submissionRepository.findByStudent(student);

        return submissions.stream().map(sub -> {
            Double marks = computeFinalMarks(sub);
            Double percentage = (marks != null)
                    ? (marks / sub.getExam().getTotalMarks()) * 100.0
                    : null;
            boolean reviewed = sub.getStatus() == SubmissionStatus.REVIEWED;

            return new StudentProgressResponse(
                    sub.getId(),
                    sub.getExam().getId(),
                    sub.getExam().getTitle(),
                    sub.getExam().getClassroom().getClassName(),
                    sub.getExam().getTotalMarks(),
                    marks,
                    percentage,
                    reviewed,
                    sub.getSubmittedAt()
            );
        }).collect(Collectors.toList());
    }

    // ── Helpers ──────────────────────────────────────────────────────────────

    private ExamAnalyticsResponse buildExamAnalytics(Exam exam) {
        long totalStudents = classroomMemberRepository.countByClassroom(exam.getClassroom());
        List<Submission> submissions = submissionRepository.findByExam(exam);
        long submissionCount = submissions.size();
        double submissionRate = totalStudents > 0
                ? (submissionCount * 100.0 / totalStudents) : 0.0;

        List<Double> allFinalMarks = submissions.stream()
                .map(this::computeFinalMarks)
                .filter(m -> m != null)
                .collect(Collectors.toList());

        Double avg     = allFinalMarks.isEmpty() ? null : allFinalMarks.stream().mapToDouble(d -> d).average().orElse(0);
        Double highest = allFinalMarks.isEmpty() ? null : allFinalMarks.stream().mapToDouble(d -> d).max().orElse(0);
        Double lowest  = allFinalMarks.isEmpty() ? null : allFinalMarks.stream().mapToDouble(d -> d).min().orElse(0);

        // Read review state straight off Submission.status — works the same
        // way for both single-answer and multi-question exams, unlike the
        // old Evaluation-table lookups which were always empty for multi-
        // question submissions.
        long reviewed = submissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.REVIEWED)
                .count();
        long pendingReview = submissions.stream()
                .filter(s -> s.getStatus() == SubmissionStatus.AI_EVALUATED)
                .count();

        return new ExamAnalyticsResponse(
                exam.getId(),
                exam.getTitle(),
                exam.getTotalMarks(),
                totalStudents,
                submissionCount,
                submissionRate,
                avg,
                highest,
                lowest,
                pendingReview,
                reviewed
        );
    }

    // Same aggregation approach as SubmissionService.toResponse(): for multi-
    // question exams, sum each question's teacher marks (falling back to AI
    // marks) across that submission's question_evaluations rows. For single-
    // answer exams, read the legacy Evaluation row's finalMarks. Returns null
    // until at least one question/the evaluation has been AI-scored.
    private Double computeFinalMarks(Submission submission) {
        if (Boolean.TRUE.equals(submission.getExam().getIsMultiQuestion())) {
            List<QuestionEvaluation> evals =
                    questionEvaluationRepository.findByQuestionSubmissionSubmissionId(submission.getId());

            boolean anyEvaluated = evals.stream().anyMatch(qe -> qe.getAiMarks() != null);
            if (evals.isEmpty() || !anyEvaluated) {
                return null;
            }

            return evals.stream()
                    .mapToDouble(qe -> qe.getTeacherMarks() != null
                            ? qe.getTeacherMarks()
                            : (qe.getAiMarks() != null ? qe.getAiMarks() : 0.0))
                    .sum();
        }

        return evaluationRepository.findBySubmission(submission)
                .map(Evaluation::getFinalMarks)
                .orElse(null);
    }

    private void requireTeacherOwnership(Classroom classroom) {
        User user = currentUser();
        if (!classroom.getTeacher().getId().equals(user.getId())) {
            throw new UnauthorizedActionException("You do not own this classroom");
        }
    }

    private User currentUser() {
        String email = SecurityContextHolder.getContext().getAuthentication().getName();
        return userRepository.findByEmail(email)
                .orElseThrow(() -> new ResourceNotFoundException("Authenticated user not found"));
    }
}