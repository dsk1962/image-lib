package com.dkgeneric.pdf.model.itext5;

import java.awt.Color;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;

import org.springframework.util.CollectionUtils;
import org.springframework.util.FileCopyUtils;
import org.springframework.util.StringUtils;

import com.dkgeneric.pdf.model.Bookmark;
import com.dkgeneric.pdf.model.CellFont;
import com.dkgeneric.pdf.model.CellRenderEvent;
import com.dkgeneric.pdf.model.CellRenderer;
import com.dkgeneric.pdf.model.PDFDocument;
import com.dkgeneric.pdf.model.PDFPage;
import com.dkgeneric.pdf.model.PDFProcessingException;
import com.dkgeneric.pdf.model.PageNavigationData;
import com.dkgeneric.pdf.model.TOCCellRenderer;
import com.dkgeneric.pdf.model.TOCDefinition;
import com.dkgeneric.pdf.model.TOCEntry;
import com.dkgeneric.pdf.model.TableDefinition;
import com.dkgeneric.pdf.model.TextFont;
import com.dkgeneric.pdf.model.TextPosition;
import com.dkgeneric.pdf.service.itext5.IText5PDFService;
import com.itextpdf.text.BaseColor;
import com.itextpdf.text.Document;
import com.itextpdf.text.DocumentException;
import com.itextpdf.text.Element;
import com.itextpdf.text.Font;
import com.itextpdf.text.PageSize;
import com.itextpdf.text.Paragraph;
import com.itextpdf.text.Phrase;
import com.itextpdf.text.Rectangle;
import com.itextpdf.text.pdf.ColumnText;
import com.itextpdf.text.pdf.PdfAction;
import com.itextpdf.text.pdf.PdfAnnotation;
import com.itextpdf.text.pdf.PdfBorderArray;
import com.itextpdf.text.pdf.PdfContentByte;
import com.itextpdf.text.pdf.PdfDestination;
import com.itextpdf.text.pdf.PdfPCell;
import com.itextpdf.text.pdf.PdfPCellEvent;
import com.itextpdf.text.pdf.PdfPTable;
import com.itextpdf.text.pdf.PdfReader;
import com.itextpdf.text.pdf.PdfSmartCopy;
import com.itextpdf.text.pdf.PdfStamper;
import com.itextpdf.text.pdf.PdfWriter;
import com.itextpdf.text.pdf.parser.PdfReaderContentParser;
import com.itextpdf.text.pdf.parser.TextMarginFinder;
import com.itextpdf.text.pdf.parser.TextRenderInfo;

import lombok.extern.slf4j.Slf4j;

@Slf4j
public class IText5PDFDocument implements PDFDocument {

	private class CellEvent implements PdfPCellEvent {
		protected com.dkgeneric.pdf.model.CellRenderer renderer;
		protected int cellNum;

		CellEvent(CellRenderer renderer, int cellNum) {
			this.renderer = renderer;
			this.cellNum = cellNum;
		}

		@Override
		public void cellLayout(PdfPCell cell, Rectangle cellRectangle, PdfContentByte[] canvases) {
			if (renderer != null) {
				com.dkgeneric.pdf.model.Rectangle tocRectangle = new com.dkgeneric.pdf.model.Rectangle(
						cellRectangle.getLeft(), cellRectangle.getRight(), cellRectangle.getTop(),
						cellRectangle.getBottom());
				renderer.cellRendered(
						new CellRenderEvent(canvases[0].getPdfWriter().getPageNumber(), cellNum, tocRectangle));
			}
		}

	}

	public static Phrase applyTextFont(Phrase paragraph, TextFont textFont) {
		if (paragraph != null) {
			if (textFont == null)
				return paragraph;
			paragraph.setFont(new Font(IText5PDFService.getFontFamily(textFont.getFontName()), textFont.getFontSize(),
					getStyle(textFont), getBaseColor(textFont.getFontColor())));
		}
		return paragraph;
	}

	public static BaseColor getBaseColor(Color color) {
		return new BaseColor(color.getRed(), color.getGreen(), color.getBlue());
	}

