package com.mov.config;

import com.mov.model.*;
import com.mov.model.enums.*;
import com.mov.repository.*;
import com.mov.service.ArgumentoService;
import com.mov.service.FileStorageService;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.springframework.boot.CommandLineRunner;
import org.springframework.security.crypto.password.PasswordEncoder;
import org.springframework.stereotype.Component;
import org.springframework.transaction.annotation.Transactional;

import java.nio.charset.StandardCharsets;
import java.time.LocalDateTime;
import java.util.HashSet;
import java.util.Set;

@Component
@RequiredArgsConstructor
@Slf4j
public class DataInitializer implements CommandLineRunner {

    private final UsuarioRepository usuarioRepository;
    private final SalaDebateRepository salaDebateRepository;
    private final GrupoRepository grupoRepository;
    private final DocumentoRepository documentoRepository;
    private final ArgumentoRegistroRepository argumentoRepository;
    private final ArgumentoService argumentoService;
    private final FileStorageService fileStorageService;
    private final PasswordEncoder passwordEncoder;

    @Override
    @Transactional
    public void run(String... args) throws Exception {
        // 1. Perfis de Utilização (Seção 4)
        Usuario admin = garantirUsuario("Coordenação M.O.V. (Admin)", "admin@mov.com", "Senac@2026", TipoUsuario.ADMIN, "M.O.V. / Senac SP");
        Usuario prof = garantirUsuario("Equipe Organização M.O.V.", "organizacao@mov.com", "Senac@2026", TipoUsuario.PROFESSOR, "M.O.V. / Senac SP");
        Usuario mesa = garantirUsuario("Mesa Diretora ONU", "mesa@mov.com", "Senac@2026", TipoUsuario.MESA_DIRETORA, "Senac SP");
        garantirUsuario("Equipe de Imprensa", "imprensa@mov.com", "Senac@2026", TipoUsuario.IMPRENSA, "Senac SP");
        garantirUsuario("Checagem de Informações (Fact-Checking)", "checagem@mov.com", "Senac@2026", TipoUsuario.CHECAGEM, "Senac SP");

        // Estudantes / Delegações (Seção 3.1 e 4.1)
        Usuario delBrasil1 = garantirUsuario("Ana Beatriz (Delegação Brasil)", "brasil1@senac.edu.br", "Senac@2026", TipoUsuario.DELEGADO, "Senac SP");
        Usuario delBrasil2 = garantirUsuario("Lucas Mendes (Delegação Brasil)", "brasil2@senac.edu.br", "Senac@2026", TipoUsuario.DELEGADO, "Senac SP");
        Usuario delEua = garantirUsuario("Gabriel Costa (Delegação EUA)", "eua1@senac.edu.br", "Senac@2026", TipoUsuario.DELEGADO, "Senac SP");
        Usuario delChina = garantirUsuario("Mariana Silva (Delegação China)", "china1@senac.edu.br", "Senac@2026", TipoUsuario.DELEGADO, "Senac SP");
        Usuario delFranca = garantirUsuario("Pedro Oliveira (Delegação França)", "franca1@senac.edu.br", "Senac@2026", TipoUsuario.DELEGADO, "Senac SP");

        // 2. Sala de Simulação da ONU
        SalaDebate salaOnu;
        if (salaDebateRepository.count() == 0) {
            salaOnu = SalaDebate.builder()
                    .titulo("Assembleia Geral da ONU — Governança Digital, IA e Sustentabilidade")
                    .categoria("Simulação Presencial ONU • Piloto Senac SP")
                    .descricao("Ambiente complementar de organização documental, construção de argumentos, fontes e contrapontos para o debate presencial.")
                    .regras("1. Toda afirmação deve ser acompanhada de justificativa e fonte verificável.\n2. O uso da plataforma é complementar à condução presencial da Mesa Diretora.\n3. Respeite o isolamento e as permissões de cada delegação.")
                    .status(StatusSala.EM_ANDAMENTO)
                    .criadoPor(admin)
                    .dataCriacao(LocalDateTime.now())
                    .build();
            salaOnu = salaDebateRepository.save(salaOnu);
        } else {
            salaOnu = salaDebateRepository.findAll().get(0);
        }

        // 3. Criação dos Grupos de Delegações previstos na Especificação (Seção 3.1)
        Grupo grpBrasil = garantirGrupo(
                "Grupo — Delegação do Brasil",
                "Brasil",
                "BRA",
                "Espaço oficial da Delegação do Brasil para armazenamento de documentos de posição e construção conjunta de argumentos e fontes.",
                salaOnu,
                admin,
                Set.of(delBrasil1, delBrasil2)
        );

        Grupo grpEua = garantirGrupo(
                "Grupo — Delegação dos Estados Unidos",
                "Estados Unidos",
                "USA",
                "Espaço oficial da Delegação dos Estados Unidos para preparação de posicionamentos, referências e contrapontos.",
                salaOnu,
                admin,
                Set.of(delEua)
        );

        Grupo grpChina = garantirGrupo(
                "Grupo — Delegação da China",
                "China",
                "CHN",
                "Espaço oficial da Delegação da China para articulação de propostas, fontes e documentos diplomáticos.",
                salaOnu,
                admin,
                Set.of(delChina)
        );

        Grupo grpFranca = garantirGrupo(
                "Grupo — Delegação da França",
                "França",
                "FRA",
                "Espaço oficial da Delegação da França para organização de dossiês, diretrizes europeias e argumentação.",
                salaOnu,
                admin,
                Set.of(delFranca)
        );

        garantirGrupo(
                "Grupo — Delegação do Reino Unido",
                "Reino Unido",
                "GBR",
                "Espaço oficial da Delegação do Reino Unido para preparação de posicionamentos, documentos e argumentos.",
                salaOnu,
                admin,
                Set.of()
        );

        garantirGrupo(
                "Grupo — Delegação da Alemanha",
                "Alemanha",
                "DEU",
                "Espaço oficial da Delegação da Alemanha para organização documental e desenvolvimento de argumentos e fontes.",
                salaOnu,
                admin,
                Set.of()
        );

        garantirGrupo(
                "Grupo — Delegação do Japão",
                "Japão",
                "JPN",
                "Espaço oficial da Delegação do Japão para registro de argumentos, referências técnicas e contrapontos.",
                salaOnu,
                admin,
                Set.of()
        );

        garantirGrupo(
                "Grupo — Delegação da Índia",
                "Índia",
                "IND",
                "Espaço oficial da Delegação da Índia para articulação diplomática, documentos e argumentos compartilhados.",
                salaOnu,
                admin,
                Set.of()
        );

        garantirGrupo(
                "Grupo — Delegação da África do Sul",
                "África do Sul",
                "ZAF",
                "Espaço oficial da Delegação da África do Sul para construção de posicionamentos, fontes e contrapontos.",
                salaOnu,
                admin,
                Set.of()
        );

        // 4. Documentos Iniciais (Gerais da Simulação e Específicos de Delegação - Seção 3.2)
        if (documentoRepository.count() == 0) {
            criarDocumentoPdfInicial(
                    "Guia Oficial de Estudos e Regras de Procedimento — Simulação ONU Senac SP",
                    "Documento oficial elaborado pela organização contendo o regulamento da Mesa Diretora, cronograma das sessões e contextualização do tema.",
                    TipoDocumento.OFICIAL,
                    "Guia_Oficial_Simulacao_ONU_Senac.pdf",
                    null,
                    true,
                    admin
            );

            criarDocumentoPdfInicial(
                    "Relatório UNESCO — Governança de IA e Integridade da Informação",
                    "Material de referência geral disponibilizado para consulta de todas as delegações, Mesa Diretora e equipe de checagem.",
                    TipoDocumento.REFERENCIA,
                    "Relatorio_UNESCO_Governanca_Digital.pdf",
                    null,
                    true,
                    prof
            );

            criarDocumentoPdfInicial(
                    "Documento de Posição Oficial — Delegação do Brasil",
                    "Posicionamento diplomático exclusivo da Delegação do Brasil sobre soberania digital, transferência tecnológica e inclusão educacional.",
                    TipoDocumento.POSICAO,
                    "Posicao_Delegacao_Brasil.pdf",
                    grpBrasil,
                    false,
                    delBrasil1
            );

            criarDocumentoPdfInicial(
                    "Dossiê de Pesquisa — Delegação dos Estados Unidos",
                    "Levantamento interno de dados econômicos e marcos regulatórios de inovação da Delegação dos EUA.",
                    TipoDocumento.PESQUISA,
                    "Dossie_Delegacao_EUA.pdf",
                    grpEua,
                    false,
                    delEua
            );
        }

        // 5. Registros Iniciais no Arquivo Compartilhado de Argumentos e Fontes (Seção 3.3)
        if (argumentoRepository.count() == 0) {
            argumentoService.registrarArgumento(
                    grpBrasil.getId(),
                    "Brasil",
                    "A governança internacional de Inteligência Artificial deve incluir mecanismos de transferência tecnológica e financiamento para países em desenvolvimento.",
                    "Sem cooperação técnica multilateral, a concentração de infraestrutura computacional ampliará a desigualdade educacional e econômica entre o Norte e o Sul Global.",
                    "Resolução ONU A/78/L.49 e Relatório UNESCO sobre IA no Sul Global (2025)",
                    "https://www.unesco.org/en/artificial-intelligence",
                    "Algumas delegações argumentam que exigências de transferência podem desestimular investimentos privados, mas o Brasil defende parcerias voluntárias com incentivos públicos.",
                    true,
                    delBrasil1
            );

            argumentoService.registrarArgumento(
                    grpFranca.getId(),
                    "França",
                    "Defendemos um marco regulatório baseado em níveis de risco e transparência algorítmica obrigatória para sistemas de alto impacto educacional e social.",
                    "A auditoria independente de modelos fundacionais previne a desinformação em massa e protege direitos fundamentais dos cidadãos e estudantes.",
                    "Marco Europeu de Inteligência Artificial (EU AI Act) - Parlamento Europeu",
                    "https://artificialintelligenceact.eu/",
                    "Responder às preocupações sobre excesso de burocracia propondo sandboxes regulatórios para instituições de ensino e pesquisa.",
                    true,
                    delFranca
            );
        }

        log.info("Ambiente do Projeto-Piloto M.O.V. (Simulação ONU - Senac SP) inicializado e pronto!");
    }

