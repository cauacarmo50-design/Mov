package com.mov.service;

import jakarta.mail.MessagingException;
import lombok.extern.slf4j.Slf4j;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.beans.factory.annotation.Value;
import org.springframework.mail.javamail.JavaMailSender;
import org.springframework.mail.javamail.MimeMessageHelper;
import org.springframework.stereotype.Service;

@Service
@Slf4j
public class EmailService {

    @Autowired(required = false)
    private JavaMailSender mailSender;

    @Value("${spring.mail.username:noreply@mov.com}")
    private String remetente;

    public void enviarEmailAtivacao(String destinatario, String nome, String linkAtivacao) {
        if (mailSender == null) {
            log.info("JavaMailSender não configurado; envio de e-mail ignorado para {}", destinatario);
            return;
        }
        try {
            var mensagem = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mensagem, true, "UTF-8");

            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject("M.O.V - Ative sua conta");

            var htmlMsg = """
                    <div style='font-family: Arial, sans-serif; padding: 20px;'>
                        <h2>Olá, %s!</h2>
                        <p>Obrigado por se cadastrar na Plataforma M.O.V.</p>
                        <p>Para ativar sua conta, clique no link abaixo. Ele expira em 5 minutos.</p>
                        <a href='%s' style='background-color: #007bff; color: white; padding: 10px 20px; text-decoration: none; border-radius: 5px;'>Ativar Conta</a>
                        <p>Se o botão não funcionar, copie e cole este link no navegador:</p>
                        <p>%s</p>
                    </div>
                    """.formatted(nome, linkAtivacao, linkAtivacao);

            helper.setText(htmlMsg, true);

            mailSender.send(mensagem);
            log.info("E-mail de ativação enviado para {}", destinatario);

        } catch (MessagingException e) {
            log.error("Erro ao enviar e-mail de ativação para {}: {}", destinatario, e.getMessage());
        }
    }

    public void enviarAlertaNovoDispositivo(String destinatario, String nome, String ipAddress, String userAgent) {
        if (mailSender == null) {
            return;
        }
        try {
            var mensagem = mailSender.createMimeMessage();
            var helper = new MimeMessageHelper(mensagem, true, "UTF-8");

            helper.setFrom(remetente);
            helper.setTo(destinatario);
            helper.setSubject("M.O.V - Alerta de Novo Acesso Detectado");

            var htmlMsg = """
                    <div style='font-family: Arial, sans-serif; padding: 20px; border-left: 4px solid #ef4444; background-color: #fef2f2;'>
                        <h2>Aviso de Segurança</h2>
                        <p>Olá, %s,</p>
                        <p>Detectamos um novo acesso à sua conta M.O.V. a partir de um dispositivo não reconhecido.</p>
                        <ul>
                            <li><strong>Endereço IP:</strong> %s</li>
                            <li><strong>Navegador/Dispositivo:</strong> %s</li>
                        </ul>
                        <p><strong>Se não foi você, altere sua senha imediatamente!</strong></p>
                    </div>
                    """.formatted(nome, ipAddress, userAgent);

            helper.setText(htmlMsg, true);

            mailSender.send(mensagem);
            log.warn("Alerta de novo dispositivo enviado para {}", destinatario);

        } catch (MessagingException e) {
            log.error("Erro ao enviar alerta de segurança para {}: {}", destinatario, e.getMessage());
        }
    }
}
