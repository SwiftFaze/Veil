#!/usr/bin/env bash
#
# Runs a mutation-testing command (PIT or the acceptance mutator) under the
# devflow plugin's mutation lock: one run at a time across every worktree of
# this repo, since parallel runs overload the dev machine. The plugin's
# mutation-guard hook blocks an unlocked run while extra worktrees exist.
#
#   bash .claude/tools/pit-lock.sh mvn org.pitest:pitest-maven:mutationCoverage
#   MUTATION_LOCK_WAIT=600 bash .claude/tools/pit-lock.sh <command...>
#
# Exit: the command's own exit code; 75 lock held by a live run; 64 bad usage
# or devflow plugin not installed.

lock=$(ls -d "$HOME"/.claude/plugins/cache/claude-lib/devflow/*/skills/run-milestone/scripts/mutation-lock.sh 2>/dev/null | sort -V | tail -1)
[ -n "$lock" ] || { echo "pit-lock: devflow plugin (claude-lib marketplace) not installed" >&2; exit 64; }
exec bash "$lock" "$@"
