/*
 * $Id: $
 *
 * Copyright (C) 2010 by Brockmann Consult (info@brockmann-consult.de)
 *
 * This program is free software; you can redistribute it and/or modify it
 * under the terms of the GNU General Public License as published by the
 * Free Software Foundation. This program is distributed in the hope it will
 * be useful, but WITHOUT ANY WARRANTY; without even the implied warranty
 * of MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.
 * See the GNU General Public License for more details.
 *
 * You should have received a copy of the GNU General Public License
 * along with this program; if not, write to the Free Software
 * Foundation, Inc., 59 Temple Place - Suite 330, Boston, MA  02111-1307, USA.
 */
package org.esa.beam.visat.internal;

import com.bc.ceres.core.Assert;

import org.esa.beam.framework.datamodel.Band;
import org.esa.beam.framework.datamodel.Mask;
import org.esa.beam.framework.datamodel.Product;
import org.esa.beam.framework.datamodel.ProductNodeGroup;
import org.esa.beam.framework.datamodel.RasterDataNode;
import org.esa.beam.framework.datamodel.TiePointGrid;
import org.esa.beam.framework.datamodel.VectorDataNode;
import org.esa.beam.framework.datamodel.VirtualBand;
import org.esa.beam.util.Debug;
import org.esa.beam.visat.VisatApp;

import java.beans.PropertyVetoException;
import java.text.MessageFormat;
import java.util.ArrayList;
import java.util.Collection;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import javax.swing.JInternalFrame;
import javax.swing.JOptionPane;

/**
 * Confirms Raster Data Node deletion by the user and performs them.
 *
 * @author Marco Zuehlke
 * @version $Revision$ $Date$
 * @since BEAM 4.7
 */
public class RasterDataNodeDeleter {
    
    private static final String INDENT = "    ";
    
    public static void deleteVectorDataNode(VectorDataNode vectorDataNode) {
        String message = MessageFormat.format("Do you really want to delete the geometry ''{0}''?\nThis action cannot be undone.\n\n", vectorDataNode.getName());
        int status = VisatApp.getApp().showQuestionDialog("Delete Geometry",
                                                          message, null);
        if (status == JOptionPane.YES_OPTION) {
            Product product = vectorDataNode.getProduct();
            product.getVectorDataGroup().remove(vectorDataNode);
        }
    }
    
    public static void deleteRasterDataNodes(RasterDataNode[] rasterNodes) {
        Assert.notNull(rasterNodes);
        if (rasterNodes.length == 0) {
            return;
        }
        Set<RasterDataNode> virtualBandsSet = new HashSet<RasterDataNode>();
        Set<RasterDataNode> validMaskNodesSet = new HashSet<RasterDataNode>();
        Set<RasterDataNode> masksSet = new HashSet<RasterDataNode>();
        
        for (RasterDataNode raster : rasterNodes) {
            virtualBandsSet.addAll(getReferencedVirtualBands(raster));
            validMaskNodesSet.addAll(getReferencedValidMasks(raster));
            masksSet.addAll(getReferencedMasks(raster));
        }
        for (RasterDataNode raster : rasterNodes) {
            virtualBandsSet.remove(raster);
            validMaskNodesSet.remove(raster);
            masksSet.remove(raster);
        }
        String message = formatPromptMessage(rasterNodes, virtualBandsSet, validMaskNodesSet, masksSet);
        deleteRasterDataNodesImpl(rasterNodes, message);
    }
    
    public static void deleteRasterDataNode(RasterDataNode raster) {
        Assert.notNull(raster);
        List<RasterDataNode> virtualBands = getReferencedVirtualBands(raster);
        List<RasterDataNode> validMaskNodes = getReferencedValidMasks(raster);
        List<RasterDataNode> masks = getReferencedMasks(raster);
        
        RasterDataNode[] rasters = new RasterDataNode[] {raster};
        String message = formatPromptMessage(rasters, virtualBands, validMaskNodes, masks);
        deleteRasterDataNodesImpl(rasters, message);
    }

    private static void deleteRasterDataNodesImpl(RasterDataNode[] rasters, String message) {

        final int status = VisatApp.getApp().showQuestionDialog("Delete Raster Data",
                                                                message, null);
        if (status == JOptionPane.YES_OPTION) {
            for (RasterDataNode raster : rasters) {
                final JInternalFrame[] internalFrames = VisatApp.getApp().findInternalFrames(raster);
                for (final JInternalFrame internalFrame : internalFrames) {
                    try {
                        internalFrame.setClosed(true);
                    } catch (PropertyVetoException e) {
                        Debug.trace(e);
                    }
                }
                if (raster.hasRasterData()) {
                    raster.unloadRasterData();
                }
                final Product product = raster.getProduct();
                if (raster instanceof Mask) {
                    Mask mask = (Mask) raster;
                    product.getMaskGroup().remove(mask);
                    for (Band band : product.getBands()) {
                        deleteMaskFromGroup(band.getRoiMaskGroup(), mask);
                        deleteMaskFromGroup(band.getOverlayMaskGroup(), mask);
                    }
                    TiePointGrid[] tiePointGrids = product.getTiePointGrids();
                    for (TiePointGrid tiePointGrid : tiePointGrids) {
                        deleteMaskFromGroup(tiePointGrid.getRoiMaskGroup(), mask);
                        deleteMaskFromGroup(tiePointGrid.getOverlayMaskGroup(), mask);
                    }
                } else if (raster instanceof Band) {
                    product.removeBand((Band) raster);
                } else if (raster instanceof TiePointGrid) {
                    product.removeTiePointGrid((TiePointGrid) raster);
                }
            }
        }
    }
    
