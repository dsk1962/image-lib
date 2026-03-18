package com.dkgeneric.pdf.model;

import java.awt.Color;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public abstract class TableDefinition {

	private String[] columnHeaders;
	private float[] columnWidths;
	private Rectangle tableFirstPagePosition;
	private String pageSize = PDFDocument.LETTER_SIZE;

	public String getFontName(int cellNum, boolean isHeader, Object value) {
		return isHeader ? "Helvetica-Bold" : "Helvetica";
	}

	public float getFontSize(int cellNum, boolean isHeader, Object value) {
		return 10;
	}

	public Color getFontColor(int cellNum, boolean isHeader, Object value) {
		return Color.BLACK;
	}

	public Color getBorderColor(int cellNum, boolean isHeader, Object value) {
		return Color.BLACK;
	}

	public String getAlignment(int cellNum, boolean isHeader, Object value) {
		return PDFDocument.LEFT;
	}

	public CellFont getCellFont(int cellNum, boolean isHeader, Object value) {
		CellFont cellfont = new CellFont(getAlignment(cellNum, isHeader, value),
				getBorderColor(cellNum, isHeader, value));
		cellfont.setFontName(getFontName(cellNum, isHeader, value));
		cellfont.setFontSize(getFontSize(cellNum, isHeader, value));
		cellfont.setFontColor(getFontColor(cellNum, isHeader, value));
		return cellfont;
	}
}
