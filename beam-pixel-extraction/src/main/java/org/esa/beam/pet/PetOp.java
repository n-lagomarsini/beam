/*
 * Copyright (C) 2010 Brockmann Consult GmbH (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the Free
 * Software Foundation; either version 3 of the License, or (at your option)
 * any later version.
 * This program is distributed in the hope that it will be useful, but WITHOUT
 * ANY WARRANTY; without even the implied warranty of MERCHANTABILITY or
 * FITNESS FOR A PARTICULAR PURPOSE. See the GNU General Public License for
 * more details.
 *
 * You should have received a copy of the GNU General Public License along
 * with this program; if not, see http://www.gnu.org/licenses/
 */

package org.esa.beam.pet;

import com.bc.ceres.binding.Property;
import com.bc.ceres.binding.ValidationException;
import com.bc.ceres.binding.Validator;
import org.esa.beam.dataio.placemark.PlacemarkIO;
import org.esa.beam.framework.dataio.ProductIO;
import org.esa.beam.framework.datamodel.GeoCoding;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.gpf.Operator;
import org.esa.beam.framework.gpf.OperatorException;
import org.esa.beam.framework.gpf.OperatorSpi;
import org.esa.beam.framework.gpf.annotations.OperatorMetadata;
import org.esa.beam.framework.gpf.annotations.Parameter;
import org.esa.beam.framework.gpf.annotations.SourceProducts;
import org.esa.beam.framework.gpf.annotations.TargetProperty;
import org.esa.beam.jai.ResolutionLevel;
import org.esa.beam.jai.VirtualBandOpImage;
import org.esa.beam.util.SystemUtils;
import org.esa.beam.util.math.MathUtils;

import javax.media.jai.operator.ConstantDescriptor;
import java.awt.Rectangle;
import java.awt.image.Raster;
import java.awt.image.RenderedImage;
import java.io.File;
import java.io.FileReader;
import java.io.FileWriter;
import java.io.IOException;
import java.io.StringWriter;
import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.logging.Logger;

@SuppressWarnings({
        "IOResourceOpenedButNotSafelyClosed", "MismatchedReadAndWriteOfArray",
        "FieldCanBeLocal", "UnusedDeclaration"
})
@OperatorMetadata(
        alias = "Pet",
        version = "1.0",
        authors = "Marco Peters, Thomas Storm",
        copyright = "(c) 2010 by Brockmann Consult",
        description = "Generates a CSV file from a given pixel location of source products.")
public class PetOp extends Operator {

    @SourceProducts()
    private Product[] sourceProducts;

    @TargetProperty()
    private Map<String, List<Measurement>> measurements;

    @Parameter(description = "The paths to be scanned for input products. May point to a single file or a directory.")
    private File[] inputPaths;

    @Parameter(description = "Specifies if bands are to be exported", defaultValue = "true")
    private Boolean exportBands;

    @Parameter(description = "Specifies if tie-points are to be exported", defaultValue = "true")
    private Boolean exportTiePoints;

    @Parameter(description = "Specifies if masks are to be exported", defaultValue = "true")
    private Boolean exportMasks;

    @Parameter(description = "The geo-coordinates", itemAlias = "coordinate")
    private Coordinate[] coordinates;

    @Parameter(description = "Path to a file containing geo-coordinates")
    private File coordinatesFile;

    @Parameter(description = "Side length of surrounding window (uneven)", defaultValue = "1",
               validator = WindowSizeValidator.class)
    private Integer windowSize;

    @Parameter(description = "The output directory.")
    private File outputDir;

    @Parameter(description = "Band maths expression (optional)")
    private String expression;

    @Parameter(description = "If true, the expression result is exported, otherwise the expression is used as filter.",
               defaultValue = "true")
    private Boolean exportExpressionResult;

    private Map<String, String[]> rasterNamesMap = new HashMap<String, String[]>(37);
    private ProductValidator validator;
    private List<Coordinate> coordinateList;
    private boolean isTargetProductInitialized = false;

    @Override
    public void initialize() throws OperatorException {
        if (coordinatesFile == null && coordinates == null) {
            throw new OperatorException("No coordinates specified.");
        }
        if (outputDir != null && outputDir.isFile()) {
            outputDir = new File(outputDir.getParent());
        }
        coordinateList = initCoordinateList();

        validator = new ProductValidator();
        measurements = new HashMap<String, List<Measurement>>();
        if (sourceProducts != null) {
            for (Product product : sourceProducts) {
                extractMeasurements(product);
            }
        }
        if (inputPaths != null) {
            inputPaths = cleanPathNames(inputPaths);
            extractMeasurements(inputPaths);
        }
        if (outputDir != null) {
            writeOutputToFile();
        } else {
            writeOutputToClipboard();
        }
        if (!isTargetProductInitialized) {
            setDummyProduct();
        }
    }

