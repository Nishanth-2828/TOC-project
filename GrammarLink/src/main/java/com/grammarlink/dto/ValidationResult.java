package com.grammarlink.dto;

import com.grammarlink.model.PDAStep;
import com.grammarlink.model.Production;

import java.util.List;
import java.util.Map;

/**
 * Data Transfer Object for the complete validation response.
 * Carries all results back to the frontend for visualization.
 */
public class ValidationResult {

    // ── Input info ──────────────────────────────────────────────────────────────
    private String testString;
    private String originalGrammar;

    // ── CNF Conversion ──────────────────────────────────────────────────────────
    private List<String> cnfConversionSteps;      // Human-readable steps of CNF conversion
    private List<Production> cnfProductions;       // Final CNF grammar productions
    private boolean wasAlreadyCNF;

    // ── CYK Parsing ─────────────────────────────────────────────────────────────
    private boolean cykAccepted;
    private String[][] cykTable;                   // [row][col] = set of non-terminals as string
    private List<String> cykSteps;                 // Human-readable step explanations
    private long cykTimeNanos;                     // Execution time in nanoseconds

    // ── PDA Simulation ──────────────────────────────────────────────────────────
    private boolean pdaAccepted;
    private List<PDAStep> pdaSteps;                // Full stack trace
    private long pdaTimeNanos;                     // Execution time in nanoseconds

    // ── Equivalence & Ambiguity ──────────────────────────────────────────────────
    private boolean equivalent;                    // CYK result == PDA result
    private boolean ambiguous;                     // Multiple parse trees found
    private int parseTreeCount;                    // Number of distinct parse trees
    private List<String> ambiguityDetails;

    // ── Error handling ──────────────────────────────────────────────────────────
    private boolean hasError;
    private String errorMessage;

    // ── Parse Trees (for visualization) ─────────────────────────────────────────
    private List<Map<String, Object>> parseTrees;  // JSON-serializable tree structures

    public ValidationResult() {}

    // ── Factory helpers ──────────────────────────────────────────────────────────
    public static ValidationResult error(String message) {
        ValidationResult r = new ValidationResult();
        r.hasError = true;
        r.errorMessage = message;
        return r;
    }

    // ── Getters and Setters ──────────────────────────────────────────────────────
    public String getTestString() { return testString; }
    public void setTestString(String testString) { this.testString = testString; }

    public String getOriginalGrammar() { return originalGrammar; }
    public void setOriginalGrammar(String originalGrammar) { this.originalGrammar = originalGrammar; }

    public List<String> getCnfConversionSteps() { return cnfConversionSteps; }
    public void setCnfConversionSteps(List<String> cnfConversionSteps) { this.cnfConversionSteps = cnfConversionSteps; }

    public List<Production> getCnfProductions() { return cnfProductions; }
    public void setCnfProductions(List<Production> cnfProductions) { this.cnfProductions = cnfProductions; }

    public boolean isWasAlreadyCNF() { return wasAlreadyCNF; }
    public void setWasAlreadyCNF(boolean wasAlreadyCNF) { this.wasAlreadyCNF = wasAlreadyCNF; }

    public boolean isCykAccepted() { return cykAccepted; }
    public void setCykAccepted(boolean cykAccepted) { this.cykAccepted = cykAccepted; }

    public String[][] getCykTable() { return cykTable; }
    public void setCykTable(String[][] cykTable) { this.cykTable = cykTable; }

    public List<String> getCykSteps() { return cykSteps; }
    public void setCykSteps(List<String> cykSteps) { this.cykSteps = cykSteps; }

    public long getCykTimeNanos() { return cykTimeNanos; }
    public void setCykTimeNanos(long cykTimeNanos) { this.cykTimeNanos = cykTimeNanos; }

    public boolean isPdaAccepted() { return pdaAccepted; }
    public void setPdaAccepted(boolean pdaAccepted) { this.pdaAccepted = pdaAccepted; }

    public List<PDAStep> getPdaSteps() { return pdaSteps; }
    public void setPdaSteps(List<PDAStep> pdaSteps) { this.pdaSteps = pdaSteps; }

    public long getPdaTimeNanos() { return pdaTimeNanos; }
    public void setPdaTimeNanos(long pdaTimeNanos) { this.pdaTimeNanos = pdaTimeNanos; }

    public boolean isEquivalent() { return equivalent; }
    public void setEquivalent(boolean equivalent) { this.equivalent = equivalent; }

    public boolean isAmbiguous() { return ambiguous; }
    public void setAmbiguous(boolean ambiguous) { this.ambiguous = ambiguous; }

    public int getParseTreeCount() { return parseTreeCount; }
    public void setParseTreeCount(int parseTreeCount) { this.parseTreeCount = parseTreeCount; }

    public List<String> getAmbiguityDetails() { return ambiguityDetails; }
    public void setAmbiguityDetails(List<String> ambiguityDetails) { this.ambiguityDetails = ambiguityDetails; }

    public boolean isHasError() { return hasError; }
    public void setHasError(boolean hasError) { this.hasError = hasError; }

    public String getErrorMessage() { return errorMessage; }
    public void setErrorMessage(String errorMessage) { this.errorMessage = errorMessage; }

    public List<Map<String, Object>> getParseTrees() { return parseTrees; }
    public void setParseTrees(List<Map<String, Object>> parseTrees) { this.parseTrees = parseTrees; }
}
