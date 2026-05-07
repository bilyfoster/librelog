// LibreLog v2 - single-page client
// Vanilla JS, no build step. Talks to the Spring Boot API on the same origin.

const state = {
    token: localStorage.getItem('librelog.token'),
    user: null,
    stations: [],
    currentStationId: localStorage.getItem('librelog.stationId'),
    currentTab: 'dayBuilder',
    day: null,
    showInstances: [],
    spotPool: [],
    customers: [],
    orders: [],
    spotsByOrder: {},
    selectedOrderId: null,
};

// ---------- HTTP ----------
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
            throw new Error('Session expired - please sign in again');
        }
        const text = await res.text();
        const data = text ? JSON.parse(text) : null;
        if (!res.ok) {
            const msg = data?.error || data?.message || `${res.status} ${res.statusText}`;
            const err = new Error(msg);
            err.status = res.status;
            throw err;
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
    if (state.currentStationId && !state.stations.some(s => s.id === state.currentStationId)) {
        state.currentStationId = null;
    }
    if (!state.currentStationId && state.stations.length) {
        state.currentStationId = state.stations[0].id;
    }
    if (state.currentStationId) localStorage.setItem('librelog.stationId', state.currentStationId);
    else localStorage.removeItem('librelog.stationId');
    renderStationPicker();
    renderStations();
    renderGlobalBanner();
}

function renderGlobalBanner() {
    const b = document.getElementById('globalBanner');
    if (!state.stations.length) {
        b.className = 'banner warn';
        b.innerHTML = `<strong>Get started:</strong> create a station and connect it to LibreTime. <button class="link" id="bannerCreateStation">Create station</button>`;
        b.classList.remove('hidden');
        document.getElementById('bannerCreateStation').addEventListener('click', () => stationModal(null));
    } else {
        b.classList.add('hidden');
    }
}

function renderStationPicker() {
    const el = document.getElementById('stationPicker');
    if (!state.stations.length) {
        el.innerHTML = '';
        return;
    }
    el.innerHTML = '<select id="stationSelect">' +
        state.stations.map(s => `<option value="${s.id}" ${s.id === state.currentStationId ? 'selected' : ''}>${escapeHtml(s.name)}</option>`).join('') +
        '</select>';
    document.getElementById('stationSelect').addEventListener('change', e => {
        state.currentStationId = e.target.value;
        localStorage.setItem('librelog.stationId', state.currentStationId);
        // refresh all data scoped to a station
        state.day = null;
        state.spotsByOrder = {};
        showTab(state.currentTab);
    });
}

function renderStations() {
    const tbody = document.getElementById('stationsBody');
    if (!state.stations.length) {
        tbody.innerHTML = '<tr><td colspan="4"><div class="empty-state"><strong>No stations yet</strong>Click "New station" to add the first one.</div></td></tr>';
        return;
    }
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
    tbody.querySelectorAll('[data-station-edit]').forEach(b =>
        b.addEventListener('click', () => stationModal(state.stations.find(s => s.id === b.dataset.stationEdit))));
    tbody.querySelectorAll('[data-station-delete]').forEach(b =>
        b.addEventListener('click', () => deleteStation(b.dataset.stationDelete)));
}

async function deleteStation(id) {
    if (!confirm('Delete station? This removes all related customers, orders, schedules.')) return;
    try {
        await API.del('/api/stations/' + id);
        toast('Station deleted', 'ok');
        await loadStations();
    } catch (e) { toast(e.message, 'error'); }
}

function stationModal(existing) {
    openModal(existing ? 'Edit station' : 'New station', `
        <label>Name <input name="name" required value="${escapeAttr(existing?.name)}" /></label>
        <label>Call letters <input name="callLetters" value="${escapeAttr(existing?.callLetters)}" /></label>
        <label>Time zone <input name="timeZone" value="${escapeAttr(existing?.timeZone || 'UTC')}" placeholder="UTC, America/Phoenix, ..." /></label>
    `, async (form) => {
        const body = formToJson(form);
        if (existing) await API.put('/api/stations/' + existing.id, body);
        else await API.post('/api/stations', body);
        toast(existing ? 'Station updated' : 'Station created', 'ok');
        await loadStations();
    });
}

// ---------- LibreTime Connection ----------
async function loadConnection() {
    if (!state.currentStationId) {
        document.getElementById('connStatus').innerHTML = '<span class="status-bad">Pick or create a station first.</span>';
        return;
    }
    try {
        const c = await API.get(`/api/stations/${state.currentStationId}/librtime/connection`);
        document.getElementById('connBaseUrl').value = c.baseUrl || '';
        document.getElementById('connUsername').value = c.username || '';
        document.getElementById('connPassword').value = '';
        document.getElementById('connPassword').placeholder = c.configured
            ? '(leave blank to keep existing)'
            : '(LibreTime user password)';
        const status = document.getElementById('connStatus');
        if (!c.configured) status.innerHTML = '<span class="muted">Not configured.</span>';
        else if (c.lastTestedAt) {
            const cls = c.lastTestOk ? 'status-ok' : 'status-bad';
            status.innerHTML = `Last test: <span class="${cls}">${escapeHtml(c.lastTestMessage || (c.lastTestOk ? 'OK' : 'Failed'))}</span> at ${new Date(c.lastTestedAt).toLocaleString()}`;
        } else status.innerHTML = '<span class="muted">Configured but not yet tested.</span>';
    } catch (e) {
        document.getElementById('connStatus').innerHTML = `<span class="status-bad">${escapeHtml(e.message)}</span>`;
    }
}

