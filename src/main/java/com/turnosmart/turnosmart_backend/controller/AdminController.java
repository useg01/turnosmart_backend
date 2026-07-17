package com.turnosmart.turnosmart_backend.controller;

import com.turnosmart.turnosmart_backend.entity.Appointment;
import com.turnosmart.turnosmart_backend.entity.AppointmentStatus;
import com.turnosmart.turnosmart_backend.entity.Lawyer;
import com.turnosmart.turnosmart_backend.entity.User;
import com.turnosmart.turnosmart_backend.repository.AppointmentRepository;
import com.turnosmart.turnosmart_backend.service.AnalyticsService;
import com.turnosmart.turnosmart_backend.service.AppointmentService;
import com.turnosmart.turnosmart_backend.service.LawyerService;
import com.turnosmart.turnosmart_backend.service.UserService;
import lombok.RequiredArgsConstructor;
import org.apache.poi.ss.usermodel.*;
import org.apache.poi.ss.util.CellRangeAddress;
import org.apache.poi.xssf.usermodel.XSSFWorkbook;
import org.springframework.format.annotation.DateTimeFormat;
import org.springframework.http.HttpHeaders;
import org.springframework.http.MediaType;
import org.springframework.http.ResponseEntity;
import org.springframework.stereotype.Controller;
import org.springframework.ui.Model;
import org.springframework.web.bind.annotation.*;

import java.io.ByteArrayOutputStream;
import java.security.Principal;
import java.time.LocalDate;
import java.time.LocalDateTime;
import java.time.LocalTime;
import java.time.format.DateTimeFormatter;
import java.util.List;
import java.util.Map;
import java.util.stream.Collectors;

@Controller
@RequestMapping("/admin")
@RequiredArgsConstructor
public class AdminController {

    private final AppointmentRepository appointmentRepository;
    private final AnalyticsService analyticsService;
    private final AppointmentService appointmentService;
    private final LawyerService lawyerService;
    private final UserService userService;

    @GetMapping("/dashboard")
    public String dashboard(@RequestParam(name = "filter", defaultValue = "mes") String filter, Model model) {
        model.addAttribute("metrics", analyticsService.getDashboardMetrics(filter));
        model.addAttribute("currentFilter", filter);
        model.addAttribute("tramites", appointmentRepository.findAllWithDetails());
        return "admin/dashboard";
    }

    @GetMapping("/reportes")
    public String reportes(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta,
            Model model,
            Principal principal) {

        if (principal != null) {
            User user = userService.findByEmail(principal.getName());
            model.addAttribute("user", user);
        }

        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();

        LocalDateTime desdeDateTime = desde.atStartOfDay();
        LocalDateTime hastaDateTime = hasta.atTime(LocalTime.MAX);

        List<Appointment> tramites = appointmentRepository.findByCreatedAtBetween(desdeDateTime, hastaDateTime);

        long totalTramites = tramites.size();

        long countPendientes = tramites.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.PENDIENTE_EVALUACION
                        || a.getStatus() == AppointmentStatus.REVISION)
                .count();

