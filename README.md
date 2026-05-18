# KubeJS Profiler

A Forge 1.20.1 mod that profiles KubeJS script execution. Records how long each script takes to load, and aggregates per-function timings so you can find what's slow.

For modpack dev only. Don't ship it to end users.

## Requirements

- Minecraft 1.20.1
- Forge 47.4.10+
- [KubeJS](https://www.curseforge.com/minecraft/mc-mods/kubejs) 2001.6.5-build.16+
- [Rhino](https://www.curseforge.com/minecraft/mc-mods/rhino) 2001.2.3-build.10+ (usually shipped with KubeJS)

## Install

Drop the jar from [Releases](https://github.com/BJDubb/kubejs-profiler/releases) into your `mods/` folder alongside KubeJS and Rhino.

The config (`config/kubejsprofiler-common.toml`) is generated on first launch, with profiling enabled.

## Output

Two files land in `<gamedir>/kubejs-profiler/` when the server stops, on JVM shutdown, or via `/kubejsprofiler dump`:

- `trace-<timestamp>.json` — Chrome Trace Event Format. Open in [ui.perfetto.dev](https://ui.perfetto.dev) or `chrome://tracing`.
- `summary-<timestamp>.json` — aggregated per-function stats, sorted by total time.

By default the trace only contains script-load spans. Flip `traceSpans=true` in the config to also record per-function spans.

The summary is usually what you want. Example:

```json
{
  "generatedAt": "2026-05-18T13:46:00",
  "totalFunctions": 1234,
  "reportedFunctions": 200,
  "functions": [
    {
      "sourceName": "kubejs/server_scripts/main.js",
      "functionName": "registerRecipes",
      "firstLine": 17,
      "callCount": 1,
      "totalMs": 412.83,
      "averageMs": 412.83,
      "maxMs": 412.83
    }
  ]
}
```

## Config

`config/kubejsprofiler-common.toml`:

- `enabled` (default `true`) — master switch.
- `traceSpans` (default `false`) — emit per-function spans. Off because it gets big; summary covers most needs.
- `traceScriptLoads` (default `true`) — record script load durations.
- `minFunctionDurationNs` (default `5000000`) — only emit function spans this long or longer. `traceSpans` only.
- `maxTraceEvents` (default `25000`) — buffer cap before new events are dropped.
- `topNFunctions` (default `200`) — max entries in the summary. `0` for all.

Config changes apply after the next launch.

## Commands

Op-only (permission level 2).

- `/kubejsprofiler status` — current config + buffer sizes.
- `/kubejsprofiler dump` — write trace + summary now.
- `/kubejsprofiler reset` — clear all recorded data.
- `/kubejsprofiler toggle` — flip `enabled` for this session.

## Building

```bash
git clone https://github.com/BJDubb/kubejs-profiler.git
cd kubejs-profiler
./gradlew build
```

Needs JDK 17. Jar lands in `build/libs/`.

To release, bump `mod_version` in `gradle.properties`, then:

```bash
git tag v0.1.0
git push origin v0.1.0
```

The tag triggers the release workflow, which builds and attaches the jar.

## License

MIT.
