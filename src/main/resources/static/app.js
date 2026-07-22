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
    libreTimeShows: [],
    /** Map LibreTime file id -> human-readable label (built from name or filepath). */
    libreTimeFileById: {},
    playbackRows: [],
    fulfillmentRows: [],
    carts: [],
    cartsById: {},
    selectedCartId: null,
    cartMembers: [],
    clocks: [],
    selectedClockId: null,
    dayParts: [],
    dayPartsStationId: null,
    cartCategories: { library: [], commercial: [] },
    mediaUploads: [],
};

const CATEGORY_LABELS = {
    MUSIC: 'Music',
    IMAGING: 'Imaging (IDs / jingles / sweepers)',
    CONTENT: 'Content / segments',
    INTERVIEW: 'Interviews',
    NEWS: 'News',
    WEATHER: 'Weather',
    PROMO: 'Promo',
    VOICETRACK: 'Voicetrack',
    COMMERCIAL: 'Commercial',
    SPONSORED_FEATURE: 'Sponsored feature',
};

function categoryLabel(c) { return CATEGORY_LABELS[c] || c || ''; }

const COMMON_TIME_ZONES = [
    'America/Phoenix',
    'America/Los_Angeles',
    'America/Denver',
    'America/Chicago',
    'America/New_York',
    'UTC',
];

// ---------- Build / version (footer) ----------
async function loadVersionIntoElement(el) {
    if (!el) return;
    try {
        const res = await fetch('/api/version', { headers: { Accept: 'application/json' } });
        if (!res.ok) throw new Error(String(res.status));
        const v = await res.json();
        const ver = v.version || 'unknown';
        const time = v.time ? formatVersionBuildTime(v.time) : '';
        el.textContent = time ? `LibreLog ${ver} · built ${time}` : `LibreLog ${ver}`;
    } catch {
        el.textContent = 'LibreLog (version unavailable)';
    }
}

function formatVersionBuildTime(iso) {
    try {
        const d = new Date(iso);
        if (Number.isNaN(d.getTime())) return iso;
        return d.toLocaleString(undefined, { dateStyle: 'medium', timeStyle: 'short' });
    } catch {
        return iso;
    }
}

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
    async upload(path, formData) {
        const opts = { method: 'POST', headers: { 'Accept': 'application/json' }, body: formData };
        // No Content-Type: the browser sets the multipart boundary itself.
        if (state.token) opts.headers['Authorization'] = 'Bearer ' + state.token;
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
    loadVersionIntoElement(document.getElementById('appFooter'));
}

