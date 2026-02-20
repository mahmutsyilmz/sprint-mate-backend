-- =====================================================
-- Sprint Mate - V1 Initial Schema
-- Target: Microsoft SQL Server
-- =====================================================

-- Users
CREATE TABLE users (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    github_url NVARCHAR(500),
    name NVARCHAR(255),
    surname NVARCHAR(255),
    role NVARCHAR(50),
    bio NVARCHAR(255),
    waiting_since DATETIME2(6),
    CONSTRAINT pk_users PRIMARY KEY (id)
);

-- User skills (ElementCollection)
CREATE TABLE user_skills (
    user_id UNIQUEIDENTIFIER NOT NULL,
    skill NVARCHAR(255),
    CONSTRAINT fk_user_skills_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_user_skills_user_id ON user_skills(user_id);

-- Matches
CREATE TABLE matches (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    status NVARCHAR(50) NOT NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT GETDATE(),
    expires_at DATETIME2(6),
    communication_link NVARCHAR(500),
    CONSTRAINT pk_matches PRIMARY KEY (id)
);
CREATE INDEX idx_matches_status ON matches(status);

-- Match participants
CREATE TABLE match_participants (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    match_id UNIQUEIDENTIFIER NOT NULL,
    user_id UNIQUEIDENTIFIER NOT NULL,
    participant_role NVARCHAR(50) NOT NULL,
    CONSTRAINT pk_match_participants PRIMARY KEY (id),
    CONSTRAINT fk_match_participants_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_participants_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_match_participants_match_id ON match_participants(match_id);
CREATE INDEX idx_match_participants_user_id ON match_participants(user_id);

-- Project templates
CREATE TABLE project_templates (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    title NVARCHAR(255) NOT NULL,
    description NVARCHAR(2000),
    CONSTRAINT pk_project_templates PRIMARY KEY (id)
);

-- Project ideas
CREATE TABLE project_ideas (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    category NVARCHAR(100) NOT NULL,
    name NVARCHAR(255) NOT NULL,
    pitch NVARCHAR(500) NOT NULL,
    core_concept NVARCHAR(1000) NOT NULL,
    key_features NVARCHAR(1000) NOT NULL,
    bonus_features NVARCHAR(500),
    example_use_case NVARCHAR(500),
    portfolio_value NVARCHAR(500),
    difficulty INT NOT NULL,
    tags NVARCHAR(200),
    active BIT NOT NULL DEFAULT 1,
    created_at DATETIME2(6) NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_project_ideas PRIMARY KEY (id)
);
CREATE INDEX idx_project_ideas_active ON project_ideas(active);

-- Project prompt contexts
CREATE TABLE project_prompt_contexts (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    industry NVARCHAR(100) NOT NULL,
    sub_domain NVARCHAR(100),
    company_stage NVARCHAR(50),
    team_size NVARCHAR(50),
    crisis_category NVARCHAR(100),
    crisis_scenario NVARCHAR(2000),
    urgency_level NVARCHAR(50),
    primary_constraint NVARCHAR(100),
    secondary_constraint NVARCHAR(100),
    architecture_pattern NVARCHAR(100),
    backend_stack NVARCHAR(200),
    frontend_stack NVARCHAR(200),
    database_requirement NVARCHAR(200),
    infrastructure NVARCHAR(100),
    budget_constraint NVARCHAR(50),
    timeline NVARCHAR(50),
    stakeholder_pressure NVARCHAR(500),
    success_metric NVARCHAR(200),
    legacy_system_issue NVARCHAR(500),
    compliance_requirement NVARCHAR(200),
    integration_challenge NVARCHAR(500),
    difficulty_score INT,
    created_at DATETIME2(6) NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_project_prompt_contexts PRIMARY KEY (id)
);

-- Match projects
CREATE TABLE match_projects (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    match_id UNIQUEIDENTIFIER NOT NULL,
    project_template_id UNIQUEIDENTIFIER NOT NULL,
    project_idea_id UNIQUEIDENTIFIER,
    project_prompt_context_id UNIQUEIDENTIFIER,
    start_date DATE NOT NULL,
    end_date DATE NOT NULL,
    CONSTRAINT pk_match_projects PRIMARY KEY (id),
    CONSTRAINT fk_match_projects_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_projects_template FOREIGN KEY (project_template_id) REFERENCES project_templates(id),
    CONSTRAINT fk_match_projects_idea FOREIGN KEY (project_idea_id) REFERENCES project_ideas(id),
    CONSTRAINT fk_match_projects_prompt FOREIGN KEY (project_prompt_context_id) REFERENCES project_prompt_contexts(id)
);
CREATE INDEX idx_match_projects_match_id ON match_projects(match_id);

-- Match completions
CREATE TABLE match_completions (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    match_id UNIQUEIDENTIFIER NOT NULL,
    completed_at DATETIME2(6) NOT NULL DEFAULT GETDATE(),
    repo_url NVARCHAR(500),
    CONSTRAINT pk_match_completions PRIMARY KEY (id),
    CONSTRAINT fk_match_completions_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    CONSTRAINT uk_match_completions_match_id UNIQUE (match_id)
);

-- Sprint reviews
CREATE TABLE sprint_reviews (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    match_id UNIQUEIDENTIFIER NOT NULL,
    repo_url NVARCHAR(500) NOT NULL,
    score INT NOT NULL,
    ai_feedback NVARCHAR(4000),
    strengths NVARCHAR(2000),
    missing_elements NVARCHAR(2000),
    readme_content NVARCHAR(MAX),
    created_at DATETIME2(6) NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_sprint_reviews PRIMARY KEY (id),
    CONSTRAINT fk_sprint_reviews_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);
CREATE INDEX idx_sprint_reviews_match_id ON sprint_reviews(match_id);

-- Chat messages
CREATE TABLE chat_messages (
    id UNIQUEIDENTIFIER NOT NULL DEFAULT NEWID(),
    match_id UNIQUEIDENTIFIER NOT NULL,
    sender_id UNIQUEIDENTIFIER NOT NULL,
    sender_name NVARCHAR(255) NOT NULL,
    content NVARCHAR(2000) NOT NULL,
    created_at DATETIME2(6) NOT NULL DEFAULT GETDATE(),
    CONSTRAINT pk_chat_messages PRIMARY KEY (id)
);
CREATE INDEX idx_chat_messages_match_id ON chat_messages(match_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at);
