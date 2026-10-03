"""Verify the standalone release artifacts. Requires Python 3.11+; no packages."""
from pathlib import Path
import argparse
import hashlib
import json
import tomllib
import zipfile

ROOT = Path(__file__).resolve().parents[1]


def verify(tag=None):
    versions = [
        line.split("=", 1)[1].strip()
        for line in (ROOT / "gradle.properties").read_text().splitlines()
        if line.startswith("mod_version=")
    ]
    if len(versions) != 1:
        raise ValueError("Expected one mod_version")
    version = versions[0]
    if tag is not None and tag != f"v{version}":
        raise ValueError(f"Tag {tag} does not match v{version}")
    if f"## [{version}]" not in (ROOT / "CHANGELOG.md").read_text(encoding="utf-8"):
        raise ValueError(f"Missing changelog section for {version}")
    licenses = {
        "META-INF/LICENSE": ROOT / "LICENSE",
        "META-INF/LICENSES.md": ROOT / "LICENSES.md",
        "META-INF/THIRD_PARTY_NOTICES.md": ROOT / "THIRD_PARTY_NOTICES.md",
        "META-INF/AE2LT-LICENSE_ASSETS.md": ROOT / "art/licenses/AE2LT-LICENSE_ASSETS.md",
    }
    artifacts = []
    for classifier in ("", "-sources"):
        path = ROOT / f"build/libs/overload_sim-{version}{classifier}.jar"
        with zipfile.ZipFile(path) as jar:
            names = jar.namelist()
            if len(names) != len(set(names)):
                raise ValueError(f"Duplicate ZIP entries in {path.name}")
            for entry, original in licenses.items():
                if jar.read(entry) != original.read_bytes():
                    raise ValueError(f"Missing or changed license: {path.name}/{entry}")
            if classifier == "":
                mods = tomllib.loads(jar.read("META-INF/neoforge.mods.toml").decode())
                mod = next(m for m in mods["mods"] if m["modId"] == "overload_sim")
                template = (ROOT / "src/main/resources/META-INF/neoforge.mods.toml").read_text(encoding="utf-8")
                expected_mods = tomllib.loads(template.replace("${mod_version}", version))
                if mods != expected_mods or mod["version"] != version:
                    raise ValueError("Packaged version/author metadata mismatch")
                if jar.testzip() is not None:
                    raise ValueError("Corrupt JAR")
                dependencies = {d["modId"] for d in mods["dependencies"]["overload_sim"]}
                if dependencies != {"minecraft", "neoforge", "ae2", "ae2lt", "thunderbolt", "guideme"}:
                    raise ValueError("Required dependency declarations changed")
                forbidden = ("net/minecraft/", "net/neoforged/", "appeng/", "mekanism/", "com/ae2lt/", "com/thunderbolt/")
                for name in names:
                    if name.endswith(".jar") or (name.endswith(".class") and not name.startswith("dev/overloadsim/")):
                        raise ValueError(f"Bundled dependency: {name}")
                    if name.startswith(forbidden):
                        raise ValueError(f"Unexpected upstream classes: {name}")
                resources = ROOT / "src/main/resources"
                for source in resources.rglob("*"):
                    if not source.is_file() or source.name == "neoforge.mods.toml":
                        continue
                    entry = source.relative_to(resources).as_posix()
                    if jar.read(entry) != source.read_bytes():
                        raise ValueError(f"Runtime resource differs: {entry}")
                fixtures = ROOT / "src/gametest/resources"
                for source in fixtures.rglob("*"):
                    entry = source.relative_to(fixtures).as_posix()
                    # A test override may share an ID with a production tag. The
                    # resource comparison above already requires the production bytes.
                    if source.is_file() and not (resources / entry).is_file() and entry in names:
                        raise ValueError(f"Test fixture packaged: {source}")
        artifacts.append({
            "file": path.name,
            "bytes": path.stat().st_size,
            "sha256": hashlib.sha256(path.read_bytes()).hexdigest(),
        })
    return {"version": version, "tag": f"v{version}", "artifacts": artifacts}


if __name__ == "__main__":
    parser = argparse.ArgumentParser(description=__doc__)
    parser.add_argument("--tag", help="Reject a Git tag that differs from mod_version")
    args = parser.parse_args()
    print(json.dumps(verify(args.tag), ensure_ascii=False, indent=2))
