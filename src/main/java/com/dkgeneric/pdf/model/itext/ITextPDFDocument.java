package com.dkgeneric.pdf.model.itext;

import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.dkgeneric.pdf.model.Bookmark;
import com.dkgeneric.pdf.model.CellFont;
import com.dkgeneric.pdf.model.CellRenderEvent;
import com.dkgeneric.pdf.model.PDFDocument;
import com.dkgeneric.pdf.model.PDFPage;
import com.dkgeneric.pdf.model.PageNavigationData;
import com.dkgeneric.pdf.model.TOCCellRenderer;
import com.dkgeneric.pdf.model.TOCDefinition;
import com.dkgeneric.pdf.model.TOCEntry;
import com.dkgeneric.pdf.model.TableDefinition;
import com.dkgeneric.pdf.model.TextFont;
import com.dkgeneric.pdf.model.TextPosition;
import com.dkgeneric.pdf.service.itext.PdfTextLocator;
import com.itextpdf.kernel.colors.DeviceRgb;
import com.itextpdf.kernel.font.PdfFontFactory;
import com.itextpdf.kernel.geom.PageSize;
import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfAnnotationBorder;
import com.itextpdf.kernel.pdf.PdfDocument;
import com.itextpdf.kernel.pdf.PdfOutline;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.PdfReader;
import com.itextpdf.kernel.pdf.PdfWriter;
import com.itextpdf.kernel.pdf.action.PdfAction;
import com.itextpdf.kernel.pdf.annot.PdfAnnotation;
import com.itextpdf.kernel.pdf.annot.PdfLinkAnnotation;
import com.itextpdf.kernel.pdf.navigation.PdfExplicitDestination;
import com.itextpdf.layout.Canvas;
import com.itextpdf.layout.Document;
import com.itextpdf.layout.element.Cell;
import com.itextpdf.layout.element.Paragraph;
import com.itextpdf.layout.element.Table;
import com.itextpdf.layout.properties.TextAlignment;
import com.itextpdf.layout.properties.UnitValue;
import com.itextpdf.layout.renderer.CellRenderer;
import com.itextpdf.layout.renderer.DrawContext;
import com.itextpdf.layout.renderer.IRenderer;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter

public class ITextPDFDocument implements PDFDocument {

	private class CellTitleRenderer extends CellRenderer {
		protected com.dkgeneric.pdf.model.CellRenderer renderer;
		protected int cellNum;

		public CellTitleRenderer(Cell modelElement, int cellNum, com.dkgeneric.pdf.model.CellRenderer renderer) {
			super(modelElement);
			this.renderer = renderer;
			this.cellNum = cellNum;
		}

		@Override
		public void drawBorder(DrawContext drawContext) {
			if (renderer != null)
				renderer.cellRendered(new CellRenderEvent(getOccupiedArea().getPageNumber(), cellNum,
						convertRectangle(getOccupiedAreaBBox())));
		}

		// If a renderer overflows on the next area, iText uses #getNextRenderer()
		// method to create a new renderer for the overflow part.
		// If #getNextRenderer() isn't overridden, the default method will be used and
		// thus the default rather than the custom
		// renderer will be created
		@Override
		public IRenderer getNextRenderer() {
			return new CellTitleRenderer((Cell) modelElement, cellNum, renderer);
		}
	}

	public static Paragraph applyTextFont(Paragraph paragraph, TextFont textFont) throws IOException {
		if (paragraph == null || textFont == null)
			return null;
		if (textFont.getFontSize() > 0)
			paragraph.setFontSize(textFont.getFontSize());
		if (textFont.getFontName() != null)
			paragraph.setFont(PdfFontFactory.createFont(textFont.getFontName()));
		if (textFont.getFontColor() != null)
			paragraph.setFontColor(new DeviceRgb(textFont.getFontColor()));
		return paragraph;
	}

	public static PageSize getPageSize(String type) {
		if (A4_SIZE.equalsIgnoreCase(type))
			return PageSize.A4;
		return PageSize.LETTER;
	}

