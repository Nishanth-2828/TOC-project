/**
 * GrammarLink — Main Frontend JavaScript
 * Handles: AJAX validation, results rendering, CYK table, PDA trace,
 *          parse tree canvas, performance bars, presets, tabs, toasts.
 */

'use strict';

// ── State ──────────────────────────────────────────────────────────────────────
let currentResult = null;
let parseTreeData = [];

// ── DOM Ready ──────────────────────────────────────────────────────────────────
document.addEventListener('DOMContentLoaded', () => {
    initTabs();
    initPresets();
    initForm();
    initExamples();
    animateOnScroll();
});

// ═══════════════════════════════════════════════════════════════════════════════
//  TABS
// ═══════════════════════════════════════════════════════════════════════════════
function initTabs() {
    document.querySelectorAll('.tab-btn').forEach(btn => {
        btn.addEventListener('click', () => {
            const group = btn.closest('.tabs').dataset.group;
            const target = btn.dataset.tab;

            document.querySelectorAll(`.tab-btn[data-group="${group}"]`).forEach(b => b.classList.remove('active'));
            btn.classList.add('active');

            document.querySelectorAll(`.tab-content[data-group="${group}"]`).forEach(c => c.classList.remove('active'));
            const content = document.getElementById(target);
            if (content) {
                content.classList.add('active');
                if (target === 'tab-parse-tree' && parseTreeData.length > 0) {
                    renderParseTree(parseTreeData[0]);
                }
                if (target === 'tab-performance') {
                    animatePerformanceBars();
                }
            }
        });
    });
}

// ═══════════════════════════════════════════════════════════════════════════════
//  PRESET GRAMMAR SELECTOR
// ═══════════════════════════════════════════════════════════════════════════════
function initPresets() {
    document.querySelectorAll('.preset-card').forEach(card => {
        card.addEventListener('click', async () => {
            document.querySelectorAll('.preset-card').forEach(c => c.classList.remove('active'));
            card.classList.add('active');

            const presetId = card.dataset.preset;
            try {
                const resp = await fetch(`/api/presets/${presetId}`);
                if (!resp.ok) throw new Error('Preset not found');
                const preset = await resp.json();

                const grammarArea = document.getElementById('grammarInput');
                const testInput = document.getElementById('testInput');
                if (grammarArea) {
                    grammarArea.value = preset.grammarText;
                    grammarArea.dispatchEvent(new Event('input'));
                }
                if (testInput && preset.exampleAccepted) {
                    testInput.value = preset.exampleAccepted;
                }
                showToast(`Loaded: ${preset.name}`, 'info');
            } catch (e) {
                showToast('Failed to load preset', 'error');
            }
        });
    });
}

// ═══════════════════════════════════════════════════════════════════════════════
//  FORM SUBMISSION
// ═══════════════════════════════════════════════════════════════════════════════
function initForm() {
    const form = document.getElementById('validateForm');
    if (!form) return;

    form.addEventListener('submit', async (e) => {
        e.preventDefault();
        await runValidation();
    });

    // Live character count for test input
    const testInput = document.getElementById('testInput');
    if (testInput) {
        testInput.addEventListener('input', () => {
            const counter = document.getElementById('testCharCount');
            if (counter) counter.textContent = testInput.value.length + ' chars';
        });
    }

    // Clear button
    const clearBtn = document.getElementById('clearBtn');
    if (clearBtn) {
        clearBtn.addEventListener('click', () => {
            document.getElementById('grammarInput').value = '';
            document.getElementById('testInput').value = '';
            document.querySelectorAll('.preset-card').forEach(c => c.classList.remove('active'));
            hideResult();
        });
    }
}

async function runValidation() {
    const grammarText = document.getElementById('grammarInput')?.value?.trim();
    const testString  = document.getElementById('testInput')?.value?.trim() ?? '';

    if (!grammarText) {
        showToast('Please enter grammar rules or select a preset.', 'error');
        return;
    }

    showLoading(true);

    try {
        const response = await fetch('/api/validate', {
            method: 'POST',
            headers: { 'Content-Type': 'application/json' },
            body: JSON.stringify({ grammarText, testString })
        });

        if (!response.ok) throw new Error(`HTTP ${response.status}`);
        const result = await response.json();
        currentResult = result;

        if (result.hasError) {
            showToast(result.errorMessage, 'error');
            hideResult();
        } else {
            renderResult(result);
            showToast(result.cykAccepted ? '✓ String ACCEPTED by the grammar' : '✗ String REJECTED', result.cykAccepted ? 'success' : 'error');
        }
    } catch (err) {
        showToast('Connection error: ' + err.message, 'error');
    } finally {
        showLoading(false);
    }
}

