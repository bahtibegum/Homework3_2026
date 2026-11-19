# Updating your existing GitHub repository from this ZIP

This ZIP is a new copy of the project, not a Git clone. Your existing repository is `https://github.com/bahtibegum/Homework3_2026`.

1. Extract the ZIP and first inspect `README_2026.md` and `FIXES_AND_TESTS.md`.
2. Make a backup of your local `Homework3_2026_Revised` folder (or commit any work in progress).
3. Copy the **contents** of the extracted `Homework3_2026_Code_Fixed` directory into your existing `Homework3_2026_Revised` folder, allowing source files and scripts to replace the older versions. Do not remove your original data, Git configuration or uncommitted team changes. Since the slides are unchanged, you may leave your existing PPTX files as they are.
4. In IntelliJ's PowerShell terminal, run `.\scripts\build.ps1`. Next run the `DeadlockTest` on your dedicated database, followed by a five-second test. Follow `README_2026.md` exactly; the 30-second matrix resets `blocking_data` before every experiment.
5. To share code changes on GitHub after confirming they run:

```powershell
git status
git add src scripts README_2026.md FIXES_AND_TESTS.md HOW_TO_UPDATE_GITHUB.md MEASURED_RESULTS_START_HERE.md results .gitignore
git commit -m "Fix 2026 transaction sampling, retries and JDBC batching"
git push origin main
```

Use `git status` before committing to check which files are actually staged. If teammates have changed the branch, coordinate before pushing; don't force-push or overwrite their changes.

## Important

The screenshots and original benchmark CSV are **pre-fix evidence**. Running the revised code produces **new** CSV results. Do not claim the new optimization is faster until you've measured it; include citations for older code/AI as required by the professor.
