package com.turnosmart.turnosmart_backend.service;

import com.lowagie.text.DocumentException;
import com.turnosmart.turnosmart_backend.entity.Appointment;
import com.turnosmart.turnosmart_backend.exception.BusinessException;
import lombok.RequiredArgsConstructor;
import org.springframework.stereotype.Service;
import org.thymeleaf.TemplateEngine;
import org.thymeleaf.context.Context;
import org.xhtmlrenderer.pdf.ITextRenderer;

import java.io.FileOutputStream;
import java.io.IOException;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.Paths;
import java.time.LocalDate;
import java.time.format.DateTimeFormatter;
import java.util.Locale;

/**
 * Genera automáticamente los documentos legales (Carta de Poder,
 * Carta de Representación Legal Persona Natural/Jurídica) en formato PDF
 * usando plantillas Thymeleaf convertidas con Flying Saucer.
 *
 * Se invoca cuando el abogado aprueba los documentos subidos por el cliente
 * (estado DOCUMENTOS_ENVIADOS → ENTREGADO).
 */
@Service
@RequiredArgsConstructor
public class DocumentGeneratorService {

    private final TemplateEngine templateEngine;

    private static final Path OUTPUT_DIR = Paths.get("uploads/cartas");

    // Mes en español para la fecha formal del documento
    private static final String[] MESES = {
            "enero", "febrero", "marzo", "abril", "mayo", "junio",
            "julio", "agosto", "septiembre", "octubre", "noviembre", "diciembre"
    };

    /**
     * Genera el PDF legal correspondiente al trámite y retorna la ruta del archivo.
     *
     * @param appointment El trámite aprobado con todos sus datos.
     * @return Ruta relativa del PDF generado (ej. /uploads/cartas/TS-XXXX_carta.pdf)
     */
    public String generarCarta(Appointment appointment) {
        try {
            if (!Files.exists(OUTPUT_DIR)) {
                Files.createDirectories(OUTPUT_DIR);
            }

            String templateName = resolverPlantilla(appointment);
            Context ctx = construirContexto(appointment);

            String html = templateEngine.process(templateName, ctx);

            String nombreArchivo = appointment.getTicketNumber() + "_carta_legal.pdf";
            Path rutaSalida = OUTPUT_DIR.resolve(nombreArchivo);

            generarPdf(html, rutaSalida);

            return "/uploads/cartas/" + nombreArchivo;

        } catch (IOException | DocumentException e) {
            throw new BusinessException("Error al generar la carta legal: " + e.getMessage());
        }
    }

    // -----------------------------------------------------------------------
    // Privados
    // -----------------------------------------------------------------------

    /**
     * Decide qué plantilla Thymeleaf usar según el nombre del trámite y el
     * tipo de representación del cliente.
     *
     * Mapeo:
     *   "Poderes"               → documentos/carta-poder
     *   "Representación Legal" + JURIDICA → documentos/carta-rep-juridica
     *   "Representación Legal" + NATURAL  → documentos/carta-rep-natural
     */
    private String resolverPlantilla(Appointment app) {
        String nombreTramite = app.getProcedureType().getName().toLowerCase();

        if (nombreTramite.contains("poder")) {
            return "documentos/carta-poder";
        }

        if (nombreTramite.contains("representaci")) {
            String tipo = app.getRepresentationType();
            if ("JURIDICA".equalsIgnoreCase(tipo)) {
                return "documentos/carta-rep-juridica";
            }
            return "documentos/carta-rep-natural";
        }

        // Fallback: si el nombre del trámite no encaja en ningún patrón
        // usar carta poder como genérica hasta que se agregue más tipos.
        return "documentos/carta-poder";
    }

    /**
     * Construye el Thymeleaf Context con todas las variables que usan las plantillas.
     * Parsea clientNotes para extraer los datos del representado y apoderado
     * (ya que se almacenan como texto plano estructurado).
     */
    private Context construirContexto(Appointment app) {
        Context ctx = new Context(new Locale("es", "PE"));

        LocalDate hoy = LocalDate.now();
        String lugarFecha = "San Isidro, " + hoy.getDayOfMonth()
                + " de " + MESES[hoy.getMonthValue() - 1]
                + " de " + hoy.getYear();

        ctx.setVariable("lugarFecha", lugarFecha);
        ctx.setVariable("fechaGeneracion", hoy.format(DateTimeFormatter.ofPattern("dd/MM/yyyy")));
        ctx.setVariable("ticketNumber", app.getTicketNumber());

        // Datos del representado — extraídos de clientNotes
        ctx.setVariable("repDni",        extraer(app.getClientNotes(), "DNI:"));
        ctx.setVariable("repNombre",     extraer(app.getClientNotes(), "Nombres Completos:"));
        ctx.setVariable("repEstadoCivil", extraer(app.getClientNotes(), "Estado Civil:"));
        ctx.setVariable("repNacionalidad", extraer(app.getClientNotes(), "Nacionalidad:"));

        // Datos del apoderado — extraídos de clientNotes
        ctx.setVariable("apoDni",    extraerBloque(app.getClientNotes(), "DATOS DEL APODERADO", "DNI:"));
        ctx.setVariable("apoNombre", extraerBloque(app.getClientNotes(), "DATOS DEL APODERADO", "Nombres Completos:"));
        ctx.setVariable("facultades", extraerBloque(app.getClientNotes(), "DATOS DEL APODERADO", "Facultades:"));

        // Gestión específica — viene de las notas generales del cliente
        ctx.setVariable("gestion", extraer(app.getClientNotes(), "Descripción/Notas del Cliente:"));

        // Datos de empresa (solo para carta jurídica)
        ctx.setVariable("razonSocial", app.getBusinessName() != null ? app.getBusinessName() : "");
        ctx.setVariable("ruc", app.getIdentifier() != null ? app.getIdentifier() : "");

        return ctx;
    }

    private String extraer(String texto, String clave) {
        if (texto == null) return "";
        for (String linea : texto.split("\n")) {
            if (linea.toLowerCase().contains(clave.toLowerCase())) {
                int pos = linea.indexOf(':');
                if (pos >= 0) return linea.substring(pos + 1).trim();
            }
        }
        return "";
    }

    private String extraerBloque(String texto, String encabezadoBloque, String clave) {
        if (texto == null) return "";
        String[] partes = texto.split("(?i)\\[" + encabezadoBloque);
        if (partes.length < 2) return "";
        // Tomar solo el bloque después del encabezado buscado
        String bloque = partes[1].split("\\[")[0];
        return extraer(bloque, clave);
    }

    private void generarPdf(String html, Path salida) throws DocumentException, IOException {
        ITextRenderer renderer = new ITextRenderer();
        renderer.setDocumentFromString(html);
        renderer.layout();

        try (FileOutputStream fos = new FileOutputStream(salida.toFile())) {
            renderer.createPDF(fos);
        }
    }
}