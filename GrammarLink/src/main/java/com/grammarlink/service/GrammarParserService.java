package com.grammarlink.service;

import com.grammarlink.model.Grammar;
import com.grammarlink.model.Production;
import org.springframework.stereotype.Service;

import java.util.*;
import java.util.logging.Logger;

/**
 * GrammarParserService: Parses raw text grammar into a Grammar object.
 *
 * Supported format:
 *   S -> AB | ε
 *   A -> a | aA
 *   B -> b
 *
 * Rules:
 *  - Left-hand side must be a single uppercase letter (non-terminal).
 *  - Right-hand side alternatives separated by '|'.
 *  - Symbols are individual characters; non-terminals are uppercase letters.
 *  - 'ε' or 'eps' or 'epsilon' means empty string.
 *  - First rule's LHS is taken as the start symbol unless it's 'S'.
 */
@Service
public class GrammarParserService {

    private static final Logger log = Logger.getLogger(GrammarParserService.class.getName());

    /**
     * Parse raw grammar text into a Grammar object.
     *
     * @param grammarText raw text input
     * @return parsed Grammar
     * @throws IllegalArgumentException if grammar is malformed
     */
    public Grammar parse(String grammarText) {
        if (grammarText == null || grammarText.isBlank()) {
            throw new IllegalArgumentException("Grammar text cannot be empty.");
        }

        Grammar grammar = new Grammar();
        Set<String> nonTerminals = new LinkedHashSet<>();
        List<Production> productions = new ArrayList<>();
        String startSymbol = null;

        String[] lines = grammarText.split("\\r?\\n");

        for (String rawLine : lines) {
            String line = rawLine.trim();
            if (line.isEmpty() || line.startsWith("//") || line.startsWith("#")) continue;

            // Support both '->' and '→'
            String separator = line.contains("->") ? "->" : (line.contains("→") ? "→" : null);
            if (separator == null) {
                throw new IllegalArgumentException("Invalid production rule (missing '->'): " + line);
            }

            String[] parts = line.split(separator, 2);
            if (parts.length != 2) {
                throw new IllegalArgumentException("Malformed rule: " + line);
            }

            String lhs = parts[0].trim();
            if (!lhs.matches("[A-Z][A-Z0-9']*")) {
                throw new IllegalArgumentException(
                    "Left-hand side must be a non-terminal (uppercase letter/symbol): '" + lhs + "'");
            }

            nonTerminals.add(lhs);
            if (startSymbol == null) startSymbol = lhs;

            // Split alternatives by '|'
            String[] alternatives = parts[1].split("\\|");
            for (String alt : alternatives) {
                String body = alt.trim();
                List<String> bodySymbols = parseBody(body);
                productions.add(new Production(lhs, bodySymbols));
                log.fine("Parsed: " + lhs + " -> " + bodySymbols);
            }
        }

        if (nonTerminals.isEmpty()) {
            throw new IllegalArgumentException("No valid production rules found.");
        }

        // Prefer 'S' as start symbol if it exists
        if (nonTerminals.contains("S")) startSymbol = "S";

        // Identify terminals: any symbol in production bodies that is NOT a non-terminal
        Set<String> terminals = new LinkedHashSet<>();
        for (Production p : productions) {
            for (String sym : p.getBody()) {
                if (!nonTerminals.contains(sym) && !sym.equals("ε")) {
                    terminals.add(sym);
                }
            }
        }

        grammar.setNonTerminals(nonTerminals);
        grammar.setTerminals(terminals);
        grammar.setProductions(productions);
        grammar.setStartSymbol(startSymbol);

        log.info("Parsed grammar: " + nonTerminals.size() + " non-terminals, "
                + terminals.size() + " terminals, " + productions.size() + " productions.");

        return grammar;
    }

    /**
     * Parse the body of a production rule into a list of symbols.
     * Each character is treated as a symbol; multi-char non-terminals must be uppercase sequences.
     * Special: 'ε', 'eps', 'epsilon' → epsilon production (empty body).
     */
    private List<String> parseBody(String body) {
        if (body.isEmpty() || body.equalsIgnoreCase("ε") ||
            body.equalsIgnoreCase("eps") || body.equalsIgnoreCase("epsilon")) {
            return new ArrayList<>(); // epsilon production
        }

        List<String> symbols = new ArrayList<>();
        int i = 0;
        while (i < body.length()) {
            char c = body.charAt(i);

            // Skip whitespace
            if (Character.isWhitespace(c)) { i++; continue; }

            // Check for multi-character non-terminal (e.g., "S'" or "A1")
            if (Character.isUpperCase(c)) {
                StringBuilder sb = new StringBuilder();
                sb.append(c);
                i++;
                while (i < body.length() &&
                       (Character.isLetterOrDigit(body.charAt(i)) || body.charAt(i) == '\'')) {
                    sb.append(body.charAt(i));
                    i++;
                }
                symbols.add(sb.toString());
            } else {
                // Single character terminal
                symbols.add(String.valueOf(c));
                i++;
            }
        }
        return symbols;
    }
}
