# やるベル (Android starter project)

Android向けTodo＋アラームアプリのソースプロジェクトです。Android Studioで開いて実行することを想定しています。

## 現在の実装
- Todo追加（毎日・毎週・毎月・単発）
- 時刻指定
- 毎週の曜日指定
- 端末内へのタスク保存
- 完了チェックと削除
- Android AlarmManagerによる正確な時刻のアラーム予約
- アラーム音を25秒再生し、未完了なら5分後に再度鳴動
- 完了・5分後・停止の通知アクション
- 再起動／時刻変更時のアラーム再登録

## ビルド
1. Android Studioでこのフォルダを開く
2. Gradle同期を行う
3. Android端末を接続して実行する

## 注意
この環境ではAndroid SDK/Gradleビルド環境が確認できないため、APKのビルド・実機検証は未実施です。Android 12以降では「正確なアラーム」の許可、Android 13以降では通知許可が必要です。メーカー独自の省電力設定やサイレント／おやすみモード、音量設定によって鳴動が制限される場合があります。初版のため、日付変更後の再予約やアラームの多重起動など、実機テストで追加調整が必要です。


## やるベル v2 作業内容
- マスター一覧の初期表示を「すべて」に変更し、毎日・単発などを同じ一覧に表示
- 日付選択を追加し、選んだ日に該当する繰り返しタスクと単発タスクを表示
- 全体のベースカラーを明るいブルー系に変更

※開始・終了時刻、延長操作、全画面アラーム、色設定は今後の実装項目です。


## V2 作業メモ（2026-11-23）

- アラームの一時停止を追加。1時間・2時間・手動再開までの3通り。
- 停止中は開始アラームを鳴らさず、タスクと登録時刻は保持する。時間指定停止は期限到来時に自動再開する。
- タスク一覧の繰り返し表示を調整し、毎日は `[毎日]`、単発は `[月/日]` と表示する。
- アプリのバージョンを 0.2.0 に更新。

※ 現時点の元プロジェクトには、開始・終了の2時刻登録、複数音源のランダム再生、カレンダーの日別件数表示はまだ未実装。

## V2 work in progress (0.3.0)
- New tasks can store separate start and end times.
- Start alarms show 「始める」; end alarms show 「今日はここまで」.
- Existing V1 task records retain their original `hour`/`minute` as the start time. Tap a task name to edit it and set its end time.
- Alarm pause continues to suppress both alarm types.
- Multiple system alarm sounds can be selected, previewed, and randomly chosen for both alarm types.
- A monthly calendar shows the number of scheduled tasks on each date, including daily recurring tasks.
- Importing custom audio files and the configurable alarm-screen color are not implemented yet.
- Build and device verification have not been performed in this environment.

## Build APK with GitHub Actions
The repository includes `.github/workflows/android.yml`. After the project files are pushed to the repository, GitHub Actions can build a debug APK on each push to `main` or via **Actions → Build Android APK → Run workflow**. The APK is uploaded as the `yaberu-v2-debug-apk` artifact.