async function saveConnection(e) {
    e.preventDefault();
    if (!state.currentStationId) { toast('Pick or create a station first.', 'error'); return; }
    const body = {
        baseUrl: document.getElementById('connBaseUrl').value.trim(),
        username: document.getElementById('connUsername').value.trim(),
        password: document.getElementById('connPassword').value || null,
    };
    try {
        await API.put(`/api/stations/${state.currentStationId}/librtime/connection`, body);
        document.getElementById('connStatus').innerHTML = '<span class="status-ok">Saved.</span>';
        document.getElementById('connPassword').value = '';
        toast('Connection saved', 'ok');
    } catch (err) {
        toast(err.message, 'error');
    }
}

async function testConnection() {
    if (!state.currentStationId) return;
    document.getElementById('connStatus').innerHTML = 'Testing...';
    try {
        const r = await API.post(`/api/stations/${state.currentStationId}/librtime/connection/test`, {});
        const cls = r.ok ? 'status-ok' : 'status-bad';
        document.getElementById('connStatus').innerHTML =
            `<span class="${cls}">${r.ok ? 'OK' : 'Failed'}: ${escapeHtml(r.message || '')}</span>`;
        toast(r.ok ? 'Connection OK' : 'Connection failed: ' + r.message, r.ok ? 'ok' : 'error');
    } catch (e) {
        document.getElementById('connStatus').innerHTML = `<span class="status-bad">${escapeHtml(e.message)}</span>`;
        toast(e.message, 'error');
    }
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
    tbody.querySelectorAll('[data-user-edit]').forEach(b =>
        b.addEventListener('click', () => userModal(list.find(u => u.id === b.dataset.userEdit))));
    tbody.querySelectorAll('[data-user-delete]').forEach(b =>
        b.addEventListener('click', async () => {
            if (!confirm('Delete user?')) return;
            try { await API.del('/api/users/' + b.dataset.userDelete); toast('User deleted', 'ok'); await loadUsers(); }
            catch (e) { toast(e.message, 'error'); }
        }));
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
        <label>${existing ? 'New password (blank = unchanged)' : 'Password'}
            <input name="password" type="password" minlength="${existing ? 0 : 8}" ${existing ? '' : 'required'} autocomplete="new-password" />
        </label>
    `, async (form) => {
        const body = formToJson(form);
        if (body.active !== undefined) body.active = body.active === 'true';
        if (existing) {
            if (!body.password) delete body.password;
            await API.put('/api/users/' + existing.id, body);
        } else {
            await API.post('/api/users', body);
        }
        toast(existing ? 'User updated' : 'User created', 'ok');
        await loadUsers();
    });
}

// ---------- Library tab ----------
async function loadLibraryTab() {
    if (!state.currentStationId) {
        ['libFiles', 'libShowInstances', 'libTemplates'].forEach(id =>
            document.getElementById(id).innerHTML = '<div class="empty-state"><strong>Pick a station first</strong></div>');
        return;
    }
    document.getElementById('libFiles').textContent = 'Loading...';
    document.getElementById('libShowInstances').textContent = 'Loading...';
    document.getElementById('libTemplates').textContent = 'Loading...';

    Promise.allSettled([
        librarySearch(),
        loadLibShowInstances(),
        loadLibTemplates(),
    ]);
}

async function librarySearch() {
    const q = document.getElementById('libSearch').value;
    try {
        const files = await API.get(`/api/stations/${state.currentStationId}/librtime/library?q=${encodeURIComponent(q || '')}&limit=200`);
        document.getElementById('libCount').textContent = files.length + (files.length === 200 ? '+ files' : ' files');
        document.getElementById('libFiles').innerHTML = renderFilesTable(files);
    } catch (e) {
        document.getElementById('libFiles').innerHTML = libreTimeError(e);
    }
}

async function loadLibShowInstances() {
    const today = new Date().toISOString().slice(0, 10);
    try {
        const inst = await API.get(`/api/stations/${state.currentStationId}/librtime/show-instances?date=${today}`);
        document.getElementById('libShowInstances').innerHTML = renderInstancesTable(inst);
    } catch (e) {
        document.getElementById('libShowInstances').innerHTML = libreTimeError(e);
    }
}

async function loadLibTemplates() {
    try {
        const t = await API.get(`/api/stations/${state.currentStationId}/librtime/templates`);
        if (!t.length) {
            document.getElementById('libTemplates').innerHTML = '<div class="empty-state"><strong>No smart blocks or playlists</strong>Create them in LibreTime.</div>';
            return;
        }
        document.getElementById('libTemplates').innerHTML =
            '<table class="data-table"><thead><tr><th>Type</th><th>Name</th><th>Description</th></tr></thead><tbody>' +
            t.map(x => `<tr><td>${x.type}</td><td>${escapeHtml(x.name || ('#' + x.id))}</td><td>${escapeHtml(x.description || '')}</td></tr>`).join('') +
            '</tbody></table>';
    } catch (e) {
        document.getElementById('libTemplates').innerHTML = libreTimeError(e);
    }
}

function libreTimeError(e) {
    return `<div class="empty-state"><strong>Couldn't reach LibreTime</strong>${escapeHtml(e.message)}<br><br>
        <button class="link" onclick="showTab('connection')">Open LibreTime Connection</button></div>`;
}

function renderFilesTable(files) {
    if (!files?.length) return '<div class="empty-state"><strong>No files match</strong>Try a different search or clear it.</div>';
    return '<table class="data-table"><thead><tr><th>Name</th><th>Length</th><th>Format</th><th>Filepath</th></tr></thead><tbody>' +
        files.map(f => `<tr>
            <td>${escapeHtml(f.name || '(untitled)')}</td>
            <td>${escapeHtml(f.length || '')}</td>
            <td>${escapeHtml(f.mime || '')}</td>
            <td class="muted small">${escapeHtml(f.filepath || '')}</td>
        </tr>`).join('') + '</tbody></table>';
}

function renderInstancesTable(items) {
    if (!items?.length) return '<div class="empty-state"><strong>No show instances</strong>Schedule shows in LibreTime first.</div>';
    return '<table class="data-table"><thead><tr><th>Show</th><th>Starts</th><th>Ends</th><th>Filled</th></tr></thead><tbody>' +
        items.map(i => `<tr>
            <td>${escapeHtml(i.showName)}</td>
            <td>${formatTime(i.startsAt)}</td>
            <td>${formatTime(i.endsAt)}</td>
            <td>${escapeHtml(i.filledTime || '')}</td>
        </tr>`).join('') + '</tbody></table>';
}

// ---------- Customers ----------
async function loadCustomers() {
    if (!state.currentStationId) return;
    state.customers = await API.get('/api/customers?stationId=' + state.currentStationId);
    const tbody = document.getElementById('customersBody');
    if (!state.customers.length) {
        tbody.innerHTML = '<tr><td colspan="4"><div class="empty-state"><strong>No customers yet</strong>Add your first customer to start placing orders.</div></td></tr>';
        return;
    }
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
    tbody.querySelectorAll('[data-cust-edit]').forEach(b =>
        b.addEventListener('click', () => customerModal(state.customers.find(c => c.id === b.dataset.custEdit))));
    tbody.querySelectorAll('[data-cust-delete]').forEach(b =>
        b.addEventListener('click', async () => {
            if (!confirm('Delete customer? Their orders go with them.')) return;
            try { await API.del('/api/customers/' + b.dataset.custDelete); toast('Customer deleted', 'ok'); await loadCustomers(); }
            catch (e) { toast(e.message, 'error'); }
        }));
}

function customerModal(existing) {
    openModal(existing ? 'Edit customer' : 'New customer', `
        <label>Name <input name="name" required value="${escapeAttr(existing?.name)}" /></label>
        <label>Contact <input name="contact" value="${escapeAttr(existing?.contact)}" placeholder="email, phone, etc." /></label>
        <label>Notes <textarea name="notes" rows="3">${escapeHtml(existing?.notes || '')}</textarea></label>
    `, async (form) => {
        const body = formToJson(form);
        body.stationId = state.currentStationId;
        if (existing) await API.put('/api/customers/' + existing.id, body);
        else await API.post('/api/customers', body);
        toast(existing ? 'Customer updated' : 'Customer created', 'ok');
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
    if (!state.orders.length) {
        tbody.innerHTML = '<tr><td colspan="5"><div class="empty-state"><strong>No orders yet</strong>Click "New order" to start.</div></td></tr>';
        return;
    }
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
    tbody.querySelectorAll('[data-order-open]').forEach(b =>
        b.addEventListener('click', () => openOrderDetail(b.dataset.orderOpen)));
    tbody.querySelectorAll('[data-order-edit]').forEach(b =>
        b.addEventListener('click', () => orderModal(state.orders.find(o => o.id === b.dataset.orderEdit))));
    tbody.querySelectorAll('[data-order-delete]').forEach(b =>
        b.addEventListener('click', async () => {
            if (!confirm('Delete order? Spots go too.')) return;
            try { await API.del('/api/orders/' + b.dataset.orderDelete); toast('Order deleted', 'ok'); await loadOrders(); }
            catch (e) { toast(e.message, 'error'); }
        }));
}

function orderModal(existing) {
    if (!state.customers.length) {
        toast('Add a customer first.', 'error');
        return;
    }
    const opts = state.customers.map(c =>
        `<option value="${c.id}" ${existing?.customerId === c.id ? 'selected' : ''}>${escapeHtml(c.name)}</option>`).join('');
    openModal(existing ? 'Edit order' : 'New order', `
        <label>Customer <select name="customerId" required>${opts}</select></label>
        <label>Order name <input name="name" required value="${escapeAttr(existing?.name)}" /></label>
        <div class="row">
            <label class="grow">Start date <input name="startDate" type="date" required value="${existing?.startDate || ''}" /></label>
            <label class="grow">End date <input name="endDate" type="date" required value="${existing?.endDate || ''}" /></label>
        </div>
        <label>Total spots <input name="totalSpots" type="number" min="1" required value="${existing?.totalSpots || 1}" /></label>
        <label>Notes <textarea name="notes" rows="3">${escapeHtml(existing?.notes || '')}</textarea></label>
    `, async (form) => {
        const body = formToJson(form);
        body.stationId = state.currentStationId;
        body.totalSpots = parseInt(body.totalSpots);
        if (existing) await API.put('/api/orders/' + existing.id, body);
        else await API.post('/api/orders', body);
        toast(existing ? 'Order updated' : 'Order created', 'ok');
        await loadOrders();
    });
}

async function openOrderDetail(orderId) {
    state.selectedOrderId = orderId;
    const order = state.orders.find(o => o.id === orderId);
    document.getElementById('orderDetail').classList.remove('hidden');
    document.getElementById('orderDetailTitle').textContent = 'Spots for: ' + order.name;
    document.getElementById('orderReconciliation').classList.add('hidden');
    await loadSpotsForOrder(orderId);
}

function closeOrderDetail() {
    state.selectedOrderId = null;
    document.getElementById('orderDetail').classList.add('hidden');
}

async function loadSpotsForOrder(orderId) {
    const list = await API.get('/api/orders/' + orderId + '/spots');
    state.spotsByOrder[orderId] = list;
    const tbody = document.getElementById('spotsBody');
    if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="5"><div class="empty-state"><strong>No spots yet</strong>Click "New spot" to add one.</div></td></tr>';
        return;
    }
    tbody.innerHTML = list.map(s => `
        <tr>
            <td>${escapeHtml(s.label)}</td>
            <td>${s.lengthSeconds}s</td>
            <td>${s.librtimeFileId ?? '<span class="muted">to-produce</span>'}</td>
            <td>${s.rotationKind === 'SPECIFIC_SHOW' ? 'Specific show #' + (s.targetShowId || '?') : 'Any time'}</td>
            <td>
                <button class="link" data-spot-edit="${s.id}">Edit</button>
                <button class="link" data-spot-delete="${s.id}">Delete</button>
            </td>
        </tr>`).join('');
    tbody.querySelectorAll('[data-spot-edit]').forEach(b =>
        b.addEventListener('click', () => spotModal(orderId, list.find(s => s.id === b.dataset.spotEdit))));
    tbody.querySelectorAll('[data-spot-delete]').forEach(b =>
        b.addEventListener('click', async () => {
            if (!confirm('Delete spot?')) return;
            try { await API.del('/api/spots/' + b.dataset.spotDelete); toast('Spot deleted', 'ok'); await loadSpotsForOrder(orderId); }
            catch (e) { toast(e.message, 'error'); }
        }));
}

async function spotModal(orderId, existing) {
    let pickedFile = existing?.librtimeFileId
        ? { id: existing.librtimeFileId, name: '#' + existing.librtimeFileId }
        : null;

    const buildBody = () => `
        <label>Label <input name="label" required value="${escapeAttr(existing?.label)}" /></label>
        <label>Length (seconds) <input name="lengthSeconds" type="number" min="1" required value="${existing?.lengthSeconds || 30}" /></label>
        <label>LibreTime file
            <div class="row">
                <span id="spotFileLabel" class="grow">${pickedFile ? escapeHtml(pickedFile.name) : '<span class="muted">to-produce</span>'}</span>
                <button type="button" class="link" id="spotPickFileBtn">Pick file</button>
                <button type="button" class="link" id="spotClearFileBtn">Clear</button>
            </div>
        </label>
        <label>Rotation
            <select name="rotationKind">
                <option value="ANY_TIME" ${existing?.rotationKind === 'ANY_TIME' ? 'selected' : ''}>Any time</option>
                <option value="SPECIFIC_SHOW" ${existing?.rotationKind === 'SPECIFIC_SHOW' ? 'selected' : ''}>Specific show</option>
            </select>
        </label>
        <label>Target show id (only for "Specific show") <input name="targetShowId" type="number" value="${existing?.targetShowId ?? ''}" /></label>
    `;

    openModal(existing ? 'Edit spot' : 'New spot', buildBody(), async (form) => {
        const body = formToJson(form);
        body.lengthSeconds = parseInt(body.lengthSeconds);
        body.librtimeFileId = pickedFile ? pickedFile.id : null;
        body.targetShowId = body.targetShowId === '' ? null : parseInt(body.targetShowId);
        if (existing) await API.put('/api/spots/' + existing.id, body);
        else await API.post('/api/orders/' + orderId + '/spots', body);
        toast(existing ? 'Spot updated' : 'Spot created', 'ok');
        await loadSpotsForOrder(orderId);
    });

    const wireFilePickButtons = () => {
        document.getElementById('spotPickFileBtn').addEventListener('click', async () => {
            const f = await pickFile();
            if (f) {
                pickedFile = f;
                document.getElementById('spotFileLabel').textContent = f.name + (f.length ? ' (' + f.length + ')' : '');
            }
        });
        document.getElementById('spotClearFileBtn').addEventListener('click', () => {
            pickedFile = null;
            document.getElementById('spotFileLabel').innerHTML = '<span class="muted">to-produce</span>';
        });
    };
    wireFilePickButtons();
}

// ---------- Reconciliation ----------
async function viewReconciliation(orderId) {
    if (!orderId) return;
    try {
        const r = await API.get('/api/orders/' + orderId + '/reconciliation');
        const recDiv = document.getElementById('orderReconciliation');
        recDiv.classList.remove('hidden');
        recDiv.innerHTML = `
            <h4>Reconciliation</h4>
            <div class="recon-stats">
                <div class="recon-stat"><div class="muted small">Scheduled</div><div class="recon-stat-num">${r.scheduled}</div></div>
                <div class="recon-stat matched"><div class="muted small">Matched</div><div class="recon-stat-num">${r.matched}</div></div>
                <div class="recon-stat missed"><div class="muted small">Missed</div><div class="recon-stat-num">${r.missed}</div></div>
                <div class="recon-stat pending"><div class="muted small">Pending</div><div class="recon-stat-num">${r.scheduled - r.matched - r.missed}</div></div>
            </div>
            ${r.rows.length ? `<table class="data-table">
                <thead><tr><th>Spot</th><th>Scheduled at</th><th>Status</th></tr></thead>
                <tbody>${r.rows.map(row => `<tr>
                    <td>${escapeHtml(row.spotLabel || '')}</td>
                    <td>${row.scheduledAt ? new Date(row.scheduledAt).toLocaleString() : ''}</td>
                    <td><span class="badge ${row.status.toLowerCase()}">${row.status}</span></td>
                </tr>`).join('')}</tbody>
            </table>` : '<p class="muted">No scheduled instances yet for this order. Build a day in the Day Builder and push it to LibreTime.</p>'}
        `;
    } catch (e) { toast(e.message, 'error'); }
}

// ---------- Day Builder ----------
async function initDayBuilder() {
    if (!state.currentStationId) {
        document.getElementById('dayBuilderShows').innerHTML = '<div class="empty-state"><strong>Pick a station first</strong></div>';
        return;
    }
    const dateInput = document.getElementById('dayBuilderDate');
    if (!dateInput.value) dateInput.value = new Date().toISOString().slice(0, 10);
    await loadDay();
}

async function loadDay() {
    if (!state.currentStationId) return;
    const date = document.getElementById('dayBuilderDate').value;
    if (!date) return;

    document.getElementById('dayBuilderShows').textContent = 'Loading...';
    document.getElementById('dayBuilderSlots').textContent = '';
    document.getElementById('dayBuilderSpotPool').textContent = '';

    try {
        state.day = await API.get(`/api/stations/${state.currentStationId}/days/${date}`);
    } catch (e) {
        document.getElementById('dayBuilderShows').innerHTML = `<div class="empty-state"><strong>Couldn't load schedule</strong>${escapeHtml(e.message)}</div>`;
        return;
    }

    try {
        state.showInstances = await API.get(`/api/stations/${state.currentStationId}/librtime/show-instances?date=${date}`);
    } catch (e) {
        state.showInstances = [];
        document.getElementById('dayBuilderShows').innerHTML = libreTimeError(e);
    }
    await loadActiveSpotPool(date);
    renderDay();
}

