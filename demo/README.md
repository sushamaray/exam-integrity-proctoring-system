# Demo exam

`demo-exam.csv` is a ready-made exam of 10 MCQs and 1 written question, for demonstrating exam sessions, proctoring and collusion detection end to end.

## Accounts

The login page has four quick-fill buttons (bottom right). The password for all of them is `password123`.

| Button | Account | Role |
|---|---|---|
| **F** | faculty@uiu.ac.bd | Examiner |
| **1** | Tanvir Rahman, 0112210001@bscse.uiu.ac.bd | Student |
| **2** | Nusrat Hossain, 0112210002@bscse.uiu.ac.bd | Student |
| **3** | Sadia Chowdhury, 0112210003@bscse.uiu.ac.bd | Student |

Log in as each student in its own browser window. Use private/incognito windows or separate browser profiles, because a normal window shares one login.

## Create the exam

1. Log in with **F**, go to **Exams**, then **Schedule exam**.
2. Pick the course, give it a title, and drop `demo-exam.csv` on the upload box.
   - Use **60 minutes** for a normal demo.
   - Use **3 minutes** to show auto-submit live.
3. The exam appears as **Live**.

Each student can sit each exam once. For a fresh run, upload the CSV again as a new exam.

## Demo: exam sessions

1. Log in as student **1**, open the dashboard, go to the briefing, and start the exam.
2. Answer a question and watch the header show **Saved**.
3. Press **F5**: the same paper comes back, with the same answers, and the timer keeps running.
4. Submit, then open **Post-exam review** to see the score, the answers and the explanations. The written answer shows as *Awaiting grading*.

## Demo: proctoring

Keep the faculty window on **Live monitor** alongside the student windows.

1. **Leaving the tab.** A student switches to another tab and comes back. Their tile goes to **Review** straight away. A second switch takes it to **Escalate**.
2. **Copy-paste.**
   - Copy a paragraph of 120 characters or more (a Wikipedia paragraph works).
   - Paste it into the **written question** (the one with a text box).
   - The tile shows **Large paste**.
3. **Idle, then burst.** Answer nothing for 90 seconds, then answer 3 questions within 20 seconds.
4. Click a tile to open **Session detail**: the score breakdown, the correlated cluster, and the raw browser events. Then **Uphold** (a written note is required) or **Dismiss**.

## Demo: collusion

1. Students **1** and **2** paste the **same paragraph** into the written question, then both submit.
2. Wait until the exam has no sittings in progress: everyone has submitted, or their time has run out.
3. As faculty, open **Collusion graph**, pick the exam, and click **Run analysis**. The pair appears.
   - Click it to see both answers side by side, with the shared text underlined.
   - Choosing the same wrong MCQ options also links a pair.

## Answer key

Options are shuffled per student, so match the **text**, not the letter on screen.

| # | Correct answer |
|---|---|
| Written | Graded by a person (3 marks) |
| 1 | Decorator |
| 2 | Observer |
| 3 | Reading elements as Number but not adding |
| 4 | Type erasure keeps bytecode compatible with older code |
| 5 | LinkedHashMap |
| 6 | Lookups with an equal key may fail |
| 7 | It must be declared or caught |
| 8 | The value from finally |
| 9 | Lost updates because ++ is not atomic |
| 10 | Single Responsibility |

## CSV format

```
topic,question,option_a,option_b,option_c,option_d,correct,marks
```

- `correct` is a, b, c or d.
- `marks` may be blank, which means 1.
- **Leave all four options and `correct` blank for a written question.**