    private Usuario garantirUsuario(String nome, String email, String senhaPura, TipoUsuario tipo, String instituicao) {
        return usuarioRepository.findByEmail(email).orElseGet(() -> {
            Usuario u = Usuario.builder()
                    .nome(nome)
                    .email(email)
                    .senha(passwordEncoder.encode(senhaPura))
                    .tipo(tipo)
                    .instituicao(instituicao)
                    .ativo(true)
                    .nivel(tipo == TipoUsuario.ADMIN ? 99 : 1)
                    .consensosAlcancados(0)
                    .debatesParticipados(0)
                    .tentativasFalhasLogin(0)
                    .build();
            return usuarioRepository.save(u);
        });
    }

    private Grupo garantirGrupo(String nome, String pais, String sigla, String descricao, SalaDebate sala, Usuario criador, Set<Usuario> membros) {
        return grupoRepository.findByNomeIgnoreCase(nome).orElseGet(() -> {
            Grupo g = Grupo.builder()
                    .nome(nome)
                    .paisDelegacao(pais)
                    .sigla(sigla)
                    .descricao(descricao)
                    .textoColaborativo("# Documento de Posição — Delegação: " + pais + "\n\n" +
                            "1. Posicionamento Central:\n" +
                            "- Descreva aqui as diretrizes principais defendidas pela delegação.\n\n" +
                            "2. Propostas de Resolução:\n" +
                            "- Liste os pontos de consenso e cláusulas operatórias para o debate presencial.")
                    .ultimoEditorTextoNome(criador.getNome())
                    .dataAtualizacaoTexto(LocalDateTime.now())
                    .salaDebate(sala)
                    .criadoPor(criador)
                    .integrantes(new HashSet<>(membros))
                    .ativo(true)
                    .dataCriacao(LocalDateTime.now())
                    .build();
            return grupoRepository.save(g);
        });
    }

