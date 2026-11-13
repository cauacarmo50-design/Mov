package com.mov;

import com.mov.model.*;
import com.mov.model.enums.MomentoSimulacao;
import com.mov.model.enums.TipoDocumento;
import com.mov.model.enums.TipoUsuario;
import com.mov.repository.ArgumentoRegistroRepository;
import com.mov.repository.GrupoRepository;
import com.mov.repository.UsuarioRepository;
import com.mov.service.*;
import org.junit.jupiter.api.DisplayName;
import org.junit.jupiter.api.Test;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.autoconfigure.web.servlet.AutoConfigureMockMvc;
import org.springframework.boot.test.context.SpringBootTest;
import org.springframework.mock.web.MockMultipartFile;
import org.springframework.security.test.context.support.WithUserDetails;
import org.springframework.test.web.servlet.MockMvc;

import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.concurrent.*;
import java.util.concurrent.atomic.AtomicInteger;

import static org.assertj.core.api.Assertions.assertThat;
import static org.assertj.core.api.Assertions.assertThatThrownBy;
import static org.springframework.security.test.web.servlet.request.SecurityMockMvcRequestPostProcessors.csrf;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.get;
import static org.springframework.test.web.servlet.request.MockMvcRequestBuilders.post;
import static org.springframework.test.web.servlet.result.MockMvcResultMatchers.status;

@SpringBootTest
@AutoConfigureMockMvc
class PilotoOnuIntegrationAndLoadTest {

    @Autowired
    private MockMvc mockMvc;

    @Autowired
    private UsuarioRepository usuarioRepository;

    @Autowired
    private GrupoRepository grupoRepository;

    @Autowired
    private ArgumentoRegistroRepository argumentoRepository;

    @Autowired
    private GrupoService grupoService;

    @Autowired
    private DocumentoService documentoService;

    @Autowired
    private ArgumentoService argumentoService;

    @Autowired
    private TelemetriaService telemetriaService;

    @Autowired
    private RelatorioExportService relatorioExportService;

    @Autowired
    private UsuarioService usuarioService;

    @Test
    @DisplayName("1. Verificação de Grupos, Integrantes e Isolamento de Segurança entre Delegações (Seções 3.1 e 9)")
    void deveGarantirCriacaoGerenciamentoEIsolamentoEntreGrupos() {
        Usuario admin = usuarioRepository.findByEmail("admin@mov.com").orElseThrow();
        Usuario delBrasil1 = usuarioRepository.findByEmail("brasil1@senac.edu.br").orElseThrow();
        Usuario delEua = usuarioRepository.findByEmail("eua1@senac.edu.br").orElseThrow();

        Grupo grpBrasil = grupoRepository.findByNomeIgnoreCase("Grupo — Delegação do Brasil").orElseThrow();
        Grupo grpEua = grupoRepository.findByNomeIgnoreCase("Grupo — Delegação dos Estados Unidos").orElseThrow();

        // Delegado do Brasil acessa seu próprio grupo normalmente
        Grupo acessado = grupoService.buscarPorIdComPermissao(grpBrasil.getId(), delBrasil1);
        assertThat(acessado.getPaisDelegacao()).isEqualTo("Brasil");
        assertThat(acessado.getIntegrantes()).extracting(Usuario::getEmail).contains("brasil1@senac.edu.br", "brasil2@senac.edu.br");

        // Delegado do Brasil NÃO pode acessar o grupo restrito dos Estados Unidos
        assertThatThrownBy(() -> grupoService.buscarPorIdComPermissao(grpEua.getId(), delBrasil1))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("Acesso negado");

        // Equipe de organização pode criar novo grupo e adicionar participante
        Grupo grpJapao = grupoService.criarGrupo("Grupo — Delegação do Japão", "Japão", "JPN", "Delegação do Japão", null, admin);
        Usuario novoAluno = usuarioService.criarOuAtualizarParticipanteOrganizacao("Kenji Sato", "japao1@senac.edu.br", "Senac@2026", TipoUsuario.DELEGADO, "Senac SP");
        grupoService.adicionarIntegrante(grpJapao.getId(), novoAluno.getId(), admin);

        Grupo grpJapaoAtualizado = grupoService.buscarPorIdComPermissao(grpJapao.getId(), novoAluno);
        assertThat(grpJapaoAtualizado.contemIntegrante(novoAluno.getId())).isTrue();
    }