async function loadActiveSpotPool(date) {
    if (!state.orders.length) {
        try { state.orders = await API.get('/api/orders?stationId=' + state.currentStationId); }
        catch { state.orders = []; }
    }
    const active = state.orders.filter(o => o.startDate <= date && o.endDate >= date);
    const pool = [];
    for (const o of active) {
        if (!state.spotsByOrder[o.id]) {
            try { state.spotsByOrder[o.id] = await API.get('/api/orders/' + o.id + '/spots'); }
            catch { state.spotsByOrder[o.id] = []; }
        }
        for (const s of state.spotsByOrder[o.id]) pool.push({ order: o, spot: s });
    }
    state.spotPool = pool;
}

function renderDay() {
    const d = state.day;
    if (!d) return;

    document.getElementById('dayBuilderStatus').textContent = d.status;
    document.getElementById('dayBuilderStatus').className = 'badge ' + d.status.toLowerCase();

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
    document.getElementById('dayBuilderLockBtn').textContent = haveLock ? 'Edit locked by you' : 'Edit this day';
    document.getElementById('dayBuilderSaveBtn').disabled = !haveLock || !editable;
    document.getElementById('dayBuilderPushBtn').disabled = !haveLock || !editable;
    document.getElementById('dayBuilderReopenBtn').disabled = !isAdmin || d.status !== 'PUSHED';
    document.getElementById('dayBuilderForceUnlockBtn').disabled = !isAdmin || !d.lock;

    // Shows
    const showsCount = state.showInstances.length;
    document.getElementById('dayBuilderShowsCount').textContent = showsCount ? '(' + showsCount + ')' : '';
    if (!showsCount) {
        document.getElementById('dayBuilderShows').innerHTML = state.currentStationId
            ? `<div class="empty-state"><strong>No LibreTime show instances</strong>Schedule shows in LibreTime for this date, or check the LibreTime connection.</div>`
            : '<div class="empty-state">Pick a station first.</div>';
    } else {
        document.getElementById('dayBuilderShows').innerHTML = state.showInstances.map(s => `
            <div class="show-block" data-show-id="${s.id}">
                <div class="show-name">${escapeHtml(s.showName)}</div>
                <div class="show-time">${formatTime(s.startsAt)} &rarr; ${formatTime(s.endsAt)}</div>
                <div class="show-actions">
                    <button class="link" data-add-slot="${s.id}">+ Add empty slot</button>
                </div>
            </div>`).join('');
        document.querySelectorAll('[data-add-slot]').forEach(b =>
            b.addEventListener('click', () => addPlaceholderSlot(parseInt(b.dataset.addSlot))));
    }

    // Slots, grouped by show instance
    const items = (d.items || []).slice().sort((a, b) => a.position - b.position);
    document.getElementById('dayBuilderSlotsCount').textContent = items.length ? '(' + items.length + ')' : '';
    if (!items.length) {
        document.getElementById('dayBuilderSlots').innerHTML = '<div class="empty-state"><strong>No slots yet</strong>Click "+ Add empty slot" on a show to start.</div>';
    } else {
        const showName = id => state.showInstances.find(s => s.id === id)?.showName || ('Show #' + id);
        let html = '';
        let lastShow = null;
        let originalIdx = 0;
        const groups = items.map((it, i) => ({ ...it, _idx: i }));
        for (const it of groups) {
            if (it.showInstanceId !== lastShow) {
                html += `<div class="slot-group-header">${escapeHtml(showName(it.showInstanceId))}</div>`;
                lastShow = it.showInstanceId;
            }
            html += slotHtml(it, it._idx);
        }
        document.getElementById('dayBuilderSlots').innerHTML = html;
        document.querySelectorAll('[data-fill-spot]').forEach(b =>
            b.addEventListener('click', () => fillSlotFromSpot(parseInt(b.dataset.fillSpot))));
        document.querySelectorAll('[data-fill-track]').forEach(b =>
            b.addEventListener('click', () => fillSlotFromTrack(parseInt(b.dataset.fillTrack))));
        document.querySelectorAll('[data-remove-slot]').forEach(b =>
            b.addEventListener('click', () => removeSlot(parseInt(b.dataset.removeSlot))));
    }

    // Spot pool
    document.getElementById('dayBuilderSpotPoolCount').textContent = state.spotPool.length ? '(' + state.spotPool.length + ')' : '';
    if (!state.spotPool.length) {
        document.getElementById('dayBuilderSpotPool').innerHTML = `<div class="empty-state"><strong>No active spots for this date</strong>Add an order whose date range includes ${escapeHtml(d.date)}.</div>`;
    } else {
        document.getElementById('dayBuilderSpotPool').innerHTML = state.spotPool.map(p => `
            <div class="spot-card">
                <div class="spot-label">${escapeHtml(p.spot.label)}</div>
                <div class="spot-meta">${escapeHtml(p.order.name)} &middot; ${p.spot.lengthSeconds}s &middot; ${p.spot.librtimeFileId ? 'file ' + p.spot.librtimeFileId : 'to-produce'}</div>
            </div>`).join('');
    }
}

