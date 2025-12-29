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
let timelineScale = 60; // pixels per minute (1 minute = 60px)

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
        const token = localStorage.getItem('token');
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

    // Add time markers (every 5 minutes)
    for (let i = 0; i <= 60; i += 5) {
        const marker = document.createElement('div');
        marker.className = 'timeline-marker' + (i % 15 === 0 ? ' major' : '');
        marker.style.left = `${i * timelineScale}px`;
        const label = document.createElement('span');
        label.className = 'timeline-marker-label';
        label.textContent = formatTime(i);
        marker.appendChild(label);
        ruler.appendChild(marker);
    }

    wrapper.appendChild(ruler);

    // Create timeline track
    const track = document.createElement('div');
    track.className = 'timeline-track';
    track.id = 'timelineTrack';

    // Add snap grid lines (every 15 minutes)
    for (let i = 0; i <= 60; i += 15) {
        const gridLine = document.createElement('div');
        gridLine.className = 'snap-grid-line';
        gridLine.style.left = `${i * timelineScale}px`;
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

    // Calculate position and width
    const startMinutes = timeToMinutes(element.startTime);
    const durationMinutes = element.durationSeconds ? element.durationSeconds / 60 : 0.5; // Default 30 seconds for point elements
    const left = startMinutes * timelineScale;
    const width = durationMinutes * timelineScale;

    block.style.left = `${left}px`;
    block.style.width = `${Math.max(width, 30)}px`; // Minimum 30px width

    // Set content
    block.innerHTML = `
        <span class="element-name">${element.name}</span>
        ${element.type === 'break' ? `<span class="element-duration">${formatDuration(element.durationSeconds)}</span>` : ''}
        <div class="resize-handle left"></div>
        <div class="resize-handle right"></div>
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

        // Add resize handles
        const leftHandle = element.querySelector('.resize-handle.left');
        const rightHandle = element.querySelector('.resize-handle.right');

        if (leftHandle) {
            leftHandle.addEventListener('mousedown', (e) => {
                e.stopPropagation();
                handleResizeStart(e, element, 'left');
            });
        }

        if (rightHandle) {
            rightHandle.addEventListener('mousedown', (e) => {
                e.stopPropagation();
                handleResizeStart(e, element, 'right');
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
    dragOffset.y = (e.clientY || e.touches[0].clientY) - rect.top;

    selectedElement.classList.add('dragging');

    // Add move and end handlers
    document.addEventListener('mousemove', handleDrag);
    document.addEventListener('mouseup', handleDragEnd);
    document.addEventListener('touchmove', handleDrag, { passive: false });
    document.addEventListener('touchend', handleDragEnd);
}

// Handle drag
function handleDrag(e) {
    if (!isDragging || !selectedElement) return;

    e.preventDefault();
    const clientX = e.clientX || e.touches[0].clientX;
    const track = document.getElementById('timelineTrack');
    const trackRect = track.getBoundingClientRect();
    
    let newLeft = clientX - trackRect.left - dragOffset.x;
    newLeft = Math.max(0, Math.min(newLeft, 3600 - selectedElement.offsetWidth)); // Constrain to timeline
    
    // Snap to grid (every 15 minutes)
    const snapInterval = 15 * timelineScale;
    newLeft = Math.round(newLeft / snapInterval) * snapInterval;
    
    selectedElement.style.left = `${newLeft}px`;
    
    // Update tooltip
    updateDragTooltip(e, newLeft);
}

// Handle drag end
function handleDragEnd(e) {
    if (!isDragging || !selectedElement) return;

    e.preventDefault();
    isDragging = false;
    
    const elementId = selectedElement.getAttribute('data-element-id');
    const element = clockElements.find(el => el.id === elementId);
    
    if (element) {
        // Calculate new start time
        const newLeft = parseInt(selectedElement.style.left);
        const newMinutes = newLeft / timelineScale;
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

// Handle resize start
function handleResizeStart(e, element, direction) {
    e.preventDefault();
    e.stopPropagation();
    
    const startX = e.clientX || e.touches[0].clientX;
    const startLeft = parseInt(element.style.left);
    const startWidth = element.offsetWidth;
    
    const elementId = element.getAttribute('data-element-id');
    const clockElement = clockElements.find(el => el.id === elementId);
    
    if (!clockElement) return;
    
    const handleResize = (moveEvent) => {
        moveEvent.preventDefault();
        const currentX = moveEvent.clientX || moveEvent.touches[0].clientX;
        const deltaX = currentX - startX;
        
        if (direction === 'left') {
            const newLeft = Math.max(0, startLeft + deltaX);
            const newWidth = Math.max(30, startWidth - deltaX);
            element.style.left = `${newLeft}px`;
            element.style.width = `${newWidth}px`;
        } else {
            const newWidth = Math.max(30, startWidth + deltaX);
            const maxWidth = 3600 - parseInt(element.style.left);
            element.style.width = `${Math.min(newWidth, maxWidth)}px`;
        }
    };
    
    const handleResizeEnd = (endEvent) => {
        endEvent.preventDefault();
        const newLeft = parseInt(element.style.left);
        const newWidth = parseInt(element.style.width);
        const newMinutes = newLeft / timelineScale;
        const newDurationSeconds = (newWidth / timelineScale) * 60;
        
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
                <label>Start Time</label>
                <input type="time" id="propStartTime" value="${formatTimeForInput(element.startTime)}">
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
                <label>Start Time</label>
                <input type="time" id="propStartTime" value="${formatTimeForInput(element.startTime)}">
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
        const startTime = document.getElementById('propStartTime')?.value;
        
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
        const token = localStorage.getItem('token');
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
        const token = localStorage.getItem('token');
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
        const token = localStorage.getItem('token');
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
    return timeString.substring(0, 5); // HH:MM
}

function formatTimeForInput(timeString) {
    return formatTimeFromString(timeString);
}

function timeToMinutes(timeString) {
    if (!timeString) return 0;
    const parts = timeString.split(':');
    return parseInt(parts[0]) * 60 + parseInt(parts[1]);
}

function minutesToTime(minutes) {
    const hours = Math.floor(minutes / 60);
    const mins = Math.floor(minutes % 60);
    return `${hours.toString().padStart(2, '0')}:${mins.toString().padStart(2, '0')}:00`;
}

function formatDuration(seconds) {
    if (!seconds) return '';
    const mins = Math.floor(seconds / 60);
    const secs = seconds % 60;
    return `${mins}:${secs.toString().padStart(2, '0')}`;
}

function updateDragTooltip(e, left) {
    // Create or update tooltip
    let tooltip = document.getElementById('dragTooltip');
    if (!tooltip) {
        tooltip = document.createElement('div');
        tooltip.id = 'dragTooltip';
        tooltip.className = 'element-tooltip';
        document.body.appendChild(tooltip);
    }
    
    const minutes = left / timelineScale;
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

// Add break button handler
function addBreakToClock() {
    if (!currentClockTemplate) {
        alert('Please select a clock template first');
        return;
    }
    // Open break creation modal
    // This will be integrated with the existing break creation flow
    alert('Break creation will be integrated with the break management UI');
}

// Add fixed asset button handler
function addFixedAssetToClock() {
    if (!currentClockTemplate) {
        alert('Please select a clock template first');
        return;
    }
    alert('Fixed asset creation will be integrated with the asset management UI');
}

// Add automation command button handler
function addAutomationCommandToClock() {
    if (!currentClockTemplate) {
        alert('Please select a clock template first');
        return;
    }
    alert('Automation command creation will be integrated with the command management UI');
}

