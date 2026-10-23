package com.grammarlink.model;

import java.util.List;

/**
 * Represents a single step in the PDA simulation stack trace.
 * Each step records what happened at a particular transition.
 */
public class PDAStep {

    private int stepNumber;
    private String currentState;
    private String remainingInput;
    private String topOfStack;
    private String action;          // e.g., "PUSH (S)", "POP and PUSH (AB)", "MATCH 'a'"
    private List<String> stackContents; // snapshot of the full stack after this step
    private boolean isMatch;        // did a terminal match happen?
    private boolean isAccepting;    // did we reach an accepting configuration?

    public PDAStep() {}

    public PDAStep(int stepNumber, String currentState, String remainingInput,
                   String topOfStack, String action, List<String> stackContents,
                   boolean isMatch, boolean isAccepting) {
        this.stepNumber = stepNumber;
        this.currentState = currentState;
        this.remainingInput = remainingInput;
        this.topOfStack = topOfStack;
        this.action = action;
        this.stackContents = stackContents;
        this.isMatch = isMatch;
        this.isAccepting = isAccepting;
    }

    // Getters and Setters
    public int getStepNumber() { return stepNumber; }
    public void setStepNumber(int stepNumber) { this.stepNumber = stepNumber; }

    public String getCurrentState() { return currentState; }
    public void setCurrentState(String currentState) { this.currentState = currentState; }

    public String getRemainingInput() { return remainingInput; }
    public void setRemainingInput(String remainingInput) { this.remainingInput = remainingInput; }

    public String getTopOfStack() { return topOfStack; }
    public void setTopOfStack(String topOfStack) { this.topOfStack = topOfStack; }

    public String getAction() { return action; }
    public void setAction(String action) { this.action = action; }

    public List<String> getStackContents() { return stackContents; }
    public void setStackContents(List<String> stackContents) { this.stackContents = stackContents; }

    public boolean isMatch() { return isMatch; }
    public void setMatch(boolean match) { isMatch = match; }

    public boolean isAccepting() { return isAccepting; }
    public void setAccepting(boolean accepting) { isAccepting = accepting; }
}
