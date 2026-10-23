package com.grammarlink.model;

import java.util.*;

/**
 * Represents a Context-Free Grammar (CFG).
 * G = (V, Σ, P, S) where:
 *   V = set of non-terminals (variables)
 *   Σ = set of terminals
 *   P = set of productions
 *   S = start symbol
 */
public class Grammar {

    /** Set of non-terminal symbols (variables), e.g., {S, A, B} */
    private Set<String> nonTerminals;

    /** Set of terminal symbols, e.g., {a, b, (, )} */
    private Set<String> terminals;

    /** List of production rules */
    private List<Production> productions;

    /** Start symbol */
    private String startSymbol;

    public Grammar() {
        this.nonTerminals = new LinkedHashSet<>();
        this.terminals = new LinkedHashSet<>();
        this.productions = new ArrayList<>();
    }

    public Grammar(Set<String> nonTerminals, Set<String> terminals,
                   List<Production> productions, String startSymbol) {
        this.nonTerminals = new LinkedHashSet<>(nonTerminals);
        this.terminals = new LinkedHashSet<>(terminals);
        this.productions = new ArrayList<>(productions);
        this.startSymbol = startSymbol;
    }

    /** Get all productions whose left-hand side is the given non-terminal */
    public List<Production> getProductionsFor(String nonTerminal) {
        List<Production> result = new ArrayList<>();
        for (Production p : productions) {
            if (p.getLeft().equals(nonTerminal)) {
                result.add(p);
            }
        }
        return result;
    }

    /** Returns true if this grammar is already in CNF */
    public boolean isInCNF() {
        for (Production p : productions) {
            if (!p.isCNF(nonTerminals)) return false;
        }
        return true;
    }

    /** Deep copy of this grammar */
    public Grammar copy() {
        Grammar copy = new Grammar();
        copy.nonTerminals = new LinkedHashSet<>(this.nonTerminals);
        copy.terminals = new LinkedHashSet<>(this.terminals);
        copy.startSymbol = this.startSymbol;
        for (Production p : this.productions) {
            copy.productions.add(new Production(p.getLeft(), new ArrayList<>(p.getBody())));
        }
        return copy;
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        sb.append("G = ({").append(String.join(", ", nonTerminals)).append("}, ");
        sb.append("{").append(String.join(", ", terminals)).append("}, P, ").append(startSymbol).append(")\n");
        sb.append("Productions P:\n");
        for (Production p : productions) {
            sb.append("  ").append(p.toString()).append("\n");
        }
        return sb.toString();
    }

    // Getters and Setters
    public Set<String> getNonTerminals() { return nonTerminals; }
    public void setNonTerminals(Set<String> nonTerminals) { this.nonTerminals = nonTerminals; }

    public Set<String> getTerminals() { return terminals; }
    public void setTerminals(Set<String> terminals) { this.terminals = terminals; }

    public List<Production> getProductions() { return productions; }
    public void setProductions(List<Production> productions) { this.productions = productions; }

    public String getStartSymbol() { return startSymbol; }
    public void setStartSymbol(String startSymbol) { this.startSymbol = startSymbol; }
}
