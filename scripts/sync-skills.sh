#!/bin/sh
# Sincronizează skill-urile din sursa canonică Claude către oglinda Copilot.

set -eu

ROOT=$(CDPATH= cd -- "$(dirname -- "$0")/.." && pwd)
SOURCE_ROOT="$ROOT/.claude/skills"
TARGET_ROOT="$ROOT/.github/skills"

CHECK=0
case "${1-}" in
  --check) CHECK=1 ;;
  "") ;;
  *) echo "usage: $0 [--check]" >&2; exit 2 ;;
esac

status=0
for source in "$SOURCE_ROOT"/*/SKILL.md; do
  [ -f "$source" ] || continue
  skill=$(basename "$(dirname "$source")")
  target="$TARGET_ROOT/$skill/SKILL.md"

  if [ "$CHECK" -eq 1 ]; then
    if [ ! -f "$target" ]; then
      echo "out of sync: .github/skills/$skill/SKILL.md is missing" >&2
      status=1
    elif ! diff -q "$source" "$target" >/dev/null 2>&1; then
      echo "out of sync: $skill differs" >&2
      status=1
    fi
  else
    mkdir -p "$(dirname "$target")"
    cp "$source" "$target"
    echo "wrote .github/skills/$skill/SKILL.md"
  fi
done

if [ "$CHECK" -eq 1 ]; then
  if [ "$status" -ne 0 ]; then
    echo "run: sh scripts/sync-skills.sh" >&2
    exit 1
  fi
  echo "skill files are in sync"
fi
