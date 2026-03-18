package com.dkgeneric.pdf.service.itext5;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.util.HashMap;
import java.util.Map;

import org.springframework.boot.autoconfigure.condition.ConditionalOnProperty;
import org.springframework.stereotype.Component;
import org.springframework.util.FileCopyUtils;

import com.dkgeneric.pdf.model.PDFDocument;
import com.dkgeneric.pdf.model.itext5.IText5PDFDocument;
import com.dkgeneric.pdf.service.PDFService;
import com.itextpdf.kernel.exceptions.PdfException;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Font.FontFamily;
import com.itextpdf.text.Image;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.io.RandomAccessSourceFactory;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.RandomAccessFileOrArray;
import com.itextpdf.text.pdf.codec.TiffImage;
import com.itextpdf.tool.xml.XMLWorkerHelper;

import lombok.extern.slf4j.Slf4j;

@Slf4j
@Component
@ConditionalOnProperty(prefix = "dkgeneric.pdflib", name = "type", havingValue = "itext5")
public class IText5PDFService implements PDFService {

	private static Map<String, FontFamily> fonts = new HashMap<>();
	static {
		fonts.put("Helvetica-Bold", FontFamily.HELVETICA);
		fonts.put("Helvetica", FontFamily.HELVETICA);
	}

	public static FontFamily getFontFamily(String name) {
		return fonts.getOrDefault(name, FontFamily.HELVETICA);
	}

	private ByteArrayOutputStream convertHtml(InputStream is, Rectangle pageSize)
			throws DocumentException, IOException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
			Document document = new Document(pageSize);
			PdfWriter pdfWriter = PdfWriter.getInstance(document, baos);
			document.open();
			XMLWorkerHelper.getInstance().parseXHtml(pdfWriter, document, is);
			document.close();
			return baos;
		}
	}

	private ByteArrayOutputStream convertImage(byte[] data, Rectangle pageSize) throws DocumentException, IOException {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Document document = new Document(pageSize);
		PdfWriter.getInstance(document, baos);
		Image image = Image.getInstance(data);
		scaleImage(image, pageSize);
		document.open();
		document.add(image);
		document.close();
		return baos;
	}

	private ByteArrayOutputStream convertTiff(byte[] data, Rectangle pageSize) throws IOException, DocumentException {
		try (ByteArrayOutputStream baos = new ByteArrayOutputStream();) {
			Document document = new Document(pageSize);
			PdfWriter.getInstance(document, baos);

			RandomAccessSourceFactory randomAccessSourceFactory = new RandomAccessSourceFactory();
			RandomAccessFileOrArray ra = null;
			ra = new RandomAccessFileOrArray(randomAccessSourceFactory.createSource(data));

			int numberOfPages = TiffImage.getNumberOfPages(ra);
			document.open();

			for (int i = 1; i <= numberOfPages; i++) {
				Image img = TiffImage.getTiffImage(ra, i);
				scaleImage(img, pageSize);
				document.add(img);
			}
			document.close();
			return baos;
		}
	}

	@Override
	public PDFDocument convertToPDF(InputStream is, String contentType) throws Exception {
		return convertToPDF(is, contentType, PDFDocument.LETTER_SIZE);
	}
	@Override
	public PDFDocument convertToPDF(InputStream is, String contentType, String pageSize) throws Exception {
		return new IText5PDFDocument(IText5PDFDocument.getPageSizeRectangle(pageSize),
				new PdfReader(convertToPDFStream(is, contentType, pageSize)));
	}

	@Override
	public PDFDocument convertToPDF(InputStream is, String contentType, String pageSize,
			Map<String, Object> conversionProperties) throws Exception {
		return new IText5PDFDocument(IText5PDFDocument.getPageSizeRectangle(pageSize),
				new PdfReader(convertToPDFStream(is, contentType, pageSize, conversionProperties)));
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
		} else if ("image/tiff".equalsIgnoreCase(contentType)) {
			try (ByteArrayOutputStream dataStream = new ByteArrayOutputStream()) {
				FileCopyUtils.copy(is, dataStream);
				return new ByteArrayInputStream(
						convertTiff(dataStream.toByteArray(), IText5PDFDocument.getPageSizeRectangle(pageSize))
								.toByteArray());
			}
		} else if (contentType != null && contentType.toLowerCase().startsWith("image")) {
			try (ByteArrayOutputStream dataStream = new ByteArrayOutputStream()) {
				FileCopyUtils.copy(is, dataStream);
				return new ByteArrayInputStream(
						convertImage(dataStream.toByteArray(), IText5PDFDocument.getPageSizeRectangle(pageSize))
								.toByteArray());
			}

		} else if ("text/html".equalsIgnoreCase(contentType))
			return new ByteArrayInputStream(
					convertHtml(is, IText5PDFDocument.getPageSizeRectangle(pageSize)).toByteArray());
		else
			throw new PdfException("Content type " + contentType + " is not supported.");
	}

	@Override
	public PDFDocument createDocument(InputStream is) throws Exception {
		return new IText5PDFDocument(IText5PDFDocument.getPageSizeRectangle(null),
				new com.itextpdf.text.pdf.PdfReader(is));
	}

	@Override
	public PDFDocument createDocument(OutputStream os, String pageSize) throws Exception {
		return new IText5PDFDocument(IText5PDFDocument.getPageSizeRectangle(pageSize), os);
	}

	@Override
	public String getAPIType() {
		return API_TYPE_ITEXT5;
	}

	private void scaleImage(Image img, Rectangle rectangle) {
		float scale = Math.max(img.getWidth() / rectangle.getWidth(), img.getHeight() / rectangle.getHeight());
		if (scale > 1)
			img.scaleToFit(img.getWidth() / scale, img.getHeight() / scale);
		if (img.getWidth() < rectangle.getWidth())
			img.setAbsolutePosition((rectangle.getWidth() - img.getWidth()) / 2,
					(rectangle.getHeight() - img.getHeight()) / 2);
	}
}
