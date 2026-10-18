# Local mobile patch

Base: mGBA 0.10.5, commit 26b7884bc25a5933960f3cdcd98bac1ae14d42e2.

`src/core/serialize.c`: optional `stateCompressionLevel` config for PNG savestates.
When configured to 1, core and extdata zlib use level 1; screenshot PNG uses
level 1 and the SUB row filter. Default behavior is retained if the option is absent.
The file stays a normal mGBA PNG savestate with screenshot, SRAM, RTC and metadata.
The Android wrapper opts in to level 1. MPL-2.0 headers and license are preserved.
