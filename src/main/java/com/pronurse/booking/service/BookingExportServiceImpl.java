package com.pronurse.booking.service;

import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.pronurse.booking.dto.BookingHistoryFilterRequest;
import com.pronurse.booking.dto.BookingHistoryResponse;
import com.pronurse.booking.specification.BookingSpecification;
import com.pronurse.booking.entity.Booking;
import com.pronurse.booking.repository.BookingRepository;
import lombok.RequiredArgsConstructor;
import lombok.extern.slf4j.Slf4j;
import org.apache.commons.csv.CSVFormat;
import org.apache.commons.csv.CSVPrinter;
import org.springframework.data.domain.Sort;
import org.springframework.data.jpa.domain.Specification;
import org.springframework.stereotype.Service;
import org.springframework.transaction.annotation.Transactional;

import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStreamWriter;
import java.math.BigDecimal;
import java.time.format.DateTimeFormatter;
import java.util.List;

@Service
@RequiredArgsConstructor
@Slf4j
public class BookingExportServiceImpl implements BookingExportService {

    private final BookingRepository bookingRepository;
    private final BookingService bookingService;

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPatientHistoryToCSV(String patientMobile, BookingHistoryFilterRequest filter) {
        Specification<Booking> spec = BookingSpecification.buildPatientSpec(patientMobile, filter);
        List<Booking> bookings = bookingRepository.findAll(spec, 
            Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy()));
        
        return generateCSV(bookings, true);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportNurseHistoryToCSV(String nurseMobile, BookingHistoryFilterRequest filter) {
        Specification<Booking> spec = BookingSpecification.buildNurseSpec(nurseMobile, filter);
        List<Booking> bookings = bookingRepository.findAll(spec,
            Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy()));
        
        return generateCSV(bookings, false);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportPatientHistoryToPDF(String patientMobile, BookingHistoryFilterRequest filter) {
        Specification<Booking> spec = BookingSpecification.buildPatientSpec(patientMobile, filter);
        List<Booking> bookings = bookingRepository.findAll(spec,
            Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy()));
        
