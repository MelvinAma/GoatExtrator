# GoatExtrator
A program that accesses Gmail using the IMAP protocol to download shipping labels and commercial invoices sent by Goat.
This can be used for mass-printing shipping labels or bookkeeping purposes.

Create a Gmail filter that adds all mails containing "Shipping label and instructions" in the subject to a folder called "labels" (can be changed). This ensures only mails from Goat will be read.


IMPORTANT NOTES:
 * IMAP must be enabled: 
   
	https://support.google.com/mail/answer/7126229?hl=en
 * If you are unable to login using your normal Gmail password you need to create an app password:
 
   https://support.google.com/accounts/answer/185833?hl=en