        long countProceso = tramites.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CONFORME
                        || a.getStatus() == AppointmentStatus.DOCUMENTOS_ENVIADOS
                        || a.getStatus() == AppointmentStatus.REGULARIZAR
                        || a.getStatus() == AppointmentStatus.PROCESO_DETENIDO)
                .count();

        long countFinalizados = tramites.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.ENTREGADO)
                .count();

        long countCancelados = tramites.stream()
                .filter(a -> a.getStatus() == AppointmentStatus.CANCELADO)
                .count();

        double recaudacion = tramites.stream()
                .filter(a -> Boolean.TRUE.equals(a.getIsPaid()))
                .mapToDouble(a -> {
                    String nombre = a.getProcedureType().getName();
                    if (nombre.contains("Representación")) return 300.0;
                    if (nombre.contains("Poderes"))        return 150.0;
                    return 0.0;
                })
                .sum();

        Map<String, Long> porEstado = tramites.stream()
                .collect(Collectors.groupingBy(a -> a.getStatus().getLabel(), Collectors.counting()));

        List<String> estadosLabels     = List.copyOf(porEstado.keySet());
        List<Long>   estadosCantidades = estadosLabels.stream().map(porEstado::get).collect(Collectors.toList());

        Map<String, Long> porTipo = tramites.stream()
                .collect(Collectors.groupingBy(a -> a.getProcedureType().getName(), Collectors.counting()));

        List<String> tiposLabels     = List.copyOf(porTipo.keySet());
        List<Long>   tiposCantidades = tiposLabels.stream().map(porTipo::get).collect(Collectors.toList());

        model.addAttribute("tramites",          tramites);
        model.addAttribute("totalTramites",     totalTramites);
        model.addAttribute("countPendientes",   countPendientes);
        model.addAttribute("countProceso",      countProceso);
        model.addAttribute("countFinalizados",  countFinalizados);
        model.addAttribute("countCancelados",   countCancelados);
        model.addAttribute("recaudacionTotal",  String.format("%.2f", recaudacion));
        model.addAttribute("estadosLabels",     estadosLabels);
        model.addAttribute("estadosCantidades", estadosCantidades);
        model.addAttribute("tiposLabels",       tiposLabels);
        model.addAttribute("tiposCantidades",   tiposCantidades);
        model.addAttribute("desde", desde);
        model.addAttribute("hasta", hasta);

        return "admin/reportes";
    }

    @GetMapping("/reportes/exportar")
    public ResponseEntity<byte[]> exportarExcel(
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate desde,
            @RequestParam(required = false) @DateTimeFormat(iso = DateTimeFormat.ISO.DATE) LocalDate hasta) throws Exception {

        if (desde == null) desde = LocalDate.now().withDayOfMonth(1);
        if (hasta == null) hasta = LocalDate.now();

        List<Appointment> tramites = appointmentRepository.findByCreatedAtBetween(
                desde.atStartOfDay(), hasta.atTime(LocalTime.MAX));

        XSSFWorkbook workbook = new XSSFWorkbook();

        CellStyle estiloTitulo = workbook.createCellStyle();
        Font fuenteTitulo = workbook.createFont();
        fuenteTitulo.setBold(true);
        fuenteTitulo.setFontHeightInPoints((short) 14);
        estiloTitulo.setFont(fuenteTitulo);
        estiloTitulo.setFillForegroundColor(IndexedColors.DARK_BLUE.getIndex());
        estiloTitulo.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        fuenteTitulo.setColor(IndexedColors.WHITE.getIndex());
        estiloTitulo.setAlignment(HorizontalAlignment.CENTER);

        CellStyle estiloHeader = workbook.createCellStyle();
        Font fuenteHeader = workbook.createFont();
        fuenteHeader.setBold(true);
        estiloHeader.setFont(fuenteHeader);
        estiloHeader.setFillForegroundColor(IndexedColors.GREY_25_PERCENT.getIndex());
        estiloHeader.setFillPattern(FillPatternType.SOLID_FOREGROUND);
        estiloHeader.setBorderBottom(BorderStyle.THIN);

        CellStyle estiloCelda = workbook.createCellStyle();
        estiloCelda.setBorderBottom(BorderStyle.HAIR);

        CellStyle estiloPagado = workbook.createCellStyle();
        Font fuenteVerde = workbook.createFont();
        fuenteVerde.setColor(IndexedColors.GREEN.getIndex());
        fuenteVerde.setBold(true);
        estiloPagado.setFont(fuenteVerde);

        DateTimeFormatter fmt = DateTimeFormatter.ofPattern("dd/MM/yyyy HH:mm");

        Sheet hoja = workbook.createSheet("Trámites");

        Row filaTitulo = hoja.createRow(0);
        Cell celdaTitulo = filaTitulo.createCell(0);
        celdaTitulo.setCellValue("Reporte de Trámites — Turnosmart | " + desde + " al " + hasta);
        celdaTitulo.setCellStyle(estiloTitulo);
        hoja.addMergedRegion(new CellRangeAddress(0, 0, 0, 7));

        String[] headers = {"Ticket", "Cliente", "DNI Cliente", "Tipo de Trámite", "Especialista", "Fecha Solicitud", "Estado", "Pago"};
        Row filaHeader = hoja.createRow(2);
        for (int i = 0; i < headers.length; i++) {
            Cell c = filaHeader.createCell(i);
            c.setCellValue(headers[i]);
            c.setCellStyle(estiloHeader);
        }

        int fila = 3;
        for (Appointment t : tramites) {
            Row row = hoja.createRow(fila++);

            Cell c0 = row.createCell(0); c0.setCellValue(t.getTicketNumber()); c0.setCellStyle(estiloCelda);
            Cell c1 = row.createCell(1); c1.setCellValue(t.getClient().getFirstName() + " " + t.getClient().getLastName()); c1.setCellStyle(estiloCelda);
            Cell c2 = row.createCell(2); c2.setCellValue(t.getClient().getDni()); c2.setCellStyle(estiloCelda);
            Cell c3 = row.createCell(3); c3.setCellValue(t.getProcedureType().getName()); c3.setCellStyle(estiloCelda);
            Cell c4 = row.createCell(4); c4.setCellValue(t.getLawyer().getUser().getFirstName() + " " + t.getLawyer().getUser().getLastName()); c4.setCellStyle(estiloCelda);
            Cell c5 = row.createCell(5); c5.setCellValue(t.getCreatedAt() != null ? t.getCreatedAt().format(fmt) : ""); c5.setCellStyle(estiloCelda);
            Cell c6 = row.createCell(6); c6.setCellValue(t.getStatus().getLabel()); c6.setCellStyle(estiloCelda);
            Cell c7 = row.createCell(7);
            c7.setCellValue(Boolean.TRUE.equals(t.getIsPaid()) ? "Pagado" : "Pendiente");
            c7.setCellStyle(Boolean.TRUE.equals(t.getIsPaid()) ? estiloPagado : estiloCelda);
        }

        Sheet hojaResumen = workbook.createSheet("Resumen");

        Row rTitulo = hojaResumen.createRow(0);
        Cell cTitulo = rTitulo.createCell(0);
        cTitulo.setCellValue("Resumen del Período");
        cTitulo.setCellStyle(estiloTitulo);
        hojaResumen.addMergedRegion(new CellRangeAddress(0, 0, 0, 1));

        String[][] resumen = {
                {"Total Trámites", String.valueOf(tramites.size())},
                {"En Revisión", String.valueOf(tramites.stream().filter(a -> a.getStatus() == AppointmentStatus.PENDIENTE_EVALUACION || a.getStatus() == AppointmentStatus.REVISION).count())},
                {"Completados", String.valueOf(tramites.stream().filter(a -> a.getStatus() == AppointmentStatus.ENTREGADO).count())},
                {"Cancelados", String.valueOf(tramites.stream().filter(a -> a.getStatus() == AppointmentStatus.CANCELADO).count())},
                {"Recaudación Total", "S/ " + String.format("%.2f", tramites.stream().filter(a -> Boolean.TRUE.equals(a.getIsPaid())).mapToDouble(a -> { String n = a.getProcedureType().getName(); return n.contains("Representación") ? 300.0 : n.contains("Poderes") ? 150.0 : 0.0; }).sum())}
        };

        for (int i = 0; i < resumen.length; i++) {
            Row r = hojaResumen.createRow(i + 2);
            Cell k = r.createCell(0); k.setCellValue(resumen[i][0]); k.setCellStyle(estiloHeader);
            Cell v = r.createCell(1); v.setCellValue(resumen[i][1]);
        }

        for (int i = 0; i < headers.length; i++) hoja.autoSizeColumn(i);
        hojaResumen.autoSizeColumn(0);
        hojaResumen.autoSizeColumn(1);

        ByteArrayOutputStream out = new ByteArrayOutputStream();
        workbook.write(out);
        workbook.close();

        String nombreArchivo = "Reporte_Turnosmart_" + desde + "_" + hasta + ".xlsx";

        return ResponseEntity.ok()
                .header(HttpHeaders.CONTENT_DISPOSITION, "attachment; filename=\"" + nombreArchivo + "\"")
                .contentType(MediaType.parseMediaType("application/vnd.openxmlformats-officedocument.spreadsheetml.sheet"))
                .body(out.toByteArray());
    }

    @GetMapping("/usuarios")
    public String listarClientes(Model model, Principal principal) {
        if (principal != null) {
            User currentUser = userService.findByEmail(principal.getName());
            model.addAttribute("user", currentUser);
        }
        List<User> usuariosActivos = userService.findAll().stream()
                .filter(u -> u.getEnabled() != null && u.getEnabled())
                .filter(u -> u.getRoles().stream().anyMatch(r -> r.getName().equals("ROLE_CLIENTE")))
                .toList();
        model.addAttribute("usuarios", usuariosActivos);
        return "admin/usuarios";
    }

    @GetMapping("/usuarios/detalle/{id}")
    public String verDetalleUsuario(@PathVariable Long id, Model model) {
        model.addAttribute("usuario", userService.findById(id));
        return "admin/detalle-usuario";
    }

    @GetMapping("/usuarios/eliminar/{id}")
    public String eliminarUsuario(@PathVariable Long id) {
        userService.deactivate(id);
        return "redirect:/admin/usuarios";
    }

    @GetMapping("/abogados")
    public String listarAbogados(Model model) {
        model.addAttribute("abogados", lawyerService.findAll());
        return "admin/abogados";
    }

    @GetMapping("/abogados/nuevo")
    public String formularioAbogado(Model model) {
        Lawyer lawyer = new Lawyer();
        lawyer.setUser(new User());
        model.addAttribute("lawyer", lawyer);
        return "admin/nuevo-abogado";
    }

    @GetMapping("/abogados/editar/{id}")
    public String editarAbogado(@PathVariable Long id, Model model) {
        model.addAttribute("lawyer", lawyerService.findById(id));
        return "admin/nuevo-abogado";
    }

    @PostMapping("/abogados/guardar")
    public String guardarAbogado(@ModelAttribute Lawyer lawyer, Model model) {
        User user = lawyer.getUser();
        try {
            if (lawyer.getId() != null) {
                Lawyer existingLawyer = lawyerService.findById(lawyer.getId());
                User existingUser = existingLawyer.getUser();
                existingUser.setFirstName(user.getFirstName());
                existingUser.setLastName(user.getLastName());
                existingUser.setEmail(user.getEmail());
                existingUser.setDni(user.getDni());
                existingUser.setPhone(user.getPhone());
                existingLawyer.setColegiatura(lawyer.getColegiatura());
                existingLawyer.setSpecialization(lawyer.getSpecialization());
                existingLawyer.setBio(lawyer.getBio());
                lawyerService.save(existingLawyer);
            } else {
                user.setEnabled(true);
                if (user.getPassword() == null || user.getPassword().isEmpty()) {
                    user.setPassword(user.getDni());
                }
                lawyerService.save(lawyer);
            }
            return "redirect:/admin/abogados?success";
        } catch (com.turnosmart.turnosmart_backend.exception.BusinessException e) {
            model.addAttribute("error", e.getMessage());
            model.addAttribute("lawyer", lawyer);
            return "admin/nuevo-abogado";
        }
    }

    @GetMapping("/abogados/eliminar/{id}")
    public String eliminarAbogado(@PathVariable Long id) {
        lawyerService.delete(id);
        return "redirect:/admin/abogados?deleted";
    }
}