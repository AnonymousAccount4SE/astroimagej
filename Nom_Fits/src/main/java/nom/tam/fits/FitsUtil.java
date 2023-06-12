package nom.tam.fits;

/*
 * #%L
 * nom.tam FITS library
 * %%
 * Copyright (C) 2004 - 2021 nom-tam-fits
 * %%
 * This is free and unencumbered software released into the public domain.
 *
 * Anyone is free to copy, modify, publish, use, compile, sell, or
 * distribute this software, either in source code form or as a compiled
 * binary, for any purpose, commercial or non-commercial, and by any
 * means.
 *
 * In jurisdictions that recognize copyright laws, the author or authors
 * of this software dedicate any and all copyright interest in the
 * software to the public domain. We make this dedication for the benefit
 * of the public at large and to the detriment of our heirs and
 * successors. We intend this dedication to be an overt act of
 * relinquishment in perpetuity of all present and future rights to this
 * software under copyright law.
 *
 * THE SOFTWARE IS PROVIDED "AS IS", WITHOUT WARRANTY OF ANY KIND,
 * EXPRESS OR IMPLIED, INCLUDING BUT NOT LIMITED TO THE WARRANTIES OF
 * MERCHANTABILITY, FITNESS FOR A PARTICULAR PURPOSE AND NONINFRINGEMENT.
 * IN NO EVENT SHALL THE AUTHORS BE LIABLE FOR ANY CLAIM, DAMAGES OR
 * OTHER LIABILITY, WHETHER IN AN ACTION OF CONTRACT, TORT OR OTHERWISE,
 * ARISING FROM, OUT OF OR IN CONNECTION WITH THE SOFTWARE OR THE USE OR
 * OTHER DEALINGS IN THE SOFTWARE.
 * #L%
 */

import nom.tam.util.*;

import java.io.Closeable;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.ProtocolException;
import java.net.URL;
import java.net.URLConnection;
import java.util.Arrays;
import java.util.logging.Level;
import java.util.logging.Logger;

/**
 * Static utility functions used throughout the FITS classes.
 */
public final class FitsUtil {

    private static final int BYTE_REPRESENTING_BLANK = 32;

    private static final int BYTE_REPRESENTING_MAX_ASCII_VALUE = 126;

    /**
     * the logger to log to.
     */
    private static final Logger LOG = Logger.getLogger(FitsUtil.class.getName());

    private static boolean wroteCheckingError = false;

    /**
     * Utility class, do not instantiate it.
     */
    private FitsUtil() {
    }

    /**
     * @deprecated      use {@link #addPadding(long)} instead. Calculates the amount of padding needed to complete the
     *                      last FITS block at the specified current size.
     * 
     * @return          Total size of blocked FITS element, using e.v. padding to fits block size.
     *
     * @param      size the current size.
     */
    public static int addPadding(int size) {
        return size + padding(size);
    }

    /**
     * Calculates the amount of padding needed to complete the last FITS block at the specified current size.
     * 
     * @return      Total size of blocked FITS element, using e.v. padding to fits block size.
     *
     * @param  size the current size.
     * 
     * @see         #padding(long)
     * @see         #pad(ArrayDataOutput, long)
     */
    public static long addPadding(long size) {
        return size + padding(size);
    }

    /**
     * Converts a boolean array to its FITS representation as an array of bytes.
     * 
     * @return      Convert an array of booleans to bytes.
     *
     * @param  bool array of booleans
     */
    static byte[] booleanToByte(boolean[] bool) {

        byte[] byt = new byte[bool.length];
        for (int i = 0; i < bool.length; i++) {
            byt[i] = FitsEncoder.byteForBoolean(bool[i]);
        }
        return byt;
    }

