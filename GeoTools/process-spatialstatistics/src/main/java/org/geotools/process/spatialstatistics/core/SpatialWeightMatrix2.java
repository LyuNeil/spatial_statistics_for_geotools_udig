/*
 *    GeoTools - The Open Source Java GIS Toolkit
 *    http://geotools.org
 *
 *    (C) 2014, Open Source Geospatial Foundation (OSGeo)
 *
 *    This library is free software; you can redistribute it and/or
 *    modify it under the terms of the GNU Lesser General Public
 *    License as published by the Free Software Foundation;
 *    version 2.1 of the License.
 *
 *    This library is distributed in the hope that it will be useful,
 *    but WITHOUT ANY WARRANTY; without even the implied warranty of
 *    MERCHANTABILITY or FITNESS FOR A PARTICULAR PURPOSE.  See the GNU
 *    Lesser General Public License for more details.
 */
package org.geotools.process.spatialstatistics.core;

import java.util.ArrayList;
import java.util.List;
import java.util.logging.Logger;

import org.geotools.data.simple.SimpleFeatureCollection;
import org.geotools.data.simple.SimpleFeatureIterator;
import org.geotools.factory.CommonFactoryFinder;
import org.geotools.factory.GeoTools;
import org.geotools.process.spatialstatistics.enumeration.SpatialConcept;
import org.geotools.process.spatialstatistics.enumeration.StandardizationMethod;
import org.geotools.util.logging.Logging;
import org.opengis.feature.simple.SimpleFeature;
import org.opengis.filter.FilterFactory2;
import org.opengis.filter.expression.Expression;

import com.vividsolutions.jts.geom.Coordinate;
import com.vividsolutions.jts.geom.Geometry;

/**
 * SpatialWeightMatrix 2
 * 
 * @author Minpa Lee, MangoSystem
 * 
 * @source $URL$
 */
public class SpatialWeightMatrix2 extends SpatialWeightMatrix {
    protected static final Logger LOGGER = Logging.getLogger(SpatialWeightMatrix2.class);

    public double dPopSum = 0;

    public double dPop2Sum = 0;

    public double dPop3Sum = 0;

    public double dPop4Sum = 0;

    public SpatialWeightMatrix2() {
    }

    public SpatialWeightMatrix2(SpatialConcept spatialConcept,
            StandardizationMethod standardizationMethod) {
        this.spatialConcept = spatialConcept;
        this.standardizationMethod = standardizationMethod;
        this.beta = spatialConcept == SpatialConcept.InverseDistanceSquared ? 2.0 : 1.0;
    }

    public void buildWeightMatrix(SimpleFeatureCollection inputFeatures, String obsField,
            String popField) {

        this.events = loadEvents(inputFeatures, obsField, popField);

        if (distanceBandWidth == 0) {
            calculateDistanceBand();
        }

        if (standardizationMethod == StandardizationMethod.Row) {
            calculateRowSum();
        }
    }

    private List<SpatialEvent> loadEvents(SimpleFeatureCollection features, String obsField,
            String popField) {
        List<SpatialEvent> eventList = new ArrayList<SpatialEvent>();

        this.dZSum = this.dZ2Sum = this.dZ3Sum = this.dZ4Sum = 0.0;
        this.dPopSum = this.dPop2Sum = this.dPop3Sum = this.dPop4Sum = 0.0;

        obsField = FeatureTypes.validateProperty(features.getSchema(), obsField);
        popField = FeatureTypes.validateProperty(features.getSchema(), popField);

        FilterFactory2 ff = CommonFactoryFinder.getFilterFactory2(GeoTools.getDefaultHints());
        Expression obsExpression = ff.property(obsField);
        Expression popExpression = ff.property(popField);

        SimpleFeatureIterator featureIter = features.features();
        try {
            while (featureIter.hasNext()) {
                SimpleFeature feature = featureIter.next();
                Geometry geometry = (Geometry) feature.getDefaultGeometry();
                Coordinate coordinate = geometry.getCentroid().getCoordinate();

                SpatialEvent sEvent = new SpatialEvent(feature.getID(), coordinate);
                sEvent.weight = getValue(feature, obsExpression);

                dZSum += sEvent.weight;
                dZ2Sum += Math.pow(sEvent.weight, 2.0);
                dZ3Sum += Math.pow(sEvent.weight, 3.0);
                dZ4Sum += Math.pow(sEvent.weight, 4.0);

                sEvent.population = getValue(feature, popExpression);

                dPopSum += sEvent.population;
                dPop2Sum += Math.pow(sEvent.population, 2.0);
                dPop3Sum += Math.pow(sEvent.population, 3.0);
                dPop4Sum += Math.pow(sEvent.population, 4.0);

                eventList.add(sEvent);
            }
        } finally {
            featureIter.close();
        }

        return eventList;
    }
}
