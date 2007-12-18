package org.esa.beam.visat.toolviews.stat;

import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.ui.UIUtils;
import org.esa.beam.layer.ROILayer;

import javax.swing.*;
import java.awt.*;
import java.awt.event.ActionEvent;
import java.awt.event.ActionListener;


/**
 * A panel which performs the 'compute' action.
 *
 * @author Marco Peters
 */
class ComputePane extends JPanel {

    private static final LayerObserver roiLayerObserver = LayerObserver.getInstance(ROILayer.class);
    private final JButton computeButton;
    private final JCheckBox useRoiCheckBox;
    private RasterDataNode raster;

    public RasterDataNode getRaster() {
        return raster;
    }

    static ComputePane createComputePane(final ActionListener allPixelsActionListener,
                                         final ActionListener roiActionListener,
                                         final RasterDataNode raster) {
        return new ComputePane(allPixelsActionListener, roiActionListener, raster);
    }


    private ComputePane(final ActionListener allPixelsActionListener,
                        final ActionListener roiActionListener,
                        final RasterDataNode raster) {

        setRaster(raster);

        final Icon icon = UIUtils.loadImageIcon("icons/Gears20.gif");

        computeButton = new JButton("Compute");     /*I18N*/
        computeButton.setMnemonic('A');
        computeButton.setEnabled(raster != null);
        computeButton.addActionListener(new ActionListener() {
            public void actionPerformed(ActionEvent e) {
                if (useRoiCheckBox.isEnabled() && useRoiCheckBox.isSelected()) {
                    roiActionListener.actionPerformed(e);
                } else {
                    allPixelsActionListener.actionPerformed(e);
                }
            }
        });
        computeButton.setIcon(icon);

        useRoiCheckBox = new JCheckBox("Use ROI");     /*I18N*/
        useRoiCheckBox.setMnemonic('R');
        useRoiCheckBox.setEnabled(raster != null && raster.isROIUsable());

        roiLayerObserver.addLayerObserverListener(new LayerObserver.LayerObserverListener() {
            public void layerChanged() {
                useRoiCheckBox.setEnabled(isROIUsable());
            }
        });
        setLayout(new BorderLayout(2, 2));
        add(computeButton, BorderLayout.NORTH);
        add(useRoiCheckBox, BorderLayout.SOUTH);
    }

    private boolean isROIUsable() {
        return getRaster() != null && getRaster().isROIUsable();
    }

    public void setRaster(final RasterDataNode raster) {
        if (this.raster != raster) {
            this.raster = raster;
            roiLayerObserver.setRaster(this.raster);
            computeButton.setEnabled(this.raster != null);
            useRoiCheckBox.setEnabled(this.raster != null && this.raster.isROIUsable());
        }
    }

}