	public static TextAlignment getTextAlignment(CellFont cellFont) {
		if (cellFont == null)
			return TextAlignment.LEFT;
		return getTextAlignment(cellFont.getAlignment());
	}

	public static TextAlignment getTextAlignment(String alignment) {
		if (CENTER.equalsIgnoreCase(alignment))
			return TextAlignment.CENTER;
		if (RIGHT.equalsIgnoreCase(alignment))
			return TextAlignment.RIGHT;
		return TextAlignment.LEFT;
	}

	private PdfDocument document;
	private Document internalDocument;
	private String pageSize;

	public ITextPDFDocument(PdfDocument document) {
		this.document = document;
	}

	public ITextPDFDocument(PdfDocument document, String pageSize) {
		this.document = document;
		this.pageSize = pageSize;
	}

	private Document getInternalDocument() {
		if (internalDocument == null)
			internalDocument = new Document(document, getPageSize(pageSize));
		return internalDocument;
	}

	@Override
	public void addBookmarks(List<Bookmark> bookmarks) {
		addBookmarks(bookmarks, false);
	}

	@Override
	public void addBookmarks(List<Bookmark> bookmarks, boolean clearExisting) {
		if (bookmarks == null)
			return;
		document.getOutlines(clearExisting);
		for (Bookmark bookmark : bookmarks) {
			PdfOutline outline = document.getOutlines(false);
			outline = outline.addOutline(bookmark.getText());
			outline.addAction(
					PdfAction.createGoTo(PdfExplicitDestination.createFit(document.getPage(bookmark.getPageNum()))));
		}
	}

	@Override
	public void addNavigationActions(List<PageNavigationData> navigations) {
		for (PageNavigationData data : navigations) {
			PdfLinkAnnotation linkAnnotation = new PdfLinkAnnotation(new Rectangle(data.getRectangle().getX(),
					data.getRectangle().getBottom(), data.getRectangle().getWidth(), data.getRectangle().getHeight()))
							.setAction(PdfAction.createGoTo(
									PdfExplicitDestination.createFit(document.getPage(data.getTargetPage()))));
			linkAnnotation.setBorder(new PdfAnnotationBorder(0, 0, 0));
			linkAnnotation.setHighlightMode(PdfAnnotation.HIGHLIGHT_INVERT);
			document.getPage(data.getActionPage()).addAnnotation(linkAnnotation);
		}
	}

	@Override
	public void addPageNumbers(float rightOffset, float bottomOffset, String format, int firstPage, int startValue,
			TextFont textFont) throws IOException {
		for (int page = firstPage; page <= document.getNumberOfPages(); page++) {
			PdfPage pdfPage = document.getPage(page);
			pdfPage.newContentStreamAfter();
			try (Canvas canvas = new Canvas(pdfPage, pdfPage.getMediaBox())) {
				Paragraph paragraph = new Paragraph(String.format(format, startValue++));
				applyTextFont(paragraph, textFont);
				canvas.showTextAligned(paragraph, pdfPage.getPageSize().getWidth() - rightOffset, bottomOffset,
						TextAlignment.RIGHT);
			}
		}
	}

	@Override
	public void addTOCTable(TOCDefinition tocTableDefinition, List<TOCEntry> entries) throws Exception {
		ByteArrayOutputStream baos = new ByteArrayOutputStream();
		try (PdfDocument tocDocument = new PdfDocument(new PdfWriter(baos));
				Document doc = new Document(tocDocument, getPageSize(tocTableDefinition.getPageSize()));) {
			doc.setTopMargin(0);
			if (StringUtils.hasText(tocTableDefinition.getTocTitle())) {
				Paragraph paragraph = new Paragraph(tocTableDefinition.getTocTitle())
						.setTextAlignment(TextAlignment.CENTER).setMarginTop(tocTableDefinition.getTitlePosition().getY());
				applyTextFont(paragraph, tocTableDefinition.getTitleTextFont());
				doc.add(paragraph);
			}
			List<String> cellValues = new ArrayList<>();
			for (TOCEntry entry : entries)
				for (String text : entry.getColumnValues())
					cellValues.add(text);
			drawTable(doc, tocTableDefinition, cellValues, new TOCCellRenderer(entries));
		}
		try (PdfDocument tocDocument = new PdfDocument(new PdfReader(new ByteArrayInputStream(baos.toByteArray())))) {
			tocDocument.copyPagesTo(1, tocDocument.getNumberOfPages(), (PdfDocument) getDocument(), 1);
		}
	}

