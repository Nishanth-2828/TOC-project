package com.grammarlink.model;

import java.util.ArrayList;
import java.util.List;

/**
 * Represents a single production rule in a Context-Free Grammar.
 * Format: left → body (e.g., S → AB or A → a)
 */
public class Production {

    /** Left-hand side non-terminal (single uppercase letter) */
    private String left;

    /** Right-hand side: list of symbols (terminals or non-terminals) */
    private List<String> body;

    public Production() {
        this.body = new ArrayList<>();
    }

    public Production(String left, List<String> body) {
        this.left = left;
        this.body = new ArrayList<>(body);
    }

    public Production(String left, String... symbols) {
        this.left = left;
        this.body = new ArrayList<>(List.of(symbols));
    }

    /** Check if this is an epsilon (empty) production */
    public boolean isEpsilon() {
        return body.isEmpty() || (body.size() == 1 && body.get(0).equals("ε"));
    }

    /** Check if this is a unit production (A → B where B is a non-terminal) */
    public boolean isUnit(java.util.Set<String> nonTerminals) {
        return body.size() == 1 && nonTerminals.contains(body.get(0));
    }

    /** Check if this is a terminal production (A → a) */
    public boolean isTerminal(java.util.Set<String> nonTerminals) {
        return body.size() == 1 && !nonTerminals.contains(body.get(0));
    }

    /** Check if this is in CNF form (A → BC or A → a) */
    public boolean isCNF(java.util.Set<String> nonTerminals) {
        if (body.size() == 1 && !nonTerminals.contains(body.get(0))) return true;
        if (body.size() == 2 && nonTerminals.contains(body.get(0)) && nonTerminals.contains(body.get(1))) return true;
        return false;
    }

    public String bodyAsString() {
        return String.join("", body);
    }

    @Override
    public String toString() {
        return left + " → " + (body.isEmpty() ? "ε" : String.join(" ", body));
    }

    // Getters and Setters
    public String getLeft() { return left; }
    public void setLeft(String left) { this.left = left; }

    public List<String> getBody() { return body; }
    public void setBody(List<String> body) { this.body = body; }
}
