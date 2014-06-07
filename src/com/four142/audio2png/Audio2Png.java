/*
 * This program is free software: you can redistribute it and/or modify
 * it under the terms of the GNU General Public License as published by
 * the Free Software Foundation, either version 3 of the License, or
 * (at your option) any later version.
 *
 * This program is distributed in the hope that it will be useful,
 * but WITHOUT ANY WARRANTY; without even the implied warranty of
 * MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the
 * GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program.  If not, see <http://www.gnu.org/licenses/>.
 */

package com.four142.audio2png;

import ar.com.hjg.pngj.ImageInfo;
import ar.com.hjg.pngj.ImageLineInt;
import ar.com.hjg.pngj.PngWriter;

import java.io.ByteArrayOutputStream;
import java.io.File;
import java.io.IOException;
import java.io.InputStream;

public class Audio2Png {

    public static void main(String[] args) {

        if (args.length != 3) {
            System.err.println("USAGE: java -jar audio2png.jar <input file> <output file> <path to ffmpeg executable>");
            System.exit(1);
        }

        File input = new File(args[0]);
        File output = new File(args[1]);
        File ffmpeg = new File(args[2]);

        byte[] data = null;

        // Try to convert the input file to Ogg Vorbis
        try {
            data = convert(input, ffmpeg);
        } catch (IOException e) {
            System.err.println("ERROR: unable to start ffmpeg process, please make sure the specified path is correct");
            System.exit(2);
        } catch (Exception e) {
            System.err.println("ERROR: unable to convert input file, please make sure that it exists and is readable");
            System.exit(3);
        }

        // Try to convert and output the Ogg Vorbis data
        try {
            output(output, data);
        } catch (IOException e) {
            System.err.println("ERROR: unable to write output file, please make sure that the specified path is writable");
            System.exit(4);
        }
    }

    /**
     * Converts the input file into Ogg Vorbis and returns the output.
     *
     * @param input  the input file to convert
     * @param ffmpeg the ffmpeg executable
     * @return a byte array containing the output of the conversion
     * @throws IOException if ffmpeg could not be started
     * @throws Exception   if the conversion failed
     */
    public static byte[] convert(File input, File ffmpeg) throws IOException, Exception {

        Process process = new ProcessBuilder(ffmpeg.getPath(), "-i", input.getPath(), "-vn", "-f", "ogg", "-acodec", "libvorbis", "pipe:").start();

        final InputStream processInputStream = process.getInputStream();
        final ByteArrayOutputStream byteArrayOutputStream = new ByteArrayOutputStream();

        // Thread for reading pipe output from ffmpeg
        Thread inputThread = new Thread(new Runnable() {
            @Override
            public void run() {
                byte[] buffer = new byte[1024];
                int length;

                try {
                    while ((length = processInputStream.read(buffer)) != -1) {
                        byteArrayOutputStream.write(buffer, 0, length);
                    }
                } catch (IOException e) {
                    System.err.println("ERROR: unable to read from ffmpeg process");
                    System.exit(5);
                } finally {
                    try {
                        processInputStream.close();
                    } catch (IOException e) {
                    }
                }
            }
        });

        // Begin piping from ffmpeg into the buffer
        inputThread.start();

        try {
            int exitCode = process.waitFor();

            // If ffmpeg didn't exit with 0 then an error occurred or the input file didn't exist
            if (exitCode == 0)
                return byteArrayOutputStream.toByteArray();
            else
                throw new Exception("ffmpeg exited abnormally");
        } catch (InterruptedException e) {
            throw new Exception("ffmpeg process was interrupted");
        }
    }

    /**
     * Converts the specified byte array into a png image and outputs the result to the specified file.
     *
     * @param output the file to output the png to
     * @param data   the data to convert
     * @throws IOException if the output file could not be written
     */
    public static void output(File output, byte[] data) throws IOException {

        // Since PngWriter doesn't have error checking (or at least I don't think it does), ensure file is writable
        if (!output.canWrite() && !output.createNewFile()) {
            throw new IOException("unable to write output file");
        }

        // Each pixel can store four bytes, and one additional pixel is needed for the length
        int pixelsNeeded = (int) Math.ceil(data.length / 4) + 1;

        // We want to make square images
        int side = (int) Math.ceil(Math.sqrt(pixelsNeeded));

        ImageInfo imageInfo = new ImageInfo(side, side, 8, true);
        PngWriter pngWriter = new PngWriter(output, imageInfo);

        // Current data position
        int position = -1;

        ImageLineInt imageLine;
        int[] scanLine;

        for (int y = 0; y < side; y++) {
            imageLine = new ImageLineInt(imageInfo);
            scanLine = imageLine.getScanline();

            for (int x = 0; x < side; x++) {
                // Encode the length in the very first pixel
                if (position > -1) {
                    for (int i = 0; i < 4; i++) {
                        // Write black pixels for the remainder of the image if the data has been exhausted
                        scanLine[x * 4 + i] = position < data.length ? data[position++] : 0;
                    }
                } else {
                    for (int i = 0; i < 4; i++) {
                        scanLine[x * 4 + i] = data.length >> 24 - 8 * i;
                    }

                    position++;
                }
            }

            pngWriter.writeRow(imageLine);
        }

        // Finish up
        pngWriter.end();
    }

}
