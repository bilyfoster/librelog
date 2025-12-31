/**
 * Clock Builder JavaScript
 * Handles timeline visualization, drag-and-drop, and clock element management
 */

// Global state
let currentClockTemplate = null;
let clockElements = [];
let selectedElement = null;
let isDragging = false;
let dragOffset = { x: 0, y: 0 };
let timelineScale = 60; // pixels per minute vertically (1 minute = 60px height)

// Initialize clock builder
function initClockBuilder(clockTemplateId) {
    if (!clockTemplateId) {
        showClockBuilderEmpty();
        return;
    }

    loadClockStructure(clockTemplateId);
}

// Load clock structure from API
async function loadClockStructure(clockTemplateId) {
    try {
        const token = localStorage.getItem('authToken');
        if (!token) {
            throw new Error('Authentication token not found. Please log in again.');
        }
        const response = await fetch(`/api/clock-templates/${clockTemplateId}/structure`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });

        if (!response.ok) {
            throw new Error('Failed to load clock structure');
        }

        const data = await response.json();
        currentClockTemplate = data;
        clockElements = [];

        // Process breaks
        if (data.breaks) {
            data.breaks.forEach(breakItem => {
                clockElements.push({
                    id: breakItem.id,
                    type: 'break',
                    name: breakItem.name,
                    startTime: breakItem.startTime,
                    durationSeconds: breakItem.durationSeconds,
                    isFloating: breakItem.isFloating || false,
                    availType: breakItem.availTypeName,
                    availTypeId: breakItem.availTypeId,
                    timingType: breakItem.timingType,
                    transitionCode: breakItem.transitionCode,
                    assetType: breakItem.assetType,
                    musicCategory: breakItem.musicCategory,
                    showSegmentName: breakItem.showSegmentName
                });
            });
        }

        // Process fixed assets
        if (data.fixedAssets) {
            data.fixedAssets.forEach(asset => {
                clockElements.push({
                    id: asset.id,
                    type: 'fixed-asset',
                    name: asset.name,
                    startTime: asset.startTime,
                    assetType: asset.assetType,
                    assetIdentifier: asset.assetIdentifier,
                    timingType: asset.timingType,
                    musicCategory: asset.musicCategory,
                    showSegmentName: asset.showSegmentName
                });
            });
        }

        // Process automation commands
        if (data.automationCommands) {
            data.automationCommands.forEach(command => {
                clockElements.push({
                    id: command.id,
                    type: 'automation-command',
                    name: command.commandType,
                    startTime: command.triggerTime,
                    commandType: command.commandType,
                    priority: command.priority
                });
            });
        }

        renderTimeline();
        loadValidation(clockTemplateId);
        loadRevenueAnalysis(clockTemplateId);
        
        // Normalize all time inputs after loading
        setTimeout(() => {
            document.querySelectorAll('input[type="time"]').forEach(input => {
                normalizeTimeInput(input);
            });
        }, 100);
    } catch (error) {
        console.error('Error loading clock structure:', error);
        showClockBuilderError(error.message);
    }
}

// Render timeline with all elements
function renderTimeline() {
    const container = document.getElementById('timelineContainer');
    if (!container) return;

    // Clear existing content
    container.innerHTML = '';

    // Create timeline wrapper
    const wrapper = document.createElement('div');
    wrapper.className = 'timeline-wrapper';
    wrapper.id = 'timelineWrapper';

    // Create ruler
    const ruler = document.createElement('div');
    ruler.className = 'timeline-ruler';
    ruler.id = 'timelineRuler';

    // Add time markers (every minute, major every 5 minutes)
    for (let i = 0; i <= 60; i++) {
        const marker = document.createElement('div');
        marker.className = 'timeline-marker' + (i % 5 === 0 ? ' major' : '');
        const label = document.createElement('span');
        label.className = 'timeline-marker-label';
        if (i % 5 === 0) {
            label.textContent = formatTime(i);
        }
        marker.appendChild(label);
        ruler.appendChild(marker);
    }

    wrapper.appendChild(ruler);

    // Create timeline track
    const track = document.createElement('div');
    track.className = 'timeline-track';
    track.id = 'timelineTrack';

    // Add snap grid lines (every 15 minutes) - horizontal lines
    for (let i = 0; i <= 60; i += 15) {
        const gridLine = document.createElement('div');
        gridLine.className = 'snap-grid-line';
        gridLine.style.top = `${i * timelineScale}px`;
        track.appendChild(gridLine);
    }

    // Render clock elements
    clockElements.forEach(element => {
        const elementBlock = createElementBlock(element);
        track.appendChild(elementBlock);
    });

    wrapper.appendChild(track);
    container.appendChild(wrapper);

    // Initialize drag and drop
    initDragAndDrop();
}

// Create element block for timeline
function createElementBlock(element) {
    const block = document.createElement('div');
    block.className = `clock-element ${element.type}`;
    if (element.isFloating) {
        block.classList.add('floating');
    }
    block.id = `element-${element.id}`;
    block.setAttribute('data-element-id', element.id);
    block.setAttribute('data-element-type', element.type);
    block.setAttribute('tabindex', '0');
    block.setAttribute('role', 'button');
    block.setAttribute('aria-label', `${element.type} ${element.name} at ${formatTimeFromString(element.startTime)}`);

    // Calculate position and height (vertical layout)
    const startMinutes = timeToMinutes(element.startTime);
    const durationMinutes = element.durationSeconds ? element.durationSeconds / 60 : 0.5; // Default 30 seconds for point elements
    const top = startMinutes * timelineScale;
    const height = durationMinutes * timelineScale;

    block.style.top = `${top}px`;
    block.style.height = `${Math.max(height, 30)}px`; // Minimum 30px height

    // Set content
    block.innerHTML = `
        <span class="element-name">${element.name}</span>
        ${element.type === 'break' ? `<span class="element-duration">${formatDuration(element.durationSeconds)}</span>` : ''}
        <div class="resize-handle top"></div>
        <div class="resize-handle bottom"></div>
    `;

    // Add click handler
    block.addEventListener('click', (e) => {
        if (!isDragging) {
            selectElement(element.id);
        }
    });

    // Add keyboard handler
    block.addEventListener('keydown', (e) => {
        if (e.key === 'Enter' || e.key === ' ') {
            e.preventDefault();
            selectElement(element.id);
        }
    });

    return block;
}

