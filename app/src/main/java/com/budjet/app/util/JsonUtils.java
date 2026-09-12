package com.budjet.app.util;

import com.budjet.app.data.model.ExportData;
import com.google.gson.Gson;
import com.google.gson.GsonBuilder;
import com.google.gson.JsonSyntaxException;

import java.io.InputStream;
import java.io.InputStreamReader;
import java.io.OutputStream;
import java.io.OutputStreamWriter;
import java.io.Reader;
import java.io.Writer;
import java.nio.charset.StandardCharsets;

public class JsonUtils {

    private static final Gson GSON = new GsonBuilder()
            .setPrettyPrinting()
            .disableHtmlEscaping()
            .serializeNulls()
            .create();

    public static String toJson(ExportData data) {
        return GSON.toJson(data);
    }

    public static void writeJsonToStream(ExportData data, OutputStream outputStream) throws Exception {
        try (Writer writer = new OutputStreamWriter(outputStream, StandardCharsets.UTF_8)) {
            GSON.toJson(data, writer);
            writer.flush();
        }
    }

    public static ExportData fromJsonStream(InputStream inputStream) throws JsonSyntaxException {
        Reader reader = new InputStreamReader(inputStream, StandardCharsets.UTF_8);
        return GSON.fromJson(reader, ExportData.class);
    }

    public static ExportData fromJsonString(String json) throws JsonSyntaxException {
        return GSON.fromJson(json, ExportData.class);
    }
}
