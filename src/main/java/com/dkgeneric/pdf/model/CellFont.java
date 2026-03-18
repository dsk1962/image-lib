package com.dkgeneric.pdf.model;

import java.awt.Color;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;
import lombok.ToString;

@Getter
@Setter
@ToString
@NoArgsConstructor
@AllArgsConstructor
public class CellFont extends TextFont {
	private String alignment = PDFDocument.LEFT;
	private Color borderColor = Color.BLACK;
}