	public static Font getFont(TextFont textFont) {
		return new Font(IText5PDFService.getFontFamily(textFont.getFontName()), textFont.getFontSize(),
				getStyle(textFont), getBaseColor(textFont.getFontColor()));
	}

	public static Rectangle getPageSizeRectangle(String type) {
		if (A4_SIZE.equalsIgnoreCase(type))
			return PageSize.A4;
		return PageSize.LETTER;
	}

	public static int getStyle(TextFont textFont) {
		return (textFont != null && textFont.getFontName().endsWith("-Bold")) ? Font.BOLD : Font.NORMAL;
	}

	public static int getTextAlignment(CellFont cellFont) {
		if (cellFont == null)
			return Element.ALIGN_LEFT;
		return getTextAlignment(cellFont.getAlignment());
	}

	public static int getTextAlignment(String alignment) {
		if (CENTER.equalsIgnoreCase(alignment))
			return Element.ALIGN_CENTER;
		if (RIGHT.equalsIgnoreCase(alignment))
			return Element.ALIGN_RIGHT;
		return Element.ALIGN_LEFT;
	}

	private Document document;

	private PdfReader pdfReader;

	private PdfSmartCopy pdfSmartCopy;

	private PdfWriter pdfWriter;

	private Rectangle pageSize;

	private OutputStream os;

	private ByteArrayOutputStream tempOs;

	public IText5PDFDocument(Rectangle pageSize, OutputStream os) {
		this.pageSize = pageSize;
		this.document = new Document(pageSize);
		this.os = os;
		tempOs = new ByteArrayOutputStream();
	}

	public IText5PDFDocument(Rectangle pageSize, PdfReader pdfReader) {
		this.pageSize = pageSize;
		this.document = new Document(pageSize);
		this.pdfReader = pdfReader;
	}

	@Override
	public void addBookmarks(List<Bookmark> bookmarks) {
		addBookmarks(bookmarks, false);
	}
	
	@Override
	public void addBookmarks(List<Bookmark> bookmarks,boolean clearExisting) {
		if (bookmarks == null)
			return;
		List<HashMap<String, Object>> outlines = new ArrayList<>();
		for (Bookmark bookmark : bookmarks) {
			HashMap<String, Object> entry = new HashMap<>();
			entry.put("Title", bookmark.getText());
			entry.put("Action", "GoTo");
			entry.put("Page", String.format("%d Fit", bookmark.getPageNum()));
			outlines.add(entry);
		}
		getExistingPdfWriter().setOutlines(outlines);
	}

	@Override
	public void addNavigationActions(List<PageNavigationData> navigations)
			throws IOException, DocumentException, PDFProcessingException {
		document.close();
		PdfReader tempReader = new PdfReader(tempOs.toByteArray());
		document = new Document(pageSize);
		pdfSmartCopy = null;
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfStamper stamper = new PdfStamper(tempReader, baos);
		for (PageNavigationData data : navigations) {
			PdfAction action = PdfAction.gotoLocalPage(data.getTargetPage(), new PdfDestination(0),
					stamper.getWriter());
			com.itextpdf.awt.geom.Rectangle rectangle = new com.itextpdf.awt.geom.Rectangle(data.getRectangle().getX(),
					data.getRectangle().getBottom(), data.getRectangle().getWidth(), data.getRectangle().getHeight());
			PdfAnnotation annotation = PdfAnnotation.createLink(stamper.getWriter(), new Rectangle(rectangle),
					PdfAnnotation.HIGHLIGHT_INVERT, action);
			annotation.setBorder(new PdfBorderArray(0f, 0f, 0f));
			stamper.addAnnotation(annotation, data.getActionPage());
		}
		stamper.close();
		tempReader.close();
		tempOs = new ByteArrayOutputStream();
		getPdfSmartCopy().addDocument(new PdfReader(baos.toByteArray()));
	}