// Initialize drag and drop functionality
function initDragAndDrop() {
    const elements = document.querySelectorAll('.clock-element');
    
    elements.forEach(element => {
        // Make draggable
        element.addEventListener('mousedown', handleDragStart);
        element.addEventListener('touchstart', handleDragStart, { passive: false });

        // Add resize handles (top/bottom for vertical layout)
        const topHandle = element.querySelector('.resize-handle.top');
        const bottomHandle = element.querySelector('.resize-handle.bottom');

        if (topHandle) {
            topHandle.addEventListener('mousedown', (e) => {
                e.stopPropagation();
                handleResizeStart(e, element, 'top');
            });
        }

        if (bottomHandle) {
            bottomHandle.addEventListener('mousedown', (e) => {
                e.stopPropagation();
                handleResizeStart(e, element, 'bottom');
            });
        }
    });
}

// Handle drag start
function handleDragStart(e) {
    e.preventDefault();
    isDragging = true;
    selectedElement = e.currentTarget;
    
    const elementId = selectedElement.getAttribute('data-element-id');
    const element = clockElements.find(el => el.id === elementId);
    
    if (!element) return;

    const rect = selectedElement.getBoundingClientRect();
    const trackRect = document.getElementById('timelineTrack').getBoundingClientRect();
    
    dragOffset.x = (e.clientX || e.touches[0].clientX) - rect.left;
    dragOffset.y = (e.clientY || e.touches[0].clientY) - rect.top; // Vertical offset

    selectedElement.classList.add('dragging');

    // Add move and end handlers
    document.addEventListener('mousemove', handleDrag);
    document.addEventListener('mouseup', handleDragEnd);
    document.addEventListener('touchmove', handleDrag, { passive: false });
    document.addEventListener('touchend', handleDragEnd);
}

// Handle drag (vertical layout)
function handleDrag(e) {
    if (!isDragging || !selectedElement) return;

    e.preventDefault();
    const clientY = e.clientY || e.touches[0].clientY;
    const track = document.getElementById('timelineTrack');
    const trackRect = track.getBoundingClientRect();
    
    let newTop = clientY - trackRect.top - dragOffset.y;
    newTop = Math.max(0, Math.min(newTop, 3600 - selectedElement.offsetHeight)); // Constrain to timeline (60 minutes)
    
    // Snap to grid (every 15 minutes)
    const snapInterval = 15 * timelineScale;
    newTop = Math.round(newTop / snapInterval) * snapInterval;
    
    selectedElement.style.top = `${newTop}px`;
    
    // Update tooltip
    updateDragTooltip(e, newTop);
}

// Handle drag end (vertical layout)
function handleDragEnd(e) {
    if (!isDragging || !selectedElement) return;

    e.preventDefault();
    isDragging = false;
    
    const elementId = selectedElement.getAttribute('data-element-id');
    const element = clockElements.find(el => el.id === elementId);
    
    if (element) {
        // Calculate new start time from vertical position
        const newTop = parseInt(selectedElement.style.top) || 0;
        const newMinutes = newTop / timelineScale;
        const newTime = minutesToTime(newMinutes);
        
        // Update element
        element.startTime = newTime;
        
        // Save to backend
        saveElementPosition(element);
    }
    
    selectedElement.classList.remove('dragging');
    selectedElement = null;
    
    // Remove tooltip
    removeDragTooltip();
    
    // Remove event listeners
    document.removeEventListener('mousemove', handleDrag);
    document.removeEventListener('mouseup', handleDragEnd);
    document.removeEventListener('touchmove', handleDrag);
    document.removeEventListener('touchend', handleDragEnd);
}

// Handle resize start (vertical layout)
function handleResizeStart(e, element, direction) {
    e.preventDefault();
    e.stopPropagation();
    
    const startY = e.clientY || e.touches[0].clientY;
    const startTop = parseInt(element.style.top) || 0;
    const startHeight = element.offsetHeight;
    
    const elementId = element.getAttribute('data-element-id');
    const clockElement = clockElements.find(el => el.id === elementId);
    
    if (!clockElement) return;
    
    const handleResize = (moveEvent) => {
        moveEvent.preventDefault();
        const currentY = moveEvent.clientY || moveEvent.touches[0].clientY;
        const deltaY = currentY - startY;
        
        if (direction === 'top') {
            const newTop = Math.max(0, startTop + deltaY);
            const newHeight = Math.max(30, startHeight - deltaY);
            element.style.top = `${newTop}px`;
            element.style.height = `${newHeight}px`;
        } else { // bottom
            const newHeight = Math.max(30, startHeight + deltaY);
            const maxHeight = 3600 - parseInt(element.style.top) || 0;
            element.style.height = `${Math.min(newHeight, maxHeight)}px`;
        }
    };
    
    const handleResizeEnd = (endEvent) => {
        endEvent.preventDefault();
        const newTop = parseInt(element.style.top) || 0;
        const newHeight = parseInt(element.style.height) || 30;
        const newMinutes = newTop / timelineScale;
        const newDurationSeconds = (newHeight / timelineScale) * 60;
        
        clockElement.startTime = minutesToTime(newMinutes);
        if (clockElement.durationSeconds !== undefined) {
            clockElement.durationSeconds = Math.round(newDurationSeconds);
        }
        
        saveElementPosition(clockElement);
        
        document.removeEventListener('mousemove', handleResize);
        document.removeEventListener('mouseup', handleResizeEnd);
        document.removeEventListener('touchmove', handleResize);
        document.removeEventListener('touchend', handleResizeEnd);
    };
    
    document.addEventListener('mousemove', handleResize);
    document.addEventListener('mouseup', handleResizeEnd);
    document.addEventListener('touchmove', handleResize, { passive: false });
    document.addEventListener('touchend', handleResizeEnd);
}

