-- =====================================================
-- Sprint Mate - V1 Initial Schema
-- Target: PostgreSQL
-- =====================================================

-- Users
CREATE TABLE users (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    github_url VARCHAR(500),
    name VARCHAR(255),
    surname VARCHAR(255),
    role VARCHAR(50),
    bio VARCHAR(255),
    waiting_since TIMESTAMP,
    CONSTRAINT pk_users PRIMARY KEY (id)
);

-- User skills (ElementCollection)
CREATE TABLE user_skills (
    user_id UUID NOT NULL,
    skill VARCHAR(255),
    CONSTRAINT fk_user_skills_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_user_skills_user_id ON user_skills(user_id);

-- Matches
CREATE TABLE matches (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    status VARCHAR(50) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    expires_at TIMESTAMP,
    communication_link VARCHAR(500),
    CONSTRAINT pk_matches PRIMARY KEY (id)
);
CREATE INDEX idx_matches_status ON matches(status);

-- Match participants
CREATE TABLE match_participants (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL,
    user_id UUID NOT NULL,
    participant_role VARCHAR(50) NOT NULL,
    CONSTRAINT pk_match_participants PRIMARY KEY (id),
    CONSTRAINT fk_match_participants_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    CONSTRAINT fk_match_participants_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);
CREATE INDEX idx_match_participants_match_id ON match_participants(match_id);
CREATE INDEX idx_match_participants_user_id ON match_participants(user_id);

-- Project templates
CREATE TABLE project_templates (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    title VARCHAR(255) NOT NULL,
    description VARCHAR(2000),
    CONSTRAINT pk_project_templates PRIMARY KEY (id)
);

-- Project ideas
CREATE TABLE project_ideas (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    category VARCHAR(100) NOT NULL,
    name VARCHAR(255) NOT NULL,
    pitch VARCHAR(500) NOT NULL,
    core_concept VARCHAR(1000) NOT NULL,
    key_features VARCHAR(1000) NOT NULL,
    bonus_features VARCHAR(500),
    example_use_case VARCHAR(500),
    portfolio_value VARCHAR(500),
    difficulty INT NOT NULL,
    tags VARCHAR(200),
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_ideas PRIMARY KEY (id)
);
CREATE INDEX idx_project_ideas_active ON project_ideas(active);

-- Project prompt contexts
CREATE TABLE project_prompt_contexts (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    industry VARCHAR(100) NOT NULL,
    sub_domain VARCHAR(100),
    company_stage VARCHAR(50),
    team_size VARCHAR(50),
    crisis_category VARCHAR(100),
    crisis_scenario VARCHAR(2000),
    urgency_level VARCHAR(50),
    primary_constraint VARCHAR(100),
    secondary_constraint VARCHAR(100),
    architecture_pattern VARCHAR(100),
    backend_stack VARCHAR(200),
    frontend_stack VARCHAR(200),
    database_requirement VARCHAR(200),
    infrastructure VARCHAR(100),
    budget_constraint VARCHAR(50),
    timeline VARCHAR(50),
    stakeholder_pressure VARCHAR(500),
    success_metric VARCHAR(200),
    legacy_system_issue VARCHAR(500),
    compliance_requirement VARCHAR(200),
    integration_challenge VARCHAR(500),
    difficulty_score INT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_prompt_contexts PRIMARY KEY (id)
);

-- Match projects
CREATE TABLE match_projects (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL,
    project_template_id UUID NOT NULL,
    project_idea_id UUID,
    project_prompt_context_id UUID,
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
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL,
    completed_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    repo_url VARCHAR(500),
    CONSTRAINT pk_match_completions PRIMARY KEY (id),
    CONSTRAINT fk_match_completions_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE,
    CONSTRAINT uk_match_completions_match_id UNIQUE (match_id)
);

-- Sprint reviews
CREATE TABLE sprint_reviews (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL,
    repo_url VARCHAR(500) NOT NULL,
    score INT NOT NULL,
    ai_feedback VARCHAR(4000),
    strengths VARCHAR(2000),
    missing_elements VARCHAR(2000),
    readme_content TEXT,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_sprint_reviews PRIMARY KEY (id),
    CONSTRAINT fk_sprint_reviews_match FOREIGN KEY (match_id) REFERENCES matches(id) ON DELETE CASCADE
);
CREATE INDEX idx_sprint_reviews_match_id ON sprint_reviews(match_id);

-- Chat messages
CREATE TABLE chat_messages (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    match_id UUID NOT NULL,
    sender_id UUID NOT NULL,
    sender_name VARCHAR(255) NOT NULL,
    content VARCHAR(2000) NOT NULL,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_chat_messages PRIMARY KEY (id)
);
CREATE INDEX idx_chat_messages_match_id ON chat_messages(match_id);
CREATE INDEX idx_chat_messages_created_at ON chat_messages(created_at);
