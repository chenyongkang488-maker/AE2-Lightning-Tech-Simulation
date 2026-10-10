# Contributing / 贡献指南

## Build and test

Use Java 21 JDK, Python 3.11+ for release checks, and the checked-in Gradle wrapper. Set `JAVA_HOME` if Java 21
is not already on PATH. Follow the build commands in [README.en.md](README.en.md).
No fixed drive letter, global Gradle installation or publishing credentials are required.
PowerShell 7 is required for the bootstrap script on Linux/macOS.

The normal release checks are:

```powershell
.\scripts\bootstrap.ps1
.\gradlew.bat verifyReleaseVersion build runGameTestServer --no-daemon --console=plain
python .\scripts\verify-release.py
```

JUnit results appear in `build/test-results/test/`; GameTest logs in
`run-gametest/logs/latest.log`. The GitHub workflow runs the same checks on
Windows and Linux. It uploads build artifacts and reports and does not publish a release.

Optional integration checks:

- Mekanism: add `-PmekTests` to the Gradle command.
- Mystical Agriculture: place `MysticalAgriculture-1.21.1-8.0.28.jar` and
  `Cucumber-1.21.1-8.0.16.jar` in ignored `libs/`, then add `-PcropTests`.
- Both: `gradlew.bat build runGameTestServer -PcropTests -PmekTests`.
- Applied Flux: place `AppliedFlux-2.1.6.jar` and `Glodium-2.2.jar` in ignored
  `libs/`, then add `-PfluxTests`.

Compilation targets AE2 19.2.17 / AE2LT 2.1.0 / Thunderbolt 2.0.0. The default
runtime uses AE2 19.2.18 / AE2LT 2.1.2-beta2 / Thunderbolt 2.0.3-beta2. To test the same
classes against the original runtime without editing sources:

```powershell
.\gradlew.bat runGameTestServer '-Pae2RuntimeVersion=19.2.17' '-Pae2ltRuntimeVersion=2.1.0' '-PthunderboltRuntimeVersion=2.0.0'
```

Select another 1.21.1 loader with `'-PneoForgeVersion=21.1.255'`. For runtime
version overrides, supply the corresponding JARs in `libs/`. The bootstrap
manifest contains both tested upstream sets with SHA-256 checksums.

Do not install fixtures into a normal game. `src/gametest/resources` is loaded
only by the GameTest run and excluded from release archives. Existing GameTest
classes use a dedicated test-server setup.

## Changes

Explain the concrete player or modpack problem and the changed behavior. Run the
tests relevant to gameplay changes, then the release checks for a release candidate.
Keep build/test dependencies checksum-pinned. Distinguish metadata admission ranges
from versions verified in real runtime tests; do not claim untested future API compatibility.
Preserve existing registry IDs and serialized component formats or provide a migration.
The public APIs and datapack examples are linked from the README; update them when
changing observable behavior.

To change the version, edit only `mod_version` in `gradle.properties` and add
the matching changelog section. Gradle expands the packaged mod metadata.
Verify a tag before creating it:

```powershell
.\gradlew.bat verifyReleaseVersion '-PreleaseModVersion=0.2.0-alpha.5'
```

Release tags are `v<mod_version>`. Preserve existing tags and previously published
JARs. Use a new beta version for another public candidate.

## Blockbench artwork

The editable projects and generator plugins are in `art/`. Open the `.bbmodel`
files directly in Blockbench; their textures are embedded. For the workshop plugins,
set their `ROOT` project directory and the multiblock helper's referenced path to
your checkout before loading them. These legacy editor helpers have local export
paths and are not part of the Gradle build.

Plugins can overwrite exported artwork. Save changes with Git first. Keep pixel
styling and the pink/white palette. Original image pixels should be edited and
exported through Blockbench. Texture checks use Pillow:

```powershell
python .\scripts\verify-multiblock-resources.py
```

## Licensing

Contributions to code/docs/original artwork use the applicable MIT license.
Changes to adapted crystals and module icons retain CC BY-NC-SA 3.0.
Keep source attribution and describe modifications in THIRD_PARTY_NOTICES.md.
Do not commit dependency JARs, launcher accounts, worlds, caches, logs or tokens.

## 中文说明

提交时说明问题、行为变化及验证结果。使用 Java 21 和 Gradle Wrapper；
神秘农业/通用机械只是可选测试环境。保留已有物品与存档 ID，修改 API 时更新文档。
用 Blockbench 编辑素材并保留上游署名。版本只改 gradle.properties 的 mod_version，
同时添加更新记录；公开标签不覆盖。问题反馈可使用仓库的双语模板。
