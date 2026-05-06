/**
 * Clock Builder Quick-Start Module
 * Replaces the blocking tour with template-based quick setup
 */

const CLOCK_TEMPLATES = {
    'music-hour': {
        name: 'Standard Music Hour',
        description: '11 songs + 3 breaks + Legal IDs',
        elements: [
            { type: 'fixed-asset', name: 'Legal ID', startTime: '00:00:00', durationSeconds: 0, assetType: 'ID', musicCategory: null },
            { type: 'fixed-asset', name: 'Current Hit 1', startTime: '00:01:00', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Current Hit 2', startTime: '00:04:30', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Recurrent', startTime: '00:08:00', durationSeconds: 210, assetType: 'SH', musicCategory: 'S2' },
            { type: 'break', name: 'Break 1', startTime: '00:11:30', durationSeconds: 180, assetType: 'CM', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Current Hit 3', startTime: '00:14:30', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Gold', startTime: '00:18:00', durationSeconds: 210, assetType: 'SH', musicCategory: 'S3' },
            { type: 'fixed-asset', name: 'Current Hit 4', startTime: '00:21:30', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'break', name: 'Break 2', startTime: '00:25:00', durationSeconds: 180, assetType: 'CM', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Current Hit 5', startTime: '00:28:00', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Recurrent 2', startTime: '00:31:30', durationSeconds: 210, assetType: 'SH', musicCategory: 'S2' },
            { type: 'fixed-asset', name: 'Current Hit 6', startTime: '00:35:00', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'break', name: 'Break 3', startTime: '00:38:30', durationSeconds: 180, assetType: 'CM', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Gold 2', startTime: '00:41:30', durationSeconds: 210, assetType: 'SH', musicCategory: 'S3' },
            { type: 'fixed-asset', name: 'Current Hit 7', startTime: '00:45:00', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Recurrent 3', startTime: '00:48:30', durationSeconds: 210, assetType: 'SH', musicCategory: 'S2' },
            { type: 'fixed-asset', name: 'Legal ID 2', startTime: '00:52:00', durationSeconds: 0, assetType: 'ID', musicCategory: null },
            { type: 'fixed-asset', name: 'Current Hit 8', startTime: '00:53:00', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Gold 3', startTime: '00:56:30', durationSeconds: 150, assetType: 'SH', musicCategory: 'S3' }
        ]
    },
    'talk-hour': {
        name: 'Talk Radio Hour',
        description: '4 talk segments + 3 breaks + imaging',
        elements: [
            { type: 'fixed-asset', name: 'Station Imaging', startTime: '00:00:00', durationSeconds: 30, assetType: 'IM', musicCategory: null },
            { type: 'fixed-asset', name: 'Talk Segment 1', startTime: '00:00:30', durationSeconds: 600, assetType: 'VT', musicCategory: null },
            { type: 'break', name: 'Break 1', startTime: '00:10:30', durationSeconds: 300, assetType: 'CM', musicCategory: null },
            { type: 'fixed-asset', name: 'Talk Segment 2', startTime: '00:15:30', durationSeconds: 600, assetType: 'VT', musicCategory: null },
            { type: 'break', name: 'Break 2', startTime: '00:25:30', durationSeconds: 300, assetType: 'CM', musicCategory: null },
            { type: 'fixed-asset', name: 'Talk Segment 3', startTime: '00:30:30', durationSeconds: 600, assetType: 'VT', musicCategory: null },
            { type: 'break', name: 'Break 3', startTime: '00:40:30', durationSeconds: 300, assetType: 'CM', musicCategory: null },
            { type: 'fixed-asset', name: 'Talk Segment 4', startTime: '00:45:30', durationSeconds: 600, assetType: 'VT', musicCategory: null },
            { type: 'fixed-asset', name: 'Station Imaging Out', startTime: '00:55:30', durationSeconds: 30, assetType: 'IM', musicCategory: null }
        ]
    },
    'morning-drive': {
        name: 'Morning Drive Mix',
        description: 'High energy: 8 songs + 4 short breaks',
        elements: [
            { type: 'fixed-asset', name: 'Morning Imaging', startTime: '00:00:00', durationSeconds: 15, assetType: 'IM', musicCategory: null },
            { type: 'fixed-asset', name: 'High Energy Opener', startTime: '00:00:15', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Weather/Traffic', startTime: '00:03:45', durationSeconds: 60, assetType: 'VT', musicCategory: null },
            { type: 'fixed-asset', name: 'Power Song 1', startTime: '00:04:45', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'break', name: 'Quick Break 1', startTime: '00:08:15', durationSeconds: 120, assetType: 'CM', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Power Song 2', startTime: '00:10:15', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Power Song 3', startTime: '00:13:45', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'break', name: 'Quick Break 2', startTime: '00:17:15', durationSeconds: 120, assetType: 'CM', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Recurrent Hit', startTime: '00:19:15', durationSeconds: 210, assetType: 'SH', musicCategory: 'S2' },
            { type: 'fixed-asset', name: 'Power Song 4', startTime: '00:22:45', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'break', name: 'Quick Break 3', startTime: '00:26:15', durationSeconds: 120, assetType: 'CM', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Power Song 5', startTime: '00:28:15', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Weather/Traffic 2', startTime: '00:31:45', durationSeconds: 60, assetType: 'VT', musicCategory: null },
            { type: 'fixed-asset', name: 'Power Song 6', startTime: '00:32:45', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'break', name: 'Quick Break 4', startTime: '00:36:15', durationSeconds: 120, assetType: 'CM', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Power Song 7', startTime: '00:38:15', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Gold Classic', startTime: '00:41:45', durationSeconds: 240, assetType: 'SH', musicCategory: 'S3' },
            { type: 'fixed-asset', name: 'Power Song 8', startTime: '00:45:45', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Legal ID', startTime: '00:49:15', durationSeconds: 0, assetType: 'ID', musicCategory: null },
            { type: 'fixed-asset', name: 'Recurrent 2', startTime: '00:50:00', durationSeconds: 210, assetType: 'SH', musicCategory: 'S2' },
            { type: 'fixed-asset', name: 'Request Song', startTime: '00:53:30', durationSeconds: 210, assetType: 'SH', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Morning Imaging Out', startTime: '00:57:00', durationSeconds: 15, assetType: 'IM', musicCategory: null },
            { type: 'fixed-asset', name: 'High Energy Closer', startTime: '00:57:15', durationSeconds: 165, assetType: 'SH', musicCategory: 'S1' }
        ]
    },
    'minimal': {
        name: 'Minimal Template',
        description: 'Legal ID + 2 breaks + fill with music',
        elements: [
            { type: 'fixed-asset', name: 'Legal ID', startTime: '00:00:00', durationSeconds: 0, assetType: 'ID', musicCategory: null },
            { type: 'break', name: 'Break 1', startTime: '00:15:00', durationSeconds: 180, assetType: 'CM', musicCategory: 'S1' },
            { type: 'break', name: 'Break 2', startTime: '00:30:00', durationSeconds: 180, assetType: 'CM', musicCategory: 'S1' },
            { type: 'break', name: 'Break 3', startTime: '00:45:00', durationSeconds: 180, assetType: 'CM', musicCategory: 'S1' },
            { type: 'fixed-asset', name: 'Legal ID Out', startTime: '00:59:00', durationSeconds: 0, assetType: 'ID', musicCategory: null }
        ]
    }
};

/**
 * Show the quick-start modal instead of the tour
 */
function showQuickStartModal() {
    // Remove existing modal if present
    const existingModal = document.getElementById('quickStartModal');
    if (existingModal) existingModal.remove();

    const modal = document.createElement('div');
    modal.id = 'quickStartModal';
    modal.className = 'modal show';
    modal.style.zIndex = '11000';
    
    modal.innerHTML = `
        <div class="modal-content" style="max-width: 700px; max-height: 90vh; overflow-y: auto;">
            <div class="modal-header">
                <h2>🕐 Quick Start: Choose a Template</h2>
                <button class="close-btn" onclick="closeQuickStartModal()">&times;</button>
            </div>
            <div style="padding: 1.5rem;">
                <p style="color: #b0b0b0; margin-bottom: 1.5rem;">
                    Select a template to instantly create a working clock. You can customize it afterward.
                </p>
                
                <div style="display: grid; grid-template-columns: repeat(2, 1fr); gap: 1rem; margin-bottom: 1.5rem;">
                    ${Object.entries(CLOCK_TEMPLATES).map(([key, template]) => `
                        <button onclick="applyClockTemplate('${key}')" 
                                style="text-align: left; padding: 1.25rem; background: rgba(102, 126, 234, 0.1); 
                                       border: 2px solid rgba(102, 126, 234, 0.3); border-radius: 8px; 
                                       cursor: pointer; transition: all 0.2s; color: inherit;"
                                onmouseover="this.style.background='rgba(102, 126, 234, 0.2)'; this.style.borderColor='#667eea';"
                                onmouseout="this.style.background='rgba(102, 126, 234, 0.1)'; this.style.borderColor='rgba(102, 126, 234, 0.3)';">
                            <div style="font-weight: 600; font-size: 1.1rem; margin-bottom: 0.5rem; color: #e0e0e0;">
                                ${template.name}
                            </div>
                            <div style="font-size: 0.9rem; color: #888;">
                                ${template.description}
                            </div>
                            <div style="font-size: 0.8rem; color: #667eea; margin-top: 0.75rem;">
                                ${template.elements.length} elements
                            </div>
                        </button>
                    `).join('')}
                </div>
                
                <div style="border-top: 1px solid rgba(255,255,255,0.1); padding-top: 1.5rem; margin-top: 1.5rem;">
                    <h3 style="margin-bottom: 1rem; color: #d0d0d0;">Other Options</h3>
                    <div style="display: flex; gap: 1rem; flex-wrap: wrap;">
                        <button onclick="copyFromExistingClock()" class="btn btn-secondary">
                            📋 Copy from Existing Clock
                        </button>
                        <button onclick="startFromScratch(); closeQuickStartModal();" class="btn btn-secondary">
                            🔧 Start from Scratch
                        </button>
                        <button onclick="showInlineHelp(); closeQuickStartModal();" class="btn btn-secondary">
                            ❓ Show Help Panel
                        </button>
                    </div>
                </div>
            </div>
        </div>
    `;
    
    document.body.appendChild(modal);
}

function closeQuickStartModal() {
    const modal = document.getElementById('quickStartModal');
    if (modal) modal.remove();
}

/**
 * Apply a template to the current clock
 */
async function applyClockTemplate(templateKey) {
    const template = CLOCK_TEMPLATES[templateKey];
    if (!template || !currentClockTemplate) {
        alert('Please select a clock template first');
        return;
    }

    if (!confirm(`Apply "${template.name}"? This will add ${template.elements.length} elements to your clock.`)) {
        return;
    }

    closeQuickStartModal();
    
    // Show loading
    const container = document.getElementById('timelineContainer');
    if (container) {
        container.innerHTML = '<div class="clock-builder-loading">Applying template... (0/' + template.elements.length + ')</div>';
    }

    try {
        const token = localStorage.getItem('authToken');
        const clockTemplateId = currentClockTemplate.id;
        
        let successCount = 0;
        let errorCount = 0;
        
        // Add each element via API
        for (let i = 0; i < template.elements.length; i++) {
            const element = template.elements[i];
            
            // Update loading text
            if (container) {
                container.innerHTML = `<div class="clock-builder-loading">Applying template... (${i+1}/${template.elements.length}): ${element.name}</div>`;
            }
            
            try {
                if (element.type === 'break') {
                    const response = await fetch(`/api/clock-templates/${clockTemplateId}/breaks`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': `Bearer ${token}`
                        },
                        body: JSON.stringify({
                            clockTemplateId: clockTemplateId,
                            name: element.name,
                            startTime: element.startTime.substring(0, 5), // Format as "00:00"
                            durationSeconds: element.durationSeconds,
                            assetType: element.assetType || 'CM',
                            musicCategory: element.musicCategory,
                            timingType: 'HARD_START',
                            isFloating: false,
                            transitionCode: 'SEGUE'
                        })
                    });
                    
                    if (!response.ok) {
                        const errorText = await response.text();
                        console.error('Break creation failed:', element.name, errorText);
                        errorCount++;
                        continue;
                    }
                    successCount++;
                    
                } else if (element.type === 'fixed-asset') {
                    const response = await fetch(`/api/clock-templates/${clockTemplateId}/fixed-assets`, {
                        method: 'POST',
                        headers: {
                            'Content-Type': 'application/json',
                            'Authorization': `Bearer ${token}`
                        },
                        body: JSON.stringify({
                            clockTemplateId: clockTemplateId,
                            name: element.name,
                            assetType: element.assetType,
                            startTime: element.startTime.substring(0, 5), // Format as "00:00"
                            musicCategory: element.musicCategory,
                            timingType: 'HARD_START',
                            showSegmentName: element.name.replace(/\s+/g, '_').toUpperCase()
                        })
                    });
                    
                    if (!response.ok) {
                        const errorText = await response.text();
                        console.error('Fixed asset creation failed:', element.name, errorText);
                        errorCount++;
                        continue;
                    }
                    successCount++;
                }
            } catch (elementError) {
                console.error('Error creating element:', element.name, elementError);
                errorCount++;
            }
        }

        // Reload the clock
        await loadClockStructure(clockTemplateId);
        
        // Show result
        if (errorCount === 0) {
            showSuccess(`✅ Template "${template.name}" applied! ${successCount} elements added.`);
        } else {
            showSuccess(`⚠️ Template applied with ${successCount} successes and ${errorCount} errors. Check console for details.`);
        }
        
        // Show inline help
        showInlineHelp();
        
    } catch (error) {
        console.error('Error applying template:', error);
        showError('Failed to apply template: ' + error.message);
        
        // Reload anyway to show current state
        if (currentClockTemplate) {
            await loadClockStructure(currentClockTemplate.id);
        }
    }
}

/**
 * Copy from existing clock
 */
async function copyFromExistingClock() {
    alert('Copy from Existing Clock: This feature would show a dropdown of existing clocks to duplicate.\n\nFor now, please use the Templates or Start from Scratch.');
    closeQuickStartModal();
}

/**
 * Show inline help panel instead of blocking tour
 */
function showInlineHelp() {
    // Remove existing help panel
    const existingPanel = document.getElementById('inlineHelpPanel');
    if (existingPanel) existingPanel.remove();

    const panel = document.createElement('div');
    panel.id = 'inlineHelpPanel';
    panel.style.cssText = `
        position: fixed;
        right: 20px;
        top: 100px;
        width: 300px;
        max-height: calc(100vh - 120px);
        overflow-y: auto;
        background: rgba(26, 26, 46, 0.98);
        border: 1px solid rgba(102, 126, 234, 0.3);
        border-radius: 8px;
        padding: 1rem;
        z-index: 1000;
        box-shadow: 0 4px 20px rgba(0,0,0,0.5);
    `;
    
    panel.innerHTML = `
        <div style="display: flex; justify-content: space-between; align-items: center; margin-bottom: 1rem;">
            <h3 style="margin: 0; color: #667eea; font-size: 1rem;">📚 Clock Builder Help</h3>
            <button onclick="document.getElementById('inlineHelpPanel').remove()" 
                    style="background: none; border: none; color: #888; cursor: pointer; font-size: 1.2rem;">&times;</button>
        </div>
        
        <div style="font-size: 0.85rem; color: #b0b0b0; line-height: 1.6;">
            <div style="margin-bottom: 1rem;">
                <strong style="color: #e0e0e0;">Timeline:</strong> Vertical view of your hour. 
                00:00 (top) to 00:59 (bottom).
            </div>
            
            <div style="margin-bottom: 1rem;">
                <strong style="color: #e0e0e0;">Elements:</strong>
                <ul style="margin: 0.5rem 0; padding-left: 1.2rem;">
                    <li><span style="color: #667eea;">Breaks</span> = Commercial slots</li>
                    <li><span style="color: #51cf66;">Assets</span> = Songs, IDs, Voice</li>
                    <li><span style="color: #f093fb;">Commands</span> = Automation</li>
                </ul>
            </div>
            
            <div style="margin-bottom: 1rem;">
                <strong style="color: #e0e0e0;">Actions:</strong>
                <ul style="margin: 0.5rem 0; padding-left: 1.2rem;">
                    <li>Drag to move</li>
                    <li>Click to edit</li>
                    <li>Resize handles = duration</li>
                </ul>
            </div>
            
            <div style="background: rgba(102, 126, 234, 0.1); padding: 0.75rem; border-radius: 4px; font-size: 0.8rem;">
                <strong>💡 Tip:</strong> Breaks always need duration. 
                Fixed assets need duration for songs but not for instant IDs.
            </div>
        </div>
        
        <div style="margin-top: 1rem; padding-top: 1rem; border-top: 1px solid rgba(255,255,255,0.1);">
            <button onclick="showQuickStartModal(); document.getElementById('inlineHelpPanel').remove();" 
                    class="btn btn-primary" style="width: 100%; font-size: 0.85rem; padding: 0.5rem;">
                🕐 Apply Template
            </button>
        </div>
    `;
    
    document.body.appendChild(panel);
}

function startFromScratch() {
    showSuccess('Starting from scratch. Use the buttons above to add elements.');
}

// Export functions
window.showQuickStartModal = showQuickStartModal;
window.closeQuickStartModal = closeQuickStartModal;
window.applyClockTemplate = applyClockTemplate;
window.copyFromExistingClock = copyFromExistingClock;
window.showInlineHelp = showInlineHelp;
window.startFromScratch = startFromScratch;
