# BA Methodology — Business Analysis Framework

## 1. Mục đích

Tài liệu này cung cấp phương pháp phân tích nghiệp vụ tổng quát cho Business Analyst — đây là **lớp phương pháp
phân tích**, không phải workflow dự án. Quy tắc của dự án, domain, knowledge base và workflow riêng luôn ưu tiên
cao hơn tài liệu này (xem §12).

## 2. Nguyên tắc phân tích (First Principles)

Không mặc định một convention hoặc hành vi hiện tại là yêu cầu nghiệp vụ. Với mỗi yêu cầu, luôn xác định: vấn đề
nghiệp vụ thực sự là gì, ai bị ảnh hưởng, hệ thống cần đạt trạng thái nào và tại sao trạng thái đó có giá trị, business
rule/ngoại lệ nào chi phối, thay đổi này ảnh hưởng quy trình/capability nào khác.

Mọi quyết định nghiệp vụ phải có cơ sở từ: yêu cầu stakeholder, tài liệu nghiệp vụ hiện có, domain knowledge, hành
vi hiện tại đã xác nhận, hoặc quyết định rõ ràng của stakeholder. Không biến assumption thành requirement.

## 3. Khai thác & đánh giá đầy đủ yêu cầu

Phương pháp khai thác: phỏng vấn stakeholder, phân tích tài liệu/quy trình hiện tại, quan sát nghiệp vụ, workshop,
phân tích use case/User Story/Acceptance Criteria — chọn phương pháp phù hợp bối cảnh, không cần dùng hết.

Với mỗi yêu cầu, phải xác định đủ các trường sau trước khi coi là "rõ ràng" (dùng làm checklist đầu vào, không chỉ
thu thập rồi bỏ qua):

* **Actor** — ai thực hiện, ai bị ảnh hưởng, có actor khác liên quan không.
* **Trigger** — điều gì kích hoạt nghiệp vụ.
* **Preconditions** — điều kiện đầu vào phải tồn tại trước khi thực hiện.
* **Main Flow / Alternative Flow** — luồng chính và luồng thay thế (nếu có).
* **Business Rules** — quy tắc chi phối.
* **Exception** — điều gì xảy ra khi nghiệp vụ không hoàn thành được.
* **Outcome / Impact** — kết quả cuối cùng và thành phần nghiệp vụ khác bị ảnh hưởng.

Phân biệt người sử dụng hệ thống với người quyết định requirement (stakeholder) — không mặc định hai vai trò này là
một.

## 4. Hiện trạng, khoảng cách, nguyên nhân gốc & cải tiến

Khi có đủ thông tin, xác định hiện trạng (quy trình đang chạy thế nào, ai thực hiện, hệ thống hỗ trợ bước nào, bước
nào thủ công, constraint/pain point nào) rồi phân biệt triệu chứng, nguyên nhân, constraint và yêu cầu thực sự —
không mặc định giải pháp do stakeholder đề xuất chính là nguyên nhân hoặc requirement.

Chỉ ra khoảng cách giữa hiện trạng và trạng thái mong muốn: cái gì đang sai/chưa đáp ứng, cái gì cần đổi, kết quả
mong muốn, điều gì **không** được đổi.

Khi yêu cầu xuất phát từ một vấn đề, ưu tiên tìm nguyên nhân gốc thay vì chỉ xử lý triệu chứng (5 Whys, Root Cause
Analysis, Fishbone, phân tích luồng nghiệp vụ). Câu hỏi trọng tâm: *nếu chỉ sửa triệu chứng, vấn đề có thể xuất hiện
lại ở đâu?*

Khi phân tích cải tiến một quy trình hiện tại, cân nhắc thêm: bottleneck, thao tác dư thừa/lặp lại, manual work,
điểm kiểm soát, cơ hội tự động hóa/đơn giản hóa — nhưng **có thể tự động hóa ≠ nên tự động hóa**, luôn cân nhắc giá
trị và rủi ro nghiệp vụ. Nếu quy trình đủ phức tạp cần sơ đồ hóa (nhiều nhánh, nhiều actor, swimlane) → dùng
Mermaid flowchart trong spec thay vì mô tả bằng văn xuôi.

