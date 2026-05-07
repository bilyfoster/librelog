// LibreLog v2 - single-page client for the MVP
// Vanilla JS, no build step. Talks to the Spring Boot API on the same origin.

const state = {
    token: localStorage.getItem('librelog.token'),
    user: null,
    stations: [],
    currentStationId: localStorage.getItem('librelog.stationId'),
    currentTab: 'dayBuilder',
    day: null,
    showInstances: [],
    libraryFiles: [],
    spotPool: [],
    customers: [],
    orders: [],
    spotsByOrder: {},
    selectedOrderId: null,
};

const API = {
    async req(method, path, body) {
        const opts = { method, headers: { 'Accept': 'application/json' } };
        if (state.token) opts.headers['Authorization'] = 'Bearer ' + state.token;
        if (body !== undefined) {
            opts.headers['Content-Type'] = 'application/json';
            opts.body = JSON.stringify(body);
        }
        const res = await fetch(path, opts);
        if (res.status === 401) {
            logout();
            throw new Error('Unauthorized');
        }
        const text = await res.text();
        const data = text ? JSON.parse(text) : null;
        if (!res.ok) {
            const msg = data?.error || data?.message || `${res.status} ${res.statusText}`;
            throw new Error(msg);
        }
        return data;
    },
    get(p) { return this.req('GET', p); },
    post(p, b) { return this.req('POST', p, b); },
    put(p, b) { return this.req('PUT', p, b); },
    del(p) { return this.req('DELETE', p); },
};

// ---------- Auth ----------
async function login(email, password) {
    const r = await API.post('/api/auth/login', { email, password });
    state.token = r.token;
    state.user = r.user;
    localStorage.setItem('librelog.token', r.token);
    localStorage.setItem('librelog.user', JSON.stringify(r.user));
    await bootstrap();
}

function logout() {
    state.token = null;
    state.user = null;
    localStorage.removeItem('librelog.token');
    localStorage.removeItem('librelog.user');
    localStorage.removeItem('librelog.stationId');
    document.getElementById('app').classList.add('hidden');
    document.getElementById('loginScreen').classList.remove('hidden');
}

async function bootstrap() {
    document.getElementById('loginScreen').classList.add('hidden');
    document.getElementById('app').classList.remove('hidden');
    if (!state.user) {
        try { state.user = await API.get('/api/auth/me'); }
        catch { logout(); return; }
    }
    document.getElementById('currentUserLabel').textContent =
        (state.user.name || state.user.email) + ' (' + state.user.role + ')';
    document.querySelectorAll('.admin-only').forEach(el => {
        el.style.display = state.user.role === 'ADMIN' ? '' : 'none';
    });
    await loadStations();
    showTab(state.currentTab);
}

// ---------- Stations ----------
async function loadStations() {
    state.stations = await API.get('/api/stations');
    if (!state.currentStationId && state.stations.length) {
        state.currentStationId = state.stations[0].id;
        localStorage.setItem('librelog.stationId', state.currentStationId);
    }
    renderStationPicker();
    renderStations();
}

function renderStationPicker() {
    const el = document.getElementById('stationPicker');
    if (!state.stations.length) {
        el.innerHTML = '<span class="muted">No station yet. Create one in Setup &rarr; Station.</span>';
        return;
    }
    el.innerHTML = '<select id="stationSelect">' +
        state.stations.map(s => `<option value="${s.id}" ${s.id === state.currentStationId ? 'selected' : ''}>${escapeHtml(s.name)}</option>`).join('') +
        '</select>';
    document.getElementById('stationSelect').addEventListener('change', e => {
        state.currentStationId = e.target.value;
        localStorage.setItem('librelog.stationId', state.currentStationId);
        if (state.currentTab === 'dayBuilder' || state.currentTab === 'connection' || state.currentTab === 'library') {
            showTab(state.currentTab);
        }
    });
}

