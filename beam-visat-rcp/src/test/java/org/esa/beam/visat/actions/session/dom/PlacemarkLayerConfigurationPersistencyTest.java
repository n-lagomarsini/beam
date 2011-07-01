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

package org.esa.beam.visat.actions.session.dom;

import com.bc.ceres.binding.PropertySet;
import com.bc.ceres.glayer.Layer;
import com.bc.ceres.glayer.LayerType;
import com.bc.ceres.glayer.LayerTypeRegistry;
import org.esa.beam.framework.datamodel.GeoPos;
import org.esa.beam.framework.datamodel.PinDescriptor;
import org.esa.beam.framework.datamodel.PixelPos;
import org.esa.beam.framework.datamodel.Placemark;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.glayer.PlacemarkLayerType;
import org.junit.After;
import org.junit.Before;

import java.awt.geom.AffineTransform;

public class PlacemarkLayerConfigurationPersistencyTest extends AbstractLayerConfigurationPersistencyTest {

    private Product product;

    public PlacemarkLayerConfigurationPersistencyTest() {
        super(LayerTypeRegistry.getLayerType(PlacemarkLayerType.class));
    }

    @Before
    public void setup() {
        product = createTestProduct("Test", "Test");
        final Placemark placemark = createPlacemark("Pin");
        product.getPinGroup().add(placemark);

        getProductManager().addProduct(product);
    }

    @After
    public void tearDown() {
        getProductManager().removeProduct(product);
    }

    @Override
    protected Layer createLayer(LayerType layerType) throws Exception {
        final PropertySet configuration = layerType.createLayerConfig(null);
        configuration.setValue("product", product);
        configuration.setValue("placemarkDescriptor", PinDescriptor.INSTANCE);
        configuration.setValue("imageToModelTransform", new AffineTransform());
        return layerType.createLayer(null, configuration);
    }

    private Placemark createPlacemark(String name) {
        return new Placemark(name, "", "", new PixelPos(), new GeoPos(), PinDescriptor.INSTANCE, product.getGeoCoding());
    }
}