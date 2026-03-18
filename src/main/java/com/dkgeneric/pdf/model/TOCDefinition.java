package com.dkgeneric.pdf.model;

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

public class TOCDefinition extends TableDefinition {
	private String tocTitle;
	private Position titlePosition;
	private TextFont titleTextFont;
	private Position titlePagePosition;
}
