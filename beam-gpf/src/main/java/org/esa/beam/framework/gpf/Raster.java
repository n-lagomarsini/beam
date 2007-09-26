package org.esa.beam.framework.gpf;

import org.esa.beam.framework.datamodel.ProductData;
import org.esa.beam.framework.datamodel.RasterDataNode;

import java.awt.Rectangle;

// todo - rename to Tile (nf - 26.09.2007)
/**
 * A tile.
 */
public interface Raster {

    /**
     * Checks if this is a target tile. Non-target tiles are read only.
     *
     * @return <code>true</code> if this is a target tile.
     */
    boolean isTarget();

    /**
     * The tile rectangle in raster coordinates.
     *
     * @return the tile rectangle
     */
    Rectangle getRectangle();

    /**
     * Gets the x-offset of the {@link #getRectangle() rectangle} within the scene covered by the {@link RasterDataNode}.
     *
     * @return the x-offset
     */
    int getOffsetX();

    /**
     * Gets the y-offset of the {@link #getRectangle() rectangle} within the scene covered by the {@link RasterDataNode}.
     *
     * @return the y-offset
     */
    int getOffsetY();

    /**
     * Gets the width of the {@link #getRectangle() rectangle} within the scene covered by the {@link RasterDataNode}.
     *
     * @return the width
     */
    int getWidth();

    /**
     * Gets the height of the {@link #getRectangle() rectangle} within the scene covered by the {@link RasterDataNode}.
     *
     * @return the height
     */
    int getHeight();

    /**
     * The raster dataset to which this raster belongs to.
     *
     * @return the raster data node of a data product, e.g. a {@link org.esa.beam.framework.datamodel.Band} or
     *         {@link org.esa.beam.framework.datamodel.TiePointGrid TiePointGrid}.
     */
    RasterDataNode getRasterDataNode();

    // todo - rename to getSampleData (nf - 26.09.2007)
    /**
     * Gets the (raw) sample data of this tile's underlying raster.
     * <p>The number of samples equals
     * <code>width*height</code> of this tile's {@link #getRectangle() rectangle}.</p>
     * <p>Note: Changing the samples will not necessarily
     * alter the underlying tile raster data before the {@link #setSampleData(org.esa.beam.framework.datamodel.ProductData) setProductData()}
     * method is called with the modified sample data.</p>
     *
     * @return the sample data
     */
    ProductData getDataBuffer();

    /**
     * Gets the (raw) sample data of this tile's underlying raster.
     * <p>The number of samples must equal
     * <code>width*height</code> of this tile's {@link #getRectangle() rectangle}.</p>
     *
     * @param sampleData the sample data
     * @see #getDataBuffer()
     */
    void setSampleData(ProductData sampleData);

    /**
     * Gets the integer value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     * @return the integer value
     */
    int getInt(int x, int y);

    /**
     * Sets the integer value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     * @param v the integer value
     */
    void setInt(int x, int y, int v);

    /**
     * Gets the float value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     * @return the float value
     */
    float getFloat(int x, int y);

    /**
     * Sets the float value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     * @param v the float value
     */
    void setFloat(int x, int y, float v);

    /**
     * Gets the double value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     * @return the double value
     */
    double getDouble(int x, int y);

    /**
     * Sets the double value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     * @param v the double value
     */
    void setDouble(int x, int y, double v);

    /**
     * Gets the boolean value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     * @return the boolean value
     */
    boolean getBoolean(int x, int y);

    /**
     * Sets the boolean value at the given position.
     *
     * @param x x-coordinate within the raster of the {@link RasterDataNode}
     * @param y y-coordinate within the raster of the {@link RasterDataNode}
     * @param v the boolean value
     */
    void setBoolean(int x, int y, boolean v);
}
