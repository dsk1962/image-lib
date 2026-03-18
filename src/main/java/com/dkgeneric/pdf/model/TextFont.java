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
public class TextFont {

	private String fontName = "Helvetica";
	private float fontSize = 10;
	private Color fontColor = Color.BLACK;
}
