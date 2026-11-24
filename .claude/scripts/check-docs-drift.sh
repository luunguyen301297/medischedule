#!/bin/bash
# check-docs-drift.sh
# Kiểm tra PROJECT/ docs có còn sync với codebase không
# Chạy thủ công: bash .claude/scripts/check-docs-drift.sh

# ---------------------------------------------------------------------------------------------------------------
# CONFIG — chỉnh theo stack dự án (đè bằng biến môi trường; mặc định trung tính: chưa cấu hình route thì bỏ qua)
# ---------------------------------------------------------------------------------------------------------------
# ROUTER_REGISTRY_FILE: file HOẶC THƯ MỤC chứa khai báo route (thư mục thì grep -r, bỏ node_modules/.git/target/build);
# ROUTER_PREFIX_REGEX : regex (grep -E) lấy route/prefix từ đó. Rỗng = bỏ mục [1]. Ví dụ:
#   FastAPI : ROUTER_REGISTRY_FILE=backend/app/api/v1/__init__.py  ROUTER_PREFIX_REGEX='prefix="[^"]+"'
#   Spring  : ROUTER_REGISTRY_FILE=backend/src/main/java
#             ROUTER_PREFIX_REGEX='@(Request|Get|Post|Put|Patch|Delete)Mapping\((value = )?"/[^"]*"'
#   Express : ROUTER_REGISTRY_FILE=src  ROUTER_PREFIX_REGEX="(app|router)\.(use|get|post|put|patch|delete)\('[^']+'"
ROUTER_REGISTRY_FILE="${ROUTER_REGISTRY_FILE:-}"
# mặc định: nhận chung Spring @…Mapping("/…"), FastAPI prefix="…", Express/Koa .get('/…') — nên đặt regex riêng
_default_route_regex='@(Request|Get|Post|Put|Patch|Delete)Mapping\([^)]*"/[^"]*"|prefix="[^"]+"|\.(use|get|post|put|patch|delete)\(["'"'"']/[^"'"'"']*["'"'"']'
ROUTER_PREFIX_REGEX="${ROUTER_PREFIX_REGEX:-$_default_route_regex}"
# thư mục chứa các module API (mỗi module một thư mục con; rỗng = bỏ mục [3]) và file capabilities tương ứng
#   ví dụ FastAPI: backend/app/api/v1/modules; Spring: backend/src/main/java/<group>/<app>
API_MODULES_DIR="${API_MODULES_DIR:-}"
CAPABILITIES_DOC="${CAPABILITIES_DOC:-docs/_ref/capabilities.md}"
# thư mục frontend + mẫu tên file store (cách nhau dấu cách) để so với frontend-state.md (thư mục không có thì bỏ)
FE_SRC_DIR="${FE_SRC_DIR:-frontend/src}"
FE_STORE_PATTERNS="${FE_STORE_PATTERNS:-*.store.ts *Store.ts}"
ARCH_DOC="${ARCH_DOC:-.claude/PROJECT/architecture.md}"
FE_STATE_DOC="${FE_STATE_DOC:-.claude/PROJECT/frontend-state.md}"
# ---------------------------------------------------------------------------------------------------------------

echo "=== PROJECT Docs Drift Check ==="
echo ""

# 1. Kiểm tra endpoints trong architecture.md vs routes thực tế
if [ -z "${ROUTER_REGISTRY_FILE}" ]; then
  echo "[1] API routes: bỏ qua (chưa đặt ROUTER_REGISTRY_FILE — xem khối CONFIG)"
elif [ -d "${ROUTER_REGISTRY_FILE}" ]; then
  echo "[1] API routes đang active (grep -r ${ROUTER_REGISTRY_FILE}):"
  grep -rhoE --exclude-dir=node_modules --exclude-dir=.git --exclude-dir=target --exclude-dir=build \
    --exclude-dir=dist --exclude-dir=__pycache__ "${ROUTER_PREFIX_REGEX}" "${ROUTER_REGISTRY_FILE}" 2>/dev/null | sort -u
else
  echo "[1] API routes đang active (${ROUTER_REGISTRY_FILE}):"
  grep -hoE "${ROUTER_PREFIX_REGEX}" "${ROUTER_REGISTRY_FILE}" 2>/dev/null | sort -u
fi

echo ""
echo "[2] Endpoints trong ${ARCH_DOC}:"
grep -E '^\s*- `(GET|POST|PUT|DELETE|PATCH)' "${ARCH_DOC}" 2>/dev/null | head -30

echo ""

# 2. Kiểm tra module nghiệp vụ trong capabilities doc vs thực tế
if [ -n "${API_MODULES_DIR}" ]; then
  echo "[3] Module API trong codebase (${API_MODULES_DIR}):"
  ls "${API_MODULES_DIR}" 2>/dev/null | grep -v __pycache__ | sort
else
  echo "[3] Module API: bỏ qua (chưa đặt API_MODULES_DIR)"
fi

echo ""
echo "[4] Module trong ${CAPABILITIES_DOC}:"
grep -E '^## [0-9]+\.' "${CAPABILITIES_DOC}" 2>/dev/null | head -30

echo ""

# 3. Kiểm tra stores frontend trong frontend-state.md vs thực tế
echo "[5] Stores thực tế (${FE_SRC_DIR}: ${FE_STORE_PATTERNS}):"
if [ -d "${FE_SRC_DIR}" ]; then
  find_args=()
  set -f  # không để shell tự bung glob mẫu tên file
  for pat in ${FE_STORE_PATTERNS}; do
    [ ${#find_args[@]} -gt 0 ] && find_args+=(-o)
    find_args+=(-name "${pat}")
  done
  set +f
  find "${FE_SRC_DIR}" \( "${find_args[@]}" \) 2>/dev/null | sort
fi

echo ""
echo "[6] Stores trong ${FE_STATE_DOC}:"
grep -E 'Store|store' "${FE_STATE_DOC}" 2>/dev/null | grep -v "^#" | head -20

echo ""
echo "=== Review output trên và cập nhật PROJECT/ files nếu có drift ==="
