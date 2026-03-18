package com.dkgeneric.pdf.service.itext;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import com.itextpdf.kernel.geom.Rectangle;
import com.itextpdf.kernel.pdf.PdfPage;
import com.itextpdf.kernel.pdf.canvas.parser.EventType;
import com.itextpdf.kernel.pdf.canvas.parser.PdfTextExtractor;
import com.itextpdf.kernel.pdf.canvas.parser.data.IEventData;
import com.itextpdf.kernel.pdf.canvas.parser.data.TextRenderInfo;
import com.itextpdf.kernel.pdf.canvas.parser.listener.LocationTextExtractionStrategy;

import lombok.Getter;
import lombok.Setter;
import lombok.extern.slf4j.Slf4j;

@Getter
@Setter
@Slf4j
public class PdfTextLocator extends LocationTextExtractionStrategy {

	private final String textToSearchFor;
	private final Map<Float, List<TextChunk>> lines = new HashMap<>();

	/**
	 * Returns a rectangle with a given location of text on a page. Returns null if
	 * not found.
	 *
	 * @param page the page where search
	 * @param s the searct text
	 * @return the text coordinates
	 */
	public static List<Rectangle> getTextCoordinates(PdfPage page, String s) {
		PdfTextLocator strat = new PdfTextLocator(s);
		PdfTextExtractor.getTextFromPage(page, strat);
		List<Rectangle> results = new ArrayList<>();
		Rectangle startPos = null;
		String search = s;
		for (List<TextChunk> list : strat.getLines().values()) {
			Collections.sort(list,
					(l, r) -> Float.compare(l.getResultCoordinates().getX(), r.getResultCoordinates().getX()));
			for (TextChunk c : list) {
				if (!s.startsWith(c.getText())) {
					s = search;
					startPos = null;
				}

				if (s.startsWith(c.getText())) {
					if (startPos == null) {
						startPos = c.getResultCoordinates();
					}
					s = s.substring(c.getText().length());
					if (s.isEmpty()) {
						float startX = startPos.getX();
						float startY = startPos.getY();
						results.add(new Rectangle(startX, startY, c.getResultCoordinates().getRight() - startX,
								Math.max(c.getResultCoordinates().getHeight(), startPos.getHeight())));
					}
				}
			}
		}
		return results;
	}

	public PdfTextLocator(String textToSearchFor) {
		this.textToSearchFor = textToSearchFor;
	}

	@Override
	public void eventOccurred(IEventData data, EventType type) {
		if (!type.equals(EventType.RENDER_TEXT))
			return;

		TextRenderInfo renderInfo = (TextRenderInfo) data;
		List<TextRenderInfo> text = renderInfo.getCharacterRenderInfos();
		for (int i = 0; i < text.size(); i++) {
			String s = text.get(i).getText();
			float startX = text.get(i).getDescentLine().getStartPoint().get(0);
			float startY = text.get(i).getDescentLine().getStartPoint().get(1);

			List<TextChunk> list = lines.computeIfAbsent(startY, v -> new ArrayList<>());
			list.add(new TextChunk(s,
					new Rectangle(startX, startY, text.get(i).getAscentLine().getEndPoint().get(0) - startX,
							text.get(i).getAscentLine().getEndPoint().get(1) - startY)));
		}
	}
}
