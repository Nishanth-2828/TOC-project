package com.grammarlink.service;

import com.grammarlink.model.Grammar;
import com.grammarlink.model.Production;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * CYKParserService: Implements the Cocke-Younger-Kasami (CYK) parsing algorithm.
 *
 * Algorithm overview:
 *  - Input: CNF grammar G, string w = a1 a2 ... an
 *  - Uses a triangular DP table T[i][j] = set of non-terminals that can derive w[i..j]
 *  - T[i][i] = { A | A → ai ∈ P }
 *  - T[i][j] = { A | A → BC ∈ P, B ∈ T[i][k], C ∈ T[k+1][j] for some k }
 *  - w ∈ L(G) iff S ∈ T[1][n]
 */
@Service
public class CYKParserService {

    public CYKResult parse(Grammar cnfGrammar, String input) {
        long startTime = System.nanoTime();
        List<String> steps = new ArrayList<>();

        if (input == null || input.isEmpty()) {
            // Special case: check if start symbol can derive epsilon
            boolean acceptsEpsilon = cnfGrammar.getProductions().stream()
                    .anyMatch(p -> p.getLeft().equals(cnfGrammar.getStartSymbol()) && p.isEpsilon());
            steps.add("Empty string: checking if start symbol <b>" + cnfGrammar.getStartSymbol()
                    + "</b> derives ε → " + (acceptsEpsilon ? "YES" : "NO"));
            return new CYKResult(acceptsEpsilon, new Set[0][0], steps,
                    System.nanoTime() - startTime, new ArrayList<>());
        }

        // Convert input string to array of characters (symbols)
        String[] w = input.chars()
                .mapToObj(c -> String.valueOf((char) c))
                .toArray(String[]::new);
        int n = w.length;

        // T[i][j] = set of non-terminals deriving w[i..j] (0-indexed internally)
        @SuppressWarnings("unchecked")
        Set<String>[][] table = new Set[n][n];
        for (int i = 0; i < n; i++)
            for (int j = 0; j < n; j++)
                table[i][j] = new HashSet<>();

        // ── Base case: length-1 substrings ───────────────────────────────────
        steps.add("<b>Base Case</b>: Fill table diagonal (single characters)");
        for (int i = 0; i < n; i++) {
            for (Production p : cnfGrammar.getProductions()) {
                if (p.getBody().size() == 1 && p.getBody().get(0).equals(w[i])) {
                    table[i][i].add(p.getLeft());
                }
            }
            steps.add("  T[" + (i+1) + "][" + (i+1) + "] = <b>" +
                    (table[i][i].isEmpty() ? "∅" : "{" + String.join(", ", table[i][i]) + "}") +
                    "</b>  (for symbol '" + w[i] + "')");
        }

        // ── Fill table for increasing lengths ────────────────────────────────
        for (int len = 2; len <= n; len++) {
            steps.add("<b>Length " + len + " substrings:</b>");
            for (int i = 0; i <= n - len; i++) {
                int j = i + len - 1;
                for (int k = i; k < j; k++) {
                    // Try each production A → BC
                    for (Production p : cnfGrammar.getProductions()) {
                        if (p.getBody().size() == 2) {
                            String B = p.getBody().get(0);
                            String C = p.getBody().get(1);
                            if (table[i][k].contains(B) && table[k+1][j].contains(C)) {
                                boolean added = table[i][j].add(p.getLeft());
                                if (added) {
                                    steps.add("  T[" + (i+1) + "][" + (j+1) + "] ← <b>" + p.getLeft() +
                                            "</b> via " + p.getLeft() + "→" + B + C +
                                            " (split at k=" + (k+1) + ")");
                                }
                            }
                        }
                    }
                }
            }
        }

        boolean accepted = table[0][n-1].contains(cnfGrammar.getStartSymbol());
        steps.add("<b>Result</b>: T[1][" + n + "] = {" + String.join(", ", table[0][n-1]) + "}");
        steps.add("Start symbol <b>" + cnfGrammar.getStartSymbol() + "</b> " +
                (accepted ? "IS ✓" : "is NOT ✗") + " in T[1][" + n + "] → String is <b>" +
                (accepted ? "ACCEPTED" : "REJECTED") + "</b>");

        // Build string representation of the table for display
        String[][] tableStr = new String[n][n];
        for (int i = 0; i < n; i++) {
            for (int j = 0; j < n; j++) {
                tableStr[i][j] = table[i][j].isEmpty() ? "∅" :
                        "{" + String.join(",", new TreeSet<>(table[i][j])) + "}";
            }
        }

        // Count parse trees (for ambiguity detection) - simplified backtracking
        List<Map<String, Object>> parseTrees = buildParseTrees(cnfGrammar, table, w, 0, n - 1,
                cnfGrammar.getStartSymbol(), 0);

        return new CYKResult(accepted, tableStr, steps, System.nanoTime() - startTime, parseTrees);
    }

    /**
     * Builds parse tree(s) via backtracking through the CYK table.
     * Returns up to 5 parse trees for ambiguity detection.
     */
    private List<Map<String, Object>> buildParseTrees(Grammar g, Set<String>[][] table,
                                                        String[] w, int i, int j,
                                                        String symbol, int depth) {
        List<Map<String, Object>> results = new ArrayList<>();
        if (depth > 20 || results.size() > 5) return results; // depth/count guard

        if (i == j) {
            // Terminal production
            Map<String, Object> node = new LinkedHashMap<>();
            node.put("label", symbol);
            node.put("children", List.of(Map.of("label", w[i], "children", List.of())));
            results.add(node);
            return results;
        }

        // Try all binary splits
        for (int k = i; k < j && results.size() <= 5; k++) {
            for (Production p : g.getProductions()) {
                if (p.getBody().size() == 2 && p.getLeft().equals(symbol)) {
                    String B = p.getBody().get(0);
                    String C = p.getBody().get(1);
                    if (table[i][k].contains(B) && table[k+1][j].contains(C)) {
                        List<Map<String, Object>> leftTrees = buildParseTrees(g, table, w, i, k, B, depth + 1);
                        List<Map<String, Object>> rightTrees = buildParseTrees(g, table, w, k + 1, j, C, depth + 1);
                        for (Map<String, Object> lt : leftTrees) {
                            for (Map<String, Object> rt : rightTrees) {
                                if (results.size() > 5) break;
                                Map<String, Object> node = new LinkedHashMap<>();
                                node.put("label", symbol);
                                node.put("children", List.of(lt, rt));
                                results.add(node);
                            }
                        }
                    }
                }
            }
        }
        return results;
    }

    // ── Result class ──────────────────────────────────────────────────────────

    public static class CYKResult {
        private final boolean accepted;
        private final Object tableData; // String[][] or Set[][]
        private final List<String> steps;
        private final long timeNanos;
        private final List<Map<String, Object>> parseTrees;

        public CYKResult(boolean accepted, Object tableData, List<String> steps,
                         long timeNanos, List<Map<String, Object>> parseTrees) {
            this.accepted = accepted;
            this.tableData = tableData;
            this.steps = steps;
            this.timeNanos = timeNanos;
            this.parseTrees = parseTrees;
        }

        public boolean isAccepted() { return accepted; }
        public Object getTableData() { return tableData; }
        public List<String> getSteps() { return steps; }
        public long getTimeNanos() { return timeNanos; }
        public List<Map<String, Object>> getParseTrees() { return parseTrees; }
    }
}