// ═══════════════════════════════════════════════════════════════════════════════
//  RESULT RENDERING
// ═══════════════════════════════════════════════════════════════════════════════
function renderResult(result) {
    const panel = document.getElementById('resultPanel');
    if (!panel) return;

    // ── Verdict ──────────────────────────────────────────────────────────────
    renderVerdict(result);

    // ── CNF Tab ──────────────────────────────────────────────────────────────
    renderCNF(result);

    // ── CYK Tab ──────────────────────────────────────────────────────────────
    renderCYK(result);

    // ── PDA Tab ──────────────────────────────────────────────────────────────
    renderPDA(result);

    // ── Parse Trees ──────────────────────────────────────────────────────────
    parseTreeData = result.parseTrees || [];
    renderParseTreeTab(result);

    // ── Performance ──────────────────────────────────────────────────────────
    renderPerformance(result);

    // ── Ambiguity ────────────────────────────────────────────────────────────
    renderAmbiguity(result);

    // Show panel and scroll to it
    panel.classList.add('visible');
    panel.scrollIntoView({ behavior: 'smooth', block: 'start' });

    // Activate first tab
    const firstTab = panel.querySelector('.tab-btn');
    if (firstTab && !panel.querySelector('.tab-btn.active')) {
        firstTab.click();
    }
}

function renderVerdict(result) {
    const el = document.getElementById('verdictBanner');
    if (!el) return;

    const accepted = result.cykAccepted;
    el.className = 'verdict ' + (accepted ? 'accepted' : 'rejected');

    const eqBadge = result.equivalent
        ? `<div class="equivalence-badge ok">⚖️ CFG–PDA Equivalent</div>`
        : `<div class="equivalence-badge mismatch">⚠️ Result Mismatch</div>`;

    el.innerHTML = `
        <div class="verdict-icon">${accepted ? '✅' : '❌'}</div>
        <div class="verdict-text">
            <div class="verdict-title">${accepted ? 'ACCEPTED' : 'REJECTED'}</div>
            <div class="verdict-subtitle">
                String "<span class="mono">${escHtml(result.testString || 'ε')}</span>"
                ${accepted ? 'belongs to' : 'does not belong to'} the language L(G)
            </div>
            <div class="method-badges mt-sm">
                <span class="method-badge cyk">🔷 CYK: ${result.cykAccepted ? 'Accept' : 'Reject'}</span>
                <span class="method-badge pda">🔶 PDA: ${result.pdaAccepted ? 'Accept' : 'Reject'}</span>
            </div>
        </div>
        <div class="verdict-badge">${eqBadge}</div>
    `;
}

function renderCNF(result) {
    const stepsEl = document.getElementById('cnfSteps');
    const prodsEl = document.getElementById('cnfProductions');
    if (!stepsEl || !prodsEl) return;

    stepsEl.innerHTML = '';
    (result.cnfConversionSteps || []).forEach(step => {
        const div = document.createElement('div');
        div.className = 'cnf-step';
        div.innerHTML = step;
        stepsEl.appendChild(div);
    });

    prodsEl.innerHTML = '';
    (result.cnfProductions || []).forEach(prod => {
        const tag = document.createElement('span');
        tag.className = 'prod-tag';
        const body = prod.body && prod.body.length > 0 ? prod.body.join(' ') : 'ε';
        tag.textContent = prod.left + ' → ' + body;
        prodsEl.appendChild(tag);
    });
}

