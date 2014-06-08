audio2png
=========

Creates PNG files from audio files. You can use [png2audio-player](https://github.com/4142/png2audio-player) or [png2audio-web](https://github.com/4142/png2audio-web) to play them back.

Download
--------

The latest JAR file is available on the [releases page](https://github.com/4142/audio2png/releases).

How it works
------------

An audio file is first converted to the Ogg Vorbis format using ffmpeg. Then, the resulting file is encoded into a PNG image. Bytes are encoded as colour values for each pixel in the image, so that for every pixel four bytes can be encoded (three colour channels and an alpha channel).

Other stuff
-----------

Requires ffmpeg, which is available at http://www.ffmpeg.org/ or through the package manager in most GNU/Linux distributions.

Licensed under the GNU GPLv3.
Makes use of [pngj](https://code.google.com/p/pngj/), which is licensed under the Apache License 2.0.