function renderStations() {
    const tbody = document.getElementById('stationsBody');
    tbody.innerHTML = state.stations.map(s => `
        <tr>
            <td>${escapeHtml(s.name)}</td>
            <td>${escapeHtml(s.callLetters || '')}</td>
            <td>${escapeHtml(s.timeZone)}</td>
            <td>
                <button class="link" data-station-edit="${s.id}">Edit</button>
                <button class="link" data-station-delete="${s.id}">Delete</button>
            </td>
        </tr>`).join('');
    tbody.querySelectorAll('[data-station-edit]').forEach(b => {
        b.addEventListener('click', () => stationModal(state.stations.find(s => s.id === b.dataset.stationEdit)));
    });
    tbody.querySelectorAll('[data-station-delete]').forEach(b => {
        b.addEventListener('click', async () => {
            if (!confirm('Delete station? This removes all related data.')) return;
            await API.del('/api/stations/' + b.dataset.stationDelete);
            if (state.currentStationId === b.dataset.stationDelete) {
                state.currentStationId = null;
                localStorage.removeItem('librelog.stationId');
            }
            await loadStations();
        });
    });
}

function stationModal(existing) {
    openModal(existing ? 'Edit station' : 'New station', `
        <label>Name <input name="name" required value="${escapeAttr(existing?.name)}" /></label>
        <label>Call letters <input name="callLetters" value="${escapeAttr(existing?.callLetters)}" /></label>
        <label>Time zone <input name="timeZone" value="${escapeAttr(existing?.timeZone || 'UTC')}" /></label>
    `, async (form) => {
        const body = formToJson(form);
        if (existing) await API.put('/api/stations/' + existing.id, body);
        else await API.post('/api/stations', body);
        await loadStations();
    });
}

// ---------- LibreTime Connection ----------
async function loadConnection() {
    if (!state.currentStationId) return;
    const c = await API.get(`/api/stations/${state.currentStationId}/librtime/connection`);
    document.getElementById('connBaseUrl').value = c.baseUrl || '';
    document.getElementById('connApiKey').value = '';
    const status = document.getElementById('connStatus');
    if (!c.configured) status.innerHTML = '<span class="muted">Not configured.</span>';
    else if (c.lastTestedAt) {
        const cls = c.lastTestOk ? 'status-ok' : 'status-bad';
        status.innerHTML = `Last test: <span class="${cls}">${escapeHtml(c.lastTestMessage || (c.lastTestOk ? 'OK' : 'Failed'))}</span> at ${new Date(c.lastTestedAt).toLocaleString()}`;
    } else status.innerHTML = '<span class="muted">Configured but not yet tested.</span>';
}

async function saveConnection(e) {
    e.preventDefault();
    if (!state.currentStationId) { alert('Pick or create a station first.'); return; }
    const body = {
        baseUrl: document.getElementById('connBaseUrl').value,
        apiKey: document.getElementById('connApiKey').value || null,
    };
    await API.put(`/api/stations/${state.currentStationId}/librtime/connection`, body);
    document.getElementById('connStatus').innerHTML = '<span class="status-ok">Saved.</span>';
    document.getElementById('connApiKey').value = '';
}

async function testConnection() {
    if (!state.currentStationId) return;
    const r = await API.post(`/api/stations/${state.currentStationId}/librtime/connection/test`, {});
    const cls = r.ok ? 'status-ok' : 'status-bad';
    document.getElementById('connStatus').innerHTML =
        `<span class="${cls}">${r.ok ? 'OK' : 'Failed'}: ${escapeHtml(r.message || '')}</span>`;
}

// ---------- Users ----------
async function loadUsers() {
    if (state.user.role !== 'ADMIN') return;
    const list = await API.get('/api/users');
    const tbody = document.getElementById('usersBody');
    tbody.innerHTML = list.map(u => `
        <tr>
            <td>${escapeHtml(u.email)}</td>
            <td>${escapeHtml(u.name || '')}</td>
            <td>${u.role}</td>
            <td>${u.active ? 'Yes' : 'No'}</td>
            <td>
                <button class="link" data-user-edit="${u.id}">Edit</button>
                ${u.id === state.user.id ? '' : `<button class="link" data-user-delete="${u.id}">Delete</button>`}
            </td>
        </tr>`).join('');
    tbody.querySelectorAll('[data-user-edit]').forEach(b => {
        b.addEventListener('click', () => userModal(list.find(u => u.id === b.dataset.userEdit)));
    });
    tbody.querySelectorAll('[data-user-delete]').forEach(b => {
        b.addEventListener('click', async () => {
            if (!confirm('Delete user?')) return;
            await API.del('/api/users/' + b.dataset.userDelete);
            await loadUsers();
        });
    });
}