function slotHtml(it, idx) {
    const k = (it.kind || 'PLACEHOLDER').toLowerCase();
    const meta = [];
    if (it.spotId) meta.push('spot ' + it.spotId.slice(0, 8));
    if (it.librtimeFileId) meta.push('file ' + it.librtimeFileId);
    if (it.lengthSeconds) meta.push(it.lengthSeconds + 's');
    return `<div class="slot ${k}">
        <div><strong>#${idx + 1}</strong> ${escapeHtml(it.kind)}</div>
        <div class="slot-meta">${meta.join(' &middot; ') || '<span class="muted">empty</span>'}</div>
        <div class="slot-actions">
            <button class="link" data-fill-spot="${idx}">Use spot</button>
            <button class="link" data-fill-track="${idx}">Use track</button>
            <button class="link" data-remove-slot="${idx}">Remove</button>
        </div>
    </div>`;
}

function addPlaceholderSlot(showInstanceId) {
    if (!state.day || !state.day.lock || !state.day.lock.self) {
        toast('Acquire the day lock first ("Edit this day").', 'error');
        return;
    }
    const items = (state.day.items || []).slice();
    const slotIdx = items.filter(x => x.showInstanceId === showInstanceId).length;
    items.push({
        showInstanceId,
        slotIndex: slotIdx,
        kind: 'PLACEHOLDER',
        spotId: null,
        librtimeFileId: null,
        scheduledAt: null,
        lengthSeconds: 30,
        position: items.length,
    });
    state.day.items = items;
    renderDay();
}