// Select element
function selectElement(elementId) {
    // Deselect previous
    document.querySelectorAll('.clock-element.selected').forEach(el => {
        el.classList.remove('selected');
    });
    
    // Select new
    const element = document.getElementById(`element-${elementId}`);
    if (element) {
        element.classList.add('selected');
        selectedElement = element;
        
        // Open properties panel
        openPropertiesPanel(elementId);
    }
}

// Open properties panel
function openPropertiesPanel(elementId) {
    const element = clockElements.find(el => el.id === elementId);
    if (!element) return;
    
    const panel = document.getElementById('propertiesPanel');
    if (!panel) return;
    
    panel.classList.add('open');
    
    // Populate form
    populatePropertiesForm(element);
}

// Close properties panel
function closePropertiesPanel() {
    const panel = document.getElementById('propertiesPanel');
    if (panel) {
        panel.classList.remove('open');
    }
    selectedElement = null;
    document.querySelectorAll('.clock-element.selected').forEach(el => {
        el.classList.remove('selected');
    });
}

// Populate properties form
function populatePropertiesForm(element) {
    // This will be implemented based on element type
    const form = document.getElementById('propertiesForm');
    if (!form) return;
    
    // Clear form
    form.innerHTML = '';
    
    // Add form fields based on element type
    if (element.type === 'break') {
        const assetTypeOptions = ['', 'IM', 'ID', 'CM', 'PR', 'VT', 'SH'].map(opt => 
            `<option value="${opt}" ${element.assetType === opt ? 'selected' : ''}>${opt || 'None'}</option>`
        ).join('');
        const musicCategoryOptions = ['', 'S1', 'S2', 'S3'].map(opt => 
            `<option value="${opt}" ${element.musicCategory === opt ? 'selected' : ''}>${opt || 'None'}</option>`
        ).join('');
        const transitionCodeOptions = ['', 'SEGUE', 'OVERLAP', 'HARD_START'].map(opt => 
            `<option value="${opt}" ${element.transitionCode === opt ? 'selected' : ''}>${opt || 'None'}</option>`
        ).join('');
        const timingTypeOptions = ['', 'HARD_START', 'SOFT_START'].map(opt => 
            `<option value="${opt}" ${element.timingType === opt ? 'selected' : ''}>${opt || 'None'}</option>`
        ).join('');
        
        form.innerHTML = `
            <div class="properties-form-group">
                <label>Name</label>
                <input type="text" id="propName" value="${element.name || ''}">
            </div>
            <div class="properties-form-group">
                <label>Start Time (00:00 to 00:59)</label>
                <input type="text" id="propStartTime" value="${formatTimeForInput(element.startTime)}" pattern="00:[0-5][0-9]" placeholder="00:00" maxlength="5" style="font-family: monospace; font-size: 1rem; text-align: center;">
            </div>
            <div class="properties-form-group">
                <label>Duration (seconds)</label>
                <input type="number" id="propDuration" value="${element.durationSeconds || 60}" min="1">
            </div>
            <div class="properties-form-group">
                <label>Floating</label>
                <input type="checkbox" id="propIsFloating" ${element.isFloating ? 'checked' : ''}>
            </div>
            <div class="properties-form-group">
                <label>Timing Type</label>
                <select id="propTimingType">${timingTypeOptions}</select>
            </div>
            <div class="properties-form-group">
                <label>Transition Code</label>
                <select id="propTransitionCode">${transitionCodeOptions}</select>
            </div>
            <div class="properties-form-group">
                <label>Asset Type (WideOrbit)</label>
                <select id="propAssetType">${assetTypeOptions}</select>
            </div>
            <div class="properties-form-group">
                <label>Music Category (WideOrbit)</label>
                <select id="propMusicCategory">${musicCategoryOptions}</select>
            </div>
            <div class="properties-form-group">
                <label>Show Segment Name (WideOrbit)</label>
                <input type="text" id="propShowSegmentName" value="${element.showSegmentName || ''}" placeholder="e.g., SH_MORNING_SEG1">
            </div>
            <div class="properties-form-group">
                <button type="button" class="btn btn-primary" onclick="saveElementProperties('${element.id}')">Save</button>
                <button type="button" class="btn btn-secondary" onclick="deleteElement('${element.id}')">Delete</button>
            </div>
        `;
    } else if (element.type === 'fixed-asset') {
        const assetTypeOptions = ['IM', 'ID', 'CM', 'PR', 'VT', 'SH'].map(opt => 
            `<option value="${opt}" ${element.assetType === opt ? 'selected' : ''}>${opt}</option>`
        ).join('');
        const musicCategoryOptions = ['', 'S1', 'S2', 'S3'].map(opt => 
            `<option value="${opt}" ${element.musicCategory === opt ? 'selected' : ''}>${opt || 'None'}</option>`
        ).join('');
        const timingTypeOptions = ['HARD_START', 'SOFT_START'].map(opt => 
            `<option value="${opt}" ${element.timingType === opt ? 'selected' : ''}>${opt}</option>`
        ).join('');
        
        form.innerHTML = `
            <div class="properties-form-group">
                <label>Name</label>
                <input type="text" id="propName" value="${element.name || ''}">
            </div>
            <div class="properties-form-group">
                <label>Asset Type (WideOrbit)</label>
                <select id="propAssetType">${assetTypeOptions}</select>
            </div>
            <div class="properties-form-group">
                <label>Start Time (00:00 to 00:59)</label>
                <input type="text" id="propStartTime" value="${formatTimeForInput(element.startTime)}" pattern="00:[0-5][0-9]" placeholder="00:00" maxlength="5" style="font-family: monospace; font-size: 1rem; text-align: center;">
            </div>
            <div class="properties-form-group">
                <label>Asset Identifier</label>
                <input type="text" id="propAssetIdentifier" value="${element.assetIdentifier || ''}">
            </div>
            <div class="properties-form-group">
                <label>Timing Type</label>
                <select id="propTimingType">${timingTypeOptions}</select>
            </div>
            <div class="properties-form-group">
                <label>Music Category (WideOrbit)</label>
                <select id="propMusicCategory">${musicCategoryOptions}</select>
            </div>
            <div class="properties-form-group">
                <label>Show Segment Name (WideOrbit)</label>
                <input type="text" id="propShowSegmentName" value="${element.showSegmentName || ''}" placeholder="e.g., SH_MORNING_SEG1">
            </div>
            <div class="properties-form-group">
                <button type="button" class="btn btn-primary" onclick="saveElementProperties('${element.id}')">Save</button>
                <button type="button" class="btn btn-secondary" onclick="deleteElement('${element.id}')">Delete</button>
            </div>
        `;
    }
    // Add other element types as needed
}