async function loadStationsTab() {
    await loadStations();
    if (state.currentStationId) await loadDayPartsForStation(state.currentStationId);
    renderDayPartsPanel();
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
    state.libreTimeShows = [];
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
        state.libreTimeShows = [];
        state.libreTimeFileById = {};
        state.carts = [];
        state.cartsById = {};
        state.clocks = [];
        state.mediaUploads = [];
        state.selectedCartId = null;
        state.selectedClockId = null;
        state.dayParts = [];
        state.dayPartsStationId = null;
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
            <td data-label="Name">${escapeHtml(s.name)}</td>
            <td data-label="Call letters">${escapeHtml(s.callLetters || '')}</td>
            <td data-label="Time zone">${escapeHtml(s.timeZone)}</td>
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
    const selectedTimeZone = existing?.timeZone || 'America/Phoenix';
    const timeZoneOptions = COMMON_TIME_ZONES.map(tz =>
        `<option value="${tz}" ${selectedTimeZone === tz ? 'selected' : ''}>${tz}</option>`).join('');
    openModal(existing ? 'Edit station' : 'New station', `
        <label>Name <input name="name" required value="${escapeAttr(existing?.name)}" /></label>
        <label>Call letters <input name="callLetters" value="${escapeAttr(existing?.callLetters)}" /></label>
        <label>Time zone
            <select name="timeZone" required>${timeZoneOptions}</select>
        </label>
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
            <td data-label="Email">${escapeHtml(u.email)}</td>
            <td data-label="Name">${escapeHtml(u.name || '')}</td>
            <td data-label="Role">${u.role}</td>
            <td data-label="Active">${u.active ? 'Yes' : 'No'}</td>
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
            t.map(x => `<tr>
                <td data-label="Type">${x.type}</td>
                <td data-label="Name">${escapeHtml(x.name || ('#' + x.id))}</td>
                <td data-label="Description">${escapeHtml(x.description || '')}</td>
            </tr>`).join('') +
            '</tbody></table>';
    } catch (e) {
        document.getElementById('libTemplates').innerHTML = libreTimeError(e);
    }
}

async function loadLibreTimeShowsForSelect() {
    if (!state.currentStationId) return [];
    if (state.libreTimeShows.length) return state.libreTimeShows;

    try {
        const shows = await API.get(`/api/stations/${state.currentStationId}/librtime/shows`);
        state.libreTimeShows = (shows || [])
            .map(s => ({
                id: Number(s.id),
                name: s.name || s.title || ('Show #' + s.id),
            }))
            .filter(s => Number.isFinite(s.id) && s.id > 0)
            .sort((a, b) => a.name.localeCompare(b.name));
    } catch (e) {
        // If the raw show list is unavailable, fall back to show instances
        // already loaded for the selected day so users still get a dropdown.
        const byId = new Map();
        (state.showInstances || []).forEach(i => {
            if (i.showId) byId.set(Number(i.showId), i.showName || ('Show #' + i.showId));
        });
        state.libreTimeShows = Array.from(byId, ([id, name]) => ({ id, name }))
            .sort((a, b) => a.name.localeCompare(b.name));
        if (!state.libreTimeShows.length) {
            toast('Could not load LibreTime shows: ' + e.message, 'error');
        }
    }

    return state.libreTimeShows;
}

function showNameById(id) {
    if (!id) return null;
    return state.libreTimeShows.find(s => Number(s.id) === Number(id))?.name
        || state.showInstances.find(s => Number(s.showId) === Number(id))?.showName
        || null;
}

function libreTimeError(e) {
    return `<div class="empty-state"><strong>Couldn't reach LibreTime</strong>${escapeHtml(e.message)}<br><br>
        <button class="link" onclick="showTab('connection')">Open LibreTime Connection</button></div>`;
}

function renderFilesTable(files) {
    if (!files?.length) return '<div class="empty-state"><strong>No files match</strong>Try a different search or clear it.</div>';
    return '<table class="data-table"><thead><tr><th>Name</th><th>Length</th><th>Format</th><th>Filepath</th></tr></thead><tbody>' +
        files.map(f => `<tr>
            <td data-label="Name">${escapeHtml(f.name || '(untitled)')}</td>
            <td data-label="Length">${escapeHtml(f.length || '')}</td>
            <td data-label="Format">${escapeHtml(f.mime || '')}</td>
            <td data-label="Filepath" class="muted small">${escapeHtml(f.filepath || '')}</td>
        </tr>`).join('') + '</tbody></table>';
}

function renderInstancesTable(items) {
    if (!items?.length) return '<div class="empty-state"><strong>No show instances</strong>Schedule shows in LibreTime first.</div>';
    return '<table class="data-table"><thead><tr><th>Show</th><th>Starts</th><th>Ends</th><th>Filled</th></tr></thead><tbody>' +
        items.map(i => `<tr>
            <td data-label="Show">${escapeHtml(i.showName)}</td>
            <td data-label="Starts">${formatTime(i.startsAt)}</td>
            <td data-label="Ends">${formatTime(i.endsAt)}</td>
            <td data-label="Filled">${escapeHtml(i.filledTime || '')}</td>
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
            <td data-label="Name">${escapeHtml(c.name)}</td>
            <td data-label="Contact">${escapeHtml(c.contact || '')}</td>
            <td data-label="Notes">${escapeHtml(c.notes || '')}</td>
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
function orderKindLabel(kind) {
    if (kind === 'FOUNDING_MEMBER') return 'Founding member';
    return 'Standard';
}

function orderDateRangeDisplay(o) {
    const end = o.endDate ? o.endDate : 'open-ended';
    return o.startDate + ' → ' + end;
}

function orderActiveOnDate(o, dateStr) {
    if (!o || o.startDate > dateStr) return false;
    if (o.endDate && o.endDate < dateStr) return false;
    return true;
}

async function loadOrders() {
    if (!state.currentStationId) return;
    state.orders = await API.get('/api/orders?stationId=' + state.currentStationId);
    if (!state.customers.length) state.customers = await API.get('/api/customers?stationId=' + state.currentStationId);
    const customerName = id => state.customers.find(c => c.id === id)?.name || '(unknown)';
    const tbody = document.getElementById('ordersBody');
    if (!state.orders.length) {
        tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state"><strong>No orders yet</strong>Click "New order" to start.</div></td></tr>';
        return;
    }
    tbody.innerHTML = state.orders.map(o => `
        <tr>
            <td data-label="Name"><button class="link" data-order-open="${o.id}">${escapeHtml(o.name)}</button></td>
            <td data-label="Type">${escapeHtml(orderKindLabel(o.orderKind))}</td>
            <td data-label="Customer">${escapeHtml(customerName(o.customerId))}</td>
            <td data-label="Dates">${escapeHtml(orderDateRangeDisplay(o))}</td>
            <td data-label="Spots">${o.spotCount} / ${o.spotCap}</td>
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
    const kind = existing?.orderKind || 'STANDARD';
    const opts = state.customers.map(c =>
        `<option value="${c.id}" ${existing?.customerId === c.id ? 'selected' : ''}>${escapeHtml(c.name)}</option>`).join('');
    const priceDollars = existing?.monthlyPriceCents != null
        ? (existing.monthlyPriceCents / 100).toFixed(2).replace(/\.00$/, '')
        : '';
    openModal(existing ? 'Edit order' : 'New order', `
        <label>Customer <select name="customerId" required>${opts}</select></label>
        <label>Order name <input name="name" required value="${escapeAttr(existing?.name)}" /></label>
        <label>Order type
            <select name="orderKind" required onchange="toggleOrderKindFields(this)">
                <option value="STANDARD" ${kind === 'STANDARD' ? 'selected' : ''}>Standard — fixed dates &amp; total spot count</option>
                <option value="FOUNDING_MEMBER" ${kind === 'FOUNDING_MEMBER' ? 'selected' : ''}>Founding member — monthly spot allowance &amp; billing</option>
            </select>
        </label>
        <label>Start date <input name="startDate" type="date" required value="${existing?.startDate || ''}" /></label>

        <div data-order-standard style="display:${kind === 'STANDARD' ? 'block' : 'none'}">
            <label>End date <input name="endDate" type="date" required value="${existing?.endDate || ''}" /></label>
            <label>Total spots (max rows in this campaign) <input name="totalSpots" type="number" min="1" value="${existing?.orderKind === 'FOUNDING_MEMBER' ? 1 : (existing?.totalSpots || 1)}" /></label>
        </div>

        <div data-order-founding style="display:${kind === 'FOUNDING_MEMBER' ? 'block' : 'none'}">
            <p class="muted small">Spots you add count toward the allowance below (e.g. 10 creatives for 10 spots/month). Open-ended until you set an end date or cancel.</p>
            <label>End date (optional — leave empty while active) <input name="endDateFounding" type="date" value="${kind === 'FOUNDING_MEMBER' && existing?.endDate ? existing.endDate : ''}" /></label>
            <label>Spots per month (max spot rows on this order) <input name="monthlySpotAllowance" type="number" min="1" value="${existing?.monthlySpotAllowance || 10}" /></label>
            <label>Monthly bill (USD, optional) <input name="monthlyPriceDollars" type="number" min="0" step="0.01" placeholder="100" value="${escapeAttr(priceDollars)}" /></label>
        </div>

        <label>Notes <textarea name="notes" rows="3">${escapeHtml(existing?.notes || '')}</textarea></label>
    `, async (form) => {
        const fd = new FormData(form);
        const k = fd.get('orderKind');
        const body = {
            stationId: state.currentStationId,
            customerId: fd.get('customerId'),
            name: fd.get('name'),
            startDate: fd.get('startDate'),
            notes: fd.get('notes') || null,
            orderKind: k,
        };
        if (k === 'FOUNDING_MEMBER') {
            const end = fd.get('endDateFounding');
            body.endDate = end && String(end).trim() ? end : null;
            body.monthlySpotAllowance = parseInt(fd.get('monthlySpotAllowance'), 10);
            if (isNaN(body.monthlySpotAllowance) || body.monthlySpotAllowance < 1) {
                throw new Error('Spots per month must be at least 1');
            }
            const dollars = fd.get('monthlyPriceDollars');
            if (dollars != null && String(dollars).trim() !== '') {
                const n = parseFloat(String(dollars));
                if (isNaN(n) || n < 0) throw new Error('Monthly bill must be a non-negative number');
                body.monthlyPriceCents = Math.round(n * 100);
            } else {
                body.monthlyPriceCents = null;
            }
            body.totalSpots = null;
        } else {
            body.endDate = fd.get('endDate');
            if (!body.endDate) throw new Error('Standard orders need an end date');
            body.totalSpots = parseInt(fd.get('totalSpots'), 10);
            if (isNaN(body.totalSpots) || body.totalSpots < 1) throw new Error('Total spots must be at least 1');
            body.monthlySpotAllowance = null;
            body.monthlyPriceCents = null;
        }
        if (existing) await API.put('/api/orders/' + existing.id, body);
        else await API.post('/api/orders', body);
        toast(existing ? 'Order updated' : 'Order created', 'ok');
        await loadOrders();
    });
    const sel = document.querySelector('#modalForm select[name="orderKind"]');
    if (sel) toggleOrderKindFields(sel);
}

function toggleOrderKindFields(selectEl) {
    const form = selectEl.closest('form');
    if (!form) return;
    const kind = selectEl.value;
    form.querySelectorAll('[data-order-standard]').forEach(el => {
        el.style.display = kind === 'STANDARD' ? 'block' : 'none';
    });
    form.querySelectorAll('[data-order-founding]').forEach(el => {
        el.style.display = kind === 'FOUNDING_MEMBER' ? 'block' : 'none';
    });
    const endStd = form.querySelector('input[name="endDate"]');
    if (endStd) endStd.required = kind === 'STANDARD';
}

async function openOrderDetail(orderId) {
    state.selectedOrderId = orderId;
    const order = state.orders.find(o => o.id === orderId);
    document.getElementById('orderDetail').classList.remove('hidden');
    const typeBit = order?.orderKind === 'FOUNDING_MEMBER'
        ? ' (founding member)'
        : '';
    document.getElementById('orderDetailTitle').textContent = 'Spots for: ' + order.name + typeBit;
    document.getElementById('orderReconciliation').classList.add('hidden');
    if (order?.stationId) await loadDayPartsForStation(order.stationId);
    await loadSpotsForOrder(orderId);
}

function closeOrderDetail() {
    state.selectedOrderId = null;
    document.getElementById('orderDetail').classList.add('hidden');
}

async function loadSpotsForOrder(orderId) {
    const list = await API.get('/api/orders/' + orderId + '/spots');
    state.spotsByOrder[orderId] = list;
    const order = state.orders.find(o => o.id === orderId);
    const cap = order ? order.spotCap : 0;
    const spotBtn = document.getElementById('spotNewBtn');
    if (spotBtn) {
        spotBtn.disabled = list.length >= cap;
        spotBtn.title = list.length >= cap
            ? 'Order is at its spot limit. Edit the order to raise it or remove a spot.'
            : '';
    }
    const tbody = document.getElementById('spotsBody');
    if (!list.length) {
        tbody.innerHTML = '<tr><td colspan="7"><div class="empty-state"><strong>No spots yet</strong>Click "New spot" to add one.</div></td></tr>';
        return;
    }
    tbody.innerHTML = list.map(s => `
        <tr>
            <td data-label="Label">${escapeHtml(s.label)}</td>
            <td data-label="Length">${s.lengthSeconds}s</td>
            <td data-label="LibreTime file">${s.librtimeFileId ?? '<span class="muted">to-produce</span>'}</td>
            <td data-label="Status">${spotStatusCell(s)}</td>
            <td data-label="Rotation">${s.rotationKind === 'SPECIFIC_SHOW' ? 'Specific show: ' + escapeHtml(showNameById(s.targetShowId) || ('#' + (s.targetShowId || '?'))) : 'Any time'}</td>
            <td data-label="Local window">${spotWindowCell(s)}</td>
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
    tbody.querySelectorAll('[data-spot-advance]').forEach(b =>
        b.addEventListener('click', async () => {
            try {
                await API.post('/api/spots/' + b.dataset.spotAdvance + '/status', { status: b.dataset.toStatus });
                toast('Spot ' + b.dataset.toStatus.toLowerCase(), 'ok');
                await loadSpotsForOrder(orderId);
            } catch (e) { toast(e.message, 'error'); }
        }));
}

const SPOT_STATUS_LABEL = { DRAFT: 'Draft', PRODUCED: 'Produced', APPROVED: 'Approved', TRAFFICKED: 'Trafficked' };
// The next status a user can advance to, and the button label for it.
const SPOT_NEXT_STATUS = { DRAFT: ['PRODUCED', 'Mark produced'], PRODUCED: ['APPROVED', 'Approve'] };

function spotStatusCell(s) {
    const status = s.status || 'DRAFT';
    const badge = `<span class="badge status-${status.toLowerCase()}">${SPOT_STATUS_LABEL[status] || status}</span>`;
    const next = SPOT_NEXT_STATUS[status];
    if (!next) return badge;
    return `${badge} <button class="link" data-spot-advance="${s.id}" data-to-status="${next[0]}">${next[1]}</button>`;
}

async function spotModal(orderId, existing) {
    const order = state.orders.find(o => o.id === orderId);
    if (order?.stationId) await loadDayPartsForStation(order.stationId);
    let pickedFile = existing?.librtimeFileId
        ? { id: existing.librtimeFileId, name: '#' + existing.librtimeFileId }
        : null;
    const shows = await loadLibreTimeShowsForSelect();
    const showOptions = [
        `<option value="">No specific show</option>`,
        ...shows.map(show =>
            `<option value="${show.id}" ${Number(existing?.targetShowId) === Number(show.id) ? 'selected' : ''}>${escapeHtml(show.name)}</option>`)
    ].join('');

    const buildBody = () => `
        <label>Label <input id="spotLabelInput" name="label" required value="${escapeAttr(existing?.label)}" /></label>
        <label>Length (seconds) <input id="spotLengthInput" name="lengthSeconds" type="number" min="1" required value="${existing?.lengthSeconds || 30}" /></label>
        <label>LibreTime file
            <div class="row">
                <span id="spotFileLabel" class="grow">${pickedFile ? escapeHtml(pickedFile.name) : '<span class="muted">to-produce</span>'}</span>
                <button type="button" class="link" id="spotPickFileBtn">Pick file</button>
                <button type="button" class="link" id="spotClearFileBtn">Clear</button>
            </div>
        </label>
        <label>Day part (optional)
            <select id="spotDayPartSelect">${dayPartSelectOptionsHtml(existing?.dayPartId)}</select>
            <span class="muted small">Or choose Custom and set times below.</span>
        </label>
        <label>Local air window (optional, station time zone, half-open — end is exclusive)
            <div class="row" style="gap:8px;align-items:center">
                <input type="time" id="spotWinStart" value="${escapeAttr(minutesToTimeInput(existing?.localWindowStartMinutes))}" />
                <span class="muted">to</span>
                <input type="time" id="spotWinEnd" value="${escapeAttr(existing?.localWindowEndMinutes === 1440 ? '' : minutesToTimeInput(existing?.localWindowEndMinutes))}" />
            </div>
            <span class="muted small">Leave both empty for any time of day. Example: 06:00 to 12:00 = morning drive through noon (exclusive).</span>
        </label>
        <label>Rotation
            <select name="rotationKind">
                <option value="ANY_TIME" ${existing?.rotationKind === 'ANY_TIME' ? 'selected' : ''}>Any time</option>
                <option value="SPECIFIC_SHOW" ${existing?.rotationKind === 'SPECIFIC_SHOW' ? 'selected' : ''}>Specific show</option>
            </select>
        </label>
        <label>Target show
            <select id="spotTargetShowSelect" name="targetShowId">
                ${showOptions}
            </select>
            <span class="muted small">${shows.length ? 'Pick the LibreTime show this spot belongs in.' : 'No LibreTime shows loaded yet. Check the LibreTime connection.'}</span>
        </label>
    `;

    openModal(existing ? 'Edit spot' : 'New spot', buildBody(), async (form) => {
        const body = formToJson(form);
        body.lengthSeconds = parseInt(body.lengthSeconds);
        body.librtimeFileId = pickedFile ? pickedFile.id : null;
        body.targetShowId = body.targetShowId === '' ? null : parseInt(body.targetShowId);
        const dps = document.getElementById('spotDayPartSelect');
        if (dps && dps.value) {
            body.dayPartId = dps.value;
            body.localWindowStartMinutes = null;
            body.localWindowEndMinutes = null;
        } else {
            body.dayPartId = null;
            body.localWindowStartMinutes = timeInputToMinutes(document.getElementById('spotWinStart').value);
            body.localWindowEndMinutes = timeInputToMinutes(document.getElementById('spotWinEnd').value);
        }
        if (existing) await API.put('/api/spots/' + existing.id, body);
        else await API.post('/api/orders/' + orderId + '/spots', body);
        toast(existing ? 'Spot updated' : 'Spot created', 'ok');
        await loadSpotsForOrder(orderId);
    });

    const syncSpotDayPartUi = () => {
        const dps = document.getElementById('spotDayPartSelect');
        const custom = !dps || !dps.value;
        ['spotWinStart', 'spotWinEnd'].forEach(id => {
            const el = document.getElementById(id);
            if (el) el.disabled = !custom;
        });
    };
    document.getElementById('spotDayPartSelect').addEventListener('change', syncSpotDayPartUi);
    syncSpotDayPartUi();

    const syncTargetShow = () => {
        const rotation = document.querySelector('[name="rotationKind"]');
        const target = document.getElementById('spotTargetShowSelect');
        const specific = rotation?.value === 'SPECIFIC_SHOW';
        if (target) {
            target.disabled = !specific;
            if (!specific) target.value = '';
        }
    };

    const wireFilePickButtons = () => {
        document.getElementById('spotPickFileBtn').addEventListener('click', async () => {
            const f = await pickFile();
            if (f) {
                pickedFile = f;
                document.getElementById('spotFileLabel').textContent = f.name + (f.length ? ' (' + f.length + ')' : '');
                const labelInput = document.getElementById('spotLabelInput');
                const lengthInput = document.getElementById('spotLengthInput');
                if (labelInput && !labelInput.value.trim()) labelInput.value = f.name || '';
                const seconds = parseLengthSeconds(f.length);
                if (lengthInput && seconds && (!existing || Number(lengthInput.value) === 30)) {
                    lengthInput.value = seconds;
                }
            }
        });
        document.getElementById('spotClearFileBtn').addEventListener('click', () => {
            pickedFile = null;
            document.getElementById('spotFileLabel').innerHTML = '<span class="muted">to-produce</span>';
        });
    };
    document.querySelector('[name="rotationKind"]').addEventListener('change', syncTargetShow);
    syncTargetShow();
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
                    <td data-label="Spot">${escapeHtml(row.spotLabel || '')}</td>
                    <td data-label="Scheduled at">${row.scheduledAt ? new Date(row.scheduledAt).toLocaleString() : ''}</td>
                    <td data-label="Status"><span class="badge ${row.status.toLowerCase()}">${row.status}</span></td>
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
    document.getElementById('dayBuilderPlayback').textContent = '';
    document.getElementById('dayBuilderFulfillment').textContent = '';

    try {
        state.day = await API.get(`/api/stations/${state.currentStationId}/days/${date}`);
        if (!Array.isArray(state.day.clockSegments)) state.day.clockSegments = [];
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
    await loadLibreTimeFileLabels();
    await loadCartsForCurrentStation();
    await loadClocksForCurrentStation();
    await loadPlaybackRows(date, { autoImport: isPastDate(date), quiet: true });
    renderDay();
}

function isPastDate(date) {
    return date < new Date().toISOString().slice(0, 10);
}

async function loadPlaybackRows(date, opts = {}) {
    state.playbackRows = [];
    try {
        let rows = await API.get(`/api/stations/${state.currentStationId}/playback?date=${date}`);
        if ((!rows || !rows.length) && opts.autoImport) {
            await API.post(`/api/stations/${state.currentStationId}/playback/import?date=${date}`, {});
            rows = await API.get(`/api/stations/${state.currentStationId}/playback?date=${date}`);
            if (!opts.quiet) toast(`Pulled ${rows.length} playback rows`, 'ok');
        }
        state.playbackRows = rows || [];
    } catch (e) {
        if (!opts.quiet) toast(e.message, 'error');
        state.playbackRows = [];
    }
    state.fulfillmentRows = [];
    try {
        state.fulfillmentRows = await API.get(`/api/playback/fulfillment?stationId=${state.currentStationId}&date=${date}`) || [];
    } catch { /* non-fatal: fulfillment section stays empty */ }
}

async function loadActiveSpotPool(date) {
    if (!state.orders.length) {
        try { state.orders = await API.get('/api/orders?stationId=' + state.currentStationId); }
        catch { state.orders = []; }
    }
    const active = state.orders.filter(o => orderActiveOnDate(o, date));
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
    document.getElementById('dayBuilderPreloadBtn').disabled = !haveLock || !editable || !state.showInstances.length;
    const segs = Array.isArray(d.clockSegments) ? d.clockSegments : [];
    document.getElementById('dayBuilderApplyClockScheduleBtn').disabled = !haveLock || !editable || !state.showInstances.length || segs.length === 0;
    document.getElementById('dayBuilderPushBtn').disabled = !haveLock || !editable;
    const previewBtn = document.getElementById('dayBuilderPreviewBtn');
    if (previewBtn) previewBtn.disabled = !d.items?.length;
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
                    <button class="link" data-apply-clock="${s.id}">Apply clock</button>
                </div>
            </div>`).join('');
        document.querySelectorAll('[data-add-slot]').forEach(b =>
            b.addEventListener('click', () => addPlaceholderSlot(parseInt(b.dataset.addSlot))));
        document.querySelectorAll('[data-apply-clock]').forEach(b =>
            b.addEventListener('click', () => applyClockToShow(parseInt(b.dataset.applyClock))));
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
        document.querySelectorAll('[data-fill-music-cart]').forEach(b =>
            b.addEventListener('click', () => fillSlotFromCart(parseInt(b.dataset.fillMusicCart), 'music_cart')));
        document.querySelectorAll('[data-fill-commercial-cart]').forEach(b =>
            b.addEventListener('click', () => fillSlotFromCart(parseInt(b.dataset.fillCommercialCart), 'commercial_cart')));
        document.querySelectorAll('[data-remove-slot]').forEach(b =>
            b.addEventListener('click', () => removeSlot(parseInt(b.dataset.removeSlot))));
        document.querySelectorAll('[data-record-vt]').forEach(b =>
            b.addEventListener('click', () => vtRecorderModal(parseInt(b.dataset.recordVt))));
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

    renderPlaybackRows(d.date);
    renderClockScheduleEditor();
}

function renderPlaybackRows(date) {
    renderFulfillmentRows(date);
    const rows = state.playbackRows || [];
    document.getElementById('dayBuilderPlaybackCount').textContent = rows.length ? '(' + rows.length + ')' : '';
    const target = document.getElementById('dayBuilderPlayback');
    if (!rows.length) {
        target.innerHTML = isPastDate(date)
            ? '<div class="empty-state"><strong>No playback imported yet</strong>Click Pull playback to read what LibreTime says played.</div>'
            : '<div class="empty-state"><strong>No as-run yet</strong>Playback rows appear here after airtime, or when you click Pull playback.</div>';
        return;
    }
    target.innerHTML = rows.map(r => {
        const name = r.name || r.fileName || (r.librtimeFileId ? 'File #' + r.librtimeFileId : '(unknown file)');
        const meta = [
            r.playedAt ? new Date(r.playedAt).toLocaleTimeString([], { hour: '2-digit', minute: '2-digit' }) : null,
            r.showName,
            r.librtimeFileId ? 'file ' + r.librtimeFileId : null,
            r.lengthSeconds ? r.lengthSeconds + 's' : null,
        ].filter(Boolean).join(' &middot; ');
        return `<div class="slot track">
            <div>${escapeHtml(name)}</div>
            <div class="slot-meta">${escapeHtml(meta)}</div>
        </div>`;
    }).join('');
}

function renderFulfillmentRows(date) {
    const rows = state.fulfillmentRows || [];
    document.getElementById('dayBuilderFulfillmentCount').textContent = rows.length ? '(' + rows.length + ')' : '';
    const target = document.getElementById('dayBuilderFulfillment');
    if (!rows.length) {
        target.innerHTML = isPastDate(date)
            ? '<div class="empty-state"><strong>No fulfillment data</strong>Pull playback for this date to compute make-ups owed.</div>'
            : '<div class="empty-state"><strong>No fulfillment yet</strong>Per-order fulfillment appears here after airtime.</div>';
        return;
    }
    target.innerHTML = rows.map(r => {
        const title = `${escapeHtml(r.customerName || '(unknown customer)')} &middot; ${escapeHtml(r.orderName || '(unnamed order)')}`;
        const meta = r.missed > 0
            ? `Played ${r.played} of ${r.ordered} &middot; ${r.missed} missed &rarr; make-ups owed: ${r.missedSpotLabels.map(escapeHtml).join(', ')}`
            : `Played ${r.played} of ${r.ordered} &middot; all spots aired`;
        return `<div class="slot track">
            <div>${title}</div>
            <div class="slot-meta">${meta}</div>
        </div>`;
    }).join('');
}

/**
 * Build id -> display label for library files (name, else last path segment).
 * Used so Day Builder slots show a title, not only "TRACK" and a numeric id.
 */
async function loadLibreTimeFileLabels() {
    state.libreTimeFileById = {};
    if (!state.currentStationId) return;
    try {
        const files = await API.get(`/api/stations/${state.currentStationId}/librtime/library?limit=8000`);
        for (const f of (files || [])) {
            if (f && f.id != null) state.libreTimeFileById[f.id] = fileDisplayLabel(f);
        }
    } catch { /* non-fatal: slots still show file id */ }
}

function fileDisplayLabel(f) {
    if (!f) return '';
    const name = (f.name || '').trim();
    if (name) return name;
    const fp = f.filepath || '';
    const base = fp.includes('/') ? fp.split('/').pop() : fp;
    return (base || '').trim() || ('File #' + f.id);
}

function spotLabelById(spotId) {
    if (!spotId || !state.spotPool?.length) return null;
    const p = state.spotPool.find(x => x.spot.id === spotId);
    return p ? p.spot.label : null;
}

function cartLabelById(cartId) {
    if (!cartId) return null;
    const c = state.cartsById[cartId];
    return c ? c.name : null;
}

function slotPrimaryLabel(it, kindLower) {
    if (kindLower === 'music_cart' || kindLower === 'commercial_cart') {
        if (it.cartCategory && !it.cartId) {
            const nice = categoryLabel(it.cartCategory) || it.cartCategory;
            return (kindLower === 'music_cart' ? 'Library' : 'Commercial') + ' by category: ' + nice
                + ' — picks a cart at push from station pool (spot day parts apply)';
        }
        const cart = it.cartId ? state.cartsById[it.cartId] : null;
        const cartName = cart ? cart.name : null;
        const cat = cart ? categoryLabel(cart.category) : (kindLower === 'music_cart' ? 'Library cart' : 'Commercial cart');
        const resolved = it.label ? ' — last picked: ' + it.label : ' — picks at push time';
        return cat + ': ' + (cartName || '(missing cart)') + resolved;
    }
    if (kindLower === 'spot') {
        const lab = spotLabelById(it.spotId) || it.label;
        return lab ? ('Spot: ' + lab) : 'Commercial / traffic spot (pick label below)';
    }
    if (kindLower === 'voicetrack') {
        if (it.librtimeFileId != null) {
            const lab = it.label || (state.libreTimeFileById && state.libreTimeFileById[it.librtimeFileId]);
            return 'Voice track: ' + (lab || ('file #' + it.librtimeFileId));
        }
        return 'Empty VT Slot — record a voice track for this slot';
    }
    if (kindLower === 'track') {
        const lab = (it.librtimeFileId != null ? state.libreTimeFileById[it.librtimeFileId] : null) || it.label;
        if (lab) return 'Music / library: ' + lab;
        return it.librtimeFileId != null
            ? ('Music / library file #' + it.librtimeFileId + ' (see Library tab if unnamed)')
            : 'Music / library track';
    }
    return 'Empty slot — click Use cart, Use spot, or Use track to fill it';
}

function slotHtml(it, idx) {
    const k = (it.kind || 'PLACEHOLDER').toLowerCase();
    const kindNice = k === 'placeholder' ? 'empty' : k.replace('_', ' ');
    const meta = [];
    if (it.scheduledAt) meta.push('Scheduled in LibreTime ' + formatTime(it.scheduledAt));
    if (it.cartId) meta.push('cart id ' + it.cartId.slice(0, 8) + '…');
    if (it.cartCategory) meta.push('category ' + it.cartCategory);
    if (it.spotId) meta.push('spot id ' + it.spotId.slice(0, 8) + '…');
    if (it.librtimeFileId != null) meta.push('LibreTime file id ' + it.librtimeFileId);
    if (it.lengthSeconds) meta.push(it.lengthSeconds + 's');
    const primary = slotPrimaryLabel(it, k);
    const vtEditable = state.day && !state.day.readOnly && state.day.status !== 'PUSHED'
        && state.day.lock && state.day.lock.self;
    const vtButton = k === 'voicetrack'
        ? `<button class="link" data-record-vt="${idx}" ${vtEditable ? '' : 'disabled'}>${it.librtimeFileId != null ? 'Re-record' : 'Record'}</button>`
        : '';
    return `<div class="slot ${k}">
        <div><strong>Slot ${idx + 1}</strong> <span class="muted small">(${kindNice})</span></div>
        <div class="slot-primary">${escapeHtml(primary)}</div>
        <div class="slot-meta">${meta.join(' &middot; ') || '<span class="muted">—</span>'}</div>
        <div class="slot-actions">
            <button class="link" data-fill-music-cart="${idx}">Use library cart</button>
            <button class="link" data-fill-commercial-cart="${idx}">Use commercial cart</button>
            <button class="link" data-fill-spot="${idx}">Use spot</button>
            <button class="link" data-fill-track="${idx}">Use track</button>
            ${vtButton}
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
        cartId: null,
        librtimeFileId: null,
        scheduledAt: null,
        lengthSeconds: 30,
        position: items.length,
        label: null,
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
    state.day.items[idx].cartId = null;
    state.day.items[idx].librtimeFileId = f.id;
    state.day.items[idx].lengthSeconds = parseLengthSeconds(f.length) || 180;
    state.day.items[idx].label = f.name || null;
    renderDay();
}

async function fillSlotFromCart(idx, kind) {
    if (!state.cartsById || !Object.keys(state.cartsById).length) {
        await loadCartsForCurrentStation();
    }
    const wanted = kind === 'music_cart' ? 'MUSIC' : 'COMMERCIAL';
    const list = state.carts.filter(c => c.kind === wanted);
    if (!list.length) {
        const friendly = kind === 'music_cart' ? 'library' : 'commercial';
        toast('No ' + friendly + ' carts yet — create one in the Carts tab.', 'info');
        return;
    }
    const friendly = kind === 'music_cart' ? 'library (music / imaging / content)' : 'commercial';
    const choice = await pickFromList('Pick a ' + friendly + ' cart',
        list.map(c => ({
            id: c.id,
            title: c.name,
            meta: `${categoryLabel(c.category)} · ${c.memberCount} members · separation: ${describePolicy(c.policy)}`,
            _data: c,
        })));
    if (!choice) return;
    const c = choice._data;
    const it = state.day.items[idx];
    it.kind = kind === 'music_cart' ? 'MUSIC_CART' : 'COMMERCIAL_CART';
    it.cartId = c.id;
    it.cartCategory = null;
    it.spotId = null;
    it.librtimeFileId = null;
    it.label = c.name;
    if (!it.lengthSeconds) it.lengthSeconds = (kind === 'commercial_cart' ? 30 : 180);
    renderDay();
}

function describePolicy(p) {
    if (!p) return 'none';
    const parts = [];
    if (p.sameCart) parts.push('cart ' + p.sameCart + 'm');
    if (p.sameArtist) parts.push('artist ' + p.sameArtist + 'm');
    if (p.sameTitle) parts.push('title ' + p.sameTitle + 'm');
    if (p.sameSponsor) parts.push('sponsor ' + p.sameSponsor + 'm');
    if (p.sameProduct) parts.push('product ' + p.sameProduct + 'm');
    return parts.length ? parts.join(', ') : 'none';
}

async function applyClockToShow(showInstanceId) {
    if (!state.day || !state.day.lock || !state.day.lock.self) {
        toast('Acquire the day lock first ("Edit this day").', 'error');
        return;
    }
    if (!state.clocks.length) await loadClocksForCurrentStation();
    if (!state.clocks.length) {
        toast('No clocks yet — create one in the Clocks tab.', 'info');
        return;
    }
    const choice = await pickFromList('Pick a clock to apply', state.clocks.map(c => ({
        id: c.id, title: c.name,
        meta: (c.slots ? c.slots.length + ' slots' : '0 slots') + (c.description ? ' · ' + c.description : ''),
        _data: c,
    })));
    if (!choice) return;
    try {
        state.day = await API.post(
            `/api/days/${state.day.id}/apply-clock?instance=${showInstanceId}&clock=${choice._data.id}`, {});
        toast('Clock applied — Save draft, then Push.', 'ok');
        renderDay();
    } catch (e) { toast(e.message, 'error'); }
}

async function applyClockScheduleToShows() {
    if (!state.day || !state.day.lock || !state.day.lock.self) {
        toast('Acquire the day lock first ("Edit this day").', 'error');
        return;
    }
    const segs = Array.isArray(state.day.clockSegments) ? state.day.clockSegments : [];
    if (!segs.length) {
        toast('Add and save at least one clock schedule row first.', 'info');
        return;
    }
    if (!state.showInstances.length) {
        toast('No show instances for this date.', 'info');
        return;
    }
    if (!confirm('Apply the saved clock schedule? Each show’s slots are replaced by the clock whose time window contains that show’s start (station local). Shows outside every window are left unchanged.')) return;
    try {
        const r = await API.post(`/api/days/${state.day.id}/apply-clock-schedule`, {});
        state.day = r.day;
        const updated = r.instancesUpdated != null ? r.instancesUpdated : 0;
        toast(`Applied schedule to ${updated} show instance(s) — Save draft, then Push.`, 'ok');
        renderDay();
    } catch (e) { toast(e.message, 'error'); }
}

async function saveClockScheduleDraft() {
    if (!state.day || !state.day.lock || !state.day.lock.self) {
        toast('Acquire the day lock first ("Edit this day").', 'error');
        return;
    }
    let segments;
    try {
        segments = readClockScheduleFromDom();
    } catch (err) {
        toast(err.message || 'Invalid clock schedule rows', 'error');
        return;
    }
    try {
        state.day = await API.put(`/api/days/${state.day.id}/clock-schedule`, { segments });
        toast('Clock schedule saved', 'ok');
        renderDay();
    } catch (e) { toast(e.message, 'error'); }
}

function readClockScheduleFromDom() {
    const rows = document.querySelectorAll('.cs-row');
    const out = [];
    rows.forEach(r => {
        const start = timeInputToMinutes(r.querySelector('.cs-start').value);
        if (start === null) throw new Error('Every row needs a start time');
        let endM = timeInputToMinutes(r.querySelector('.cs-end').value);
        if (endM === null) endM = 1440;
        const clockId = r.querySelector('.cs-clock').value;
        if (!clockId) throw new Error('Pick a clock in every row');
        out.push({
            localStartMinutes: start,
            localEndMinutes: endM,
            clockTemplateId: clockId,
        });
    });
    return out;
}

function clockSelectOptionsHtml(selectedId) {
    const list = state.clocks || [];
    if (!list.length) return '<select class="cs-clock"><option value="">(no clocks)</option></select>';
    const opts = list.map(c =>
        `<option value="${c.id}" ${String(selectedId || '') === c.id ? 'selected' : ''}>${escapeHtml(c.name)}</option>`).join('');
    return `<select class="cs-clock">${opts}</select>`;
}

function renderClockScheduleEditor() {
    const rowsEl = document.getElementById('dayBuilderClockScheduleRows');
    const countEl = document.getElementById('dayBuilderClockScheduleCount');
    const addBtn = document.getElementById('dayBuilderClockScheduleAddBtn');
    const saveBtn = document.getElementById('dayBuilderSaveClockScheduleBtn');
    if (!rowsEl || !countEl || !addBtn || !saveBtn) return;
    const d = state.day;
    if (!d) {
        rowsEl.innerHTML = '<span class="muted">Pick a date to load.</span>';
        return;
    }
    if (!Array.isArray(d.clockSegments)) d.clockSegments = [];
    const segs = d.clockSegments;
    countEl.textContent = segs.length ? '(' + segs.length + ')' : '';
    const haveLock = d.lock && d.lock.self;
    const editable = !d.readOnly && d.status !== 'PUSHED';
    addBtn.disabled = !haveLock || !editable;
    saveBtn.disabled = !haveLock || !editable;
    const applyBtn = document.getElementById('dayBuilderApplyClockScheduleBtn');
    if (applyBtn) {
        applyBtn.disabled = !haveLock || !editable || !state.showInstances?.length || segs.length === 0;
    }

    if (!segs.length) {
        rowsEl.innerHTML = '<div class="muted">No rows yet — click Add row, set start/end and clock, then Save clock schedule.</div>';
        return;
    }
    rowsEl.innerHTML = segs.map((s, i) => `
        <div class="cs-row" data-cs-idx="${i}" style="display:flex;flex-wrap:wrap;gap:8px;align-items:center;margin-bottom:8px">
            <span class="muted">${i + 1}.</span>
            <label class="inline small">Start <input type="time" class="cs-start" value="${escapeAttr(minutesToTimeInput(s.localStartMinutes))}" /></label>
            <label class="inline small">End <input type="time" class="cs-end" value="${escapeAttr(s.localEndMinutes === 1440 ? '' : minutesToTimeInput(s.localEndMinutes))}" /></label>
            ${clockSelectOptionsHtml(s.clockTemplateId)}
            <button type="button" class="link cs-remove" data-cs-remove="${i}">Remove</button>
        </div>`).join('');
    rowsEl.querySelectorAll('[data-cs-remove]').forEach(b =>
        b.addEventListener('click', () => {
            const idx = parseInt(b.dataset.csRemove, 10);
            state.day.clockSegments.splice(idx, 1);
            renderClockScheduleEditor();
        }));
}

async function previewPush() {
    if (!state.day) return;
    try {
        const rows = await API.get(`/api/days/${state.day.id}/preview`);
        const lines = rows.map((r, i) => {
            const head = `${i + 1}. ${r.kind}`;
            const what = r.cartName
                ? `cart "${r.cartName}" → ${r.label || ('file #' + (r.librtimeFileId || '?'))}`
                : (r.label || ('file #' + (r.librtimeFileId || '?')));
            const text = head + ' — ' + what + (r.note ? ` ⚠ ${r.note}` : '');
            return r.violation
                ? `<span class="preview-violation">${escapeHtml(text)}</span>`
                : escapeHtml(text);
        });
        const html = '<pre style="max-height:55vh;overflow:auto;white-space:pre-wrap">'
            + lines.join('\n') + '</pre>';
        openModal('Preview push (dry run)', html, () => true);
    } catch (e) { toast(e.message, 'error'); }
}

function removeSlot(idx) {
    state.day.items.splice(idx, 1);
    state.day.items.forEach((it, i) => it.position = i);
    renderDay();
}

/**
 * Voice-track recorder modal (Phase 4): capture a take with MediaRecorder, preview it,
 * set segue/duck markers, then upload to /api/voicetracks which transcodes, ships to
 * Jazz, and links the imported file to the schedule item.
 */
function vtRecorderModal(idx) {
    const it = state.day && state.day.items ? state.day.items[idx] : null;
    if (!it || String(it.kind || '').toUpperCase() !== 'VOICETRACK') return;
    if (!it.id) {
        toast('Save the day first so this slot exists on the server.', 'error');
        return;
    }
    if (!navigator.mediaDevices || !navigator.mediaDevices.getUserMedia || typeof MediaRecorder === 'undefined') {
        toast('Audio recording is not supported in this browser (needs getUserMedia + MediaRecorder).', 'error');
        return;
    }
    const html = `
        <p class="muted small">Slot ${idx + 1} &middot; ${escapeHtml(it.label || 'Voice track')}</p>
        <div class="row" style="display:flex;gap:8px;margin-bottom:8px">
            <button type="button" class="secondary" name="vtRecord">Record</button>
            <button type="button" class="secondary" name="vtStop" disabled>Stop</button>
        </div>
        <audio name="vtPreview" controls style="width:100%;display:none;margin-bottom:8px"></audio>
        <label>Host <input name="host" value="${escapeAttr((state.user && (state.user.name || state.user.email)) || '')}" required /></label>
        <label>Segue offset (seconds) <input name="segueOffsetSeconds" type="number" min="0" step="1" value="${it.segueOffsetSeconds ?? 0}" /></label>
        <label>Duck (dB) <input name="duckDb" type="number" step="0.5" value="${it.duckDb ?? 0}" /></label>
    `;
    let stream = null, recorder = null, chunks = [], takeBlob = null;
    const stopStream = () => {
        if (stream) { stream.getTracks().forEach(t => t.stop()); stream = null; }
    };
    openModal('Record voice track', html, async (form) => {
        if (!takeBlob) throw new Error('Record a take first.');
        const fd = new FormData();
        fd.append('file', takeBlob, 'take.webm');
        fd.append('scheduleItemId', it.id);
        fd.append('host', form.querySelector('[name="host"]').value.trim());
        fd.append('segueOffsetSeconds', form.querySelector('[name="segueOffsetSeconds"]').value || '0');
        fd.append('duckDb', form.querySelector('[name="duckDb"]').value || '0');
        const saved = await API.upload('/api/voicetracks', fd);
        Object.assign(it, saved);
        renderDay();
        toast('Voice track saved', 'ok');
        stopStream();
    });
    const body = document.getElementById('modalBody');
    const recBtn = body.querySelector('[name="vtRecord"]');
    const stopBtn = body.querySelector('[name="vtStop"]');
    const preview = body.querySelector('[name="vtPreview"]');
    const cancelBtn = document.getElementById('modalCancel');
    const prevCancel = cancelBtn.onclick;
    cancelBtn.onclick = () => { stopStream(); if (prevCancel) prevCancel(); };
    recBtn.addEventListener('click', async () => {
        try {
            stream = await navigator.mediaDevices.getUserMedia({ audio: true });
        } catch (e) {
            toast('Microphone unavailable: ' + e.message, 'error');
            return;
        }
        chunks = [];
        const mime = MediaRecorder.isTypeSupported('audio/webm;codecs=opus') ? 'audio/webm;codecs=opus' : '';
        recorder = new MediaRecorder(stream, mime ? { mimeType: mime } : undefined);
        recorder.ondataavailable = ev => { if (ev.data && ev.data.size) chunks.push(ev.data); };
        recorder.onstop = () => {
            takeBlob = new Blob(chunks, { type: recorder.mimeType || 'audio/webm' });
            preview.src = URL.createObjectURL(takeBlob);
            preview.style.display = 'block';
            stopStream();
        };
        recorder.start();
        recBtn.disabled = true;
        stopBtn.disabled = false;
    });
    stopBtn.addEventListener('click', () => {
        if (recorder && recorder.state !== 'inactive') recorder.stop();
        recBtn.disabled = false;
        stopBtn.disabled = true;
    });
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
                cartId: it.cartId,
                cartCategory: it.cartCategory || null,
                label: it.label,
                segueOffsetSeconds: it.segueOffsetSeconds ?? null,
                duckDb: it.duckDb ?? null,
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
        const r = await API.post(`/api/days/${state.day.id}/push`, {});
        let msg = `Pushed ${r.rowsWritten} row${r.rowsWritten === 1 ? '' : 's'} to ${r.instancesTouched} show instance${r.instancesTouched === 1 ? '' : 's'}`;
        if (r.rowsResolved) msg += ` (${r.rowsResolved} resolved from carts)`;
        if (r.rowsSkipped) msg += ` (skipped ${r.rowsSkipped})`;
        toast(msg, r.rowsSkipped ? 'info' : 'ok');
        if (r.notes && r.notes.length) console.warn('Push notes:', r.notes);
        await loadDay();
    } catch (e) { toast('Push failed: ' + e.message, 'error'); }
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
        await loadPlaybackRows(date, { quiet: true });
        renderPlaybackRows(date);
        toast(`Imported ${r.entriesSaved} playback entries (${r.matched} matched, ${r.missed} missed)`, 'ok');
    } catch (e) { toast(e.message, 'error'); }
}

async function preloadFromLibreTime() {
    const date = document.getElementById('dayBuilderDate').value;
    if (!date || !state.day) return;
    const hasItems = (state.day.items || []).length > 0;
    const replace = hasItems && confirm('Replace the existing LibreLog slots for this day with the current LibreTime schedule?');
    if (hasItems && !replace) return;
    try {
        state.day = await API.post(`/api/stations/${state.currentStationId}/days/${date}/preload?replace=${replace}`, {});
        renderDay();
        toast(`Preloaded ${state.day.items.length} slots from LibreTime`, 'ok');
    } catch (e) {
        toast(e.message, 'error');
    }
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

function truncateMiddle(s, max) {
    if (!s || s.length <= max) return s;
    const half = Math.floor((max - 1) / 2);
    return s.slice(0, half) + '…' + s.slice(s.length - half);
}

/**
 * Multi-select library files with rich columns; POSTs /members/bulk-files.
 * @returns {Promise<object|null>} bulk API result or null if cancelled / error before open
 */
async function openLibraryBulkPicker(cart) {
    const dlg = document.getElementById('libraryBulkModal');
    const searchEl = document.getElementById('libraryBulkSearch');
    const wrap = document.getElementById('libraryBulkTableWrap');

    let files = [];
    try {
        files = await API.get(`/api/stations/${state.currentStationId}/librtime/library?limit=8000`);
    } catch (e) {
        toast('Couldn\'t reach LibreTime: ' + e.message, 'error');
        return null;
    }
    if (!files.length) {
        toast('LibreTime library is empty.', 'info');
        return null;
    }

    const selected = new Set();
    let settled = false;

    function haystack(f) {
        return [f.id, f.name, f.artist, f.title, f.album, f.genre, f.filepath, f.length, f.mime, f.description]
            .filter(x => x != null && x !== '')
            .join(' ')
            .toLowerCase();
    }

    function visibleFiles() {
        const q = (searchEl.value || '').trim().toLowerCase();
        if (!q) return files;
        return files.filter(f => haystack(f).includes(q));
    }

    function updateChrome(visLen) {
        document.getElementById('libraryBulkCount').textContent =
            `${selected.size} selected · ${visLen} row${visLen === 1 ? '' : 's'} match filter · ${files.length} files loaded`;
        const btn = document.getElementById('libraryBulkAdd');
        btn.disabled = selected.size === 0;
        btn.textContent = selected.size ? `Add ${selected.size} to cart` : 'Add to cart';
    }

    function render() {
        const vis = visibleFiles();
        const thead = `<thead><tr>
            <th class="chk-col"><input type="checkbox" id="libraryBulkMaster" title="Toggle all visible rows" /></th>
            <th>ID</th><th>Title / name</th><th>Artist</th><th>Album</th><th>Genre</th><th>Len</th><th>Path</th>
        </tr></thead>`;
        const tbody = vis.map(f => {
            const dispTitle = (f.title || f.name || '').trim() || '—';
            const ck = selected.has(f.id) ? 'checked' : '';
            return `<tr data-fid="${f.id}" class="library-bulk-row">
                <td><input type="checkbox" class="library-bulk-cb" data-fid="${f.id}" ${ck} /></td>
                <td class="mono">${f.id}</td>
                <td>${escapeHtml(dispTitle)}</td>
                <td>${escapeHtml(f.artist || '—')}</td>
                <td>${escapeHtml(f.album || '—')}</td>
                <td>${escapeHtml(f.genre || '—')}</td>
                <td class="mono">${escapeHtml(f.length || '—')}</td>
                <td class="filepath-cell" title="${escapeAttr(f.filepath || '')}">${escapeHtml(truncateMiddle((f.filepath || '').replace(/^.*\//, '') || f.filepath || '—', 56))}</td>
            </tr>`;
        }).join('');
        wrap.innerHTML = `
            <div class="library-bulk-scroll">
                <table class="data-table library-bulk-table">${thead}<tbody>${tbody}</tbody></table>
            </div>`;

        const master = document.getElementById('libraryBulkMaster');
        if (master && vis.length) {
            const allOn = vis.every(f => selected.has(f.id));
            const someOn = vis.some(f => selected.has(f.id));
            master.checked = allOn;
            master.indeterminate = someOn && !allOn;
            master.onchange = () => {
                if (master.checked) vis.forEach(f => selected.add(f.id));
                else vis.forEach(f => selected.delete(f.id));
                render();
            };
        }

        wrap.querySelectorAll('.library-bulk-cb').forEach(cb => {
            cb.onchange = () => {
                const id = parseInt(cb.dataset.fid, 10);
                if (cb.checked) selected.add(id); else selected.delete(id);
                render();
            };
        });
        wrap.querySelectorAll('.library-bulk-row').forEach(tr => {
            tr.onclick = (ev) => {
                if (ev.target.closest('input')) return;
                const id = parseInt(tr.dataset.fid, 10);
                if (selected.has(id)) selected.delete(id); else selected.add(id);
                render();
            };
        });
        updateChrome(vis.length);
    }

    return new Promise((resolve) => {
        const done = (value) => {
            if (settled) return;
            settled = true;
            dlg.removeEventListener('close', onClose);
            searchEl.oninput = null;
            resolve(value);
        };

        const onClose = () => {
            if (!settled) done(null);
        };

        searchEl.value = '';
        searchEl.oninput = () => render();

        document.getElementById('libraryBulkSelectVisible').onclick = () => {
            visibleFiles().forEach(f => selected.add(f.id));
            render();
        };
        document.getElementById('libraryBulkClear').onclick = () => {
            selected.clear();
            render();
        };

        document.getElementById('libraryBulkCancel').onclick = () => {
            done(null);
            dlg.close();
        };

        document.getElementById('libraryBulkAdd').onclick = async () => {
            if (!selected.size) return;
            try {
                const r = await API.post(`/api/carts/${cart.id}/library-batch`, { fileIds: [...selected] });
                let msg = `Added ${r.added} file${r.added === 1 ? '' : 's'}`;
                if (r.skippedAlreadyInCart) msg += ` · skipped ${r.skippedAlreadyInCart} already in cart`;
                if (r.skippedNotFound) msg += ` · ${r.skippedNotFound} not found`;
                if (r.skippedDuplicateInRequest) msg += ` · ${r.skippedDuplicateInRequest} duplicate id in request`;
                toast(msg, r.added ? 'ok' : 'info');
                done(r);
                dlg.close();
            } catch (e) {
                toast(e.message, 'error');
            }
        };

        dlg.addEventListener('close', onClose);
        render();
        dlg.showModal();
        setTimeout(() => searchEl.focus(), 50);
    });
}

async function cartAddMusicMember(cart) {
    const r = await openLibraryBulkPicker(cart);
    if (r != null) await renderCartDetail(cart.id);
}

// ---------- Audio Uploads (Rumble → Jazz) ----------
function formatDurationSeconds(s) {
    if (s == null) return '';
    const m = Math.floor(s / 60);
    const sec = s % 60;
    return m + ':' + String(sec).padStart(2, '0');
}

async function loadMediaUploads() {
    const form = document.getElementById('mediaUploadForm');
    if (form && !form.dataset.wired) {
        form.dataset.wired = '1';
        form.addEventListener('submit', submitMediaUpload);
    }
    if (!state.currentStationId) return;
    state.mediaUploads = await API.get('/api/media/uploads?stationId=' + state.currentStationId);
    renderMediaUploads();
}

function renderMediaUploads() {
    const tbody = document.getElementById('mediaUploadsBody');
    if (!state.mediaUploads.length) {
        tbody.innerHTML = '<tr><td colspan="6"><div class="empty-state"><strong>No uploads yet</strong>Upload a voice track to transcode it and hand it off to Jazz.</div></td></tr>';
        return;
    }
    tbody.innerHTML = state.mediaUploads.map(u => `
        <tr>
            <td data-label="Name">${escapeHtml(u.originalFileName)}</td>
            <td data-label="Artist / Title">${escapeHtml(u.artistTag || '')}${u.artistTag && u.titleTag ? ' — ' : ''}${escapeHtml(u.titleTag || '')}</td>
            <td data-label="Length">${escapeHtml(formatDurationSeconds(u.durationSeconds))}</td>
            <td data-label="Status"><span class="badge">${escapeHtml(u.status)}</span></td>
            <td data-label="Error" class="muted small">${escapeHtml(u.error || '')}</td>
            <td data-label="Created">${escapeHtml(formatTime(u.createdAt))}</td>
        </tr>`).join('');
}

async function submitMediaUpload(e) {
    e.preventDefault();
    const form = e.target;
    const fileInput = form.querySelector('input[name="file"]');
    if (!fileInput.files.length) { toast('Pick an audio file first', 'error'); return; }
    const fd = new FormData();
    fd.append('file', fileInput.files[0]);
    fd.append('artist', form.querySelector('input[name="artist"]').value.trim());
    fd.append('title', form.querySelector('input[name="title"]').value.trim());
    fd.append('stationId', state.currentStationId);
    try {
        const row = await API.upload('/api/media/uploads', fd);
        if (row.status === 'FAILED') toast('Upload failed: ' + (row.error || 'unknown error'), 'error');
        else if (row.status === 'IMPORTED') toast('Uploaded and imported into Jazz', 'ok');
        else toast('Uploaded (not sent to Jazz)', 'ok');
        form.reset();
        await loadMediaUploads();
    } catch (err) {
        toast(err.message, 'error');
    }
}

// ---------- Tabs ----------
const tabLoaders = {
    dayBuilder: { title: 'Day Builder', load: initDayBuilder },
    carts: { title: 'Carts', load: loadCartsTab },
    clocks: { title: 'Clocks', load: loadClocksTab },
    library: { title: 'LibreTime Library', load: loadLibraryTab },
    media: { title: 'Audio Uploads', load: loadMediaUploads },
    customers: { title: 'Customers', load: loadCustomers },
    orders: { title: 'Orders & Spots', load: loadOrders },
    stations: { title: 'Station & day parts', load: loadStationsTab },
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
    closeSidebar();
}

// ---------- Theme ----------
function applyTheme(pref) {
    // pref is 'light' | 'dark' | 'auto' (or null = auto)
    const mode = pref || 'auto';
    let effective = mode;
    if (mode === 'auto') {
        effective = window.matchMedia('(prefers-color-scheme: light)').matches ? 'light' : 'dark';
    }
    document.documentElement.setAttribute('data-theme', effective);
    const themeColor = effective === 'light' ? '#ffffff' : '#0f172a';
    let meta = document.querySelector('meta[name="theme-color"]');
    if (meta) meta.setAttribute('content', themeColor);
    document.querySelectorAll('[data-theme-set]').forEach(b =>
        b.classList.toggle('active', b.dataset.themeSet === mode));
}

function setTheme(pref) {
    if (pref === 'auto') localStorage.removeItem('librelog.theme');
    else localStorage.setItem('librelog.theme', pref);
    // Store the preference itself separately so the toolbar can show "auto" as active.
    localStorage.setItem('librelog.themePref', pref);
    applyTheme(pref);
}

function initTheme() {
    const pref = localStorage.getItem('librelog.themePref') || 'auto';
    applyTheme(pref);
    // Re-apply when the system preference changes (only matters in auto mode).
    if (window.matchMedia) {
        const mql = window.matchMedia('(prefers-color-scheme: light)');
        const handler = () => {
            const p = localStorage.getItem('librelog.themePref') || 'auto';
            if (p === 'auto') applyTheme('auto');
        };
        if (mql.addEventListener) mql.addEventListener('change', handler);
        else if (mql.addListener) mql.addListener(handler);
    }
}

// ---------- Sidebar drawer (mobile) ----------
function openSidebar() { document.body.classList.add('sidebar-open'); }
function closeSidebar() { document.body.classList.remove('sidebar-open'); }
function toggleSidebar() { document.body.classList.toggle('sidebar-open'); }

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

/** HTML time input → minutes from midnight, or null if empty. */
function timeInputToMinutes(v) {
    if (v == null || v === '') return null;
    const parts = String(v).split(':');
    if (parts.length < 2) return null;
    const h = parseInt(parts[0], 10);
    const m = parseInt(parts[1], 10);
    if (Number.isNaN(h) || Number.isNaN(m)) return null;
    return h * 60 + m;
}

function minutesToTimeInput(min) {
    if (min == null) return '';
    const h = Math.floor(min / 60) % 24;
    const mi = min % 60;
    return String(h).padStart(2, '0') + ':' + String(mi).padStart(2, '0');
}

/** Half-open [start,end) local window for table display. */
function formatLocalWindowCell(startM, endM) {
    if (startM == null || endM == null) return '—';
    const endLabel = endM === 1440 ? '24:00' : minutesToTimeInput(endM);
    return minutesToTimeInput(startM) + '–' + endLabel;
}

async function loadDayPartsForStation(stationId) {
    if (!stationId) {
        state.dayParts = [];
        state.dayPartsStationId = null;
        return;
    }
    try {
        state.dayParts = await API.get('/api/stations/' + stationId + '/day-parts');
        state.dayPartsStationId = stationId;
    } catch (e) {
        state.dayParts = [];
        state.dayPartsStationId = null;
        toast('Could not load day parts: ' + e.message, 'error');
    }
}

function dayPartSelectOptionsHtml(selectedId) {
    let html = '<option value="">Custom times…</option>';
    for (const dp of (state.dayParts || [])) {
        const sel = String(selectedId || '') === dp.id ? ' selected' : '';
        const label = escapeHtml(dp.name) + ' — ' + escapeHtml(formatLocalWindowCell(dp.startMinutes, dp.endMinutes));
        html += `<option value="${escapeAttr(dp.id)}"${sel}>${label}</option>`;
    }
    return html;
}

function spotWindowCell(s) {
    if (s.dayPartId) {
        const dp = (state.dayParts || []).find(d => d.id === s.dayPartId);
        if (dp) {
            return `${escapeHtml(dp.name)} <span class="muted small">(${escapeHtml(formatLocalWindowCell(dp.startMinutes, dp.endMinutes))})</span>`;
        }
        return '<span class="muted">Day part</span>';
    }
    return escapeHtml(formatLocalWindowCell(s.localWindowStartMinutes, s.localWindowEndMinutes));
}

function renderDayPartsPanel() {
    const body = document.getElementById('dayPartsBody');
    const countEl = document.getElementById('dayPartsListCount');
    if (!body || !countEl) return;
    const sid = state.currentStationId;
    if (!sid) {
        countEl.textContent = '';
        body.innerHTML = '<tr><td colspan="5"><span class="muted">Select a station in the header.</span></td></tr>';
        return;
    }
    countEl.textContent = state.dayParts.length ? '(' + state.dayParts.length + ')' : '';
    if (!state.dayParts.length) {
        body.innerHTML = '<tr><td colspan="5"><div class="empty-state"><strong>No day parts yet</strong>Create named windows (e.g. Morning drive, Midday) then pick them on commercial spots.</div></td></tr>';
    } else {
        body.innerHTML = state.dayParts.map(dp => `
            <tr>
                <td data-label="Name">${escapeHtml(dp.name)}</td>
                <td data-label="Start">${escapeHtml(minutesToTimeInput(dp.startMinutes))}</td>
                <td data-label="End">${escapeHtml(dp.endMinutes === 1440 ? '24:00 (exclusive)' : minutesToTimeInput(dp.endMinutes))}</td>
                <td data-label="Sort">${dp.sortOrder}</td>
                <td>
                    <button type="button" class="link" data-day-part-edit="${dp.id}">Edit</button>
                    <button type="button" class="link danger" data-day-part-del="${dp.id}">Delete</button>
                </td>
            </tr>`).join('');
        body.querySelectorAll('[data-day-part-edit]').forEach(b =>
            b.addEventListener('click', () => dayPartModal(state.dayParts.find(d => d.id === b.dataset.dayPartEdit), sid)));
        body.querySelectorAll('[data-day-part-del]').forEach(b =>
            b.addEventListener('click', async () => {
                if (!confirm('Delete this day part? Slots and spots using it will fall back to custom or no window.')) return;
                try {
                    await API.del('/api/stations/' + sid + '/day-parts/' + b.dataset.dayPartDel);
                    toast('Day part deleted', 'ok');
                    await loadDayPartsForStation(sid);
                    renderDayPartsPanel();
                } catch (e) { toast(e.message, 'error'); }
            }));
    }
    const nb = document.getElementById('dayPartNewBtn');
    if (nb && !nb.dataset.wired) {
        nb.dataset.wired = '1';
        nb.addEventListener('click', () => dayPartModal(null, sid));
    }
}

async function dayPartModal(existing, stationId) {
    const buildBody = () => `
        <label>Name <input id="dpName" required maxlength="200" value="${escapeAttr(existing?.name || '')}" /></label>
        <label>Start (inclusive) <input type="time" id="dpStart" value="${escapeAttr(minutesToTimeInput(existing?.startMinutes))}" /></label>
        <label>End (exclusive) <input type="time" id="dpEnd" value="${escapeAttr(existing?.endMinutes === 1440 ? '' : minutesToTimeInput(existing?.endMinutes))}" /></label>
        <label>Sort order <input type="number" id="dpSort" value="${existing != null ? existing.sortOrder : 0}" /></label>
        <p class="muted small">Times are station-local. End is exclusive. For <strong>through midnight</strong> (e.g. 7pm–12am), set end to <strong>12:00 AM</strong> — it is stored as end-of-day.</p>
    `;
    openModal(existing ? 'Edit day part' : 'New day part', buildBody(), async () => {
        const name = document.getElementById('dpName').value.trim();
        const startMinutes = timeInputToMinutes(document.getElementById('dpStart').value);
        const endMinutes = timeInputToMinutes(document.getElementById('dpEnd').value);
        const sortOrder = parseInt(document.getElementById('dpSort').value, 10);
        if (!name) throw new Error('Name is required');
        if (startMinutes == null || endMinutes == null) {
            throw new Error('Start and end times are required');
        }
        const body = { name, startMinutes, endMinutes, sortOrder: Number.isNaN(sortOrder) ? 0 : sortOrder };
        if (existing) await API.put('/api/stations/' + stationId + '/day-parts/' + existing.id, body);
        else await API.post('/api/stations/' + stationId + '/day-parts', body);
        toast(existing ? 'Day part updated' : 'Day part created', 'ok');
        await loadDayPartsForStation(stationId);
        renderDayPartsPanel();
    });
}

// ---------- Header clock ----------
let headerClockTimer = null;

function currentStationTimeZone() {
    if (!state.currentStationId) return null;
    const s = state.stations.find(x => x.id === state.currentStationId);
    return s ? s.timeZone : null;
}

function tickHeaderClock() {
    const now = new Date();
    const tz = currentStationTimeZone() || Intl.DateTimeFormat().resolvedOptions().timeZone || 'UTC';
    document.getElementById('clockStationTz').textContent = tz;
    try {
        document.getElementById('clockStationTime').textContent = now.toLocaleString([], {
            timeZone: tz, weekday: 'short',
            month: 'short', day: 'numeric',
            hour: '2-digit', minute: '2-digit', second: '2-digit',
        });
    } catch {
        document.getElementById('clockStationTime').textContent = now.toString();
    }
    document.getElementById('clockUtcTime').textContent = now.toLocaleString([], {
        timeZone: 'UTC', weekday: 'short',
        month: 'short', day: 'numeric',
        hour: '2-digit', minute: '2-digit', second: '2-digit',
    }) + ' UTC';
}

function startHeaderClock() {
    if (headerClockTimer) clearInterval(headerClockTimer);
    tickHeaderClock();
    headerClockTimer = setInterval(tickHeaderClock, 1000);
}

// ---------- Carts ----------

async function loadCartsForCurrentStation() {
    if (!state.currentStationId) { state.carts = []; state.cartsById = {}; return; }
    try {
        state.carts = await API.get(`/api/stations/${state.currentStationId}/carts`);
        state.cartsById = {};
        for (const c of state.carts) state.cartsById[c.id] = c;
    } catch (e) {
        state.carts = []; state.cartsById = {};
        toast('Could not load carts: ' + e.message, 'error');
    }
    if (!state.cartCategories.library?.length) {
        try { state.cartCategories = await API.get('/api/cart-categories'); }
        catch (e) { /* keep defaults */ }
    }
}

async function loadCartsTab() {
    await loadCartsForCurrentStation();
    populateCategoryFilter();
    renderCartList();
    if (state.selectedCartId && state.cartsById[state.selectedCartId]) {
        await renderCartDetail(state.selectedCartId);
    } else {
        state.selectedCartId = null;
        document.getElementById('cartDetail').innerHTML =
            '<div class="empty-state"><strong>No cart selected</strong>Pick a cart on the left, or create a new one.</div>';
    }
}

function populateCategoryFilter() {
    const sel = document.getElementById('cartFilterCategory');
    if (!sel) return;
    const all = [...state.cartCategories.library, ...state.cartCategories.commercial];
    const current = sel.value;
    sel.innerHTML = '<option value="">All categories</option>'
        + all.map(c => `<option value="${c}">${escapeHtml(categoryLabel(c))}</option>`).join('');
    sel.value = current;
}

function renderCartList() {
    const filter = document.getElementById('cartFilterCategory')?.value || '';
    const list = state.carts.filter(c => !filter || c.category === filter);
    document.getElementById('cartListCount').textContent = list.length ? '(' + list.length + ')' : '';
    if (!list.length) {
        document.getElementById('cartList').innerHTML =
            '<div class="empty-state"><strong>No carts yet</strong>Click "New cart" to get started.</div>';
        return;
    }
    document.getElementById('cartList').innerHTML = list.map(c => `
        <div class="cart-row ${state.selectedCartId === c.id ? 'selected' : ''}" data-cart-id="${c.id}">
            <div class="cart-row-name"><strong>${escapeHtml(c.name)}</strong>
                <span class="cart-kind ${(c.category || c.kind).toLowerCase()}">${escapeHtml(categoryLabel(c.category) || c.kind)}</span>
                ${c.source === 'ORDER' ? '<span class="cart-kind order">order</span>' : ''}
            </div>
            <div class="muted small">${c.memberCount} member${c.memberCount === 1 ? '' : 's'}
                · separation: ${escapeHtml(describePolicy(c.policy))}</div>
        </div>`).join('');
    document.querySelectorAll('[data-cart-id]').forEach(el =>
        el.addEventListener('click', () => {
            state.selectedCartId = el.dataset.cartId;
            renderCartList();
            renderCartDetail(el.dataset.cartId);
        }));
}

async function renderCartDetail(cartId) {
    const cart = state.cartsById[cartId];
    if (!cart) return;
    let members = [];
    try { members = await API.get(`/api/carts/${cartId}/members`); }
    catch (e) { toast('Members load failed: ' + e.message, 'error'); }
    state.cartMembers = members;
    const isMusic = cart.kind === 'MUSIC';
    const isOrder = cart.source === 'ORDER';
    const policy = cart.policy || {};

    const memberRows = members.length
        ? members.map((m, i) => `
            <tr data-member-id="${m.id}">
                <td>${i + 1}</td>
                <td>${m.weight}</td>
                <td>${escapeHtml(m.artist || '')}</td>
                <td>${escapeHtml(m.title || '')}</td>
                <td>${escapeHtml(m.sponsor || '')}</td>
                <td>${escapeHtml(m.product || '')}</td>
                <td>${m.lengthSeconds || ''}${m.lengthSeconds ? 's' : ''}</td>
                <td>${m.librtimeFileId ?? ''}${m.spotId ? ' (spot)' : ''}</td>
                <td>${m.enabled ? 'on' : 'off'}</td>
                <td>
                    <button class="link" data-edit-member="${m.id}">edit</button>
                    ${isOrder ? '' : `<button class="link" data-delete-member="${m.id}">remove</button>`}
                </td>
            </tr>`).join('')
        : `<tr><td colspan="10" class="muted">No members yet — ${isOrder
            ? 'add spots to the order or click "Sync from order".'
            : 'click "Add member" to start.'}</td></tr>`;

    document.getElementById('cartDetail').innerHTML = `
        <div class="row">
            <h3>${escapeHtml(cart.name)}
                <span class="cart-kind ${(cart.category || cart.kind).toLowerCase()}">${escapeHtml(categoryLabel(cart.category) || cart.kind)}</span>
                ${isOrder ? '<span class="cart-kind order">order-backed</span>' : ''}
            </h3>
            <span class="grow"></span>
            <button class="link" data-cart-rename="${cart.id}">Edit</button>
            <button class="link danger" data-cart-delete="${cart.id}">Delete</button>
        </div>
        <p class="muted small">${escapeHtml(cart.description || '')}</p>

        <h4>Separation policy <span class="muted small">(minutes between repeats; 0 = no rule)</span></h4>
        <form id="cartPolicyForm" class="form-inline">
            <label>Same cart <input type="number" min="0" name="sameCart" value="${policy.sameCart || 0}" /></label>
            <label>Same artist <input type="number" min="0" name="sameArtist" value="${policy.sameArtist || 0}" /></label>
            <label>Same title <input type="number" min="0" name="sameTitle" value="${policy.sameTitle || 0}" /></label>
            <label>Same sponsor <input type="number" min="0" name="sameSponsor" value="${policy.sameSponsor || 0}" /></label>
            <label>Same product <input type="number" min="0" name="sameProduct" value="${policy.sameProduct || 0}" /></label>
            <button type="submit">Save policy</button>
        </form>

        <h4>Freshness <span class="muted small">(how this cart picks its next play)</span></h4>
        <form id="cartFreshnessForm" class="form-inline">
            <label>Strategy
                <select name="selectionStrategy">
                    <option value="ROTATION" ${cart.selectionStrategy !== 'NEWEST_FIRST' ? 'selected' : ''}>Rotation (round-robin)</option>
                    <option value="NEWEST_FIRST" ${cart.selectionStrategy === 'NEWEST_FIRST' ? 'selected' : ''}>Newest first</option>
                </select>
            </label>
            <label>Max age (hours) <input type="number" min="1" name="maxAgeHours" placeholder="no limit" value="${cart.maxAgeHours ?? ''}" /></label>
            <button type="submit">Save freshness</button>
        </form>
        <p class="muted small">For news and voicetracks pick <strong>Newest first</strong> with a max age (e.g. 6h) so stale audio never airs. Leave <strong>Rotation</strong> for music and commercials.</p>

        <h4>Members <span class="muted small">${members.length}</span></h4>
        <div class="row">
            ${isOrder
                ? `<button class="secondary" id="cartSyncOrderBtn">Sync from order</button>`
                : (isMusic
                    ? `<button id="cartAddMusicBtn">+ Add music file</button>`
                    : `<button id="cartAddSpotBtn">+ Add spot</button>`)}
            <span class="muted small">Rotation pointer: ${cart.rotationPointer}</span>
        </div>
        <table class="data-table">
            <thead><tr>
                <th>#</th><th>Wt</th><th>Artist</th><th>Title</th><th>Sponsor</th><th>Product</th>
                <th>Len</th><th>File / spot</th><th>On</th><th></th>
            </tr></thead>
            <tbody>${memberRows}</tbody>
        </table>
    `;

    document.getElementById('cartPolicyForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        const body = {
            sameCart: parseInt(fd.get('sameCart')) || 0,
            sameArtist: parseInt(fd.get('sameArtist')) || 0,
            sameTitle: parseInt(fd.get('sameTitle')) || 0,
            sameSponsor: parseInt(fd.get('sameSponsor')) || 0,
            sameProduct: parseInt(fd.get('sameProduct')) || 0,
        };
        try {
            await API.put(`/api/carts/${cart.id}/policy`, body);
            toast('Separation policy saved', 'ok');
            await loadCartsForCurrentStation();
            renderCartList();
            await renderCartDetail(cart.id);
        } catch (err) { toast(err.message, 'error'); }
    });
    document.getElementById('cartFreshnessForm').addEventListener('submit', async (e) => {
        e.preventDefault();
        const fd = new FormData(e.target);
        const maxRaw = (fd.get('maxAgeHours') || '').trim();
        const body = {
            selectionStrategy: fd.get('selectionStrategy'),
            // -1 tells the API to clear the limit; otherwise the parsed hours.
            maxAgeHours: maxRaw === '' ? -1 : parseInt(maxRaw),
        };
        try {
            await API.put(`/api/carts/${cart.id}`, body);
            toast('Freshness saved', 'ok');
            await loadCartsForCurrentStation();
            renderCartList();
            await renderCartDetail(cart.id);
        } catch (err) { toast(err.message, 'error'); }
    });
    document.querySelector('[data-cart-rename]')?.addEventListener('click', () => cartRenameModal(cart));
    document.querySelector('[data-cart-delete]')?.addEventListener('click', async () => {
        if (!confirm(`Delete cart "${cart.name}"? Slots referencing it will lose their cart link.`)) return;
        try {
            await API.del(`/api/carts/${cart.id}`);
            state.selectedCartId = null;
            await loadCartsTab();
            toast('Cart deleted', 'ok');
        } catch (e) { toast(e.message, 'error'); }
    });
    document.getElementById('cartAddMusicBtn')?.addEventListener('click', () => cartAddMusicMember(cart));
    document.getElementById('cartAddSpotBtn')?.addEventListener('click', () => cartAddSpotMember(cart));
    document.getElementById('cartSyncOrderBtn')?.addEventListener('click', async () => {
        try { await API.post(`/api/carts/${cart.id}/sync-order`, {}); toast('Synced from order', 'ok'); await renderCartDetail(cart.id); }
        catch (e) { toast(e.message, 'error'); }
    });
    document.querySelectorAll('[data-edit-member]').forEach(b =>
        b.addEventListener('click', () => cartEditMember(cart, b.dataset.editMember)));
    document.querySelectorAll('[data-delete-member]').forEach(b =>
        b.addEventListener('click', async () => {
            if (!confirm('Remove this member from the cart?')) return;
            try { await API.del(`/api/cart-members/${b.dataset.deleteMember}`); await renderCartDetail(cart.id); }
            catch (e) { toast(e.message, 'error'); }
        }));
}

function cartCreateModal() {
    const libraryOpts = state.cartCategories.library.map(c =>
        `<option value="${c}">${escapeHtml(categoryLabel(c))}</option>`).join('');
    const commercialOpts = state.cartCategories.commercial.map(c =>
        `<option value="${c}">${escapeHtml(categoryLabel(c))}</option>`).join('');
    const html = `
        <label>Name <input name="name" required /></label>
        <label>Source
            <select name="kind">
                <option value="MUSIC">Library file (songs, IDs, jingles, news, weather, promos, voicetracks)</option>
                <option value="COMMERCIAL">Spot (paid commercials, sponsored features)</option>
            </select>
        </label>
        <label>Category
            <select name="category" id="cartCategorySel">${libraryOpts}</select>
        </label>
        <label>Population
            <select name="source">
                <option value="MANUAL">Manual (pick members yourself)</option>
                <option value="ORDER">From an order (auto-sync spots)</option>
            </select>
        </label>
        <label class="hidden" id="cartOrderLabel">Order
            <select name="orderId"><option value="">(loading orders...)</option></select>
        </label>
        <label>Description <textarea name="description"></textarea></label>
        <p class="muted small">A sensible separation policy will be seeded for the chosen category — you can change it on the cart afterwards.</p>
    `;
    openModal('New cart', html, async () => {
        const form = document.getElementById('modalForm');
        const fd = new FormData(form);
        const body = {
            name: fd.get('name')?.toString().trim(),
            kind: fd.get('kind'),
            category: fd.get('category'),
            source: fd.get('source'),
            description: fd.get('description'),
        };
        if (body.source === 'ORDER') {
            const orderId = fd.get('orderId');
            if (!orderId) { toast('Pick an order', 'error'); return false; }
            body.orderId = orderId;
        }
        try {
            const created = await API.post(`/api/stations/${state.currentStationId}/carts`, body);
            state.selectedCartId = created.id;
            await loadCartsTab();
            toast('Cart created', 'ok');
            return true;
        } catch (e) { toast(e.message, 'error'); return false; }
    });
    const kindSel = document.querySelector('select[name="kind"]');
    const catSel = document.getElementById('cartCategorySel');
    const sourceSel = document.querySelector('select[name="source"]');
    const orderLabel = document.getElementById('cartOrderLabel');
    const orderSel = document.querySelector('select[name="orderId"]');

    const refreshCats = () => {
        catSel.innerHTML = kindSel.value === 'COMMERCIAL' ? commercialOpts : libraryOpts;
        // Order-backed only makes sense for spot-sourced carts.
        if (kindSel.value !== 'COMMERCIAL' && sourceSel.value === 'ORDER') {
            sourceSel.value = 'MANUAL';
            orderLabel.classList.add('hidden');
        }
    };
    kindSel.addEventListener('change', refreshCats);

    const refreshOrders = async () => {
        if (sourceSel.value !== 'ORDER') { orderLabel.classList.add('hidden'); return; }
        orderLabel.classList.remove('hidden');
        try {
            const orders = await API.get('/api/orders?stationId=' + encodeURIComponent(state.currentStationId));
            orderSel.innerHTML = orders.map(o => `<option value="${o.id}">${escapeHtml(o.name)}</option>`).join('') || '<option value="">(no orders)</option>';
        } catch (e) {
            orderSel.innerHTML = '<option value="">(orders failed: ' + escapeHtml(e.message) + ')</option>';
        }
    };
    sourceSel.addEventListener('change', refreshOrders);
    refreshCats();
    refreshOrders();
}

function cartRenameModal(cart) {
    const cats = catListForKind(cart.kind === 'COMMERCIAL' ? 'COMMERCIAL_CART' : 'MUSIC_CART');
    const catOpts = cats.map(c =>
        `<option value="${escapeAttr(c)}" ${cart.category === c ? 'selected' : ''}>${escapeHtml(categoryLabel(c))}</option>`).join('');
    const html = `
        <label>Name <input name="name" value="${escapeAttr(cart.name)}" required /></label>
        <label>Category
            <select name="category">${catOpts}</select>
        </label>
        <p class="muted small">Cart type (library vs. spot) can't change once members exist — category can.</p>
        <label>Description <textarea name="description">${escapeHtml(cart.description || '')}</textarea></label>
    `;
    openModal('Edit cart', html, async () => {
        const fd = new FormData(document.getElementById('modalForm'));
        try {
            await API.put(`/api/carts/${cart.id}`, {
                name: fd.get('name'),
                category: fd.get('category'),
                description: fd.get('description'),
            });
            await loadCartsTab(); toast('Saved', 'ok'); return true;
        } catch (e) { toast(e.message, 'error'); return false; }
    });
}

async function cartAddSpotMember(cart) {
    let pool = state.spotPool;
    if (!pool || !pool.length) {
        try {
            const orders = await API.get('/api/orders?stationId=' + encodeURIComponent(state.currentStationId));
            pool = [];
            for (const o of orders) {
                const spots = await API.get(`/api/orders/${o.id}/spots`);
                spots.forEach(s => pool.push({ order: o, spot: s }));
            }
        } catch (e) { toast(e.message, 'error'); return; }
    }
    if (!pool.length) { toast('No spots available — create an order first.', 'info'); return; }
    const choice = await pickFromList('Pick a spot', pool.map(p => ({
        id: p.spot.id,
        title: p.spot.label,
        meta: `${p.order.name} · ${p.spot.lengthSeconds}s`,
        _data: p.spot,
    })));
    if (!choice) return;
    const body = {
        spotId: choice._data.id,
        sponsor: choice._data.label,
        lengthSeconds: choice._data.lengthSeconds,
        librtimeFileId: choice._data.librtimeFileId,
        weight: 1, enabled: true,
    };
    try { await API.post(`/api/carts/${cart.id}/members`, body); await renderCartDetail(cart.id); toast('Added', 'ok'); }
    catch (e) { toast(e.message, 'error'); }
}

function cartEditMember(cart, memberId) {
    const m = (state.cartMembers || []).find(x => x.id === memberId);
    if (!m) return;
    const html = `
        <label>Weight <input type="number" min="1" name="weight" value="${m.weight}" /></label>
        <label>Artist <input name="artist" value="${escapeAttr(m.artist || '')}" /></label>
        <label>Title <input name="title" value="${escapeAttr(m.title || '')}" /></label>
        <label>Sponsor <input name="sponsor" value="${escapeAttr(m.sponsor || '')}" /></label>
        <label>Product <input name="product" value="${escapeAttr(m.product || '')}" /></label>
        <label>Length (seconds) <input type="number" min="0" name="lengthSeconds" value="${m.lengthSeconds || ''}" /></label>
        <label class="checkbox"><input type="checkbox" name="enabled" ${m.enabled ? 'checked' : ''}/> Enabled</label>
    `;
    openModal('Edit cart member', html, async () => {
        const fd = new FormData(document.getElementById('modalForm'));
        const body = {
            weight: parseInt(fd.get('weight')) || 1,
            artist: fd.get('artist') || null,
            title: fd.get('title') || null,
            sponsor: fd.get('sponsor') || null,
            product: fd.get('product') || null,
            lengthSeconds: fd.get('lengthSeconds') ? parseInt(fd.get('lengthSeconds')) : null,
            enabled: fd.get('enabled') === 'on',
        };
        try { await API.put(`/api/cart-members/${memberId}`, body); await renderCartDetail(cart.id); return true; }
        catch (e) { toast(e.message, 'error'); return false; }
    });
}

// ---------- Clocks ----------

async function loadClocksForCurrentStation() {
    if (!state.currentStationId) { state.clocks = []; return; }
    try {
        state.clocks = await API.get(`/api/stations/${state.currentStationId}/clocks`);
    } catch (e) {
        state.clocks = [];
        toast('Could not load clocks: ' + e.message, 'error');
    }
    try {
        state.cartCategories = await API.get('/api/cart-categories');
    } catch {
        state.cartCategories = { library: [], commercial: [] };
    }
}

function catListForKind(kind) {
    if (!state.cartCategories) return [];
    return kind === 'COMMERCIAL_CART' ? (state.cartCategories.commercial || [])
        : (state.cartCategories.library || []);
}

function syncCartBindRow(row) {
    if (!row) return;
    const mode = row.querySelector('.cart-bind-mode');
    const cid = row.querySelector('[name="cartId"]');
    const cat = row.querySelector('[name="cartCategory"]');
    if (!mode || !cid || !cat) return;
    const catMode = mode.value === 'category';
    cid.disabled = catMode;
    cat.disabled = !catMode;
    const kindSel = row.querySelector('[name="kind"]');
    const k = kindSel ? kindSel.value : 'MUSIC_CART';
    if (catMode) {
        const sel = cat.value;
        cat.innerHTML = catListForKind(k).map(c =>
            `<option value="${escapeAttr(c)}" ${String(sel || '') === c ? 'selected' : ''}>${escapeHtml(c)}</option>`).join('');
    }
}

function clockCartOptionsHtml(kind, selectedId) {
    const want = kind === 'COMMERCIAL_CART' ? 'COMMERCIAL' : 'MUSIC';
    return (state.carts || []).filter(c => c.kind === want).map(c =>
        `<option value="${c.id}" ${String(selectedId || '') === c.id ? 'selected' : ''}>${escapeHtml(c.name)} (${escapeHtml((c.category || '').toLowerCase())})</option>`).join('');
}

function clockCategoryOptionsHtml(kind, selectedCat) {
    return catListForKind(kind).map(c =>
        `<option value="${escapeAttr(c)}" ${String(selectedCat || '') === c ? 'selected' : ''}>${escapeHtml(c)}</option>`).join('');
}

async function loadClocksTab() {
    await loadClocksForCurrentStation();
    if (!state.carts.length) await loadCartsForCurrentStation();
    renderClockList();
    if (state.selectedClockId) {
        const found = state.clocks.find(c => c.id === state.selectedClockId);
        if (found) renderClockDetail(found);
        else { state.selectedClockId = null; renderClockBlank(); }
    } else renderClockBlank();
}

function renderClockBlank() {
    document.getElementById('clockDetail').innerHTML =
        '<div class="empty-state"><strong>No clock selected</strong>Pick a clock on the left or create a new one.</div>';
}

function renderClockList() {
    document.getElementById('clockListCount').textContent = state.clocks.length ? '(' + state.clocks.length + ')' : '';
    if (!state.clocks.length) {
        document.getElementById('clockList').innerHTML =
            '<div class="empty-state"><strong>No clocks yet</strong>Click "New clock" to get started.</div>';
        return;
    }
    document.getElementById('clockList').innerHTML = state.clocks.map(c => `
        <div class="cart-row ${state.selectedClockId === c.id ? 'selected' : ''}" data-clock-id="${c.id}">
            <div><strong>${escapeHtml(c.name)}</strong></div>
            <div class="muted small">${(c.slots || []).length} slots${c.description ? ' · ' + escapeHtml(c.description) : ''}</div>
        </div>`).join('');
    document.querySelectorAll('[data-clock-id]').forEach(el =>
        el.addEventListener('click', () => {
            state.selectedClockId = el.dataset.clockId;
            renderClockList();
            const found = state.clocks.find(c => c.id === el.dataset.clockId);
            if (found) renderClockDetail(found);
        }));
}

function renderClockDetail(clock) {
    const slots = clock.slots || [];
    const slotRows = slots.length ? slots.map((s, i) => clockSlotRowHtml(s, i)).join('')
        : '<div class="muted">No slots yet — add some below.</div>';
    document.getElementById('clockDetail').innerHTML = `
        <div class="row">
            <h3>${escapeHtml(clock.name)}</h3>
            <span class="grow"></span>
            <button class="link" data-clock-rename>Rename</button>
            <button class="link danger" data-clock-delete>Delete</button>
        </div>
        <p class="muted small">${escapeHtml(clock.description || '')}</p>

        <div class="clock-panel-help muted small">
            <strong>On this screen:</strong> each <strong>slot</strong> is one step in the hour. For music/commercial rows choose either a <strong>specific cart</strong> or a <strong>category</strong> (VOICETRACK, MUSIC, …) — at <strong>push</strong> LibreLog picks the first cart in that category (alphabetically) whose rotation and spot <em>day parts</em> allow a pick at that slot time.
            <strong>When</strong> this template runs is set in <strong>Day Builder → Clock schedule</strong>. Click <strong>Save slots</strong> when finished.
        </div>

        <div id="clockSlotsEditor">${slotRows}</div>

        <div class="row" style="margin-top:8px">
            <button class="secondary" id="clockAddSlotBtn">+ Add slot</button>
            <span class="grow"></span>
            <button id="clockSaveSlotsBtn">Save slots</button>
        </div>
    `;
    document.querySelector('[data-clock-rename]').addEventListener('click', () => clockRenameModal(clock));
    document.querySelector('[data-clock-delete]').addEventListener('click', async () => {
        if (!confirm(`Delete clock "${clock.name}"?`)) return;
        try { await API.del(`/api/clocks/${clock.id}`); state.selectedClockId = null; await loadClocksTab(); }
        catch (e) { toast(e.message, 'error'); }
    });
    document.getElementById('clockAddSlotBtn').addEventListener('click', () => {
        const editor = document.getElementById('clockSlotsEditor');
        const i = editor.querySelectorAll('.clock-slot-row').length;
        editor.insertAdjacentHTML('beforeend', clockSlotRowHtml({ kind: 'MUSIC_CART' }, i));
        const rows = editor.querySelectorAll('.clock-slot-row');
        syncCartBindRow(rows[rows.length - 1]);
    });
    document.getElementById('clockSaveSlotsBtn').addEventListener('click', async () => {
        const rows = document.querySelectorAll('.clock-slot-row');
        const payload = [];
        for (const r of rows) {
            const kind = r.querySelector('[name="kind"]').value;
            const lf = r.querySelector('[name="librtimeFileId"]').value;
            const spotId = r.querySelector('[name="spotId"]').value || null;
            const label = r.querySelector('[name="label"]').value || null;
            const len = r.querySelector('[name="defaultLengthSeconds"]').value;
            const bindMode = r.querySelector('.cart-bind-mode')?.value || 'cart';
            let cartId = null;
            let cartCategory = null;
            if (kind === 'MUSIC_CART' || kind === 'COMMERCIAL_CART') {
                if (bindMode === 'category') {
                    cartCategory = r.querySelector('[name="cartCategory"]')?.value || null;
                    if (!cartCategory) {
                        toast('Pick a cart category for every row in "Category @ push" mode.', 'error');
                        return;
                    }
                } else {
                    cartId = r.querySelector('[name="cartId"]')?.value || null;
                    if (kind === 'MUSIC_CART' && !cartId) {
                        toast('Music cart slots need a specific cart, or switch to Category @ push.', 'error');
                        return;
                    }
                }
            }
            payload.push({
                kind,
                cartId: (kind === 'MUSIC_CART' || kind === 'COMMERCIAL_CART') ? cartId : null,
                cartCategory: (kind === 'MUSIC_CART' || kind === 'COMMERCIAL_CART') ? cartCategory : null,
                librtimeFileId: kind === 'TRACK' && lf ? parseInt(lf) : null,
                spotId: kind === 'SPOT' ? spotId : null,
                label,
                defaultLengthSeconds: len ? parseInt(len) : null,
            });
        }
        try {
            await API.put(`/api/clocks/${clock.id}/slots`, payload);
            toast('Slots saved', 'ok');
            await loadClocksTab();
        } catch (e) { toast(e.message, 'error'); }
    });
    document.querySelectorAll('.clock-slot-row [data-remove-clock-slot]').forEach(b =>
        b.addEventListener('click', () => b.closest('.clock-slot-row').remove()));
    document.querySelectorAll('.clock-slot-row').forEach(syncCartBindRow);
}

function clockSlotRowHtml(s, i) {
    const k = s.kind || 'MUSIC_CART';
    const useCat = !!(s.cartCategory && !s.cartId);
    const bindMode = useCat ? 'category' : 'cart';
    const showCartBind = k === 'MUSIC_CART' || k === 'COMMERCIAL_CART';
    return `<div class="clock-slot-row">
        <div class="clock-slot-row-main">
        <span class="clock-slot-num">${i + 1}.</span>
        <select name="kind">
            ${['MUSIC_CART','COMMERCIAL_CART','TRACK','SPOT','VOICETRACK','NOTE'].map(x =>
                `<option value="${x}" ${x === k ? 'selected' : ''}>${x.replace('_',' ').toLowerCase()}</option>`).join('')}
        </select>
        <span class="clock-cart-bind-wrap" style="display:${showCartBind ? 'inline-flex' : 'none'};flex-wrap:wrap;gap:6px;align-items:center;margin-left:4px">
            <select class="cart-bind-mode" title="Named cart vs category pool">
                <option value="cart" ${bindMode === 'cart' ? 'selected' : ''}>Specific cart</option>
                <option value="category" ${bindMode === 'category' ? 'selected' : ''}>Category @ push</option>
            </select>
            <select name="cartId" ${bindMode === 'category' ? 'disabled' : ''}>
                <option value="">(pick cart)</option>${clockCartOptionsHtml(k, s.cartId)}
            </select>
            <select name="cartCategory" ${bindMode === 'cart' ? 'disabled' : ''}>${clockCategoryOptionsHtml(k, s.cartCategory)}</select>
        </span>
        <input name="librtimeFileId" type="number" placeholder="file id" value="${s.librtimeFileId ?? ''}" ${k === 'TRACK' ? '' : 'disabled'} />
        <input name="spotId" placeholder="spot uuid" value="${s.spotId ?? ''}" ${k === 'SPOT' ? '' : 'disabled'} />
        <input name="label" placeholder="label" value="${escapeAttr(s.label || '')}" />
        <input name="defaultLengthSeconds" type="number" placeholder="len s" value="${s.defaultLengthSeconds ?? ''}" />
        <button class="link" data-remove-clock-slot>x</button>
        </div>
    </div>`;
}

document.addEventListener('change', (e) => {
    if (e.target.matches('.clock-slot-row [name="kind"]')) {
        const row = e.target.closest('.clock-slot-row');
        const k = e.target.value;
        row.querySelector('[name="librtimeFileId"]').disabled = k !== 'TRACK';
        row.querySelector('[name="spotId"]').disabled = k !== 'SPOT';
        const wrap = row.querySelector('.clock-cart-bind-wrap');
        if (wrap) {
            wrap.style.display = (k === 'MUSIC_CART' || k === 'COMMERCIAL_CART') ? 'inline-flex' : 'none';
        }
        if (k === 'MUSIC_CART' || k === 'COMMERCIAL_CART') {
            const cid = row.querySelector('[name="cartId"]');
            const prevId = cid.value;
            cid.innerHTML = '<option value="">(pick cart)</option>' + clockCartOptionsHtml(k, prevId);
            syncCartBindRow(row);
        }
    }
    if (e.target.matches('.clock-slot-row .cart-bind-mode')) {
        syncCartBindRow(e.target.closest('.clock-slot-row'));
    }
});

function clockRenameModal(clock) {
    const html = `
        <label>Name <input name="name" value="${escapeAttr(clock.name)}" required /></label>
        <label>Description <textarea name="description">${escapeHtml(clock.description || '')}</textarea></label>
    `;
    openModal('Rename clock', html, async () => {
        const fd = new FormData(document.getElementById('modalForm'));
        try {
            await API.put(`/api/clocks/${clock.id}`, { name: fd.get('name'), description: fd.get('description') });
            await loadClocksTab(); return true;
        } catch (e) { toast(e.message, 'error'); return false; }
    });
}

function clockCreateModal() {
    const html = `
        <label>Name <input name="name" required placeholder="e.g. Morning Drive Hour" /></label>
        <label>Description <textarea name="description"></textarea></label>
    `;
    openModal('New clock', html, async () => {
        const fd = new FormData(document.getElementById('modalForm'));
        try {
            const created = await API.post(`/api/stations/${state.currentStationId}/clocks`,
                { name: fd.get('name'), description: fd.get('description') });
            state.selectedClockId = created.id;
            await loadClocksTab();
            return true;
        } catch (e) { toast(e.message, 'error'); return false; }
    });
}

// ---------- Starter guide (read-me-first walkthrough) ----------
const STARTER_GUIDE_HTML = `
<p class="muted">LibreLog turns sales into an on-air, reconciled log. Two lanes — <strong>sales</strong>
and <strong>programming</strong> — meet in the <strong>Day Builder</strong>, which pushes the finished
day into LibreTime. Here is the whole flow.</p>

<h4>Once, to set up</h4>
<ol class="guide-steps">
    <li><strong>Station &amp; time zone</strong> — create your station; the time zone defines what "today" means everywhere else.</li>
    <li><strong>Connect LibreTime</strong> — save the base URL and a LibreTime user, then <em>Test</em>. LibreLog reads shows/library from it and pushes the schedule back.</li>
</ol>

<h4>Sales lane — turning an order into an approved spot</h4>
<ol class="guide-steps">
    <li><strong>Customer → Order</strong> — add the advertiser, then an order (standard date-range, or founding-member allowance).</li>
    <li><strong>Add a spot</strong> — each spot starts as <span class="badge status-draft">Draft</span>. Attach the produced audio (a LibreTime file).</li>
    <li><strong>Produce → Approve</strong> — mark it <span class="badge status-produced">Produced</span>, then <span class="badge status-approved">Approved</span>.
        <em>Only approved spots can air</em> — draft/produced spots are skipped at push. (Removing the audio drops a spot back to Draft.)</li>
</ol>

<h4>Programming lane — building the sound</h4>
<ol class="guide-steps">
    <li><strong>Carts</strong> are rotating bins. A <em>commercial cart sourced "from an order"</em> auto-fills with that order's spots (and re-syncs whenever you change them). Music/news/imaging carts pull library files.</li>
    <li><strong>Freshness</strong> — each cart picks either by <em>Rotation</em> (round-robin: music, commercials) or <em>Newest first</em> with a <em>max age</em> (news, voicetracks — so stale audio never airs).</li>
    <li><strong>Clocks</strong> are reusable hour patterns: an ordered list of slots (music cart, commercial cart, fixed track, spot).</li>
</ol>

<h4>Day Builder — where the lanes meet</h4>
<ol class="guide-steps">
    <li><strong>Pick the date</strong> (station-local) and <strong>lock the day</strong> so only you edit it.</li>
    <li><strong>Clock schedule</strong> — map local hours to clocks, then <strong>Apply clock schedule</strong> so each show gets the right template. <strong>Save draft</strong>.</li>
    <li><strong>Preview push</strong> (dry run), then <strong>Push to LibreTime</strong>. At push, carts resolve to real files using rotation/freshness, separation cooldowns, time windows, show targeting, and the approval gate. Approved spots flip to <span class="badge status-trafficked">Trafficked</span>.</li>
    <li><strong>After air → Pull playback</strong> — imports what LibreTime actually played and reconciles it against your schedule. Each order's <em>Reconciliation</em> shows matched vs. missed.</li>
</ol>

<p class="muted small">Tip: pre-scheduled <em>tracks</em> (music, interviews, content) get exact air times you can announce; <em>carts</em> stay dynamic and resolve at push so news and ads are fresh.</p>
`;

function showStarterGuide() {
    const dlg = document.getElementById('modal');
    document.getElementById('modalTitle').textContent = 'How LibreLog works, end to end';
    document.getElementById('modalBody').innerHTML = STARTER_GUIDE_HTML;
    const save = document.getElementById('modalSave');
    const cancel = document.getElementById('modalCancel');
    const prevSave = save.textContent;
    save.textContent = 'Got it';
    cancel.style.display = 'none';
    const form = document.getElementById('modalForm');
    form.onsubmit = null; // method="dialog" closes it; no save action
    const restore = () => {
        save.textContent = prevSave;
        cancel.style.display = '';
        dlg.removeEventListener('close', restore);
    };
    dlg.addEventListener('close', restore);
    dlg.showModal();
}

// ---------- Guided tours (Driver.js) ----------
let librelogActiveTour = null;

function closeTourHelpPanel() {
    const el = document.getElementById('tourHelpDetails');
    if (el) el.removeAttribute('open');
}

function librelogTourStep(selector, title, description, popoverSide, onHighlighted) {
    const step = {
        element: selector,
        popover: {
            title,
            description,
            side: popoverSide || 'bottom',
            align: 'start',
        },
    };
    if (onHighlighted) {
        step.onHighlighted = () => { onHighlighted(); };
    }
    return step;
}

function librelogTourDriverFactory() {
    const f = window.driver && window.driver.js && window.driver.js.driver;
    return typeof f === 'function' ? f : null;
}

function buildLibrelogTourSteps(route) {
    const S = librelogTourStep;

    const setup = [
        S('#stationPicker', 'Pick a station',
            'Everything below—LibreTime connection, carts, clocks, orders, and the day you build—uses the station selected here. Switch stations if you manage more than one.',
            'bottom'),
        S('aside#sidebar', 'Sidebar: main areas',
            'Use these links to move between scheduling (Day Builder, Carts, Clocks, Library) and sales setup (Customers, Orders), plus Station and LibreTime connection.',
            'right'),
        S('.nav-item[data-tab="stations"]', 'Station',
            'Set the station name, call letters, and especially the time zone. The time zone drives which calendar day you are building in Day Builder.',
            'right', () => showTab('stations')),
        S('#stationNewBtn', 'Add or edit stations',
            'Create your facility here before connecting LibreTime. Time zone should match how you think about “today” for traffic.',
            'bottom'),
        S('.nav-item[data-tab="connection"]', 'LibreTime connection',
            'LibreLog reads show instances, library files, and pushes your finished day into LibreTime. Save the base URL and LibreTime user credentials, then use Test.',
            'right', () => showTab('connection')),
        S('#connectionForm', 'Connection fields',
            'Base URL is your LibreTime site (https://…). Username and password are the LibreTime web/API user. Password can be left blank when saving if you are not changing it.',
            'bottom', () => { showTab('connection'); loadConnection(); }),
    ];

    const orders = [
        S('.nav-item[data-tab="customers"]', 'Customers',
            'Advertisers and clients live here. You need at least one customer before creating orders.',
            'right', () => showTab('customers')),
        S('#customerNewBtn', 'New customer',
            'Add name, contact, and notes. Orders are always tied to a customer.',
            'bottom'),
        S('.nav-item[data-tab="orders"]', 'Orders & spots',
            'An order is a contract: standard (date range + total spots) or founding member (monthly spot allowance, optional open end date, optional monthly amount).',
            'right', () => showTab('orders')),
        S('#orderNewBtn', 'New order',
            'Choose order type, dates, and limits. Then open the order and add spots—each spot is a creative (label, length, the produced LibreTime file).',
            'bottom'),
        S('#ordersBody', 'Order list & spot lifecycle',
            'Open an order to manage its spots. Each spot moves Draft → Produced → Approved using the buttons in the Status column. <strong>Only approved spots air</strong>; once pushed, a spot becomes Trafficked automatically. The order’s Reconciliation (after air) shows matched vs. missed.',
            'bottom'),
    ];

    const carts = [
        S('.nav-item[data-tab="carts"]', 'Carts',
            'Carts are rotating bins. Music carts pick library files; commercial carts pick spots. The day builder and clocks reference carts by name—not specific advertisers.',
            'right', () => showTab('carts')),
        S('#cartNewBtn', 'New cart',
            'Choose music vs commercial, a category (music, imaging, commercial, etc.), and source: manual members, or “from an order” to auto-sync that order’s spots.',
            'bottom'),
        S('#cartList', 'Cart list',
            'Select a cart to edit members, separation rules (cooldowns), and sync from order for order-backed carts.',
            'bottom'),
        S('#cartDetail', 'Cart detail & freshness',
            'After you pick a cart: add library files, set the separation policy, and choose a <strong>Freshness</strong> strategy. Use <em>Newest first</em> + a max age for news and voicetracks so stale audio never airs; leave <em>Rotation</em> for music and commercials. Order-backed carts re-sync automatically when the order’s spots change.',
            'bottom'),
    ];

    const clocks = [
        S('.nav-item[data-tab="clocks"]', 'Clocks',
            'A clock is a reusable hour template: an ordered list of slots (music cart, commercial cart, fixed track, spot, or placeholder).',
            'right', () => showTab('clocks')),
        S('#clockNewBtn', 'New clock',
            'Give the template a name (e.g. “Weekday hour”). Then add slots in order—this is the pattern that gets applied to show instances in Day Builder.',
            'bottom'),
        S('#clockList', 'Build slot order',
            'Pick a clock on the left, then use <strong>Add slot</strong> and <strong>Save slots</strong>. In Day Builder, the <strong>clock schedule</strong> decides which clock runs in which local hours; you can still apply one clock to a single show.',
            'bottom', () => showTab('clocks')),
    ];

    const library = [
        S('.nav-item[data-tab="library"]', 'LibreTime library',
            'Search files, see today’s show instances, and browse playlist/smart block names from LibreTime (read-only reference).',
            'right', () => showTab('library')),
        S('#libSearch', 'File search',
            'Filter by name, path, or description to find LibreTime file ids for tracks or to sanity-check media.',
            'bottom'),
    ];

    const daybuilder = [
        S('.nav-item[data-tab="dayBuilder"]', 'Day Builder',
            'Daily workflow: pick the date, lock the day, set the clock schedule (hours → templates) or lay out slots by hand, save, then push into LibreTime’s schedule for each show instance.',
            'right', () => showTab('dayBuilder')),
        S('#dayBuilderDate', 'Date',
            'Uses the station’s time zone. Shows come from LibreTime’s calendar for this local day.',
            'bottom', () => showTab('dayBuilder')),
        S('#dayBuilderLockBtn', 'Edit this day',
            'Acquire a short lock so only you edit the draft. Extend by saving; others see read-only until the lock expires or is released.',
            'bottom', () => showTab('dayBuilder')),
        S('#dayBuilderShows', 'Show instances',
            'These are the actual hours/shows LibreTime has on the calendar. Use the clock schedule to map hours to templates, or Apply clock on one show to override.',
            'bottom', () => showTab('dayBuilder')),
        S('#dayBuilderSlots', 'Slots',
            'Your trafficking: carts resolve to real files at push time; empty slots can be filled manually. Preview push shows a dry run.',
            'bottom', () => showTab('dayBuilder')),
        S('#dayBuilderSpotPool', 'Active spot pool',
            'Spots from orders whose dates include this day (and founding orders with no end date). Use when filling commercial slots.',
            'bottom', () => showTab('dayBuilder')),
        S('#dayBuilderApplyClockScheduleBtn', 'Apply clock schedule',
            'After you save clock schedule rows: each show gets the first clock whose local time window contains that show’s start. Then save slots and push.',
            'bottom', () => showTab('dayBuilder')),
        S('#dayBuilderSaveBtn', 'Save draft',
            'Writes your slot list to LibreLog. You must be holding the lock.',
            'bottom', () => showTab('dayBuilder')),
        S('#dayBuilderPushBtn', 'Push to LibreTime',
            'Clears and rewrites LibreTime’s schedule rows for those instances. Cart slots become concrete files using rotation/freshness, separation, time windows, show targeting, and the approval gate (unapproved spots are skipped). Approved spots that air flip to Trafficked.',
            'bottom', () => showTab('dayBuilder')),
        S('#dayBuilderPullPlayback', 'Pull playback',
            'After air, import what LibreTime logged as played—as-run and reconciliation context.',
            'bottom', () => showTab('dayBuilder')),
    ];

    const intro = [
        S('.page-header', 'Welcome to LibreLog',
            'This guided tour highlights each area. Use <strong>Next</strong> and <strong>Back</strong>, or press <strong>Esc</strong> to exit. You can run a section again anytime from <strong>Guided tour</strong>.',
            'bottom'),
    ];

    switch (route) {
        case 'setup':
            return setup;
        case 'orders':
            return orders;
        case 'carts':
            return carts;
        case 'clocks':
            return clocks;
        case 'daybuilder':
            return daybuilder;
        case 'full':
            return [...intro, ...setup, ...orders, ...carts, ...clocks, ...library, ...daybuilder];
        default:
            return [...intro, ...setup];
    }
}

function startLibrelogTour(route) {
    const factory = librelogTourDriverFactory();
    if (!factory) {
        toast('Tour could not start: the tour library did not load. Check your network and refresh the page.', 'error');
        return;
    }
    if (librelogActiveTour && typeof librelogActiveTour.isActive === 'function' && librelogActiveTour.isActive()) {
        try { librelogActiveTour.destroy(); } catch (e) { /* ignore */ }
        librelogActiveTour = null;
    }
    const steps = buildLibrelogTourSteps(route);
    if (!steps.length) {
        toast('No tour steps for: ' + route, 'info');
        return;
    }
    closeTourHelpPanel();
    librelogActiveTour = factory({
        showProgress: true,
        allowClose: true,
        smoothScroll: true,
        nextBtnText: 'Next →',
        prevBtnText: '← Back',
        doneBtnText: 'Done',
        steps,
        onDestroyed: () => { librelogActiveTour = null; },
    });
    librelogActiveTour.drive();
}

// ---------- Wire up ----------
window.addEventListener('DOMContentLoaded', () => {
    initTheme();
    startHeaderClock();
    loadVersionIntoElement(document.getElementById('loginVersionLine'));
    document.querySelectorAll('[data-theme-set]').forEach(b =>
        b.addEventListener('click', () => setTheme(b.dataset.themeSet)));

    document.getElementById('menuToggle').addEventListener('click', toggleSidebar);
    document.getElementById('sidebarCloseBtn').addEventListener('click', closeSidebar);
    document.getElementById('sidebarBackdrop').addEventListener('click', closeSidebar);

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
    document.getElementById('dayBuilderPreloadBtn').addEventListener('click', preloadFromLibreTime);
    document.getElementById('dayBuilderApplyClockScheduleBtn').addEventListener('click', applyClockScheduleToShows);
    document.getElementById('dayBuilderSaveClockScheduleBtn').addEventListener('click', saveClockScheduleDraft);
    document.getElementById('dayBuilderClockScheduleAddBtn').addEventListener('click', () => {
        if (!state.day || !state.day.lock || !state.day.lock.self) {
            toast('Acquire the day lock first.', 'error');
            return;
        }
        if (!state.clocks.length) {
            toast('Create a clock in the Clocks tab first.', 'info');
            return;
        }
        if (!Array.isArray(state.day.clockSegments)) state.day.clockSegments = [];
        state.day.clockSegments.push({
            localStartMinutes: 6 * 60,
            localEndMinutes: 12 * 60,
            clockTemplateId: state.clocks[0].id,
        });
        renderClockScheduleEditor();
    });
    document.getElementById('dayBuilderSaveBtn').addEventListener('click', saveDay);
    document.getElementById('dayBuilderPushBtn').addEventListener('click', pushDay);
    document.getElementById('dayBuilderReopenBtn').addEventListener('click', reopenDay);
    document.getElementById('dayBuilderForceUnlockBtn').addEventListener('click', forceUnlockDay);
    document.getElementById('dayBuilderPullPlayback').addEventListener('click', pullPlayback);
    const previewBtn = document.getElementById('dayBuilderPreviewBtn');
    if (previewBtn) previewBtn.addEventListener('click', previewPush);
    // Carts / Clocks
    document.getElementById('cartNewBtn')?.addEventListener('click', cartCreateModal);
    document.getElementById('cartFilterCategory')?.addEventListener('change', renderCartList);
    document.getElementById('clockNewBtn')?.addEventListener('click', clockCreateModal);
    // Picker
    document.getElementById('pickerCancel').addEventListener('click', () => closePicker(null));

    document.getElementById('starterGuideBtn')?.addEventListener('click', () => {
        document.getElementById('tourHelpDetails')?.removeAttribute('open');
        showStarterGuide();
    });
    document.getElementById('tourHelpDetails')?.addEventListener('click', (e) => {
        const btn = e.target.closest('[data-tour]');
        if (!btn) return;
        e.preventDefault();
        startLibrelogTour(btn.getAttribute('data-tour') || 'full');
    });

    if (state.token) {
        const cached = localStorage.getItem('librelog.user');
        if (cached) state.user = JSON.parse(cached);
        bootstrap().catch(() => logout());
    }
});
