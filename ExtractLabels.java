import java.io.FileOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.net.HttpURLConnection;
import java.net.URL;

public class ExtractLabels {
    /**
     * Handles URL redirects caused by AWS
     * Reference: https://stackoverflow.com/questions/29443632/how-to-follow-redirected-url-in-java
     *
     * @param url, URL from body text
     * @return finalRedirectedUrl, PDF URL
     */
    public static String getFinalRedirectedUrl(String url) {
        String finalRedirectedUrl = url;
        try {
            HttpURLConnection connection;
            do {
                connection = (HttpURLConnection) new URL(finalRedirectedUrl).openConnection();
                connection.setInstanceFollowRedirects(false);
                connection.setUseCaches(false);
                connection.setRequestMethod("GET");
                connection.connect();
                int responseCode = connection.getResponseCode();
                if (responseCode >= 300 && responseCode < 400) {
                    String redirectedUrl = connection.getHeaderField("Location");
                    if (null == redirectedUrl) {
                        break;
                    }
                    finalRedirectedUrl = redirectedUrl;
                } else
                    break;
            } while (connection.getResponseCode() != HttpURLConnection.HTTP_OK);
            connection.disconnect();
        } catch (Exception e) {
            e.printStackTrace();
        }
        return finalRedirectedUrl;
    }

    /**
     * Downloads PDF to the matching folder
     * Reference: https://stackoverflow.com/questions/20265740/how-to-download-a-pdf-from-a-given-url-in-java
     *
     * @param url,  PDF URL
     * @param type, "Label" or "Invoice"
     */
    public static void DownloadPDFs(String filePath, String url, String orderNumber, String type) throws IOException {
        URL pdfUrl = new URL(url);
        try {
            InputStream in = pdfUrl.openStream();
            FileOutputStream fos = new FileOutputStream
                    (String.format(filePath + "%s-%s.pdf", orderNumber, type));
            int length;
            // buffer for portion of data from connection
            byte[] buffer = new byte[1024];
            while ((length = in.read(buffer)) > -1) {
                fos.write(buffer, 0, length);
            }
            fos.close();
            in.close();
        } catch (IOException ex) {
            // Url has expired, PDF can't be accessed
            System.out.println("URL EXPIRED FOR ORDER " + orderNumber);
        }
    }
}