## 5. Business Rules

Mỗi requirement quan trọng phải có business rule được xác định rõ: điều kiện, giới hạn, quyền, trạng thái, thứ tự
xử lý, điều kiện chuyển trạng thái, ngoại lệ, ràng buộc nghiệp vụ. Business Rule phải được phân biệt với UI behavior,
implementation detail, database constraint và API design — những thứ đó không phải business rule.

## 6. User Story & Acceptance Criteria

User Story thể hiện actor, mục tiêu, giá trị nghiệp vụ — không mô tả implementation. Format mặc định: *"Với vai trò
là [vai trò], tôi muốn [mục tiêu/hành động], để [giá trị nghiệp vụ]."* Không dùng cấu trúc `As a... I want... So
that...` trong BA Spec tiếng Việt.

Acceptance Criteria phải rõ ràng, không mơ hồ, kiểm thử được, phản ánh đúng requirement và bao phủ business rule
quan trọng. Format mặc định: *"Với điều kiện [điều kiện], khi [hành động], thì [kết quả]."* Tối thiểu xem xét: luồng
chính, trường hợp biên, trường hợp không hợp lệ, trường hợp ngoại lệ (nếu có). Không tạo AC chỉ để tăng số lượng.

## 7. Phân tích trường hợp biên

Với mỗi requirement, cân nhắc: dữ liệu thiếu/không hợp lệ, trạng thái không mong đợi, hành động lặp lại hoặc sai thứ
tự, nhiều actor cùng tác động, thay đổi giữa chừng, nghiệp vụ đã hoàn tất nhưng tiếp tục thao tác, trường hợp không
có dữ liệu / có nhiều dữ liệu. Chỉ đưa vào BA Spec những trường hợp có ý nghĩa nghiệp vụ — không thêm cho đủ số
lượng.

## 8. Phân tích tác động & rủi ro

**Tác động:** mỗi thay đổi phải được xem xét trên quy trình, actor, business rule, capability, feature, trạng thái,
báo cáo, vận hành và các quy trình phụ thuộc. Với hệ thống có dependency graph, phải kiểm tra các capability phụ
thuộc capability đang thay đổi.

**Rủi ro:** xác định rủi ro nghiệp vụ, điều kiện gây ra, tác động, khả năng xảy ra và biện pháp giảm thiểu (nếu
cần). Không biến phân tích rủi ro thành thiết kế kỹ thuật.

## 9. Requirements Traceability

Duy trì khả năng truy ngược: Business Objective → Requirement → User Story → Acceptance Criteria → Test Scope.
Trước khi hoàn thành BA Spec, kiểm tra: mọi requirement quan trọng có User Story, mọi User Story có Acceptance
Criteria, mọi AC truy ngược được về requirement (không có AC vô căn cứ), Business Rule quan trọng đã được phản ánh,
trường hợp biên quan trọng đã được xem xét.

## 10. Giá trị nghiệp vụ, Success Metrics & dữ liệu

Khi phù hợp, xác định giá trị nghiệp vụ (lợi ích, chi phí, thời gian, giảm lỗi/thao tác, tăng hiệu suất/kiểm soát,
cải thiện trải nghiệm) — ROI/cost-benefit chỉ phân tích khi có đủ dữ liệu hoặc stakeholder yêu cầu, không tự bịa số
liệu. Khi requirement cần đo lường, xác định chỉ số, cách đo, thời điểm đo, trạng thái hiện tại, mục tiêu, owner nếu
cần — không ép mọi feature phải có KPI. Có thể dùng dữ liệu sẵn có để xác nhận nghiệp vụ (độ chính xác, đầy đủ, xu
hướng, insight) nhưng không tự chuyển sang thiết kế DB/SQL nếu task chỉ yêu cầu phân tích nghiệp vụ.

## 11. Solution Boundary

BA được phép xác định: yêu cầu chức năng, business rule, quy trình, actor, business state, business validation,
integration requirement và testing scope ở mức nghiệp vụ.

BA **không** tự quyết định: kiến trúc hệ thống, cấu trúc database, schema, API contract, class, framework,
implementation detail — các nội dung này thuộc phạm vi SA/Architecture/Development.

## 12. Nguyên tắc ưu tiên

Khi có xung đột, thứ tự ưu tiên: (1) quy tắc nghiệp vụ đã được stakeholder xác nhận, (2) quy tắc và workflow của dự
án, (3) Domain Knowledge Base, (4) Shared Domain Context, (5) yêu cầu hiện tại, (6) phương pháp BA trong tài liệu
này, (7) convention chung của ngành, (8) assumption của BA. Không dùng convention hoặc assumption để tự quyết định
một business requirement chưa được xác nhận.

## 13. Checklist cuối cùng

Trước khi đưa requirement sang thiết kế, xác nhận:

* [ ] Business objective, actor, trigger đã rõ.
* [ ] Current state đã hiểu; problem đã phân biệt với symptom; root cause đã xem xét.
* [ ] Desired state đã rõ; không có requirement mâu thuẫn; không có conflict với business rule hiện tại.
* [ ] Business Rules đã xác định; main flow đã rõ.
* [ ] Edge case và exception case quan trọng đã được xem xét.
* [ ] Impact (bao gồm capability khác) đã được đánh giá.
* [ ] Requirement có khả năng truy vết (User Stories đầy đủ, Acceptance Criteria kiểm thử được).
* [ ] Không còn assumption chưa được xác nhận.
* [ ] Business value / success metrics đã xác định khi cần.
* [ ] Solution boundary không bị vượt sang technical design.

## 14. Thứ tự đặt câu hỏi làm rõ (Frontier)

> Nguồn cảm hứng: kỹ thuật *design tree / frontier* của skill `grilling`
> ([mattpocock/skills](https://github.com/mattpocock/skills), MIT) — không vendor skill này vào repo, chỉ mượn khái
> niệm và điều chỉnh lại để dùng tool `AskUserQuestion` thay vì format hỏi bằng text thuần.

Khi GATE 1 phát hiện nhiều điểm mơ hồ cùng lúc, không hỏi tùy tiện theo thứ tự phát hiện ra. Áp dụng:

1. **Lọc fact vs decision** trước khi liệt kê câu hỏi:
    * **Fact** — tự tra được bằng Read/Grep/code index/KB (hành vi hệ thống hiện tại, convention có sẵn, dữ liệu đã
      tồn tại) → tự đi tìm, KHÔNG đưa vào danh sách hỏi stakeholder.
    * **Decision** — chỉ stakeholder quyết định được (business rule mới, đánh đổi nghiệp vụ, phạm vi tính năng) →
      đưa vào danh sách hỏi.
2. Với các câu hỏi Decision còn lại, xác định phụ thuộc: câu B phụ thuộc câu A nếu câu trả lời của A làm thay đổi
   nội dung hoặc tính cần thiết của câu B.
3. **Frontier của 1 lượt hỏi** = tập câu hỏi Decision không phụ thuộc câu Decision nào khác còn treo. Gộp toàn bộ
   frontier vào 1 lần gọi `AskUserQuestion`. Nếu frontier có nhiều câu hơn giới hạn của tool (tối đa 4 câu/lần) →
   ưu tiên theo mức Cao/Trung bình/Thấp, phần còn lại chuyển sang lượt kế dù không phụ thuộc.
4. Câu hỏi phụ thuộc vào một câu Decision chưa có câu trả lời → để lại lượt sau, không hỏi gộp cùng lượt với câu nó
   phụ thuộc.
5. Sau khi stakeholder trả lời một lượt, tính lại frontier (một số câu trước đây bị chặn nay đã đủ điều kiện) rồi
   lặp lại bước 3.
6. Dừng khi frontier rỗng — không còn điểm mơ hồ nào chưa hỏi hoặc chưa tự tra được.

Nguyên tắc phân loại ưu tiên Cao/Trung bình/Thấp hiện có vẫn giữ nguyên vai trò: dùng để sắp xếp thứ tự **trong một
frontier** khi vượt quá giới hạn câu hỏi/lần, không thay thế cách nhóm theo frontier.
