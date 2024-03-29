/*
 * Copyright Elasticsearch B.V. and/or licensed to Elasticsearch B.V. under one
 * or more contributor license agreements. Licensed under the Elastic License
 * 2.0 and the Server Side Public License, v 1; you may not use this file except
 * in compliance with, at your election, the Elastic License 2.0 or the Server
 * Side Public License, v 1.
 */

package org.elasticsearch.aggregations.metric;

import org.elasticsearch.common.util.Maps;
import org.elasticsearch.search.aggregations.ParsedAggregation;
import org.elasticsearch.xcontent.ObjectParser;
import org.elasticsearch.xcontent.ParseField;
import org.elasticsearch.xcontent.XContentBuilder;
import org.elasticsearch.xcontent.XContentParser;

import java.io.IOException;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.Map;
import java.util.Objects;

public class ParsedMatrixStats extends ParsedAggregation {

    private final Map<String, Long> counts = new LinkedHashMap<>();
    private final Map<String, Double> means = new HashMap<>();
    private final Map<String, Double> variances = new HashMap<>();
    private final Map<String, Double> skewness = new HashMap<>();
    private final Map<String, Double> kurtosis = new HashMap<>();
    private final Map<String, Map<String, Double>> covariances = new HashMap<>();
    private final Map<String, Map<String, Double>> correlations = new HashMap<>();

    private long docCount;

    @Override
    public String getType() {
        return MatrixStatsAggregationBuilder.NAME;
    }

    private void setDocCount(long docCount) {
        this.docCount = docCount;
    }

    public long getDocCount() {
        return docCount;
    }

    public long getFieldCount(String field) {
        if (counts.containsKey(field) == false) {
            return 0;
        }
        return counts.get(field);
    }

    public double getMean(String field) {
        return checkedGet(means, field);
    }

    public double getVariance(String field) {
        return checkedGet(variances, field);
    }

    public double getSkewness(String field) {
        return checkedGet(skewness, field);
    }

    public double getKurtosis(String field) {
        return checkedGet(kurtosis, field);
    }

    public double getCovariance(String fieldX, String fieldY) {
        if (fieldX.equals(fieldY)) {
            return checkedGet(variances, fieldX);
        }
        return MatrixStatsResults.getValFromUpperTriangularMatrix(covariances, fieldX, fieldY);
    }

    public double getCorrelation(String fieldX, String fieldY) {
        if (fieldX.equals(fieldY)) {
            return 1.0;
        }
        return MatrixStatsResults.getValFromUpperTriangularMatrix(correlations, fieldX, fieldY);
    }

    @Override
    protected XContentBuilder doXContentBody(XContentBuilder builder, Params params) throws IOException {
        builder.field(CommonFields.DOC_COUNT.getPreferredName(), getDocCount());
        if (counts.isEmpty() == false) {
            builder.startArray(InternalMatrixStats.Fields.FIELDS);
            for (String fieldName : counts.keySet()) {
                builder.startObject();
                builder.field(InternalMatrixStats.Fields.NAME, fieldName);
                builder.field(InternalMatrixStats.Fields.COUNT, getFieldCount(fieldName));
                builder.field(InternalMatrixStats.Fields.MEAN, getMean(fieldName));
                builder.field(InternalMatrixStats.Fields.VARIANCE, getVariance(fieldName));
                builder.field(InternalMatrixStats.Fields.SKEWNESS, getSkewness(fieldName));
                builder.field(InternalMatrixStats.Fields.KURTOSIS, getKurtosis(fieldName));
                {
                    builder.startObject(InternalMatrixStats.Fields.COVARIANCE);
                    Map<String, Double> covars = covariances.get(fieldName);
                    if (covars != null) {
                        for (Map.Entry<String, Double> covar : covars.entrySet()) {
                            builder.field(covar.getKey(), covar.getValue());
                        }
                    }
                    builder.endObject();
                }
                {
                    builder.startObject(InternalMatrixStats.Fields.CORRELATION);
                    Map<String, Double> correls = correlations.get(fieldName);
                    if (correls != null) {
                        for (Map.Entry<String, Double> correl : correls.entrySet()) {
                            builder.field(correl.getKey(), correl.getValue());
                        }
                    }
                    builder.endObject();
                }
                builder.endObject();
            }
            builder.endArray();
        }
        return builder;
    }