        return generatePDF(bookings, true, patientMobile);
    }

    @Override
    @Transactional(readOnly = true)
    public byte[] exportNurseHistoryToPDF(String nurseMobile, BookingHistoryFilterRequest filter) {
        Specification<Booking> spec = BookingSpecification.buildNurseSpec(nurseMobile, filter);
        List<Booking> bookings = bookingRepository.findAll(spec,
            Sort.by(Sort.Direction.fromString(filter.getSortDirection()), filter.getSortBy()));
        
        return generatePDF(bookings, false, nurseMobile);
    }

    private byte[] generateCSV(List<Booking> bookings, boolean isPatientView) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream();
             OutputStreamWriter writer = new OutputStreamWriter(out);
             CSVPrinter csvPrinter = new CSVPrinter(writer, CSVFormat.DEFAULT.withHeader(
                     "Booking No", "Date", "Time", "Status", "Payment Status",
                     isPatientView ? "Nurse Name" : "Patient Name",
                     "Services", "Total Amount", "Emergency", "Created At"))) {

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");
            DateTimeFormatter dateTimeFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss");

            for (Booking booking : bookings) {
                BigDecimal total = booking.getSelectedItems().stream()
                        .map(item -> item.getPriceCharged())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                String services = booking.getSelectedItems().stream()
                        .map(item -> item.getItemName())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");

                String otherPartyName = isPatientView
                        ? (booking.getAssignedNurseUser() != null ? booking.getAssignedNurseUser().getName() : "Not Assigned")
                        : booking.getPatientUser().getName();

                csvPrinter.printRecord(
                        booking.getBookingNo(),
                        booking.getBookingDate().format(dateFormatter),
                        booking.getBookingTime(),
                        booking.getBookingStatus(),
                        booking.getPaymentStatus(),
                        otherPartyName,
                        services,
                        total.toString(),
                        booking.getIsEmergency() != null && booking.getIsEmergency() ? "Yes" : "No",
                        booking.getCreatedAt().format(dateTimeFormatter)
                );
            }

            csvPrinter.flush();
            return out.toByteArray();

        } catch (IOException e) {
            log.error("Error generating CSV export", e);
            throw new RuntimeException("Failed to generate CSV export", e);
        }
    }

    private byte[] generatePDF(List<Booking> bookings, boolean isPatientView, String userMobile) {
        try (ByteArrayOutputStream out = new ByteArrayOutputStream()) {
            PdfWriter writer = new PdfWriter(out);
            PdfDocument pdf = new PdfDocument(writer);
            Document document = new Document(pdf);

            // Title
            Paragraph title = new Paragraph("Booking History Report")
                    .setFontSize(20)
                    .setBold()
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(title);

            Paragraph userInfo = new Paragraph("User: " + userMobile)
                    .setFontSize(12)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(userInfo);

            Paragraph generatedDate = new Paragraph("Generated: " + 
                    java.time.LocalDateTime.now().format(DateTimeFormatter.ofPattern("yyyy-MM-dd HH:mm:ss")))
                    .setFontSize(10)
                    .setTextAlignment(TextAlignment.CENTER);
            document.add(generatedDate);

            document.add(new Paragraph("\n"));

            // Table
            float[] columnWidths = {2, 2, 1.5f, 2, 2, 2, 3, 1.5f};
            Table table = new Table(columnWidths);
            table.setWidth(550);

            // Headers
            table.addHeaderCell("Booking No");
            table.addHeaderCell("Date");
            table.addHeaderCell("Time");
            table.addHeaderCell("Status");
            table.addHeaderCell(isPatientView ? "Nurse" : "Patient");
            table.addHeaderCell("Services");
            table.addHeaderCell("Amount");
            table.addHeaderCell("Emergency");

            DateTimeFormatter dateFormatter = DateTimeFormatter.ofPattern("yyyy-MM-dd");

            for (Booking booking : bookings) {
                BigDecimal total = booking.getSelectedItems().stream()
                        .map(item -> item.getPriceCharged())
                        .reduce(BigDecimal.ZERO, BigDecimal::add);

                String services = booking.getSelectedItems().stream()
                        .map(item -> item.getItemName())
                        .reduce((a, b) -> a + ", " + b)
                        .orElse("");

                String otherPartyName = isPatientView
                        ? (booking.getAssignedNurseUser() != null ? booking.getAssignedNurseUser().getName() : "Not Assigned")
                        : booking.getPatientUser().getName();

                table.addCell(booking.getBookingNo());
                table.addCell(booking.getBookingDate().format(dateFormatter));
                table.addCell(booking.getBookingTime());
                table.addCell(booking.getBookingStatus());
                table.addCell(otherPartyName);
                table.addCell(services);
                table.addCell("₹" + total.toString());
                table.addCell(booking.getIsEmergency() != null && booking.getIsEmergency() ? "Yes" : "No");
            }

            document.add(table);

            // Summary
            document.add(new Paragraph("\n"));
            BigDecimal grandTotal = bookings.stream()
                    .flatMap(b -> b.getSelectedItems().stream())
                    .map(item -> item.getPriceCharged())
                    .reduce(BigDecimal.ZERO, BigDecimal::add);

            Paragraph summary = new Paragraph("Total Bookings: " + bookings.size() + 
                    " | Grand Total: ₹" + grandTotal.toString())
                    .setFontSize(12)
                    .setBold()
                    .setTextAlignment(TextAlignment.RIGHT);
            document.add(summary);

            document.close();
            return out.toByteArray();

        } catch (Exception e) {
            log.error("Error generating PDF export", e);
            throw new RuntimeException("Failed to generate PDF export", e);
        }
    }
}

// Made with Bob
