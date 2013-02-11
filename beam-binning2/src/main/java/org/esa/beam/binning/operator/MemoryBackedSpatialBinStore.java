package org.esa.beam.binning.operator;

import org.esa.beam.binning.BinningContext;
import org.esa.beam.binning.SpatialBin;

import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.SortedMap;
import java.util.TreeMap;

/**
* An implementation of {@link SpatialBinStore} which simply stores the consumed {@link SpatialBin spatial bins} in a map.
*/
public class MemoryBackedSpatialBinStore implements SpatialBinStore {

    // Note, we use a sorted map in order to sort entries on-the-fly
    final private SortedMap<Long, List<SpatialBin>> spatialBinMap = new TreeMap<Long, List<SpatialBin>>();

    @Override
    public SortedSpatialBinList getSpatialBinMap() {
        return new SortedSpatialBinList() {
            @Override
            public Iterator<List<SpatialBin>> values() {
                return spatialBinMap.values().iterator();
            }

            @Override
            public long size() {
                return spatialBinMap.size();
            }

            @Override
            public boolean isEmpty() {
                return spatialBinMap.isEmpty();
            }

            @Override
            public void clear() {
                spatialBinMap.clear();
            }
        };
    }

    @Override
    public void consumeSpatialBins(BinningContext binningContext, List<SpatialBin> spatialBins) {

        for (SpatialBin spatialBin : spatialBins) {
            final long spatialBinIndex = spatialBin.getIndex();
            List<SpatialBin> spatialBinList = spatialBinMap.get(spatialBinIndex);
            if (spatialBinList == null) {
                spatialBinList = new ArrayList<SpatialBin>();
                spatialBinMap.put(spatialBinIndex, spatialBinList);
            }
            spatialBinList.add(spatialBin);
        }
    }

    @Override
    public void consumingCompleted() {
    }
}