    private static <T> T checkedGet(final Map<String, T> values, final String fieldName) {
        if (fieldName == null) {
            throw new IllegalArgumentException("field name cannot be null");
        }
        if (values.containsKey(fieldName) == false) {
            throw new IllegalArgumentException("field " + fieldName + " does not exist");
        }
        return values.get(fieldName);
    }

    private static final ObjectParser<ParsedMatrixStats, Void> PARSER = new ObjectParser<>(
        ParsedMatrixStats.class.getSimpleName(),
        true,
        ParsedMatrixStats::new
    );
    static {
        declareAggregationFields(PARSER);
        PARSER.declareLong(ParsedMatrixStats::setDocCount, CommonFields.DOC_COUNT);
        PARSER.declareObjectArray((matrixStats, results) -> {
            for (ParsedMatrixStatsResult result : results) {
                final String fieldName = result.name;
                matrixStats.counts.put(fieldName, result.count);
                matrixStats.means.put(fieldName, result.mean);
                matrixStats.variances.put(fieldName, result.variance);
                matrixStats.skewness.put(fieldName, result.skewness);
                matrixStats.kurtosis.put(fieldName, result.kurtosis);
                matrixStats.covariances.put(fieldName, result.covariances);
                matrixStats.correlations.put(fieldName, result.correlations);
            }
        }, (p, c) -> ParsedMatrixStatsResult.fromXContent(p), new ParseField(InternalMatrixStats.Fields.FIELDS));
    }

    public static ParsedMatrixStats fromXContent(XContentParser parser, String name) throws IOException {
        ParsedMatrixStats aggregation = PARSER.parse(parser, null);
        aggregation.setName(name);
        return aggregation;
    }

    static class ParsedMatrixStatsResult {

        String name;
        Long count;
        Double mean;
        Double variance;
        Double skewness;
        Double kurtosis;
        Map<String, Double> covariances;
        Map<String, Double> correlations;

        private static final ObjectParser<ParsedMatrixStatsResult, Void> RESULT_PARSER = new ObjectParser<>(
            ParsedMatrixStatsResult.class.getSimpleName(),
            true,
            ParsedMatrixStatsResult::new
        );
        static {
            RESULT_PARSER.declareString((result, name) -> result.name = name, new ParseField(InternalMatrixStats.Fields.NAME));
            RESULT_PARSER.declareLong((result, count) -> result.count = count, new ParseField(InternalMatrixStats.Fields.COUNT));
            RESULT_PARSER.declareDouble((result, mean) -> result.mean = mean, new ParseField(InternalMatrixStats.Fields.MEAN));
            RESULT_PARSER.declareDouble(
                (result, variance) -> result.variance = variance,
                new ParseField(InternalMatrixStats.Fields.VARIANCE)
            );
            RESULT_PARSER.declareDouble(
                (result, skewness) -> result.skewness = skewness,
                new ParseField(InternalMatrixStats.Fields.SKEWNESS)
            );
            RESULT_PARSER.declareDouble(
                (result, kurtosis) -> result.kurtosis = kurtosis,
                new ParseField(InternalMatrixStats.Fields.KURTOSIS)
            );

            RESULT_PARSER.declareObject((ParsedMatrixStatsResult result, Map<String, Object> covars) -> {
                result.covariances = Maps.newLinkedHashMapWithExpectedSize(covars.size());
                for (Map.Entry<String, Object> covar : covars.entrySet()) {
                    result.covariances.put(covar.getKey(), mapValueAsDouble(covar.getValue()));
                }
            }, (p, c) -> p.mapOrdered(), new ParseField(InternalMatrixStats.Fields.COVARIANCE));

            RESULT_PARSER.declareObject((ParsedMatrixStatsResult result, Map<String, Object> correls) -> {
                result.correlations = Maps.newLinkedHashMapWithExpectedSize(correls.size());
                for (Map.Entry<String, Object> correl : correls.entrySet()) {
                    result.correlations.put(correl.getKey(), mapValueAsDouble(correl.getValue()));
                }
            }, (p, c) -> p.mapOrdered(), new ParseField(InternalMatrixStats.Fields.CORRELATION));
        }

        private static Double mapValueAsDouble(Object value) {
            if (value instanceof Double) {
                return (Double) value;
            }
            return Double.valueOf(Objects.toString(value));
        }

        static ParsedMatrixStatsResult fromXContent(XContentParser parser) throws IOException {
            return RESULT_PARSER.parse(parser, null);
        }
    }
}