async function fillSlotFromSpot(idx) {
    if (!state.spotPool.length) { toast('No spots in the pool. Add an order with active dates first.', 'error'); return; }
    const choice = await pickFromList('Pick a spot to schedule', state.spotPool.map(p => ({
        id: p.spot.id,
        title: p.spot.label,
        meta: `${p.order.name} · ${p.spot.lengthSeconds}s · ${p.spot.librtimeFileId ? 'file ' + p.spot.librtimeFileId : 'to-produce'}`,
        _data: p,
    })));
    if (!choice) return;
    const p = choice._data;
    state.day.items[idx].kind = 'SPOT';
    state.day.items[idx].spotId = p.spot.id;
    state.day.items[idx].librtimeFileId = p.spot.librtimeFileId;
    state.day.items[idx].lengthSeconds = p.spot.lengthSeconds;
    renderDay();
}

async function fillSlotFromTrack(idx) {
    const f = await pickFile();
    if (!f) return;
    state.day.items[idx].kind = 'TRACK';
    state.day.items[idx].spotId = null;
    state.day.items[idx].librtimeFileId = f.id;
    state.day.items[idx].lengthSeconds = parseLengthSeconds(f.length) || 180;
    renderDay();
}

function removeSlot(idx) {
    state.day.items.splice(idx, 1);
    state.day.items.forEach((it, i) => it.position = i);
    renderDay();
}

