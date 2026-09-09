# Repository Guidelines

## プロジェクト概要

PlayInNotification は、通知エリアを主なプレイ場所として30秒の短時間ゲームを行うAndroidアプリです。

普段のプレイは通知から完結することを基本とし、アプリ本体は設定と成績確認を主な役割とします。

AndroidアプリはKotlinとAndroid標準Viewで実装し、単一の`app`モジュールで構成します。`compileSdk`と`targetSdk`は37、`minSdk`は23です。

確定済みのプロダクト仕様はGitHub Issueを正とし、`AGENTS.md`へ重複して記載しません。

## Worktree

- Codexは、現在のworktreeと対応ブランチ内だけを変更してください。
- 他のworktreeや、`scripts/create-codex-worktree.sh`を実行したコピー元worktreeを変更しないでください。
- Issue用worktreeは、コピー元worktreeで`scripts/create-codex-worktree.sh <Issue番号> <接尾辞>`を実行して作成してください。

## 開発方針

- 依頼またはIssueのscopeに必要な変更へ集中し、無関係な変更を同じPull Requestへ混ぜないでください。
- 実装前に関連する既存コード、呼び出し元、既存テストを確認してください。
- 既存の設計、パッケージ構成、命名、記述形式がある場合はそれを優先してください。
- 仕様が不明確で複数の妥当な実装がある場合は、推測で決めず実装前にユーザーへ確認してください。
- 確定済みの仕様を、実装都合だけを理由に変更しないでください。
- 現在のIssueに不要な機能や抽象化を先回りして追加しないでください。
- 生成されたキャッシュやIDE固有ファイルをcommitしないでください。

## コーディング規約

- `.editorconfig`に従ってください。
- productionコードの識別子は通常のKotlin命名規則に従い、日本語化しないでください。
- アプリ側で定義するユーザー向け文言はAndroid string resourcesで管理してください。

### コメント

- コメントやKDocは必要な場合に限って追加してください。
- コードだけでは残せない理由、制約、意図が将来の変更判断に必要な場合に記述してください。
- 処理を言い換えるだけのコメントやKDocは書かないでください。
- 記述する場合は日本語としてください。

### テスト関数名

- Kotlinの`@Test`関数はbacktick形式を使い、日本語で記述してください。
- `@Test`以外の識別子は通常のKotlin命名規則に従ってください。

## ビルドとテスト

- JDK 17とAndroid SDK Platform 37を使用してください。
- Gradleはリポジトリ同梱のGradle Wrapperを使用してください。
- unit testは`./gradlew testDebugUnitTest`で実行してください。
- lintは`./gradlew lintDebug`で実行してください。
- debug buildは`./gradlew assembleDebug`で実行してください。
- Pull RequestのCIと同じ一括検証は`./gradlew testDebugUnitTest lintDebug assembleDebug`で実行してください。
- 変更後は`git diff --check`を実行してください。
- documentationやrepository運用設定のみの変更では、Gradleによる検証は不要です。
- Androidのproductionコード、テスト、build設定等へ影響する変更では、変更範囲に対応するunit test、lint、buildを実行してください。
- 実行できなかった検証や失敗した検証を成功扱いにせず、実行内容と理由を報告してください。

## UI確認

- AIエージェントは実機、エミュレーター、adb、UI automationを使用してアプリのUI操作や目視確認を行わないでください。
- UIの見た目や操作感はユーザーが実機で確認します。
- UI確認が必要な場合は、ユーザーが確認すべき項目を報告してください。
- UI確認を目的としたinstrumented test、emulator操作、adb操作を代替手段として追加しないでください。

## 秘密情報

- `local.properties`、署名情報、APIキー、アクセストークン等の秘密情報を新規作成、編集、commit、log出力しないでください。
- 必要な秘密情報が環境にない場合は、ダミー値や設定変更で迂回せず、実行できなかった作業として報告してください。
- `.gitignore`を変更して秘密ファイルを追跡対象にしないでください。

## Git と Pull Request

- `main`へ直接commit / pushせず、専用branchとPull Requestを使用してください。
- Codexのbranchは`codex/<short-task-name>`、Claudeのbranchは`claude/<short-task-name>`、ChatGPTのbranchは`chatgpt/<short-task-name>`としてください。
- commit件名は英語の命令形で簡潔に記述してください。
- Issue対応を依頼された場合は、調査、実装、検証、commit、pushを行い、Pull RequestをReady for reviewとして作成してください。
- Pull Request本文には対応Issue、目的または原因、変更内容、最終的な検証結果を記載してください。
- 影響のある未実施または失敗した検証が残る場合はPull Request本文に記載してください。
- UI変更で実機確認が有用な場合は、ユーザーが確認すべき項目をPull Request本文または完了報告に記載してください。
- ユーザーの承認なしにPull Requestをマージしないでください。
- Issueを手動でcloseせず、タグやReleaseを作成しないでください。
- 破壊的操作、追加の認証、依頼範囲外の変更が必要な場合は、実行前にユーザーへ確認してください。
- Pull Requestを自動Approveしないでください。

## Review

- Claude / Codex等へ実装やreviewを依頼した場合、後から確認する必要のある作業結果や判断はGitHub上に残してください。指示内容を言い換えただけの報告は不要です。
- 作業中に確認できる既存のreview threadは、判断済みのまま未resolvedで放置しないでください。
- 指摘へ対応した場合は、対応内容を簡潔に返信してresolveしてください。
- 対応不要と判断した場合は、理由を簡潔に返信してresolveしてください。
- ユーザー判断が必要な場合はresolveせず報告してください。
- 権限や利用可能なツールの制約で返信・resolveできない場合は、その旨を報告してください。
- 新しいreviewの到着をsleepやpollingで待たないでください。

reviewのseverityをそのまま修正優先度として扱わないでください。

まず、通常の人間操作だけで再現できるかを確認してください。

高速操作、狭いrace window、特殊な端末状態、I/O障害、fault injection、テスト用Fake等が必要な場合は、その条件と現実の発生可能性を明示してください。

論理的に到達可能、またはunit testで再現可能というだけではproduction修正の理由にしないでください。

発生頻度、実害、不可逆性、修正による複雑化、新規不具合リスクを比較して対応を判断してください。

security、privacy、データ破損・喪失等は、低頻度でも被害の大きさを考慮してください。
