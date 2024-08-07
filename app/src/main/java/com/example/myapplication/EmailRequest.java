package com.example.myapplication;

import java.util.List;

public class EmailRequest {
    private Sender sender;
    private List<Recipient> to;
    private String subject;
    private String htmlContent;

    public EmailRequest(Sender sender, List<Recipient> to, String subject, String htmlContent) {
        this.sender = sender;
        this.to = to;
        this.subject = subject;
        this.htmlContent = htmlContent;
    }

    public static class Sender {
        private String name;
        private String email;

        public Sender(String name, String email) {
            this.name = name;
            this.email = email;
        }
    }

    public static class Recipient {
        private String email;

        public Recipient(String email) {
            this.email = email;
        }
    }
}
