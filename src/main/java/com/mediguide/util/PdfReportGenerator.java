package com.mediguide.util;

import com.itextpdf.text.*;
import com.itextpdf.text.pdf.*;
import com.itextpdf.text.pdf.draw.LineSeparator;
import com.mediguide.models.MedicalRecord;
import com.mediguide.models.Patient;
import com.mediguide.models.RiskAssessmentResult;

import java.io.File;
import java.io.FileOutputStream;
import java.text.SimpleDateFormat;
import java.util.Date;

/**
 * PDF Report Generator for MediGuide AI using iTextPDF.
 * Creates formatted, official-looking clinical decision support reports.
 */
public class PdfReportGenerator {

    private static final Font TITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 18, BaseColor.DARK_GRAY);
    private static final Font SUBTITLE_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.GRAY);
    private static final Font SECTION_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, new BaseColor(14, 116, 144)); // Teal
    private static final Font BOLD_FONT = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 10, BaseColor.BLACK);
    private static final Font NORMAL_FONT = FontFactory.getFont(FontFactory.HELVETICA, 10, BaseColor.BLACK);
    private static final Font DISCLAIMER_FONT = FontFactory.getFont(FontFactory.HELVETICA, 8, Font.ITALIC, BaseColor.GRAY);

    public static File generateReport(Patient patient, MedicalRecord record, RiskAssessmentResult result, String outputDirectory) throws Exception {
        File dir = new File(outputDirectory);
        if (!dir.exists()) {
            dir.mkdirs();
        }

        String timeStamp = new SimpleDateFormat("yyyyMMdd_HHmmss").format(new Date());
        String fileName = "MediGuide_Report_" + patient.getPatientId() + "_" + timeStamp + ".pdf";
        File pdfFile = new File(dir, fileName);

        Document document = new Document(PageSize.A4, 36, 36, 36, 36);
        PdfWriter.getInstance(document, new FileOutputStream(pdfFile));

        document.open();

        // 1. Header Banner
        PdfPTable headerTable = new PdfPTable(2);
        headerTable.setWidthPercentage(100);
        headerTable.setWidths(new float[]{3, 1});

        PdfPCell titleCell = new PdfPCell();
        titleCell.setBorder(Rectangle.NO_BORDER);
        titleCell.addElement(new Paragraph("MediGuide AI — Clinical Assessment Report", TITLE_FONT));
        titleCell.addElement(new Paragraph("Explainable AI & Hybrid Decision Support System", SUBTITLE_FONT));

        PdfPCell dateCell = new PdfPCell();
        dateCell.setBorder(Rectangle.NO_BORDER);
        dateCell.setHorizontalAlignment(Element.ALIGN_RIGHT);
        dateCell.addElement(new Paragraph("Date: " + new SimpleDateFormat("yyyy-MM-dd HH:mm").format(new Date()), NORMAL_FONT));
        dateCell.addElement(new Paragraph("Report ID: RPT-" + System.currentTimeMillis() % 100000, SUBTITLE_FONT));

        headerTable.addCell(titleCell);
        headerTable.addCell(dateCell);
        document.add(headerTable);

        document.add(new Chunk(new LineSeparator(1f, 100f, new BaseColor(226, 232, 240), Element.ALIGN_CENTER, -5)));
        document.add(Chunk.NEWLINE);

        // 2. Patient Demographics & Record Info Table
        document.add(new Paragraph("PATIENT & RECORD DEMOGRAPHICS", SECTION_FONT));
        document.add(Chunk.NEWLINE);

        PdfPTable patientTable = new PdfPTable(4);
        patientTable.setWidthPercentage(100);
        patientTable.setSpacingAfter(10f);

        addTableCell(patientTable, "Patient Name:", BOLD_FONT);
        addTableCell(patientTable, patient.getName(), NORMAL_FONT);
        addTableCell(patientTable, "Patient ID:", BOLD_FONT);
        addTableCell(patientTable, String.valueOf(patient.getPatientId()), NORMAL_FONT);

        addTableCell(patientTable, "Age / Gender:", BOLD_FONT);
        addTableCell(patientTable, patient.getAge() + " yrs / " + patient.getGender(), NORMAL_FONT);
        addTableCell(patientTable, "Contact / Phone:", BOLD_FONT);
        addTableCell(patientTable, patient.getPhone() != null ? patient.getPhone() : "N/A", NORMAL_FONT);

        addTableCell(patientTable, "Blood Pressure:", BOLD_FONT);
        addTableCell(patientTable, record != null ? record.getBloodPressureString() : "N/A", NORMAL_FONT);
        addTableCell(patientTable, "Fasting Glucose:", BOLD_FONT);
        addTableCell(patientTable, record != null ? record.getGlucose() + " mg/dL" : "N/A", NORMAL_FONT);

        addTableCell(patientTable, "Total Cholesterol:", BOLD_FONT);
        addTableCell(patientTable, record != null ? record.getCholesterol() + " mg/dL" : "N/A", NORMAL_FONT);
        addTableCell(patientTable, "Serum Creatinine:", BOLD_FONT);
        addTableCell(patientTable, record != null ? record.getCreatinine() + " mg/dL" : "N/A", NORMAL_FONT);

        addTableCell(patientTable, "Body Mass Index:", BOLD_FONT);
        addTableCell(patientTable, record != null ? String.format("%.1f kg/m²", record.getBmi()) : "N/A", NORMAL_FONT);
        addTableCell(patientTable, "Family History:", BOLD_FONT);
        addTableCell(patientTable, record != null && record.getFamilyHistory() != null ? record.getFamilyHistory() : "None", NORMAL_FONT);

        document.add(patientTable);

        // 3. Risk Assessment Summary (Hybrid ML + Rule)
        document.add(new Paragraph("HYBRID AI ASSESSMENT SUMMARY", SECTION_FONT));
        document.add(Chunk.NEWLINE);

        PdfPTable summaryTable = new PdfPTable(2);
        summaryTable.setWidthPercentage(100);

        BaseColor riskColor = BaseColor.GREEN;
        if ("High".equalsIgnoreCase(result.getOverallRiskLevel())) riskColor = BaseColor.RED;
        else if ("Medium".equalsIgnoreCase(result.getOverallRiskLevel())) riskColor = BaseColor.ORANGE;

        Font riskFont = FontFactory.getFont(FontFactory.HELVETICA_BOLD, 12, riskColor);

        addTableCell(summaryTable, "Primary Evaluated Condition:", BOLD_FONT);
        addTableCell(summaryTable, result.getPrimaryCondition(), NORMAL_FONT);

        addTableCell(summaryTable, "Overall Assessed Risk Level:", BOLD_FONT);
        addTableCell(summaryTable, result.getOverallRiskLevel().toUpperCase(), riskFont);

        addTableCell(summaryTable, "Confidence Score:", BOLD_FONT);
        addTableCell(summaryTable, String.format("%.1f%%", result.getConfidenceScore()), NORMAL_FONT);

        addTableCell(summaryTable, "Decision Strategy:", BOLD_FONT);
        addTableCell(summaryTable, result.getFusionStrategy() != null ? result.getFusionStrategy() : "Hybrid ML (60%) + Rule (40%)", NORMAL_FONT);

        addTableCell(summaryTable, "Weka ML Model Prediction:", BOLD_FONT);
        addTableCell(summaryTable, result.getMlPrediction() != null ? result.getMlPrediction() : "N/A", NORMAL_FONT);

        addTableCell(summaryTable, "Rule Engine Risk Level:", BOLD_FONT);
        addTableCell(summaryTable, result.getRulePrediction() != null ? result.getRulePrediction() : "N/A", NORMAL_FONT);

        document.add(summaryTable);
        document.add(Chunk.NEWLINE);

        // 4. Contributing Risk Factors & Explanations
        document.add(new Paragraph("CONTRIBUTING RISK FACTORS & CLINICAL EXPLANATIONS", SECTION_FONT));
        document.add(Chunk.NEWLINE);

        com.itextpdf.text.List factorList = new com.itextpdf.text.List(com.itextpdf.text.List.UNORDERED);
        factorList.setListSymbol("• ");

        if (result.getTopRiskFactors() != null && !result.getTopRiskFactors().isEmpty()) {
            for (String factor : result.getTopRiskFactors()) {
                factorList.add(new ListItem(factor, NORMAL_FONT));
            }
        } else {
            factorList.add(new ListItem("No critical risk factors identified.", NORMAL_FONT));
        }

        if (result.getExplanations() != null && !result.getExplanations().isEmpty()) {
            for (String exp : result.getExplanations()) {
                factorList.add(new ListItem(exp, NORMAL_FONT));
            }
        }
        document.add(factorList);
        document.add(Chunk.NEWLINE);

        // 5. Clinical Recommendations
        document.add(new Paragraph("ACTIONABLE CLINICAL RECOMMENDATIONS", SECTION_FONT));
        document.add(Chunk.NEWLINE);

        com.itextpdf.text.List recList = new com.itextpdf.text.List(com.itextpdf.text.List.ORDERED);
        if (result.getRecommendations() != null && !result.getRecommendations().isEmpty()) {
            for (String rec : result.getRecommendations()) {
                recList.add(new ListItem(rec, NORMAL_FONT));
            }
        } else {
            recList.add(new ListItem("Routine clinical monitoring advised.", NORMAL_FONT));
        }
        document.add(recList);
        document.add(Chunk.NEWLINE);

        // 6. Medical Disclaimer & Footer
        document.add(new Chunk(new LineSeparator(1f, 100f, new BaseColor(226, 232, 240), Element.ALIGN_CENTER, 5)));
        Paragraph disclaimer = new Paragraph(
                "DISCLAIMER: MediGuide AI is a decision-support prototype system for educational and clinical research demonstration. " +
                "All outputs must be reviewed and verified by a licensed healthcare professional before making any medical decisions.",
                DISCLAIMER_FONT
        );
        disclaimer.setSpacingBefore(10f);
        document.add(disclaimer);

        document.close();
        return pdfFile;
    }

    private static void addTableCell(PdfPTable table, String text, Font font) {
        PdfPCell cell = new PdfPCell(new Phrase(text, font));
        cell.setPadding(6f);
        cell.setBorderColor(new BaseColor(226, 232, 240));
        table.addCell(cell);
    }
}
