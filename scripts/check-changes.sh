#!/bin/sh
# Kiểm thay đổi, không phụ thuộc AI hay Python. Git hook và CI dùng chung.
#   sh scripts/check-changes.sh --staged          # phần đã git add, so với nhánh gốc
#   sh scripts/check-changes.sh --base <commit>   # HEAD so với <commit> (CI)
# Exit 0 = đạt, 1 = vi phạm, 2 = gọi sai.
set -u
cd "$(git rev-parse --show-toplevel)" || exit 2

MIG=backend/src/main/resources/db/migration
fail=0
err() { printf '[check-changes] %s\n' "$1" >&2; fail=1; }

case "${1:-}" in
  --staged)
    cached=--cached
    target=
    if git rev-parse -q --verify origin/main >/dev/null; then
      base=$(git merge-base HEAD origin/main) || exit 2
    else
      base=HEAD
    fi
    secret_base=HEAD
    files=$(git ls-files "$MIG")
    ;;
  --base)
    [ -n "${2:-}" ] || { echo "Thiếu commit gốc" >&2; exit 2; }
    cached=
    target=HEAD
    base=$(git merge-base "$2" HEAD) || exit 2
    secret_base=$base
    files=$(git ls-tree -r --name-only HEAD -- "$MIG")
    ;;
  *) echo "Dùng: $0 --staged | --base <commit>" >&2; exit 2 ;;
esac

# 1. Migration V* đã có trên nhánh gốc: cấm sửa / xoá / đổi tên
touched=$(git diff $cached --name-only --no-renames --diff-filter=MDT "$base" $target -- "$MIG/V*.sql") || exit 2
[ -z "$touched" ] || err "Không sửa/xoá/đổi tên migration đã lên nhánh gốc (tạo version mới):
$touched"

# 2. Version migration không được trùng (Flyway coi V4, V04, V4.0 là một)
dup=$(printf '%s\n' "$files" | sed -n 's#.*/V\([0-9][0-9._]*\)__.*\.sql$#\1#p' |
  sed -E -e 's/_/./g' -e 's/(^|\.)0+([0-9])/\1\2/g' -e 's/(\.0+)+$//' | sort | uniq -d)
[ -z "$dup" ] || err "Trùng version migration: $dup"

# 3. Tên file migration phải đúng mẫu
bad=$(printf '%s\n' "$files" | grep '\.sql$' | grep -Ev '/(V[0-9][0-9._]*|R)__[A-Za-z0-9_]+\.sql$')
[ -z "$bad" ] || err "Tên migration sai mẫu V{n}__mo_ta.sql:
$bad"

# 4. File .env thật
envs=$(git diff $cached --name-only --diff-filter=AMR "$secret_base" $target | grep -E '(^|/)\.env(\.[^/]+)?$' | grep -vE '\.env\.example$')
[ -z "$envs" ] || err "Không commit file .env: $envs"

# 5. Khoá / token dạng rõ ràng trong dòng thêm mới (không in giá trị)
hits=$(git diff $cached -U0 --no-color "$secret_base" $target | grep -E '^\+' |
  grep -cE -e '-----BEGIN [A-Z ]*PRIVATE KEY-----|gh[pousr]_[A-Za-z0-9]{36}|github_pat_[A-Za-z0-9_]{50,}|glpat-[A-Za-z0-9_-]{20,}|AKIA[0-9A-Z]{16}|sk-ant-[A-Za-z0-9_-]{20,}')
[ "${hits:-0}" -eq 0 ] || err "Có $hits dòng giống khoá/token bí mật"

[ "$fail" -eq 0 ] || exit 1