    private void criarDocumentoPdfInicial(String titulo, String descricao, TipoDocumento tipo, String nomeOriginal, Grupo grupo, boolean publicoGeral, Usuario autor) {
        byte[] pdfBytes = gerarPdfMinimoValido(titulo, descricao);
        String nomeSalvo = fileStorageService.storeBytes(pdfBytes, nomeOriginal);
        Documento doc = Documento.builder()
                .titulo(titulo)
                .descricao(descricao)
                .tipoDocumento(tipo)
                .nomeOriginal(nomeOriginal)
                .nomeArmazenado(nomeSalvo)
                .contentType("application/pdf")
                .tamanhoBytes((long) pdfBytes.length)
                .publicoGeral(publicoGeral)
                .somenteLeitura(publicoGeral)
                .grupo(grupo)
                .enviadoPor(autor)
                .totalConsultas(1)
                .dataEnvio(LocalDateTime.now())
                .build();
        documentoRepository.save(doc);
    }

    private byte[] gerarPdfMinimoValido(String titulo, String subtitulo) {
        String textoLimpo = (titulo + " - " + subtitulo)
                .replace("(", "")
                .replace(")", "")
                .replaceAll("[^\\x20-\\x7E]", " ");
        if (textoLimpo.length() > 90) {
            textoLimpo = textoLimpo.substring(0, 90);
        }
        String pdf = "%PDF-1.4\n" +
                "1 0 obj << /Type /Catalog /Pages 2 0 R >> endobj\n" +
                "2 0 obj << /Type /Pages /Kids [3 0 R] /Count 1 >> endobj\n" +
                "3 0 obj << /Type /Page /Parent 2 0 R /Resources << /Font << /F1 4 0 R >> >> /MediaBox [0 0 612 792] /Contents 5 0 R >> endobj\n" +
                "4 0 obj << /Type /Font /Subtype /Type1 /BaseFont /Helvetica >> endobj\n" +
                "5 0 obj << /Length 120 >> stream\n" +
                "BT /F1 12 Tf 50 720 Td (Plataforma M.O.V. - Simulacao Presencial da ONU - Senac SP) Tj 0 -25 Td (" + textoLimpo + ") Tj ET\n" +
                "endstream endobj\n" +
                "xref\n0 6\n0000000000 65535 f \n0000000010 00000 n \n0000000060 00000 n \n0000000117 00000 n \n0000000240 00000 n \n0000000308 00000 n \n" +
                "trailer << /Size 6 /Root 1 0 R >>\nstartxref\n480\n%%EOF";
        return pdf.getBytes(StandardCharsets.US_ASCII);
    }
}
