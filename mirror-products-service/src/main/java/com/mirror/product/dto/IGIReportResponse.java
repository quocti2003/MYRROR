package com.mirror.product.dto;

import com.fasterxml.jackson.annotation.JsonProperty;
import java.time.LocalDateTime;

/**
 * DTO for IGI (International Gemological Institute) Diamond Report
 * Maps the response from api.igi.org/ReportDetail.php
 */
public class IGIReportResponse {

    @JsonProperty("REPORT NUMBER")
    private String reportNumber;

    @JsonProperty("REPORT DATE")
    private String reportDate;

    @JsonProperty("DESCRIPTION")
    private String description;

    @JsonProperty("SHAPE AND CUT")
    private String shapeAndCut;

    @JsonProperty("CARAT WEIGHT")
    private String caratWeight;

    @JsonProperty("COLOR GRADE")
    private String colorGrade;

    @JsonProperty("CLARITY GRADE")
    private String clarityGrade;

    @JsonProperty("CUT GRADE")
    private String cutGrade;

    @JsonProperty("POLISH")
    private String polish;

    @JsonProperty("SYMMETRY")
    private String symmetry;

    @JsonProperty("Measurements")
    private String measurements;

    @JsonProperty("Table Size")
    private String tableSize;

    @JsonProperty("Crown Height")
    private String crownHeight;

    @JsonProperty("Pavilion Depth")
    private String pavilionDepth;

    @JsonProperty("Girdle Thickness")
    private String girdleThickness;

    @JsonProperty("Culet")
    private String culet;

    @JsonProperty("Total Depth")
    private String totalDepth;

    @JsonProperty("FLUORESCENCE")
    private String fluorescence;

    @JsonProperty("COMMENTS")
    private String comments;

    @JsonProperty("Inscription(s)")
    private String inscriptions;

    @JsonProperty("REPORT_SUF")
    private String reportSuf;

    @JsonProperty("PDF_FLAG")
    private String pdfFlag;

    @JsonProperty("REPORT1_PDF")
    private String report1Pdf;

    @JsonProperty("REPORT2_PDF")
    private String report2Pdf;

    @JsonProperty("GOODS_FLAG")
    private String goodsFlag;

    @JsonProperty("HNA_FLAG")
    private String hnaFlag;

    @JsonProperty("LOCATION_MST_ID")
    private Integer locationMstId;

    @JsonProperty("REPORT_TYPE")
    private Integer reportType;

    @JsonProperty("REPORT_FORMAT")
    private Integer reportFormat;

    @JsonProperty("REPORT_VIDEO")
    private String reportVideo;

    @JsonProperty("REPORT_IMAGE")
    private String reportImage;

    // Additional fields for our system
    private LocalDateTime fetchedAt;
    private String source;
    private boolean fromCache;

    // Getters and Setters
    public String getReportNumber() {
        return reportNumber;
    }

    public void setReportNumber(String reportNumber) {
        this.reportNumber = reportNumber;
    }

    public String getReportDate() {
        return reportDate;
    }

    public void setReportDate(String reportDate) {
        this.reportDate = reportDate;
    }

    public String getDescription() {
        return description;
    }

    public void setDescription(String description) {
        this.description = description;
    }

    public String getShapeAndCut() {
        return shapeAndCut;
    }

    public void setShapeAndCut(String shapeAndCut) {
        this.shapeAndCut = shapeAndCut;
    }

    public String getCaratWeight() {
        return caratWeight;
    }

    public void setCaratWeight(String caratWeight) {
        this.caratWeight = caratWeight;
    }

    public String getColorGrade() {
        return colorGrade;
    }

    public void setColorGrade(String colorGrade) {
        this.colorGrade = colorGrade;
    }

    public String getClarityGrade() {
        return clarityGrade;
    }

    public void setClarityGrade(String clarityGrade) {
        this.clarityGrade = clarityGrade;
    }

    public String getCutGrade() {
        return cutGrade;
    }

    public void setCutGrade(String cutGrade) {
        this.cutGrade = cutGrade;
    }

    public String getPolish() {
        return polish;
    }

    public void setPolish(String polish) {
        this.polish = polish;
    }

    public String getSymmetry() {
        return symmetry;
    }

    public void setSymmetry(String symmetry) {
        this.symmetry = symmetry;
    }

