package com.grammarlink.dto;

/**
 * Data Transfer Object for incoming validation requests.
 * Contains the raw grammar rules (as text) and the test string.
 */
public class ValidationRequest {

    /**
     * Grammar rules in text format.
     * Format: one rule per line, e.g.:
     *   S -> AB | a
     *   A -> aA | a
     *   B -> bB | b
     */
    private String grammarText;

    /** The string to test for membership in the language */
    private String testString;

    /** Name of the preset grammar to load (optional) */
    private String presetName;

    public ValidationRequest() {}

    public ValidationRequest(String grammarText, String testString) {
        this.grammarText = grammarText;
        this.testString = testString;
    }

    public String getGrammarText() { return grammarText; }
    public void setGrammarText(String grammarText) { this.grammarText = grammarText; }

    public String getTestString() { return testString; }
    public void setTestString(String testString) { this.testString = testString; }

    public String getPresetName() { return presetName; }
    public void setPresetName(String presetName) { this.presetName = presetName; }
}
