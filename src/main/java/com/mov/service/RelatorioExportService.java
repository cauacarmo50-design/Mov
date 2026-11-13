package com.mov.service;

import com.mov.model.ArgumentoRegistro;
import com.mov.model.Documento;
import com.mov.model.ProblemaTecnico;
import com.mov.model.Usuario;
import com.mov.model.enums.TipoEventoLog;
import com.mov.repository.ArgumentoRegistroRepository;
import com.mov.repository.DocumentoRepository;
import com.mov.repository.ProblemaTecnicoRepository;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.time.format.DateTimeFormatter;
import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.zip.ZipEntry;
import java.util.zip.ZipOutputStream;

@Service
@RequiredArgsConstructor
public class RelatorioExportService {

    private final TelemetriaService telemetriaService;
    private final ArgumentoRegistroRepository argumentoRepository;
    private final DocumentoRepository documentoRepository;
    private final ProblemaTecnicoRepository problemaTecnicoRepository;

    private static final DateTimeFormatter FMT = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm:ss");

    public byte[] gerarCsvRelatorioPiloto(boolean anonimizado, Usuario solicitante) {
        telemetriaService.registrarEvento(
                TipoEventoLog.EXPORTACAO_DADOS,
                solicitante,
                null,
                null,
                "Exportação CSV do relatório piloto (anonimizado=" + anonimizado + ")"
        );

        Map<String, Object> ind = telemetriaService.obterIndicadoresPiloto();
        StringBuilder sb = new StringBuilder();
        // UTF-8 BOM para o Excel reconhecer acentuação automaticamente
        sb.append('\uFEFF');

        sb.append("RELATORIO DE UTILIZACAO DO PROJETO-PILOTO M.O.V. - SIMULACAO DA ONU (SENAC SP)\n");
        sb.append("Modo de Protecao de Dados (LGPD);").append(anonimizado ? "AGREGADO E ANONIMIZADO" : "DETALHADO ADMINISTRATIVO").append("\n\n");

        sb.append("1. INDICADORES GERAIS DO PILOTO (SECAO 6)\n");
        sb.append("Indicador;Valor;Descricao\n");
        sb.append("Acessos (Usuarios Unicos);").append(ind.get("usuariosComAcesso")).append(";Quantidade de usuarios que acessaram a plataforma\n");
        sb.append("Total de Logins Realizados;").append(ind.get("totalLogins")).append(";Frequencia total de autenticacoes\n");
        sb.append("Participacao Ativa;").append(ind.get("usuariosComContribuicao")).append(";Quantidade de usuarios que realizaram alguma contribuicao\n");
        sb.append("Grupos (Delegacoes);").append(ind.get("totalGrupos")).append(";Quantidade de grupos criados e utilizados\n");
        sb.append("Documentos Inseridos;").append(ind.get("totalDocumentos")).append(";Quantidade de documentos disponibilizados\n");
        sb.append("Consultas/Downloads de Documentos;").append(ind.get("totalConsultasDocumentos")).append(";Frequencia de abertura e download dos documentos\n");
        sb.append("Argumentos Registrados;").append(ind.get("totalArgumentos")).append(";Quantidade de argumentos registrados nos arquivos compartilhados\n");
        sb.append("Fontes Compartilhadas;").append(ind.get("totalFontes")).append(";Quantidade de referencias bibliograficas e links adicionados\n");
        sb.append("Contrapontos Registrados;").append(ind.get("totalContrapontos")).append(";Quantidade de contrapontos registrados\n");
        sb.append("Historico de Versoes/Edicoes;").append(ind.get("totalAlteracoesHistorico")).append(";Total de salvamentos e versoes registradas\n");
        sb.append("Problemas Tecnicos Registrados;").append(ind.get("totalProblemas")).append(";Erros e dificuldades tecnicas reportadas\n");
        sb.append("Tentativas de Acesso Indevido Bloqueadas;").append(ind.get("acessosIndevidosBloqueados")).append(";Protecao de isolamento entre grupos\n\n");

        sb.append("2. INDICADORES POR GRUPO / DELEGACAO (SECAO 6.1)\n");
        sb.append("Grupo;Pais / Delegacao;Integrantes;Usuarios Ativos;Documentos;Argumentos;Fontes;Contrapontos\n");
        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metricasGrupo = (List<Map<String, Object>>) ind.get("metricasPorGrupo");
        if (metricasGrupo != null) {
            for (Map<String, Object> mg : metricasGrupo) {
                sb.append(csvEscape(mg.get("nome"))).append(";")
                  .append(csvEscape(mg.get("paisDelegacao"))).append(";")
                  .append(mg.get("totalIntegrantes")).append(";")
                  .append(mg.get("usuariosAtivos")).append(";")
                  .append(mg.get("totalDocumentos")).append(";")
                  .append(mg.get("totalArgumentos")).append(";")
                  .append(mg.get("totalFontes")).append(";")
                  .append(mg.get("totalContrapontos")).append("\n");
            }
        }
        sb.append("\n");

        sb.append("3. REGISTROS DO ARQUIVO COMPARTILHADO DE ARGUMENTOS E FONTES (SECAO 3.3)\n");
        sb.append("Data/Hora;Fase do Evento;Grupo;Delegacao;Argumento;Justificativa;Fonte;Link;Contraponto;Autor (Identificacao);Versao\n");
        List<ArgumentoRegistro> argumentos = argumentoRepository.findAllByOrderByDataAtualizacaoDesc();
        for (ArgumentoRegistro a : argumentos) {
            String autorStr = anonimizado
                    ? TelemetriaService.anonimizarIdentificador(a.getAutor() != null ? a.getAutor().getId() : null)
                    : (a.getAutor() != null ? a.getAutor().getNome() : "N/A");
            sb.append(a.getDataAtualizacao() != null ? a.getDataAtualizacao().format(FMT) : "").append(";")
              .append(a.getMomento() != null ? a.getMomento().name() : "").append(";")
              .append(csvEscape(a.getGrupo() != null ? a.getGrupo().getNome() : "Plenario Geral")).append(";")
              .append(csvEscape(a.getDelegacao())).append(";")
              .append(csvEscape(a.getArgumento())).append(";")
              .append(csvEscape(a.getJustificativa())).append(";")
              .append(csvEscape(a.getFonte())).append(";")
              .append(csvEscape(a.getLinkFonte())).append(";")
              .append(csvEscape(a.getContraponto())).append(";")
              .append(csvEscape(autorStr)).append(";")
              .append("v").append(a.getNumeroVersao()).append("\n");
        }
        sb.append("\n");

        sb.append("4. DOCUMENTOS INSERIDOS E FREQUENCIA DE CONSULTA (SECAO 3.2 E 6.1)\n");
        sb.append("Titulo;Tipo;Escopo/Grupo;Formato;Tamanho;Total Consultas/Downloads;Data Envio\n");
        for (Documento d : documentoRepository.findAllByOrderByDataEnvioDesc()) {
            String escopo = Boolean.TRUE.equals(d.getPublicoGeral()) ? "Geral (Toda a Simulacao)"
                    : (d.getGrupo() != null ? d.getGrupo().getNome() : "Geral");
            sb.append(csvEscape(d.getTitulo())).append(";")
              .append(d.getTipoDocumento() != null ? d.getTipoDocumento().getRotulo() : "").append(";")
              .append(csvEscape(escopo)).append(";")
              .append(csvEscape(d.getNomeOriginal())).append(";")
              .append(d.getTamanhoFormatado()).append(";")
              .append(d.getTotalConsultas()).append(";")
              .append(d.getDataEnvio() != null ? d.getDataEnvio().format(FMT) : "").append("\n");
        }
        sb.append("\n");

        sb.append("5. REGISTRO DE PROBLEMAS TECNICOS (SECAO 6 E 11)\n");
        sb.append("Data/Hora;Categoria;Status;Dispositivo;Descricao;Resposta Equipe\n");
        for (ProblemaTecnico p : problemaTecnicoRepository.findAllByOrderByDataRegistroDesc()) {
            sb.append(p.getDataRegistro() != null ? p.getDataRegistro().format(FMT) : "").append(";")
              .append(csvEscape(p.getCategoria())).append(";")
              .append(csvEscape(p.getStatus())).append(";")
              .append(csvEscape(p.getDispositivoInfo())).append(";")
              .append(csvEscape(p.getDescricao())).append(";")
              .append(csvEscape(p.getRespostaEquipe())).append("\n");
        }

        return sb.toString().getBytes(StandardCharsets.UTF_8);
    }

