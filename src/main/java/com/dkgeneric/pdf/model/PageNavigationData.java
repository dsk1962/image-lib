package com.dkgeneric.pdf.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class PageNavigationData {

	private int actionPage;
	private int targetPage;
	private Rectangle rectangle;
}
