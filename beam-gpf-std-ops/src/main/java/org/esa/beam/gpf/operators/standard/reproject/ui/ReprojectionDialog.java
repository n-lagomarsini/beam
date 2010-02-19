package org.esa.beam.gpf.operators.standard.reproject.ui;

import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.dataop.dem.ElevationModelDescriptor;
import org.esa.beam.framework.dataop.dem.ElevationModelRegistry;
import org.esa.beam.framework.gpf.GPF;
import org.esa.beam.framework.gpf.ui.DefaultAppContext;
import org.esa.beam.framework.gpf.ui.SingleTargetProductDialog;
import org.esa.beam.framework.ui.AppContext;
import org.opengis.referencing.crs.CoordinateReferenceSystem;

import java.util.Map;

/**
 * User: Marco
 * Date: 16.08.2009
 */
class ReprojectionDialog extends SingleTargetProductDialog {

    private final ReprojectionForm form;

    public static void main(String[] args) {
        final DefaultAppContext context = new DefaultAppContext("Reproj");
        final ReprojectionDialog dialog = new ReprojectionDialog(true, "ReproTestDialog", null, context);
        dialog.show();

    }
    ReprojectionDialog(boolean orthorectify, final String title, final String helpID, AppContext appContext) {
        super(appContext, title, helpID);
        form = new ReprojectionForm(getTargetProductSelector(), orthorectify, appContext);
    }

    @Override
    protected boolean verifyUserInput() {
        if(form.getSourceProduct() == null) {
            showErrorDialog("No product to reproject selected.");
            return false;
        }

        final CoordinateReferenceSystem crs = form.getSelectedCrs();
        if(crs == null) {
            showErrorDialog("No 'Coordinate Reference System' selected.");
            return false;
        }

        String externalDemName = form.getExternalDemName();
        if (externalDemName != null) {
            final ElevationModelRegistry elevationModelRegistry = ElevationModelRegistry.getInstance();
            final ElevationModelDescriptor demDescriptor = elevationModelRegistry.getDescriptor(externalDemName);
            if (demDescriptor == null) {
                showErrorDialog("The DEM '" + externalDemName + "' is not supported.");
                close();
                return false;
            }
            if (demDescriptor.isInstallingDem()) {
                showErrorDialog("The DEM '" + externalDemName + "' is currently being installed.");
                close();
                return false;
            }
            if (!demDescriptor.isDemInstalled()) {
                final boolean ok = demDescriptor.installDemFiles(getParent());
                if (ok) {
                    // close dialog becuase DEM will be installed first
                    close();
                }
                return false;
            }
        }
        return true;
    }

    @Override
    protected Product createTargetProduct() throws Exception {
        final Map<String, Product> productMap = form.getProductMap();
        final Map<String, Object> parameterMap = form.getParameterMap();
        return GPF.createProduct("Reproject", parameterMap, productMap);
    }

    @Override
    public int show() {
        form.prepareShow();
        setContent(form);
        return super.show();
    }

    @Override
    public void hide() {
        form.prepareHide();
        super.hide();
    }

}