function renderCYK(result) {
    const tableContainer = document.getElementById('cykTableContainer');
    const stepsEl = document.getElementById('cykStepsList');
    if (!tableContainer) return;

    const input = result.testString || '';
    const table = result.cykTable;

    if (!input || !table || table.length === 0) {
        tableContainer.innerHTML = `<p class="text-muted" style="text-align:center;padding:2rem">
            ${!input ? 'Empty string — CYK checks if start symbol derives ε.' : 'No CYK table data.'}
        </p>`;
    } else {
        const n = table.length;
        const chars = input.split('');

        let html = `<div class="cyk-table-wrapper">
            <table class="cyk-table">
            <thead><tr>
                <th>i\\j</th>
                ${chars.map((c, i) => `<th>${i+1}<br><span class="text-muted">${escHtml(c)}</span></th>`).join('')}
            </tr></thead>
            <tbody>`;

        for (let i = 0; i < n; i++) {
            html += `<tr><th>${i+1}</th>`;
            for (let j = 0; j < n; j++) {
                if (j < i) {
                    html += `<td class="empty" style="background:rgba(0,0,0,0.3)">—</td>`;
                } else {
                    const cell = table[i][j] || '∅';
                    const isKey = i === 0 && j === n-1;
                    const isEmpty = cell === '∅';
                    let cls = isEmpty ? 'empty' : '';
                    if (isKey && !isEmpty) cls += ' has-start';
                    html += `<td class="${cls}" title="T[${i+1}][${j+1}]">${escHtml(cell)}</td>`;
                }
            }
            html += '</tr>';
        }
        html += '</tbody></table></div>';

        // Add legend
        html += `<div class="flex gap-sm mt-md" style="flex-wrap:wrap">
            <span style="font-size:0.75rem;color:var(--text-muted)">
                💡 T[i][j] = non-terminals deriving substring w[i..j].
                <span style="color:var(--accent-green)">Green cell</span> = start symbol found (string accepted).
            </span>
        </div>`;
        tableContainer.innerHTML = html;
    }

    // CYK steps
    if (stepsEl) {
        stepsEl.innerHTML = '';
        (result.cykSteps || []).forEach(step => {
            const div = document.createElement('div');
            div.className = 'cyk-step-item' + (step.startsWith('<b>') ? ' header' : '');
            div.innerHTML = step;
            stepsEl.appendChild(div);
        });
    }
}

function renderPDA(result) {
    const traceEl = document.getElementById('pdaTrace');
    if (!traceEl) return;

    const steps = result.pdaSteps || [];
    if (steps.length === 0) {
        traceEl.innerHTML = '<p class="text-muted" style="text-align:center;padding:2rem">No PDA steps available.</p>';
        return;
    }

    traceEl.innerHTML = '';

    steps.forEach((step, idx) => {
        const div = document.createElement('div');
        const isMatch   = step.match;
        const isAccept  = step.accepting;
        const isApply   = !isMatch && !isAccept;
        div.className = 'pda-step ' + (isAccept ? 'accept' : isMatch ? 'match' : 'apply');
        div.style.animationDelay = Math.min(idx * 0.03, 1) + 's';

        const stackHTML = (step.stackContents || []).slice(0, 8).map(sym => {
            const isNT = /^[A-Z]/.test(sym) && sym !== '$';
            return `<span class="stack-sym ${isNT ? 'nt' : sym === '$' ? 'bottom' : 'term'}">${escHtml(sym)}</span>`;
        }).join('');

        const moreStack = (step.stackContents || []).length > 8
            ? `<span class="stack-sym">+${step.stackContents.length - 8}</span>` : '';

        div.innerHTML = `
            <span class="step-num">#${step.stepNumber}</span>
            <span class="step-action">
                ${isAccept ? '🎯 ' : isMatch ? '✓ ' : '📌 '}
                ${escHtml(step.action)}
                ${step.remainingInput !== undefined && step.remainingInput !== null ?
                    `<br><span style="font-size:0.75rem;color:var(--text-muted)">
                        Remaining: <span class="mono">${escHtml(step.remainingInput) || 'ε'}</span>
                     </span>` : ''}
            </span>
            <div class="step-stack">${stackHTML}${moreStack}</div>
        `;
        traceEl.appendChild(div);
    });

    const stepCountEl = document.getElementById('pdaStepCount');
    if (stepCountEl) stepCountEl.textContent = steps.length + ' steps';
}

function renderParseTreeTab(result) {
    const container = document.getElementById('parseTreeInfo');
    if (!container) return;

    const count = result.parseTreeCount || 0;
    if (count === 0) {
        container.innerHTML = `<p class="text-muted" style="text-align:center;padding:2rem">
            ${result.cykAccepted ? 'Parse tree data not available.' : 'No parse tree (string rejected).'}
        </p>`;
        return;
    }

    container.innerHTML = `<p style="font-size:0.875rem;color:var(--text-secondary);margin-bottom:1rem">
        Found <b style="color:var(--accent-primary)">${count}</b> parse tree(s).
        ${count > 1 ? '<span style="color:var(--accent-orange)">⚠️ Grammar is ambiguous.</span>' : ''}
    </p>`;

    if (parseTreeData.length > 0) {
        renderParseTree(parseTreeData[0]);
    }
}

