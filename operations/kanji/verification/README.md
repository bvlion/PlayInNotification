# 運用検証記録

実際の漢字問題は作成・再選定・監査していない。根拠欄の空・非空だけを判定する模擬記録で、担当起動と状態遷移を確認する。

## 確認方法

Codex CLI 0.162.1の `codex exec --strict-config --json` を使用し、管理役は `gpt-6.1-sol` / `medium`、作業担当は `.codex/agents/` の指定を使用した。各監査の起動は `agent_type` を指定し、`fork_turns="none"` とした。親・子スレッドの実行履歴から `session_meta` の役割・親IDと `turn_context` のmodel・effortを確認した。生の履歴や認証情報は保存していない。

1. 作成担当に問題を含まない版1を返させる。
2. 3監査を独立起動し、同じ版1とSHA-256だけを渡して、それぞれの根拠欄を判定させる。
3. 管理役が原結果と指摘の対応を保存して停止する。外側の検証者がcommit・pushし、Issue #140へ保存位置を記録する。
4. 前管理セッションの履歴を継承せずに新しい `codex exec` を開始し、「続きお願い」と検証範囲の制約を渡す。コミットIDや指摘内容は渡さない。新管理役が `gh api` でIssueコメントと保存コミットの状態・スナップショット・監査結果を取得し、SHA-256を照合して再開する。
5. 初回の形式監査が空欄を見落としたため、原結果を保持し、空なら指摘1件・非空なら0件という模擬判定条件を明確にして独立再確認する。
6. 状態を再保存し、別の新管理セッションから問題作成担当による模擬修正と全監査担当の再確認へ進む。ユーザー承認待ちで停止する。

GitHub読み取りを行う非対話検証では `workspace-write` と `sandbox_workspace_write.network_access=true` を使用した。実行途中の保存・pushは外側の検証者が行った。管理役自身によるcommit・push・コメント投稿は本検証では実行していない。

## 記録の意味

- `environment.json`: Mac miniのCLI併存、モデル単体確認、失敗した試行、アプリデータのSHA-256
- `initial-execution.json`: 管理役と4担当の実行モデル・推論レベル・親子関係・初回起動のコンテキスト設定
- `revision-1.json`、`initial-*.json`、`initial-state.json`: 初回の未修正入力・原監査結果・保存時点の状態
- `recovery.json`: 新規管理セッションがGitHubから復元した保存位置・状態とハッシュ照合
- `initial-recheck-*.json`: 初回監査の模擬判定再確認。原結果を上書きしない
- `state.json`、`findings.json`: 最新状態と指摘対応
- `continue-check.json`: 「続きお願い」だけを渡した新規セッションの保存記録確認と停止結果

模擬記録での運用確認は、本番問題の正答、日本語品質、形式的な消去法、難易度を保証しない。実際の50問処理、辞書等の根拠調査、ユーザーによる問題承認、完全自動化、アプリ反映、CLI対話画面、別端末や空の認証環境からの復帰は未検証。

初回の実施結果は `report.md`、PRレビュー対応後の追加検証と残る制約は `review-followup.md` に記録する。

再レビュー後の担当数上限・承認者確認・通常の対話CLIの承認要求は `interactive-review.md`、実行証跡は `second-review-checks.json` と `interactive-save-check.json` に記録する。
