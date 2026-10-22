CREATE TABLE IF NOT EXISTS grade_subject_domains (
    grade_id UUID NOT NULL REFERENCES grades(id) ON DELETE CASCADE,
    subject_domain VARCHAR(100) NOT NULL,
    CONSTRAINT pk_grade_subject_domains PRIMARY KEY (grade_id, subject_domain)
);

-- Give existing grades useful defaults. Administrators can later update the
-- subjectDomains list through the Grade API without changing course records.
INSERT INTO grade_subject_domains (grade_id, subject_domain)
SELECT g.id, subject_domain
FROM grades g
CROSS JOIN LATERAL unnest(
    CASE
        WHEN lower(g.name) LIKE '%12%' THEN ARRAY[
            'Mathematics', 'Physics', 'Chemistry', 'Biology', 'Tamil', 'English', 'Information Technology'
        ]
        WHEN lower(g.name) LIKE '%10%' OR lower(g.name) LIKE '%11%' THEN ARRAY[
            'Mathematics', 'Science', 'Tamil', 'English', 'History', 'Information Technology'
        ]
        WHEN lower(g.name) LIKE '%6%' OR lower(g.name) LIKE '%7%' OR lower(g.name) LIKE '%8%' OR lower(g.name) LIKE '%9%' THEN ARRAY[
            'Mathematics', 'Science', 'Tamil', 'English', 'Social Studies', 'Information Technology'
        ]
        ELSE ARRAY['Mathematics', 'Science', 'Tamil', 'English', 'Social Studies']
    END
) AS subject_domain
ON CONFLICT (grade_id, subject_domain) DO NOTHING;