    public byte[] gerarXlsxRelatorioPiloto(boolean anonimizado, Usuario solicitante) {
        telemetriaService.registrarEvento(
                TipoEventoLog.EXPORTACAO_DADOS,
                solicitante,
                null,
                null,
                "Exportação XLSX do relatório piloto (anonimizado=" + anonimizado + ")"
        );

        Map<String, Object> ind = telemetriaService.obterIndicadoresPiloto();

        // Aba 1: Indicadores Gerais e Por Grupo
        List<List<String>> aba1 = new ArrayList<>();
        aba1.add(List.of("RELATÓRIO PILOTO M.O.V. - SIMULAÇÃO DA ONU (SENAC SP)", anonimizado ? "MODO ANONIMIZADO (LGPD)" : "MODO ADMINISTRATIVO"));
        aba1.add(List.of("Indicador", "Valor", "Finalidade"));
        aba1.add(List.of("Acessos (Usuários Únicos)", String.valueOf(ind.get("usuariosComAcesso")), "Quantidade de usuários que acessaram a plataforma"));
        aba1.add(List.of("Participação (Com Contribuição)", String.valueOf(ind.get("usuariosComContribuicao")), "Quantidade de usuários que registraram contribuições"));
        aba1.add(List.of("Grupos Criados e Utilizados", String.valueOf(ind.get("totalGrupos")), "Delegações ativas no piloto"));
        aba1.add(List.of("Documentos Inseridos", String.valueOf(ind.get("totalDocumentos")), "Materiais disponibilizados"));
        aba1.add(List.of("Consultas a Documentos", String.valueOf(ind.get("totalConsultasDocumentos")), "Visualizações e downloads realizados"));
        aba1.add(List.of("Argumentos Registrados", String.valueOf(ind.get("totalArgumentos")), "Total de argumentos nos arquivos compartilhados"));
        aba1.add(List.of("Fontes e Referências", String.valueOf(ind.get("totalFontes")), "Total de fontes citadas"));
        aba1.add(List.of("Contrapontos Registrados", String.valueOf(ind.get("totalContrapontos")), "Total de contrapontos registrados"));
        aba1.add(List.of("Alterações no Histórico", String.valueOf(ind.get("totalAlteracoesHistorico")), "Versões salvas nos arquivos compartilhados"));
        aba1.add(List.of("Problemas Técnicos", String.valueOf(ind.get("totalProblemas")), "Ocorrências técnicas registradas"));
        aba1.add(List.of("", "", ""));
        aba1.add(List.of("GRUPO / DELEGAÇÃO", "PAÍS", "INTEGRANTES", "USUÁRIOS ATIVOS", "DOCUMENTOS", "ARGUMENTOS", "FONTES", "CONTRAPONTOS"));

        @SuppressWarnings("unchecked")
        List<Map<String, Object>> metricasGrupo = (List<Map<String, Object>>) ind.get("metricasPorGrupo");
        if (metricasGrupo != null) {
            for (Map<String, Object> mg : metricasGrupo) {
                aba1.add(List.of(
                        String.valueOf(mg.get("nome")),
                        String.valueOf(mg.get("paisDelegacao")),
                        String.valueOf(mg.get("totalIntegrantes")),
                        String.valueOf(mg.get("usuariosAtivos")),
                        String.valueOf(mg.get("totalDocumentos")),
                        String.valueOf(mg.get("totalArgumentos")),
                        String.valueOf(mg.get("totalFontes")),
                        String.valueOf(mg.get("totalContrapontos"))
                ));
            }
        }

        // Aba 2: Argumentos e Fontes
        List<List<String>> aba2 = new ArrayList<>();
        aba2.add(List.of("Data/Hora", "Fase", "Grupo", "Delegação", "Argumento", "Justificativa", "Fonte", "Link", "Contraponto", "Autoria", "Versão"));
        for (ArgumentoRegistro a : argumentoRepository.findAllByOrderByDataAtualizacaoDesc()) {
            String autorStr = anonimizado
                    ? TelemetriaService.anonimizarIdentificador(a.getAutor() != null ? a.getAutor().getId() : null)
                    : (a.getAutor() != null ? a.getAutor().getNome() : "N/A");
            aba2.add(List.of(
                    a.getDataAtualizacao() != null ? a.getDataAtualizacao().format(FMT) : "",
                    a.getMomento() != null ? a.getMomento().name() : "",
                    a.getGrupo() != null ? a.getGrupo().getNome() : "Plenário Geral",
                    a.getDelegacao() != null ? a.getDelegacao() : "",
                    a.getArgumento() != null ? a.getArgumento() : "",
                    a.getJustificativa() != null ? a.getJustificativa() : "",
                    a.getFonte() != null ? a.getFonte() : "",
                    a.getLinkFonte() != null ? a.getLinkFonte() : "",
                    a.getContraponto() != null ? a.getContraponto() : "",
                    autorStr,
                    "v" + a.getNumeroVersao()
            ));
        }

        return empacotarXlsxNativo(aba1, aba2);
    }

