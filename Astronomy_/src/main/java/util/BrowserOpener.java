package util;

import ij.IJ;

import java.io.IOException;
import java.net.URL;

/**
 * Class to facilitate in the opening of webpages in the system's default browser.
 *
 * @author Kevin Eastridge
 */
public class BrowserOpener {

    public static void openURL(URL url) throws IOException {
        openURL(url.toString());
    }

    public static void openURL(String url) throws IOException {
        Runtime rt = Runtime.getRuntime();
        String[] command;
        if (IJ.isWindows()) {
            command = new String[]{"rundll32", "url.dll,FileProtocolHandler", url};
        } else if (IJ.isMacOSX()) {
            command = new String[]{"open", url};
        } else if (IJ.isLinux()) {
            command = new String[]{"xdg-open", url};
        } else {
            IJ.log("Could not determine OS to open a URL in, URL: " + url);
            command = new String[]{"start", "url"}; // final option at opening the link
        }

        rt.exec(command);

    }

}
