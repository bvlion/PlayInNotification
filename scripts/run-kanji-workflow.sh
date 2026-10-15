#!/usr/bin/env bash
set -Eeuo pipefail

repository_root=$(git rev-parse --show-toplevel)
cd "$repository_root"
branch_name=$(git branch --show-current)
if [[ "$branch_name" != codex/* ]]; then
  printf '専用のcodexブランチのworktree内で実行してください。\n' >&2
  exit 1
fi
if (( $# > 1 )); then
  printf '使い方: ./scripts/run-kanji-workflow.sh [指示]\n' >&2
  exit 2
fi
codex_binary="${KANJI_CODEX_BINARY:-codex}"
codex_version=$("$codex_binary" --version)
python3 - "$codex_version" <<'PYTHON'
import re
import sys
match = re.fullmatch(r"codex-cli (\d+)\.(\d+)\.(\d+)", sys.argv[1])
if match is None or tuple(map(int, match.groups())) < (0, 162, 1):
    sys.exit("Codex CLI 0.162.1以降が必要です。KANJI_CODEX_BINARYで使用する実行ファイルを指定してください。")
PYTHON
exec "$codex_binary" --strict-config -C "$repository_root" \
  -m gpt-6.1-sol -c 'model_reasoning_effort="medium"' \
  -c 'agents.enabled=true' -c 'agents.max_concurrent_threads_per_session=4' \
  "難読漢字の管理役としてoperations/kanji/README.mdとworkflow.jsonを読み、GitHubの保存済み状態と照合してください。${1:-続きお願い}"
