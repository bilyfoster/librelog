/**
 * Clock Builder Interactive Tour/Walkthrough
 * Guides users through setting up a mixed hour clock template
 */

let tourStep = 0;
let tourActive = false;
let tourData = null;

const tourSteps = [
    {
        title: "Welcome to Clock Builder!",
        content: "This tour will guide you through creating a mixed hour clock template with both music and talk content. Let's get started!",
        target: "#clockBuilderContainer",
        position: "bottom",
        action: null
    },
    {
        title: "Understanding the Timeline",
        content: "This vertical timeline represents a 60-minute hour. Time flows from top (00:00) to bottom (59:59). Each element you add will appear as a block on this timeline.",
        target: "#timelineContainer",
        position: "right",
        action: null
    },
    {
        title: "Adding Elements",
        content: "Use these buttons to add different types of elements: Breaks (commercial slots), Fixed Assets (songs, interviews, IDs), and Automation Commands (system triggers).",
        target: ".clock-builder-actions",
        position: "bottom",
        action: null
    },
    {
        title: "Step 1: Add a Legal ID",
        content: "Let's start with a Legal ID at the top of the hour. Legal IDs are instant elements (no duration) that identify your station. Click '+ Add Fixed Asset' to begin.",
        target: ".btn[onclick*='addFixedAssetToClock']",
        position: "top",
        action: "highlight",
        nextAction: () => {
            // Wait for modal to open, then continue
            setTimeout(() => {
                if (document.getElementById('addFixedAssetModal').classList.contains('show')) {
                    tourStep = 3.5;
                    showTourStep();
                }
            }, 500);
        }
    },
    {
        title: "Legal ID Form",
        content: "Fill in: Name = 'Legal ID', Asset Type = 'ID', Start Time = '00:00', Duration = leave empty (instant), Content Type = 'MIXED'. Then click 'Add Fixed Asset'.",
        target: "#addFixedAssetForm",
        position: "left",
        action: "highlight",
        skipIf: () => !document.getElementById('addFixedAssetModal').classList.contains('show')
    },
    {
        title: "Step 2: Add a Song",
        content: "Now let's add a song. Songs are fixed assets WITH duration. Click '+ Add Fixed Asset' again.",
        target: ".btn[onclick*='addFixedAssetToClock']",
        position: "top",
        action: "highlight"
    },
    {
        title: "Song Form",
        content: "Fill in: Name = 'Morning Song 1', Asset Type = 'SH', Start Time = '00:01', Duration = '240' (4 minutes), Content Type = 'MUSIC', Music Category = 'S1'. Click 'Add Fixed Asset'.",
        target: "#addFixedAssetForm",
        position: "left",
        action: "highlight",
        skipIf: () => !document.getElementById('addFixedAssetModal').classList.contains('show')
    },
    {
        title: "Step 3: Add a Talk Segment",
        content: "For a mixed hour, we need talk content. Click '+ Add Fixed Asset' to add a talk segment.",
        target: ".btn[onclick*='addFixedAssetToClock']",
        position: "top",
        action: "highlight"
    },
    {
        title: "Talk Segment Form",
        content: "Fill in: Name = 'Talk Segment 1', Asset Type = 'SH', Start Time = '00:05', Duration = '300' (5 minutes), Content Type = 'TALK'. Music Category = leave empty. Click 'Add Fixed Asset'.",
        target: "#addFixedAssetForm",
        position: "left",
        action: "highlight",
        skipIf: () => !document.getElementById('addFixedAssetModal').classList.contains('show')
    },
    {
        title: "Step 4: Add a Commercial Break",
        content: "Now let's add a commercial break. Breaks ALWAYS need duration. Click '+ Add Break'.",
        target: ".btn[onclick*='addBreakToClock']",
        position: "top",
        action: "highlight"
    },
    {
        title: "Break Form",
        content: "Fill in: Name = 'Break 1', Start Time = '00:10', Duration = '180' (3 minutes), Asset Type = 'CM', Music Category = 'S1'. Click 'Add Break'.",
        target: "#addBreakForm",
        position: "left",
        action: "highlight",
        skipIf: () => !document.getElementById('addBreakModal').classList.contains('show')
    },
    {
        title: "Understanding Element Types",
        content: "Notice how elements appear on the timeline. Breaks are shown in one color, fixed assets in another. You can drag elements to adjust timing and resize breaks by dragging their edges.",
        target: "#timelineContainer",
        position: "right",
        action: "highlight"
    },
    {
        title: "Validation Panel",
        content: "The validation panel shows any errors or warnings. It checks for overlaps, timing conflicts, and missing required fields. Always check this before exporting!",
        target: "#validationPanel",
        position: "top",
        action: "highlight"
    },
    {
        title: "Revenue Analysis",
        content: "The revenue panel shows potential revenue based on break durations and rates. Use this to optimize your clock for maximum revenue.",
        target: "#revenuePanel",
        position: "top",
        action: "highlight"
    },
    {
        title: "LibreTime Export",
        content: "Once your clock is complete and validated, you can export it to LibreTime. Breaks become smart blocks, fixed assets link to carts/files, and the system generates daily logs.",
        target: "#clockBuilderContainer",
        position: "bottom",
        action: null
    },
    {
        title: "Tour Complete!",
        content: "You now understand how to create a mixed hour! Remember: Breaks always need duration, fixed assets need duration for songs/interviews but not for instant elements like Legal IDs. Use Content Type to categorize your hour (MUSIC, TALK, INTERVIEW, or MIXED).",
        target: "#clockBuilderContainer",
        position: "bottom",
        action: null
    }
];

