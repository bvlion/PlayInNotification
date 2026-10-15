# 難読漢字の継続作成・監査

Issue [#130](https://github.com/bvlion/PlayInNotification/issues/130) と [#140](https://github.com/bvlion/PlayInNotification/issues/140) を仕様の正とする。これはCodex CLI 0.162.1で検証する運用であり、問題データの正本を移動するものではない。

## 起動

Issue専用worktree内で `./scripts/run-kanji-workflow.sh` を実行する。Python 3を使用してCLI 0.162.1以降か検査する。複数のCLIが導入されている場合は `KANJI_CODEX_BINARY=/path/to/codex ./scripts/run-kanji-workflow.sh` で実行ファイルを指定する。Mac miniで検証した0.162.1は `/Users/iwai/.asdf/installs/nodejs/22.17.0/bin/codex` にあり、通常の `codex` が旧Homebrew版0.148.0を指すシェルがある。個人のPATHやインストール設定は変更しない。新規worktreeは既存の `scripts/create-agent-worktree.sh codex <short-task-name>` で作成する。CLIに同じworktreeを信頼する設定が必要な場合は、ユーザーがCodexの通常の信頼確認を行う。個人の認証・設定をリポジトリへ保存しない。プロジェクト設定が読み込まれずカスタム担当が見つからない場合は停止し、標準担当へ置換しない。

管理役のモデル指定は専用起動スクリプトのCLI引数だけで行い、リポジトリ共通のmodel・model_reasoning_effortは設定しない。通常のCodex起動ではユーザー設定またはその起動時の指定を維持する。管理役はメインセッション（gpt-6.1-sol / medium）。問題作成、正誤検証、日本語品質監査はgpt-6.1-sol / high、形式監査はgpt-6-luna / high。4個のカスタム担当を `.codex/agents/` に定義し、同時に開ける子スレッドを4にする。担当の追加起動は禁止する。管理役は設定値と、取得可能なら実行履歴の実測値を区別してモデル・推論レベルを確認し、利用不可・指定不一致なら結果を保存して停止する。別モデルへの代替はユーザー判断とする。

`workflow.json` の初期状態は `setup_only`。この状態で「続きお願い」を受けても実際の問題は作成・監査しない。作成開始の明示指示をGitHubコメントへ記録し、そのURLを `productionStartApproval` に保存してから本番へ進む。完全自動化には別の承認が必要であり、`automationApproval` がない限り各範囲のユーザーレビューを省略しない。

## 管理役の進行手順

1. Issue本文、確定記録、最新の保存位置を照合する。Lv1 表記→読み168問は完了済みとして保護する。確定済みTSVの変更・追加・再選定は行わない。次の対象はLv1 読み→表記。今回は本番開始前である。
2. 本番の開始承認後、専用worktreeの `operations/kanji/batches/<batch-id>/` を使用する。問題ID、対象Lv、方向、範囲、元データのブランチとコミットを固定する。既存素材は `feature/issue-130-difficult-kanji` から読む。対象範囲と件数はIssueに従い、品質のために仕様判断が必要ならユーザーへ報告する。
3. `kanji_creator` を起動し、作成または修正を依頼する。成果物は管理役だけが保存する。監査担当を問題作成の代わりに使用しない。問題作成が完了したら結果受領と完了状態を記録する。
4. 入力を `revisions/<revision>/questions.tsv` と `evidence.json` に保存し、SHA-256を記録して変更しない。TSVは既存の5列形式とする。対象外の出題方向の候補欄は `-`。IDとTSV行の対応、正答・各候補の根拠を `evidence.json` に保存する。件数固定のテストやアプリ本体への反映は、この監査運用の段階では行わない。
5. `kanji_correctness_auditor`、`kanji_format_auditor`、`kanji_japanese_quality_auditor` を別々に起動する。初回は必ず新しいコンテキスト（`fork_context=false`、同等の `fork_turns="none"` があるクライアントではそれを使用）で、同じ版・SHA-256・問題・根拠・担当観点だけを渡す。ほかの監査結果や統合指摘を渡さない。スナップショットを直接メッセージに含め、監査担当に保存先ディレクトリを探索させない。各担当が返るまで入力を変更しない。
6. 全員の結果を `audits/<revision>/<role>.json` に保存する。管理役は問題IDと論点で指摘を統合し、原指摘との対応を残す。担当間の見解相違を多数決で消さず、根拠を確認できるまで未解決にする。根拠不明、未完了、担当エラー、監査版違いも合格にしない。初回監査完了後は全担当の完了状態を記録する。クローズ機能があるクライアントでは担当を閉じる。機能がない場合は未クローズを記録し、上限に達したら途中保存して新しい管理セッションから不足担当を起動する。
7. 未解決があれば統合指摘を問題作成担当へ渡す。修正は新しい版へ保存する。全監査担当を新規コンテキストで起動し、修正版の全問・全候補を再確認する。旧指摘の解消だけでなく、それぞれの担当観点全体を再確認する。必要な回数この手順を繰り返す。担当の読める入力と指示を管理役が制限する方式であり、OS上の読み取り権限を担当ごとに隔離する方式ではない。
8. 最新版に対する全監査の完了、未解決指摘ゼロを確認し、`awaiting_user_approval` にする。ユーザーへ全問と監査結果を提示し、その版に対する明示承認コメントURLを得るまで確定しない。監査担当や管理役の合格はユーザー承認にならない。修正されたら旧承認は適用せず、全監査とユーザーレビューをやり直す。
9. 承認URLと版を記録してその範囲を `approved` とする。次の範囲へ進めるのは承認後だけ。アプリ反映は別の明示された作業として既存の統合ブランチとPull Requestを使用し、勝手にmerge・Issue closeしない。

## 保存する記録

`workflow.json` に本番開始承認・自動化承認・保護対象・次の対象・完了範囲を保存する。`activeBatch` は未開始なら `null`、進行中なら次の情報を持つオブジェクトにする。

- `batchId`、`difficulty`、`direction`、`range`、`sourceBranch`、`sourceCommit`、`workBranch`
- `revision`、`snapshotPath`、`snapshotSha256`、`status`、`nextAction`
- `assignments`: 担当名、モデル、推論レベル、スレッドID（取得不可なら `null` と担当パス）、状態、監査結果のパス
- `unresolvedFindings`: 指摘ID、元監査担当、問題ID、候補、理由、根拠・出典URL、状態、解消を確認した版
- `userApproval`: `status`（`pending` / `approved`）、対象版、承認コメントURL（未承認は `null`）

CLIの起動結果がスレッドIDや実測モデルを返さない場合は、設定値を実測値と偽らず未取得として保存する。同じ端末では対象セッションのrolloutの `session_meta` と `turn_context` から担当パス・親ID・model・effortを確認できる。認証ファイルや生の会話ログをGitHubへ保存しない。

各監査結果は `role`、`revision`、`snapshotSha256`、`status`（`completed` / `incomplete`）、`findings` を持つ。指摘は安定したID、問題ID、対象候補、理由、根拠、出典URLを持つ。問題がなくても空の `findings` と確認範囲を返す。検証の模擬記録は `verification/` に隔離し、本番状態や承認に転用しない。

## GitHubへの途中保存

管理役だけがファイルとGitHubを更新する。作成完了時、各監査結果の受領時、統合時、修正完了時、承認待ち・中断時に状態を保存する。途中で一部担当だけが完了しても、その結果と未完了担当を保存する。

1. 対象ファイルを明示して `git add`、`git diff --cached --check`、英語命令形の件名でcommitし、専用ブランチをpushする。問題・根拠・監査結果・未解決指摘・状態を同じcommitに含める。
2. push成功を確認してから、Issue #140へ `<!-- kanji-workflow-checkpoint -->` を含むコメントを投稿する。モード（本番 / 検証）、保存ブランチ、完全なコミットID、状態ファイルのGitHubリンク、現在の版、未解決数、未完了担当、承認状態、次の作業を記載する。コメント本文はファイルから `gh issue comment --body-file` で送信する。
3. 失敗時はローカルに残った未保存分を報告し、GitHub保存成功とみなさない。承認待ちの状態を保存したら停止する。承認や新しいreviewを待ってsleep・pollingしない。

Mac miniのCLI 0.162.1で、`workspace-write`・ネットワーク許可・`approval_policy=never`の非対話セッション自身による保存を追加検証したところ、模擬ファイルの作成後、`git add`が共有Git管理領域の `index.lock` 作成時に `Operation not permitted` で失敗した。この条件では管理役だけでcommit・push・Issue投稿まで完結できる運用は未成立である。専用起動スクリプトは権限を変更しない。拒否されたら以後の保存操作を停止し、GitHub保存成功とみなさず制約を報告する。別の権限条件での成功は確認していない。

代表的な失敗は、途中保存のpush前に端末が停止し、ローカルだけの監査結果が失われることである。復帰はpush済みの版を基準とし、その後の未保存担当だけを再実行する。

## 新しいセッションからの復帰

1. 専用起動スクリプト、Issue #130 / #140の明示、または現在の会話で確定した難読漢字の作業文脈がある場合に、この復帰手順を適用する。その文脈で「続きお願い」を受けたらIssue #140の保存コメントを新しい順に確認する。別Issueの作業中は元の作業を継続する。文脈がない新規セッションの「続きお願い」だけでは対象を確認し、ブランチ名や保存記録の存在だけでこの手順を開始しない。`<!-- kanji-workflow-checkpoint -->` を識別し、本番・検証を区別する。最新コメントのブランチと完全なコミットIDを取得し、該当する `workflow.json` または検証状態を読む。初期状態より新しい本番記録があればそちらを採用する。検証記録を本番の進捗とみなさない。
2. ローカルの未commit変更や、リモートの後続commitを確認する。不一致があれば上書きせず差分と候補を報告する。別worktreeの内容を変更しない。保存ブランチが他worktreeにある場合はそのworktreeを使い、新しいcheckoutが必要なら既存スクリプトで専用worktreeを作成して保存済みcommitを取り込む。
3. スナップショットSHA-256、版、担当、未解決指摘、承認URLを照合する。ローカルの古いセッションIDだけに依存しない。新しい管理セッションから不足担当だけを再起動できる。
4. `creating` は保存済み作成結果を確認して不足分を作成担当へ渡す。`auditing` は同じ版に対する完了済み独立監査を再利用し、不足担当だけを新規起動する。`revising` は統合指摘から修正を再開する。修正完了後は全監査担当を再実行する。`awaiting_user_approval` は承認を待ち、次の問題を作らない。`setup_only` は実際の問題作成開始の指示を待つ。
5. CLIの `codex resume <session-id>` / `codex exec resume <session-id>` は同じ端末に履歴がある場合の補助とする。GitHub記録からの新規セッション復帰を正とする。

## 検証と公式仕様

検証では実際の問題を扱わず、`verification/` に版と3担当用の根拠欄だけを持つ模擬記録を使う。作成担当の起動、3担当の独立監査、指摘統合、途中push、新規管理セッションでの復元、修正担当の再起動、全監査担当の再確認、承認待ちを確認する。これは問題品質や50問処理の評価ではない。

- [Subagentsとカスタム担当の公式仕様](https://learn.chatgpt.com/docs/agent-configuration/subagents)
- [プロジェクト設定と優先順位](https://learn.chatgpt.com/docs/config-file/config-basic)
- [設定項目](https://learn.chatgpt.com/docs/config-file/config-reference)