function userModal(existing) {
    openModal(existing ? 'Edit user' : 'New user', `
        ${existing ? '' : '<label>Email <input name="email" type="email" required /></label>'}
        <label>Name <input name="name" value="${escapeAttr(existing?.name)}" /></label>
        <label>Role
            <select name="role">
                <option value="EDITOR" ${existing?.role === 'EDITOR' ? 'selected' : ''}>Editor</option>
                <option value="ADMIN" ${existing?.role === 'ADMIN' ? 'selected' : ''}>Admin</option>
            </select>
        </label>
        ${existing ? `<label>Active <select name="active"><option value="true" ${existing.active ? 'selected' : ''}>Yes</option><option value="false" ${!existing.active ? 'selected' : ''}>No</option></select></label>` : ''}
        <label>${existing ? 'New password (blank = unchanged)' : 'Password'} <input name="password" type="password" minlength="${existing ? 0 : 8}" ${existing ? '' : 'required'} /></label>
    `, async (form) => {
        const body = formToJson(form);
        if (body.active !== undefined) body.active = body.active === 'true';
        if (existing) {
            if (!body.password) delete body.password;
            await API.put('/api/users/' + existing.id, body);
        } else {
            await API.post('/api/users', body);
        }
        await loadUsers();
    });
}

// ---------- Library tab ----------
async function loadLibraryTab() {
    if (!state.currentStationId) return;
    document.getElementById('libFiles').innerHTML = 'Loading...';
    try {
        const files = await API.get(`/api/stations/${state.currentStationId}/librtime/library?limit=50`);
        document.getElementById('libFiles').innerHTML = renderFilesTable(files);
    } catch (e) {
        document.getElementById('libFiles').innerHTML = `<span class="error">${escapeHtml(e.message)}</span>`;
    }
    try {
        const today = new Date().toISOString().slice(0, 10);
        const inst = await API.get(`/api/stations/${state.currentStationId}/librtime/show-instances?date=${today}`);
        document.getElementById('libShowInstances').innerHTML = renderInstancesList(inst);
    } catch (e) {
        document.getElementById('libShowInstances').innerHTML = `<span class="error">${escapeHtml(e.message)}</span>`;
    }
    try {
        const t = await API.get(`/api/stations/${state.currentStationId}/librtime/templates`);
        document.getElementById('libTemplates').innerHTML =
            '<h4>Smart blocks</h4>' + renderNamedList(t.smartBlocks) +
            '<h4>Playlists</h4>' + renderNamedList(t.playlists);
    } catch (e) {
        document.getElementById('libTemplates').innerHTML = `<span class="error">${escapeHtml(e.message)}</span>`;
    }
}

async function librarySearch() {
    const q = document.getElementById('libSearch').value;
    const files = await API.get(`/api/stations/${state.currentStationId}/librtime/library?q=${encodeURIComponent(q)}&limit=50`);
    document.getElementById('libFiles').innerHTML = renderFilesTable(files);
}

function renderFilesTable(files) {
    if (!files?.length) return '<p class="muted">No files.</p>';
    return '<table class="data-table"><thead><tr><th>Title</th><th>Artist</th><th>Length</th></tr></thead><tbody>' +
        files.map(f => `<tr>
            <td>${escapeHtml(f.track_title || f.filepath || '(untitled)')}</td>
            <td>${escapeHtml(f.artist_name || '')}</td>
            <td>${escapeHtml(f.length || '')}</td>
        </tr>`).join('') + '</tbody></table>';
}

function renderInstancesList(items) {
    if (!items?.length) return '<p class="muted">No show instances for today.</p>';
    return '<ul>' + items.map(i =>
        `<li>${escapeHtml(i.show_name || i.name || '')} <span class="muted">${escapeHtml(i.starts || '')} &mdash; ${escapeHtml(i.ends || '')}</span></li>`
    ).join('') + '</ul>';
}

function renderNamedList(items) {
    if (!items?.length) return '<p class="muted">None.</p>';
    return '<ul>' + items.map(i => `<li>${escapeHtml(i.name || ('#' + i.id))}</li>`).join('') + '</ul>';
}