function startClockBuilderTour() {
    if (!document.getElementById('clockBuilderContainer') || 
        document.getElementById('clockBuilderContainer').style.display === 'none') {
        alert('Please open a clock builder first by clicking "Build" on a clock template.');
        return;
    }
    
    tourActive = true;
    tourStep = 0;
    showTourStep();
}

function showTourStep() {
    if (!tourActive || tourStep >= tourSteps.length) {
        endTour();
        return;
    }
    
    const step = tourSteps[tourStep];
    
    // Skip step if skipIf condition is true
    if (step.skipIf && step.skipIf()) {
        tourStep++;
        showTourStep();
        return;
    }
    
    // Remove existing tour overlay
    removeTourOverlay();
    
    // Create tour overlay
    const overlay = document.createElement('div');
    overlay.id = 'tourOverlay';
    overlay.style.cssText = `
        position: fixed;
        top: 0;
        left: 0;
        width: 100%;
        height: 100%;
        background: rgba(0, 0, 0, 0.7);
        z-index: 10000;
        pointer-events: none;
    `;
    document.body.appendChild(overlay);
    
    // Get target element
    const target = document.querySelector(step.target);
    if (!target) {
        console.warn('Tour target not found:', step.target);
        tourStep++;
        setTimeout(showTourStep, 500);
        return;
    }
    
    // Highlight target
    const rect = target.getBoundingClientRect();
    const highlight = document.createElement('div');
    highlight.id = 'tourHighlight';
    highlight.style.cssText = `
        position: fixed;
        top: ${rect.top}px;
        left: ${rect.left}px;
        width: ${rect.width}px;
        height: ${rect.height}px;
        border: 3px solid #667eea;
        border-radius: 4px;
        box-shadow: 0 0 0 9999px rgba(0, 0, 0, 0.7), 0 0 20px rgba(102, 126, 234, 0.5);
        z-index: 10001;
        pointer-events: none;
        transition: all 0.3s ease;
    `;
    document.body.appendChild(highlight);
    
    // Create tooltip
    const tooltip = document.createElement('div');
    tooltip.id = 'tourTooltip';
    tooltip.style.cssText = `
        position: fixed;
        z-index: 10002;
        background: rgba(26, 26, 46, 0.98);
        border: 1px solid rgba(102, 126, 234, 0.5);
        border-radius: 8px;
        padding: 1.5rem;
        max-width: 400px;
        box-shadow: 0 10px 40px rgba(0, 0, 0, 0.5);
        pointer-events: auto;
    `;
    
    // Position tooltip
    let top, left;
    switch(step.position) {
        case 'top':
            top = rect.top - 200;
            left = rect.left + (rect.width / 2) - 200;
            break;
        case 'bottom':
            top = rect.bottom + 20;
            left = rect.left + (rect.width / 2) - 200;
            break;
        case 'left':
            top = rect.top + (rect.height / 2) - 100;
            left = rect.left - 420;
            break;
        case 'right':
            top = rect.top + (rect.height / 2) - 100;
            left = rect.right + 20;
            break;
        default:
            top = rect.bottom + 20;
            left = rect.left + (rect.width / 2) - 200;
    }
    
    // Keep tooltip in viewport
    if (top < 20) top = 20;
    if (left < 20) left = 20;
    if (left + 400 > window.innerWidth) left = window.innerWidth - 420;
    if (top + 200 > window.innerHeight) top = window.innerHeight - 220;
    
    tooltip.style.top = top + 'px';
    tooltip.style.left = left + 'px';
    
    tooltip.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: start; margin-bottom: 1rem;">
            <h3 style="margin: 0; color: #e0e0e0; font-size: 1.2rem;">${step.title}</h3>
            <button onclick="endTour()" style="background: none; border: none; color: #888; font-size: 1.5rem; cursor: pointer; padding: 0; line-height: 1;">&times;</button>
        </div>
        <p style="margin: 0 0 1.5rem 0; color: #b0b0b0; line-height: 1.6;">${step.content}</p>
        <div style="display: flex; justify-content: space-between; align-items: center;">
            <div style="color: #888; font-size: 0.85rem;">
                Step ${tourStep + 1} of ${tourSteps.length}
            </div>
            <div style="display: flex; gap: 0.5rem;">
                ${tourStep > 0 ? '<button onclick="previousTourStep()" style="padding: 0.5rem 1rem; background: rgba(255,255,255,0.1); border: 1px solid rgba(255,255,255,0.2); border-radius: 4px; color: #e0e0e0; cursor: pointer;">Previous</button>' : ''}
                <button onclick="nextTourStep()" style="padding: 0.5rem 1rem; background: #667eea; border: none; border-radius: 4px; color: white; cursor: pointer; font-weight: 600;">
                    ${tourStep === tourSteps.length - 1 ? 'Finish' : 'Next'}
                </button>
            </div>
        </div>
    `;
    
    document.body.appendChild(tooltip);
    
    // Make target clickable if it's a button
    if (step.action === 'highlight' && target.tagName === 'BUTTON') {
        target.style.pointerEvents = 'auto';
        target.style.zIndex = '10003';
        target.style.position = 'relative';
    }
    
    // Scroll target into view
    target.scrollIntoView({ behavior: 'smooth', block: 'center' });
    
    // Execute nextAction if provided
    if (step.nextAction) {
        step.nextAction();
    }
}

function nextTourStep() {
    tourStep++;
    showTourStep();
}

function previousTourStep() {
    if (tourStep > 0) {
        tourStep--;
        showTourStep();
    }
}

function endTour() {
    tourActive = false;
    removeTourOverlay();
}

function removeTourOverlay() {
    const overlay = document.getElementById('tourOverlay');
    const highlight = document.getElementById('tourHighlight');
    const tooltip = document.getElementById('tourTooltip');
    
    if (overlay) overlay.remove();
    if (highlight) highlight.remove();
    if (tooltip) tooltip.remove();
}

// Export functions to global scope
window.startClockBuilderTour = startClockBuilderTour;
window.nextTourStep = nextTourStep;
window.previousTourStep = previousTourStep;
window.endTour = endTour;

