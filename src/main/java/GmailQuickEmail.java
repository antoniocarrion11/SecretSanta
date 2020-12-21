// Copyright 2018 Google LLC
//
// Licensed under the Apache License, Version 2.0 (the "License");
// you may not use this file except in compliance with the License.
// You may obtain a copy of the License at
//
//     http://www.apache.org/licenses/LICENSE-2.0
//
// Unless required by applicable law or agreed to in writing, software
// distributed under the License is distributed on an "AS IS" BASIS,
// WITHOUT WARRANTIES OR CONDITIONS OF ANY KIND, either express or implied.
// See the License for the specific language governing permissions and
// limitations under the License.

// [START gmail_quickstart]
import com.google.api.client.auth.oauth2.Credential;
import com.google.api.client.extensions.java6.auth.oauth2.AuthorizationCodeInstalledApp;
import com.google.api.client.extensions.jetty.auth.oauth2.LocalServerReceiver;
import com.google.api.client.googleapis.auth.oauth2.GoogleAuthorizationCodeFlow;
import com.google.api.client.googleapis.auth.oauth2.GoogleClientSecrets;
import com.google.api.client.googleapis.javanet.GoogleNetHttpTransport;
import com.google.api.client.http.javanet.NetHttpTransport;
import com.google.api.client.json.JsonFactory;
import com.google.api.client.json.jackson2.JacksonFactory;
import com.google.api.client.util.Base64;
import com.google.api.client.util.store.FileDataStoreFactory;
import com.google.api.services.gmail.Gmail;
import com.google.api.services.gmail.GmailScopes;
import com.google.api.services.gmail.model.Label;
import com.google.api.services.gmail.model.ListLabelsResponse;
import com.google.api.services.gmail.model.Message;

import javax.mail.MessagingException;
import javax.mail.Session;
import javax.mail.internet.InternetAddress;
import javax.mail.internet.MimeMessage;
import java.io.*;
import java.security.GeneralSecurityException;
import java.util.*;

public class GmailQuickEmail {
    private static final String APPLICATION_NAME = "Secret Santa Gmail API Java Quickstart";
    private static final JsonFactory JSON_FACTORY = JacksonFactory.getDefaultInstance();
    private static final String TOKENS_DIRECTORY_PATH = "tokens";
    private static final String SECRET_SANTA_BODY = "MERRY CHRISTMAS %s!!! Here is your pick for Secret Santa \n" +
            "\n" +
            "NAME: %s\n" +
            "\n" +
            "ADDRESS: %s\n" +
            "\n" +
            "EMAIL: %s\n" +
            " \n" +
            "\n" +
            "This year is a secret Santa meaning that you are responsible for " +
            "getting a present(s) for whoever was chosen for you. There are no limitations on the price of the present, " +
            "but only spend what you can afford. We are hoping that by assigning one person that the present can be a " +
            "little more thoughtful and a lot lighter on your bank account.\n" +
            "\n" +
            " \n" +
            "\n" +
            "DELIVERING PRESENTS:\n" +
            "\n" +
            "Getting your present(s) to your person can be a difficult task without spoiling the surprise. " +
            "Please use the home or email address provided in this email to be able to drop off/ship directly to. " +
            "If you are using amazon they have a gift wrapping option that disguises who the recipient is. " +
            "If this does not suffice, please contact Jesus to help with a workaround.\n" +
            "\n"+
            "OPENING UP THE PRESENT:\n" +
            "\n" +
            "In the case that we do not get together to trade presents we are having a recorded reaction video. " +
            "Meaning that on Christmas day we will grab our phones/cameras and record ourselves opening up the present(s). " +
            "All of us will then post it onto discord, in order for the secret Santa to be able to see the reaction. " +
            "Please try to show off the present to the camera before and after opening.\n" +
            "\n" +
            "*You can upload to youtube or other video hosting sites if needed. Just post the link whenever is convenient to you.\n" +
            "\n" +
            "Thanks everyone, and if you have any questions please ask Jesus for any assistance.\n" +
            "\n" +
            "Happy Holidays btw.";

    /**
     * Global instance of the scopes required by this quickstart.
     * If modifying these scopes, delete your previously saved tokens/ folder.
     */
    private static final List<String> SCOPES = Arrays.asList(GmailScopes.GMAIL_LABELS, GmailScopes.GMAIL_SEND);
    private static final String CREDENTIALS_FILE_PATH = "/credentials.json";