// ---------- Customers ----------
async function loadCustomers() {
    if (!state.currentStationId) return;
    state.customers = await API.get('/api/customers?stationId=' + state.currentStationId);
    const tbody = document.getElementById('customersBody');
    tbody.innerHTML = state.customers.map(c => `
        <tr>
            <td>${escapeHtml(c.name)}</td>
            <td>${escapeHtml(c.contact || '')}</td>
            <td>${escapeHtml(c.notes || '')}</td>
            <td>
                <button class="link" data-cust-edit="${c.id}">Edit</button>
                <button class="link" data-cust-delete="${c.id}">Delete</button>
            </td>
        </tr>`).join('');
    tbody.querySelectorAll('[data-cust-edit]').forEach(b => {
        b.addEventListener('click', () => customerModal(state.customers.find(c => c.id === b.dataset.custEdit)));
    });
    tbody.querySelectorAll('[data-cust-delete]').forEach(b => {
        b.addEventListener('click', async () => {
            if (!confirm('Delete customer? Their orders go too.')) return;
            await API.del('/api/customers/' + b.dataset.custDelete);
            await loadCustomers();
        });
    });
}

function customerModal(existing) {
    openModal(existing ? 'Edit customer' : 'New customer', `
        <label>Name <input name="name" required value="${escapeAttr(existing?.name)}" /></label>
        <label>Contact <input name="contact" value="${escapeAttr(existing?.contact)}" /></label>
        <label>Notes <textarea name="notes">${escapeHtml(existing?.notes || '')}</textarea></label>
    `, async (form) => {
        const body = formToJson(form);
        body.stationId = state.currentStationId;
        if (existing) await API.put('/api/customers/' + existing.id, body);
        else await API.post('/api/customers', body);
        await loadCustomers();
    });
}

// ---------- Orders + Spots ----------
async function loadOrders() {
    if (!state.currentStationId) return;
    state.orders = await API.get('/api/orders?stationId=' + state.currentStationId);
    if (!state.customers.length) state.customers = await API.get('/api/customers?stationId=' + state.currentStationId);
    const customerName = id => state.customers.find(c => c.id === id)?.name || '(unknown)';
    const tbody = document.getElementById('ordersBody');
    tbody.innerHTML = state.orders.map(o => `
        <tr>
            <td><button class="link" data-order-open="${o.id}">${escapeHtml(o.name)}</button></td>
            <td>${escapeHtml(customerName(o.customerId))}</td>
            <td>${o.startDate} &rarr; ${o.endDate}</td>
            <td>${o.spotCount} / ${o.totalSpots}</td>
            <td>
                <button class="link" data-order-edit="${o.id}">Edit</button>
                <button class="link" data-order-delete="${o.id}">Delete</button>
            </td>
        </tr>`).join('');
    tbody.querySelectorAll('[data-order-open]').forEach(b => {
        b.addEventListener('click', () => openOrderDetail(b.dataset.orderOpen));
    });
    tbody.querySelectorAll('[data-order-edit]').forEach(b => {
        b.addEventListener('click', () => orderModal(state.orders.find(o => o.id === b.dataset.orderEdit)));
    });
    tbody.querySelectorAll('[data-order-delete]').forEach(b => {
        b.addEventListener('click', async () => {
            if (!confirm('Delete order? Spots go too.')) return;
            await API.del('/api/orders/' + b.dataset.orderDelete);
            await loadOrders();
        });
    });
}

function orderModal(existing) {
    const opts = state.customers.map(c =>
        `<option value="${c.id}" ${existing?.customerId === c.id ? 'selected' : ''}>${escapeHtml(c.name)}</option>`).join('');
    openModal(existing ? 'Edit order' : 'New order', `
        <label>Customer <select name="customerId" required>${opts}</select></label>
        <label>Name <input name="name" required value="${escapeAttr(existing?.name)}" /></label>
        <label>Start date <input name="startDate" type="date" required value="${existing?.startDate || ''}" /></label>
        <label>End date <input name="endDate" type="date" required value="${existing?.endDate || ''}" /></label>
        <label>Total spots <input name="totalSpots" type="number" min="1" required value="${existing?.totalSpots || 1}" /></label>
        <label>Notes <textarea name="notes">${escapeHtml(existing?.notes || '')}</textarea></label>
    `, async (form) => {
        const body = formToJson(form);
        body.stationId = state.currentStationId;
        body.totalSpots = parseInt(body.totalSpots);
        if (existing) await API.put('/api/orders/' + existing.id, body);
        else await API.post('/api/orders', body);
        await loadOrders();
    });
}

async function openOrderDetail(orderId) {
    state.selectedOrderId = orderId;
    const order = state.orders.find(o => o.id === orderId);
    document.getElementById('orderDetail').classList.remove('hidden');
    document.getElementById('orderDetailTitle').textContent = 'Spots for: ' + order.name;
    await loadSpotsForOrder(orderId);
}

