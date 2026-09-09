#!/bin/sh
# Generates the example Kartensatz's media with ffmpeg: three short test-pattern
# videos and one spoken-word stand-in, so the player can be exercised on the
# phone before a single real sign video or stimmquelle file exists.
#
# Nothing here is a sign. The files are colour bars with a tone; they prove the
# mechanics (card -> video, external speech, silence) and nothing about content.
# They are generated rather than committed because a generated file may only
# carry what its inputs determine, and an ffmpeg output carries its version.
#
#     tools/make-example-media.sh          # writes into example/kartensatz/
set -eu
cd "$(dirname "$0")/../example/kartensatz"
mkdir -p videos audio

# essen: the video carries the word (a tone stands in for it) -> speech: video
ffmpeg -y -loglevel error -f lavfi -i "testsrc2=size=640x360:rate=25:duration=3" \
  -f lavfi -i "sine=frequency=660:duration=0.6" \
  -c:v libx264 -pix_fmt yuv420p -profile:v baseline -c:a aac -shortest videos/essen.mp4
# trinken: silent video, the word comes from audio/trinken.mp3 -> speech: external
ffmpeg -y -loglevel error -f lavfi -i "smptebars=size=640x360:rate=25:duration=3" \
  -c:v libx264 -pix_fmt yuv420p -profile:v baseline -an videos/trinken.mp4
ffmpeg -y -loglevel error -f lavfi -i "sine=frequency=440:duration=0.6" -c:a libmp3lame -b:a 128k audio/trinken.mp3
# schlafen: silent on purpose -> speech: none
ffmpeg -y -loglevel error -f lavfi -i "color=c=0x334455:size=640x360:rate=25:duration=3" \
  -c:v libx264 -pix_fmt yuv420p -profile:v baseline -an videos/schlafen.mp4

ls -l videos audio
