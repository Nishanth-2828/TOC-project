package com.grammarlink.service;

import com.grammarlink.model.Grammar;
import com.grammarlink.model.Production;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * CNFConverterService: Converts any CFG to Chomsky Normal Form (CNF).
 *
 * Steps performed (in order):
 *  1. ADD new start symbol S0 → S  (to handle start symbol in RHS)
 *  2. ELIMINATE ε-productions (nullable variables)
 *  3. ELIMINATE unit productions (A → B)
 *  4. ELIMINATE useless symbols (unreachable / non-generating)
 *  5. CONVERT remaining long productions to CNF (A → BC or A → a)
 */
@Service
public class CNFConverterService {

    /**
     * Convert grammar to CNF. Returns the CNF grammar and a list of
     * human-readable conversion steps for display.
     */
    public ConversionResult convert(Grammar original) {
        List<String> steps = new ArrayList<>();
        Grammar g = original.copy();

        // ── Step 1: New start symbol ─────────────────────────────────────────
        String newStart = g.getStartSymbol() + "0";
        while (g.getNonTerminals().contains(newStart)) newStart += "0";
        g.getNonTerminals().add(newStart);
        g.getProductions().add(0, new Production(newStart, g.getStartSymbol()));
        g.setStartSymbol(newStart);
        steps.add("Step 1 — New start symbol: Added <b>" + newStart + " → " + original.getStartSymbol() + "</b> to prevent start symbol on RHS.");

        // ── Step 2: Eliminate ε-productions ─────────────────────────────────
        Set<String> nullable = findNullable(g);
        if (!nullable.isEmpty()) {
            eliminateEpsilon(g, nullable);
            steps.add("Step 2 — Eliminate ε-productions: Nullable variables = <b>{" +
                    String.join(", ", nullable) + "}</b>. Added all combinations without nullable symbols.");
        } else {
            steps.add("Step 2 — Eliminate ε-productions: No nullable variables found. Grammar unchanged.");
        }

        // ── Step 3: Eliminate unit productions ───────────────────────────────
        int unitCount = eliminateUnitProductions(g);
        steps.add("Step 3 — Eliminate unit productions: Removed/replaced <b>" + unitCount + "</b> unit production(s).");

        // ── Step 4: Eliminate useless symbols ───────────────────────────────
        int uselessCount = eliminateUseless(g);
        steps.add("Step 4 — Eliminate useless symbols: Removed <b>" + uselessCount + "</b> useless production(s).");

        // ── Step 5: Convert to binary CNF form ──────────────────────────────
        int newVarCount = convertToBinary(g);
        steps.add("Step 5 — Convert to CNF binary form: Introduced <b>" + newVarCount + "</b> new variable(s) to handle long/mixed productions.");

        steps.add("✓ CNF conversion complete. Grammar now has <b>" + g.getProductions().size() + "</b> production(s).");

        return new ConversionResult(g, steps);
    }

    // ── Finding Nullable variables ────────────────────────────────────────────

