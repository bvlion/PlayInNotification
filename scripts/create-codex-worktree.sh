#!/usr/bin/env bash

set -Eeuo pipefail

usage() {
  cat <<'EOF'
使い方: ./scripts/create-codex-worktree.sh <Issue番号> <ブランチ名の接尾辞>
EOF
}

die() {
  printf 'エラー: %s\n' "$*" >&2
  exit 1
}

if [[ "${1:-}" == "-h" || "${1:-}" == "--help" ]]; then
  usage
  exit 0
fi

if (( $# != 2 )); then
  usage >&2
  exit 2
fi

issue_number="$1"
branch_suffix="$2"

[[ "$issue_number" =~ ^[1-9][0-9]*$ ]] || die "Issue番号には1以上の整数を指定してください。"
[[ "$branch_suffix" =~ ^[a-z0-9]+([._-][a-z0-9]+)*$ ]] || die "接尾辞には小文字英数字と区切り文字（.、_、-）を指定してください。"

branch_name="agent/issue-${issue_number}-${branch_suffix}"
git check-ref-format --branch "$branch_name" >/dev/null || die "ブランチ名がGitの形式に適合しません: ${branch_name}"

source_worktree=$(git rev-parse --show-toplevel 2>/dev/null) || die "Git worktree内で実行してください。"
primary_worktree=$(
  git -C "$source_worktree" worktree list --porcelain |
    awk '$1 == "worktree" { sub(/^worktree /, ""); print; exit }'
)
[[ -n "$primary_worktree" ]] || die "メインworktreeの場所を取得できませんでした。"

worktree_root="$(dirname "$primary_worktree")/PlayInNotification-worktrees"
target_worktree="${worktree_root}/issue-${issue_number}"

git -C "$source_worktree" fetch origin
git -C "$source_worktree" rev-parse --verify --quiet "refs/remotes/origin/main^{commit}" >/dev/null ||
  die "origin/mainを取得できませんでした。"

if git -C "$source_worktree" show-ref --verify --quiet "refs/heads/${branch_name}"; then
  die "同名のローカルブランチが存在します: ${branch_name}"
fi

if git -C "$source_worktree" ls-remote --exit-code --heads origin "refs/heads/${branch_name}" >/dev/null; then
  die "同名のリモートブランチが存在します: origin/${branch_name}"
else
  ls_remote_status=$?
  if (( ls_remote_status != 2 )); then
    die "origin上の同名ブランチを確認できませんでした: ${branch_name}"
  fi
fi

if [[ -e "$target_worktree" || -L "$target_worktree" ]]; then
  die "作成先が既に存在します: ${target_worktree}"
fi

if git -C "$source_worktree" worktree list --porcelain |
  awk -v target="$target_worktree" '
    $1 == "worktree" {
      sub(/^worktree /, "")
      if ($0 == target) found = 1
    }
    END { exit !found }
  '; then
  die "作成先は既にworktreeとして登録されています: ${target_worktree}"
fi

is_worktree_root_created=false
is_target_worktree_created=false
is_worktree_creation_started=false
is_issue_branch_created=false
is_completed=false

cleanup() {
  exit_status=$?
  if [[ "$is_completed" == true || "$is_issue_branch_created" != true ]]; then
    return
  fi

  printf '作成処理に失敗したため、今回作成したworktreeとブランチを削除します。\n' >&2
  if [[ "$is_worktree_creation_started" == true ]] &&
    git -C "$source_worktree" worktree list --porcelain |
    awk -v target="$target_worktree" '
      $1 == "worktree" {
        sub(/^worktree /, "")
        if ($0 == target) found = 1
      }
      END { exit !found }
    '; then
    if ! git -C "$source_worktree" worktree remove --force "$target_worktree"; then
      printf 'worktreeを削除できませんでした。確認後に次を実行してください:\n  git -C %q worktree remove --force %q\n' \
        "$source_worktree" "$target_worktree" >&2
    fi
  fi
  if [[ "$is_target_worktree_created" == true && ( -e "$target_worktree" || -L "$target_worktree" ) ]]; then
    if ! rm -rf -- "$target_worktree"; then
      printf '作成先を削除できませんでした。確認後に次を実行してください:\n  rm -rf -- %q\n' \
        "$target_worktree" >&2
    fi
  fi
  if git -C "$source_worktree" show-ref --verify --quiet "refs/heads/${branch_name}"; then
    if ! git -C "$source_worktree" branch -D "$branch_name"; then
      printf 'ブランチを削除できませんでした。確認後に次を実行してください:\n  git -C %q branch -D %q\n' \
        "$source_worktree" "$branch_name" >&2
    fi
  fi
  if [[ "$is_worktree_root_created" == true ]]; then
    rmdir "$worktree_root" 2>/dev/null || true
  fi

  return "$exit_status"
}
trap cleanup EXIT

if [[ ! -d "$worktree_root" ]]; then
  mkdir -p "$worktree_root"
  is_worktree_root_created=true
fi

git -C "$source_worktree" branch --no-track "$branch_name" origin/main
is_issue_branch_created=true

mkdir "$target_worktree"
is_target_worktree_created=true
is_worktree_creation_started=true
git -C "$source_worktree" worktree add "$target_worktree" "$branch_name"

if git -C "$target_worktree" rev-parse --abbrev-ref --symbolic-full-name '@{upstream}' >/dev/null 2>&1; then
  die "作成したIssueブランチにupstreamが設定されています: ${branch_name}"
fi

if [[ -f "${target_worktree}/.gitmodules" ]]; then
  git -C "$target_worktree" submodule update --init --recursive
fi

worktree_status=$(git -C "$target_worktree" status --porcelain --untracked-files=all)
[[ -z "$worktree_status" ]] || die "作成したworktreeに未追跡または変更済みのファイルがあります。"

is_completed=true

printf '\nCodex用worktreeを作成しました。\n'
printf '  Issue: #%s\n' "$issue_number"
printf '  ブランチ: %s\n' "$branch_name"
printf '  worktree: %s\n' "$target_worktree"
printf '\n次の手順:\n'
printf '  cd %q\n' "$target_worktree"
printf '  codex\n'
printf '  Permissionを手動でFull Accessに変更してください。\n'
printf '  Codexへの依頼: Issue #%sを対応してください\n' "$issue_number"
