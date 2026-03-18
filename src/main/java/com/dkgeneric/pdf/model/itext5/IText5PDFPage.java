package com.dkgeneric.pdf.model.itext5;

import com.dkgeneric.pdf.model.PDFPage;
import com.itextpdf.text.pdf.PdfDictionary;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class IText5PDFPage implements PDFPage {

	private PdfDictionary page;

	public IText5PDFPage(PdfDictionary page) {
		this.page = page;
	}

}