// Save element properties from form
async function saveElementProperties(elementId) {
    const element = clockElements.find(el => el.id === elementId);
    if (!element) return;
    
    try {
        // Read form values
        const name = document.getElementById('propName')?.value;
        const startTimeInput = document.getElementById('propStartTime');
        let startTime = startTimeInput?.value;
        
        // Validate time input to ensure 24-hour format
        if (startTimeInput) {
            validateTimeInput(startTimeInput);
            const inputValue = startTimeInput.value.trim();
            startTime = inputValue && inputValue.match(/^00:[0-5][0-9]$/) ? inputValue : startTime;
        }
        
        if (element.type === 'break') {
            element.name = name;
            element.startTime = startTime ? startTime + ':00' : element.startTime;
            element.durationSeconds = parseInt(document.getElementById('propDuration')?.value || element.durationSeconds);
            element.isFloating = document.getElementById('propIsFloating')?.checked || false;
            element.timingType = document.getElementById('propTimingType')?.value || null;
            element.transitionCode = document.getElementById('propTransitionCode')?.value || null;
            element.assetType = document.getElementById('propAssetType')?.value || null;
            element.musicCategory = document.getElementById('propMusicCategory')?.value || null;
            element.showSegmentName = document.getElementById('propShowSegmentName')?.value || null;
        } else if (element.type === 'fixed-asset') {
            element.name = name;
            element.startTime = startTime ? startTime + ':00' : element.startTime;
            element.assetType = document.getElementById('propAssetType')?.value;
            element.assetIdentifier = document.getElementById('propAssetIdentifier')?.value || null;
            element.timingType = document.getElementById('propTimingType')?.value || null;
            element.musicCategory = document.getElementById('propMusicCategory')?.value || null;
            element.showSegmentName = document.getElementById('propShowSegmentName')?.value || null;
        }
        
        // Save to backend
        await saveElementPosition(element);
        
        // Re-render timeline
        renderTimeline();
        
        // Close properties panel
        closePropertiesPanel();
        
        // Show success message
        alert('Element properties saved successfully');
    } catch (error) {
        console.error('Error saving element properties:', error);
        alert('Failed to save element properties: ' + error.message);
    }
}

// Save element position
async function saveElementPosition(element) {
    try {
        const token = localStorage.getItem('authToken');
        let endpoint = '';
        let method = 'PUT';
        let body = {};
        
        if (element.type === 'break') {
            endpoint = `/api/clock-templates/breaks/${element.id}`;
            body = {
                clockTemplateId: currentClockTemplate.id,
                name: element.name,
                startTime: element.startTime,
                durationSeconds: element.durationSeconds,
                isFloating: element.isFloating,
                availTypeId: element.availTypeId || null,
                timingType: element.timingType || null,
                transitionCode: element.transitionCode || null,
                assetType: element.assetType || null,
                musicCategory: element.musicCategory || null,
                showSegmentName: element.showSegmentName || null
            };
        } else if (element.type === 'fixed-asset') {
            endpoint = `/api/clock-templates/fixed-assets/${element.id}`;
            body = {
                clockTemplateId: currentClockTemplate.id,
                name: element.name,
                assetType: element.assetType,
                startTime: element.startTime,
                assetIdentifier: element.assetIdentifier || null,
                timingType: element.timingType || null,
                musicCategory: element.musicCategory || null,
                showSegmentName: element.showSegmentName || null
            };
        } else if (element.type === 'automation-command') {
            endpoint = `/api/clock-templates/automation-commands/${element.id}`;
            body = {
                clockTemplateId: currentClockTemplate.id,
                commandType: element.commandType,
                triggerTime: element.startTime,
                priority: element.priority,
                parameters: element.parameters || {}
            };
        }
        
        const response = await fetch(endpoint, {
            method: method,
            headers: {
                'Content-Type': 'application/json',
                'Authorization': `Bearer ${token}`
            },
            body: JSON.stringify(body)
        });
        
        if (!response.ok) {
            throw new Error('Failed to save element');
        }
        
        // Reload validation and revenue
        if (currentClockTemplate) {
            loadValidation(currentClockTemplate.id);
            loadRevenueAnalysis(currentClockTemplate.id);
        }
    } catch (error) {
        console.error('Error saving element position:', error);
        alert('Failed to save changes: ' + error.message);
    }
}