    Integer getWindowSize() {
        return windowSize;
    }

    void setWindowSize(Integer windowSize) {
        this.windowSize = windowSize;
    }

    Map<String, String[]> getRasterNamesMap() {
        return rasterNamesMap;
    }

    void setRasterNamesMap(Map<String, String[]> rasterNamesMap) {
        this.rasterNamesMap = rasterNamesMap;
    }

    void readMeasurement(Product product, Coordinate coordinate, int coordinateID,
                         Map<String, List<Measurement>> measurements) throws IOException {
        PixelPos centerPos = product.getGeoCoding().getPixelPos(coordinate.getGeoPos(), null);
        if (!product.containsPixel(centerPos)) {
            return;
        }
        String productType = product.getProductType();
        String[] rasterNames = rasterNamesMap.get(productType);
        int offset = MathUtils.floorInt(windowSize / 2);
        int upperLeftX = MathUtils.floorInt(centerPos.x - offset);
        int upperLeftY = MathUtils.floorInt(centerPos.y - offset);
        final double[] values = new double[rasterNames.length];
        Arrays.fill(values, Double.NaN);
        final int numPixels = windowSize * windowSize;
        final RenderedImage expressionImage;
        if (expression != null) {
            expressionImage = VirtualBandOpImage.create(expression, ProductData.TYPE_UINT8, 0,
                                                        product, ResolutionLevel.MAXRES);
        } else {
            expressionImage = ConstantDescriptor.create((float) product.getSceneRasterWidth(),
                                                        (float) product.getSceneRasterHeight(),
                                                        new Byte[]{-1}, null);
        }
        final Raster validData = expressionImage.getData(new Rectangle(upperLeftX, upperLeftY, windowSize, windowSize));
        for (int n = 0; n < numPixels; n++) {
            int x = upperLeftX + n % windowSize;
            int y = upperLeftY + n / windowSize;
            for (int i = 0; i < rasterNames.length; i++) {
                RasterDataNode raster = product.getRasterDataNode(rasterNames[i]);
                if (raster != null && product.containsPixel(x, y)) {
                    double[] temp = new double[1];
                    raster.readPixels(x, y, 1, 1, temp);
                    values[i] = temp[0];
                }
            }
            GeoPos currentGeoPos = product.getGeoCoding().getGeoPos(new PixelPos(x, y), null);
            boolean isValid = validData.getSample(x,y, 0) != 0;
            final Measurement measure = new Measurement(coordinateID, coordinate.getName(),
                                                        product.getStartTime(), currentGeoPos, values, isValid);
            List<Measurement> measurementList = measurements.get(productType);
            if (measurementList == null) {
                measurementList = new ArrayList<Measurement>();
                measurements.put(productType, measurementList);
            }
            measurementList.add(measure);
        }
    }

    private List<Coordinate> initCoordinateList() {
        List<Coordinate> list = new ArrayList<Coordinate>();
        if (coordinatesFile != null) {
            list.addAll(extractGeoPositions(coordinatesFile));
        }
        if (coordinates != null) {
            list.addAll(Arrays.asList(coordinates));
        }
        return list;
    }

    private List<Coordinate> extractGeoPositions(File coordinatesFile) {
        final List<Coordinate> coordinateList = new ArrayList<Coordinate>();
        try {
            final List<Placemark> pins = PlacemarkIO.readPlacemarks(new FileReader(coordinatesFile),
                                                                    null, // no GeoCoding needed
                                                                    PinDescriptor.INSTANCE);
            for (Placemark pin : pins) {
                final GeoPos geoPos = pin.getGeoPos();
                if (geoPos != null) {
                    coordinateList.add(new Coordinate(pin.getName(), geoPos));
                }
            }
        } catch (IOException ignore) {
            return Collections.emptyList();
        }
        return coordinateList;
    }

    private void extractMeasurements(File[] files) {
        for (File file : files) {
            if (file.isDirectory()) {
                final File[] subFiles = file.listFiles();
                for (File subFile : subFiles) {
                    if (subFile.isFile()) {
                        extractMeasurements(subFile);
                    }
                }
            } else {
                extractMeasurements(file);
            }
        }
    }

