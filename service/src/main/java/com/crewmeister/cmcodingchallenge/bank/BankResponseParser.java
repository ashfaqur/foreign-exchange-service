package com.crewmeister.cmcodingchallenge.bank;

import com.fasterxml.jackson.databind.JsonNode;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.time.format.DateTimeParseException;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class BankResponseParser {
    private static final Logger log = LoggerFactory.getLogger(BankResponseParser.class);
    private static final String DIM_CURRENCY = "BBK_STD_CURRENCY";
    private static final String DIM_TIME_PERIOD = "TIME_PERIOD";
    private static final double WARN_SKIP_RATIO = 0.50;

    public List<ExchangeRateRow> parseRates(JsonNode root) {
        if (!isObjectNode(root)) {
            return List.of();
        }

        JsonNode data = safePathObject(root, "data");
        JsonNode structure = safePathObject(data, "structure");
        JsonNode dims = safePathObject(structure, "dimensions");
        if (!isObjectNode(data) || !isObjectNode(structure) || !isObjectNode(dims)) {
            return List.of();
        }

        List<LocalDate> dates = extractDates(dims);
        List<String> currencies = extractCurrencies(dims);
        if (dates.isEmpty() || currencies.isEmpty()) {
            return List.of();
        }

        JsonNode seriesNode = data.path("dataSets").path(0).path("series");
        if (seriesNode.isMissingNode() || !seriesNode.isObject()) {
            return List.of();
        }

        List<ExchangeRateRow> out = new ArrayList<>();
        int skippedSeries = 0;
        int skippedObservations = 0;

        Iterator<Map.Entry<String, JsonNode>> seriesIt = seriesNode.fields();
        while (seriesIt.hasNext()) {
            var entry = seriesIt.next();
            String seriesKey = entry.getKey();
            JsonNode series = entry.getValue();

            Integer currencyIndex = parseCurrencyIndex(seriesKey);
            if (currencyIndex == null || currencyIndex < 0 || currencyIndex >= currencies.size()){
                skippedSeries++;
                continue;
            }
            String currency = currencies.get(currencyIndex);

            JsonNode observations = series.path("observations");
            if (!observations.isObject()){
                skippedSeries++;
                continue;
            }
            Iterator<Map.Entry<String, JsonNode>> obsIt = observations.fields();
            while (obsIt.hasNext()) {
                var obsEntry = obsIt.next();
                Integer timeIdx = tryParseInt(obsEntry.getKey());
                if (timeIdx == null || timeIdx < 0 || timeIdx >= dates.size()){
                    skippedObservations++;
                    continue;
                }
                LocalDate date = dates.get(timeIdx);
                if (date == null) {
                    skippedObservations++;
                    continue;
                }
                JsonNode obsArray = obsEntry.getValue();
                BigDecimal rate = extractRate(obsArray);
                if (rate == null){
                    skippedObservations++;
                    continue;
                }
                out.add(new ExchangeRateRow(date, currency, rate));
            }
        }

        int parsedRows = out.size();
        int skippedTotal = skippedSeries + skippedObservations;
        int totalProcessed = parsedRows + skippedTotal;

        if (totalProcessed > 0) {
            double skipRatio = (double) skippedTotal / totalProcessed;
            if (skipRatio >= WARN_SKIP_RATIO) {
                log.warn("Parsed exchange rates with high skip ratio: parsedRows={}, skippedSeries={}, skippedObservations={}",
                        parsedRows, skippedSeries, skippedObservations);
            } else if (log.isDebugEnabled()) {
                log.debug("Parsed exchange rates: parsedRows={}, skippedSeries={}, skippedObservations={}",
                        parsedRows, skippedSeries, skippedObservations);
            }
        }

        return out;
    }

    private static Integer tryParseInt(String s) {
        try {
            return Integer.parseInt(s);
        } catch (NumberFormatException e) {
            return null;
        }
    }

    private static Integer parseCurrencyIndex(String seriesKey) {
        // "0:40:0:0:0:0" -> take token[1]
        String[] parts = seriesKey.split(":");
        if (parts.length < 2) return null;
        return tryParseInt(parts[1]);
    }

    private static List<String> extractCurrencies(JsonNode dims) {
        JsonNode seriesDims = dims.path("series");
        Integer currencyDimIndex = findDimIndexById(seriesDims, DIM_CURRENCY);
        if (currencyDimIndex == null) {
            return List.of();
        }
        JsonNode values = seriesDims.path(currencyDimIndex).path("values");
        if (!values.isArray()) {
            return List.of();
        }

        List<String> out = new ArrayList<>(values.size());
        for (JsonNode v : values) {
            String currency = v.path("id").asText(null);
            if (currency != null && !currency.isBlank()) {
                out.add(currency);
            }
        }
        return out;
    }

    private static List<LocalDate> extractDates(JsonNode dims) {
        JsonNode obsDims = dims.path("observation");
        Integer timeIdx = findDimIndexById(obsDims, DIM_TIME_PERIOD);
        if (timeIdx == null) {
            return List.of();
        }
        JsonNode values = obsDims.path(timeIdx).path("values");
        if (!values.isArray()) {
            return List.of();
        }

        List<LocalDate> out = new ArrayList<>(values.size());
        for (JsonNode v : values) {
            String dateText = v.path("id").asText(null);
            if (dateText == null || dateText.isBlank()) {
                out.add(null);
                continue;
            }
            try {
                out.add(LocalDate.parse(dateText));
            } catch (DateTimeParseException e) {
                out.add(null);
            }
        }
        return out;
    }

    private static Integer findDimIndexById(JsonNode dimsArray, String id) {
        if (!dimsArray.isArray()) {
            return null;
        }
        for (int i = 0; i < dimsArray.size(); i++) {
            if (id.equals(dimsArray.get(i).path("id").asText())) return i;
        }
        return null;
    }

    private static BigDecimal extractRate(JsonNode obsArray) {
        if (obsArray == null || !obsArray.isArray() || obsArray.isEmpty()) {
            return null;
        }
        JsonNode value = obsArray.get(0);
        if (value == null || value.isNull()) {
            return null;
        }
        if (value.isNumber()) {
            return value.decimalValue();
        }
        if (value.isTextual()) {
            String text = value.textValue();
            if (text == null){
                return null;
            }
            text = text.trim();
            if (text.isEmpty() || text.equalsIgnoreCase("null")) {
                return null;
            }
            try {
                return new BigDecimal(text);
            } catch (NumberFormatException ignored) {
                // ignore non numerical value
                return null;
            }
        }
        // Unexpected type
        return null;
    }

    private static boolean isObjectNode(JsonNode node) {
        return node != null && node.isObject();
    }

    private static JsonNode safePathObject(JsonNode node, String field) {
        if (!isObjectNode(node)) {
            return null;
        }
        JsonNode child = node.path(field);
        return child.isObject() ? child : null;
    }

}
