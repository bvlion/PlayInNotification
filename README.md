# PlayInNotification

通知エリアで30秒だけ遊べるAndroidアプリ。

仕様と開発タスクはGitHub Issuesで管理します。

## 開発環境

- JDK 25 LTS（Gradle / CI実行環境）
- Java target 17
- Android SDK Platform 37

## ビルドと検証

```shell
./gradlew testDebugUnitTest lintDebug assembleDebug
```
