package com.dkgeneric.pdf.model.pdfbox;

import java.awt.Color;
import java.io.IOException;
import java.io.OutputStream;
import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

import org.apache.pdfbox.multipdf.PDFMergerUtility;
import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.PDPageTree;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.font.PDFont;
import org.apache.pdfbox.pdmodel.font.PDSimpleFont;
import org.apache.pdfbox.pdmodel.font.encoding.Encoding;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDAnnotationLink;
import org.apache.pdfbox.pdmodel.interactive.annotation.PDBorderStyleDictionary;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.destination.PDPageXYZDestination;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDDocumentOutline;
import org.apache.pdfbox.pdmodel.interactive.documentnavigation.outline.PDOutlineItem;
import org.apache.pdfbox.text.PDFTextStripper;
import org.springframework.util.CollectionUtils;
import org.springframework.util.StringUtils;

import com.dkgeneric.pdf.model.Bookmark;
import com.dkgeneric.pdf.model.CellFont;
import com.dkgeneric.pdf.model.CellRenderEvent;
import com.dkgeneric.pdf.model.CellRenderer;
import com.dkgeneric.pdf.model.PDFDocument;
import com.dkgeneric.pdf.model.PDFPage;
import com.dkgeneric.pdf.model.PageNavigationData;
import com.dkgeneric.pdf.model.Rectangle;
import com.dkgeneric.pdf.model.TOCCellRenderer;
import com.dkgeneric.pdf.model.TOCDefinition;
import com.dkgeneric.pdf.model.TOCEntry;
import com.dkgeneric.pdf.model.TableDefinition;
import com.dkgeneric.pdf.model.TextFont;
import com.dkgeneric.pdf.model.TextPosition;
import com.dkgeneric.pdf.service.pdfbox.PDFBoxPDFService;
import com.dkgeneric.pdf.service.pdfbox.TextPositionSequence;

