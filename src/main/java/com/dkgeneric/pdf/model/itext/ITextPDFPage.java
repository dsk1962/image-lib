package com.dkgeneric.pdf.model.itext;

import com.dkgeneric.pdf.model.PDFPage;
import com.itextpdf.kernel.pdf.PdfPage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class ITextPDFPage implements PDFPage {

	private PdfPage page;

	public ITextPDFPage(PdfPage page) {
		this.page = page;
	}

}