async function loadSpotsForOrder(orderId) {
    const list = await API.get('/api/orders/' + orderId + '/spots');
    state.spotsByOrder[orderId] = list;
    const tbody = document.getElementById('spotsBody');
    tbody.innerHTML = list.map(s => `
        <tr>
            <td>${escapeHtml(s.label)}</td>
            <td>${s.lengthSeconds}s</td>
            <td>${s.librtimeFileId ?? '<span class="muted">to-produce</span>'}</td>
            <td>${s.rotationKind}${s.targetShowId ? ' (#' + s.targetShowId + ')' : ''}</td>
            <td>
                <button class="link" data-spot-edit="${s.id}">Edit</button>
                <button class="link" data-spot-delete="${s.id}">Delete</button>
            </td>
        </tr>`).join('');
    tbody.querySelectorAll('[data-spot-edit]').forEach(b => {
        b.addEventListener('click', () => spotModal(orderId, list.find(s => s.id === b.dataset.spotEdit)));
    });
    tbody.querySelectorAll('[data-spot-delete]').forEach(b => {
        b.addEventListener('click', async () => {
            if (!confirm('Delete spot?')) return;
            await API.del('/api/spots/' + b.dataset.spotDelete);
            await loadSpotsForOrder(orderId);
        });
    });
}

function spotModal(orderId, existing) {
    openModal(existing ? 'Edit spot' : 'New spot', `
        <label>Label <input name="label" required value="${escapeAttr(existing?.label)}" /></label>
        <label>Length (seconds) <input name="lengthSeconds" type="number" min="1" required value="${existing?.lengthSeconds || 30}" /></label>
        <label>LibreTime file id (blank = to-produce) <input name="librtimeFileId" type="number" value="${existing?.librtimeFileId ?? ''}" /></label>
        <label>Rotation
            <select name="rotationKind">
                <option value="ANY_TIME" ${existing?.rotationKind === 'ANY_TIME' ? 'selected' : ''}>Any time</option>
                <option value="SPECIFIC_SHOW" ${existing?.rotationKind === 'SPECIFIC_SHOW' ? 'selected' : ''}>Specific show</option>
            </select>
        </label>
        <label>Target show id (when specific) <input name="targetShowId" type="number" value="${existing?.targetShowId ?? ''}" /></label>
    `, async (form) => {
        const body = formToJson(form);
        body.lengthSeconds = parseInt(body.lengthSeconds);
        body.librtimeFileId = body.librtimeFileId === '' ? null : parseInt(body.librtimeFileId);
        body.targetShowId = body.targetShowId === '' ? null : parseInt(body.targetShowId);
        if (existing) await API.put('/api/spots/' + existing.id, body);
        else await API.post('/api/orders/' + orderId + '/spots', body);
        await loadSpotsForOrder(orderId);
    });
}

// ---------- Reconciliation ----------
async function viewReconciliation(orderId) {
    if (!orderId) return;
    showTab('reconciliation');
    const r = await API.get('/api/orders/' + orderId + '/reconciliation');
    document.getElementById('reconciliationDetail').innerHTML = `
        <h3>${escapeHtml(r.orderName)}</h3>
        <p>Scheduled: ${r.scheduled} &nbsp; Matched: ${r.matched} &nbsp; Missed: ${r.missed}</p>
        <table class="data-table">
            <thead><tr><th>Spot</th><th>Scheduled at</th><th>Status</th></tr></thead>
            <tbody>${r.rows.map(row => `<tr>
                <td>${escapeHtml(row.spotLabel || '')}</td>
                <td>${row.scheduledAt ? new Date(row.scheduledAt).toLocaleString() : ''}</td>
                <td>${row.status}</td>
            </tr>`).join('')}</tbody>
        </table>
    `;
}

// ---------- Day Builder ----------
async function loadDayBuilder() {
    if (!state.currentStationId) {
        document.getElementById('dayBuilderShows').innerHTML = '<span class="muted">Create or pick a station first.</span>';
        return;
    }
    const dateInput = document.getElementById('dayBuilderDate');
    if (!dateInput.value) dateInput.value = new Date().toISOString().slice(0, 10);
}