    /**
     * Converts a FITS byte sequence to a Java string array
     * 
     * @return        Convert bytes to Strings.
     *
     * @param  bytes  byte array to convert
     * @param  maxLen the max string length
     */
    public static String[] byteArrayToStrings(byte[] bytes, int maxLen) {
        boolean checking = FitsFactory.getCheckAsciiStrings();

        // Note that if a String in a binary table contains an internal 0,
        // the FITS standard says that it is to be considered as terminating
        // the string at that point, so that software reading the
        // data back may not include subsequent characters.
        // No warning of this truncation is given.

        String[] res = new String[bytes.length / maxLen];
        for (int i = 0; i < res.length; i++) {

            int start = i * maxLen;

            // AK: The FITS standard states that a 0 byte terminates the
            // string before the fixed length, and characters beyond it
            // are undefined. So, we first check where the string ends.
            int l = 0;
            for (; l < maxLen; l++) {
                if (bytes[l] == 0) {
                    break;
                }
            }
            int end = start + l;

            // Pre-trim the string to avoid keeping memory
            // hanging around. (Suggested by J.C. Segovia, ESA).

            // Note that the FITS standard does not mandate
            // that we should be trimming the string at all, but
            // this seems to best meet the desires of the community.
            for (; start < end; start++) {
                if (bytes[start] != BYTE_REPRESENTING_BLANK) {
                    break; // Skip only spaces.
                }
            }

            for (; end > start; end--) {
                if (bytes[end - 1] != BYTE_REPRESENTING_BLANK) {
                    break;
                }
            }

            // For FITS binary tables, 0 values are supposed
            // to terminate strings, a la C. [They shouldn't appear in
            // any other context.]
            // Other non-printing ASCII characters
            // should always be an error which we can check for
            // if the user requests.

            // The lack of handling of null bytes was noted by Laurent Bourges.
            boolean errFound = false;
            for (int j = start; j < end; j++) {

                if (bytes[j] == 0) {
                    end = j;
                    break;
                }
                if (checking && (bytes[j] < BYTE_REPRESENTING_BLANK || bytes[j] > BYTE_REPRESENTING_MAX_ASCII_VALUE)) {
                    errFound = true;
                    bytes[j] = BYTE_REPRESENTING_BLANK;
                }
            }
            res[i] = AsciiFuncs.asciiString(bytes, start, end - start);
            if (errFound && !FitsUtil.wroteCheckingError) {
                LOG.log(Level.SEVERE, "Warning: Invalid ASCII character[s] detected in string: " + res[i]
                        + " Converted to space[s].  Any subsequent invalid characters will be converted silently");
                FitsUtil.wroteCheckingError = true;
            }
        }
        return res;
    }

    /**
     * Converts a FITS representation of boolean values as bytes to a java boolean array. This implementation does not
     * handle FITS <code>null</code> values.
     * 
     * @return       Convert an array of bytes to booleans.
     *
     * @param  bytes the array of bytes to get the booleans from.
     * 
     * @see          FitsDecoder#booleanFor(int)
     */
    static boolean[] byteToBoolean(byte[] bytes) {
        boolean[] bool = new boolean[bytes.length];

        for (int i = 0; i < bytes.length; i++) {
            bool[i] = FitsDecoder.booleanFor(bytes[i]);
        }
        return bool;
    }

    /**
     * Gets the file offset for the given IO resource.
     * 
     * @return   The offset from the beginning of file (if random accessible), or -1 otherwise.
     *
     * @param  o the stream to get the position
     */
    public static long findOffset(Closeable o) {
        if (o instanceof RandomAccess) {
            return ((RandomAccess) o).getFilePointer();
        }
        return -1;
    }

    /**
     * Gets and input stream for a given URL resource.
     * 
     * @return             Get a stream to a URL accommodating possible redirections. Note that if a redirection request
     *                         points to a different protocol than the original request, then the redirection is not
     *                         handled automatically.
     *
     * @param  url         the url to get the stream from
     * @param  level       max levels of redirection
     *
     * @throws IOException if the operation failed
     */
    public static InputStream getURLStream(URL url, int level) throws IOException {
        URLConnection conn = null;
        int code = -1;
        try {
            conn = url.openConnection();
            if (conn instanceof HttpURLConnection) {
                code = ((HttpURLConnection) conn).getResponseCode();
            }
            return conn.getInputStream();
        } catch (ProtocolException e) {
            LOG.log(Level.WARNING, "could not connect to " + url + (code >= 0 ? " got responce-code" + code : ""), e);
            throw e;
        }
    }

    /**
     * Returns the maximum String length in an array of Strings.
     * 
     * @return               Get the maximum length of a String in a String array.
     *
     * @param  strings       array of strings to check
     *
     * @throws FitsException if the operation failed
     */
    public static int maxLength(String[] strings) throws FitsException {

        int max = 0;
        for (String element : strings) {
            if (element != null && element.length() > max) {
                max = element.length();
            }
        }
        return max;
    }

