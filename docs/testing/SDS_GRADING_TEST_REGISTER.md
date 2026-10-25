# SDS grading unit test register

This register records the focused unit cases that protect the SDS grading use cases and feed the root Maven JaCoCo gate. The local verification was `mvn --batch-mode --no-transfer-progress verify -pl services/assignment-service -am -Djacoco.skip=false`: 95 tests passed, with zero failures, errors, or skips. The named grading classes each reached 100% line and branch coverage. This is local evidence; attach the successful backend Actions run and JaCoCo XML after the branch CI run completes.

| Case ID | SDS requirement/scenario | Test class and method | Expected result | Actual/status |
|---|---|---|---|---|
| BQ-GRADE-001 | Quiz grading: correct answer | `QuizGradingServiceImplTest#correctAnswerReceivesFullMarks` | Correct option receives full marks | Pass |
| BQ-GRADE-002 | Quiz grading: incorrect answer | `QuizGradingServiceImplTest#wrongAnswerReceivesZeroMarks` | Wrong option receives zero marks | Pass |
| BQ-GRADE-003 | Quiz grading: unanswered item and percentage | `QuizGradingServiceImplTest#unansweredQuestionReceivesZeroMarksAndPercentageIsCalculatedCorrectly` | Missing answer scores zero and percentage is calculated | Pass |
| BQ-GRADE-004 | Quiz grading: option outside question | `QuizGradingServiceImplTest#answerSelectingAnOptionOutsideTheQuestionIsIncorrect` | Foreign option is not accepted as correct | Pass |
| BQ-GRADE-005 | Quiz grading: zero total marks | `QuizGradingServiceImplTest#zeroTotalMarksProducesZeroPercentage` | Avoid division by zero; percentage is zero | Pass |
| BQ-GRADE-006 | Quiz grading: null total/pass marks | `QuizGradingServiceImplTest#nullTotalAndPassMarksProduceZeroPercentageAndFail` | Null totals are handled safely and result fails | Pass |
| BQ-GRADE-007 | Quiz grading: question outside quiz | `QuizGradingServiceImplTest#answersForQuestionsOutsideTheQuizAreIgnored` | Unknown question answers do not change the score | Pass |
| BA-GRADE-001 | Teacher grades own submission; notification optional | `GradingServiceImplTest#teacherCanGradeOwnSubmissionAndOptionalNotificationCanBeAbsent` | Grade and persist authorized submission | Pass |
| BA-GRADE-002 | Administrator grades with feedback notification | `GradingServiceImplTest#administratorCanGradeAndPublishesFeedbackNotification` | Admin grade is persisted and feedback notification is sent | Pass |
| BA-GRADE-003 | Missing or negative marks | `GradingServiceImplTest#gradingRejectsMissingOrNegativeMarksBeforeSaving` | Invalid marks are rejected without persistence | Pass |
| BA-GRADE-004 | Non-teacher role | `GradingServiceImplTest#nonTeacherCannotGrade` | Unauthorized role is rejected | Pass |
| BA-GRADE-005 | Teacher ownership | `GradingServiceImplTest#teacherCannotGradeAnotherTeachersAssignmentOrAnAssignmentWithoutOwner` | Cross-teacher and ownerless grading are rejected | Pass |
| BA-GRADE-006 | Missing submission | `GradingServiceImplTest#missingSubmissionIsRejected` | Missing submission returns an error | Pass |
| BA-GRADE-007 | Return graded submission feedback | `GradingServiceImplTest#returnSubmissionHandlesBlankAndPresentFeedback` | Blank and provided feedback are handled | Pass |
| BA-GRADE-008 | Course lookup fallback | `GradingServiceImplTest#courseLookupUsesNameAndOptionalCodeAndFallsBackOnNullOrFailure` | Course fields and null/error fallback are handled | Pass |
| BA-GRADE-009 | Notification without student identity | `GradingServiceImplTest#optionalNotificationIsSkippedWhenStudentIdIsMissing` | No invalid notification call is made | Pass |
| BA-GRADE-010 | Course response fallback and valid details | `GradingServiceImplTest#courseLookupFallsBackForMissingResponsesAndUsesNonblankDetailsWhenAvailable` | Missing details use fallback; valid details are preserved | Pass |

## Coverage rule and scope

`pom.xml` enforces at least 90% JaCoCo branch coverage independently for:

- `com.mentify.quiz.service.impl.QuizGradingServiceImpl`
- `com.mentify.assignment.service.impl.GradingServiceImpl`

The local report measured Quiz at 35/35 covered lines and 14/14 covered branches, and Assignment at 68/68 covered lines and 38/38 covered branches. The SDS mapping and rationale are documented in `README.md` under “SDS Critical Business Logic Coverage Scope”. The 90% rule is class-scoped to these two SDS grading use cases; it is not a claim that every critical class in the complete platform has been identified or covered.
