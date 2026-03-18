package com.dkgeneric.pdf.service.pdfbox;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDType1Font;
import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;
import org.xhtmlrenderer.pdf.ITextRenderer;

import com.dkgeneric.pdf.model.PDFDocument;
import com.dkgeneric.pdf.model.PDFProcessingException;
import com.dkgeneric.pdf.model.pdfbox.PDFBoxPDFDocument;
import com.dkgeneric.pdf.service.PDFService;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "dkgeneric.pdflib", name = "type", havingValue = "pdfbox")
public class PDFBoxPDFService implements PDFService {

	private static Map<String, PDFont> fonts = new HashMap<>();
	static {
		fonts.put("Helvetica-Bold", PDType1Font.HELVETICA_BOLD);
		fonts.put("Helvetica", PDType1Font.HELVETICA);
	}

	public static PDFont getFont(String name) {
		return fonts.getOrDefault(name, PDType1Font.HELVETICA);
	}

	private ByteArrayOutputStream convertHtml(byte[] data) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
			ITextRenderer renderer = new ITextRenderer();
			renderer.setDocumentFromString(new String(data));
			renderer.layout();
			renderer.createPDF(baos);
			return baos;
		}
	}

	@Override
	public PDFDocument convertToPDF(InputStream is, String contentType) throws Exception {
		return convertToPDF(is, contentType, PDFDocument.LETTER_SIZE);
	}

	@Override
	public PDFDocument convertToPDF(InputStream is, String contentType, String pageSize) throws Exception {
		return new PDFBoxPDFDocument(PDDocument.load(convertToPDFStream(is, contentType, pageSize)));
	}

	@Override
	public PDFDocument convertToPDF(InputStream is, String contentType, String pageSize,
			Map<String, Object> conversionProperties) throws Exception {
		return new PDFBoxPDFDocument(
				PDDocument.load(convertToPDFStream(is, contentType, pageSize, conversionProperties)));
	}

	@Override
	public InputStream convertToPDFStream(InputStream is, String contentType) throws Exception {
		return convertToPDFStream(is, contentType, PDFDocument.LETTER_SIZE);
	}

	@Override
	public InputStream convertToPDFStream(InputStream is, String contentType, String pageSize) throws Exception {
		return convertToPDFStream(is, contentType, pageSize, null);
	}

	@Override
	public InputStream convertToPDFStream(InputStream is, String contentType, String pageSize,
			Map<String, Object> conversionProperties) throws Exception {
		if ("application/pdf".equalsIgnoreCase(contentType)) {
			return is;

		} else if ("text/html".equalsIgnoreCase(contentType)) {
			try (ByteArrayOutputStream dataStream = new ByteArrayOutputStream()) {
				FileCopyUtils.copy(is, dataStream);
				return new ByteArrayInputStream(convertHtml(dataStream.toByteArray()).toByteArray());
			}
		} else if ("image/tiff".equalsIgnoreCase(contentType)) {
			return new ImageConversionUtils(PDFBoxPDFDocument.getPageSize(pageSize)).convertTiffToPdfUsingPdfBox(is);
		} else if (contentType != null && contentType.toLowerCase().startsWith("image")) {
			return new ImageConversionUtils(PDFBoxPDFDocument.getPageSize(pageSize)).convertImageToPdfUsingPdfBox(is);
		} else
			throw new PDFProcessingException("Content type " + contentType + " is not supported.");
	}

	@Override
	public PDFDocument createDocument(InputStream is) throws IOException {
		return new PDFBoxPDFDocument(PDDocument.load(is));
	}

	@Override
	public PDFDocument createDocument(OutputStream os, String pageSize) throws IOException {
		return new PDFBoxPDFDocument(new PDDocument(), os);
	}

	@Override
	public String getAPIType() {
		return API_TYPE_PDFBOX;
	}

}
