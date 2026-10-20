# Teacher gradebook and analytics APIs

## Read-only assessment rules

Gradebook and analytics include only completed assessments:

- assignments with status `CLOSED`;
- quizzes with status `CLOSED`;
- exams with status `COMPLETED`.

An assessment with no score for an enrolled student is still represented in the
gradebook with `marksObtained: 0`, `status: MISSING`, and a zero percentage.
Overall percentages use the total maximum marks of all completed assessments,
so missing work contributes zero while preserving a consistent denominator.
Students with no completed assessments have an overall percentage of `0` and
are not included in assessment averages or grade distributions.

## Analytics calculations

`period` accepts `WEEK`, `MONTH`, or `TERM`. Unless explicit `from` and `to`
dates are supplied for backwards-compatible clients, these periods mean the
last 7, 30, and 90 calendar days respectively, inclusive of today.

Submission rate is submitted student-assessments divided by enrolled
student-assessments. Attendance counts `PRESENT` and `LATE` as attended;
`ABSENT` counts as not attended; `EXCUSED` and `NOT_MARKED` are excluded.

The API's GPA is a derived 4-point value from the percentage letter-grade
scale (`A=4`, `B=3`, `C=2`, `D=1`, `F=0`). It is not an institutional grade
point lookup. Grade boundaries are `A >= 90`, `B >= 80`, `C >= 70`,
`D >= 60`, and `F < 60`.

At-risk students have attendance below `75%` and/or overall assessment
percentage below `60%`. Correlation buckets are `0-59`, `60-69`, `70-79`,
`80-89`, and `90-100`.

All collection fields are returned as empty arrays when there is no data.
Invalid UUIDs or periods return `400`, missing courses return `404`, and a
teacher requesting an unassigned course returns `403`.
