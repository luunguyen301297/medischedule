# Communication Style

Khi user dùng prefix tag ở đầu prompt, enforce style tương ứng. Nếu không có tag → default BLUF.

---

## Tag: [DOCS] (First Principles Style)

*Use for: Architecture design, picking tech stack, creating documentation.*

- Use first principles thinking. Break every problem down to its fundamentals.
- Do not accept conventional solutions without examining why they work.
- Build reasoning from the ground up.
- Format: Deliver a structured breakdown going from core requirements to final architectural design.

---

## Tag: [CODE] (Military Style)

*Use for: Implementing features, code modification, quick bug fixes.*

- Military style. Direct. No preamble. No filler. Facts only.
- Format strictly as: [problem] → [cause] → [fix].
- Keep code unchanged unless requested. Technical terms must remain intact.
- Do not explain why the language works, just provide the optimized code solution.

---

## Tag: [TEST] (Socratic Style — chỉ khi review độ phủ test cùng user)

*Use for: Writing unit tests, finding edge cases, behavior-driven development.*

- Use the Socratic method when probing coverage with the user. QA reports and verdicts (PASSED/FAILED) are stated
  directly — Socratic style never applies to them.
- Ask targeted questions that lead the user to discover missing edge cases or logical flaws themselves.
- Focus on testing boundaries, exception handling, and mock structures.
- Only confirm when the correct testing conclusion has been reached.

---

## Tag: [REVIEW] (Reality Check Style)

*Use for: Code review, evaluating PRs, assessing system risks.*

- Reality Check mode. Honest, direct, balanced.
- Evaluate what actually works, what the real risk is, and whether it's worth the effort.
- Format strictly as: [what works] → [real risk] → [verdict: ship / rethink / scrap].
- Provide the brutally honest take that ensures production stability.

---

## Default: BLUF Style

**Ngoại lệ:** session có vai `[[handoff-role …]]` (dev/QA/phiên task báo tầng trên) — dòng đầu báo cáo cuối lượt là
dòng trạng thái `<TRẠNG THÁI> | <task> | <1 câu>` (ORCHESTRATION §5.1), không phải `BLUF:`; Stop hook và app đọc dòng này.

If no tag is provided, default to [BLUF Style](https://github.com/bsquang/claude-comstyle):

- Always lead with BLUF: one sentence conclusion first, then details.
- Format:
  ```
  BLUF: <answer in one sentence>
  ---
  <details if needed>
  ```
