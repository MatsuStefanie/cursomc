package com.cristal.stefanie.cursomc.services;

import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.context.annotation.Profile;
import org.springframework.mail.MailSender;
import org.springframework.mail.SimpleMailMessage;
import org.springframework.mail.javamail.JavaMailSender;

import javax.mail.internet.MimeMessage;

public class SmtpEmailService extends AbstractEmailService {

    private static final Logger LOG = LoggerFactory.getLogger(MockEmailService.class);

    @Autowired
    public MailSender mailSender;

    @Autowired
    public JavaMailSender javaMailSender;

    @Override
    public void sendHtmlEmail(MimeMessage msg) {
        LOG.info("Enviando email...");
        javaMailSender.send(msg);
        LOG.info("Email enviado!!!");
    }

    @Override
    public void sendEmail(SimpleMailMessage msg) {
        LOG.info("Enviando email...");
        mailSender.send(msg);
        LOG.info("Email enviado!!!");
    }
}
