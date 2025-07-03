package pw.edu.javandmi_ndvi.core;

import org.geotools.coverage.grid.GridCoverage2D;

import java.awt.*;
import java.awt.image.BufferedImage;
import java.awt.image.Raster;
import java.util.concurrent.Callable;
import java.util.concurrent.atomic.AtomicBoolean;

public class TileProcessor implements Callable<Void> {
    private final GridCoverage2D red;
    private final GridCoverage2D nir;
    private final GridCoverage2D swir;
    private final Rectangle tileRect;
    private final BufferedImage ndmiImage;
    private final BufferedImage ndviImage;
    private final Boolean calculateNdvi;
    private final Boolean calculateNdmi;
    private final Object pauseLock;
    private final AtomicBoolean paused;

    public TileProcessor(GridCoverage2D red, GridCoverage2D nir, GridCoverage2D swir,
                         BufferedImage ndviImage, BufferedImage ndmiImage,
                         int tileX, int tileY, int tileWidth, int tileHeight,
                         Boolean calculateNdvi, Boolean calculateNdmi, Object pauseLock, AtomicBoolean paused) {
        this.red = red;
        this.nir = nir;
        this.swir = swir;
        this.tileRect = new Rectangle(tileX, tileY, tileWidth, tileHeight);
        this.calculateNdvi = calculateNdvi;
        this.calculateNdmi = calculateNdmi;
        this.ndmiImage = ndmiImage;
        this.ndviImage = ndviImage;
        this.pauseLock = pauseLock;
        this.paused = paused;
    }

    @Override
    public Void call() {
        Raster redRasterTile = red.getRenderedImage().getData(tileRect);
        Raster nirRasterTile = nir.getRenderedImage().getData(tileRect);
        Raster swirRasterTile = swir.getRenderedImage().getData(tileRect);

        int tileWidth = tileRect.width;
        int tileHeight = tileRect.height;
        int tileX = tileRect.x;
        int tileY = tileRect.y;

        BufferedImage localNdviImage = null;
        BufferedImage localNdmiImage = null;

        if (calculateNdvi) {
            localNdviImage = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_RGB);
        }
        if (calculateNdmi) {
            localNdmiImage = new BufferedImage(tileWidth, tileHeight, BufferedImage.TYPE_INT_RGB);
        }

        for (int ty = 0; ty < tileHeight; ty++) {

            synchronized (pauseLock) {
                while (paused.get()) {
                    try {
                        pauseLock.wait();
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                        return null;
                    }
                }
            }

            for (int tx = 0; tx < tileWidth; tx++) {
                int sampleX = tx + redRasterTile.getMinX();
                int sampleY = ty + redRasterTile.getMinY();

                double redVal = redRasterTile.getSampleDouble(sampleX, sampleY, 0);
                double nirVal = nirRasterTile.getSampleDouble(sampleX, sampleY, 0);
                double swirVal = swirRasterTile.getSampleDouble(sampleX, sampleY, 0);

                if (calculateNdvi) {
                    double ndvi = calculateIndex(nirVal, redVal);
                    Color c = getColorForNdvi((float) ndvi);
                    localNdviImage.setRGB(tx, ty, c.getRGB());
                }

                if (calculateNdmi) {
                    double ndmi = calculateIndex(nirVal, swirVal);
                    Color c = getColorForNdmi((float) ndmi);
                    localNdmiImage.setRGB(tx, ty, c.getRGB());
                }
            }
        }

        if (calculateNdvi) {
            saveNdviResult(tileX, tileY, localNdviImage);
        }

        if (calculateNdmi) {
            saveNdmiResult(tileX, tileY, localNdmiImage);
        }

        return null;
    }

    private static double calculateIndex(double a, double b) {
        double denom = a + b;
        return denom == 0 ? 0 : (a - b) / denom;
    }

    private synchronized void saveNdviResult(int tileX, int tileY, BufferedImage localNdviTile) {
        ndviImage.getRaster().setRect(tileX, tileY, localNdviTile.getRaster());
    }

    private synchronized void saveNdmiResult(int tileX, int tileY, BufferedImage localNdmiTile) {
        ndmiImage.getRaster().setRect(tileX, tileY, localNdmiTile.getRaster());
    }

    private Color getColorForNdvi(float value) {
        value = Math.max(-1f, Math.min(1f, value));
        float norm = (value + 1f) / 2f;
        int red = (int)(255 * (1 - norm));
        int green = (int)(255 * norm);
        return new Color(red, green, 0);
    }

    private Color getColorForNdmi(float value) {
        value = Math.max(-1f, Math.min(1f, value));
        float norm = (value + 1f) / 2f;

        if (norm < 0.5f) {
            int red = (int)(255 * (1 - norm));
            int green = (int)(255 * norm);
            return new Color(red, green, 0);
        } else {
            int green = (int)(255 * (1 - norm));
            int blue = (int)(255 * norm);
            return new Color(0, green, blue);
        }
    }
}
