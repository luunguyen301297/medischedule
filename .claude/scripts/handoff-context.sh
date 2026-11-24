#!/bin/bash
# handoff-context.sh — UserPromptSubmit hook: in danh sách task đang chạy (docs/HANDOFF.<task>.local.md).
# Logic nằm ở handoff.py; xem .claude/ORCHESTRATION.md §4.
python -I "${CLAUDE_PROJECT_DIR:-.}/.claude/scripts/handoff.py" status 2>/dev/null \
  || echo "[HANDOFF] không chạy được handoff.py — xem docs/HANDOFF.*.local.md thủ công"