async function loadDay() {
    if (!state.currentStationId) return;
    const date = document.getElementById('dayBuilderDate').value;
    state.day = await API.get(`/api/stations/${state.currentStationId}/days/${date}`);
    try {
        state.showInstances = await API.get(`/api/stations/${state.currentStationId}/librtime/show-instances?date=${date}`);
    } catch (e) {
        state.showInstances = [];
        document.getElementById('dayBuilderShows').innerHTML = `<span class="error">LibreTime: ${escapeHtml(e.message)}</span>`;
    }
    await loadActiveSpotPool(date);
    renderDay();
}

async function loadActiveSpotPool(date) {
    if (!state.orders.length) state.orders = await API.get('/api/orders?stationId=' + state.currentStationId);
    const active = state.orders.filter(o => o.startDate <= date && o.endDate >= date);
    const pool = [];
    for (const o of active) {
        if (!state.spotsByOrder[o.id]) state.spotsByOrder[o.id] = await API.get('/api/orders/' + o.id + '/spots');
        for (const s of state.spotsByOrder[o.id]) pool.push({ order: o, spot: s });
    }
    state.spotPool = pool;
}

function renderDay() {
    const d = state.day;
    document.getElementById('dayBuilderStatus').textContent = d.status;
    document.getElementById('dayBuilderStatus').className = 'badge ' + d.status.toLowerCase();

    // Lock badge
    const lockBadge = document.getElementById('dayBuilderLockBadge');
    if (d.lock) {
        if (d.lock.self) {
            lockBadge.textContent = 'Locked by you (until ' + new Date(d.lock.expiresAt).toLocaleTimeString() + ')';
            lockBadge.className = 'badge locked-self';
        } else {
            lockBadge.textContent = 'Locked by ' + (d.lock.userName || d.lock.userId);
            lockBadge.className = 'badge locked-other';
        }
    } else {
        lockBadge.textContent = 'Unlocked';
        lockBadge.className = 'badge unlocked';
    }

    const isAdmin = state.user.role === 'ADMIN';
    const haveLock = d.lock && d.lock.self;
    const editable = !d.readOnly && d.status !== 'PUSHED';
    document.getElementById('dayBuilderLockBtn').disabled = !editable || haveLock;
    document.getElementById('dayBuilderSaveBtn').disabled = !haveLock || !editable;
    document.getElementById('dayBuilderPushBtn').disabled = !haveLock || !editable;
    document.getElementById('dayBuilderReopenBtn').disabled = !isAdmin || d.status !== 'PUSHED';
    document.getElementById('dayBuilderForceUnlockBtn').disabled = !isAdmin || !d.lock;

    // Shows
    document.getElementById('dayBuilderShows').innerHTML = state.showInstances.length
        ? state.showInstances.map(s => `
            <div class="show-block" data-show-id="${s.id || s.instance_id || ''}">
                <div class="show-name">${escapeHtml(s.show_name || s.name || '(unnamed show)')}</div>
                <div class="show-time">${escapeHtml(s.starts || '')} &rarr; ${escapeHtml(s.ends || '')}</div>
                <button class="link" data-add-slots-for="${s.id || s.instance_id || ''}">Add 4 placeholder slots</button>
            </div>`).join('')
        : '<p class="muted">No LibreTime show instances for this date.</p>';

    document.querySelectorAll('[data-add-slots-for]').forEach(b => {
        b.addEventListener('click', () => addPlaceholderSlots(parseInt(b.dataset.addSlotsFor)));
    });

    // Slots
    const slots = (d.items || []).slice().sort((a, b) => a.position - b.position);
    document.getElementById('dayBuilderSlots').innerHTML = slots.length
        ? slots.map((it, idx) => slotHtml(it, idx)).join('')
        : '<p class="muted">No slots yet. Pick a show on the left to add slots.</p>';

    document.querySelectorAll('[data-fill-spot]').forEach(b => {
        b.addEventListener('click', () => fillSlotFromSpot(parseInt(b.dataset.slotIdx)));
    });
    document.querySelectorAll('[data-fill-track]').forEach(b => {
        b.addEventListener('click', () => fillSlotFromTrack(parseInt(b.dataset.slotIdx)));
    });
    document.querySelectorAll('[data-remove-slot]').forEach(b => {
        b.addEventListener('click', () => removeSlot(parseInt(b.dataset.removeSlot)));
    });

    // Spot pool
    document.getElementById('dayBuilderSpotPool').innerHTML = state.spotPool.length
        ? state.spotPool.map(p => `
            <div class="slot spot">
                <div><strong>${escapeHtml(p.spot.label)}</strong> (${p.spot.lengthSeconds}s)</div>
                <div class="slot-meta">${escapeHtml(p.order.name)} ${p.spot.librtimeFileId ?? '(to-produce)'}</div>
            </div>`).join('')
        : '<p class="muted">No active orders for this date.</p>';
}

