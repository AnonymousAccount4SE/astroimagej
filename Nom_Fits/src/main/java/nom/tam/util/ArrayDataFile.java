package nom.tam.util;

import java.io.EOFException;
import java.io.File;
import java.io.IOException;

/**
 * Efficient reading and writing of arrays to and from files, with custom
 * encoding. Compared to its superclass, it add only the translation layer for
 * encoding and decoding binary data to convert between Java arrays and their
 * binary representation in file.
 * 
 * @author Attila Kovacs
 * @since 1.16
 * @see ArrayInputStream
 * @see ArrayOutputStream
 */
public class ArrayDataFile extends BufferedFileIO {

    /** conversion from Java arrays to FITS binary representation */
    private OutputEncoder encoder;

    /** conversion from FITS binary representation to Java arrays */
    private InputDecoder decoder;

    /**
     * Instantiates a new file for high-performance array IO operations. For use
     * by subclass constructors only
     * 
     * @param f
     *            the file
     * @param mode
     *            the access mode, such as "rw" (see
     *            {@link java.io.RandomAccessFile} for more info).
     * @param bufferSize
     *            the size of the buffer in bytes
     * @throws IOException
     *             if there was an IO error getting the required access to the
     *             file.
     */
    protected ArrayDataFile(File f, String mode, int bufferSize) throws IOException {
        super(f, mode, bufferSize);
    }

    /**
     * Instantiates a new file for high-performance array IO operations. For use
     * by subclass constructors only
     * 
     * @param f
     *            the RandomAccessFileIO file
     * @param bufferSize
     *            the size of the buffer in bytes
     */
    protected ArrayDataFile(RandomAccessFileIO f, int bufferSize) {
        super(f, bufferSize);
    }

    /**
     * Sets the conversion from Java arrays to their binary representation in
     * file. For use by subclass constructors only.
     * 
     * @param java2bin
     *            the conversion from Java arrays to their binary representation
     *            in file
     * @see #getEncoder()
     * @see #setDecoder(InputDecoder)
     */
    protected void setEncoder(OutputEncoder java2bin) {
        encoder = java2bin;
    }

    /**
     * Returns the conversion from Java arrays to their binary representation in
     * file. Subclass implementations can use this to access the required
     * conversion when writing data to file.
     * 
     * @return the conversion from Java arrays to their binary representation in
     *         file
     * @see #setEncoder(OutputEncoder)
     * @see #getDecoder()
     */
    public OutputEncoder getEncoder() {
        return encoder;
    }

    /**
     * Sets the conversion from the binary representation of arrays in file to
     * Java arrays. For use by subclass constructors only.
     * 
     * @param bin2java
     *            the conversion from the binary representation of arrays in the
     *            file to Java arrays.
     * @see #getDecoder()
     * @see #setEncoder(OutputEncoder)
     */
    protected void setDecoder(InputDecoder bin2java) {
        decoder = bin2java;
    }

    /**
     * Returns the conversion from the binary representation of arrays in file
     * to Java arrays. Subclass implementeations can use this to access the
     * required conversion when writing data to file.
     * 
     * @return the conversion from the binary representation of arrays in the
     *         file to Java arrays
     * @see #setDecoder(InputDecoder)
     * @see #getEncoder()
     */
    public InputDecoder getDecoder() {
        return decoder;
    }

    /**
     * See {@link ArrayDataInput#readLArray(Object)} for a contract of this
     * method.
     * 
     * @param o
     *            an array, to be populated
     * @return the actual number of bytes read from the input, or -1 if already
     *         at the end-of-file.
     * @throws IllegalArgumentException
     *             if the argument is not an array or if it contains an element
     *             that is not supported for decoding.
     * @throws IOException
     *             if there was an IO error reading from the input
     * @see #readArrayFully(Object)
     * @see #readImage(Object)
     */
    public synchronized long readLArray(Object o) throws IOException, IllegalArgumentException {
        return decoder.readArray(o);
    }

    /**
     * See {@link ArrayDataInput#readArrayFully(Object)} for a contract of this
     * method.
     * 
     * @param o
     *            an array, to be populated
     * @throws IllegalArgumentException
     *             if the argument is not an array or if it contains an element
     *             that is not supported for decoding.
     * @throws IOException
     *             if there was an IO error reading from the input
     * @see #readLArray(Object)
     * @see #readImage(Object)
     */
    public synchronized void readArrayFully(Object o) throws IOException, IllegalArgumentException {
        decoder.readArrayFully(o);
    }

    /**
     * Like {@link #readArrayFully(Object)} but strictly for numerical types
     * only.
     * 
     * @param o
     *            An any-dimensional array containing only numerical types
     * @throws IllegalArgumentException
     *             if the argument is not an array or if it contains an element
     *             that is not supported.
     * @throws EOFException
     *             if already at the end of file.
     * @throws IOException
     *             if there was an IO error
     * @see #readArrayFully(Object)
     * @since 1.18
     */
    public void readImage(Object o) throws EOFException, IOException, IllegalArgumentException {
        decoder.readImage(o);
    }

    /**
     * See {@link ArrayDataOutput#writeArray(Object)} for a contract of this
     * method.
     * 
     * @param o
     *            an array ot any type.
     * @throws IllegalArgumentException
     *             if the argument is not an array or if it contains an element
     *             that is not supported for encoding.
     * @throws IOException
     *             if there was an IO error writing to the output.
     */
    public synchronized void writeArray(Object o) throws IOException, IllegalArgumentException {
        try {
            getEncoder().writeArray(o);
        } catch (IllegalArgumentException e) {
            throw new IOException(e);
        }
    }
}