    @Test
    @DisplayName("2. Inserção, Visualização, Download e Proteção de Documentos por Grupo (Seção 3.2)")
    void devePermitirInsercaoVisualizacaoEBloquearAcessoIndevidoADocumentos() {
        Usuario delBrasil = usuarioRepository.findByEmail("brasil1@senac.edu.br").orElseThrow();
        Usuario delChina = usuarioRepository.findByEmail("china1@senac.edu.br").orElseThrow();
        Grupo grpBrasil = grupoRepository.findByNomeIgnoreCase("Grupo — Delegação do Brasil").orElseThrow();

        byte[] pdfContent = "%PDF-1.4\n1 0 obj <<>> endobj\ntrailer <<>>\n%%EOF".getBytes(StandardCharsets.US_ASCII);
        MockMultipartFile file = new MockMultipartFile("arquivo", "Proposta_Brasil.pdf", "application/pdf", pdfContent);

        Documento doc = documentoService.enviarDocumento(
                file,
                "Proposta Diplomática Interna — Brasil",
                "Material estratégico exclusivo da delegação brasileira",
                TipoDocumento.POSICAO,
                grpBrasil.getId(),
                false,
                delBrasil
        );

        assertThat(doc.getId()).isNotNull();

        // Integrante da mesma delegação consegue abrir/baixar
        Documento consultado = documentoService.obterDocumentoParaAcesso(doc.getId(), delBrasil, false);
        assertThat(consultado.getTotalConsultas()).isEqualTo(1);
        assertThat(documentoService.carregarRecursoArquivo(consultado).exists()).isTrue();

        // Integrante de outra delegação (China) é bloqueado ao tentar abrir documento restrito do Brasil
        assertThatThrownBy(() -> documentoService.obterDocumentoParaAcesso(doc.getId(), delChina, true))
                .isInstanceOf(SecurityException.class)
                .hasMessageContaining("permissão");
    }

    @Test
    @DisplayName("3. Arquivo Compartilhado de Argumentos, Fontes, Contrapontos, Autoria e Recuperação de Versão (Seção 3.3)")
    void deveGerenciarArgumentosFontesContrapontosERecuperarVersoes() {
        Usuario delBrasil1 = usuarioRepository.findByEmail("brasil1@senac.edu.br").orElseThrow();
        Usuario delBrasil2 = usuarioRepository.findByEmail("brasil2@senac.edu.br").orElseThrow();
        Grupo grpBrasil = grupoRepository.findByNomeIgnoreCase("Grupo — Delegação do Brasil").orElseThrow();

        // 1. Inserção inicial pelo integrante 1 (v1)
        ArgumentoRegistro v1 = argumentoService.registrarArgumento(
                grpBrasil.getId(),
                "Brasil",
                "Argumento Inicial v1 sobre Clima e IA",
                "Justificativa inicial fundamentada em dados energéticos",
                "Relatório IPCC 2025",
                "https://ipcc.ch",
                "",
                true,
                delBrasil1
        );
        assertThat(v1.getNumeroVersao()).isEqualTo(1);
        assertThat(v1.getAutor().getEmail()).isEqualTo("brasil1@senac.edu.br");

        // 2. Edição colaborativa pelo integrante 2 da mesma delegação (v2)
        ArgumentoRegistro v2 = argumentoService.editarArgumento(
                v1.getId(),
                "Brasil",
                "Argumento Aprimorado v2 sobre Clima e IA Verde",
                "Justificativa expandida com matriz energética limpa brasileira",
                "Relatório IPCC 2025 + ANEEL",
                "https://ipcc.ch",
                "Contraponto preventivo adicionado",
                true,
                delBrasil2
        );
        assertThat(v2.getNumeroVersao()).isEqualTo(2);
        assertThat(v2.getUltimoEditor().getEmail()).isEqualTo("brasil2@senac.edu.br");

        // 3. Registro de contraponto (v3)
        ArgumentoRegistro v3 = argumentoService.registrarContraponto(
                v1.getId(),
                "Contraponto formal apresentado em sessão plenária",
                "Estados Unidos",
                delBrasil1
        );
        assertThat(v3.getNumeroVersao()).isEqualTo(3);
        assertThat(v3.getContraponto()).contains("Contraponto formal");

        // 4. Consulta de histórico e restauração da versão 1 (v4)
        List<HistoricoArgumento> historico = argumentoService.listarHistoricoDoArgumento(v1.getId(), delBrasil1);
        assertThat(historico).hasSizeGreaterThanOrEqualTo(3);
        HistoricoArgumento snapshotV1 = historico.stream()
                .filter(h -> h.getNumeroVersao() == 1)
                .findFirst()
                .orElseThrow();

        ArgumentoRegistro restaurado = argumentoService.restaurarVersao(snapshotV1.getId(), delBrasil1);
        assertThat(restaurado.getNumeroVersao()).isEqualTo(4);
        assertThat(restaurado.getArgumento()).isEqualTo("Argumento Inicial v1 sobre Clima e IA");
    }

