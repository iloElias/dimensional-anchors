#!/usr/bin/env bash
set -euo pipefail

trap 'printf "Test build/install failed at line %s.\n" "$LINENO" >&2' ERR

project_dir="$(cd -- "$(dirname -- "${BASH_SOURCE[0]}")" && pwd)"
mods_dir="$HOME/Documents/curseforge/minecraft/Instances/DA Mod Test/mods"
cd -- "$project_dir"

if [[ ! -d "$mods_dir" ]]; then
    printf 'CurseForge mods directory not found: %s\n' "$mods_dir" >&2
    exit 1
fi

mod_id="$(sed -n 's/^mod_id=//p' gradle.properties)"
mod_version="$(sed -n 's/^mod_version=//p' gradle.properties)"
if [[ ! "$mod_id" =~ ^[a-z][a-z0-9_]+$ || ! "$mod_version" =~ ^[a-zA-Z0-9][a-zA-Z0-9.+-]*$ ]]; then
    printf 'Invalid mod_id or mod_version in gradle.properties.\n' >&2
    exit 1
fi

printf 'Close Minecraft before installing this test build.\n'
bash ./gradlew --no-daemon --no-parallel --max-workers=1 build

jar_name="$mod_id-$mod_version.jar"
artifact="$project_dir/build/libs/$jar_name"
if [[ ! -f "$artifact" ]]; then
    printf 'Built mod JAR not found: %s\n' "$artifact" >&2
    exit 1
fi

staged="$(mktemp "$mods_dir/.dimensionalanchors-install.XXXXXX")"
trap 'rm -f -- "$staged"' EXIT
install -m 644 -- "$artifact" "$staged"
cmp -- "$artifact" "$staged"
mv -f -- "$staged" "$mods_dir/$jar_name"
find "$mods_dir" -maxdepth 1 -type f -name "$mod_id-*.jar" ! -name "$jar_name" -delete
printf 'Installed: %s/%s\n' "$mods_dir" "$jar_name"
