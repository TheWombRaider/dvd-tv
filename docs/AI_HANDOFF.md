# Resumable state

Read PROJECT_PROMPT.md, AI_STATE.md, AI_DECISIONS.md and VALIDATION.md. The app is implemented and the final APK is installed for preview. All 28 app JVM tests and the Gradle release/lint build passed. Reference and seven actual TV captures passed the documented tolerances. The selected Pipes screensaver was preserved; the TV was restored to sleep.

Changed files: new native app/ source and tests, Gradle wrapper/build files, reference measurements, Python validation/artwork tools and project documentation. No existing pipes-tv source was changed. A separate cleanyourtv worktree contains only a SHOWCASE.md contribution.

Source is published at https://github.com/TheWombRaider/dvd-tv. Showcase PR https://github.com/Gathin23/cleanyourtv/pull/969 changes only SHOWCASE.md, with a real TV screenshot. It was open and mergeable with checks queued at submission; do not merge it. No app work remains under the authorized preview-only scope.

For further development, use OpenCode's persisted project session or an official native client with this repository and these notes. On the original ThinkPad, run `ai-dev -Project <path-to-dvd-tv> -Resume`; for a manual Google continuation, use `ai-dev -Project <path-to-dvd-tv> -Agent Google`. A quota interruption must not reset Git or delete partial changes. There is no automatic Gemini fallback in the deployed environment.

Known unresolved issue: the clipped 150-second reference endpoint differs by two ticks. Actual DreamService activation, long-duration device performance and native 4K output remain unverified. Last failed check: cleanyourtv's full Python suite on Windows, mainly command-launch failures; see VALIDATION.md. Do not alter unrelated upstream tests.
