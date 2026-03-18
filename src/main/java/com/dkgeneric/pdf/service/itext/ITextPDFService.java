package com.dkgeneric.pdf.service.itext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.List;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import com.dkgeneric.commons.common.ApplicationValue;
import com.dkgeneric.pdf.model.PDFDocument;
import com.dkgeneric.pdf.model.itext.ITextPDFDocument;
import com.dkgeneric.pdf.service.PDFService;
import com.fasterxml.jackson.databind.JsonNode;
import com.itextpdf.html2pdf.ConverterProperties;
import com.itextpdf.html2pdf.HtmlConverter;
import com.itextpdf.io.font.FontProgramFactory;
import com.itextpdf.io.image.ImageDataFactory;
import com.itextpdf.io.image.TiffImageData;
import com.itextpdf.kernel.exceptions.PdfException;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.element.Image;
import com.itextpdf.layout.font.FontProvider;
import com.itextpdf.licensing.base.LicenseKey;
import com.itextpdf.styledxmlparser.resolver.font.BasicFontProvider;

import jakarta.annotation.PostConstruct;
import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(name = "dkgeneric.pdflib.type", havingValue = "itext7")

public class ITextPDFService implements PDFService {

	@ApplicationValue(key = "iTextLicense")
	private JsonNode license;

	private ByteArrayOutputStream convertImage(byte[] data, PageSize pageSize) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PdfDocument pdfDocument = new PdfDocument(new PdfWriter(baos));) {
			PdfPage pdfPage = pdfDocument.addNewPage(pageSize);
			Image image = new Image(ImageDataFactory.create(data));
			scaleImage(image, pdfPage);
			try (Canvas canvas = new Canvas(pdfPage, pdfPage.getMediaBox())) {
				canvas.add(image);
			}
			return baos;
		}
	}

	private ByteArrayOutputStream convertTiff(byte[] data, PageSize pageSize) throws IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();
				PdfDocument pdfDocument = new PdfDocument(new PdfWriter(baos));) {
			int numberOfPages = TiffImageData.getNumberOfPages(data);

			for (int i = 1; i <= numberOfPages; i++) {
				PdfPage pdfPage = pdfDocument.addNewPage(pageSize);
				Image tiffImage = new Image(ImageDataFactory.createTiff(data, false, i, false));
				scaleImage(tiffImage, pdfPage);
				try (Canvas canvas = new Canvas(pdfPage, pdfPage.getMediaBox())) {
					canvas.add(tiffImage);
				}
			}
			return baos;
		}
	}

	@Override
	public PDFDocument convertToPDF(InputStream is, String contentType) throws PdfException, IOException {
		return convertToPDF(is, contentType, PDFDocument.LETTER_SIZE);
	}

	@Override
	public PDFDocument convertToPDF(InputStream is, String contentType, String pageSize)
			throws PdfException, IOException {
		return new ITextPDFDocument(new PdfDocument(new PdfReader(convertToPDFStream(is, contentType, pageSize))));
	}

	@Override
	public PDFDocument convertToPDF(InputStream is, String contentType, String pageSize,
			Map<String, Object> conversionProperties) throws Exception {
		return new ITextPDFDocument(
				new PdfDocument(new PdfReader(convertToPDFStream(is, contentType, pageSize, conversionProperties))));
	}

	@Override
	public InputStream convertToPDFStream(InputStream is, String contentType) throws PdfException, IOException {
		return convertToPDFStream(is, contentType, PDFDocument.LETTER_SIZE);
	}

	@Override
	public InputStream convertToPDFStream(InputStream is, String contentType, String pageSize)
			throws PdfException, IOException {
		return convertToPDFStream(is, contentType, pageSize, null);
	}

	@Override
	public InputStream convertToPDFStream(InputStream is, String contentType, String pageSize,
			Map<String, Object> conversionProperties) throws PdfException, IOException {
		if ("application/pdf".equalsIgnoreCase(contentType)) {
			return is;
		} else if ("image/tiff".equalsIgnoreCase(contentType)) {
			try (ByteArrayOutputStream dataStream = new ByteArrayOutputStream()) {
				FileCopyUtils.copy(is, dataStream);
				return new ByteArrayInputStream(
						convertTiff(dataStream.toByteArray(), ITextPDFDocument.getPageSize(pageSize)).toByteArray());
			}
		} else if (contentType != null && contentType.toLowerCase().startsWith("image")) {
			try (ByteArrayOutputStream dataStream = new ByteArrayOutputStream()) {
				FileCopyUtils.copy(is, dataStream);
				return new ByteArrayInputStream(
						convertImage(dataStream.toByteArray(), ITextPDFDocument.getPageSize(pageSize)).toByteArray());
			}

		} else if ("text/html".equalsIgnoreCase(contentType)) {
			ByteArrayOutputStream baos = new ByteArrayOutputStream();
			try (PdfDocument pdf = new PdfDocument(new PdfWriter(baos));) {
				ConverterProperties properties = createConverterProperties(conversionProperties);
				pdf.setDefaultPageSize(ITextPDFDocument.getPageSize(pageSize));
				HtmlConverter.convertToPdf(is, pdf, properties);
			}
			return new ByteArrayInputStream(baos.toByteArray());
		} else
			throw new PdfException("Content type " + contentType + " is not supported.");

	}

	private ConverterProperties createConverterProperties(Map<String, Object> conversionProperties) throws IOException {
		ConverterProperties properties = new ConverterProperties();
		if (conversionProperties != null) {
			FontProgramFactory.clearRegisteredFonts();
			@SuppressWarnings("unchecked")
			List<String> fontList = (List<String>) conversionProperties.get(FONT_FILE_NAMES);
			if (fontList != null) {
				FontProvider fontProvider = new BasicFontProvider();
				for (String name : fontList)
					fontProvider.addFont(FontProgramFactory.createFont(name));
				properties.setFontProvider(fontProvider);
			}
		}
		return properties;
	}

	@Override
	public PDFDocument createDocument(InputStream is) throws PdfException, IOException {
		return new ITextPDFDocument(new PdfDocument(new PdfReader(is)));
	}

	@Override
	public PDFDocument createDocument(OutputStream os, String pageSize) throws PdfException, IOException {
		return new ITextPDFDocument(new PdfDocument(new PdfWriter(os)), pageSize);
	}

	@Override
	public String getAPIType() {
		return API_TYPE_ITEXT7;
	}

	@PostConstruct
	public void init() {
		if (license != null) {
			log.debug("Loading iText license.");
			LicenseKey.loadLicenseFile(new ByteArrayInputStream(license.toString().getBytes()));
			log.debug("iText license loaded successfully.");
		}
		else
			log.warn("No iText license configured. Work with AGPL license.");
	}

	private void scaleImage(Image img, PdfPage page) {
		Rectangle rectangle = page.getMediaBox();
		float scale = Math.max(img.getImageWidth() / rectangle.getWidth(),
				img.getImageHeight() / rectangle.getHeight());
		if (scale > 1)
			img.scaleToFit(img.getImageWidth() / scale, img.getImageHeight() / scale);
		if (img.getImageScaledWidth() < rectangle.getWidth())
			img.setMarginLeft((rectangle.getWidth() - img.getImageScaledWidth()) / 2);
		if (img.getImageScaledHeight() < rectangle.getHeight())
			img.setMarginTop((rectangle.getHeight() - img.getImageScaledHeight()) / 2);
	}
}