async function acquireDayLock() {
    try { await API.post(`/api/days/${state.day.id}/lock`, {}); toast('Locked for editing', 'ok'); }
    catch (e) { toast(e.message, 'error'); return; }
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
        toast('Draft saved', 'ok');
    } catch (e) { toast(e.message, 'error'); }
}

async function pushDay() {
    if (!confirm('Push this day to LibreTime? Existing schedule items in those show instances will be replaced.')) return;
    try {
        await API.post(`/api/days/${state.day.id}/push`, {});
        toast('Pushed to LibreTime', 'ok');
        await loadDay();
    } catch (e) { toast(e.message, 'error'); }
}

async function reopenDay() {
    if (!confirm('Reopen this day for editing?')) return;
    try { await API.post(`/api/days/${state.day.id}/reopen`, {}); toast('Reopened', 'ok'); await loadDay(); }
    catch (e) { toast(e.message, 'error'); }
}

async function forceUnlockDay() {
    if (!confirm('Force-unlock this day? The other user will lose unsaved changes.')) return;
    try { await API.del(`/api/days/${state.day.id}/lock?force=true`); toast('Lock released', 'ok'); await loadDay(); }
    catch (e) { toast(e.message, 'error'); }
}

async function pullPlayback() {
    const date = document.getElementById('dayBuilderDate').value;
    if (!date) return;
    try {
        const r = await API.post(`/api/stations/${state.currentStationId}/playback/import?date=${date}`, {});
        toast(`Imported ${r.entriesSaved} playback entries (${r.matched} matched, ${r.missed} missed)`, 'ok');
    } catch (e) { toast(e.message, 'error'); }
}

