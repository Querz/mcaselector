package net.querz.mcaselector.text;

import javafx.geometry.VPos;
import javafx.scene.SnapshotParameters;
import javafx.scene.canvas.Canvas;
import javafx.scene.canvas.GraphicsContext;
import javafx.scene.image.Image;
import javafx.scene.image.WritableImage;
import javafx.scene.paint.Color;
import javafx.scene.text.Font;
import javafx.scene.text.Text;

public class CoordinateRenderer {

	private static final String ALLOWED_CHARS = "0123456789,.-KMB";
	private static final double STROKE_WIDTH = 3.0;
	private static final double PADDING = STROKE_WIDTH * 2;

	private final Image[] charImages = new Image[128];
	private final double[] charWidths = new double[128];
	private final double strokeOffset = STROKE_WIDTH;
	private final double lineHeight;

	public CoordinateRenderer(Font font, Color fill, Color stroke) {
		// measure line height using a helper Text node
		Text metricHelper = new Text("8");
		metricHelper.setFont(font);
		this.lineHeight = metricHelper.getLayoutBounds().getHeight();

		SnapshotParameters snapParams = new SnapshotParameters();
		snapParams.setFill(Color.TRANSPARENT);

		for (char c : ALLOWED_CHARS.toCharArray()) {
			Text chHelper = new Text(String.valueOf(c));
			chHelper.setFont(font);

			double charWidth = Math.ceil(chHelper.getLayoutBounds().getWidth());
			double charHeight = Math.ceil(chHelper.getLayoutBounds().getHeight());

			// create temporary canvas with padding to prevent outline clipping
			Canvas canvas = new Canvas(charWidth + PADDING, charHeight + PADDING);
			GraphicsContext ctx = canvas.getGraphicsContext2D();
			ctx.setFont(font);
			ctx.setTextBaseline(VPos.TOP);

			// draw outlined text
			ctx.setStroke(stroke);
			ctx.setLineWidth(STROKE_WIDTH);
			ctx.strokeText(String.valueOf(c), strokeOffset, strokeOffset);
			ctx.setFill(fill);
			ctx.fillText(String.valueOf(c), strokeOffset, strokeOffset);

			// save to cache
			WritableImage img = canvas.snapshot(snapParams, null);
			charImages[c] = img;
			charWidths[c] = charWidth;
		}
	}

	public void draw(GraphicsContext ctx, String coordinate, double x, double y) {
		double currentX = x;
		double currentY = y;

		for (int i = 0; i < coordinate.length(); i++) {
			char c = coordinate.charAt(i);

			if (c == '\n') {
				currentX = x;
				currentY += lineHeight;
				continue;
			}

			if (c < 128) {
				Image img = charImages[c];
				if (img != null) {
					// offset the image draw to account for the internal stroke padding
					ctx.drawImage(img, currentX - strokeOffset, currentY - strokeOffset);
					currentX += charWidths[c];
				}
			}
		}
	}

	public void drawRotated90(GraphicsContext ctx, String coordinate, double alpha, double x, double y) {
		ctx.save();
		ctx.translate(x, y);
		ctx.rotate(90);
		ctx.setGlobalAlpha(alpha);
		draw(ctx, coordinate, 0, 0);
		ctx.restore();
	}
}