    private byte[] empacotarXlsxNativo(List<List<String>> sheet1Rows, List<List<String>> sheet2Rows) {
        try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
             ZipOutputStream zos = new ZipOutputStream(baos, StandardCharsets.UTF_8)) {

            addZipXml(zos, "[Content_Types].xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Types xmlns=\"http://schemas.openxmlformats.org/package/2006/content-types\">" +
                    "<Default Extension=\"rels\" ContentType=\"application/vnd.openxmlformats-package.relationships+xml\"/>" +
                    "<Default Extension=\"xml\" ContentType=\"application/xml\"/>" +
                    "<Override PartName=\"/xl/workbook.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.sheet.main+xml\"/>" +
                    "<Override PartName=\"/xl/worksheets/sheet1.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                    "<Override PartName=\"/xl/worksheets/sheet2.xml\" ContentType=\"application/vnd.openxmlformats-officedocument.spreadsheetml.worksheet+xml\"/>" +
                    "</Types>");

            addZipXml(zos, "_rels/.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/officeDocument\" Target=\"xl/workbook.xml\"/>" +
                    "</Relationships>");

            addZipXml(zos, "xl/workbook.xml",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<workbook xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\" xmlns:r=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships\">" +
                    "<sheets>" +
                    "<sheet name=\"Indicadores_Senac\" sheetId=\"1\" r:id=\"rId1\"/>" +
                    "<sheet name=\"Argumentos_e_Fontes\" sheetId=\"2\" r:id=\"rId2\"/>" +
                    "</sheets>" +
                    "</workbook>");

            addZipXml(zos, "xl/_rels/workbook.xml.rels",
                    "<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>" +
                    "<Relationships xmlns=\"http://schemas.openxmlformats.org/package/2006/relationships\">" +
                    "<Relationship Id=\"rId1\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet1.xml\"/>" +
                    "<Relationship Id=\"rId2\" Type=\"http://schemas.openxmlformats.org/officeDocument/2006/relationships/worksheet\" Target=\"worksheets/sheet2.xml\"/>" +
                    "</Relationships>");

            addZipXml(zos, "xl/worksheets/sheet1.xml", buildWorksheetXml(sheet1Rows));
            addZipXml(zos, "xl/worksheets/sheet2.xml", buildWorksheetXml(sheet2Rows));

            zos.finish();
            return baos.toByteArray();
        } catch (IOException e) {
            throw new RuntimeException("Erro ao gerar arquivo XLSX: " + e.getMessage(), e);
        }
    }

    private String buildWorksheetXml(List<List<String>> rows) {
        StringBuilder sb = new StringBuilder();
        sb.append("<?xml version=\"1.0\" encoding=\"UTF-8\" standalone=\"yes\"?>");
        sb.append("<worksheet xmlns=\"http://schemas.openxmlformats.org/spreadsheetml/2006/main\"><sheetData>");
        for (int r = 0; r < rows.size(); r++) {
            sb.append("<row r=\"").append(r + 1).append("\">");
            List<String> cols = rows.get(r);
            for (int c = 0; c < cols.size(); c++) {
                String colRef = toColLetter(c) + (r + 1);
                String val = cols.get(c) != null ? cols.get(c) : "";
                sb.append("<c r=\"").append(colRef).append("\" t=\"inlineStr\"><is><t>")
                  .append(xmlEscape(val))
                  .append("</t></is></c>");
            }
            sb.append("</row>");
        }
        sb.append("</sheetData></worksheet>");
        return sb.toString();
    }

    private String toColLetter(int colIndex) {
        StringBuilder sb = new StringBuilder();
        int n = colIndex;
        while (n >= 0) {
            sb.insert(0, (char) ('A' + (n % 26)));
            n = (n / 26) - 1;
        }
        return sb.toString();
    }

    private void addZipXml(ZipOutputStream zos, String entryName, String content) throws IOException {
        ZipEntry entry = new ZipEntry(entryName);
        zos.putNextEntry(entry);
        zos.write(content.getBytes(StandardCharsets.UTF_8));
        zos.closeEntry();
    }

    private String xmlEscape(String s) {
        if (s == null) return "";
        return s.replace("&", "&amp;")
                .replace("<", "&lt;")
                .replace(">", "&gt;")
                .replace("\"", "&quot;")
                .replace("'", "&apos;");
    }

    private String csvEscape(Object val) {
        if (val == null) return "";
        String s = String.valueOf(val).replace("\r", " ").replace("\n", " ").replace(";", ",");
        return "\"" + s.replace("\"", "\"\"") + "\"";
    }
}
