CREATE TABLE organizations (
    id          UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    name        VARCHAR(255) NOT NULL,
    slug        VARCHAR(100) NOT NULL UNIQUE,
    created_at  TIMESTAMPTZ  NOT NULL DEFAULT now(),
    updated_at  TIMESTAMPTZ  NOT NULL DEFAULT now()
);

CREATE TABLE organization_members (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    user_id         UUID NOT NULL REFERENCES users (id) ON DELETE CASCADE,
    role            VARCHAR(50) NOT NULL DEFAULT 'MEMBER',
    joined_at       TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_org_member UNIQUE (organization_id, user_id)
);

CREATE INDEX idx_org_members_user ON organization_members (user_id);

CREATE TABLE projects (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    organization_id UUID NOT NULL REFERENCES organizations (id) ON DELETE CASCADE,
    name            VARCHAR(255) NOT NULL,
    key             VARCHAR(100) NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_project_key_per_org UNIQUE (organization_id, key)
);

CREATE TABLE feature_flags (
    id                   UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    project_id           UUID NOT NULL REFERENCES projects (id) ON DELETE CASCADE,
    key                  VARCHAR(150) NOT NULL,
    name                 VARCHAR(255) NOT NULL,
    description          TEXT,
    enabled              BOOLEAN NOT NULL DEFAULT FALSE,
    rollout_percentage   SMALLINT NOT NULL DEFAULT 0,
    created_by           UUID REFERENCES users (id),
    created_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    updated_at           TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_flag_key_per_project UNIQUE (project_id, key),
    CONSTRAINT chk_rollout_percentage CHECK (rollout_percentage BETWEEN 0 AND 100)
);

CREATE INDEX idx_flags_project ON feature_flags (project_id);

CREATE TABLE flag_overrides (
    id              UUID PRIMARY KEY DEFAULT gen_random_uuid(),
    flag_id         UUID NOT NULL REFERENCES feature_flags (id) ON DELETE CASCADE,
    targeting_key   VARCHAR(255) NOT NULL,
    enabled         BOOLEAN NOT NULL,
    created_at      TIMESTAMPTZ NOT NULL DEFAULT now(),
    CONSTRAINT uq_override_per_target UNIQUE (flag_id, targeting_key)
);

CREATE INDEX idx_overrides_flag ON flag_overrides (flag_id);