	@Override
	public void addPageNumbers(float rightOffset, float bottomOffset, String format, int firstPage, int startValue,
			TextFont textFont) throws IOException, DocumentException, PDFProcessingException {
		document.close();
		PdfReader tempReader = new PdfReader(tempOs.toByteArray());
		document = new Document(pageSize);
		pdfSmartCopy = null;
		int n = tempReader.getNumberOfPages();
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		PdfStamper stamper = new PdfStamper(tempReader, baos);
		PdfContentByte pagecontent;
		for (int i = firstPage; i <= n; i++) {
			pagecontent = stamper.getOverContent(i);
			ColumnText.showTextAligned(pagecontent, Element.ALIGN_RIGHT,
					new Phrase(String.format(format, startValue++), getFont(textFont)),
					pageSize.getRight() - rightOffset, bottomOffset, 0);
		}
		stamper.close();
		tempReader.close();
		tempOs = new ByteArrayOutputStream();
		getPdfSmartCopy().addDocument(new PdfReader(baos.toByteArray()));
	}

	@Override
	public void addTOCTable(TOCDefinition tocTableDefinition, List<TOCEntry> entries) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		Document tocDocument = new Document(getPageSizeRectangle(tocTableDefinition.getPageSize()));
		PdfWriter.getInstance(tocDocument, baos);
		tocDocument.open();

		if (StringUtils.hasText(tocTableDefinition.getTocTitle())) {
			Paragraph paragraph = new Paragraph(tocTableDefinition.getTocTitle(),
					getFont(tocTableDefinition.getTitleTextFont()));
			paragraph.setAlignment(Element.ALIGN_CENTER);
			tocDocument.add(paragraph);
		}

