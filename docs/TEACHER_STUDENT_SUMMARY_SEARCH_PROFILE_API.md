# Teacher student summary, search, and profile APIs

## Student academic summary

`GET /api/v1/teacher/students/{studentId}/summary?courseId={courseId}` is
available to the assigned teacher, admins, and super admins. The student must
be enrolled in the requested course; otherwise the service returns `404`.
Attendance counts `PRESENT` and `LATE` as attended and `ABSENT` as absent.
Assignment, quiz, and exam averages include completed assessments with a
score; missing completed assessments count as zero in the weighted overall
percentage and increment `submissionsPending`. GPA uses the percentage scale
`A=4`, `B=3`, `C=2`, `D=1`, `F=0`.

## Unified teacher search

`GET /api/v1/teacher/search?q={query}&types=course,student,assignment,quiz,exam&limit=50`
returns a bounded `{results,total}` payload. Queries must contain at least two
characters and `limit` is restricted to 1–100. Results are scoped to courses
assigned to the authenticated teacher; admins can search all courses. Empty
matches return an empty `results` array. The bounded limit prevents unbounded
payloads and the minimum query length avoids accidental expensive searches.

## Teacher profile and digest preferences

`PATCH /api/v1/users/teachers/me` supports `title`, `department`,
`officeLocation`, `officeHours`, `bio`, and `avatarUrl` in addition to the
existing phone and specialization fields. `PATCH /api/v1/users/me` supports
`dailyDigestEnabled` and `dailyDigestTime` (`HH:mm`). These values are stored
in the user-service database and returned by the existing profile response.