function slotHtml(it, idx) {
    const k = (it.kind || 'PLACEHOLDER').toLowerCase();
    return `<div class="slot ${k}">
        <div><strong>#${idx + 1}</strong> ${escapeHtml(it.kind)} &middot; show ${it.showInstanceId ?? '?'} slot ${it.slotIndex}</div>
        <div class="slot-meta">${it.spotId ? 'spot ' + it.spotId.slice(0, 8) : ''} ${it.librtimeFileId ? 'file ' + it.librtimeFileId : ''} ${it.lengthSeconds ? '(' + it.lengthSeconds + 's)' : ''}</div>
        <button class="link" data-fill-spot="${idx}">Use spot</button>
        <button class="link" data-fill-track="${idx}">Use track</button>
        <button class="link" data-remove-slot="${idx}">Remove</button>
    </div>`;
}

function addPlaceholderSlots(showInstanceId) {
    if (!state.day) return;
    const items = (state.day.items || []).slice();
    for (let i = 0; i < 4; i++) {
        items.push({
            showInstanceId,
            slotIndex: i,
            kind: 'PLACEHOLDER',
            spotId: null,
            librtimeFileId: null,
            scheduledAt: null,
            lengthSeconds: 30,
            position: items.length,
        });
    }
    state.day.items = items;
    renderDay();
}

function fillSlotFromSpot(idx) {
    if (!state.spotPool.length) { alert('No active spots in pool.'); return; }
    const choices = state.spotPool.map((p, i) => `${i}: ${p.spot.label} (${p.order.name})`).join('\n');
    const pick = prompt('Pick spot index:\n' + choices, '0');
    if (pick === null) return;
    const p = state.spotPool[parseInt(pick)];
    if (!p) return;
    state.day.items[idx].kind = 'SPOT';
    state.day.items[idx].spotId = p.spot.id;
    state.day.items[idx].librtimeFileId = p.spot.librtimeFileId;
    state.day.items[idx].lengthSeconds = p.spot.lengthSeconds;
    renderDay();
}

async function fillSlotFromTrack(idx) {
    const q = prompt('Search library for track:');
    if (!q) return;
    const files = await API.get(`/api/stations/${state.currentStationId}/librtime/library?q=${encodeURIComponent(q)}&limit=10`);
    if (!files.length) { alert('No matches.'); return; }
    const choices = files.map((f, i) => `${i}: ${f.track_title || f.filepath} - ${f.artist_name || ''}`).join('\n');
    const pick = prompt('Pick track index:\n' + choices, '0');
    if (pick === null) return;
    const f = files[parseInt(pick)];
    if (!f) return;
    state.day.items[idx].kind = 'TRACK';
    state.day.items[idx].spotId = null;
    state.day.items[idx].librtimeFileId = f.id;
    state.day.items[idx].lengthSeconds = f.cuein_seconds || 180;
    renderDay();
}

function removeSlot(idx) {
    state.day.items.splice(idx, 1);
    state.day.items.forEach((it, i) => it.position = i);
    renderDay();
}

async function acquireDayLock() {
    try { await API.post(`/api/days/${state.day.id}/lock`, {}); }
    catch (e) { alert(e.message); return; }
    await loadDay();
}

async function saveDay() {
    try {
        const r = await API.put(`/api/days/${state.day.id}`, {
            expectedVersion: state.day.version,
            items: state.day.items.map(it => ({
                showInstanceId: it.showInstanceId,
                slotIndex: it.slotIndex,
                kind: it.kind,
                spotId: it.spotId,
                librtimeFileId: it.librtimeFileId,
                scheduledAt: it.scheduledAt,
                lengthSeconds: it.lengthSeconds,
            })),
        });
        state.day = r;
        renderDay();
    } catch (e) { alert(e.message); }
}

async function pushDay() {
    if (!confirm('Push this day to LibreTime? Day becomes read-only after push.')) return;
    try {
        await API.post(`/api/days/${state.day.id}/push`, {});
        await loadDay();
    } catch (e) { alert(e.message); }
}

async function reopenDay() {
    if (!confirm('Reopen this day for editing?')) return;
    await API.post(`/api/days/${state.day.id}/reopen`, {});
    await loadDay();
}

