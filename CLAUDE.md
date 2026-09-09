# Working in this repository

The same rules as in `Lautstark/vorlaut-app` and `Lautstark/vorlaut-diy-talker`,
which is where they were earned. The short version:

1. **Take a worktree named after your branch.** `git worktree add -b
   claude/<task> .claude/worktrees/<task> main`. No generated names; `git
   worktree list` is the dashboard. A fresh worktree needs `local.properties`
   written by hand (`sdk.dir=…`).
2. **Say who you are, first.** `git config branch.$(git branch
   --show-current).description "what you are doing"` before the first edit.
3. **Read what you are about to merge.** `git log --oneline main..HEAD`, every
   time. Never `git checkout -b x || git checkout x`.
4. **Repo-wide edits belong on main, in their own session.** Renames,
   formatting sweeps, dependency bumps — never inside a feature branch.
5. **Land your own finished work.** Trunk-based, no pull requests. Stage named
   paths, never `git add -A`: other sessions work in this checkout. Rebase onto
   `origin/main`, fast-forward, push. Ask about decisions, not about permission
   to merge.

Conventional commits are the gate (`tools/check-commit-subject.sh`, enforced by
CI; `git config core.hooksPath .githooks` tells you before the push).

## What this repository is

A player. Not an editor, not a card maker, not a symbol search, not a voice.
Every one of those exists somewhere else in the family, and
`docs/lautstark-integration.md` says where. If a task here starts to need one,
it is the wrong repository for that task.

`:cardset` must stay free of `android.*` — the build fails if it does not, on
purpose. `:app` may not decide anything a `:cardset` test could decide.

## Building

There is no system JDK on the development Mac. Point `JAVA_HOME` at Android
Studio's bundled runtime:

```sh
export JAVA_HOME="/Applications/Android Studio.app/Contents/jbr/Contents/Home"
./gradlew :cardset:check :app:assembleDebug :app:testDebugUnitTest :app:lintDebug spotlessCheck
```

Lint warnings are errors and ktlint is checked; `./gradlew spotlessApply` fixes
formatting.

## Language

Code, comments, commits and documentation are English. Text a person sees on
the phone is German first (`res/values`), English as a translation
(`res/values-en`). Domain nouns stay German in code where they are the
family's word for the thing: Kartensatz, Karten.

## Licensing, in one sentence

Nothing licensed is ever in this repository, and the app has no path that
moves a Kartensatz off the phone. Keep both true.
