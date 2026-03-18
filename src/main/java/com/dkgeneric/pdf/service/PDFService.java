package com.dkgeneric.pdf.service;

import java.io.InputStream;
import java.io.OutputStream;
import java.util.Map;

import com.dkgeneric.pdf.model.PDFDocument;

public interface PDFService {

	public static final String FONT_FILE_NAMES = "fontFileNames";

	public static final String API_TYPE_ITEXT7 = "itext7";
	public static final String API_TYPE_ITEXT5 = "itext5";
	public static final String API_TYPE_PDFBOX = "pdfbox";

	public PDFDocument convertToPDF(InputStream is, String contentType) throws Exception;

	public PDFDocument convertToPDF(InputStream is, String contentType, String pageSize) throws Exception;

	public PDFDocument convertToPDF(InputStream is, String contentType, String pageSize,
			Map<String, Object> conversionProperties) throws Exception;

	public InputStream convertToPDFStream(InputStream is, String contentType) throws Exception;

	public InputStream convertToPDFStream(InputStream is, String contentType, String pageSize) throws Exception;

	public InputStream convertToPDFStream(InputStream is, String contentType, String pageSize,
			Map<String, Object> conversionProperties) throws Exception;

	public PDFDocument createDocument(InputStream is) throws Exception;

	public PDFDocument createDocument(OutputStream is, String pageSize) throws Exception;
	
	public String getAPIType();
}
