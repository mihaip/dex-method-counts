/*
 * Copyright (C) 2009 The Android Open Source Project
 *
 * Licensed under the Apache License, Version 2.0 (the "License");
 * you may not use this file except in compliance with the License.
 * You may obtain a copy of the License at
 *
 *      http://www.apache.org/licenses/LICENSE-2.0
 *
 * Unless required by applicable law or agreed to in writing, software
 * distributed under the License is distributed on an "AS IS" BASIS,
 * WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
 * See the License for the specific language governing permissions and
 * limitations under the License.
 */

package com.android.dexdeps;

import java.io.File;
import java.io.FileNotFoundException;
import java.io.IOException;
import java.io.InputStream;
import java.io.RandomAccessFile;
import java.util.zip.ZipEntry;
import java.util.zip.ZipException;
import java.util.zip.ZipFile;
import java.util.zip.ZipInputStream;

public class Main {
    private static final String CLASSES_DEX = "classes.dex";

    private String[] mInputFileNames;
    private String mOutputFormat = "xml";

    /**
     * whether to only emit info about classes used; when {@code false},
     * info about fields and methods is also emitted
     */
    private boolean mJustClasses = false;

    /**
     * Entry point.
     */
    public static void main(String[] args) {
        Main main = new Main();
        main.run(args);
    }

    /**
     * Start things up.
     */
    void run(String[] args) {
        try {
            parseArgs(args);
            boolean first = true;

            for (String fileName : mInputFileNames) {
                RandomAccessFile raf = openInputFile(fileName);
                DexData dexData = new DexData(raf);
                dexData.load();

                if (first) {
                    first = false;
                    Output.generateFirstHeader(fileName, mOutputFormat);
                } else {
                    Output.generateHeader(fileName, mOutputFormat);
                }

                Output.generate(dexData, mOutputFormat, mJustClasses);
                Output.generateFooter(mOutputFormat);
                raf.close();
            }
        } catch (UsageException ue) {
            usage();
            System.exit(2);
        } catch (IOException ioe) {
            if (ioe.getMessage() != null) {
                System.err.println("Failed: " + ioe);
            }
            System.exit(1);
        } catch (DexDataException dde) {
            /* a message was already reported, just bail quietly */
            System.exit(1);
        }
    }

    /**
     * Opens an input file, which could be a .dex or a .jar/.apk with a
     * classes.dex inside.  If the latter, we extract the contents to a
     * temporary file.
     *
     * @param fileName the name of the file to open
     */
    RandomAccessFile openInputFile(String fileName) throws IOException {
        RandomAccessFile raf;

        raf = openInputFileAsZip(fileName);
        if (raf == null) {
            File inputFile = new File(fileName);
            raf = new RandomAccessFile(inputFile, "r");
        }

        return raf;
    }

    /**
     * Tries to open an input file as a Zip archive (jar/apk) with a
     * "classes.dex" inside.
     *
     * @param fileName the name of the file to open
     * @return a RandomAccessFile for classes.dex, or null if the input file
     *         is not a zip archive
     * @throws IOException if the file isn't found, or it's a zip and
     *         classes.dex isn't found inside
     */
    RandomAccessFile openInputFileAsZip(String fileName) throws IOException {
        ZipFile zipFile;

        /*
         * Try it as a zip file.
         */
        try {
            zipFile = new ZipFile(fileName);
        } catch (FileNotFoundException fnfe) {
            /* not found, no point in retrying as non-zip */
            System.err.println("Unable to open '" + fileName + "': " +
                fnfe.getMessage());
            throw fnfe;
        } catch (ZipException ze) {
            /* not a zip */
            return null;
        }

        /*
         * We know it's a zip; see if there's anything useful inside.  A
         * failure here results in some type of IOException (of which
         * ZipException is a subclass).
         */
        ZipEntry entry = zipFile.getEntry(CLASSES_DEX);
        if (entry == null) {
            System.err.println("Unable to find '" + CLASSES_DEX +
                "' in '" + fileName + "'");
            zipFile.close();
            throw new ZipException();
        }

        InputStream zis = zipFile.getInputStream(entry);

        /*
         * Create a temp file to hold the DEX data, open it, and delete it
         * to ensure it doesn't hang around if we fail.
         */
        File tempFile = File.createTempFile("dexdeps", ".dex");
        //System.out.println("+++ using temp " + tempFile);
        RandomAccessFile raf = new RandomAccessFile(tempFile, "rw");
        tempFile.delete();

        /*
         * Copy all data from input stream to output file.
         */
        byte copyBuf[] = new byte[32768];
        int actual;

        while (true) {
            actual = zis.read(copyBuf);
            if (actual == -1)
                break;

            raf.write(copyBuf, 0, actual);
        }

        zis.close();
        raf.seek(0);

        return raf;
    }


    /**
     * Parses command-line arguments.
     *
     * @throws UsageException if arguments are missing or poorly formed
     */
    void parseArgs(String[] args) {
        int idx;

        for (idx = 0; idx < args.length; idx++) {
            String arg = args[idx];

            if (arg.equals("--") || !arg.startsWith("--")) {
                break;
            } else if (arg.startsWith("--format=")) {
                mOutputFormat = arg.substring(arg.indexOf('=') + 1);
                if (!mOutputFormat.equals("brief") &&
                    !mOutputFormat.equals("xml"))
                {
                    System.err.println("Unknown format '" + mOutputFormat +"'");
                    throw new UsageException();
                }
                //System.out.println("+++ using format " + mOutputFormat);
            } else if (arg.equals("--just-classes")) {
                mJustClasses = true;
            } else {
                System.err.println("Unknown option '" + arg + "'");
                throw new UsageException();
            }
        }

        // We expect at least one more argument (file name).
        int fileCount = args.length - idx;
        if (fileCount == 0) {
            throw new UsageException();
        }

        mInputFileNames = new String[fileCount];
        System.arraycopy(args, idx, mInputFileNames, 0, fileCount);
    }

    /**
     * Prints command-line usage info.
     */
    void usage() {
        System.err.print(
                "DEX dependency scanner v1.2\n" +
                "Copyright (C) 2009 The Android Open Source Project\n\n" +
                "Usage: dexdeps [options] <file.{dex,apk,jar}> ...\n" +
                "Options:\n" +
                "  --format={xml,brief}\n" +
                "  --just-classes\n");
    }
}
