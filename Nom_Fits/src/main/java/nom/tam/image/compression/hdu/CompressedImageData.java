package nom.tam.image.compression.hdu;

import nom.tam.fits.BinaryTable;
import nom.tam.fits.FitsException;
import nom.tam.fits.Header;
import nom.tam.fits.HeaderCard;
import nom.tam.fits.compression.algorithm.api.ICompressOption;
import nom.tam.fits.header.Compression;
import nom.tam.image.compression.tile.TiledImageCompressionOperation;
import nom.tam.util.ArrayFuncs;

import java.nio.Buffer;

import static nom.tam.fits.header.Compression.ZIMAGE;

/**
 * FITS representation of a compressed image. Compressed images are essentially stored as FITS binary tables. They are
 * distinguished only by a set of mandatory header keywords and specific column data structure. The image-specific
 * header keywords of the original image are re-mapped to keywords starting with 'Z' prefixed so as to not interfere
 * with the binary table description. (e.g. NAXIS1 of the original image is stored as ZNAXIS1 in the compressed table).
 * 
 * @see CompressedImageHDU
 */
public class CompressedImageData extends BinaryTable {

    /**
     * tile information, only available during compressing or decompressing.
     */
    private TiledImageCompressionOperation tiledImageOperation;

    /**
     * Creates a new empty compressed image data to be initialized at a later point
     */
    protected CompressedImageData() {
        super();
    }

    /**
     * Creates a new compressed image data based on the prescription of the supplied header.
     * 
     * @param  hdr           The header that describes the compressed image
     * 
     * @throws FitsException If the header is invalid or could not be accessed.
     */
    protected CompressedImageData(Header hdr) throws FitsException {
        super(hdr);
    }

    @Override
    public void fillHeader(Header h) throws FitsException {
        super.fillHeader(h);
        h.addValue(ZIMAGE, true);
    }

    /**
     * Returns the class that handles the (de)compression of the image tiles.
     * 
     * @return the class handling the (de)compression of the image tiles.
     */
    private TiledImageCompressionOperation tiledImageOperation() {
        if (tiledImageOperation == null) {
            tiledImageOperation = new TiledImageCompressionOperation(this);
        }
        return tiledImageOperation;
    }

    /**
     * This should only be called by {@link CompressedImageHDU}.
     */
    @SuppressWarnings("javadoc")
    protected void compress(CompressedImageHDU hdu) throws FitsException {
        tiledImageOperation().compress(hdu);
    }

    /**
     * This should only be called buy{@link CompressedImageHDU}.
     */
    @SuppressWarnings("javadoc")
    protected void forceNoLoss(int x, int y, int width, int heigth) {
        tiledImageOperation.forceNoLoss(x, y, width, heigth);
    }

    /**
     * Returns the compression (or quantization) options for a selected compression option class. It is presumed that
     * the requested options are appropriate for the compression and/or quantization algorithm that was selected. E.g.,
     * if you called <code>setCompressionAlgorithm({@link Compression#ZCMPTYPE_RICE_1})</code>, then you can retrieve
     * options for it with this method as
     * <code>getCompressOption({@link nom.tam.fits.compression.algorithm.rice.RiceCompressOption}.class)</code>.
     * 
     * @param  <T>   The generic type of the compression class
     * @param  clazz the compression class
     * 
     * @return       The current set of options for the requested type, or <code>null</code> if there are no options or
     *                   if the requested type does not match the algorithm(s) selected.
     * 
     * @see          nom.tam.fits.compression.algorithm.hcompress.HCompressorOption
     * @see          nom.tam.fits.compression.algorithm.rice.RiceCompressOption
     * @see          nom.tam.fits.compression.algorithm.quant.QuantizeOption
     */
    protected <T extends ICompressOption> T getCompressOption(Class<T> clazz) {
        return tiledImageOperation().compressOptions().unwrap(clazz);
    }

    /**
     * This should only be called by {@link CompressedImageHDU}.
     */
    @SuppressWarnings("javadoc")
    protected Buffer getUncompressedData(Header hdr) throws FitsException {
        try {
            tiledImageOperation = new TiledImageCompressionOperation(this).read(hdr);
            return tiledImageOperation.decompress();
        } finally {
            tiledImageOperation = null;
        }
    }

    /**
     * This should only be called by {@link CompressedImageHDU}.
     */
    @SuppressWarnings("javadoc")
    protected void prepareUncompressedData(Object data, Header header) throws FitsException {
        tiledImageOperation().readPrimaryHeaders(header);
        Buffer source = tiledImageOperation().getBaseType().newBuffer(tiledImageOperation.getBufferSize());
        ArrayFuncs.copyInto(data, source.array());
        tiledImageOperation().prepareUncompressedData(source);
    }

    /**
     * preserve the null values in the image even if the compression algorithm is lossy.
     *
     * @param nullValue            the value representing null for byte/short and integer pixel values
     * @param compressionAlgorithm compression algorithm to use for the null pixel mask
     */
    protected void preserveNulls(long nullValue, String compressionAlgorithm) {
        tiledImageOperation().preserveNulls(nullValue, compressionAlgorithm);
    }

    /**
     * This should only be called by {@link CompressedImageHDU}.
     */
    @SuppressWarnings("javadoc")
    protected CompressedImageData setAxis(int[] axes) {
        tiledImageOperation().setAxes(axes);
        return this;
    }

    /**
     * Sets the compression algorithm that was used to generate the HDU.
     * 
     * @param compressAlgorithmCard the FITS header card that specifies the compression algorithm that was used.
     * 
     * @see                         #setQuantAlgorithm(HeaderCard)
     */
    protected void setCompressAlgorithm(HeaderCard compressAlgorithmCard) {
        tiledImageOperation().setCompressAlgorithm(compressAlgorithmCard);
    }

    /**
     * Sets the quantization algorithm that was used to generate the HDU.
     * 
     * @param  quantAlgorithmCard the FITS header card that specifies the quantization algorithm that was used.
     * 
     * @throws FitsException      if no algorithm is available by the specified name
     * 
     * @see                       #setCompressAlgorithm(HeaderCard)
     */
    protected void setQuantAlgorithm(HeaderCard quantAlgorithmCard) throws FitsException {
        tiledImageOperation().setQuantAlgorithm(quantAlgorithmCard);
    }

    /**
     * This should only be called by {@link CompressedImageHDU}.
     */
    @SuppressWarnings("javadoc")
    protected CompressedImageData setTileSize(int... axes) throws FitsException {
        tiledImageOperation().setTileAxes(axes);
        return this;
    }
}
