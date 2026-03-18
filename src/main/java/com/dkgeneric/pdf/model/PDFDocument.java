package com.dkgeneric.pdf.model;

import java.util.List;

public interface PDFDocument {

	public static final String A4_SIZE = "A4";
	public static final String LETTER_SIZE = "Letter";

	public static final String CENTER = "center";
	public static final String LEFT = "left";
	public static final String RIGHT = "right";

	public Object getDocument();

	public int getNumberOfPages();

	public List<PDFPage> getPages();

	public PDFPage getPage(int pageNum);

	public void addBookmarks(List<Bookmark> bookmarks) throws Exception;

	public void addBookmarks(List<Bookmark> bookmarks, boolean clearExisting) throws Exception;

	public void close() throws Exception;

	public void addNavigationActions(List<PageNavigationData> navigations) throws Exception;

	public void addPageNumbers(float rightOffset, float bottomOffset, String format, int startValue, int firstPage,
			TextFont textFont) throws Exception;

	public void addTOCTable(TOCDefinition tocTableDefinition, List<TOCEntry> entries) throws Exception;

	public int appendDocument(PDFDocument pdfDocumentToAppend) throws Exception;

	public int appendDocument(PDFDocument pdfDocumentToAppend, int position) throws Exception;

	public List<Integer> appendDocuments(List<PDFDocument> pdfDocumentsToAppend) throws Exception;

	public List<TextPosition> findText(int page, List<String> texts) throws Exception;

	public void drawTable(TableDefinition tableDefinition, List<String> cellValues, CellRenderer renderer)
			throws Exception;
}
