-- V1: Tabelas base do ArchitectAI
-- Compatível com PostgreSQL e H2 (MODE=PostgreSQL)

CREATE TABLE IF NOT EXISTS projects (
    id              VARCHAR(64)     NOT NULL PRIMARY KEY,
    repo_url        VARCHAR(2048)   NOT NULL,
    default_branch  VARCHAR(128)    NOT NULL,
    created_at      TIMESTAMP       NOT NULL
);

CREATE TABLE IF NOT EXISTS analyses (
    id                              VARCHAR(64)     NOT NULL PRIMARY KEY,
    project_id                      VARCHAR(64)     NOT NULL,
    type                            VARCHAR(64)     NOT NULL,
    status                          VARCHAR(32)     NOT NULL,
    created_at                      TIMESTAMP       NOT NULL,
    started_at                      TIMESTAMP,
    finished_at                     TIMESTAMP,
    agent_version                   VARCHAR(128),
    repository_path                 VARCHAR(2048),
    error_message                   VARCHAR(4096),
    findings_count                  INTEGER,
    report_url                      VARCHAR(2048),
    commercial_report_url           VARCHAR(2048),
    manifest_url                    VARCHAR(2048),
    estimated_cost_rs               BIGINT,
    static_analysis_report_url      VARCHAR(2048)
);

CREATE INDEX IF NOT EXISTS idx_analyses_project_id ON analyses (project_id);
CREATE INDEX IF NOT EXISTS idx_analyses_status     ON analyses (status);

CREATE TABLE IF NOT EXISTS analysis_static_findings_by_tool (
    analysis_id     VARCHAR(64)     NOT NULL,
    tool            VARCHAR(128)    NOT NULL,
    finding_count   INTEGER         NOT NULL,
    PRIMARY KEY (analysis_id, tool),
    FOREIGN KEY (analysis_id) REFERENCES analyses(id) ON DELETE CASCADE
);

CREATE TABLE IF NOT EXISTS findings (
    persistence_id  VARCHAR(64)     NOT NULL PRIMARY KEY,
    analysis_id     VARCHAR(64),
    tool            VARCHAR(128)    NOT NULL,
    id              VARCHAR(128)    NOT NULL,
    type            VARCHAR(128)    NOT NULL,
    severity        VARCHAR(32)     NOT NULL,
    file            VARCHAR(1024),
    line            INTEGER,
    message         VARCHAR(4096),
    evidence        VARCHAR(8192),
    tags            VARCHAR(4096),
    FOREIGN KEY (analysis_id) REFERENCES analyses(id) ON DELETE CASCADE
);

CREATE INDEX IF NOT EXISTS idx_findings_analysis_id ON findings (analysis_id);

