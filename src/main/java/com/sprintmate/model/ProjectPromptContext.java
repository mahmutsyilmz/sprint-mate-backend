package com.sprintmate.model;

import jakarta.persistence.*;
import lombok.*;
import org.hibernate.annotations.CreationTimestamp;

import java.time.LocalDateTime;
import java.util.UUID;

/**
 * Represents a crisis scenario context for AI project generation.
 *
 * Business Intent:
 * Stores pre-defined crisis scenarios from various industries that the AI
 * uses to generate realistic, urgent project briefs. The PROBLEM comes from
 * this entity, but the SOLUTION STACK comes from the matched users' skills.
 *
 * Key Rule: backend_stack and frontend_stack are for flavor/legacy context only.
 * The actual tech stack used in the generated project MUST come from user skills.
 */
@Entity
@Table(name = "project_prompt_contexts")
@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor(access = AccessLevel.PRIVATE)
@Builder
public class ProjectPromptContext {

    @Id
    @GeneratedValue(strategy = GenerationType.UUID)
    private UUID id;

    /**
     * Industry vertical (e.g., "Healthcare", "FinTech", "E-Commerce").
     */
    @Column(nullable = false)
    private String industry;

    /**
     * Specific sub-domain within the industry (e.g., "Patient Records", "Payment Processing").
     */
    @Column(name = "sub_domain")
    private String subDomain;

    /**
     * Company stage (e.g., "Startup", "Growth", "Enterprise").
     */
    @Column(name = "company_stage")
    private String companyStage;

    /**
     * Team size context (e.g., "Small (2-5)", "Medium (6-15)").
     */
    @Column(name = "team_size")
    private String teamSize;

    /**
     * Category of crisis (e.g., "System Failure", "Security Breach", "Scalability").
     */
    @Column(name = "crisis_category")
    private String crisisCategory;

    /**
     * Detailed crisis scenario description.
     * This is the core "problem statement" that drives the project.
     */
    @Column(name = "crisis_scenario", length = 2000)
    private String crisisScenario;

    /**
     * Urgency level (e.g., "Critical", "High", "Medium").
     */
    @Column(name = "urgency_level")
    private String urgencyLevel;

    /**
     * Primary constraint (e.g., "Time", "Budget", "Resources").
     */
    @Column(name = "primary_constraint")
    private String primaryConstraint;

    /**
     * Secondary constraint affecting the solution.
     */
    @Column(name = "secondary_constraint")
    private String secondaryConstraint;

    /**
     * Suggested architecture pattern (e.g., "Microservices", "Monolith", "Event-Driven").
     * Used as guidance, not as a strict requirement.
     */
    @Column(name = "architecture_pattern")
    private String architecturePattern;

    /**
     * Legacy backend stack context (for flavor/context ONLY).
     * DO NOT use this to force user's tech stack - use user skills instead.
     */
    @Column(name = "backend_stack")
    private String backendStack;

    /**
     * Legacy frontend stack context (for flavor/context ONLY).
     * DO NOT use this to force user's tech stack - use user skills instead.
     */
    @Column(name = "frontend_stack")
    private String frontendStack;

    /**
     * Database requirement (e.g., "ACID Compliant", "High Availability", "NoSQL").
     */
    @Column(name = "database_requirement")
    private String databaseRequirement;

    /**
     * Infrastructure context (e.g., "Cloud-Native", "On-Premise", "Hybrid").
     */
    @Column(name = "infrastructure")
    private String infrastructure;

    /**
     * Budget constraint level (e.g., "Limited", "Moderate", "Flexible").
     */
    @Column(name = "budget_constraint")
    private String budgetConstraint;

    /**
     * Timeline constraint (e.g., "1 week", "2 weeks", "1 month").
     */
    @Column(name = "timeline")
    private String timeline;

    /**
     * Level of stakeholder pressure (e.g., "CEO breathing down your neck", "Board presentation Friday").
     */
    @Column(name = "stakeholder_pressure", length = 500)
    private String stakeholderPressure;

    /**
     * Success metric for the project (e.g., "99.9% uptime", "50% faster response time").
     */
    @Column(name = "success_metric")
    private String successMetric;

    /**
     * Legacy system issues if applicable (e.g., "COBOL mainframe", "PHP 5.2 monolith").
     */
    @Column(name = "legacy_system_issue", length = 500)
    private String legacySystemIssue;

    /**
     * Compliance requirements (e.g., "HIPAA", "GDPR", "PCI-DSS", "SOC2").
     */
    @Column(name = "compliance_requirement")
    private String complianceRequirement;

    /**
     * Integration challenges (e.g., "Must integrate with SAP", "Legacy API limitations").
     */
    @Column(name = "integration_challenge", length = 500)
    private String integrationChallenge;

    /**
     * Difficulty score (1-10) for the scenario.
     */
    @Column(name = "difficulty_score")
    private Integer difficultyScore;

    /**
     * Timestamp when this context was created.
     */
    @CreationTimestamp
    @Column(name = "created_at", nullable = false, updatable = false)
    private LocalDateTime createdAt;
}