    /**
     * Adds the necessary amount of padding needed to complete the last FITS block.
     *
     * @param  stream        stream to pad
     * @param  size          the current size of the stream (total number of bytes written to it since the beginning of
     *                           the FITS).
     *
     * @throws FitsException if the operation failed
     * 
     * @see                  #pad(ArrayDataOutput, long, byte)
     */
    public static void pad(ArrayDataOutput stream, long size) throws FitsException {
        pad(stream, size, (byte) 0);
    }

    /**
     * Adds the necessary amount of padding needed to complete the last FITS block., usign the designated padding byte
     * value.
     *
     * @param  stream        stream to pad
     * @param  size          the current size of the stream (total number of bytes written to it since the beginning of
     *                           the FITS).
     * @param  fill          the byte value to use for the padding
     *
     * @throws FitsException if the operation failed
     * 
     * @see                  #pad(ArrayDataOutput, long)
     */
    public static void pad(ArrayDataOutput stream, long size, byte fill) throws FitsException {
        int len = padding(size);
        if (len > 0) {
            byte[] buf = new byte[len];
            Arrays.fill(buf, fill);
            try {
                stream.write(buf);
                stream.flush();
            } catch (Exception e) {
                throw new FitsException("Unable to write padding", e);
            }
        }
    }

    /**
     * @deprecated      see Use {@link #padding(long)} instead.
     * 
     * @return          How many bytes are needed to fill a 2880 block?
     *
     * @param      size the size without padding
     */
    public static int padding(int size) {
        return padding((long) size);
    }

    /**
     * Calculated the amount of padding we need to add given the current size of a FITS file (under construction)
     * 
     * @param  size the current size of our FITS file before the padding
     * 
     * @return      the number of bytes of padding we need to add at the end to complete the FITS block.
     * 
     * @see         #addPadding(long)
     * @see         #pad(ArrayDataOutput, long)
     */
    public static int padding(long size) {

        int mod = (int) (size % FitsFactory.FITS_BLOCK_SIZE);
        if (mod > 0) {
            mod = FitsFactory.FITS_BLOCK_SIZE - mod;
        }
        return mod;
    }

    /**
     * Attempts to reposition a FITS input ot output. The call will succeed only if the underlying input or output is
     * random accessible. Othewise, an exception will be thrown.
     *
     * @deprecated               This method wraps an {@link IOException} into a {@link FitsException} for no good
     *                               reason really. A revision of the API could reduce the visibility of this method,
     *                               and/or procees the underlying exception instead.
     *
     * @param      o             the FITS input or output
     * @param      offset        the offset to position it to.
     *
     * @throws     FitsException if the underlying input/output is not random accessible or if the requested position is
     *                               invalid.
     */
    @Deprecated
    public static void reposition(FitsIO o, long offset) throws FitsException {
        // TODO AK: argument should be RandomAccess instead of Closeable, since
        // that's the only type we actually handle...

        if (o == null) {
            throw new FitsException("Attempt to reposition null stream");
        }

        if (!(o instanceof RandomAccess) || offset < 0) {
            throw new FitsException(
                    "Invalid attempt to reposition stream " + o + " of type " + o.getClass().getName() + " to " + offset);
        }

        try {
            ((RandomAccess) o).seek(offset);
        } catch (IOException e) {
            throw new FitsException("Unable to repostion stream " + o + " of type " + o.getClass().getName() + " to "
                    + offset + ": " + e.getMessage(), e);
        }
    }

    /**
     * Convert an array of Strings to bytes.
     *
     * @return             the resulting bytes
     *
     * @param  stringArray the array with Strings
     * @param  maxLen      the max length (in bytes) of every String
     */
    public static byte[] stringsToByteArray(String[] stringArray, int maxLen) {
        byte[] res = new byte[stringArray.length * maxLen];
        for (int i = 0; i < stringArray.length; i++) {
            byte[] bstr;
            if (stringArray[i] == null) {
                bstr = new byte[0];
            } else {
                bstr = AsciiFuncs.getBytes(stringArray[i]);
            }
            int cnt = bstr.length;
            if (cnt > maxLen) {
                cnt = maxLen;
            }
            System.arraycopy(bstr, 0, res, i * maxLen, cnt);
            for (int j = cnt; j < maxLen; j++) {
                res[i * maxLen + j] = (byte) ' ';
            }
        }
        return res;
    }
}
