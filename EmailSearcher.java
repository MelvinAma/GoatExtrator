import org.jsoup.Jsoup;
import org.jsoup.nodes.Document;

import javax.mail.*;
import javax.mail.search.SearchTerm;
import java.io.IOException;
import java.util.List;
import java.util.Properties;
import java.util.regex.Pattern;

/**
 * A program that accesses Gmail using the IMAP protocol to download
 * shipping labels and commercial invoices sent by Goat
 * <p>
 * IMPORTANT NOTES:
 * IMAP must be enabled:
 * https://support.google.com/mail/answer/7126229?hl=en
 * If you are unable to login using your normal Gmail password you need to create an app password:
 * https://support.google.com/accounts/answer/185833?hl=en
 *
 * @author Melvin (HBB#7625)
 */
public class EmailSearcher {
    int y = 1;

    public static void main(String[] args) {
        // https://support.google.com/mail/answer/7126229?hl=en
        String filePath = "/Desired/Download/Path/Here/";
        String mailFolder = "labels"; // Change as needed
        String host = "imap.gmail.com";
        String port = "993";
        String userName = "yourMail@gmail.com";
        String password = "yourPassword";
        EmailSearcher searcher = new EmailSearcher();
        String keyword = "Shipping Label and Instructions";
        searcher.searchEmail(filePath, mailFolder, host, port, userName, password, keyword);
    }

    /**
     * Fetches the mails that include the keyword(s) in the Subject
     * and then finds the order number and the URL that redirects to the label PDF
     * Reference: https://www.codejava.net/java-ee/javamail/using-javamail-for-searching-e-mail-messages
     */
    public void searchEmail(
            String filePath, String mailFolder, String host,
            String port, String userName, String password, String keyword) {

        Properties properties = new Properties();

        // Server setting
        properties.put("mail.imap.host", host);
        properties.put("mail.imap.port", port);

        // SSL setting
        properties.setProperty("mail.imap.socketFactory.class", "javax.net.ssl.SSLSocketFactory");
        properties.setProperty("mail.imap.socketFactory.fallback", "false");
        properties.setProperty("mail.imap.socketFactory.port", String.valueOf(port));

        Session session = Session.getDefaultInstance(properties);

        try {
            // Connects to the message store
            Store store = session.getStore("imap");
            store.connect(userName, password);

            // Opens the dedicated label folder
            Folder folder = store.getFolder(mailFolder);
            folder.open(Folder.READ_ONLY);
            System.out.println("Search results: " + folder.getMessageCount());

            // Creates a search condition
            SearchTerm searchCondition = new SearchTerm() {
                @Override
                public boolean match(Message message) {
                    try {
                        if (message.getSubject().contains(keyword)) {
                            // Order number
                            String orderNumber = message.getSubject().substring(7, 16);

                            // Fetch body text as HTML
                            Object content = message.getContent();
                            if (content instanceof Multipart) {
                                Multipart mp = (Multipart) content;
                                for (int i = 0; i < mp.getCount(); i++) {
                                    BodyPart bp = mp.getBodyPart(i);
                                    if (Pattern.compile(Pattern.quote("text/html"),
                                            Pattern.CASE_INSENSITIVE).matcher(bp.getContentType()).find()) {
                                        // Found HTML part
                                        String htmlTable = bp.getContent().toString();
                                        Document doc = Jsoup.parse(htmlTable);

                                        // Get all URL's in the mail
                                        List<String> urls = doc.getElementsByTag("a").eachAttr("href");

                                        // Index 0 is always the label URL
                                        String labelUrl = urls.get(0);


                                        // Logs progress
                                        System.out.println("Exported " + y + "/" + folder.getMessageCount());
                                        y++;

                                        ExtractLabels.DownloadPDFs(
                                                filePath,
                                                ExtractLabels.getFinalRedirectedUrl(labelUrl),
                                                orderNumber,
                                                "Label");

                                        if (htmlTable.contains("DHL Service Point")) {
                                            // Mail is in old format (commercial invoice)
                                            String invoiceUrl = urls.get(1);
                                            ExtractLabels.DownloadPDFs(
                                                    filePath,
                                                    ExtractLabels.getFinalRedirectedUrl(invoiceUrl),
                                                    orderNumber,
                                                    "Invoice");
                                        }
                                    }
                                }
                            }
                            return true;
                        }
                    } catch (MessagingException | IOException ex) {
                        ex.printStackTrace();
                    }
                    return false;
                }
            };

            // Performs search
            folder.search(searchCondition);

            // Disconnect
            folder.close(false);
            store.close();
        } catch (NoSuchProviderException ex) {
            System.out.println("No provider");
            ex.printStackTrace();
        } catch (MessagingException ex) {
            System.out.println("Could not connect to the message store");
            ex.printStackTrace();
        }
    }
}
