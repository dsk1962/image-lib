package com.dkgeneric.pdf.model;

import lombok.AllArgsConstructor;
import lombok.Getter;
import lombok.NoArgsConstructor;
import lombok.Setter;

@Getter
@Setter
@NoArgsConstructor
@AllArgsConstructor
public class CellRenderEvent {
	private int pageNum;
	private int cellNum;
	private Rectangle rectangle;
}