    private static String formatPromptMessage(RasterDataNode[] rasters, 
                                              Collection<RasterDataNode> virtualBands, Collection<RasterDataNode> validMaskNodes,
                                              Collection<RasterDataNode> masks) {
        String description = getDescription(rasters);
        
        String name;
        StringBuilder message = new StringBuilder();
        if ((rasters.length>1)) {
            message.append(MessageFormat.format("Do you really want to delete the following {0}:\n", description));
            for (RasterDataNode raster : rasters) {
                message.append(INDENT);
                message.append(raster.getName());
                message.append("\n");
            }
        } else {
            name = rasters[0].getName();
            message.append(MessageFormat.format("Do you really want to delete the {0} ''{1}''?\n", description, name));
        }
        message.append("This action cannot be undone.\n\n");
        
        if (!virtualBands.isEmpty()
                || !validMaskNodes.isEmpty()
                || !masks.isEmpty()) {
            if ((rasters.length>1)) {
                message.append(MessageFormat.format("The {0} to be deleted are referenced by\n", description));
            } else {
                message.append(MessageFormat.format("The {0} to be deleted is referenced by\n", description));
            }
        }
        if (!virtualBands.isEmpty()) {
            message.append("the expression of virtual band(s):\n");
            for (RasterDataNode virtualBand : virtualBands) {
                message.append(INDENT);
                message.append(virtualBand.getName());
                message.append("\n");
            }
        }
        if (!validMaskNodes.isEmpty()) {
            message.append("the valid-mask expression of band(s) or tie-point grid(s)\n");
            for (RasterDataNode validMaskNode : validMaskNodes) {
                message.append(INDENT);
                message.append(validMaskNode.getName());
                message.append("\n");
            }
        }
        if (!masks.isEmpty()) {
            message.append("the mask(s):\n");
            for (RasterDataNode mask : masks) {
                message.append(INDENT);
                message.append(mask.getName());
                message.append("\n");
            }
        }
        return message.toString();
    }
    
    private static String getDescription(RasterDataNode[] rasters) {
        String description = "";
        if (rasters[0] instanceof Mask) {
            description = "mask";
        } else if (rasters[0] instanceof Band) {
            description = "band";
        } else if (rasters[0] instanceof TiePointGrid) {
            description = "tie-point grid";
        }
        if (rasters.length>1) {
            description += "s";
        }
        return description;
    }
    
    private static void deleteMaskFromGroup(ProductNodeGroup<Mask> group, Mask mask) {
        if (group.contains(mask)) {
            group.remove(mask);
        }
    }

    private static List<RasterDataNode> getReferencedValidMasks(final RasterDataNode node) {
        final Product product = node.getProduct();
        final List<RasterDataNode> rasterList = new ArrayList<RasterDataNode>();
        if (product != null) {
            for (int i = 0; i < product.getNumBands(); i++) {
                final Band band = product.getBandAt(i);
                if (band != node) {
                    if (isNodeReferencedByExpression(node, band.getValidPixelExpression())) {
                        rasterList.add(band);
                    }
                }
            }
            for (int i = 0; i < product.getNumTiePointGrids(); i++) {
                final TiePointGrid tiePointGrid = product.getTiePointGridAt(i);
                if (tiePointGrid != node) {
                    if (isNodeReferencedByExpression(node, tiePointGrid.getValidPixelExpression())) {
                        rasterList.add(tiePointGrid);
                    }
                }
            }
        }
        return rasterList;
    }

    private static List<RasterDataNode> getReferencedMasks(final RasterDataNode node) {
        final Product product = node.getProduct();
        final List<RasterDataNode> rasterList = new ArrayList<RasterDataNode>();
        if (product != null) {
            final ProductNodeGroup<Mask> maskGroup = product.getMaskGroup();
            final Mask[] masks = maskGroup.toArray(new Mask[maskGroup.getNodeCount()]);
            for (final Mask mask : masks) {
                final String expression;
                if (mask.getImageType() instanceof Mask.BandMathType) {
                    expression = Mask.BandMathType.getExpression(mask);
                } else if (mask.getImageType() instanceof Mask.RangeType) {
                    expression = Mask.RangeType.getRasterName(mask);
                } else {
                    expression = null;
                }
                if (isNodeReferencedByExpression(node, expression)) {
                    rasterList.add(mask);
                }
            }
        }
        return rasterList;
    }

    private static List<RasterDataNode> getReferencedVirtualBands(final RasterDataNode node) {
        final Product product = node.getProduct();
        final List<RasterDataNode> rasterList = new ArrayList<RasterDataNode>();
        if (product != null) {
            for (int i = 0; i < product.getNumBands(); i++) {
                final Band band = product.getBandAt(i);
                if (band instanceof VirtualBand) {
                    final VirtualBand virtualBand = (VirtualBand) band;
                    if (isNodeReferencedByExpression(node, virtualBand.getExpression())) {
                        rasterList.add(virtualBand);
                    }
                }
            }
        }
        return rasterList;
    }

    @SuppressWarnings({"SimplifiableIfStatement"})
    private static boolean isNodeReferencedByExpression(RasterDataNode node, String expression) {
        if (expression == null || expression.trim().isEmpty()) {
            return false;
        }

        return expression.matches(".*\\b" + node.getName() + "\\b.*");
    }
}