		List<String> cellValues = new ArrayList<>();
		for (TOCEntry entry : entries)
			for (String text : entry.getColumnValues())
				cellValues.add(text);
		drawTable(tocDocument, tocTableDefinition, cellValues, new TOCCellRenderer(entries));
		tocDocument.close();
		appendDocument(new IText5PDFDocument(pageSize, new PdfReader(new ByteArrayInputStream(baos.toByteArray()))), 1);
	}

	@Override
	public int appendDocument(PDFDocument pdfDocumentToAppend) throws Exception {
		return appendDocument(pdfDocumentToAppend, -1);
	}

	@Override
	public int appendDocument(PDFDocument pdfDocumentToAppend, int position) throws Exception {
		int beforeNumber = getNumberOfPages();
		int result = position < 1 || position > getNumberOfPages() ? getNumberOfPages() + 1 : position;
		boolean reorder = result < getNumberOfPages() + 1;
		getPdfSmartCopy().addDocument(((IText5PDFDocument) pdfDocumentToAppend).pdfReader);
		int afterNumber = getNumberOfPages();
		if (reorder) {
			document.close();
			PdfReader tempReader = new PdfReader(tempOs.toByteArray());
			document = new Document(pageSize);
			pdfSmartCopy = null;
			List<Integer> pageList = new ArrayList<>();
			for (int i = 1; i < result; i++)
				pageList.add(i);
			for (int i = beforeNumber + 1; i <= afterNumber; i++)
				pageList.add(i);
			for (int i = result; i <= beforeNumber; i++)
				pageList.add(i);
			getPdfSmartCopy().addDocument(tempReader, pageList);
			tempReader.close();
		}
		return result;
	}

	@Override
	public List<Integer> appendDocuments(List<PDFDocument> pdfDocumentsToAppend) throws Exception {
		if (CollectionUtils.isEmpty(pdfDocumentsToAppend))
			return Collections.emptyList();
		List<Integer> result = new ArrayList<>(pdfDocumentsToAppend.size());
		for (PDFDocument doc : pdfDocumentsToAppend)
			result.add(appendDocument(doc));
		return result;
	}

	@Override
	public void close() throws IOException {
		document.close();
		if (tempOs != null)
			FileCopyUtils.copy(tempOs.toByteArray(), os);
	}

	@Override
	public List<TextPosition> findText(int page, List<String> texts) throws Exception {
		PdfReaderContentParser parser = new PdfReaderContentParser(pdfReader);
		List<TextRenderInfo> parseResults = new ArrayList<>();
		List<TextPosition> result = new ArrayList<>();
		parser.processContent(page, new TextMarginFinder() {
			@Override
			public void renderText(TextRenderInfo renderInfo) {
				parseResults.add(renderInfo);
				super.renderText(renderInfo);
			}
		});
		for (String text : texts) {
			TextPosition textPosition = new TextPosition(text, new ArrayList<>());
			for (TextRenderInfo info : parseResults) {
				if (info.getText().equals(text)) {
					com.itextpdf.awt.geom.Rectangle2D.Float fl = info.getBaseline().getBoundingRectange();
					com.itextpdf.awt.geom.Rectangle2D.Float fla = info.getAscentLine().getBoundingRectange();
					textPosition.getPositions()
							.add(new com.dkgeneric.pdf.model.Rectangle(fl.x, fl.x + fl.width, fla.y, fl.y));
				}
			}
			result.add(textPosition);
		}
		return result;
	}

	@Override
	public Object getDocument() {
		return document;
	}

	private PdfWriter getExistingPdfWriter() {
		return pdfWriter != null ? pdfWriter : pdfSmartCopy;
	}

	@Override
	public int getNumberOfPages() {
		if (pdfReader != null)
			return pdfReader.getNumberOfPages();
		int result = 0;
		if (pdfWriter != null)
			result = pdfWriter.getPageNumber();
		if (pdfSmartCopy != null) {
			result = pdfSmartCopy.getPageNumber();
			if (result > 0)
				result--;
		}
		return result;
	}

	@Override
	public PDFPage getPage(int pageNum) {
		return pdfReader != null ? new IText5PDFPage(pdfReader.getPageN(pageNum)) : null;
	}

	@Override
	public List<PDFPage> getPages() {
		List<PDFPage> result = new ArrayList<>();
		if (pdfReader != null) {
			for (int i = 1; i <= pdfReader.getNumberOfPages(); i++)
				result.add(new IText5PDFPage(pdfReader.getPageN(i)));
		}
		return result;
	}

	private PdfSmartCopy getPdfSmartCopy() throws PDFProcessingException, DocumentException {
		if (pdfSmartCopy == null) {
			if (pdfWriter != null)
				throw new PDFProcessingException("Append operation not allowed.");
			pdfSmartCopy = new PdfSmartCopy(document, tempOs);
			document.open();
		}
		return pdfSmartCopy;
	}

	private PdfWriter getPdfWriter() throws PDFProcessingException, DocumentException {
		if (pdfWriter == null) {
			if (pdfSmartCopy != null)
				throw new PDFProcessingException("Document inititialized as copy");
			pdfWriter = PdfWriter.getInstance(document, os);
			document.open();
		}
		return pdfWriter;
	}

	@Override
	public void drawTable(TableDefinition tableDefinition, List<String> cellValues, CellRenderer renderer)
			throws Exception {
		getPdfWriter();
		drawTable(document, tableDefinition, cellValues, renderer);

	}

	public void drawTable(Document tocDocument, TableDefinition tableDefinition, List<String> cellValues,
			CellRenderer renderer) throws Exception {
		PdfPTable table = new PdfPTable(tableDefinition.getColumnWidths());
		table.setWidthPercentage(100);
		for (int i = 0; i < tableDefinition.getColumnHeaders().length; i++) {
			String header = tableDefinition.getColumnHeaders()[i];
			CellFont cellFont = tableDefinition.getCellFont(i, true, header);
			PdfPCell cell = new PdfPCell(new Phrase(header, getFont(cellFont)));
			cell.setHorizontalAlignment(getTextAlignment(cellFont));
			table.addCell(cell);
			table.setHeaderRows(1);
		}
		for (int i = 0; i < cellValues.size(); i++) {
			int column = i % tableDefinition.getColumnWidths().length;
			String text = cellValues.get(i);
			CellFont cellFont = tableDefinition.getCellFont(column, false, text);
			PdfPCell cell = new PdfPCell(new Phrase(text, getFont(cellFont)));
			cell.setCellEvent(new CellEvent(renderer, i));
			cell.setHorizontalAlignment(getTextAlignment(cellFont));
			table.addCell(cell);
		}
		tocDocument.add(table);

	}
}