// ---------- Picker (replaces prompt/alert pickers) ----------
let pickerResolve = null;
function pickFromList(title, items) {
    return new Promise(resolve => {
        pickerResolve = resolve;
        document.getElementById('pickerTitle').textContent = title;
        const search = document.getElementById('pickerSearch');
        search.value = '';
        const list = document.getElementById('pickerList');
        const render = (filter) => {
            const f = (filter || '').toLowerCase();
            const filtered = !f ? items : items.filter(i =>
                (i.title || '').toLowerCase().includes(f) || (i.meta || '').toLowerCase().includes(f));
            if (!filtered.length) {
                list.innerHTML = '<div class="empty-state"><strong>No matches</strong></div>';
                return;
            }
            list.innerHTML = filtered.map((i, idx) => `
                <div class="picker-row" data-pick-idx="${items.indexOf(i)}">
                    <div class="grow">
                        <div class="picker-title">${escapeHtml(i.title || '')}</div>
                        <div class="picker-meta">${escapeHtml(i.meta || '')}</div>
                    </div>
                </div>`).join('');
            list.querySelectorAll('[data-pick-idx]').forEach(row =>
                row.addEventListener('click', () => {
                    const i = items[parseInt(row.dataset.pickIdx)];
                    closePicker(i);
                }));
        };
        search.oninput = () => render(search.value);
        render('');
        document.getElementById('pickerModal').showModal();
        setTimeout(() => search.focus(), 50);
    });
}