    @Test
    @DisplayName("4. Teste de Carga Simultâneo — Múltiplas Delegações e Estudantes Concorrentes sem Perda de Dados (Seção 8)")
    void deveSuportarCargaSimultaneaSemPerdaDeContribuicoes() throws Exception {
        Usuario delBrasil1 = usuarioRepository.findByEmail("brasil1@senac.edu.br").orElseThrow();
        Usuario delEua = usuarioRepository.findByEmail("eua1@senac.edu.br").orElseThrow();
        Usuario delChina = usuarioRepository.findByEmail("china1@senac.edu.br").orElseThrow();
        Usuario delFranca = usuarioRepository.findByEmail("franca1@senac.edu.br").orElseThrow();

        Grupo grpBrasil = grupoRepository.findByNomeIgnoreCase("Grupo — Delegação do Brasil").orElseThrow();
        Grupo grpEua = grupoRepository.findByNomeIgnoreCase("Grupo — Delegação dos Estados Unidos").orElseThrow();
        Grupo grpChina = grupoRepository.findByNomeIgnoreCase("Grupo — Delegação da China").orElseThrow();
        Grupo grpFranca = grupoRepository.findByNomeIgnoreCase("Grupo — Delegação da França").orElseThrow();

        Usuario[] usuarios = {delBrasil1, delEua, delChina, delFranca};
        Grupo[] grupos = {grpBrasil, grpEua, grpChina, grpFranca};

        int totalRequisicoesSimultaneas = 40;
        long contagemAntes = argumentoRepository.count();

        ExecutorService pool = Executors.newFixedThreadPool(16);
        CountDownLatch largada = new CountDownLatch(1);
        AtomicInteger sucessos = new AtomicInteger(0);
        List<Future<?>> futures = new ArrayList<>();

        for (int i = 0; i < totalRequisicoesSimultaneas; i++) {
            final int idx = i;
            futures.add(pool.submit(() -> {
                try {
                    largada.await();
                    Usuario u = usuarios[idx % usuarios.length];
                    Grupo g = grupos[idx % grupos.length];
                    argumentoService.registrarArgumento(
                            g.getId(),
                            g.getPaisDelegacao(),
                            "Argumento Simultâneo #" + idx + " registrado sob carga presencial",
                            "Justificativa técnica concorrente #" + idx,
                            "Fonte Oficial ONU #" + idx,
                            "https://un.org/doc/" + idx,
                            "Contraponto #" + idx,
                            true,
                            u
                    );
                    sucessos.incrementAndGet();
                } catch (Exception e) {
                    throw new RuntimeException(e);
                }
            }));
        }

        // Dispara todas as 40 contribuições simultaneamente
        telemetriaService.setMomentoAtual(MomentoSimulacao.DURANTE_EVENTO);
        largada.countDown();

        for (Future<?> f : futures) {
            f.get(15, TimeUnit.SECONDS);
        }
        pool.shutdown();

        long contagemDepois = argumentoRepository.count();
        assertThat(sucessos.get()).isEqualTo(totalRequisicoesSimultaneas);
        assertThat(contagemDepois - contagemAntes).isEqualTo(totalRequisicoesSimultaneas);
    }

