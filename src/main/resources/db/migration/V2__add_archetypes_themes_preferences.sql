-- =====================================================
-- Sprint Mate - V2 Add Archetypes, Themes, User Preferences
-- Reason: V1 was missing tables for the archetype/theme-based
--         project generation feature added after initial schema.
-- =====================================================

-- Project archetypes (structure patterns: CRUD_APP, REAL_TIME_APP, etc.)
CREATE TABLE project_archetypes (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    structure_description VARCHAR(500) NOT NULL,
    component_patterns VARCHAR(500),
    api_patterns VARCHAR(300),
    min_complexity INT NOT NULL,
    max_complexity INT NOT NULL,
    active BOOLEAN NOT NULL DEFAULT true,
    created_at TIMESTAMP NOT NULL DEFAULT CURRENT_TIMESTAMP,
    CONSTRAINT pk_project_archetypes PRIMARY KEY (id),
    CONSTRAINT uq_project_archetypes_code UNIQUE (code)
);

-- Project themes (domain contexts: finance, health, gaming, etc.)
CREATE TABLE project_themes (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    code VARCHAR(50) NOT NULL,
    display_name VARCHAR(100) NOT NULL,
    domain_context VARCHAR(500),
    example_entities VARCHAR(300),
    active BOOLEAN NOT NULL DEFAULT true,
    CONSTRAINT pk_project_themes PRIMARY KEY (id),
    CONSTRAINT uq_project_themes_code UNIQUE (code)
);

-- User preferences (difficulty, learning goals, preferred themes)
CREATE TABLE user_preferences (
    id UUID NOT NULL DEFAULT gen_random_uuid(),
    user_id UUID NOT NULL,
    difficulty_preference INT,
    learning_goals VARCHAR(500),
    CONSTRAINT pk_user_preferences PRIMARY KEY (id),
    CONSTRAINT uq_user_preferences_user_id UNIQUE (user_id),
    CONSTRAINT fk_user_preferences_user FOREIGN KEY (user_id) REFERENCES users(id) ON DELETE CASCADE
);

-- Join table: user_preferences <-> project_themes (many-to-many)
CREATE TABLE user_preferred_themes (
    user_preference_id UUID NOT NULL,
    theme_id UUID NOT NULL,
    CONSTRAINT pk_user_preferred_themes PRIMARY KEY (user_preference_id, theme_id),
    CONSTRAINT fk_upt_preference FOREIGN KEY (user_preference_id) REFERENCES user_preferences(id) ON DELETE CASCADE,
    CONSTRAINT fk_upt_theme FOREIGN KEY (theme_id) REFERENCES project_themes(id) ON DELETE CASCADE
);

-- Add archetype_id and theme_id to match_projects
ALTER TABLE match_projects
    ADD COLUMN archetype_id UUID,
    ADD COLUMN theme_id UUID;

ALTER TABLE match_projects
    ADD CONSTRAINT fk_match_projects_archetype FOREIGN KEY (archetype_id) REFERENCES project_archetypes(id),
    ADD CONSTRAINT fk_match_projects_theme FOREIGN KEY (theme_id) REFERENCES project_themes(id);