function closePicker(value) {
    document.getElementById('pickerModal').close();
    if (pickerResolve) {
        pickerResolve(value);
        pickerResolve = null;
    }
}

async function pickFile() {
    if (!state.currentStationId) return null;
    const search = prompt; // unused
    let files = [];
    try {
        files = await API.get(`/api/stations/${state.currentStationId}/librtime/library?limit=500`);
    } catch (e) {
        toast('Couldn\'t reach LibreTime: ' + e.message, 'error');
        return null;
    }
    if (!files.length) {
        toast('LibreTime library is empty.', 'info');
        return null;
    }
    return await pickFromList('Pick a LibreTime file', files.map(f => ({
        id: f.id,
        title: f.name || ('#' + f.id),
        meta: [f.length, f.mime, f.filepath].filter(Boolean).join(' · '),
        _data: f,
    }))).then(c => c ? { id: c._data.id, name: c._data.name, length: c._data.length } : null);
}

// ---------- Tabs ----------
const tabLoaders = {
    dayBuilder: { title: 'Day Builder', load: initDayBuilder },
    library: { title: 'LibreTime Library', load: loadLibraryTab },
    customers: { title: 'Customers', load: loadCustomers },
    orders: { title: 'Orders & Spots', load: loadOrders },
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
        } catch (err) { toast(err.message, 'error'); }
    };
    form.onsubmit = handler;
    document.getElementById('modalCancel').onclick = () => dlg.close();
    dlg.showModal();
}

// ---------- Toast ----------
let toastTimer = null;
function toast(message, kind) {
    const el = document.getElementById('toast');
    el.textContent = message;
    el.className = 'toast ' + (kind || 'info');
    if (toastTimer) clearTimeout(toastTimer);
    toastTimer = setTimeout(() => el.classList.add('hidden'), 4000);
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
function formatTime(iso) {
    if (!iso) return '';
    try {
        const d = new Date(iso);
        if (isNaN(d.getTime())) return iso;
        return d.toLocaleString([], { month: 'short', day: 'numeric', hour: '2-digit', minute: '2-digit' });
    } catch { return iso; }
}
function parseLengthSeconds(lenStr) {
    if (!lenStr) return null;
    // LibreTime returns "0:03:42.123" style strings
    const m = String(lenStr).match(/^(\d+):(\d+):(\d+)/);
    if (m) return parseInt(m[1]) * 3600 + parseInt(m[2]) * 60 + parseInt(m[3]);
    const n = parseInt(lenStr);
    return isNaN(n) ? null : n;
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

    // Stations
    document.getElementById('stationNewBtn').addEventListener('click', () => stationModal(null));
    // Users
    document.getElementById('userNewBtn').addEventListener('click', () => userModal(null));
    // Connection
    document.getElementById('connectionForm').addEventListener('submit', saveConnection);
    document.getElementById('connTestBtn').addEventListener('click', testConnection);
    // Library
    document.getElementById('libSearchBtn').addEventListener('click', librarySearch);
    document.getElementById('libSearch').addEventListener('keydown', (e) => { if (e.key === 'Enter') librarySearch(); });
    // Customers
    document.getElementById('customerNewBtn').addEventListener('click', () => customerModal(null));
    // Orders / Spots
    document.getElementById('orderNewBtn').addEventListener('click', () => orderModal(null));
    document.getElementById('spotNewBtn').addEventListener('click', () => spotModal(state.selectedOrderId, null));
    document.getElementById('orderReconcileBtn').addEventListener('click', () => viewReconciliation(state.selectedOrderId));
    document.getElementById('orderDetailClose').addEventListener('click', closeOrderDetail);
    // Day Builder
    document.getElementById('dayBuilderDate').addEventListener('change', loadDay);
    document.getElementById('dayBuilderRefresh').addEventListener('click', loadDay);
    document.getElementById('dayBuilderLockBtn').addEventListener('click', acquireDayLock);
    document.getElementById('dayBuilderSaveBtn').addEventListener('click', saveDay);
    document.getElementById('dayBuilderPushBtn').addEventListener('click', pushDay);
    document.getElementById('dayBuilderReopenBtn').addEventListener('click', reopenDay);
    document.getElementById('dayBuilderForceUnlockBtn').addEventListener('click', forceUnlockDay);
    document.getElementById('dayBuilderPullPlayback').addEventListener('click', pullPlayback);
    // Picker
    document.getElementById('pickerCancel').addEventListener('click', () => closePicker(null));

    if (state.token) {
        const cached = localStorage.getItem('librelog.user');
        if (cached) state.user = JSON.parse(cached);
        bootstrap().catch(() => logout());
    }
});
