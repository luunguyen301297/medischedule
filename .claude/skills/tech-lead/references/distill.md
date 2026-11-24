# Phase `distill` — KB Distill (tech-lead)

Chỉ chạy khi `docs/HANDOFF.<task>.local.md` có `knowledge_impact != none` và QA PASS (hoặc flow không có QA nhưng user
yêu cầu distill). **Không viết production code.** Output → `docs/knowledge/{domain}/` (L2/L3). Spec:
`docs/knowledge/rules/knowledge_building.md`.

1. **Input:** `phase=distill`, đọc L4 artifacts (`docs/tasks/`, `docs/design/`, code đã merge).
2. **Checklist:**
    - [ ] Rules mới/sửa → `capabilities/{cap}/business-rules/*.yaml` + `index.yaml`
    - [ ] Workflow variant → `features/{name}/workflow.md` (Mermaid)
    - [ ] Feature mới → `registry.yaml` + folder `features/{name}/`
    - [ ] ADR quan trọng → `adr/ADR-*.md`
    - [ ] Symbols → `traceability.yaml` (chỉ tên symbol — verify trong code). Symbol thật trong code KHÔNG có trong
      `main_symbols`/`traceability.yaml` → set `drift.symbols: true` (không chỉ khi rename)
    - [ ] `depends_on` capability đổi → `python scripts/gen_dependency_graph.py --domain {domain}`
    - [ ] Bump `knowledge_version`, set `confidence`, reset `drift`
    - [ ] `generated_from` → path BA spec L4
3. **Không ghi:** danh sách endpoint, file path, full DB schema (→ L5: code + `docs/_ref/`).
4. **Validate:** `python scripts/validate_knowledge.py --domain {domain}`.
5. **Handoff:** HANDOFF `knowledge_distill_status: done`, `phase=idle`, rồi
   `python .claude/scripts/handoff.py close <task> --summary "…"` (append history + xoá file). Không hỏi user.