async function forceUnlockDay() {
    if (!confirm('Force-unlock this day? The other user will lose unsaved changes.')) return;
    await API.del(`/api/days/${state.day.id}/lock?force=true`);
    await loadDay();
}

// ---------- Tabs ----------
const tabLoaders = {
    dayBuilder: { title: 'Day Builder', load: loadDayBuilder },
    library: { title: 'LibreTime Library', load: loadLibraryTab },
    customers: { title: 'Customers', load: loadCustomers },
    orders: { title: 'Orders & Spots', load: loadOrders },
    reconciliation: { title: 'Reconciliation', load: () => {} },
    stations: { title: 'Station', load: () => {} },
    connection: { title: 'LibreTime Connection', load: loadConnection },
    users: { title: 'Users', load: loadUsers },
};

function showTab(name) {
    state.currentTab = name;
    document.querySelectorAll('.tab').forEach(t => t.classList.add('hidden'));
    const el = document.getElementById(name + 'Tab');
    if (el) el.classList.remove('hidden');
    document.querySelectorAll('.nav-item').forEach(b => b.classList.toggle('active', b.dataset.tab === name));
    document.getElementById('pageTitle').textContent = tabLoaders[name]?.title || name;
    tabLoaders[name]?.load();
}

// ---------- Modal helper ----------
function openModal(title, html, onSave) {
    const dlg = document.getElementById('modal');
    document.getElementById('modalTitle').textContent = title;
    document.getElementById('modalBody').innerHTML = html;
    const form = document.getElementById('modalForm');
    const handler = async (e) => {
        e.preventDefault();
        try {
            await onSave(form);
            dlg.close();
        } catch (err) { alert(err.message); }
    };
    form.onsubmit = handler;
    document.getElementById('modalCancel').onclick = () => dlg.close();
    dlg.showModal();
}

// ---------- Helpers ----------
function escapeHtml(s) {
    if (s == null) return '';
    return String(s).replace(/[&<>"']/g, c =>
        ({ '&': '&amp;', '<': '&lt;', '>': '&gt;', '"': '&quot;', "'": '&#39;' }[c]));
}
function escapeAttr(s) { return escapeHtml(s); }
function formToJson(form) {
    const out = {};
    new FormData(form).forEach((v, k) => { out[k] = typeof v === 'string' ? v.trim() : v; });
    return out;
}

// ---------- Wire up ----------
window.addEventListener('DOMContentLoaded', () => {
    document.getElementById('loginForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        document.getElementById('loginError').textContent = '';
        try {
            await login(document.getElementById('loginEmail').value,
                       document.getElementById('loginPassword').value);
        } catch (err) {
            document.getElementById('loginError').textContent = err.message;
        }
    });
    document.getElementById('logoutButton').addEventListener('click', logout);
    document.querySelectorAll('.nav-item').forEach(b => b.addEventListener('click', () => showTab(b.dataset.tab)));
    document.getElementById('stationNewBtn').addEventListener('click', () => stationModal(null));
    document.getElementById('userNewBtn').addEventListener('click', () => userModal(null));
    document.getElementById('connectionForm').addEventListener('submit', saveConnection);
    document.getElementById('connTestBtn').addEventListener('click', testConnection);
    document.getElementById('libSearchBtn').addEventListener('click', librarySearch);
    document.getElementById('customerNewBtn').addEventListener('click', () => customerModal(null));
    document.getElementById('orderNewBtn').addEventListener('click', () => orderModal(null));
    document.getElementById('spotNewBtn').addEventListener('click', () => spotModal(state.selectedOrderId, null));
    document.getElementById('orderReconcileBtn').addEventListener('click', () => viewReconciliation(state.selectedOrderId));
    document.getElementById('dayBuilderLoad').addEventListener('click', loadDay);
    document.getElementById('dayBuilderLockBtn').addEventListener('click', acquireDayLock);
    document.getElementById('dayBuilderSaveBtn').addEventListener('click', saveDay);
    document.getElementById('dayBuilderPushBtn').addEventListener('click', pushDay);
    document.getElementById('dayBuilderReopenBtn').addEventListener('click', reopenDay);
    document.getElementById('dayBuilderForceUnlockBtn').addEventListener('click', forceUnlockDay);

    if (state.token) {
        const cached = localStorage.getItem('librelog.user');
        if (cached) state.user = JSON.parse(cached);
        bootstrap().catch(() => logout());
    }
});
