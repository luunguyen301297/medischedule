# Knowledge Lifecycle (KDD) — phân tầng, distill, trường HANDOFF

Tách từ `WORKFLOW.md` §5.1, 5.3–5.5. Đọc khi `distill`, hoặc khi BA/TL khai `knowledge_impact`. Quy trình nạp KB đầu task (§5.2) vẫn ở `WORKFLOW.md`. Spec đầy đủ: [`docs/knowledge/rules/knowledge_building.md`](../../docs/knowledge/rules/knowledge_building.md).

## K1 Phân tầng tài liệu

| Layer         | Path                                           | Ai ghi                            |
|---------------|------------------------------------------------|-----------------------------------|
| L2 Capability | `docs/knowledge/medischedule/capabilities/`        | `tech-lead` @ `distill` |
| L3 Feature    | `docs/knowledge/medischedule/features/`            | `tech-lead` @ `distill` |
| L4 Ephemeral  | `docs/tasks/`, `docs/design/`, `docs/reviews/` | BA, SA, TL, QA trong pipeline     |
| L5 Structural | `docs/_ref/`, code (CLAUDE.md §Tìm code)       | Auto-gen — **không sửa tay**      |

**Dependency graph (cross-capability impact):** `docs/knowledge/medischedule/dependency-graph.yaml` — auto-generated
bởi `scripts/gen_dependency_graph.py` từ `depends_on` (hand-curated trong capability `meta.yaml`), tính ngược thành
`depended_by`. Không sửa tay. Regenerate khi `depends_on` đổi; `validate_knowledge.py` tự check staleness.

## Prefix mã business rule theo capability

Mỗi capability một prefix riêng (2–3 chữ hoa); mã rule = `<PREFIX>` + 3 chữ số, tăng dần trong capability
(`docs/knowledge/rules/knowledge_building.md` § Business Rules). Thêm capability mới thì thêm dòng ở đây **trước** khi
tạo rule đầu tiên; không dùng lại prefix đã có.

**Không trùng mã khác của quy trình:** prefix không được trùng tiền tố mã đã dùng trong spec/test/tài liệu — `AC`
(acceptance criteria `AC-01`), `TC` (test case `TC-01`), `US` (user story `US-01`), `ADR` (`ADR-001`), `BR` (business
rule chung), cùng tên vai / phase viết tắt (`BA`, `SA`, `TL`, `CR`, `QA`) — nếu không, tìm theo mã sẽ lẫn rule với
AC/test case (vd capability "account" đặt `AC` thì `AC001` lẫn với `AC-01`). Kiểm thêm mã riêng dự án (mã lỗi API,
mã ticket, mã use case trong tài liệu/code như `UC-xx`, `G1`) trước khi chốt.

**Cách lập bảng:** agent **đề xuất** capability và prefix dựa trên code (module/package, tên miền nghiệp vụ trong code),
ghi `chờ xác nhận` ở cột Ghi chú, rồi **hỏi user xác nhận** tên capability và prefix. Chưa xác nhận thì chưa tạo rule
nào dùng prefix đó; xác nhận xong đổi Ghi chú thành `đã xác nhận`.

| Capability (module) | `<PREFIX>` | Ghi chú |
|---|---|---|
| `auth` | `AU` | chờ xác nhận |
| `booking` | `BK` | chờ xác nhận |
| `schedule` | `SC` | chờ xác nhận |
| `clinic` | `CL` | chờ xác nhận |
| `patient` | `PT` | chờ xác nhận |
| `emr` | `EM` | chờ xác nhận |
| `pharmacy` | `PH` | chờ xác nhận |
| `billing` | `BL` | chờ xác nhận |
| `commission` | `CM` | chờ xác nhận |
| `aftercare` | `AF` | chờ xác nhận |
| `notification` | `NT` | chờ xác nhận |
| `cms` | `CN` | chờ xác nhận |
| `storage` | `ST` | chờ xác nhận |
| `ai` | `AI` | chờ xác nhận |

Đề xuất theo package module backend (`vn.medischedule.<module>`). Đã tránh trùng: `AC`/`TC`/`US`/`ADR`/`BR`/tên vai, và
mã use case trong `package-info.java` (`G1`, `P0-A`…`P6`, `R1`…`R4`, `D1`…`D7`, `DS1`…`DS3`, `A1a`…`A6` — nên không
dùng `DS`, và prefix một chữ). `auth` không dùng `AC` (trùng acceptance criteria).

## K3 Task End — Distill KB (write, conditional)

**Không distill sau mọi task.** Chỉ khi `knowledge_impact != none` và QA PASS (hoặc CR LGTM cho flow không có QA).

| Flow                         | `knowledge_impact` mặc định | Distill?                                                                   |
|------------------------------|-----------------------------|----------------------------------------------------------------------------|
| Full Pipeline                | `full`                      | Có — rules, workflow, ADR, traceability                                    |
| C — Modify                   | `partial`                   | Có — delta rules/workflow                                                  |
| D — Refactor (rename symbol) | `partial`                   | Có — chỉ `traceability.yaml` + `drift.symbols`                             |
| A — Hotfix                   | `none`                      | Không (trừ user ghi `partial`)                                             |
| B — Mini                     | `none`                      | Không                                                                      |
| E — Performance              | `none`                      | Không                                                                      |
| F — Upgrade                  | `none`                      | Không                                                                      |
| G — Bug Investigation        | `none`                      | Không (trừ khi fix hé lộ business rule/workflow chưa document → `partial`) |

**Owner:** `tech-lead` @ `phase=distill`. Dev/QA **không** ghi KB khi implement.

## K4 Distill checklist (tech-lead)

* [ ] Rules mới/sửa → `capabilities/{cap}/business-rules/<PREFIX>*.yaml` (prefix của capability — bảng "Prefix mã business rule" ở trên) + `index.yaml`
* [ ] Workflow variant → `features/{name}/workflow.md`
* [ ] Feature mới → `registry.yaml` + folder `features/{name}/`
* [ ] ADR quan trọng → `adr/ADR-*.md`
* [ ] Symbols → `traceability.yaml` (verify symbol trong code, không dùng file path)
* [ ] **Symbol rename:** Set `drift.symbols: true` trên meta bị ảnh hưởng khi refactor merge; distill traceability trước
  release tiếp (xem `docs/knowledge/medischedule/MAINTENANCE.md`)
* [ ] Bump `knowledge_version`, set `confidence`, reset `drift`
* [ ] `generated_from` → path BA spec L4

## K5 HANDOFF fields (KB)

```yaml
domain: medischedule              # nullable — task không thuộc domain có KB thì bỏ trống
capability: order               # nullable — ví dụ
feature: preorder               # nullable — ví dụ
knowledge_impact: full          # none | partial | full
knowledge_distill_status: pending  # pending | done | skipped
needs_db_detail: false          # set bởi Tech Lead khi APPROVED, xem §3.3 — chỉ có ý nghĩa ở phase=contract
needs_api_detail: false         # set bởi Tech Lead khi APPROVED, xem §3.3 — chỉ có ý nghĩa ở phase=contract
```

`knowledge_impact` do **BA** khai báo (Full/Modify) hoặc **Tech Lead** (Refactor). User có thể override.

`needs_db_detail`/`needs_api_detail` do **Tech Lead** khai báo khi APPROVED design (Full Pipeline, Modify, Bug
Investigation — xem §3.3). Mặc định `false` (không cần bước `contract`, đi thẳng `dev`).
