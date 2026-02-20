package com.crewmeister.cmcodingchallenge.bank;

import com.fasterxml.jackson.databind.JsonNode;
import org.springframework.stereotype.Component;

import java.math.BigDecimal;
import java.time.LocalDate;
import java.util.ArrayList;
import java.util.Iterator;
import java.util.List;
import java.util.Map;

@Component
public class BankResponseParser {

    public List<ExchangeRateRow> parseRates(JsonNode root) {
        JsonNode data = root.path("data");
        JsonNode structure = data.path("structure");
        JsonNode dims = structure.path("dimensions");

        List<LocalDate> dates = extractDates(dims);
        List<String> currencies = extractCurrencies(dims);

        JsonNode seriesNode = data.path("dataSets").path(0).path("series");
        if (seriesNode.isMissingNode() || !seriesNode.isObject()) {
            return List.of();
        }

        List<ExchangeRateRow> out = new ArrayList<>();

        Iterator<Map.Entry<String, JsonNode>> seriesIt = seriesNode.fields();
        while (seriesIt.hasNext()) {
            var entry = seriesIt.next();
            String seriesKey = entry.getKey();
            JsonNode series = entry.getValue();

            Integer currencyIndex = parseCurrencyIndex(seriesKey); // position 1
            if (currencyIndex == null || currencyIndex < 0 || currencyIndex >= currencies.size()) continue;
            String currency = currencies.get(currencyIndex);

            JsonNode observations = series.path("observations");
            if (!observations.isObject()) continue;

            Iterator<Map.Entry<String, JsonNode>> obsIt = observations.fields();
            while (obsIt.hasNext()) {
                var obsEntry = obsIt.next();
                int timeIdx = Integer.parseInt(obsEntry.getKey());
                if (timeIdx < 0 || timeIdx >= dates.size()) continue;

                JsonNode obsArray = obsEntry.getValue();
                if (!obsArray.isArray() || obsArray.isEmpty()) continue;

                BigDecimal rate = new BigDecimal(obsArray.get(0).asText());
                out.add(new ExchangeRateRow(dates.get(timeIdx), currency, rate));
            }
        }

        return out;
    }

    private static Integer parseCurrencyIndex(String seriesKey) {
        // "0:40:0:0:0:0" -> take token[1]
        String[] parts = seriesKey.split(":");
        if (parts.length < 2) return null;
        return Integer.parseInt(parts[1]);
    }

    private static List<String> extractCurrencies(JsonNode dims) {
        JsonNode seriesDims = dims.path("series");
        int currencyDimIndex = findDimIndexById(seriesDims, "BBK_STD_CURRENCY");
        JsonNode values = seriesDims.path(currencyDimIndex).path("values");

        List<String> out = new ArrayList<>(values.size());
        for (JsonNode v : values) out.add(v.path("id").asText());
        return out;
    }

    private static List<LocalDate> extractDates(JsonNode dims) {
        JsonNode obsDims = dims.path("observation");
        int timeIdx = findDimIndexById(obsDims, "TIME_PERIOD");
        JsonNode values = obsDims.path(timeIdx).path("values");

        List<LocalDate> out = new ArrayList<>(values.size());
        for (JsonNode v : values) out.add(LocalDate.parse(v.path("id").asText()));
        return out;
    }

    private static int findDimIndexById(JsonNode dimsArray, String id) {
        for (int i = 0; i < dimsArray.size(); i++) {
            if (id.equals(dimsArray.get(i).path("id").asText())) return i;
        }
        throw new IllegalArgumentException("Missing dimension: " + id);
    }

}
