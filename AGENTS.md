# DVD screensaver development

Build the project described in PROJECT_PROMPT.md. Keep the app scope small: one faithful DVD logo animation, a native DreamService and a launcher preview. Use official native coding tools through the deployed OpenCode gateway; no paid inference or provider OAuth relays.

During the initial implementation, the root coordinator owned tools/, docs/, PROJECT_PROMPT.md, README.md, reference measurements, visual assets, GitHub publication and real-TV validation; the OpenCode coding client owned app/, Gradle build files and app unit tests. That implementation is complete. Future maintenance may edit the files required by the user's task. Preserve others' edits and adjust to the measured reference. Do not spawn subagents. Do not reset Git or remove partial work.

Read docs/AI_STATE.md, docs/AI_DECISIONS.md, docs/AI_HANDOFF.md and docs/REFERENCE_ANALYSIS.md when present. Update state/handoff at safe boundaries. Report real command output and distinguish passing tests from unverified checks. Do not include secrets, device identifiers or private reasoning in committed notes.

No network permission, telemetry, audio, bundled video, settings dashboard or persistent background work. TV install is authorized for preview only. Never change the selected screensaver, its enabled state, Home or unrelated TV apps. Coordinator may wake the TV briefly for the authorized preview, then restore its prior sleep state. Do not publish device IPs, identifiers, user data, raw videos or local SDK paths. Keep local evidence under ignored work/.
