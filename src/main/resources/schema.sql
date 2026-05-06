CREATE DATABASE IF NOT EXISTS codesentinel_db;
USE codesentinel_db;

CREATE TABLE IF NOT EXISTS users (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    username VARCHAR(60) NOT NULL UNIQUE,
    email VARCHAR(120) NOT NULL UNIQUE,
    password_hash VARCHAR(255) NOT NULL,
    role ENUM('ADMIN','DEVELOPER') NOT NULL,
    full_name VARCHAR(120) NOT NULL,
    created_at DATETIME NOT NULL,
    last_login DATETIME NULL,
    is_active BOOLEAN NOT NULL DEFAULT TRUE
);

CREATE TABLE IF NOT EXISTS code_submissions (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    user_id BIGINT NOT NULL,
    title VARCHAR(160) NOT NULL,
    language VARCHAR(40) NOT NULL,
    source_code TEXT NOT NULL,
    submission_type ENUM('FILE','PASTE') NOT NULL,
    file_name VARCHAR(255),
    submitted_at DATETIME NOT NULL,
    status VARCHAR(30) NOT NULL,
    CONSTRAINT fk_submission_user FOREIGN KEY (user_id) REFERENCES users(id)
);

CREATE TABLE IF NOT EXISTS analysis_reports (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    submission_id BIGINT NOT NULL UNIQUE,
    total_bugs INT NOT NULL,
    critical_count INT NOT NULL,
    major_count INT NOT NULL,
    minor_count INT NOT NULL,
    complexity_score INT NOT NULL,
    time_complexity VARCHAR(40) NOT NULL DEFAULT 'O(1)',
    space_complexity VARCHAR(40) NOT NULL DEFAULT 'O(1)',
    risk_level ENUM('LOW','MEDIUM','HIGH','CRITICAL') NOT NULL,
    analysis_duration_ms BIGINT NOT NULL,
    created_at DATETIME NOT NULL,
    CONSTRAINT fk_report_submission FOREIGN KEY (submission_id) REFERENCES code_submissions(id)
);

CREATE TABLE IF NOT EXISTS bug_issues (
    id BIGINT AUTO_INCREMENT PRIMARY KEY,
    report_id BIGINT NOT NULL,
    line_number INT NOT NULL,
    issue_type VARCHAR(80) NOT NULL,
    severity ENUM('CRITICAL','MAJOR','MINOR','INFO') NOT NULL,
    description VARCHAR(700) NOT NULL,
    suggestion VARCHAR(700) NOT NULL,
    code_snippet TEXT,
    CONSTRAINT fk_issue_report FOREIGN KEY (report_id) REFERENCES analysis_reports(id)
);
