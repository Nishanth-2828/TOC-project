package com.grammarlink.service;

import com.grammarlink.dto.ValidationRequest;
import com.grammarlink.dto.ValidationResult;
import com.grammarlink.model.Grammar;
import com.grammarlink.model.Production;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * ValidationOrchestrator: The main service that coordinates the full validation pipeline.
 *
 * Pipeline:
 *   Parse Grammar → Convert to CNF → Run CYK → Simulate PDA → Compare Results → Return
 */
@Service
public class ValidationOrchestrator {

    @Autowired private GrammarParserService parserService;
    @Autowired private CNFConverterService cnfConverter;
    @Autowired private CYKParserService cykParser;
    @Autowired private PDASimulatorService pdaSimulator;
    @Autowired private PresetGrammarService presetService;

    public ValidationResult validate(ValidationRequest request) {
        try {
            // ── Resolve grammar text (preset or custom) ───────────────────────
            String grammarText = request.getGrammarText();
            if (grammarText == null || grammarText.isBlank()) {
                if (request.getPresetName() != null && !request.getPresetName().isBlank()) {
                    grammarText = presetService.getPreset(request.getPresetName()).grammarText();
                } else {
                    return ValidationResult.error("Please provide grammar rules or select a preset.");
                }
            }

            String testString = request.getTestString() == null ? "" : request.getTestString().trim();

            // ── Parse grammar ─────────────────────────────────────────────────
            Grammar original;
            try {
                original = parserService.parse(grammarText);
            } catch (IllegalArgumentException e) {
                return ValidationResult.error("Grammar parse error: " + e.getMessage());
            }

            // Validate test string contains only valid terminals
            if (!testString.isEmpty()) {
                Set<String> validChars = new HashSet<>(original.getTerminals());
                // Allow epsilon characters to be matched
                for (char c : testString.toCharArray()) {
                    String sym = String.valueOf(c);
                    if (!validChars.contains(sym)) {
                        return ValidationResult.error("Character '" + sym +
                                "' in test string is not a terminal of this grammar. " +
                                "Valid terminals: " + original.getTerminals());
                    }
                }
            }

            // ── Convert to CNF (needed for CYK) ──────────────────────────────
            boolean wasAlreadyCNF = original.isInCNF();
            CNFConverterService.ConversionResult cnfResult = cnfConverter.convert(original);
            Grammar cnfGrammar = cnfResult.getCnfGrammar();

            // ── Run CYK Parser ────────────────────────────────────────────────
            CYKParserService.CYKResult cykResult = cykParser.parse(cnfGrammar, testString);

            // ── Simulate PDA ──────────────────────────────────────────────────
            PDASimulatorService.PDAResult pdaResult = pdaSimulator.simulate(original, testString);

            // ── Detect ambiguity ──────────────────────────────────────────────
            boolean ambiguous = cykResult.getParseTrees().size() > 1;
            List<String> ambiguityDetails = new ArrayList<>();
            if (ambiguous) {
                ambiguityDetails.add("Grammar is <b>ambiguous</b>: " +
                        cykResult.getParseTrees().size() + " distinct parse tree(s) found for '" + testString + "'.");
                ambiguityDetails.add("Ambiguity means multiple leftmost derivations exist for the same string.");
            }

            // ── Build result ──────────────────────────────────────────────────
            ValidationResult result = new ValidationResult();
            result.setTestString(testString);
            result.setOriginalGrammar(grammarText);
            result.setWasAlreadyCNF(wasAlreadyCNF);
            result.setCnfConversionSteps(cnfResult.getSteps());
            result.setCnfProductions(cnfGrammar.getProductions());

            result.setCykAccepted(cykResult.isAccepted());
            result.setCykSteps(cykResult.getSteps());
            result.setCykTimeNanos(cykResult.getTimeNanos());
            if (cykResult.getTableData() instanceof String[][] table) {
                result.setCykTable(table);
            }

            result.setPdaAccepted(pdaResult.isAccepted());
            result.setPdaSteps(pdaResult.getSteps());
            result.setPdaTimeNanos(pdaResult.getTimeNanos());

            result.setEquivalent(cykResult.isAccepted() == pdaResult.isAccepted());
            result.setAmbiguous(ambiguous);
            result.setParseTreeCount(cykResult.getParseTrees().size());
            result.setAmbiguityDetails(ambiguityDetails);
            result.setParseTrees(cykResult.getParseTrees());

            return result;

        } catch (Exception e) {
            return ValidationResult.error("Unexpected error: " + e.getMessage());
        }
    }
}