    /**
     * Creates an authorized Credential object.
     * @param HTTP_TRANSPORT The network HTTP Transport.
     * @return An authorized Credential object.
     * @throws IOException If the credentials.json file cannot be found.
     */
    private static Credential getCredentials(final NetHttpTransport HTTP_TRANSPORT) throws IOException {
        // Load client secrets.
        InputStream in = GmailQuickEmail.class.getResourceAsStream(CREDENTIALS_FILE_PATH);
        if (in == null) {
            throw new FileNotFoundException("Resource not found: " + CREDENTIALS_FILE_PATH);
        }
        GoogleClientSecrets clientSecrets = GoogleClientSecrets.load(JSON_FACTORY, new InputStreamReader(in));

        // Build flow and trigger user authorization request.
        GoogleAuthorizationCodeFlow flow = new GoogleAuthorizationCodeFlow.Builder(
                HTTP_TRANSPORT, JSON_FACTORY, clientSecrets, SCOPES)
                .setDataStoreFactory(new FileDataStoreFactory(new java.io.File(TOKENS_DIRECTORY_PATH)))
                .setAccessType("offline")
                .build();
        LocalServerReceiver receiver = new LocalServerReceiver.Builder().setPort(8080).build();
        return new AuthorizationCodeInstalledApp(flow, receiver).authorize("user");
    }
    /**
     * Create a MimeMessage using the parameters provided.
     *
     * @param to email address of the receiver
     * @param from email address of the sender, the mailbox account
     * @param subject subject of the email
     * @param bodyText body text of the email
     * @return the MimeMessage to be used to send email
     * @throws MessagingException
     */
    public static MimeMessage createEmail(String to,
                                          String from,
                                          String subject,
                                          String bodyText)
            throws MessagingException {
        Properties props = new Properties();
        Session session = Session.getDefaultInstance(props, null);

        MimeMessage email = new MimeMessage(session);

        email.setFrom(new InternetAddress(from));
        email.addRecipient(javax.mail.Message.RecipientType.TO, new InternetAddress(to));
        email.setSubject(subject);
        email.setText(bodyText);
        return email;
    }

    /**
     * Create a message from an email.
     *
     * @param emailContent Email to be set to raw of message
     * @return a message containing a base64url encoded email
     * @throws IOException
     * @throws MessagingException
     */
    public static Message createMessageWithEmail(MimeMessage emailContent)
            throws MessagingException, IOException {
        ByteArrayOutputStream buffer = new ByteArrayOutputStream();
        emailContent.writeTo(buffer);
        byte[] bytes = buffer.toByteArray();
        String encodedEmail = Base64.encodeBase64URLSafeString(bytes);
        Message message = new Message();
        message.setRaw(encodedEmail);
        return message;
    }

    /**
     * Send an email from the user's mailbox to its recipient.
     *
     * @param service Authorized Gmail API instance.
     * @param userId User's email address. The special value "me"
     * can be used to indicate the authenticated user.
     * @param emailContent Email to be sent.
     * @return The sent message
     * @throws MessagingException
     * @throws IOException
     */
    public static Message sendMessage(Gmail service,
                                      String userId,
                                      MimeMessage emailContent)
            throws MessagingException, IOException {
        Message message = createMessageWithEmail(emailContent);
        Gmail.Users.Messages.Send send = service.users().messages().send(userId, message);
        send.execute();
        System.out.println("Message id: " + message.getId());
        System.out.println(message.toPrettyString());
        return message;
    }

    public static void main(String... args) throws IOException, GeneralSecurityException {
        // Build a new authorized API client service.
        final NetHttpTransport HTTP_TRANSPORT = GoogleNetHttpTransport.newTrustedTransport();
        Gmail service = new Gmail.Builder(HTTP_TRANSPORT, JSON_FACTORY, getCredentials(HTTP_TRANSPORT))
                .setApplicationName(APPLICATION_NAME)
                .build();
        try {
        SecretSanta santaStation = new SecretSanta();
        LinkedHashMap<SecretSantaParticipant, SecretSantaParticipant> foundParty = santaStation.findRecepients();

        // Print the labels in the user's account.
        String user = "me";
        ListLabelsResponse listResponse = service.users().labels().list(user).execute();
        List<Label> labels = listResponse.getLabels();


             for(Map.Entry<SecretSantaParticipant, SecretSantaParticipant> participant : foundParty.entrySet()) {
                 MimeMessage welcomeEmail = createEmail(
                         participant.getKey().getEmail(),
                         "MrTacoTommy@gmail.com",
                         "WELCOME to Secret Santa UwU",
                         String.format(SECRET_SANTA_BODY, participant.getKey().getName(),
                                 participant.getValue().getName(),
                                 participant.getValue().getAddress(),
                                 participant.getValue().getEmail()));
                 Message sentEmail = sendMessage(service, user, welcomeEmail);
                 System.out.println(sentEmail);
             }
             
        } catch (MessagingException | IOException e) {
            e.printStackTrace();
        }
    }
}
// [END gmail_quickstart]