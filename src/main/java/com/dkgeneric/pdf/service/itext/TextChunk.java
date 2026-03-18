package com.dkgeneric.pdf.service.itext;

import com.itextpdf.kernel.geom.Rectangle;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class TextChunk {

	private String text;
	private Rectangle resultCoordinates;
}