function renderParseTree(treeData) {
    const canvas = document.getElementById('parseTreeCanvas');
    if (!canvas || !treeData) return;

    const ctx = canvas.getContext('2d');
    const dpr = window.devicePixelRatio || 1;
    const W = canvas.offsetWidth;
    const H = Math.max(300, canvas.offsetHeight);

    canvas.width = W * dpr;
    canvas.height = H * dpr;
    ctx.scale(dpr, dpr);

    ctx.clearRect(0, 0, W, H);
    ctx.fillStyle = 'rgba(15, 22, 41, 1)';
    ctx.fillRect(0, 0, W, H);

    // Calculate tree layout
    const layout = [];
    calculateLayout(treeData, W / 2, 40, W / 4, layout, 0);

    // Draw edges
    ctx.strokeStyle = 'rgba(108, 99, 255, 0.4)';
    ctx.lineWidth = 1.5;
    layout.forEach(node => {
        if (node.parent) {
            ctx.beginPath();
            ctx.moveTo(node.parent.x, node.parent.y);
            ctx.lineTo(node.x, node.y);
            ctx.stroke();
        }
    });

    // Draw nodes
    layout.forEach(node => {
        const isTerminal = !node.children || node.children.length === 0;
        const radius = isTerminal ? 18 : 22;

        // Node circle
        const gradient = ctx.createRadialGradient(node.x, node.y, 0, node.x, node.y, radius);
        if (isTerminal) {
            gradient.addColorStop(0, 'rgba(0, 212, 255, 0.4)');
            gradient.addColorStop(1, 'rgba(0, 212, 255, 0.1)');
            ctx.strokeStyle = 'rgba(0, 212, 255, 0.8)';
        } else {
            gradient.addColorStop(0, 'rgba(108, 99, 255, 0.5)');
            gradient.addColorStop(1, 'rgba(108, 99, 255, 0.1)');
            ctx.strokeStyle = 'rgba(108, 99, 255, 0.8)';
        }

        ctx.beginPath();
        ctx.arc(node.x, node.y, radius, 0, Math.PI * 2);
        ctx.fillStyle = gradient;
        ctx.fill();
        ctx.lineWidth = 1.5;
        ctx.stroke();

        // Label
        ctx.font = 'bold 13px JetBrains Mono, monospace';
        ctx.fillStyle = isTerminal ? '#00d4ff' : '#a78bfa';
        ctx.textAlign = 'center';
        ctx.textBaseline = 'middle';
        ctx.fillText(node.label, node.x, node.y);
    });
}

function calculateLayout(node, x, y, spread, result, depth) {
    const me = { x, y, label: node.label, children: node.children || [], depth };
    result.push(me);

    const children = node.children || [];
    const levelHeight = 70;
    const childSpread = Math.max(spread * 0.6, 40);

    children.forEach((child, i) => {
        const offset = (i - (children.length - 1) / 2) * spread;
        const childNode = calculateLayout(child, x + offset, y + levelHeight, childSpread, result, depth + 1);
        childNode.parent = me;
    });

    return me;
}

function renderAmbiguity(result) {
    const banner = document.getElementById('ambiguityBanner');
    if (!banner) return;

    if (result.ambiguous && (result.ambiguityDetails || []).length > 0) {
        banner.classList.remove('hidden');
        const textEl = banner.querySelector('.text');
        if (textEl) {
            textEl.innerHTML = result.ambiguityDetails.join('<br>');
        }
    } else {
        banner.classList.add('hidden');
    }
}

function renderPerformance(result) {
    const cykTimeEl = document.getElementById('cykTime');
    const pdaTimeEl = document.getElementById('pdaTime');
    if (cykTimeEl) cykTimeEl.textContent = formatNanos(result.cykTimeNanos);
    if (pdaTimeEl) pdaTimeEl.textContent = formatNanos(result.pdaTimeNanos);

    // Store for animated bars
    window._cykNanos = result.cykTimeNanos || 0;
    window._pdaNanos = result.pdaTimeNanos || 0;
}

