# Lv1 読み→表記 1〜50問 実試運用

このバッチは現在のユーザー会話（2026-10-16）の開始承認に基づきます。承認原文は [approval.md](approval.md)、正本状態は [../../workflow.json](../../workflow.json)、指定・実測モデルとスレッド情報は [execution.json](execution.json) に記録します。

- 元素材: `feature/issue-130-difficult-kanji` の `51c8797801c0045e26d84039ba4326f659e72e66`。difficulty=1かつwrittenFormWrongAnswersが`-`でない300行の先頭50行。
- 元ID: 抽出順001〜050（足跡〜一部）。5列TSV自体にID列を追加せず、根拠JSONで対応を保持します。
- 既存素材は [source/questions.tsv](source/questions.tsv)。完成品ではありません。
- 両Issueの取得本文、確定済み168問の4コメント、アプリ配下39ファイルのSHA-256はsource配下に保存しています。
- 管理役だけが保存します。作成担当および3監査担当には書き込みと追加担当起動を禁止しています。
- 監査版は固定後変更せず、修正は新revisionへ保存します。各監査の原指摘は維持します。

問題確定はpending、完全自動化承認はnullです。次の51〜100、アプリ反映、merge、Issue closeへ進みません。未完了・未確認を合格として扱いません。