    private Set<String> findNullable(Grammar g) {
        Set<String> nullable = new HashSet<>();

        // Direct epsilon productions
        for (Production p : g.getProductions()) {
            if (p.isEpsilon()) nullable.add(p.getLeft());
        }

        // Propagate: A → B1B2...Bn where all Bi are nullable → A is nullable
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Production p : g.getProductions()) {
                if (!nullable.contains(p.getLeft())) {
                    if (!p.isEpsilon() && p.getBody().stream().allMatch(nullable::contains)) {
                        nullable.add(p.getLeft());
                        changed = true;
                    }
                }
            }
        }
        return nullable;
    }

    private void eliminateEpsilon(Grammar g, Set<String> nullable) {
        List<Production> toAdd = new ArrayList<>();
        List<Production> toRemove = new ArrayList<>();

        for (Production p : g.getProductions()) {
            if (p.isEpsilon() && !p.getLeft().equals(g.getStartSymbol())) {
                toRemove.add(p);
                continue;
            }
            // Generate all subsets with nullable symbols removed
            List<String> body = p.getBody();
            List<Integer> nullablePositions = new ArrayList<>();
            for (int i = 0; i < body.size(); i++) {
                if (nullable.contains(body.get(i))) nullablePositions.add(i);
            }

            int subsets = 1 << nullablePositions.size();
            for (int mask = 1; mask < subsets; mask++) {
                List<String> newBody = new ArrayList<>(body);
                List<Integer> toRemoveIdx = new ArrayList<>();
                for (int bit = 0; bit < nullablePositions.size(); bit++) {
                    if ((mask & (1 << bit)) != 0) toRemoveIdx.add(nullablePositions.get(bit));
                }
                Collections.sort(toRemoveIdx, Collections.reverseOrder());
                for (int idx : toRemoveIdx) newBody.remove(idx);
                if (!newBody.isEmpty()) {
                    Production newProd = new Production(p.getLeft(), newBody);
                    if (!containsProduction(g.getProductions(), newProd) && !containsProduction(toAdd, newProd)) {
                        toAdd.add(newProd);
                    }
                }
            }
        }

        g.getProductions().removeAll(toRemove);
        g.getProductions().addAll(toAdd);
    }

    // ── Eliminate unit productions ────────────────────────────────────────────

    private int eliminateUnitProductions(Grammar g) {
        int count = 0;
        boolean changed = true;
        while (changed) {
            changed = false;
            List<Production> toRemove = new ArrayList<>();
            List<Production> toAdd = new ArrayList<>();
            for (Production p : g.getProductions()) {
                if (p.isUnit(g.getNonTerminals())) {
                    toRemove.add(p);
                    String target = p.getBody().get(0);
                    for (Production p2 : g.getProductions()) {
                        if (p2.getLeft().equals(target) && !p2.isUnit(g.getNonTerminals())) {
                            Production newProd = new Production(p.getLeft(), new ArrayList<>(p2.getBody()));
                            if (!containsProduction(g.getProductions(), newProd) && !containsProduction(toAdd, newProd)) {
                                toAdd.add(newProd);
                            }
                        }
                    }
                    changed = true;
                    count++;
                }
            }
            g.getProductions().removeAll(toRemove);
            g.getProductions().addAll(toAdd);
        }
        return count;
    }

    // ── Eliminate useless symbols ─────────────────────────────────────────────

    private int eliminateUseless(Grammar g) {
        int originalCount = g.getProductions().size();

        // Find generating symbols (can derive a terminal string)
        Set<String> generating = new HashSet<>();
        for (Production p : g.getProductions()) {
            if (p.getBody().stream().allMatch(sym -> g.getTerminals().contains(sym))) {
                generating.add(p.getLeft());
            }
        }
        boolean changed = true;
        while (changed) {
            changed = false;
            for (Production p : g.getProductions()) {
                if (!generating.contains(p.getLeft())) {
                    if (p.getBody().stream().allMatch(sym ->
                            g.getTerminals().contains(sym) || generating.contains(sym))) {
                        generating.add(p.getLeft());
                        changed = true;
                    }
                }
            }
        }

        // Find reachable symbols
        Set<String> reachable = new HashSet<>();
        reachable.add(g.getStartSymbol());
        changed = true;
        while (changed) {
            changed = false;
            Set<String> toAdd = new HashSet<>();
            for (Production p : g.getProductions()) {
                if (reachable.contains(p.getLeft())) {
                    for (String sym : p.getBody()) {
                        if (g.getNonTerminals().contains(sym) && !reachable.contains(sym)) {
                            toAdd.add(sym);
                            changed = true;
                        }
                    }
                }
            }
            reachable.addAll(toAdd);
        }

        Set<String> useful = new HashSet<>(generating);
        useful.retainAll(reachable);

        g.getProductions().removeIf(p -> !useful.contains(p.getLeft()) ||
                p.getBody().stream().anyMatch(sym ->
                        g.getNonTerminals().contains(sym) && !useful.contains(sym)));

        g.getNonTerminals().retainAll(useful);
        return originalCount - g.getProductions().size();
    }

    // ── Convert to binary CNF ─────────────────────────────────────────────────

    private int convertToBinary(Grammar g) {
        int newVarCount = 0;
        Map<String, String> terminalMap = new HashMap<>(); // terminal → new NT for it
        int varIndex = 1;

        List<Production> toAdd = new ArrayList<>();
        List<Production> toRemove = new ArrayList<>();

        for (Production p : g.getProductions()) {
            List<String> body = p.getBody();

            // Already in CNF
            if (p.isCNF(g.getNonTerminals())) continue;

            toRemove.add(p);

            // Replace terminals in mixed/long productions with dedicated non-terminals
            List<String> replaced = new ArrayList<>();
            for (String sym : body) {
                if (g.getTerminals().contains(sym) && (body.size() > 1)) {
                    if (!terminalMap.containsKey(sym)) {
                        String newNT = "T" + sym.toUpperCase() + (varIndex++);
                        while (g.getNonTerminals().contains(newNT)) newNT += "'";
                        terminalMap.put(sym, newNT);
                        g.getNonTerminals().add(newNT);
                        toAdd.add(new Production(newNT, sym));
                        newVarCount++;
                    }
                    replaced.add(terminalMap.get(sym));
                } else {
                    replaced.add(sym);
                }
            }

            // Binarize: A → B1 B2 B3 ... Bn becomes A → B1 X1, X1 → B2 X2, ..., Xn-2 → Bn-1 Bn
            while (replaced.size() > 2) {
                String newNT = "X" + (varIndex++);
                while (g.getNonTerminals().contains(newNT)) newNT += "'";
                g.getNonTerminals().add(newNT);
                List<String> tail = new ArrayList<>(replaced.subList(1, replaced.size()));
                toAdd.add(new Production(newNT, tail));
                replaced = new ArrayList<>(List.of(replaced.get(0), newNT));
                newVarCount++;
            }

            toAdd.add(new Production(p.getLeft(), replaced));
        }

        g.getProductions().removeAll(toRemove);
        g.getProductions().addAll(toAdd);

        // Update terminals set (might have been altered)
        g.getTerminals().clear();
        for (Production p : g.getProductions()) {
            for (String sym : p.getBody()) {
                if (!g.getNonTerminals().contains(sym)) {
                    g.getTerminals().add(sym);
                }
            }
        }

        return newVarCount;
    }

    // ── Helper ────────────────────────────────────────────────────────────────

    private boolean containsProduction(List<Production> list, Production p) {
        return list.stream().anyMatch(x -> x.getLeft().equals(p.getLeft()) &&
                x.getBody().equals(p.getBody()));
    }

    // ── Inner result class ────────────────────────────────────────────────────

    public static class ConversionResult {
        private final Grammar cnfGrammar;
        private final List<String> steps;

        public ConversionResult(Grammar cnfGrammar, List<String> steps) {
            this.cnfGrammar = cnfGrammar;
            this.steps = steps;
        }

        public Grammar getCnfGrammar() { return cnfGrammar; }
        public List<String> getSteps() { return steps; }
    }
}