function animatePerformanceBars() {
    const cykNanos = window._cykNanos || 0;
    const pdaNanos = window._pdaNanos || 0;
    const max = Math.max(cykNanos, pdaNanos, 1);

    const cykBar = document.getElementById('cykBar');
    const pdaBar = document.getElementById('pdaBar');

    setTimeout(() => {
        if (cykBar) cykBar.style.width = Math.max(5, (cykNanos / max) * 100) + '%';
        if (pdaBar) pdaBar.style.width = Math.max(5, (pdaNanos / max) * 100) + '%';
    }, 100);
}

// ═══════════════════════════════════════════════════════════════════════════════
//  UTILITIES
// ═══════════════════════════════════════════════════════════════════════════════
function showLoading(show) {
    const overlay = document.getElementById('loadingOverlay');
    if (!overlay) return;
    if (show) overlay.classList.add('active');
    else overlay.classList.remove('active');

    // Cycle loading messages
    if (show) {
        const messages = [
            'Parsing grammar rules...',
            'Converting to CNF form...',
            'Running CYK algorithm...',
            'Simulating PDA transitions...',
            'Checking equivalence...'
        ];
        let i = 0;
        window._loadingInterval = setInterval(() => {
            const el = document.getElementById('loadingStep');
            if (el) el.textContent = messages[i++ % messages.length];
        }, 600);
    } else {
        clearInterval(window._loadingInterval);
    }
}

function hideResult() {
    const panel = document.getElementById('resultPanel');
    if (panel) panel.classList.remove('visible');
}

function showToast(message, type = 'info') {
    const container = document.getElementById('toastContainer');
    if (!container) return;

    const toast = document.createElement('div');
    toast.className = `toast ${type}`;
    const icons = { success: '✅', error: '❌', info: '💡' };
    toast.innerHTML = `<span>${icons[type] || '💡'}</span><span>${message}</span>`;
    container.appendChild(toast);

    setTimeout(() => {
        toast.style.opacity = '0';
        toast.style.transform = 'translateX(20px)';
        toast.style.transition = 'all 0.3s ease';
        setTimeout(() => toast.remove(), 300);
    }, 4000);
}

function formatNanos(nanos) {
    if (!nanos) return '0 μs';
    if (nanos < 1000) return nanos + ' ns';
    if (nanos < 1_000_000) return (nanos / 1000).toFixed(1) + ' μs';
    return (nanos / 1_000_000).toFixed(2) + ' ms';
}

function escHtml(str) {
    if (str == null) return '';
    return String(str)
        .replace(/&/g, '&amp;')
        .replace(/</g, '&lt;')
        .replace(/>/g, '&gt;')
        .replace(/"/g, '&quot;');
}

// ═══════════════════════════════════════════════════════════════════════════════
//  EXAMPLE BUTTONS (quick test)
// ═══════════════════════════════════════════════════════════════════════════════
function initExamples() {
    document.querySelectorAll('[data-example-accept]').forEach(btn => {
        btn.addEventListener('click', () => {
            const input = document.getElementById('testInput');
            if (input) {
                input.value = btn.dataset.exampleAccept;
                input.dispatchEvent(new Event('input'));
            }
        });
    });
    document.querySelectorAll('[data-example-reject]').forEach(btn => {
        btn.addEventListener('click', () => {
            const input = document.getElementById('testInput');
            if (input) {
                input.value = btn.dataset.exampleReject;
                input.dispatchEvent(new Event('input'));
            }
        });
    });
}

// ═══════════════════════════════════════════════════════════════════════════════
//  SCROLL ANIMATIONS
// ═══════════════════════════════════════════════════════════════════════════════
function animateOnScroll() {
    const observer = new IntersectionObserver((entries) => {
        entries.forEach(entry => {
            if (entry.isIntersecting) {
                entry.target.style.opacity = '1';
                entry.target.style.transform = 'translateY(0)';
                observer.unobserve(entry.target);
            }
        });
    }, { threshold: 0.1 });

    document.querySelectorAll('.feature-card, .preset-card, .card').forEach(el => {
        el.style.opacity = '0';
        el.style.transform = 'translateY(20px)';
        el.style.transition = 'opacity 0.5s ease, transform 0.5s ease';
        observer.observe(el);
    });
}

// ═══════════════════════════════════════════════════════════════════════════════
//  NAVBAR ACTIVE LINK
// ═══════════════════════════════════════════════════════════════════════════════
(function markActiveNav() {
    const path = window.location.pathname;
    document.querySelectorAll('.nav-links a').forEach(link => {
        if (link.getAttribute('href') === path) link.classList.add('active');
    });
})();