// Load validation results
async function loadValidation(clockTemplateId) {
    try {
        const token = localStorage.getItem('authToken');
        const response = await fetch(`/api/clocks/${clockTemplateId}/validate`, {
            method: 'POST',
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) return;
        
        const validation = await response.json();
        displayValidation(validation);
    } catch (error) {
        console.error('Error loading validation:', error);
    }
}

// Display validation results
function displayValidation(validation) {
    const panel = document.getElementById('validationPanel');
    if (!panel) return;
    
    panel.innerHTML = '<div class="validation-panel-title">Validation</div>';
    
    if (validation.errors && validation.errors.length > 0) {
        validation.errors.forEach(error => {
            const item = document.createElement('div');
            item.className = 'validation-item error';
            item.textContent = error;
            panel.appendChild(item);
        });
    }
    
    if (validation.warnings && validation.warnings.length > 0) {
        validation.warnings.forEach(warning => {
            const item = document.createElement('div');
            item.className = 'validation-item warning';
            item.textContent = warning;
            panel.appendChild(item);
        });
    }
    
    if ((!validation.errors || validation.errors.length === 0) &&
        (!validation.warnings || validation.warnings.length === 0)) {
        const item = document.createElement('div');
        item.className = 'validation-item info';
        item.textContent = 'No validation issues found';
        panel.appendChild(item);
    }
}

// Load revenue analysis
async function loadRevenueAnalysis(clockTemplateId) {
    try {
        const token = localStorage.getItem('authToken');
        const response = await fetch(`/api/clock-templates/${clockTemplateId}/revenue-analysis`, {
            headers: {
                'Authorization': `Bearer ${token}`
            }
        });
        
        if (!response.ok) return;
        
        const revenue = await response.json();
        displayRevenueAnalysis(revenue);
    } catch (error) {
        console.error('Error loading revenue analysis:', error);
    }
}

// Display revenue analysis
function displayRevenueAnalysis(revenue) {
    const panel = document.getElementById('revenuePanel');
    if (!panel) return;
    
    panel.innerHTML = `
        <div class="revenue-panel-title">Revenue Analysis</div>
        <div class="revenue-stat">
            <span class="revenue-stat-label">Total Inventory</span>
            <span class="revenue-stat-value">${revenue.totalInventoryMinutes?.toFixed(2) || 0} min</span>
        </div>
        <div class="revenue-stat">
            <span class="revenue-stat-label">Potential Revenue</span>
            <span class="revenue-stat-value">$${revenue.potentialRevenue?.toFixed(2) || '0.00'}</span>
        </div>
        <div class="revenue-stat">
            <span class="revenue-stat-label">Avg per Minute</span>
            <span class="revenue-stat-value">$${revenue.averageRevenuePerMinute?.toFixed(2) || '0.00'}</span>
        </div>
        <div class="revenue-stat">
            <span class="revenue-stat-label">Avg per Break</span>
            <span class="revenue-stat-value">$${revenue.averageRevenuePerBreak?.toFixed(2) || '0.00'}</span>
        </div>
    `;
}

// Utility functions
function formatTime(minutes) {
    const hours = Math.floor(minutes / 60);
    const mins = Math.floor(minutes % 60);
    return `${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}`;
}

function formatTimeFromString(timeString) {
    if (!timeString) return '00:00';
    // Extract HH:MM from HH:MM:SS format
    const parts = timeString.split(':');
    if (parts.length >= 2) {
        const hours = parseInt(parts[0]) || 0;
        const minutes = parseInt(parts[1]) || 0;
        // Ensure 24-hour format (00:00 to 00:59 for clock templates)
        const normalizedHours = hours % 24; // For clock templates, always use 00-23 range
        return `${normalizedHours.toString().padStart(2, '0')}:${minutes.toString().padStart(2, '0')}`;
    }
    return timeString.substring(0, 5); // Fallback: HH:MM
}

function formatTimeForInput(timeString) {
    const formatted = formatTimeFromString(timeString);
    // Force 24-hour format by ensuring hour is 00 for clock templates (00:00 to 00:59)
    const parts = formatted.split(':');
    if (parts.length === 2) {
        const hours = parseInt(parts[0]) || 0;
        const minutes = parseInt(parts[1]) || 0;
        // For clock templates, always use 00:MM format
        return `00:${minutes.toString().padStart(2, '0')}`;
    }
    return formatted;
}

// Validate and format 24-hour time input (00:00 to 00:59)
function validateTimeInput(inputElement) {
    if (!inputElement) return;
    
    let value = inputElement.value.trim();
    
    // Remove any non-digit characters except colon
    value = value.replace(/[^\d:]/g, '');
    
    // Auto-format as user types
    if (value.length > 0 && !value.includes(':')) {
        // If user types digits without colon, auto-insert colon
        if (value.length === 2) {
            value = value + ':';
        } else if (value.length > 2) {
            value = value.substring(0, 2) + ':' + value.substring(2);
        }
    }
    
    // Ensure format is HH:MM
    const parts = value.split(':');
    if (parts.length === 2) {
        let hours = parseInt(parts[0]) || 0;
        let minutes = parseInt(parts[1]) || 0;
        
        // Force hours to 00 for clock templates (00:00 to 00:59)
        if (hours !== 0) {
            hours = 0;
        }
        
        // Validate minutes (0-59)
        if (minutes < 0) minutes = 0;
        if (minutes > 59) minutes = 59;
        
        // Format as 00:MM
        const formattedValue = `00:${minutes.toString().padStart(2, '0')}`;
        
        // Update input if value changed
        if (inputElement.value !== formattedValue) {
            inputElement.value = formattedValue;
            inputElement.setCustomValidity(''); // Clear any previous validation errors
        }
    } else if (value.length > 0) {
        // Invalid format - show error
        inputElement.setCustomValidity('Time must be in format 00:MM (e.g., 00:15)');
    } else {
        inputElement.setCustomValidity(''); // Clear error if empty
    }
    
    // Check pattern match
    if (inputElement.value && !inputElement.value.match(/^00:[0-5][0-9]$/)) {
        inputElement.setCustomValidity('Time must be between 00:00 and 00:59');
    } else {
        inputElement.setCustomValidity('');
    }
}

// Setup time input handlers for custom 24-hour format inputs
function setupTimeInput(inputId) {
    const input = document.getElementById(inputId);
    if (!input) return;
    
    // Add event listeners
    input.addEventListener('input', function() {
        validateTimeInput(this);
    });
    
    input.addEventListener('blur', function() {
        validateTimeInput(this);
        // If empty, set to 00:00
        if (!this.value) {
            this.value = '00:00';
        }
    });
    
    input.addEventListener('keydown', function(e) {
        // Allow: backspace, delete, tab, escape, enter, colon
        if ([8, 9, 27, 13, 46, 186].indexOf(e.keyCode) !== -1 ||
            // Allow: Ctrl+A, Ctrl+C, Ctrl+V, Ctrl+X
            (e.keyCode === 65 && e.ctrlKey === true) ||
            (e.keyCode === 67 && e.ctrlKey === true) ||
            (e.keyCode === 86 && e.ctrlKey === true) ||
            (e.keyCode === 88 && e.ctrlKey === true) ||
            // Allow: home, end, left, right
            (e.keyCode >= 35 && e.keyCode <= 39)) {
            return;
        }
        // Ensure that it is a number and stop the keypress
        if ((e.shiftKey || (e.keyCode < 48 || e.keyCode > 57)) && (e.keyCode < 96 || e.keyCode > 105)) {
            e.preventDefault();
        }
    });
}

function timeToMinutes(timeString) {
    if (!timeString) return 0;
    const parts = timeString.split(':');
    const hours = parseInt(parts[0]) || 0;
    const minutes = parseInt(parts[1]) || 0;
    // For clock templates, we work within a 60-minute window
    // If hours > 0, we still only care about the minutes portion (0-59)
    return (hours * 60) + minutes;
}

function minutesToTime(minutes) {
    // For clock templates, we work within a 60-minute window (00:00 to 00:59)
    // Always use hour 00, and minutes should be 0-59
    const mins = Math.floor(minutes % 60);
    return `00:${mins.toString().padStart(2, '0')}:00`;
}

function formatDuration(seconds) {
    if (!seconds) return '';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
}

function updateDragTooltip(e, top) {
    // Create or update tooltip
    let tooltip = document.getElementById('dragTooltip');
    if (!tooltip) {
        tooltip = document.createElement('div');
        tooltip.id = 'dragTooltip';
        tooltip.className = 'element-tooltip';
        document.body.appendChild(tooltip);
    }
    
    const minutes = top / timelineScale;
    tooltip.textContent = formatTime(minutes);
    tooltip.style.left = `${(e.clientX || e.touches[0].clientX) + 10}px`;
    tooltip.style.top = `${(e.clientY || e.touches[0].clientY) - 30}px`;
    tooltip.style.display = 'block';
}

function removeDragTooltip() {
    const tooltip = document.getElementById('dragTooltip');
    if (tooltip) {
        tooltip.remove();
    }
}

function showClockBuilderEmpty() {
    const container = document.getElementById('clockBuilderContainer');
    if (container) {
        container.innerHTML = `
            <div class="clock-builder-empty">
                <div class="clock-builder-empty-icon">🕐</div>
                <div class="clock-builder-empty-text">No Clock Template Selected</div>
                <div class="clock-builder-empty-hint">Select a clock template from the table above to start building</div>
            </div>
        `;
    }
}

function showClockBuilderError(message) {
    const container = document.getElementById('clockBuilderContainer');
    if (container) {
        container.innerHTML = `
            <div class="clock-builder-empty">
                <div class="clock-builder-empty-icon">⚠️</div>
                <div class="clock-builder-empty-text">Error Loading Clock Template</div>
                <div class="clock-builder-empty-hint">${message}</div>
            </div>
        `;
    }
}

// Get next available start time based on existing elements
function getNextAvailableStartTime() {
    if (!clockElements || clockElements.length === 0) {
        return '00:00:00';
    }
    
    let latestEndTime = 0; // in minutes
    
    clockElements.forEach(element => {
        const startMinutes = timeToMinutes(element.startTime);
        let endMinutes = startMinutes;
        
        // For breaks, calculate end time (start + duration in minutes)
        if (element.type === 'break' && element.durationSeconds) {
            // Convert seconds to minutes (round up to nearest minute)
            const durationMinutes = Math.ceil(element.durationSeconds / 60);
            endMinutes = startMinutes + durationMinutes;
        }
        // For fixed assets and automation commands, they're point-in-time
        // Use the start time as the end time (next element can start at the same time or after)
        else {
            endMinutes = startMinutes;
        }
        
        if (endMinutes > latestEndTime) {
            latestEndTime = endMinutes;
        }
    });
    
    // Cap at 59:59 (60 minutes = 1 hour clock, max is 59:59)
    if (latestEndTime >= 60) {
        latestEndTime = 59;
    }
    
    return minutesToTime(latestEndTime);
}

// Add break button handler
function addBreakToClock() {
    if (!currentClockTemplate) {
        alert('Please select a clock template first');
        return;
    }
    const modal = document.getElementById('addBreakModal');
    const form = document.getElementById('addBreakForm');
    const errorMsg = document.getElementById('addBreakModalErrorMessage');
    
    if (errorMsg) errorMsg.classList.remove('show');
    form.reset();
    
    // Set default start time to next available time
    const nextStartTime = getNextAvailableStartTime();
    const timeInput = document.getElementById('breakStartTime');
    const normalizedTime = formatTimeForInput(nextStartTime);
    
    // Set value
    timeInput.value = normalizedTime;
    
    // Setup time input handler
    setupTimeInput('breakStartTime');
    validateTimeInput(timeInput);
    
    modal.classList.add('show');
}

function closeAddBreakModal() {
    document.getElementById('addBreakModal').classList.remove('show');
}

// Add fixed asset button handler
function addFixedAssetToClock() {
    if (!currentClockTemplate) {
        alert('Please select a clock template first');
        return;
    }
    const modal = document.getElementById('addFixedAssetModal');
    const form = document.getElementById('addFixedAssetForm');
    const errorMsg = document.getElementById('addFixedAssetModalErrorMessage');
    
    if (errorMsg) errorMsg.classList.remove('show');
    form.reset();
    
    // Set default start time to next available time
    const nextStartTime = getNextAvailableStartTime();
    const timeInput = document.getElementById('fixedAssetStartTime');
    timeInput.value = formatTimeForInput(nextStartTime);
    
    // Force normalization immediately
    setTimeout(() => {
        normalizeTimeInput(timeInput);
    }, 50);
    
    // Add event listeners to normalize on focus and change
    timeInput.addEventListener('focus', () => normalizeTimeInput(timeInput));
    timeInput.addEventListener('change', () => normalizeTimeInput(timeInput));
    timeInput.addEventListener('blur', () => normalizeTimeInput(timeInput));
    
    // Clear duration and content type for new asset
    document.getElementById('fixedAssetDuration').value = '';
    document.getElementById('fixedAssetContentType').value = '';
    
    modal.classList.add('show');
}

function closeAddFixedAssetModal() {
    document.getElementById('addFixedAssetModal').classList.remove('show');
}

// Add automation command button handler
function addAutomationCommandToClock() {
    if (!currentClockTemplate) {
        alert('Please select a clock template first');
        return;
    }
    const modal = document.getElementById('addAutomationCommandModal');
    const form = document.getElementById('addAutomationCommandForm');
    const errorMsg = document.getElementById('addAutomationCommandModalErrorMessage');
    
    if (errorMsg) errorMsg.classList.remove('show');
    form.reset();
    
    // Set default trigger time to next available time
    const nextStartTime = getNextAvailableStartTime();
    const timeInput = document.getElementById('commandTriggerTime');
    const normalizedTime = formatTimeForInput(nextStartTime);
    
    // Set value
    timeInput.value = normalizedTime;
    
    // Setup time input handler
    setupTimeInput('commandTriggerTime');
    validateTimeInput(timeInput);
    
    modal.classList.add('show');
}

function closeAddAutomationCommandModal() {
    document.getElementById('addAutomationCommandModal').classList.remove('show');
}

// Form submission handlers
document.addEventListener('DOMContentLoaded', function() {
    // Break form submission
    const breakForm = document.getElementById('addBreakForm');
    if (breakForm) {
        breakForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const errorMsg = document.getElementById('addBreakModalErrorMessage');
            if (errorMsg) errorMsg.classList.remove('show');
            
            if (!currentClockTemplate) {
                if (errorMsg) {
                    errorMsg.textContent = 'No clock template selected';
                    errorMsg.classList.add('show');
                }
                return;
            }
            
            try {
                // Convert time input (HH:MM) to HH:MM:SS format
                const startTimeInput = document.getElementById('breakStartTime').value;
                const startTime = startTimeInput ? `${startTimeInput}:00` : '00:00:00';
                
                const breakData = {
                    clockTemplateId: currentClockTemplate.id,
                    name: document.getElementById('breakName').value,
                    startTime: startTime,
                    durationSeconds: parseInt(document.getElementById('breakDuration').value),
                    isFloating: document.getElementById('breakIsFloating').checked,
                    timingType: document.getElementById('breakTimingType').value || null,
                    transitionCode: document.getElementById('breakTransitionCode').value || null,
                    assetType: document.getElementById('breakAssetType').value || null,
                    musicCategory: document.getElementById('breakMusicCategory').value || null,
                    showSegmentName: document.getElementById('breakShowSegmentName').value || null
                };
                
                const authToken = localStorage.getItem('authToken');
                const response = await fetch(`/api/clock-templates/${currentClockTemplate.id}/breaks`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${authToken}`
                    },
                    body: JSON.stringify(breakData)
                });
                
                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({ message: 'Failed to add break' }));
                    throw new Error(errorData.message || 'Failed to add break');
                }
                
                closeAddBreakModal();
                await loadClockStructure(currentClockTemplate.id);
                showClockBuilderSuccess('Break added successfully');
            } catch (error) {
                console.error('Error adding break:', error);
                if (errorMsg) {
                    errorMsg.textContent = error.message || 'Failed to add break';
                    errorMsg.classList.add('show');
                }
            }
        });
    }
    
    // Fixed asset form submission
    const fixedAssetForm = document.getElementById('addFixedAssetForm');
    if (fixedAssetForm) {
        fixedAssetForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const errorMsg = document.getElementById('addFixedAssetModalErrorMessage');
            if (errorMsg) errorMsg.classList.remove('show');
            
            if (!currentClockTemplate) {
                if (errorMsg) {
                    errorMsg.textContent = 'No clock template selected';
                    errorMsg.classList.add('show');
                }
                return;
            }
            
            try {
                // Convert time input (HH:MM) to HH:MM:SS format
                const startTimeInput = document.getElementById('fixedAssetStartTime').value.trim();
                // Ensure format is 00:MM, then add :00 for seconds
                const startTime = startTimeInput && startTimeInput.match(/^00:[0-5][0-9]$/) ? `${startTimeInput}:00` : '00:00:00';
                
                // Get duration (optional - empty string means null)
                const durationInput = document.getElementById('fixedAssetDuration').value;
                const durationSeconds = durationInput ? parseInt(durationInput) : null;
                
                const fixedAssetData = {
                    clockTemplateId: currentClockTemplate.id,
                    name: document.getElementById('fixedAssetName').value,
                    assetType: document.getElementById('fixedAssetType').value,
                    startTime: startTime,
                    durationSeconds: durationSeconds,
                    contentType: document.getElementById('fixedAssetContentType').value || null,
                    assetIdentifier: document.getElementById('fixedAssetIdentifier').value || null,
                    timingType: document.getElementById('fixedAssetTimingType').value || null,
                    musicCategory: document.getElementById('fixedAssetMusicCategory').value || null,
                    showSegmentName: document.getElementById('fixedAssetShowSegmentName').value || null
                };
                
                const authToken = localStorage.getItem('authToken');
                const response = await fetch(`/api/clock-templates/${currentClockTemplate.id}/fixed-assets`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${authToken}`
                    },
                    body: JSON.stringify(fixedAssetData)
                });
                
                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({ message: 'Failed to add fixed asset' }));
                    throw new Error(errorData.message || 'Failed to add fixed asset');
                }
                
                closeAddFixedAssetModal();
                await loadClockStructure(currentClockTemplate.id);
                showClockBuilderSuccess('Fixed asset added successfully');
            } catch (error) {
                console.error('Error adding fixed asset:', error);
                if (errorMsg) {
                    errorMsg.textContent = error.message || 'Failed to add fixed asset';
                    errorMsg.classList.add('show');
                }
            }
        });
    }
    
    // Automation command form submission
    const automationCommandForm = document.getElementById('addAutomationCommandForm');
    if (automationCommandForm) {
        automationCommandForm.addEventListener('submit', async (e) => {
            e.preventDefault();
            const errorMsg = document.getElementById('addAutomationCommandModalErrorMessage');
            if (errorMsg) errorMsg.classList.remove('show');
            
            if (!currentClockTemplate) {
                if (errorMsg) {
                    errorMsg.textContent = 'No clock template selected';
                    errorMsg.classList.add('show');
                }
                return;
            }
            
            try {
                let parameters = null;
                const paramsText = document.getElementById('commandParameters').value.trim();
                if (paramsText) {
                    try {
                        parameters = JSON.parse(paramsText);
                    } catch (parseError) {
                        throw new Error('Invalid JSON in parameters field');
                    }
                }
                
                // Convert time input (HH:MM) to HH:MM:SS format
                const triggerTimeInput = document.getElementById('commandTriggerTime').value.trim();
                // Ensure format is 00:MM, then add :00 for seconds
                const triggerTime = triggerTimeInput && triggerTimeInput.match(/^00:[0-5][0-9]$/) ? `${triggerTimeInput}:00` : '00:00:00';
                
                const commandData = {
                    clockTemplateId: currentClockTemplate.id,
                    commandType: document.getElementById('commandType').value,
                    triggerTime: triggerTime,
                    priority: document.getElementById('commandPriority').value || null,
                    parameters: parameters
                };
                
                const authToken = localStorage.getItem('authToken');
                const response = await fetch(`/api/clock-templates/${currentClockTemplate.id}/automation-commands`, {
                    method: 'POST',
                    headers: {
                        'Content-Type': 'application/json',
                        'Authorization': `Bearer ${authToken}`
                    },
                    body: JSON.stringify(commandData)
                });
                
                if (!response.ok) {
                    const errorData = await response.json().catch(() => ({ message: 'Failed to add automation command' }));
                    throw new Error(errorData.message || 'Failed to add automation command');
                }
                
                closeAddAutomationCommandModal();
                await loadClockStructure(currentClockTemplate.id);
                showClockBuilderSuccess('Automation command added successfully');
            } catch (error) {
                console.error('Error adding automation command:', error);
                if (errorMsg) {
                    errorMsg.textContent = error.message || 'Failed to add automation command';
                    errorMsg.classList.add('show');
                }
            }
        });
    }
});

// Helper function to show success message
function showClockBuilderSuccess(message) {
    // Try to use the dashboard's showSuccess function if available
    if (typeof showSuccess === 'function') {
        showSuccess(message);
    } else {
        // Fallback to alert
        alert(message);
    }
}

