package com.grammarlink.service;

import com.grammarlink.model.Grammar;
import com.grammarlink.model.Production;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * PresetGrammarService: Provides predefined well-known CFG examples.
 *
 * Available presets:
 *  1. balanced_parens   — Balanced parentheses: L = { ()^n | n ≥ 0 }
 *  2. equal_ab          — Equal a's and b's: L = { w | #a(w) = #b(w) }
 *  3. palindrome        — Palindromes over {a,b}: L = { w | w = w^R }
 *  4. arithmetic_expr   — Simple arithmetic expressions: a + a * a
 *  5. anbn              — Classic a^n b^n: L = { a^n b^n | n ≥ 1 }
 *  6. xml_tags          — Simple XML-like tags: <a>...</a>
 */
@Service
public class PresetGrammarService {

    public record Preset(String id, String name, String description,
                         String grammarText, String exampleAccepted, String exampleRejected) {}

    private final Map<String, Preset> presets = new LinkedHashMap<>();

    public PresetGrammarService() {
        register(new Preset(
                "balanced_parens",
                "Balanced Parentheses",
                "Generates all strings of balanced parentheses. L = { ε, (), (()), ()(), ... }",
                "S -> ( S ) S | ε",
                "(())",
                "(()"
        ));

        register(new Preset(
                "anbn",
                "aⁿbⁿ Language",
                "Classic CFL: equal number of a's followed by equal number of b's.",
                "S -> a S b | a b",
                "aaabbb",
                "aabb b"
        ));

        register(new Preset(
                "equal_ab",
                "Equal a's and b's",
                "Strings over {a,b} with equal count of a's and b's.",
                "S -> a S b S | b S a S | ε",
                "aabb",
                "aab"
        ));

        register(new Preset(
                "palindrome",
                "Palindromes over {a,b}",
                "All palindromes over alphabet {a,b}.",
                "S -> a S a | b S b | a | b | ε",
                "ababa",
                "abbc"
        ));

        register(new Preset(
                "arithmetic_expr",
                "Arithmetic Expressions",
                "Simple arithmetic expressions with +, *, and identifier 'a'.",
                "E -> E + T | T\nT -> T * F | F\nF -> a",
                "a+a*a",
                "a+*a"
        ));

        register(new Preset(
                "anbncn_approx",
                "aⁿbⁿcⁿ Approximation (CFL version)",
                "Context-free approximation: a's then b's then c's (n,m ≥ 1, not necessarily equal).",
                "S -> A B\nA -> a A b | a b\nB -> b B c | b c",
                "aabbbc",
                "abc"
        ));
    }

    private void register(Preset p) { presets.put(p.id(), p); }

    public List<Preset> getAllPresets() { return new ArrayList<>(presets.values()); }

    public Preset getPreset(String id) {
        if (id == null || !presets.containsKey(id)) {
            throw new IllegalArgumentException("Unknown preset: " + id);
        }
        return presets.get(id);
    }

    public boolean hasPreset(String id) { return presets.containsKey(id); }
}
