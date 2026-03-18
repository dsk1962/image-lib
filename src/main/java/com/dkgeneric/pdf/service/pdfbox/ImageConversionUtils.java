package com.dkgeneric.pdf.service.pdfbox;

import java.awt.Dimension;
import java.awt.image.BufferedImage;
import java.io.ByteArrayInputStream;
import java.io.ByteArrayOutputStream;
import java.io.IOException;
import java.io.InputStream;
import java.util.ArrayList;
import java.util.List;

import javax.imageio.ImageIO;
import javax.media.jai.NullOpImage;
import javax.media.jai.OpImage;
import javax.media.jai.PlanarImage;

import org.apache.pdfbox.pdmodel.PDDocument;
import org.apache.pdfbox.pdmodel.PDPage;
import org.apache.pdfbox.pdmodel.PDPageContentStream;
import org.apache.pdfbox.pdmodel.common.PDRectangle;
import org.apache.pdfbox.pdmodel.graphics.image.JPEGFactory;
import org.apache.pdfbox.pdmodel.graphics.image.PDImageXObject;

import com.sun.media.jai.codec.ImageCodec;
import com.sun.media.jai.codec.ImageDecoder;

import lombok.Getter;
import lombok.Setter;

public class ImageConversionUtils {
	@Getter
	@Setter
	private float quality = 0.5f;
	@Getter
	@Setter
	private String imageConvertType = "tiff";
	@Getter
	@Setter
	private PDRectangle pageSize= PDRectangle.LETTER;

	public ImageConversionUtils(PDRectangle pageSize) {
		this.pageSize = pageSize;
	}

	/**
	 * This method is used for converting multipage tiff file into pdf using pdfbox
	 * 
	 * @param inputStream  stream of tiff file passing
	 * @return converted pdf as inputstream
	 * @throws IOException
	 * @throws Exception
	 */
	public ByteArrayInputStream convertImageToPdfUsingPdfBox(InputStream inputStream) throws Exception {
		List<BufferedImage> bimgList = new ArrayList<>();
		bimgList.add(ImageIO.read(inputStream));
		return convertToPdf(bimgList);
	}

	/**
	 * This method is used for converting jpeg file into pdf using pdfbox
	 * 
	 * @param inputStream  stream of jpeg file passing
	 * @return converted pdf as inputstream
	 * @throws IOException
	 * @throws Exception
	 */
	public ByteArrayInputStream convertTiffToPdfUsingPdfBox(InputStream inputStream) throws Exception {
		List<BufferedImage> bimages = getBufferImageFromInputStream(inputStream);

		return convertToPdf(bimages);
	}

	private void drawImage(PDPageContentStream contentStream,PDImageXObject image, Dimension scaledDim ,PDPage page) throws IOException {
		PDRectangle rectangle = page.getMediaBox();
		float xOffset = rectangle.getWidth() > image.getWidth() ? (rectangle.getWidth() - image.getWidth())/2:1; 
		float yOffset = rectangle.getHeight() > image.getHeight() ? (rectangle.getHeight() - image.getHeight())/2:1; 
		contentStream.drawImage(image, xOffset, yOffset, scaledDim.width, scaledDim.height);
		
	}
	
	/**
	 * This method is used for adding buffered images to list
	 * 
	 * @param bimages tiff /jpeg pages 
	 * @return pdf as inputstream
	 * @throws IOException
	 * @throws Exception
	 */
	private ByteArrayInputStream convertToPdf(List<BufferedImage> bimages) throws IOException  {
		ByteArrayOutputStream outStream = new ByteArrayOutputStream();
		try (PDDocument doc = new PDDocument();) {

			for (BufferedImage bi : bimages) {
				PDPage page = new PDPage(pageSize);
				doc.addPage(page);
				try (PDPageContentStream contentStream = new PDPageContentStream(doc, page);) {

					// the .08F can be tweaked. Go up for better quality,
					// but the size of the PDF will increase
					PDImageXObject image = JPEGFactory.createFromImage(doc, bi, quality);
					Dimension scaledDim = getScaledDimension(new Dimension(image.getWidth(), image.getHeight()),
							new Dimension((int) page.getMediaBox().getWidth(), (int) page.getMediaBox().getHeight()));
					drawImage(contentStream, image, scaledDim,page);
				}
			}
			doc.save(outStream);
		}
		byte[] result = outStream.toByteArray();
		return new ByteArrayInputStream(result);
	}

	private Dimension getScaledDimension(Dimension imgSize, Dimension boundary) {
		int originalWidth = imgSize.width;
		int originalHeight = imgSize.height;
		int boundWidth = boundary.width;
		int boundHeight = boundary.height;
		int newWidth = originalWidth;
		int newHeight = originalHeight;
		// first check if we need to scale width
		if (originalWidth > boundWidth) {
			// scale width to fit
			newWidth = boundWidth;
			// scale height to maintain aspect ratio
			newHeight = (newWidth * originalHeight) / originalWidth;
		}

		// then check if we need to scale even with the new height
		if (newHeight > boundHeight) {
			// scale height to fit instead
			newHeight = boundHeight;
			// scale width to maintain aspect ratio
			newWidth = (newHeight * originalWidth) / originalHeight;
		}
		return new Dimension(newWidth, newHeight);
	}

	/**
	 * This method is used for adding buffered images to list
	 * 
	 * @param inputStream stream of tiff file passing
	 * @return images from tiff file
	 * @throws IOException
	 * @throws Exception
	 */

	private List<BufferedImage> getBufferImageFromInputStream(InputStream inputStream) throws IOException {

		ImageDecoder imageDecoderObj = ImageCodec.createImageDecoder(imageConvertType, inputStream, null);
		PlanarImage planarImageObj = null;
		BufferedImage bufferedImageObj = null;
		List<BufferedImage> images = new ArrayList<>();
		// getting No.of pages of particular tiff image
		int numPages = imageDecoderObj.getNumPages();

		// iterating for number of pages
		for (int j = 0; j < numPages; j++) {

			// PlanarImage for the tiff image which is rendered to pixels NullOpImage for
			// forwarding the request to sourcefile

			planarImageObj = new NullOpImage(imageDecoderObj.decodeAsRenderedImage(j), null, null, OpImage.OP_IO_BOUND);

			bufferedImageObj = planarImageObj.getAsBufferedImage();

			if (bufferedImageObj != null) {
				// adding the images which obtained as buffered image
				images.add(bufferedImageObj);
			}
		}

		return images;

	}

}
