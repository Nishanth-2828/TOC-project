package com.grammarlink;

import com.grammarlink.model.Grammar;
import com.grammarlink.service.*;
import org.junit.jupiter.api.*;
import org.springframework.beans.factory.annotation.Autowired;
import org.springframework.boot.test.context.SpringBootTest;

import static org.junit.jupiter.api.Assertions.*;

/**
 * Integration tests for the full GrammarLink validation pipeline.
 * Tests: Grammar Parsing, CNF Conversion, CYK Parsing, PDA Simulation.
 */
@SpringBootTest
class GrammarLinkApplicationTests {

    @Autowired private GrammarParserService parserService;
    @Autowired private CNFConverterService cnfConverter;
    @Autowired private CYKParserService cykParser;
    @Autowired private PDASimulatorService pdaSimulator;
    @Autowired private PresetGrammarService presetService;

    // ── Grammar Parsing Tests ─────────────────────────────────────────────────

    @Test
    @DisplayName("Parse balanced parentheses grammar")
    void testParseBalancedParens() {
        Grammar g = parserService.parse("S -> ( S ) S | ε");
        assertFalse(g.getNonTerminals().isEmpty());
        assertEquals("S", g.getStartSymbol());
        assertFalse(g.getProductions().isEmpty());
    }

    @Test
    @DisplayName("Parse error on invalid LHS")
    void testParseInvalidLHS() {
        assertThrows(IllegalArgumentException.class,
                () -> parserService.parse("lowercase -> a b"));
    }

    @Test
    @DisplayName("Parse error on missing arrow")
    void testParseMissingArrow() {
        assertThrows(IllegalArgumentException.class,
                () -> parserService.parse("S  AB | a"));
    }

    // ── CYK Parser Tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("CYK: aⁿbⁿ — accept 'aabb'")
    void testCYKAcceptAnBn() {
        Grammar g = parserService.parse("S -> a S b | a b");
        CNFConverterService.ConversionResult cnf = cnfConverter.convert(g);
        CYKParserService.CYKResult r = cykParser.parse(cnf.getCnfGrammar(), "aabb");
        assertTrue(r.isAccepted(), "Expected 'aabb' to be accepted");
    }

    @Test
    @DisplayName("CYK: aⁿbⁿ — reject 'aab'")
    void testCYKRejectAnBn() {
        Grammar g = parserService.parse("S -> a S b | a b");
        CNFConverterService.ConversionResult cnf = cnfConverter.convert(g);
        CYKParserService.CYKResult r = cykParser.parse(cnf.getCnfGrammar(), "aab");
        assertFalse(r.isAccepted(), "Expected 'aab' to be rejected");
    }

    @Test
    @DisplayName("CYK: balanced parens — accept '(())'")
    void testCYKAcceptBalancedParens() {
        Grammar g = parserService.parse("S -> ( S ) S | ε");
        CNFConverterService.ConversionResult cnf = cnfConverter.convert(g);
        CYKParserService.CYKResult r = cykParser.parse(cnf.getCnfGrammar(), "(())");
        assertTrue(r.isAccepted(), "Expected '(())' to be accepted");
    }

    // ── PDA Simulator Tests ──────────────────────────────────────────────────

    @Test
    @DisplayName("PDA: aⁿbⁿ — accept 'ab'")
    void testPDAAcceptAb() {
        Grammar g = parserService.parse("S -> a S b | a b");
        PDASimulatorService.PDAResult r = pdaSimulator.simulate(g, "ab");
        assertTrue(r.isAccepted(), "PDA should accept 'ab'");
    }

    @Test
    @DisplayName("PDA: aⁿbⁿ — reject 'ba'")
    void testPDARejectBa() {
        Grammar g = parserService.parse("S -> a S b | a b");
        PDASimulatorService.PDAResult r = pdaSimulator.simulate(g, "ba");
        assertFalse(r.isAccepted(), "PDA should reject 'ba'");
    }

    // ── Equivalence Tests ─────────────────────────────────────────────────────

    @Test
    @DisplayName("CFG–PDA Equivalence: aⁿbⁿ — both agree on 'aaabbb'")
    void testEquivalenceAnBn() {
        Grammar g = parserService.parse("S -> a S b | a b");
        CNFConverterService.ConversionResult cnf = cnfConverter.convert(g);
        CYKParserService.CYKResult cyk = cykParser.parse(cnf.getCnfGrammar(), "aaabbb");
        PDASimulatorService.PDAResult pda = pdaSimulator.simulate(g, "aaabbb");
        assertEquals(cyk.isAccepted(), pda.isAccepted(),
                "CYK and PDA must agree on the same string");
    }

    @Test
    @DisplayName("CFG–PDA Equivalence: aⁿbⁿ — both reject 'aaabb'")
    void testEquivalenceRejection() {
        Grammar g = parserService.parse("S -> a S b | a b");
        CNFConverterService.ConversionResult cnf = cnfConverter.convert(g);
        CYKParserService.CYKResult cyk = cykParser.parse(cnf.getCnfGrammar(), "aaabb");
        PDASimulatorService.PDAResult pda = pdaSimulator.simulate(g, "aaabb");
        assertEquals(cyk.isAccepted(), pda.isAccepted(),
                "CYK and PDA must agree on rejection");
    }

    // ── CNF Tests ─────────────────────────────────────────────────────────────

    @Test
    @DisplayName("CNF: converted grammar is valid CNF")
    void testCNFConversion() {
        Grammar g = parserService.parse("S -> a S b | a b");
        CNFConverterService.ConversionResult result = cnfConverter.convert(g);
        Grammar cnf = result.getCnfGrammar();
        assertTrue(cnf.isInCNF(), "Converted grammar should be in CNF");
        assertFalse(result.getSteps().isEmpty(), "Should have conversion steps");
    }

    // ── Preset Tests ──────────────────────────────────────────────────────────

    @Test
    @DisplayName("Presets: all preset grammars load correctly")
    void testAllPresetsLoad() {
        assertFalse(presetService.getAllPresets().isEmpty());
        for (PresetGrammarService.Preset preset : presetService.getAllPresets()) {
            assertDoesNotThrow(() -> parserService.parse(preset.grammarText()),
                    "Preset '" + preset.name() + "' should parse without error");
        }
    }
}