    public String getMeasurements() {
        return measurements;
    }

    public void setMeasurements(String measurements) {
        this.measurements = measurements;
    }

    public String getTableSize() {
        return tableSize;
    }

    public void setTableSize(String tableSize) {
        this.tableSize = tableSize;
    }

    public String getCrownHeight() {
        return crownHeight;
    }

    public void setCrownHeight(String crownHeight) {
        this.crownHeight = crownHeight;
    }

    public String getPavilionDepth() {
        return pavilionDepth;
    }

    public void setPavilionDepth(String pavilionDepth) {
        this.pavilionDepth = pavilionDepth;
    }

    public String getGirdleThickness() {
        return girdleThickness;
    }

    public void setGirdleThickness(String girdleThickness) {
        this.girdleThickness = girdleThickness;
    }

    public String getCulet() {
        return culet;
    }

    public void setCulet(String culet) {
        this.culet = culet;
    }

    public String getTotalDepth() {
        return totalDepth;
    }

    public void setTotalDepth(String totalDepth) {
        this.totalDepth = totalDepth;
    }

    public String getFluorescence() {
        return fluorescence;
    }

    public void setFluorescence(String fluorescence) {
        this.fluorescence = fluorescence;
    }

    public String getComments() {
        return comments;
    }

    public void setComments(String comments) {
        this.comments = comments;
    }

    public String getInscriptions() {
        return inscriptions;
    }

    public void setInscriptions(String inscriptions) {
        this.inscriptions = inscriptions;
    }

    public String getReportSuf() {
        return reportSuf;
    }

    public void setReportSuf(String reportSuf) {
        this.reportSuf = reportSuf;
    }

    public String getPdfFlag() {
        return pdfFlag;
    }

    public void setPdfFlag(String pdfFlag) {
        this.pdfFlag = pdfFlag;
    }

    public String getReport1Pdf() {
        return report1Pdf;
    }

    public void setReport1Pdf(String report1Pdf) {
        this.report1Pdf = report1Pdf;
    }

    public String getReport2Pdf() {
        return report2Pdf;
    }

    public void setReport2Pdf(String report2Pdf) {
        this.report2Pdf = report2Pdf;
    }

    public String getGoodsFlag() {
        return goodsFlag;
    }

    public void setGoodsFlag(String goodsFlag) {
        this.goodsFlag = goodsFlag;
    }

    public String getHnaFlag() {
        return hnaFlag;
    }

    public void setHnaFlag(String hnaFlag) {
        this.hnaFlag = hnaFlag;
    }

    public Integer getLocationMstId() {
        return locationMstId;
    }

    public void setLocationMstId(Integer locationMstId) {
        this.locationMstId = locationMstId;
    }

    public Integer getReportType() {
        return reportType;
    }

    public void setReportType(Integer reportType) {
        this.reportType = reportType;
    }

    public Integer getReportFormat() {
        return reportFormat;
    }

    public void setReportFormat(Integer reportFormat) {
        this.reportFormat = reportFormat;
    }

    public String getReportVideo() {
        return reportVideo;
    }

    public void setReportVideo(String reportVideo) {
        this.reportVideo = reportVideo;
    }

    public String getReportImage() {
        return reportImage;
    }

    public void setReportImage(String reportImage) {
        this.reportImage = reportImage;
    }

    public LocalDateTime getFetchedAt() {
        return fetchedAt;
    }

    public void setFetchedAt(LocalDateTime fetchedAt) {
        this.fetchedAt = fetchedAt;
    }

    public String getSource() {
        return source;
    }

    public void setSource(String source) {
        this.source = source;
    }

    public boolean isFromCache() {
        return fromCache;
    }

    public void setFromCache(boolean fromCache) {
        this.fromCache = fromCache;
    }

    // Helper method to check if this is a lab-grown diamond
    public boolean isLabGrown() {
        return description != null && description.toUpperCase().contains("LABORATORY GROWN");
    }

    // Helper method to get numeric carat weight
    public Double getCaratWeightNumeric() {
        if (caratWeight == null) return null;
        try {
            return Double.parseDouble(caratWeight.replaceAll("[^0-9.]", ""));
        } catch (NumberFormatException e) {
            return null;
        }
    }
}
