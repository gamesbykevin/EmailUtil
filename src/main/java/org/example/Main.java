package org.example;

import jakarta.mail.*;
import jakarta.mail.internet.MimeMessage;

import java.io.File;
import java.io.FileInputStream;
import java.io.FileOutputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.nio.file.StandardCopyOption;
import java.time.LocalDateTime;
import java.time.format.DateTimeFormatter;
import java.util.Calendar;
import java.util.Properties;

//TIP To <b>Run</b> code, press <shortcut actionId="Run"/> or
// click the <icon src="AllIcons.Actions.Execute"/> icon in the gutter.
public class Main {

    public static final String EMAIL_DIR = "C:\\Users\\kevin\\OneDrive\\Desktop\\emails\\";

    public static void main(String[] args) {

        try {

            String[] folders = {"inbox"};

            for (String emailFolder : folders) {
                downloadFolder(emailFolder, false);
                Main.organize(emailFolder + "/");
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }

    public static void downloadFolder(String emailFolder, boolean yahoo) throws Exception {

        String host = yahoo ? "imap.mail.yahoo.com" : "imap.googlemail.com";
        String username = yahoo ? "@yahoo.com" : "@gmail.com";
        String password = yahoo ? "" : "";

        Properties props = new Properties();
        props.put("mail.store.protocol", "imaps");

        Session session = Session.getDefaultInstance(props, null);
        Store store = session.getStore("imaps");
        store.connect(host, username, password);

        String parentDir = EMAIL_DIR + emailFolder + "/";

        System.out.println("Folder = " + parentDir);

        File directory = new File(parentDir);
        directory.mkdirs();

        Folder inbox = store.getFolder(emailFolder);
        inbox.open(Folder.READ_ONLY);

        Message[] messages = inbox.getMessages();
        for (int i = 0; i < messages.length; i++) {

            if (i > 0 && i % 5 == 0) {
                System.out.println("closing and reconnecting to folder");
                inbox.close();
                store.close();
                store.connect(host, username, password);
                inbox = store.getFolder(emailFolder);
                inbox.open(Folder.READ_ONLY);
                messages = inbox.getMessages();
            }

            LocalDateTime now = LocalDateTime.now();
            DateTimeFormatter formatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");
            String formattedDate = now.format(formatter);
            System.out.println(i + " of " + messages.length + " " + formattedDate);

            Message message = messages[i];
            String subject = message.getSubject();
            subject = (subject != null) ? subject.replaceAll("[^a-zA-Z0-9\\.\\-]", "_") : "email_" + i;

            if (subject.length() >= 125)
                subject = subject.substring(0, 125);

            subject += "-" + Calendar.getInstance().getTimeInMillis();

            try (OutputStream out = new FileOutputStream(parentDir + subject + ".eml")) {
                message.writeTo(out);
                System.out.println("Saved: " + subject + ".eml");
            }

            Thread.sleep(500);
        }

        inbox.close(false);
        store.close();
    }

    public static void organize(String folder) {

        try {

            final String parentPath = EMAIL_DIR + folder;
            File[] files = new File(parentPath).listFiles();

            boolean success = true;

            //make sure have valid date
            for (File file : files) {
                try {
                    MimeMessage msg = new MimeMessage(null, new FileInputStream(file));
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(msg.getSentDate());
                } catch (Exception ex) {
                    System.out.println(file);
                    success = false;
                }
            }

            if (success) {

                //move to destination folder based on when email was sent
                for (File file : files) {

                    MimeMessage msg = new MimeMessage(null, new FileInputStream(file));
                    Calendar calendar = Calendar.getInstance();
                    calendar.setTime(msg.getSentDate());

                    String day = calendar.get(Calendar.DAY_OF_MONTH) + "";
                    String month = calendar.get(Calendar.MONTH) < 9 ? "0" + (calendar.get(Calendar.MONTH) + 1) : "" + (calendar.get(Calendar.MONTH) + 1);
                    String year = calendar.get(Calendar.YEAR) + "";

                    String destination = EMAIL_DIR + year + "\\" + month + "\\" + day + "\\";
                    File directory = new File(destination);
                    directory.mkdirs();

                    Path sourcePath = Paths.get(file.getPath()); // Replace with your source file path
                    Path targetPath = Paths.get(destination + file.getName()); // Replace with your destination path and new file name
                    Files.copy(sourcePath, targetPath, StandardCopyOption.REPLACE_EXISTING);

                    System.out.println("File moved successfully!");
                }
            }

        } catch (Exception e) {
            e.printStackTrace();
        }
    }
}