	@Override
	public int appendDocument(PDFDocument pdfDocumentToAppend) throws Exception {
		return appendDocument(pdfDocumentToAppend, -1);
	}

	@Override
	public int appendDocument(PDFDocument pdfDocumentToAppend, int position) throws Exception {
		int result = position < 1 || position > getNumberOfPages() ? getNumberOfPages() + 1 : position;
		PdfDocument appDoc = (PdfDocument) pdfDocumentToAppend.getDocument();
		appDoc.copyPagesTo(1, appDoc.getNumberOfPages(), (PdfDocument) getDocument(), result);
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
	public void close() {
		if (internalDocument != null)
			internalDocument.close();
		document.close();
	}

	private com.dkgeneric.pdf.model.Rectangle convertRectangle(Rectangle r) {
		return new com.dkgeneric.pdf.model.Rectangle(r.getLeft(), r.getRight(), r.getTop(), r.getBottom());
	}

	@Override
	public List<TextPosition> findText(int page, List<String> texts) {
		PDFPage pdfPage = getPage(page);
		List<TextPosition> result = new ArrayList<>();
		for (String text : texts) {
			List<Rectangle> positions = PdfTextLocator.getTextCoordinates((PdfPage) pdfPage.getPage(), text);
			TextPosition textPosition = new TextPosition(text, new ArrayList<>());
			positions.forEach(r -> textPosition.getPositions().add(convertRectangle(r)));
			result.add(textPosition);
		}
		return result;
	}

	private Cell getCell(String content, int cellNum, TextFont textFont, com.dkgeneric.pdf.model.CellRenderer renderer)
			throws IOException {

		Cell cell = new Cell().add(applyTextFont(new Paragraph(content), textFont));
		cell.setNextRenderer(new CellTitleRenderer(cell, cellNum, renderer));
		return cell;
	}

	@Override
	public Object getDocument() {
		return document;
	}

	@Override
	public int getNumberOfPages() {
		return document.getNumberOfPages();
	}

	@Override
	public PDFPage getPage(int pageNum) {
		return new ITextPDFPage(document.getPage(pageNum));
	}

	@Override
	public List<PDFPage> getPages() {
		List<PDFPage> result = new ArrayList<>();
		for (int i = 1; i <= document.getNumberOfPages(); i++)
			result.add(new ITextPDFPage(document.getPage(i)));
		return result;
	}

	@Override
	public void drawTable(TableDefinition tableDefinition, List<String> cellValues,
			com.dkgeneric.pdf.model.CellRenderer renderer) throws Exception {
		drawTable(getInternalDocument(), tableDefinition, cellValues, renderer);
	}

	private void drawTable(Document document, TableDefinition tableDefinition, List<String> cellValues,
			com.dkgeneric.pdf.model.CellRenderer renderer) throws Exception {
		Table table = new Table(UnitValue.createPercentArray(tableDefinition.getColumnWidths())).useAllAvailableWidth();
		for (int i = 0; i < tableDefinition.getColumnHeaders().length; i++) {
			String header = tableDefinition.getColumnHeaders()[i];
			CellFont cellFont = tableDefinition.getCellFont(i, true, header);
			table.addHeaderCell(new Cell().setTextAlignment(getTextAlignment(cellFont))
					.add(applyTextFont(new Paragraph(header), cellFont)));
		}
		for (int i = 0; i < cellValues.size(); i++) {
			int column = i % tableDefinition.getColumnWidths().length;
			String text = cellValues.get(i);
			CellFont cellFont = tableDefinition.getCellFont(column, false, text);
			table.addCell(getCell(text, i, cellFont, renderer).setTextAlignment(getTextAlignment(cellFont)));
		}
		document.add(table);
	}

}
