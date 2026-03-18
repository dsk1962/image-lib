package com.dkgeneric.pdf.model;

import lombok.NoArgsConstructor;

@NoArgsConstructor
public class PDFProcessingException extends Exception {
	private static final long serialVersionUID = 1L;

	public PDFProcessingException(String message) {
		super(message);
	}

}
