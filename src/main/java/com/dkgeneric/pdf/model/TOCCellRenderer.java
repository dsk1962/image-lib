package com.dkgeneric.pdf.model;

import java.util.List;

public class TOCCellRenderer implements CellRenderer{
	
	private List<TOCEntry> entries;
	private int columnNumber;
	public TOCCellRenderer(List<TOCEntry> entries) {
		this.entries = entries;
		columnNumber = entries.get(0).getColumnValues().size();
	}

	@Override
	public void cellRendered(CellRenderEvent event) {
		int entryNum = event.getCellNum()/columnNumber;
		if(entryNum < entries.size()) {
			TOCEntry entry = entries.get(entryNum);
			entry.setRectanglePage(event.getPageNum());
			com.dkgeneric.pdf.model.Rectangle tocRectangle = entry.getRectangle();
			Rectangle cellRectangle = event.getRectangle();
			if (tocRectangle == null) {
				entry.setRectangle(cellRectangle);
				return;
			}
			tocRectangle.setLeft(Math.min(tocRectangle.getLeft(), cellRectangle.getLeft()));
			tocRectangle.setTop(Math.max(tocRectangle.getTop(), cellRectangle.getTop()));
			tocRectangle.setRight(Math.max(tocRectangle.getRight(), cellRectangle.getRight()));
			tocRectangle.setBottom(Math.min(tocRectangle.getBottom(), cellRectangle.getBottom()));

		}
	}

}
