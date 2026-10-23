package com.grammarlink.service;

import com.grammarlink.model.Grammar;
import com.grammarlink.model.PDAStep;
import com.grammarlink.model.Production;
import org.springframework.stereotype.Service;

import java.util.*;

/**
 * PDASimulatorService: Simulates a Pushdown Automaton (PDA) derived from the CFG.
 *
 * The PDA is constructed using the standard CFG → PDA construction:
 *   States: {q_start, q_loop, q_accept}
 *   Stack alphabet: Σ ∪ V ∪ {$} (terminals + non-terminals + bottom marker)
 *
 * Transitions:
 *   (q_start, ε, ε) → (q_loop, S$)          — push start symbol and bottom
 *   (q_loop, ε, A) → (q_loop, body)           — for each production A → body
 *   (q_loop, a, a) → (q_loop, ε)              — match terminal on top
 *   (q_loop, ε, $) → (q_accept, ε)            — accept on empty stack
 *
 * Uses recursive backtracking to find an accepting path.
 * Returns full step-by-step stack trace.
 */
@Service
public class PDASimulatorService {

    private static final int MAX_STEPS = 2000; // Safety limit against infinite loops
    private static final int MAX_DEPTH = 200;

    public PDAResult simulate(Grammar grammar, String input) {
        long startTime = System.nanoTime();
        List<PDAStep> allSteps = new ArrayList<>();

        char[] inputChars = input.toCharArray();

        // Initial PDA configuration: stack = [S, $], input position = 0
        Deque<String> initialStack = new ArrayDeque<>();
        initialStack.push("$");
        initialStack.push(grammar.getStartSymbol());

        // Try to find accepting computation path via DFS backtracking
        boolean accepted = dfsSimulate(grammar, inputChars, 0, initialStack, allSteps, new int[]{0}, 0);

        if (!accepted && allSteps.isEmpty()) {
            // Add a failure step
            allSteps.add(new PDAStep(1, "q_loop", input, grammar.getStartSymbol(),
                    "No valid path found — string REJECTED", List.of(grammar.getStartSymbol(), "$"), false, false));
        }

        return new PDAResult(accepted, allSteps, System.nanoTime() - startTime);
    }

    /**
     * DFS backtracking simulation.
     * Returns true if an accepting path is found.
     * Fills `steps` with the accepting path's steps.
     */
    private boolean dfsSimulate(Grammar grammar, char[] input, int pos,
                                  Deque<String> stack, List<PDAStep> steps,
                                  int[] stepCounter, int depth) {

        if (depth > MAX_DEPTH || stepCounter[0] > MAX_STEPS) return false;

        String stackTop = stack.isEmpty() ? null : stack.peek();

        // ── Accepting condition: input exhausted and stack has only $ ─────────
        if (stackTop != null && stackTop.equals("$") && pos == input.length) {
            Deque<String> acceptStack = new ArrayDeque<>(stack);
            acceptStack.pop(); // remove $
            steps.add(new PDAStep(
                    ++stepCounter[0], "q_accept",
                    "", "$",
                    "Stack bottom $ reached with empty input — ✓ ACCEPTED",
                    new ArrayList<>(), false, true
            ));
            return true;
        }

        if (stack.isEmpty() || stackTop == null) return false;

        // ── Case 1: Top of stack is a NON-TERMINAL → apply a production ──────
        if (grammar.getNonTerminals().contains(stackTop)) {
            List<Production> productions = grammar.getProductionsFor(stackTop);
            if (productions.isEmpty()) return false;

            for (Production p : productions) {
                // Try applying this production
                Deque<String> newStack = new ArrayDeque<>(stack);
                newStack.pop(); // remove non-terminal

                // Push body in reverse order (so first symbol is on top)
                List<String> body = p.getBody();
                if (!body.isEmpty()) {
                    List<String> reversed = new ArrayList<>(body);
                    Collections.reverse(reversed);
                    for (String sym : reversed) newStack.push(sym);
                }

                List<String> stackSnapshot = new ArrayList<>(newStack);
                String stackStr = body.isEmpty() ? "ε" : String.join("", body);

                PDAStep step = new PDAStep(
                        ++stepCounter[0], "q_loop",
                        new String(input, pos, input.length - pos),
                        stackTop,
                        "Apply: " + stackTop + " → " + (body.isEmpty() ? "ε" : stackStr),
                        stackSnapshot, false, false
                );

                List<PDAStep> subSteps = new ArrayList<>();
                subSteps.add(step);

                if (dfsSimulate(grammar, input, pos, newStack, subSteps, stepCounter, depth + 1)) {
                    steps.addAll(subSteps);
                    return true;
                }
                // Backtrack: undo the step counter for failed attempts
                stepCounter[0]--;
            }
            return false;
        }

        // ── Case 2: Top of stack is a TERMINAL → must match current input ─────
        if (pos < input.length && stackTop.equals(String.valueOf(input[pos]))) {
            Deque<String> newStack = new ArrayDeque<>(stack);
            newStack.pop();

            List<String> stackSnapshot = new ArrayList<>(newStack);
            String remaining = new String(input, pos + 1, input.length - pos - 1);

            steps.add(new PDAStep(
                    ++stepCounter[0], "q_loop",
                    remaining,
                    stackTop,
                    "Match terminal '" + stackTop + "' — consume from input ✓",
                    stackSnapshot, true, false
            ));

            return dfsSimulate(grammar, input, pos + 1, newStack, steps, stepCounter, depth + 1);
        }

        // ── Mismatch: fail this path ──────────────────────────────────────────
        return false;
    }

    // ── Result class ──────────────────────────────────────────────────────────

    public static class PDAResult {
        private final boolean accepted;
        private final List<PDAStep> steps;
        private final long timeNanos;

        public PDAResult(boolean accepted, List<PDAStep> steps, long timeNanos) {
            this.accepted = accepted;
            this.steps = steps;
            this.timeNanos = timeNanos;
        }

        public boolean isAccepted() { return accepted; }
        public List<PDAStep> getSteps() { return steps; }
        public long getTimeNanos() { return timeNanos; }
    }
}