import be.quodlibet.boxable.BaseTable;
import be.quodlibet.boxable.Cell;
import be.quodlibet.boxable.HorizontalAlignment;
import be.quodlibet.boxable.Row;
import be.quodlibet.boxable.line.LineStyle;
import be.quodlibet.boxable.page.DefaultPageProvider;
import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class PDFBoxPDFDocument implements PDFDocument {

	public static Cell<PDPage> applyTextFont(Cell<PDPage> cell, TextFont textFont) {
		if (cell == null)
			return null;
		if (textFont == null)
			textFont = new TextFont();

		cell.setFont(PDFBoxPDFService.getFont(textFont.getFontName()));
		cell.setFontSize(textFont.getFontSize());
		if (textFont.getFontColor() != null)
			cell.setTextColor(textFont.getFontColor());
		return cell;
	}

	public static PDPageContentStream applyTextFont(PDPageContentStream contentStream, TextFont textFont)
			throws IOException {
		if (contentStream == null || textFont == null)
			return null;
		if (textFont.getFontSize() > 0 && textFont.getFontName() != null)
			contentStream.setFont(PDFBoxPDFService.getFont(textFont.getFontName()), textFont.getFontSize());
		if (textFont.getFontColor() != null)
			contentStream.setNonStrokingColor(textFont.getFontColor());
		return contentStream;
	}

	public static PDRectangle getPageSize(String type) {
		if (PDFDocument.A4_SIZE.equalsIgnoreCase(type))
			return PDRectangle.A4;
		return PDRectangle.LETTER;
	}

	public static HorizontalAlignment getTextAlignment(CellFont cellFont) {
		return cellFont != null ? getTextAlignment(cellFont.getAlignment()) : HorizontalAlignment.LEFT;
	}

	public static HorizontalAlignment getTextAlignment(String alignment) {
		if (PDFDocument.CENTER.equalsIgnoreCase(alignment))
			return HorizontalAlignment.CENTER;
		if (PDFDocument.RIGHT.equalsIgnoreCase(alignment))
			return HorizontalAlignment.RIGHT;
		return HorizontalAlignment.LEFT;
	}

	private PDDocument document;

	private OutputStream os;

	public PDFBoxPDFDocument(PDDocument document) {
		this.document = document;
	}

	public PDFBoxPDFDocument(PDDocument document, OutputStream os) {
		this.document = document;
		this.os = os;
	}

	@Override
	public void addBookmarks(List<Bookmark> bookmarks) {
		addBookmarks(bookmarks, false);
	}
	
	@Override
	public void addBookmarks(List<Bookmark> bookmarks,boolean clearExisting) {
		if (bookmarks == null)
			return;
		if (document.getDocumentCatalog().getDocumentOutline() == null || clearExisting)
			document.getDocumentCatalog().setDocumentOutline(new PDDocumentOutline());
		for (Bookmark bookmark : bookmarks) {
			PDOutlineItem item = new PDOutlineItem();
			item.setTitle(bookmark.getText());
			item.setDestination(document.getPages().get(bookmark.getPageNum() - 1));
			document.getDocumentCatalog().getDocumentOutline().addLast(item);
		}
	}

	@Override
	public void addNavigationActions(List<PageNavigationData> navigations) throws IOException {
		for (PageNavigationData data : navigations) {
			com.dkgeneric.pdf.model.Rectangle rectangle = data.getRectangle();
			PDRectangle rect = new PDRectangle(rectangle.getX(), rectangle.getY(), rectangle.getWidth(),
					rectangle.getHeight());
			// just making these x,y coords up for sample

			PDAnnotationLink link = new PDAnnotationLink();
			link.setRectangle(rect);
			link.setPage(document.getPage(data.getActionPage() - 1));
			PDBorderStyleDictionary borderStyleDictionary = new PDBorderStyleDictionary();
			borderStyleDictionary.setWidth(0);
			link.setBorderStyle(borderStyleDictionary);
			PDPageDestination destination = new PDPageXYZDestination();
			destination.setPage(document.getPage(data.getTargetPage() - 1));
			link.setDestination(destination);

			document.getPage(data.getActionPage() - 1).getAnnotations().add(link);
		}
	}

	@Override
	public void addPageNumbers(float rightOffset, float bottomOffset, String format, int firstPage, int startValue,
			TextFont textFont) throws IOException {
		int count = document.getNumberOfPages();
		for (int page = firstPage - 1; page < count; page++) {
			PDPage pdPage = document.getPage(page);
			try (PDPageContentStream contentStream = new PDPageContentStream(document, pdPage,
					PDPageContentStream.AppendMode.APPEND, true, true)) {
				contentStream.setNonStrokingColor(textFont.getFontColor());
				contentStream.setFont(PDFBoxPDFService.getFont(textFont.getFontName()), textFont.getFontSize());
				float x = pdPage.getMediaBox().getWidth() - rightOffset;
				float y = bottomOffset;
				// Write the page number
				contentStream.beginText();
				contentStream.newLineAtOffset(x, y);
				contentStream.showText(String.format(format, startValue++));
				contentStream.endText();
			}
		}
	}

	@Override
	public void addTOCTable(TOCDefinition tocTableDefinition, List<TOCEntry> entries) throws Exception {

		PDDocument pdDocument = (PDDocument) getDocument();
		PDPageTree pages = pdDocument.getDocumentCatalog().getPages();
		int pdfPages = pages.getCount();
		PDPage firstTocPage = new PDPage(getPageSize(tocTableDefinition.getPageSize()));
		pages.add(firstTocPage);
		if (StringUtils.hasText(tocTableDefinition.getTocTitle()))
			outputCenteredText(pdDocument, firstTocPage, tocTableDefinition.getTitleTextFont(),
					tocTableDefinition.getTocTitle(), tocTableDefinition.getTitlePosition().getY());

		List<String> cellValues = new ArrayList<>();
		for (TOCEntry entry : entries)
			for (String text : entry.getColumnValues())
				cellValues.add(text);
		drawTable(firstTocPage, tocTableDefinition, cellValues, new TOCCellRenderer(entries));
		// move toc from bottom to top
		for (int i = 0; i < pages.getCount() - pdfPages; i++) {

			PDPage lastPage = pages.get(pages.getCount() - 1);
			pages.remove(lastPage);
			pages.insertBefore(lastPage, pages.get(0));
		}
	}

	@Override
	public int appendDocument(PDFDocument pdfDocumentToAppend) throws Exception {
		return appendDocument(pdfDocumentToAppend, -1);
	}

	@Override
	public int appendDocument(PDFDocument pdfDocumentToAppend, int position) throws Exception {
		int result = position < 1 || position > getNumberOfPages() ? getNumberOfPages() + 1 : position;
		int pdfPages = getNumberOfPages();
		try (PDDocument appDoc = (PDDocument) pdfDocumentToAppend.getDocument();) {
			PDDocument pdDoc = (PDDocument) getDocument();
			PDPageTree pages = pdDoc.getDocumentCatalog().getPages();
			PDFMergerUtility mergerUtility = new PDFMergerUtility();
			mergerUtility.appendDocument(pdDoc, appDoc);
			// move pages to new position if required
			if (result < pdfPages) {
				int count = getNumberOfPages();
				for (int i = 0; i < count - pdfPages; i++) {

					PDPage lastPage = pages.get(pages.getCount() - 1);
					pages.remove(lastPage);
					pages.insertBefore(lastPage, pages.get(result - 1));
				}
			}
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
		if (os != null)
			document.save(os);
		document.close();
	}

	// create table cell
	private Cell<PDPage> createCell(Row<PDPage> row, float width, String text, CellFont textFont) {
		Cell<PDPage> cell = row.createCell(width, "");
		if (cell != null) {
			applyTextFont(cell, textFont);
			boolean noLeftCell = cell.getLeftBorder() == null;
			cell.setBorderStyle(new LineStyle(textFont != null ? textFont.getBorderColor() : Color.BLACK, .5f));
			if (noLeftCell)
				cell.setLeftBorderStyle(null);
			cell.setAlign(getTextAlignment(textFont));
			cell.setText(processIllegalCharacters(text, cell.getFont()));
		}
		return cell;
	}

	// create TOC row
	private Row<PDPage> createTOCRow(BaseTable table, List<String> cellValues, TableDefinition tocTableDefinition) {
		// iterate through documents with same capture id
		int i = 0;
		Row<PDPage> row = table.createRow(12f);
		for (String value : cellValues) {
			CellFont cellFont = tocTableDefinition.getCellFont(i, false, value);
			createCell(row, tocTableDefinition.getColumnWidths()[i++], value, cellFont);
			row.setBookmark(new PDOutlineItem());
		}
		return row;
	}

	private void drawTable(PDPage firstTocPage, TableDefinition tableDefinition, List<String> cellValues,
			CellRenderer renderer) throws IOException {
		PDPageTree pages = document.getDocumentCatalog().getPages();
		int pdfPages = pages.getCount();
		float margin = 50;
		float tableWidth = firstTocPage.getMediaBox().getWidth() - (2 * margin);
		float yStartNewPage = firstTocPage.getMediaBox().getHeight();

		BaseTable table = new BaseTable(tableDefinition.getTableFirstPagePosition().getTop(), yStartNewPage, margin,
				margin, tableWidth, margin, document, firstTocPage, true, true,
				new DefaultPageProvider(document, getPageSize(tableDefinition.getPageSize())));
		// add table headers
		Row<PDPage> headerRow = table.createRow(15f);
		table.addHeaderRow(headerRow);
		for (int i = 0; i < tableDefinition.getColumnHeaders().length; i++) {
			String header = tableDefinition.getColumnHeaders()[i];
			CellFont cellFont = tableDefinition.getCellFont(i, true, header);
			createCell(headerRow, tableDefinition.getColumnWidths()[i], header, cellFont);
		}
		// add table rows
		List<Row<PDPage>> rows = new ArrayList<>();
		int columnNumber = tableDefinition.getColumnWidths().length;
		for (int i = 0; i < cellValues.size(); i += columnNumber)
			rows.add(createTOCRow(table, cellValues.subList(i, i + columnNumber), tableDefinition));

		table.draw();
		PDRectangle tableRect = ((PDPageXYZDestination) rows.get(0).getBookmark().getDestination()).getPage()
				.getMediaBox();

		if (renderer != null) {
			int i = 0;
			for (Row<PDPage> row : rows) {
				float xStart = margin + tableRect.getLowerLeftX();
				PDPageXYZDestination destination = (PDPageXYZDestination) row.getBookmark().getDestination();
				float top = destination.getTop() - row.getHeight();
				float bottom = destination.getTop();
				int page = pages.indexOf(destination.getPage()) - pdfPages + 2;
				for (Cell<PDPage> cell : row.getCells()) {
					renderer.cellRendered(new CellRenderEvent(page, i++,
							new Rectangle(xStart, xStart + cell.getWidth(), top, bottom)));
					xStart += cell.getWidth();
				}
			}
		}
	}

	@Override
	public void drawTable(TableDefinition tableDefinition, List<String> cellValues, CellRenderer renderer)
			throws Exception {
		PDPageTree pages = document.getDocumentCatalog().getPages();
		PDPage firstTocPage = new PDPage(getPageSize(tableDefinition.getPageSize()));
		pages.add(firstTocPage);
		drawTable(firstTocPage, tableDefinition, cellValues, renderer);
	}

	protected List<TextPositionSequence> findSubwords(PDDocument document, int page, String searchTerm)
			throws IOException {
		final List<TextPositionSequence> hits = new ArrayList<>();
		PDFTextStripper stripper = new PDFTextStripper() {
			@Override
			protected void writeString(String text, List<org.apache.pdfbox.text.TextPosition> textPositions)
					throws IOException {
				TextPositionSequence word = new TextPositionSequence(textPositions);
				String string = word.toString();
				int fromIndex = 0;
				int index;
				while ((index = string.indexOf(searchTerm, fromIndex)) > -1) {
					hits.add(word.subSequence(index, index + searchTerm.length()));
					fromIndex = index + 1;
				}
				super.writeString(text, textPositions);
			}
		};

		stripper.setSortByPosition(true);
		stripper.setStartPage(page);
		stripper.setEndPage(page);
		stripper.getText(document);
		return hits;
	}

	@Override
	public List<TextPosition> findText(int page, List<String> texts) throws IOException {
		List<TextPosition> result = new ArrayList<>();
		for (String text : texts) {
			List<TextPositionSequence> hits = findSubwords(document, page, text);
			TextPosition textPosition = new TextPosition(text, new ArrayList<>());
			for (TextPositionSequence hit : hits) {
				org.apache.pdfbox.text.TextPosition lastPosition = hit.textPositionAt(hit.length() - 1);
				org.apache.pdfbox.text.TextPosition firstPosition = hit.textPositionAt(0);
				textPosition.getPositions()
						.add(new com.dkgeneric.pdf.model.Rectangle(firstPosition.getX(), lastPosition.getEndX(),
								lastPosition.getEndY(), lastPosition.getEndY() + lastPosition.getFontSizeInPt()));
			}
			result.add(textPosition);
		}
		return result;
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
		return new PDFBoxPDFPage(document.getPage(pageNum));
	}

	@Override
	public List<PDFPage> getPages() {
		List<PDFPage> result = new ArrayList<>();
		for (int i = 1; i <= document.getNumberOfPages(); i++)
			result.add(new PDFBoxPDFPage(document.getPage(i)));
		return result;
	}

	// output centered text
	private void outputCenteredText(PDDocument mainDocument, PDPage page, TextFont textFont, String text,
			float yPosition) throws IOException {
		if (textFont == null)
			textFont = new TextFont();
		PDFont font = PDFBoxPDFService.getFont(textFont.getFontName());
		float titleWidth = font.getStringWidth(text) / 1000 * textFont.getFontSize();
		float titleHeight = font.getFontDescriptor().getFontBoundingBox().getHeight() / 1000 * textFont.getFontSize();

		try (PDPageContentStream stream = new PDPageContentStream(mainDocument, page)) {
			stream.beginText();
			applyTextFont(stream, textFont);
			stream.newLineAtOffset((page.getMediaBox().getWidth() - titleWidth) / 2,
					page.getMediaBox().getHeight() - yPosition - titleHeight);
			stream.showText(processIllegalCharacters(text, font));
			stream.endText();
		}
	}

	// remove characters not supported by font
	private String processIllegalCharacters(String value, PDFont font) {
		if (!StringUtils.hasText(value))
			return value;
		value = StringUtils.replace(value, "\n", "<br/>");
		StringBuilder b = new StringBuilder();
		Encoding encoding = ((PDSimpleFont) font).getEncoding();

		for (int i = 0; i < value.length(); i++)
			if (encoding.contains(value.charAt(i)))
				b.append(value.charAt(i));
		return b.toString();
	}
}
