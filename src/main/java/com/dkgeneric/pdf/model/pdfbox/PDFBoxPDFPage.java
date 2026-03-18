package com.dkgeneric.pdf.model.pdfbox;

import org.apache.pdfbox.pdmodel.PDPage;

import com.dkgeneric.pdf.model.PDFPage;

import lombok.Getter;
import lombok.Setter;

@Getter
@Setter
public class PDFBoxPDFPage implements PDFPage {

	private PDPage page;

	public PDFBoxPDFPage(PDPage page) {
		this.page = page;
	}

}