    @Test
    @DisplayName("5. Indicadores do Piloto e Exportação CSV e XLSX Anonimizados para o Senac SP (Seção 6, 6.1 e 6.2)")
    @WithUserDetails("admin@mov.com")
    void deveGerarIndicadoresEExportarCsvEXlsxAnonimizados() throws Exception {
        Usuario admin = usuarioRepository.findByEmail("admin@mov.com").orElseThrow();

        Map<String, Object> indicadores = telemetriaService.obterIndicadoresPiloto();
        assertThat((Integer) indicadores.get("totalGrupos")).isGreaterThanOrEqualTo(4);
        assertThat((Long) indicadores.get("totalArgumentos")).isGreaterThanOrEqualTo(2L);
        assertThat((Long) indicadores.get("totalFontes")).isGreaterThanOrEqualTo(2L);

        // Exportação CSV Anonimizada
        byte[] csvBytes = relatorioExportService.gerarCsvRelatorioPiloto(true, admin);
        String csvTexto = new String(csvBytes, StandardCharsets.UTF_8);
        assertThat(csvTexto).contains("RELATORIO DE UTILIZACAO DO PROJETO-PILOTO M.O.V. - SIMULACAO DA ONU (SENAC SP)");
        assertThat(csvTexto).contains("AGREGADO E ANONIMIZADO");
        assertThat(csvTexto).contains("Participante #");
        // Garante que o nome pessoal da aluna não vaza no relatório anonimizado
        assertThat(csvTexto).doesNotContain("Ana Beatriz (Delegação Brasil)");

        // Exportação XLSX Nativa
        byte[] xlsxBytes = relatorioExportService.gerarXlsxRelatorioPiloto(true, admin);
        assertThat(xlsxBytes.length).isGreaterThan(500);
        // Verifica assinatura PK (ZIP/OOXML)
        assertThat(xlsxBytes[0]).isEqualTo((byte) 0x50);
        assertThat(xlsxBytes[1]).isEqualTo((byte) 0x4B);

        // Testa endpoints HTTP de administração e exportação via MockMvc
        mockMvc.perform(get("/admin/piloto"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/exportar/csv").param("anonimizado", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(get("/admin/exportar/xlsx").param("anonimizado", "true"))
                .andExpect(status().isOk());

        mockMvc.perform(post("/suporte/reportar")
                        .with(csrf())
                        .param("categoria", "Conexão / Dispositivo")
                        .param("descricao", "Teste de registro de suporte técnico durante a simulação")
                        .param("paginaOrigem", "/dashboard"))
                .andExpect(status().is3xxRedirection());
    }

    @Test
    @DisplayName("6. Renderização Completa das Interfaces Web (Thymeleaf) para Participantes e Organização (Seção 7)")
    @WithUserDetails("brasil1@senac.edu.br")
    void deveRenderizarTodasAsTelasDoParticipanteSemErros() throws Exception {
        Grupo grpBrasil = grupoRepository.findByNomeIgnoreCase("Grupo — Delegação do Brasil").orElseThrow();

        mockMvc.perform(get("/dashboard")).andExpect(status().isOk());
        mockMvc.perform(get("/grupos/" + grpBrasil.getId())).andExpect(status().isOk());
        mockMvc.perform(get("/estudos")).andExpect(status().isOk());
        mockMvc.perform(get("/simulacao")).andExpect(status().isOk());
        mockMvc.perform(get("/consenso")).andExpect(status().isOk());
        mockMvc.perform(get("/leitura")).andExpect(status().isOk());
        mockMvc.perform(get("/api/grupos/" + grpBrasil.getId() + "/argumentos")).andExpect(status().isOk());
    }
}
