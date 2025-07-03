package pw.edu.javandmi_ndvi.core;

import org.geotools.api.referencing.crs.CoordinateReferenceSystem;
import org.geotools.coverage.grid.GridCoverage2D;
import org.geotools.coverage.processing.Operations;
import org.geotools.gce.geotiff.GeoTiffReader;

import javax.media.jai.Interpolation;
import java.io.File;
import java.io.IOException;

public class GeoUtils {

    public static GridCoverage2D readCoverage(String filepath) {
        try {
            File file = new File(filepath);

            GeoTiffReader reader = new GeoTiffReader(file);

            return reader.read(null);
        } catch (IOException e) {
            return null;
        }
    }

    public static GridCoverage2D resampleCoverage(GridCoverage2D source, GridCoverage2D target) {
        CoordinateReferenceSystem crs = target.getCoordinateReferenceSystem();
        Operations ops = new Operations(null);

        return (GridCoverage2D) ops.resample(source, crs, target.getGridGeometry(), Interpolation.getInstance(Interpolation.INTERP_BILINEAR));
    }
}