    private void extractMeasurements(File file) {
        Product product = null;
        try {
            product = ProductIO.readProduct(file);
            extractMeasurements(product);
        } catch (IOException ignore) {
        } finally {
            if (product != null) {
                if (isTargetProductInitialized) {
                    product.dispose();
                } else {
                    setTargetProduct(product);
                    isTargetProductInitialized = true;
                }
            }
        }
    }

    private void extractMeasurements(Product product) {
        if (!validator.validate(product)) {
            return;
        }

        if (!rasterNamesMap.containsKey(product.getProductType())) {
            rasterNamesMap.put(product.getProductType(), getAllRasterNames(product));
        }

        for (int i = 0, coordinateListSize = coordinateList.size(); i < coordinateListSize; i++) {
            Coordinate coordinate = coordinateList.get(i);
            try {
                readMeasurement(product, coordinate, i + 1, measurements);
            } catch (IOException e) {
                getLogger().warning(e.getMessage());
            }
        }
    }

    private String[] getAllRasterNames(Product product) {
        final List<RasterDataNode> allRasterList = new ArrayList<RasterDataNode>();
        if (exportBands) {
            allRasterList.addAll(Arrays.asList(product.getBands()));
        }
        if (exportTiePoints) {
            allRasterList.addAll(Arrays.asList(product.getTiePointGrids()));
        }
        if (exportMasks) {
            allRasterList.addAll(Arrays.asList(product.getMaskGroup().toArray(new Mask[0])));
        }
        String[] allRasterNames = new String[allRasterList.size()];
        for (int i = 0; i < allRasterList.size(); i++) {
            allRasterNames[i] = allRasterList.get(i).getName();
        }
        return allRasterNames;
    }

    private void setDummyProduct() {
        final Product product = new Product("dummy", "dummy", 2, 2);
        product.addBand("dummy", ProductData.TYPE_INT8);
        setTargetProduct(product);
    }

    @SuppressWarnings({"IOResourceOpenedButNotSafelyClosed"})
    private void writeOutputToFile() {
        FileWriter writer = null;

        for (String productType : measurements.keySet()) {
            List<Measurement> measurementList = measurements.get(productType);
            try {
                String[] rasterNames = rasterNamesMap.get(productType);
                writer = new FileWriter(new File(outputDir.getAbsolutePath(), "expix_" + productType + ".txt"));
                MeasurementWriter.write(measurementList, writer, rasterNames, expression, exportExpressionResult);
            } catch (IOException e) {
                throw new OperatorException("Could not write to output file.", e);
            } finally {
                try {
                    if (writer != null) {
                        writer.close();
                    }
                } catch (IOException ignored) {
                }
            }
        }
    }

    private void writeOutputToClipboard() {
        StringWriter stringWriter = new StringWriter();
        for (String productType : measurements.keySet()) {
            List<Measurement> measurementList = measurements.get(productType);
            try {
                String[] rasterNames = rasterNamesMap.get(productType);
                MeasurementWriter.write(measurementList, stringWriter, rasterNames, expression, exportExpressionResult);
                stringWriter.append("\n\n");
            } finally {
                try {
                    stringWriter.close();
                } catch (IOException ignore) {
                }
            }
        }

        SystemUtils.copyToClipboard(stringWriter.toString());
    }

    private static File[] cleanPathNames(File[] paths) {
        for (int i = 0; i < paths.length; i++) {
            File path = paths[i];
            paths[i] = new File(path.getPath().trim());
        }
        return paths;
    }

    private class ProductValidator {

        public boolean validate(Product product) {
            final Logger logger = getLogger();
            if (product == null) {
                return false;
            }
            final GeoCoding geoCoding = product.getGeoCoding();
            if (geoCoding == null) {
                final String msgPattern = "Product [%s] refused. Cause: Product is not geo-coded.";
                logger.warning(String.format(msgPattern, product.getFileLocation()));
                return false;
            }
            if (!geoCoding.canGetPixelPos()) {
                final String msgPattern = "Product [%s] refused. Cause: Pixel position can not be determined.";
                logger.warning(String.format(msgPattern, product.getFileLocation()));
                return false;
            }
            return true;
        }
    }

    /**
     * The Service Provider Interface (SPI) for the operator.
     * It provides operator meta-data and is a factory for new operator instances.
     */
    public static class Spi extends OperatorSpi {

        public Spi() {
            super(PetOp.class);
        }
    }

    public static class WindowSizeValidator implements Validator {

        @Override
        public void validateValue(Property property, Object value) throws ValidationException {
            if (((Integer) value) % 2 == 0) {
                throw new ValidationException("Value of squareSize must be uneven");
            }
        }
    }

}