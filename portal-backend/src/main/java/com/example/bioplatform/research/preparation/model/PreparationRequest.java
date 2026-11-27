package com.example.bioplatform.research.preparation.model;
import java.util.List;
public record PreparationRequest(List<String> sourceCodes, Integer batchSize) {
}
