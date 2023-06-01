package nom.tam.fits;

import nom.tam.fits.header.Bitpix;
import nom.tam.util.ArrayDataInput;
import nom.tam.util.ArrayDataOutput;

import static nom.tam.fits.header.Standard.*;

/**
 * A subclass of <code>Data</code> containing no actual Data. It wraps an underlying data of <code>null</code>.
 * 
 * @author Attila Kovacs
 * 
 * @since 1.18
 */
public final class NullData extends Data {

    @Override
    protected void fillHeader(Header head) {
        head.setSimple(true);
        head.setBitpix(Bitpix.INTEGER);
        head.setNaxes(0);

        try {
            // Just in case!
            head.addValue(EXTEND, true);
            head.addValue(GCOUNT, 1);
            head.addValue(PCOUNT, 0);
        } catch (HeaderCardException e) {
            // we don't really care...
        }
    }

    @Override
    protected void loadData(ArrayDataInput in) {
        return;
    }

    @Override
    protected Void getCurrentData() {
        return null;
    }

    @Override
    protected long getTrueSize() {
        return 0;
    }

    @Override
    public void read(ArrayDataInput in) {
    }

    @Override
    public void write(ArrayDataOutput o) {
    }

}